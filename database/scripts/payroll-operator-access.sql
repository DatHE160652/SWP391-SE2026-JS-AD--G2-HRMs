-- Payroll authorization projection over the project's existing Users/Roles.
-- No user is assigned a role; historical reviewer/approver identities remain valid.
SET XACT_ABORT ON;
GO
CREATE OR ALTER VIEW dbo.vw_PayrollOperators
AS
    SELECT u.UserId,u.CompanyId,r.RoleCode
    FROM dbo.Users u JOIN dbo.Roles r ON r.RoleId=u.RoleId
    WHERE u.CompanyId IS NOT NULL AND r.RoleCode IN ('MANAGER','COMPANY_ADMIN','HR')
        AND u.IsActive=1 AND u.IsLocked=0 AND u.MustChangePassword=0
        AND (u.LockedUntil IS NULL OR u.LockedUntil<=SYSUTCDATETIME());
GO
