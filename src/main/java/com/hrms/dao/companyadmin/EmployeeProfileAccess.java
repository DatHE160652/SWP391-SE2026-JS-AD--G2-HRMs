package com.hrms.dao.companyadmin;

import com.hrms.service.FieldValidationException;
import java.sql.Connection;
import java.sql.SQLException;

public final class EmployeeProfileAccess {
    private EmployeeProfileAccess() {}
    public static final String ACTIVE_ACCOUNT = """
            EXISTS (SELECT 1 FROM Users u WHERE u.CompanyId=e.CompanyId AND u.UserId=e.UserId
                AND u.IsActive=1 AND u.IsLocked=0 AND
            """ + ContractAccessPolicy.ALLOWED + ")";
    public static void requireActive(Connection connection,int companyId,int employeeId) throws SQLException {
        if (new EmployeeLifecycleDAO().query(connection,
                "SELECT 1 FROM Employees e WHERE e.CompanyId=? AND e.EmployeeId=? AND e.Status='Active' AND "+ACTIVE_ACCOUNT,companyId,employeeId).isEmpty())
            throw new FieldValidationException("employeeId","Hồ sơ không hoạt động. Vui lòng tạo hoặc mở lại tài khoản trước khi thực hiện thao tác này.");
    }
}
