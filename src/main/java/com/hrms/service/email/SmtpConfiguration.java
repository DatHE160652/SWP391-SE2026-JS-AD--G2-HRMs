package com.hrms.service.email;

import java.util.Properties;
import java.util.function.Function;

/** Process-level SMTP settings; never serialized or logged with credentials. */
final class SmtpConfiguration {
    final String host;
    final String from;
    final String username;
    final String password;
    final String protocol;
    final Properties properties;

    private SmtpConfiguration(String host, String from, String username, String password,
                              String protocol, Properties properties) {
        this.host = host;
        this.from = from;
        this.username = username;
        this.password = password;
        this.protocol = protocol;
        this.properties = properties;
    }

    static SmtpConfiguration load() throws EmailDeliveryException {
        return load(System::getenv, System::getProperty);
    }

    static SmtpConfiguration load(Function<String, String> environment, Function<String, String> systemProperty)
            throws EmailDeliveryException {
        String host = setting(environment, systemProperty, "HRM_SMTP_HOST", "hrm.smtp.host", "smtp.gmail.com").trim();
        String from = setting(environment, systemProperty, "HRM_SMTP_FROM", "hrm.smtp.from", "").trim();
        String username = setting(environment, systemProperty, "HRM_SMTP_USER", "hrm.smtp.user", "").trim();
        String password = setting(environment, systemProperty, "HRM_SMTP_PASSWORD", "hrm.smtp.password", "");
        boolean ssl = Boolean.parseBoolean(setting(environment, systemProperty, "HRM_SMTP_SSL", "hrm.smtp.ssl", "false"));
        boolean tls = !ssl && Boolean.parseBoolean(setting(environment, systemProperty,
                "HRM_SMTP_STARTTLS", "hrm.smtp.starttls", "true"));
        if (host.isBlank() || from.isBlank()
                || ("smtp.gmail.com".equalsIgnoreCase(host) && (username.isBlank() || password.isBlank()))) {
            throw new EmailDeliveryException("MAIL_NOT_CONFIGURED", "Required SMTP settings are missing.");
        }
        if ("smtp.gmail.com".equalsIgnoreCase(host) && !ssl && !tls) {
            throw new EmailDeliveryException("MAIL_CONFIGURATION_INVALID", "Gmail SMTP requires TLS or SSL.");
        }
        String protocol = ssl ? "smtps" : "smtp";
        String prefix = "mail." + protocol + ".";
        Properties properties = new Properties();
        properties.setProperty(prefix + "host", host);
        properties.setProperty(prefix + "port", setting(environment, systemProperty,
                "HRM_SMTP_PORT", "hrm.smtp.port", ssl ? "465" : "587").trim());
        properties.setProperty(prefix + "auth", Boolean.toString(!username.isBlank()));
        properties.setProperty(prefix + "starttls.enable", Boolean.toString(tls));
        properties.setProperty(prefix + "starttls.required", Boolean.toString(tls));
        properties.setProperty(prefix + "ssl.checkserveridentity", "true");
        properties.setProperty(prefix + "connectiontimeout", "5000");
        properties.setProperty(prefix + "timeout", "10000");
        properties.setProperty(prefix + "writetimeout", "10000");
        return new SmtpConfiguration(host, from, username, password, protocol, properties);
    }

    private static String setting(Function<String, String> environment, Function<String, String> systemProperty,
                                  String environmentKey, String propertyKey, String fallback) {
        String value = environment.apply(environmentKey);
        if (value == null || value.isBlank()) value = systemProperty.apply(propertyKey);
        return value == null || value.isBlank() ? fallback : value;
    }
}
