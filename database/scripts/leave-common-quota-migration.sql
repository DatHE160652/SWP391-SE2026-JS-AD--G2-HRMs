-- Upgrade only: stop the application and back up the database before running.
-- Preserves all requests and archives per-category balances without merging them.
-- Does not choose quota defaults or execute automatically on application startup.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID('dbo.CompanyLeavePolicies','U') IS NULL
    CREATE TABLE dbo.CompanyLeavePolicies (
        CompanyId INT PRIMARY KEY REFERENCES dbo.Companies(CompanyId),
        QuotaMode VARCHAR(10) NOT NULL CHECK(QuotaMode IN('MONTHLY','ANNUAL')),
        QuotaDays INT NOT NULL CHECK(QuotaDays BETWEEN 0 AND 366),
        MaxDaysPerRequest INT NOT NULL CHECK(MaxDaysPerRequest BETWEEN 1 AND 366),
        UpdatedByUserId INT NULL REFERENCES dbo.Users(UserId),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
    );

IF COL_LENGTH('dbo.LeaveRequests','IsLegacy') IS NULL
    EXEC('ALTER TABLE dbo.LeaveRequests ADD IsLegacy BIT NOT NULL CONSTRAINT DF_LeaveRequests_IsLegacy DEFAULT 0,
        LegacyCategoryName NVARCHAR(100) NULL,LegacyIsPaid BIT NULL;');

IF OBJECT_ID('dbo.LeaveBalanceArchive','U') IS NULL
    CREATE TABLE dbo.LeaveBalanceArchive (
        ArchiveId INT IDENTITY PRIMARY KEY,
        ArchivedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        BalanceSnapshot NVARCHAR(MAX) NOT NULL CHECK(ISJSON(BalanceSnapshot)=1)
    );

IF COL_LENGTH('dbo.LeaveRequests','LeaveTypeId') IS NOT NULL
BEGIN
    EXEC('UPDATE lr SET IsLegacy=1,LegacyCategoryName=lt.TypeName,LegacyIsPaid=lt.IsPaid
        FROM dbo.LeaveRequests lr JOIN dbo.LeaveTypes lt ON lt.LeaveTypeId=lr.LeaveTypeId;');
    IF OBJECT_ID('dbo.LeaveBalances','U') IS NOT NULL
    BEGIN
        EXEC('INSERT INTO dbo.LeaveBalanceArchive(BalanceSnapshot)
            SELECT (SELECT b.*,lt.TypeName,lt.IsPaid FOR JSON PATH,WITHOUT_ARRAY_WRAPPER)
            FROM dbo.LeaveBalances b JOIN dbo.LeaveTypes lt ON lt.LeaveTypeId=b.LeaveTypeId;');
        DROP TABLE dbo.LeaveBalances;
    END;
    DECLARE @drop NVARCHAR(MAX)=N'';
    SELECT @drop=@drop+N'ALTER TABLE dbo.LeaveRequests DROP CONSTRAINT '+QUOTENAME(fk.name)+N';'
    FROM sys.foreign_keys fk
    WHERE fk.parent_object_id=OBJECT_ID('dbo.LeaveRequests') AND fk.referenced_object_id=OBJECT_ID('dbo.LeaveTypes');
    EXEC sys.sp_executesql @drop;
    EXEC('ALTER TABLE dbo.LeaveRequests DROP COLUMN LeaveTypeId;');
    DROP TABLE dbo.LeaveTypes;
END;

IF OBJECT_ID('dbo.LeaveRequestDates','U') IS NULL
BEGIN
    CREATE TABLE dbo.LeaveRequestDates (
        LeaveRequestId INT NOT NULL REFERENCES dbo.LeaveRequests(LeaveRequestId) ON DELETE CASCADE,
        LeaveDate DATE NOT NULL,
        CONSTRAINT PK_LeaveRequestDates PRIMARY KEY(LeaveRequestId,LeaveDate)
    );
    CREATE INDEX IX_LeaveRequestDates_Date ON dbo.LeaveRequestDates(LeaveDate,LeaveRequestId);
END;
-- Legacy requests retain their original range. Do not invent exact dates, recalculate
-- historical quantities, or debit the new quota. Pending legacy requests may be
-- rejected/cancelled, but must be resubmitted to use the new policy.
COMMIT;
