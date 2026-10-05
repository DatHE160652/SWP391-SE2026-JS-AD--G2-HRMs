-- Sample data for local/development only.
-- Inserts two visible Registration Requests without creating a tenant.
USE HRMsDB;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.RegistrationRequests
        WHERE EmailNormalized = 'minh.anh.demo@hrms.local'
           OR CompanyNameNormalized = N'công ty tnhh sao bắc'
    )
    BEGIN
        INSERT INTO dbo.RegistrationRequests (
            FullName, Email, Phone, CompanyName, Region,
            CompanyNameNormalized, Status
        )
        VALUES (
            N'Nguyễn Minh Anh',
            'minh.anh.demo@hrms.local',
            '0901234567',
            N'Công ty TNHH Sao Bắc',
            'NORTH',
            N'công ty tnhh sao bắc',
            'NEW'
        );
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.RegistrationRequests
        WHERE EmailNormalized = 'bao.tran.demo@hrms.local'
           OR CompanyNameNormalized = N'công nghệ mây việt'
    )
    BEGIN
        INSERT INTO dbo.RegistrationRequests (
            FullName, Email, Phone, CompanyName, Region,
            CompanyNameNormalized, Status
        )
        VALUES (
            N'Trần Quốc Bảo',
            'bao.tran.demo@hrms.local',
            '0912345678',
            N'Công nghệ Mây Việt',
            'SOUTH',
            N'công nghệ mây việt',
            'UNDER_REVIEW'
        );
    END;

    COMMIT TRANSACTION;

    SELECT RegistrationRequestId, FullName, Email, Phone, CompanyName,
           Region, Status, CreatedAt, UpdatedAt
    FROM dbo.RegistrationRequests
    WHERE EmailNormalized IN ('minh.anh.demo@hrms.local', 'bao.tran.demo@hrms.local')
    ORDER BY RegistrationRequestId;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
