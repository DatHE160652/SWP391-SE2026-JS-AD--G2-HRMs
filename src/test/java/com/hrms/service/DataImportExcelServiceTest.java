package com.hrms.service;

import com.hrms.model.ImportPreviewResult;
import com.hrms.service.importdata.DataImportExcelService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Unit tests for DataImportExcelService template generation and preview validation.
 */
public class DataImportExcelServiceTest {

    private DataImportExcelService importService;

    @Before
    public void setUp() {
        importService = new DataImportExcelService();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectGenerateTemplateDepartment() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        importService.generateTemplate("DEPARTMENT", "NovaService Test", os);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectGenerateTemplatePosition() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        importService.generateTemplate("POSITION", "NovaService Test", os);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectGenerateTemplateResponsibilityLevel() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        importService.generateTemplate("LEVEL", "NovaService Test", os);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectPreviewDepartmentExcel() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Mã phòng ban (*)");
            header.createCell(1).setCellValue("Tên phòng ban (*)");
            header.createCell(2).setCellValue("Mô tả");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("MKT");
            r1.createCell(1).setCellValue("Phòng Marketing");
            r1.createCell(2).setCellValue("Quảng bá sản phẩm");

            wb.write(os);
        }
        importService.previewImport("DEPARTMENT", 1, new ByteArrayInputStream(os.toByteArray()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectDetectPositionExcel() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Mã chức danh (*)");
            header.createCell(1).setCellValue("Tên chức danh (*)");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("DEV");
            r1.createCell(1).setCellValue("Lập trình viên");

            wb.write(os);
        }
        // Even if user specifies EMPLOYEE, file content detection will intercept position headers and reject
        importService.previewImport("EMPLOYEE", 1, new ByteArrayInputStream(os.toByteArray()));
    }

