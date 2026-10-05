package com.hrms.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Provides JDBC connections to the application's SQL Server database. */
public final class DBConnection {

    private static final String DEFAULT_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private static final String DEFAULT_SERVER = "localhost";
    private static final String DEFAULT_INSTANCE = "SQLEXPRESS";
    private static final String DEFAULT_DATABASE = "HRMsDB";
    private static final String CONFIG_RESOURCE = "/db.properties";

    private DBConnection() {
        // Utility class.
    }

    public static Connection getConnection() throws SQLException {
        Properties fileProperties = loadProperties();
        String driver = getSetting(fileProperties, "HRM_DB_DRIVER", "hrm.db.driver",
                "db.driver", DEFAULT_DRIVER);

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQL Server JDBC driver was not found.", e);
        }

        String authentication = getSetting(fileProperties, "HRM_DB_AUTH", "hrm.db.auth",
                "db.auth", "SQL").trim();
        if ("WINDOWS".equalsIgnoreCase(authentication)) {
            return openWindowsConnection(fileProperties);
        }
        if (!"SQL".equalsIgnoreCase(authentication)) {
            throw new SQLException("HRM_DB_AUTH must be WINDOWS or SQL.");
        }
        return openSqlAuthenticationConnection(fileProperties);
    }

    private static Connection openSqlAuthenticationConnection(Properties fileProperties)
            throws SQLException {
        String url = getSetting(fileProperties, "HRM_DB_URL", "hrm.db.url", "db.url", null);
        if (url == null || url.isBlank()) {
            url = buildServerUrl(fileProperties);
        }

        String user = getRequiredSetting(fileProperties, "HRM_DB_USER", "hrm.db.user", "db.username");
        String password = getRequiredSetting(fileProperties, "HRM_DB_PASSWORD", "hrm.db.password", "db.password");
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", user);
        connectionProperties.setProperty("password", password);
        return DriverManager.getConnection(url, connectionProperties);
    }

    private static Connection openWindowsConnection(Properties fileProperties) throws SQLException {
        String url = getSetting(fileProperties, "HRM_DB_URL", "hrm.db.url", "db.url", null);
        if (url == null || url.isBlank()) {
            url = buildServerUrl(fileProperties);
        }

        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("integratedSecurity", "true");
        connectionProperties.setProperty("authenticationScheme", "NativeAuthentication");
        connectionProperties.setProperty("loginTimeout", "15");
        return DriverManager.getConnection(url, connectionProperties);
    }

    private static String buildServerUrl(Properties fileProperties) {
        String server = getSetting(fileProperties, "HRM_DB_SERVER", "hrm.db.server",
                "db.server", DEFAULT_SERVER);
        String port = getSetting(fileProperties, "HRM_DB_PORT", "hrm.db.port", "db.port", "").trim();
        String database = getSetting(fileProperties, "HRM_DB_NAME", "hrm.db.name",
                "db.database", DEFAULT_DATABASE);
        StringBuilder url = new StringBuilder("jdbc:sqlserver://").append(server);
        if (!port.isEmpty()) {
            url.append(':').append(port);
        } else {
            String instance = getSetting(fileProperties, "HRM_DB_INSTANCE", "hrm.db.instance",
                    "db.instance", DEFAULT_INSTANCE);
            if (!instance.isBlank()) {
                url.append(';').append("instanceName=").append(instance);
            }
        }
        return url.append(";databaseName=").append(database)
                .append(";encrypt=true;trustServerCertificate=true").toString();
    }

    private static Properties loadProperties() throws SQLException {
        Properties properties = new Properties();
        try (InputStream input = DBConnection.class.getResourceAsStream(CONFIG_RESOURCE)) {
            if (input != null) {
                properties.load(input);
            }
            return properties;
        } catch (IOException exception) {
            throw new SQLException("Unable to load database configuration resource.", exception);
        }
    }

    private static String getSetting(Properties fileProperties, String environmentVariable,
                                     String systemProperty, String fileProperty, String defaultValue) {
        String value = System.getenv(environmentVariable);
        if (value == null || value.isBlank()) {
            value = System.getProperty(systemProperty);
        }
        if (value == null || value.isBlank()) {
            value = fileProperties.getProperty(fileProperty);
        }
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static String getRequiredSetting(Properties fileProperties, String environmentVariable,
                                             String systemProperty, String fileProperty)
            throws SQLException {
        String value = getSetting(fileProperties, environmentVariable, systemProperty, fileProperty, null);
        if (value == null || value.isBlank()) {
            throw new SQLException("Missing database configuration: " + environmentVariable);
        }
        return value;
    }
}
