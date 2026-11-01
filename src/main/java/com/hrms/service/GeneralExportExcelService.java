package com.hrms.service;

import com.hrms.model.Employee;
import com.hrms.model.LeaveRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service xuat file Excel (.xlsx) cho:
 * 1. Danh sach nhan su
 * 2. Danh sach don nghi phep
 * 3. Danh sach hop dong lao dong
 */
public class GeneralExportExcelService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ==========================================
    // 1. XUẤT DANH SÁCH NHÂN SỰ
    // ==========================================
    public void exportEmployees(String companyName, List<Employee> employees, OutputStream os) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Danh sach nhan su");
            sheet.setDisplayGridlines(true);

            CellStyle titleStyle = createTitleStyle(wb, IndexedColors.DARK_BLUE);
            CellStyle subtitleStyle = createSubtitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb, IndexedColors.ROYAL_BLUE);
            CellStyle dataStyle = createDataStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);

            // Title
            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("DANH SÁCH NHÂN SỰ");
            c0.setCellStyle(titleStyle);

            Row r1 = sheet.createRow(1);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Công ty: " + (companyName != null ? companyName : "HRMs Cloud") + " | Ngày xuất: " + LocalDate.now().format(DATE_FMT));
            c1.setCellStyle(subtitleStyle);

            String[] headers = {
                    "STT", "Mã NV", "Họ và tên", "Phòng ban", "Chức danh",
                    "Email", "Số điện thoại", "Giới tính", "Ngày sinh",
                    "Số CCCD/CMND", "Ngày cấp", "Nơi cấp", "Quê quán",
                    "Ngành học", "Địa chỉ thường trú", "Trạng thái"
            };

            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(26);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            int stt = 1;
            for (Employee e : employees) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                createCell(row, 0, String.valueOf(stt++), centerStyle);
                createCell(row, 1, e.getEmployeeCode(), centerStyle);
                createCell(row, 2, e.getFullName(), dataStyle);
                createCell(row, 3, e.getDepartmentName() != null ? e.getDepartmentName() : "-", dataStyle);
                createCell(row, 4, e.getPositionTitle() != null ? e.getPositionTitle() : "-", dataStyle);
                createCell(row, 5, e.getEmail() != null ? e.getEmail() : "-", dataStyle);
                createCell(row, 6, e.getPhone() != null ? e.getPhone() : "-", centerStyle);
                createCell(row, 7, formatGender(e.getGender()), centerStyle);
                createCell(row, 8, e.getDateOfBirth() != null ? e.getDateOfBirth().toLocalDate().format(DATE_FMT) : "-", centerStyle);
                createCell(row, 9, e.getIdentityNumber() != null ? e.getIdentityNumber() : "-", centerStyle);
                createCell(row, 10, e.getIdentityIssuedDate() != null ? e.getIdentityIssuedDate().toLocalDate().format(DATE_FMT) : "-", centerStyle);
                createCell(row, 11, e.getIdentityIssuedPlace() != null ? e.getIdentityIssuedPlace() : "-", dataStyle);
                createCell(row, 12, e.getHometown() != null ? e.getHometown() : "-", dataStyle);
                createCell(row, 13, e.getMajor() != null ? e.getMajor() : "-", dataStyle);
                createCell(row, 14, e.getPermanentAddress() != null ? e.getPermanentAddress() : "-", dataStyle);
                createCell(row, 15, "Active".equalsIgnoreCase(e.getStatus()) ? "Đang làm việc" : "Ngừng hoạt động", centerStyle);
            }

            autoSizeColumns(sheet, headers.length);
            wb.write(os);
        }
    }

    // ==========================================
    // 2. XUẤT DANH SÁCH ĐƠN NGHỈ PHÉP
    // ==========================================
    public void exportLeaveRequests(String companyName, List<LeaveRequest> requests, OutputStream os) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Danh sach don nghi phep");
            sheet.setDisplayGridlines(true);

            CellStyle titleStyle = createTitleStyle(wb, IndexedColors.DARK_GREEN);
            CellStyle subtitleStyle = createSubtitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb, IndexedColors.SEA_GREEN);
            CellStyle dataStyle = createDataStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);

            // Title
            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("DANH SÁCH ĐƠN NGHỈ PHÉP");
            c0.setCellStyle(titleStyle);

            Row r1 = sheet.createRow(1);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Công ty: " + (companyName != null ? companyName : "HRMs Cloud") + " | Ngày xuất: " + LocalDate.now().format(DATE_FMT));
            c1.setCellStyle(subtitleStyle);

            String[] headers = {
                    "STT", "Mã NV", "Họ và tên", "Phòng ban", "Loại nghỉ phép",
                    "Hưởng lương", "Từ ngày", "Đến ngày", "Số ngày",
                    "Lý do nghỉ", "Trạng thái", "Người duyệt", "Thời gian duyệt",
                    "Ý kiến duyệt", "Ngày nộp đơn"
            };

            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(26);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            int stt = 1;
            for (LeaveRequest req : requests) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                createCell(row, 0, String.valueOf(stt++), centerStyle);
                createCell(row, 1, req.getEmployeeCode() != null ? req.getEmployeeCode() : "-", centerStyle);
                createCell(row, 2, req.getEmployeeFullName() != null ? req.getEmployeeFullName() : "-", dataStyle);
                createCell(row, 3, req.getDepartmentName() != null ? req.getDepartmentName() : "-", dataStyle);
                createCell(row, 4, req.getLeaveTypeName() != null ? req.getLeaveTypeName() : "-", dataStyle);
                createCell(row, 5, Boolean.TRUE.equals(req.getIsPaid()) ? "Có lương" : "Không lương", centerStyle);
                createCell(row, 6, req.getStartDate() != null ? req.getStartDate().format(DATE_FMT) : "-", centerStyle);
                createCell(row, 7, req.getEndDate() != null ? req.getEndDate().format(DATE_FMT) : "-", centerStyle);
                createCell(row, 8, req.getTotalDays() != null ? req.getTotalDays().stripTrailingZeros().toPlainString() : "0", centerStyle);
                createCell(row, 9, req.getReason() != null ? req.getReason() : "-", dataStyle);
                createCell(row, 10, formatLeaveStatus(req.getStatus()), centerStyle);
                createCell(row, 11, req.getApproverDisplayName() != null ? req.getApproverDisplayName() : "-", dataStyle);
                createCell(row, 12, req.getReviewedAt() != null ? req.getReviewedAt().format(DATETIME_FMT) : "-", centerStyle);
                createCell(row, 13, req.getApproverComment() != null ? req.getApproverComment() : "-", dataStyle);
                createCell(row, 14, req.getCreatedAt() != null ? req.getCreatedAt().format(DATETIME_FMT) : "-", centerStyle);
            }

            autoSizeColumns(sheet, headers.length);
            wb.write(os);
        }
    }

    // ==========================================
    // 3. XUẤT DANH SÁCH HỢP ĐỒNG LAO ĐỘNG
    // ==========================================
    public void exportContracts(String companyName, List<Map<String, Object>> contracts, OutputStream os) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Danh sach hop dong");
            sheet.setDisplayGridlines(true);

            CellStyle titleStyle = createTitleStyle(wb, IndexedColors.DARK_BLUE);
            CellStyle subtitleStyle = createSubtitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb, IndexedColors.GREY_50_PERCENT);
            CellStyle dataStyle = createDataStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);

            // Title
            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("DANH SÁCH HỢP ĐỒNG LAO ĐỘNG");
            c0.setCellStyle(titleStyle);

            Row r1 = sheet.createRow(1);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Công ty: " + (companyName != null ? companyName : "HRMs Cloud") + " | Ngày xuất: " + LocalDate.now().format(DATE_FMT));
            c1.setCellStyle(subtitleStyle);

            String[] headers = {
                    "STT", "Mã HĐ", "Mã NV", "Họ và tên", "Loại hợp đồng",
                    "Ngày bắt đầu", "Ngày kết thúc", "Lương trước khấu trừ (VND)",
                    "Phụ cấp đi lại (VND)", "Trạng thái", "Ngày ký kết",
                    "Ngày chấm dứt", "Lý do chấm dứt", "Ghi chú"
            };

            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(26);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            int stt = 1;
            for (Map<String, Object> c : contracts) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                createCell(row, 0, String.valueOf(stt++), centerStyle);
                createCell(row, 1, val(c.get("ContractCode")), centerStyle);
                createCell(row, 2, val(c.get("EmployeeCode")), centerStyle);
                createCell(row, 3, val(c.get("FullName")), dataStyle);
                createCell(row, 4, formatContractType(val(c.get("ContractType"))), centerStyle);
                createCell(row, 5, formatDateVal(c.get("StartDate")), centerStyle);
                createCell(row, 6, c.get("EndDate") != null ? formatDateVal(c.get("EndDate")) : "Không xác định", centerStyle);

                // GrossSalary
                Cell grossCell = row.createCell(7);
                grossCell.setCellValue(parseNumeric(c.get("GrossSalary")));
                grossCell.setCellStyle(currencyStyle);

                // TravelAllowance
                Cell travelCell = row.createCell(8);
                travelCell.setCellValue(parseNumeric(c.get("TravelAllowance")));
                travelCell.setCellStyle(currencyStyle);

                createCell(row, 9, formatContractStatus(val(c.get("DisplayStatus"))), centerStyle);
                createCell(row, 10, formatDateVal(c.get("SignedDate")), centerStyle);
                createCell(row, 11, formatDateVal(c.get("TerminatedDate")), centerStyle);
                createCell(row, 12, val(c.get("TerminationReason")), dataStyle);
                createCell(row, 13, val(c.get("Notes")), dataStyle);
            }

            autoSizeColumns(sheet, headers.length);
            wb.write(os);
        }
    }

    // ==========================================
    // HELPER STYLES & METHODS
    // ==========================================
    private CellStyle createTitleStyle(Workbook wb, IndexedColors color) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 15);
        f.setColor(color.getIndex());
        s.setFont(f);
        return s;
    }

    private CellStyle createSubtitleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setItalic(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        s.setFont(f);
        return s;
    }

    private CellStyle createHeaderStyle(Workbook wb, IndexedColors bg) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        f.setFontHeightInPoints((short) 11);
        s.setFont(f);
        s.setFillForegroundColor(bg.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(s);
        return s;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(s);
        return s;
    }

    private CellStyle createCenterStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(s);
        return s;
    }

    private CellStyle createCurrencyStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.RIGHT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        DataFormat df = wb.createDataFormat();
        s.setDataFormat(df.getFormat("#,##0"));
        setBorders(s);
        return s;
    }

    private void setBorders(CellStyle s) {
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    private void createCell(Row r, int col, String value, CellStyle style) {
        Cell c = r.createCell(col);
        c.setCellValue(value != null && !value.isBlank() ? value : "-");
        c.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet, int numCols) {
        for (int i = 0; i < numCols; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            if (width < 3200) sheet.setColumnWidth(i, 3200);
            if (width > 12000) sheet.setColumnWidth(i, 12000);
        }
    }

    private String val(Object obj) {
        return obj == null ? "" : String.valueOf(obj);
    }

    private double parseNumeric(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(obj));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String formatDateVal(Object obj) {
        if (obj == null) return "-";
        String s = String.valueOf(obj);
        if (s.length() >= 10) {
            try {
                LocalDate d = LocalDate.parse(s.substring(0, 10));
                return d.format(DATE_FMT);
            } catch (Exception ignored) {}
        }
        return s;
    }

    private String formatGender(String g) {
        if (g == null) return "-";
        return switch (g.toLowerCase()) {
            case "male", "nam" -> "Nam";
            case "female", "nữ", "nu" -> "Nữ";
            default -> "Khác";
        };
    }

    private String formatLeaveStatus(String status) {
        if (status == null) return "-";
        return switch (status.toUpperCase()) {
            case "PENDING" -> "Chờ duyệt";
            case "APPROVED" -> "Đã duyệt";
            case "REJECTED" -> "Bị từ chối";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }

    private String formatContractType(String type) {
        if (type == null) return "-";
        return switch (type.toUpperCase()) {
            case "PROBATION" -> "Thử việc";
            case "FIXED_TERM" -> "Có thời hạn";
            case "INDEFINITE" -> "Không xác định thời hạn";
            default -> type;
        };
    }

    private String formatContractStatus(String status) {
        if (status == null) return "-";
        return switch (status.toUpperCase()) {
            case "DRAFT" -> "Bản nháp";
            case "ACTIVE" -> "Có hiệu lực";
            case "EXPIRED" -> "Đã hết hạn";
            case "TERMINATED" -> "Đã chấm dứt";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }
}

