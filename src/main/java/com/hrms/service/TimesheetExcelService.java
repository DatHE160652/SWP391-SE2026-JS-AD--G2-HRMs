package com.hrms.service;

import com.hrms.dao.AttendanceDAO.AttendanceDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service generating formatted Excel workbook (.xlsx) for monthly Timesheet Matrix.
 */
public class TimesheetExcelService {

    public static class TimesheetEmpData {
        private final int employeeId;
        private final String employeeCode;
        private final String fullName;
        private final String departmentName;
        private final String positionTitle;
        private final Map<Integer, AttendanceDTO> records;

        public TimesheetEmpData(int employeeId, String employeeCode, String fullName,
                                String departmentName, String positionTitle,
                                Map<Integer, AttendanceDTO> records) {
            this.employeeId = employeeId;
            this.employeeCode = employeeCode != null ? employeeCode : "";
            this.fullName = fullName != null ? fullName : "";
            this.departmentName = departmentName != null ? departmentName : "-";
            this.positionTitle = positionTitle != null ? positionTitle : "-";
            this.records = records != null ? records : Map.of();
        }

        public int getEmployeeId() { return employeeId; }
        public String getEmployeeCode() { return employeeCode; }
        public String getFullName() { return fullName; }
        public String getDepartmentName() { return departmentName; }
        public String getPositionTitle() { return positionTitle; }
        public Map<Integer, AttendanceDTO> getRecords() { return records; }
    }

