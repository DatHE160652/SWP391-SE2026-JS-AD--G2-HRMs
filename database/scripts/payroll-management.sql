-- Run once on HRMsDB before deploying the payroll module (safe to re-run).
-- Existing monetary CHECKs, foreign keys, UNIQUE keys and computed NetSalary
-- in db.sql remain the authority for PAY-BR-01/02/03/04/07/08/14/15/16/20/29/30/31.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF COL_LENGTH('dbo.Payslips','ResponsibilityAllowance') IS NULL
    ALTER TABLE dbo.Payslips ADD ResponsibilityAllowance DECIMAL(18,2) NOT NULL
        CONSTRAINT DF_Payroll_ResponsibilityAllowance DEFAULT(0) WITH VALUES;
GO
-- Existing slips retain their exact totals: the added allowance starts at zero.
IF EXISTS (SELECT 1 FROM sys.computed_columns WHERE object_id=OBJECT_ID('dbo.Payslips')
    AND name='NetSalary' AND definition NOT LIKE '%ResponsibilityAllowance%')
BEGIN
    ALTER TABLE dbo.Payslips DROP COLUMN NetSalary;
    ALTER TABLE dbo.Payslips ADD NetSalary AS (BaseSalary+ResponsibilityAllowance+Bonus-Deduction);
END;
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID('dbo.Payslips')
    AND name='CK_Payroll_ResponsibilityAllowance')
    ALTER TABLE dbo.Payslips ADD CONSTRAINT CK_Payroll_ResponsibilityAllowance CHECK(ResponsibilityAllowance>=0);

IF COL_LENGTH('dbo.PayrollPeriods', 'IsClosed') IS NULL
    ALTER TABLE dbo.PayrollPeriods ADD IsClosed BIT NOT NULL
        CONSTRAINT DF_PayrollPeriods_IsClosed DEFAULT 0;
IF COL_LENGTH('dbo.PayrollPeriods', 'PublishedAt') IS NULL
    ALTER TABLE dbo.PayrollPeriods ADD PublishedAt DATETIME2 NULL;
IF COL_LENGTH('dbo.PayrollPeriods', 'PublishedByUserId') IS NULL
    ALTER TABLE dbo.PayrollPeriods ADD PublishedByUserId INT NULL
        CONSTRAINT FK_PayrollPeriods_PublishedBy REFERENCES dbo.Users(UserId);

IF OBJECT_ID('dbo.PayrollNotifications', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.PayrollNotifications (
        NotificationId BIGINT IDENTITY(1,1) PRIMARY KEY,
        PayslipId BIGINT NOT NULL,
        EmployeeId INT NOT NULL,
        CompanyId INT NOT NULL,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_PayrollNotifications_CreatedAt DEFAULT SYSUTCDATETIME(),
        ReadAt DATETIME2 NULL,
        CONSTRAINT FK_PayrollNotifications_Payslip FOREIGN KEY (PayslipId) REFERENCES dbo.Payslips(PayslipId),
        CONSTRAINT FK_PayrollNotifications_Employee FOREIGN KEY (EmployeeId) REFERENCES dbo.Employees(EmployeeId),
        CONSTRAINT FK_PayrollNotifications_Company FOREIGN KEY (CompanyId) REFERENCES dbo.Companies(CompanyId),
        CONSTRAINT UQ_PayrollNotifications_Payslip UNIQUE (PayslipId)
    );
    CREATE INDEX IX_PayrollNotifications_Employee ON dbo.PayrollNotifications(CompanyId, EmployeeId, CreatedAt DESC);
END;

-- A reference existing on its own is insufficient: both records must belong
-- to the same tenant, including the creator/approver/publisher.
EXEC(N'CREATE OR ALTER TRIGGER dbo.TR_PayrollAdjustments_Tenant ON dbo.PayrollAdjustments
AFTER INSERT, UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i JOIN dbo.PayrollPeriods p ON p.PayrollPeriodId=i.PayrollPeriodId
        JOIN dbo.Employees e ON e.EmployeeId=i.EmployeeId JOIN dbo.Users u ON u.UserId=i.CreatedByUserId
        WHERE e.CompanyId<>p.CompanyId OR u.CompanyId IS NULL OR u.CompanyId<>p.CompanyId)
        THROW 51001, ''Payroll adjustment references another company.'', 1;
END');
EXEC(N'CREATE OR ALTER TRIGGER dbo.TR_Payslips_Tenant ON dbo.Payslips
AFTER INSERT, UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i JOIN dbo.PayrollPeriods p ON p.PayrollPeriodId=i.PayrollPeriodId
        JOIN dbo.Employees e ON e.EmployeeId=i.EmployeeId WHERE e.CompanyId<>p.CompanyId)
        THROW 51002, ''Payslip references another company.'', 1;
END');
EXEC(N'CREATE OR ALTER TRIGGER dbo.TR_PayrollPeriods_Actors ON dbo.PayrollPeriods
AFTER INSERT, UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i LEFT JOIN dbo.Users a ON a.UserId=i.ApprovedByUserId
        LEFT JOIN dbo.Users b ON b.UserId=i.PublishedByUserId
        WHERE (i.ApprovedByUserId IS NOT NULL AND (a.CompanyId IS NULL OR a.CompanyId<>i.CompanyId))
           OR (i.PublishedByUserId IS NOT NULL AND (b.CompanyId IS NULL OR b.CompanyId<>i.CompanyId)))
        THROW 51003, ''Payroll actor references another company.'', 1;
END');
COMMIT;
