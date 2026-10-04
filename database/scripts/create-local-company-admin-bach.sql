-- Create a local/development tenant and Company Admin.
-- Run this script in SSMS after the main HRMsDB schema and seed data exist.
-- Temporary password was provided out-of-band; this file stores only its BCrypt hash.
-- Intended values: Company = Bach Company, workspace = bach, region = NORTH,
-- Company Admin = Bach, email = bach@gmail.com, username = bach.
-- The account is created with MustChangePassword = 1.

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
    SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
    BEGIN TRANSACTION;

    DECLARE @WorkspaceCode VARCHAR(50) = 'bach';
    DECLARE @CompanyName NVARCHAR(200) = N'Bach Company';
    DECLARE @CompanyNameNormalized NVARCHAR(200) = N'bach company';
    DECLARE @Region VARCHAR(10) = 'NORTH';
    DECLARE @ContactEmail VARCHAR(150) = 'bach@gmail.com';
    DECLARE @Username VARCHAR(100) = 'bach';
    DECLARE @DisplayName NVARCHAR(150) = N'Bach';
    DECLARE @PasswordHash VARCHAR(255) =
        '$2a$12$5a1dZFKk2cOmM0oka7/C2u.bh/Kn0XuByIa6REyEigLVFCSwo/CoO';
    DECLARE @Now DATETIME2 = SYSUTCDATETIME();
    DECLARE @RoleId INT;
    DECLARE @PlanId INT;
    DECLARE @CompanyId INT;
    DECLARE @CompanyByNameId INT;
    DECLARE @UserByUsernameId INT;
    DECLARE @UserByEmailId INT;
    DECLARE @SubscriptionId INT;
    DECLARE @UserId INT;

    SELECT @RoleId = RoleId
    FROM dbo.Roles WITH (UPDLOCK, HOLDLOCK)
    WHERE RoleCode = 'COMPANY_ADMIN';

    SELECT TOP (1) @PlanId = PlanId
    FROM dbo.Plans WITH (UPDLOCK, HOLDLOCK)
    WHERE PlanName = N'DEFAULT'
      AND IsActive = 1
      AND MaxAccounts = 20
      AND MaxManagers = 3
      AND BillingCycle = N'MONTHLY'
    ORDER BY PlanId;

    IF @RoleId IS NULL
        THROW 51001, 'Role COMPANY_ADMIN is missing. Run the main seed script first.', 1;

    IF @PlanId IS NULL
        THROW 51002, 'Active DEFAULT plan is missing. Run the main seed script first.', 1;

    SELECT @CompanyId = CompanyId
    FROM dbo.Companies WITH (UPDLOCK, HOLDLOCK)
    WHERE WorkspaceCode = @WorkspaceCode;

    SELECT @CompanyByNameId = CompanyId
    FROM dbo.Companies WITH (UPDLOCK, HOLDLOCK)
    WHERE CompanyNameNormalized = @CompanyNameNormalized;

    SELECT @UserByUsernameId = UserId
    FROM dbo.Users WITH (UPDLOCK, HOLDLOCK)
    WHERE Username = @Username;

    SELECT @UserByEmailId = UserId
    FROM dbo.Users WITH (UPDLOCK, HOLDLOCK)
    WHERE EmailNormalized = @ContactEmail;

    IF @CompanyId IS NOT NULL AND @CompanyByNameId IS NOT NULL AND @CompanyId <> @CompanyByNameId
        THROW 51003, 'Workspace code and normalized company name belong to different companies. No changes were made.', 1;

    IF @CompanyId IS NULL AND @CompanyByNameId IS NOT NULL
        THROW 51004, 'Company name already exists with another workspace. No changes were made.', 1;

    IF @CompanyId IS NOT NULL
       AND (
            EXISTS (SELECT 1 FROM dbo.Companies WHERE CompanyId = @CompanyId AND CompanyName <> @CompanyName)
            OR EXISTS (SELECT 1 FROM dbo.Companies WHERE CompanyId = @CompanyId AND Region <> @Region)
            OR EXISTS (SELECT 1 FROM dbo.Companies WHERE CompanyId = @CompanyId AND ContactEmail <> @ContactEmail)
          )
        THROW 51005, 'Workspace bach already exists with different company data. No changes were made.', 1;

    IF @UserByUsernameId IS NOT NULL AND @UserByEmailId IS NOT NULL
       AND @UserByUsernameId <> @UserByEmailId
        THROW 51006, 'Username bach and email bach@gmail.com belong to different users. No changes were made.', 1;

    IF @UserByUsernameId IS NULL AND @UserByEmailId IS NOT NULL
        THROW 51007, 'Email bach@gmail.com already belongs to another user. No changes were made.', 1;

    IF @UserByUsernameId IS NOT NULL
       AND (
            EXISTS (SELECT 1 FROM dbo.Users WHERE UserId = @UserByUsernameId AND CompanyId <> @CompanyId)
            OR EXISTS (SELECT 1 FROM dbo.Users WHERE UserId = @UserByUsernameId AND RoleId <> @RoleId)
            OR EXISTS (SELECT 1 FROM dbo.Users WHERE UserId = @UserByUsernameId AND EmailNormalized <> @ContactEmail)
          )
        THROW 51008, 'User bach already exists with different account data. No changes were made.', 1;

    IF @CompanyId IS NULL AND @UserByUsernameId IS NOT NULL
        THROW 51009, 'User bach exists without the requested company. No changes were made.', 1;

    IF @CompanyId IS NOT NULL
    BEGIN
        SELECT @SubscriptionId = SubscriptionId
        FROM dbo.Subscriptions WITH (UPDLOCK, HOLDLOCK)
        WHERE CompanyId = @CompanyId;

        IF @UserByUsernameId IS NOT NULL
           AND @SubscriptionId IS NOT NULL
           AND EXISTS (
                SELECT 1
                FROM dbo.Users u
                WHERE u.UserId = @UserByUsernameId
                  AND u.Username = @Username
                  AND u.EmailNormalized = @ContactEmail
                  AND u.DisplayName = @DisplayName
                  AND u.PasswordHash = @PasswordHash
                  AND u.CompanyId = @CompanyId
                  AND u.RoleId = @RoleId
                  AND u.MustChangePassword = 1
                  AND u.ActivatedAt IS NULL
                  AND u.PasswordChangedAt IS NULL
                  AND u.IsActive = 1
                  AND u.IsLocked = 0
                  AND u.FailedLoginAttempts = 0
                  AND u.LockedUntil IS NULL
           )
           AND EXISTS (
                SELECT 1
                FROM dbo.Subscriptions s
                WHERE s.SubscriptionId = @SubscriptionId
                  AND s.CompanyId = @CompanyId
                  AND s.PlanId = @PlanId
                  AND s.Status = N'ACTIVE'
                  AND s.SuspendedAt IS NULL
                  AND s.SuspendedByUserId IS NULL
                  AND s.SuspensionReason IS NULL
           )
        BEGIN
            COMMIT TRANSACTION;
            SELECT c.CompanyId, c.WorkspaceCode, c.CompanyName, c.Region,
                   s.SubscriptionId, s.Status AS SubscriptionStatus,
                   u.UserId, u.Username, u.Email, u.DisplayName,
                   u.MustChangePassword
            FROM dbo.Companies c
            INNER JOIN dbo.Subscriptions s ON s.CompanyId = c.CompanyId
            INNER JOIN dbo.Users u ON u.UserId = @UserByUsernameId
            WHERE c.CompanyId = @CompanyId;
            RETURN;
        END;

        IF @SubscriptionId IS NOT NULL OR @UserByUsernameId IS NOT NULL
            THROW 51010, 'A partial tenant/account already exists but does not match the requested seed data. No changes were made.', 1;
    END;

    IF @CompanyId IS NULL AND (@SubscriptionId IS NOT NULL OR @UserByUsernameId IS NOT NULL)
        THROW 51011, 'Partial tenant data exists. No changes were made.', 1;

    INSERT INTO dbo.Companies (
        WorkspaceCode, CompanyName, CompanyNameNormalized, Region,
        ContactEmail, IsActive
    )
    VALUES (
        @WorkspaceCode, @CompanyName, @CompanyNameNormalized, @Region,
        @ContactEmail, 1
    );
    SET @CompanyId = CONVERT(INT, SCOPE_IDENTITY());

    INSERT INTO dbo.Subscriptions (
        CompanyId, PlanId, StartDate, EndDate, Status,
        SuspendedAt, SuspendedByUserId, SuspensionReason,
        LastActivatedAt, LastActivatedByUserId, PaymentMethod
    )
    VALUES (
        @CompanyId, @PlanId, CONVERT(DATE, @Now), NULL, N'ACTIVE',
        NULL, NULL, NULL, NULL, NULL, NULL
    );
    SET @SubscriptionId = CONVERT(INT, SCOPE_IDENTITY());

    INSERT INTO dbo.Users (
        CompanyId, RoleId, Username, Email, DisplayName, PasswordHash,
        MustChangePassword, ActivatedAt, PasswordChangedAt,
        IsActive, IsLocked, FailedLoginAttempts, LockedUntil
    )
    VALUES (
        @CompanyId, @RoleId, @Username, @ContactEmail, @DisplayName, @PasswordHash,
        1, NULL, NULL,
        1, 0, 0, NULL
    );
    SET @UserId = CONVERT(INT, SCOPE_IDENTITY());

    COMMIT TRANSACTION;

    SELECT c.CompanyId, c.WorkspaceCode, c.CompanyName, c.Region,
           s.SubscriptionId, s.Status AS SubscriptionStatus,
           u.UserId, u.Username, u.Email, u.DisplayName,
           u.MustChangePassword
    FROM dbo.Companies c
    INNER JOIN dbo.Subscriptions s ON s.CompanyId = c.CompanyId
    INNER JOIN dbo.Users u ON u.UserId = @UserId
    WHERE c.CompanyId = @CompanyId;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
