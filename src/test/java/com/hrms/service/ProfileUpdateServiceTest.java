package com.hrms.service;

import com.hrms.dao.ProfileUpdateDAO;
import com.hrms.model.ProfileUpdatePeriod;
import com.hrms.service.security.TenantPrincipal;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.Assert.*;

public class ProfileUpdateServiceTest {
    private final RecordingDAO dao = new RecordingDAO();
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 18, 10, 0);
    private final ProfileUpdateService service = new ProfileUpdateService(dao,
            Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneOffset.UTC));

    @Test
    public void onlyCompanyAdminAndHrCanManagePeriods() throws Exception {
        for (String role : List.of("COMPANY_ADMIN", "HR")) {
            assertEquals(42L, service.create(principal(role), "  Phone update  ", now, now.plusDays(1), "127.0.0.1"));
            assertEquals("Phone update", dao.title);
            assertEquals(10, dao.companyId);
            service.close(principal(role), 42, "127.0.0.1");
        }
        for (String role : List.of("EMPLOYEE", "MANAGER", "SYSTEM_ADMIN")) {
            assertThrows(SecurityException.class,
                    () -> service.create(principal(role), "Test", now, now.plusDays(1), "127.0.0.1"));
            assertThrows(SecurityException.class, () -> service.getPeriods(principal(role)));
            assertThrows(SecurityException.class, () -> service.close(principal(role), 42, "127.0.0.1"));
        }
    }

    @Test
    public void rejectsInvalidPeriodDatesAndTitles() {
        assertThrows(FieldValidationException.class,
                () -> service.create(principal("HR"), " ", now, now.plusHours(1), "127.0.0.1"));
        assertThrows(FieldValidationException.class,
                () -> service.create(principal("HR"), "Test", null, now.plusHours(1), "127.0.0.1"));
        for (LocalDateTime end : List.of(now.minusHours(1), now)) {
            FieldValidationException exception = assertThrows(FieldValidationException.class,
                    () -> service.create(principal("COMPANY_ADMIN"), "Test", now, end, "127.0.0.1"));
            assertEquals("endsAt", exception.getField());
        }
    }

    @Test
    public void employeeAndManagerCanOnlySubmitTheirOwnPhone() throws Exception {
        for (String role : List.of("EMPLOYEE", "MANAGER")) {
            service.updatePhone(principal(role), " 0901234567 ", "127.0.0.1");
            assertEquals("0901234567", dao.phone);
            assertEquals(10, dao.companyId);
            assertEquals(20, dao.userId);
            assertEquals(30, dao.employeeId);
        }
        for (String role : List.of("COMPANY_ADMIN", "HR", "SYSTEM_ADMIN")) {
            assertThrows(SecurityException.class, () -> service.updatePhone(principal(role), "0901234567", "127.0.0.1"));
            assertNull(service.getAvailablePeriod(principal(role)));
        }
    }

    @Test
    public void rejectsInvalidPhonesWithInlineFieldName() {
        for (String phone : new String[]{null, "", "abc", "901234567", "09012345678"}) {
            FieldValidationException exception = assertThrows(FieldValidationException.class,
                    () -> service.updatePhone(principal("EMPLOYEE"), phone, "127.0.0.1"));
            assertEquals("phone", exception.getField());
        }
    }

    @Test
    public void rejectsMissingEmployeeAndPasswordChangeRequiredSessions() {
        TenantPrincipal missingEmployee = new TenantPrincipal(20, 10, "test", "Test", "Test", "test@example.com",
                "EMPLOYEE", null, false);
        TenantPrincipal temporaryPassword = new TenantPrincipal(20, 10, "test", "Test", "Test", "test@example.com",
                "MANAGER", 30, true);
        assertThrows(SecurityException.class, () -> service.updatePhone(null, "0901234567", "127.0.0.1"));
        assertThrows(SecurityException.class, () -> service.updatePhone(missingEmployee, "0901234567", "127.0.0.1"));
        assertThrows(SecurityException.class, () -> service.updatePhone(temporaryPassword, "0901234567", "127.0.0.1"));
    }

    @Test
    public void periodStartsInclusivelyAndEndsExclusively() {
        ProfileUpdatePeriod period = new ProfileUpdatePeriod(42, 10, "Test", now, now.plusHours(1), null);
        assertFalse(period.isOpen(now.minusNanos(1)));
        assertTrue(period.isOpen(now));
        assertFalse(period.isOpen(now.plusHours(1)));
        assertFalse(new ProfileUpdatePeriod(42, 10, "Test", now, now.plusHours(1), now).isOpen(now));
    }

    private TenantPrincipal principal(String role) {
        return new TenantPrincipal(20, 10, "test", "Test", "Test", "test@example.com", role, 30, false);
    }

    private static class RecordingDAO extends ProfileUpdateDAO {
        private int companyId;
        private int userId;
        private int employeeId;
        private String title;
        private String phone;

        @Override
        public long create(int companyId, int userId, String title, LocalDateTime startsAt, LocalDateTime endsAt,
                           String ipAddress) {
            this.companyId = companyId;
            this.title = title;
            return 42;
        }

        @Override
        public void close(int companyId, int userId, long periodId, String ipAddress) {
            this.companyId = companyId;
        }

        @Override
        public void updatePhone(int companyId, int userId, int employeeId, String phone, String ipAddress) {
            this.companyId = companyId;
            this.userId = userId;
            this.employeeId = employeeId;
            this.phone = phone;
        }
    }
}
