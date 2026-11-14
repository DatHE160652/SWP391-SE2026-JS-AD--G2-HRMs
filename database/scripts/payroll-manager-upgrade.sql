-- SQLCMD mode; execute from the repository root against the intended database.
-- Take and verify a backup first. This upgrades existing lifecycle tables only;
-- it does not run bootstrap seeds or historical contract/salary backfill.
:ON ERROR EXIT
SET XACT_ABORT ON;
SET LOCK_TIMEOUT 20000;
BEGIN TRANSACTION;
GO
:r database/scripts/database-update-20261026.sql
GO
:r database/scripts/payroll-management.sql
GO
:r database/scripts/payroll-review-approval.sql
GO
:r database/scripts/payroll-operator-access.sql
GO
IF @@TRANCOUNT<>1
    THROW 51152, 'Payroll upgrade transaction nesting is invalid.', 1;
COMMIT TRANSACTION;
GO
