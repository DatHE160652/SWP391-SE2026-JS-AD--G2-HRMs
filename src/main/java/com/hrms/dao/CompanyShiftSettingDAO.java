package com.hrms.dao;

import com.hrms.model.CompanyShiftSetting;
import com.hrms.util.DBConnection;

import java.math.BigDecimal;
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

    private static volatile boolean schemaChecked = false;

    private void checkSchemaIfNeeded() {
        if (!schemaChecked) {
            synchronized (CompanyShiftSettingDAO.class) {
                if (!schemaChecked) {
                    ensureTableExists();
                    schemaChecked = true;
                }
            }
        }
    }

    public CompanyShiftSetting getShiftSetting(int companyId) throws SQLException {
        checkSchemaIfNeeded();
        String sql = """
                SELECT s.CompanyId, s.ShiftStartTime, s.ShiftEndTime, s.LunchBreakMinutes,
                       s.GracePeriodMinutes, s.TimesheetCutoffDay, s.MinHoursHalfDay, s.MinHoursFullDay,
                       s.UpdatedAt, s.UpdatedByUserId,
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
            if (msg.contains("companyshiftsettings") || msg.contains("invalid column name")) {
                ensureTableExists();
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, companyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return mapSetting(rs);
                        }
                    }
                } catch (SQLException retryEx) {
                    return new CompanyShiftSetting(companyId);
                }
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
        if (setting.getTimesheetCutoffDay() < 1 || setting.getTimesheetCutoffDay() > 31) {
            setting.setTimesheetCutoffDay(CompanyShiftSetting.DEFAULT_CUTOFF_DAY);
        }
        BigDecimal minHalf = setting.getMinHoursHalfDay();
        BigDecimal minFull = setting.getMinHoursFullDay();
        if (minHalf == null || minHalf.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Mốc giờ tối thiểu tính nửa công (0.5 công) phải lớn hơn 0.");
        }
        if (minFull == null || minFull.compareTo(minHalf) <= 0) {
            throw new IllegalArgumentException("Mốc giờ tính 1.0 công phải lớn hơn mốc tính nửa công (0.5 công).");
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
                        TimesheetCutoffDay = ?,
                        MinHoursHalfDay = ?,
                        MinHoursFullDay = ?,
                        UpdatedAt = SYSUTCDATETIME(),
                        UpdatedByUserId = ?
                    WHERE CompanyId = ?;
                END
                ELSE
                BEGIN
                    INSERT INTO dbo.CompanyShiftSettings
                        (CompanyId, ShiftStartTime, ShiftEndTime, LunchBreakMinutes, GracePeriodMinutes, TimesheetCutoffDay, MinHoursHalfDay, MinHoursFullDay, UpdatedAt, UpdatedByUserId)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, SYSUTCDATETIME(), ?);
                END
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, setting.getCompanyId());
            ps.setTime(2, Time.valueOf(setting.getShiftStartTime()));
            ps.setTime(3, Time.valueOf(setting.getShiftEndTime()));
            ps.setInt(4, setting.getLunchBreakMinutes());
            ps.setInt(5, setting.getGracePeriodMinutes());
            ps.setInt(6, setting.getTimesheetCutoffDay());
            ps.setBigDecimal(7, minHalf);
            ps.setBigDecimal(8, minFull);
            if (setting.getUpdatedByUserId() != null && setting.getUpdatedByUserId() > 0) {
                ps.setInt(9, setting.getUpdatedByUserId());
            } else {
                ps.setNull(9, Types.INTEGER);
            }
            ps.setInt(10, setting.getCompanyId());

            ps.setInt(11, setting.getCompanyId());
            ps.setTime(12, Time.valueOf(setting.getShiftStartTime()));
            ps.setTime(13, Time.valueOf(setting.getShiftEndTime()));
            ps.setInt(14, setting.getLunchBreakMinutes());
            ps.setInt(15, setting.getGracePeriodMinutes());
            ps.setInt(16, setting.getTimesheetCutoffDay());
            ps.setBigDecimal(17, minHalf);
            ps.setBigDecimal(18, minFull);
            if (setting.getUpdatedByUserId() != null && setting.getUpdatedByUserId() > 0) {
                ps.setInt(19, setting.getUpdatedByUserId());
            } else {
                ps.setNull(19, Types.INTEGER);
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
                        TimesheetCutoffDay INT NOT NULL DEFAULT 5,
                        MinHoursHalfDay    DECIMAL(4,2) NOT NULL DEFAULT 4.00,
                        MinHoursFullDay    DECIMAL(4,2) NOT NULL DEFAULT 6.00,
                        UpdatedAt          DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
                        UpdatedByUserId    INT NULL,
                        CONSTRAINT FK_CompanyShiftSettings_Companies FOREIGN KEY (CompanyId) REFERENCES dbo.Companies(CompanyId) ON DELETE CASCADE,
                        CONSTRAINT FK_CompanyShiftSettings_Users FOREIGN KEY (UpdatedByUserId) REFERENCES dbo.Users(UserId),
                        CONSTRAINT CK_CompanyShiftSettings_Times CHECK (ShiftEndTime > ShiftStartTime)
                    );
                END
                ELSE
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.CompanyShiftSettings') AND name = 'TimesheetCutoffDay')
                    BEGIN
                        ALTER TABLE dbo.CompanyShiftSettings ADD TimesheetCutoffDay INT NOT NULL DEFAULT 5;
                    END
                    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.CompanyShiftSettings') AND name = 'MinHoursHalfDay')
                    BEGIN
                        ALTER TABLE dbo.CompanyShiftSettings ADD MinHoursHalfDay DECIMAL(4,2) NOT NULL DEFAULT 4.00;
                    END
                    IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.CompanyShiftSettings') AND name = 'MinHoursFullDay')
                    BEGIN
                        ALTER TABLE dbo.CompanyShiftSettings ADD MinHoursFullDay DECIMAL(4,2) NOT NULL DEFAULT 6.00;
                    END
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

        try {
            int cutoff = rs.getInt("TimesheetCutoffDay");
            if (!rs.wasNull() && cutoff > 0) {
                setting.setTimesheetCutoffDay(cutoff);
            }
        } catch (SQLException ignored) {
            setting.setTimesheetCutoffDay(CompanyShiftSetting.DEFAULT_CUTOFF_DAY);
        }

        try {
            BigDecimal minHalf = rs.getBigDecimal("MinHoursHalfDay");
            if (minHalf != null) {
                setting.setMinHoursHalfDay(minHalf);
            }
        } catch (SQLException ignored) {
            setting.setMinHoursHalfDay(CompanyShiftSetting.DEFAULT_MIN_HOURS_HALF_DAY);
        }

        try {
            BigDecimal minFull = rs.getBigDecimal("MinHoursFullDay");
            if (minFull != null) {
                setting.setMinHoursFullDay(minFull);
            }
        } catch (SQLException ignored) {
            setting.setMinHoursFullDay(CompanyShiftSetting.DEFAULT_MIN_HOURS_FULL_DAY);
        }

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

