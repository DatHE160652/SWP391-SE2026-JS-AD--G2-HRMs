package com.hrms.service;

import com.hrms.dao.AttendanceDAO;
import com.hrms.dao.AttendanceDAO.AttendanceDTO;
import com.hrms.dao.AttendanceReportDAO;
import com.hrms.dao.AttendanceSummaryDAO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class AttendanceService {
    private final AttendanceDAO attendanceDAO;

    public static final LocalTime SHIFT_START = LocalTime.of(8, 30);
    public static final LocalTime SHIFT_END = LocalTime.of(17, 30);

    public AttendanceService() {
        this.attendanceDAO = new AttendanceDAO();
    }

    public AttendanceService(AttendanceDAO attendanceDAO) {
        this.attendanceDAO = attendanceDAO;
    }

    public void requireEmployee(int companyId, int employeeId) throws SQLException {
        attendanceDAO.requireEmployee(companyId, employeeId);
    }

    public AttendanceDTO checkIn(int companyId, int employeeId, LocalDate date, LocalTime time) throws SQLException {
        requireEmployee(companyId, employeeId);
        return checkIn(employeeId, date, time);
    }

    public AttendanceDTO checkOut(int companyId, int employeeId, LocalDate date, LocalTime time) throws SQLException {
        requireEmployee(companyId, employeeId);
        return checkOut(employeeId, date, time);
    }

    public List<AttendanceDTO> getPersonalAttendance(int companyId, int employeeId, int month, int year) throws SQLException {
        requireEmployee(companyId, employeeId);
        return getPersonalAttendance(employeeId, month, year);
    }

    public AttendanceSummaryDAO getPersonalSummary(int companyId, int employeeId, int month, int year) throws SQLException {
        requireEmployee(companyId, employeeId);
        return getPersonalSummary(employeeId, month, year);
    }

    public List<AttendanceDTO> getTeamAttendance(int companyId, int managerId, LocalDate date) throws SQLException {
        requireEmployee(companyId, managerId);
        return attendanceDAO.getTeamAttendance(companyId, managerId, date == null ? LocalDate.now() : date);
    }

    public List<AttendanceDTO> getAllAttendanceByDate(int companyId, LocalDate date) throws SQLException {
        requireCompany(companyId);
        return attendanceDAO.getAllAttendanceByDate(companyId, date == null ? LocalDate.now() : date);
    }

    public List<AttendanceReportDAO> getManagerAttendanceReport(int companyId, int managerId, Integer departmentId,
                                                               int month, int year) throws SQLException {
        requireEmployee(companyId, managerId);
        return attendanceDAO.getAttendanceReportForManager(companyId, managerId, departmentId, month, year);
    }

    private void requireCompany(Integer companyId) {
        if (companyId == null || companyId <= 0) throw new SecurityException("Vui lòng đăng nhập vào workspace.");
    }

    /**
     * UC29: Manager performs Check-in for an employee
     */
    public AttendanceDTO checkIn(int employeeId, LocalDate date, LocalTime checkInTime) throws SQLException {
        if (date == null) {
            date = LocalDate.now();
        }
        if (checkInTime == null) {
            checkInTime = LocalTime.now().withNano(0);
        }

        AttendanceDTO existing = attendanceDAO.getAttendanceByEmployeeAndDate(employeeId, date);
        if (existing != null && existing.getCheckInTime() != null) {
            throw new IllegalArgumentException("Nhân viên này đã được điểm danh vào ca hôm nay lúc " + existing.getCheckInTime());
        }

        String status = checkInTime.isAfter(SHIFT_START) ? "Late" : "Present";

        if (existing != null) {
            attendanceDAO.updateCheckIn(existing.getAttendanceId(), checkInTime, status);
        } else {
            int companyId = attendanceDAO.getEmployeeCompanyId(employeeId);
            attendanceDAO.insertCheckIn(employeeId, companyId, date, checkInTime, status);
        }

        return attendanceDAO.getAttendanceByEmployeeAndDate(employeeId, date);
    }

    /**
     * UC30: Manager performs Check-out for an employee
     */
    public AttendanceDTO checkOut(int employeeId, LocalDate date, LocalTime checkOutTime) throws SQLException {
        if (date == null) {
            date = LocalDate.now();
        }
        if (checkOutTime == null) {
            checkOutTime = LocalTime.now().withNano(0);
        }

        AttendanceDTO existing = attendanceDAO.getAttendanceByEmployeeAndDate(employeeId, date);
        if (existing == null || existing.getCheckInTime() == null) {
            throw new IllegalArgumentException("Nhân viên chưa có bản ghi vào ca (Check-in). Hãy thực hiện điểm danh vào ca trước.");
        }

        if (checkOutTime.isBefore(existing.getCheckInTime())) {
            throw new IllegalArgumentException("Thời gian tan ca (Check-out) không thể sớm hơn thời gian vào ca (" + existing.getCheckInTime() + ").");
        }

        Duration duration = Duration.between(existing.getCheckInTime(), checkOutTime);
        long minutes = duration.toMinutes();

        // Trừ 1 giờ ăn trưa nếu làm việc từ 5 tiếng trở lên
        if (minutes >= 300) {
            minutes = minutes - 60;
        }

        BigDecimal totalHours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        if (totalHours.compareTo(BigDecimal.ZERO) < 0) {
            totalHours = BigDecimal.ZERO;
        }

        String status = existing.getStatus();
        if (checkOutTime.isBefore(SHIFT_END)) {
            status = "Early Leave";
        } else if ("Absent".equalsIgnoreCase(status) || status == null) {
            status = "Present";
        }

        attendanceDAO.updateCheckOut(existing.getAttendanceId(), checkOutTime, totalHours, status);
        return attendanceDAO.getAttendanceByEmployeeAndDate(employeeId, date);
    }

    /**
     * UC31: View personal attendance history (Employee read-only)
     */
    public List<AttendanceDTO> getPersonalAttendance(int employeeId, int month, int year) throws SQLException {
        return attendanceDAO.getPersonalAttendance(employeeId, month, year);
    }

    public AttendanceSummaryDAO getPersonalSummary(int employeeId, int month, int year) throws SQLException {
        return attendanceDAO.getPersonalSummary(employeeId, month, year);
    }

    /**
     * UC32: View team attendance (Manager monitoring subordinates)
     */
    public List<AttendanceDTO> getTeamAttendance(int managerId, LocalDate date) throws SQLException {
        if (date == null) {
            date = LocalDate.now();
        }
        return attendanceDAO.getTeamAttendance(managerId, date);
    }

    /**
     * UC33: Monitor and search company-wide attendance (HR / Company Admin)
     */
    public List<AttendanceDTO> searchCompanyAttendance(Integer companyId, Integer departmentId, String status,
                                                      LocalDate fromDate, LocalDate toDate, String keyword) throws SQLException {
        requireCompany(companyId);
        return attendanceDAO.searchCompanyAttendance(companyId, departmentId, status, fromDate, toDate, keyword);
    }

    /**
     * UC36: Generate attendance reports by department / company
     */
    public List<AttendanceReportDAO> getAttendanceReport(Integer companyId, Integer departmentId, int month, int year) throws SQLException {
        requireCompany(companyId);
        return attendanceDAO.getAttendanceReport(companyId, departmentId, month, year);
    }

    public List<AttendanceReportDAO> getManagerAttendanceReport(int managerId, Integer departmentId,
                                                                int month, int year) throws SQLException {
        return attendanceDAO.getAttendanceReportForManager(managerId, departmentId, month, year);
    }

    /**
     * UC36: Export attendance report to CSV formatted string with UTF-8 BOM
     */
    public String exportCsvReport(List<AttendanceReportDAO> reports, int month, int year) {
        StringBuilder csv = new StringBuilder();
        // UTF-8 BOM for Excel to open Vietnamese characters properly
        csv.append("\uFEFF");
        csv.append("BÁO CÁO TỔNG HỢP CHẤM CÔNG THÁNG ").append(month).append("/").append(year).append("\n\n");
        csv.append("Mã Nhân Viên,Họ và Tên,Phòng Ban,Chức Danh,Số Ngày Đi Làm,Tổng Giờ Làm,Đúng Giờ,Đi Muộn,Về Sớm,Vắng Mặt,Nghỉ Phép,Tỉ Lệ Đúng Giờ (%)\n");

        for (AttendanceReportDAO r : reports) {
            csv.append("\"").append(escapeCsv(r.getEmployeeCode())).append("\",");
            csv.append("\"").append(escapeCsv(r.getFullName())).append("\",");
            csv.append("\"").append(escapeCsv(r.getDepartmentName())).append("\",");
            csv.append("\"").append(escapeCsv(r.getPositionTitle())).append("\",");
            csv.append(r.getTotalWorkDays()).append(",");
            csv.append(r.getTotalHours()).append(",");
            csv.append(r.getPresentCount()).append(",");
            csv.append(r.getLateCount()).append(",");
            csv.append(r.getEarlyLeaveCount()).append(",");
            csv.append(r.getAbsentCount()).append(",");
            csv.append(r.getOnLeaveCount()).append(",");
            csv.append(r.getPunctualityRate()).append("%\n");
        }
        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
