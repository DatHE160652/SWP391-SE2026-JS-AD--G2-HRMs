package com.hrms.api;

/** Central route definitions shared by public portal and System Admin controllers. */
public final class ApiRoutes {
    public static final String PUBLIC_PORTAL = "/";
    public static final String PUBLIC_HOME = "/homepage";
    public static final String PUBLIC_FEATURES = "/features-solutions";
    public static final String PUBLIC_PRICING = "/Pricing";
    public static final String PUBLIC_FAQ = "/faqs";
    public static final String PUBLIC_CONSULTATION = "/consultation-request";
    public static final String SYSTEM_ADMIN_PREFIX = "/systemadmin";
    public static final String SYSTEM_ADMIN_LOGIN = SYSTEM_ADMIN_PREFIX + "/login";
    public static final String SYSTEM_ADMIN_LOGOUT = SYSTEM_ADMIN_PREFIX + "/logout";
    public static final String SYSTEM_ADMIN_DASHBOARD = SYSTEM_ADMIN_PREFIX + "/dashboard";
    public static final String REGISTRATION_REQUESTS = SYSTEM_ADMIN_PREFIX + "/registration-request";
    public static final String REGISTRATION_REQUEST_DETAILS = SYSTEM_ADMIN_PREFIX + "/registration-request-details";
    public static final String REGISTRATION_REQUEST_CONFIRM = REGISTRATION_REQUESTS + "/confirm";
    public static final String REGISTRATION_REQUEST_DELETE = REGISTRATION_REQUESTS + "/delete";
    public static final String SYSTEM_ADMIN_COMPANIES = SYSTEM_ADMIN_PREFIX + "/company";
    public static final String SYSTEM_ADMIN_COMPANY_DETAILS = SYSTEM_ADMIN_PREFIX + "/company-details";
    public static final String SYSTEM_ADMIN_COMPANY_SUSPEND = SYSTEM_ADMIN_COMPANIES + "/suspend";
    public static final String SYSTEM_ADMIN_COMPANY_ACTIVATE = SYSTEM_ADMIN_COMPANIES + "/activate";
    public static final String TENANT_WORKSPACE_TEMPLATE = "https://{companydomain}.hrms.com/";
    public static final String TENANT_LOGIN = "/login";
    public static final String TENANT_LOGOUT = "/logout";
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

    // Attendance View Pages (Canonical routes)
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

    // Compatibility / Legacy Manager Routes (Redirect to canonical /attendance routes)
    public static final String MANAGER_TEAM_ATTENDANCE = "/manager/team-attendance";
    public static final String MANAGER_EMPLOYEE_ATTENDANCE = "/manager/employee-attendance";
    public static final String MANAGER_EMPLOYEE_ATTENDANCE_HTML = "/manager/employee-attendance.html";
    public static final String MANAGER_COMPANY_ATTENDANCE_HTML = "/manager/company-attendance.html";
    public static final String MANAGER_ATTENDANCE_REPORT_HTML = "/manager/attendance-report.html";

    // --- Leave Approval Management Flow (BF-05) ---
    public static final String LEAVE_PREFIX = "/leave";

    // Leave View Page Routes
    public static final String LEAVE_PAGE_TYPES = LEAVE_PREFIX + "/types";
    public static final String LEAVE_PAGE_MY_REQUESTS = LEAVE_PREFIX + "/my-requests";
    public static final String LEAVE_PAGE_SUBMIT = LEAVE_PREFIX + "/submit";
    public static final String LEAVE_PAGE_PENDING_APPROVALS = LEAVE_PREFIX + "/pending-approvals";
    public static final String LEAVE_PAGE_ALL_REQUESTS = LEAVE_PREFIX + "/all-requests";

    // Leave JSP Views
    public static final String VIEW_LEAVE_POLICY = "/WEB-INF/views/comadmin-hr/leave-policy.jsp";
    public static final String VIEW_LEAVE_MY_REQUESTS = "/WEB-INF/views/employee/leave-my-requests.jsp";
    public static final String VIEW_LEAVE_SUBMIT = "/WEB-INF/views/employee/leave-submit.jsp";
    public static final String VIEW_LEAVE_PENDING_APPROVALS = "/WEB-INF/views/manager/leave-pending.jsp";
    public static final String VIEW_LEAVE_ALL_REQUESTS = "/WEB-INF/views/comadmin-hr/leave-all-requests.jsp";

    // Leave API Endpoints
    public static final String LEAVE_API_TYPES = LEAVE_PREFIX + "/api/types";
    public static final String LEAVE_API_MY_REQUESTS = LEAVE_PREFIX + "/api/my-requests";
    public static final String LEAVE_API_SUBMIT = LEAVE_PREFIX + "/api/submit";
    public static final String LEAVE_API_CANCEL = LEAVE_PREFIX + "/api/cancel";
    public static final String LEAVE_API_PENDING_APPROVALS = LEAVE_PREFIX + "/api/pending-approvals";
    public static final String LEAVE_API_APPROVE = LEAVE_PREFIX + "/api/approve";
    public static final String LEAVE_API_REJECT = LEAVE_PREFIX + "/api/reject";
    public static final String LEAVE_API_ALL_REQUESTS = LEAVE_PREFIX + "/api/all-requests";

    private ApiRoutes() {
    }
}
