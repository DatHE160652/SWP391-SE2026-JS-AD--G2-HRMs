package com.hrms.util;

import java.util.Locale;
import java.util.regex.Pattern;

/** Shared email validation for the platform and workspace login screens. */
public final class EmailSupport {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private EmailSupport() { }

    public static String normalize(String value) {
        if (value == null) return null;
        String email = value.trim().toLowerCase(Locale.ROOT);
        return email.length() <= 150 && EMAIL.matcher(email).matches() ? email : null;
    }
}
