-- Demo-only seed: three demo workspaces with 3 months of coherent HR data.
-- Passwords are intentionally plaintext for this local/demo project only.
-- Run AFTER database/db.sql plus the lifecycle/payroll/profile migrations
-- (payroll-lifecycle-sources.sql, payroll-management.sql, payroll-review-approval.sql,
-- profile-update-periods.sql). The script fails fast if those objects are missing.
-- Additive and re-runnable: existing identities and published payroll are preserved.
USE HRMsDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @Now DATETIME2(7) = SYSUTCDATETIME();
    DECLARE @Today DATE = CONVERT(DATE, DATEADD(HOUR, 7, @Now)); -- Asia/Ho_Chi_Minh (UTC+7)
    DECLARE @CurrentMonth DATE = DATEFROMPARTS(YEAR(@Today), MONTH(@Today), 1);
    DECLARE @FirstMonth DATE = DATEADD(MONTH, -4, @CurrentMonth); -- supplementary payroll month
    DECLARE @Password NVARCHAR(255) = N'12345678';
    DECLARE @LockResult INT;

    EXEC @LockResult = sys.sp_getapplock @Resource = N'Seed.ThreeDemoCompanies.v2',
        @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 10000;
    IF @LockResult < 0 THROW 52000, 'Could not acquire the demo seed lock.', 1;

    IF EXISTS (SELECT 1 FROM (VALUES
            ('EmploymentContracts'), ('EmployeeCompensationHistories'), ('EmployeePositionAssignments'),
            ('ResponsibilityLevels'), ('ResponsibilityAllowancePolicies'), ('CompanyPayrollSettings'),
            ('PayrollPeriods'), ('PayrollAdjustments'), ('Payslips'), ('PayrollNotifications'),
            ('AttendanceExplanations'), ('TimesheetLocks'), ('ProfileUpdatePeriods'),
            ('ProfileUpdatePeriodFields'), ('ProfileUpdatePeriodEmployees'), ('ProfileUpdatePeriodLogs'),
            ('CompanyDocumentTypes'), ('DocumentUpdatePeriods'), ('DocumentUpdatePeriodTypes'),
            ('DocumentUpdatePeriodEmployees'), ('AuditLogs')) r(TableName)
        WHERE OBJECT_ID(CONCAT('dbo.', r.TableName), 'U') IS NULL)
        THROW 52004, 'Required lifecycle/payroll/profile tables are missing. Run the migrations before this seed.', 1;
    IF COL_LENGTH('dbo.Payslips', 'ContractBaseSalary') IS NULL
       OR COL_LENGTH('dbo.Payslips', 'ActualWorkDays') IS NULL
       OR COL_LENGTH('dbo.Payslips', 'StandardWorkDays') IS NULL
       OR COL_LENGTH('dbo.Payslips', 'TravelAllowance') IS NULL
       OR COL_LENGTH('dbo.Positions', 'ResponsibilityLevelId') IS NULL
       OR COL_LENGTH('dbo.EmployeeDocuments', 'DocumentPeriodId') IS NULL
        THROW 52005, 'Required lifecycle/payroll columns are missing. Run the migrations before this seed.', 1;

    DECLARE @DefaultPlanId INT = (SELECT TOP (1) PlanId FROM dbo.Plans WHERE PlanName = N'DEFAULT' AND IsActive = 1 ORDER BY PlanId);
    IF @DefaultPlanId IS NULL
        THROW 52001, 'The active DEFAULT plan is required before seeding demo companies.', 1;

    DECLARE @RoleCompanyAdmin INT = (SELECT RoleId FROM dbo.Roles WHERE RoleCode = 'COMPANY_ADMIN');
    DECLARE @RoleManager INT = (SELECT RoleId FROM dbo.Roles WHERE RoleCode = 'MANAGER');
    DECLARE @RoleEmployee INT = (SELECT RoleId FROM dbo.Roles WHERE RoleCode = 'EMPLOYEE');
    IF @RoleCompanyAdmin IS NULL OR @RoleManager IS NULL OR @RoleEmployee IS NULL
        THROW 52002, 'Required tenant roles are missing. Run the role seed in database/db.sql first.', 1;

    -- ---------------------------------------------------------------------
    -- Static input
    -- ---------------------------------------------------------------------
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

    -- One Company Admin acting as HR, three managers, eleven employees.
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

    -- ---------------------------------------------------------------------
    -- Working variables
    -- ---------------------------------------------------------------------
    DECLARE @CompanyName NVARCHAR(200), @CompanyNameNormalized NVARCHAR(200), @Region VARCHAR(10),
            @ContactEmail VARCHAR(150), @ContactPhone VARCHAR(30), @Address NVARCHAR(300),
            @WorkspaceCode VARCHAR(50), @CompanyId INT;

    DECLARE @DepartmentGeneral INT, @DepartmentTech INT, @DepartmentHr INT;
    DECLARE @PositionAdmin INT, @PositionManager INT, @PositionEmployee INT;
    DECLARE @AccountNo INT, @RoleCode VARCHAR(30), @RoleId INT, @AccountSuffix VARCHAR(30), @DisplaySuffix NVARCHAR(100);
    DECLARE @Username VARCHAR(150), @Email VARCHAR(150), @EmployeeCode VARCHAR(50), @FullName NVARCHAR(150),
            @DepartmentId INT, @PositionId INT;
    DECLARE @UserId INT, @UserCompanyId INT, @UserIdByEmail INT;
    DECLARE @LinkedEmployeeId INT, @LinkedEmployeeCompanyId INT;
    DECLARE @CodeEmployeeId INT, @CodeEmployeeUserId INT, @CodeEmployeeEmailOwner INT;
    DECLARE @EmployeeId INT;
    DECLARE @WsLoop INT = 1, @AcctLoop INT;
    DECLARE @Manager1 INT, @Manager2 INT, @Manager3 INT;
    DECLARE @AdminUserId INT, @LevelExec INT, @LevelLead INT, @LevelStaff INT;
    DECLARE @PayMonth DATE, @PayStatus NVARCHAR(30), @PeriodId INT;
    DECLARE @PeriodStart DATE, @GeneratedAt DATETIME2(7), @ReviewedAt DATETIME2(7),
            @ApprovedAt DATETIME2(7), @PublishedAt DATETIME2(7);
    DECLARE @PendingDate DATE, @ApprovedLeaveDate DATE;
    DECLARE @LatestApprovedMonth DATE = DATEADD(MONTH, -1, @CurrentMonth);
    DECLARE @ProfileClosed BIGINT, @ProfileOpen BIGINT, @DocClosed BIGINT, @DocOpen BIGINT;
    DECLARE @DocTypeIdCard INT, @DocTypeDegree INT, @DocTypeContract INT;

    DECLARE @DemoUsers TABLE (UserId INT PRIMARY KEY);
    DECLARE @DemoEmployees TABLE (EmployeeId INT PRIMARY KEY);
    DECLARE @PayMonths TABLE (PayMonth DATE PRIMARY KEY, PayStatus NVARCHAR(30) NOT NULL);

    -- Next weekday (Mon=0 ... Sun=6 in the day-offset convention used below).
    SET @PendingDate = DATEADD(DAY, 1, @Today);
    SET @PendingDate = CASE DATEDIFF(DAY, '19000101', @PendingDate) % 7
                           WHEN 5 THEN DATEADD(DAY, 3, @Today)  -- Saturday -> Monday
                           WHEN 6 THEN DATEADD(DAY, 2, @Today)  -- Sunday   -> Monday
                           ELSE DATEADD(DAY, 1, @Today) END;

    -- ---------------------------------------------------------------------
    -- Calendar covering the supplementary month and the three demo months
    -- (M-4 .. M-1). Weekdays use a DATEFIRST-independent offset.
    -- ---------------------------------------------------------------------
    IF OBJECT_ID('tempdb..#Seed3Calendar') IS NOT NULL DROP TABLE #Seed3Calendar;
    CREATE TABLE #Seed3Calendar (
        CalendarDate DATE NOT NULL PRIMARY KEY,
        YearNo INT NOT NULL,
        MonthNo INT NOT NULL,
        IsWorkday BIT NOT NULL
    );
    ;WITH cal AS (
        SELECT @FirstMonth AS d
        UNION ALL
        SELECT DATEADD(DAY, 1, d) FROM cal WHERE d < EOMONTH(DATEADD(MONTH, -1, @CurrentMonth))
    )
    INSERT INTO #Seed3Calendar (CalendarDate, YearNo, MonthNo, IsWorkday)
    SELECT d, YEAR(d), MONTH(d), CASE WHEN DATEDIFF(DAY, '19000101', d) % 7 <= 4 THEN 1 ELSE 0 END
    FROM cal
    OPTION (MAXRECURSION 0);

    WHILE @WsLoop <= (SELECT COUNT(*) FROM @Companies)
    BEGIN
        SELECT @WorkspaceCode = WorkspaceCode, @CompanyName = CompanyName, @CompanyNameNormalized = CompanyNameNormalized,
               @Region = Region, @ContactEmail = ContactEmail, @ContactPhone = ContactPhone, @Address = Address
        FROM @Companies WHERE Rn = @WsLoop;

        DELETE FROM @DemoUsers;
        DELETE FROM @DemoEmployees;
        DELETE FROM @PayMonths;
        SET @EmployeeId = NULL;
        SET @UserId = NULL;

        -- -----------------------------------------------------------------
        -- Company + subscription
        -- -----------------------------------------------------------------
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

        IF NOT EXISTS (SELECT 1 FROM dbo.Subscriptions WHERE CompanyId = @CompanyId)
            INSERT INTO dbo.Subscriptions (CompanyId, PlanId, StartDate, Status, CreatedAt)
            VALUES (@CompanyId, @DefaultPlanId, CONVERT(DATE, @Now), N'ACTIVE', @Now);

        -- Refuse to touch a workspace that holds unrelated accounts.
        IF EXISTS (
            SELECT 1 FROM dbo.Users
            WHERE CompanyId = @CompanyId
              AND Username NOT LIKE CONCAT(@WorkspaceCode, '-seed3-%')
        )
            THROW 52003, 'The demo workspace already contains non-demo users. Review it before running this seed.', 1;

        -- -----------------------------------------------------------------
        -- Departments and positions
        -- -----------------------------------------------------------------
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

        SET @PositionAdmin = (SELECT PositionId FROM dbo.Positions WHERE CompanyId = @CompanyId AND PositionTitle = N'Demo Company Administrator');
        IF @PositionAdmin IS NULL
        BEGIN
            INSERT INTO dbo.Positions (CompanyId, PositionTitle, DepartmentId, PositionCode, IsActive, CreatedAt)
            VALUES (@CompanyId, N'Demo Company Administrator', @DepartmentGeneral, 'DEMO-ADMIN', 1, @Now);
            SET @PositionAdmin = CONVERT(INT, SCOPE_IDENTITY());
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

        -- -----------------------------------------------------------------
        -- Accounts and employee profiles (identity-preserving upsert)
        -- -----------------------------------------------------------------
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
            SET @DepartmentId = CASE @RoleCode WHEN 'COMPANY_ADMIN' THEN @DepartmentGeneral ELSE @DepartmentTech END;
            SET @PositionId = CASE @RoleCode WHEN 'COMPANY_ADMIN' THEN @PositionAdmin WHEN 'MANAGER' THEN @PositionManager ELSE @PositionEmployee END;

            SET @UserId = (SELECT UserId FROM dbo.Users WHERE Username = @Username);
            IF @UserId IS NULL
            BEGIN
                SET @UserIdByEmail = (SELECT UserId FROM dbo.Users WHERE EmailNormalized = LOWER(LTRIM(RTRIM(@Email))));
                IF @UserIdByEmail IS NOT NULL
                    THROW 52010, 'A demo email address is already used by a different account.', 1;
            END

            SET @UserCompanyId = (SELECT CompanyId FROM dbo.Users WHERE UserId = @UserId);
            IF @UserId IS NOT NULL AND @UserCompanyId <> @CompanyId
                THROW 52006, 'A demo username already belongs to a different company.', 1;

            SET @LinkedEmployeeId = (SELECT EmployeeId FROM dbo.Employees WHERE UserId = @UserId);
            SET @LinkedEmployeeCompanyId = (SELECT CompanyId FROM dbo.Employees WHERE EmployeeId = @LinkedEmployeeId);
            IF @LinkedEmployeeId IS NOT NULL AND @LinkedEmployeeCompanyId <> @CompanyId
                THROW 52007, 'A demo account is already linked to an employee in a different company.', 1;

            SET @CodeEmployeeId = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = @EmployeeCode);
            SET @CodeEmployeeUserId = (SELECT UserId FROM dbo.Employees WHERE EmployeeId = @CodeEmployeeId);
            SET @CodeEmployeeEmailOwner = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND Email = @Email);

            IF @LinkedEmployeeId IS NOT NULL
            BEGIN
                IF @CodeEmployeeId IS NOT NULL AND @CodeEmployeeId <> @LinkedEmployeeId
                    THROW 52008, 'The demo EmployeeCode belongs to a different employee profile.', 1;
                IF @CodeEmployeeEmailOwner IS NOT NULL AND @CodeEmployeeEmailOwner <> @LinkedEmployeeId
                    THROW 52011, 'The demo employee email belongs to a different employee profile.', 1;
                SET @EmployeeId = @LinkedEmployeeId;
            END
            ELSE IF @CodeEmployeeId IS NOT NULL
            BEGIN
                IF @CodeEmployeeUserId IS NOT NULL
                    THROW 52009, 'The demo EmployeeCode is already linked to a different account.', 1;
                IF @CodeEmployeeEmailOwner IS NOT NULL AND @CodeEmployeeEmailOwner <> @CodeEmployeeId
                    THROW 52011, 'The demo employee email belongs to a different employee profile.', 1;
                SET @EmployeeId = @CodeEmployeeId;
            END
            ELSE
                SET @EmployeeId = NULL;

            IF @UserId IS NULL
            BEGIN
                INSERT INTO dbo.Users
                    (CompanyId, RoleId, Username, Email, DisplayName, Password,
                     MustChangePassword, ActivatedAt, PasswordChangedAt,
                     IsActive, IsLocked, FailedLoginAttempts, CreatedAt, UpdatedAt)
                VALUES
                    (@CompanyId, @RoleId, @Username, @Email, @FullName, @Password,
                     0, @Now, @Now, 1, 0, 0, @Now, @Now);
                SET @UserId = CONVERT(INT, SCOPE_IDENTITY());
            END
            ELSE
                UPDATE dbo.Users
                SET CompanyId = @CompanyId, RoleId = @RoleId, Email = @Email, DisplayName = @FullName,
                    ActivatedAt = COALESCE(ActivatedAt, @Now),
                    PasswordChangedAt = COALESCE(PasswordChangedAt, @Now),
                    UpdatedAt = @Now
                WHERE UserId = @UserId;

            IF @EmployeeId IS NULL
            BEGIN
                INSERT INTO dbo.Employees
                    (CompanyId, UserId, EmployeeCode, FullName, Email, DepartmentId, PositionId, Status, CreatedAt, UpdatedAt)
                VALUES
                    (@CompanyId, @UserId, @EmployeeCode, @FullName, @Email, @DepartmentId, @PositionId, 'Active', @Now, @Now);
                SET @EmployeeId = CONVERT(INT, SCOPE_IDENTITY());
            END
            ELSE
                UPDATE dbo.Employees
                SET UserId = @UserId, EmployeeCode = @EmployeeCode, FullName = @FullName, Email = @Email,
                    DepartmentId = @DepartmentId, PositionId = @PositionId, UpdatedAt = @Now
                WHERE EmployeeId = @EmployeeId;

            INSERT INTO @DemoUsers (UserId) VALUES (@UserId);
            IF NOT EXISTS (SELECT 1 FROM @DemoEmployees WHERE EmployeeId = @EmployeeId)
                INSERT INTO @DemoEmployees (EmployeeId) VALUES (@EmployeeId);

            SET @AcctLoop = @AcctLoop + 1;
        END

        SET @AdminUserId = (SELECT UserId FROM dbo.Users
                            WHERE CompanyId = @CompanyId AND Username = CONCAT(@WorkspaceCode, '-seed3-admin'));
        IF @AdminUserId IS NULL
            THROW 52013, 'The demo Company Admin account could not be resolved.', 1;

        -- Reporting lines: D05..D15 -> D02/D03/D04, managers and admin have none.
        SET @Manager1 = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = 'D02');
        SET @Manager2 = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = 'D03');
        SET @Manager3 = (SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = @CompanyId AND EmployeeCode = 'D04');
        IF @Manager1 IS NULL OR @Manager2 IS NULL OR @Manager3 IS NULL
            THROW 52012, 'The demo manager profiles could not be resolved.', 1;

        UPDATE e
        SET ManagerId = CASE ((CONVERT(INT, RIGHT(e.EmployeeCode, 2)) - 5) % 3)
                            WHEN 0 THEN @Manager1
                            WHEN 1 THEN @Manager2
                            ELSE @Manager3
                        END
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        WHERE e.CompanyId = @CompanyId
          AND (e.EmployeeCode LIKE 'D0[5-9]' OR e.EmployeeCode LIKE 'D1[0-5]');

        UPDATE e SET ManagerId = NULL
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        WHERE e.CompanyId = @CompanyId
          AND e.EmployeeCode IN ('D01','D02','D03','D04');

        -- -----------------------------------------------------------------
        -- Responsibility levels and allowance policies
        -- -----------------------------------------------------------------
        IF NOT EXISTS (SELECT 1 FROM dbo.ResponsibilityLevels WHERE CompanyId = @CompanyId AND LevelCode = 'DEMO-EXEC')
            INSERT INTO dbo.ResponsibilityLevels (CompanyId, LevelCode, LevelName, RankOrder, IsActive, CreatedAt)
            VALUES (@CompanyId, 'DEMO-EXEC', N'Demo Executive', 3, 1, @Now);
        IF NOT EXISTS (SELECT 1 FROM dbo.ResponsibilityLevels WHERE CompanyId = @CompanyId AND LevelCode = 'DEMO-LEAD')
            INSERT INTO dbo.ResponsibilityLevels (CompanyId, LevelCode, LevelName, RankOrder, IsActive, CreatedAt)
            VALUES (@CompanyId, 'DEMO-LEAD', N'Demo Team Lead', 2, 1, @Now);
        IF NOT EXISTS (SELECT 1 FROM dbo.ResponsibilityLevels WHERE CompanyId = @CompanyId AND LevelCode = 'DEMO-STAFF')
            INSERT INTO dbo.ResponsibilityLevels (CompanyId, LevelCode, LevelName, RankOrder, IsActive, CreatedAt)
            VALUES (@CompanyId, 'DEMO-STAFF', N'Demo Staff', 1, 1, @Now);

        SET @LevelExec = (SELECT ResponsibilityLevelId FROM dbo.ResponsibilityLevels WHERE CompanyId = @CompanyId AND LevelCode = 'DEMO-EXEC');
        SET @LevelLead = (SELECT ResponsibilityLevelId FROM dbo.ResponsibilityLevels WHERE CompanyId = @CompanyId AND LevelCode = 'DEMO-LEAD');
        SET @LevelStaff = (SELECT ResponsibilityLevelId FROM dbo.ResponsibilityLevels WHERE CompanyId = @CompanyId AND LevelCode = 'DEMO-STAFF');

        IF NOT EXISTS (SELECT 1 FROM dbo.ResponsibilityAllowancePolicies WHERE CompanyId = @CompanyId AND ResponsibilityLevelId = @LevelExec AND EffectiveFrom = @FirstMonth)
            INSERT INTO dbo.ResponsibilityAllowancePolicies (CompanyId, ResponsibilityLevelId, Amount, EffectiveFrom, CreatedAt, CreatedByUserId)
            VALUES (@CompanyId, @LevelExec, 2500000.00, @FirstMonth, @Now, @AdminUserId);
        IF NOT EXISTS (SELECT 1 FROM dbo.ResponsibilityAllowancePolicies WHERE CompanyId = @CompanyId AND ResponsibilityLevelId = @LevelLead AND EffectiveFrom = @FirstMonth)
            INSERT INTO dbo.ResponsibilityAllowancePolicies (CompanyId, ResponsibilityLevelId, Amount, EffectiveFrom, CreatedAt, CreatedByUserId)
            VALUES (@CompanyId, @LevelLead, 1500000.00, @FirstMonth, @Now, @AdminUserId);
        IF NOT EXISTS (SELECT 1 FROM dbo.ResponsibilityAllowancePolicies WHERE CompanyId = @CompanyId AND ResponsibilityLevelId = @LevelStaff AND EffectiveFrom = @FirstMonth)
            INSERT INTO dbo.ResponsibilityAllowancePolicies (CompanyId, ResponsibilityLevelId, Amount, EffectiveFrom, CreatedAt, CreatedByUserId)
            VALUES (@CompanyId, @LevelStaff, 0.00, @FirstMonth, @Now, @AdminUserId);

        UPDATE dbo.Positions
        SET ResponsibilityLevelId = CASE PositionCode
                WHEN 'DEMO-ADMIN' THEN @LevelExec WHEN 'DEMO-MGR' THEN @LevelLead ELSE @LevelStaff END
        WHERE CompanyId = @CompanyId AND PositionCode IN ('DEMO-ADMIN','DEMO-MGR','DEMO-EMP');

        -- -----------------------------------------------------------------
        -- Employee profile details
        -- -----------------------------------------------------------------
        UPDATE e
        SET Phone = CONCAT('09', RIGHT(CONCAT('00000000', e.EmployeeId), 8)),
            Gender = CASE WHEN e.EmployeeId % 2 = 0 THEN N'Nam' ELSE N'Nữ' END,
            DateOfBirth = DATEADD(YEAR, -(24 + e.EmployeeId % 12), @Today),
            IdentityNumber = CONCAT('079', RIGHT(CONCAT('000000000', e.EmployeeId), 9)),
            IdentityIssuedDate = DATEADD(YEAR, -3, @Today),
            IdentityIssuedPlace = N'Cục Cảnh sát QLHC về TTXH',
            PermanentAddress = CONCAT(N'Số ', 10 + e.EmployeeId % 80, N' Đường Demo, Việt Nam'),
            Hometown = N'Việt Nam',
            Major = CASE WHEN e.EmployeeCode IN ('D02','D03','D04') THEN N'Quản trị kinh doanh' ELSE N'Công nghệ thông tin' END,
            IntendedContractType = CASE WHEN e.EmployeeCode = 'D05' THEN 'PROBATION'
                                        WHEN e.EmployeeCode = 'D06' THEN 'FIXED_TERM'
                                        ELSE 'INDEFINITE' END,
            BaseSalary = CASE
                WHEN e.EmployeeCode = 'D01' THEN 30000000.00
                WHEN e.EmployeeCode IN ('D02','D03','D04') THEN 22000000.00
                WHEN e.EmployeeCode IN ('D05','D06','D07','D08','D09','D10') THEN 14000000.00
                ELSE 12000000.00 END,
            UpdatedAt = @Now
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        WHERE e.CompanyId = @CompanyId;

        -- -----------------------------------------------------------------
        -- Contracts, compensation history and position assignments
        -- -----------------------------------------------------------------
        INSERT INTO dbo.EmploymentContracts
            (CompanyId, EmployeeId, ContractCode, ContractType, StartDate, EndDate, GrossSalary, Status,
             SignedDate, CreatedByUserId, CreatedAt, Notes)
        SELECT @CompanyId, e.EmployeeId, CONCAT('SEED3-ACTIVE-', e.EmployeeCode), e.IntendedContractType,
               @FirstMonth,
               CASE WHEN e.EmployeeCode = 'D06' THEN EOMONTH(DATEADD(MONTH, 6, @CurrentMonth)) END,
               e.BaseSalary, 'ACTIVE', @FirstMonth, @AdminUserId, @Now,
               N'Seed3 demo employment contract'
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        WHERE e.CompanyId = @CompanyId
          AND NOT EXISTS (SELECT 1 FROM dbo.EmploymentContracts c
                          WHERE c.CompanyId = e.CompanyId AND c.ContractCode = CONCAT('SEED3-ACTIVE-', e.EmployeeCode));

        INSERT INTO dbo.EmploymentContracts
            (CompanyId, EmployeeId, ContractCode, ContractType, StartDate, EndDate, GrossSalary, Status,
             SignedDate, TerminatedDate, TerminationReason, CreatedByUserId, CreatedAt, Notes)
        SELECT @CompanyId, e.EmployeeId, x.ContractCode, x.ContractType,
               DATEADD(MONTH, x.StartOffset, @CurrentMonth), DATEADD(MONTH, x.EndOffset, @CurrentMonth),
               e.BaseSalary, x.Status, DATEADD(MONTH, x.StartOffset, @CurrentMonth),
               CASE WHEN x.Status = 'TERMINATED' THEN DATEADD(MONTH, x.EndOffset, @CurrentMonth) END,
               CASE WHEN x.Status = 'TERMINATED' THEN N'Chấm dứt theo thỏa thuận' END,
               @AdminUserId, @Now, x.Note
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        CROSS APPLY (VALUES
            ('D07', 'SEED3-EXPIRED-D07',   'FIXED_TERM', -6, -5, 'EXPIRED',    N'Hợp đồng cũ đã hết hạn'),
            ('D08', 'SEED3-TERMINATED-D08','INDEFINITE', -6, -5, 'TERMINATED', N'Hợp đồng cũ đã chấm dứt'),
            ('D09', 'SEED3-CANCELLED-D09', 'PROBATION',  -6, -5, 'CANCELLED',  N'Hợp đồng nháp đã hủy'),
            ('D10', 'SEED3-DRAFT-D10',     'FIXED_TERM',  1, 13, 'DRAFT',      N'Hợp đồng nháp tương lai')
        ) x(EmployeeCode, ContractCode, ContractType, StartOffset, EndOffset, Status, Note)
        WHERE e.CompanyId = @CompanyId AND e.EmployeeCode = x.EmployeeCode
          AND NOT EXISTS (SELECT 1 FROM dbo.EmploymentContracts c
                          WHERE c.CompanyId = e.CompanyId AND c.ContractCode = x.ContractCode);

        INSERT INTO dbo.EmployeeCompensationHistories
            (CompanyId, EmployeeId, BaseSalary, EffectiveFrom, EffectiveTo, Reason, SourceContractId,
             TravelAllowance, CreatedAt, CreatedByUserId)
        SELECT @CompanyId, e.EmployeeId, e.BaseSalary, @FirstMonth, NULL, N'Seed3 initial salary',
               c.ContractId,
               CASE WHEN e.EmployeeCode LIKE 'D1[0-5]' OR e.EmployeeCode LIKE 'D0[5-9]' THEN 300000.00 ELSE 0.00 END,
               @Now, @AdminUserId
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        JOIN dbo.EmploymentContracts c ON c.CompanyId = e.CompanyId AND c.EmployeeId = e.EmployeeId
             AND c.ContractCode = CONCAT('SEED3-ACTIVE-', e.EmployeeCode)
        WHERE e.CompanyId = @CompanyId
          AND NOT EXISTS (SELECT 1 FROM dbo.EmployeeCompensationHistories h
                          WHERE h.CompanyId = e.CompanyId AND h.EmployeeId = e.EmployeeId);

        INSERT INTO dbo.EmployeePositionAssignments
            (CompanyId, EmployeeId, DepartmentId, PositionId, ManagerId, EffectiveFrom, EffectiveTo,
             ChangeType, Reason, CreatedAt)
        SELECT @CompanyId, e.EmployeeId, e.DepartmentId, e.PositionId, e.ManagerId, @FirstMonth, NULL,
               'BASELINE', N'Seed3 baseline assignment', @Now
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        WHERE e.CompanyId = @CompanyId
          AND NOT EXISTS (SELECT 1 FROM dbo.EmployeePositionAssignments a
                          WHERE a.CompanyId = e.CompanyId AND a.EmployeeId = e.EmployeeId);

        -- -----------------------------------------------------------------
        -- Company settings
        -- -----------------------------------------------------------------
        IF NOT EXISTS (SELECT 1 FROM dbo.CompanyShiftSettings WHERE CompanyId = @CompanyId)
            INSERT INTO dbo.CompanyShiftSettings
                (CompanyId, ShiftStartTime, ShiftEndTime, LunchBreakMinutes, GracePeriodMinutes,
                 TimesheetCutoffDay, MinHoursHalfDay, MinHoursFullDay, UpdatedAt, UpdatedByUserId)
            VALUES (@CompanyId, '08:30', '17:30', 60, 0, 5, 4.00, 6.00, @Now, @AdminUserId);

        IF NOT EXISTS (SELECT 1 FROM dbo.CompanyLeavePolicies WHERE CompanyId = @CompanyId)
            INSERT INTO dbo.CompanyLeavePolicies (CompanyId, QuotaMode, QuotaDays, MaxDaysPerRequest, UpdatedByUserId, UpdatedAt)
            VALUES (@CompanyId, 'ANNUAL', 12, 3, @AdminUserId, @Now);

        IF NOT EXISTS (SELECT 1 FROM dbo.CompanyPayrollSettings WHERE CompanyId = @CompanyId)
            INSERT INTO dbo.CompanyPayrollSettings
                (CompanyId, SalaryCalculationMode, StandardDaysMode, IncludePaidLeave,
                 IncludeResponsibilityAllowance, IncludeTravelAllowance, UpdatedAt, UpdatedByUserId)
            VALUES (@CompanyId, 'WORKDAYS', 'MON_FRI', 1, 1, 1, @Now, @AdminUserId);

        -- -----------------------------------------------------------------
        -- Leave requests (weekday date in month M-2, plus workflow samples)
        -- -----------------------------------------------------------------
        SET @ApprovedLeaveDate = (
            SELECT MIN(c.CalendarDate)
            FROM #Seed3Calendar c
            WHERE c.IsWorkday = 1
              AND c.YearNo = YEAR(DATEADD(MONTH, -2, @CurrentMonth))
              AND c.MonthNo = MONTH(DATEADD(MONTH, -2, @CurrentMonth))
              AND c.CalendarDate >= DATEADD(DAY, 2, DATEADD(MONTH, -2, @CurrentMonth)));

        INSERT INTO dbo.LeaveRequests
            (CompanyId, EmployeeId, StartDate, EndDate, TotalDays, Reason, Status,
             ApproverId, ApproverComment, ReviewedAt, CreatedAt, UpdatedAt)
        SELECT @CompanyId, e.EmployeeId, @ApprovedLeaveDate, @ApprovedLeaveDate, 1,
               N'Việc gia đình (demo)', 'Approved',
               CASE WHEN e.EmployeeCode IN ('D02','D03','D04') THEN @AdminUserId ELSE m.UserId END,
               N'Đã duyệt (demo)', DATEADD(DAY, -5, @ApprovedLeaveDate), DATEADD(DAY, -7, @ApprovedLeaveDate), DATEADD(DAY, -5, @ApprovedLeaveDate)
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        LEFT JOIN dbo.Employees m ON m.EmployeeId = e.ManagerId
        WHERE e.CompanyId = @CompanyId
          AND e.EmployeeCode <> 'D01'
          AND @ApprovedLeaveDate IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM dbo.LeaveRequests lr
                          WHERE lr.CompanyId = e.CompanyId AND lr.EmployeeId = e.EmployeeId
                            AND lr.Status = 'Approved'
                            AND lr.StartDate >= DATEADD(MONTH, -3, @CurrentMonth));

        -- Pending (future weekday) for the interactive approval screen.
        INSERT INTO dbo.LeaveRequests
            (CompanyId, EmployeeId, StartDate, EndDate, TotalDays, Reason, Status,
             ApproverId, CreatedAt, UpdatedAt)
        SELECT @CompanyId, e.EmployeeId, @PendingDate, @PendingDate, 1, N'Việc cá nhân (demo)', 'Pending',
               m.UserId, @Now, @Now
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        JOIN dbo.Employees m ON m.EmployeeId = e.ManagerId
        WHERE e.CompanyId = @CompanyId AND e.EmployeeCode = 'D05'
          AND NOT EXISTS (SELECT 1 FROM dbo.LeaveRequests lr
                          WHERE lr.CompanyId = e.CompanyId AND lr.EmployeeId = e.EmployeeId AND lr.Status = 'Pending');

        -- Rejected sample.
        INSERT INTO dbo.LeaveRequests
            (CompanyId, EmployeeId, StartDate, EndDate, TotalDays, Reason, Status,
             ApproverId, ApproverComment, ReviewedAt, CreatedAt, UpdatedAt)
        SELECT @CompanyId, e.EmployeeId,
               DATEADD(DAY, 2, DATEADD(MONTH, -1, @CurrentMonth)), DATEADD(DAY, 2, DATEADD(MONTH, -1, @CurrentMonth)),
               1, N'Nghỉ không có lý do rõ ràng (demo)', 'Rejected',
               m.UserId, N'Không đủ căn cứ (demo)', @Now, @Now, @Now
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        JOIN dbo.Employees m ON m.EmployeeId = e.ManagerId
        WHERE e.CompanyId = @CompanyId AND e.EmployeeCode = 'D05'
          AND NOT EXISTS (SELECT 1 FROM dbo.LeaveRequests lr
                          WHERE lr.CompanyId = e.CompanyId AND lr.EmployeeId = e.EmployeeId AND lr.Status = 'Rejected');

        -- Cancelled sample.
        INSERT INTO dbo.LeaveRequests
            (CompanyId, EmployeeId, StartDate, EndDate, TotalDays, Reason, Status,
             ApproverId, ApproverComment, ReviewedAt, CreatedAt, UpdatedAt)
        SELECT @CompanyId, e.EmployeeId,
               DATEADD(DAY, 5, DATEADD(MONTH, -1, @CurrentMonth)), DATEADD(DAY, 5, DATEADD(MONTH, -1, @CurrentMonth)),
               1, N'Đã hủy đơn (demo)', 'Cancelled',
               e.UserId, N'Nhân viên tự hủy (demo)', @Now, @Now, @Now
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        WHERE e.CompanyId = @CompanyId AND e.EmployeeCode = 'D06'
          AND NOT EXISTS (SELECT 1 FROM dbo.LeaveRequests lr
                          WHERE lr.CompanyId = e.CompanyId AND lr.EmployeeId = e.EmployeeId AND lr.Status = 'Cancelled');

        -- Canonical leave dates (one row per request date).
        INSERT INTO dbo.LeaveRequestDates (LeaveRequestId, LeaveDate)
        SELECT lr.LeaveRequestId, lr.StartDate
        FROM dbo.LeaveRequests lr
        WHERE lr.CompanyId = @CompanyId
          AND NOT EXISTS (SELECT 1 FROM dbo.LeaveRequestDates ld WHERE ld.LeaveRequestId = lr.LeaveRequestId);

        -- -----------------------------------------------------------------
        -- Attendance
        -- -----------------------------------------------------------------
        -- Approved leave becomes an On Leave day (no punches, zero hours).
        INSERT INTO dbo.Attendances
            (CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime, TotalHours, Status, Note)
        SELECT DISTINCT @CompanyId, lr.EmployeeId, ld.LeaveDate, NULL, NULL, 0.00, 'On Leave', N'Nghỉ phép đã duyệt (demo)'
        FROM dbo.LeaveRequests lr
        JOIN dbo.LeaveRequestDates ld ON ld.LeaveRequestId = lr.LeaveRequestId
        JOIN @DemoEmployees d ON d.EmployeeId = lr.EmployeeId
        WHERE lr.CompanyId = @CompanyId AND lr.Status = 'Approved'
          AND NOT EXISTS (SELECT 1 FROM dbo.Attendances a
                          WHERE a.EmployeeId = lr.EmployeeId AND a.AttendanceDate = ld.LeaveDate);

        -- Worked / absent days across the supplementary month and demo months.
        ;WITH EmpSeed AS (
            SELECT e.EmployeeId, ROW_NUMBER() OVER (ORDER BY e.EmployeeCode) AS Rn
            FROM dbo.Employees e
            JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
            WHERE e.CompanyId = @CompanyId
        ),
        DaySeed AS (
            SELECT s.EmployeeId, s.Rn, c.CalendarDate,
                   (s.Rn * 7 + DAY(c.CalendarDate)) % 20 AS Marker
            FROM EmpSeed s
            CROSS JOIN #Seed3Calendar c
            WHERE c.IsWorkday = 1
        )
        INSERT INTO dbo.Attendances
            (CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime, TotalHours, Status, Note)
        SELECT @CompanyId, ds.EmployeeId, ds.CalendarDate,
               CASE ds.Marker WHEN 0 THEN NULL WHEN 1 THEN '09:05' WHEN 2 THEN '08:25' ELSE '08:25' END,
               CASE ds.Marker WHEN 0 THEN NULL WHEN 1 THEN '17:35' WHEN 2 THEN '15:30' ELSE '17:35' END,
               CASE ds.Marker WHEN 0 THEN 0.00 WHEN 1 THEN 7.50 WHEN 2 THEN 6.08 ELSE 8.17 END,
               CASE ds.Marker WHEN 0 THEN 'Absent' WHEN 1 THEN 'Late' WHEN 2 THEN 'Early Leave' ELSE 'Present' END,
               CASE ds.Marker WHEN 0 THEN N'Vắng không phép (demo)' ELSE N'Chấm công demo' END
        FROM DaySeed ds
        WHERE NOT EXISTS (SELECT 1 FROM dbo.Attendances a
                          WHERE a.EmployeeId = ds.EmployeeId AND a.AttendanceDate = ds.CalendarDate);

        -- Attendance explanations covering every supported status.
        INSERT INTO dbo.AttendanceExplanations
            (CompanyId, EmployeeId, AttendanceDate, ProposedCheckIn, ProposedCheckOut,
             ReasonType, ReasonDetail, Status, ReviewerId, ReviewerComment, ReviewedAt, CreatedAt)
        SELECT @CompanyId, e.EmployeeId, DATEADD(DAY, 1, DATEADD(MONTH, -1, @CurrentMonth)),
               '08:30', '17:30', N'FORGOT_PUNCH', N'Quên chấm công (demo)', 'APPROVED',
               m.UserId, N'Đã xác nhận (demo)', @Now, @Now
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        JOIN dbo.Employees m ON m.EmployeeId = e.ManagerId
        WHERE e.CompanyId = @CompanyId AND e.EmployeeCode = 'D07'
          AND NOT EXISTS (SELECT 1 FROM dbo.AttendanceExplanations x
                          WHERE x.CompanyId = e.CompanyId AND x.EmployeeId = e.EmployeeId);

        INSERT INTO dbo.AttendanceExplanations
            (CompanyId, EmployeeId, AttendanceDate, ProposedCheckIn, ProposedCheckOut,
             ReasonType, ReasonDetail, Status, ReviewerId, ReviewerComment, ReviewedAt, CreatedAt)
        SELECT @CompanyId, e.EmployeeId, DATEADD(DAY, 2, DATEADD(MONTH, -1, @CurrentMonth)),
               '08:30', '17:30', N'TECHNICAL_ISSUE', N'Máy chấm công lỗi (demo)', 'PENDING',
               NULL, NULL, NULL, @Now
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        WHERE e.CompanyId = @CompanyId AND e.EmployeeCode = 'D08'
          AND NOT EXISTS (SELECT 1 FROM dbo.AttendanceExplanations x
                          WHERE x.CompanyId = e.CompanyId AND x.EmployeeId = e.EmployeeId);

        INSERT INTO dbo.AttendanceExplanations
            (CompanyId, EmployeeId, AttendanceDate, ProposedCheckIn, ProposedCheckOut,
             ReasonType, ReasonDetail, Status, ReviewerId, ReviewerComment, ReviewedAt, CreatedAt)
        SELECT @CompanyId, e.EmployeeId, DATEADD(DAY, 3, DATEADD(MONTH, -1, @CurrentMonth)),
               '08:30', '17:30', N'OTHER', N'Giải trình không hợp lệ (demo)', 'REJECTED',
               m.UserId, N'Không chấp nhận (demo)', @Now, @Now
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        JOIN dbo.Employees m ON m.EmployeeId = e.ManagerId
        WHERE e.CompanyId = @CompanyId AND e.EmployeeCode = 'D09'
          AND NOT EXISTS (SELECT 1 FROM dbo.AttendanceExplanations x
                          WHERE x.CompanyId = e.CompanyId AND x.EmployeeId = e.EmployeeId);

        INSERT INTO dbo.AttendanceExplanations
            (CompanyId, EmployeeId, AttendanceDate, ProposedCheckIn, ProposedCheckOut,
             ReasonType, ReasonDetail, Status, ReviewerId, ReviewerComment, ReviewedAt, CreatedAt)
        SELECT @CompanyId, e.EmployeeId, DATEADD(DAY, 4, DATEADD(MONTH, -1, @CurrentMonth)),
               NULL, NULL, N'BUSINESS_TRIP', N'Công tác rồi hủy (demo)', 'CANCELLED',
               e.UserId, N'Nhân viên hủy (demo)', @Now, @Now
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        WHERE e.CompanyId = @CompanyId AND e.EmployeeCode = 'D10'
          AND NOT EXISTS (SELECT 1 FROM dbo.AttendanceExplanations x
                          WHERE x.CompanyId = e.CompanyId AND x.EmployeeId = e.EmployeeId);

        -- Timesheet locks for the three demo months (supplementary month stays open).
        INSERT INTO dbo.TimesheetLocks (CompanyId, [Month], [Year], IsLocked, LockedByUserId, LockedAt, Notes)
        SELECT @CompanyId, m.MonthNo, m.YearNo, 1, @AdminUserId, @Now, N'Chốt công demo'
        FROM (
            SELECT MONTH(DATEADD(MONTH, -1, @CurrentMonth)) AS MonthNo, YEAR(DATEADD(MONTH, -1, @CurrentMonth)) AS YearNo
            UNION ALL
            SELECT MONTH(DATEADD(MONTH, -2, @CurrentMonth)), YEAR(DATEADD(MONTH, -2, @CurrentMonth))
            UNION ALL
            SELECT MONTH(DATEADD(MONTH, -3, @CurrentMonth)), YEAR(DATEADD(MONTH, -3, @CurrentMonth))
        ) m
        WHERE NOT EXISTS (SELECT 1 FROM dbo.TimesheetLocks t
                          WHERE t.CompanyId = @CompanyId AND t.[Month] = m.MonthNo AND t.[Year] = m.YearNo);

        -- -----------------------------------------------------------------
        -- Payroll: three published months + one supplementary workflow month
        --   demo-alpha : M-4 Draft       (no payslips)
        --   demo-beta  : M-4 Calculated  (payslips)
        --   demo-gamma : M-4 Reviewed    (payslips)
        -- -----------------------------------------------------------------
        INSERT INTO @PayMonths (PayMonth, PayStatus) VALUES
            (DATEADD(MONTH, -1, @CurrentMonth), 'Approved'),
            (DATEADD(MONTH, -2, @CurrentMonth), 'Approved'),
            (DATEADD(MONTH, -3, @CurrentMonth), 'Approved'),
            (DATEADD(MONTH, -4, @CurrentMonth),
             CASE @WorkspaceCode WHEN 'demo-alpha' THEN 'Draft'
                                 WHEN 'demo-beta' THEN 'Calculated'
                                 ELSE 'Reviewed' END);

        WHILE EXISTS (SELECT 1 FROM @PayMonths)
        BEGIN
            SELECT TOP (1) @PayMonth = PayMonth FROM @PayMonths ORDER BY PayMonth DESC;
            SET @PayStatus = (SELECT PayStatus FROM @PayMonths WHERE PayMonth = @PayMonth);
            SET @PeriodId = NULL;

            SET @PeriodStart = DATEADD(MONTH, 1, @PayMonth);
            SET @GeneratedAt = DATEADD(DAY, 4, @PeriodStart);
            SET @ReviewedAt  = DATEADD(DAY, 5, @PeriodStart);
            SET @ApprovedAt  = DATEADD(DAY, 6, @PeriodStart);
            SET @PublishedAt = DATEADD(DAY, 7, @PeriodStart);

            IF NOT EXISTS (SELECT 1 FROM dbo.PayrollPeriods
                           WHERE CompanyId = @CompanyId AND [Month] = MONTH(@PayMonth) AND [Year] = YEAR(@PayMonth))
                INSERT INTO dbo.PayrollPeriods
                    (CompanyId, PeriodName, [Month], [Year], Status, ApprovedByUserId, ApprovedAt,
                     ReviewedByUserId, ReviewedAt, IsClosed, PublishedAt, PublishedByUserId, CreatedAt)
                VALUES
                    (@CompanyId,
                     CONCAT(N'Bảng lương tháng ', RIGHT('0' + CONVERT(VARCHAR(2), MONTH(@PayMonth)), 2), '/', CONVERT(VARCHAR(4), YEAR(@PayMonth))),
                     MONTH(@PayMonth), YEAR(@PayMonth), @PayStatus,
                     CASE WHEN @PayStatus = 'Approved' THEN @AdminUserId END,
                     CASE WHEN @PayStatus = 'Approved' THEN @ApprovedAt END,
                     CASE WHEN @PayStatus IN ('Reviewed','Approved') THEN @AdminUserId END,
                     CASE WHEN @PayStatus IN ('Reviewed','Approved') THEN @ReviewedAt END,
                     CASE WHEN @PayStatus = 'Approved' THEN 1 ELSE 0 END,
                     CASE WHEN @PayStatus = 'Approved' THEN @PublishedAt END,
                     CASE WHEN @PayStatus = 'Approved' THEN @AdminUserId END,
                     @GeneratedAt);

            SELECT @PeriodId = PayrollPeriodId, @PayStatus = Status
            FROM dbo.PayrollPeriods
            WHERE CompanyId = @CompanyId AND [Month] = MONTH(@PayMonth) AND [Year] = YEAR(@PayMonth);

            IF @PeriodId IS NOT NULL AND @PayStatus <> 'Draft'
            BEGIN
                INSERT INTO dbo.Payslips
                    (PayrollPeriodId, EmployeeId, BaseSalary, Bonus, Deduction, ResponsibilityAllowance,
                     ContractBaseSalary, ActualWorkDays, StandardWorkDays, TravelAllowance, GeneratedAt)
                SELECT @PeriodId, e.EmployeeId,
                       ROUND(sal.MonthlySalary * work.Credits / NULLIF(std.StandardDays, 0), 2),
                       CASE WHEN @PayMonth = @LatestApprovedMonth AND e.EmployeeCode IN ('D05','D06')
                            THEN 1000000.00 ELSE 0.00 END,
                       0.00,
                       sal.ResponsibilityAllowance,
                       sal.MonthlySalary,
                       work.Credits,
                       std.StandardDays,
                       sal.TravelAllowance,
                       @GeneratedAt
                FROM dbo.Employees e
                JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
                CROSS APPLY (SELECT
                    CASE WHEN e.EmployeeCode = 'D01' THEN 30000000.00
                         WHEN e.EmployeeCode IN ('D02','D03','D04') THEN 22000000.00
                         WHEN e.EmployeeCode IN ('D05','D06','D07','D08','D09','D10') THEN 14000000.00
                         ELSE 12000000.00 END AS MonthlySalary,
                    CASE WHEN e.EmployeeCode = 'D01' THEN 2500000.00
                         WHEN e.EmployeeCode IN ('D02','D03','D04') THEN 1500000.00
                         ELSE 0.00 END AS ResponsibilityAllowance,
                    CASE WHEN e.EmployeeCode IN ('D01','D02','D03','D04') THEN 0.00 ELSE 300000.00 END AS TravelAllowance
                ) sal
                CROSS APPLY (
                    SELECT CAST(COALESCE(SUM(CASE
                               WHEN a.Status IN ('Present','Late','Early Leave') AND a.TotalHours >= 6 THEN 1.0
                               WHEN a.Status IN ('Present','Late','Early Leave') AND a.TotalHours > 0 THEN 0.5
                               WHEN a.Status = 'On Leave' THEN 1.0
                               ELSE 0 END), 0) AS DECIMAL(5,2)) AS Credits
                    FROM dbo.Attendances a
                    WHERE a.CompanyId = e.CompanyId AND a.EmployeeId = e.EmployeeId
                      AND a.AttendanceDate >= DATEFROMPARTS(YEAR(@PayMonth), MONTH(@PayMonth), 1)
                      AND a.AttendanceDate <= EOMONTH(@PayMonth)
                ) work
                CROSS APPLY (
                    SELECT CAST(COUNT(*) AS DECIMAL(5,2)) AS StandardDays
                    FROM #Seed3Calendar c
                    WHERE c.IsWorkday = 1 AND c.YearNo = YEAR(@PayMonth) AND c.MonthNo = MONTH(@PayMonth)
                ) std
                WHERE e.CompanyId = @CompanyId
                  AND NOT EXISTS (SELECT 1 FROM dbo.Payslips ps
                                  WHERE ps.PayrollPeriodId = @PeriodId AND ps.EmployeeId = e.EmployeeId);
            END

            -- Bonus adjustments mirror the payslip snapshot for the latest month.
            IF @PayStatus = 'Approved' AND @PayMonth = @LatestApprovedMonth
                INSERT INTO dbo.PayrollAdjustments
                    (PayrollPeriodId, EmployeeId, Bonus, Deduction, Note, CreatedByUserId, CreatedAt)
                SELECT @PeriodId, e.EmployeeId, 1000000.00, 0.00, N'Thưởng hiệu suất (demo)', @AdminUserId, @GeneratedAt
                FROM dbo.Employees e
                JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
                WHERE e.CompanyId = @CompanyId AND e.EmployeeCode IN ('D05','D06')
                  AND NOT EXISTS (SELECT 1 FROM dbo.PayrollAdjustments pa
                                  WHERE pa.PayrollPeriodId = @PeriodId AND pa.EmployeeId = e.EmployeeId);

            -- Publication notifications for every published payslip.
            IF @PayStatus = 'Approved'
                INSERT INTO dbo.PayrollNotifications (PayslipId, EmployeeId, CompanyId, CreatedAt, ReadAt)
                SELECT ps.PayslipId, ps.EmployeeId, @CompanyId, @PublishedAt, NULL
                FROM dbo.Payslips ps
                WHERE ps.PayrollPeriodId = @PeriodId
                  AND NOT EXISTS (SELECT 1 FROM dbo.PayrollNotifications pn WHERE pn.PayslipId = ps.PayslipId);

            DELETE FROM @PayMonths WHERE PayMonth = @PayMonth;
        END

        -- -----------------------------------------------------------------
        -- Profile update campaign and history
        -- -----------------------------------------------------------------
        SET @ProfileClosed = (SELECT PeriodId FROM dbo.ProfileUpdatePeriods
                              WHERE CompanyId = @CompanyId AND Title = N'Đợt cập nhật hồ sơ (demo - đã đóng)');
        IF @ProfileClosed IS NULL
        BEGIN
            INSERT INTO dbo.ProfileUpdatePeriods
                (CompanyId, Title, StartsAt, EndsAt, ClosedAt, CreatedByUserId, CreatedAt, PeriodType)
            VALUES
                (@CompanyId, N'Đợt cập nhật hồ sơ (demo - đã đóng)',
                 DATETIME2FROMPARTS(YEAR(DATEADD(MONTH,-3,@CurrentMonth)), MONTH(DATEADD(MONTH,-3,@CurrentMonth)), 1, 0,0,0,0,7),
                 DATETIME2FROMPARTS(YEAR(@CurrentMonth), MONTH(@CurrentMonth), 1, 0,0,0,0,7),
                 @Now, @AdminUserId, @Now, 'PROFILE');
            SET @ProfileClosed = CONVERT(BIGINT, SCOPE_IDENTITY());
        END

        SET @ProfileOpen = (SELECT PeriodId FROM dbo.ProfileUpdatePeriods
                            WHERE CompanyId = @CompanyId AND Title = N'Đợt cập nhật hồ sơ (demo - đang mở)');
        IF @ProfileOpen IS NULL
        BEGIN
            INSERT INTO dbo.ProfileUpdatePeriods
                (CompanyId, Title, StartsAt, EndsAt, ClosedAt, CreatedByUserId, CreatedAt, PeriodType)
            VALUES
                (@CompanyId, N'Đợt cập nhật hồ sơ (demo - đang mở)',
                 DATETIME2FROMPARTS(YEAR(@CurrentMonth), MONTH(@CurrentMonth), 1, 0,0,0,0,7),
                 DATETIME2FROMPARTS(YEAR(DATEADD(MONTH,2,@CurrentMonth)), MONTH(DATEADD(MONTH,2,@CurrentMonth)), 1, 0,0,0,0,7),
                 NULL, @AdminUserId, @Now, 'PROFILE');
            SET @ProfileOpen = CONVERT(BIGINT, SCOPE_IDENTITY());
        END

        INSERT INTO dbo.ProfileUpdatePeriodFields (PeriodId, FieldKey)
        SELECT @ProfileOpen, f.FieldKey
        FROM (VALUES ('phone'), ('permanentAddress'), ('hometown')) f(FieldKey)
        WHERE NOT EXISTS (SELECT 1 FROM dbo.ProfileUpdatePeriodFields x
                          WHERE x.PeriodId = @ProfileOpen AND x.FieldKey = f.FieldKey);

        INSERT INTO dbo.ProfileUpdatePeriodLogs
            (PeriodId, CompanyId, EmployeeId, FieldKey, FieldLabel, OldValue, NewValue, UpdatedAt)
        SELECT @ProfileClosed, @CompanyId, e.EmployeeId, v.FieldKey, v.FieldLabel, v.OldValue, v.NewValue,
               DATEADD(DAY, v.DayOffset, DATETIME2FROMPARTS(YEAR(@CurrentMonth), MONTH(@CurrentMonth), 1, 9,0,0,0,7))
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        CROSS APPLY (VALUES
            ('phone', N'Số điện thoại', N'0900000000', CONCAT('09', RIGHT(CONCAT('00000000', e.EmployeeId), 8)), -20),
            ('permanentAddress', N'Địa chỉ thường trú', N'Chưa cập nhật',
             CONCAT(N'Số ', 10 + e.EmployeeId % 80, N' Đường Demo, Việt Nam'), -18)
        ) v(FieldKey, FieldLabel, OldValue, NewValue, DayOffset)
        WHERE e.CompanyId = @CompanyId
          AND (e.EmployeeCode LIKE 'D0[5-9]' OR e.EmployeeCode LIKE 'D1[0-5]')
          AND NOT EXISTS (SELECT 1 FROM dbo.ProfileUpdatePeriodLogs l
                          WHERE l.PeriodId = @ProfileClosed AND l.EmployeeId = e.EmployeeId);

        -- -----------------------------------------------------------------
        -- Document types, document campaign and employee documents
        -- -----------------------------------------------------------------
        IF NOT EXISTS (SELECT 1 FROM dbo.CompanyDocumentTypes WHERE CompanyId = @CompanyId AND TypeName = N'CCCD/CMND')
            INSERT INTO dbo.CompanyDocumentTypes (CompanyId, TypeName, IsActive, CreatedAt)
            VALUES (@CompanyId, N'CCCD/CMND', 1, @Now);
        IF NOT EXISTS (SELECT 1 FROM dbo.CompanyDocumentTypes WHERE CompanyId = @CompanyId AND TypeName = N'Bằng cấp')
            INSERT INTO dbo.CompanyDocumentTypes (CompanyId, TypeName, IsActive, CreatedAt)
            VALUES (@CompanyId, N'Bằng cấp', 1, @Now);
        IF NOT EXISTS (SELECT 1 FROM dbo.CompanyDocumentTypes WHERE CompanyId = @CompanyId AND TypeName = N'Hợp đồng lao động')
            INSERT INTO dbo.CompanyDocumentTypes (CompanyId, TypeName, IsActive, CreatedAt)
            VALUES (@CompanyId, N'Hợp đồng lao động', 1, @Now);

        SET @DocTypeIdCard = (SELECT DocumentTypeId FROM dbo.CompanyDocumentTypes WHERE CompanyId = @CompanyId AND TypeName = N'CCCD/CMND');
        SET @DocTypeDegree = (SELECT DocumentTypeId FROM dbo.CompanyDocumentTypes WHERE CompanyId = @CompanyId AND TypeName = N'Bằng cấp');
        SET @DocTypeContract = (SELECT DocumentTypeId FROM dbo.CompanyDocumentTypes WHERE CompanyId = @CompanyId AND TypeName = N'Hợp đồng lao động');

        SET @DocClosed = (SELECT DocumentPeriodId FROM dbo.DocumentUpdatePeriods
                          WHERE CompanyId = @CompanyId AND Title = N'Đợt bổ sung tài liệu (demo - đã đóng)');
        IF @DocClosed IS NULL
        BEGIN
            INSERT INTO dbo.DocumentUpdatePeriods
                (CompanyId, Title, StartsAt, EndsAt, ScopeType, ClosedAt, CreatedByUserId, CreatedAt)
            VALUES
                (@CompanyId, N'Đợt bổ sung tài liệu (demo - đã đóng)',
                 DATETIME2FROMPARTS(YEAR(DATEADD(MONTH,-3,@CurrentMonth)), MONTH(DATEADD(MONTH,-3,@CurrentMonth)), 1, 0,0,0,0,7),
                 DATETIME2FROMPARTS(YEAR(DATEADD(MONTH,-2,@CurrentMonth)), MONTH(DATEADD(MONTH,-2,@CurrentMonth)), 1, 0,0,0,0,7),
                 'ALL', @Now, @AdminUserId, @Now);
            SET @DocClosed = CONVERT(BIGINT, SCOPE_IDENTITY());
        END

        SET @DocOpen = (SELECT DocumentPeriodId FROM dbo.DocumentUpdatePeriods
                        WHERE CompanyId = @CompanyId AND Title = N'Đợt bổ sung tài liệu (demo - đang mở)');
        IF @DocOpen IS NULL
        BEGIN
            INSERT INTO dbo.DocumentUpdatePeriods
                (CompanyId, Title, StartsAt, EndsAt, ScopeType, ClosedAt, CreatedByUserId, CreatedAt)
            VALUES
                (@CompanyId, N'Đợt bổ sung tài liệu (demo - đang mở)',
                 DATETIME2FROMPARTS(YEAR(@CurrentMonth), MONTH(@CurrentMonth), 1, 0,0,0,0,7),
                 DATETIME2FROMPARTS(YEAR(DATEADD(MONTH,2,@CurrentMonth)), MONTH(DATEADD(MONTH,2,@CurrentMonth)), 1, 0,0,0,0,7),
                 'SELECTED', NULL, @AdminUserId, @Now);
            SET @DocOpen = CONVERT(BIGINT, SCOPE_IDENTITY());
        END

        INSERT INTO dbo.DocumentUpdatePeriodTypes (DocumentPeriodId, DocumentTypeId)
        SELECT x.DocumentPeriodId, x.DocumentTypeId
        FROM (VALUES (@DocClosed, @DocTypeIdCard), (@DocClosed, @DocTypeDegree), (@DocClosed, @DocTypeContract),
                     (@DocOpen, @DocTypeIdCard), (@DocOpen, @DocTypeContract)) x(DocumentPeriodId, DocumentTypeId)
        WHERE @DocClosed IS NOT NULL AND @DocOpen IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM dbo.DocumentUpdatePeriodTypes t
                          WHERE t.DocumentPeriodId = x.DocumentPeriodId AND t.DocumentTypeId = x.DocumentTypeId);

        INSERT INTO dbo.DocumentUpdatePeriodEmployees (DocumentPeriodId, EmployeeId)
        SELECT @DocOpen, e.EmployeeId
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        WHERE e.CompanyId = @CompanyId
          AND e.EmployeeCode IN ('D05','D06','D07')
          AND NOT EXISTS (SELECT 1 FROM dbo.DocumentUpdatePeriodEmployees x
                          WHERE x.DocumentPeriodId = @DocOpen AND x.EmployeeId = e.EmployeeId);

        -- Metadata only: no physical file is written by SQL. FilePath stays NULL,
        -- so the UI lists the document but download is unavailable until a file is provisioned.
        INSERT INTO dbo.EmployeeDocuments
            (EmployeeId, DocumentName, DocumentType, FileName, FilePath, UploadedBy, UploadedDate,
             Status, MimeType, FileSizeBytes, ContractId, DocumentPeriodId)
        SELECT e.EmployeeId,
               CONCAT(N'CCCD - ', e.EmployeeCode), N'CCCD/CMND',
               CONCAT('cccd-', e.EmployeeCode, '.pdf'), NULL, e.UserId, @Now,
               CASE WHEN e.EmployeeCode = 'D06' THEN 'Inactive' ELSE 'Active' END,
               'application/pdf', 102400, NULL, @DocClosed
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        WHERE e.CompanyId = @CompanyId
          AND (e.EmployeeCode LIKE 'D0[1-9]' OR e.EmployeeCode LIKE 'D1[0-5]')
          AND NOT EXISTS (SELECT 1 FROM dbo.EmployeeDocuments doc
                          WHERE doc.EmployeeId = e.EmployeeId AND doc.DocumentName = CONCAT(N'CCCD - ', e.EmployeeCode));

        INSERT INTO dbo.EmployeeDocuments
            (EmployeeId, DocumentName, DocumentType, FileName, FilePath, UploadedBy, UploadedDate,
             Status, MimeType, FileSizeBytes, ContractId, DocumentPeriodId)
        SELECT e.EmployeeId,
               CONCAT(N'Bằng cấp - ', e.EmployeeCode), N'Bằng cấp',
               CONCAT('bang-cap-', e.EmployeeCode, '.pdf'), NULL, e.UserId, @Now,
               'Active', 'application/pdf', 204800, NULL, @DocClosed
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        WHERE e.CompanyId = @CompanyId
          AND e.EmployeeCode IN ('D01','D02','D03','D04','D05')
          AND NOT EXISTS (SELECT 1 FROM dbo.EmployeeDocuments doc
                          WHERE doc.EmployeeId = e.EmployeeId AND doc.DocumentName = CONCAT(N'Bằng cấp - ', e.EmployeeCode));

        INSERT INTO dbo.EmployeeDocuments
            (EmployeeId, DocumentName, DocumentType, FileName, FilePath, UploadedBy, UploadedDate,
             Status, MimeType, FileSizeBytes, ContractId, DocumentPeriodId)
        SELECT e.EmployeeId,
               CONCAT(N'Hợp đồng - ', e.EmployeeCode), N'Hợp đồng lao động',
               CONCAT('hop-dong-', e.EmployeeCode, '.pdf'), NULL, @AdminUserId, @Now,
               'Active', 'application/pdf', 307200, c.ContractId, @DocOpen
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        JOIN dbo.EmploymentContracts c ON c.CompanyId = e.CompanyId AND c.EmployeeId = e.EmployeeId
             AND c.ContractCode = CONCAT('SEED3-ACTIVE-', e.EmployeeCode)
        WHERE e.CompanyId = @CompanyId
          AND NOT EXISTS (SELECT 1 FROM dbo.EmployeeDocuments doc
                          WHERE doc.EmployeeId = e.EmployeeId AND doc.DocumentName = CONCAT(N'Hợp đồng - ', e.EmployeeCode));

        -- -----------------------------------------------------------------
        -- Audit trail for profile and document activity
        -- -----------------------------------------------------------------
        INSERT INTO dbo.AuditLogs (CompanyId, UserId, Action, TableName, RecordId, OldValues, NewValues, IpAddress, CreatedAt)
        SELECT @CompanyId, e.UserId, 'UPDATE_SELF_PROFILE', 'Employees', CONVERT(VARCHAR(20), e.EmployeeId),
               N'{"phone":"0900000000"}',
               CONCAT(N'{"phone":"09', RIGHT(CONCAT('00000000', e.EmployeeId), 8), N'"}'),
               '127.0.0.1',
               DATEADD(DAY, -20, @Now)
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        WHERE e.CompanyId = @CompanyId
          AND e.EmployeeCode IN ('D05','D06','D07','D08','D09','D10')
          AND NOT EXISTS (SELECT 1 FROM dbo.AuditLogs l
                          WHERE l.CompanyId = @CompanyId AND l.Action = 'UPDATE_SELF_PROFILE'
                            AND l.RecordId = CONVERT(VARCHAR(20), e.EmployeeId));

        INSERT INTO dbo.AuditLogs (CompanyId, UserId, Action, TableName, RecordId, OldValues, NewValues, IpAddress, CreatedAt)
        SELECT @CompanyId, e.UserId, 'UPLOAD_EMPLOYEE_DOCUMENT', 'EmployeeDocuments',
               CONVERT(VARCHAR(20), e.EmployeeId), NULL,
               CONCAT(N'{"document":"', N'CCCD - ', e.EmployeeCode, N'","status":"Active"}'),
               '127.0.0.1', DATEADD(DAY, -15, @Now)
        FROM dbo.Employees e
        JOIN @DemoEmployees d ON d.EmployeeId = e.EmployeeId
        WHERE e.CompanyId = @CompanyId
          AND e.EmployeeCode IN ('D05','D06','D07')
          AND NOT EXISTS (SELECT 1 FROM dbo.AuditLogs l
                          WHERE l.CompanyId = @CompanyId AND l.Action = 'UPLOAD_EMPLOYEE_DOCUMENT'
                            AND l.RecordId = CONVERT(VARCHAR(20), e.EmployeeId));

        SET @WsLoop = @WsLoop + 1;
    END

    COMMIT TRANSACTION;

    SELECT c.WorkspaceCode, c.CompanyName, c.CompanyId,
           (SELECT COUNT(*) FROM dbo.Users u WHERE u.CompanyId = c.CompanyId) AS Accounts,
           (SELECT COUNT(*) FROM dbo.Employees e WHERE e.CompanyId = c.CompanyId) AS Employees,
           (SELECT COUNT(*) FROM dbo.Attendances a WHERE a.CompanyId = c.CompanyId) AS AttendanceRows,
           (SELECT COUNT(*) FROM dbo.LeaveRequests l WHERE l.CompanyId = c.CompanyId) AS LeaveRequests,
           (SELECT COUNT(*) FROM dbo.PayrollPeriods p WHERE p.CompanyId = c.CompanyId) AS PayrollPeriods,
           (SELECT COUNT(*) FROM dbo.Payslips s
              JOIN dbo.PayrollPeriods p ON p.PayrollPeriodId = s.PayrollPeriodId
             WHERE p.CompanyId = c.CompanyId) AS Payslips
    FROM dbo.Companies c
    WHERE c.WorkspaceCode IN ('demo-alpha','demo-beta','demo-gamma')
    ORDER BY c.WorkspaceCode;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO