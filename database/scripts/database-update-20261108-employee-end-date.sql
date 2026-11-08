-- Restore the employee end date required by termination and reactivation.
-- Safe to rerun on an existing HRMs database; existing employee data is preserved.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID('dbo.Employees', 'U') IS NULL
        THROW 51150, 'An existing HRMs Employees table is required.', 1;
    IF COL_LENGTH('dbo.Employees', 'EndDate') IS NULL
        ALTER TABLE dbo.Employees ADD EndDate DATE NULL;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
