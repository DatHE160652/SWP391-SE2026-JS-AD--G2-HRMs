package com.hrms.dao;

import com.hrms.model.EmployeeAssignment;
import com.hrms.service.FieldValidationException;
import com.hrms.util.DBConnection;
import java.sql.*;
import java.util.*;

public class EmployeeAssignmentDAO {
    private static final String MEMBERS = """
            SELECT e.EmployeeId,e.EmployeeCode,e.FullName,e.DepartmentId,e.ManagerId,e.PositionId,
                   p.PositionTitle,d.DepartmentName,m.FullName AS ManagerName,r.RoleCode,
                   CASE WHEN e.Status='Active' AND p.IsActive=1 AND p.CanManageEmployees=1
                        AND EXISTS (SELECT 1 FROM ResponsibilityLevels level WHERE level.CompanyId=p.CompanyId
                            AND level.ResponsibilityLevelId=p.ResponsibilityLevelId AND level.IsActive=1
                            AND UPPER(LTRIM(RTRIM(level.LevelCode))) NOT IN ('EMP','EMPLOYEE')
                            AND UPPER(LTRIM(RTRIM(level.LevelName)))<>'EMPLOYEE')
                        AND p.DepartmentId=e.DepartmentId AND d.IsActive=1
                        AND EXISTS(SELECT 1 FROM EmployeePositionAssignments currentAssignment
                            WHERE currentAssignment.CompanyId=e.CompanyId AND currentAssignment.EmployeeId=e.EmployeeId
                                AND currentAssignment.PositionId=e.PositionId AND currentAssignment.DepartmentId=e.DepartmentId
                                AND currentAssignment.EffectiveFrom<=CONVERT(date,SYSDATETIME())
                                AND (currentAssignment.EffectiveTo IS NULL OR currentAssignment.EffectiveTo>CONVERT(date,SYSDATETIME())))
                        THEN 1 ELSE 0 END AS CanManageTeam
            FROM Employees e
            LEFT JOIN Positions p ON p.CompanyId=e.CompanyId AND p.PositionId=e.PositionId
            LEFT JOIN Departments d ON d.CompanyId=e.CompanyId AND d.DepartmentId=e.DepartmentId
            LEFT JOIN Employees m ON m.CompanyId=e.CompanyId AND m.EmployeeId=e.ManagerId
            LEFT JOIN Users u ON u.CompanyId=e.CompanyId AND u.UserId=e.UserId
            LEFT JOIN Roles r ON r.RoleId=u.RoleId
            WHERE e.CompanyId=? ORDER BY e.FullName,e.EmployeeId
            """;

