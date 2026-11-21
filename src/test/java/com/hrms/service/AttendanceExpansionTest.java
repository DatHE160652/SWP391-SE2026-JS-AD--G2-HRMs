package com.hrms.service;

import com.hrms.dao.manager.AttendanceDAO;
import com.hrms.dao.manager.AttendanceExplanationDAO;
import com.hrms.dao.manager.CompanyShiftSettingDAO;
import com.hrms.dao.manager.TimesheetLockDAO;
import com.hrms.model.AttendanceExplanation;
import com.hrms.model.CompanyShiftSetting;
import com.hrms.model.TimesheetLock;
import com.hrms.service.manager.AttendanceService;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests covering UC85, UC86, UC87, UC88:
 * - UC85: Company shift settings configuration
 * - UC86: Employee attendance explanation submission and cancellation
 * - UC87: Manager review, approval, and auto-sync of attendance records
 * - UC88: Timesheet locking and protection against unauthorized adjustments
 */
public class AttendanceExpansionTest {

    private AttendanceDAO attendanceDAO;
    private CompanyShiftSettingDAO shiftSettingDAO;
    private AttendanceExplanationDAO explanationDAO;
    private TimesheetLockDAO timesheetLockDAO;
    private AttendanceService attendanceService;

    private static final int COMPANY_ID = 1;
    private static final int EMP_ID = 100;
    private static final int MANAGER_ID = 50;
    private static final int USER_ID = 10;

    @Before
    public void setUp() throws SQLException {
        attendanceDAO = mock(AttendanceDAO.class);
        shiftSettingDAO = mock(CompanyShiftSettingDAO.class);
        explanationDAO = mock(AttendanceExplanationDAO.class);
        timesheetLockDAO = mock(TimesheetLockDAO.class);

        when(explanationDAO.isDirectSubordinate(anyInt(), anyInt(), anyInt())).thenReturn(true);

        attendanceService = new AttendanceService(attendanceDAO, shiftSettingDAO, explanationDAO, timesheetLockDAO);
    }

    // --- UC86: Submit Explanation Tests ---

    @Test
    public void testSubmitExplanation_Success() throws SQLException {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        AttendanceExplanation exp = new AttendanceExplanation(COMPANY_ID, EMP_ID, pastDate,
                LocalTime.of(8, 30), LocalTime.of(17, 30), "FORGOT_PUNCH", "Quên bấm vân tay lúc vào ca");

        when(timesheetLockDAO.isMonthLocked(COMPANY_ID, pastDate.getMonthValue(), pastDate.getYear())).thenReturn(false);
        when(explanationDAO.hasPendingExplanation(COMPANY_ID, EMP_ID, pastDate)).thenReturn(false);
        when(explanationDAO.createExplanation(any())).thenReturn(true);

        boolean result = attendanceService.submitExplanation(exp);
        assertTrue(result);
        verify(explanationDAO).createExplanation(exp);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubmitExplanation_RejectsFutureDate() throws SQLException {
        LocalDate futureDate = LocalDate.now().plusDays(2);
        AttendanceExplanation exp = new AttendanceExplanation(COMPANY_ID, EMP_ID, futureDate,
                LocalTime.of(8, 30), LocalTime.of(17, 30), "FORGOT_PUNCH", "Lý do tương lai");

        attendanceService.submitExplanation(exp);
    }

    @Test
    public void testSubmitExplanation_RejectsIfAlreadyPending() throws SQLException {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        AttendanceExplanation exp = new AttendanceExplanation(COMPANY_ID, EMP_ID, pastDate,
                LocalTime.of(8, 30), LocalTime.of(17, 30), "FORGOT_PUNCH", "Đã gửi trước đó");

        when(timesheetLockDAO.isMonthLocked(COMPANY_ID, pastDate.getMonthValue(), pastDate.getYear())).thenReturn(false);
        when(explanationDAO.hasPendingExplanation(COMPANY_ID, EMP_ID, pastDate)).thenReturn(true);

        try {
            attendanceService.submitExplanation(exp);
            fail("Expected IllegalStateException when duplicate pending explanation exists");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("chờ duyệt"));
        }
    }

    @Test
    public void testCancelExplanation_Success() throws SQLException {
        when(explanationDAO.cancelExplanation(10, EMP_ID)).thenReturn(true);
        boolean res = attendanceService.cancelExplanation(10, EMP_ID);
        assertTrue(res);
        verify(explanationDAO).cancelExplanation(10, EMP_ID);
    }

    // --- UC87: Review Explanation Tests ---

