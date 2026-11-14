-- Run in the intended application database BEFORE payroll-management.sql.
-- Safe to re-run. Adds missing source tables/columns; never drops Employees data.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID('dbo.EmploymentContracts','U') IS NULL
BEGIN
    CREATE TABLE dbo.EmploymentContracts (
        ContractId BIGINT IDENTITY PRIMARY KEY,
        CompanyId INT NOT NULL REFERENCES dbo.Companies(CompanyId),
        EmployeeId INT NOT NULL REFERENCES dbo.Employees(EmployeeId),
        ContractCode VARCHAR(60) NOT NULL,ContractType VARCHAR(30) NOT NULL,
        StartDate DATE NOT NULL,EndDate DATE NULL,GrossSalary DECIMAL(18,2) NOT NULL CHECK (GrossSalary>=0),
        Status VARCHAR(25) NOT NULL,SignedDate DATE NULL,TerminatedDate DATE NULL,
        TerminationReason NVARCHAR(500) NULL,Notes NVARCHAR(1000) NULL,
        CreatedByUserId INT NULL,CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),UpdatedAt DATETIME2 NULL,
        UNIQUE(CompanyId,ContractCode),CHECK (EndDate IS NULL OR EndDate>=StartDate)
    );
END;

IF OBJECT_ID('dbo.EmployeeCompensationHistories','U') IS NULL
BEGIN
    CREATE TABLE dbo.EmployeeCompensationHistories (
        CompensationHistoryId BIGINT IDENTITY PRIMARY KEY,
        CompanyId INT NOT NULL REFERENCES dbo.Companies(CompanyId),
        EmployeeId INT NOT NULL REFERENCES dbo.Employees(EmployeeId),
        BaseSalary DECIMAL(18,2) NOT NULL CHECK(BaseSalary>=0),
        EffectiveFrom DATE NOT NULL,EffectiveTo DATE NULL,Reason NVARCHAR(500) NULL,
        SourceChangeId BIGINT NULL,SourceContractId BIGINT NULL,
        InsuranceSalary DECIMAL(18,2) NULL,PositionAllowance DECIMAL(18,2) NULL,TravelAllowance DECIMAL(18,2) NULL,
        SalaryStructureCode VARCHAR(50) NULL,CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),CreatedByUserId INT NULL,
        CHECK (EffectiveTo IS NULL OR EffectiveTo>=EffectiveFrom)
    );
END;

IF OBJECT_ID('dbo.EmployeePositionAssignments','U') IS NULL
BEGIN
    CREATE TABLE dbo.EmployeePositionAssignments (
        AssignmentId BIGINT IDENTITY PRIMARY KEY,
        CompanyId INT NOT NULL REFERENCES dbo.Companies(CompanyId),
        EmployeeId INT NOT NULL REFERENCES dbo.Employees(EmployeeId),
        DepartmentId INT NOT NULL,PositionId INT NOT NULL,ManagerId INT NULL,
        EffectiveFrom DATE NOT NULL,EffectiveTo DATE NULL,ChangeType VARCHAR(20) NOT NULL,
        Reason NVARCHAR(500) NULL,SourceChangeId BIGINT NULL,CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CHECK (EffectiveTo IS NULL OR EffectiveTo>=EffectiveFrom)
    );
END;

IF OBJECT_ID('dbo.ResponsibilityLevels','U') IS NULL
BEGIN
    CREATE TABLE dbo.ResponsibilityLevels (
        ResponsibilityLevelId INT IDENTITY PRIMARY KEY,CompanyId INT NOT NULL REFERENCES dbo.Companies(CompanyId),
        LevelCode VARCHAR(50) NOT NULL,LevelName NVARCHAR(100) NOT NULL,RankOrder INT NOT NULL,
        IsActive BIT NOT NULL DEFAULT 1,CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),UpdatedAt DATETIME2 NULL,
        UNIQUE(CompanyId,LevelCode),UNIQUE(CompanyId,ResponsibilityLevelId)
    );
END;

IF OBJECT_ID('dbo.ResponsibilityAllowancePolicies','U') IS NULL
BEGIN
    CREATE TABLE dbo.ResponsibilityAllowancePolicies (
        AllowancePolicyId BIGINT IDENTITY PRIMARY KEY,CompanyId INT NOT NULL,
        ResponsibilityLevelId INT NOT NULL,Amount DECIMAL(18,2) NOT NULL CHECK (Amount>=0),
        EffectiveFrom DATE NOT NULL,EffectiveTo DATE NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),CreatedByUserId INT NULL,
        FOREIGN KEY(CompanyId,ResponsibilityLevelId) REFERENCES dbo.ResponsibilityLevels(CompanyId,ResponsibilityLevelId),
        UNIQUE(CompanyId,ResponsibilityLevelId,EffectiveFrom),CHECK(EffectiveTo IS NULL OR EffectiveTo>=EffectiveFrom)
    );
END;
IF COL_LENGTH('dbo.Positions','ResponsibilityLevelId') IS NULL
    ALTER TABLE dbo.Positions ADD ResponsibilityLevelId INT NULL;
IF COL_LENGTH('dbo.EmployeeCompensationHistories','SourceContractId') IS NULL
    ALTER TABLE dbo.EmployeeCompensationHistories ADD SourceContractId BIGINT NULL;
IF COL_LENGTH('dbo.EmployeeCompensationHistories','SalaryStructureCode') IS NULL
    ALTER TABLE dbo.EmployeeCompensationHistories ADD SalaryStructureCode VARCHAR(50) NULL;
GO

-- Copy a legacy employee's salary/start date only when that employee has NO
-- lifecycle records yet. Never overwrite an existing contract or salary history.
IF COL_LENGTH('dbo.Employees','StartDate') IS NOT NULL AND COL_LENGTH('dbo.Employees','BaseSalary') IS NOT NULL
EXEC(N'
    INSERT INTO dbo.EmploymentContracts(CompanyId,EmployeeId,ContractCode,ContractType,StartDate,EndDate,GrossSalary,Status,Notes)
    SELECT e.CompanyId,e.EmployeeId,CONCAT(''PAYROLL-LEGACY-'',e.EmployeeId),''INDEFINITE'',e.StartDate,e.EndDate,e.BaseSalary,
        CASE WHEN e.EndDate<CONVERT(date,SYSDATETIME()) THEN ''EXPIRED'' ELSE ''ACTIVE'' END,
        N''Bảo toàn dữ liệu payroll cũ trước khi chuyển sang lịch sử lương''
    FROM dbo.Employees e WHERE e.StartDate IS NOT NULL AND e.BaseSalary IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM dbo.EmploymentContracts c WHERE c.CompanyId=e.CompanyId AND c.EmployeeId=e.EmployeeId);

    INSERT INTO dbo.EmployeeCompensationHistories(CompanyId,EmployeeId,BaseSalary,EffectiveFrom,Reason)
    SELECT e.CompanyId,e.EmployeeId,e.BaseSalary,e.StartDate,N''Chuyển dữ liệu payroll cũ''
    FROM dbo.Employees e WHERE EXISTS (
        SELECT 1 FROM dbo.EmploymentContracts c WHERE c.CompanyId=e.CompanyId AND c.EmployeeId=e.EmployeeId
            AND c.ContractCode=CONCAT(''PAYROLL-LEGACY-'',e.EmployeeId)) AND NOT EXISTS (
        SELECT 1 FROM dbo.EmployeeCompensationHistories h WHERE h.CompanyId=e.CompanyId AND h.EmployeeId=e.EmployeeId);
');
COMMIT TRANSACTION;
GO
