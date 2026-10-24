package com.hrms.dao;

import com.hrms.model.Position;
import com.hrms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PositionDAO {
    private static final String SELECT = """
            SELECT p.*, d.DepartmentName, r.LevelName, r.LevelCode,
                CAST(CASE WHEN EXISTS(SELECT 1 FROM EmployeePositionAssignments occupied
                    WHERE occupied.CompanyId=p.CompanyId AND occupied.PositionId=p.PositionId
                    AND (occupied.EffectiveTo IS NULL OR occupied.EffectiveTo>CONVERT(date,SYSDATETIME()))
                    AND (occupied.EffectiveTo IS NULL OR occupied.EffectiveTo>occupied.EffectiveFrom))
                    THEN 1 ELSE 0 END AS bit) AS AssignmentOccupied,
                (SELECT CASE WHEN COUNT(*)=1 THEN MAX(a.Amount) END
                    FROM ResponsibilityAllowancePolicies a WHERE a.CompanyId=p.CompanyId
                    AND a.ResponsibilityLevelId=p.ResponsibilityLevelId AND a.EffectiveFrom<=CONVERT(date,SYSDATETIME())
                    AND (a.EffectiveTo IS NULL OR a.EffectiveTo>CONVERT(date,SYSDATETIME()))) AS EffectiveAllowance
            FROM Positions p
            JOIN Departments d ON d.CompanyId=p.CompanyId AND d.DepartmentId=p.DepartmentId
            JOIN ResponsibilityLevels r ON r.CompanyId=p.CompanyId AND r.ResponsibilityLevelId=p.ResponsibilityLevelId
            """;
    private static final String UNUSED = """
            NOT EXISTS (SELECT 1 FROM Employees e WHERE e.CompanyId=p.CompanyId AND e.PositionId=p.PositionId)
            AND NOT EXISTS (SELECT 1 FROM EmployeePositionAssignments a WHERE a.CompanyId=p.CompanyId AND a.PositionId=p.PositionId)
            AND NOT EXISTS (SELECT 1 FROM EmployeePositionChanges c WHERE c.CompanyId=p.CompanyId
                AND (c.OldPositionId=p.PositionId OR c.NewPositionId=p.PositionId))
            """;

    public List<Position> getAllPositions(int companyId) throws SQLException {
        return getPositions(companyId, null, false);
    }

    public List<Position> getPositions(int companyId, Integer departmentId, boolean activeOnly) throws SQLException {
        String sql = SELECT + " WHERE p.CompanyId=? AND (? IS NULL OR p.DepartmentId=?)"
                + (activeOnly ? " AND d.IsActive=1 AND p.IsActive=1 AND r.IsActive=1" : "")
                + " ORDER BY p.PositionId DESC";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            statement.setObject(2, departmentId, Types.INTEGER);
            statement.setObject(3, departmentId, Types.INTEGER);
            try (ResultSet rows = statement.executeQuery()) {
                List<Position> positions = new ArrayList<>();
                while (rows.next()) positions.add(mapPosition(rows));
                return positions;
            }
        }
    }

    public Position getPositionById(int companyId, int id) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT + " WHERE p.CompanyId=? AND p.PositionId=?")) {
            statement.setInt(1, companyId);
            statement.setInt(2, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? mapPosition(rows) : null;
            }
        }
    }

    public boolean insertPosition(Position position) throws SQLException {
        String sql = """
                INSERT INTO Positions (CompanyId,DepartmentId,ResponsibilityLevelId,
                    PositionTitle,Description,IsActive,CanManageEmployees,PositionAllowance,ReportsToPositionId,CreatedAt)
                SELECT ?,?,?,?,?,?,?,?,?,SYSUTCDATETIME()
                WHERE EXISTS (SELECT 1 FROM Companies WHERE CompanyId=? AND IsActive=1)
                  AND EXISTS (SELECT 1 FROM Departments WITH (HOLDLOCK) WHERE CompanyId=? AND DepartmentId=? AND IsActive=1)
                  AND EXISTS (SELECT 1 FROM ResponsibilityLevels WITH (HOLDLOCK) WHERE CompanyId=? AND ResponsibilityLevelId=? AND IsActive=1)
                """;
        return OrganizationTransaction.execute(position.getCompanyId(), connection -> {
            new ReportingPositionDAO().validatePosition(connection,position);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, position.getCompanyId());
                statement.setInt(2, position.getDepartmentId());
                statement.setInt(3, position.getResponsibilityLevelId());
                statement.setString(4, position.getPositionTitle());
                statement.setString(5, position.getDescription());
                statement.setBoolean(6, position.isActive());
                statement.setBoolean(7, position.isCanManageEmployees());
                statement.setBigDecimal(8, position.getPositionAllowance());
                statement.setObject(9, position.getReportsToPositionId(), Types.INTEGER);
                statement.setInt(10, position.getCompanyId());
                statement.setInt(11, position.getCompanyId());
                statement.setInt(12, position.getDepartmentId());
                statement.setInt(13, position.getCompanyId());
                statement.setInt(14, position.getResponsibilityLevelId());
                return statement.executeUpdate() == 1;
            }
        });
    }

    public boolean updatePosition(Position position) throws SQLException {
        String sql = """
                UPDATE p SET DepartmentId=?,ResponsibilityLevelId=?,PositionTitle=?,
                    Description=?,IsActive=?,CanManageEmployees=?,PositionAllowance=?,ReportsToPositionId=?,UpdatedAt=SYSUTCDATETIME()
                FROM Positions p WITH (UPDLOCK, HOLDLOCK)
                WHERE p.CompanyId=? AND p.PositionId=?
                  AND EXISTS (SELECT 1 FROM Departments WITH (HOLDLOCK) WHERE CompanyId=p.CompanyId AND DepartmentId=?
                    AND (IsActive=1 OR (DepartmentId=p.DepartmentId AND (p.IsActive=1 OR ?=0))))
                  AND EXISTS (SELECT 1 FROM ResponsibilityLevels WHERE CompanyId=p.CompanyId AND ResponsibilityLevelId=?
                    AND (IsActive=1 OR ResponsibilityLevelId=p.ResponsibilityLevelId))
                  AND ((p.DepartmentId=? AND p.ResponsibilityLevelId=?) OR (
                """ + UNUSED + "))";
        return OrganizationTransaction.execute(position.getCompanyId(), connection -> {
            new ReportingPositionDAO().validatePosition(connection,position);
            if (!position.isActive() || !position.isCanManageEmployees()) {
                try (PreparedStatement check = connection.prepareStatement("""
                        SELECT 1 FROM Employees manager JOIN Employees report
                          ON report.CompanyId=manager.CompanyId AND report.ManagerId=manager.EmployeeId
                        WHERE manager.CompanyId=? AND manager.PositionId=?
                        """)) {
                    check.setInt(1, position.getCompanyId());
                    check.setInt(2, position.getPositionId());
                    try (ResultSet rows = check.executeQuery()) {
                        if (rows.next()) throw new com.hrms.service.FieldValidationException("canManageEmployees",
                                "Vui lòng phân công lại hoặc gỡ toàn bộ cấp dưới trước khi tắt quyền quản lý hoặc ngừng hoạt động chức danh.");
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, position.getDepartmentId());
                statement.setInt(2, position.getResponsibilityLevelId());
                statement.setString(3, position.getPositionTitle());
                statement.setString(4, position.getDescription());
                statement.setBoolean(5, position.isActive());
                statement.setBoolean(6, position.isCanManageEmployees());
                statement.setBigDecimal(7, position.getPositionAllowance());
                statement.setObject(8, position.getReportsToPositionId(), Types.INTEGER);
                statement.setInt(9, position.getCompanyId());
                statement.setInt(10, position.getPositionId());
                statement.setInt(11, position.getDepartmentId());
                statement.setBoolean(12, position.isActive());
                statement.setInt(13, position.getResponsibilityLevelId());
                statement.setInt(14, position.getDepartmentId());
                statement.setInt(15, position.getResponsibilityLevelId());
                return statement.executeUpdate() == 1;
            }
        });
    }

    public boolean isPositionAssignedToEmployees(int companyId, int id) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM Positions p WHERE p.CompanyId=? AND p.PositionId=? AND (" + UNUSED + ")")) {
            statement.setInt(1, companyId);
            statement.setInt(2, id);
            try (ResultSet rows = statement.executeQuery()) {
                return !rows.next();
            }
        }
    }

    public boolean positionTitleExists(int companyId, int departmentId, String title, Integer excludeId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT 1 FROM Positions WHERE CompanyId=? AND DepartmentId=? AND PositionTitle=?
                         AND (? IS NULL OR PositionId<>?)
                     """)) {
            statement.setInt(1, companyId);
            statement.setInt(2, departmentId);
            statement.setString(3, title);
            statement.setObject(4, excludeId, Types.INTEGER);
            statement.setObject(5, excludeId, Types.INTEGER);
            try (ResultSet rows = statement.executeQuery()) { return rows.next(); }
        }
    }

    private Position mapPosition(ResultSet rows) throws SQLException {
        Position position = new Position();
        position.setPositionId(rows.getInt("PositionId"));
        position.setAssignmentOccupied(rows.getBoolean("AssignmentOccupied"));
        position.setReportsToPositionId((Integer)rows.getObject("ReportsToPositionId"));
        position.setCompanyId(rows.getInt("CompanyId"));
        position.setDepartmentId(rows.getInt("DepartmentId"));
        position.setDepartmentName(rows.getString("DepartmentName"));
        position.setResponsibilityLevelId(rows.getInt("ResponsibilityLevelId"));
        position.setResponsibilityLevelName(rows.getString("LevelName"));
        position.setPositionAllowance(rows.getBigDecimal("PositionAllowance"));
        position.setEffectiveAllowance(rows.getBigDecimal("EffectiveAllowance"));
        position.setPositionTitle(rows.getString("PositionTitle"));
        position.setDescription(rows.getString("Description"));
        position.setActive(rows.getBoolean("IsActive"));
        position.setCanManageEmployees(rows.getBoolean("CanManageEmployees")
                && !"EMPLOYEE".equalsIgnoreCase(rows.getString("LevelCode").trim())
                && !"EMP".equalsIgnoreCase(rows.getString("LevelCode").trim())
                && !"EMPLOYEE".equalsIgnoreCase(rows.getString("LevelName").trim()));
        Timestamp created = rows.getTimestamp("CreatedAt");
        Timestamp updated = rows.getTimestamp("UpdatedAt");
        position.setCreatedAt(created == null ? null : created.toLocalDateTime());
        position.setUpdatedAt(updated == null ? null : updated.toLocalDateTime());
        return position;
    }
}
