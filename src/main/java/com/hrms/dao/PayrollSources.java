package com.hrms.dao;

/** Tenant-scoped, effective-dated MVP inputs. Legacy salary/allowance fields are never fallbacks. */
final class PayrollSources {
    private PayrollSources() { }

    static final String MONTH_START = "DATEFROMPARTS(p.Year,p.Month,1)";
    static final String MONTH_END = "EOMONTH(" + MONTH_START + ")";
    static final String ELIGIBLE = """
        employment.PayrollDate IS NOT NULL
        AND e.Status IN ('Active','On Leave','Inactive')
        """;
    static final String ISSUE = """
        CASE WHEN employment.PayrollDate IS NULL THEN N'Chưa có hợp đồng có hiệu lực trong kỳ'
             WHEN compensation.SourceCount=0 THEN N'Thiếu lịch sử lương có hiệu lực trong kỳ'
             WHEN compensation.SourceCount>1 THEN N'Lịch sử lương bị chồng lấn'
             WHEN compensation.StartSourceCount=0 THEN N'Thiếu lịch sử lương ở đầu thời gian có hợp đồng trong kỳ'
             ELSE NULL END
        """;

    static String joins(String cutoff) {
        return joins(cutoff,false,true);
    }

    static String joins(String cutoff, boolean includeUncontracted) {
        return joins(cutoff,includeUncontracted,true);
    }

