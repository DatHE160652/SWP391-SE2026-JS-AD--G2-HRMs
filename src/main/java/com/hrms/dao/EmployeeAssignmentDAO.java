package com.hrms.dao;

import com.hrms.model.EmployeeAssignment;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EmployeeAssignmentDAO {
    public boolean assignDepartment(int employeeId, int departmentId) throws SQLException {
        String sql = "UPDATE Employees SET DepartmentId = ?, UpdatedAt = SYSUTCDATETIME() WHERE EmployeeId = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, departmentId);
            statement.setInt(2, employeeId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean assignPosition(int employeeId, int positionId) throws SQLException {
        String sql = "UPDATE Employees SET PositionId = ?, UpdatedAt = SYSUTCDATETIME() WHERE EmployeeId = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, positionId);
            statement.setInt(2, employeeId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean clearPosition(int employeeId) throws SQLException {
        String sql = "UPDATE Employees SET PositionId = NULL, UpdatedAt = SYSUTCDATETIME() WHERE EmployeeId = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean assignManager(int employeeId, int managerId) throws SQLException {
        String sql = "UPDATE Employees SET ManagerId = ?, UpdatedAt = SYSUTCDATETIME() WHERE EmployeeId = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, managerId);
            statement.setInt(2, employeeId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean assignDepartmentMembers(int departmentId, int managerId, int[] employeeIds) throws SQLException {
        String managerSql = "UPDATE Employees SET DepartmentId = ?, UpdatedAt = SYSUTCDATETIME() WHERE EmployeeId = ?";
        String memberSql = """
                UPDATE Employees
                SET DepartmentId = ?,
                    ManagerId = ?,
                    UpdatedAt = SYSUTCDATETIME()
                WHERE EmployeeId = ?
                """;

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int affected = 0;
                try (PreparedStatement statement = connection.prepareStatement(managerSql)) {
                    statement.setInt(1, departmentId);
                    statement.setInt(2, managerId);
                    affected += statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(memberSql)) {
                    for (int employeeId : employeeIds) {
                        statement.setInt(1, departmentId);
                        statement.setInt(2, managerId);
                        statement.setInt(3, employeeId);
                        statement.addBatch();
                    }
                    for (int count : statement.executeBatch()) {
                        if (count == Statement.SUCCESS_NO_INFO || count > 0) {
                            affected++;
                        }
                    }
                }
                connection.commit();
                return affected > 0;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<DepartmentMemberOption> getDepartmentMemberOptions(int departmentId) throws SQLException {
        String sql = """
                SELECT e.EmployeeId, e.EmployeeCode, e.FullName, e.DepartmentId, e.ManagerId, e.PositionId,
                       p.PositionTitle, r.RoleCode
                FROM Employees e
                LEFT JOIN Positions p ON p.PositionId = e.PositionId
                LEFT JOIN Users u ON u.UserId = e.UserId
                LEFT JOIN Roles r ON r.RoleId = u.RoleId
                ORDER BY e.FullName
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<DepartmentMemberOption> employees = new ArrayList<>();
            while (resultSet.next()) {
                Integer currentDepartmentId = getNullableInt(resultSet, "DepartmentId");
                employees.add(new DepartmentMemberOption(
                        resultSet.getInt("EmployeeId"),
                        resultSet.getString("EmployeeCode"),
                        resultSet.getString("FullName"),
                        currentDepartmentId,
                        getNullableInt(resultSet, "ManagerId"),
                        getNullableInt(resultSet, "PositionId"),
                        resultSet.getString("PositionTitle"),
                        resultSet.getString("RoleCode"),
                        currentDepartmentId != null && currentDepartmentId == departmentId
                ));
            }
            return employees;
        }
    }

    public EmployeeAssignment getEmployeeOrganization(int employeeId) throws SQLException {
        String sql = """
                SELECT EmployeeId, DepartmentId, PositionId, ManagerId
                FROM Employees
                WHERE EmployeeId = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new EmployeeAssignment(
                        resultSet.getInt("EmployeeId"),
                        getNullableInt(resultSet, "DepartmentId"),
                        getNullableInt(resultSet, "PositionId"),
                        getNullableInt(resultSet, "ManagerId")
                );
            }
        }
    }

    public boolean employeeExists(int employeeId) throws SQLException {
        return recordExists("SELECT COUNT(*) FROM Employees WHERE EmployeeId = ?", employeeId);
    }

    public boolean departmentExists(int departmentId) throws SQLException {
        return recordExists("SELECT COUNT(*) FROM Departments WHERE DepartmentId = ?", departmentId);
    }

    public boolean positionExists(int positionId) throws SQLException {
        return recordExists("SELECT COUNT(*) FROM Positions WHERE PositionId = ?", positionId);
    }

    public boolean isManagerInDepartment(int managerId, int departmentId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM Employees e
                INNER JOIN Users u ON u.UserId = e.UserId
                INNER JOIN Roles r ON r.RoleId = u.RoleId
                WHERE e.EmployeeId = ?
                  AND e.DepartmentId = ?
                  AND r.RoleCode = 'MANAGER'
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, managerId);
            statement.setInt(2, departmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean hasDifferentDepartment(int employeeId, int departmentId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM Employees
                WHERE EmployeeId = ?
                  AND DepartmentId IS NOT NULL
                  AND DepartmentId <> ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            statement.setInt(2, departmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private boolean recordExists(String sql, int id) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private Integer getNullableInt(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    public record DepartmentMemberOption(int employeeId, String employeeCode, String fullName,
                                         Integer departmentId, Integer managerId, Integer positionId,
                                         String positionTitle, String roleCode, boolean selected) {
    }
}
