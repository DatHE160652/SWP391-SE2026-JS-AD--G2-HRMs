SET XACT_ABORT ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID('dbo.EmploymentContracts', 'U') IS NULL
        OR OBJECT_ID('dbo.EmployeePositionChanges', 'U') IS NULL
        OR OBJECT_ID('dbo.EmployeePositionAssignments', 'U') IS NULL
        OR OBJECT_ID('dbo.ProfileUpdatePeriods', 'U') IS NULL
        THROW 51000, 'An existing employee lifecycle schema is required before applying this migration.', 1;

    IF COL_LENGTH('dbo.Users', 'Password') IS NULL
    BEGIN
        IF COL_LENGTH('dbo.Users', 'PasswordHash') IS NULL
            THROW 51000, 'Users has no supported credential column.', 1;
        EXEC sys.sp_rename N'dbo.Users.PasswordHash', N'Password', N'COLUMN';
    END;

    IF COL_LENGTH('dbo.Employees', 'IntendedContractType') IS NULL
        ALTER TABLE dbo.Employees ADD IntendedContractType VARCHAR(30) NULL;

    IF COL_LENGTH('dbo.EmploymentContracts', 'TravelAllowance') IS NULL
        ALTER TABLE dbo.EmploymentContracts ADD TravelAllowance DECIMAL(18,2) NOT NULL
            CONSTRAINT DF_EmploymentContracts_TravelAllowance DEFAULT (0) WITH VALUES;

    IF COL_LENGTH('dbo.ProfileUpdatePeriods', 'PeriodType') IS NULL
        ALTER TABLE dbo.ProfileUpdatePeriods ADD PeriodType VARCHAR(20) NOT NULL
            CONSTRAINT DF_ProfileUpdatePeriods_PeriodType DEFAULT ('PROFILE') WITH VALUES;

    IF COL_LENGTH('dbo.EmployeePositionChanges', 'OldManagerId') IS NULL
        ALTER TABLE dbo.EmployeePositionChanges ADD OldManagerId INT NULL;

    IF EXISTS (SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id=OBJECT_ID('dbo.EmployeePositionChanges') AND name='CK_EmployeePositionChanges_Type')
        ALTER TABLE dbo.EmployeePositionChanges DROP CONSTRAINT CK_EmployeePositionChanges_Type;

    ALTER TABLE dbo.EmployeePositionChanges WITH CHECK ADD CONSTRAINT CK_EmployeePositionChanges_Type
        CHECK (ChangeType IN ('CORRECTION','DEMOTION','TRANSFER','PROMOTION','MANAGER_CHANGE'));

    IF EXISTS (SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id=OBJECT_ID('dbo.EmployeePositionAssignments') AND name='CK_EmployeePositionAssignments_Type')
        ALTER TABLE dbo.EmployeePositionAssignments DROP CONSTRAINT CK_EmployeePositionAssignments_Type;

    ALTER TABLE dbo.EmployeePositionAssignments WITH CHECK ADD CONSTRAINT CK_EmployeePositionAssignments_Type
        CHECK (ChangeType IN ('CORRECTION','DEMOTION','TRANSFER','PROMOTION','HIRE','BASELINE','MANAGER_CHANGE'));

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

