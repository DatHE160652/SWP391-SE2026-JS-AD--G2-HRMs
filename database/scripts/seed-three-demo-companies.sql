-- Demo-only seed: three active tenant workspaces, 15 usable accounts each.
-- Passwords are intentionally plaintext for this local/demo project only.
-- Run after database/db.sql (and leave-common-quota-migration.sql when upgrading).
-- Idempotent: safe to re-run, does not delete data, does not touch other tenants.
USE HRMsDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @Now DATETIME2 = SYSUTCDATETIME();
    DECLARE @Password NVARCHAR(255) = N'12345678';
    DECLARE @DefaultPlanId INT = (SELECT TOP (1) PlanId FROM dbo.Plans WHERE PlanName = N'DEFAULT' AND IsActive = 1 ORDER BY PlanId);
    IF @DefaultPlanId IS NULL
        THROW 52001, 'The active DEFAULT plan is required before seeding demo companies.', 1;

    DECLARE @RoleCompanyAdmin INT = (SELECT RoleId FROM dbo.Roles WHERE RoleCode = 'COMPANY_ADMIN');
    DECLARE @RoleHr INT = (SELECT RoleId FROM dbo.Roles WHERE RoleCode = 'HR');
    DECLARE @RoleManager INT = (SELECT RoleId FROM dbo.Roles WHERE RoleCode = 'MANAGER');
    DECLARE @RoleEmployee INT = (SELECT RoleId FROM dbo.Roles WHERE RoleCode = 'EMPLOYEE');
    IF @RoleCompanyAdmin IS NULL OR @RoleHr IS NULL OR @RoleManager IS NULL OR @RoleEmployee IS NULL
        THROW 52002, 'Required tenant roles are missing. Run the role seed in database/db.sql first.', 1;

    DECLARE @Companies TABLE (
        Rn INT IDENTITY(1,1) PRIMARY KEY,
        WorkspaceCode VARCHAR(50) NOT NULL UNIQUE,
        CompanyName NVARCHAR(200) NOT NULL,
        CompanyNameNormalized NVARCHAR(200) NOT NULL,
        Region VARCHAR(10) NOT NULL,
        ContactEmail VARCHAR(150) NOT NULL,
        ContactPhone VARCHAR(30) NOT NULL,
        Address NVARCHAR(300) NOT NULL
    );
    INSERT INTO @Companies (WorkspaceCode, CompanyName, CompanyNameNormalized, Region, ContactEmail, ContactPhone, Address) VALUES
        ('demo-alpha', N'Demo Alpha Technology Co., Ltd.', N'demo alpha technology co ltd', 'NORTH',   'contact@demo-alpha.example', '0901000001', N'1 Demo Alpha Street'),
        ('demo-beta',  N'Demo Beta Services Co., Ltd.',    N'demo beta services co ltd',    'CENTRAL', 'contact@demo-beta.example',  '0901000002', N'2 Demo Beta Street'),
        ('demo-gamma', N'Demo Gamma Trading Co., Ltd.',    N'demo gamma trading co ltd',    'SOUTH',   'contact@demo-gamma.example', '0901000003', N'3 Demo Gamma Street');

    DECLARE @Blueprint TABLE (
        AccountNo INT NOT NULL PRIMARY KEY,
        RoleCode VARCHAR(30) NOT NULL,
        RoleId INT NOT NULL,
        AccountSuffix VARCHAR(30) NOT NULL,
        DisplaySuffix NVARCHAR(100) NOT NULL
    );
    INSERT INTO @Blueprint VALUES
        (1,  'COMPANY_ADMIN', @RoleCompanyAdmin, 'seed3-admin',       N'Company Admin / HR'),
        (2,  'MANAGER',       @RoleManager,      'seed3-manager-1',   N'Manager 1'),
        (3,  'MANAGER',       @RoleManager,      'seed3-manager-2',   N'Manager 2'),
        (4,  'MANAGER',       @RoleManager,      'seed3-manager-3',   N'Manager 3'),
        (5,  'EMPLOYEE',      @RoleEmployee,     'seed3-employee-01', N'Employee 01'),
        (6,  'EMPLOYEE',      @RoleEmployee,     'seed3-employee-02', N'Employee 02'),
        (7,  'EMPLOYEE',      @RoleEmployee,     'seed3-employee-03', N'Employee 03'),
        (8,  'EMPLOYEE',      @RoleEmployee,     'seed3-employee-04', N'Employee 04'),
        (9,  'EMPLOYEE',      @RoleEmployee,     'seed3-employee-05', N'Employee 05'),
        (10, 'EMPLOYEE',      @RoleEmployee,     'seed3-employee-06', N'Employee 06'),
        (11, 'EMPLOYEE',      @RoleEmployee,     'seed3-employee-07', N'Employee 07'),
        (12, 'EMPLOYEE',      @RoleEmployee,     'seed3-employee-08', N'Employee 08'),
        (13, 'EMPLOYEE',      @RoleEmployee,     'seed3-employee-09', N'Employee 09'),
        (14, 'EMPLOYEE',      @RoleEmployee,     'seed3-employee-10', N'Employee 10'),
        (15, 'EMPLOYEE',      @RoleEmployee,     'seed3-employee-11', N'Employee 11');

    DECLARE @CompanyName NVARCHAR(200), @CompanyNameNormalized NVARCHAR(200), @Region VARCHAR(10),
            @ContactEmail VARCHAR(150), @ContactPhone VARCHAR(30), @Address NVARCHAR(300),
            @WorkspaceCode VARCHAR(50), @CompanyId INT;

    DECLARE @DepartmentGeneral INT, @DepartmentTech INT, @DepartmentHr INT;
    DECLARE @PositionAdmin INT, @PositionHr INT, @PositionManager INT, @PositionEmployee INT;
    DECLARE @AccountNo INT, @RoleCode VARCHAR(30), @RoleId INT, @AccountSuffix VARCHAR(30), @DisplaySuffix NVARCHAR(100);
    DECLARE @Username VARCHAR(150), @Email VARCHAR(150), @EmployeeCode VARCHAR(50), @FullName NVARCHAR(150),
            @DepartmentId INT, @PositionId INT;
    DECLARE @UserId INT, @UserCompanyId INT, @UserIdByEmail INT;
    DECLARE @LinkedEmployeeId INT, @LinkedEmployeeCompanyId INT;
    DECLARE @CodeEmployeeId INT, @CodeEmployeeUserId INT, @CodeEmployeeEmailOwner INT;
    DECLARE @EmployeeId INT;
    DECLARE @WsLoop INT = 1, @AcctLoop INT;
    DECLARE @Manager1 INT, @Manager2 INT, @Manager3 INT;

    WHILE @WsLoop <= (SELECT COUNT(*) FROM @Companies)
    BEGIN
        SELECT @WorkspaceCode = WorkspaceCode, @CompanyName = CompanyName, @CompanyNameNormalized = CompanyNameNormalized,
               @Region = Region, @ContactEmail = ContactEmail, @ContactPhone = ContactPhone, @Address = Address
        FROM @Companies WHERE Rn = @WsLoop;

        -- Company upsert
        SET @CompanyId = (SELECT CompanyId FROM dbo.Companies WHERE WorkspaceCode = @WorkspaceCode);
        IF @CompanyId IS NULL
        BEGIN
            INSERT INTO dbo.Companies
                (WorkspaceCode, CompanyName, CompanyNameNormalized, Region, ContactEmail, ContactPhone, Address, IsActive, CreatedAt)
            VALUES
                (@WorkspaceCode, @CompanyName, @CompanyNameNormalized, @Region, @ContactEmail, @ContactPhone, @Address, 1, @Now);
            SET @CompanyId = CONVERT(INT, SCOPE_IDENTITY());
        END
        ELSE
            UPDATE dbo.Companies SET IsActive = 1, UpdatedAt = @Now WHERE CompanyId = @CompanyId;

        -- Subscription upsert (TR_Users_MaxAccounts requires an active plan per company)
        IF NOT EXISTS (SELECT 1 FROM dbo.Subscriptions WHERE CompanyId = @CompanyId)
            INSERT INTO dbo.Subscriptions (CompanyId, PlanId, StartDate, Status, CreatedAt)
            VALUES (@CompanyId, @DefaultPlanId, CONVERT(DATE, @Now), N'ACTIVE', @Now);
        ELSE
            UPDATE dbo.Subscriptions
            SET PlanId = @DefaultPlanId, Status = N'ACTIVE', EndDate = NULL, SuspendedAt = NULL,
                SuspendedByUserId = NULL, SuspensionReason = NULL, UpdatedAt = @Now
            WHERE CompanyId = @CompanyId;

        -- Safety guards: only touch an untouched demo workspace
        IF EXISTS (
            SELECT 1 FROM dbo.Users
            WHERE CompanyId = @CompanyId
              AND Username NOT LIKE CONCAT(@WorkspaceCode, '-seed3-%')
              AND Username NOT LIKE CONCAT(@WorkspaceCode, '-admin')
              AND Username NOT LIKE CONCAT(@WorkspaceCode, '-hr')
              AND Username NOT LIKE CONCAT(@WorkspaceCode, '-manager-%')
              AND Username NOT LIKE CONCAT(@WorkspaceCode, '-employee-%')
        )
            THROW 52003, 'The demo workspace already contains non-demo users. Review it before running this seed.', 1;

        -- Remove previous partial runs of THIS seed only (demo accounts of this workspace).
        DECLARE @DemoUsers TABLE (UserId INT PRIMARY KEY);
        INSERT INTO @DemoUsers (UserId)
        SELECT UserId FROM dbo.Users
        WHERE CompanyId = @CompanyId
          AND (Username LIKE CONCAT(@WorkspaceCode, '-seed3-%')
            OR Username LIKE CONCAT(@WorkspaceCode, '-admin')
            OR Username LIKE CONCAT(@WorkspaceCode, '-hr')
            OR Username LIKE CONCAT(@WorkspaceCode, '-manager-%')
            OR Username LIKE CONCAT(@WorkspaceCode, '-employee-%'));

        DECLARE @DemoEmployees TABLE (EmployeeId INT PRIMARY KEY);
        INSERT INTO @DemoEmployees (EmployeeId)
        SELECT EmployeeId FROM dbo.Employees
        WHERE CompanyId = @CompanyId
          AND (UserId IN (SELECT UserId FROM @DemoUsers)
            OR EmployeeCode LIKE 'D0[1-9]' OR EmployeeCode LIKE 'D1[0-5]');

        IF EXISTS (SELECT 1 FROM @DemoEmployees)
        BEGIN
            DELETE rd FROM dbo.LeaveRequestDates rd
            JOIN dbo.LeaveRequests lr ON lr.LeaveRequestId = rd.LeaveRequestId
            WHERE lr.CompanyId = @CompanyId AND lr.EmployeeId IN (SELECT EmployeeId FROM @DemoEmployees);
            DELETE lr FROM dbo.LeaveRequests lr
            WHERE lr.CompanyId = @CompanyId AND lr.EmployeeId IN (SELECT EmployeeId FROM @DemoEmployees);
            DELETE a FROM dbo.Attendances a
            WHERE a.CompanyId = @CompanyId AND a.EmployeeId IN (SELECT EmployeeId FROM @DemoEmployees);
            UPDATE e SET ManagerId = NULL
            WHERE e.CompanyId = @CompanyId AND e.EmployeeId IN (SELECT EmployeeId FROM @DemoEmployees);
            DELETE e FROM dbo.Employees e
            WHERE e.CompanyId = @CompanyId AND e.EmployeeId IN (SELECT EmployeeId FROM @DemoEmployees);
        END
        IF EXISTS (SELECT 1 FROM @DemoUsers)
        BEGIN
            UPDATE lr SET ApproverId = NULL
            FROM dbo.LeaveRequests lr
            WHERE lr.CompanyId = @CompanyId AND lr.ApproverId IN (SELECT UserId FROM @DemoUsers);
            DELETE u FROM dbo.Users u
            WHERE u.CompanyId = @CompanyId AND u.UserId IN (SELECT UserId FROM @DemoUsers);
        END
        DELETE FROM @DemoEmployees;
        DELETE FROM @DemoUsers;

        -- Departments
        SET @DepartmentGeneral = (SELECT DepartmentId FROM dbo.Departments WHERE CompanyId = @CompanyId AND DepartmentCode = 'DEMO-GEN');
        IF @DepartmentGeneral IS NULL
        BEGIN
            INSERT INTO dbo.Departments (CompanyId, DepartmentCode, DepartmentName, IsActive, CreatedAt)
            VALUES (@CompanyId, 'DEMO-GEN', N'Demo General', 1, @Now);
            SET @DepartmentGeneral = CONVERT(INT, SCOPE_IDENTITY());
        END

        SET @DepartmentTech = (SELECT DepartmentId FROM dbo.Departments WHERE CompanyId = @CompanyId AND DepartmentCode = 'DEMO-TECH');
        IF @DepartmentTech IS NULL
        BEGIN
            INSERT INTO dbo.Departments (CompanyId, DepartmentCode, DepartmentName, IsActive, CreatedAt)
            VALUES (@CompanyId, 'DEMO-TECH', N'Demo Technology', 1, @Now);
            SET @DepartmentTech = CONVERT(INT, SCOPE_IDENTITY());
        END

        SET @DepartmentHr = (SELECT DepartmentId FROM dbo.Departments WHERE CompanyId = @CompanyId AND DepartmentCode = 'DEMO-HR');
        IF @DepartmentHr IS NULL
        BEGIN
            INSERT INTO dbo.Departments (CompanyId, DepartmentCode, DepartmentName, IsActive, CreatedAt)
            VALUES (@CompanyId, 'DEMO-HR', N'Demo Human Resources', 1, @Now);
            SET @DepartmentHr = CONVERT(INT, SCOPE_IDENTITY());
        END

        -- Positions
        SET @PositionAdmin = (SELECT PositionId FROM dbo.Positions WHERE CompanyId = @CompanyId AND PositionTitle = N'Demo Company Administrator');
        IF @PositionAdmin IS NULL
        BEGIN
            INSERT INTO dbo.Positions (CompanyId, PositionTitle, DepartmentId, PositionCode, IsActive, CreatedAt)
            VALUES (@CompanyId, N'Demo Company Administrator', @DepartmentGeneral, 'DEMO-ADMIN', 1, @Now);
            SET @PositionAdmin = CONVERT(INT, SCOPE_IDENTITY());
        END

        SET @PositionHr = (SELECT PositionId FROM dbo.Positions WHERE CompanyId = @CompanyId AND PositionTitle = N'Demo HR Specialist');
        IF @PositionHr IS NULL
        BEGIN
            INSERT INTO dbo.Positions (CompanyId, PositionTitle, DepartmentId, PositionCode, IsActive, CreatedAt)
            VALUES (@CompanyId, N'Demo HR Specialist', @DepartmentHr, 'DEMO-HR', 1, @Now);
            SET @PositionHr = CONVERT(INT, SCOPE_IDENTITY());
        END

        SET @PositionManager = (SELECT PositionId FROM dbo.Positions WHERE CompanyId = @CompanyId AND PositionTitle = N'Demo Team Manager');
        IF @PositionManager IS NULL
        BEGIN
            INSERT INTO dbo.Positions (CompanyId, PositionTitle, DepartmentId, PositionCode, IsActive, CreatedAt)
            VALUES (@CompanyId, N'Demo Team Manager', @DepartmentTech, 'DEMO-MGR', 1, @Now);
            SET @PositionManager = CONVERT(INT, SCOPE_IDENTITY());
        END

        SET @PositionEmployee = (SELECT PositionId FROM dbo.Positions WHERE CompanyId = @CompanyId AND PositionTitle = N'Demo Employee');
        IF @PositionEmployee IS NULL
        BEGIN
            INSERT INTO dbo.Positions (CompanyId, PositionTitle, DepartmentId, PositionCode, IsActive, CreatedAt)
            VALUES (@CompanyId, N'Demo Employee', @DepartmentTech, 'DEMO-EMP', 1, @Now);
            SET @PositionEmployee = CONVERT(INT, SCOPE_IDENTITY());
        END

        -- Accounts
        SET @AcctLoop = 1;
        WHILE @AcctLoop <= (SELECT COUNT(*) FROM @Blueprint)
        BEGIN
            SELECT @AccountNo = AccountNo, @RoleCode = RoleCode, @RoleId = RoleId,
                   @AccountSuffix = AccountSuffix, @DisplaySuffix = DisplaySuffix
            FROM @Blueprint WHERE AccountNo = @AcctLoop;

            SET @Username = CONCAT(@WorkspaceCode, '-', @AccountSuffix);
            SET @Email = CONCAT(@Username, '@example.test');
            SET @EmployeeCode = CONCAT('D', RIGHT('00' + CONVERT(VARCHAR(2), @AccountNo), 2));
            SET @FullName = CONCAT(@CompanyName, N' - ', @DisplaySuffix);
            SET @DepartmentId = CASE @RoleCode WHEN 'COMPANY_ADMIN' THEN @DepartmentGeneral WHEN 'HR' THEN @DepartmentHr ELSE @DepartmentTech END;
            SET @PositionId = CASE @RoleCode WHEN 'COMPANY_ADMIN' THEN @PositionAdmin WHEN 'HR' THEN @PositionHr WHEN 'MANAGER' THEN @PositionManager ELSE @PositionEmployee END;

            -- Resolve existing user (by username, then by email)
            SET @UserId = (SELECT UserId FROM dbo.Users WHERE Username = @Username);
            IF @UserId IS NULL
            BEGIN
                SET @UserIdByEmail = (SELECT UserId FROM dbo.Users WHERE EmailNormalized = LOWER(LTRIM(RTRIM(@Email))));
                IF @UserIdByEmail IS NOT NULL
                    THROW 52010, 'A demo email address is already used by a different account. Use a different demo email namespace.', 1;
            END

            SET @UserCompanyId = (SELECT CompanyId FROM dbo.Users WHERE UserId = @UserId);
            IF @UserId IS NOT NULL AND @UserCompanyId <> @CompanyId
                THROW 52006, 'A demo username already belongs to a different company. Review that account first.', 1;

            SET @LinkedEmployeeId = (SELECT EmployeeId FROM dbo.Employees WHERE UserId = @UserId);
            SET @LinkedEmployeeCompanyId = (SELECT CompanyId FROM dbo.Employees WHERE EmployeeId = @LinkedEmployeeId);
            IF @LinkedEmployeeId IS NOT NULL AND @LinkedEmployeeCompanyId <> @CompanyId
                THROW 52007, 'A demo account is already linked to an employee in a different company. Review that profile first.', 1;

            SET @CodeEmployeeId = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = @EmployeeCode);
            SET @CodeEmployeeUserId = (SELECT UserId FROM dbo.Employees WHERE EmployeeId = @CodeEmployeeId);
            SET @CodeEmployeeEmailOwner = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND Email = @Email);

            IF @LinkedEmployeeId IS NOT NULL
            BEGIN
                -- The account already owns a profile: reuse it and normalise its employee code.
                IF @CodeEmployeeId IS NOT NULL AND @CodeEmployeeId <> @LinkedEmployeeId
                    THROW 52008, 'The demo EmployeeCode belongs to a different employee profile in this workspace.', 1;
                IF @CodeEmployeeEmailOwner IS NOT NULL AND @CodeEmployeeEmailOwner <> @LinkedEmployeeId
                    THROW 52011, 'The demo employee email belongs to a different employee profile in this workspace.', 1;
                SET @EmployeeId = @LinkedEmployeeId;
            END
            ELSE IF @CodeEmployeeId IS NOT NULL
            BEGIN
                -- Reuse a profile that has no linked account yet.
                IF @CodeEmployeeUserId IS NOT NULL
                    THROW 52009, 'The demo EmployeeCode is already linked to a different account.', 1;
                IF @CodeEmployeeEmailOwner IS NOT NULL AND @CodeEmployeeEmailOwner <> @CodeEmployeeId
                    THROW 52011, 'The demo employee email belongs to a different employee profile in this workspace.', 1;
                SET @EmployeeId = @CodeEmployeeId;
            END
            ELSE
                SET @EmployeeId = NULL;

            -- Upsert the account
            IF @UserId IS NULL
            BEGIN
                INSERT INTO dbo.Users
                    (CompanyId, RoleId, Username, Email, DisplayName, Password,
                     MustChangePassword, ActivatedAt, PasswordChangedAt,
                     IsActive, IsLocked, FailedLoginAttempts, CreatedAt, UpdatedAt)
                VALUES
                    (@CompanyId, @RoleId, @Username, @Email, @FullName, @Password,
                     0, @Now, @Now,
                     1, 0, 0, @Now, @Now);
                SET @UserId = CONVERT(INT, SCOPE_IDENTITY());
            END
            ELSE
                UPDATE dbo.Users
                SET CompanyId = @CompanyId, RoleId = @RoleId, Email = @Email, DisplayName = @FullName,
                    Password = @Password, MustChangePassword = 0,
                    ActivatedAt = COALESCE(ActivatedAt, @Now), PasswordChangedAt = COALESCE(PasswordChangedAt, @Now),
                    IsActive = 1, IsLocked = 0, FailedLoginAttempts = 0, LockedUntil = NULL, UpdatedAt = @Now
                WHERE UserId = @UserId;

            -- Upsert the employee profile (one profile per user)
            IF @EmployeeId IS NULL
                INSERT INTO dbo.Employees
                    (CompanyId, UserId, EmployeeCode, FullName, Email, DepartmentId, PositionId, Status, CreatedAt, UpdatedAt)
                VALUES
                    (@CompanyId, @UserId, @EmployeeCode, @FullName, @Email, @DepartmentId, @PositionId, 'Active', @Now, @Now);
            ELSE
                UPDATE dbo.Employees
                SET UserId = @UserId, EmployeeCode = @EmployeeCode, FullName = @FullName, Email = @Email,
                    DepartmentId = @DepartmentId, PositionId = @PositionId, Status = 'Active', UpdatedAt = @Now
                WHERE EmployeeId = @EmployeeId;

            SET @AcctLoop = @AcctLoop + 1;
        END

        -- Admin has no manager. Employees D05..D15 report to managers D02/D03/D04.
        SET @Manager1 = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = 'D02');
        SET @Manager2 = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = 'D03');
        SET @Manager3 = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = 'D04');

        UPDATE dbo.Employees
        SET ManagerId = NULL
        WHERE CompanyId = @CompanyId;

        UPDATE e
        SET ManagerId = CASE ((CONVERT(INT, RIGHT(e.EmployeeCode, 2)) - 5) % 3)
                            WHEN 0 THEN @Manager1
                            WHEN 1 THEN @Manager2
                            ELSE @Manager3
                        END
        FROM dbo.Employees e
        WHERE e.CompanyId = @CompanyId
          AND (e.EmployeeCode LIKE 'D0[5-9]' OR e.EmployeeCode LIKE 'D1[0-5]');

        -- Shift settings
        IF OBJECT_ID(N'dbo.CompanyShiftSettings', N'U') IS NOT NULL
           AND NOT EXISTS (SELECT 1 FROM dbo.CompanyShiftSettings WHERE CompanyId = @CompanyId)
            INSERT INTO dbo.CompanyShiftSettings
                (CompanyId, ShiftStartTime, ShiftEndTime, LunchBreakMinutes, GracePeriodMinutes,
                 TimesheetCutoffDay, MinHoursHalfDay, MinHoursFullDay, UpdatedAt)
            VALUES
                (@CompanyId, '08:30', '17:30', 60, 0, 5, 4.00, 6.00, @Now);

        -- Leave quota policy
        IF OBJECT_ID(N'dbo.CompanyLeavePolicies', N'U') IS NOT NULL
           AND NOT EXISTS (SELECT 1 FROM dbo.CompanyLeavePolicies WHERE CompanyId = @CompanyId)
            INSERT INTO dbo.CompanyLeavePolicies (CompanyId, QuotaMode, QuotaDays, MaxDaysPerRequest, UpdatedAt)
            VALUES (@CompanyId, 'ANNUAL', 12, 3, @Now);

        SET @WsLoop = @WsLoop + 1;
    END

    COMMIT TRANSACTION;

    SELECT c.WorkspaceCode, c.CompanyName, c.CompanyId,
           COUNT(DISTINCT u.UserId) AS Accounts,
           COUNT(DISTINCT e.EmployeeId) AS Employees
    FROM dbo.Companies c
    LEFT JOIN dbo.Users u ON u.CompanyId = c.CompanyId
    LEFT JOIN dbo.Employees e ON e.CompanyId = c.CompanyId
    WHERE c.WorkspaceCode IN ('demo-alpha','demo-beta','demo-gamma')
    GROUP BY c.WorkspaceCode, c.CompanyName, c.CompanyId
    ORDER BY c.WorkspaceCode;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