    @Test
    public void testGenerateTemplateEmployee() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        importService.generateTemplate("EMPLOYEE", "NovaService Test", os);
        byte[] bytes = os.toByteArray();
        assertTrue(bytes.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertNotNull(sheet);
            Row headerRow = sheet.getRow(3);
            assertNotNull(headerRow);
            assertEquals("Họ tên (*)", headerRow.getCell(0).getStringCellValue());
            assertEquals("Email (*)", headerRow.getCell(1).getStringCellValue());
            assertEquals("Điện thoại (*)", headerRow.getCell(2).getStringCellValue());
            assertEquals("Trạng thái (*)", headerRow.getCell(3).getStringCellValue());
        }
    }

    @Test
    public void testGenerateTemplateAttendance() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        importService.generateTemplate("ATTENDANCE", "NovaService Test", os);
        byte[] bytes = os.toByteArray();
        assertTrue(bytes.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertNotNull(sheet);
            Row headerRow = sheet.getRow(3);
            assertNotNull(headerRow);
            assertEquals("STT", headerRow.getCell(0).getStringCellValue());
            assertEquals("Mã NV (*)", headerRow.getCell(1).getStringCellValue());
            assertEquals("Ngày chấm công (yyyy-MM-dd) (*)", headerRow.getCell(4).getStringCellValue());
        }
    }

    @Test
    public void testGenerateTemplateContract() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        importService.generateTemplate("CONTRACT", "NovaService Test", os);
        byte[] bytes = os.toByteArray();
        assertTrue(bytes.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertNotNull(sheet);
            Row headerRow = sheet.getRow(3);
            assertNotNull(headerRow);
            assertEquals("STT", headerRow.getCell(0).getStringCellValue());
            assertEquals("Mã HĐ (*)", headerRow.getCell(1).getStringCellValue());
            assertEquals("Mã NV (*)", headerRow.getCell(2).getStringCellValue());
            assertEquals("Họ và tên", headerRow.getCell(3).getStringCellValue());
            assertEquals("Loại hợp đồng (*)", headerRow.getCell(4).getStringCellValue());
            assertEquals("Ngày bắt đầu (dd/MM/yyyy) (*)", headerRow.getCell(5).getStringCellValue());
            assertEquals("Lương trước khấu trừ (VND) (*)", headerRow.getCell(7).getStringCellValue());
        }
    }

    @Test
    public void testPreviewWithRealContractFile() throws IOException {
        java.io.File file = new java.io.File("C:\\Users\\Asus\\Downloads\\mau_nhap_contract.xlsx");
        if (file.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                ImportPreviewResult result = importService.previewImport("EMPLOYEE", 1, fis);
                assertNotNull(result);
                assertEquals("CONTRACT", result.getType());
                assertEquals(2, result.getTotalRows());
                assertTrue(result.getColumnHeaders().contains("Mã HĐ (*)"));
                assertTrue(result.getColumnHeaders().contains("Mã NV (*)"));
                assertTrue(result.getColumnHeaders().contains("Loại hợp đồng (*)"));

                var row1 = result.getPreviewRows().get(0).getData();
                assertNotNull(row1.get("Mã HĐ (*)"));
                assertNotNull(row1.get("Mã NV (*)"));
                assertNotNull(row1.get("Loại hợp đồng (*)"));
                assertNotNull(row1.get("Ngày bắt đầu (dd/MM/yyyy) (*)"));

                if (result.getPreviewRows().size() > 1) {
                    var row2 = result.getPreviewRows().get(1).getData();
                    assertNotNull(row2.get("Mã HĐ (*)"));
                    assertNotNull(row2.get("Mã NV (*)"));
                    assertNotNull(row2.get("Loại hợp đồng (*)"));
                    assertNotNull(row2.get("Ngày bắt đầu (dd/MM/yyyy) (*)"));
                }
            }
        }
    }

    @Test
    public void testNormalizeContractType() {
        assertEquals("PROBATION", importService.normalizeContractType("Thử việc"));
        assertEquals("PROBATION", importService.normalizeContractType("probation"));
        assertEquals("FIXED_TERM", importService.normalizeContractType("Có thời hạn"));
        assertEquals("FIXED_TERM", importService.normalizeContractType("FIXED_TERM"));
        assertEquals("INDEFINITE", importService.normalizeContractType("Không xác định thời hạn"));
        assertEquals("INDEFINITE", importService.normalizeContractType("Vô thời hạn"));
    }

    @Test
    public void testParseBigDecimalSafe() {
        assertEquals(new java.math.BigDecimal("15000000"), importService.parseBigDecimalSafe("15,000,000"));
        assertEquals(new java.math.BigDecimal("15000000"), importService.parseBigDecimalSafe("15.000.000"));
        assertEquals(new java.math.BigDecimal("15000000"), importService.parseBigDecimalSafe("15000000"));
        assertEquals(new java.math.BigDecimal("500000"), importService.parseBigDecimalSafe("500.000 ₫"));
        assertEquals(java.math.BigDecimal.ZERO, importService.parseBigDecimalSafe(""));
        assertEquals(java.math.BigDecimal.ZERO, importService.parseBigDecimalSafe(null));
    }

    @Test
    public void testPreviewImportContractValidationErrors() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Mã HĐ (*)");
            header.createCell(1).setCellValue("Mã NV (*)");
            header.createCell(2).setCellValue("Loại hợp đồng (*)");
            header.createCell(3).setCellValue("Ngày bắt đầu (dd/MM/yyyy) (*)");
            header.createCell(4).setCellValue("Ngày kết thúc (dd/MM/yyyy)");
            header.createCell(5).setCellValue("Lương trước khấu trừ (VND) (*)");

            // Empty code
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("");
            r1.createCell(1).setCellValue("NV001");
            r1.createCell(2).setCellValue("Thử việc");
            r1.createCell(3).setCellValue("01/01/2026");
            r1.createCell(4).setCellValue("01/03/2026");
            r1.createCell(5).setCellValue("10000000");

            wb.write(os);
        }

        ImportPreviewResult result = importService.previewImport("CONTRACT", 1, new ByteArrayInputStream(os.toByteArray()));
        assertEquals(1, result.getTotalRows());
        assertEquals(0, result.getValidRows());
        assertEquals(1, result.getErrorRows());
        assertFalse(result.isCanProceed());
        assertTrue(result.getPreviewRows().get(0).getErrorMessage().contains("không được để trống"));
    }
}

