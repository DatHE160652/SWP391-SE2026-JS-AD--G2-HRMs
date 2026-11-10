package com.hrms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

public class ContractCompensationDAO {
    private final EmployeeLifecycleDAO dao=new EmployeeLifecycleDAO();

    public void synchronize(Connection connection,int companyId,int employeeId,long contractId,
            String contractCode,LocalDate effectiveDate,BigDecimal grossSalary,BigDecimal travelAllowance,Integer actorId) throws SQLException {
        if (grossSalary==null || grossSalary.signum()<0) throw new IllegalArgumentException("Hợp đồng phải có lương Gross hợp lệ.");
        OrganizationAllowanceDAO.validate(travelAllowance,"travelAllowance");
        if (!dao.query(connection,"""
                SELECT 1 FROM PayrollPeriods WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND Status='Approved'
                    AND ([Year]>YEAR(?) OR ([Year]=YEAR(?) AND [Month]>=MONTH(?)))
                """,companyId,effectiveDate,effectiveDate,effectiveDate).isEmpty())
            throw new IllegalArgumentException("Kỳ lương đã duyệt không cho phép đồng bộ Gross tại ngày bắt đầu hợp đồng.");
        var future=dao.query(connection,"""
                SELECT TOP(1) EffectiveFrom FROM EmployeeCompensationHistories WITH (UPDLOCK,HOLDLOCK)
                WHERE CompanyId=? AND EmployeeId=? AND EffectiveFrom>?
                    AND (EffectiveTo IS NULL OR EffectiveTo>EffectiveFrom)
                ORDER BY EffectiveFrom ASC,CompensationHistoryId ASC
                """,companyId,employeeId,effectiveDate);
        Object nextEffectiveFrom=future.isEmpty()?null:future.get(0).get("EffectiveFrom");
        var current=dao.query(connection,"""
                SELECT CompensationHistoryId,BaseSalary,TravelAllowance FROM EmployeeCompensationHistories WITH (UPDLOCK,HOLDLOCK)
                WHERE CompanyId=? AND EmployeeId=? AND EffectiveFrom<=?
                    AND (EffectiveTo IS NULL OR EffectiveTo>?)
                """,companyId,employeeId,effectiveDate,effectiveDate);
        if (current.size()>1) throw new IllegalArgumentException("Các kỳ lương đang chồng lấn. Cần kiểm tra trước khi kích hoạt hợp đồng.");
        BigDecimal travel=travelAllowance!=null?travelAllowance:current.isEmpty()?null:(BigDecimal)current.get(0).get("TravelAllowance");
        if (!current.isEmpty()) {
            dao.execute(connection,"""
                    UPDATE EmployeeCompensationHistories SET EffectiveTo=?
                    WHERE CompanyId=? AND EmployeeId=? AND CompensationHistoryId=?
                    """,effectiveDate,companyId,employeeId,current.get(0).get("CompensationHistoryId"));
        }
        BigDecimal allowance=new OrganizationAllowanceDAO().employeeAllowance(connection,companyId,employeeId,effectiveDate);
        dao.execute(connection,"""
                INSERT INTO EmployeeCompensationHistories(CompanyId,EmployeeId,BaseSalary,PositionAllowance,TravelAllowance,
                    EffectiveFrom,EffectiveTo,Reason,CreatedByUserId,SourceContractId)
                VALUES(?,?,?,?,?,?,?,?,?,?)
                """,companyId,employeeId,grossSalary,allowance,travel,effectiveDate,nextEffectiveFrom,
                "Gross theo hợp đồng "+contractCode,actorId,contractId);
        OrganizationTransaction.audit(connection,companyId,actorId,employeeId,"CONTRACT_COMPENSATION_SYNC",
                current.isEmpty()?null:"Gross="+current.get(0).get("BaseSalary"),"ContractId="+contractId+"; Gross="+grossSalary);
        invalidateUnapprovedPayrollPeriods(connection,companyId,effectiveDate);
    }

