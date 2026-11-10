package com.hrms.service;

import com.hrms.dao.PayrollDAO;
import com.hrms.model.PayrollAdjustmentInput;
import com.hrms.model.PayrollFormulaSettings;
import com.hrms.model.PayrollImportInput;
import com.hrms.model.PayrollPeriod;
import com.hrms.model.PayrollCalculation;
import com.hrms.model.PayrollSlipView;
import com.hrms.service.security.TenantPrincipal;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PayrollService {
    private static final BigDecimal MAX_MONEY = new BigDecimal("9999999999999999.99");
    private final PayrollDAO dao;
    public PayrollService() { this(new PayrollDAO()); }
    public PayrollService(PayrollDAO dao) { this.dao=dao; }

    public static void requireTenant(TenantPrincipal p) {
        if (p==null || p.getCompanyId()==null || p.getCompanyId()<1 || p.getUserId()==null || p.getUserId()<1)
            throw new PayrollException(401,"Vui lòng đăng nhập vào công ty của bạn.");
        if (p.isMustChangePassword()) throw new PayrollException(403,"Vui lòng đổi mật khẩu tạm trước khi tiếp tục.");
    }

    public static void requirePayrollAccess(TenantPrincipal p) {
        requireTenant(p);
        if (!p.canManagePayroll())
            throw new PayrollException(403,"Chỉ Manager hoặc Company Admin/HR được quản lý bảng lương của công ty.");
    }

    public static int ownEmployee(TenantPrincipal p) {
        requireTenant(p);
        if (p.getEmployeeId()==null || p.getEmployeeId()<1 || p.getRoleCode()==null
                || !Set.of("EMPLOYEE","MANAGER","HR","COMPANY_ADMIN").contains(p.getRoleCode().toUpperCase(Locale.ROOT)))
            throw new PayrollException(403,"Tài khoản chưa có quyền xem phiếu lương cá nhân.");
        return p.getEmployeeId();
    }

    /** Always read saved monetary snapshots, without consulting current compensation. */
    public List<PayrollSlipView> periodPayslips(TenantPrincipal p,int periodId) throws SQLException {
        requirePayrollAccess(p);
        if (periodId<1) throw new PayrollException(400,"Mã kỳ lương không hợp lệ.");
        return dao.periodPayslips(p.getCompanyId(),periodId);
    }

    public List<PayrollSlipView> ownPayslips(TenantPrincipal p) throws SQLException {
        int employeeId=ownEmployee(p);
        return dao.ownPayslips(p.getCompanyId(),employeeId);
    }

    public PayrollSlipView payslip(TenantPrincipal p,long payslipId,boolean management) throws SQLException {
        Integer employeeId;
        if (management) { requirePayrollAccess(p);employeeId=null; }
        else employeeId=ownEmployee(p);
        if (payslipId<1) throw new PayrollException(400,"Mã phiếu lương không hợp lệ.");
        return dao.payslip(p.getCompanyId(),employeeId,payslipId);
    }

    public static void validatePeriod(int month,int year) {
        if(month<1 || month>12) throw new PayrollException(400,"Tháng phải từ 1 đến 12.");
        // SQL Server DATEFROMPARTS supports years through 9999.
        if(year<2000 || year>9999) throw new PayrollException(400,"Năm phải từ 2000 đến 9999.");
        LocalDate now = LocalDate.now();
        if (year > now.getYear() || (year == now.getYear() && month >= now.getMonthValue())) {
            throw new PayrollException(400, "Chỉ được tạo kỳ lương cho các tháng đã kết thúc (từ tháng trước trở về trước).");
        }
    }

    public static BigDecimal money(String raw,String label) {
        try {
            if(raw==null || raw.isBlank()) throw new NumberFormatException();
            BigDecimal value=new BigDecimal(raw.trim());
            if(value.signum()<0 || value.compareTo(MAX_MONEY)>0 || value.stripTrailingZeros().scale()>2)
                throw new NumberFormatException();
            return value.setScale(2);
        } catch(ArithmeticException | NumberFormatException e) {
            throw new PayrollException(400,label+" phải là số không âm, tối đa 2 chữ số thập phân và phù hợp DECIMAL(18,2).");
        }
    }

    public static String note(String value) {
        String text=value==null?"":value.trim();
        if(text.length()>300) throw new PayrollException(400,"Ghi chú không được vượt quá 300 ký tự.");
        return text;
    }

    public List<PayrollPeriod> periods(TenantPrincipal p) throws SQLException {
        requirePayrollAccess(p);
        return dao.periods(p.getCompanyId());
    }

    public PayrollPeriod period(TenantPrincipal p, int periodId) throws SQLException {
        requirePayrollAccess(p);
        if (periodId < 1) throw new PayrollException(400, "Mã kỳ lương không hợp lệ.");
        return dao.findPeriod(p.getCompanyId(), periodId);
    }

    public int create(TenantPrincipal p,String name,int month,int year) throws SQLException {
        requirePayrollAccess(p); validatePeriod(month,year);
        String title=name==null?"":name.trim();
        if(title.isBlank()) title=String.format("Bảng lương tháng %02d/%d",month,year);
        if(title.length()>100) throw new PayrollException(400,"Tên kỳ lương tối đa 100 ký tự.");
        return dao.createPeriod(p.getCompanyId(),p.getUserId(),title,month,year);
    }

    public void setSalary(TenantPrincipal p,int employeeId,String rawAmount) throws SQLException {
        setSalary(p,employeeId,rawAmount,LocalDate.now().toString());
    }

    public void setSalary(TenantPrincipal p,int employeeId,String rawAmount,String rawEffectiveFrom) throws SQLException {
        requirePayrollAccess(p);
        LocalDate effectiveFrom;
        try {
            effectiveFrom=LocalDate.parse(rawEffectiveFrom==null?"":rawEffectiveFrom);
            if (effectiveFrom.getYear()<2000 || effectiveFrom.getYear()>9999) throw new DateTimeParseException("range",rawEffectiveFrom,0);
        } catch (DateTimeParseException e) {
            throw new PayrollException(400,"Ngày hiệu lực phải hợp lệ, theo dạng yyyy-MM-dd, từ năm 2000 đến 9999.");
        }
        if (employeeId<1) throw new PayrollException(400,"Mã nhân viên không hợp lệ.");
        dao.setBaseSalary(p.getCompanyId(),p.getUserId(),employeeId,money(rawAmount,"Lương cơ bản"),effectiveFrom);
    }

    public boolean adjustment(TenantPrincipal p,int periodId,int employeeId,String bonus,String deduction,String note) throws SQLException {
        requirePayrollAccess(p);
        if (periodId<1 || employeeId<1) throw new PayrollException(400,"Mã kỳ lương hoặc nhân viên không hợp lệ.");
        return dao.saveAdjustment(p.getCompanyId(),p.getUserId(),periodId,employeeId,money(bonus,"Thưởng"),money(deduction,"Khấu trừ"),note(note));
    }

    public int importRows(TenantPrincipal p,int periodId,List<PayrollAdjustmentInput> rows) throws SQLException {
        requirePayrollAccess(p);
        if(rows==null || rows.isEmpty() || rows.size()>5000) throw new PayrollException(400,"File cần có từ 1 đến 5.000 dòng dữ liệu.");
        Set<String> codes=new HashSet<>();
        for(PayrollAdjustmentInput row:rows) {
            if(row.employeeCode()==null || row.employeeCode().isBlank() || !codes.add(row.employeeCode().toUpperCase(Locale.ROOT)))
                throw new PayrollException(400,"Mã nhân viên bị trống hoặc trùng tại dòng "+row.rowNumber()+".");
            money(row.bonus().toPlainString(),"Thưởng"); money(row.deduction().toPlainString(),"Khấu trừ"); note(row.note());
        }
        return dao.importAdjustments(p.getCompanyId(),p.getUserId(),periodId,rows);
    }

    public int importPayroll(TenantPrincipal p, int periodId, List<PayrollImportInput> rows) throws SQLException {
        requirePayrollAccess(p);
        if (periodId < 1) throw new PayrollException(400, "Mã kỳ lương không hợp lệ.");
        if (rows == null || rows.isEmpty() || rows.size() > 5000)
            throw new PayrollException(400, "File cần có từ 1 đến 5.000 dòng dữ liệu.");
        Set<String> codes = new HashSet<>();
        for (PayrollImportInput row : rows) {
            if (row.employeeCode() == null || row.employeeCode().isBlank() || !codes.add(row.employeeCode().toUpperCase(Locale.ROOT)))
                throw new PayrollException(400, "Mã nhân viên bị trống hoặc trùng tại dòng " + row.rowNumber() + ".");
            if (row.contractBaseSalary() == null || row.contractBaseSalary().compareTo(BigDecimal.ZERO) < 0)
                throw new PayrollException(400, "Lương cơ bản không hợp lệ tại dòng " + row.rowNumber() + ".");
            if (row.actualWorkDays() == null || row.actualWorkDays().compareTo(BigDecimal.ZERO) < 0)
                throw new PayrollException(400, "Ngày công thực tế không hợp lệ tại dòng " + row.rowNumber() + ".");
            if (row.standardWorkDays() == null || row.standardWorkDays().compareTo(BigDecimal.ZERO) <= 0)
                throw new PayrollException(400, "Ngày công chuẩn phải lớn hơn 0 tại dòng " + row.rowNumber() + ".");
            if (row.bonus() != null) money(row.bonus().toPlainString(), "Thưởng");
            note(row.note());
        }
        return dao.importPayroll(p.getCompanyId(), p.getUserId(), periodId, rows);
    }

    public java.util.Map<String, Object> applyBonusCampaign(TenantPrincipal p, int periodId,
                                                            String campaignName, String calcType,
                                                            String rawMonthsMultiplier, boolean prorateByTenure,
                                                            String rawFixedAmount, String applyMode) throws SQLException {
        requirePayrollAccess(p);
        if (periodId < 1) throw new PayrollException(400, "Mã kỳ lương không hợp lệ.");
        String name = campaignName == null ? "" : campaignName.trim();
        if (name.isBlank()) name = "Đợt thưởng";
        if (name.length() > 100) throw new PayrollException(400, "Tên đợt thưởng không được vượt quá 100 ký tự.");

        String type = "CONTRACT_SALARY".equalsIgnoreCase(calcType) ? "CONTRACT_SALARY" : "FIXED_AMOUNT";
        BigDecimal multiplier = BigDecimal.ONE;
        if ("CONTRACT_SALARY".equals(type)) {
            try {
                if (rawMonthsMultiplier != null && !rawMonthsMultiplier.isBlank()) {
                    multiplier = new BigDecimal(rawMonthsMultiplier.trim());
                    if (multiplier.compareTo(BigDecimal.ZERO) <= 0 || multiplier.compareTo(new BigDecimal("12.00")) > 0) {
                        throw new NumberFormatException();
                    }
                }
            } catch (NumberFormatException e) {
                throw new PayrollException(400, "Số tháng lương thưởng phải lớn hơn 0 và không vượt quá 12 tháng.");
            }
        }

        BigDecimal fixedAmt = BigDecimal.ZERO;
        if ("FIXED_AMOUNT".equals(type)) {
            fixedAmt = money(rawFixedAmount, "Số tiền thưởng");
        }

        String mode = "ADD".equalsIgnoreCase(applyMode) ? "ADD" : "REPLACE";
        return dao.applyBonusCampaign(p.getCompanyId(), p.getUserId(), periodId, name, type, multiplier, prorateByTenure, fixedAmt, mode);
    }

    public List<java.util.Map<String, Object>> bonusCandidates(TenantPrincipal p, int periodId) throws SQLException {
        requirePayrollAccess(p);
        period(p, periodId);
        return dao.bonusCandidates(p.getCompanyId(), periodId);
    }

    public java.util.Map<String, Object> saveBonusAdjustments(TenantPrincipal p, int periodId, int[] employeeIds, BigDecimal[] bonuses, String[] notes) throws SQLException {
        requirePayrollAccess(p);
        if (periodId < 1) throw new PayrollException(400, "Mã kỳ lương không hợp lệ.");
        if (employeeIds == null || employeeIds.length == 0) throw new PayrollException(400, "Danh sách nhân viên không được để trống.");
        return dao.saveBonusAdjustments(p.getCompanyId(), p.getUserId(), periodId, employeeIds, bonuses, notes);
    }

    public int calculate(TenantPrincipal p,int periodId) throws SQLException {
        requirePayrollAccess(p);
        if (periodId<1) throw new PayrollException(400,"Mã kỳ lương không hợp lệ.");
        return dao.calculate(p.getCompanyId(),p.getUserId(),periodId);
    }
    public PayrollCalculation preview(TenantPrincipal p,int periodId) throws SQLException {
        requirePayrollAccess(p);
        if (periodId<1) throw new PayrollException(400,"Mã kỳ lương không hợp lệ.");
        return dao.preview(p.getCompanyId(),periodId);
    }
    public void transition(TenantPrincipal p,int periodId,String action) throws SQLException {
        requirePayrollAccess(p);
        if (periodId<1) throw new PayrollException(400,"Mã kỳ lương không hợp lệ.");
        if(action==null || !Set.of("review","approve","close","reopen").contains(action)) throw new PayrollException(400,"Thao tác kỳ lương không hợp lệ.");
        dao.transition(p.getCompanyId(),p.getUserId(),periodId,action);
    }
    public int publish(TenantPrincipal p,int periodId) throws SQLException {
        requirePayrollAccess(p); return dao.publish(p.getCompanyId(),p.getUserId(),periodId);
    }

    public PayrollFormulaSettings formulaSettings(TenantPrincipal p) throws SQLException {
        requirePayrollAccess(p);
        return dao.getFormulaSettings(p.getCompanyId());
    }

    public PayrollFormulaSettings saveFormulaSettings(TenantPrincipal p, String salaryCalculationMode,
                                                      String standardDaysMode, String rawFixedStandardDays,
                                                      String rawWeekdayWeights,
                                                      boolean includePaidLeave, boolean includeResponsibilityAllowance,
                                                      boolean includeTravelAllowance) throws SQLException {
        requirePayrollAccess(p);
        String calcMode = salaryCalculationMode == null ? "WORKDAYS" : salaryCalculationMode.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("WORKDAYS", "FIXED_MONTHLY").contains(calcMode)) {
            throw new PayrollException(400, "Chế độ tính lương cơ bản không hợp lệ.");
        }
        String stdMode = standardDaysMode == null ? "MON_FRI" : standardDaysMode.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("MON_FRI", "MON_SAT_HALF", "MON_SAT", "CUSTOM_WEEKDAYS", "FIXED").contains(stdMode)) {
            throw new PayrollException(400, "Cách tính số công tiêu chuẩn không hợp lệ.");
        }
        BigDecimal fixedDays = new BigDecimal("22.00");
        if (rawFixedStandardDays != null && !rawFixedStandardDays.isBlank()) {
            try {
                fixedDays = new BigDecimal(rawFixedStandardDays.trim()).setScale(2, java.math.RoundingMode.HALF_UP);
                if (fixedDays.compareTo(BigDecimal.ONE) < 0 || fixedDays.compareTo(new BigDecimal("31.00")) > 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                throw new PayrollException(400, "Số công tiêu chuẩn cố định phải từ 1 đến 31 ngày.");
            }
        }
        String weights = PayrollFormulaSettings.defaultWeightsForMode(stdMode);
        if ("CUSTOM_WEEKDAYS".equals(stdMode)) {
            if (rawWeekdayWeights == null || rawWeekdayWeights.isBlank()) {
                weights = "1,1,1,1,1,0,0";
            } else {
                String[] parts = rawWeekdayWeights.trim().split(",");
                if (parts.length != 7) {
                    throw new PayrollException(400, "Cấu hình ngày công trong tuần phải đủ 7 ngày (Thứ 2 đến Chủ nhật).");
                }
                StringBuilder normalized = new StringBuilder();
                BigDecimal weeklySum = BigDecimal.ZERO;
                for (int i = 0; i < 7; i++) {
                    try {
                        BigDecimal w = new BigDecimal(parts[i].trim()).stripTrailingZeros();
                        if (w.compareTo(BigDecimal.ZERO) < 0 || w.compareTo(BigDecimal.ONE) > 0) {
                            throw new NumberFormatException();
                        }
                        weeklySum = weeklySum.add(w);
                        if (i > 0) normalized.append(',');
                        normalized.append(w.toPlainString());
                    } catch (NumberFormatException e) {
                        throw new PayrollException(400, "Hệ số công mỗi ngày trong tuần phải từ 0 đến 1 (ví dụ: 0, 0.5 hoặc 1).");
                    }
                }
                if (weeklySum.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new PayrollException(400, "Tổng số công tiêu chuẩn trong tuần phải lớn hơn 0.");
                }
                weights = normalized.toString();
            }
        }
        PayrollFormulaSettings settings = new PayrollFormulaSettings(
                p.getCompanyId(),
                calcMode,
                stdMode,
                fixedDays,
                weights,
                includePaidLeave,
                includeResponsibilityAllowance,
                includeTravelAllowance
        );
        return dao.saveFormulaSettings(p.getCompanyId(), p.getUserId(), settings);
    }
}