    static String joins(String cutoff, boolean includeUncontracted, boolean monthly) {
        String salaryDate=!monthly?cutoff:includeUncontracted?"COALESCE(employment.PayrollDate,"+cutoff+")":"employment.PayrollDate";
        String start=monthly?"COALESCE(employment.FirstDay,"+MONTH_START+")":salaryDate;
        String windowStart=monthly?MONTH_START:cutoff;
        String allowedStatuses=monthly?"('ACTIVE','EXPIRED','TERMINATED')":"('ACTIVE')";
        return """
            LEFT JOIN dbo.CompanyPayrollSettings payCfg ON payCfg.CompanyId=e.CompanyId
            LEFT JOIN dbo.CompanyShiftSettings shiftCfg ON shiftCfg.CompanyId=e.CompanyId
            OUTER APPLY (
                SELECT MIN(coveredStart.FirstDay) FirstDay,MAX(covered.LastDay) PayrollDate
                FROM EmploymentContracts contract
                CROSS APPLY (SELECT MIN(boundary.EndDate) LastDay
                    FROM (VALUES (contract.EndDate),(contract.TerminatedDate),(e.EndDate),(%1$s)) boundary(EndDate)) covered
                CROSS APPLY (SELECT MAX(boundary.StartDate) FirstDay
                    FROM (VALUES (contract.StartDate),(%4$s)) boundary(StartDate)) coveredStart
                WHERE contract.CompanyId=e.CompanyId AND contract.EmployeeId=e.EmployeeId
                    AND contract.Status IN %5$s
                    AND (e.Status<>'Inactive' OR e.EndDate IS NOT NULL
                        OR contract.EndDate IS NOT NULL OR contract.TerminatedDate IS NOT NULL)
                    AND coveredStart.FirstDay<=covered.LastDay
            ) employment
            CROSS APPLY (SELECT %2$s PayrollDate,%3$s StartDate) payrollWindow
            """.formatted(cutoff,salaryDate,start,windowStart,allowedStatuses)
            + history("EmployeeCompensationHistories", "compensation", "history.BaseSalary", "BaseSalary", false)
            + history("EmployeePositionAssignments", "assignment", "history.PositionId", "PositionId", true)
            + """
            LEFT JOIN Positions curPos ON curPos.CompanyId=e.CompanyId AND curPos.PositionId=e.PositionId
                AND e.DepartmentId IS NOT NULL AND curPos.DepartmentId=e.DepartmentId
            OUTER APPLY (
                SELECT COUNT(CASE WHEN policy.AtEnd=1 THEN 1 END) SourceCount,
                    COUNT(CASE WHEN policy.AtStart=1 THEN 1 END) StartSourceCount,
                    COUNT(*) PeriodSourceCount,
                    MAX(CASE WHEN policy.AtEnd=1 THEN policy.Amount END) Amount,
                    MAX(CASE WHEN policy.AtEnd=1 THEN policy.EffectiveFrom END) EffectiveFrom
                FROM (
                    SELECT policy.Amount,policy.EffectiveFrom,
                        CASE WHEN policy.EffectiveTo IS NULL OR policy.EffectiveTo>=payrollWindow.PayrollDate THEN 1 ELSE 0 END AtEnd,
                        CASE WHEN policy.EffectiveFrom<=payrollWindow.StartDate THEN 1 ELSE 0 END AtStart
                    FROM ResponsibilityAllowancePolicies policy
                    WHERE policy.CompanyId=e.CompanyId
                    AND policy.ResponsibilityLevelId=COALESCE(assignment.ResponsibilityLevelId,curPos.ResponsibilityLevelId)
                    AND policy.EffectiveFrom<=payrollWindow.PayrollDate
                    AND (policy.EffectiveTo IS NULL OR policy.EffectiveTo>=payrollWindow.StartDate)
                    AND (policy.EffectiveTo IS NULL OR policy.EffectiveTo>=policy.EffectiveFrom)
                ) policy
            ) allowance
            OUTER APPLY (
                SELECT TOP 1 policy.Amount
                FROM ResponsibilityAllowancePolicies policy
                WHERE policy.CompanyId=e.CompanyId
                  AND policy.ResponsibilityLevelId=COALESCE(assignment.ResponsibilityLevelId,curPos.ResponsibilityLevelId)
                  AND policy.EffectiveFrom<=CONVERT(date,SYSDATETIME())
                  AND (policy.EffectiveTo IS NULL OR policy.EffectiveTo>CONVERT(date,SYSDATETIME()))
                  AND (policy.EffectiveTo IS NULL OR policy.EffectiveTo>=policy.EffectiveFrom)
                ORDER BY policy.EffectiveFrom DESC,policy.AllowancePolicyId DESC
            ) currentAllowance
            OUTER APPLY (
                SELECT
                    CAST(CASE
                        WHEN COALESCE(payCfg.StandardDaysMode, 'MON_FRI') = 'FIXED'
                            THEN COALESCE(payCfg.FixedStandardDays, 22.00)
                        ELSE COALESCE(SUM(dayMetric.DayWeight), 22.0)
                    END AS DECIMAL(5,2)) AS StandardWorkDays,
                    CAST(COALESCE(SUM(dayMetric.DayCredit), 0.00) AS DECIMAL(5,2)) AS ActualWorkDays,
                    CAST(COALESCE(SUM(dayMetric.DayEarnedBaseCredit), 0.00) AS DECIMAL(18,2)) AS EarnedBasePayCredits,
                    CAST(COALESCE(SUM(dayMetric.DayScheduledBaseCredit), 0.00) AS DECIMAL(18,2)) AS ScheduledBasePayCredits,
                    CAST(COALESCE(SUM(dayMetric.DayWeight), 0.00) AS DECIMAL(5,2)) AS ScheduledWorkDays
                FROM (
                    SELECT DATEADD(day, v.n, DATEFROMPARTS(p.Year, p.Month, 1)) AS DayDate
                    FROM (VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9),(10),(11),(12),(13),(14),(15),(16),(17),(18),(19),(20),(21),(22),(23),(24),(25),(26),(27),(28),(29),(30)) v(n)
                    WHERE DATEADD(day, v.n, DATEFROMPARTS(p.Year, p.Month, 1)) <= EOMONTH(DATEFROMPARTS(p.Year, p.Month, 1))
                ) cal
                LEFT JOIN dbo.Attendances att
                    ON att.CompanyId = e.CompanyId AND att.EmployeeId = e.EmployeeId
                   AND att.AttendanceDate = cal.DayDate
                OUTER APPLY (
                    SELECT TOP 1 dc.BaseSalary
                    FROM EmployeeCompensationHistories dc
                    WHERE dc.CompanyId = e.CompanyId AND dc.EmployeeId = e.EmployeeId
                      AND dc.EffectiveFrom <= cal.DayDate
                      AND (dc.EffectiveTo IS NULL OR dc.EffectiveTo >= cal.DayDate)
                      AND (dc.EffectiveTo IS NULL OR dc.EffectiveTo >= dc.EffectiveFrom)
                    ORDER BY dc.EffectiveFrom DESC, dc.CompensationHistoryId DESC
                ) dayComp
                CROSS APPLY (
                    SELECT
                        CAST(CASE COALESCE(payCfg.StandardDaysMode, 'MON_FRI')
                            WHEN 'CUSTOM_WEEKDAYS' THEN COALESCE(TRY_CAST(JSON_VALUE(
                                CONCAT('[', COALESCE(payCfg.WeekdayWeights, '1,1,1,1,1,0,0'), ']'),
                                CONCAT('$[', DATEDIFF(day, '19000101', cal.DayDate) % 7, ']')
                            ) AS DECIMAL(5,2)), 0.0)
                            WHEN 'MON_SAT_HALF' THEN CASE
                                WHEN DATEDIFF(day, '19000101', cal.DayDate) % 7 BETWEEN 0 AND 4 THEN 1.0
                                WHEN DATEDIFF(day, '19000101', cal.DayDate) % 7 = 5 THEN 0.5
                                ELSE 0.0 END
                            WHEN 'MON_SAT' THEN CASE
                                WHEN DATEDIFF(day, '19000101', cal.DayDate) % 7 BETWEEN 0 AND 5 THEN 1.0 ELSE 0.0 END
                            ELSE CASE
                                WHEN DATEDIFF(day, '19000101', cal.DayDate) % 7 BETWEEN 0 AND 4 THEN 1.0 ELSE 0.0 END
                        END AS DECIMAL(5,2)) AS DayWeight,
                        CAST(CASE
                            WHEN att.Status IN ('Present', 'Late', 'Early Leave') THEN
                                CASE
                                    WHEN att.TotalHours >= COALESCE(shiftCfg.MinHoursFullDay, 6.00) OR (att.TotalHours = 0 AND att.Status = 'Present') THEN 1.0
                                    WHEN att.TotalHours >= COALESCE(shiftCfg.MinHoursHalfDay, 4.00) OR att.TotalHours > 0 THEN 0.5
                                    ELSE 0.0
                                END
                            WHEN att.Status = 'On Leave' AND COALESCE(payCfg.IncludePaidLeave, 1) = 1 THEN 1.0
                            WHEN COALESCE(payCfg.IncludePaidLeave, 1) = 1 AND EXISTS (
                                SELECT 1 FROM dbo.LeaveRequests lr
                                WHERE lr.CompanyId = e.CompanyId AND lr.EmployeeId = e.EmployeeId
                                  AND lr.Status = 'Approved'
                                  AND ((lr.IsLegacy=0 AND EXISTS(SELECT 1 FROM dbo.LeaveRequestDates rd
                                        WHERE rd.LeaveRequestId=lr.LeaveRequestId AND rd.LeaveDate=cal.DayDate))
                                    OR (lr.IsLegacy=1 AND lr.LegacyIsPaid=1
                                        AND cal.DayDate BETWEEN lr.StartDate AND lr.EndDate))
                            ) THEN 1.0
                            ELSE 0.0
                        END AS DECIMAL(5,2)) AS DayCredit
                ) w
                CROSS APPLY (
                    SELECT
                        w.DayWeight,
                        w.DayCredit,
                        CAST(w.DayCredit * COALESCE(dayComp.BaseSalary, 0) AS DECIMAL(18,2)) AS DayEarnedBaseCredit,
                        CAST(w.DayWeight * COALESCE(dayComp.BaseSalary, 0) AS DECIMAL(18,2)) AS DayScheduledBaseCredit
                ) dayMetric
            ) workMetrics
            """;
    }

