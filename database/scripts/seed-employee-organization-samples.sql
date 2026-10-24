USE HRMsDB;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

DECLARE @CompanyId int = 1;
DECLARE @ManagerId int = 17;
DECLARE @ItDepartmentCode varchar(50) = 'IT';

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @DepartmentId int;
    DECLARE @LeaderPositionId int;
    SELECT @DepartmentId=e.DepartmentId, @LeaderPositionId=e.PositionId
    FROM dbo.Employees e WITH (UPDLOCK,HOLDLOCK)
    JOIN dbo.Positions p ON p.CompanyId=e.CompanyId AND p.PositionId=e.PositionId
        AND p.DepartmentId=e.DepartmentId AND p.IsActive=1
    WHERE e.CompanyId=@CompanyId AND e.EmployeeId=@ManagerId AND e.Status='Active';
    IF @DepartmentId IS NULL
        THROW 51000, 'Sample manager must exist in the company with an active department position.', 1;

    DECLARE @ItDepartmentId int;
    SELECT @ItDepartmentId=DepartmentId
    FROM dbo.Departments
    WHERE CompanyId=@CompanyId AND DepartmentCode=@ItDepartmentCode AND IsActive=1;
    IF @ItDepartmentId IS NULL
        THROW 51001, 'Configure @ItDepartmentCode with an active department code in this company.', 1;

    DECLARE @StaffLevelId int;
    SELECT @StaffLevelId=ResponsibilityLevelId FROM dbo.ResponsibilityLevels WITH (UPDLOCK,HOLDLOCK)
    WHERE CompanyId=@CompanyId AND LevelCode='DEMO_ORG_STAFF';
    IF @StaffLevelId IS NULL
    BEGIN
        INSERT INTO dbo.ResponsibilityLevels(CompanyId,LevelCode,LevelName,RankOrder,IsActive)
        VALUES(@CompanyId,'DEMO_ORG_STAFF',N'Demo Organization Staff',0,1);
        SET @StaffLevelId=CONVERT(int,SCOPE_IDENTITY());
    END;

    DECLARE @MarketingPositionId int;
    SELECT @MarketingPositionId=PositionId FROM dbo.Positions WITH (UPDLOCK,HOLDLOCK)
    WHERE CompanyId=@CompanyId AND DepartmentId=@DepartmentId AND PositionTitle=N'Demo Marketing Specialist';
    IF @MarketingPositionId IS NULL
    BEGIN
        INSERT INTO dbo.Positions(CompanyId,DepartmentId,ResponsibilityLevelId,
                                  PositionTitle,PositionCode,Description,CreatedAt,IsActive)
        VALUES(@CompanyId,@DepartmentId,@StaffLevelId,
               N'Demo Marketing Specialist','DEMO_ORG_MKT',N'Organization test fixture',SYSUTCDATETIME(),1);
        SET @MarketingPositionId=CONVERT(int,SCOPE_IDENTITY());
    END;

    DECLARE @ItPositionId int;
    SELECT @ItPositionId=PositionId FROM dbo.Positions WITH (UPDLOCK,HOLDLOCK)
    WHERE CompanyId=@CompanyId AND DepartmentId=@ItDepartmentId AND PositionTitle=N'Demo IT Specialist';
    IF @ItPositionId IS NULL
    BEGIN
        INSERT INTO dbo.Positions(CompanyId,DepartmentId,ResponsibilityLevelId,
                                  PositionTitle,PositionCode,Description,CreatedAt,IsActive)
        VALUES(@CompanyId,@ItDepartmentId,@StaffLevelId,
               N'Demo IT Specialist','DEMO_ORG_IT',N'Organization test fixture',SYSUTCDATETIME(),1);
        SET @ItPositionId=CONVERT(int,SCOPE_IDENTITY());
    END;

    DECLARE @Samples TABLE(EmployeeCode varchar(50),FullName nvarchar(150),DepartmentId int,PositionId int,ManagerId int);
    INSERT INTO @Samples(EmployeeCode,FullName,DepartmentId,PositionId,ManagerId)
    VALUES
        ('DEMO-ORG-001',N'[Demo] Nguyễn Minh Anh',@DepartmentId,@MarketingPositionId,@ManagerId),
        ('DEMO-ORG-002',N'[Demo] Lê Hoàng Nam',@DepartmentId,@MarketingPositionId,@ManagerId),
        ('DEMO-ORG-003',N'[Demo] Trần Ngọc Mai',@DepartmentId,@MarketingPositionId,NULL),
        ('DEMO-ORG-004',N'[Demo] Phạm Quốc Huy',@ItDepartmentId,@ItPositionId,NULL),
        ('DEMO-ORG-005',N'[Demo] Đỗ Thu Hà',@DepartmentId,@LeaderPositionId,NULL);

    DECLARE @Created TABLE(EmployeeId int,DepartmentId int,PositionId int);
    DECLARE @EmployeeRoleId int = (SELECT RoleId FROM dbo.Roles WHERE RoleCode='EMPLOYEE');
    IF @EmployeeRoleId IS NULL THROW 51002, 'EMPLOYEE role is required for disabled fixture user records.', 1;
    DECLARE @NewUsers TABLE(UserId int,Username nvarchar(256));
    INSERT INTO dbo.Users(CompanyId,RoleId,Username,Email,DisplayName,PasswordHash,IsActive,IsLocked)
    OUTPUT inserted.UserId,inserted.Username INTO @NewUsers
    SELECT @CompanyId,@EmployeeRoleId,CONCAT('demo-org-',@CompanyId,'-',s.EmployeeCode),
           CONCAT('demo-org-',@CompanyId,'-',s.EmployeeCode,'@example.invalid'),s.FullName,
           CONCAT('DISABLED_FIXTURE_',CONVERT(varchar(128),CRYPT_GEN_RANDOM(64),2)),0,1
    FROM @Samples s
    WHERE NOT EXISTS (SELECT 1 FROM dbo.Employees e WITH (UPDLOCK,HOLDLOCK)
                      WHERE e.CompanyId=@CompanyId AND e.EmployeeCode=s.EmployeeCode);

    INSERT INTO dbo.Employees(CompanyId,UserId,EmployeeCode,FullName,DepartmentId,PositionId,ManagerId,Status)
    OUTPUT inserted.EmployeeId,inserted.DepartmentId,inserted.PositionId INTO @Created
    SELECT @CompanyId,u.UserId,s.EmployeeCode,s.FullName,s.DepartmentId,s.PositionId,s.ManagerId,'Active'
    FROM @Samples s
    JOIN @NewUsers u ON u.Username=CONCAT('demo-org-',@CompanyId,'-',s.EmployeeCode);

    INSERT INTO dbo.EmployeePositionAssignments(CompanyId,EmployeeId,DepartmentId,PositionId,EffectiveFrom,ChangeType,Reason)
    SELECT @CompanyId,EmployeeId,DepartmentId,PositionId,CONVERT(date,SYSDATETIME()),'HIRE',
           N'Organization demo fixture. Reporting relationships are legacy test data pending eligibility schema.'
    FROM @Created;

    COMMIT TRANSACTION;

    SELECT e.EmployeeId,e.EmployeeCode,e.FullName,d.DepartmentName,p.PositionTitle,e.ManagerId,m.FullName AS ManagerName
    FROM dbo.Employees e
    JOIN @Samples s ON s.EmployeeCode=e.EmployeeCode
    LEFT JOIN dbo.Departments d ON d.CompanyId=e.CompanyId AND d.DepartmentId=e.DepartmentId
    LEFT JOIN dbo.Positions p ON p.CompanyId=e.CompanyId AND p.PositionId=e.PositionId
    LEFT JOIN dbo.Employees m ON m.CompanyId=e.CompanyId AND m.EmployeeId=e.ManagerId
    WHERE e.CompanyId=@CompanyId ORDER BY e.EmployeeCode;
END TRY
BEGIN CATCH
    IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
