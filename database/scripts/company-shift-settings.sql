USE HRMsDB;
GO

-- ========================================================================
-- BẢNG: CompanyShiftSettings (Cấu hình ca làm việc theo từng công ty)
-- Phục vụ cho Quản lý (Manager) / Admin cài đặt giờ làm việc riêng biệt
-- ========================================================================

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
        CONSTRAINT CK_CompanyShiftSettings_Times CHECK (ShiftEndTime > ShiftStartTime),
        CONSTRAINT CK_CompanyShiftSettings_CutoffDay CHECK (TimesheetCutoffDay BETWEEN 1 AND 31),
        CONSTRAINT CK_CompanyShiftSettings_Hours CHECK (MinHoursFullDay > MinHoursHalfDay AND MinHoursHalfDay > 0)
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
GO

-- Seed default settings cho các công ty hiện có nếu chưa có cấu hình
INSERT INTO dbo.CompanyShiftSettings (CompanyId, ShiftStartTime, ShiftEndTime, LunchBreakMinutes, GracePeriodMinutes, TimesheetCutoffDay, MinHoursHalfDay, MinHoursFullDay)
SELECT c.CompanyId, '08:30:00', '17:30:00', 60, 0, 5, 4.00, 6.00
FROM dbo.Companies c
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.CompanyShiftSettings s WHERE s.CompanyId = c.CompanyId
);
GO

