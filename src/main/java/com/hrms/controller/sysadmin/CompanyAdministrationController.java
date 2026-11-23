package com.hrms.controller.sysadmin;

import com.hrms.model.CompanyAdministrationFilter;
import com.hrms.model.CompanyAdministrationView;
import com.hrms.service.sysadmin.CompanyAdministrationService;
import com.hrms.service.sysadmin.RegistrationRequestService;
import com.hrms.util.SecuritySupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Read-only System Admin Company list/detail controller. */
public class CompanyAdministrationController extends HttpServlet {
    private final CompanyAdministrationService companyService;
    private final RegistrationRequestService registrationRequestService;

    public CompanyAdministrationController() {
        this(new CompanyAdministrationService(), new RegistrationRequestService());
    }

    CompanyAdministrationController(CompanyAdministrationService companyService,
                                    RegistrationRequestService registrationRequestService) {
        this.companyService = companyService;
        this.registrationRequestService = registrationRequestService;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        String path = request.getServletPath();
        if ("/systemadmin/company".equals(path)) {
            showList(request, response);
            return;
        }
        if ("/systemadmin/company-details".equals(path)) {
            showDetails(request, response);
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }


    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            CompanyAdministrationFilter filter = companyService.createFilter(
                    request.getParameter("q"), request.getParameter("status"),
                    request.getParameter("region"), request.getParameter("sort"));
            setNavigation(request);
            request.setAttribute("allCompanies", companyService.getCompanies());
            request.setAttribute("companies", companyService.getCompanies(filter));
            request.setAttribute("filter", filter);
            request.getRequestDispatcher("/WEB-INF/views/sysadmin/company-list.jsp").forward(request, response);
        } catch (IllegalArgumentException exception) {
            showErrorWithStatus(request, response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            showError(request, response, "Không thể tải danh sách Company.");
        }
    }

    private void showDetails(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int companyId;
        try {
            companyId = parsePositiveId(request.getParameter("id"));
        } catch (IllegalArgumentException exception) {
            showErrorWithStatus(request, response, HttpServletResponse.SC_BAD_REQUEST, "Mã Company không hợp lệ.");
            return;
        }
        try {
            CompanyAdministrationView company = companyService.getCompany(companyId);
            if (company == null) {
                showErrorWithStatus(request, response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy Company.");
                return;
            }
            setNavigation(request);
            request.setAttribute("company", company);
            request.getRequestDispatcher("/WEB-INF/views/sysadmin/company-details.jsp").forward(request, response);
        } catch (SQLException exception) {
            showError(request, response, "Không thể tải chi tiết Company.");
        }
    }

    private void setNavigation(HttpServletRequest request) throws SQLException {
        SecuritySupport.ensureCsrfToken(request.getSession(true));
        request.setAttribute("activeNav", "companies");
        request.setAttribute("pendingCount", registrationRequestService.getStats(false).getPendingCount());
    }

    private int parsePositiveId(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException();
        try {
            int id = Integer.parseInt(value);
            if (id <= 0) throw new IllegalArgumentException();
            return id;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException();
        }
    }



    private void showError(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        showErrorWithStatus(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
    }

    private void showErrorWithStatus(HttpServletRequest request, HttpServletResponse response,
                                     int status, String message) throws ServletException, IOException {
        response.setStatus(status);
        request.setAttribute("errorMessage", message);
        request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
    }
}
