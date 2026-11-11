package com.hrms.service;

import com.hrms.dao.AttendanceDAO;
import com.hrms.dao.CompanyShiftSettingDAO;
import com.hrms.dao.DepartmentDAO;
import com.hrms.dao.EmployeeDAO;
import com.hrms.dao.PositionDAO;
import com.hrms.model.CompanyShiftSetting;
import com.hrms.model.Department;
import com.hrms.model.Employee;
import com.hrms.model.ImportExecuteResult;
import com.hrms.model.ImportPreviewResult;
import com.hrms.model.ImportRowResult;
import com.hrms.model.Position;
import com.hrms.util.DBConnection;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Service handling Excel data import and template generation for:
 * 1. Departments
 * 2. Positions
 * 3. Employees
 * 4. Attendances
 */
public class DataImportExcelService {

    public static final String TYPE_DEPARTMENT = "DEPARTMENT";
    public static final String TYPE_POSITION = "POSITION";
    public static final String TYPE_EMPLOYEE = "EMPLOYEE";
    public static final String TYPE_ATTENDANCE = "ATTENDANCE";
    public static final String TYPE_CONTRACT = "CONTRACT";

    private static final DateTimeFormatter DATE_FMT_VN_STANDARD = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("d/M/yy"),
            DateTimeFormatter.ofPattern("d-M-yy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final DepartmentDAO departmentDAO;
    private final PositionDAO positionDAO;
    private final EmployeeDAO employeeDAO;
    private final AttendanceDAO attendanceDAO;
    private final AttendanceService attendanceService;

    public DataImportExcelService() {
        this.departmentDAO = new DepartmentDAO();
        this.positionDAO = new PositionDAO();
        this.employeeDAO = new EmployeeDAO();
        this.attendanceDAO = new AttendanceDAO();
        this.attendanceService = new AttendanceService();
    }

    public DataImportExcelService(DepartmentDAO departmentDAO, PositionDAO positionDAO,
                                  EmployeeDAO employeeDAO, AttendanceDAO attendanceDAO,
                                  AttendanceService attendanceService) {
        this.departmentDAO = departmentDAO;
        this.positionDAO = positionDAO;
        this.employeeDAO = employeeDAO;
        this.attendanceDAO = attendanceDAO;
        this.attendanceService = attendanceService;
    }

    // =========================================================================
    // 1. GENERATE EXCEL TEMPLATE
    // =========================================================================

    public void generateTemplate(String rawType, String companyName, OutputStream os) throws IOException {
        String type = normalizeType(rawType);
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Import Template");
            sheet.setDisplayGridlines(true);

            CellStyle titleStyle = createTitleStyle(wb, IndexedColors.DARK_BLUE);
            CellStyle subtitleStyle = createSubtitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb, IndexedColors.ROYAL_BLUE);
            CellStyle sampleStyle = createSampleStyle(wb);

            // Title
            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("MẪU NHẬP DỮ LIỆU EXCEL - " + getTypeNameVi(type).toUpperCase());
            c0.setCellStyle(titleStyle);

            Row r1 = sheet.createRow(1);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Công ty: " + (companyName != null ? companyName : "HRMs Cloud") + " | Cột có dấu (*) là bắt buộc. Dòng 5 là dữ liệu mẫu minh họa.");
            c1.setCellStyle(subtitleStyle);

            String[] headers = getHeadersForType(type);
            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(26);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample row at row 4
            String[] sampleData = getSampleDataForType(type);
            Row sampleRow = sheet.createRow(4);
            sampleRow.setHeightInPoints(20);
            for (int i = 0; i < sampleData.length; i++) {
                Cell cell = sampleRow.createCell(i);
                cell.setCellValue(sampleData[i]);
                cell.setCellStyle(sampleStyle);
            }

            autoSizeColumns(sheet, headers.length);
            wb.write(os);
        }
    }

    // =========================================================================
    // 2. PREVIEW IMPORT (PARSE & VALIDATE WITHOUT WRITING TO DB)
    // =========================================================================

    public ImportPreviewResult previewImport(String rawType, int companyId, InputStream is) throws IOException {
        if (rawType != null && !rawType.isBlank()) {
            normalizeType(rawType);
        }
        ImportPreviewResult result = new ImportPreviewResult();

        try (Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                result.setType(normalizeType(rawType));
                result.getGlobalErrors().add("File Excel không có dữ liệu sheet nào.");
                return result;
            }

            // Auto-detect actual data type in file, or fallback to requested rawType
            String type = detectFileType(sheet, rawType);
            result.setType(type);

            String[] expectedHeaders = getHeadersForType(type);
            result.setColumnHeaders(Arrays.asList(expectedHeaders));

            int headerRowIdx = findHeaderRowIndex(sheet, type);
            if (headerRowIdx < 0) {
                result.getGlobalErrors().add("Không tìm thấy hàng tiêu đề hợp lệ. Vui lòng sử dụng đúng file mẫu tải về từ hệ thống.");
                return result;
            }

            // Map each expectedHeader to column index in the sheet
            Row headerRow = sheet.getRow(headerRowIdx);
            Map<String, Integer> colIndexMap = new HashMap<>();
            if (headerRow != null) {
                for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                    String hText = getCellStringValue(headerRow.getCell(c)).trim();
                    if (!hText.isEmpty()) {
                        colIndexMap.put(hText.toLowerCase(), c);
                    }
                }
            }

            int[] headerColMapping = new int[expectedHeaders.length];
            for (int i = 0; i < expectedHeaders.length; i++) {
                String exp = expectedHeaders[i];
                String expLower = exp.toLowerCase();
                String cleanExp = exp.replace("(*)", "").trim().toLowerCase();
                Integer col = colIndexMap.get(expLower);
                if (col == null) {
                    col = colIndexMap.get(cleanExp);
                }
                if (col == null) {
                    for (Map.Entry<String, Integer> entry : colIndexMap.entrySet()) {
                        String k = entry.getKey();
                        if (k.contains(cleanExp) || cleanExp.contains(k)) {
                            col = entry.getValue();
                            break;
                        }
                    }
                }
                headerColMapping[i] = (col != null) ? col : i;
            }

            int lastRowNum = sheet.getLastRowNum();

            // Intelligent auto-alignment check:
            // Check if user pasted data from export format [STT, Mã HĐ, Mã NV, Họ và tên, Loại HĐ, ...]
            // into an old template where headers did not have STT
            if (TYPE_CONTRACT.equals(type) && !colIndexMap.containsKey("stt")) {
                for (int checkR = headerRowIdx + 1; checkR <= lastRowNum; checkR++) {
                    Row checkRow = sheet.getRow(checkR);
                    if (checkRow == null || isRowEmpty(checkRow, 5)) continue;
                    String val0 = getCellStringValue(checkRow.getCell(0));
                    String val4 = getCellStringValue(checkRow.getCell(4));
                    if ((val0.matches("^\\d+$") || "1".equals(val0))
                            && (val4.toLowerCase().contains("thời hạn") || val4.toLowerCase().contains("thử việc")
                            || val4.toUpperCase().contains("PROBATION") || val4.toUpperCase().contains("FIXED")
                            || val4.toUpperCase().contains("INDEFINITE"))) {
                        // Data row has [STT (0), Mã HĐ (1), Mã NV (2), Họ và tên (3), Loại HĐ (4), Ngày bắt đầu (5), Ngày kết thúc (6), Lương (7), Phụ cấp (8), Trạng thái (9), Ngày ký (10), Ghi chú (11)]
                        headerColMapping = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
                        break;
                    }
                    break;
                }
            }

            int total = 0;
            int valid = 0;
            int error = 0;

            Set<String> seenCodesInFile = new HashSet<>();

            for (int r = headerRowIdx + 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row, expectedHeaders.length)) {
                    continue;
                }

                if (isSampleRow(type, row)) {
                    continue;
                }

                total++;
                Map<String, String> rowData = new LinkedHashMap<>();
                for (int c = 0; c < expectedHeaders.length; c++) {
                    int colIdx = headerColMapping[c];
                    Cell cell = (colIdx >= 0 && colIdx < row.getLastCellNum()) ? row.getCell(colIdx) : null;
                    String val = getCellStringValue(cell);
                    rowData.put(expectedHeaders[c], val);
                }

                String rowError = validateRowData(type, companyId, rowData, seenCodesInFile, r + 1);
                boolean isValid = (rowError == null);
                if (isValid) {
                    valid++;
                } else {
                    error++;
                }

                result.getPreviewRows().add(new ImportRowResult(r + 1, rowData, isValid, rowError));
            }

            result.setTotalRows(total);
            result.setValidRows(valid);
            result.setErrorRows(error);
            result.setCanProceed(total > 0 && error == 0);

            if (total == 0) {
                result.getGlobalErrors().add("Tệp Excel chưa có dòng dữ liệu nào để nhập.");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            result.getGlobalErrors().add("Lỗi khi đọc file Excel: " + e.getMessage());
        }

        return result;
    }

    // =========================================================================
    // 3. EXECUTE IMPORT (COMMIT TO DB IN TRANSACTION)
    // =========================================================================

    public ImportExecuteResult executeImport(String rawType, int companyId, Integer userId, InputStream is) {
        ImportExecuteResult result = new ImportExecuteResult();

        try {
            ImportPreviewResult preview = previewImport(rawType, companyId, is);
            String type = preview.getType();
            if (!preview.getGlobalErrors().isEmpty()) {
                result.setSuccess(false);
                result.setMessage(String.join("; ", preview.getGlobalErrors()));
                return result;
            }
            if (preview.getTotalRows() == 0) {
                result.setSuccess(false);
                result.setMessage("Không có dữ liệu để nhập.");
                return result;
            }
            if (preview.getErrorRows() > 0) {
                result.setSuccess(false);
                result.setMessage("Có " + preview.getErrorRows() + " dòng dữ liệu không hợp lệ. Vui lòng kiểm tra lại file trước khi nhập.");
                List<String> errors = new ArrayList<>();
                for (ImportRowResult r : preview.getPreviewRows()) {
                    if (!r.isValid()) {
                        errors.add("Dòng " + r.getRowNumber() + ": " + r.getErrorMessage());
                    }
                }
                result.setErrorMessages(errors);
                return result;
            }

            // Execute according to type
            switch (type) {
                case TYPE_EMPLOYEE -> executeImportEmployees(companyId, preview.getPreviewRows(), result);
                case TYPE_ATTENDANCE -> executeImportAttendances(companyId, userId, preview.getPreviewRows(), result);
                case TYPE_CONTRACT -> executeImportContracts(companyId, userId, preview.getPreviewRows(), result);
                default -> throw new IllegalArgumentException("Hệ thống không hỗ trợ nhập dữ liệu tự động cho loại: " + type + ". Quản trị viên vui lòng tự thiết lập thủ công.");
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Lỗi xử lý cơ sở dữ liệu: " + e.getMessage());
        }

        return result;
    }

    // =========================================================================
    // SPECIFIC IMPORT EXECUTION IMPLEMENTATIONS
    // =========================================================================

    private void executeImportEmployees(int companyId, List<ImportRowResult> rows, ImportExecuteResult result) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String checkSql = "SELECT EmployeeId FROM Employees WHERE CompanyId = ? AND UPPER(EmployeeCode) = UPPER(?)";
                String insertSql = """
                        INSERT INTO Employees (
                            CompanyId, EmployeeCode, FullName, Email, Phone, Status,
                            DateOfBirth, Gender, IdentityNumber, IdentityIssuedDate,
                            IdentityIssuedPlace, PermanentAddress, Hometown, Major, CreatedAt
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATETIME())
                        """;
                String updateSql = """
                        UPDATE Employees SET
                            FullName = ?, Email = ?, Phone = ?, Status = ?,
                            DateOfBirth = ?, Gender = ?, IdentityNumber = ?, IdentityIssuedDate = ?,
                            IdentityIssuedPlace = ?, PermanentAddress = ?, Hometown = ?, Major = ?,
                            UpdatedAt = SYSDATETIME()
                        WHERE EmployeeId = ? AND CompanyId = ?
                        """;

                try (PreparedStatement psCheck = conn.prepareStatement(checkSql);
                     PreparedStatement psInsert = conn.prepareStatement(insertSql);
                     PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {

                    int inserted = 0;
                    int updated = 0;

                    for (ImportRowResult r : rows) {
                        Map<String, String> d = r.getData();
                        String code = d.get("Mã NV (*)");
                        String name = d.get("Họ tên (*)");
                        String email = d.get("Email (*)");
                        String phone = d.get("Điện thoại (*)");
                        String status = "Chưa hoạt động".equalsIgnoreCase(d.get("Trạng thái (*)")) ? "Inactive" : "Active";
                        Date dob = parseSqlDate(d.get("Ngày sinh (*)"));
                        String gender = normalizeGender(d.get("Giới tính (*)"));
                        String idNum = d.get("Số CCCD/CMND (*)");
                        Date idDate = parseSqlDate(d.get("Ngày cấp CCCD"));
                        String idPlace = d.get("Nơi cấp");
                        String address = d.get("Địa chỉ thường trú");
                        String hometown = d.get("Quê quán");
                        String major = d.get("Ngành học");

                        psCheck.setInt(1, companyId);
                        psCheck.setString(2, code.trim());
                        Integer existingId = null;
                        try (ResultSet rs = psCheck.executeQuery()) {
                            if (rs.next()) existingId = rs.getInt("EmployeeId");
                        }

                        if (existingId != null) {
                            psUpdate.setString(1, name.trim());
                            psUpdate.setString(2, email != null ? email.trim() : null);
                            psUpdate.setString(3, phone != null ? phone.trim() : null);
                            psUpdate.setString(4, status);
                            setNullableDate(psUpdate, 5, dob);
                            psUpdate.setString(6, gender);
                            psUpdate.setString(7, idNum != null ? idNum.trim() : null);
                            setNullableDate(psUpdate, 8, idDate);
                            psUpdate.setString(9, idPlace != null ? idPlace.trim() : null);
                            psUpdate.setString(10, address != null ? address.trim() : null);
                            psUpdate.setString(11, hometown != null ? hometown.trim() : null);
                            psUpdate.setString(12, major != null ? major.trim() : null);
                            psUpdate.setInt(13, existingId);
                            psUpdate.setInt(14, companyId);
                            psUpdate.executeUpdate();
                            updated++;
                        } else {
                            psInsert.setInt(1, companyId);
                            psInsert.setString(2, code.trim());
                            psInsert.setString(3, name.trim());
                            psInsert.setString(4, email != null ? email.trim() : null);
                            psInsert.setString(5, phone != null ? phone.trim() : null);
                            psInsert.setString(6, status);
                            setNullableDate(psInsert, 7, dob);
                            psInsert.setString(8, gender);
                            psInsert.setString(9, idNum != null ? idNum.trim() : null);
                            setNullableDate(psInsert, 10, idDate);
                            psInsert.setString(11, idPlace != null ? idPlace.trim() : null);
                            psInsert.setString(12, address != null ? address.trim() : null);
                            psInsert.setString(13, hometown != null ? hometown.trim() : null);
                            psInsert.setString(14, major != null ? major.trim() : null);
                            psInsert.executeUpdate();
                            inserted++;
                        }
                    }

                    conn.commit();
                    result.setSuccess(true);
                    result.setTotalProcessed(rows.size());
                    result.setInsertedCount(inserted);
                    result.setUpdatedCount(updated);
                    result.setMessage("Nhập hồ sơ nhân sự thành công: Thêm mới " + inserted + ", Cập nhật " + updated + " nhân viên.");
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void executeImportAttendances(int companyId, Integer userId, List<ImportRowResult> rows, ImportExecuteResult result) throws SQLException {
        CompanyShiftSetting setting = new CompanyShiftSettingDAO().getShiftSetting(companyId);
        int lunchBreak = setting != null ? setting.getLunchBreakMinutes() : 60;

        int processed = 0;
        int failed = 0;

        for (ImportRowResult r : rows) {
            Map<String, String> d = r.getData();
            String code = d.get("Mã NV (*)");
            String dateStr = d.get("Ngày chấm công (yyyy-MM-dd) (*)");
            String inStr = d.get("Giờ vào (HH:mm)");
            String outStr = d.get("Giờ ra (HH:mm)");
            String note = d.get("Ghi chú");

            LocalDate date = parseDateFlexible(dateStr);
            LocalTime inTime = parseTimeFlexible(inStr);
            LocalTime outTime = parseTimeFlexible(outStr);

            Integer empId = findEmployeeIdByCode(companyId, code);
            if (empId == null || date == null) {
                failed++;
                continue;
            }

            BigDecimal totalHours = BigDecimal.ZERO;
            if (inTime != null && outTime != null && outTime.isAfter(inTime)) {
                long minutes = Duration.between(inTime, outTime).toMinutes();
                if (minutes >= 300) {
                    minutes = Math.max(0, minutes - lunchBreak);
                }
                totalHours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            }

            String status = determineAttendanceStatus(inTime, outTime, setting);
            attendanceDAO.upsertManualAdjustment(companyId, empId, date, inTime, outTime, totalHours, status,
                    note != null ? note.trim() : "Import từ Excel", userId);
            processed++;
        }

        result.setSuccess(true);
        result.setTotalProcessed(processed);
        result.setInsertedCount(processed);
        result.setFailedCount(failed);
        result.setMessage("Nhập dữ liệu chấm công thành công cho " + processed + " lượt điểm danh.");
    }

    private void executeImportContracts(int companyId, Integer userId, List<ImportRowResult> rows, ImportExecuteResult result) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String checkSql = "SELECT ContractId FROM EmploymentContracts WHERE CompanyId = ? AND UPPER(ContractCode) = UPPER(?)";
                String insertSql = """
                        INSERT INTO EmploymentContracts (
                            CompanyId, EmployeeId, ContractCode, ContractType, StartDate, EndDate,
                            GrossSalary, TravelAllowance, Status, SignedDate, Notes, CreatedByUserId, CreatedAt
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATETIME())
                        """;
                String updateSql = """
                        UPDATE EmploymentContracts SET
                            EmployeeId = ?, ContractType = ?, StartDate = ?, EndDate = ?,
                            GrossSalary = ?, TravelAllowance = ?, Status = ?, SignedDate = ?,
                            Notes = ?, UpdatedAt = SYSDATETIME()
                        WHERE CompanyId = ? AND ContractId = ?
                        """;

                try (PreparedStatement psCheck = conn.prepareStatement(checkSql);
                     PreparedStatement psInsert = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {

                    int inserted = 0;
                    int updated = 0;

                    for (ImportRowResult r : rows) {
                        Map<String, String> d = r.getData();
                        String contractCode = d.get("Mã HĐ (*)");
                        String empCode = d.get("Mã NV (*)");
                        String rawType = d.get("Loại hợp đồng (*)");
                        String startStr = d.get("Ngày bắt đầu (dd/MM/yyyy) (*)");
                        String endStr = d.get("Ngày kết thúc (dd/MM/yyyy)");
                        String grossStr = d.get("Lương trước khấu trừ (VND) (*)");
                        String travelStr = d.get("Phụ cấp đi lại (VND)");
                        String statusStr = d.get("Trạng thái (ACTIVE/DRAFT)");
                        String signedStr = d.get("Ngày ký kết (dd/MM/yyyy)");
                        String notes = d.get("Ghi chú");

                        Integer empId = findEmployeeIdByCode(conn, companyId, empCode);
                        if (empId == null) continue;

                        String contractType = normalizeContractType(rawType);
                        Date startDate = parseSqlDate(startStr);
                        Date endDate = parseSqlDate(endStr);
                        BigDecimal grossSalary = parseBigDecimalSafe(grossStr);
                        BigDecimal travelAllowance = parseBigDecimalSafe(travelStr);
                        String status = normalizeContractStatus(statusStr, endDate);
                        Date signedDate = parseSqlDate(signedStr);
                        if (signedDate == null && "ACTIVE".equals(status)) {
                            signedDate = startDate;
                        }

                        psCheck.setInt(1, companyId);
                        psCheck.setString(2, contractCode.trim());
                        Long existingId = null;
                        try (ResultSet rs = psCheck.executeQuery()) {
                            if (rs.next()) existingId = rs.getLong("ContractId");
                        }

                        long finalContractId = 0L;
                        if (existingId != null) {
                            psUpdate.setInt(1, empId);
                            psUpdate.setString(2, contractType);
                            setNullableDate(psUpdate, 3, startDate);
                            setNullableDate(psUpdate, 4, endDate);
                            psUpdate.setBigDecimal(5, grossSalary);
                            psUpdate.setBigDecimal(6, travelAllowance);
                            psUpdate.setString(7, status);
                            setNullableDate(psUpdate, 8, signedDate);
                            psUpdate.setString(9, notes != null ? notes.trim() : null);
                            psUpdate.setInt(10, companyId);
                            psUpdate.setLong(11, existingId);
                            psUpdate.executeUpdate();
                            finalContractId = existingId;
                            updated++;
                        } else {
                            psInsert.setInt(1, companyId);
                            psInsert.setInt(2, empId);
                            psInsert.setString(3, contractCode.trim());
                            psInsert.setString(4, contractType);
                            setNullableDate(psInsert, 5, startDate);
                            setNullableDate(psInsert, 6, endDate);
                            psInsert.setBigDecimal(7, grossSalary);
                            psInsert.setBigDecimal(8, travelAllowance);
                            psInsert.setString(9, status);
                            setNullableDate(psInsert, 10, signedDate);
                            psInsert.setString(11, notes != null ? notes.trim() : null);
                            setNullableInt(psInsert, 12, userId);
                            psInsert.executeUpdate();
                            try (ResultSet gk = psInsert.getGeneratedKeys()) {
                                if (gk.next()) finalContractId = gk.getLong(1);
                            }
                            inserted++;
                        }

                        if ("ACTIVE".equals(status) && grossSalary != null && grossSalary.signum() >= 0
                                && startDate != null && finalContractId > 0) {
                            try {
                                new com.hrms.dao.ContractCompensationDAO().synchronize(conn, companyId, empId,
                                        finalContractId, contractCode.trim(), startDate.toLocalDate(),
                                        grossSalary, travelAllowance, userId);
                            } catch (Exception ex) {
                                // Non-blocking: contract saved even if compensation sync encounters constraints
                            }
                        }
                    }

                    conn.commit();
                    result.setSuccess(true);
                    result.setTotalProcessed(rows.size());
                    result.setInsertedCount(inserted);
                    result.setUpdatedCount(updated);
                    result.setMessage("Nhập hợp đồng lao động thành công: Thêm mới " + inserted + ", Cập nhật " + updated + " hợp đồng.");
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // =========================================================================
    // VALIDATION HELPERS
    // =========================================================================

    private String validateRowData(String type, int companyId, Map<String, String> data, Set<String> seenCodes, int lineNum) {
        switch (type) {
            case TYPE_EMPLOYEE -> {
                String code = data.get("Mã NV (*)");
                String name = data.get("Họ tên (*)");
                String email = data.get("Email (*)");
                String phone = data.get("Điện thoại (*)");
                String idNum = data.get("Số CCCD/CMND (*)");
                String dob = data.get("Ngày sinh (*)");

                if (isBlank(code)) return "Mã nhân viên không được để trống.";
                if (isBlank(name)) return "Họ tên không được để trống.";
                if (isBlank(email) || !email.contains("@")) return "Email không hợp lệ.";
                if (isBlank(phone) || phone.trim().length() < 9) return "Số điện thoại không hợp lệ (tối thiểu 9 số).";
                if (isBlank(idNum)) return "Số CCCD/CMND không được để trống.";

                if (isBlank(dob) || parseSqlDate(dob) == null) {
                    return "Ngày sinh không được để trống và phải đúng định dạng (dd/MM/yyyy).";
                }

                if (!seenCodes.add("EMP_CODE:" + code.trim().toUpperCase())) {
                    return "Mã nhân viên '" + code.trim() + "' bị trùng lặp trong tệp Excel.";
                }
                if (!seenCodes.add("EMP_EMAIL:" + email.trim().toLowerCase())) {
                    return "Email '" + email.trim() + "' bị trùng lặp trong tệp Excel.";
                }
            }
            case TYPE_ATTENDANCE -> {
                String code = data.get("Mã NV (*)");
                String dateStr = data.get("Ngày chấm công (yyyy-MM-dd) (*)");
                String inStr = data.get("Giờ vào (HH:mm)");
                String outStr = data.get("Giờ ra (HH:mm)");

                if (isBlank(code)) return "Mã nhân viên không được để trống.";
                if (isBlank(dateStr)) return "Ngày chấm công không được để trống.";

                LocalDate date = parseDateFlexible(dateStr);
                if (date == null) return "Ngày chấm công không đúng định dạng (yyyy-MM-dd hoặc dd/MM/yyyy).";

                try {
                    if (attendanceService.isMonthLocked(companyId, date.getMonthValue(), date.getYear())) {
                        return "Bảng chấm công tháng " + date.getMonthValue() + "/" + date.getYear() + " đã bị khóa chốt lương.";
                    }
                } catch (SQLException ignored) {}

                if (!isBlank(inStr) && parseTimeFlexible(inStr) == null) {
                    return "Giờ vào không đúng định dạng (HH:mm).";
                }
                if (!isBlank(outStr) && parseTimeFlexible(outStr) == null) {
                    return "Giờ ra không đúng định dạng (HH:mm).";
                }
            }
            case TYPE_CONTRACT -> {
                String contractCode = data.get("Mã HĐ (*)");
                String empCode = data.get("Mã NV (*)");
                String rawType = data.get("Loại hợp đồng (*)");
                String startStr = data.get("Ngày bắt đầu (dd/MM/yyyy) (*)");
                String endStr = data.get("Ngày kết thúc (dd/MM/yyyy)");
                String grossStr = data.get("Lương trước khấu trừ (VND) (*)");
                String travelStr = data.get("Phụ cấp đi lại (VND)");
                String signedStr = data.get("Ngày ký kết (dd/MM/yyyy)");
                String notes = data.get("Ghi chú");

                if (isBlank(contractCode)) return "Mã hợp đồng không được để trống.";
                if (contractCode.trim().length() > 60) return "Mã hợp đồng không được vượt quá 60 ký tự.";
                if (!seenCodes.add("CONTRACT:" + contractCode.trim().toUpperCase())) {
                    return "Mã hợp đồng '" + contractCode.trim() + "' bị trùng lặp trong tệp Excel.";
                }

                if (isBlank(empCode)) return "Mã nhân viên không được để trống.";
                Integer empId = findEmployeeIdByCode(companyId, empCode.trim());
                if (empId == null) {
                    return "Không tìm thấy nhân viên có mã '" + empCode.trim() + "' trong công ty.";
                }

                if (isBlank(rawType)) return "Loại hợp đồng không được để trống.";
                String contractType = normalizeContractType(rawType);
                if (!Set.of("PROBATION", "FIXED_TERM", "INDEFINITE").contains(contractType)) {
                    return "Loại hợp đồng không hợp lệ (hỗ trợ: Thử việc, Có thời hạn, Không xác định thời hạn).";
                }

                if (isBlank(startStr)) return "Ngày bắt đầu hợp đồng không được để trống.";
                LocalDate startDate = parseDateFlexible(startStr);
                if (startDate == null) return "Ngày bắt đầu không đúng định dạng (dd/MM/yyyy).";

                LocalDate endDate = parseDateFlexible(endStr);
                boolean hasEndStr = !isBlank(endStr) && !"không xác định".equalsIgnoreCase(endStr.trim()) && !"khong xac dinh".equalsIgnoreCase(endStr.trim());
                if (!"INDEFINITE".equals(contractType) && !hasEndStr) {
                    return "Hợp đồng '" + rawType.trim() + "' bắt buộc phải có Ngày kết thúc.";
                }
                if (hasEndStr && endDate == null) {
                    return "Ngày kết thúc không đúng định dạng (dd/MM/yyyy).";
                }
                if (endDate != null && endDate.isBefore(startDate)) {
                    return "Ngày kết thúc không được trước ngày bắt đầu.";
                }

                if (isBlank(grossStr)) return "Lương trước khấu trừ không được để trống.";
                try {
                    BigDecimal gross = parseBigDecimalSafe(grossStr);
                    if (gross.signum() < 0) return "Lương trước khấu trừ không được âm.";
                } catch (Exception e) {
                    return "Lương trước khấu trừ không đúng định dạng số.";
                }

                if (!isBlank(travelStr)) {
                    try {
                        BigDecimal travel = parseBigDecimalSafe(travelStr);
                        if (travel.signum() < 0) return "Phụ cấp đi lại không được âm.";
                    } catch (Exception e) {
                        return "Phụ cấp đi lại không đúng định dạng số.";
                    }
                }

                if (!isBlank(signedStr) && parseDateFlexible(signedStr) == null) {
                    return "Ngày ký kết không đúng định dạng (dd/MM/yyyy).";
                }

                if (notes != null && notes.length() > 1000) {
                    return "Ghi chú không được vượt quá 1000 ký tự.";
                }
            }
        }
        return null;
    }

    // =========================================================================
    // METADATA & CELL UTILITIES
    // =========================================================================

    public static String normalizeType(String type) {
        if (type == null) return TYPE_EMPLOYEE;
        String t = type.trim().toUpperCase();
        return switch (t) {
            case "DEPT", "DEPARTMENT", "PHONG_BAN" ->
                    throw new IllegalArgumentException("Hệ thống không hỗ trợ nhập file cho Phòng ban. Quản trị viên vui lòng tự thiết lập thủ công.");
            case "POS", "POSITION", "CHUC_DANH" ->
                    throw new IllegalArgumentException("Hệ thống không hỗ trợ nhập file cho Chức danh. Quản trị viên vui lòng tự thiết lập thủ công.");
            case "RESPONSIBILITY", "LEVEL", "CAP_TRACH_NHIEM" ->
                    throw new IllegalArgumentException("Hệ thống không hỗ trợ nhập file cho Cấp trách nhiệm. Quản trị viên vui lòng tự thiết lập thủ công.");
            case "ATTENDANCE", "CHAM_CONG", "TIMESHEET" -> TYPE_ATTENDANCE;
            case "CONTRACT", "CONTRACTS", "HOP_DONG", "HDLD" -> TYPE_CONTRACT;
            default -> TYPE_EMPLOYEE;
        };
    }

    public static String getTypeNameVi(String type) {
        return switch (type) {
            case TYPE_ATTENDANCE -> "Chấm công";
            case TYPE_CONTRACT -> "Hợp đồng lao động";
            default -> "Nhân sự";
        };
    }

    private String[] getHeadersForType(String type) {
        return switch (type) {
            case TYPE_ATTENDANCE -> new String[]{
                    "STT", "Mã NV (*)", "Họ và tên", "Phòng ban",
                    "Ngày chấm công (yyyy-MM-dd) (*)", "Giờ vào (HH:mm)", "Giờ ra (HH:mm)",
                    "Tổng giờ", "Trạng thái", "Ghi chú"
            };
            case TYPE_CONTRACT -> new String[]{
                    "STT", "Mã HĐ (*)", "Mã NV (*)", "Họ và tên", "Loại hợp đồng (*)", "Ngày bắt đầu (dd/MM/yyyy) (*)",
                    "Ngày kết thúc (dd/MM/yyyy)", "Lương trước khấu trừ (VND) (*)", "Phụ cấp đi lại (VND)",
                    "Trạng thái (ACTIVE/DRAFT)", "Ngày ký kết (dd/MM/yyyy)", "Ghi chú"
            };
            default -> new String[]{
                    "Mã NV (*)", "Họ tên (*)", "Email (*)", "Điện thoại (*)",
                    "Trạng thái (*)", "Ngày sinh (*)", "Giới tính (*)", "Số CCCD/CMND (*)",
                    "Ngày cấp CCCD", "Nơi cấp", "Địa chỉ thường trú", "Quê quán", "Ngành học"
            };
        };
    }

    private String[] getSampleDataForType(String type) {
        return switch (type) {
            case TYPE_ATTENDANCE -> new String[]{
                    "1", "NV001", "Nguyễn Văn A", "Phòng Kỹ thuật", LocalDate.now().toString(), "08:25", "17:35", "8.00", "Đúng giờ", "Điểm danh đúng giờ"
            };
            case TYPE_CONTRACT -> new String[]{
                    "1", "HDLD-001", "NV001", "Nguyễn Văn A", "Có thời hạn", "01/01/2026",
                    "31/12/2026", "15000000", "500000",
                    "Có hiệu lực", "01/01/2026", "Hợp đồng lao động 12 tháng"
            };
            default -> new String[]{
                    "NV001", "Nguyễn Văn A", "vana@company.com", "0912345678",
                    "Đang làm việc", "15/08/1995", "Nam", "001095012345",
                    "10/05/2021", "Cục CSQLHC về TTXH", "Cầu Giấy, Hà Nội", "Hà Nội", "Công nghệ thông tin"
            };
        };
    }

    public String detectFileType(Sheet sheet, String fallbackType) {
        if (sheet == null) return normalizeType(fallbackType);

        int maxR = Math.min(12, sheet.getLastRowNum());
        for (int r = 0; r <= maxR; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < row.getLastCellNum(); c++) {
                sb.append(" ").append(getCellStringValue(row.getCell(c)).toLowerCase());
            }
            String rowText = sb.toString();

            if (rowText.contains("mã phòng ban") || rowText.contains("tên phòng ban")
                    || rowText.contains("mã chức danh") || rowText.contains("tên chức danh")
                    || rowText.contains("cấp trách nhiệm")) {
                throw new IllegalArgumentException("Hệ thống không hỗ trợ nhập file cho Phòng ban / Chức danh / Cấp trách nhiệm. Quản trị viên vui lòng tự thiết lập thủ công.");
            }
            if (rowText.contains("mã hđ") || rowText.contains("hợp đồng lao động") || rowText.contains("loại hợp đồng")
                    || (rowText.contains("lương trước khấu trừ") && !rowText.contains("bảng lương"))) {
                return TYPE_CONTRACT;
            }
            if (rowText.contains("ngày chấm công") || (rowText.contains("giờ vào") && rowText.contains("giờ ra"))) {
                return TYPE_ATTENDANCE;
            }
            if (rowText.contains("số cccd") || rowText.contains("ngày cấp cccd") || rowText.contains("quê quán") || rowText.contains("ngành học")) {
                return TYPE_EMPLOYEE;
            }
        }

        return normalizeType(fallbackType);
    }

    private int findHeaderRowIndex(Sheet sheet, String type) {
        String[] keywords = switch (type) {
            case TYPE_CONTRACT -> new String[]{"mã hđ", "loại hợp đồng"};
            case TYPE_ATTENDANCE -> new String[]{"ngày chấm công", "giờ vào"};
            default -> new String[]{"mã nv", "họ tên", "số cccd"};
        };

        for (int r = 0; r <= Math.min(10, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row != null && row.getLastCellNum() >= 3) {
                int nonEmpty = 0;
                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String val = getCellStringValue(row.getCell(c)).toLowerCase();
                    if (!val.isBlank()) {
                        nonEmpty++;
                        sb.append(" ").append(val);
                    }
                }
                if (nonEmpty >= 3) {
                    String rowText = sb.toString();
                    for (String kw : keywords) {
                        if (rowText.contains(kw)) {
                            return r;
                        }
                    }
                }
            }
        }
        return -1;
    }

    private boolean isSampleRow(String type, Row row) {
        if (row == null) return false;
        for (int c = 0; c < Math.min(6, row.getLastCellNum()); c++) {
            String val = getCellStringValue(row.getCell(c));
            if ("HDLD-001".equalsIgnoreCase(val) || "DEV-01".equalsIgnoreCase(val)
                    || ("Nguyễn Văn A".equalsIgnoreCase(val) && c > 0 && "NV001".equalsIgnoreCase(getCellStringValue(row.getCell(c - 1))))) {
                return true;
            }
        }
        return false;
    }

    private boolean isRowEmpty(Row row, int maxCols) {
        if (row == null) return true;
        int limit = Math.max(maxCols, (int) row.getLastCellNum());
        for (int c = 0; c < limit; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !getCellStringValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            try {
                java.time.LocalDateTime ldt = cell.getLocalDateTimeCellValue();
                if (ldt.getYear() <= 1900) {
                    return ldt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                } else {
                    return ldt.toLocalDate().format(DATE_FMT_VN_STANDARD);
                }
            } catch (Exception ignored) {}
        }
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String normalizeGender(String g) {
        if (isBlank(g)) return "Other";
        String lower = g.trim().toLowerCase();
        if (lower.contains("nam") || lower.equals("male") || lower.equals("m")) return "Male";
        if (lower.contains("nữ") || lower.contains("nu") || lower.equals("female") || lower.equals("f")) return "Female";
        return "Other";
    }

    private LocalDate parseDateFlexible(String str) {
        if (isBlank(str)) return null;
        String s = str.trim();
        if ("Không xác định".equalsIgnoreCase(s) || "Khong xac dinh".equalsIgnoreCase(s)
                || "-".equals(s) || "null".equalsIgnoreCase(s) || "vô thời hạn".equalsIgnoreCase(s)
                || "vo thoi han".equalsIgnoreCase(s)) {
            return null;
        }

        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(s, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private LocalTime parseTimeFlexible(String str) {
        if (isBlank(str)) return null;
        String s = str.trim();
        if (s.length() == 5) {
            try {
                return LocalTime.parse(s, TIME_FMT);
            } catch (DateTimeParseException ignored) {}
        } else if (s.length() >= 8) {
            try {
                return LocalTime.parse(s.substring(0, 5), TIME_FMT);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private Date parseSqlDate(String str) {
        LocalDate ld = parseDateFlexible(str);
        return ld != null ? Date.valueOf(ld) : null;
    }

    private void setNullableInt(PreparedStatement ps, int idx, Integer val) throws SQLException {
        if (val != null && val > 0) ps.setInt(idx, val);
        else ps.setNull(idx, Types.INTEGER);
    }

    private void setNullableDate(PreparedStatement ps, int idx, Date val) throws SQLException {
        if (val != null) ps.setDate(idx, val);
        else ps.setNull(idx, Types.DATE);
    }

    private Integer findEmployeeIdByCode(int companyId, String employeeCode) {
        if (isBlank(employeeCode)) return null;
        try (Connection conn = DBConnection.getConnection()) {
            return findEmployeeIdByCode(conn, companyId, employeeCode);
        } catch (SQLException ignored) {}
        return null;
    }

    private Integer findEmployeeIdByCode(Connection conn, int companyId, String employeeCode) {
        if (isBlank(employeeCode)) return null;
        String sql = "SELECT EmployeeId FROM Employees WHERE CompanyId = ? AND UPPER(EmployeeCode) = UPPER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            ps.setString(2, employeeCode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("EmployeeId");
            }
        } catch (SQLException ignored) {}
        return null;
    }

    public String normalizeContractType(String raw) {
        if (isBlank(raw)) return "FIXED_TERM";
        String s = raw.trim().toUpperCase();
        if (s.contains("THỬ VIỆC") || s.contains("THU VIEC") || s.equals("PROBATION")) {
            return "PROBATION";
        }
        if (s.contains("KHÔNG XÁC ĐỊNH") || s.contains("KHONG XAC DINH") || s.equals("INDEFINITE") || s.contains("VÔ THỜI HẠN") || s.contains("VO THOI HAN")) {
            return "INDEFINITE";
        }
        if (s.contains("CÓ THỜI HẠN") || s.contains("CO THOI HAN") || s.equals("FIXED_TERM") || s.equals("DEFINITE")) {
            return "FIXED_TERM";
        }
        return s;
    }

    public String normalizeContractStatus(String statusStr, Date endDate) {
        if (isBlank(statusStr)) {
            if (endDate != null && endDate.before(new java.util.Date())) {
                return "EXPIRED";
            }
            return "ACTIVE";
        }
        String s = statusStr.trim().toUpperCase();
        if (s.contains("NHÁP") || s.contains("NHAP") || s.equals("DRAFT")) return "DRAFT";
        if (s.contains("HẾT HẠN") || s.contains("HET HAN") || s.equals("EXPIRED")) return "EXPIRED";
        if (s.contains("HỦY") || s.contains("HUY") || s.equals("CANCELLED") || s.equals("CANCELED")) return "CANCELLED";
        if (s.contains("CHẤM DỨT") || s.contains("CHAM DUT") || s.equals("TERMINATED")) return "TERMINATED";
        if (s.contains("HIỆU LỰC") || s.contains("HIEU LUC") || s.equals("ACTIVE")) {
            if (endDate != null && endDate.before(new java.util.Date())) {
                return "EXPIRED";
            }
            return "ACTIVE";
        }
        return "ACTIVE";
    }

    public BigDecimal parseBigDecimalSafe(String val) {
        if (isBlank(val)) return BigDecimal.ZERO;
        try {
            String clean = val.trim().replace("₫", "").replace("đ", "").replace("Đ", "").replace("VND", "").replace("vnd", "").trim();
            if (clean.contains(",") && !clean.contains(".")) {
                if (clean.matches(".*,\\d{3}(,\\d{3})*")) {
                    clean = clean.replace(",", "");
                } else {
                    clean = clean.replace(",", ".");
                }
            } else if (clean.contains(".") && !clean.contains(",")) {
                if (clean.matches(".*\\.\\d{3}(\\.\\d{3})*")) {
                    clean = clean.replace(".", "");
                }
            } else if (clean.contains(",") && clean.contains(".")) {
                if (clean.lastIndexOf('.') > clean.lastIndexOf(',')) {
                    clean = clean.replace(",", "");
                } else {
                    clean = clean.replace(".", "").replace(",", ".");
                }
            }
            clean = clean.replaceAll("\\s+", "");
            return new BigDecimal(clean);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String determineAttendanceStatus(LocalTime in, LocalTime out, CompanyShiftSetting setting) {
        if (in == null && out == null) return "Absent";
        LocalTime shiftStart = setting != null ? setting.getShiftStartTime() : LocalTime.of(8, 30);
        int grace = setting != null ? setting.getGracePeriodMinutes() : 0;
        LocalTime lateLimit = shiftStart.plusMinutes(grace);

        if (in != null && in.isAfter(lateLimit)) return "Late";
        return "Present";
    }

    // =========================================================================
    // STYLES
    // =========================================================================

    private CellStyle createTitleStyle(Workbook wb, IndexedColors color) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        f.setColor(color.getIndex());
        s.setFont(f);
        return s;
    }

    private CellStyle createSubtitleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setItalic(true);
        f.setFontHeightInPoints((short) 9);
        f.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        s.setFont(f);
        return s;
    }

    private CellStyle createHeaderStyle(Workbook wb, IndexedColors bg) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(bg.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(s);
        return s;
    }

    private CellStyle createSampleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setItalic(true);
        f.setFontHeightInPoints((short) 9);
        f.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(s);
        return s;
    }

    private void setBorders(CellStyle s) {
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    private void autoSizeColumns(Sheet sheet, int numCols) {
        for (int i = 0; i < numCols; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            if (width < 3400) sheet.setColumnWidth(i, 3400);
            if (width > 12000) sheet.setColumnWidth(i, 12000);
        }
    }
}