    public List<DepartmentMemberOption> getDepartmentMemberOptions(int companyId, int departmentId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            return members(connection, companyId).values().stream().map(member -> member.withSelection(
                    Objects.equals(member.departmentId(), departmentId), true)).toList();
        }
    }

    public List<DepartmentMemberOption> getDirectReports(int companyId, int managerEmployeeId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            var employees = members(connection, companyId);
            requireEmployee(employees, managerEmployeeId);
            return employees.values().stream().filter(member -> Objects.equals(member.managerId(), managerEmployeeId)).toList();
        }
    }

    public List<DepartmentMemberOption> getEligibleManagers(int companyId, Integer departmentId, int employeeId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            var employees = members(connection, companyId);
            if ("MANAGER".equals(requireEmployee(employees, employeeId).roleCode())) return List.of();
            var links=new EmployeeLifecycleDAO().query(connection,"SELECT PositionId,ReportsToPositionId FROM Positions WHERE CompanyId=?",companyId);
            Integer parent=links.stream().filter(link->Objects.equals(link.get("PositionId"),requireEmployee(employees,employeeId).positionId()))
                    .map(link->(Integer)link.get("ReportsToPositionId")).filter(Objects::nonNull).findFirst().orElse(null);
            return employees.values().stream().filter(manager -> manager.canManageTeam()
                    && departmentId != null && departmentId.equals(manager.departmentId())
                    && (parent==null || parent.equals(manager.positionId()))
                    && !wouldCycle(employees, employeeId, manager.employeeId())).toList();
        }
    }

    public List<DepartmentMemberOption> getTeamOptions(int companyId, int managerId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            var employees = members(connection, companyId);
            var manager = requireEmployee(employees, managerId);
            Map<Integer,Integer> links=new HashMap<>();
            for (var link:new EmployeeLifecycleDAO().query(connection,"SELECT PositionId,ReportsToPositionId FROM Positions WHERE CompanyId=?",companyId))
                links.put((Integer)link.get("PositionId"),(Integer)link.get("ReportsToPositionId"));
            return employees.values().stream().filter(member -> member.employeeId() != managerId
                    && (Objects.equals(member.managerId(), managerId) || Objects.equals(member.departmentId(), manager.departmentId())))
                    .map(member -> member.withSelection(Objects.equals(member.managerId(), managerId),
                            manager.canManageTeam() && Objects.equals(member.departmentId(), manager.departmentId())
                                    && member.managerId()==null
                                    && (links.get(member.positionId())==null || links.get(member.positionId()).equals(manager.positionId()))
                                    && !"MANAGER".equals(member.roleCode())
                                    && !wouldCycle(employees, member.employeeId(), managerId))).toList();
        }
    }

    public DepartmentMemberOption getMember(int companyId, int employeeId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            return requireEmployee(members(connection, companyId), employeeId);
        }
    }

    public boolean assignManager(int companyId, int employeeId, Integer managerId) throws SQLException {
        return assignManager(companyId,employeeId,managerId,null,null);
    }

    public boolean assignManager(int companyId,int employeeId,Integer managerId,Integer actorId,String reason) throws SQLException {
        return OrganizationTransaction.execute(companyId, connection -> {
            var employees = members(connection, companyId);
            var employee = requireEmployee(employees, employeeId);
            requireManagerSnapshot(connection,companyId,employeeId,employee.managerId());
            requireInitialManagerAssignment(employee.managerId(),managerId);
            validateManagerAssignment(employees, employeeId, managerId, employee.departmentId());
            updateManager(connection, companyId, employeeId, managerId);
            if (actorId!=null) OrganizationTransaction.recordManagerChange(connection,companyId,actorId,employeeId,employee.managerId(),managerId,reason);
            return true;
        });
    }

    public boolean assignDepartmentMembers(int companyId, int departmentId, int managerId, int[] employeeIds) throws SQLException {
        return assignDepartmentMembers(companyId,departmentId,managerId,employeeIds,null);
    }

    public boolean assignDepartmentMembers(int companyId,int departmentId,int managerId,int[] employeeIds,Integer actorId) throws SQLException {
        return OrganizationTransaction.execute(companyId, connection -> {
            var employees = members(connection, companyId);
            var manager = requireEmployee(employees, managerId);
            if (!manager.canManageTeam() || !Objects.equals(manager.departmentId(), departmentId)) {
                throw new FieldValidationException("managerId", "Vui lòng chọn quản lý đang hoạt động và có quyền quản lý trong phòng ban.");
            }
            for (int employeeId : new LinkedHashSet<>(Arrays.stream(employeeIds).boxed().toList())) {
                var employee = requireEmployee(employees, employeeId);
                requireManagerSnapshot(connection,companyId,employeeId,employee.managerId());
                if (employee.managerId() != null && !Objects.equals(employee.managerId(), managerId)) {
                    throw new FieldValidationException("employeeIds", "Nhân viên đã có quản lý. Vui lòng sử dụng phân công trực tiếp tại Phòng ban.");
                }
                validateManagerAssignment(employees, employeeId, managerId, employee.departmentId());
                updateManager(connection, companyId, employeeId, managerId);
                if (actorId!=null) OrganizationTransaction.recordManagerChange(connection,companyId,actorId,employeeId,employee.managerId(),managerId,null);
            }
            return true;
        });
    }

    public boolean manageTeam(int companyId, int managerId, int[] additions, int[] removals,
                              Map<Integer,Integer> expectedManagers, boolean confirmTransfers, Integer actorId) throws SQLException {
        return manageTeam(companyId,managerId,additions,removals,expectedManagers,confirmTransfers,actorId,null);
    }

    public boolean manageTeam(int companyId,int managerId,int[] additions,int[] removals,
                              Map<Integer,Integer> expectedManagers,boolean confirmTransfers,Integer actorId,String reason) throws SQLException {
        if (actorId == null) throw new SecurityException("Cần quyền quản trị nhân sự để quản lý thành viên nhóm.");
        return OrganizationTransaction.execute(companyId, connection -> {
            var employees = members(connection, companyId);
            var manager = requireEmployee(employees, managerId);
            Set<Integer> added = new LinkedHashSet<>(Arrays.stream(additions).boxed().toList());
            Set<Integer> removed = new LinkedHashSet<>(Arrays.stream(removals).boxed().toList());
            if(!removed.isEmpty()) throw new FieldValidationException("employeeIds","Vui lòng gỡ quản lý bằng phân công trực tiếp tại Phòng ban, không bỏ chọn tại danh sách nhóm.");
            if (added.stream().anyMatch(removed::contains)) throw new FieldValidationException("employeeIds", "Thay đổi thành viên nhóm bị xung đột.");
            if (!added.isEmpty() && !manager.canManageTeam()) {
                throw new FieldValidationException("managerId", "Chức danh hiện tại của nhân viên này không có quyền quản lý nhân viên.");
            }
            for (int employeeId : added) {
                var member = requireEmployee(employees, employeeId);
                if (!expectedManagers.containsKey(employeeId) || !Objects.equals(member.managerId(), expectedManagers.get(employeeId))) {
                    throw new FieldValidationException("employeeIds", "Quản lý của một nhân viên đã thay đổi. Vui lòng tải lại trước khi lưu.");
                }
                requireManagerSnapshot(connection,companyId,employeeId,member.managerId());
                requireInitialManagerAssignment(member.managerId(),managerId);
                validateManagerAssignment(employees, employeeId, managerId, member.departmentId());
                updateManager(connection, companyId, employeeId, managerId);
                OrganizationTransaction.recordManagerChange(connection,companyId,actorId,employeeId,member.managerId(),managerId,reason);
                employees.put(employeeId, member.withManager(managerId));
            }
            return true;
        });
    }

    public void validateManagerAssignment(Connection connection, int companyId, int employeeId,
                                           Integer managerId, Integer departmentId) throws SQLException {
        validateManagerAssignment(members(connection, companyId), employeeId, managerId, departmentId);
        var employee=getEmployeePosition(connection,companyId,employeeId);
        new ReportingPositionDAO().validateManager(connection,companyId,employee,managerId);
    }

    private Integer getEmployeePosition(Connection connection,int companyId,int employeeId) throws SQLException {
        return (Integer)new EmployeeLifecycleDAO().employee(connection,companyId,employeeId).get("PositionId");
    }

    public void validateManagerAssignment(Connection connection, int companyId, int employeeId,
            Integer managerId, Integer departmentId, Integer positionId) throws SQLException {
        validateManagerAssignment(members(connection,companyId),employeeId,managerId,departmentId);
        new ReportingPositionDAO().validateManager(connection,companyId,positionId,managerId);
    }

    public static void requireInitialManagerAssignment(Integer existing,Integer requested) {
        if(existing!=null && !Objects.equals(existing,requested))
            throw new FieldValidationException("managerId","Nhân viên đã có quản lý. Vui lòng chuyển hoặc gỡ quản lý tại Phòng ban → Phân công nhân viên.");
    }

    public void requireManagerSnapshot(Connection connection,int companyId,int employeeId,Integer expected) throws SQLException {
        try(PreparedStatement statement=connection.prepareStatement("SELECT ManagerId FROM EmployeePositionAssignments WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND EmployeeId=? AND EffectiveTo IS NULL")) {
            statement.setInt(1,companyId);statement.setInt(2,employeeId);
            try(ResultSet rows=statement.executeQuery()) {
                if(rows.next()) {
                    if(!Objects.equals(expected,rows.getObject("ManagerId")) || rows.next())
                        throw new FieldValidationException("managerId","Phân công quản lý hiện tại chưa đồng bộ. Vui lòng kiểm tra lịch sử trước khi thay đổi.");
                }
            }
        }
    }

    private void validateManagerAssignment(Map<Integer,DepartmentMemberOption> employees, int employeeId,
                                            Integer managerId, Integer departmentId) {
        var employee = requireEmployee(employees, employeeId);
        if (managerId == null) return;
        if ("MANAGER".equals(employee.roleCode())) {
            throw new FieldValidationException("managerId", "Nhân viên có quyền MANAGER không được gán quản lý trực tiếp.");
        }
        var manager = employees.get(managerId);
        if (manager == null || !manager.canManageTeam()) {
            throw new FieldValidationException("managerId", "Vui lòng chọn nhân viên đang hoạt động trong công ty và có chức danh cho phép quản lý.");
        }
        if (departmentId == null || !departmentId.equals(manager.departmentId())) {
            throw new FieldValidationException("managerId", "Quản lý trực tiếp phải thuộc phòng ban đã chọn.");
        }
        if (wouldCycle(employees, employeeId, managerId)) {
            throw new FieldValidationException("managerId", "Phân công này sẽ tạo ra vòng lặp quản lý.");
        }
    }

    private boolean wouldCycle(Map<Integer,DepartmentMemberOption> employees, int employeeId, int managerId) {
        Set<Integer> visited = new HashSet<>();
        Integer current = managerId;
        while (current != null) {
            if (current == employeeId || !visited.add(current)) return true;
            var manager = employees.get(current);
            if (manager == null) return true;
            current = manager.managerId();
        }
        return false;
    }

    public void validateDirectReports(Connection connection, int companyId, int employeeId,
                                      Integer departmentId, Integer positionId) throws SQLException {
        var employees = members(connection, companyId);
        boolean hasReports = employees.values().stream().anyMatch(member -> Objects.equals(member.managerId(), employeeId));
        if (!hasReports) return;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM Positions WHERE CompanyId=? AND PositionId=? AND DepartmentId=?
                    AND IsActive=1 AND CanManageEmployees=1
                    AND EXISTS (SELECT 1 FROM ResponsibilityLevels level WHERE level.CompanyId=Positions.CompanyId
                        AND level.ResponsibilityLevelId=Positions.ResponsibilityLevelId AND level.IsActive=1
                        AND UPPER(LTRIM(RTRIM(level.LevelCode))) NOT IN ('EMP','EMPLOYEE')
                        AND UPPER(LTRIM(RTRIM(level.LevelName)))<>'EMPLOYEE')
                """)) {
            statement.setInt(1, companyId);
            statement.setObject(2, positionId, Types.INTEGER);
            statement.setObject(3, departmentId, Types.INTEGER);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next() || !Objects.equals(requireEmployee(employees, employeeId).departmentId(), departmentId)) {
                    throw new FieldValidationException("positionId", "Vui lòng xử lý các cấp dưới hiện tại trước khi bỏ khả năng quản lý hoặc chuyển phòng ban.");
                }
            }
        }
    }

    public void updateManager(Connection connection, int companyId, int employeeId, Integer managerId) throws SQLException {
        new ReportingPositionDAO().validateManager(connection,companyId,getEmployeePosition(connection,companyId,employeeId),managerId);
        if (managerId != null) {
            try (PreparedStatement statement=connection.prepareStatement("""
                    SELECT 1 FROM Employees e JOIN Users u ON u.CompanyId=e.CompanyId AND u.UserId=e.UserId
                    JOIN Roles r ON r.RoleId=u.RoleId
                    WHERE e.CompanyId=? AND e.EmployeeId=? AND r.RoleCode='MANAGER'
                    """)) {
                statement.setInt(1,companyId);
                statement.setInt(2,employeeId);
                try (ResultSet rows=statement.executeQuery()) {
                    if (rows.next()) throw new FieldValidationException("managerId","Nhân viên có quyền MANAGER không được gán quản lý trực tiếp.");
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE Employees SET ManagerId=?,UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND EmployeeId=?")) {
            statement.setObject(1, managerId, Types.INTEGER);
            statement.setInt(2, companyId);
            statement.setInt(3, employeeId);
            if (statement.executeUpdate() != 1) throw new SQLException("Phân công nhân viên đã thay đổi. Vui lòng tải lại trang.");
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                DECLARE @CompanyId int=?,@EmployeeId int=?,@ManagerId int=?,@Today date=CONVERT(date,SYSDATETIME());
                DECLARE @Previous TABLE(CompanyId int,EmployeeId int,DepartmentId int,PositionId int);
                UPDATE EmployeePositionAssignments SET EffectiveTo=@Today
                OUTPUT DELETED.CompanyId,DELETED.EmployeeId,DELETED.DepartmentId,DELETED.PositionId INTO @Previous
                WHERE CompanyId=@CompanyId AND EmployeeId=@EmployeeId AND EffectiveTo IS NULL
                    AND EffectiveFrom<@Today AND ISNULL(ManagerId,0)<>ISNULL(@ManagerId,0);
                INSERT INTO EmployeePositionAssignments(CompanyId,EmployeeId,DepartmentId,PositionId,ManagerId,EffectiveFrom,ChangeType,Reason)
                SELECT CompanyId,EmployeeId,DepartmentId,PositionId,@ManagerId,@Today,'MANAGER_CHANGE',N'Manager relationship changed' FROM @Previous;
                UPDATE EmployeePositionAssignments SET ManagerId=@ManagerId
                WHERE CompanyId=@CompanyId AND EmployeeId=@EmployeeId AND EffectiveTo IS NULL;
                """)) {
            statement.setInt(1, companyId);
            statement.setInt(2, employeeId);
            statement.setObject(3, managerId, Types.INTEGER);
            statement.executeUpdate();
        }
    }

    private Map<Integer,DepartmentMemberOption> members(Connection connection, int companyId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(MEMBERS)) {
            statement.setInt(1, companyId);
            try (ResultSet rows = statement.executeQuery()) {
                Map<Integer,DepartmentMemberOption> members = new LinkedHashMap<>();
                while (rows.next()) {
                    var member = new DepartmentMemberOption(rows.getInt("EmployeeId"), rows.getString("EmployeeCode"),
                            rows.getString("FullName"), (Integer) rows.getObject("DepartmentId"), (Integer) rows.getObject("ManagerId"),
                            (Integer) rows.getObject("PositionId"), rows.getString("PositionTitle"), rows.getString("RoleCode"), false,
                            rows.getBoolean("CanManageTeam"), rows.getString("DepartmentName"), rows.getString("ManagerName"), true);
                    members.put(member.employeeId(), member);
                }
                return members;
            }
        }
    }

    private DepartmentMemberOption requireEmployee(Map<Integer,DepartmentMemberOption> employees, int employeeId) {
        var employee = employees.get(employeeId);
        if (employee == null) throw new IllegalArgumentException("Không tìm thấy nhân viên trong công ty của bạn.");
        return employee;
    }

    public EmployeeAssignment getEmployeeOrganization(int companyId, int employeeId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT EmployeeId,DepartmentId,PositionId,ManagerId FROM Employees WHERE CompanyId=? AND EmployeeId=?")) {
            statement.setInt(1, companyId);
            statement.setInt(2, employeeId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? new EmployeeAssignment(rows.getInt("EmployeeId"), (Integer) rows.getObject("DepartmentId"),
                        (Integer) rows.getObject("PositionId"), (Integer) rows.getObject("ManagerId")) : null;
            }
        }
    }

    public boolean employeeExists(int companyId, int employeeId) throws SQLException {
        return getEmployeeOrganization(companyId, employeeId) != null;
    }

    public boolean departmentExists(int companyId, int departmentId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM Departments WHERE CompanyId=? AND DepartmentId=?")) {
            statement.setInt(1, companyId);
            statement.setInt(2, departmentId);
            try (ResultSet rows = statement.executeQuery()) { return rows.next(); }
        }
    }

    public record DepartmentMemberOption(int employeeId, String employeeCode, String fullName,
                                         Integer departmentId, Integer managerId, Integer positionId,
                                         String positionTitle, String roleCode, boolean selected, boolean canManageTeam,
                                         String departmentName, String managerName, boolean assignable) {
        DepartmentMemberOption withSelection(boolean selected, boolean assignable) {
            return new DepartmentMemberOption(employeeId, employeeCode, fullName, departmentId, managerId, positionId,
                    positionTitle, roleCode, selected, canManageTeam, departmentName, managerName, assignable);
        }
        DepartmentMemberOption withManager(Integer newManager) {
            return new DepartmentMemberOption(employeeId, employeeCode, fullName, departmentId, newManager, positionId,
                    positionTitle, roleCode, selected, canManageTeam, departmentName, null, assignable);
        }
    }
}
