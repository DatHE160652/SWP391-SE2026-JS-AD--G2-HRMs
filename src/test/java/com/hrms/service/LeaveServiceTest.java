package com.hrms.service;

import com.hrms.dao.LeaveReportDTO;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit and component tests for LeaveService business logic, date calculations,
 * validation rules, and Excel report generation.
 */
public class LeaveServiceTest {

    private LeaveService leaveService;

    @Before
    public void setUp() {
        leaveService = new LeaveService();
    }

    @Test
    public void testCalculateWorkingDays_SingleWeekday() {
        // 2026-10-12 is Monday
        LocalDate monday = LocalDate.of(2026, 10, 12);
        BigDecimal days = leaveService.calculateWorkingDays(monday, monday);
        assertEquals(BigDecimal.valueOf(1), days);
    }

    @Test
    public void testCalculateWorkingDays_SingleWeekendDay() {
        // 2026-10-17 is Saturday, 2026-10-18 is Sunday
        LocalDate saturday = LocalDate.of(2026, 10, 17);
        LocalDate sunday = LocalDate.of(2026, 10, 18);

        assertEquals(BigDecimal.ZERO, leaveService.calculateWorkingDays(saturday, saturday));
        assertEquals(BigDecimal.ZERO, leaveService.calculateWorkingDays(sunday, sunday));
        assertEquals(BigDecimal.ZERO, leaveService.calculateWorkingDays(saturday, sunday));
    }

    @Test
    public void testCalculateWorkingDays_SpanningWeekend() {
        // Friday 2026-10-16 to Monday 2026-10-19 -> Friday & Monday (2 working days)
        LocalDate friday = LocalDate.of(2026, 10, 16);
        LocalDate nextMonday = LocalDate.of(2026, 10, 19);

        BigDecimal days = leaveService.calculateWorkingDays(friday, nextMonday);
        assertEquals(BigDecimal.valueOf(2), days);
    }

    @Test
    public void testCalculateWorkingDays_FullTwoWeeks() {
        // Monday 2026-10-05 to Friday 2026-10-16 -> 10 working days
        LocalDate start = LocalDate.of(2026, 10, 5);
        LocalDate end = LocalDate.of(2026, 10, 16);

        BigDecimal days = leaveService.calculateWorkingDays(start, end);
        assertEquals(BigDecimal.valueOf(10), days);
    }

    @Test
    public void testCalculateWorkingDays_InvalidRanges() {
        LocalDate start = LocalDate.of(2026, 10, 15);
        LocalDate end = LocalDate.of(2026, 10, 10);

        assertEquals(BigDecimal.ZERO, leaveService.calculateWorkingDays(start, end));
        assertEquals(BigDecimal.ZERO, leaveService.calculateWorkingDays(null, end));
        assertEquals(BigDecimal.ZERO, leaveService.calculateWorkingDays(start, null));
        assertEquals(BigDecimal.ZERO, leaveService.calculateWorkingDays(null, null));
    }

    @Test
    public void testGetWorkingDates_ExcludesWeekends() {
        // Friday to Tuesday: Fri (16), Sat (17 - excluded), Sun (18 - excluded), Mon (19), Tue (20)
        LocalDate friday = LocalDate.of(2026, 10, 16);
        LocalDate tuesday = LocalDate.of(2026, 10, 20);

        List<LocalDate> dates = leaveService.getWorkingDates(friday, tuesday);
        assertEquals(3, dates.size());
        assertEquals(friday, dates.get(0));
        assertEquals(LocalDate.of(2026, 10, 19), dates.get(1));
        assertEquals(tuesday, dates.get(2));
    }

    @Test
    public void testCalculateWorkingDays_CrossMonth() {
        // Oct 30, 2026 (Friday) to Nov 3, 2026 (Tuesday)
        // Fri Oct 30 (1), Sat Oct 31 (0), Sun Nov 1 (0), Mon Nov 2 (1), Tue Nov 3 (1) -> 3 days
        LocalDate start = LocalDate.of(2026, 10, 30);
        LocalDate end = LocalDate.of(2026, 11, 3);
        BigDecimal days = leaveService.calculateWorkingDays(start, end);
        assertEquals(BigDecimal.valueOf(3), days);
    }

    @Test
    public void testCalculateWorkingDays_LeapYearFebruary() {
        // Feb 26, 2024 (Monday) to Mar 1, 2024 (Friday)
        // 2024 is leap year: Feb 26, 27, 28, 29 (all weekdays), Mar 1 (weekday) -> 5 working days
        LocalDate start = LocalDate.of(2024, 2, 26);
        LocalDate end = LocalDate.of(2024, 3, 1);
        BigDecimal days = leaveService.calculateWorkingDays(start, end);
        assertEquals(BigDecimal.valueOf(5), days);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubmitValidation_NullDates() throws Exception {
        leaveService.submitLeaveRequest(1, 1, 1, null, null, "Lý do");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubmitValidation_EndDateBeforeStartDate() throws Exception {
        LocalDate start = LocalDate.of(2026, 10, 20);
        LocalDate end = LocalDate.of(2026, 10, 18);
        leaveService.submitLeaveRequest(1, 1, 1, start, end, "Lý do");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubmitValidation_EmptyReason() throws Exception {
        LocalDate start = LocalDate.of(2026, 10, 20);
        LocalDate end = LocalDate.of(2026, 10, 21);
        leaveService.submitLeaveRequest(1, 1, 1, start, end, "   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubmitValidation_WeekendOnly() throws Exception {
        // Saturday 2026-10-17 to Sunday 2026-10-18
        LocalDate saturday = LocalDate.of(2026, 10, 17);
        LocalDate sunday = LocalDate.of(2026, 10, 18);
        leaveService.submitLeaveRequest(1, 1, 1, saturday, sunday, "Nghỉ cuối tuần");
    }

    @Test
    public void testGenerateExcelSpreadsheetXml() {
        List<LeaveReportDTO> reports = new ArrayList<>();
        LeaveReportDTO dto = new LeaveReportDTO();
        dto.setEmployeeId(101);
        dto.setEmployeeCode("EMP001");
        dto.setFullName("Trần Văn A & B");
        dto.setDepartmentName("Kỹ thuật <IT>");
        dto.setPositionTitle("Lập trình viên \"Senior\"");
        dto.setEntitledDays(BigDecimal.valueOf(14.0));
        dto.setUsedDays(BigDecimal.valueOf(4.0));
        dto.setRemainingDays(BigDecimal.valueOf(10.0));
        dto.setPaidDays(BigDecimal.valueOf(4.0));
        dto.setUnpaidDays(BigDecimal.ZERO);
        dto.setTotalRequests(2);
        dto.setApprovedRequests(2);
        dto.setPendingRequests(0);
        dto.setRejectedRequests(0);
        reports.add(dto);

        String xml = leaveService.generateExcelSpreadsheetXml(reports, 2026);

        assertNotNull(xml);
        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(xml.contains("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\""));
        assertTrue(xml.contains("BÁO CÁO TÌNH HÌNH NGHỈ PHÉP NĂM 2026"));
        assertTrue(xml.contains("EMP001"));
        // Check proper XML escaping
        assertTrue(xml.contains("Trần Văn A &amp; B"));
        assertTrue(xml.contains("Kỹ thuật &lt;IT&gt;"));
        assertTrue(xml.contains("Lập trình viên &quot;Senior&quot;"));
        assertTrue(xml.contains("</Workbook>"));
    }
}
