package com.hrms.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Provides JDBC connections to the application's SQL Server database. */
public final class DBConnection {

    private static final String DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private static final String DEFAULT_SERVER = "localhost";
    private static final String DEFAULT_PORT = "1433";
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

        String server = getSetting("HRM_DB_SERVER", "hrm.db.server", DEFAULT_SERVER);
        String port = getSetting("HRM_DB_PORT", "hrm.db.port", DEFAULT_PORT);
        String database = getSetting("HRM_DB_NAME", "hrm.db.name", DEFAULT_DATABASE);
        String user = getRequiredSetting("HRM_DB_USER", "hrm.db.user");
        String password = getRequiredSetting("HRM_DB_PASSWORD", "hrm.db.password");

        String url = "jdbc:sqlserver://" + server + ":" + port
                + ";databaseName=" + database
                + ";encrypt=true;trustServerCertificate=true";

        return DriverManager.getConnection(url, user, password);
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
