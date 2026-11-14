package com.hrms.dao;

public final class ContractAccessPolicy {
    public static final String ALLOWED = """
            NOT EXISTS (
                SELECT 1 FROM Employees contractEmployee
                WHERE contractEmployee.CompanyId=u.CompanyId AND contractEmployee.UserId=u.UserId
                    AND EXISTS (
                        SELECT 1 FROM EmploymentContracts closedContract
                        WHERE closedContract.CompanyId=contractEmployee.CompanyId
                            AND closedContract.EmployeeId=contractEmployee.EmployeeId
                            AND (closedContract.Status IN ('EXPIRED','TERMINATED')
                                OR (closedContract.Status='ACTIVE' AND closedContract.EndDate<CONVERT(date,SYSDATETIME())))
                    )
                    AND NOT EXISTS (
                        SELECT 1 FROM EmploymentContracts effectiveContract
                        WHERE effectiveContract.CompanyId=contractEmployee.CompanyId
                            AND effectiveContract.EmployeeId=contractEmployee.EmployeeId
                            AND effectiveContract.Status='ACTIVE'
                            AND effectiveContract.StartDate<=CONVERT(date,SYSDATETIME())
                            AND (effectiveContract.EndDate IS NULL OR effectiveContract.EndDate>=CONVERT(date,SYSDATETIME()))
                    )
            )
            """;

    private ContractAccessPolicy() {}
}
