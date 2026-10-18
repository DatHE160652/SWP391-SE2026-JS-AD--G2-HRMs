USE HRMsDB;
GO

SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF COL_LENGTH('dbo.Employees', 'IdentityNumber') IS NULL
        ALTER TABLE dbo.Employees ADD IdentityNumber VARCHAR(50) NULL;

    DECLARE @dropConstraints NVARCHAR(MAX) = N'';

    SELECT @dropConstraints = @dropConstraints
        + N'ALTER TABLE dbo.Employees DROP CONSTRAINT ' + QUOTENAME(constraintInfo.name) + N';'
    FROM sys.default_constraints constraintInfo
    JOIN sys.columns columnInfo
        ON columnInfo.object_id = constraintInfo.parent_object_id
        AND columnInfo.column_id = constraintInfo.parent_column_id
    WHERE constraintInfo.parent_object_id = OBJECT_ID('dbo.Employees')
        AND columnInfo.name IN ('StartDate', 'BaseSalary');

    SELECT @dropConstraints = @dropConstraints
        + N'ALTER TABLE dbo.Employees DROP CONSTRAINT ' + QUOTENAME(constraintInfo.name) + N';'
    FROM sys.check_constraints constraintInfo
    WHERE constraintInfo.parent_object_id = OBJECT_ID('dbo.Employees')
        AND EXISTS (
            SELECT 1 FROM sys.columns columnInfo
            WHERE columnInfo.object_id = constraintInfo.parent_object_id
                AND columnInfo.name IN ('StartDate', 'BaseSalary')
                AND (columnInfo.column_id = constraintInfo.parent_column_id OR EXISTS (
                    SELECT 1 FROM sys.sql_expression_dependencies dependencyInfo
                    WHERE dependencyInfo.referencing_id = constraintInfo.object_id
                        AND dependencyInfo.referenced_id = columnInfo.object_id
                        AND dependencyInfo.referenced_minor_id = columnInfo.column_id
                ))
        );

    IF @dropConstraints <> N''
        EXEC sys.sp_executesql @dropConstraints;

    IF COL_LENGTH('dbo.Employees', 'StartDate') IS NOT NULL
        ALTER TABLE dbo.Employees DROP COLUMN StartDate;

    IF COL_LENGTH('dbo.Employees', 'BaseSalary') IS NOT NULL
        ALTER TABLE dbo.Employees DROP COLUMN BaseSalary;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
