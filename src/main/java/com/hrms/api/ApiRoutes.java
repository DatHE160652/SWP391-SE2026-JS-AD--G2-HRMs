package com.hrms.api;

/** Central route definitions shared by public portal and System Admin controllers. */
public final class ApiRoutes {
    public static final String PUBLIC_PORTAL = "/";
    public static final String PUBLIC_HOME = "/homepage";
    public static final String PUBLIC_FEATURES = "/features-solutions";
    public static final String PUBLIC_PRICING = "/Pricing";
    public static final String PUBLIC_FAQ = "/faqs";
    public static final String PUBLIC_CONSULTATION = "/consultation-request";
    public static final String PUBLIC_FAQ_TOPICS = "/faqs/topics";
    public static final String PUBLIC_FAQ_COMMENTS = "/faqs/comments";
    public static final String SYSTEM_ADMIN_PREFIX = "/systemadmin";
    public static final String SYSTEM_ADMIN_LOGIN = SYSTEM_ADMIN_PREFIX + "/login";
    public static final String SYSTEM_ADMIN_LOGOUT = SYSTEM_ADMIN_PREFIX + "/logout";
    public static final String SYSTEM_ADMIN_DASHBOARD = SYSTEM_ADMIN_PREFIX + "/dashboard";
    public static final String REGISTRATION_REQUESTS = SYSTEM_ADMIN_PREFIX + "/registration-request";
    public static final String REGISTRATION_REQUEST_DETAILS = SYSTEM_ADMIN_PREFIX + "/registration-request-details";
    public static final String TENANT_WORKSPACE_TEMPLATE = "https://{companydomain}.hrms.com/";
    public static final String TENANT_LOGIN = "/login";
    public static final String CANONICAL_ORIGIN = "https://hrms.com";

    // --- Attendance Flow (BF-03) ---
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
    public static final String ATTENDANCE_META = ATTENDANCE_PREFIX + "/meta";

    // Attendance View Pages (Canonical routes)
    public static final String ATTENDANCE_PAGE_MANAGER = ATTENDANCE_PREFIX + "/manager-attendance";
    public static final String ATTENDANCE_PAGE_EMPLOYEE = ATTENDANCE_PREFIX + "/employee-attendance";
    public static final String ATTENDANCE_PAGE_COMPANY = ATTENDANCE_PREFIX + "/company-attendance";
    public static final String ATTENDANCE_PAGE_REPORT = ATTENDANCE_PREFIX + "/attendance-report";
    public static final String VIEW_ATTENDANCE_MANAGER = ATTENDANCE_PREFIX + "/manager-attendance.html";
    public static final String VIEW_ATTENDANCE_EMPLOYEE = ATTENDANCE_PREFIX + "/employee-attendance.html";
    public static final String VIEW_ATTENDANCE_COMPANY = ATTENDANCE_PREFIX + "/company-attendance.html";
    public static final String VIEW_ATTENDANCE_REPORT = ATTENDANCE_PREFIX + "/attendance-report.html";

    // Compatibility / Legacy Manager Routes (Redirect to canonical /attendance routes)
    public static final String MANAGER_TEAM_ATTENDANCE = "/manager/team-attendance";
    public static final String MANAGER_EMPLOYEE_ATTENDANCE = "/manager/employee-attendance";
    public static final String MANAGER_EMPLOYEE_ATTENDANCE_HTML = "/manager/employee-attendance.html";
    public static final String MANAGER_COMPANY_ATTENDANCE_HTML = "/manager/company-attendance.html";
    public static final String MANAGER_ATTENDANCE_REPORT_HTML = "/manager/attendance-report.html";

    private ApiRoutes() {
    }
}
