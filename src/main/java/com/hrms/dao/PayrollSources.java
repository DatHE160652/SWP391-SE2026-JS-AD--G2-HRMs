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
             WHEN compensation.EffectiveFrom>payrollWindow.StartDate THEN N'Lương thay đổi giữa tháng; chưa có quy tắc tính cho kỳ này'
             WHEN compensation.PeriodSourceCount>1 THEN N'Lịch sử lương bị chồng lấn trong kỳ'
             WHEN assignment.SourceCount=0 THEN N'Thiếu phân công chức danh có hiệu lực trong kỳ'
             WHEN assignment.SourceCount>1 THEN N'Phân công chức danh bị chồng lấn'
             WHEN assignment.StartSourceCount=0 THEN N'Thiếu phân công chức danh ở đầu thời gian có hợp đồng trong kỳ'
             WHEN assignment.EffectiveFrom>payrollWindow.StartDate THEN N'Phân công chức danh thay đổi giữa tháng; chưa có quy tắc tính cho kỳ này'
             WHEN assignment.PeriodSourceCount>1 THEN N'Phân công chức danh bị chồng lấn trong kỳ'
             WHEN assignment.ResponsibilityLevelId IS NULL THEN N'Thiếu cấp bậc trách nhiệm của chức danh'
             WHEN allowance.SourceCount=0 THEN N'Thiếu chính sách phụ cấp trách nhiệm có hiệu lực trong kỳ'
             WHEN allowance.SourceCount>1 THEN N'Chính sách phụ cấp bị chồng lấn'
             WHEN allowance.StartSourceCount=0 THEN N'Thiếu chính sách phụ cấp ở đầu thời gian có hợp đồng trong kỳ'
             WHEN allowance.EffectiveFrom>payrollWindow.StartDate THEN N'Chính sách phụ cấp thay đổi giữa tháng; chưa có quy tắc tính cho kỳ này'
             WHEN allowance.PeriodSourceCount>1 THEN N'Chính sách phụ cấp bị chồng lấn trong kỳ'
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
        return """
            OUTER APPLY (
                SELECT MIN(coveredStart.FirstDay) FirstDay,MAX(covered.LastDay) PayrollDate
                FROM EmploymentContracts contract
                CROSS APPLY (SELECT MIN(boundary.EndDate) LastDay
                    FROM (VALUES (contract.EndDate),(contract.TerminatedDate),(e.EndDate),(%1$s)) boundary(EndDate)) covered
                CROSS APPLY (SELECT MAX(boundary.StartDate) FirstDay
                    FROM (VALUES (contract.StartDate),(DATEFROMPARTS(p.Year,p.Month,1))) boundary(StartDate)) coveredStart
                WHERE contract.CompanyId=e.CompanyId AND contract.EmployeeId=e.EmployeeId
                    AND contract.Status IN ('ACTIVE','EXPIRED','TERMINATED')
                    AND (e.Status<>'Inactive' OR e.EndDate IS NOT NULL
                        OR contract.EndDate IS NOT NULL OR contract.TerminatedDate IS NOT NULL)
                    AND coveredStart.FirstDay<=covered.LastDay
            ) employment
            CROSS APPLY (SELECT %2$s PayrollDate,%3$s StartDate) payrollWindow
            """.formatted(cutoff,salaryDate,start)
            + history("EmployeeCompensationHistories", "compensation", "history.BaseSalary", "BaseSalary", false)
            + history("EmployeePositionAssignments", "assignment", "history.PositionId", "PositionId", true)
            + """
            OUTER APPLY (
                SELECT COUNT(CASE WHEN policy.AtEnd=1 THEN 1 END) SourceCount,
                    COUNT(CASE WHEN policy.AtStart=1 THEN 1 END) StartSourceCount,
                    COUNT(*) PeriodSourceCount,
                    MAX(CASE WHEN policy.AtEnd=1 THEN policy.Amount END) Amount,
                    MAX(CASE WHEN policy.AtEnd=1 THEN policy.EffectiveFrom END) EffectiveFrom
                FROM (
                    SELECT policy.Amount,policy.EffectiveFrom,
                        CASE WHEN policy.EffectiveTo IS NULL OR policy.EffectiveTo>payrollWindow.PayrollDate THEN 1 ELSE 0 END AtEnd,
                        CASE WHEN policy.EffectiveFrom<=payrollWindow.StartDate THEN 1 ELSE 0 END AtStart
                    FROM ResponsibilityAllowancePolicies policy
                    WHERE policy.CompanyId=e.CompanyId AND policy.ResponsibilityLevelId=assignment.ResponsibilityLevelId
                    AND policy.EffectiveFrom<=payrollWindow.PayrollDate
                    AND (policy.EffectiveTo IS NULL OR policy.EffectiveTo>payrollWindow.StartDate)
                    AND (policy.EffectiveTo IS NULL OR policy.EffectiveTo>policy.EffectiveFrom)
                ) policy
            ) allowance
            """;
    }

    private static String history(String table,String alias,String value,String column,boolean position) {
        String active="history.AtEnd=1";
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
                        CASE WHEN history.EffectiveTo IS NULL OR history.EffectiveTo>payrollWindow.PayrollDate THEN 1 ELSE 0 END AtEnd,
                        CASE WHEN history.EffectiveFrom<=payrollWindow.StartDate THEN 1 ELSE 0 END AtStart
                    FROM %5$s history %6$s
                    WHERE history.CompanyId=e.CompanyId AND history.EmployeeId=e.EmployeeId
                    AND history.EffectiveFrom<=payrollWindow.PayrollDate
                    AND (history.EffectiveTo IS NULL OR history.EffectiveTo>payrollWindow.StartDate)
                    AND (history.EffectiveTo IS NULL OR history.EffectiveTo>history.EffectiveFrom)
                ) history
            ) %7$s
            """.formatted(active,value,column,
                position?",MAX(CASE WHEN "+active+" THEN history.ResponsibilityLevelId END) ResponsibilityLevelId":"",
                table,position?"LEFT JOIN Positions position ON position.CompanyId=e.CompanyId AND position.PositionId=history.PositionId":"",alias,
                position?",position.ResponsibilityLevelId":"");
    }

    static final String ALLOWANCE = "allowance.Amount";
}
