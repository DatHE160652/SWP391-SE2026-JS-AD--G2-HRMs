package com.hrms.service;

import com.hrms.dao.LeaveBalanceDAO;
import com.hrms.dao.LeaveReportDTO;
import com.hrms.dao.LeaveRequestDAO;
import com.hrms.dao.LeaveTypeDAO;
import com.hrms.model.LeaveBalance;
import com.hrms.model.LeaveRequest;
import com.hrms.model.LeaveType;
import com.hrms.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing leave applications, quota validations, multi-step approval transactions,
 * and attendance synchronization.
 */
public class LeaveService {
    private final LeaveTypeDAO leaveTypeDAO;
    private final LeaveBalanceDAO leaveBalanceDAO;
    private final LeaveRequestDAO leaveRequestDAO;

    public LeaveService() {
        this.leaveTypeDAO = new LeaveTypeDAO();
        this.leaveBalanceDAO = new LeaveBalanceDAO();
        this.leaveRequestDAO = new LeaveRequestDAO();
    }

    public LeaveService(LeaveTypeDAO leaveTypeDAO, LeaveBalanceDAO leaveBalanceDAO, LeaveRequestDAO leaveRequestDAO) {
        this.leaveTypeDAO = leaveTypeDAO;
        this.leaveBalanceDAO = leaveBalanceDAO;
        this.leaveRequestDAO = leaveRequestDAO;
    }

