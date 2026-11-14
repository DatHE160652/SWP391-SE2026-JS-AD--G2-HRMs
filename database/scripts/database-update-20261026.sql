-- Upgrade an existing lifecycle database to the reconciled db.sql/dbver.text source.
-- Back up first. Never execute CREATE DATABASE or copy bootstrap seeds over existing data.
-- Run in the intended database, then management/review/operator scripts.
-- Lifecycle tables must already exist. No legacy contract/salary backfill is run here.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID('dbo.Users','U') IS NULL OR OBJECT_ID('dbo.Employees','U') IS NULL
        OR OBJECT_ID('dbo.PayrollPeriods','U') IS NULL
        THROW 51150, 'An existing HRMs lifecycle database is required.', 1;

    IF COL_LENGTH('dbo.Users','Password') IS NULL
    BEGIN
        IF COL_LENGTH('dbo.Users','PasswordHash') IS NULL
            THROW 51151, 'Users has no existing credential column to preserve.', 1;
        EXEC sys.sp_rename N'dbo.Users.PasswordHash',N'Password',N'COLUMN';
    END;
    -- Profiles can be recorded before contracts; do not invent an employment date.
    IF COL_LENGTH('dbo.Employees','StartDate') IS NULL
        ALTER TABLE dbo.Employees ADD StartDate DATE NULL;
    ELSE IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID('dbo.Employees')
        AND name='StartDate' AND is_nullable=0)
        ALTER TABLE dbo.Employees ALTER COLUMN StartDate DATE NULL;
    IF COL_LENGTH('dbo.Employees','EndDate') IS NULL
        ALTER TABLE dbo.Employees ADD EndDate DATE NULL;
    IF COL_LENGTH('dbo.Employees','BaseSalary') IS NULL
        ALTER TABLE dbo.Employees ADD BaseSalary DECIMAL(18,2) NOT NULL
            CONSTRAINT DF_Employees_LegacyBaseSalary DEFAULT(0) WITH VALUES;
    IF COL_LENGTH('dbo.Employees','IntendedContractType') IS NULL
        ALTER TABLE dbo.Employees ADD IntendedContractType VARCHAR(30) NULL;
    IF COL_LENGTH('dbo.EmploymentContracts','TravelAllowance') IS NULL
        ALTER TABLE dbo.EmploymentContracts ADD TravelAllowance DECIMAL(18,2) NOT NULL
            CONSTRAINT DF_EmploymentContracts_TravelAllowance DEFAULT(0) WITH VALUES;
    IF COL_LENGTH('dbo.ProfileUpdatePeriods','PeriodType') IS NULL
        ALTER TABLE dbo.ProfileUpdatePeriods ADD PeriodType VARCHAR(10) NOT NULL
            CONSTRAINT DF_ProfileUpdatePeriods_PeriodType DEFAULT('PROFILE') WITH VALUES;
    IF COL_LENGTH('dbo.EmployeePositionChanges','OldManagerId') IS NULL
        ALTER TABLE dbo.EmployeePositionChanges ADD OldManagerId INT NULL;
    IF COL_LENGTH('dbo.EmployeeCompensationHistories','SourceContractId') IS NULL
        ALTER TABLE dbo.EmployeeCompensationHistories ADD SourceContractId BIGINT NULL;
    IF COL_LENGTH('dbo.EmployeeCompensationHistories','SalaryStructureCode') IS NULL
        ALTER TABLE dbo.EmployeeCompensationHistories ADD SalaryStructureCode VARCHAR(50) NULL;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
