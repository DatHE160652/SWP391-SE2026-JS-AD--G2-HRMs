package com.hrms.controller.manager;

import com.hrms.dao.manager.AttendanceDAO.AttendanceDTO;
import com.hrms.dao.manager.AttendanceReportDAO;
import com.hrms.dao.manager.AttendanceSummaryDAO;
import com.hrms.model.CompanyShiftSetting;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.SecuritySupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Common test helper for Attendance module screen unit tests.
 */
public final class AttendanceTestHelper {

    public static final String DEFAULT_CSRF_TOKEN = "test-public-csrf-token-xyz123";

    private AttendanceTestHelper() {}

    public static TenantPrincipal createPrincipal(int userId, int companyId, Integer employeeId, String roleCode, String displayName) {
        return new TenantPrincipal(
                userId,
                companyId,
                "WS_TEST",
                "Test Workspace Co.",
                displayName,
                displayName.toLowerCase().replace(" ", "") + "@example.com",
                roleCode,
                employeeId,
                false
        );
    }

    public static HttpSession setupMockSession(HttpServletRequest request, TenantPrincipal principal) {
        return setupMockSession(request, principal, DEFAULT_CSRF_TOKEN);
    }

    public static HttpSession setupMockSession(HttpServletRequest request, TenantPrincipal principal, String csrfToken) {
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(true)).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);

        if (principal != null) {
            when(session.getAttribute(SecuritySupport.TENANT_PRINCIPAL_ATTRIBUTE)).thenReturn(principal);
        }
        if (csrfToken != null) {
            when(session.getAttribute(SecuritySupport.PUBLIC_CSRF_ATTRIBUTE)).thenReturn(csrfToken);
        }
        return session;
    }

    public static AttendanceDTO createAttendanceDTO(long attendanceId, int companyId, int employeeId,
                                                    String employeeCode, String fullName,
                                                    Integer deptId, String deptName,
                                                    LocalDate date, LocalTime checkIn, LocalTime checkOut,
                                                    BigDecimal totalHours, String status) {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setAttendanceId(attendanceId);
        dto.setCompanyId(companyId);
        dto.setEmployeeId(employeeId);
        dto.setEmployeeCode(employeeCode);
        dto.setFullName(fullName);
        dto.setDepartmentId(deptId);
        dto.setDepartmentName(deptName);
        dto.setAttendanceDate(date);
        dto.setCheckInTime(checkIn);
        dto.setCheckOutTime(checkOut);
        dto.setTotalHours(totalHours);
        dto.setWorkCredit(AttendanceDTO.calculateWorkCredit(totalHours));
        dto.setStatus(status);
        dto.setPositionTitle("Nhân viên");
        dto.setRoleCode("EMPLOYEE");
        return dto;
    }

    public static CompanyShiftSetting createShiftSetting(int companyId, LocalTime start, LocalTime end, int lunch, int grace) {
        CompanyShiftSetting setting = new CompanyShiftSetting();
        setting.setCompanyId(companyId);
        setting.setShiftStartTime(start);
        setting.setShiftEndTime(end);
        setting.setLunchBreakMinutes(lunch);
        setting.setGracePeriodMinutes(grace);
        setting.setUpdatedByName("Quản lý A");
        return setting;
    }

    public static AttendanceReportDAO createReport(int employeeId, String code, String name, String dept,
                                                   int workDays, BigDecimal hours, int present, int late,
                                                   int early, int absent, int onLeave, double rate) {
        AttendanceReportDAO r = new AttendanceReportDAO();
        r.setEmployeeId(employeeId);
        r.setEmployeeCode(code);
        r.setFullName(name);
        r.setDepartmentName(dept);
        r.setPositionTitle("Nhân viên");
        r.setTotalWorkDays(workDays);
        r.setTotalHours(hours);
        r.setPresentCount(present);
        r.setLateCount(late);
        r.setEarlyLeaveCount(early);
        r.setAbsentCount(absent);
        r.setOnLeaveCount(onLeave);
        r.setPunctualityRate(rate);
        return r;
    }

    public static AttendanceSummaryDAO createSummary(int workDays, BigDecimal hours, int present, int late,
                                                     int early, int absent, int onLeave) {
        return createSummary(workDays, BigDecimal.valueOf(workDays), hours, present, late, early, absent, onLeave);
    }

    public static AttendanceSummaryDAO createSummary(int workDays, BigDecimal workCredits, BigDecimal hours, int present, int late,
                                                     int early, int absent, int onLeave) {
        AttendanceSummaryDAO s = new AttendanceSummaryDAO();
        s.setTotalWorkingDays(workDays);
        s.setTotalWorkCredits(workCredits);
        s.setTotalHours(hours);
        s.setPresentDays(present);
        s.setLateDays(late);
        s.setEarlyLeaveDays(early);
        s.setAbsentDays(absent);
        s.setOnLeaveDays(onLeave);
        return s;
    }
}

