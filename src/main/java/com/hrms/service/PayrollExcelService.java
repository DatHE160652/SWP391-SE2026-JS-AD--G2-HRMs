package com.hrms.service;

import com.hrms.model.PayrollImportInput;
import com.hrms.model.PayrollPeriod;
import com.hrms.model.PayrollRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class PayrollExcelService {
    public static final List<String> HEADERS = List.of(
        "Mã nhân viên",
        "Họ và tên",
        "Lương cơ bản",
        "Công thực",
        "Công chuẩn",
        "Lương theo công",
        "Phụ cấp trách nhiệm",
        "Phụ cấp đi lại",
        "Thưởng",
        "Ghi chú"
    );

    public List<PayrollImportInput> read(InputStream input) throws IOException {
        List<String> errors = new ArrayList<>();
        List<PayrollImportInput> result = new ArrayList<>();
        Set<String> codes = new HashSet<>();

        try (Workbook workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() < 1) throw new PayrollException(400, "File cần có ít nhất một sheet dữ liệu.");
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() > 5000) throw new PayrollException(400, "File tối đa 5.000 dòng dữ liệu.");

            Row header = sheet.getRow(0);
            if (header == null) throw new PayrollException(400, "File trống, không có dòng tiêu đề.");

            int colEmpCode = 0, colFullName = 1, colContractBase = 2, colActualWork = 3;
            int colStdWork = 4, colEarnedBase = 5, colResp = 6, colTravel = 7, colBonus = 8, colNote = 9;

            for (int c = 0; c < header.getLastCellNum(); c++) {
                String h = normalizeHeader(text(header.getCell(c)));
                if (h.contains("employeecode") || h.contains("manhanvien") || h.contains("manv")) colEmpCode = c;
                else if (h.contains("fullname") || h.contains("hoten") || h.contains("tennhanvien")) colFullName = c;
                else if (h.contains("contractbase") || h.contains("luongcoban") || h.contains("grosssalary")) colContractBase = c;
                else if (h.contains("actualwork") || h.contains("congthuc") || h.contains("ngaycongthuc")) colActualWork = c;
                else if (h.contains("standardwork") || h.contains("congchuan") || h.contains("ngaycongchuan")) colStdWork = c;
                else if (h.contains("earnedbase") || h.contains("luongtheocong") || h.contains("luongcong")) colEarnedBase = c;
                else if (h.contains("responsibility") || h.contains("trachnhiem")) colResp = c;
                else if (h.contains("travel") || h.contains("dilai")) colTravel = c;
                else if (h.contains("bonus") || h.contains("thuong") || h.contains("tienthuong")) colBonus = c;
                else if (h.contains("note") || h.contains("ghichu")) colNote = c;
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || blank(row)) continue;
                try {
                    String code = text(row.getCell(colEmpCode)).trim();
                    if (code.isBlank() || code.length() > 50)
                        throw new PayrollException(400, "Mã nhân viên bị trống hoặc quá dài.");
                    if (!codes.add(code.toUpperCase(Locale.ROOT)))
                        throw new PayrollException(400, "Mã nhân viên " + code + " bị trùng trong file.");

                    String fullName = text(row.getCell(colFullName)).trim();
                    BigDecimal contractBase = amount(row.getCell(colContractBase), "Lương cơ bản");
                    BigDecimal actualWork = decimalAmount(row.getCell(colActualWork), "Công thực tế", new BigDecimal("26.00"));
                    BigDecimal stdWork = decimalAmount(row.getCell(colStdWork), "Công chuẩn", new BigDecimal("26.00"));
                    if (stdWork.compareTo(BigDecimal.ZERO) <= 0) stdWork = new BigDecimal("26.00");

                    BigDecimal earnedBase = amount(row.getCell(colEarnedBase), "Lương theo công");
                    if (earnedBase.compareTo(BigDecimal.ZERO) <= 0) {
                        earnedBase = contractBase.multiply(actualWork).divide(stdWork, 2, RoundingMode.HALF_UP);
                    }

                    BigDecimal resp = amount(row.getCell(colResp), "Phụ cấp trách nhiệm");
                    BigDecimal travel = amount(row.getCell(colTravel), "Phụ cấp đi lại");
                    BigDecimal bonus = amount(row.getCell(colBonus), "Thưởng");
                    String note = PayrollService.note(text(row.getCell(colNote)));

                    BigDecimal netSalary = earnedBase.add(resp).add(travel).add(bonus);

                    result.add(new PayrollImportInput(i + 1, code, fullName, contractBase, actualWork,
                            stdWork, earnedBase, resp, travel, bonus, netSalary, note));
                } catch (PayrollException e) {
                    errors.add("Dòng " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (PayrollException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new PayrollException(400, "Không đọc được file Excel. Hãy tải lại file mẫu .xlsx hoặc .xls.");
        }

        if (!errors.isEmpty()) throw new PayrollException(400, "Chưa nhập được dữ liệu. Hãy sửa các dòng lỗi.", errors);
        if (result.isEmpty()) throw new PayrollException(400, "File chưa có dòng dữ liệu nhân viên nào.");
        return result;
    }

    public void template(PayrollPeriod period, List<PayrollRow> employees, OutputStream output) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            String sheetName = period != null ? ("BangLuong_T" + period.getMonth() + "_" + period.getYear()) : "BangLuong";
            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle heading = workbook.createCellStyle();
            heading.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            heading.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            heading.setFont(font);

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            CellStyle workDaysStyle = workbook.createCellStyle();
            workDaysStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

            Row header = sheet.createRow(0);
            header.setHeightInPoints(26);
            for (int i = 0; i < HEADERS.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS.get(i));
                cell.setCellStyle(heading);
            }

            BigDecimal defaultStdDays = new BigDecimal("26.00");
            for (int i = 0; i < employees.size(); i++) {
                PayrollRow emp = employees.get(i);
                Row row = sheet.createRow(i + 1);

                row.createCell(0).setCellValue(emp.employeeCode());
                row.createCell(1).setCellValue(emp.fullName() != null ? emp.fullName() : "");

                BigDecimal contractBase = emp.contractBaseSalary() != null ? emp.contractBaseSalary() : (emp.baseSalary() != null ? emp.baseSalary() : BigDecimal.ZERO);
                Cell cBase = row.createCell(2);
                cBase.setCellValue(contractBase.doubleValue());
                cBase.setCellStyle(moneyStyle);

                BigDecimal stdDays = emp.standardWorkDays() != null && emp.standardWorkDays().compareTo(BigDecimal.ZERO) > 0 ? emp.standardWorkDays() : defaultStdDays;
                BigDecimal actualDays = (emp.actualWorkDays() != null && emp.actualWorkDays().compareTo(BigDecimal.ZERO) > 0) ? emp.actualWorkDays() : stdDays;

                Cell cActDays = row.createCell(3);
                cActDays.setCellValue(actualDays.doubleValue());
                cActDays.setCellStyle(workDaysStyle);

                Cell cStdDays = row.createCell(4);
                cStdDays.setCellValue(stdDays.doubleValue());
                cStdDays.setCellStyle(workDaysStyle);

                BigDecimal earnedBase = stdDays.compareTo(BigDecimal.ZERO) > 0 ? contractBase.multiply(actualDays).divide(stdDays, 2, RoundingMode.HALF_UP) : contractBase;
                Cell cEarned = row.createCell(5);
                cEarned.setCellValue(earnedBase.doubleValue());
                cEarned.setCellStyle(moneyStyle);

                BigDecimal resp = emp.responsibilityAllowance() != null ? emp.responsibilityAllowance() : BigDecimal.ZERO;
                Cell cResp = row.createCell(6);
                cResp.setCellValue(resp.doubleValue());
                cResp.setCellStyle(moneyStyle);

                BigDecimal travel = emp.travelAllowance() != null ? emp.travelAllowance() : BigDecimal.ZERO;
                Cell cTravel = row.createCell(7);
                cTravel.setCellValue(travel.doubleValue());
                cTravel.setCellStyle(moneyStyle);

                BigDecimal bonus = emp.bonus() != null ? emp.bonus() : BigDecimal.ZERO;
                Cell cBonus = row.createCell(8);
                cBonus.setCellValue(bonus.doubleValue());
                cBonus.setCellStyle(moneyStyle);

                row.createCell(9).setCellValue(emp.note() != null ? emp.note() : "");
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, employees.size()), 0, HEADERS.size() - 1));

            sheet.setColumnWidth(0, 18 * 256);
            sheet.setColumnWidth(1, 26 * 256);
            sheet.setColumnWidth(2, 20 * 256);
            sheet.setColumnWidth(3, 16 * 256);
            sheet.setColumnWidth(4, 16 * 256);
            sheet.setColumnWidth(5, 20 * 256);
            sheet.setColumnWidth(6, 22 * 256);
            sheet.setColumnWidth(7, 20 * 256);
            sheet.setColumnWidth(8, 18 * 256);
            sheet.setColumnWidth(9, 36 * 256);

            workbook.write(output);
        }
    }

    private static String normalizeHeader(String text) {
        if (text == null) return "";
        return text.toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")
                .replace("đ", "d")
                .replace("á", "a").replace("à", "a").replace("ả", "a").replace("ã", "a").replace("ạ", "a")
                .replace("ă", "a").replace("ắ", "a").replace("ằ", "a").replace("ẳ", "a").replace("ẵ", "a").replace("ặ", "a")
                .replace("â", "a").replace("ấ", "a").replace("ầ", "a").replace("ẩ", "a").replace("ẫ", "a").replace("ậ", "a")
                .replace("é", "e").replace("è", "e").replace("ẻ", "e").replace("ẽ", "e").replace("ẹ", "e")
                .replace("ê", "e").replace("ế", "e").replace("ề", "e").replace("ể", "e").replace("ễ", "e").replace("ệ", "e")
                .replace("í", "i").replace("ì", "i").replace("ỉ", "i").replace("ĩ", "i").replace("ị", "i")
                .replace("ó", "o").replace("ò", "o").replace("ỏ", "o").replace("õ", "o").replace("ọ", "o")
                .replace("ô", "o").replace("ố", "o").replace("ồ", "o").replace("ổ", "o").replace("ỗ", "o").replace("ộ", "o")
                .replace("ơ", "o").replace("ớ", "o").replace("ờ", "o").replace("ở", "o").replace("ỡ", "o").replace("ợ", "o")
                .replace("ú", "u").replace("ù", "u").replace("ủ", "u").replace("ũ", "u").replace("ụ", "u")
                .replace("ư", "u").replace("ứ", "u").replace("ừ", "u").replace("ử", "u").replace("ữ", "u").replace("ự", "u")
                .replace("ý", "y").replace("ỳ", "y").replace("ỷ", "y").replace("ỹ", "y").replace("ỵ", "y");
    }

    private boolean blank(Row row) {
        for (Cell c : row) {
            if (c.getCellType() != CellType.BLANK && !new DataFormatter(Locale.ROOT).formatCellValue(c).isBlank()) return false;
        }
        return true;
    }

    private String text(Cell c) {
        if (c == null || c.getCellType() == CellType.BLANK) return "";
        if (c.getCellType() == CellType.FORMULA) {
            try {
                return new DataFormatter(Locale.ROOT).formatCellValue(c).trim();
            } catch (Exception ignored) {
                return "";
            }
        }
        if (c.getCellType() == CellType.ERROR || c.getCellType() == CellType.BOOLEAN) {
            throw new PayrollException(400, "Ô dữ liệu không hợp lệ.");
        }
        return new DataFormatter(Locale.ROOT).formatCellValue(c).trim();
    }

    private BigDecimal amount(Cell cell, String label) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return new BigDecimal("0.00");
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) throw new PayrollException(400, label + " không được là ngày tháng.");
            return PayrollService.money(BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString(), label);
        }
        String raw = text(cell);
        return PayrollService.money(raw.isBlank() ? "0" : raw, label);
    }

    private BigDecimal decimalAmount(Cell cell, String label, BigDecimal defaultVal) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return defaultVal;
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) throw new PayrollException(400, label + " không được là ngày tháng.");
            double d = cell.getNumericCellValue();
            if (d < 0) throw new PayrollException(400, label + " không được âm.");
            return BigDecimal.valueOf(d).setScale(2, RoundingMode.HALF_UP);
        }
        String raw = text(cell);
        if (raw.isBlank()) return defaultVal;
        try {
            BigDecimal val = new BigDecimal(raw.trim());
            if (val.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
            return val.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new PayrollException(400, label + " phải là số không âm.");
        }
    }
}
