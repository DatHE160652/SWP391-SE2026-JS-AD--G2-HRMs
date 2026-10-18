package com.hrms.service;

import com.hrms.dao.ProfileUpdateDAO;
import com.hrms.model.ProfileUpdatePeriod;
import com.hrms.service.security.TenantPrincipal;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class ProfileUpdateService {
    private final ProfileUpdateDAO profileUpdateDAO;
    private final Clock clock;

    public ProfileUpdateService() {
        this(new ProfileUpdateDAO(), Clock.systemUTC());
    }

    ProfileUpdateService(ProfileUpdateDAO profileUpdateDAO, Clock clock) {
        this.profileUpdateDAO = profileUpdateDAO;
        this.clock = clock;
    }

    public void requireAdministrator(TenantPrincipal principal) {
        if (!validSession(principal) || !Set.of("COMPANY_ADMIN", "HR").contains(principal.getRoleCode())) {
            throw new SecurityException("Chỉ Company Admin hoặc HR được quản lý đợt cập nhật hồ sơ.");
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
        return canUpdateOwnPhone(principal) ? profileUpdateDAO.getAvailablePeriod(principal.getCompanyId()) : null;
    }

    public void updatePhone(TenantPrincipal principal, String phone, String ipAddress) throws SQLException {
        if (!canUpdateOwnPhone(principal)) {
            throw new SecurityException("Chỉ Employee hoặc Manager được cập nhật số điện thoại của chính mình theo đợt.");
        }
        if (phone == null || !phone.trim().matches("0[0-9]{9}")) {
            throw new FieldValidationException("phone", "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.");
        }
        profileUpdateDAO.updatePhone(principal.getCompanyId(), principal.getUserId(), principal.getEmployeeId(),
                phone.trim(), ipAddress);
    }

    private boolean canUpdateOwnPhone(TenantPrincipal principal) {
        return validSession(principal) && principal.getEmployeeId() != null && principal.getEmployeeId() > 0
                && Set.of("EMPLOYEE", "MANAGER").contains(principal.getRoleCode());
    }

    private boolean validSession(TenantPrincipal principal) {
        return principal != null && principal.getCompanyId() != null && principal.getUserId() != null
                && principal.getCompanyId() > 0 && principal.getUserId() > 0 && principal.getRoleCode() != null
                && !principal.isMustChangePassword();
    }
}
