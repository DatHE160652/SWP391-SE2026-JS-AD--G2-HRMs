package com.hrms.dao;

import com.hrms.model.*;
import com.hrms.service.PayrollException;
import com.hrms.service.PayrollCalculator;
import com.hrms.util.DBConnection;
import com.hrms.util.PayrollJson;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Every query is tenant-scoped. Writes are serialized per company and atomic. */
public class PayrollDAO {
    @FunctionalInterface
    public interface ConnectionFactory { Connection open() throws SQLException; }
    @FunctionalInterface
    private interface Work<T> { T run(Connection connection) throws SQLException; }
    private final ConnectionFactory connections;

    public PayrollDAO() { this(DBConnection::getConnection); }
    public PayrollDAO(ConnectionFactory connections) { this.connections = connections; }

    private static final String ELIGIBLE = PayrollSources.ELIGIBLE;
    private static final String SOURCES = PayrollSources.joins(PayrollSources.MONTH_END);

    private static final String PERIOD_COLUMNS = """
        SELECT p.*, (SELECT COUNT(*) FROM Payslips s WHERE s.PayrollPeriodId=p.PayrollPeriodId) EmployeeCount,
            COALESCE((SELECT SUM(s.NetSalary) FROM Payslips s WHERE s.PayrollPeriodId=p.PayrollPeriodId),0) TotalNetSalary,
            COALESCE((SELECT SUM(s.BaseSalary+s.ResponsibilityAllowance+s.Bonus) FROM Payslips s
                WHERE s.PayrollPeriodId=p.PayrollPeriodId),0) TotalIncome,
            COALESCE((SELECT SUM(s.Deduction) FROM Payslips s WHERE s.PayrollPeriodId=p.PayrollPeriodId),0) TotalDeduction,
            (SELECT u.DisplayName FROM Users u WHERE u.UserId=p.ReviewedByUserId AND u.CompanyId=p.CompanyId) ReviewedByName,
            (SELECT u.DisplayName FROM Users u WHERE u.UserId=p.ApprovedByUserId AND u.CompanyId=p.CompanyId) ApprovedByName
        """;
    private static final String PERIOD_SELECT = PERIOD_COLUMNS + """
        FROM PayrollPeriods p JOIN Companies company ON company.CompanyId=p.CompanyId
        WHERE p.CompanyId=?
        """;

