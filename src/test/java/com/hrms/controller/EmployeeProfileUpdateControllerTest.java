package com.hrms.controller;

import com.hrms.filter.TenantAuthorizationFilter;
import com.hrms.service.ProfileUpdateService;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.SecuritySupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class EmployeeProfileUpdateControllerTest {
    @Test
    public void onlyPhoneIsPassedToTheUpdateService() throws Exception {
        Fixture fixture = new Fixture("EMPLOYEE", "/employees/me/update-profile", "POST");
        fixture.parameters.put("phone", new String[]{"0901234567"});
        RecordingService service = new RecordingService();
        new EmployeeServlet(service).doPost(fixture.request(), fixture.response());
        assertEquals(200, fixture.status);
        assertEquals(1, service.calls);
        assertEquals("0901234567", service.phone);
    }

    @Test
    public void rejectsForgedProtectedFieldsAndEmployeeIds() throws Exception {
        for (String field : List.of("fullName", "gender", "dateOfBirth", "identityNumber", "employeeId", "status")) {
            Fixture fixture = new Fixture("EMPLOYEE", "/employees/me/update-profile", "POST");
            fixture.parameters.put("phone", new String[]{"0901234567"});
            fixture.parameters.put(field, new String[]{"forged"});
            RecordingService service = new RecordingService();
            new EmployeeServlet(service).doPost(fixture.request(), fixture.response());
            assertEquals(400, fixture.status);
            assertEquals(0, service.calls);
            assertTrue(fixture.body.toString().contains("fieldErrors"));
        }
    }

    @Test
    public void closedPeriodReturnsForbiddenAndInvalidCsrfNeverCallsTheService() throws Exception {
        Fixture closed = new Fixture("MANAGER", "/employees/me/update-profile", "POST");
        closed.parameters.put("phone", new String[]{"0901234567"});
        ProfileUpdateService denied = new ProfileUpdateService() {
            @Override
            public void updatePhone(TenantPrincipal principal, String phone, String ipAddress) {
                throw new SecurityException("No open period");
            }
        };
        new EmployeeServlet(denied).doPost(closed.request(), closed.response());
        assertEquals(403, closed.status);
        Fixture invalidCsrf = new Fixture("EMPLOYEE", "/employees/me/update-profile", "POST");
        invalidCsrf.parameters.remove("csrfToken");
        RecordingService service = new RecordingService();
        new EmployeeServlet(service).doPost(invalidCsrf.request(), invalidCsrf.response());
        assertEquals(403, invalidCsrf.status);
        assertEquals(0, service.calls);
    }

    @Test
    public void authorizationFilterKeepsPeriodAdministrationAwayFromEmployeeAndManager() throws Exception {
        for (String role : List.of("EMPLOYEE", "MANAGER")) {
            for (String path : List.of("/employee/profile-update-periods", "/employees/profile-update-periods",
                    "/employees/profile-update-periods/create", "/employees/profile-update-periods/close", "/employees/update")) {
                Fixture fixture = new Fixture(role, path, path.endsWith("periods") ? "GET" : "POST");
                assertFalse(allowed(fixture));
                assertEquals(403, fixture.status);
            }
            assertTrue(allowed(new Fixture(role, "/employees/me/update-profile", "POST")));
        }
    }

    @Test
    public void hrCanManagePeriodsWithoutGainingUnrelatedAdministratorPermissions() throws Exception {
        for (String role : List.of("HR", "COMPANY_ADMIN")) {
            assertTrue(allowed(new Fixture(role, "/employee/profile-update-periods", "GET")));
            assertTrue(allowed(new Fixture(role, "/employees/profile-update-periods", "GET")));
            assertTrue(allowed(new Fixture(role, "/employees/profile-update-periods/create", "POST")));
            assertTrue(allowed(new Fixture(role, "/employees/profile-update-periods/close", "POST")));
        }
        assertFalse(allowed(new Fixture("HR", "/employees/update", "POST")));
    }

    private boolean allowed(Fixture fixture) throws Exception {
        boolean[] reached = {false};
        FilterChain chain = (request, response) -> reached[0] = true;
        new TenantAuthorizationFilter().doFilter(fixture.request(), fixture.response(), chain);
        return reached[0];
    }

    private static class RecordingService extends ProfileUpdateService {
        private int calls;
        private String phone;

        @Override
        public void updatePhone(TenantPrincipal principal, String phone, String ipAddress) {
            calls++;
            this.phone = phone;
        }
    }

    private static class Fixture {
        private final Map<String, String[]> parameters = new HashMap<>();
        private final Map<String, Object> sessionValues = new HashMap<>();
        private final String path;
        private final String method;
        private final StringWriter body = new StringWriter();
        private int status = 200;

        private Fixture(String role, String path, String method) {
            this.path = path;
            this.method = method;
            parameters.put("csrfToken", new String[]{"test-token"});
            sessionValues.put(SecuritySupport.PUBLIC_CSRF_ATTRIBUTE, "test-token");
            sessionValues.put(SecuritySupport.TENANT_PRINCIPAL_ATTRIBUTE,
                    new TenantPrincipal(20, 10, "test", "Test", "Test", "test@example.com", role, 30, false));
        }

        private HttpServletRequest request() {
            HttpSession session = (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{HttpSession.class}, (proxy, called, arguments) ->
                            "getAttribute".equals(called.getName()) ? sessionValues.get(arguments[0]) : null);
            return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{HttpServletRequest.class}, (proxy, called, arguments) -> switch (called.getName()) {
                        case "getSession" -> session;
                        case "getServletPath" -> path;
                        case "getMethod" -> method;
                        case "getHeader" -> "application/json";
                        case "getParameterMap" -> parameters;
                        case "getParameter" -> parameters.containsKey(arguments[0]) ? parameters.get(arguments[0])[0] : null;
                        case "getRemoteAddr" -> "127.0.0.1";
                        default -> null;
                    });
        }

        private HttpServletResponse response() {
            return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{HttpServletResponse.class}, (proxy, called, arguments) -> {
                        if ("setStatus".equals(called.getName())) {
                            status = (int) arguments[0];
                        }
                        return "getWriter".equals(called.getName()) ? new PrintWriter(body) : null;
                    });
        }
    }
}
