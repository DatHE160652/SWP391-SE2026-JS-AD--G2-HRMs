package com.hrms.service;

import com.hrms.dao.AttendanceDAO;
import com.hrms.dao.AttendanceDAO.AttendanceDTO;
import com.hrms.dao.AttendanceExplanationDAO;
import com.hrms.dao.AttendanceReportDAO;
import com.hrms.dao.AttendanceSummaryDAO;
import com.hrms.dao.CompanyShiftSettingDAO;
import com.hrms.dao.TimesheetLockDAO;
import com.hrms.model.AttendanceExplanation;
import com.hrms.model.CompanyShiftSetting;
import com.hrms.model.TimesheetLock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class AttendanceService {
    private final AttendanceDAO attendanceDAO;
    private final CompanyShiftSettingDAO shiftSettingDAO;
    private final AttendanceExplanationDAO explanationDAO;
    private final TimesheetLockDAO timesheetLockDAO;

    public static final LocalTime SHIFT_START = CompanyShiftSetting.DEFAULT_SHIFT_START;
    public static final LocalTime SHIFT_END = CompanyShiftSetting.DEFAULT_SHIFT_END;

    public AttendanceService() {
        this(new AttendanceDAO(), new CompanyShiftSettingDAO(), new AttendanceExplanationDAO(), new TimesheetLockDAO());
    }

    public AttendanceService(AttendanceDAO attendanceDAO) {
        this(attendanceDAO, new CompanyShiftSettingDAO(), new AttendanceExplanationDAO(), new TimesheetLockDAO());
    }

    public AttendanceService(AttendanceDAO attendanceDAO, CompanyShiftSettingDAO shiftSettingDAO) {
        this(attendanceDAO, shiftSettingDAO, new AttendanceExplanationDAO(), new TimesheetLockDAO());
    }

    public AttendanceService(AttendanceDAO attendanceDAO, CompanyShiftSettingDAO shiftSettingDAO,
                             AttendanceExplanationDAO explanationDAO, TimesheetLockDAO timesheetLockDAO) {
        this.attendanceDAO = attendanceDAO;
        this.shiftSettingDAO = shiftSettingDAO != null ? shiftSettingDAO : new CompanyShiftSettingDAO();
        this.explanationDAO = explanationDAO != null ? explanationDAO : new AttendanceExplanationDAO();
        this.timesheetLockDAO = timesheetLockDAO != null ? timesheetLockDAO : new TimesheetLockDAO();
    }

    public void requireEmployee(int companyId, int employeeId) throws SQLException {
        attendanceDAO.requireEmployee(companyId, employeeId);
    }

    // CheckIn Service
    public AttendanceDTO checkIn(int companyId, int employeeId, LocalDate date, LocalTime time) throws SQLException {
        requireEmployee(companyId, employeeId);
        return checkInInternal(companyId, employeeId, date, time);
    }

    public AttendanceDTO checkOut(int companyId, int employeeId, LocalDate date, LocalTime time) throws SQLException {
        requireEmployee(companyId, employeeId);
        return checkOutInternal(companyId, employeeId, date, time);
    }

    public List<AttendanceDTO> getPersonalAttendance(int companyId, int employeeId, int month, int year) throws SQLException {
        requireEmployee(companyId, employeeId);
        List<AttendanceDTO> list = attendanceDAO.getPersonalAttendance(employeeId, month, year);
        CompanyShiftSetting setting = shiftSettingDAO != null ? shiftSettingDAO.getShiftSetting(companyId) : null;
        if (setting != null && list != null) {
            for (AttendanceDTO dto : list) {
                dto.setWorkCredit(setting.calculateWorkCredit(dto.getTotalHours()));
            }
        }
        return list;
    }

    public AttendanceSummaryDAO getPersonalSummary(int companyId, int employeeId, int month, int year) throws SQLException {
        requireEmployee(companyId, employeeId);
        AttendanceSummaryDAO summary = attendanceDAO.getPersonalSummary(employeeId, month, year);
        CompanyShiftSetting setting = shiftSettingDAO != null ? shiftSettingDAO.getShiftSetting(companyId) : null;
        if (setting != null && summary != null) {
            List<AttendanceDTO> records = attendanceDAO.getPersonalAttendance(employeeId, month, year);
            if (records != null && !records.isEmpty()) {
                BigDecimal credits = BigDecimal.ZERO;
                for (AttendanceDTO r : records) {
                    if (r.getTotalHours() != null && ("Present".equalsIgnoreCase(r.getStatus()) || "Late".equalsIgnoreCase(r.getStatus()) || "Early Leave".equalsIgnoreCase(r.getStatus()))) {
                        credits = credits.add(setting.calculateWorkCredit(r.getTotalHours()));
                    }
                }
                summary.setTotalWorkCredits(credits);
            }
        }
        return summary;
    }

    public List<AttendanceDTO> getTeamAttendance(int companyId, int managerId, LocalDate date) throws SQLException {
        requireEmployee(companyId, managerId);
        List<AttendanceDTO> list = attendanceDAO.getTeamAttendance(companyId, managerId, date == null ? LocalDate.now() : date);
        CompanyShiftSetting setting = shiftSettingDAO != null ? shiftSettingDAO.getShiftSetting(companyId) : null;
        if (setting != null && list != null) {
            for (AttendanceDTO dto : list) {
                dto.setWorkCredit(setting.calculateWorkCredit(dto.getTotalHours()));
            }
        }
        return list;
    }

    public List<AttendanceDTO> getAllAttendanceByDate(int companyId, LocalDate date) throws SQLException {
        requireCompany(companyId);
        List<AttendanceDTO> list = attendanceDAO.getAllAttendanceByDate(companyId, date == null ? LocalDate.now() : date);
        CompanyShiftSetting setting = shiftSettingDAO != null ? shiftSettingDAO.getShiftSetting(companyId) : null;
        if (setting != null && list != null) {
            for (AttendanceDTO dto : list) {
                dto.setWorkCredit(setting.calculateWorkCredit(dto.getTotalHours()));
            }
        }
        return list;
    }

    public List<AttendanceReportDAO> getManagerAttendanceReport(int companyId, int managerId, Integer departmentId,
                                                               int month, int year) throws SQLException {
        requireEmployee(companyId, managerId);
        return attendanceDAO.getAttendanceReportForManager(companyId, managerId, departmentId, month, year);
    }

    public CompanyShiftSetting getCompanyShiftSetting(int companyId) throws SQLException {
        requireCompany(companyId);
        return shiftSettingDAO.getShiftSetting(companyId);
    }

    public void updateCompanyShiftSetting(CompanyShiftSetting setting) throws SQLException {
        if (setting == null) {
            throw new IllegalArgumentException("Dữ liệu cài đặt ca làm việc không hợp lệ.");
        }
        requireCompany(setting.getCompanyId());
        shiftSettingDAO.saveShiftSetting(setting);
    }

    private void checkTimesheetNotLocked(int companyId, LocalDate date) throws SQLException {
        if (date != null && isMonthLocked(companyId, date.getMonthValue(), date.getYear())) {
            throw new IllegalStateException("Bảng chấm công tháng " + date.getMonthValue() + "/" + date.getYear() + " đã bị khóa để chốt lương. Không thể chỉnh sửa.");
        }
    }

    /**
     * Điểm danh bổ sung / Điều chỉnh công thủ công cho bất kỳ ngày nào
     */
    public AttendanceDTO manualAdjustAttendance(int companyId, int employeeId, LocalDate date,
                                                LocalTime checkInTime, LocalTime checkOutTime,
                                                String note, Integer adjustedByUserId) throws SQLException {
        requireEmployee(companyId, employeeId);
        if (date == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày điểm danh bổ sung.");
        }
        checkTimesheetNotLocked(companyId, date);

        if (checkInTime == null && checkOutTime == null) {
            throw new IllegalArgumentException("Vui lòng nhập giờ vào ca hoặc giờ tan ca.");
        }
        if (checkInTime != null && checkOutTime != null && checkOutTime.isBefore(checkInTime)) {
            throw new IllegalArgumentException("Thời gian tan ca (Check-out) không thể sớm hơn thời gian vào ca (Check-in).");
        }

        CompanyShiftSetting shiftSetting = shiftSettingDAO.getShiftSetting(companyId);

        BigDecimal totalHours = BigDecimal.ZERO;
        String status = "Present";

        if (checkInTime != null && checkOutTime != null) {
            Duration duration = Duration.between(checkInTime, checkOutTime);
            long minutes = duration.toMinutes();
            if (minutes >= 300 && shiftSetting.getLunchBreakMinutes() > 0) {
                minutes = Math.max(0, minutes - shiftSetting.getLunchBreakMinutes());
            }
            totalHours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            if (totalHours.compareTo(BigDecimal.ZERO) < 0) {
                totalHours = BigDecimal.ZERO;
            }

            boolean isLate = checkInTime.isAfter(shiftSetting.getLateThreshold());
            boolean isEarly = checkOutTime.isBefore(shiftSetting.getShiftEndTime());

            if (isLate) {
                status = "Late";
            } else if (isEarly) {
                status = "Early Leave";
            } else {
                status = "Present";
            }
        } else if (checkInTime != null) {
            status = checkInTime.isAfter(shiftSetting.getLateThreshold()) ? "Late" : "Present";
        } else {
            status = checkOutTime.isBefore(shiftSetting.getShiftEndTime()) ? "Early Leave" : "Present";
        }

        String finalNote = note != null && !note.isBlank() ? note.trim() : "Điểm danh bổ sung";

        attendanceDAO.upsertManualAdjustment(companyId, employeeId, date, checkInTime, checkOutTime, totalHours, status, finalNote, adjustedByUserId);
        AttendanceDTO adjusted = attendanceDAO.getAttendanceByEmployeeAndDate(employeeId, date);
        if (adjusted != null && shiftSetting != null) {
            adjusted.setWorkCredit(shiftSetting.calculateWorkCredit(adjusted.getTotalHours()));
        }
        return adjusted;
    }

    private void requireCompany(Integer companyId) {
        if (companyId == null || companyId <= 0) throw new SecurityException("Vui lòng đăng nhập vào workspace.");
    }

    /**
     * UC29: Manager performs Check-in for an employee
     */
    public AttendanceDTO checkIn(int employeeId, LocalDate date, LocalTime checkInTime) throws SQLException {
        int companyId = attendanceDAO.getEmployeeCompanyId(employeeId);
        return checkInInternal(companyId, employeeId, date, checkInTime);
    }

    private AttendanceDTO checkInInternal(int companyId, int employeeId, LocalDate date, LocalTime checkInTime) throws SQLException {
        if (date == null) {
            date = LocalDate.now();
        }
        checkTimesheetNotLocked(companyId, date);

        if (checkInTime == null) {
            checkInTime = LocalTime.now().withNano(0);
        }

        AttendanceDTO existing = attendanceDAO.getAttendanceByEmployeeAndDate(employeeId, date);
        if (existing != null && existing.getCheckInTime() != null) {
            throw new IllegalArgumentException("Nhân viên này đã được điểm danh vào ca hôm nay lúc " + existing.getCheckInTime());
        }

        CompanyShiftSetting shiftSetting = shiftSettingDAO.getShiftSetting(companyId);
        LocalTime lateThreshold = shiftSetting.getLateThreshold();
        String status = checkInTime.isAfter(lateThreshold) ? "Late" : "Present";

        if (existing != null) {
            attendanceDAO.updateCheckIn(existing.getAttendanceId(), checkInTime, status);
        } else {
            attendanceDAO.insertCheckIn(employeeId, companyId, date, checkInTime, status);
        }

        AttendanceDTO checkedIn = attendanceDAO.getAttendanceByEmployeeAndDate(employeeId, date);
        if (checkedIn != null && shiftSetting != null) {
            checkedIn.setWorkCredit(shiftSetting.calculateWorkCredit(checkedIn.getTotalHours()));
        }
        return checkedIn;
    }

    /**
     * UC30: Manager performs Check-out for an employee
     */
    public AttendanceDTO checkOut(int employeeId, LocalDate date, LocalTime checkOutTime) throws SQLException {
        int companyId = attendanceDAO.getEmployeeCompanyId(employeeId);
        return checkOutInternal(companyId, employeeId, date, checkOutTime);
    }

    private AttendanceDTO checkOutInternal(int companyId, int employeeId, LocalDate date, LocalTime checkOutTime) throws SQLException {
        if (date == null) {
            date = LocalDate.now();
        }
        checkTimesheetNotLocked(companyId, date);

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

        CompanyShiftSetting shiftSetting = shiftSettingDAO.getShiftSetting(companyId);

        Duration duration = Duration.between(existing.getCheckInTime(), checkOutTime);
        long minutes = duration.toMinutes();

        // Trừ thời gian ăn trưa nếu làm việc từ 5 tiếng trở lên
        if (minutes >= 300 && shiftSetting.getLunchBreakMinutes() > 0) {
            minutes = Math.max(0, minutes - shiftSetting.getLunchBreakMinutes());
        }

        BigDecimal totalHours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        if (totalHours.compareTo(BigDecimal.ZERO) < 0) {
            totalHours = BigDecimal.ZERO;
        }

        String status = existing.getStatus();
        if (checkOutTime.isBefore(shiftSetting.getShiftEndTime())) {
            status = "Early Leave";
        } else if ("Absent".equalsIgnoreCase(status) || status == null) {
            status = "Present";
        }

        attendanceDAO.updateCheckOut(existing.getAttendanceId(), checkOutTime, totalHours, status);
        AttendanceDTO checkedIn = attendanceDAO.getAttendanceByEmployeeAndDate(employeeId, date);
        if (checkedIn != null && shiftSetting != null) {
            checkedIn.setWorkCredit(shiftSetting.calculateWorkCredit(checkedIn.getTotalHours()));
        }
        return checkedIn;
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
        List<AttendanceDTO> list = attendanceDAO.searchCompanyAttendance(companyId, departmentId, status, fromDate, toDate, keyword);
        CompanyShiftSetting setting = shiftSettingDAO != null ? shiftSettingDAO.getShiftSetting(companyId) : null;
        if (setting != null && list != null) {
            for (AttendanceDTO dto : list) {
                dto.setWorkCredit(setting.calculateWorkCredit(dto.getTotalHours()));
            }
        }
        return list;
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

    // =========================================================================
    // UC86: Attendance Explanations / Correction Requests (Employee)
    // =========================================================================
    public boolean submitExplanation(AttendanceExplanation exp) throws SQLException {
        if (exp == null) {
            throw new IllegalArgumentException("Thông tin giải trình không hợp lệ.");
        }
        requireCompany(exp.getCompanyId());
        requireEmployee(exp.getCompanyId(), exp.getEmployeeId());

        if (exp.getAttendanceDate() == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày cần giải trình công.");
        }
        if (exp.getAttendanceDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Không thể gửi giải trình cho ngày trong tương lai.");
        }
        checkTimesheetNotLocked(exp.getCompanyId(), exp.getAttendanceDate());

        if (explanationDAO.hasPendingExplanation(exp.getCompanyId(), exp.getEmployeeId(), exp.getAttendanceDate())) {
            throw new IllegalStateException("Đã có yêu cầu giải trình đang chờ duyệt cho ngày này.");
        }
        if (exp.getReasonDetail() == null || exp.getReasonDetail().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do giải trình chi tiết.");
        }
        if (exp.getProposedCheckIn() != null && exp.getProposedCheckOut() != null &&
                exp.getProposedCheckOut().isBefore(exp.getProposedCheckIn())) {
            throw new IllegalArgumentException("Thời gian tan ca không thể sớm hơn thời gian vào ca.");
        }

        return explanationDAO.createExplanation(exp);
    }

    public List<AttendanceExplanation> getMyExplanations(int companyId, int employeeId, Integer month, Integer year) throws SQLException {
        requireCompany(companyId);
        requireEmployee(companyId, employeeId);
        return explanationDAO.getExplanationsByEmployee(companyId, employeeId, month, year);
    }

    public boolean cancelExplanation(int explanationId, int employeeId) throws SQLException {
        return explanationDAO.cancelExplanation(explanationId, employeeId);
    }

    // =========================================================================
    // UC87: Review & Approve Attendance Explanations (Manager / Admin)
    // =========================================================================
    public List<AttendanceExplanation> getPendingExplanations(int companyId, Integer managerEmployeeId) throws SQLException {
        requireCompany(companyId);
        return explanationDAO.getPendingExplanations(companyId, managerEmployeeId);
    }

    public boolean reviewExplanation(int explanationId, int reviewerUserId, Integer reviewerEmployeeId,
                                     boolean approve, String comment, int companyId) throws SQLException {
        requireCompany(companyId);
        AttendanceExplanation exp = explanationDAO.getExplanationById(explanationId);
        if (exp == null || exp.getCompanyId() != companyId) {
            throw new IllegalArgumentException("Không tìm thấy đơn giải trình chấm công này.");
        }
        if (!"PENDING".equalsIgnoreCase(exp.getStatus())) {
            throw new IllegalStateException("Đơn giải trình này đã được xử lý trước đó.");
        }
        checkTimesheetNotLocked(companyId, exp.getAttendanceDate());

        if (approve) {
            boolean updated = explanationDAO.updateStatus(explanationId, "APPROVED", reviewerUserId,
                    comment != null && !comment.isBlank() ? comment.trim() : "Chấp thuận giải trình");
            if (updated) {
                // Tự động đồng bộ sang bảng Attendances
                LocalTime in = exp.getProposedCheckIn();
                LocalTime out = exp.getProposedCheckOut();
                if (in == null && out == null) {
                    CompanyShiftSetting shift = shiftSettingDAO.getShiftSetting(companyId);
                    in = shift.getShiftStartTime();
                    out = shift.getShiftEndTime();
                }
                manualAdjustAttendance(companyId, exp.getEmployeeId(), exp.getAttendanceDate(),
                        in, out, "Duyệt giải trình: " + exp.getReasonDetail(), reviewerUserId);
            }
            return updated;
        } else {
            return explanationDAO.updateStatus(explanationId, "REJECTED", reviewerUserId,
                    comment != null && !comment.isBlank() ? comment.trim() : "Từ chối giải trình");
        }
    }

    public AttendanceExplanation getPendingExplanationByEmployeeAndDate(int companyId, int employeeId, LocalDate date) throws SQLException {
        if (explanationDAO == null) return null;
        return explanationDAO.getPendingExplanationByEmployeeAndDate(companyId, employeeId, date);
    }

    public boolean approveExplanationDirectly(int explanationId, Integer reviewerUserId, String comment) throws SQLException {
        if (explanationDAO == null) return false;
        return explanationDAO.updateStatus(explanationId, "APPROVED", reviewerUserId,
                comment != null && !comment.isBlank() ? comment.trim() : "Chấp thuận giải trình");
    }

    // =========================================================================
    // UC88: Timesheet Locking for Payroll Finalization (Manager / Admin)
    // =========================================================================
    public boolean isMonthLocked(int companyId, int month, int year) throws SQLException {
        // 1. Kiểm tra nếu có bản ghi khóa thủ công rõ ràng hoặc DAO ghi nhận
        if (timesheetLockDAO != null && timesheetLockDAO.isMonthLocked(companyId, month, year)) {
            return true;
        }
        TimesheetLock manualLock = timesheetLockDAO != null ? timesheetLockDAO.getLock(companyId, month, year) : null;
        if (manualLock != null) {
            return manualLock.isLocked();
        }

        // 2. Cơ chế tự động khóa ngầm theo Ngày chốt công hàng tháng (TimesheetCutoffDay)
        CompanyShiftSetting setting = getCompanyShiftSetting(companyId);
        int cutoffDay = setting != null ? setting.getTimesheetCutoffDay() : CompanyShiftSetting.DEFAULT_CUTOFF_DAY;

        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();
        int currentDay = today.getDayOfMonth();

        // Tháng trong tương lai hoặc tháng hiện tại đang làm việc -> Chưa khóa
        if (year > currentYear || (year == currentYear && month >= currentMonth)) {
            return false;
        }

        LocalDate targetMonthStart = LocalDate.of(year, month, 1);
        LocalDate currentMonthStart = LocalDate.of(currentYear, currentMonth, 1);

        // Các tháng cũ hơn tháng trước liền kề (T-2, T-3...) -> Luôn luôn khóa
        if (targetMonthStart.plusMonths(1).isBefore(currentMonthStart)) {
            return true;
        }

        // Tháng trước liền kề (T-1, ví dụ tháng 10 khi hiện tại là tháng 11):
        // Tự động khóa ngầm từ ngày chốt công trở đi (currentDay >= cutoffDay)
        return currentDay >= cutoffDay;
    }

    public TimesheetLock getTimesheetLock(int companyId, int month, int year) throws SQLException {
        TimesheetLock manualLock = timesheetLockDAO.getLock(companyId, month, year);
        if (manualLock != null) {
            return manualLock;
        }

        boolean autoLocked = isMonthLocked(companyId, month, year);
        if (autoLocked) {
            CompanyShiftSetting setting = getCompanyShiftSetting(companyId);
            int cutoffDay = setting != null ? setting.getTimesheetCutoffDay() : CompanyShiftSetting.DEFAULT_CUTOFF_DAY;
            TimesheetLock auto = new TimesheetLock(companyId, month, year, true, 0,
                    "Tự động khóa theo ngày chốt công hàng tháng (Ngày " + cutoffDay + ")");
            auto.setLockedByUserName("Hệ thống (Tự động)");
            return auto;
        }

        return null;
    }

    public boolean setTimesheetLock(int companyId, int month, int year, boolean isLocked, int userId, String notes) throws SQLException {
        requireCompany(companyId);
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Tháng không hợp lệ (1-12).");
        }
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Năm không hợp lệ.");
        }
        return timesheetLockDAO.setLock(companyId, month, year, isLocked, userId, notes != null ? notes.trim() : null);
    }
}
