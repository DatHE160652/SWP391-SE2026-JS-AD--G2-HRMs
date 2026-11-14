package com.hrms.dao;

import java.sql.Connection;
import java.sql.SQLException;

public final class OrganizationRoleDAO {
    public String roleForEmployee(Connection connection, int companyId, int employeeId) throws SQLException {
        var rows = new EmployeeLifecycleDAO().query(connection, """
                SELECT e.PositionId,e.ManagerId,a.PositionId AS EffectivePositionId FROM Employees e
                LEFT JOIN EmployeePositionAssignments a ON a.CompanyId=e.CompanyId AND a.EmployeeId=e.EmployeeId
                    AND a.EffectiveFrom<=CONVERT(date,SYSDATETIME())
                    AND (a.EffectiveTo IS NULL OR a.EffectiveTo>CONVERT(date,SYSDATETIME()))
                WHERE e.CompanyId=? AND e.EmployeeId=?
                """,companyId,employeeId);
        if (rows.size()!=1 || !java.util.Objects.equals(rows.get(0).get("PositionId"),rows.get(0).get("EffectivePositionId")))
            throw new com.hrms.service.FieldValidationException("positionId","Phân công hiện tại chưa đồng bộ. Vui lòng kiểm tra trước khi cấp tài khoản.");
        String role=roleForPosition(connection,companyId,(Integer)rows.get(0).get("EffectivePositionId"));
        if ("MANAGER".equals(role) && rows.get(0).get("ManagerId")!=null)
            throw new com.hrms.service.FieldValidationException("managerId","Chức danh quản lý đang có quản lý cấp trên không hợp lệ. Vui lòng cập nhật phân công trước khi cấp tài khoản.");
        return role;
    }

    public String roleForPosition(Connection connection, int companyId, Integer positionId) throws SQLException {
        if (positionId == null) return "EMPLOYEE";
        var rows = new EmployeeLifecycleDAO().query(connection, """
                SELECT CASE WHEN UPPER(LTRIM(RTRIM(r.LevelCode))) IN ('EMP','EMPLOYEE')
                    OR UPPER(LTRIM(RTRIM(r.LevelName)))='EMPLOYEE' THEN 'EMPLOYEE'
                    WHEN UPPER(LTRIM(RTRIM(r.LevelCode)))='MANAGER'
                    OR UPPER(LTRIM(RTRIM(r.LevelName)))='MANAGER' OR p.CanManageEmployees=1
                    THEN 'MANAGER' ELSE 'EMPLOYEE' END AS RoleCode
                FROM Positions p JOIN ResponsibilityLevels r
                    ON r.CompanyId=p.CompanyId AND r.ResponsibilityLevelId=p.ResponsibilityLevelId
                WHERE p.CompanyId=? AND p.PositionId=?
                """, companyId, positionId);
        if (rows.isEmpty()) throw new IllegalArgumentException("Không tìm thấy chức danh trong công ty.");
        return (String) rows.get(0).get("RoleCode");
    }

    public void synchronize(Connection connection, int companyId, int employeeId, Integer positionId, Integer actorId) throws SQLException {
        var dao = new EmployeeLifecycleDAO();
        var accounts = dao.query(connection, """
                SELECT u.UserId,r.RoleCode FROM Employees e JOIN Users u ON u.CompanyId=e.CompanyId AND u.UserId=e.UserId
                JOIN Roles r ON r.RoleId=u.RoleId WHERE e.CompanyId=? AND e.EmployeeId=?
                """, companyId, employeeId);
        if (accounts.isEmpty()) return;
        var account = accounts.get(0);
        String previous = (String) account.get("RoleCode");
        if (!java.util.Set.of("EMPLOYEE","MANAGER").contains(previous)) return;
        String next = roleForPosition(connection, companyId, positionId);
        if (previous.equals(next)) return;
        if (dao.execute(connection, """
                UPDATE Users SET RoleId=(SELECT RoleId FROM Roles WHERE RoleCode=?),UpdatedAt=SYSUTCDATETIME()
                WHERE CompanyId=? AND UserId=?
                """, next, companyId, account.get("UserId")) != 1)
            throw new SQLException("Không đồng bộ được vai trò theo phân công.");
        OrganizationTransaction.audit(connection, companyId, actorId, employeeId, "ASSIGN_ACCOUNT_ROLE", previous, next);
    }
}
