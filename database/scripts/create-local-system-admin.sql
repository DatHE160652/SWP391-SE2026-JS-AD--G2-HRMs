-- Tao System Admin cho moi truong LOCAL/DEVELOPMENT, khong dung cho production.
-- Chay sau SWP2.sql tren database ung dung.
-- Chi dung local/demo: mat khau khoi tao duoc luu dang plaintext trong Users.Password.
-- Khong reset, mo khoa hay thay doi tai khoan da ton tai khi chay lai.
USE HRMsDB;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @RoleId INT;
    SELECT @RoleId = RoleId
    FROM dbo.Roles
    WHERE RoleCode = 'SYSTEM_ADMIN';

    IF @RoleId IS NULL
        THROW 50001, 'Role SYSTEM_ADMIN is missing. Initialize the Roles table first.', 1;

    DECLARE @Now DATETIME2 = SYSUTCDATETIME();

    IF NOT EXISTS (
        SELECT 1 FROM dbo.Users WITH (UPDLOCK, HOLDLOCK)
        WHERE Username = 'sysadmin'
           OR LOWER(LTRIM(RTRIM(Email))) = 'sysadmin@hrms.local'
    )
    BEGIN
        INSERT INTO dbo.Users (
            CompanyId, RoleId, Username, Email, DisplayName, Password,
            MustChangePassword, ActivatedAt, PasswordChangedAt,
            IsActive, IsLocked, FailedLoginAttempts, LockedUntil
        )
        VALUES (
            NULL, @RoleId, 'sysadmin', 'sysadmin@hrms.local', N'System Admin',
            N'12345',
            0, @Now, @Now, 1, 0, 0, NULL
        );
    END

    IF NOT EXISTS (
        SELECT 1 FROM dbo.Users WITH (UPDLOCK, HOLDLOCK)
        WHERE Username = 'sysadmin2'
           OR LOWER(LTRIM(RTRIM(Email))) = 'sysadmin2@hrms.local'
    )
    BEGIN
        INSERT INTO dbo.Users (
            CompanyId, RoleId, Username, Email, DisplayName, Password,
            MustChangePassword, ActivatedAt, PasswordChangedAt,
            IsActive, IsLocked, FailedLoginAttempts, LockedUntil
        )
        VALUES (
            NULL, @RoleId, 'sysadmin2', 'sysadmin2@hrms.local', N'System Admin 2',
            N'12345',
            0, @Now, @Now, 1, 0, 0, NULL
        );
    END

    COMMIT TRANSACTION;

    SELECT UserId, Username, Email, DisplayName, IsActive, IsLocked
    FROM dbo.Users
    WHERE Username IN ('sysadmin', 'sysadmin2')
    ORDER BY UserId;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
