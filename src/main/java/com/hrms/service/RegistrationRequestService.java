package com.hrms.service;

import com.hrms.dao.RegistrationRequestDAO;
import com.hrms.model.RegistrationRequest;
import com.hrms.model.RegistrationRequestFilter;
import com.hrms.model.RegistrationRequestPage;
import com.hrms.model.RegistrationRequestStats;

import java.sql.SQLException;
import java.util.List;

public class RegistrationRequestService {
    private final RegistrationRequestDAO registrationRequestDAO;

    public RegistrationRequestService() {
        this(new RegistrationRequestDAO());
    }

    public RegistrationRequestService(RegistrationRequestDAO registrationRequestDAO) {
        this.registrationRequestDAO = registrationRequestDAO;
    }

    public List<RegistrationRequest> getVisibleRequests() throws SQLException {
        return registrationRequestDAO.findVisibleRequests();
    }

    public RegistrationRequestFilter createFilter(String q, String status, String region, String date,
                                                   String page, String size) {
        return new RegistrationRequestFilter(q, status, region, date, page, size);
    }

    public RegistrationRequestPage getPage(RegistrationRequestFilter filter) throws SQLException {
        return registrationRequestDAO.findPage(filter);
    }

    public RegistrationRequestStats getStats(boolean includePlatformCounts) throws SQLException {
        return registrationRequestDAO.getStats(includePlatformCounts);
    }

    public List<RegistrationRequest> getRecentPending() throws SQLException {
        return registrationRequestDAO.findRecentPending();
    }

    public RegistrationRequest getVisibleRequest(int requestId) throws SQLException {
        return registrationRequestDAO.findVisibleById(requestId);
    }
}