    @Test
    public void testReviewExplanation_ApproveSyncsAttendance() throws SQLException {
        LocalDate date = LocalDate.now().minusDays(2);
        AttendanceExplanation exp = new AttendanceExplanation(COMPANY_ID, EMP_ID, date,
                LocalTime.of(8, 30), LocalTime.of(17, 30), "FORGOT_PUNCH", "Quên chấm công");
        exp.setExplanationId(99);
        exp.setStatus("PENDING");

        when(explanationDAO.getExplanationById(99)).thenReturn(exp);
        when(timesheetLockDAO.isMonthLocked(COMPANY_ID, date.getMonthValue(), date.getYear())).thenReturn(false);
        when(explanationDAO.updateStatus(eq(99), eq("APPROVED"), eq(USER_ID), anyString())).thenReturn(true);
        when(shiftSettingDAO.getShiftSetting(COMPANY_ID)).thenReturn(new CompanyShiftSetting(COMPANY_ID));

        boolean res = attendanceService.reviewExplanation(99, USER_ID, MANAGER_ID, true, "Đồng ý bổ sung công", COMPANY_ID);
        assertTrue(res);
        verify(explanationDAO).updateStatus(eq(99), eq("APPROVED"), eq(USER_ID), anyString());
        // Verify attendance record upserted
        verify(attendanceDAO).upsertManualAdjustment(eq(COMPANY_ID), eq(EMP_ID), eq(date),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(17, 30)), any(), eq("Present"), anyString(), eq(USER_ID));
    }

    @Test
    public void testReviewExplanation_RejectDoesNotSyncAttendance() throws SQLException {
        LocalDate date = LocalDate.now().minusDays(2);
        AttendanceExplanation exp = new AttendanceExplanation(COMPANY_ID, EMP_ID, date,
                LocalTime.of(8, 30), LocalTime.of(17, 30), "FORGOT_PUNCH", "Quên chấm công");
        exp.setExplanationId(99);
        exp.setStatus("PENDING");

        when(explanationDAO.getExplanationById(99)).thenReturn(exp);
        when(timesheetLockDAO.isMonthLocked(COMPANY_ID, date.getMonthValue(), date.getYear())).thenReturn(false);
        when(explanationDAO.updateStatus(eq(99), eq("REJECTED"), eq(USER_ID), anyString())).thenReturn(true);

        boolean res = attendanceService.reviewExplanation(99, USER_ID, MANAGER_ID, false, "Không có minh chứng", COMPANY_ID);
        assertTrue(res);
        verify(explanationDAO).updateStatus(eq(99), eq("REJECTED"), eq(USER_ID), anyString());
        verify(attendanceDAO, never()).upsertManualAdjustment(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any());
    }

    // --- UC88: Timesheet Locking Tests ---

    @Test
    public void testTimesheetLock_PreventsManualAdjustment() throws SQLException {
        LocalDate lockedDate = LocalDate.of(2026, 10, 15);
        when(timesheetLockDAO.isMonthLocked(COMPANY_ID, 10, 2026)).thenReturn(true);

        try {
            attendanceService.manualAdjustAttendance(COMPANY_ID, EMP_ID, lockedDate,
                    LocalTime.of(8, 30), LocalTime.of(17, 30), "Sửa bù", USER_ID);
            fail("Expected IllegalStateException when modifying locked timesheet month");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("đã bị khóa"));
        }
        verify(attendanceDAO, never()).upsertManualAdjustment(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testTimesheetLock_PreventsCheckIn() throws SQLException {
        LocalDate lockedDate = LocalDate.of(2026, 10, 15);
        when(attendanceDAO.getEmployeeCompanyId(EMP_ID)).thenReturn(COMPANY_ID);
        when(timesheetLockDAO.isMonthLocked(COMPANY_ID, 10, 2026)).thenReturn(true);

        try {
            attendanceService.checkIn(EMP_ID, lockedDate, LocalTime.of(8, 30));
            fail("Expected IllegalStateException on check-in for locked month");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("đã bị khóa"));
        }
    }

    @Test
    public void testTimesheetLock_SetAndGetLock() throws SQLException {
        TimesheetLock lock = new TimesheetLock(COMPANY_ID, 10, 2026, true, USER_ID, "Chốt lương tháng 10");
        when(timesheetLockDAO.getLock(COMPANY_ID, 10, 2026)).thenReturn(lock);
        when(timesheetLockDAO.setLock(COMPANY_ID, 10, 2026, true, USER_ID, "Chốt lương tháng 10")).thenReturn(true);

        boolean setSuccess = attendanceService.setTimesheetLock(COMPANY_ID, 10, 2026, true, USER_ID, "Chốt lương tháng 10");
        assertTrue(setSuccess);

        TimesheetLock retrieved = attendanceService.getTimesheetLock(COMPANY_ID, 10, 2026);
        assertNotNull(retrieved);
        assertTrue(retrieved.isLocked());
        assertEquals("Chốt lương tháng 10", retrieved.getNotes());
    }

    @Test
    public void testTimesheetLock_AutoLocksPrecedingMonthWhenCutoffPassed() throws SQLException {
        // Suppose current date is Nov 6 (or beyond cutoff day 5)
        // Check for preceding month (or any older past month)
        CompanyShiftSetting setting = new CompanyShiftSetting(COMPANY_ID);
        setting.setTimesheetCutoffDay(1); // cutoff is day 1, so by any day >= 1 of next month it's locked
        when(shiftSettingDAO.getShiftSetting(COMPANY_ID)).thenReturn(setting);
        when(timesheetLockDAO.isMonthLocked(eq(COMPANY_ID), anyInt(), anyInt())).thenReturn(false);

        LocalDate pastMonth = LocalDate.now().minusMonths(2);
        boolean isLocked = attendanceService.isMonthLocked(COMPANY_ID, pastMonth.getMonthValue(), pastMonth.getYear());
        assertTrue("Past month older than 1 month should be auto-locked", isLocked);

        TimesheetLock autoLock = attendanceService.getTimesheetLock(COMPANY_ID, pastMonth.getMonthValue(), pastMonth.getYear());
        assertNotNull(autoLock);
        assertTrue(autoLock.isLocked());
        assertTrue(autoLock.getNotes().contains("Tự động khóa theo ngày chốt công"));
    }
}

