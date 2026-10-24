package com.hrms.dao;

import com.hrms.model.AuditLog;
import com.hrms.model.ProfileUpdatePeriod;
import com.hrms.service.FieldValidationException;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProfileUpdateDAO {
    private final String periodType;

    public ProfileUpdateDAO() { this("PROFILE"); }

    public ProfileUpdateDAO(String periodType) {
        if (!java.util.Set.of("PROFILE","DOCUMENT").contains(periodType)) throw new IllegalArgumentException("Loại đợt cập nhật không hợp lệ.");
        this.periodType=periodType;
    }
    public List<ProfileUpdatePeriod> getPeriods(int companyId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT TOP 100 * FROM ProfileUpdatePeriods WHERE CompanyId = ? AND PeriodType=? ORDER BY PeriodId DESC")) {
            statement.setInt(1, companyId);
            statement.setString(2, periodType);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ProfileUpdatePeriod> periods = new ArrayList<>();
                while (resultSet.next()) {
                    periods.add(mapPeriod(resultSet));
                }
                return periods;
            }
        }
    }

    public ProfileUpdatePeriod getAvailablePeriod(int companyId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            return findAvailablePeriod(connection, companyId, false);
        }
    }

    public ProfileUpdatePeriod requireAvailablePeriod(Connection connection, int companyId) throws SQLException {
        OrganizationTransaction.lockCompany(connection, companyId);
        ProfileUpdatePeriod period = findAvailablePeriod(connection, companyId, true);
        if (period == null) throw new SecurityException("Không có đợt cập nhật tài liệu đang mở. Bạn chưa được thay đổi tài liệu.");
        return period;
    }

    public long create(int companyId, int userId, String title, LocalDateTime startsAt, LocalDateTime endsAt,
                       String ipAddress) throws SQLException {
        return transaction(connection -> create(connection, companyId, userId, title, startsAt, endsAt, ipAddress));
    }

    long create(Connection connection, int companyId, int userId, String title, LocalDateTime startsAt,
                LocalDateTime endsAt, String ipAddress) throws SQLException {
        requireAdministrator(connection, companyId, userId);
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT CompanyId FROM Companies WITH (UPDLOCK, HOLDLOCK) WHERE CompanyId = ?")) {
            statement.setInt(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SecurityException("Công ty không tồn tại.");
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT PeriodId FROM ProfileUpdatePeriods WITH (UPDLOCK, HOLDLOCK)
                WHERE CompanyId = ? AND ClosedAt IS NULL AND StartsAt < ? AND EndsAt > ? AND PeriodType=?
                """)) {
            statement.setInt(1, companyId);
            statement.setTimestamp(2, Timestamp.valueOf(endsAt));
            statement.setTimestamp(3, Timestamp.valueOf(startsAt));
            statement.setString(4, periodType);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new FieldValidationException("startsAt", "Thời gian này trùng với một đợt cập nhật chưa đóng.");
                }
            }
        }
        long periodId;
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO ProfileUpdatePeriods (CompanyId, Title, StartsAt, EndsAt, CreatedByUserId, PeriodType)
                VALUES (?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, companyId);
            statement.setString(2, title);
            statement.setTimestamp(3, Timestamp.valueOf(startsAt));
            statement.setTimestamp(4, Timestamp.valueOf(endsAt));
            statement.setInt(5, userId);
            statement.setString(6, periodType);
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("Không thể tạo đợt cập nhật hồ sơ.");
                }
                periodId = resultSet.getLong(1);
            }
        }
        audit(connection, companyId, userId, "OPEN_"+periodType+"_UPDATE_PERIOD", "ProfileUpdatePeriods", periodId,
                null, title + "; " + startsAt + " - " + endsAt, ipAddress);
        return periodId;
    }

    public void close(int companyId, int userId, long periodId, String ipAddress) throws SQLException {
        transaction(connection -> {
            close(connection, companyId, userId, periodId, ipAddress);
            return null;
        });
    }

    void close(Connection connection, int companyId, int userId, long periodId, String ipAddress) throws SQLException {
        requireAdministrator(connection, companyId, userId);
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE ProfileUpdatePeriods SET ClosedAt = SYSUTCDATETIME()
                WHERE PeriodId = ? AND CompanyId = ? AND ClosedAt IS NULL AND PeriodType=?
                """)) {
            statement.setLong(1, periodId);
            statement.setInt(2, companyId);
            statement.setString(3, periodType);
            if (statement.executeUpdate() == 0) {
                throw new SecurityException("Đợt cập nhật không tồn tại trong công ty hoặc đã đóng.");
            }
        }
        audit(connection, companyId, userId, "CLOSE_"+periodType+"_UPDATE_PERIOD", "ProfileUpdatePeriods", periodId,
                "Open", "Closed", ipAddress);
    }

    public void updatePhone(int companyId, int userId, int employeeId, String phone, String ipAddress) throws SQLException {
        transaction(connection -> {
            updatePhone(connection, companyId, userId, employeeId, phone, ipAddress);
            return null;
        });
    }

    void updatePhone(Connection connection, int companyId, int userId, int employeeId, String phone,
                     String ipAddress) throws SQLException {
        updateProfile(connection,companyId,userId,employeeId,java.util.Map.of("phone",phone),ipAddress);
    }

    public void updateProfile(int companyId,int userId,int employeeId,java.util.Map<String,String> changes,String ipAddress) throws SQLException {
        updateProfile(companyId,userId,employeeId,changes,null,ipAddress);
    }

    public void updateProfile(int companyId,int userId,int employeeId,java.util.Map<String,String> changes,String expectedUpdatedAt,String ipAddress) throws SQLException {
        transaction(connection -> {
            updateProfile(connection,companyId,userId,employeeId,changes,expectedUpdatedAt,ipAddress);
            return null;
        });
    }

    public void updateProfile(Connection connection,int companyId,int userId,int employeeId,java.util.Map<String,String> changes,String ipAddress) throws SQLException {
        updateProfile(connection,companyId,userId,employeeId,changes,null,ipAddress);
    }

    public void updateProfile(Connection connection,int companyId,int userId,int employeeId,java.util.Map<String,String> changes,String expectedUpdatedAt,String ipAddress) throws SQLException {
        if (!"PROFILE".equals(periodType)) throw new SecurityException("Đợt tài liệu không được dùng để cập nhật thông tin hồ sơ.");
        java.util.Map<String,String> columns=java.util.Map.of("email","Email","phone","Phone","identityIssuedDate","IdentityIssuedDate",
                "identityIssuedPlace","IdentityIssuedPlace","permanentAddress","PermanentAddress","hometown","Hometown");
        java.util.Map<String,String> labels=java.util.Map.of("email","Email liên hệ","phone","Điện thoại","identityIssuedDate","Ngày cấp căn cước",
                "identityIssuedPlace","Nơi cấp căn cước","permanentAddress","Địa chỉ thường trú","hometown","Quê quán");
        if (changes==null || changes.isEmpty() || !columns.keySet().containsAll(changes.keySet()))
            throw new FieldValidationException("phone","Các trường cập nhật hồ sơ không hợp lệ.");
        OrganizationTransaction.lockCompany(connection,companyId);
        ProfileUpdatePeriod period=findAvailablePeriod(connection,companyId,true);
        if (period==null) throw new SecurityException("Hiện không có đợt cập nhật hồ sơ đang mở. Bạn chưa thể sửa thông tin cá nhân.");
        java.util.Map<String,String> before=new java.util.LinkedHashMap<>();
        String sql="""
                SELECT e.Email,e.Phone,e.IdentityIssuedDate,e.IdentityIssuedPlace,e.PermanentAddress,e.Hometown,e.DateOfBirth,
                    CONVERT(varchar(33),COALESCE(e.UpdatedAt,e.CreatedAt),126) AS ProfileVersion
                FROM Employees e WITH (UPDLOCK,HOLDLOCK)
                JOIN Users u ON u.UserId=e.UserId AND u.CompanyId=e.CompanyId JOIN Roles r ON r.RoleId=u.RoleId
                WHERE e.CompanyId=? AND e.EmployeeId=? AND e.UserId=? AND e.Status='Active'
                    AND u.IsActive=1 AND u.IsLocked=0 AND r.RoleCode IN ('EMPLOYEE','MANAGER')
                """+" AND "+ContractAccessPolicy.ALLOWED;
        try (PreparedStatement statement=connection.prepareStatement(sql)) {
            statement.setInt(1,companyId); statement.setInt(2,employeeId); statement.setInt(3,userId);
            try (ResultSet rows=statement.executeQuery()) {
                if (!rows.next()) throw new SecurityException("Bạn không có quyền cập nhật hồ sơ này.");
                if (expectedUpdatedAt!=null && !expectedUpdatedAt.equals(rows.getString("ProfileVersion")))
                    throw new FieldValidationException("phone","Hồ sơ đã được thay đổi. Vui lòng tải lại để tránh ghi đè thông tin mới.");
                for (String field:changes.keySet()) before.put(field,rows.getString(columns.get(field)));
                if (changes.get("identityIssuedDate")!=null && rows.getDate("DateOfBirth")!=null
                        && java.sql.Date.valueOf(changes.get("identityIssuedDate")).before(rows.getDate("DateOfBirth")))
                    throw new FieldValidationException("identityIssuedDate","Ngày cấp căn cước không được trước ngày sinh.");
            }
        }
        for (String field:java.util.List.of("phone","email")) {
            if (!changes.containsKey(field) || changes.get(field)==null) continue;
            try (PreparedStatement statement=connection.prepareStatement("SELECT EmployeeId FROM Employees WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND "+columns.get(field)+"=? AND EmployeeId<>?")) {
                statement.setInt(1,companyId); statement.setString(2,changes.get(field)); statement.setInt(3,employeeId);
                try (ResultSet rows=statement.executeQuery()) {
                    if (rows.next()) throw new FieldValidationException(field,labels.get(field)+" đã tồn tại trong công ty.");
                }
            }
        }
        java.util.List<String> changed=changes.keySet().stream().filter(field->!java.util.Objects.equals(before.get(field),changes.get(field))).toList();
        if (changed.isEmpty()) return;
        String assignments=changed.stream().map(field->columns.get(field)+"=?").collect(java.util.stream.Collectors.joining(","));
        try (PreparedStatement statement=connection.prepareStatement("UPDATE Employees SET "+assignments+",UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND EmployeeId=? AND UserId=? AND EXISTS(SELECT 1 FROM ProfileUpdatePeriods WHERE PeriodId=? AND CompanyId=? AND ClosedAt IS NULL AND StartsAt<=SYSUTCDATETIME() AND EndsAt>SYSUTCDATETIME())")) {
            int parameter=1;
            for (String field:changed) statement.setObject(parameter++,"identityIssuedDate".equals(field) && changes.get(field)!=null?java.sql.Date.valueOf(changes.get(field)):changes.get(field));
            statement.setInt(parameter++,companyId); statement.setInt(parameter++,employeeId); statement.setInt(parameter++,userId);
            statement.setLong(parameter++,period.periodId()); statement.setInt(parameter,companyId);
            if (statement.executeUpdate()!=1) throw new SecurityException("Đợt cập nhật đã kết thúc hoặc hồ sơ không còn khả dụng.");
        }
        String oldValues=changed.stream().map(field->labels.get(field)+": "+(before.get(field)==null?"Chưa cập nhật":before.get(field))).collect(java.util.stream.Collectors.joining("\n"));
        String newValues=changed.stream().map(field->labels.get(field)+": "+(changes.get(field)==null?"Chưa cập nhật":changes.get(field))).collect(java.util.stream.Collectors.joining("\n"));
        audit(connection,companyId,userId,"UPDATE_SELF_PROFILE","Employees",employeeId,oldValues,newValues+"\nĐợt cập nhật: "+period.title(),ipAddress);
    }

    private ProfileUpdatePeriod findAvailablePeriod(Connection connection, int companyId, boolean lock) throws SQLException {
        String sql = "SELECT TOP 1 * FROM ProfileUpdatePeriods " + (lock ? "WITH (UPDLOCK, HOLDLOCK) " : "")
                + "WHERE CompanyId = ? AND ClosedAt IS NULL AND StartsAt <= SYSUTCDATETIME() "
                + "AND EndsAt > SYSUTCDATETIME() AND PeriodType=? ORDER BY StartsAt DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            statement.setString(2, periodType);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapPeriod(resultSet) : null;
            }
        }
    }

    private void requireAdministrator(Connection connection, int companyId, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT u.UserId FROM Users u JOIN Roles r ON r.RoleId = u.RoleId
                WHERE u.UserId = ? AND u.CompanyId = ? AND u.IsActive = 1 AND u.IsLocked = 0
                  AND r.RoleCode IN ('COMPANY_ADMIN', 'HR')
                """)) {
            statement.setInt(1, userId);
            statement.setInt(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SecurityException("Bạn không có quyền quản lý đợt cập nhật của công ty này.");
                }
            }
        }
    }

    private ProfileUpdatePeriod mapPeriod(ResultSet resultSet) throws SQLException {
        Timestamp closedAt = resultSet.getTimestamp("ClosedAt");
        return new ProfileUpdatePeriod(resultSet.getLong("PeriodId"), resultSet.getInt("CompanyId"),
                resultSet.getString("Title"), resultSet.getTimestamp("StartsAt").toLocalDateTime(),
                resultSet.getTimestamp("EndsAt").toLocalDateTime(), closedAt == null ? null : closedAt.toLocalDateTime(),resultSet.getString("PeriodType"));
    }

    private void audit(Connection connection, int companyId, int userId, String action, String table, long recordId,
                       String oldValue, String newValue, String ipAddress) throws SQLException {
        AuditLog log = new AuditLog();
        log.setCompanyId(companyId);
        log.setUserId(userId);
        log.setAction(action);
        log.setTableName(table);
        log.setRecordId(String.valueOf(recordId));
        log.setOldValues(oldValue);
        log.setNewValues(newValue);
        log.setIpAddress(ipAddress);
        new AuditLogDAO().insert(connection, log);
    }

    private <Result> Result transaction(SqlWork<Result> work) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Result result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private interface SqlWork<Result> {
        Result execute(Connection connection) throws SQLException;
    }
}
