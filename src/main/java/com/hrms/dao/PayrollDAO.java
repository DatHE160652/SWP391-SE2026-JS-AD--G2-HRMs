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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private static volatile boolean schemaEnsured = false;

    private void ensureFormulaSchema(Connection c) throws SQLException {
        if (schemaEnsured) return;
        synchronized (PayrollDAO.class) {
            if (schemaEnsured) return;
            try (Statement s = c.createStatement()) {
                s.execute("""
                    IF OBJECT_ID('dbo.CompanyPayrollSettings', 'U') IS NULL
                    BEGIN
                        CREATE TABLE dbo.CompanyPayrollSettings (
                            CompanyId INT NOT NULL PRIMARY KEY,
                            SalaryCalculationMode VARCHAR(20) NOT NULL CONSTRAINT DF_CompanyPayrollSettings_Mode DEFAULT ('WORKDAYS'),
                            StandardDaysMode VARCHAR(25) NOT NULL CONSTRAINT DF_CompanyPayrollSettings_StdMode DEFAULT ('MON_FRI'),
                            FixedStandardDays DECIMAL(5,2) NOT NULL CONSTRAINT DF_CompanyPayrollSettings_FixedDays DEFAULT (22.00),
                            WeekdayWeights VARCHAR(40) NOT NULL CONSTRAINT DF_CompanyPayrollSettings_WeekdayWeights DEFAULT ('1,1,1,1,1,0,0'),
                            IncludePaidLeave BIT NOT NULL CONSTRAINT DF_CompanyPayrollSettings_PaidLeave DEFAULT (1),
                            IncludeResponsibilityAllowance BIT NOT NULL CONSTRAINT DF_CompanyPayrollSettings_RespAllow DEFAULT (1),
                            IncludeTravelAllowance BIT NOT NULL CONSTRAINT DF_CompanyPayrollSettings_TravelAllow DEFAULT (1),
                            UpdatedAt DATETIME2 NOT NULL CONSTRAINT DF_CompanyPayrollSettings_UpdatedAt DEFAULT (SYSUTCDATETIME()),
                            UpdatedByUserId INT NULL,
                            CONSTRAINT FK_CompanyPayrollSettings_Companies FOREIGN KEY (CompanyId) REFERENCES dbo.Companies(CompanyId) ON DELETE CASCADE
                        );
                    END;
                    IF COL_LENGTH('dbo.CompanyPayrollSettings', 'WeekdayWeights') IS NULL
                        ALTER TABLE dbo.CompanyPayrollSettings ADD WeekdayWeights VARCHAR(40) NOT NULL CONSTRAINT DF_CompanyPayrollSettings_WeekdayWeights DEFAULT ('1,1,1,1,1,0,0');
                    IF OBJECT_ID('dbo.CK_CompanyPayrollSettings_StdMode', 'C') IS NOT NULL
                        ALTER TABLE dbo.CompanyPayrollSettings DROP CONSTRAINT CK_CompanyPayrollSettings_StdMode;
                    IF COL_LENGTH('dbo.Payslips', 'ContractBaseSalary') IS NULL
                        ALTER TABLE dbo.Payslips ADD ContractBaseSalary DECIMAL(18,2) NULL;
                    IF COL_LENGTH('dbo.Payslips', 'ActualWorkDays') IS NULL
                        ALTER TABLE dbo.Payslips ADD ActualWorkDays DECIMAL(5,2) NOT NULL CONSTRAINT DF_Payslips_ActualWorkDays DEFAULT (0.00);
                    IF COL_LENGTH('dbo.Payslips', 'StandardWorkDays') IS NULL
                        ALTER TABLE dbo.Payslips ADD StandardWorkDays DECIMAL(5,2) NOT NULL CONSTRAINT DF_Payslips_StandardWorkDays DEFAULT (22.00);
                    IF COL_LENGTH('dbo.Payslips', 'TravelAllowance') IS NULL
                        ALTER TABLE dbo.Payslips ADD TravelAllowance DECIMAL(18,2) NOT NULL CONSTRAINT DF_Payslips_TravelAllowance DEFAULT (0.00);
                    IF COL_LENGTH('dbo.Payslips', 'CompanyId') IS NULL
                    BEGIN
                        ALTER TABLE dbo.Payslips ADD CompanyId INT NULL;
                        EXEC(N'UPDATE s SET CompanyId = p.CompanyId FROM dbo.Payslips s JOIN dbo.PayrollPeriods p ON p.PayrollPeriodId = s.PayrollPeriodId WHERE s.CompanyId IS NULL');
                        EXEC(N'ALTER TABLE dbo.Payslips ALTER COLUMN CompanyId INT NOT NULL');
                        IF OBJECT_ID('dbo.FK_Payslips_Companies', 'F') IS NULL
                            EXEC(N'ALTER TABLE dbo.Payslips ADD CONSTRAINT FK_Payslips_Companies FOREIGN KEY (CompanyId) REFERENCES dbo.Companies(CompanyId)');
                    END;
                    IF COL_LENGTH('dbo.PayrollAdjustments', 'CompanyId') IS NULL
                    BEGIN
                        ALTER TABLE dbo.PayrollAdjustments ADD CompanyId INT NULL;
                        EXEC(N'UPDATE a SET CompanyId = p.CompanyId FROM dbo.PayrollAdjustments a JOIN dbo.PayrollPeriods p ON p.PayrollPeriodId = a.PayrollPeriodId WHERE a.CompanyId IS NULL');
                        EXEC(N'ALTER TABLE dbo.PayrollAdjustments ALTER COLUMN CompanyId INT NOT NULL');
                        IF OBJECT_ID('dbo.FK_Adjustments_Companies', 'F') IS NULL
                            EXEC(N'ALTER TABLE dbo.PayrollAdjustments ADD CONSTRAINT FK_Adjustments_Companies FOREIGN KEY (CompanyId) REFERENCES dbo.Companies(CompanyId)');
                    END;
                """);
                s.execute("""
                    CREATE OR ALTER VIEW dbo.vw_PayrollOperators
                    AS
                        SELECT u.UserId, u.CompanyId, r.RoleCode
                        FROM dbo.Users u JOIN dbo.Roles r ON r.RoleId = u.RoleId
                        WHERE u.CompanyId IS NOT NULL AND r.RoleCode IN ('MANAGER','COMPANY_ADMIN','HR')
                            AND u.IsActive = 1 AND u.IsLocked = 0 AND u.MustChangePassword = 0
                            AND (u.LockedUntil IS NULL OR u.LockedUntil <= SYSUTCDATETIME());
                """);
                try (ResultSet rs = s.executeQuery("SELECT definition FROM sys.computed_columns WHERE object_id = OBJECT_ID('dbo.Payslips') AND name = 'NetSalary'")) {
                    if (rs.next()) {
                        String def = rs.getString(1);
                        if (def == null || !def.contains("TravelAllowance") || def.contains("Deduction")) {
                            s.execute("ALTER TABLE dbo.Payslips DROP COLUMN NetSalary");
                            s.execute("ALTER TABLE dbo.Payslips ADD NetSalary AS (BaseSalary + ResponsibilityAllowance + TravelAllowance + Bonus)");
                        }
                    }
                }
            }
            schemaEnsured = true;
        }
    }

    public PayrollFormulaSettings getFormulaSettings(int companyId) throws SQLException {
        try (Connection c = connections.open()) {
            ensureFormulaSchema(c);
            try (PreparedStatement s = c.prepareStatement("""
                SELECT SalaryCalculationMode, StandardDaysMode, FixedStandardDays, WeekdayWeights,
                       IncludePaidLeave, IncludeResponsibilityAllowance, IncludeTravelAllowance
                FROM dbo.CompanyPayrollSettings WHERE CompanyId=?
                """)) {
                s.setInt(1, companyId);
                try (ResultSet r = s.executeQuery()) {
                    if (!r.next()) return PayrollFormulaSettings.defaults(companyId);
                    String stdMode = r.getString("StandardDaysMode");
                    String weights = r.getString("WeekdayWeights");
                    if (weights == null || weights.isBlank()) {
                        weights = PayrollFormulaSettings.defaultWeightsForMode(stdMode);
                    }
                    return new PayrollFormulaSettings(
                            companyId,
                            r.getString("SalaryCalculationMode"),
                            stdMode,
                            r.getBigDecimal("FixedStandardDays"),
                            weights,
                            r.getBoolean("IncludePaidLeave"),
                            r.getBoolean("IncludeResponsibilityAllowance"),
                            r.getBoolean("IncludeTravelAllowance")
                    );
                }
            }
        }
    }

    public PayrollFormulaSettings saveFormulaSettings(int companyId, int userId, PayrollFormulaSettings settings) throws SQLException {
        return write(companyId, userId, c -> {
            ensureFormulaSchema(c);
            try (PreparedStatement s = c.prepareStatement("""
                MERGE dbo.CompanyPayrollSettings AS target
                USING (SELECT ? AS CompanyId) AS source ON target.CompanyId = source.CompanyId
                WHEN MATCHED THEN
                    UPDATE SET SalaryCalculationMode=?, StandardDaysMode=?, FixedStandardDays=?, WeekdayWeights=?,
                               IncludePaidLeave=?, IncludeResponsibilityAllowance=?, IncludeTravelAllowance=?,
                               UpdatedAt=SYSUTCDATETIME(), UpdatedByUserId=?
                WHEN NOT MATCHED THEN
                    INSERT (CompanyId, SalaryCalculationMode, StandardDaysMode, FixedStandardDays, WeekdayWeights,
                            IncludePaidLeave, IncludeResponsibilityAllowance, IncludeTravelAllowance, UpdatedByUserId)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                """)) {
                s.setInt(1, companyId);
                s.setString(2, settings.salaryCalculationMode());
                s.setString(3, settings.standardDaysMode());
                s.setBigDecimal(4, settings.fixedStandardDays());
                s.setString(5, settings.weekdayWeights());
                s.setBoolean(6, settings.includePaidLeave());
                s.setBoolean(7, settings.includeResponsibilityAllowance());
                s.setBoolean(8, settings.includeTravelAllowance());
                s.setInt(9, userId);
                s.setInt(10, companyId);
                s.setString(11, settings.salaryCalculationMode());
                s.setString(12, settings.standardDaysMode());
                s.setBigDecimal(13, settings.fixedStandardDays());
                s.setString(14, settings.weekdayWeights());
                s.setBoolean(15, settings.includePaidLeave());
                s.setBoolean(16, settings.includeResponsibilityAllowance());
                s.setBoolean(17, settings.includeTravelAllowance());
                s.setInt(18, userId);
                s.executeUpdate();
            }
            List<Integer> unapproved = new ArrayList<>();
            try (PreparedStatement s = c.prepareStatement("""
                SELECT PayrollPeriodId FROM PayrollPeriods
                WHERE CompanyId=? AND Status IN ('Calculated','Reviewed') AND PublishedAt IS NULL
                """)) {
                s.setInt(1, companyId);
                try (ResultSet r = s.executeQuery()) {
                    while (r.next()) unapproved.add(r.getInt(1));
                }
            }
            for (int pid : unapproved) invalidate(c, companyId, pid);
            return settings;
        });
    }

    private static final String PERIOD_COLUMNS = """
        SELECT p.*, (SELECT COUNT(*) FROM Payslips s WHERE s.PayrollPeriodId=p.PayrollPeriodId) EmployeeCount,
            COALESCE((SELECT SUM(s.NetSalary) FROM Payslips s WHERE s.PayrollPeriodId=p.PayrollPeriodId),0) TotalNetSalary,
            COALESCE((SELECT SUM(s.BaseSalary+s.ResponsibilityAllowance+COALESCE(s.TravelAllowance,0)+s.Bonus) FROM Payslips s
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
        try (Connection c = connections.open()) {
            ensureFormulaSchema(c);
            try (PreparedStatement s = c.prepareStatement(PERIOD_SELECT + " ORDER BY p.Year DESC,p.Month DESC")) {
                s.setInt(1, companyId);
                try (ResultSet r = s.executeQuery()) {
                    List<PayrollPeriod> result = new ArrayList<>();
                    while (r.next()) result.add(period(r));
                    return result;
                }
            }
        }
    }

    public PayrollPeriod findPeriod(int companyId, int periodId) throws SQLException {
        try (Connection c = connections.open()) {
            ensureFormulaSchema(c);
            try (PreparedStatement s = c.prepareStatement(PERIOD_SELECT + " AND p.PayrollPeriodId=?")) {
                s.setInt(1, companyId); s.setInt(2, periodId);
                try (ResultSet r = s.executeQuery()) {
                    if (!r.next()) throw new PayrollException(404, "Không tìm thấy kỳ lương trong công ty của bạn.");
                    return period(r);
                }
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
        try (Connection c = connections.open()) {
            ensureFormulaSchema(c);
            try (PreparedStatement s = c.prepareStatement("""
            SELECT e.EmployeeId,e.EmployeeCode,e.FullName,e.Status,
                CASE WHEN employment.PayrollDate IS NOT NULL THEN compensation.BaseSalary END BaseSalary,
                CASE WHEN employment.PayrollDate IS NOT NULL THEN compensation.BaseSalary END ContractBaseSalary,
                workMetrics.ActualWorkDays,workMetrics.StandardWorkDays,
                CASE WHEN employment.PayrollDate IS NOT NULL THEN COALESCE(compensation.TravelAllowance, CAST(0 AS DECIMAL(18,2))) END TravelAllowance,
                d.DepartmentName,
                allowance.Amount ResponsibilityAllowance,""" + PayrollSources.ISSUE + """
                SalaryIssue,
                CASE WHEN employment.PayrollDate IS NOT NULL THEN compensation.EffectiveFrom END SalaryEffectiveFrom,
                CASE WHEN employment.PayrollDate IS NOT NULL THEN compensation.EffectiveTo END SalaryEffectiveTo,
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
    }

    public List<PayrollRow> periodRows(int companyId, int periodId) throws SQLException {
        return periodRows(companyId,periodId,false);
    }

    /** Adjustment entry includes every employee in the period's company, even before a contract exists. */
    public List<PayrollRow> adjustmentRows(int companyId, int periodId) throws SQLException {
        return periodRows(companyId,periodId,true,false);
    }

    /** Template export always reflects each employee's assigned department/position responsibility allowance. */
    public List<PayrollRow> templateRows(int companyId, int periodId) throws SQLException {
        return periodRows(companyId,periodId,true,true);
    }

    private List<PayrollRow> periodRows(int companyId, int periodId, boolean adjustments) throws SQLException {
        return periodRows(companyId,periodId,adjustments,false);
    }

    private List<PayrollRow> periodRows(int companyId, int periodId, boolean adjustments, boolean liveAllowance) throws SQLException {
        findPeriod(companyId, periodId);
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement("""
            SELECT e.EmployeeId,e.EmployeeCode,e.FullName,e.Status,d.DepartmentName,
            """ + (adjustments ? "COALESCE(s.ContractBaseSalary,compensation.BaseSalary) BaseSalary,"
                    : "COALESCE(s.BaseSalary," + PayrollSources.PRORATED_BASE + ") BaseSalary,") + """
                COALESCE(s.ContractBaseSalary,compensation.BaseSalary) ContractBaseSalary,
                COALESCE(s.ActualWorkDays,workMetrics.ActualWorkDays) ActualWorkDays,
                COALESCE(s.StandardWorkDays,workMetrics.StandardWorkDays) StandardWorkDays,
            """ + (liveAllowance ? PayrollSources.ALLOWANCE + " ResponsibilityAllowance,"
                    : "COALESCE(s.ResponsibilityAllowance," + PayrollSources.ALLOWANCE + ") ResponsibilityAllowance,")
                + "COALESCE(s.TravelAllowance," + PayrollSources.TRAVEL_ALLOWANCE + ") TravelAllowance,"
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
            boolean changed=upsertAdjustment(c, companyId, userId, periodId, employeeId, bonus, deduction, note);
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
                changed=upsertAdjustment(c, companyId, userId, periodId, ids.get(i), row.bonus(), row.deduction(), row.note()) || changed;
            }
            if (changed) invalidate(c, companyId, periodId);
            return input.size();
        });
    }

    public int importPayroll(int companyId, int userId, int periodId,
                             List<PayrollImportInput> input) throws SQLException {
        return write(companyId, userId, c -> {
            ensureFormulaSchema(c);
            PayrollPeriod before = lockPeriod(c, companyId, periodId);
            editable(before);

            List<Integer> ids = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            for (PayrollImportInput row : input) {
                try (PreparedStatement s = c.prepareStatement("""
                    SELECT e.EmployeeId FROM Employees e
                    WHERE e.CompanyId=? AND e.EmployeeCode=?
                    """)) {
                    s.setInt(1, companyId); s.setString(2, row.employeeCode());
                    try (ResultSet r = s.executeQuery()) {
                        if (r.next()) ids.add(r.getInt(1));
                        else {
                            ids.add(null);
                            errors.add("Dòng " + row.rowNumber() + ": mã " + row.employeeCode() + " không thuộc công ty của bạn.");
                        }
                    }
                }
            }
            if (!errors.isEmpty()) throw new PayrollException(400, "Chưa nhập dữ liệu. Hãy sửa các dòng lỗi.", errors);

            // Xóa kết quả tính cũ của kỳ lương này
            invalidate(c, companyId, periodId);

            // Chèn bảng lương mới vào Payslips
            try (PreparedStatement s = c.prepareStatement("""
                INSERT INTO Payslips(CompanyId,PayrollPeriodId,EmployeeId,BaseSalary,ContractBaseSalary,ActualWorkDays,StandardWorkDays,ResponsibilityAllowance,TravelAllowance,Bonus,Deduction)
                VALUES(?,?,?,?,?,?,?,?,?,?,0.00)
                """)) {
                for (int i = 0; i < input.size(); i++) {
                    PayrollImportInput row = input.get(i);
                    s.setInt(1, companyId);
                    s.setInt(2, periodId);
                    s.setInt(3, ids.get(i));
                    s.setBigDecimal(4, row.baseSalary() == null ? BigDecimal.ZERO : row.baseSalary());
                    s.setBigDecimal(5, row.contractBaseSalary() == null ? BigDecimal.ZERO : row.contractBaseSalary());
                    s.setBigDecimal(6, row.actualWorkDays() == null ? BigDecimal.ZERO : row.actualWorkDays());
                    s.setBigDecimal(7, row.standardWorkDays() == null ? new BigDecimal("26.00") : row.standardWorkDays());
                    s.setBigDecimal(8, row.responsibilityAllowance() == null ? BigDecimal.ZERO : row.responsibilityAllowance());
                    s.setBigDecimal(9, row.travelAllowance() == null ? BigDecimal.ZERO : row.travelAllowance());
                    s.setBigDecimal(10, row.bonus() == null ? BigDecimal.ZERO : row.bonus());
                    s.addBatch();
                }
                s.executeBatch();
            }

            // Đồng bộ thưởng và ghi chú vào PayrollAdjustments
            for (int i = 0; i < input.size(); i++) {
                PayrollImportInput row = input.get(i);
                upsertAdjustment(c, companyId, userId, periodId, ids.get(i),
                        row.bonus() == null ? BigDecimal.ZERO : row.bonus(),
                        BigDecimal.ZERO,
                        row.note());
            }

            // Đưa trạng thái kỳ lương sang Calculated để HR rà soát và phê duyệt
            execute(c, "UPDATE PayrollPeriods SET Status='Calculated', ReviewedAt=NULL, ReviewedByUserId=NULL, ApprovedAt=NULL, ApprovedByUserId=NULL WHERE CompanyId=? AND PayrollPeriodId=?", companyId, periodId);
            auditPeriod(c, companyId, userId, periodId, "PAYROLL_IMPORTED_EXCEL", before);
            return input.size();
        });
    }

    public java.util.Map<String, Object> applyBonusCampaign(int companyId, int userId, int periodId,
                                                            String campaignName, String calcType,
                                                            BigDecimal monthsMultiplier, boolean prorateByTenure,
                                                            BigDecimal fixedAmount, String applyMode) throws SQLException {
        return write(companyId, userId, c -> {
            PayrollPeriod period = lockPeriod(c, companyId, periodId);
            editable(period);
            int year = period.getYear();
            int count = 0;
            BigDecimal totalBonus = BigDecimal.ZERO;
            boolean changed = false;

            try (PreparedStatement s = c.prepareStatement("""
                SELECT e.EmployeeId, e.EmployeeCode, e.FullName,
                       COALESCE(contract.GrossSalary, compensation.BaseSalary) AS ContractBaseSalary,
                       COALESCE(contract.StartDate, CAST(e.CreatedAt AS DATE)) AS EmployeeStartDate,
                       COALESCE(adj.Bonus, 0) AS CurrentBonus,
                       adj.Note AS CurrentNote
                FROM dbo.Employees e
                CROSS JOIN (SELECT p.PayrollPeriodId, p.Year, p.Month FROM dbo.PayrollPeriods p WHERE p.CompanyId = ? AND p.PayrollPeriodId = ?) p
                OUTER APPLY (
                    SELECT TOP 1 c.GrossSalary, c.StartDate
                    FROM dbo.EmploymentContracts c
                    WHERE c.CompanyId = e.CompanyId AND c.EmployeeId = e.EmployeeId
                      AND c.Status IN ('ACTIVE', 'EXPIRED', 'TERMINATED')
                      AND c.StartDate <= EOMONTH(DATEFROMPARTS(p.Year, p.Month, 1))
                      AND (c.EndDate IS NULL OR c.EndDate >= DATEFROMPARTS(p.Year, p.Month, 1))
                    ORDER BY c.StartDate DESC
                ) contract
                OUTER APPLY (
                    SELECT TOP 1 h.BaseSalary
                    FROM dbo.EmployeeCompensationHistories h
                    WHERE h.CompanyId = e.CompanyId AND h.EmployeeId = e.EmployeeId
                      AND h.EffectiveFrom <= EOMONTH(DATEFROMPARTS(p.Year, p.Month, 1))
                      AND (h.EffectiveTo IS NULL OR h.EffectiveTo > DATEFROMPARTS(p.Year, p.Month, 1))
                    ORDER BY h.EffectiveFrom DESC
                ) compensation
                LEFT JOIN dbo.PayrollAdjustments adj ON adj.PayrollPeriodId = p.PayrollPeriodId AND adj.EmployeeId = e.EmployeeId
                WHERE e.CompanyId = ? AND e.Status IN ('Active', 'On Leave', 'Inactive')
                ORDER BY e.EmployeeCode
            """)) {
                s.setInt(1, companyId);
                s.setInt(2, periodId);
                s.setInt(3, companyId);
                try (ResultSet r = s.executeQuery()) {
                    while (r.next()) {
                        int empId = r.getInt("EmployeeId");
                        BigDecimal base = r.getBigDecimal("ContractBaseSalary");
                        BigDecimal bonusAmt = BigDecimal.ZERO;
                        String noteText;

                        if ("CONTRACT_SALARY".equalsIgnoreCase(calcType)) {
                            if (base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
                                continue;
                            }
                            if (prorateByTenure) {
                                Date startSql = r.getDate("EmployeeStartDate");
                                LocalDate startDate = startSql != null ? startSql.toLocalDate() : LocalDate.of(year, 1, 1);
                                int monthsWorked;
                                if (startDate.getYear() < year) {
                                    monthsWorked = 12;
                                } else if (startDate.getYear() == year) {
                                    int m = startDate.getMonthValue();
                                    monthsWorked = 12 - m + (startDate.getDayOfMonth() <= 15 ? 1 : 0);
                                    monthsWorked = Math.max(1, Math.min(12, monthsWorked));
                                } else {
                                    monthsWorked = 0;
                                }
                                bonusAmt = base.multiply(monthsMultiplier)
                                        .multiply(BigDecimal.valueOf(monthsWorked))
                                        .divide(BigDecimal.valueOf(12), 0, java.math.RoundingMode.HALF_UP);
                                noteText = campaignName + " (" + monthsMultiplier.stripTrailingZeros().toPlainString() + " tháng lương - làm " + monthsWorked + "/12 tháng năm " + year + ")";
                            } else {
                                bonusAmt = base.multiply(monthsMultiplier).setScale(0, java.math.RoundingMode.HALF_UP);
                                noteText = campaignName + " (" + monthsMultiplier.stripTrailingZeros().toPlainString() + " tháng lương)";
                            }
                        } else {
                            bonusAmt = fixedAmount != null ? fixedAmount.setScale(0, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
                            noteText = campaignName;
                        }

                        BigDecimal currentBonus = r.getBigDecimal("CurrentBonus") != null ? r.getBigDecimal("CurrentBonus") : BigDecimal.ZERO;
                        String currentNote = r.getString("CurrentNote");
                        BigDecimal finalBonus;
                        String finalNote;

                        if ("ADD".equalsIgnoreCase(applyMode)) {
                            finalBonus = currentBonus.add(bonusAmt);
                            finalNote = (currentNote == null || currentNote.isBlank()) ? noteText : (currentNote + "; " + noteText);
                        } else {
                            finalBonus = bonusAmt;
                            finalNote = noteText;
                        }
                        if (finalNote.length() > 300) finalNote = finalNote.substring(0, 300);

                        boolean empChanged = upsertAdjustment(c, companyId, userId, periodId, empId, finalBonus, BigDecimal.ZERO, finalNote);
                        if (empChanged) changed = true;
                        count++;
                        totalBonus = totalBonus.add(finalBonus);
                    }
                }
            }
            if (changed) invalidate(c, companyId, periodId);
            return java.util.Map.of("employeeCount", count, "totalBonus", totalBonus);
        });
    }

    public List<Map<String, Object>> bonusCandidates(int companyId, int periodId) throws SQLException {
        try (Connection c = connections.open();
             PreparedStatement s = c.prepareStatement("""
                SELECT e.EmployeeId, e.EmployeeCode, e.FullName, d.DepartmentName,
                       COALESCE(contract.GrossSalary, compensation.BaseSalary) AS ContractBaseSalary,
                       COALESCE(contract.StartDate, CAST(e.CreatedAt AS DATE)) AS EmployeeStartDate,
                       COALESCE(adj.Bonus, 0) AS CurrentBonus,
                       adj.Note AS CurrentNote
                FROM dbo.Employees e
                CROSS JOIN (SELECT p.PayrollPeriodId, p.Year, p.Month FROM dbo.PayrollPeriods p WHERE p.CompanyId = ? AND p.PayrollPeriodId = ?) p
                LEFT JOIN dbo.Departments d ON d.DepartmentId = e.DepartmentId AND d.CompanyId = e.CompanyId
                OUTER APPLY (
                    SELECT TOP 1 c.GrossSalary, c.StartDate
                    FROM dbo.EmploymentContracts c
                    WHERE c.CompanyId = e.CompanyId AND c.EmployeeId = e.EmployeeId
                      AND c.Status IN ('ACTIVE', 'EXPIRED', 'TERMINATED')
                      AND c.StartDate <= EOMONTH(DATEFROMPARTS(p.Year, p.Month, 1))
                      AND (c.EndDate IS NULL OR c.EndDate >= DATEFROMPARTS(p.Year, p.Month, 1))
                    ORDER BY c.StartDate DESC
                ) contract
                OUTER APPLY (
                    SELECT TOP 1 h.BaseSalary
                    FROM dbo.EmployeeCompensationHistories h
                    WHERE h.CompanyId = e.CompanyId AND h.EmployeeId = e.EmployeeId
                      AND h.EffectiveFrom <= EOMONTH(DATEFROMPARTS(p.Year, p.Month, 1))
                      AND (h.EffectiveTo IS NULL OR h.EffectiveTo > DATEFROMPARTS(p.Year, p.Month, 1))
                    ORDER BY h.EffectiveFrom DESC
                ) compensation
                LEFT JOIN dbo.PayrollAdjustments adj ON adj.PayrollPeriodId = p.PayrollPeriodId AND adj.EmployeeId = e.EmployeeId
                WHERE e.CompanyId = ? AND e.Status IN ('Active', 'On Leave', 'Inactive')
                ORDER BY e.EmployeeCode
             """)) {
            s.setInt(1, companyId);
            s.setInt(2, periodId);
            s.setInt(3, companyId);
            List<Map<String, Object>> list = new ArrayList<>();
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("employeeId", r.getInt("EmployeeId"));
                    map.put("employeeCode", r.getString("EmployeeCode"));
                    map.put("fullName", r.getString("FullName"));
                    map.put("departmentName", r.getString("DepartmentName"));
                    BigDecimal contractBase = r.getBigDecimal("ContractBaseSalary");
                    map.put("contractBaseSalary", contractBase == null ? null : contractBase.toPlainString());
                    Date startDate = r.getDate("EmployeeStartDate");
                    map.put("startDate", startDate == null ? null : startDate.toString());
                    BigDecimal curBonus = r.getBigDecimal("CurrentBonus");
                    map.put("currentBonus", curBonus == null ? "0.00" : curBonus.toPlainString());
                    map.put("currentNote", r.getString("CurrentNote"));
                    list.add(map);
                }
            }
            return list;
        }
    }

    public Map<String, Object> saveBonusAdjustments(int companyId, int userId, int periodId,
                                                    int[] employeeIds, BigDecimal[] bonuses, String[] notes) throws SQLException {
        return write(companyId, userId, c -> {
            PayrollPeriod period = lockPeriod(c, companyId, periodId);
            editable(period);
            int count = 0;
            BigDecimal totalBonus = BigDecimal.ZERO;
            boolean changed = false;
            for (int i = 0; i < employeeIds.length; i++) {
                int empId = employeeIds[i];
                BigDecimal bonus = bonuses[i] == null ? BigDecimal.ZERO : bonuses[i].setScale(2, java.math.RoundingMode.HALF_UP);
                String note = (notes != null && i < notes.length) ? notes[i] : null;
                if (note != null && note.length() > 300) note = note.substring(0, 300);
                boolean empChanged = upsertAdjustment(c, companyId, userId, periodId, empId, bonus, BigDecimal.ZERO, note);
                if (empChanged) changed = true;
                if (bonus.compareTo(BigDecimal.ZERO) > 0) count++;
                totalBonus = totalBonus.add(bonus);
            }
            if (changed) invalidate(c, companyId, periodId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("employeeCount", count);
            result.put("totalBonus", totalBonus.setScale(0, java.math.RoundingMode.HALF_UP));
            return result;
        });
    }

    /** A consistent, read-only preview. No payslip, status or adjustment is written. */
    public PayrollCalculation preview(int companyId,int periodId) throws SQLException {
        try (Connection c=connections.open()) {
            ensureFormulaSchema(c);
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
                INSERT INTO Payslips(CompanyId,PayrollPeriodId,EmployeeId,BaseSalary,ContractBaseSalary,ActualWorkDays,StandardWorkDays,ResponsibilityAllowance,TravelAllowance,Bonus,Deduction)
                VALUES(?,?,?,?,?,?,?,?,?,?,?)
                """)) {
                for (PayrollRow row:result.rows()) {
                    s.setInt(1,companyId);
                    s.setInt(2,periodId);
                    s.setInt(3,row.employeeId());
                    s.setBigDecimal(4,row.baseSalary());
                    s.setBigDecimal(5,row.contractBaseSalary()==null?row.baseSalary():row.contractBaseSalary());
                    s.setBigDecimal(6,row.actualWorkDays()==null?BigDecimal.ZERO:row.actualWorkDays());
                    s.setBigDecimal(7,row.standardWorkDays()==null?new BigDecimal("22.00"):row.standardWorkDays());
                    s.setBigDecimal(8,row.responsibilityAllowance());
                    s.setBigDecimal(9,row.travelAllowance()==null?BigDecimal.ZERO:row.travelAllowance());
                    s.setBigDecimal(10,row.bonus());
                    s.setBigDecimal(11,row.deduction());
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
        try (Connection c = connections.open()) {
            ensureFormulaSchema(c);
            try (PreparedStatement s = c.prepareStatement(SLIP_SELECT + """
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
    }

    public PayrollSlipView payslip(int companyId, Integer employeeId, long id) throws SQLException {
        String restriction = employeeId == null ? "" : " AND e.EmployeeId=? AND p.PublishedAt IS NOT NULL AND p.Status='Approved'";
        try (Connection c = connections.open()) {
            ensureFormulaSchema(c);
            try (PreparedStatement s = c.prepareStatement(SLIP_SELECT
                + " WHERE p.CompanyId=? AND s.PayslipId=?" + restriction)) {
                s.setInt(1, companyId); s.setLong(2, id); if (employeeId != null) s.setInt(3, employeeId);
                try (ResultSet r = s.executeQuery()) {
                    if (!r.next()) throw new PayrollException(404, "Không tìm thấy phiếu lương.");
                    return slip(r);
                }
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
            ensureFormulaSchema(c);
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
        try (PreparedStatement s = c.prepareStatement("""
            SELECT COUNT(*) SlipCount,
                   COALESCE(MIN(s.BaseSalary), 0) MinBase,
                   COALESCE(MIN(s.NetSalary), 0) MinNet
            FROM Payslips s JOIN PayrollPeriods p ON p.PayrollPeriodId=s.PayrollPeriodId
            WHERE p.CompanyId=? AND p.PayrollPeriodId=?
            """)) {
            s.setInt(1, companyId); s.setInt(2, periodId);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next() || r.getInt("SlipCount") == 0) {
                    throw new PayrollException(409, "Kỳ lương chưa có dữ liệu bảng lương đã tính. Hãy tính lương hoặc nhập Excel trước khi rà soát và duyệt.");
                }
                if (r.getBigDecimal("MinBase").compareTo(BigDecimal.ZERO) < 0 || r.getBigDecimal("MinNet").compareTo(BigDecimal.ZERO) < 0) {
                    throw new PayrollException(409, "Kết quả tính lương chứa mức lương không hợp lệ. Hãy kiểm tra lại bảng lương.");
                }
            }
        }
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
            """ + PayrollSources.PRORATED_BASE + """
                BaseSalary,compensation.BaseSalary ContractBaseSalary,
                workMetrics.ActualWorkDays,workMetrics.StandardWorkDays,
                compensation.EffectiveFrom SalaryEffectiveFrom,compensation.EffectiveTo SalaryEffectiveTo,
            """ + PayrollSources.ALLOWANCE + " ResponsibilityAllowance,"
                + PayrollSources.TRAVEL_ALLOWANCE + " TravelAllowance," + """
                COALESCE(a.Bonus,0) Bonus,COALESCE(a.Deduction,0) Deduction,
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

    private boolean upsertAdjustment(Connection c,int companyId,int userId,int periodId,int employeeId,
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
            INSERT INTO PayrollAdjustments(CompanyId,PayrollPeriodId,EmployeeId,Bonus,Deduction,Note,CreatedByUserId)
            VALUES(?,?,?,?,?,?,?)
            """)) {
            s.setInt(1,companyId); s.setInt(2,periodId); s.setInt(3,employeeId); s.setBigDecimal(4,bonus); s.setBigDecimal(5,deduction);
            s.setString(6,note); s.setInt(7,userId); s.executeUpdate();
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
                    r.getBigDecimal("ResponsibilityAllowance"),r.getString("SalaryIssue"),date(r,"SalaryEffectiveFrom"),date(r,"SalaryEffectiveTo"),
                    r.getBigDecimal("ContractBaseSalary"),r.getBigDecimal("ActualWorkDays"),r.getBigDecimal("StandardWorkDays"),r.getBigDecimal("TravelAllowance")));
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
        BigDecimal baseSalary = r.getBigDecimal("BaseSalary");
        BigDecimal contractBase = r.getBigDecimal("ContractBaseSalary");
        return new PayrollSlipView(r.getLong("PayslipId"),r.getInt("PayrollPeriodId"),r.getInt("EmployeeId"),
                r.getString("EmployeeCode"),r.getString("FullName"),r.getString("CompanyName"),r.getString("PeriodName"),
                r.getInt("Month"),r.getInt("Year"),baseSalary,r.getBigDecimal("Bonus"),
                r.getBigDecimal("Deduction"),r.getBigDecimal("NetSalary"),time(r,"GeneratedAt"),time(r,"PublishedAt"),
                r.getBigDecimal("ResponsibilityAllowance"),r.getString("PeriodStatus"),
                contractBase == null ? baseSalary : contractBase,
                r.getBigDecimal("ActualWorkDays"),r.getBigDecimal("StandardWorkDays"),
                r.getBigDecimal("TravelAllowance") == null ? BigDecimal.ZERO : r.getBigDecimal("TravelAllowance"));
    }

    private LocalDate date(ResultSet r,String name) throws SQLException {
        Date date=r.getDate(name);return date==null?null:date.toLocalDate();
    }

    private LocalDateTime time(ResultSet r,String name) throws SQLException {
        Timestamp timestamp=r.getTimestamp(name); return timestamp==null?null:timestamp.toLocalDateTime();
    }
}
