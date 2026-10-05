-- Tao System Admin cho moi truong LOCAL/DEVELOPMENT, khong dung cho production.
-- Chay sau SWP2.sql tren database ung dung. Khong luu plaintext password trong file.
-- Mat khau duoc cung cap rieng trong hoi thoai; hash BCrypt cost 12 da duoc xac minh.
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

    IF EXISTS (
        SELECT 1 FROM dbo.Users WITH (UPDLOCK, HOLDLOCK)
        WHERE Username = 'sysadmin'
           OR EmailNormalized = 'sysadmin@hrms.local'
    )
        THROW 50002, 'Username/email already exists. No account or password was changed.', 1;

    DECLARE @Now DATETIME2 = SYSUTCDATETIME();
    INSERT INTO dbo.Users (
        CompanyId, RoleId, Username, Email, DisplayName, PasswordHash,
        MustChangePassword, ActivatedAt, PasswordChangedAt,
        IsActive, IsLocked, FailedLoginAttempts, LockedUntil
    )
    VALUES (
        NULL, @RoleId, 'sysadmin', 'sysadmin@hrms.local', N'System Admin',
        '$2a$12$X5Q9Em1J.RtWKUHrMGCCHO9Gz/l36vrvrkmOvGPeU6AoPE38z/a7W',
        0, @Now, @Now, 1, 0, 0, NULL
    );

    DECLARE @UserId INT = CONVERT(INT, SCOPE_IDENTITY());
    COMMIT TRANSACTION;

    SELECT UserId, Username, Email, DisplayName, IsActive, IsLocked
    FROM dbo.Users WHERE UserId = @UserId;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
