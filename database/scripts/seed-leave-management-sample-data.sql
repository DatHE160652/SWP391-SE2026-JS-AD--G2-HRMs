-- Demo data for UC39 and its supporting leave workflow. Run only on a test DB.
-- Uses an existing tenant and employee; does not create/delete workspaces/accounts.
-- Replace workspace code if required. Run bootstrap or upgrade migration first.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @CompanyId INT=(SELECT CompanyId FROM Companies WHERE WorkspaceCode='anphat');
IF @CompanyId IS NULL THROW 51200,'Create the demo workspace before seeding leave data.',1;
DECLARE @EmployeeId INT=(SELECT TOP 1 e.EmployeeId FROM Employees e
    JOIN Users u ON u.UserId=e.UserId AND u.CompanyId=e.CompanyId JOIN Roles r ON r.RoleId=u.RoleId
    WHERE e.CompanyId=@CompanyId AND e.ManagerId IS NOT NULL AND e.Status='Active' AND r.RoleCode='EMPLOYEE'
    ORDER BY e.EmployeeId);
IF @EmployeeId IS NULL THROW 51201,'An active employee with an assigned manager is required.',1;
IF NOT EXISTS(SELECT 1 FROM CompanyLeavePolicies WHERE CompanyId=@CompanyId)
    INSERT INTO CompanyLeavePolicies(CompanyId,QuotaMode,QuotaDays,MaxDaysPerRequest) VALUES(@CompanyId,'MONTHLY',3,3);
-- Two non-contiguous weekdays beginning tomorrow. No weekend dates are inserted.
DECLARE @Date1 DATE=DATEADD(day,1,CONVERT(date,SYSDATETIMEOFFSET() AT TIME ZONE 'SE Asia Standard Time'));
WHILE DATEDIFF(day,'19000101',@Date1)%7 NOT BETWEEN 0 AND 4 SET @Date1=DATEADD(day,1,@Date1);
DECLARE @Date2 DATE=DATEADD(day,2,@Date1);
WHILE DATEDIFF(day,'19000101',@Date2)%7 NOT BETWEEN 0 AND 4 SET @Date2=DATEADD(day,1,@Date2);
IF NOT EXISTS(SELECT 1 FROM LeaveRequests WHERE CompanyId=@CompanyId AND EmployeeId=@EmployeeId AND Reason=N'[DEMO UC39] Nghỉ giải quyết việc gia đình')
BEGIN
    INSERT INTO LeaveRequests(CompanyId,EmployeeId,StartDate,EndDate,TotalDays,Reason,Status)
    VALUES(@CompanyId,@EmployeeId,@Date1,@Date2,2,N'[DEMO UC39] Nghỉ giải quyết việc gia đình','Pending');
    DECLARE @RequestId INT=SCOPE_IDENTITY();
    INSERT INTO LeaveRequestDates(LeaveRequestId,LeaveDate) VALUES(@RequestId,@Date1),(@RequestId,@Date2);
END;
COMMIT;
