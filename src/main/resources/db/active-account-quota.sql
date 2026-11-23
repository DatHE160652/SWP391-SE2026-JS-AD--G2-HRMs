-- Only enabled, unlocked accounts consume a workspace's MaxAccounts quota.
CREATE OR ALTER TRIGGER dbo.TR_Users_MaxAccounts
ON dbo.Users
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @AffectedCompanies TABLE(CompanyId INT PRIMARY KEY);
    INSERT @AffectedCompanies(CompanyId)
    SELECT CompanyId FROM (
        SELECT CompanyId,SUM(CONVERT(BIGINT,CASE WHEN IsActive=1 AND IsLocked=0 THEN 1 ELSE 0 END)) Delta
        FROM inserted WHERE CompanyId IS NOT NULL GROUP BY CompanyId
        UNION ALL
        SELECT CompanyId,-SUM(CONVERT(BIGINT,CASE WHEN IsActive=1 AND IsLocked=0 THEN 1 ELSE 0 END))
        FROM deleted WHERE CompanyId IS NOT NULL GROUP BY CompanyId
    ) changes GROUP BY CompanyId HAVING SUM(Delta)>0;

    DECLARE @CompanyId INT,@LockResult INT,@Resource NVARCHAR(255),@MaxAccounts INT,@AccountCount BIGINT;
    DECLARE CompanyQuotaCursor CURSOR LOCAL FAST_FORWARD FOR SELECT CompanyId FROM @AffectedCompanies ORDER BY CompanyId;
    OPEN CompanyQuotaCursor;
    FETCH NEXT FROM CompanyQuotaCursor INTO @CompanyId;
    WHILE @@FETCH_STATUS=0
    BEGIN
        SET @Resource=N'Users.MaxAccounts.Company.'+CONVERT(NVARCHAR(20),@CompanyId);
        EXEC @LockResult=sys.sp_getapplock @Resource=@Resource,@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
        IF @LockResult<0
        BEGIN
            CLOSE CompanyQuotaCursor;
            DEALLOCATE CompanyQuotaCursor;
            THROW 51160,'Could not acquire the tenant account quota lock.',1;
        END;
        SET @MaxAccounts=NULL;
        SELECT @MaxAccounts=p.MaxAccounts FROM dbo.Subscriptions s WITH(UPDLOCK,HOLDLOCK)
        JOIN dbo.Plans p WITH(UPDLOCK,HOLDLOCK) ON p.PlanId=s.PlanId WHERE s.CompanyId=@CompanyId;
        IF @MaxAccounts IS NULL
        BEGIN
            CLOSE CompanyQuotaCursor;
            DEALLOCATE CompanyQuotaCursor;
            THROW 51161,'A tenant account requires a Subscription and Plan.',1;
        END;
        SELECT @AccountCount=COUNT_BIG(*) FROM dbo.Users WITH(UPDLOCK,HOLDLOCK,INDEX(IX_Users_CompanyId))
        WHERE CompanyId=@CompanyId AND IsActive=1 AND IsLocked=0;
        IF @AccountCount>@MaxAccounts
        BEGIN
            CLOSE CompanyQuotaCursor;
            DEALLOCATE CompanyQuotaCursor;
            THROW 51162,N'Đã đạt đến giới hạn của gói bạn đang dùng hiện tại',1;
        END;
        FETCH NEXT FROM CompanyQuotaCursor INTO @CompanyId;
    END;
    CLOSE CompanyQuotaCursor;
    DEALLOCATE CompanyQuotaCursor;
END;
