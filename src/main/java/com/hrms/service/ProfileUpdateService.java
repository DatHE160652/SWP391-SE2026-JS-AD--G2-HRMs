package com.hrms.service;

import com.hrms.dao.ProfileUpdateDAO;
import com.hrms.model.ProfileUpdatePeriod;
import com.hrms.service.security.TenantPrincipal;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

public class ProfileUpdateService {
    public static final Set<String> UPDATABLE_FIELDS=Set.of("email","phone","identityIssuedDate","identityIssuedPlace","permanentAddress","hometown");
    private final ProfileUpdateDAO profileUpdateDAO;
    private final Clock clock;

    public ProfileUpdateService() {
        this(new ProfileUpdateDAO(), Clock.systemUTC());
    }

    public ProfileUpdateService(String periodType) {
        this(new ProfileUpdateDAO(periodType),Clock.systemUTC());
    }

    ProfileUpdateService(ProfileUpdateDAO profileUpdateDAO, Clock clock) {
        this.profileUpdateDAO = profileUpdateDAO;
        this.clock = clock;
    }

    public void requireAdministrator(TenantPrincipal principal) {
        if (!validSession(principal) || !principal.isCompanyAdministrator()) {
            throw new SecurityException("Chỉ Company Admin / HR được quản lý các đợt cập nhật.");
        }
    }

    public List<ProfileUpdatePeriod> getPeriods(TenantPrincipal principal) throws SQLException {
        requireAdministrator(principal);
        return profileUpdateDAO.getPeriods(principal.getCompanyId());
    }

    public long create(TenantPrincipal principal, String title, LocalDateTime startsAt, LocalDateTime endsAt,
                       String ipAddress) throws SQLException {
        requireAdministrator(principal);
        if (title == null || title.isBlank() || title.trim().length() > 150) {
            throw new FieldValidationException("title", "Vui lòng nhập tên đợt cập nhật, tối đa 150 ký tự.");
        }
        if (startsAt == null) {
            throw new FieldValidationException("startsAt", "Vui lòng chọn thời gian bắt đầu.");
        }
        if (endsAt == null || !endsAt.isAfter(startsAt) || !endsAt.isAfter(LocalDateTime.now(clock))) {
            throw new FieldValidationException("endsAt", "Thời gian kết thúc phải sau thời gian bắt đầu và thời điểm hiện tại.");
        }
        return profileUpdateDAO.create(principal.getCompanyId(), principal.getUserId(), title.trim(),
                startsAt, endsAt, ipAddress);
    }

    public void close(TenantPrincipal principal, long periodId, String ipAddress) throws SQLException {
        requireAdministrator(principal);
        profileUpdateDAO.close(principal.getCompanyId(), principal.getUserId(), periodId, ipAddress);
    }

    public ProfileUpdatePeriod getAvailablePeriod(TenantPrincipal principal) throws SQLException {
        return canUpdateOwnProfile(principal) ? profileUpdateDAO.getAvailablePeriod(principal.getCompanyId()) : null;
    }

    public void updatePhone(TenantPrincipal principal, String phone, String ipAddress) throws SQLException {
        Map<String,String> changes=new LinkedHashMap<>();
        changes.put("phone",phone);
        updateProfile(principal,changes,ipAddress);
    }

    public void updateProfile(TenantPrincipal principal, Map<String,String> changes, String ipAddress) throws SQLException {
        updateProfile(principal,changes,null,ipAddress);
    }

    public void updateProfile(TenantPrincipal principal, Map<String,String> changes, String expectedUpdatedAt, String ipAddress) throws SQLException {
        if (!canUpdateOwnProfile(principal)) {
            throw new SecurityException("Chỉ nhân viên hoặc quản lý được cập nhật hồ sơ của chính mình theo đợt.");
        }
        if (changes==null || changes.isEmpty() || !UPDATABLE_FIELDS.containsAll(changes.keySet()))
            throw new FieldValidationException("phone","Chỉ được cập nhật thông tin liên hệ, ngày/nơi cấp căn cước, địa chỉ thường trú và quê quán.");
        if (!changes.keySet().equals(Set.of("phone")) && (expectedUpdatedAt==null || expectedUpdatedAt.isBlank()))
            throw new FieldValidationException("phone","Vui lòng tải lại hồ sơ trước khi cập nhật thông tin cá nhân.");
        Map<String,String> normalized=new LinkedHashMap<>();
        for (var entry:changes.entrySet()) {
            String field=entry.getKey(),value=entry.getValue()==null?null:entry.getValue().trim();
            if (value!=null && value.isEmpty()) value=null;
            if ("phone".equals(field) && (value==null || !value.matches("0[0-9]{9}")))
                throw new FieldValidationException(field,"Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.");
            int maximum=switch(field) { case "email" -> 150; case "permanentAddress" -> 500; default -> 200; };
            if (value!=null && value.length()>maximum) throw new FieldValidationException(field,"Độ dài tối đa là "+maximum+" ký tự.");
            if ("email".equals(field) && value!=null && !value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
                throw new FieldValidationException(field,"Vui lòng nhập địa chỉ email hợp lệ.");
            if ("identityIssuedDate".equals(field) && value!=null) {
                try {
                    if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") || LocalDate.parse(value).isAfter(LocalDate.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh")))))
                        throw new java.time.format.DateTimeParseException("Invalid date",value,0);
                } catch (java.time.format.DateTimeParseException exception) {
                    throw new FieldValidationException(field,"Ngày cấp căn cước phải hợp lệ và không được ở tương lai.");
                }
            }
            normalized.put(field,value);
        }
        profileUpdateDAO.updateProfile(principal.getCompanyId(), principal.getUserId(), principal.getEmployeeId(),normalized,expectedUpdatedAt,ipAddress);
    }

    private boolean canUpdateOwnProfile(TenantPrincipal principal) {
        return validSession(principal) && principal.getEmployeeId() != null && principal.getEmployeeId() > 0
                && Set.of("EMPLOYEE", "MANAGER").contains(principal.getRoleCode());
    }

    private boolean validSession(TenantPrincipal principal) {
        return principal != null && principal.getCompanyId() != null && principal.getUserId() != null
                && principal.getCompanyId() > 0 && principal.getUserId() > 0 && principal.getRoleCode() != null
                && !principal.isMustChangePassword();
    }
}
