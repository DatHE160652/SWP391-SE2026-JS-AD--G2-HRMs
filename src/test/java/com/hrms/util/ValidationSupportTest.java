package com.hrms.util;

import com.hrms.service.FieldValidationException;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public class ValidationSupportTest {
    @Test
    public void includesFieldNameForValidationErrors() {
        assertEquals("{\"success\":false,\"message\":\"Duplicate\",\"fieldErrors\":{\"phone\":\"Duplicate\"}}",
                ValidationSupport.errorJson(new FieldValidationException("phone", "Duplicate")));
    }

    @Test
    public void keepsServiceFailuresSeparateFromFieldErrors() {
        assertEquals("{\"success\":false,\"message\":\"Unavailable\"}",
                ValidationSupport.errorJson(new SQLException("Unavailable")));
    }

    @Test
    public void escapesQuotesBackslashesAndControlCharacters() {
        assertEquals("{\"success\":false,\"message\":\"\\\"quoted\\\"\\\\path\\u000a\\u0009\"}",
                ValidationSupport.errorJson(new IllegalArgumentException("\"quoted\"\\path\n\t")));
    }
}
