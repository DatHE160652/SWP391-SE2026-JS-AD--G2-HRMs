package com.hrms.service;

import com.hrms.dao.manager.AttendanceDAO;
import com.hrms.dao.manager.AttendanceSummaryDAO;
import com.hrms.dao.manager.CompanyShiftSettingDAO;
import com.hrms.model.CompanyShiftSetting;
import com.hrms.service.manager.AttendanceService;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AttendanceShiftSettingTest {

    @Test
    public void testDefaultCompanyShiftSetting() {
        CompanyShiftSetting setting = new CompanyShiftSetting(1);
        assertEquals(1, setting.getCompanyId());
        assertEquals(LocalTime.of(8, 30), setting.getShiftStartTime());
        assertEquals(LocalTime.of(17, 30), setting.getShiftEndTime());
        assertEquals(60, setting.getLunchBreakMinutes());
        assertEquals(0, setting.getGracePeriodMinutes());
        assertEquals(LocalTime.of(8, 30), setting.getLateThreshold());
    }

    @Test
    public void testLateThresholdWithGracePeriod() {
        CompanyShiftSetting setting = new CompanyShiftSetting(2, LocalTime.of(9, 0), LocalTime.of(18, 0), 60, 15);
        assertEquals(LocalTime.of(9, 15), setting.getLateThreshold());

        // Check time before and after grace period
        LocalTime onTime = LocalTime.of(9, 10);
        LocalTime late = LocalTime.of(9, 16);
        assertFalse(onTime.isAfter(setting.getLateThreshold()));
        assertTrue(late.isAfter(setting.getLateThreshold()));
    }

    @Test
    public void testValidationRejectsInvalidShiftTimes() {
        CompanyShiftSettingDAO dao = new CompanyShiftSettingDAO();

        // 1. End time before start time
        CompanyShiftSetting invalidTimes = new CompanyShiftSetting(1, LocalTime.of(18, 0), LocalTime.of(9, 0), 60, 0);
        try {
            dao.saveShiftSetting(invalidTimes);
            fail("Expected IllegalArgumentException when end time is before start time");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("sau giờ bắt đầu"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }

        // 2. Negative lunch break
        CompanyShiftSetting invalidLunch = new CompanyShiftSetting(1, LocalTime.of(8, 30), LocalTime.of(17, 30), -10, 0);
        try {
            dao.saveShiftSetting(invalidLunch);
            fail("Expected IllegalArgumentException for negative lunch break");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("nghỉ trưa"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }

        // 3. Negative grace period
        CompanyShiftSetting invalidGrace = new CompanyShiftSetting(1, LocalTime.of(8, 30), LocalTime.of(17, 30), 60, -5);
        try {
            dao.saveShiftSetting(invalidGrace);
            fail("Expected IllegalArgumentException for negative grace period");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("ân hạn"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testShiftBasedCheckInStatusDetermination() {
        // Company A: 08:00 - 17:00, grace: 0
        CompanyShiftSetting companyA = new CompanyShiftSetting(10, LocalTime.of(8, 0), LocalTime.of(17, 0), 60, 0);
        LocalTime checkInA1 = LocalTime.of(8, 0);
        LocalTime checkInA2 = LocalTime.of(8, 1);
        assertEquals("Present", checkInA1.isAfter(companyA.getLateThreshold()) ? "Late" : "Present");
        assertEquals("Late", checkInA2.isAfter(companyA.getLateThreshold()) ? "Late" : "Present");

        // Company B: 09:00 - 18:00, grace: 15
        CompanyShiftSetting companyB = new CompanyShiftSetting(20, LocalTime.of(9, 0), LocalTime.of(18, 0), 60, 15);
        LocalTime checkInB1 = LocalTime.of(9, 14);
        LocalTime checkInB2 = LocalTime.of(9, 16);
        assertEquals("Present", checkInB1.isAfter(companyB.getLateThreshold()) ? "Late" : "Present");
        assertEquals("Late", checkInB2.isAfter(companyB.getLateThreshold()) ? "Late" : "Present");
    }

    @Test
    public void testShiftBasedCheckOutStatusAndLunchDeduction() {
        // Company configured with lunch break = 45 mins, shift end = 17:30
        CompanyShiftSetting setting = new CompanyShiftSetting(1, LocalTime.of(8, 30), LocalTime.of(17, 30), 45, 0);

        LocalTime checkIn = LocalTime.of(8, 30);
        LocalTime earlyCheckOut = LocalTime.of(17, 15);
        LocalTime onTimeCheckOut = LocalTime.of(17, 30);

        // Status
        String statusEarly = earlyCheckOut.isBefore(setting.getShiftEndTime()) ? "Early Leave" : "Present";
        String statusOnTime = onTimeCheckOut.isBefore(setting.getShiftEndTime()) ? "Early Leave" : "Present";
        assertEquals("Early Leave", statusEarly);
        assertEquals("Present", statusOnTime);

        // Duration & lunch deduction: 8:30 to 17:30 = 9 hours = 540 minutes
        long minutes = Duration.between(checkIn, onTimeCheckOut).toMinutes();
        assertEquals(540, minutes);
        if (minutes >= 300 && setting.getLunchBreakMinutes() > 0) {
            minutes = Math.max(0, minutes - setting.getLunchBreakMinutes());
        }
        assertEquals(540 - 45, minutes); // 495 minutes

        BigDecimal totalHours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("8.25"), totalHours);
    }

    @Test
    public void testWorkCreditCalculation_FullHalfZeroOnly() {
        // >= 7 hours -> 1.0 công
        assertEquals(BigDecimal.valueOf(1.0), com.hrms.dao.manager.AttendanceDAO.AttendanceDTO.calculateWorkCredit(new BigDecimal("7.00")));
        assertEquals(BigDecimal.valueOf(1.0), com.hrms.dao.manager.AttendanceDAO.AttendanceDTO.calculateWorkCredit(new BigDecimal("7.50")));
        assertEquals(BigDecimal.valueOf(1.0), com.hrms.dao.manager.AttendanceDAO.AttendanceDTO.calculateWorkCredit(new BigDecimal("8.00")));
        assertEquals(BigDecimal.valueOf(1.0), com.hrms.dao.manager.AttendanceDAO.AttendanceDTO.calculateWorkCredit(new BigDecimal("8.25")));
        assertEquals(BigDecimal.valueOf(1.0), com.hrms.dao.manager.AttendanceDAO.AttendanceDTO.calculateWorkCredit(new BigDecimal("10.00")));

        // 3.5 <= hours < 7 -> 0.5 công (nửa công)
        assertEquals(BigDecimal.valueOf(0.5), com.hrms.dao.manager.AttendanceDAO.AttendanceDTO.calculateWorkCredit(new BigDecimal("3.50")));
        assertEquals(BigDecimal.valueOf(0.5), com.hrms.dao.manager.AttendanceDAO.AttendanceDTO.calculateWorkCredit(new BigDecimal("4.00")));
        assertEquals(BigDecimal.valueOf(0.5), com.hrms.dao.manager.AttendanceDAO.AttendanceDTO.calculateWorkCredit(new BigDecimal("5.50")));
        assertEquals(BigDecimal.valueOf(0.5), com.hrms.dao.manager.AttendanceDAO.AttendanceDTO.calculateWorkCredit(new BigDecimal("6.99")));

        // < 3.5 hours -> 0.0 công (không có 1/4 công)
        assertEquals(BigDecimal.ZERO, com.hrms.dao.manager.AttendanceDAO.AttendanceDTO.calculateWorkCredit(new BigDecimal("3.49")));
        assertEquals(BigDecimal.ZERO, com.hrms.dao.manager.AttendanceDAO.AttendanceDTO.calculateWorkCredit(new BigDecimal("2.00")));
        assertEquals(BigDecimal.ZERO, com.hrms.dao.manager.AttendanceDAO.AttendanceDTO.calculateWorkCredit(new BigDecimal("0.00")));
        assertEquals(BigDecimal.ZERO, com.hrms.dao.manager.AttendanceDAO.AttendanceDTO.calculateWorkCredit(null));
    }

    @Test
    public void testManualAdjustmentHoursAndStatusCalculation() {
        CompanyShiftSetting setting = new CompanyShiftSetting(1, LocalTime.of(8, 30), LocalTime.of(17, 30), 60, 5);

        // Scenario 1: Late check-in (08:40 > late threshold 08:35), tan ca 17:30 -> 7.83 hrs >= 7.0 -> 1.0 công
        LocalTime inTime1 = LocalTime.of(8, 40);
        LocalTime outTime1 = LocalTime.of(17, 30);
        assertTrue(inTime1.isAfter(setting.getLateThreshold()));
        assertFalse(outTime1.isBefore(setting.getShiftEndTime()));
        long m1 = Duration.between(inTime1, outTime1).toMinutes() - setting.getLunchBreakMinutes(); // 530 - 60 = 470m = 7.83 hrs
        BigDecimal h1 = BigDecimal.valueOf(m1).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("7.83"), h1);
        assertEquals(BigDecimal.valueOf(1.0), com.hrms.dao.manager.AttendanceDAO.AttendanceDTO.calculateWorkCredit(h1)); // 1.0 công (>= 7.0h)

        // Scenario 2: On-time check-in (08:30 <= 08:35), Early tan ca (16:00 < 17:30) -> 6.50 hrs -> 0.5 công (3.5 <= h < 7.0)
        LocalTime inTime2 = LocalTime.of(8, 30);
        LocalTime outTime2 = LocalTime.of(16, 0);
        assertFalse(inTime2.isAfter(setting.getLateThreshold()));
        assertTrue(outTime2.isBefore(setting.getShiftEndTime()));
        long m2 = Duration.between(inTime2, outTime2).toMinutes() - setting.getLunchBreakMinutes(); // 450 - 60 = 390m = 6.50 hrs
        BigDecimal h2 = BigDecimal.valueOf(m2).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("6.50"), h2);
        assertEquals(BigDecimal.valueOf(0.5), com.hrms.dao.manager.AttendanceDAO.AttendanceDTO.calculateWorkCredit(h2)); // nửa công (3.5 <= h < 7)

        // Scenario 3: Standard day (08:00 - 17:30), total 9.5h - 1h lunch = 8.5h >= 7.0 -> 1.0 công
        LocalTime inTime3 = LocalTime.of(8, 0);
        LocalTime outTime3 = LocalTime.of(17, 30);
        long m3 = Duration.between(inTime3, outTime3).toMinutes() - setting.getLunchBreakMinutes(); // 570 - 60 = 510m = 8.50 hrs
        BigDecimal h3 = BigDecimal.valueOf(m3).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("8.50"), h3);
        assertEquals(BigDecimal.valueOf(1.0), com.hrms.dao.manager.AttendanceDAO.AttendanceDTO.calculateWorkCredit(h3)); // 1.0 công
    }

    @Test
    public void testTimesheetCutoffDayDefaultAndSetter() {
        CompanyShiftSetting setting = new CompanyShiftSetting(1);
        assertEquals(5, setting.getTimesheetCutoffDay());

        setting.setTimesheetCutoffDay(10);
        assertEquals(10, setting.getTimesheetCutoffDay());

        // Zero or negative should fallback to default 5
        setting.setTimesheetCutoffDay(0);
        assertEquals(5, setting.getTimesheetCutoffDay());
    }

    @Test
    public void testCustomWorkCreditThresholdsPerCompany() {
        // Company 1: Configured per user's request: 4.0h -> 0.5 công, >= 6.0h -> 1.0 công
        CompanyShiftSetting company1 = new CompanyShiftSetting(1, LocalTime.of(8, 30), LocalTime.of(17, 30), 60, 0,
                new BigDecimal("4.00"), new BigDecimal("6.00"));

        assertEquals(new BigDecimal("4.00"), company1.getMinHoursHalfDay());
        assertEquals(new BigDecimal("6.00"), company1.getMinHoursFullDay());

        // Test Company 1 credit calculations
        assertEquals(BigDecimal.valueOf(1.0), company1.calculateWorkCredit(new BigDecimal("6.50"))); // >= 6.0h -> 1.0 công
        assertEquals(BigDecimal.valueOf(1.0), company1.calculateWorkCredit(new BigDecimal("6.00"))); // >= 6.0h -> 1.0 công
        assertEquals(BigDecimal.valueOf(0.5), company1.calculateWorkCredit(new BigDecimal("5.50"))); // 4.0h - 6.0h -> 0.5 công
        assertEquals(BigDecimal.valueOf(0.5), company1.calculateWorkCredit(new BigDecimal("4.00"))); // 4.0h - 6.0h -> 0.5 công
        assertEquals(BigDecimal.ZERO, company1.calculateWorkCredit(new BigDecimal("3.99")));        // < 4.0h -> 0.0 công
        assertEquals(BigDecimal.ZERO, company1.calculateWorkCredit(new BigDecimal("2.00")));        // < 4.0h -> 0.0 công
        assertEquals(BigDecimal.ZERO, company1.calculateWorkCredit(null));

        // Company 2: Different company with different policy (e.g., 3.0h half day, 7.5h full day)
        CompanyShiftSetting company2 = new CompanyShiftSetting(2, LocalTime.of(9, 0), LocalTime.of(18, 0), 60, 10,
                new BigDecimal("3.00"), new BigDecimal("7.50"));

        // At 6.0 hours:
        // Company 1 gives 1.0 công (since threshold is 6.0)
        assertEquals(BigDecimal.valueOf(1.0), company1.calculateWorkCredit(new BigDecimal("6.00")));
        // Company 2 gives 0.5 công (since threshold is 7.5, and 6.0 < 7.5 but >= 3.0)
        assertEquals(BigDecimal.valueOf(0.5), company2.calculateWorkCredit(new BigDecimal("6.00")));

        // At 3.5 hours:
        // Company 1 gives 0.0 công (since < 4.0)
        assertEquals(BigDecimal.ZERO, company1.calculateWorkCredit(new BigDecimal("3.50")));
        // Company 2 gives 0.5 công (since >= 3.0)
        assertEquals(BigDecimal.valueOf(0.5), company2.calculateWorkCredit(new BigDecimal("3.50")));

        // Company 2 at 7.5h gives 1.0 công
        assertEquals(BigDecimal.valueOf(1.0), company2.calculateWorkCredit(new BigDecimal("7.50")));
    }

    @Test
    public void testValidationRejectsInvalidWorkCreditThresholds() {
        CompanyShiftSettingDAO dao = new CompanyShiftSettingDAO();

        // 1. Min half day <= 0
        CompanyShiftSetting invalidHalf = new CompanyShiftSetting(1, LocalTime.of(8, 30), LocalTime.of(17, 30), 60, 0,
                new BigDecimal("0.00"), new BigDecimal("6.00"));
        try {
            dao.saveShiftSetting(invalidHalf);
            fail("Expected IllegalArgumentException when min half day is 0");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("nửa công"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }

        // 2. Min full day <= min half day
        CompanyShiftSetting invalidFull = new CompanyShiftSetting(1, LocalTime.of(8, 30), LocalTime.of(17, 30), 60, 0,
                new BigDecimal("6.00"), new BigDecimal("4.00"));
        try {
            dao.saveShiftSetting(invalidFull);
            fail("Expected IllegalArgumentException when full day <= half day");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("lớn hơn"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testShiftSettingStatus_NoAttendance_CanEditTodayDirectly() throws SQLException {
        AttendanceDAO mockAttendanceDAO = mock(AttendanceDAO.class);
        CompanyShiftSettingDAO mockShiftDAO = mock(CompanyShiftSettingDAO.class);

        CompanyShiftSetting setting = new CompanyShiftSetting(1);
        setting.setUpdatedAt(LocalDateTime.now().minusDays(2)); // Updated 2 days ago

        when(mockShiftDAO.getShiftSetting(1)).thenReturn(setting);
        when(mockAttendanceDAO.hasAnyAttendanceOnDate(eq(1), any(LocalDate.class))).thenReturn(false);

        AttendanceService service = new AttendanceService(mockAttendanceDAO, mockShiftDAO);
        AttendanceService.ShiftSettingStatus status = service.getShiftSettingStatus(1);

        assertTrue("Should be allowed to edit today", status.isCanEditToday());
        assertFalse("Should apply today directly since no one checked in", status.isWillApplyTomorrow());
        assertTrue(status.getStatusMessage().contains("chưa có nhân viên nào điểm danh"));
    }

    @Test
    public void testShiftSettingStatus_WithAttendance_WillApplyTomorrow() throws SQLException {
        AttendanceDAO mockAttendanceDAO = mock(AttendanceDAO.class);
        CompanyShiftSettingDAO mockShiftDAO = mock(CompanyShiftSettingDAO.class);

        CompanyShiftSetting setting = new CompanyShiftSetting(1);
        setting.setUpdatedAt(LocalDateTime.now().minusDays(3));

        when(mockShiftDAO.getShiftSetting(1)).thenReturn(setting);
        when(mockAttendanceDAO.hasAnyAttendanceOnDate(eq(1), any(LocalDate.class))).thenReturn(true);

        AttendanceService service = new AttendanceService(mockAttendanceDAO, mockShiftDAO);
        AttendanceService.ShiftSettingStatus status = service.getShiftSettingStatus(1);

        assertTrue("Should be allowed to set for tomorrow", status.isCanEditToday());
        assertTrue("Must apply from tomorrow because employees already checked in today", status.isWillApplyTomorrow());
        assertTrue(status.getStatusMessage().contains("đã có nhân viên trong công ty điểm danh"));
    }

    @Test
    public void testShiftSettingStatus_AlreadyUpdatedDirectlyToday_CannotEditAgain() throws SQLException {
        AttendanceDAO mockAttendanceDAO = mock(AttendanceDAO.class);
        CompanyShiftSettingDAO mockShiftDAO = mock(CompanyShiftSettingDAO.class);

        CompanyShiftSetting setting = new CompanyShiftSetting(1);
        setting.setUpdatedAt(LocalDateTime.now()); // Already updated earlier today

        when(mockShiftDAO.getShiftSetting(1)).thenReturn(setting);

        AttendanceService service = new AttendanceService(mockAttendanceDAO, mockShiftDAO);
        AttendanceService.ShiftSettingStatus status = service.getShiftSettingStatus(1);

        assertFalse("Cannot edit again today", status.isCanEditToday());
        assertTrue(status.getStatusMessage().contains("Hôm nay bạn đã cài đặt ca làm việc rồi"));
    }

    @Test
    public void testShiftSettingStatus_AlreadyUpdatedPendingToday_CannotEditAgain() throws SQLException {
        AttendanceDAO mockAttendanceDAO = mock(AttendanceDAO.class);
        CompanyShiftSettingDAO mockShiftDAO = mock(CompanyShiftSettingDAO.class);

        CompanyShiftSetting setting = new CompanyShiftSetting(1);
        setting.setPendingEffectiveDate(LocalDate.now().plusDays(1));
        setting.setPendingUpdatedAt(LocalDateTime.now()); // Saved pending update today

        when(mockShiftDAO.getShiftSetting(1)).thenReturn(setting);

        AttendanceService service = new AttendanceService(mockAttendanceDAO, mockShiftDAO);
        AttendanceService.ShiftSettingStatus status = service.getShiftSettingStatus(1);

        assertFalse("Cannot edit again today when pending shift already recorded today", status.isCanEditToday());
        assertTrue(status.getStatusMessage().contains("Mỗi ngày chỉ được phép thiết lập tối đa 1 lần"));
    }

    @Test
    public void testSaveCompanyShiftSetting_SavesDirectlyWhenNoAttendance() throws SQLException {
        AttendanceDAO mockAttendanceDAO = mock(AttendanceDAO.class);
        CompanyShiftSettingDAO mockShiftDAO = mock(CompanyShiftSettingDAO.class);

        CompanyShiftSetting currentSetting = new CompanyShiftSetting(1);
        currentSetting.setUpdatedAt(LocalDateTime.now().minusDays(5));

        when(mockShiftDAO.getShiftSetting(1)).thenReturn(currentSetting);
        when(mockAttendanceDAO.hasAnyAttendanceOnDate(eq(1), any(LocalDate.class))).thenReturn(false);

        AttendanceService service = new AttendanceService(mockAttendanceDAO, mockShiftDAO);

        CompanyShiftSetting newSetting = new CompanyShiftSetting(1, LocalTime.of(8, 0), LocalTime.of(17, 0), 60, 0);
        String msg = service.saveCompanyShiftSetting(newSetting, 1, 99);

        verify(mockShiftDAO, times(1)).saveShiftSetting(newSetting);
        verify(mockShiftDAO, never()).savePendingShiftSetting(any(), any(), any());
        assertTrue(msg.contains("áp dụng cho ngày hôm nay"));
    }

    @Test
    public void testSaveCompanyShiftSetting_SavesPendingWhenAttendanceExists() throws SQLException {
        AttendanceDAO mockAttendanceDAO = mock(AttendanceDAO.class);
        CompanyShiftSettingDAO mockShiftDAO = mock(CompanyShiftSettingDAO.class);

        CompanyShiftSetting currentSetting = new CompanyShiftSetting(1);
        currentSetting.setUpdatedAt(LocalDateTime.now().minusDays(5));

        when(mockShiftDAO.getShiftSetting(1)).thenReturn(currentSetting);
        when(mockAttendanceDAO.hasAnyAttendanceOnDate(eq(1), any(LocalDate.class))).thenReturn(true);

        AttendanceService service = new AttendanceService(mockAttendanceDAO, mockShiftDAO);

        CompanyShiftSetting newSetting = new CompanyShiftSetting(1, LocalTime.of(9, 0), LocalTime.of(18, 0), 60, 10);
        String msg = service.saveCompanyShiftSetting(newSetting, 1, 99);

        verify(mockShiftDAO, never()).saveShiftSetting(newSetting);
        verify(mockShiftDAO, times(1)).savePendingShiftSetting(eq(newSetting), eq(LocalDate.now().plusDays(1)), eq(99));
        assertTrue(msg.contains("áp dụng từ ngày mai"));
    }

    @Test(expected = IllegalStateException.class)
    public void testSaveCompanyShiftSetting_RejectsSecondAttemptOnSameDay() throws SQLException {
        AttendanceDAO mockAttendanceDAO = mock(AttendanceDAO.class);
        CompanyShiftSettingDAO mockShiftDAO = mock(CompanyShiftSettingDAO.class);

        CompanyShiftSetting currentSetting = new CompanyShiftSetting(1);
        currentSetting.setUpdatedAt(LocalDateTime.now()); // Already saved today

        when(mockShiftDAO.getShiftSetting(1)).thenReturn(currentSetting);

        AttendanceService service = new AttendanceService(mockAttendanceDAO, mockShiftDAO);

        CompanyShiftSetting newSetting = new CompanyShiftSetting(1, LocalTime.of(9, 0), LocalTime.of(18, 0), 60, 10);
        service.saveCompanyShiftSetting(newSetting, 1, 99);
    }

    @Test
    public void testEarlyArrivalCheckIn_PreservesActualCheckInTimeAndCalculatesFromShiftStart() throws SQLException {
        AttendanceDAO mockAttendanceDAO = mock(AttendanceDAO.class);
        CompanyShiftSettingDAO mockShiftDAO = mock(CompanyShiftSettingDAO.class);

        // Company setup: 08:00 - 17:00, lunch 60 min, grace 0 min, min half 4.0h, min full 7.5h
        CompanyShiftSetting setting = new CompanyShiftSetting(1, LocalTime.of(8, 0), LocalTime.of(17, 0), 60, 0,
                new BigDecimal("4.00"), new BigDecimal("7.50"));

        LocalDate today = LocalDate.now();
        LocalTime earlyCheckInTime = LocalTime.of(7, 0); // Arrives early at 7:00 AM
        LocalTime checkOutTime = LocalTime.of(17, 0);    // Leaves at 5:00 PM

        when(mockAttendanceDAO.getEmployeeCompanyId(101)).thenReturn(1);
        when(mockShiftDAO.getShiftSetting(1)).thenReturn(setting);
        when(mockAttendanceDAO.getAttendanceByEmployeeAndDate(101, today)).thenReturn(null);

        AttendanceService service = new AttendanceService(mockAttendanceDAO, mockShiftDAO);

        // Perform Check-in at 07:00
        service.checkIn(101, today, earlyCheckInTime);

        // 1. Verify that the actual check-in time (07:00:00) is saved to the database, NOT mutated to 08:00:00
        verify(mockAttendanceDAO).insertCheckIn(eq(101), eq(1), eq(today), eq(earlyCheckInTime), eq("Present"));

        // 2. Verify work calculation:
        // Duration from shift start (08:00) to check-out (17:00) is 9h (540 min).
        // Since >= 5h (300 min), 1h lunch (60 min) is deducted -> 480 min = 8.00 hours.
        BigDecimal workedHours = AttendanceService.calculateWorkedHours(earlyCheckInTime, checkOutTime, setting);
        assertEquals(new BigDecimal("8.00"), workedHours);

        // 3. Verify work credit:
        // 8.00h >= 7.50h threshold -> 1.0 công
        BigDecimal credit = setting.calculateWorkCredit(workedHours);
        assertEquals(BigDecimal.valueOf(1.0), credit);
    }

    @Test
    public void testManualAdjustment_EarlyArrival_PreservesActualCheckInTime() throws SQLException {
        AttendanceDAO mockAttendanceDAO = mock(AttendanceDAO.class);
        CompanyShiftSettingDAO mockShiftDAO = mock(CompanyShiftSettingDAO.class);

        CompanyShiftSetting setting = new CompanyShiftSetting(1, LocalTime.of(8, 0), LocalTime.of(17, 0), 60, 0,
                new BigDecimal("4.00"), new BigDecimal("7.50"));

        LocalDate today = LocalDate.now();
        LocalTime earlyCheckInTime = LocalTime.of(7, 0);
        LocalTime checkOutTime = LocalTime.of(17, 0);

        when(mockShiftDAO.getShiftSetting(1)).thenReturn(setting);

        AttendanceService service = new AttendanceService(mockAttendanceDAO, mockShiftDAO);
        service.manualAdjustAttendance(1, 101, today, earlyCheckInTime, checkOutTime, "Điểm danh sớm", 1);

        // Verify actual check-in time 07:00 is passed to upsertManualAdjustment
        verify(mockAttendanceDAO).upsertManualAdjustment(
                eq(1), eq(101), eq(today),
                eq(earlyCheckInTime), eq(checkOutTime),
                eq(new BigDecimal("8.00")), eq("Present"),
                eq("Điểm danh sớm"), eq(1)
        );
    }

    @Test
    public void testRecordManagerAutoAttendance_CheckIn_InsertsPresentRecord() throws SQLException {
        AttendanceDAO mockAttendanceDAO = mock(AttendanceDAO.class);
        CompanyShiftSettingDAO mockShiftDAO = mock(CompanyShiftSettingDAO.class);

        int companyId = 1;
        int managerEmployeeId = 50;
        LocalDate today = LocalDate.now();
        LocalTime actionTime = LocalTime.of(8, 0);

        CompanyShiftSetting setting = new CompanyShiftSetting(companyId, LocalTime.of(8, 0), LocalTime.of(17, 0), 60, 0);
        when(mockShiftDAO.getShiftSetting(companyId)).thenReturn(setting);
        when(mockAttendanceDAO.getAttendanceByEmployeeAndDate(managerEmployeeId, today)).thenReturn(null);
        when(mockAttendanceDAO.insertCheckIn(eq(managerEmployeeId), eq(companyId), eq(today), eq(actionTime), eq("Present"))).thenReturn(100L);

        AttendanceService service = new AttendanceService(mockAttendanceDAO, mockShiftDAO);
        service.recordManagerAutoAttendance(companyId, managerEmployeeId, today, actionTime, "check-in");

        // 1. Verify that manager's check-in is inserted with actionTime and Status = "Present"
        verify(mockAttendanceDAO).insertCheckIn(eq(managerEmployeeId), eq(companyId), eq(today), eq(actionTime), eq("Present"));

        // 2. Verify that manager's check-out defaults to company shift end (17:00) with 8.00 hours calculated
        verify(mockAttendanceDAO).updateCheckOut(eq(100L), eq(LocalTime.of(17, 0)), eq(new BigDecimal("8.00")), eq("Present"));
    }

    @Test
    public void testRecordManagerAutoAttendance_CheckOut_UpdatesCheckOutAndCalculatesHours() throws SQLException {
        AttendanceDAO mockAttendanceDAO = mock(AttendanceDAO.class);
        CompanyShiftSettingDAO mockShiftDAO = mock(CompanyShiftSettingDAO.class);

        int companyId = 1;
        int managerEmployeeId = 50;
        LocalDate today = LocalDate.now();
        LocalTime checkInTime = LocalTime.of(8, 0);
        LocalTime checkOutTime = LocalTime.of(17, 30);

        CompanyShiftSetting setting = new CompanyShiftSetting(companyId, LocalTime.of(8, 0), LocalTime.of(17, 0), 60, 0);
        when(mockShiftDAO.getShiftSetting(companyId)).thenReturn(setting);

        AttendanceDAO.AttendanceDTO existing = new AttendanceDAO.AttendanceDTO();
        existing.setAttendanceId(999L);
        existing.setEmployeeId(managerEmployeeId);
        existing.setAttendanceDate(today);
        existing.setCheckInTime(checkInTime);
        existing.setStatus("Present");

        when(mockAttendanceDAO.getAttendanceByEmployeeAndDate(managerEmployeeId, today)).thenReturn(existing);

        AttendanceService service = new AttendanceService(mockAttendanceDAO, mockShiftDAO);
        service.recordManagerAutoAttendance(companyId, managerEmployeeId, today, checkOutTime, "check-out");

        // 08:00 to 17:30 = 9.5 hours minus 1 hour lunch = 8.50 hours
        BigDecimal expectedHours = new BigDecimal("8.50");
        verify(mockAttendanceDAO).updateCheckOut(eq(999L), eq(checkOutTime), eq(expectedHours), eq("Present"));
    }

    @Test
    public void testRecordManagerAutoAttendance_ExistingStatusNotPresent_UpdatesToPresent() throws SQLException {
        AttendanceDAO mockAttendanceDAO = mock(AttendanceDAO.class);
        CompanyShiftSettingDAO mockShiftDAO = mock(CompanyShiftSettingDAO.class);

        int companyId = 1;
        int managerEmployeeId = 50;
        LocalDate today = LocalDate.now();
        LocalTime checkInTime = LocalTime.of(8, 30);

        AttendanceDAO.AttendanceDTO existing = new AttendanceDAO.AttendanceDTO();
        existing.setAttendanceId(888L);
        existing.setEmployeeId(managerEmployeeId);
        existing.setAttendanceDate(today);
        existing.setCheckInTime(checkInTime);
        existing.setStatus("Absent");

        when(mockAttendanceDAO.getAttendanceByEmployeeAndDate(managerEmployeeId, today)).thenReturn(existing);

        AttendanceService service = new AttendanceService(mockAttendanceDAO, mockShiftDAO);
        service.recordManagerAutoAttendance(companyId, managerEmployeeId, today, checkInTime, "check-in");

        // Verify status is updated to "Present"
        verify(mockAttendanceDAO).updateCheckIn(eq(888L), eq(checkInTime), eq("Present"));
    }
}