    /**
     * Calculates the number of working days between two dates, excluding Saturday and Sunday.
     */
    public BigDecimal calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return BigDecimal.ZERO;
        }

        long count = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            DayOfWeek day = current.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                count++;
            }
            current = current.plusDays(1);
        }
        return BigDecimal.valueOf(count);
    }

    /**
     * Returns the list of actual working dates within the specified date range.
     */
    public List<LocalDate> getWorkingDates(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> workingDates = new ArrayList<>();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return workingDates;
        }

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            DayOfWeek day = current.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                workingDates.add(current);
            }
            current = current.plusDays(1);
        }
        return workingDates;
    }

    /**
     * Retrieves leave balances for an employee, automatically initializing records if none exist for the year.
     */
    public List<LeaveBalance> getEmployeeBalances(int employeeId, int companyId, int year) throws SQLException {
        List<LeaveBalance> balances = leaveBalanceDAO.getBalancesByEmployeeAndYear(employeeId, year);
        if (balances.isEmpty()) {
            List<LeaveType> types = leaveTypeDAO.getLeaveTypesByCompany(companyId);
            try (Connection conn = DBConnection.getConnection()) {
                for (LeaveType type : types) {
                    if (!type.isActive()) continue;
                    BigDecimal initialQuota = BigDecimal.valueOf(
                            type.getDefaultDaysPerYear() != null ? type.getDefaultDaysPerYear() : 12
                    );
                    leaveBalanceDAO.insertBalance(conn, employeeId, type.getLeaveTypeId(), year, initialQuota);
                }
            }
            balances = leaveBalanceDAO.getBalancesByEmployeeAndYear(employeeId, year);
        }
        return balances;
    }

    /**
     * Validates quota availability and submits a new leave request in 'Pending' status.
     */
    public LeaveRequest submitLeaveRequest(int companyId, int employeeId, int leaveTypeId,
                                          LocalDate startDate, LocalDate endDate, String reason) throws SQLException {
        if (startDate == null || endDate == null) {
            throw new FieldValidationException("startDate", "Vui lòng chọn ngày bắt đầu và kết thúc.");
        }
        if (endDate.isBefore(startDate)) {
            throw new FieldValidationException("endDate", "Ngày kết thúc không được trước ngày bắt đầu.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new FieldValidationException("reason", "Vui lòng nhập lý do xin nghỉ phép.");
        }

        BigDecimal totalWorkingDays = calculateWorkingDays(startDate, endDate);
        if (totalWorkingDays.compareTo(BigDecimal.ZERO) <= 0) {
            throw new FieldValidationException("endDate", "Khoảng thời gian đã chọn không có ngày làm việc nào (rơi vào cuối tuần).");
        }

        LeaveType selectedType = leaveTypeDAO.getLeaveTypeById(leaveTypeId);
        if (selectedType == null || !Integer.valueOf(companyId).equals(selectedType.getCompanyId()) || !selectedType.isActive()) {
            throw new FieldValidationException("leaveTypeId", "Vui lòng chọn loại nghỉ phép đang hoạt động trong công ty.");
        }
        int year = startDate.getYear();
        LeaveBalance balance = leaveBalanceDAO.getBalance(employeeId, leaveTypeId, year);
        if (balance == null) {
            LeaveType type = leaveTypeDAO.getLeaveTypeById(leaveTypeId);
            if (type == null) {
                throw new FieldValidationException("leaveTypeId", "Loại nghỉ phép không hợp lệ.");
            }
            try (Connection conn = DBConnection.getConnection()) {
                BigDecimal initialQuota = BigDecimal.valueOf(
                        type.getDefaultDaysPerYear() != null ? type.getDefaultDaysPerYear() : 12
                );
                leaveBalanceDAO.insertBalance(conn, employeeId, leaveTypeId, year, initialQuota);
            }
            balance = leaveBalanceDAO.getBalance(employeeId, leaveTypeId, year);
        }

        if (balance.getRemainingDays() == null || totalWorkingDays.compareTo(balance.getRemainingDays()) > 0) {
            throw new FieldValidationException("endDate",
                    "Số ngày nghỉ yêu cầu (" + totalWorkingDays + " ngày) vượt quá số dư ngày phép hiện tại ("
                            + (balance.getRemainingDays() != null ? balance.getRemainingDays() : 0) + " ngày)."
            );
        }

        LeaveRequest request = new LeaveRequest();
        request.setCompanyId(companyId);
        request.setEmployeeId(employeeId);
        request.setLeaveTypeId(leaveTypeId);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setTotalDays(totalWorkingDays);
        request.setReason(reason.trim());
        request.setStatus("Pending");

        int id = leaveRequestDAO.insertRequest(request);
        request.setLeaveRequestId(id);
        return request;
    }

    /**
     * Cancels an existing pending leave request.
     */
    public boolean cancelPendingRequest(int leaveRequestId, int employeeId) throws SQLException {
        LeaveRequest r = leaveRequestDAO.getRequestById(leaveRequestId);
        if (r == null) {
            throw new IllegalArgumentException("Đơn nghỉ phép không tồn tại.");
        }
        if (r.getEmployeeId() != employeeId) {
            throw new IllegalArgumentException("Bạn chỉ có thể hủy đơn của chính mình.");
        }
        if (!"Pending".equalsIgnoreCase(r.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể hủy đơn đang ở trạng thái Chờ duyệt (Pending).");
        }

        return leaveRequestDAO.cancelRequestByEmployee(leaveRequestId, employeeId);
    }

    /**
     * Approves a leave request inside an atomic database transaction.
     * Actions:
     * 1. Updates request status to 'Approved' with reviewer info and comments.
     * 2. Deducts used days from the employee's leave balance.
     * 3. Synchronizes each working day into the attendance log as 'On Leave'.
     */
    public void approveLeaveRequest(int leaveRequestId, int approverUserId, String comment) throws SQLException {
        LeaveRequest req = leaveRequestDAO.getRequestById(leaveRequestId);
        if (req == null) {
            throw new IllegalArgumentException("Đơn nghỉ phép không tồn tại.");
        }
        if (!"Pending".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalArgumentException("Đơn nghỉ phép này đã được xử lý trước đó (trạng thái hiện tại: " + req.getStatus() + ").");
        }

        int year = req.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceDAO.getBalance(req.getEmployeeId(), req.getLeaveTypeId(), year);
        if (balance == null || balance.getRemainingDays() == null || req.getTotalDays().compareTo(balance.getRemainingDays()) > 0) {
            throw new IllegalArgumentException("Số dư ngày phép của nhân viên không đủ để phê duyệt đơn này.");
        }

        List<LocalDate> workingDates = getWorkingDates(req.getStartDate(), req.getEndDate());

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Cập nhật trạng thái đơn nghỉ
                boolean updated = leaveRequestDAO.updateStatus(conn, leaveRequestId, "Approved", approverUserId, comment);
                if (!updated) {
                    throw new SQLException("Không thể cập nhật trạng thái đơn nghỉ phép.");
                }

                // 2. Khấu trừ số ngày nghỉ vào số dư (UsedDays += TotalDays)
                boolean deducted = leaveBalanceDAO.deductUsedDays(conn, balance.getBalanceId(), req.getTotalDays());
                if (!deducted) {
                    throw new SQLException("Không thể cập nhật số dư ngày phép.");
                }

                // 3. Đồng bộ dữ liệu sang bảng chấm công (Attendances) cho các ngày làm việc
                String attendanceNote = "Nghỉ phép: " + (req.getLeaveTypeName() != null ? req.getLeaveTypeName() : "Nghỉ phép đã duyệt");
                for (LocalDate date : workingDates) {
                    leaveRequestDAO.syncAttendanceOnLeave(conn, req.getCompanyId(), req.getEmployeeId(), date, attendanceNote);
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Rejects a leave request with mandatory reviewer feedback.
     */
    public void rejectLeaveRequest(int leaveRequestId, int approverUserId, String comment) throws SQLException {
        LeaveRequest req = leaveRequestDAO.getRequestById(leaveRequestId);
        if (req == null) {
            throw new IllegalArgumentException("Đơn nghỉ phép không tồn tại.");
        }
        if (!"Pending".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalArgumentException("Đơn nghỉ phép này đã được xử lý trước đó (trạng thái: " + req.getStatus() + ").");
        }

        try (Connection conn = DBConnection.getConnection()) {
            boolean updated = leaveRequestDAO.updateStatus(conn, leaveRequestId, "Rejected", approverUserId, comment);
            if (!updated) {
                throw new SQLException("Không thể từ chối đơn nghỉ phép.");
            }
        }
    }

    /**
     * Generates a native Excel XML Spreadsheet (SpreadsheetML 2003) string for download.
     */
    public String generateExcelSpreadsheetXml(List<LeaveReportDTO> reports, int year) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
        sb.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
        sb.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n");
        sb.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n");
        sb.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
        sb.append(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n");

        // Styling definitions
        sb.append("<Styles>\n");
        sb.append("<Style ss:ID=\"Default\" ss:Name=\"Normal\">\n");
        sb.append("<Alignment ss:Vertical=\"Center\"/>\n");
        sb.append("<Font ss:FontName=\"Segoe UI\" ss:Size=\"10\" ss:Color=\"#111827\"/>\n");
        sb.append("</Style>\n");

        sb.append("<Style ss:ID=\"Header\">\n");
        sb.append("<Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>\n");
        sb.append("<Font ss:FontName=\"Segoe UI\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>\n");
        sb.append("<Interior ss:Color=\"#062579\" ss:Pattern=\"Solid\"/>\n");
        sb.append("<Borders>\n");
        sb.append("<Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#CBD5E1\"/>\n");
        sb.append("<Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#CBD5E1\"/>\n");
        sb.append("<Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#CBD5E1\"/>\n");
        sb.append("<Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#CBD5E1\"/>\n");
        sb.append("</Borders>\n");
        sb.append("</Style>\n");

        sb.append("<Style ss:ID=\"TitleStyle\">\n");
        sb.append("<Alignment ss:Horizontal=\"Left\" ss:Vertical=\"Center\"/>\n");
        sb.append("<Font ss:FontName=\"Segoe UI\" ss:Size=\"16\" ss:Bold=\"1\" ss:Color=\"#062579\"/>\n");
        sb.append("</Style>\n");

        sb.append("<Style ss:ID=\"CellText\">\n");
        sb.append("<Alignment ss:Horizontal=\"Left\" ss:Vertical=\"Center\"/>\n");
        sb.append("<Borders>\n");
        sb.append("<Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/>\n");
        sb.append("<Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/>\n");
        sb.append("<Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/>\n");
        sb.append("<Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/>\n");
        sb.append("</Borders>\n");
        sb.append("</Style>\n");

        sb.append("<Style ss:ID=\"CellCenter\">\n");
        sb.append("<Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n");
        sb.append("<Borders>\n");
        sb.append("<Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/>\n");
        sb.append("<Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/>\n");
        sb.append("<Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/>\n");
        sb.append("<Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/>\n");
        sb.append("</Borders>\n");
        sb.append("</Style>\n");

        sb.append("<Style ss:ID=\"CellNumber\">\n");
        sb.append("<Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/>\n");
        sb.append("<NumberFormat ss:Format=\"0.0\"/>\n");
        sb.append("<Borders>\n");
        sb.append("<Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/>\n");
        sb.append("<Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/>\n");
        sb.append("<Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/>\n");
        sb.append("<Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/>\n");
        sb.append("</Borders>\n");
        sb.append("</Style>\n");

        sb.append("</Styles>\n");

        // Worksheet
        sb.append("<Worksheet ss:Name=\"BaoCaoNghiPhep\">\n");
        sb.append("<Table ss:DefaultRowHeight=\"20\">\n");
        sb.append("<Column ss:Width=\"50\"/>\n");  // STT
        sb.append("<Column ss:Width=\"90\"/>\n");  // Mã NV
        sb.append("<Column ss:Width=\"150\"/>\n"); // Họ và tên
        sb.append("<Column ss:Width=\"120\"/>\n"); // Phòng ban
        sb.append("<Column ss:Width=\"120\"/>\n"); // Vị trí
        sb.append("<Column ss:Width=\"85\"/>\n");  // Được cấp
        sb.append("<Column ss:Width=\"85\"/>\n");  // Đã dùng
        sb.append("<Column ss:Width=\"85\"/>\n");  // Còn lại
        sb.append("<Column ss:Width=\"90\"/>\n");  // Nghỉ có lương
        sb.append("<Column ss:Width=\"90\"/>\n");  // Không lương
        sb.append("<Column ss:Width=\"80\"/>\n");  // Tổng đơn
        sb.append("<Column ss:Width=\"80\"/>\n");  // Đã duyệt

        // Title row
        sb.append("<Row ss:Height=\"28\">\n");
        sb.append("<Cell ss:StyleID=\"TitleStyle\"><Data ss:Type=\"String\">BÁO CÁO TÌNH HÌNH NGHỈ PHÉP NĂM ").append(year).append("</Data></Cell>\n");
        sb.append("</Row>\n");
        sb.append("<Row ss:Height=\"10\"/>\n");

        // Header row
        sb.append("<Row ss:Height=\"24\">\n");
        sb.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">STT</Data></Cell>\n");
        sb.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Mã NV</Data></Cell>\n");
        sb.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Họ và tên</Data></Cell>\n");
        sb.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Phòng ban</Data></Cell>\n");
        sb.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Chức danh</Data></Cell>\n");
        sb.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Được cấp</Data></Cell>\n");
        sb.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Đã dùng</Data></Cell>\n");
        sb.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Còn lại</Data></Cell>\n");
        sb.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Có lương</Data></Cell>\n");
        sb.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Không lương</Data></Cell>\n");
        sb.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Tổng đơn</Data></Cell>\n");
        sb.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Đã duyệt</Data></Cell>\n");
        sb.append("</Row>\n");

        // Data rows
        int stt = 1;
        for (LeaveReportDTO r : reports) {
            sb.append("<Row ss:Height=\"20\">\n");
            sb.append("<Cell ss:StyleID=\"CellCenter\"><Data ss:Type=\"Number\">").append(stt++).append("</Data></Cell>\n");
            sb.append("<Cell ss:StyleID=\"CellCenter\"><Data ss:Type=\"String\">").append(escapeXml(r.getEmployeeCode())).append("</Data></Cell>\n");
            sb.append("<Cell ss:StyleID=\"CellText\"><Data ss:Type=\"String\">").append(escapeXml(r.getFullName())).append("</Data></Cell>\n");
            sb.append("<Cell ss:StyleID=\"CellText\"><Data ss:Type=\"String\">").append(escapeXml(r.getDepartmentName() != null ? r.getDepartmentName() : "—")).append("</Data></Cell>\n");
            sb.append("<Cell ss:StyleID=\"CellText\"><Data ss:Type=\"String\">").append(escapeXml(r.getPositionTitle() != null ? r.getPositionTitle() : "—")).append("</Data></Cell>\n");
            sb.append("<Cell ss:StyleID=\"CellNumber\"><Data ss:Type=\"Number\">").append(r.getEntitledDays() != null ? r.getEntitledDays() : 0).append("</Data></Cell>\n");
            sb.append("<Cell ss:StyleID=\"CellNumber\"><Data ss:Type=\"Number\">").append(r.getUsedDays() != null ? r.getUsedDays() : 0).append("</Data></Cell>\n");
            sb.append("<Cell ss:StyleID=\"CellNumber\"><Data ss:Type=\"Number\">").append(r.getRemainingDays() != null ? r.getRemainingDays() : 0).append("</Data></Cell>\n");
            sb.append("<Cell ss:StyleID=\"CellNumber\"><Data ss:Type=\"Number\">").append(r.getPaidDays() != null ? r.getPaidDays() : 0).append("</Data></Cell>\n");
            sb.append("<Cell ss:StyleID=\"CellNumber\"><Data ss:Type=\"Number\">").append(r.getUnpaidDays() != null ? r.getUnpaidDays() : 0).append("</Data></Cell>\n");
            sb.append("<Cell ss:StyleID=\"CellCenter\"><Data ss:Type=\"Number\">").append(r.getTotalRequests()).append("</Data></Cell>\n");
            sb.append("<Cell ss:StyleID=\"CellCenter\"><Data ss:Type=\"Number\">").append(r.getApprovedRequests()).append("</Data></Cell>\n");
            sb.append("</Row>\n");
        }

        sb.append("</Table>\n");
        sb.append("</Worksheet>\n");
        sb.append("</Workbook>\n");

        return sb.toString();
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // Delegated getter methods
    public LeaveTypeDAO getLeaveTypeDAO() { return leaveTypeDAO; }
    public LeaveBalanceDAO getLeaveBalanceDAO() { return leaveBalanceDAO; }
    public LeaveRequestDAO getLeaveRequestDAO() { return leaveRequestDAO; }
}