    private static String history(String table,String alias,String value,String column,boolean position) {
        String active="history.AtEnd=1";
        String extraCols = position
                ? ",MAX(CASE WHEN "+active+" THEN history.ResponsibilityLevelId END) ResponsibilityLevelId"
                : ",COALESCE(MAX(CASE WHEN "+active+" THEN history.TravelAllowance END), CAST(0 AS DECIMAL(18,2))) TravelAllowance";
        return """
            OUTER APPLY (
                SELECT COUNT(CASE WHEN %1$s THEN 1 END) SourceCount,
                    COUNT(CASE WHEN history.AtStart=1 THEN 1 END) StartSourceCount,
                    COUNT(*) PeriodSourceCount,
                    MAX(CASE WHEN %1$s THEN %2$s END) %3$s,
                    MAX(CASE WHEN %1$s THEN history.EffectiveFrom END) EffectiveFrom,
                    MAX(CASE WHEN %1$s THEN history.EffectiveTo END) EffectiveTo
                    %4$s
                FROM (
                    SELECT history.* %8$s,
                        CASE WHEN history.EffectiveTo IS NULL OR history.EffectiveTo>=payrollWindow.PayrollDate THEN 1 ELSE 0 END AtEnd,
                        CASE WHEN history.EffectiveFrom<=payrollWindow.StartDate THEN 1 ELSE 0 END AtStart
                    FROM %5$s history %6$s
                    WHERE history.CompanyId=e.CompanyId AND history.EmployeeId=e.EmployeeId
                    AND history.EffectiveFrom<=payrollWindow.PayrollDate
                    AND (history.EffectiveTo IS NULL OR history.EffectiveTo>=payrollWindow.StartDate)
                    AND (history.EffectiveTo IS NULL OR history.EffectiveTo>=history.EffectiveFrom)
                ) history
            ) %7$s
            """.formatted(active,value,column,
                extraCols,
                table,position?"LEFT JOIN Positions position ON position.CompanyId=e.CompanyId AND position.PositionId=history.PositionId":"",alias,
                position?",position.ResponsibilityLevelId":"");
    }

