package com.hrms.service.sysadmin;

import com.hrms.dao.sysadmin.CompanyAdministrationDAO;
import com.hrms.model.CompanyAdministrationFilter;
import com.hrms.model.CompanyAdministrationView;

import java.sql.SQLException;
import java.util.List;

/** Business rules for System Admin company visibility and subscription transitions. */
public class CompanyAdministrationService {
    private final CompanyAdministrationDAO companyDAO;

    public CompanyAdministrationService() {
        this(new CompanyAdministrationDAO());
    }

    public CompanyAdministrationService(CompanyAdministrationDAO companyDAO) {
        this.companyDAO = companyDAO;
    }

    public List<CompanyAdministrationView> getCompanies() throws SQLException {
        return companyDAO.findAll();
    }

    public CompanyAdministrationFilter createFilter(String status, String region, String sort) {
        return new CompanyAdministrationFilter(status, region, sort);
    }

    public CompanyAdministrationFilter createFilter(String q, String status, String region, String sort) {
        return new CompanyAdministrationFilter(q, status, region, sort);
    }

    public List<CompanyAdministrationView> getCompanies(CompanyAdministrationFilter filter) throws SQLException {
        return companyDAO.findAll(filter);
    }

    public CompanyAdministrationView getCompany(int companyId) throws SQLException {
        return companyDAO.findById(companyId);
    }

    public void renew(int companyId, int actorId, int months, String ipAddress)
            throws SQLException, RequestActionException {
        if (companyId <= 0 || actorId <= 0) throw new IllegalArgumentException("Mã Company hoặc người thực hiện không hợp lệ.");
        if (months != 1 && months != 3 && months != 6 && months != 12) {
            throw new IllegalArgumentException("Chỉ hỗ trợ gia hạn 1, 3, 6 hoặc 12 tháng.");
        }
        requireSuccess(companyDAO.renew(companyId, actorId, months, ipAddress));
    }

    public void changePlan(int companyId, int actorId, int planId, String ipAddress)
            throws SQLException, RequestActionException {
        if (companyId <= 0 || actorId <= 0 || planId <= 0) {
            throw new IllegalArgumentException("Thông tin gói dịch vụ hoặc doanh nghiệp không hợp lệ.");
        }
        requireSuccess(companyDAO.changePlan(companyId, actorId, planId, ipAddress));
    }

    private String normalizeReason(String reason, String action) {
        if (reason == null) throw new IllegalArgumentException("Lý do " + action + " là bắt buộc.");
        String normalized = reason.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) throw new IllegalArgumentException("Lý do " + action + " là bắt buộc.");
        if (normalized.length() > 500) throw new IllegalArgumentException("Lý do " + action + " không được vượt quá 500 ký tự.");
        return normalized;
    }

    private void requireSuccess(CompanyAdministrationDAO.MutationResult result) throws RequestActionException {
        switch (result.outcome()) {
            case "CHANGED" -> { }
            case "FORBIDDEN" -> throw new RequestActionException(RequestActionException.Reason.FORBIDDEN,
                    "Phiên quản trị không còn quyền thực hiện thao tác.");
            case "NOT_FOUND" -> throw new RequestActionException(RequestActionException.Reason.NOT_FOUND,
                    "Không tìm thấy Company hoặc Subscription.");
            case "CONFLICT" -> throw new RequestActionException(RequestActionException.Reason.CONFLICT,
                    "Subscription đã thay đổi trạng thái. Vui lòng tải lại trang.");
            default -> throw new IllegalStateException("Invalid company mutation result.");
        }
    }
}
