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
    public List<ProfileUpdatePeriod> getPeriods(int companyId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT TOP 100 * FROM ProfileUpdatePeriods WHERE CompanyId = ? ORDER BY PeriodId DESC")) {
            statement.setInt(1, companyId);
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
                WHERE CompanyId = ? AND ClosedAt IS NULL AND StartsAt < ? AND EndsAt > ?
                """)) {
            statement.setInt(1, companyId);
            statement.setTimestamp(2, Timestamp.valueOf(endsAt));
            statement.setTimestamp(3, Timestamp.valueOf(startsAt));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new FieldValidationException("startsAt", "Thời gian này trùng với một đợt cập nhật chưa đóng.");
                }
            }
        }
        long periodId;
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO ProfileUpdatePeriods (CompanyId, Title, StartsAt, EndsAt, CreatedByUserId)
                VALUES (?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, companyId);
            statement.setString(2, title);
            statement.setTimestamp(3, Timestamp.valueOf(startsAt));
            statement.setTimestamp(4, Timestamp.valueOf(endsAt));
            statement.setInt(5, userId);
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("Không thể tạo đợt cập nhật hồ sơ.");
                }
                periodId = resultSet.getLong(1);
            }
        }
        audit(connection, companyId, userId, "OPEN_PROFILE_UPDATE_PERIOD", "ProfileUpdatePeriods", periodId,
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
                WHERE PeriodId = ? AND CompanyId = ? AND ClosedAt IS NULL
                """)) {
            statement.setLong(1, periodId);
            statement.setInt(2, companyId);
            if (statement.executeUpdate() == 0) {
                throw new SecurityException("Đợt cập nhật không tồn tại trong công ty hoặc đã đóng.");
            }
        }
        audit(connection, companyId, userId, "CLOSE_PROFILE_UPDATE_PERIOD", "ProfileUpdatePeriods", periodId,
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
        ProfileUpdatePeriod period = findAvailablePeriod(connection, companyId, true);
        if (period == null) {
            throw new SecurityException("Hiện không có đợt cập nhật hồ sơ đang mở. Bạn chưa thể sửa số điện thoại.");
        }
        String oldPhone;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT e.Phone FROM Employees e WITH (UPDLOCK, HOLDLOCK)
                JOIN Users u ON u.UserId = e.UserId
                JOIN Roles r ON r.RoleId = u.RoleId
                WHERE e.EmployeeId = ? AND e.CompanyId = ? AND e.UserId = ?
                  AND u.CompanyId = e.CompanyId AND u.IsActive = 1 AND u.IsLocked = 0
                  AND r.RoleCode IN ('EMPLOYEE', 'MANAGER')
                """)) {
            statement.setInt(1, employeeId);
            statement.setInt(2, companyId);
            statement.setInt(3, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SecurityException("Bạn không có quyền cập nhật hồ sơ này.");
                }
                oldPhone = resultSet.getString("Phone");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT EmployeeId FROM Employees WITH (UPDLOCK, HOLDLOCK)
                WHERE CompanyId = ? AND Phone = ? AND EmployeeId <> ?
                """)) {
            statement.setInt(1, companyId);
            statement.setString(2, phone);
            statement.setInt(3, employeeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new FieldValidationException("phone", "Số điện thoại đã tồn tại trong công ty.");
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE Employees SET Phone = ?, UpdatedAt = SYSUTCDATETIME()
                WHERE EmployeeId = ? AND CompanyId = ? AND UserId = ?
                """)) {
            statement.setString(1, phone);
            statement.setInt(2, employeeId);
            statement.setInt(3, companyId);
            statement.setInt(4, userId);
            if (statement.executeUpdate() != 1) {
                throw new SecurityException("Không thể cập nhật hồ sơ này.");
            }
        }
        audit(connection, companyId, userId, "UPDATE_PHONE", "Employees", employeeId,
                "Phone: " + oldPhone, "Phone: " + phone + "; PeriodId: " + period.periodId(), ipAddress);
    }

    private ProfileUpdatePeriod findAvailablePeriod(Connection connection, int companyId, boolean lock) throws SQLException {
        String sql = "SELECT TOP 1 * FROM ProfileUpdatePeriods " + (lock ? "WITH (UPDLOCK, HOLDLOCK) " : "")
                + "WHERE CompanyId = ? AND ClosedAt IS NULL AND StartsAt <= SYSUTCDATETIME() "
                + "AND EndsAt > SYSUTCDATETIME() ORDER BY StartsAt DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
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
                resultSet.getTimestamp("EndsAt").toLocalDateTime(), closedAt == null ? null : closedAt.toLocalDateTime());
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