    public List<PayrollPeriod> periods(int companyId) throws SQLException {
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement(
                PERIOD_SELECT + " ORDER BY p.Year DESC,p.Month DESC")) {
            s.setInt(1, companyId);
            try (ResultSet r = s.executeQuery()) {
                List<PayrollPeriod> result = new ArrayList<>();
                while (r.next()) result.add(period(r));
                return result;
            }
        }
    }

    public PayrollPeriod findPeriod(int companyId, int periodId) throws SQLException {
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement(
                PERIOD_SELECT + " AND p.PayrollPeriodId=?")) {
            s.setInt(1, companyId); s.setInt(2, periodId);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new PayrollException(404, "Không tìm thấy kỳ lương trong công ty của bạn.");
                return period(r);
            }
        }
    }

    public int createPeriod(int companyId, int userId, String name, int month, int year) throws SQLException {
        return write(companyId, userId, c -> {
            // write() holds the company lock until commit, including this duplicate check.
            try (PreparedStatement s = c.prepareStatement("""
                SELECT 1 FROM PayrollPeriods WHERE CompanyId=? AND Month=? AND Year=?
                """)) {
                s.setInt(1, companyId); s.setInt(2, month); s.setInt(3, year);
                try (ResultSet r = s.executeQuery()) {
                    if (r.next()) throw new PayrollException(409,
                            String.format("Kỳ lương tháng %02d/%d đã tồn tại trong công ty của bạn.", month, year));
                }
            }
            try (PreparedStatement s = c.prepareStatement("""
                INSERT INTO PayrollPeriods(CompanyId,PeriodName,Month,Year) VALUES(?,?,?,?)
                """, Statement.RETURN_GENERATED_KEYS)) {
                s.setInt(1, companyId); s.setString(2, name); s.setInt(3, month); s.setInt(4, year);
                s.executeUpdate();
                int id;
                try (ResultSet r = s.getGeneratedKeys()) { r.next(); id=r.getInt(1); }
                auditPeriod(c,companyId,userId,id,"PAYROLL_PERIOD_CREATED",null);
                return id;
            }
        });
    }

    public List<PayrollRow> salaries(int companyId) throws SQLException {
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement("""
            SELECT e.EmployeeId,e.EmployeeCode,e.FullName,e.Status,compensation.BaseSalary,d.DepartmentName,
            """ + PayrollSources.ALLOWANCE + " ResponsibilityAllowance," + PayrollSources.ISSUE + """
                SalaryIssue,compensation.EffectiveFrom SalaryEffectiveFrom,compensation.EffectiveTo SalaryEffectiveTo,
                CAST(0 AS DECIMAL(18,2)) Bonus,CAST(0 AS DECIMAL(18,2)) Deduction,
                CAST(NULL AS DECIMAL(20,2)) NetSalary,CAST(NULL AS NVARCHAR(300)) Note,
                CAST(NULL AS BIGINT) PayslipId
            FROM Employees e LEFT JOIN Departments d ON d.DepartmentId=e.DepartmentId AND d.CompanyId=e.CompanyId
            CROSS JOIN (SELECT YEAR(SYSDATETIME()) Year,MONTH(SYSDATETIME()) Month) p
            """ + PayrollSources.joins("CONVERT(date,SYSDATETIME())",true,false) + """
            WHERE e.CompanyId=? ORDER BY e.EmployeeCode
            """)) {
            s.setInt(1, companyId);
            return rows(s);
        }
    }

    public List<PayrollRow> periodRows(int companyId, int periodId) throws SQLException {
        return periodRows(companyId,periodId,false);
    }

    /** Adjustment entry includes every employee in the period's company, even before a contract exists. */
    public List<PayrollRow> adjustmentRows(int companyId, int periodId) throws SQLException {
        return periodRows(companyId,periodId,true);
    }

    private List<PayrollRow> periodRows(int companyId, int periodId, boolean adjustments) throws SQLException {
        findPeriod(companyId, periodId);
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement("""
            SELECT e.EmployeeId,e.EmployeeCode,e.FullName,e.Status,d.DepartmentName,
                COALESCE(s.BaseSalary,compensation.BaseSalary) BaseSalary,
            """ + "COALESCE(s.ResponsibilityAllowance," + PayrollSources.ALLOWANCE + ") ResponsibilityAllowance,"
                + "CASE WHEN s.PayslipId IS NULL THEN " + PayrollSources.ISSUE + " END SalaryIssue," + """
                compensation.EffectiveFrom SalaryEffectiveFrom,compensation.EffectiveTo SalaryEffectiveTo,
            """ + (adjustments?"COALESCE(a.Bonus,0) Bonus,COALESCE(a.Deduction,0) Deduction,":
                "COALESCE(s.Bonus,a.Bonus,0) Bonus,COALESCE(s.Deduction,a.Deduction,0) Deduction,") + """
                s.NetSalary,a.Note,s.PayslipId
            FROM PayrollPeriods p JOIN Employees e ON e.CompanyId=p.CompanyId
            """ + (adjustments?PayrollSources.joins(PayrollSources.MONTH_END,true):SOURCES) + """
            LEFT JOIN Departments d ON d.DepartmentId=e.DepartmentId AND d.CompanyId=e.CompanyId
            LEFT JOIN PayrollAdjustments a ON a.PayrollPeriodId=p.PayrollPeriodId AND a.EmployeeId=e.EmployeeId
            LEFT JOIN Payslips s ON s.PayrollPeriodId=p.PayrollPeriodId AND s.EmployeeId=e.EmployeeId
            WHERE p.CompanyId=? AND p.PayrollPeriodId=?
            """ + (adjustments?"":" AND " + """
            ((p.Status='Draft' AND (
            """ + ELIGIBLE + """
            )) OR (p.Status<>'Draft' AND s.PayslipId IS NOT NULL))
            """) + " ORDER BY e.EmployeeCode")) {
            s.setInt(1, companyId); s.setInt(2, periodId);
            return rows(s);
        }
    }

    public void setBaseSalary(int companyId, int userId, int employeeId, BigDecimal amount) throws SQLException {
        setBaseSalary(companyId,userId,employeeId,amount,LocalDate.now());
    }

    public void setBaseSalary(int companyId, int userId, int employeeId, BigDecimal amount, LocalDate effectiveFrom) throws SQLException {
        write(companyId, userId, c -> {
            LocalDate effectiveTo=PayrollSalaryHistory.save(c,companyId,userId,employeeId,amount,effectiveFrom);
            List<Integer> stale = new ArrayList<>();
            try (PreparedStatement s = c.prepareStatement("""
                SELECT p.PayrollPeriodId FROM PayrollPeriods p JOIN Employees e ON e.CompanyId=p.CompanyId
                """ + SOURCES + """
                WHERE p.CompanyId=? AND e.EmployeeId=? AND p.Status IN ('Calculated','Reviewed') AND
                """ + ELIGIBLE + " AND employment.PayrollDate>=? AND (? IS NULL OR employment.PayrollDate<?)")) {
                s.setInt(1, companyId); s.setInt(2, employeeId);
                s.setDate(3,Date.valueOf(effectiveFrom));s.setDate(4,effectiveTo==null?null:Date.valueOf(effectiveTo));
                s.setDate(5,effectiveTo==null?null:Date.valueOf(effectiveTo));
                try (ResultSet r = s.executeQuery()) { while (r.next()) stale.add(r.getInt(1)); }
            }
            for (int id : stale) invalidate(c, companyId, id);
            return null;
        });
    }

    public boolean saveAdjustment(int companyId, int userId, int periodId, int employeeId,
                               BigDecimal bonus, BigDecimal deduction, String note) throws SQLException {
        return write(companyId, userId, c -> {
            editable(lockPeriod(c, companyId, periodId));
            requireCompanyEmployee(c, companyId, employeeId);
            boolean changed=upsertAdjustment(c, userId, periodId, employeeId, bonus, deduction, note);
            if (changed) invalidate(c, companyId, periodId);
            return changed;
        });
    }

    public int importAdjustments(int companyId, int userId, int periodId,
                                 List<PayrollAdjustmentInput> input) throws SQLException {
        return write(companyId, userId, c -> {
            editable(lockPeriod(c, companyId, periodId));
            List<Integer> ids = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            for (PayrollAdjustmentInput row : input) {
                try (PreparedStatement s = c.prepareStatement("""
                    SELECT e.EmployeeId FROM Employees e JOIN PayrollPeriods p ON p.CompanyId=e.CompanyId
                    WHERE e.CompanyId=? AND p.PayrollPeriodId=? AND e.EmployeeCode=?
                    """)) {
                    s.setInt(1, companyId); s.setInt(2, periodId); s.setString(3, row.employeeCode());
                    try (ResultSet r = s.executeQuery()) {
                        if (r.next()) ids.add(r.getInt(1));
                        else { ids.add(null); errors.add("Dòng " + row.rowNumber() + ": mã " + row.employeeCode()
                                + " không thuộc công ty hoặc không có trong kỳ lương."); }
                    }
                }
            }
            if (!errors.isEmpty()) throw new PayrollException(400, "Chưa nhập dữ liệu. Hãy sửa các dòng lỗi.", errors);
            boolean changed=false;
            for (int i=0; i<input.size(); i++) {
                PayrollAdjustmentInput row = input.get(i);
                changed=upsertAdjustment(c, userId, periodId, ids.get(i), row.bonus(), row.deduction(), row.note()) || changed;
            }
            if (changed) invalidate(c, companyId, periodId);
            return input.size();
        });
    }

    /** A consistent, read-only preview. No payslip, status or adjustment is written. */
    public PayrollCalculation preview(int companyId,int periodId) throws SQLException {
        try (Connection c=connections.open()) {
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            c.setAutoCommit(false);
            try {
                try (PreparedStatement s=c.prepareStatement(PERIOD_SELECT+" AND p.PayrollPeriodId=?")) {
                    s.setInt(1,companyId);s.setInt(2,periodId);
                    try (ResultSet r=s.executeQuery()) {
                        if (!r.next()) throw new PayrollException(404,"Không tìm thấy kỳ lương trong công ty của bạn.");
                        editable(period(r));
                    }
                }
                PayrollCalculation result=calculation(c,companyId,periodId);
                c.commit();return result;
            } catch (SQLException | RuntimeException e) {
                try {c.rollback();} catch (SQLException rollback) {e.addSuppressed(rollback);}
                throw e;
            }
        }
    }

    public int calculate(int companyId, int userId, int periodId) throws SQLException {
        return write(companyId, userId, c -> {
            PayrollPeriod before=lockPeriod(c,companyId,periodId);
            editable(before);
            PayrollCalculation result=calculation(c,companyId,periodId);
            // Validate every employee before replacing any saved result.
            invalidate(c, companyId, periodId);
            // NetSalary is deliberately omitted: SQL Server computes it (PAY-BR-16).
            try (PreparedStatement s = c.prepareStatement("""
                INSERT INTO Payslips(PayrollPeriodId,EmployeeId,BaseSalary,ResponsibilityAllowance,Bonus,Deduction)
                VALUES(?,?,?,?,?,?)
                """)) {
                for (PayrollRow row:result.rows()) {
                    s.setInt(1,periodId);s.setInt(2,row.employeeId());s.setBigDecimal(3,row.baseSalary());
                    s.setBigDecimal(4,row.responsibilityAllowance());s.setBigDecimal(5,row.bonus());s.setBigDecimal(6,row.deduction());
                    s.addBatch();
                }
                s.executeBatch();
            }
            execute(c, "UPDATE PayrollPeriods SET Status='Calculated' WHERE CompanyId=? AND PayrollPeriodId=?", companyId, periodId);
            auditPeriod(c,companyId,userId,periodId,"PAYROLL_CALCULATED",before);
            return result.rows().size();
        });
    }

    public void transition(int companyId, int userId, int periodId, String action) throws SQLException {
        if (action==null || !java.util.Set.of("review","approve","close","reopen").contains(action))
            throw new PayrollException(400,"Thao tác kỳ lương không hợp lệ.");
        write(companyId, userId, c -> {
            PayrollPeriod p = lockPeriod(c, companyId, periodId);
            if (p.getPublishedAt() != null) throw new PayrollException(409, "Kỳ đã công bố được khóa để bảo toàn phiếu lương.");
            if ("close".equals(action)) {
                if (p.isClosed()) return null;
                execute(c, "UPDATE PayrollPeriods SET IsClosed=1 WHERE CompanyId=? AND PayrollPeriodId=?", companyId, periodId);
            } else if ("reopen".equals(action)) {
                invalidate(c, companyId, periodId);
                execute(c, "UPDATE PayrollPeriods SET IsClosed=0 WHERE CompanyId=? AND PayrollPeriodId=?", companyId, periodId);
            } else {
                if (p.isClosed()) throw new PayrollException(409, "Kỳ lương đang đóng. Hãy mở lại trước khi xử lý.");
                String expected = "review".equals(action) ? "Calculated" : "Reviewed";
                String target = "review".equals(action) ? "Reviewed" : "Approved";
                if (!expected.equals(p.getStatus())) throw new PayrollException(409, "Kỳ lương cần ở trạng thái " + expected + ".");
                if ("approve".equals(action) && (p.getReviewedByUserId()==null || p.getReviewedAt()==null))
                    throw new PayrollException(409,"Kỳ chưa có thông tin rà soát. Hãy tính lại và hoàn tất rà soát trước khi duyệt.");
                requireCurrentSnapshots(c, companyId, periodId);
                String metadata="review".equals(action)
                        ? "ReviewedByUserId=?,ReviewedAt=SYSUTCDATETIME(),ApprovedByUserId=NULL,ApprovedAt=NULL"
                        : "ApprovedByUserId=?,ApprovedAt=SYSUTCDATETIME()";
                try (PreparedStatement s=c.prepareStatement("UPDATE PayrollPeriods SET Status=?,"+metadata
                        +" WHERE CompanyId=? AND PayrollPeriodId=? AND Status=? AND IsClosed=0 AND PublishedAt IS NULL")) {
                    s.setString(1,target);s.setInt(2,userId);s.setInt(3,companyId);s.setInt(4,periodId);s.setString(5,expected);
                    if (s.executeUpdate()!=1) throw new PayrollException(409,"Trạng thái kỳ lương đã thay đổi. Hãy tải lại.");
                }
            }
            auditPeriod(c,companyId,userId,periodId,switch(action) {
                case "review" -> "PAYROLL_REVIEWED";
                case "approve" -> "PAYROLL_APPROVED";
                case "close" -> "PAYROLL_PERIOD_CLOSED";
                default -> "PAYROLL_PERIOD_REOPENED";
            },p);
            return null;
        });
    }

    public int publish(int companyId, int userId, int periodId) throws SQLException {
        return write(companyId, userId, c -> {
            PayrollPeriod p = lockPeriod(c, companyId, periodId);
            if (p.getPublishedAt() != null) return 0; // Idempotent; notifications are never duplicated.
            if (!"Approved".equals(p.getStatus()) || p.isClosed())
                throw new PayrollException(409, "Chỉ công bố kỳ đã Approved và đang mở.");
            int count;
            try (PreparedStatement s = c.prepareStatement("""
                INSERT INTO PayrollNotifications(PayslipId,EmployeeId,CompanyId)
                SELECT s.PayslipId,s.EmployeeId,p.CompanyId FROM Payslips s
                JOIN PayrollPeriods p ON p.PayrollPeriodId=s.PayrollPeriodId
                JOIN Employees e ON e.EmployeeId=s.EmployeeId AND e.CompanyId=p.CompanyId
                WHERE p.CompanyId=? AND p.PayrollPeriodId=?
                """)) {
                s.setInt(1, companyId); s.setInt(2, periodId); count = s.executeUpdate();
            }
            if (count == 0) throw new PayrollException(409, "Chưa có kết quả tính lương để công bố.");
            try (PreparedStatement s = c.prepareStatement("""
                UPDATE PayrollPeriods SET PublishedAt=SYSUTCDATETIME(),PublishedByUserId=?,IsClosed=1
                WHERE CompanyId=? AND PayrollPeriodId=?
                """)) {
                s.setInt(1, userId); s.setInt(2, companyId); s.setInt(3, periodId); s.executeUpdate();
            }
            auditPeriod(c,companyId,userId,periodId,"PAYROLL_PUBLISHED",p);
            return count;
        });
    }

    private static final String SLIP_SELECT = """
        SELECT s.*,p.CompanyId,p.PeriodName,p.Month,p.Year,p.PublishedAt,p.Status PeriodStatus,e.EmployeeCode,e.FullName,c.CompanyName
        FROM Payslips s JOIN PayrollPeriods p ON p.PayrollPeriodId=s.PayrollPeriodId
        JOIN Employees e ON e.EmployeeId=s.EmployeeId AND e.CompanyId=p.CompanyId
        JOIN Companies c ON c.CompanyId=p.CompanyId
        """;

    public List<PayrollSlipView> periodPayslips(int companyId,int periodId) throws SQLException {
        findPeriod(companyId,periodId);
        try (Connection c=connections.open();PreparedStatement s=c.prepareStatement(SLIP_SELECT+"""
            WHERE p.CompanyId=? AND p.PayrollPeriodId=? ORDER BY e.EmployeeCode,s.PayslipId
            """)) {
            s.setInt(1,companyId);s.setInt(2,periodId);
            try (ResultSet r=s.executeQuery()) {
                List<PayrollSlipView> result=new ArrayList<>();
                while (r.next()) result.add(slip(r));
                return result;
            }
        }
    }

    public List<PayrollSlipView> ownPayslips(int companyId, int employeeId) throws SQLException {
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement(SLIP_SELECT + """
            WHERE p.CompanyId=? AND e.EmployeeId=? AND p.PublishedAt IS NOT NULL AND p.Status='Approved'
            ORDER BY p.Year DESC,p.Month DESC
            """)) {
            s.setInt(1, companyId); s.setInt(2, employeeId);
            try (ResultSet r = s.executeQuery()) {
                List<PayrollSlipView> result = new ArrayList<>();
                while (r.next()) result.add(slip(r));
                return result;
            }
        }
    }

    public PayrollSlipView payslip(int companyId, Integer employeeId, long id) throws SQLException {
        String restriction = employeeId == null ? "" : " AND e.EmployeeId=? AND p.PublishedAt IS NOT NULL AND p.Status='Approved'";
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement(SLIP_SELECT
                + " WHERE p.CompanyId=? AND s.PayslipId=?" + restriction)) {
            s.setInt(1, companyId); s.setLong(2, id); if (employeeId != null) s.setInt(3, employeeId);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new PayrollException(404, "Không tìm thấy phiếu lương.");
                return slip(r);
            }
        }
    }

    public List<PayrollNotification> notifications(int companyId, int employeeId) throws SQLException {
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement("""
            SELECT TOP 50 n.NotificationId,n.PayslipId,n.CreatedAt,n.ReadAt,p.PeriodName
            FROM PayrollNotifications n JOIN Payslips s ON s.PayslipId=n.PayslipId AND s.EmployeeId=n.EmployeeId
            JOIN PayrollPeriods p ON p.PayrollPeriodId=s.PayrollPeriodId AND p.CompanyId=n.CompanyId
            JOIN Employees e ON e.EmployeeId=s.EmployeeId AND e.CompanyId=p.CompanyId
            WHERE n.CompanyId=? AND n.EmployeeId=? AND p.PublishedAt IS NOT NULL AND p.Status='Approved'
            ORDER BY n.CreatedAt DESC,n.NotificationId DESC
            """)) {
            s.setInt(1, companyId); s.setInt(2, employeeId);
            try (ResultSet r = s.executeQuery()) {
                List<PayrollNotification> result = new ArrayList<>();
                while (r.next()) result.add(new PayrollNotification(r.getLong("NotificationId"),r.getLong("PayslipId"),
                        r.getString("PeriodName"),time(r,"CreatedAt"),time(r,"ReadAt")));
                return result;
            }
        }
    }

    public void readNotification(int companyId, int employeeId, long id) throws SQLException {
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement("""
            UPDATE n SET ReadAt=COALESCE(n.ReadAt,SYSUTCDATETIME())
            FROM PayrollNotifications n JOIN Payslips s ON s.PayslipId=n.PayslipId AND s.EmployeeId=n.EmployeeId
            JOIN PayrollPeriods p ON p.PayrollPeriodId=s.PayrollPeriodId AND p.CompanyId=n.CompanyId
            JOIN Employees e ON e.EmployeeId=s.EmployeeId AND e.CompanyId=p.CompanyId
            WHERE n.CompanyId=? AND n.EmployeeId=? AND n.NotificationId=?
                AND p.PublishedAt IS NOT NULL AND p.Status='Approved'
            """)) {
            s.setInt(1, companyId); s.setInt(2, employeeId); s.setLong(3,id);
            if (s.executeUpdate() != 1) throw new PayrollException(404,"Không tìm thấy thông báo.");
        }
    }

    private <T> T write(int companyId, int userId, Work<T> work) throws SQLException {
        try (Connection c = connections.open()) {
            try (Statement options = c.createStatement()) {
                options.execute("SET ANSI_NULLS ON; SET QUOTED_IDENTIFIER ON; SET ANSI_PADDING ON; SET ANSI_WARNINGS ON; "
                        + "SET ARITHABORT ON; SET CONCAT_NULL_YIELDS_NULL ON; SET NUMERIC_ROUNDABORT OFF;");
            }
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            c.setAutoCommit(false);
            try {
                try (PreparedStatement s = c.prepareStatement("SELECT CompanyId FROM Companies WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=?")) {
                    s.setInt(1,companyId);
                    try (ResultSet r = s.executeQuery()) { if (!r.next()) throw new PayrollException(404,"Không tìm thấy công ty."); }
                }
                try (PreparedStatement s = c.prepareStatement("""
                    SELECT UserId FROM dbo.vw_PayrollOperators WHERE UserId=? AND CompanyId=?
                    """)) {
                    s.setInt(1,userId); s.setInt(2,companyId);
                    try (ResultSet r = s.executeQuery()) { if (!r.next()) throw new PayrollException(403,"Bạn không có quyền quản lý lương của công ty này."); }
                }
                T result = work.run(c); c.commit(); return result;
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                if (e instanceof SQLException sql && (sql.getErrorCode()==2601 || sql.getErrorCode()==2627))
                    throw new PayrollException(409,"Dữ liệu đã tồn tại. Mỗi công ty chỉ có một kỳ cho cùng tháng/năm.");
                throw e;
            }
        }
    }

    private PayrollPeriod lockPeriod(Connection c, int companyId, int periodId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(PERIOD_COLUMNS + """
            FROM PayrollPeriods p WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND PayrollPeriodId=?
            """)) {
            s.setInt(1,companyId); s.setInt(2,periodId);
            try (ResultSet r=s.executeQuery()) {
                if (!r.next()) throw new PayrollException(404,"Không tìm thấy kỳ lương trong công ty của bạn.");
                return period(r);
            }
        }
    }

    private void editable(PayrollPeriod p) {
        if (p.isClosed() || "Approved".equals(p.getStatus()) || p.getPublishedAt()!=null)
            throw new PayrollException(409,"Kỳ lương đã đóng hoặc phê duyệt. Chỉ mở lại kỳ chưa công bố để chỉnh sửa.");
    }

    private void requireCurrentSnapshots(Connection c, int companyId, int periodId) throws SQLException {
        PayrollCalculation expected=calculation(c,companyId,periodId);
        java.util.Map<Integer,PayrollRow> remaining=new java.util.HashMap<>();
        for (PayrollRow row:expected.rows()) remaining.put(row.employeeId(),row);
        // Read every saved slip, including any invalid legacy/foreign employee reference.
        // Serializable source and slip reads are held until the state and audit commit.
        try (PreparedStatement s=c.prepareStatement("""
            SELECT s.EmployeeId,s.BaseSalary,s.ResponsibilityAllowance,s.Bonus,s.Deduction,s.NetSalary
            FROM Payslips s JOIN PayrollPeriods p ON p.PayrollPeriodId=s.PayrollPeriodId
            WHERE p.CompanyId=? AND p.PayrollPeriodId=?
            """)) {
            s.setInt(1,companyId);s.setInt(2,periodId);
            try(ResultSet r=s.executeQuery()) {
                while(r.next()) {
                    PayrollRow row=remaining.remove(r.getInt("EmployeeId"));
                    if (row==null || !sameMoney(row.baseSalary(),r.getBigDecimal("BaseSalary"))
                            || !sameMoney(row.responsibilityAllowance(),r.getBigDecimal("ResponsibilityAllowance"))
                            || !sameMoney(row.bonus(),r.getBigDecimal("Bonus"))
                            || !sameMoney(row.deduction(),r.getBigDecimal("Deduction"))
                            || !sameMoney(row.netSalary(),r.getBigDecimal("NetSalary")))
                        throw staleSnapshots();
                }
            }
        }
        if (!remaining.isEmpty()) throw staleSnapshots();
    }

    private boolean sameMoney(BigDecimal expected,BigDecimal actual) {
        return expected!=null && actual!=null && expected.compareTo(actual)==0;
    }

    private PayrollException staleSnapshots() {
        return new PayrollException(409,"Kết quả tính lương chưa đầy đủ hoặc dữ liệu đã thay đổi. Hãy tính lại bảng lương trước khi duyệt.");
    }

    private void requireCompanyEmployee(Connection c,int companyId,int employeeId) throws SQLException {
        try (PreparedStatement s=c.prepareStatement("""
            SELECT EmployeeId FROM Employees WHERE CompanyId=? AND EmployeeId=?
            """)) {
            s.setInt(1,companyId); s.setInt(2,employeeId);
            try (ResultSet r=s.executeQuery()) {
                if (!r.next()) throw new PayrollException(404,"Nhân viên không thuộc công ty hoặc không có trong kỳ lương.");
            }
        }
    }

    private PayrollCalculation calculation(Connection c,int companyId,int periodId) throws SQLException {
        try (PreparedStatement s=c.prepareStatement("""
            SELECT e.EmployeeId,e.EmployeeCode,e.FullName,e.Status,d.DepartmentName,
                compensation.BaseSalary,compensation.EffectiveFrom SalaryEffectiveFrom,compensation.EffectiveTo SalaryEffectiveTo,
                allowance.Amount ResponsibilityAllowance,COALESCE(a.Bonus,0) Bonus,COALESCE(a.Deduction,0) Deduction,
                CAST(NULL AS DECIMAL(21,2)) NetSalary,CAST(NULL AS BIGINT) PayslipId,a.Note,
            """ + PayrollSources.ISSUE + """
                SalaryIssue FROM PayrollPeriods p JOIN Employees e ON e.CompanyId=p.CompanyId
            """ + SOURCES + """
            LEFT JOIN Departments d ON d.DepartmentId=e.DepartmentId AND d.CompanyId=e.CompanyId
            LEFT JOIN PayrollAdjustments a ON a.PayrollPeriodId=p.PayrollPeriodId AND a.EmployeeId=e.EmployeeId
            WHERE p.CompanyId=? AND p.PayrollPeriodId=? AND
            """ + ELIGIBLE + " ORDER BY e.EmployeeCode")) {
            s.setInt(1,companyId);s.setInt(2,periodId);
            return PayrollCalculator.calculate(rows(s));
        }
    }

    private boolean upsertAdjustment(Connection c,int userId,int periodId,int employeeId,
                                  BigDecimal bonus,BigDecimal deduction,String note) throws SQLException {
        try (PreparedStatement s=c.prepareStatement("""
            SELECT Bonus,Deduction,Note FROM PayrollAdjustments WITH (UPDLOCK,HOLDLOCK)
            WHERE PayrollPeriodId=? AND EmployeeId=?
            """)) {
            s.setInt(1,periodId);s.setInt(2,employeeId);
            try (ResultSet r=s.executeQuery()) {
                if (r.next() && r.getBigDecimal("Bonus").compareTo(bonus)==0
                        && r.getBigDecimal("Deduction").compareTo(deduction)==0
                        && java.util.Objects.equals(r.getString("Note")==null?"":r.getString("Note"),note==null?"":note))
                    return false;
            }
        }
        try (PreparedStatement s=c.prepareStatement("""
            UPDATE PayrollAdjustments SET Bonus=?,Deduction=?,Note=? WHERE PayrollPeriodId=? AND EmployeeId=?
            """)) {
            s.setBigDecimal(1,bonus); s.setBigDecimal(2,deduction); s.setString(3,note);
            s.setInt(4,periodId); s.setInt(5,employeeId);
            if (s.executeUpdate()>0) return true;
        }
        try (PreparedStatement s=c.prepareStatement("""
            INSERT INTO PayrollAdjustments(PayrollPeriodId,EmployeeId,Bonus,Deduction,Note,CreatedByUserId)
            VALUES(?,?,?,?,?,?)
            """)) {
            s.setInt(1,periodId); s.setInt(2,employeeId); s.setBigDecimal(3,bonus); s.setBigDecimal(4,deduction);
            s.setString(5,note); s.setInt(6,userId); s.executeUpdate();
        }
        return true;
    }

    private void invalidate(Connection c,int companyId,int periodId) throws SQLException {
        execute(c,"DELETE s FROM Payslips s JOIN PayrollPeriods p ON p.PayrollPeriodId=s.PayrollPeriodId WHERE p.CompanyId=? AND p.PayrollPeriodId=?",companyId,periodId);
        execute(c,"UPDATE PayrollPeriods SET Status='Draft',ReviewedAt=NULL,ReviewedByUserId=NULL,ApprovedAt=NULL,ApprovedByUserId=NULL WHERE CompanyId=? AND PayrollPeriodId=?",companyId,periodId);
    }

    /** Audit and the associated payroll changes always use the same connection. */
    private void auditPeriod(Connection c,int companyId,int userId,int periodId,String action,PayrollPeriod before) throws SQLException {
        PayrollPeriod after=lockPeriod(c,companyId,periodId);
        try (PreparedStatement s=c.prepareStatement("""
            INSERT INTO AuditLogs(CompanyId,UserId,[Action],TableName,RecordId,OldValues,NewValues,CreatedAt)
            VALUES(?,?,?,'PayrollPeriods',?,?,?,SYSUTCDATETIME())
            """)) {
            s.setInt(1,companyId);s.setInt(2,userId);s.setString(3,action);s.setString(4,String.valueOf(periodId));
            s.setString(5,before==null?null:auditValues(before));s.setString(6,auditValues(after));s.executeUpdate();
        }
    }

    private String auditValues(PayrollPeriod p) {
        return PayrollJson.encode(PayrollJson.object("status",p.getStatus(),"closed",p.isClosed(),
                "reviewedByUserId",p.getReviewedByUserId(),"reviewedAt",p.getReviewedAt(),
                "approvedByUserId",p.getApprovedByUserId(),"approvedAt",p.getApprovedAt(),"publishedAt",p.getPublishedAt(),
                "employeeCount",p.getEmployeeCount(),"totalIncome",p.getTotalIncome(),
                "totalDeduction",p.getTotalDeduction(),"totalNetSalary",p.getTotalNetSalary()));
    }

    private void execute(Connection c,String sql,int companyId,int periodId) throws SQLException {
        try (PreparedStatement s=c.prepareStatement(sql)) { s.setInt(1,companyId); s.setInt(2,periodId); s.executeUpdate(); }
    }

    private List<PayrollRow> rows(PreparedStatement s) throws SQLException {
        try (ResultSet r=s.executeQuery()) {
            List<PayrollRow> result=new ArrayList<>();
            while(r.next()) result.add(new PayrollRow(r.getInt("EmployeeId"),r.getString("EmployeeCode"),r.getString("FullName"),
                    r.getString("DepartmentName"),r.getString("Status"),r.getBigDecimal("BaseSalary"),r.getBigDecimal("Bonus"),
                    r.getBigDecimal("Deduction"),r.getBigDecimal("NetSalary"),r.getString("Note"),(Long)r.getObject("PayslipId"),
                    r.getBigDecimal("ResponsibilityAllowance"),r.getString("SalaryIssue"),date(r,"SalaryEffectiveFrom"),date(r,"SalaryEffectiveTo")));
            return result;
        }
    }

    private PayrollPeriod period(ResultSet r) throws SQLException {
        PayrollPeriod p=new PayrollPeriod();
        p.setPayrollPeriodId(r.getInt("PayrollPeriodId")); p.setCompanyId(r.getInt("CompanyId"));
        p.setPeriodName(r.getString("PeriodName")); p.setMonth(r.getInt("Month")); p.setYear(r.getInt("Year"));
        p.setStatus(r.getString("Status")); p.setClosed(r.getBoolean("IsClosed"));
        p.setReviewedByUserId((Integer)r.getObject("ReviewedByUserId"));p.setReviewedAt(time(r,"ReviewedAt"));
        p.setReviewedByName(r.getString("ReviewedByName"));p.setApprovedByName(r.getString("ApprovedByName"));
        p.setApprovedByUserId((Integer)r.getObject("ApprovedByUserId")); p.setApprovedAt(time(r,"ApprovedAt"));
        p.setCreatedAt(time(r,"CreatedAt")); p.setPublishedAt(time(r,"PublishedAt"));
        p.setEmployeeCount(r.getInt("EmployeeCount"));p.setTotalIncome(r.getBigDecimal("TotalIncome"));
        p.setTotalDeduction(r.getBigDecimal("TotalDeduction"));p.setTotalNetSalary(r.getBigDecimal("TotalNetSalary"));return p;
    }

    private PayrollSlipView slip(ResultSet r) throws SQLException {
        return new PayrollSlipView(r.getLong("PayslipId"),r.getInt("PayrollPeriodId"),r.getInt("EmployeeId"),
                r.getString("EmployeeCode"),r.getString("FullName"),r.getString("CompanyName"),r.getString("PeriodName"),
                r.getInt("Month"),r.getInt("Year"),r.getBigDecimal("BaseSalary"),r.getBigDecimal("Bonus"),
                r.getBigDecimal("Deduction"),r.getBigDecimal("NetSalary"),time(r,"GeneratedAt"),time(r,"PublishedAt"),
                r.getBigDecimal("ResponsibilityAllowance"),r.getString("PeriodStatus"));
    }

    private LocalDate date(ResultSet r,String name) throws SQLException {
        Date date=r.getDate(name);return date==null?null:date.toLocalDate();
    }

    private LocalDateTime time(ResultSet r,String name) throws SQLException {
        Timestamp timestamp=r.getTimestamp(name); return timestamp==null?null:timestamp.toLocalDateTime();
    }
}
