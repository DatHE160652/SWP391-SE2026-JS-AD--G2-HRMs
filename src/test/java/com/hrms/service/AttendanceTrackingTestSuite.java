package com.hrms.service;

import com.hrms.dao.manager.AttendanceDAO;
import com.hrms.dao.manager.AttendanceDAO.AttendanceDTO;
import com.hrms.dao.manager.AttendanceExplanationDAO;
import com.hrms.dao.manager.CompanyShiftSettingDAO;
import com.hrms.dao.manager.TimesheetLockDAO;
import com.hrms.model.AttendanceExplanation;
import com.hrms.model.CompanyShiftSetting;
import com.hrms.model.TimesheetLock;
import com.hrms.service.manager.AttendanceService;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test Suite for WF4: Attendance Tracking Management
 * Covering Unit Test Cases WF4-UT-01 through WF4-UT-25.
 * Framework: JUnit 4
 * Author: SonDH
 */
public class AttendanceTrackingTestSuite {

    private AttendanceDAO attendanceDAO;
    private CompanyShiftSettingDAO shiftSettingDAO;
    private AttendanceExplanationDAO explanationDAO;
    private TimesheetLockDAO timesheetLockDAO;
    private AttendanceService attendanceService;

    private static final int COMPANY_ID = 1;
    private static final int EMP_ID = 101;
    private static final int MANAGER_ID = 50;
    private static final int USER_ID = 10;

    @Before
    public void setUp() {
        attendanceDAO = mock(AttendanceDAO.class);
        shiftSettingDAO = mock(CompanyShiftSettingDAO.class);
        explanationDAO = mock(AttendanceExplanationDAO.class);
        timesheetLockDAO = mock(TimesheetLockDAO.class);

        attendanceService = new AttendanceService(attendanceDAO, shiftSettingDAO, explanationDAO, timesheetLockDAO);
    }

    // =========================================================================
    // SECTION 1: SHIFT SETTINGS & GRACE PERIOD (BR-067 / UC85)
    // =========================================================================

    /**
     * WF4-UT-01: Verify default shift parameters and late threshold without grace period.
     */
    @Test
    public void testDefaultShiftParametersAndLateThreshold() {
        CompanyShiftSetting setting = new CompanyShiftSetting(COMPANY_ID);
        assertEquals(LocalTime.of(8, 30), setting.getShiftStartTime());
        assertEquals(LocalTime.of(17, 30), setting.getShiftEndTime());
        assertEquals(60, setting.getLunchBreakMinutes());
        assertEquals(0, setting.getGracePeriodMinutes());
        assertEquals(LocalTime.of(8, 30), setting.getLateThreshold());
    }

    /**
     * WF4-UT-02: Verify grace period shifts late threshold dynamically (e.g. 15 mins).
     */
    @Test
    public void testShiftGracePeriodDynamicThreshold() {
        CompanyShiftSetting setting = new CompanyShiftSetting(COMPANY_ID, LocalTime.of(8, 30), LocalTime.of(17, 30), 60, 15);
        assertEquals(LocalTime.of(8, 45), setting.getLateThreshold());

        LocalTime withinGrace = LocalTime.of(8, 40);
        LocalTime beyondGrace = LocalTime.of(8, 46);
        assertFalse(withinGrace.isAfter(setting.getLateThreshold()));
        assertTrue(beyondGrace.isAfter(setting.getLateThreshold()));
    }

    /**
     * WF4-UT-03: Validate shift setting rejects end time before start time.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testShiftRejectsEndTimeBeforeStartTime() throws Exception {
        CompanyShiftSetting invalid = new CompanyShiftSetting(COMPANY_ID, LocalTime.of(17, 30), LocalTime.of(8, 30), 60, 0);
        CompanyShiftSettingDAO dao = new CompanyShiftSettingDAO();
        dao.saveShiftSetting(invalid);
    }

    /**
     * WF4-UT-04: Validate shift setting rejects negative grace period or lunch break.
     */
    @Test
    public void testShiftRejectsNegativeGraceOrLunchBreak() throws Exception {
        CompanyShiftSettingDAO dao = new CompanyShiftSettingDAO();
        try {
            dao.saveShiftSetting(new CompanyShiftSetting(COMPANY_ID, LocalTime.of(8, 30), LocalTime.of(17, 30), -30, 0));
            fail("Expected IllegalArgumentException for negative lunch break");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("nghỉ trưa"));
        }

