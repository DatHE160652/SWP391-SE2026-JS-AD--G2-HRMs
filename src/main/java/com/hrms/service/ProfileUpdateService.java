package com.hrms.service;

import com.hrms.dao.ProfileUpdateDAO;
import com.hrms.model.ProfileUpdatePeriod;
import com.hrms.service.security.TenantPrincipal;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProfileUpdateService {
    public static final Set<String> UPDATABLE_FIELDS = Set.of(
            "email", "phone", "identityNumber", "identityIssuedDate", "identityIssuedPlace",
            "permanentAddress", "hometown"
    );
    private final ProfileUpdateDAO profileUpdateDAO;
    private final Clock clock;

    public ProfileUpdateService() {
        this(new ProfileUpdateDAO(), Clock.systemUTC());
    }

    public ProfileUpdateService(String periodType) {
        this(new ProfileUpdateDAO(periodType), Clock.systemUTC());
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

    public List<Map<String, Object>> getDetailedPeriods(TenantPrincipal principal) throws SQLException {
        requireAdministrator(principal);
        return profileUpdateDAO.getDetailedPeriods(principal.getCompanyId());
    }

    public Map<String, Object> getPeriodDetail(TenantPrincipal principal, long periodId) throws SQLException {
        requireAdministrator(principal);
        return profileUpdateDAO.getPeriodDetail(principal.getCompanyId(), periodId);
    }

    public List<Map<String, Object>> getFieldCatalog() {
        return profileUpdateDAO.getFieldCatalogMaps();
    }

    public long create(TenantPrincipal principal, String title, LocalDateTime startsAt, LocalDateTime endsAt,
                       String ipAddress) throws SQLException {
        return create(principal, title, startsAt, endsAt, "ALL", ProfileUpdateDAO.DEFAULT_FIELD_KEYS, List.of(), ipAddress);
    }

    public long create(TenantPrincipal principal, String title, LocalDateTime startsAt, LocalDateTime endsAt,
                       String scopeType, List<String> allowedFields, List<Integer> employeeIds,
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
                startsAt, endsAt, scopeType, allowedFields, employeeIds, ipAddress);
    }

    public void close(TenantPrincipal principal, long periodId, String ipAddress) throws SQLException {
        requireAdministrator(principal);
        profileUpdateDAO.close(principal.getCompanyId(), principal.getUserId(), periodId, ipAddress);
    }

    public ProfileUpdatePeriod getAvailablePeriod(TenantPrincipal principal) throws SQLException {
        return canUpdateOwnProfile(principal) ? profileUpdateDAO.getAvailablePeriod(principal.getCompanyId()) : null;
    }

    public ProfileUpdateDAO.ActiveEmployeeProfileContext getActiveContext(TenantPrincipal principal) throws SQLException {
        return canUpdateOwnProfile(principal)
                ? profileUpdateDAO.getActiveContextForEmployee(principal.getCompanyId(), principal.getEmployeeId())
                : null;
    }

    public void updatePhone(TenantPrincipal principal, String phone, String ipAddress) throws SQLException {
        Map<String, String> changes = new LinkedHashMap<>();
        changes.put("phone", phone);
        updateProfile(principal, changes, ipAddress);
    }

    public void updateProfile(TenantPrincipal principal, Map<String, String> changes, String ipAddress) throws SQLException {
        updateProfile(principal, changes, null, ipAddress);
    }

    public void updateProfile(TenantPrincipal principal, Map<String, String> changes, String expectedUpdatedAt, String ipAddress) throws SQLException {
        if (!canUpdateOwnProfile(principal)) {
            throw new SecurityException("Chỉ nhân viên hoặc quản lý được cập nhật hồ sơ của chính mình theo đợt.");
        }
        if (changes == null || changes.isEmpty() || !UPDATABLE_FIELDS.containsAll(changes.keySet())) {
            throw new FieldValidationException("phone", "Chỉ được cập nhật các trường thông tin cá nhân hợp lệ.");
        }
        if (!changes.keySet().equals(Set.of("phone")) && (expectedUpdatedAt == null || expectedUpdatedAt.isBlank())) {
            throw new FieldValidationException("phone", "Vui lòng tải lại hồ sơ trước khi cập nhật thông tin cá nhân.");
        }
        LocalDate todayVn = LocalDate.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh")));
        Map<String, String> normalized = new LinkedHashMap<>();
        for (var entry : changes.entrySet()) {
            String field = entry.getKey();
            String value = entry.getValue() == null ? null : entry.getValue().trim();
            if (value != null && value.isEmpty()) {
                value = null;
            }
            if ("phone".equals(field)) {
                value = com.hrms.util.ValidationSupport.validatePhone(value);
            }
            int maximum = switch (field) {
                case "email" -> 150;
                case "permanentAddress" -> 500;
                case "identityNumber" -> 20;
                case "gender" -> 20;
                default -> 200;
            };
            if (value != null && value.length() > maximum) {
                throw new FieldValidationException(field, "Độ dài tối đa là " + maximum + " ký tự.");
            }
            if ("email".equals(field) && (value == null || value.isBlank())) {
                throw new FieldValidationException(field, "Vui lòng nhập email.");
            }
            if ("email".equals(field) && !value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new FieldValidationException(field, "Vui lòng nhập địa chỉ email hợp lệ.");
            }
            if ("identityNumber".equals(field) && value != null && !value.matches("^[0-9]{9,12}$")) {
                throw new FieldValidationException(field, "Số CCCD/CMND phải gồm từ 9 đến 12 chữ số.");
            }
            if ("gender".equals(field) && value != null && !Set.of("Male", "Female", "Other").contains(value)) {
                throw new FieldValidationException(field, "Giới tính không hợp lệ.");
            }
            if ("identityIssuedDate".equals(field) && value != null) {
                try {
                    if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") || LocalDate.parse(value).isAfter(todayVn)) {
                        throw new java.time.format.DateTimeParseException("Invalid date", value, 0);
                    }
                } catch (java.time.format.DateTimeParseException exception) {
                    throw new FieldValidationException(field, "Ngày cấp căn cước phải hợp lệ và không được ở tương lai.");
                }
            }
            if ("dateOfBirth".equals(field) && value != null) {
                try {
                    LocalDate dob = LocalDate.parse(value);
                    if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") || dob.isAfter(todayVn.minusYears(15))) {
                        throw new java.time.format.DateTimeParseException("Invalid dob", value, 0);
                    }
                } catch (java.time.format.DateTimeParseException exception) {
                    throw new FieldValidationException(field, "Ngày sinh phải hợp lệ (nhân viên từ đủ 15 tuổi trở lên).");
                }
            }
            normalized.put(field, value);
        }
        profileUpdateDAO.updateProfile(principal.getCompanyId(), principal.getUserId(), principal.getEmployeeId(), normalized, expectedUpdatedAt, ipAddress);
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
