package com.hrms.service;

import com.hrms.dao.EmployeePasswordResetDAO;
import com.hrms.service.email.*;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.TenantPasswordSupport;
import java.sql.SQLException;
import java.util.*;

public class EmployeePasswordResetService {
    private final EmployeePasswordResetDAO dao;
    private final PasswordResetEmailSender sender;
    public EmployeePasswordResetService() { this(new EmployeePasswordResetDAO(),new ConfiguredPasswordResetEmailSender()); }
    public EmployeePasswordResetService(EmployeePasswordResetDAO dao,PasswordResetEmailSender sender) { this.dao=dao; this.sender=sender; }
    public void requireEditor(TenantPrincipal actor) {
        if(actor==null || actor.getCompanyId()==null || actor.getCompanyId()<=0 || actor.getUserId()==null
                || actor.getUserId()<=0 || !actor.isCompanyAdministrator() || actor.isMustChangePassword())
            throw new SecurityException("Bạn không có quyền đặt lại mật khẩu nhân viên.");
    }
    public List<Map<String,Object>> search(TenantPrincipal actor,String email,String identityNumber) throws SQLException {
        requireEditor(actor);
        String normalizedEmail=normalizeEmail(email), identity=normalizeIdentity(identityNumber);
        if(normalizedEmail==null && identity==null) throw new IllegalArgumentException("Nhập email hoặc CCCD/CMND để tìm kiếm.");
        return dao.search(actor,normalizedEmail,identity);
    }
    static String normalizeEmail(String value) {
        if(value==null || value.isBlank()) return null;
        String normalized=value.trim().toLowerCase(Locale.ROOT);
        if(normalized.length()>150 || !normalized.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Email không hợp lệ.");
        return normalized;
    }
    static String normalizeIdentity(String value) {
        if(value==null || value.isBlank()) return null;
        String normalized=value.trim();
        if(!normalized.matches("[0-9]{1,50}")) throw new IllegalArgumentException("CCCD/CMND chỉ gồm chữ số, tối đa 50 ký tự.");
        return normalized;
    }
    public static byte[] parseVersion(String version) {
        if(version==null || !version.matches("[0-9a-fA-F]{16}")) throw new IllegalArgumentException("Phiên bản không hợp lệ. Vui lòng tìm lại nhân viên.");
        return HexFormat.of().parseHex(version);
    }
    public Map<String,Object> reset(TenantPrincipal actor,int employeeId,String version) throws SQLException {
        requireEditor(actor);
        if(employeeId<=0) throw new IllegalArgumentException("Vui lòng chọn nhân viên hợp lệ.");
        byte[] expected=parseVersion(version);
        String operationId=UUID.randomUUID().toString();
        String temporary=TenantPasswordSupport.temporaryPassword();
        try {
            PasswordResetEmailSender.Prepared prepared=dao.reset(actor,employeeId,expected,
                    TenantPasswordSupport.hash(temporary),operationId,target -> {
                        try {
                            String workspace=(String)target.get("workspaceCode");
                            return sender.prepare((String)target.get("email"),workspace,
                                    RegistrationRequestService.activationLoginUrl(workspace));
                        } catch(EmailDeliveryException|IllegalArgumentException exception) {
                            throw new ConfigurationException();
                        }
                    });
            // DAO has committed. Nothing below may claim the password update was rolled back.
            String status="ACCEPTED";
            try { prepared.send(temporary); }
            catch(EmailDeliveryException exception) { status="MAIL_PROVIDER_UNAVAILABLE".equals(exception.getCode())?"UNKNOWN":"FAILED"; }
            catch(RuntimeException exception) { status="UNKNOWN"; }
            boolean recorded=true;
            try { dao.recordDelivery(actor,employeeId,operationId,status); }
            catch(SQLException|RuntimeException exception) { recorded=false; }
            String message=switch(status) {
                case "ACCEPTED" -> "Đã đặt lại mật khẩu. Máy chủ email đã chấp nhận thư gửi (chưa đảm bảo vào hộp thư).";
                case "FAILED" -> "Mật khẩu đã được đặt lại nhưng gửi email thất bại. Kiểm tra SMTP và tìm lại nhân viên trước khi xác nhận đặt lại lần nữa.";
                default -> "Mật khẩu đã được đặt lại nhưng chưa xác định được kết quả gửi email. Không tự động thử lại; kiểm tra thư và tìm lại nhân viên nếu cần đặt lại lần nữa.";
            };
            if(!recorded) message += " Chưa ghi nhận được nhật ký kết quả gửi email; liên hệ quản trị hệ thống và cung cấp mã thao tác.";
            return Map.of("message",message,"deliveryStatus",status,"operationId",operationId,"emailMetadataRecorded",recorded);
        } finally { temporary=null; Arrays.fill(expected,(byte)0); }
    }
    public static class ConfigurationException extends RuntimeException {
        public ConfigurationException() { super("Chưa thể đặt lại mật khẩu do cấu hình email hoặc URL đăng nhập không hợp lệ. Liên hệ quản trị hệ thống."); }
    }
}
