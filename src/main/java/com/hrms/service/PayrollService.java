package com.hrms.service;

import com.hrms.dao.PayrollDAO;
import com.hrms.model.PayrollAdjustmentInput;
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
}