    public void revertContractCompensation(Connection connection,int companyId,int employeeId,long contractId,Integer actorId) throws SQLException {
        var rows=dao.query(connection,"""
                SELECT CompensationHistoryId,EffectiveFrom,EffectiveTo,BaseSalary
                FROM EmployeeCompensationHistories WITH (UPDLOCK,HOLDLOCK)
                WHERE CompanyId=? AND EmployeeId=? AND SourceContractId=?
                ORDER BY EffectiveFrom DESC,CompensationHistoryId DESC
                """,companyId,employeeId,contractId);
        for (var row : rows) {
            Object compId=row.get("CompensationHistoryId");
            Object effectiveFrom=row.get("EffectiveFrom");
            Object effectiveTo=row.get("EffectiveTo");
            var prev=dao.query(connection,"""
                    SELECT TOP(1) CompensationHistoryId FROM EmployeeCompensationHistories WITH (UPDLOCK,HOLDLOCK)
                    WHERE CompanyId=? AND EmployeeId=? AND CompensationHistoryId<>? AND EffectiveTo=?
                    ORDER BY EffectiveFrom DESC,CompensationHistoryId DESC
                    """,companyId,employeeId,compId,effectiveFrom);
            dao.execute(connection,"""
                    DELETE FROM EmployeeCompensationHistories
                    WHERE CompanyId=? AND EmployeeId=? AND CompensationHistoryId=?
                    """,companyId,employeeId,compId);
            if (!prev.isEmpty()) {
                dao.execute(connection,"""
                        UPDATE EmployeeCompensationHistories SET EffectiveTo=?
                        WHERE CompanyId=? AND EmployeeId=? AND CompensationHistoryId=?
                        """,effectiveTo,companyId,employeeId,prev.get(0).get("CompensationHistoryId"));
            }
            OrganizationTransaction.audit(connection,companyId,actorId,employeeId,"CONTRACT_COMPENSATION_REVERT",
                    "ContractId="+contractId+"; Gross="+row.get("BaseSalary"),null);
            if (effectiveFrom != null) {
                invalidateUnapprovedPayrollPeriods(connection,companyId,LocalDate.parse(effectiveFrom.toString()));
            }
        }
    }

    public void closeContractCompensation(Connection connection,int companyId,int employeeId,long contractId,LocalDate terminatedDate) throws SQLException {
        dao.execute(connection,"""
                UPDATE EmployeeCompensationHistories SET EffectiveTo=?
                WHERE CompanyId=? AND EmployeeId=? AND SourceContractId=?
                    AND EffectiveFrom<=? AND (EffectiveTo IS NULL OR EffectiveTo>?)
                """,terminatedDate,companyId,employeeId,contractId,terminatedDate,terminatedDate);
        if (terminatedDate != null) {
            invalidateUnapprovedPayrollPeriods(connection,companyId,terminatedDate);
        }
    }

    private void invalidateUnapprovedPayrollPeriods(Connection connection,int companyId,LocalDate fromDate) throws SQLException {
        if (fromDate == null) return;
        var periods=dao.query(connection,"""
                SELECT PayrollPeriodId FROM PayrollPeriods WITH (UPDLOCK,HOLDLOCK)
                WHERE CompanyId=? AND Status IN ('Calculated','Reviewed') AND PublishedAt IS NULL
                    AND ([Year]>YEAR(?) OR ([Year]=YEAR(?) AND [Month]>=MONTH(?)))
                """,companyId,fromDate,fromDate,fromDate);
        for (var p : periods) {
            Object periodId=p.get("PayrollPeriodId");
            dao.execute(connection,"DELETE s FROM Payslips s JOIN PayrollPeriods p ON p.PayrollPeriodId=s.PayrollPeriodId WHERE p.CompanyId=? AND p.PayrollPeriodId=?",companyId,periodId);
            dao.execute(connection,"UPDATE PayrollPeriods SET Status='Draft',ReviewedAt=NULL,ReviewedByUserId=NULL,ApprovedAt=NULL,ApprovedByUserId=NULL WHERE CompanyId=? AND PayrollPeriodId=?",companyId,periodId);
        }
    }
}
