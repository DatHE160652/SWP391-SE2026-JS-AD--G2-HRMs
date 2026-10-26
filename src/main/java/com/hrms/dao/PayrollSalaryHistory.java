package com.hrms.dao;

import com.hrms.service.PayrollException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

/** Called only inside PayrollDAO's company-locked write transaction. */
final class PayrollSalaryHistory {
    private PayrollSalaryHistory() { }

    static LocalDate save(Connection c,int companyId,int userId,int employeeId,
                          BigDecimal amount,LocalDate effectiveFrom) throws SQLException {
        try (PreparedStatement s=c.prepareStatement("SELECT EmployeeId FROM Employees WHERE CompanyId=? AND EmployeeId=?")) {
            s.setInt(1,companyId);s.setInt(2,employeeId);
            try (ResultSet r=s.executeQuery()) {
                if (!r.next()) throw new PayrollException(404,"Không tìm thấy nhân viên trong công ty của bạn.");
            }
        }
        Long currentId=null;
        LocalDate currentFrom=null,end=null,next=null;
        try (PreparedStatement s=c.prepareStatement("""
            SELECT CompensationHistoryId,EffectiveFrom,EffectiveTo
            FROM EmployeeCompensationHistories WITH (UPDLOCK,HOLDLOCK)
            WHERE CompanyId=? AND EmployeeId=? AND (EffectiveTo IS NULL OR EffectiveTo>EffectiveFrom)
            ORDER BY EffectiveFrom
            """)) {
            s.setInt(1,companyId);s.setInt(2,employeeId);
            try (ResultSet r=s.executeQuery()) {
                while (r.next()) {
                    LocalDate from=r.getDate("EffectiveFrom").toLocalDate();
                    Date until=r.getDate("EffectiveTo");
                    if (!from.isAfter(effectiveFrom) && (until==null || until.toLocalDate().isAfter(effectiveFrom))) {
                        if (currentId!=null) throw new PayrollException(409,"Lịch sử lương đang chồng lấn tại ngày hiệu lực. Hãy xử lý trước khi cập nhật.");
                        currentId=r.getLong("CompensationHistoryId");currentFrom=from;
                        end=until==null?null:until.toLocalDate();
                    } else if (from.isAfter(effectiveFrom) && next==null) next=from;
                }
            }
        }
        if (next!=null && (end==null || next.isBefore(end))) end=next;
        // An effective-dated edit cannot rewrite the source of an approved/published payslip.
        try (PreparedStatement s=c.prepareStatement("""
            SELECT TOP 1 p.PayrollPeriodId FROM PayrollPeriods p
            JOIN Payslips slip ON slip.PayrollPeriodId=p.PayrollPeriodId AND slip.EmployeeId=?
            WHERE p.CompanyId=? AND (p.Status='Approved' OR p.PublishedAt IS NOT NULL)
                AND EOMONTH(DATEFROMPARTS(p.Year,p.Month,1))>=?
                AND (? IS NULL OR DATEFROMPARTS(p.Year,p.Month,1)<?)
            """)) {
            s.setInt(1,employeeId);s.setInt(2,companyId);s.setDate(3,Date.valueOf(effectiveFrom));
            s.setDate(4,end==null?null:Date.valueOf(end));s.setDate(5,end==null?null:Date.valueOf(end));
            try (ResultSet r=s.executeQuery()) {
                if (r.next()) throw new PayrollException(409,"Ngày hiệu lực ảnh hưởng kỳ đã duyệt/công bố. Chọn ngày sau kỳ đó hoặc mở lại kỳ chưa công bố.");
            }
        }
        if (effectiveFrom.equals(currentFrom)) {
            try (PreparedStatement s=c.prepareStatement("""
                UPDATE EmployeeCompensationHistories SET BaseSalary=?
                WHERE CompanyId=? AND EmployeeId=? AND CompensationHistoryId=?
                """)) {
                s.setBigDecimal(1,amount);s.setInt(2,companyId);s.setInt(3,employeeId);s.setLong(4,currentId);s.executeUpdate();
            }
        } else {
            if (currentId!=null) {
                try (PreparedStatement s=c.prepareStatement("UPDATE EmployeeCompensationHistories SET EffectiveTo=? WHERE CompensationHistoryId=? AND CompanyId=? AND EmployeeId=?")) {
                    s.setDate(1,Date.valueOf(effectiveFrom));s.setLong(2,currentId);s.setInt(3,companyId);s.setInt(4,employeeId);s.executeUpdate();
                }
            }
            try (PreparedStatement s=c.prepareStatement("""
                INSERT INTO EmployeeCompensationHistories(CompanyId,EmployeeId,BaseSalary,EffectiveFrom,EffectiveTo,
                    PositionAllowance,TravelAllowance,InsuranceSalary,SalaryStructureCode,SourceContractId,Reason,CreatedByUserId)
                SELECT ?,?,?,?,?,old.PositionAllowance,old.TravelAllowance,old.InsuranceSalary,old.SalaryStructureCode,
                    old.SourceContractId,N'Cập nhật lương từ quản lý kỳ lương',?
                FROM (SELECT 1 n) seed LEFT JOIN EmployeeCompensationHistories old
                    ON old.CompensationHistoryId=? AND old.CompanyId=? AND old.EmployeeId=?
                """)) {
                s.setInt(1,companyId);s.setInt(2,employeeId);s.setBigDecimal(3,amount);s.setDate(4,Date.valueOf(effectiveFrom));
                s.setDate(5,end==null?null:Date.valueOf(end));s.setInt(6,userId);
                if (currentId==null) s.setNull(7,Types.BIGINT);else s.setLong(7,currentId);
                s.setInt(8,companyId);s.setInt(9,employeeId);s.executeUpdate();
            }
        }
        try (PreparedStatement s=c.prepareStatement("""
            INSERT INTO AuditLogs(CompanyId,UserId,Action,TableName,RecordId,NewValues)
            VALUES(?,?,'PAYROLL_SALARY_CHANGE','EmployeeCompensationHistories',?,?)
            """)) {
            s.setInt(1,companyId);s.setInt(2,userId);s.setString(3,Integer.toString(employeeId));
            s.setString(4,"BaseSalary="+amount.toPlainString()+"; EffectiveFrom="+effectiveFrom);s.executeUpdate();
        }
        return end;
    }
}
