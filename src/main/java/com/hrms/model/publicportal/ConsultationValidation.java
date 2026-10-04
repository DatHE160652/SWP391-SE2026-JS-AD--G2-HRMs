package com.hrms.model.publicportal;

import com.hrms.model.RegistrationRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConsultationValidation {
    private final RegistrationRequest request;
    private final Map<String, String> errors;

    private ConsultationValidation(RegistrationRequest request, Map<String, String> errors) {
        this.request = request;
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public static ConsultationValidation valid(RegistrationRequest request) {
        return new ConsultationValidation(request, Map.of());
    }

    public static ConsultationValidation invalid(RegistrationRequest request, Map<String, String> errors) {
        return new ConsultationValidation(request, errors);
    }

    public RegistrationRequest getRequest() { return request; }
    public Map<String, String> getErrors() { return errors; }
    public boolean isValid() { return errors.isEmpty(); }
}
