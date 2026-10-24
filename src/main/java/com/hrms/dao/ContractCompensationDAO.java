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
        if (!dao.query(connection,"""
                SELECT 1 FROM EmployeeCompensationHistories WITH (UPDLOCK,HOLDLOCK)
                WHERE CompanyId=? AND EmployeeId=? AND EffectiveFrom>?
                    AND (EffectiveTo IS NULL OR EffectiveTo>EffectiveFrom)
                """,companyId,employeeId,effectiveDate).isEmpty())
            throw new IllegalArgumentException("Có lịch lương sau ngày bắt đầu hợp đồng. Cần xử lý lịch lương trước; hệ thống không ghi đè tự động.");
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
                    EffectiveFrom,Reason,CreatedByUserId,SourceContractId)
                VALUES(?,?,?,?,?,?,?,?,?)
                """,companyId,employeeId,grossSalary,allowance,travel,effectiveDate,
                "Gross theo hợp đồng "+contractCode,actorId,contractId);
        OrganizationTransaction.audit(connection,companyId,actorId,employeeId,"CONTRACT_COMPENSATION_SYNC",
                current.isEmpty()?null:"Gross="+current.get(0).get("BaseSalary"),"ContractId="+contractId+"; Gross="+grossSalary);
    }
}