CREATE OR ALTER PROCEDURE [dbo].[usp_ApplyApprovedPositionChange]
    @PositionChangeId BIGINT,
    @AsOfDate DATE
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @AsOfDate IS NULL
        THROW 51000, '@AsOfDate (company-local) is required.', 1;

    DECLARE @CompanyId INT, @EmployeeId INT, @OldPositionId INT,
            @NewPositionId INT, @NewDepartmentId INT, @EffectiveDate DATE,
            @NewBaseSalary DECIMAL(18,2), @NewManagerId INT,
            @ChangeType VARCHAR(20), @ChangeStatus VARCHAR(20),
            @Reason NVARCHAR(500), @CreatedByUserId INT,
            @CurrentPositionId INT, @CurrentDepartmentId INT,
            @OldRank INT, @NewRank INT;

    BEGIN TRY
        BEGIN TRANSACTION;

        SELECT @CompanyId=c.CompanyId, @EmployeeId=c.EmployeeId,
               @OldPositionId=c.OldPositionId, @NewPositionId=c.NewPositionId,
               @EffectiveDate=c.EffectiveDate, @NewBaseSalary=c.NewBaseSalary,
               @NewManagerId=c.NewManagerId, @ChangeType=c.ChangeType,
               @ChangeStatus=c.Status, @Reason=c.Reason, @CreatedByUserId=c.CreatedByUserId
        FROM dbo.EmployeePositionChanges c WITH (UPDLOCK, HOLDLOCK)
        WHERE c.PositionChangeId=@PositionChangeId;

        IF @CompanyId IS NULL
            THROW 51001, 'Position change not found.', 1;
        IF @ChangeStatus <> 'APPROVED'
            THROW 51002, 'Position change must be APPROVED.', 1;
        IF @EffectiveDate > @AsOfDate
            THROW 51003, 'Effective date is in the future.', 1;

        SELECT @CurrentPositionId=e.PositionId, @CurrentDepartmentId=e.DepartmentId
        FROM dbo.Employees e WITH (UPDLOCK, HOLDLOCK)
        WHERE e.CompanyId=@CompanyId AND e.EmployeeId=@EmployeeId;

        IF @CurrentPositionId IS NULL OR @CurrentPositionId <> @OldPositionId
            THROW 51004, 'Current Position is missing/changed; re-approve the request.', 1;
        IF @CurrentPositionId=@NewPositionId AND @ChangeType<>'MANAGER_CHANGE'
            THROW 51005, 'New Position matches current Position.', 1;

        SELECT @NewDepartmentId=p.DepartmentId, @NewRank=rl.RankOrder
        FROM dbo.Positions p
        JOIN dbo.ResponsibilityLevels rl ON rl.CompanyId=p.CompanyId AND rl.ResponsibilityLevelId=p.ResponsibilityLevelId
        WHERE p.PositionId=@NewPositionId AND p.CompanyId=@CompanyId AND p.IsActive=1 AND rl.IsActive=1;

        IF @NewDepartmentId IS NULL
            THROW 51006, 'New Position is missing, inactive or incomplete.', 1;

        SELECT @OldRank=rl.RankOrder
        FROM dbo.Positions p
        JOIN dbo.ResponsibilityLevels rl ON rl.CompanyId=p.CompanyId AND rl.ResponsibilityLevelId=p.ResponsibilityLevelId
        WHERE p.PositionId=@CurrentPositionId AND p.CompanyId=@CompanyId;

        IF @ChangeType='PROMOTION' AND (@OldRank IS NULL OR @NewRank <= @OldRank)
            THROW 51007, 'PROMOTION must increase ResponsibilityLevel RankOrder.', 1;

        IF @ChangeType='DEMOTION' AND (@OldRank IS NULL OR @NewRank >= @OldRank)
            THROW 51012, 'DEMOTION must decrease ResponsibilityLevel RankOrder.', 1;

        IF EXISTS (
            SELECT 1 FROM dbo.PayrollPeriods pp
            WHERE pp.CompanyId=@CompanyId AND pp.Status='Approved'
              AND (pp.[Year] > YEAR(@EffectiveDate)
                  OR (pp.[Year]=YEAR(@EffectiveDate) AND pp.[Month]>=MONTH(@EffectiveDate)))
        ) THROW 51008, 'Approved payroll period affected. Use controlled payroll correction.', 1;

        IF NOT EXISTS (
            SELECT 1 FROM dbo.EmployeePositionAssignments a WITH (UPDLOCK, HOLDLOCK)
            WHERE a.CompanyId=@CompanyId AND a.EmployeeId=@EmployeeId
                AND a.PositionId=@CurrentPositionId AND a.DepartmentId=@CurrentDepartmentId
                AND a.EffectiveTo IS NULL AND a.EffectiveFrom < @EffectiveDate
        ) THROW 51009, 'Missing current assignment/baseline or invalid effective date.', 1;

        UPDATE dbo.EmployeePositionAssignments
        SET EffectiveTo=@EffectiveDate
        WHERE CompanyId=@CompanyId AND EmployeeId=@EmployeeId AND PositionId=@CurrentPositionId
              AND DepartmentId=@CurrentDepartmentId AND EffectiveTo IS NULL
              AND EffectiveFrom < @EffectiveDate;

        IF @@ROWCOUNT <> 1
            THROW 51010, 'Could not close previous position assignment.', 1;

        INSERT INTO dbo.EmployeePositionAssignments
            (CompanyId,EmployeeId,DepartmentId,PositionId,ManagerId,EffectiveFrom,EffectiveTo,
             ChangeType,Reason,SourceChangeId)
        VALUES
            (@CompanyId,@EmployeeId,@NewDepartmentId,@NewPositionId,@NewManagerId,@EffectiveDate,NULL,
             @ChangeType,@Reason,@PositionChangeId);

        IF @NewBaseSalary IS NOT NULL
        BEGIN
            IF EXISTS (
                SELECT 1 FROM dbo.EmployeeCompensationHistories s
                WHERE s.CompanyId=@CompanyId AND s.EmployeeId=@EmployeeId
                      AND s.EffectiveTo IS NULL AND s.EffectiveFrom>=@EffectiveDate
            ) THROW 51011, 'Conflicting current salary effective date.', 1;

            UPDATE dbo.EmployeeCompensationHistories
            SET EffectiveTo=@EffectiveDate
            WHERE CompanyId=@CompanyId AND EmployeeId=@EmployeeId
                  AND EffectiveTo IS NULL AND EffectiveFrom<@EffectiveDate;

            INSERT INTO dbo.EmployeeCompensationHistories
                (CompanyId,EmployeeId,BaseSalary,EffectiveFrom,Reason,SourceChangeId,CreatedByUserId)
            VALUES (@CompanyId,@EmployeeId,@NewBaseSalary,@EffectiveDate,@Reason,@PositionChangeId,@CreatedByUserId);
        END;

        UPDATE dbo.Employees
        SET PositionId=@NewPositionId,
            DepartmentId=@NewDepartmentId,
            ManagerId=@NewManagerId,
            UpdatedAt=SYSUTCDATETIME()
        WHERE EmployeeId=@EmployeeId AND CompanyId=@CompanyId;

        UPDATE dbo.EmployeePositionChanges
        SET Status='APPLIED',AppliedAt=SYSUTCDATETIME()
        WHERE PositionChangeId=@PositionChangeId AND CompanyId=@CompanyId;

        INSERT INTO dbo.AuditLogs
            (CompanyId,UserId,[Action],TableName,RecordId,OldValues,NewValues,CreatedAt)
        VALUES
            (@CompanyId,@CreatedByUserId,N'POSITION_CHANGE_APPLIED',N'Employees',
             CONVERT(VARCHAR(100),@EmployeeId),
             CONCAT(N'{"PositionId":',@CurrentPositionId,N',"DepartmentId":',@CurrentDepartmentId,N'}'),
             CONCAT(N'{"PositionId":',@NewPositionId,N',"DepartmentId":',@NewDepartmentId,N'}'),
             SYSUTCDATETIME());

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;

GO
