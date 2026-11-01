package com.hrms.dao;

import com.hrms.model.TimesheetLock;
import com.hrms.util.DBConnection;

import java.sql.*;

/**
 * Data Access Object for TimesheetLocks (UC88).
 */
public class TimesheetLockDAO {

    public TimesheetLock getLock(int companyId, int month, int year) throws SQLException {
        String sql = """
                SELECT tl.*, u.Username AS LockedByUserName
                FROM dbo.TimesheetLocks tl
                LEFT JOIN dbo.Users u ON tl.LockedByUserId = u.UserId
                WHERE tl.CompanyId = ? AND tl.Month = ? AND tl.Year = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            ps.setInt(2, month);
            ps.setInt(3, year);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public boolean isMonthLocked(int companyId, int month, int year) throws SQLException {
        String sql = """
                SELECT IsLocked FROM dbo.TimesheetLocks
                WHERE CompanyId = ? AND Month = ? AND Year = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            ps.setInt(2, month);
            ps.setInt(3, year);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("IsLocked");
                }
            }
        }
        return false;
    }

    public boolean setLock(int companyId, int month, int year, boolean isLocked, int userId, String notes) throws SQLException {
        String sql = """
                MERGE dbo.TimesheetLocks AS target
                USING (SELECT ? AS CompanyId, ? AS [Month], ? AS [Year]) AS source
                ON (target.CompanyId = source.CompanyId AND target.[Month] = source.[Month] AND target.[Year] = source.[Year])
                WHEN MATCHED THEN
                    UPDATE SET IsLocked = ?, LockedByUserId = ?, LockedAt = GETDATE(), Notes = ?
                WHEN NOT MATCHED THEN
                    INSERT (CompanyId, [Month], [Year], IsLocked, LockedByUserId, LockedAt, Notes)
                    VALUES (?, ?, ?, ?, ?, GETDATE(), ?);
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            ps.setInt(2, month);
            ps.setInt(3, year);

            ps.setBoolean(4, isLocked);
            ps.setInt(5, userId);
            ps.setString(6, notes);

            ps.setInt(7, companyId);
            ps.setInt(8, month);
            ps.setInt(9, year);
            ps.setBoolean(10, isLocked);
            ps.setInt(11, userId);
            ps.setString(12, notes);

            return ps.executeUpdate() > 0;
        }
    }

    private TimesheetLock mapRow(ResultSet rs) throws SQLException {
        TimesheetLock lock = new TimesheetLock();
        lock.setLockId(rs.getInt("LockId"));
        lock.setCompanyId(rs.getInt("CompanyId"));
        lock.setMonth(rs.getInt("Month"));
        lock.setYear(rs.getInt("Year"));
        lock.setLocked(rs.getBoolean("IsLocked"));
        lock.setLockedByUserId(rs.getInt("LockedByUserId"));

        Timestamp lat = rs.getTimestamp("LockedAt");
        if (lat != null) lock.setLockedAt(lat.toLocalDateTime());

        lock.setNotes(rs.getString("Notes"));
        lock.setLockedByUserName(rs.getString("LockedByUserName"));
        return lock;
    }
}

