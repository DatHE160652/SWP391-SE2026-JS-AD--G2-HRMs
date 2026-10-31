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
        UpdatedAt          DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        UpdatedByUserId    INT NULL,
        CONSTRAINT FK_CompanyShiftSettings_Companies FOREIGN KEY (CompanyId) REFERENCES dbo.Companies(CompanyId) ON DELETE CASCADE,
        CONSTRAINT FK_CompanyShiftSettings_Users FOREIGN KEY (UpdatedByUserId) REFERENCES dbo.Users(UserId),
        CONSTRAINT CK_CompanyShiftSettings_Times CHECK (ShiftEndTime > ShiftStartTime)
    );
END
GO

-- Seed default settings cho các công ty hiện có nếu chưa có cấu hình
INSERT INTO dbo.CompanyShiftSettings (CompanyId, ShiftStartTime, ShiftEndTime, LunchBreakMinutes, GracePeriodMinutes)
SELECT c.CompanyId, '08:30:00', '17:30:00', 60, 0
FROM dbo.Companies c
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.CompanyShiftSettings s WHERE s.CompanyId = c.CompanyId
);
GO

