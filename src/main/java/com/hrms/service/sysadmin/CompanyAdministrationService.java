package com.hrms.service.sysadmin;

import com.hrms.dao.sysadmin.CompanyAdministrationDAO;
import com.hrms.model.CompanyAdministrationFilter;
import com.hrms.model.CompanyAdministrationView;

import java.sql.SQLException;
import java.util.List;

/** Read-only System Admin company visibility; subscription mutations are outside v1.3.7. */
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
}
