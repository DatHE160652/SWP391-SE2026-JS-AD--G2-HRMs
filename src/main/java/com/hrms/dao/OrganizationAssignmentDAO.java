package com.hrms.dao;

import com.hrms.service.FieldValidationException;
import com.hrms.util.DBConnection;
import java.sql.*;
import java.util.Objects;

public class OrganizationAssignmentDAO {
    private static final String UNASSIGNED = """
            e.DepartmentId IS NULL AND e.PositionId IS NULL AND e.ManagerId IS NULL
            AND NOT EXISTS(SELECT 1 FROM EmployeePositionAssignments a WHERE a.CompanyId=e.CompanyId AND a.EmployeeId=e.EmployeeId)
            AND NOT EXISTS(SELECT 1 FROM EmployeePositionChanges c WHERE c.CompanyId=e.CompanyId AND c.EmployeeId=e.EmployeeId)
            """;

    public java.util.List<java.util.Map<String,Object>> initialAssignmentCandidates(int companyId, int departmentId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            validateSelection(connection, companyId, departmentId, null);
            return new EmployeeLifecycleDAO().query(connection, """
                    SELECT e.EmployeeId,e.EmployeeCode,e.FullName FROM Employees e
                    WHERE e.CompanyId=? AND e.Status='Active'
                    AND
                    """ + UNASSIGNED + " ORDER BY e.FullName,e.EmployeeId", companyId);
        }
    }

    public boolean assignDepartmentEmployees(int companyId, int[] employeeIds, int departmentId, int positionId,
            Integer managerId, Integer actorId) throws SQLException {
        return OrganizationTransaction.execute(companyId, connection -> {
            requireActor(connection, companyId, actorId);
            validateSelection(connection, companyId, departmentId, positionId);
            String role = new OrganizationRoleDAO().roleForPosition(connection, companyId, positionId);
            if ("MANAGER".equals(role) && managerId != null)
                throw new FieldValidationException("managerId", "Chức danh quản lý không được gán quản lý cấp trên.");
            if ("MANAGER".equals(role) && employeeIds.length > 1)
                throw new FieldValidationException("employeeIds", "Chức danh quản lý chỉ được chọn một nhân viên mỗi lần.");
            for (int employeeId : employeeIds) {
                if (new EmployeeLifecycleDAO().query(connection, """
                        SELECT 1 FROM Employees e WITH (UPDLOCK,HOLDLOCK)
                        WHERE e.CompanyId=? AND e.EmployeeId=? AND e.Status='Active' AND
                        """ + UNASSIGNED, companyId, employeeId).isEmpty())
                    throw new FieldValidationException("employeeIds",
                            "Danh sách đã thay đổi hoặc nhân viên đã được phân công. Vui lòng tải lại; để chuyển phòng ban/chức danh, hãy mở hồ sơ nhân viên.");
            }
            for (int employeeId : employeeIds)
                saveOrganization(connection, companyId, employeeId, departmentId, positionId, managerId, actorId,
                        "Phân công trực tiếp từ phòng ban");
            return true;
        });
    }

    public boolean recordInitialAssignment(int companyId, int employeeId, int departmentId, int positionId,
            java.time.LocalDate effectiveFrom, Integer actorId, String reason) throws SQLException {
        return OrganizationTransaction.execute(companyId, connection -> recordInitialAssignment(connection, companyId,
                employeeId, departmentId, positionId, null, effectiveFrom, actorId, reason));
    }

    public boolean recordInitialAssignments(int companyId, int[] employeeIds, int departmentId, int positionId,
            Integer managerId, Integer actorId) throws SQLException {
        return OrganizationTransaction.execute(companyId, connection -> {
            requireActor(connection, companyId, actorId);
            validateSelection(connection, companyId, departmentId, positionId);
            var dao = new EmployeeLifecycleDAO();
            boolean managerPosition = !dao.query(connection, """
                    SELECT 1 FROM Positions p JOIN ResponsibilityLevels r
                        ON r.CompanyId=p.CompanyId AND r.ResponsibilityLevelId=p.ResponsibilityLevelId
                    WHERE p.CompanyId=? AND p.PositionId=? AND UPPER(r.LevelCode)='MANAGER'
                    """, companyId, positionId).isEmpty();
            if (managerPosition && managerId != null)
                throw new FieldValidationException("managerId", "Chức danh cấp Manager không được gán quản lý cấp trên.");
            if (managerPosition && employeeIds.length > 1)
                throw new FieldValidationException("employeeIds", "Chức danh cấp Manager chỉ được phân công cho một nhân viên.");
            java.time.LocalDate today = java.time.LocalDate.parse(dao.query(connection,
                    "SELECT CONVERT(date,SYSDATETIME()) AS Today").get(0).get("Today").toString());
            for (int employeeId : employeeIds) recordInitialAssignment(connection, companyId, employeeId, departmentId,
                    positionId, managerId, today, actorId, "Ghi nhận phân công ban đầu lên hệ thống");
            return true;
        });
    }

    private boolean recordInitialAssignment(Connection connection, int companyId, int employeeId, int departmentId,
            int positionId, Integer managerId, java.time.LocalDate effectiveFrom, Integer actorId, String reason) throws SQLException {
            requireActor(connection, companyId, actorId);
            var dao = new EmployeeLifecycleDAO();
            var employee = dao.employee(connection, companyId, employeeId);
            java.time.LocalDate today = java.time.LocalDate.parse(dao.query(connection,
                    "SELECT CONVERT(date,SYSDATETIME()) AS Today").get(0).get("Today").toString());
            if (effectiveFrom == null || effectiveFrom.isAfter(today))
                throw new FieldValidationException("effectiveFrom", "Chọn ngày bắt đầu đảm nhiệm, không được sau hôm nay.");
            if (employee.get("DateOfBirth") != null && effectiveFrom.isBefore(java.time.LocalDate.parse(employee.get("DateOfBirth").toString())))
                throw new FieldValidationException("effectiveFrom", "Ngày bắt đầu phân công không được trước ngày sinh.");
            if (!"Active".equals(employee.get("Status")))
                throw new FieldValidationException("employeeId", "Chỉ phân công cho nhân viên đang hoạt động.");
            if (employee.get("PositionId") != null || employee.get("ManagerId") != null
                    || (employee.get("DepartmentId") != null && !Objects.equals(employee.get("DepartmentId"), departmentId))
                    || !dao.query(connection, """
                        SELECT 1 FROM EmployeePositionAssignments WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND EmployeeId=? AND EffectiveTo IS NULL
                        UNION ALL SELECT 1 FROM EmployeePositionChanges WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND EmployeeId=? AND Status IN ('PENDING','APPROVED')
                        """, companyId, employeeId, companyId, employeeId).isEmpty())
                throw new FieldValidationException("employeeId", "Nhân viên đang có phân công còn hiệu lực hoặc có đề xuất thay đổi đang chờ xử lý.");
            validateSelection(connection, companyId, departmentId, positionId);
            new PositionOccupancyDAO().validate(connection, companyId, employeeId, positionId, effectiveFrom, null);
            new EmployeeAssignmentDAO().validateDirectReports(connection, companyId, employeeId, departmentId, positionId);
            new EmployeeAssignmentDAO().validateManagerAssignment(connection, companyId, employeeId, managerId, departmentId, positionId);
            if (managerId != null && dao.query(connection, """
                    SELECT 1 FROM EmployeePositionAssignments a JOIN Employees manager
                        ON manager.CompanyId=a.CompanyId AND manager.EmployeeId=a.EmployeeId AND manager.PositionId=a.PositionId
                    WHERE a.CompanyId=? AND a.EmployeeId=? AND a.DepartmentId=?
                        AND a.EffectiveFrom<=? AND (a.EffectiveTo IS NULL OR a.EffectiveTo>?)
                    """, companyId, managerId, departmentId, effectiveFrom, effectiveFrom).isEmpty())
                throw new FieldValidationException("managerId", "Phân công của quản lý không có hiệu lực tại ngày phân công nhân viên.");
            dao.execute(connection, """
                    INSERT INTO EmployeePositionAssignments(CompanyId,EmployeeId,DepartmentId,PositionId,ManagerId,EffectiveFrom,ChangeType,Reason)
                    VALUES(?,?,?,?,?,?,'BASELINE',?)
                    """, companyId, employeeId, departmentId, positionId, managerId, effectiveFrom, reason);
            if (dao.execute(connection, """
                    UPDATE Employees SET DepartmentId=?,PositionId=?,ManagerId=?,UpdatedAt=SYSUTCDATETIME()
                    WHERE CompanyId=? AND EmployeeId=? AND PositionId IS NULL AND ManagerId IS NULL
                    """, departmentId, positionId, managerId, companyId, employeeId) != 1)
                throw new FieldValidationException("employeeId", "Hồ sơ đã thay đổi. Vui lòng tải lại danh sách.");
            recordInitialAssignment(connection, companyId, employeeId, (Integer) employee.get("DepartmentId"), departmentId, positionId, actorId, reason);
            OrganizationTransaction.recordManagerChange(connection, companyId, actorId, employeeId, null, managerId, reason);
            new OrganizationRoleDAO().synchronize(connection,companyId,employeeId,positionId,actorId);
            return true;
    }

    public boolean assignOrganization(int companyId, int employeeId, Integer departmentId, Integer positionId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                assignOrganization(connection, companyId, employeeId, departmentId, positionId);
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void assignOrganization(Connection connection, int companyId, int employeeId, Integer departmentId, Integer positionId) throws SQLException {
        OrganizationTransaction.lockCompany(connection, companyId);
        Integer currentManager;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT DepartmentId,PositionId,ManagerId FROM Employees WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND EmployeeId=?
                """)) {
            statement.setInt(1,companyId);
            statement.setInt(2,employeeId);
            try (ResultSet rows=statement.executeQuery()) {
                if (!rows.next()) throw new IllegalArgumentException("Không tìm thấy nhân viên trong công ty của bạn.");
                Integer oldDepartment=(Integer)rows.getObject("DepartmentId");
                Integer oldPosition=(Integer)rows.getObject("PositionId");
                currentManager=(Integer)rows.getObject("ManagerId");
                if (Objects.equals(oldDepartment,departmentId) && Objects.equals(oldPosition,positionId)) return;
                if (!Objects.equals(oldDepartment, departmentId) && rows.getObject("ManagerId") != null) {
                    throw new FieldValidationException("managerId", "Vui lòng xử lý quản lý trực tiếp hiện tại trước khi đổi phòng ban. Hệ thống không tự gỡ quản lý.");
                }
                if (oldPosition!=null) throw new FieldValidationException("positionId",
                        "Nhân viên đã có chức danh cần sử dụng thay đổi được duyệt và có ngày hiệu lực.");
            }
        }
        try (PreparedStatement statement=connection.prepareStatement("""
                SELECT 1 FROM EmployeePositionAssignments WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND EmployeeId=? AND EffectiveTo IS NULL
                UNION ALL SELECT 1 FROM EmployeePositionChanges WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND EmployeeId=? AND Status IN ('PENDING','APPROVED')
                """)) {
            statement.setInt(1,companyId); statement.setInt(2,employeeId);
            statement.setInt(3,companyId); statement.setInt(4,employeeId);
            try(ResultSet rows=statement.executeQuery()) {
                if(rows.next()) throw new FieldValidationException("positionId","Nhân viên đang có phân công chức danh còn hiệu lực hoặc có đề xuất đang chờ xử lý.");
            }
        }
        validateSelection(connection,companyId,departmentId,positionId);
        new PositionOccupancyDAO().validate(connection,companyId,employeeId,positionId,
                java.time.LocalDate.parse(new EmployeeLifecycleDAO().query(connection,"SELECT CONVERT(date,SYSDATETIME()) AS Today").get(0).get("Today").toString()),null);
        new EmployeeAssignmentDAO().validateManagerAssignment(connection,companyId,employeeId,currentManager,departmentId);
        new EmployeeAssignmentDAO().validateDirectReports(connection, companyId, employeeId, departmentId, positionId);
        try(PreparedStatement statement=connection.prepareStatement("""
                UPDATE Employees SET DepartmentId=?,PositionId=?,UpdatedAt=SYSUTCDATETIME()
                WHERE CompanyId=? AND EmployeeId=?
                """)) {
            statement.setObject(1,departmentId,Types.INTEGER); statement.setObject(2,positionId,Types.INTEGER);
            statement.setInt(3,companyId); statement.setInt(4,employeeId);
            if(statement.executeUpdate()!=1) throw new SQLException("Phân công nhân viên đã thay đổi.");
        }
        if(positionId!=null) {
            try(PreparedStatement statement=connection.prepareStatement("""
                    INSERT INTO EmployeePositionAssignments(CompanyId,EmployeeId,DepartmentId,PositionId,ManagerId,EffectiveFrom,ChangeType)
                    VALUES(?,?,?,?,?,CONVERT(date,SYSDATETIME()),'HIRE')
                    """)) {
                statement.setInt(1,companyId); statement.setInt(2,employeeId);
                statement.setInt(3,departmentId); statement.setInt(4,positionId);
                statement.setObject(5,currentManager,Types.INTEGER);
                statement.executeUpdate();
            }
        }
    }

    public boolean saveOrganization(int companyId, int employeeId, Integer departmentId, Integer positionId,
                                     Integer managerId, Integer actorUserId, String reason) throws SQLException {
        return OrganizationTransaction.execute(companyId, connection -> saveOrganization(connection, companyId, employeeId,
                departmentId, positionId, managerId, actorUserId, reason));
    }

    private boolean saveOrganization(Connection connection, int companyId, int employeeId, Integer departmentId,
            Integer positionId, Integer managerId, Integer actorUserId, String reason) throws SQLException {
            requireActor(connection, companyId, actorUserId);
            var dao = new EmployeeLifecycleDAO();
            var employee = dao.employee(connection, companyId, employeeId);
            if (!"Active".equals(employee.get("Status")))
                throw new FieldValidationException("employeeId", "Chỉ phân công cho nhân viên đang hoạt động.");
            validateSelection(connection, companyId, departmentId, positionId);
            if ("MANAGER".equals(new OrganizationRoleDAO().roleForPosition(connection, companyId, positionId)) && managerId != null)
                throw new FieldValidationException("managerId", "Chức danh quản lý không được gán quản lý cấp trên.");
            new OrganizationRoleDAO().synchronize(connection, companyId, employeeId, positionId, actorUserId);
            EmployeeAssignmentDAO reporting = new EmployeeAssignmentDAO();
            Integer oldDepartment;
            Integer oldPosition;
            Integer oldManager;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT DepartmentId,PositionId,ManagerId FROM Employees WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND EmployeeId=?")) {
                statement.setInt(1, companyId); statement.setInt(2, employeeId);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) throw new IllegalArgumentException("Không tìm thấy nhân viên trong công ty của bạn.");
                    oldDepartment = (Integer) rows.getObject("DepartmentId");
                    oldPosition = (Integer) rows.getObject("PositionId");
                    oldManager = (Integer) rows.getObject("ManagerId");
                }
            }
            reporting.requireManagerSnapshot(connection,companyId,employeeId,oldManager);
            boolean changed = !Objects.equals(oldDepartment, departmentId) || !Objects.equals(oldPosition, positionId);
            if (!Objects.equals(oldDepartment,departmentId) || !Objects.equals(oldPosition,positionId) || !Objects.equals(oldManager,managerId)) {
                requireOpenPayroll(connection,companyId);
                var pending = dao.query(connection,"SELECT PositionChangeId,Status FROM EmployeePositionChanges WHERE CompanyId=? AND EmployeeId=? AND Status IN ('PENDING','APPROVED')",companyId,employeeId);
                for (var proposal : pending) {
                    dao.execute(connection,"UPDATE EmployeePositionChanges SET Status='REJECTED' WHERE CompanyId=? AND PositionChangeId=?",companyId,proposal.get("PositionChangeId"));
                    OrganizationTransaction.audit(connection,companyId,actorUserId,employeeId,"SUPERSEDE_POSITION_PROPOSAL",
                            proposal.get("Status").toString(),"Được thay thế bằng phân công trực tiếp của nhân sự.");
                }
            }
            if (changed) {
                reporting.releaseDirectReportsIfNeeded(connection, companyId, employeeId, departmentId, positionId, actorUserId, reason);
            }
            reporting.validateManagerAssignment(connection, companyId, employeeId, managerId, departmentId, positionId);
            if (changed) {
                validateSelection(connection, companyId, departmentId, positionId);
                requireActor(connection, companyId, actorUserId);
                if (oldPosition == null) {
                    reporting.updateManager(connection, companyId, employeeId, null);
                    assignOrganization(connection, companyId, employeeId, departmentId, positionId);
                    if (positionId!=null) recordInitialAssignment(connection,companyId,employeeId,oldDepartment,departmentId,positionId,actorUserId,reason);
                } else {
                    if (positionId == null) throw new FieldValidationException("positionId", "Chọn chức danh thay thế để giữ nguyên lịch sử phân công theo ngày hiệu lực.");
                    applyPositionChange(connection, companyId, employeeId, oldDepartment, oldPosition,
                            departmentId, positionId, managerId, actorUserId, reason);
                }
            }
            reporting.updateManager(connection, companyId, employeeId, managerId);
            if (changed) {
                var values=new EmployeeHistoryValues(connection,companyId).format(
                        "DepartmentId="+oldDepartment+";PositionId="+oldPosition,
                        "DepartmentId="+departmentId+";PositionId="+positionId);
                String safeReason = reason == null ? "" : reason.replace('\r',' ').replace('\n',' ');
                OrganizationTransaction.audit(connection,companyId,actorUserId,employeeId,"CHANGE_ORGANIZATION",
                        values.oldValue(),values.newValue() + (safeReason.isBlank() ? "" : "\nLý do: " + safeReason));
            }
            OrganizationTransaction.recordManagerChange(connection,companyId,actorUserId,employeeId,oldManager,managerId,reason);
            new OrganizationRoleDAO().synchronize(connection, companyId, employeeId, positionId, actorUserId);
            return true;
    }

    private void requireOpenPayroll(Connection connection, int companyId) throws SQLException {
        if (!new EmployeeLifecycleDAO().query(connection, """
                SELECT 1 FROM PayrollPeriods WHERE CompanyId=? AND Status='Approved'
                    AND ([Year]>YEAR(SYSDATETIME()) OR ([Year]=YEAR(SYSDATETIME()) AND [Month]>=MONTH(SYSDATETIME())))
                """, companyId).isEmpty())
            throw new FieldValidationException("positionId", "Kỳ lương đã duyệt không cho phép thay đổi phân công tại thời điểm này.");
    }

    private void requireActor(Connection connection, int companyId, Integer actorUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM Users u JOIN Roles r ON r.RoleId=u.RoleId
                WHERE u.CompanyId=? AND u.UserId=? AND u.IsActive=1 AND u.IsLocked=0
                    AND r.RoleCode IN ('COMPANY_ADMIN','HR')
                """)) {
            statement.setInt(1, companyId); statement.setObject(2, actorUserId, Types.INTEGER);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new SecurityException("Cần tài khoản nhân sự hoặc quản trị viên công ty đang hoạt động để thay đổi chức danh.");
            }
        }
    }

    private void recordInitialAssignment(Connection connection,int companyId,int employeeId,Integer oldDepartment,int departmentId,int positionId,
                                          int actorId,String reason) throws SQLException {
        long assignmentId;
        try (PreparedStatement statement=connection.prepareStatement("""
                SELECT AssignmentId FROM EmployeePositionAssignments
                WHERE CompanyId=? AND EmployeeId=? AND PositionId=? AND EffectiveTo IS NULL
                """)) {
            statement.setInt(1,companyId); statement.setInt(2,employeeId); statement.setInt(3,positionId);
            try (ResultSet rows=statement.executeQuery()) {
                if (!rows.next()) throw new SQLException("Không tìm thấy lịch sử phân công ban đầu.");
                assignmentId=rows.getLong(1);
            }
        }
        var values=new EmployeeHistoryValues(connection,companyId).format("DepartmentId="+oldDepartment+";PositionId=null",
                "DepartmentId="+departmentId+";PositionId="+positionId);
        com.hrms.model.AuditLog log=new com.hrms.model.AuditLog();
        log.setCompanyId(companyId); log.setUserId(actorId); log.setTableName("EmployeePositionAssignments");
        log.setRecordId(String.valueOf(assignmentId)); log.setAction("ASSIGN_POSITION");
        log.setOldValues(values.oldValue());
        String safeReason = reason == null ? "" : reason.replace('\r',' ').replace('\n',' ');
        log.setNewValues(values.newValue() + (safeReason.isBlank() ? "" : "\nLý do: " + safeReason));
        new AuditLogDAO().insert(connection,log);
    }

    private void applyPositionChange(Connection connection, int companyId, int employeeId,
                                      Integer oldDepartment, int oldPosition, int departmentId, int positionId,
                                      Integer managerId, int actorUserId, String reason) throws SQLException {
        Date today;
        try (PreparedStatement statement = connection.prepareStatement("SELECT CONVERT(date,SYSDATETIME())");
             ResultSet rows = statement.executeQuery()) { rows.next(); today = rows.getDate(1); }
        new PositionOccupancyDAO().validate(connection,companyId,employeeId,positionId,today.toLocalDate(),null);
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM EmployeePositionChanges WHERE CompanyId=? AND EmployeeId=? AND Status IN ('PENDING','APPROVED')
                """)) {
            statement.setInt(1,companyId); statement.setInt(2,employeeId);
            try (ResultSet rows=statement.executeQuery()) {
                if (rows.next()) throw new FieldValidationException("positionId", "Vui lòng xử lý đề xuất chức danh đang chờ hoặc đã duyệt trước.");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM PayrollPeriods WHERE CompanyId=? AND Status='Approved'
                    AND ([Year]>YEAR(?) OR ([Year]=YEAR(?) AND [Month]>=MONTH(?)))
                """)) {
            statement.setInt(1,companyId); statement.setDate(2,today); statement.setDate(3,today); statement.setDate(4,today);
            try (ResultSet rows=statement.executeQuery()) {
                if (rows.next()) throw new FieldValidationException("positionId", "Kỳ lương đã duyệt không cho phép thay đổi tại ngày hiệu lực này.");
            }
        }
        Long assignmentId = null;
        Date effectiveFrom = null;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT AssignmentId,PositionId,DepartmentId,EffectiveFrom FROM EmployeePositionAssignments
                WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND EmployeeId=? AND EffectiveTo IS NULL
                """)) {
            statement.setInt(1,companyId); statement.setInt(2,employeeId);
            try (ResultSet rows=statement.executeQuery()) {
                if (rows.next()) {
                    assignmentId=rows.getLong("AssignmentId"); effectiveFrom=rows.getDate("EffectiveFrom");
                    if (rows.getInt("PositionId")!=oldPosition || !Objects.equals(rows.getObject("DepartmentId"),oldDepartment)
                            || effectiveFrom.after(today)) throw new FieldValidationException("positionId", "Lịch sử chức danh hiện tại chưa nhất quán. Vui lòng kiểm tra lịch sử nhân viên.");
                }
            }
        }
        boolean correction = effectiveFrom == null || effectiveFrom.equals(today);
        String changeType = "CORRECTION";
        if (!correction) {
            if (!Objects.equals(oldDepartment,departmentId)) changeType="TRANSFER";
            else {
                try (PreparedStatement statement=connection.prepareStatement("""
                        SELECT oldLevel.RankOrder AS OldRank,newLevel.RankOrder AS NewRank
                        FROM Positions oldPosition
                        JOIN ResponsibilityLevels oldLevel ON oldLevel.CompanyId=oldPosition.CompanyId AND oldLevel.ResponsibilityLevelId=oldPosition.ResponsibilityLevelId
                        JOIN Positions newPosition ON newPosition.CompanyId=oldPosition.CompanyId AND newPosition.PositionId=?
                        JOIN ResponsibilityLevels newLevel ON newLevel.CompanyId=newPosition.CompanyId AND newLevel.ResponsibilityLevelId=newPosition.ResponsibilityLevelId
                        WHERE oldPosition.CompanyId=? AND oldPosition.PositionId=?
                        """)) {
                    statement.setInt(1,positionId); statement.setInt(2,companyId); statement.setInt(3,oldPosition);
                    try (ResultSet rows=statement.executeQuery()) {
                        if (rows.next()) {
                            if (rows.getInt("NewRank")>rows.getInt("OldRank")) changeType="PROMOTION";
                            else if (rows.getInt("NewRank")<rows.getInt("OldRank")) changeType="DEMOTION";
                        }
                    }
                }
            }
        }
        long changeId;
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO EmployeePositionChanges(CompanyId,EmployeeId,OldPositionId,NewPositionId,ChangeType,
                    Status,EffectiveDate,NewManagerId,Reason,CreatedByUserId,ApprovedByUserId,ApprovedAt)
                VALUES(?,?,?,?,?,'APPROVED',?,?,?,?,?,SYSUTCDATETIME())
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1,companyId); statement.setInt(2,employeeId); statement.setInt(3,oldPosition);
            statement.setInt(4,positionId); statement.setString(5,changeType); statement.setDate(6,today);
            statement.setObject(7,managerId,Types.INTEGER); statement.setString(8,reason);
            statement.setInt(9,actorUserId); statement.setInt(10,actorUserId);
            if (statement.executeUpdate()!=1) throw new SQLException("Không ghi nhận được thay đổi chức danh.");
            try (ResultSet keys=statement.getGeneratedKeys()) { keys.next(); changeId=keys.getLong(1); }
        }
        if (!correction) {
            try (CallableStatement statement=connection.prepareCall("{call dbo.usp_ApplyApprovedPositionChange(?,?)}")) {
                statement.setLong(1,changeId); statement.setDate(2,today); statement.execute();
            }
            return;
        }
        if (assignmentId == null) {
            execute(connection, """
                    INSERT INTO EmployeePositionAssignments(CompanyId,EmployeeId,DepartmentId,PositionId,EffectiveFrom,ChangeType,Reason,SourceChangeId)
                    VALUES(?,?,?,?,?,'CORRECTION',?,?)
                    """, companyId,employeeId,departmentId,positionId,today,reason,changeId);
        } else {
            execute(connection, """
                    UPDATE EmployeePositionAssignments SET DepartmentId=?,PositionId=?,ChangeType='CORRECTION',Reason=?,SourceChangeId=?
                    WHERE CompanyId=? AND EmployeeId=? AND AssignmentId=? AND EffectiveTo IS NULL
                    """, departmentId,positionId,reason,changeId,companyId,employeeId,assignmentId);
        }
        execute(connection,"UPDATE Employees SET DepartmentId=?,PositionId=?,UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND EmployeeId=?",
                departmentId,positionId,companyId,employeeId);
        execute(connection,"UPDATE EmployeePositionChanges SET Status='APPLIED',AppliedAt=SYSUTCDATETIME() WHERE CompanyId=? AND PositionChangeId=?",
                companyId,changeId);
    }

    private void execute(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement=connection.prepareStatement(sql)) {
            for (int index=0; index<parameters.length; index++) statement.setObject(index+1,parameters[index]);
            if (statement.executeUpdate()!=1) throw new SQLException("Lịch sử tổ chức đã thay đổi. Vui lòng tải lại trang.");
        }
    }

    public void validateSelection(Connection connection,int companyId,Integer departmentId,Integer positionId) throws SQLException {
        if(departmentId==null) {
            if(positionId!=null) throw new FieldValidationException("departmentId","Vui lòng chọn phòng ban trước.");
            return;
        }
        try(PreparedStatement statement=connection.prepareStatement("SELECT 1 FROM Departments WITH (HOLDLOCK) WHERE CompanyId=? AND DepartmentId=? AND IsActive=1")) {
            statement.setInt(1,companyId); statement.setInt(2,departmentId);
            try(ResultSet rows=statement.executeQuery()) {
                if(!rows.next()) throw new FieldValidationException("departmentId","Không tìm thấy phòng ban trong công ty của bạn.");
            }
        }
        if(positionId==null) return;
        try(PreparedStatement statement=connection.prepareStatement("""
                SELECT 1 FROM Positions p WITH (UPDLOCK,HOLDLOCK)
                JOIN ResponsibilityLevels r ON r.CompanyId=p.CompanyId AND r.ResponsibilityLevelId=p.ResponsibilityLevelId
                WHERE p.CompanyId=? AND p.DepartmentId=? AND p.PositionId=? AND p.IsActive=1 AND r.IsActive=1
                """)) {
            statement.setInt(1,companyId); statement.setInt(2,departmentId); statement.setInt(3,positionId);
            try(ResultSet rows=statement.executeQuery()) {
                if(!rows.next()) throw new FieldValidationException("positionId","Vui lòng chọn chức danh đang hoạt động thuộc đúng phòng ban và công ty.");
            }
        }
    }
}