    public void exportTimesheet(String companyName, int month, int year, int daysInMonth,
                                List<TimesheetEmpData> employeeList, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Bảng chấm công T" + month + "_" + year);
            sheet.setDisplayGridlines(true);

            // Fonts
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            Font subFont = workbook.createFont();
            subFont.setItalic(true);
            subFont.setFontHeightInPoints((short) 9);
            subFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 10);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 9);

            Font regularFont = workbook.createFont();
            regularFont.setFontHeightInPoints((short) 9);

            // Title Style
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Subtitle Style
            CellStyle subStyle = workbook.createCellStyle();
            subStyle.setFont(subFont);
            subStyle.setAlignment(HorizontalAlignment.LEFT);
            subStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(headerStyle);

            // Header Summary Style
            CellStyle headerSummaryStyle = workbook.createCellStyle();
            headerSummaryStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerSummaryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerSummaryStyle.setFont(headerFont);
            headerSummaryStyle.setAlignment(HorizontalAlignment.CENTER);
            headerSummaryStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(headerSummaryStyle);

            // Normal Data Cell Styles
            CellStyle cellTextLeft = workbook.createCellStyle();
            cellTextLeft.setFont(regularFont);
            cellTextLeft.setAlignment(HorizontalAlignment.LEFT);
            cellTextLeft.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(cellTextLeft);

            CellStyle cellTextCenter = workbook.createCellStyle();
            cellTextCenter.setFont(regularFont);
            cellTextCenter.setAlignment(HorizontalAlignment.CENTER);
            cellTextCenter.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(cellTextCenter);

            CellStyle cellNumberBold = workbook.createCellStyle();
            cellNumberBold.setFont(boldFont);
            cellNumberBold.setAlignment(HorizontalAlignment.CENTER);
            cellNumberBold.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(cellNumberBold);

            // Status Cell Styles
            CellStyle stylePresent = createStatusStyle(workbook, regularFont, IndexedColors.LIGHT_GREEN.getIndex());
            CellStyle styleLate = createStatusStyle(workbook, regularFont, IndexedColors.LEMON_CHIFFON.getIndex());
            CellStyle styleEarly = createStatusStyle(workbook, regularFont, IndexedColors.LIGHT_TURQUOISE.getIndex());
            CellStyle styleOnLeave = createStatusStyle(workbook, regularFont, IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            CellStyle styleAbsent = createStatusStyle(workbook, regularFont, IndexedColors.ROSE.getIndex());
            CellStyle styleEmpty = createStatusStyle(workbook, regularFont, IndexedColors.WHITE.getIndex());

            int totalCols = 5 + daysInMonth + 8;

            // Row 0: Title
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(26);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BẢNG CHẤM CÔNG NHÂN SỰ - THÁNG " + month + "/" + year);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, Math.min(10, totalCols - 1)));

            // Row 1: Subtitle
            Row subRow = sheet.createRow(1);
            subRow.setHeightInPoints(18);
            Cell subCell = subRow.createCell(0);
            String subText = "Công ty: " + (companyName != null && !companyName.isBlank() ? companyName : "HRMs Cloud")
                    + "   |   Ngày xuất: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + "   |   Tổng nhân sự: " + employeeList.size();
            subCell.setCellValue(subText);
            subCell.setCellStyle(subStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, Math.min(10, totalCols - 1)));

            // Row 3: Headers
            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(24);

            int col = 0;
            createCell(headerRow, col++, "STT", headerStyle);
            createCell(headerRow, col++, "Mã NV", headerStyle);
            createCell(headerRow, col++, "Họ và Tên", headerStyle);
            createCell(headerRow, col++, "Phòng ban", headerStyle);
            createCell(headerRow, col++, "Chức danh", headerStyle);

            for (int d = 1; d <= daysInMonth; d++) {
                createCell(headerRow, col++, "N" + d, headerStyle);
            }

            createCell(headerRow, col++, "Ngày làm", headerSummaryStyle);
            createCell(headerRow, col++, "Số công", headerSummaryStyle);
            createCell(headerRow, col++, "Tổng giờ", headerSummaryStyle);
            createCell(headerRow, col++, "Đúng giờ (P)", headerSummaryStyle);
            createCell(headerRow, col++, "Đi muộn (L)", headerSummaryStyle);
            createCell(headerRow, col++, "Về sớm (EL)", headerSummaryStyle);
            createCell(headerRow, col++, "Nghỉ phép (OL)", headerSummaryStyle);
            createCell(headerRow, col++, "Vắng (A)", headerSummaryStyle);

            // Data Rows
            int rowIndex = 4;
            int stt = 1;

            for (TimesheetEmpData emp : employeeList) {
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(20);
                col = 0;

                createCell(row, col++, String.valueOf(stt++), cellTextCenter);
                createCell(row, col++, emp.getEmployeeCode(), cellTextCenter);
                createCell(row, col++, emp.getFullName(), cellTextLeft);
                createCell(row, col++, emp.getDepartmentName(), cellTextLeft);
                createCell(row, col++, emp.getPositionTitle(), cellTextLeft);

                int present = 0, late = 0, early = 0, onLeave = 0, absent = 0, workDays = 0;
                double totalHours = 0.0;
                double totalCredits = 0.0;

                for (int d = 1; d <= daysInMonth; d++) {
                    AttendanceDTO record = emp.getRecords().get(d);
                    if (record != null) {
                        String st = record.getStatus() != null ? record.getStatus().toLowerCase() : "";
                        String displaySym;
                        CellStyle curStyle;

                        if (st.contains("late") || st.contains("muộn") || st.contains("trễ")) {
                            displaySym = "L";
                            curStyle = styleLate;
                            late++;
                            workDays++;
                        } else if (st.contains("early") || st.contains("sớm")) {
                            displaySym = "EL";
                            curStyle = styleEarly;
                            early++;
                            workDays++;
                        } else if (st.contains("leave") || st.contains("phép")) {
                            displaySym = "OL";
                            curStyle = styleOnLeave;
                            onLeave++;
                        } else if (st.contains("absent") || st.contains("vắng")) {
                            displaySym = "A";
                            curStyle = styleAbsent;
                            absent++;
                        } else {
                            displaySym = "P";
                            curStyle = stylePresent;
                            present++;
                            workDays++;
                        }

                        if (record.getTotalHours() != null) {
                            totalHours += record.getTotalHours().doubleValue();
                        }
                        if (record.getWorkCredit() != null) {
                            totalCredits += record.getWorkCredit().doubleValue();
                        }

                        createCell(row, col++, displaySym, curStyle);
                    } else {
                        createCell(row, col++, "-", styleEmpty);
                    }
                }

                // Summaries
                createCell(row, col++, String.valueOf(workDays), cellNumberBold);
                createCell(row, col++, String.format(java.util.Locale.US, "%.1f", totalCredits), cellNumberBold);
                createCell(row, col++, String.format(java.util.Locale.US, "%.2f", totalHours), cellNumberBold);
                createCell(row, col++, String.valueOf(present), cellTextCenter);
                createCell(row, col++, String.valueOf(late), cellTextCenter);
                createCell(row, col++, String.valueOf(early), cellTextCenter);
                createCell(row, col++, String.valueOf(onLeave), cellTextCenter);
                createCell(row, col++, String.valueOf(absent), cellTextCenter);
            }

            // Legend Footer Row
            Row legendRow = sheet.createRow(rowIndex + 1);
            Cell legendCell = legendRow.createCell(0);
            legendCell.setCellValue("* Chú thích: (P): Đúng giờ | (L): Đi muộn | (EL): Về sớm | (OL): Nghỉ phép | (A): Vắng mặt | Quy tắc tính công: ≥ 7.0h = 1.0 công, 3.5h - 7.0h = 0.5 công, < 3.5h = 0 công.");
            legendCell.setCellStyle(subStyle);

            // Freeze first 5 columns and 4 header rows
            sheet.createFreezePane(5, 4);

            // Column Widths
            sheet.setColumnWidth(0, 6 * 256);   // STT
            sheet.setColumnWidth(1, 14 * 256);  // Mã NV
            sheet.setColumnWidth(2, 22 * 256);  // Họ và tên
            sheet.setColumnWidth(3, 20 * 256);  // Phòng ban
            sheet.setColumnWidth(4, 18 * 256);  // Chức danh

            for (int d = 1; d <= daysInMonth; d++) {
                sheet.setColumnWidth(4 + d, 6 * 256); // Ngày N
            }

            int summaryStart = 5 + daysInMonth;
            sheet.setColumnWidth(summaryStart, 12 * 256);     // Ngày làm
            sheet.setColumnWidth(summaryStart + 1, 12 * 256); // Số công
            sheet.setColumnWidth(summaryStart + 2, 12 * 256); // Tổng giờ
            sheet.setColumnWidth(summaryStart + 3, 13 * 256); // P
            sheet.setColumnWidth(summaryStart + 4, 13 * 256); // L
            sheet.setColumnWidth(summaryStart + 5, 13 * 256); // EL
            sheet.setColumnWidth(summaryStart + 6, 14 * 256); // OL
            sheet.setColumnWidth(summaryStart + 7, 12 * 256); // A

            workbook.write(outputStream);
        }
    }

    private static void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private static CellStyle createStatusStyle(Workbook workbook, Font font, short bgColor) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(bgColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private static void createCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }
}

