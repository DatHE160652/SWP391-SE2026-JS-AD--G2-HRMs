-- Proposed incremental migration for an existing payroll database.
-- Apply after payroll-management.sql, before deploying Review & Approval.
-- Does not change statuses, monetary data, roles or historical reviewer identity.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    IF COL_LENGTH('dbo.PayrollPeriods', 'ReviewedByUserId') IS NULL
        ALTER TABLE dbo.PayrollPeriods ADD ReviewedByUserId INT NULL;
    IF COL_LENGTH('dbo.PayrollPeriods', 'ReviewedAt') IS NULL
        ALTER TABLE dbo.PayrollPeriods ADD ReviewedAt DATETIME2 NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys
        WHERE parent_object_id=OBJECT_ID('dbo.PayrollPeriods') AND name='FK_PayrollPeriods_ReviewedBy')
        ALTER TABLE dbo.PayrollPeriods WITH CHECK ADD CONSTRAINT FK_PayrollPeriods_ReviewedBy
            FOREIGN KEY (ReviewedByUserId) REFERENCES dbo.Users(UserId);

    -- Dynamic SQL allows the new columns to be added and referenced in one batch.
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id=OBJECT_ID('dbo.PayrollPeriods') AND name='CK_PayrollPeriods_ReviewMetadata')
        EXEC(N'ALTER TABLE dbo.PayrollPeriods WITH CHECK ADD CONSTRAINT CK_PayrollPeriods_ReviewMetadata
            CHECK ((ReviewedByUserId IS NULL AND ReviewedAt IS NULL)
                OR (ReviewedByUserId IS NOT NULL AND ReviewedAt IS NOT NULL))');

    -- Keep the existing approver/publisher guard; this guard survives re-running it.
    EXEC(N'CREATE OR ALTER TRIGGER dbo.TR_PayrollPeriods_Reviewer ON dbo.PayrollPeriods
    AFTER INSERT, UPDATE AS
    BEGIN
        SET NOCOUNT ON;
        IF EXISTS (SELECT 1 FROM inserted i LEFT JOIN dbo.Users u ON u.UserId=i.ReviewedByUserId
            WHERE i.ReviewedByUserId IS NOT NULL AND (u.CompanyId IS NULL OR u.CompanyId<>i.CompanyId))
            THROW 51004, ''Payroll reviewer references another company.'', 1;
    END');

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
