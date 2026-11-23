SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @lockResult int;
EXEC @lockResult = sys.sp_getapplock @Resource=N'HRMs.CompanyContractAllowances.Schema', @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=15000;
IF @lockResult < 0 THROW 51000, 'Cannot initialize company allowances.', 1;

IF OBJECT_ID('dbo.CompanyAllowanceTypes','U') IS NULL
BEGIN
    CREATE TABLE dbo.CompanyAllowanceTypes (
        AllowanceTypeId INT IDENTITY PRIMARY KEY,
        CompanyId INT NOT NULL REFERENCES dbo.Companies(CompanyId),
        TypeName NVARCHAR(150) NOT NULL,
        DefaultAmount DECIMAL(18,2) NOT NULL DEFAULT(0),
        IsActive BIT NOT NULL DEFAULT(1),
        LegacyTravel BIT NOT NULL DEFAULT(0),
        CONSTRAINT UQ_CompanyAllowanceTypes_Name UNIQUE(CompanyId,TypeName),
        CONSTRAINT UQ_CompanyAllowanceTypes_Tenant UNIQUE(CompanyId,AllowanceTypeId),
        CONSTRAINT CK_CompanyAllowanceTypes_Amount CHECK(DefaultAmount>=0)
    );
END;
IF OBJECT_ID('dbo.ContractAllowances','U') IS NULL
BEGIN
    CREATE TABLE dbo.ContractAllowances (
        CompanyId INT NOT NULL,
        ContractId BIGINT NOT NULL REFERENCES dbo.EmploymentContracts(ContractId),
        AllowanceTypeId INT NOT NULL,
        TypeName NVARCHAR(150) NOT NULL,
        Amount DECIMAL(18,2) NOT NULL,
        CONSTRAINT PK_ContractAllowances PRIMARY KEY(CompanyId,ContractId,AllowanceTypeId),
        CONSTRAINT FK_ContractAllowances_Type FOREIGN KEY(CompanyId,AllowanceTypeId)
            REFERENCES dbo.CompanyAllowanceTypes(CompanyId,AllowanceTypeId),
        CONSTRAINT CK_ContractAllowances_Amount CHECK(Amount>=0)
    );
    INSERT dbo.CompanyAllowanceTypes(CompanyId,TypeName,DefaultAmount,LegacyTravel)
    SELECT DISTINCT c.CompanyId,N'Phụ cấp đi lại',0,1 FROM dbo.EmploymentContracts c
    WHERE c.TravelAllowance>0;
    INSERT dbo.ContractAllowances(CompanyId,ContractId,AllowanceTypeId,TypeName,Amount)
    SELECT c.CompanyId,c.ContractId,t.AllowanceTypeId,t.TypeName,c.TravelAllowance
    FROM dbo.EmploymentContracts c JOIN dbo.CompanyAllowanceTypes t ON t.CompanyId=c.CompanyId AND t.LegacyTravel=1
    WHERE c.TravelAllowance>0;
END;
IF OBJECT_ID('dbo.CompanyAllowanceLevelAmounts','U') IS NULL
BEGIN
    CREATE TABLE dbo.CompanyAllowanceLevelAmounts (
        CompanyId INT NOT NULL,
        AllowanceTypeId INT NOT NULL,
        ResponsibilityLevelId INT NOT NULL,
        Amount DECIMAL(18,2) NOT NULL,
        CONSTRAINT PK_CompanyAllowanceLevelAmounts PRIMARY KEY(CompanyId,AllowanceTypeId,ResponsibilityLevelId),
        CONSTRAINT FK_CompanyAllowanceLevelAmounts_Type FOREIGN KEY(CompanyId,AllowanceTypeId)
            REFERENCES dbo.CompanyAllowanceTypes(CompanyId,AllowanceTypeId),
        CONSTRAINT FK_CompanyAllowanceLevelAmounts_Level FOREIGN KEY(CompanyId,ResponsibilityLevelId)
            REFERENCES dbo.ResponsibilityLevels(CompanyId,ResponsibilityLevelId),
        CONSTRAINT CK_CompanyAllowanceLevelAmounts_Amount CHECK(Amount>=0)
    );
    -- Preserve configured defaults as the starting rate for each existing level.
    INSERT dbo.CompanyAllowanceLevelAmounts(CompanyId,AllowanceTypeId,ResponsibilityLevelId,Amount)
    SELECT t.CompanyId,t.AllowanceTypeId,l.ResponsibilityLevelId,t.DefaultAmount
    FROM dbo.CompanyAllowanceTypes t JOIN dbo.ResponsibilityLevels l ON l.CompanyId=t.CompanyId;
END;
COMMIT TRANSACTION;
