package com.hrms.api;

/**
 * Route constants for unit testing against Attendance and Export controllers.
 */
public final class ApiRoutes {
    public static final String ATTENDANCE_PREFIX = "/attendance";
    public static final String ATTENDANCE_PERSONAL = ATTENDANCE_PREFIX + "/personal";
    public static final String ATTENDANCE_TEAM = ATTENDANCE_PREFIX + "/team";
    public static final String ATTENDANCE_ALL = ATTENDANCE_PREFIX + "/all";
    public static final String ATTENDANCE_CHECK_IN = ATTENDANCE_PREFIX + "/check-in";
    public static final String ATTENDANCE_CHECK_OUT = ATTENDANCE_PREFIX + "/check-out";
    public static final String ATTENDANCE_BATCH_COMPLETE = ATTENDANCE_PREFIX + "/batch-complete";
    public static final String ATTENDANCE_COMPANY = ATTENDANCE_PREFIX + "/company";
    public static final String ATTENDANCE_REPORT = ATTENDANCE_PREFIX + "/report";
    public static final String ATTENDANCE_EXPORT_REPORT = ATTENDANCE_PREFIX + "/export-report";
    public static final String ATTENDANCE_EXPORT_TIMESHEET = ATTENDANCE_PREFIX + "/export-timesheet";
    public static final String ATTENDANCE_META = ATTENDANCE_PREFIX + "/meta";
    public static final String ATTENDANCE_SHIFT_SETTING = ATTENDANCE_PREFIX + "/shift-setting";
    public static final String ATTENDANCE_MANUAL_ADJUST = ATTENDANCE_PREFIX + "/manual-adjust";
    public static final String ATTENDANCE_EXPLANATION_SUBMIT = ATTENDANCE_PREFIX + "/explanation/submit";
    public static final String ATTENDANCE_EXPLANATION_MY = ATTENDANCE_PREFIX + "/explanation/my-requests";
    public static final String ATTENDANCE_EXPLANATION_CANCEL = ATTENDANCE_PREFIX + "/explanation/cancel";
    public static final String ATTENDANCE_EXPLANATION_PENDING = ATTENDANCE_PREFIX + "/explanation/pending";
    public static final String ATTENDANCE_EXPLANATION_REVIEW = ATTENDANCE_PREFIX + "/explanation/review";
    public static final String ATTENDANCE_TIMESHEET_LOCK = ATTENDANCE_PREFIX + "/timesheet-lock";

    public static final String ATTENDANCE_PAGE_MANAGER = ATTENDANCE_PREFIX + "/manager-attendance";
    public static final String ATTENDANCE_PAGE_EMPLOYEE = ATTENDANCE_PREFIX + "/employee-attendance";
    public static final String ATTENDANCE_PAGE_COMPANY = ATTENDANCE_PREFIX + "/company-attendance";
    public static final String ATTENDANCE_PAGE_REPORT = ATTENDANCE_PREFIX + "/attendance-report";
    public static final String ATTENDANCE_PAGE_MANAGER_PERSONAL = ATTENDANCE_PREFIX + "/manager-personal-attendance";
    public static final String VIEW_ATTENDANCE_MANAGER = "/WEB-INF/views/manager/manager-attendance.jsp";
    public static final String VIEW_ATTENDANCE_EMPLOYEE = "/WEB-INF/views/employee/employee-attendance.jsp";
    public static final String VIEW_ATTENDANCE_COMPANY = "/WEB-INF/views/comadmin-hr/company-attendance.jsp";
    public static final String VIEW_ATTENDANCE_MANAGER_PERSONAL = "/WEB-INF/views/manager/manager-personal-attendance.jsp";
    public static final String VIEW_ATTENDANCE_REPORT = "/WEB-INF/views/manager/attendance-report.jsp";

    public static final String MANAGER_TEAM_ATTENDANCE = "/manager/team-attendance";
    public static final String MANAGER_EMPLOYEE_ATTENDANCE = "/manager/employee-attendance";
    public static final String MANAGER_EMPLOYEE_ATTENDANCE_HTML = "/manager/employee-attendance.html";
    public static final String MANAGER_COMPANY_ATTENDANCE_HTML = "/manager/company-attendance.html";
    public static final String MANAGER_ATTENDANCE_REPORT_HTML = "/manager/attendance-report.html";

    private ApiRoutes() {}
}
