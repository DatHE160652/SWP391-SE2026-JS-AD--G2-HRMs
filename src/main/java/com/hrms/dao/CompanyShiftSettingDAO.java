package com.hrms.dao;

import com.hrms.model.CompanyShiftSetting;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalTime;

public class CompanyShiftSettingDAO {

    public CompanyShiftSetting getShiftSetting(int companyId) throws SQLException {
        String sql = """
                SELECT s.CompanyId, s.ShiftStartTime, s.ShiftEndTime, s.LunchBreakMinutes,
                       s.GracePeriodMinutes, s.UpdatedAt, s.UpdatedByUserId,
                       u.DisplayName AS UpdatedByName
                FROM dbo.CompanyShiftSettings s
                LEFT JOIN dbo.Users u ON s.UpdatedByUserId = u.UserId
                WHERE s.CompanyId = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSetting(rs);
                }
            }
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("companyshiftsettings")) {
                ensureTableExists();
                return new CompanyShiftSetting(companyId);
            }
            throw e;
        }

        return new CompanyShiftSetting(companyId);
    }

    public boolean saveShiftSetting(CompanyShiftSetting setting) throws SQLException {
        if (setting == null || setting.getCompanyId() <= 0) {
            throw new IllegalArgumentException("Thông tin công ty không hợp lệ.");
        }
        if (setting.getShiftStartTime() == null || setting.getShiftEndTime() == null) {
            throw new IllegalArgumentException("Giờ bắt đầu và kết thúc ca không được để trống.");
        }
        if (!setting.getShiftEndTime().isAfter(setting.getShiftStartTime())) {
            throw new IllegalArgumentException("Giờ kết thúc ca làm việc phải sau giờ bắt đầu ca.");
        }
        if (setting.getLunchBreakMinutes() < 0) {
            throw new IllegalArgumentException("Thời gian nghỉ trưa không được âm.");
        }
        if (setting.getGracePeriodMinutes() < 0) {
            throw new IllegalArgumentException("Thời gian ân hạn không được âm.");
        }

        ensureTableExists();

        String sql = """
                IF EXISTS (SELECT 1 FROM dbo.CompanyShiftSettings WHERE CompanyId = ?)
                BEGIN
                    UPDATE dbo.CompanyShiftSettings
                    SET ShiftStartTime = ?,
                        ShiftEndTime = ?,
                        LunchBreakMinutes = ?,
                        GracePeriodMinutes = ?,
                        UpdatedAt = SYSUTCDATETIME(),
                        UpdatedByUserId = ?
                    WHERE CompanyId = ?;
                END
                ELSE
                BEGIN
                    INSERT INTO dbo.CompanyShiftSettings
                        (CompanyId, ShiftStartTime, ShiftEndTime, LunchBreakMinutes, GracePeriodMinutes, UpdatedAt, UpdatedByUserId)
                    VALUES (?, ?, ?, ?, ?, SYSUTCDATETIME(), ?);
                END
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, setting.getCompanyId());
            ps.setTime(2, Time.valueOf(setting.getShiftStartTime()));
            ps.setTime(3, Time.valueOf(setting.getShiftEndTime()));
            ps.setInt(4, setting.getLunchBreakMinutes());
            ps.setInt(5, setting.getGracePeriodMinutes());
            if (setting.getUpdatedByUserId() != null && setting.getUpdatedByUserId() > 0) {
                ps.setInt(6, setting.getUpdatedByUserId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setInt(7, setting.getCompanyId());

            ps.setInt(8, setting.getCompanyId());
            ps.setTime(9, Time.valueOf(setting.getShiftStartTime()));
            ps.setTime(10, Time.valueOf(setting.getShiftEndTime()));
            ps.setInt(11, setting.getLunchBreakMinutes());
            ps.setInt(12, setting.getGracePeriodMinutes());
            if (setting.getUpdatedByUserId() != null && setting.getUpdatedByUserId() > 0) {
                ps.setInt(13, setting.getUpdatedByUserId());
            } else {
                ps.setNull(13, Types.INTEGER);
            }

            return ps.executeUpdate() > 0;
        }
    }

    public void ensureTableExists() {
        String ddl = """
                IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'CompanyShiftSettings')
                BEGIN
                    CREATE TABLE dbo.CompanyShiftSettings (
                        CompanyId          INT PRIMARY KEY,
                        ShiftStartTime     TIME(0) NOT NULL DEFAULT '08:30:00',
                        ShiftEndTime       TIME(0) NOT NULL DEFAULT '17:30:00',
                        LunchBreakMinutes  INT NOT NULL DEFAULT 60,
                        GracePeriodMinutes INT NOT NULL DEFAULT 0,
                        UpdatedAt          DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                        UpdatedByUserId    INT NULL,
                        CONSTRAINT FK_CompanyShiftSettings_Companies FOREIGN KEY (CompanyId) REFERENCES dbo.Companies(CompanyId) ON DELETE CASCADE,
                        CONSTRAINT FK_CompanyShiftSettings_Users FOREIGN KEY (UpdatedByUserId) REFERENCES dbo.Users(UserId),
                        CONSTRAINT CK_CompanyShiftSettings_Times CHECK (ShiftEndTime > ShiftStartTime)
                    );
                END
                """;
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(ddl);
        } catch (SQLException ignored) {
            // Logged or handled silently if already exists or permissions
        }
    }

    private CompanyShiftSetting mapSetting(ResultSet rs) throws SQLException {
        CompanyShiftSetting setting = new CompanyShiftSetting();
        setting.setCompanyId(rs.getInt("CompanyId"));

        Time startTime = rs.getTime("ShiftStartTime");
        setting.setShiftStartTime(startTime != null ? startTime.toLocalTime() : CompanyShiftSetting.DEFAULT_SHIFT_START);

        Time endTime = rs.getTime("ShiftEndTime");
        setting.setShiftEndTime(endTime != null ? endTime.toLocalTime() : CompanyShiftSetting.DEFAULT_SHIFT_END);

        setting.setLunchBreakMinutes(rs.getInt("LunchBreakMinutes"));
        setting.setGracePeriodMinutes(rs.getInt("GracePeriodMinutes"));

        Timestamp updatedAt = rs.getTimestamp("UpdatedAt");
        if (updatedAt != null) {
            setting.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        int updatedByUserId = rs.getInt("UpdatedByUserId");
        if (!rs.wasNull()) {
            setting.setUpdatedByUserId(updatedByUserId);
        }
        setting.setUpdatedByName(rs.getString("UpdatedByName"));

        return setting;
    }
}

