package com.hrms.service;

import com.hrms.dao.CompanyAdministrationDAO;
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

    public List<CompanyAdministrationView> getCompanies(CompanyAdministrationFilter filter) throws SQLException {
        return companyDAO.findAll(filter);
    }

    public CompanyAdministrationView getCompany(int companyId) throws SQLException {
        return companyDAO.findById(companyId);
    }

    public void suspend(int companyId, int actorId, String reason, String ipAddress)
            throws SQLException, RequestActionException {
        if (companyId <= 0 || actorId <= 0) throw new IllegalArgumentException("Mã Company hoặc người thực hiện không hợp lệ.");
        String normalizedReason = normalizeReason(reason);
        requireSuccess(companyDAO.suspend(companyId, actorId, normalizedReason, ipAddress));
    }

    public void activate(int companyId, int actorId, String ipAddress)
            throws SQLException, RequestActionException {
        if (companyId <= 0 || actorId <= 0) throw new IllegalArgumentException("Mã Company hoặc người thực hiện không hợp lệ.");
        requireSuccess(companyDAO.activate(companyId, actorId, ipAddress));
    }

    private String normalizeReason(String reason) {
        if (reason == null) throw new IllegalArgumentException("Lý do tạm ngưng là bắt buộc.");
        String normalized = reason.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) throw new IllegalArgumentException("Lý do tạm ngưng là bắt buộc.");
        if (normalized.length() > 500) throw new IllegalArgumentException("Lý do tạm ngưng không được vượt quá 500 ký tự.");
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
