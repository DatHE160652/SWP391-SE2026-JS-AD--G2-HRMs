package com.hrms.service;

import com.hrms.model.PayrollAdjustmentInput;
import com.hrms.model.PayrollRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.*;

/** Strict four-column import: formulas are rejected so imported amounts are explicit. */
public class PayrollExcelService {
    private static final List<String> HEADERS=List.of("EmployeeCode","Bonus","Deduction","Note");

    public List<PayrollAdjustmentInput> read(InputStream input) throws IOException {
        List<String> errors=new ArrayList<>();
        List<PayrollAdjustmentInput> result=new ArrayList<>();
        Set<String> codes=new HashSet<>();
        try(Workbook workbook=WorkbookFactory.create(input)) {
            if(workbook.getNumberOfSheets()!=1) throw new PayrollException(400,"File cần có đúng một sheet dữ liệu.");
            Sheet sheet=workbook.getSheetAt(0);
            if(sheet.getLastRowNum()>5000) throw new PayrollException(400,"File tối đa 5.000 dòng dữ liệu.");
            Row header=sheet.getRow(0);
            for(int col=0;col<4;col++) {
                if(header==null || !HEADERS.get(col).equals(text(header.getCell(col))))
                    throw new PayrollException(400,"Tiêu đề phải là: EmployeeCode, Bonus, Deduction, Note. Hãy dùng file mẫu.");
            }
            for(int i=1;i<=sheet.getLastRowNum();i++) {
                Row row=sheet.getRow(i);
                if(row==null || blank(row)) continue;
                try {
                    String code=text(row.getCell(0));
                    if(code.isBlank() || code.length()>50) throw new PayrollException(400,"Mã nhân viên bị trống hoặc quá dài.");
                    if(!codes.add(code.toUpperCase(Locale.ROOT))) throw new PayrollException(400,"Mã nhân viên bị trùng trong file.");
                    BigDecimal bonus=amount(row.getCell(1),"Thưởng");
                    BigDecimal deduction=amount(row.getCell(2),"Khấu trừ");
                    String note=PayrollService.note(text(row.getCell(3)));
                    if(row.getLastCellNum()>4) {
                        for(int col=4;col<row.getLastCellNum();col++)
                            if(!text(row.getCell(col)).isBlank()) throw new PayrollException(400,"Có dữ liệu ngoài bốn cột của mẫu.");
                    }
                    result.add(new PayrollAdjustmentInput(i+1,code,bonus,deduction,note));
                } catch(PayrollException e) { errors.add("Dòng "+(i+1)+": "+e.getMessage()); }
            }
        } catch(PayrollException e) { throw e; }
        catch(RuntimeException e) { throw new PayrollException(400,"Không đọc được file Excel. Hãy tải lại file mẫu .xlsx hoặc .xls."); }
        if(!errors.isEmpty()) throw new PayrollException(400,"Chưa nhập dữ liệu. Hãy sửa các dòng lỗi.",errors);
        if(result.isEmpty()) throw new PayrollException(400,"File chưa có dòng dữ liệu.");
        return result;
    }

    private boolean blank(Row row) {
        for(Cell c:row) if(c.getCellType()!=CellType.BLANK && !new DataFormatter(Locale.ROOT).formatCellValue(c).isBlank()) return false;
        return true;
    }
    private String text(Cell c) {
        if(c==null || c.getCellType()==CellType.BLANK) return "";
        if(c.getCellType()==CellType.FORMULA) throw new PayrollException(400,"Không dùng công thức Excel; hãy dán giá trị trước khi nhập.");
        if(c.getCellType()==CellType.ERROR || c.getCellType()==CellType.BOOLEAN)
            throw new PayrollException(400,"Ô dữ liệu không hợp lệ.");
        return new DataFormatter(Locale.ROOT).formatCellValue(c).trim();
    }
    private BigDecimal amount(Cell cell,String label) {
        if(cell==null || cell.getCellType()==CellType.BLANK) return new BigDecimal("0.00");
        if(cell.getCellType()==CellType.NUMERIC) {
            if(DateUtil.isCellDateFormatted(cell)) throw new PayrollException(400,label+" không được là ngày tháng.");
            return PayrollService.money(BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString(),label);
        }
        String raw=text(cell);
        return PayrollService.money(raw.isBlank()?"0":raw,label);
    }

    public void template(List<PayrollRow> employees,OutputStream output) throws IOException {
        try(Workbook workbook=new XSSFWorkbook()) {
            Sheet sheet=workbook.createSheet("Payroll Adjustments");
            CellStyle heading=workbook.createCellStyle();
            heading.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); heading.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font=workbook.createFont(); font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex()); heading.setFont(font);
            CellStyle money=workbook.createCellStyle(); money.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
            Row header=sheet.createRow(0);
            for(int i=0;i<4;i++) { Cell cell=header.createCell(i);cell.setCellValue(HEADERS.get(i));cell.setCellStyle(heading); }
            for(int i=0;i<employees.size();i++) {
                Row row=sheet.createRow(i+1);row.createCell(0).setCellValue(employees.get(i).employeeCode());
                for(int col=1;col<3;col++) { Cell cell=row.createCell(col);cell.setCellValue(0);cell.setCellStyle(money); }
                row.createCell(3).setCellValue("");
            }
            sheet.createFreezePane(0,1); sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,Math.max(0,employees.size()),0,3));
            sheet.setColumnWidth(0,24*256);sheet.setColumnWidth(1,22*256);sheet.setColumnWidth(2,22*256);sheet.setColumnWidth(3,55*256);
            workbook.write(output);
        }
    }
}