    static final String PRORATED_BASE = """
        CASE
            WHEN compensation.BaseSalary IS NULL THEN NULL
            WHEN COALESCE(payCfg.SalaryCalculationMode, 'WORKDAYS') = 'FIXED_MONTHLY' THEN
                CASE WHEN workMetrics.ScheduledWorkDays > 0
                     THEN CAST(ROUND(workMetrics.ScheduledBasePayCredits / workMetrics.ScheduledWorkDays, 2) AS DECIMAL(18,2))
                     ELSE compensation.BaseSalary END
            WHEN workMetrics.StandardWorkDays > 0 THEN CAST(ROUND(workMetrics.EarnedBasePayCredits / workMetrics.StandardWorkDays, 2) AS DECIMAL(18,2))
            ELSE CAST(0 AS DECIMAL(18,2))
        END
        """;

    static final String ALLOWANCE = """
        CASE
            WHEN COALESCE(allowance.Amount,currentAllowance.Amount) IS NULL THEN CAST(0 AS DECIMAL(18,2))
            WHEN COALESCE(payCfg.IncludeResponsibilityAllowance, 1) = 1 THEN COALESCE(allowance.Amount,currentAllowance.Amount)
            ELSE CAST(0 AS DECIMAL(18,2))
        END
        """;

    static final String TRAVEL_ALLOWANCE = """
        CASE
            WHEN COALESCE(payCfg.IncludeTravelAllowance, 1) = 1 THEN COALESCE(compensation.TravelAllowance, CAST(0 AS DECIMAL(18,2)))
            ELSE CAST(0 AS DECIMAL(18,2))
        END
        """;
}
