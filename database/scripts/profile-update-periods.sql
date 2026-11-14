USE HRMsDB;
GO

SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID('dbo.ProfileUpdatePeriods', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ProfileUpdatePeriods (
        PeriodId BIGINT IDENTITY(1,1) PRIMARY KEY,
        CompanyId INT NOT NULL,
        Title NVARCHAR(150) NOT NULL,
        StartsAt DATETIME2 NOT NULL,
        EndsAt DATETIME2 NOT NULL,
        ClosedAt DATETIME2 NULL,
        CreatedByUserId INT NOT NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_ProfileUpdatePeriods_Companies FOREIGN KEY (CompanyId) REFERENCES dbo.Companies(CompanyId),
        CONSTRAINT FK_ProfileUpdatePeriods_Users FOREIGN KEY (CreatedByUserId) REFERENCES dbo.Users(UserId),
        CONSTRAINT CK_ProfileUpdatePeriods_Dates CHECK (EndsAt > StartsAt)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.ProfileUpdatePeriods')
               AND name = 'IX_ProfileUpdatePeriods_CompanyDates')
    CREATE INDEX IX_ProfileUpdatePeriods_CompanyDates
        ON dbo.ProfileUpdatePeriods (CompanyId, ClosedAt, StartsAt, EndsAt);

COMMIT TRANSACTION;
GO
