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

    private ApiRoutes() {
    }
}
