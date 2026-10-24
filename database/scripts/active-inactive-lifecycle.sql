SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF COL_LENGTH('dbo.Departments', 'IsActive') IS NULL
    ALTER TABLE dbo.Departments ADD IsActive BIT NOT NULL CONSTRAINT DF_Departments_IsActive DEFAULT (1) WITH VALUES;

IF COL_LENGTH('dbo.LeaveTypes', 'IsActive') IS NULL
    ALTER TABLE dbo.LeaveTypes ADD IsActive BIT NOT NULL CONSTRAINT DF_LeaveTypes_IsActive DEFAULT (1) WITH VALUES;

IF OBJECT_ID('dbo.EmployeeDocuments', 'U') IS NOT NULL
    UPDATE dbo.EmployeeDocuments SET Status = 'Inactive' WHERE Status = 'Deleted';

COMMIT TRANSACTION;
