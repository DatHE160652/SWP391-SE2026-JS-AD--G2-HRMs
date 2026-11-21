package com.hrms.service;

import com.hrms.dao.manager.CompanyShiftSettingDAO;
import com.hrms.model.CompanyShiftSetting;
import com.hrms.service.manager.AttendanceService;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;

import static org.junit.Assert.*;

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
}
