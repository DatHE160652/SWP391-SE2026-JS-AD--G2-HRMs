package com.hrms.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class PersonalWorkspaceDAO {
    private final EmployeeLifecycleDAO dao=new EmployeeLifecycleDAO();
    private static final String CURRENT_ASSIGNMENTS="""
            WITH CurrentAssignments AS (
                SELECT * FROM EmployeePositionAssignments
                WHERE CompanyId=? AND EffectiveFrom<=CONVERT(date,SYSDATETIME())
                    AND (EffectiveTo IS NULL OR EffectiveTo>CONVERT(date,SYSDATETIME()))
            )
            """;
    private static final String DIRECTORY="""
            SELECT e.EmployeeId,a.PositionId,e.EmployeeCode,e.FullName,d.DepartmentName,p.PositionTitle,
                r.LevelName,m.FullName AS ManagerName,e.Email
            FROM Employees e
            LEFT JOIN CurrentAssignments a ON a.CompanyId=e.CompanyId AND a.EmployeeId=e.EmployeeId
            LEFT JOIN Departments d ON d.CompanyId=a.CompanyId AND d.DepartmentId=a.DepartmentId
            LEFT JOIN Positions p ON p.CompanyId=a.CompanyId AND p.PositionId=a.PositionId
            LEFT JOIN ResponsibilityLevels r ON r.CompanyId=p.CompanyId AND r.ResponsibilityLevelId=p.ResponsibilityLevelId
            LEFT JOIN Employees m ON m.CompanyId=a.CompanyId AND m.EmployeeId=a.ManagerId
            """;

    public Map<String,Object> read(Connection connection,int companyId,int employeeId,boolean manager) throws SQLException {
        var assignments=dao.query(connection,CURRENT_ASSIGNMENTS+"SELECT DepartmentId,PositionId,ManagerId FROM CurrentAssignments WHERE EmployeeId=?",companyId,employeeId);
        if (assignments.size()>1) throw new IllegalArgumentException("Có nhiều phân công cùng hiệu lực. Vui lòng liên hệ nhân sự.");
        Object departmentId=assignments.isEmpty()?null:assignments.get(0).get("DepartmentId");
        Object managerId=manager?employeeId:null;
        var personal=dao.query(connection,CURRENT_ASSIGNMENTS+DIRECTORY+" WHERE e.CompanyId=? AND e.EmployeeId=?",companyId,companyId,employeeId);
        var colleagues=dao.query(connection,CURRENT_ASSIGNMENTS+DIRECTORY+"""
                 WHERE e.CompanyId=? AND e.Status='Active' AND
                    (e.EmployeeId=? OR (a.DepartmentId=? AND a.ManagerId=? AND a.ManagerId IS NOT NULL))
                 ORDER BY p.PositionTitle,e.FullName,e.EmployeeId
                """,companyId,companyId,employeeId,departmentId,managerId);
        var positions=departmentId==null?List.of():dao.query(connection,"""
                SELECT p.PositionId,p.PositionTitle,r.LevelName,r.RankOrder
                FROM Positions p JOIN ResponsibilityLevels r ON r.CompanyId=p.CompanyId AND r.ResponsibilityLevelId=p.ResponsibilityLevelId
                WHERE p.CompanyId=? AND p.DepartmentId=? AND p.IsActive=1 AND (?=1 OR p.PositionId=?)
                ORDER BY r.RankOrder DESC,p.PositionTitle,p.PositionId
                """,companyId,departmentId,manager?1:0,assignments.get(0).get("PositionId"));
        return Map.of("personal",personal,"colleagues",colleagues,"positions",positions,"contracts",contracts(connection,companyId,employeeId));
    }

    public Map<String,Object> teamMember(Connection connection,int companyId,int managerId,int employeeId) throws SQLException {
        var members=dao.query(connection,CURRENT_ASSIGNMENTS+DIRECTORY
                +" WHERE e.CompanyId=? AND e.EmployeeId=? AND a.ManagerId=? AND e.Status='Active'",companyId,companyId,employeeId,managerId);
        if (members.size()!=1) throw new SecurityException("Bạn chỉ được xem thông tin công việc của nhân viên đang thuộc quyền quản lý trực tiếp.");
        return Map.of("profile",members.get(0),"contracts",contracts(connection,companyId,employeeId));
    }

    private List<Map<String,Object>> contracts(Connection connection,int companyId,int employeeId) throws SQLException {
        return dao.query(connection,"""
                SELECT ContractType,StartDate,EndDate FROM EmploymentContracts
                WHERE CompanyId=? AND EmployeeId=? AND Status IN ('ACTIVE','EXPIRED','TERMINATED')
                ORDER BY StartDate DESC,ContractId DESC
                """,companyId,employeeId);
    }

    public Map<String,Object> organization(Connection connection,int companyId,int viewerId,boolean manager) throws SQLException {
        var members=dao.query(connection,CURRENT_ASSIGNMENTS+"""
                SELECT e.EmployeeId,e.EmployeeCode,e.FullName,e.Status,a.DepartmentId,a.PositionId,a.ManagerId,
                    d.DepartmentName,p.PositionTitle,r.LevelName
                FROM Employees e
                LEFT JOIN CurrentAssignments a ON a.CompanyId=e.CompanyId AND a.EmployeeId=e.EmployeeId
                LEFT JOIN Departments d ON d.CompanyId=a.CompanyId AND d.DepartmentId=a.DepartmentId
                LEFT JOIN Positions p ON p.CompanyId=a.CompanyId AND p.PositionId=a.PositionId
                LEFT JOIN ResponsibilityLevels r ON r.CompanyId=p.CompanyId AND r.ResponsibilityLevelId=p.ResponsibilityLevelId
                WHERE e.CompanyId=? AND e.Status='Active' AND (e.EmployeeId=?
                    OR e.EmployeeId IN (SELECT ManagerId FROM CurrentAssignments WHERE EmployeeId=?)
                    OR (?=1 AND a.ManagerId=?))
                ORDER BY d.DepartmentName,e.FullName,e.EmployeeId
                """,companyId,companyId,viewerId,viewerId,manager?1:0,viewerId);
        var administrators=dao.query(connection,"""
                SELECT u.DisplayName,r.RoleCode,c.CompanyName,employee.EmployeeId
                FROM Users u JOIN Roles r ON r.RoleId=u.RoleId JOIN Companies c ON c.CompanyId=u.CompanyId
                OUTER APPLY (SELECT TOP(1) e.EmployeeId FROM Employees e WHERE e.CompanyId=u.CompanyId AND e.UserId=u.UserId ORDER BY e.EmployeeId) employee
                WHERE u.CompanyId=? AND u.IsActive=1 AND u.IsLocked=0 AND r.RoleCode IN ('COMPANY_ADMIN','HR')
                ORDER BY r.RoleCode,u.DisplayName
                """,companyId);
        return Map.of("members",members,"administrators",administrators);
    }

    public Map<String,Object> colleague(Connection connection,int companyId,int viewerId,int employeeId,boolean manager) throws SQLException {
        if(dao.query(connection,CURRENT_ASSIGNMENTS+"""
                SELECT 1 FROM Employees e
                LEFT JOIN CurrentAssignments target ON target.EmployeeId=e.EmployeeId AND target.CompanyId=e.CompanyId
                WHERE e.CompanyId=? AND e.EmployeeId=? AND (e.EmployeeId=?
                    OR e.EmployeeId IN (SELECT ManagerId FROM CurrentAssignments WHERE EmployeeId=?)
                    OR (?=1 AND target.ManagerId=?)
                    OR EXISTS (SELECT 1 FROM Users u JOIN Roles r ON r.RoleId=u.RoleId
                        WHERE u.CompanyId=e.CompanyId AND u.UserId=e.UserId AND u.IsActive=1 AND u.IsLocked=0
                            AND r.RoleCode IN ('COMPANY_ADMIN','HR')))
                """,companyId,companyId,employeeId,viewerId,viewerId,manager?1:0,viewerId).isEmpty())
            throw new SecurityException("Bạn chỉ được xem hồ sơ trong phạm vi quản lý và quản trị công ty của mình.");
        var scope=dao.query(connection,CURRENT_ASSIGNMENTS+"""
                SELECT CASE WHEN employee.EmployeeId=? OR (target.ManagerId IS NOT NULL AND target.ManagerId=viewer.ManagerId)
                    OR (?=1 AND target.ManagerId=?) THEN 1 ELSE 0 END AS CanViewContact,
                    CASE WHEN ?=1 AND target.ManagerId=? THEN 1 ELSE 0 END AS CanViewContracts
                FROM Employees employee
                LEFT JOIN CurrentAssignments target ON target.EmployeeId=employee.EmployeeId AND target.CompanyId=employee.CompanyId
                LEFT JOIN CurrentAssignments viewer ON viewer.EmployeeId=?
                WHERE employee.CompanyId=? AND employee.EmployeeId=? AND employee.Status='Active'
                """,companyId,viewerId,manager?1:0,viewerId,manager?1:0,viewerId,viewerId,companyId,employeeId);
        if(scope.size()!=1) throw new SecurityException("Không tìm thấy nhân viên trong workspace của bạn.");
        boolean contact=((Number)scope.get(0).get("CanViewContact")).intValue()==1;
        boolean contractAccess=((Number)scope.get(0).get("CanViewContracts")).intValue()==1;
        var profiles=dao.query(connection,CURRENT_ASSIGNMENTS+DIRECTORY.replace("e.Email",contact?"e.Email":"N'••••••' AS Email")
                +" WHERE e.CompanyId=? AND e.EmployeeId=?",companyId,companyId,employeeId);
        var profile=new java.util.LinkedHashMap<String,Object>(profiles.get(0));
        var masked=dao.query(connection,"""
                SELECT CONCAT(LEFT(Phone,2),N'••••••••') AS Phone,CONCAT(LEFT(IdentityNumber,3),N'•••••••••') AS IdentityNumber
                FROM Employees WHERE CompanyId=? AND EmployeeId=?
                """,companyId,employeeId).get(0);
        profile.put("Phone",masked.get("Phone"));
        profile.put("IdentityNumber",masked.get("IdentityNumber"));
        profile.put("PermanentAddress","••••••");
        profile.put("Hometown","••••••");
        profile.put("GrossSalary","•••••• ₫");
        profile.put("PositionAllowance","•••••• ₫");
        profile.put("TravelAllowance","•••••• ₫");
        return Map.of("profile",profile,"canViewContracts",contractAccess,
                "contracts",contractAccess?contracts(connection,companyId,employeeId):List.of(),"isSelf",viewerId==employeeId);
    }

    public List<Map<String,Object>> ownContracts(Connection connection,int companyId,int employeeId) throws SQLException {
        return dao.query(connection,"""
                SELECT ContractId,ContractCode,ContractType,StartDate,EndDate,GrossSalary,TravelAllowance,
                    CASE WHEN Status='ACTIVE' AND EndDate<CONVERT(date,SYSDATETIME()) THEN 'EXPIRED'
                         WHEN Status='ACTIVE' AND StartDate>CONVERT(date,SYSDATETIME()) THEN 'SCHEDULED'
                         ELSE Status END AS Status,
                    SignedDate,TerminatedDate,TerminationReason,Notes
                FROM EmploymentContracts WHERE CompanyId=? AND EmployeeId=? AND Status IN ('ACTIVE','EXPIRED','TERMINATED')
                ORDER BY StartDate DESC,ContractId DESC
                """,companyId,employeeId);
    }
}
