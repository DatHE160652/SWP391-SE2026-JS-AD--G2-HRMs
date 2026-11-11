package com.hrms.service;

import com.hrms.dao.AttendanceDAO.AttendanceDTO;
import com.hrms.model.Department;
import com.hrms.model.Employee;
import com.hrms.model.Position;
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

/** Service xuất file Excel cho nhân sự, hợp đồng, tổ chức và chấm công. */
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
    // 2. XUẤT DANH SÁCH HỢP ĐỒNG LAO ĐỘNG
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
    // 4. XUẤT DANH SÁCH PHÒNG BAN
    // ==========================================
    public void exportDepartments(String companyName, List<Department> departments, OutputStream os) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Danh sach phong ban");
            sheet.setDisplayGridlines(true);

            CellStyle titleStyle = createTitleStyle(wb, IndexedColors.DARK_BLUE);
            CellStyle subtitleStyle = createSubtitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb, IndexedColors.ROYAL_BLUE);
            CellStyle dataStyle = createDataStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);

            // Title
            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("DANH SÁCH PHÒNG BAN");
            c0.setCellStyle(titleStyle);

            Row r1 = sheet.createRow(1);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Công ty: " + (companyName != null ? companyName : "HRMs Cloud") + " | Ngày xuất: " + LocalDate.now().format(DATE_FMT));
            c1.setCellStyle(subtitleStyle);

            String[] headers = {
                    "STT", "Mã phòng ban", "Tên phòng ban", "Trạng thái", "Ngày tạo"
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
            for (Department d : departments) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                createCell(row, 0, String.valueOf(stt++), centerStyle);
                createCell(row, 1, d.getDepartmentCode(), centerStyle);
                createCell(row, 2, d.getDepartmentName(), dataStyle);
                createCell(row, 3, d.isActive() ? "Đang hoạt động" : "Ngừng hoạt động", centerStyle);
                createCell(row, 4, d.getCreatedAt() != null ? d.getCreatedAt().format(DATETIME_FMT) : "-", centerStyle);
            }

            autoSizeColumns(sheet, headers.length);
            wb.write(os);
        }
    }

    // ==========================================
    // 5. XUẤT DANH SÁCH CHỨC DANH
    // ==========================================
    public void exportPositions(String companyName, List<Position> positions, OutputStream os) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Danh sach chuc danh");
            sheet.setDisplayGridlines(true);

            CellStyle titleStyle = createTitleStyle(wb, IndexedColors.DARK_BLUE);
            CellStyle subtitleStyle = createSubtitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb, IndexedColors.ROYAL_BLUE);
            CellStyle dataStyle = createDataStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);
            CellStyle currencyStyle = createCurrencyStyle(wb);

            // Title
            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("DANH SÁCH CHỨC DANH CÔNG VIỆC");
            c0.setCellStyle(titleStyle);

            Row r1 = sheet.createRow(1);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Công ty: " + (companyName != null ? companyName : "HRMs Cloud") + " | Ngày xuất: " + LocalDate.now().format(DATE_FMT));
            c1.setCellStyle(subtitleStyle);

            String[] headers = {
                    "STT", "Tên chức danh", "Phòng ban", "Cấp trách nhiệm",
                    "Phụ cấp chức danh (VND)", "Quyền quản lý", "Trạng thái", "Mô tả công việc"
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
            for (Position p : positions) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                createCell(row, 0, String.valueOf(stt++), centerStyle);
                createCell(row, 1, p.getPositionTitle(), dataStyle);
                createCell(row, 2, p.getDepartmentName() != null ? p.getDepartmentName() : "-", dataStyle);
                createCell(row, 3, p.getResponsibilityLevelName() != null ? p.getResponsibilityLevelName() : "-", centerStyle);

                // Phụ cấp chức danh
                Cell allowanceCell = row.createCell(4);
                BigDecimal allowance = p.getEffectiveAllowance() != null ? p.getEffectiveAllowance() : p.getPositionAllowance();
                allowanceCell.setCellValue(parseNumeric(allowance));
                allowanceCell.setCellStyle(currencyStyle);

                createCell(row, 5, p.isCanManageEmployees() ? "Có (Quản lý)" : "Không", centerStyle);
                createCell(row, 6, p.isActive() ? "Đang hoạt động" : "Ngừng hoạt động", centerStyle);
                createCell(row, 7, p.getDescription() != null ? p.getDescription() : "-", dataStyle);
            }

            autoSizeColumns(sheet, headers.length);
            wb.write(os);
        }
    }

    // ==========================================
    // 6. XUẤT SƠ ĐỒ / CƠ CẤU TỔ CHỨC
    // ==========================================
    public void exportOrganization(String companyName, List<Map<String, Object>> members, OutputStream os) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Co cau to chuc");
            sheet.setDisplayGridlines(true);

            CellStyle titleStyle = createTitleStyle(wb, IndexedColors.DARK_BLUE);
            CellStyle subtitleStyle = createSubtitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb, IndexedColors.ROYAL_BLUE);
            CellStyle dataStyle = createDataStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);

            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("CƠ CẤU TỔ CHỨC NHÂN SỰ");
            c0.setCellStyle(titleStyle);

            Row r1 = sheet.createRow(1);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Công ty: " + (companyName != null ? companyName : "HRMs Cloud") + " | Ngày xuất: " + LocalDate.now().format(DATE_FMT));
            c1.setCellStyle(subtitleStyle);

            String[] headers = {
                    "STT", "Mã NV", "Họ và tên", "Phòng ban", "Chức danh",
                    "Cấp trách nhiệm", "Quản lý trực tiếp", "Email", "Số điện thoại", "Trạng thái phân công"
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
            for (Map<String, Object> m : members) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                boolean hasAssignment = Boolean.TRUE.equals(m.get("HasAssignment"));
                createCell(row, 0, String.valueOf(stt++), centerStyle);
                createCell(row, 1, val(m.get("EmployeeCode")), centerStyle);
                createCell(row, 2, val(m.get("FullName")), dataStyle);
                createCell(row, 3, !val(m.get("DepartmentName")).isBlank() ? val(m.get("DepartmentName")) : "Chưa phân công", dataStyle);
                createCell(row, 4, !val(m.get("PositionTitle")).isBlank() ? val(m.get("PositionTitle")) : "Chưa phân công", dataStyle);
                createCell(row, 5, val(m.get("LevelName")), centerStyle);
                createCell(row, 6, val(m.get("ManagerName")), dataStyle);
                createCell(row, 7, val(m.get("Email")), dataStyle);
                createCell(row, 8, val(m.get("Phone")), centerStyle);
                createCell(row, 9, hasAssignment ? "Đã phân công" : "Chưa phân công", centerStyle);
            }

            autoSizeColumns(sheet, headers.length);
            wb.write(os);
        }
    }

    // ==========================================
    // 7. XUẤT DANH SÁCH ĐIỂM DANH CHẤM CÔNG (PHÙ HỢP FORM VỚI FILE IMPORT)
    // ==========================================
    public void exportAttendances(String companyName, List<AttendanceDTO> attendances, OutputStream os) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Cham cong");
            sheet.setDisplayGridlines(true);

            CellStyle titleStyle = createTitleStyle(wb, IndexedColors.DARK_BLUE);
            CellStyle subtitleStyle = createSubtitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb, IndexedColors.ROYAL_BLUE);
            CellStyle dataStyle = createDataStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);

            // Title
            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("DANH SÁCH ĐIỂM DANH CHẤM CÔNG");
            c0.setCellStyle(titleStyle);

            Row r1 = sheet.createRow(1);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Công ty: " + (companyName != null ? companyName : "HRMs Cloud") + " | Ngày xuất: " + LocalDate.now().format(DATE_FMT));
            c1.setCellStyle(subtitleStyle);

            String[] headers = {
                    "STT", "Mã NV (*)", "Họ và tên", "Phòng ban",
                    "Ngày chấm công (yyyy-MM-dd) (*)", "Giờ vào (HH:mm)", "Giờ ra (HH:mm)",
                    "Tổng giờ", "Trạng thái", "Ghi chú"
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
            for (AttendanceDTO a : attendances) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                createCell(row, 0, String.valueOf(stt++), centerStyle);
                createCell(row, 1, a.getEmployeeCode() != null ? a.getEmployeeCode() : "-", centerStyle);
                createCell(row, 2, a.getFullName() != null ? a.getFullName() : "-", dataStyle);
                createCell(row, 3, a.getDepartmentName() != null ? a.getDepartmentName() : "-", dataStyle);
                createCell(row, 4, a.getAttendanceDate() != null ? a.getAttendanceDate().toString() : "-", centerStyle);
                createCell(row, 5, a.getCheckInTime() != null ? a.getCheckInTime().toString().substring(0, Math.min(5, a.getCheckInTime().toString().length())) : "-", centerStyle);
                createCell(row, 6, a.getCheckOutTime() != null ? a.getCheckOutTime().toString().substring(0, Math.min(5, a.getCheckOutTime().toString().length())) : "-", centerStyle);
                createCell(row, 7, a.getTotalHours() != null ? a.getTotalHours().toString() : "0.00", centerStyle);
                createCell(row, 8, a.getStatus() != null ? formatAttendanceStatus(a.getStatus()) : "-", centerStyle);
                createCell(row, 9, a.getNote() != null ? a.getNote() : "-", dataStyle);
            }

            autoSizeColumns(sheet, headers.length);
            wb.write(os);
        }
    }

    private String formatAttendanceStatus(String status) {
        if (status == null) return "-";
        return switch (status.toUpperCase()) {
            case "ON_TIME", "PRESENT" -> "Đúng giờ";
            case "LATE" -> "Đi muộn";
            case "EARLY_LEAVE" -> "Về sớm";
            case "LATE_AND_EARLY" -> "Muộn & Về sớm";
            case "ABSENT" -> "Vắng mặt";
            case "LEAVE" -> "Nghỉ phép";
            default -> status;
        };
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
            case "SCHEDULED" -> "Chờ hiệu lực";
            case "ACTIVE" -> "Có hiệu lực";
            case "EXPIRED" -> "Đã hết hạn";
            case "TERMINATED" -> "Đã chấm dứt";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }
}

