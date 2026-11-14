package com.hrms.service;

import com.hrms.model.PayrollCalculation;
import com.hrms.model.PayrollRow;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Pure calculation shared by preview and persistence validation. */
public final class PayrollCalculator {
    private PayrollCalculator() { }

    public static PayrollCalculation calculate(List<PayrollRow> inputs) {
        if (inputs==null || inputs.isEmpty())
            throw new PayrollException(409,"Kỳ này chưa có nhân viên đủ điều kiện để tính lương.");
        List<String> errors=new ArrayList<>();
        List<PayrollRow> result=new ArrayList<>();
        BigDecimal income=BigDecimal.ZERO.setScale(2),deductions=income,net=income;
        for (PayrollRow row:inputs) {
            String issue=row.salaryIssue();
            if (issue==null && row.baseSalary()==null) issue="Thiếu lịch sử lương có hiệu lực trong kỳ";
            if (issue!=null) {errors.add(row.employeeCode()+": "+issue);continue;}
            try {
                BigDecimal base=PayrollService.money(row.baseSalary().toPlainString(),"Lương theo công");
                BigDecimal contractBase=row.contractBaseSalary()==null?base:PayrollService.money(row.contractBaseSalary().toPlainString(),"Lương cơ bản");
                BigDecimal allowance=row.responsibilityAllowance()==null?BigDecimal.ZERO:PayrollService.money(row.responsibilityAllowance().toPlainString(),"Phụ cấp trách nhiệm");
                BigDecimal travel=PayrollService.money(row.travelAllowance()==null?"0":row.travelAllowance().toPlainString(),"Phụ cấp đi lại");
                BigDecimal bonus=PayrollService.money(row.bonus()==null?"0":row.bonus().toPlainString(),"Thưởng");
                BigDecimal deduction=PayrollService.money(row.deduction()==null?"0":row.deduction().toPlainString(),"Khấu trừ");
                BigDecimal gross=base.add(allowance).add(travel).add(bonus),takeHome=gross;
                income=income.add(gross);net=net.add(takeHome);
                result.add(new PayrollRow(row.employeeId(),row.employeeCode(),row.fullName(),row.departmentName(),row.status(),
                        base,bonus,BigDecimal.ZERO,takeHome,row.note(),null,allowance,null,row.salaryEffectiveFrom(),row.salaryEffectiveTo(),
                        contractBase,row.actualWorkDays(),row.standardWorkDays(),travel));
            } catch (PayrollException invalid) {errors.add(row.employeeCode()+": "+invalid.getMessage());}
        }
        if (!errors.isEmpty()) throw new PayrollException(409,
                "Chưa tính lương. Hãy xử lý dữ liệu nguồn; kết quả đã lưu được giữ nguyên.",errors);
        return new PayrollCalculation(result,income,deductions,net);
    }
}
