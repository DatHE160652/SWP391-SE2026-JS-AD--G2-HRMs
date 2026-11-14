-- Company-scoped catalogue names, including inactive records.
-- Existing duplicates must be resolved before this migration; never rename them automatically.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.Departments') AND name='UX_Departments_Company_Name')
        CREATE UNIQUE INDEX UX_Departments_Company_Name ON dbo.Departments(CompanyId,DepartmentName);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.Positions') AND name='UX_Positions_Company_Title')
        CREATE UNIQUE INDEX UX_Positions_Company_Title ON dbo.Positions(CompanyId,PositionTitle);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.ResponsibilityLevels') AND name='UX_ResponsibilityLevels_Company_Name')
        CREATE UNIQUE INDEX UX_ResponsibilityLevels_Company_Name ON dbo.ResponsibilityLevels(CompanyId,LevelName);
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
