package com.hrms.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Provides JDBC connections to the application's SQL Server database. */
public final class DBConnection {

    private static final String DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private static final String DEFAULT_SERVER = "localhost";
    private static final String DEFAULT_INSTANCE = "SQLEXPRESS";
    private static final String DEFAULT_DATABASE = "HRMsDB";

    private DBConnection() {
        // Utility class.
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQL Server JDBC driver was not found.", e);
        }

        String authentication = getSetting("HRM_DB_AUTH", "hrm.db.auth", "SQL").trim();
        boolean windowsAuthentication = "WINDOWS".equalsIgnoreCase(authentication);
        if (!windowsAuthentication && !"SQL".equalsIgnoreCase(authentication)) {
            throw new SQLException("HRM_DB_AUTH must be WINDOWS or SQL.");
        }

        Properties properties = new Properties();
        properties.setProperty("serverName", getSetting("HRM_DB_SERVER", "hrm.db.server", DEFAULT_SERVER));
        properties.setProperty("databaseName", getSetting("HRM_DB_NAME", "hrm.db.name", DEFAULT_DATABASE));
        properties.setProperty("encrypt", "true");
        // Local development only: use a trusted server certificate in production.
        properties.setProperty("trustServerCertificate", "true");
        properties.setProperty("loginTimeout", "15");

        String port = getSetting("HRM_DB_PORT", "hrm.db.port", "").trim();
        if (!port.isEmpty()) {
            // An explicit TCP port takes precedence; SQL Server Browser is not needed.
            properties.setProperty("portNumber", port);
        } else if (windowsAuthentication) {
            properties.setProperty("instanceName",
                    getSetting("HRM_DB_INSTANCE", "hrm.db.instance", DEFAULT_INSTANCE));
        } else {
            properties.setProperty("portNumber", "1433");
        }

        if (windowsAuthentication) {
            // Uses the Windows identity running Tomcat, not the web application's Users table.
            // Requires the matching x64 mssql-jdbc_auth DLL on java.library.path.
            properties.setProperty("integratedSecurity", "true");
            properties.setProperty("authenticationScheme", "NativeAuthentication");
        } else {
            properties.setProperty("user", getRequiredSetting("HRM_DB_USER", "hrm.db.user"));
            properties.setProperty("password", getRequiredSetting("HRM_DB_PASSWORD", "hrm.db.password"));
        }

        return DriverManager.getConnection("jdbc:sqlserver://", properties);
    }

    private static String getSetting(String environmentVariable,
                                     String systemProperty,
                                     String defaultValue) {
        String value = System.getenv(environmentVariable);
        if (value == null || value.isBlank()) {
            value = System.getProperty(systemProperty);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String getRequiredSetting(String environmentVariable,
                                             String systemProperty) throws SQLException {
        String value = System.getenv(environmentVariable);
        if (value == null || value.isBlank()) {
            value = System.getProperty(systemProperty);
        }
        if (value == null || value.isBlank()) {
            throw new SQLException("Missing database configuration: " + environmentVariable);
        }
        return value;
    }
}