        try {
            dao.saveShiftSetting(new CompanyShiftSetting(COMPANY_ID, LocalTime.of(8, 30), LocalTime.of(17, 30), 60, -10));
            fail("Expected IllegalArgumentException for negative grace period");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("ân hạn"));
        }
    }

    // =========================================================================
    // SECTION 2: WORK CREDIT THRESHOLDS & LUNCH DEDUCTION (BR-064 / BR-069)
    // =========================================================================

    /**
     * WF4-UT-05: Verify automatic statutory lunch break deduction of 60 minutes.
     */
    @Test
    public void testStatutoryLunchBreakDeduction() {
        CompanyShiftSetting setting = new CompanyShiftSetting(COMPANY_ID, LocalTime.of(8, 30), LocalTime.of(17, 30), 60, 0);
        LocalTime in = LocalTime.of(8, 30);
        LocalTime out = LocalTime.of(17, 30);

        long rawMinutes = Duration.between(in, out).toMinutes(); // 540 mins = 9 hrs
        long netMinutes = rawMinutes - setting.getLunchBreakMinutes(); // 480 mins = 8 hrs
        BigDecimal netHours = BigDecimal.valueOf(netMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        assertEquals(new BigDecimal("8.00"), netHours);
    }

    /**
     * WF4-UT-06: Verify configurable work credit thresholds (4.0h -> 0.5 công, >=6.0h -> 1.0 công).
     */
    @Test
    public void testConfigurableWorkCreditThresholds() {
        CompanyShiftSetting setting = new CompanyShiftSetting(COMPANY_ID, LocalTime.of(8, 30), LocalTime.of(17, 30), 60, 0,
                new BigDecimal("4.00"), new BigDecimal("6.00"));

        assertEquals(BigDecimal.valueOf(1.0), setting.calculateWorkCredit(new BigDecimal("8.00")));
        assertEquals(BigDecimal.valueOf(1.0), setting.calculateWorkCredit(new BigDecimal("6.00")));
        assertEquals(BigDecimal.valueOf(0.5), setting.calculateWorkCredit(new BigDecimal("5.50")));
        assertEquals(BigDecimal.valueOf(0.5), setting.calculateWorkCredit(new BigDecimal("4.00")));
        assertEquals(BigDecimal.ZERO, setting.calculateWorkCredit(new BigDecimal("3.50")));
        assertEquals(BigDecimal.ZERO, setting.calculateWorkCredit(null));
    }

    /**
     * WF4-UT-07: Validate shift setting rejects full-day credit threshold <= half-day threshold.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidationRejectsFullDayLessThanHalfDay() throws Exception {
        CompanyShiftSetting invalid = new CompanyShiftSetting(COMPANY_ID, LocalTime.of(8, 30), LocalTime.of(17, 30), 60, 0,
                new BigDecimal("6.00"), new BigDecimal("4.00"));
        CompanyShiftSettingDAO dao = new CompanyShiftSettingDAO();
        dao.saveShiftSetting(invalid);
    }

    // =========================================================================
    // SECTION 3: CHECK-IN & CHECK-OUT STATUS EVALUATION (UC29 / UC30 / BR-062)
    // =========================================================================

    /**
     * WF4-UT-08: Check-in before or at shift start evaluated as Present.
     */
    @Test
    public void testCheckInOnTimeEvaluatedAsPresent() {
        CompanyShiftSetting setting = new CompanyShiftSetting(COMPANY_ID, LocalTime.of(8, 30), LocalTime.of(17, 30), 60, 0);
        LocalTime checkInTime = LocalTime.of(8, 25);
        String status = checkInTime.isAfter(setting.getLateThreshold()) ? "Late" : "Present";
        assertEquals("Present", status);
    }

    /**
     * WF4-UT-09: Check-in after late threshold evaluated as Late.
     */
    @Test
    public void testCheckInAfterThresholdEvaluatedAsLate() {
        CompanyShiftSetting setting = new CompanyShiftSetting(COMPANY_ID, LocalTime.of(8, 30), LocalTime.of(17, 30), 60, 5);
        LocalTime lateCheckIn = LocalTime.of(8, 40); // threshold is 08:35
        String status = lateCheckIn.isAfter(setting.getLateThreshold()) ? "Late" : "Present";
        assertEquals("Late", status);
    }

    /**
     * WF4-UT-10: Check-out before shift end evaluated as Early Leave.
     */
    @Test
    public void testCheckOutBeforeShiftEndEvaluatedAsEarlyLeave() {
        CompanyShiftSetting setting = new CompanyShiftSetting(COMPANY_ID, LocalTime.of(8, 30), LocalTime.of(17, 30), 60, 0);
        LocalTime earlyCheckOut = LocalTime.of(17, 10);
        String status = earlyCheckOut.isBefore(setting.getShiftEndTime()) ? "Early Leave" : "Present";
        assertEquals("Early Leave", status);
    }

    /**
     * WF4-UT-11: Check-out at or after shift end evaluated as Present.
     */
    @Test
    public void testCheckOutOnTimeEvaluatedAsPresent() {
        CompanyShiftSetting setting = new CompanyShiftSetting(COMPANY_ID, LocalTime.of(8, 30), LocalTime.of(17, 30), 60, 0);
        LocalTime onTimeCheckOut = LocalTime.of(17, 30);
        String status = onTimeCheckOut.isBefore(setting.getShiftEndTime()) ? "Early Leave" : "Present";
        assertEquals("Present", status);
    }

    // =========================================================================
    // SECTION 4: SAME-DAY ADJUSTMENT & RETROSPECTIVE CONSTRAINTS (UC32 / BR-065)
    // =========================================================================

    /**
     * WF4-UT-12: Manager successfully adjusts attendance with audit trail and shift calculation.
     */
    @Test
    public void testManagerSameDayManualAdjustmentSuccess() throws SQLException {
        LocalDate today = LocalDate.now();
        when(timesheetLockDAO.isMonthLocked(COMPANY_ID, today.getMonthValue(), today.getYear())).thenReturn(false);
        when(shiftSettingDAO.getShiftSetting(COMPANY_ID)).thenReturn(new CompanyShiftSetting(COMPANY_ID));
        when(attendanceDAO.upsertManualAdjustment(anyInt(), anyInt(), any(), any(), any(), any(), any(), anyString(), anyInt())).thenReturn(101L);

        AttendanceDTO mockDto = new AttendanceDTO();
        mockDto.setAttendanceId(101L);
        mockDto.setManualAdjusted(true);
        mockDto.setAdjustedByUserId(USER_ID);
        when(attendanceDAO.getAttendanceByEmployeeAndDate(EMP_ID, today)).thenReturn(mockDto);

        AttendanceDTO result = attendanceService.manualAdjustAttendance(COMPANY_ID, EMP_ID, today,
                LocalTime.of(8, 30), LocalTime.of(17, 30), "Quản lý xác nhận công thực tế", USER_ID);

        assertNotNull(result);
        assertTrue(Boolean.TRUE.equals(result.getManualAdjusted()));
        assertEquals(Integer.valueOf(USER_ID), result.getAdjustedByUserId());
        verify(attendanceDAO).upsertManualAdjustment(eq(COMPANY_ID), eq(EMP_ID), eq(today),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(17, 30)), any(), eq("Present"), anyString(), eq(USER_ID));
    }

    /**
     * WF4-UT-13: Validate manual adjustment rejects check-out time before check-in time.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testManualAdjustmentRejectsCheckOutBeforeCheckIn() throws SQLException {
        LocalDate today = LocalDate.now();
        when(timesheetLockDAO.isMonthLocked(COMPANY_ID, today.getMonthValue(), today.getYear())).thenReturn(false);

        attendanceService.manualAdjustAttendance(COMPANY_ID, EMP_ID, today,
                LocalTime.of(17, 30), LocalTime.of(8, 30), "Giờ sai", USER_ID);
    }

    // =========================================================================
    // SECTION 5: ATTENDANCE EXPLANATION LIFECYCLE (UC86 / UC87)
    // =========================================================================

    /**
     * WF4-UT-14: Employee successfully submits attendance explanation for past work date.
     */
    @Test
    public void testSubmitExplanation_Success() throws SQLException {
        LocalDate pastDate = LocalDate.now().minusDays(2);
        AttendanceExplanation exp = new AttendanceExplanation(COMPANY_ID, EMP_ID, pastDate,
                LocalTime.of(8, 30), LocalTime.of(17, 30), "FORGOT_PUNCH", "Quên chấm công buổi sáng");

        when(timesheetLockDAO.isMonthLocked(COMPANY_ID, pastDate.getMonthValue(), pastDate.getYear())).thenReturn(false);
        when(explanationDAO.hasPendingExplanation(COMPANY_ID, EMP_ID, pastDate)).thenReturn(false);
        when(explanationDAO.createExplanation(any())).thenReturn(true);

        boolean ok = attendanceService.submitExplanation(exp);
        assertTrue(ok);
        verify(explanationDAO).createExplanation(exp);
    }

    /**
     * WF4-UT-15: Reject explanation submission for future dates.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSubmitExplanation_RejectsFutureDate() throws SQLException {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        AttendanceExplanation exp = new AttendanceExplanation(COMPANY_ID, EMP_ID, futureDate,
                LocalTime.of(8, 30), LocalTime.of(17, 30), "FORGOT_PUNCH", "Giải trình trước");

        attendanceService.submitExplanation(exp);
    }

    /**
     * WF4-UT-16: Reject explanation if a pending explanation already exists for same date.
     */
    @Test
    public void testSubmitExplanation_RejectsDuplicatePending() throws SQLException {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        AttendanceExplanation exp = new AttendanceExplanation(COMPANY_ID, EMP_ID, pastDate,
                LocalTime.of(8, 30), LocalTime.of(17, 30), "FORGOT_PUNCH", "Gửi trùng");

        when(timesheetLockDAO.isMonthLocked(COMPANY_ID, pastDate.getMonthValue(), pastDate.getYear())).thenReturn(false);
        when(explanationDAO.hasPendingExplanation(COMPANY_ID, EMP_ID, pastDate)).thenReturn(true);

        try {
            attendanceService.submitExplanation(exp);
            fail("Expected IllegalStateException for duplicate pending explanation");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("chờ duyệt"));
        }
    }

    /**
     * WF4-UT-17: Manager approval of explanation updates status to APPROVED and syncs attendance.
     */
    @Test
    public void testReviewExplanation_ApproveUpdatesStatusAndSyncsAttendance() throws SQLException {
        LocalDate date = LocalDate.now().minusDays(3);
        AttendanceExplanation exp = new AttendanceExplanation(COMPANY_ID, EMP_ID, date,
                LocalTime.of(8, 30), LocalTime.of(17, 30), "FORGOT_PUNCH", "Quên chấm công");
        exp.setExplanationId(201);
        exp.setStatus("PENDING");

        when(explanationDAO.getExplanationById(201)).thenReturn(exp);
        when(timesheetLockDAO.isMonthLocked(COMPANY_ID, date.getMonthValue(), date.getYear())).thenReturn(false);
        when(explanationDAO.updateStatus(eq(201), eq("APPROVED"), eq(USER_ID), anyString())).thenReturn(true);
        when(shiftSettingDAO.getShiftSetting(COMPANY_ID)).thenReturn(new CompanyShiftSetting(COMPANY_ID));

        boolean approved = attendanceService.reviewExplanation(201, USER_ID, MANAGER_ID, true, "Đã duyệt", COMPANY_ID);
        assertTrue(approved);
        verify(explanationDAO).updateStatus(eq(201), eq("APPROVED"), eq(USER_ID), anyString());
        verify(attendanceDAO).upsertManualAdjustment(eq(COMPANY_ID), eq(EMP_ID), eq(date),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(17, 30)), any(), eq("Present"), anyString(), eq(USER_ID));
    }

    /**
     * WF4-UT-18: Manager rejection of explanation updates status to REJECTED without modifying attendance.
     */
    @Test
    public void testReviewExplanation_RejectDoesNotSyncAttendance() throws SQLException {
        LocalDate date = LocalDate.now().minusDays(3);
        AttendanceExplanation exp = new AttendanceExplanation(COMPANY_ID, EMP_ID, date,
                LocalTime.of(8, 30), LocalTime.of(17, 30), "BUSINESS_TRIP", "Công tác không minh chứng");
        exp.setExplanationId(202);
        exp.setStatus("PENDING");

        when(explanationDAO.getExplanationById(202)).thenReturn(exp);
        when(timesheetLockDAO.isMonthLocked(COMPANY_ID, date.getMonthValue(), date.getYear())).thenReturn(false);
        when(explanationDAO.updateStatus(eq(202), eq("REJECTED"), eq(USER_ID), anyString())).thenReturn(true);

        boolean rejected = attendanceService.reviewExplanation(202, USER_ID, MANAGER_ID, false, "Thiếu chứng từ", COMPANY_ID);
        assertTrue(rejected);
        verify(explanationDAO).updateStatus(eq(202), eq("REJECTED"), eq(USER_ID), anyString());
        verify(attendanceDAO, never()).upsertManualAdjustment(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any());
    }

    // =========================================================================
    // SECTION 6: TIMESHEET LOCK PROTECTION (UC88 / BR-068)
    // =========================================================================

    /**
     * WF4-UT-19: Timesheet monthly lock blocks retrospective direct adjustments.
     */
    @Test
    public void testTimesheetLockBlocksDirectAdjustment() throws SQLException {
        LocalDate lockedDate = LocalDate.of(2026, 10, 15);
        when(timesheetLockDAO.isMonthLocked(COMPANY_ID, 10, 2026)).thenReturn(true);

        try {
            attendanceService.manualAdjustAttendance(COMPANY_ID, EMP_ID, lockedDate,
                    LocalTime.of(8, 30), LocalTime.of(17, 30), "Sửa tháng đã chốt", USER_ID);
            fail("Expected IllegalStateException for locked month adjustment");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("đã bị khóa"));
        }
    }

    /**
     * WF4-UT-20: Timesheet monthly lock blocks explanation submissions.
     */
    @Test
    public void testTimesheetLockBlocksExplanationSubmission() throws SQLException {
        LocalDate lockedDate = LocalDate.of(2026, 10, 20);
        AttendanceExplanation exp = new AttendanceExplanation(COMPANY_ID, EMP_ID, lockedDate,
                LocalTime.of(8, 30), LocalTime.of(17, 30), "FORGOT_PUNCH", "Giải trình tháng cũ");

        when(timesheetLockDAO.isMonthLocked(COMPANY_ID, 10, 2026)).thenReturn(true);

        try {
            attendanceService.submitExplanation(exp);
            fail("Expected IllegalStateException for locked month explanation");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("đã bị khóa"));
        }
    }

    /**
     * WF4-UT-21: Timesheet monthly lock toggle (Lock and Unlock) persists state.
     */
    @Test
    public void testTimesheetLockToggleState() throws SQLException {
        when(timesheetLockDAO.setLock(COMPANY_ID, 10, 2026, true, USER_ID, "Chốt bảng lương")).thenReturn(true);
        boolean locked = attendanceService.setTimesheetLock(COMPANY_ID, 10, 2026, true, USER_ID, "Chốt bảng lương");
        assertTrue(locked);
        verify(timesheetLockDAO).setLock(COMPANY_ID, 10, 2026, true, USER_ID, "Chốt bảng lương");

        when(timesheetLockDAO.setLock(COMPANY_ID, 10, 2026, false, USER_ID, "Mở lại điều chỉnh")).thenReturn(true);
        boolean unlocked = attendanceService.setTimesheetLock(COMPANY_ID, 10, 2026, false, USER_ID, "Mở lại điều chỉnh");
        assertTrue(unlocked);
    }

    // =========================================================================
    // SECTION 7: QUERY SCOPING & TENANT ISOLATION (UC31 / UC35 / BR-070)
    // =========================================================================

    /**
     * WF4-UT-22: Personal attendance query strictly filters by employee ID and month/year.
     */
    @Test
    public void testPersonalAttendanceQueryScoping() throws SQLException {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(EMP_ID);
        dto.setAttendanceDate(LocalDate.of(2026, 11, 10));

        when(attendanceDAO.getPersonalAttendance(EMP_ID, 11, 2026)).thenReturn(Collections.singletonList(dto));

        List<AttendanceDTO> list = attendanceService.getPersonalAttendance(COMPANY_ID, EMP_ID, 11, 2026);
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(Integer.valueOf(EMP_ID), list.get(0).getEmployeeId());
        verify(attendanceDAO).getPersonalAttendance(EMP_ID, 11, 2026);
    }

    /**
     * WF4-UT-23: Team attendance query scoped to manager's subordinates on specified date.
     */
    @Test
    public void testTeamAttendanceQueryScoping() throws SQLException {
        LocalDate today = LocalDate.now();
        AttendanceDTO emp1 = new AttendanceDTO();
        emp1.setEmployeeId(101);
        AttendanceDTO emp2 = new AttendanceDTO();
        emp2.setEmployeeId(102);

        when(attendanceDAO.getTeamAttendance(COMPANY_ID, MANAGER_ID, today)).thenReturn(Arrays.asList(emp1, emp2));

        List<AttendanceDTO> team = attendanceService.getTeamAttendance(COMPANY_ID, MANAGER_ID, today);
        assertEquals(2, team.size());
        verify(attendanceDAO).getTeamAttendance(COMPANY_ID, MANAGER_ID, today);
    }

    /**
     * WF4-UT-24: Company attendance search enforces tenant company ID isolation.
     */
    @Test
    public void testCompanyAttendanceSearchEnforcesTenantIsolation() throws SQLException {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 30);

        when(attendanceDAO.searchCompanyAttendance(COMPANY_ID, 5, "Late", from, to, "Nguyen"))
                .thenReturn(Collections.emptyList());

        List<AttendanceDTO> results = attendanceService.searchCompanyAttendance(COMPANY_ID, 5, "Late", from, to, "Nguyen");
        assertNotNull(results);
        verify(attendanceDAO).searchCompanyAttendance(COMPANY_ID, 5, "Late", from, to, "Nguyen");
    }

    /**
     * WF4-UT-25: Timesheet cutoff day configuration validation (fallback to 5 if invalid).
     */
    @Test
    public void testTimesheetCutoffDayValidationAndFallback() {
        CompanyShiftSetting setting = new CompanyShiftSetting(COMPANY_ID);
        assertEquals(5L, (long) setting.getTimesheetCutoffDay());

        setting.setTimesheetCutoffDay(15);
        assertEquals(15L, (long) setting.getTimesheetCutoffDay());

        setting.setTimesheetCutoffDay(-1);
        assertEquals(5L, (long) setting.getTimesheetCutoffDay()); // fallback to default 5
    }
}
