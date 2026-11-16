package com.hrms.controller.sysadmin;

import com.hrms.dao.sysadmin.PlanDAO;
import com.hrms.dao.sysadmin.RegistrationRequestDAO;
import com.hrms.model.Plan;
import com.hrms.service.security.SystemAdminPrincipal;
import com.hrms.util.SecuritySupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/** System Admin CRUD controller for platform subscription Plans. */
public class PlanController extends HttpServlet {
    private static final String FLASH_MESSAGE = "planActionMessage";
    private static final String FLASH_ERROR = "planActionError";
    private final PlanDAO planDAO = new PlanDAO();
    private final RegistrationRequestDAO requestDAO = new RegistrationRequestDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            List<Plan> plans = planDAO.findAll();
            request.setAttribute("plans", plans);
            request.setAttribute("activeNav", "plans");
            request.setAttribute("pendingCount", requestDAO.getStats(false).getPendingCount());
            moveFlash(request, FLASH_MESSAGE, "actionMessage");
            moveFlash(request, FLASH_ERROR, "actionError");
            SecuritySupport.ensureCsrfToken(request.getSession(true));
            request.getRequestDispatcher("/WEB-INF/views/sysadmin/plan-list.jsp").forward(request, response);
        } catch (SQLException ex) {
            getServletContext().log("Failed to load plans", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không tải được danh sách gói dịch vụ.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        SecuritySupport.applyNoStore(response::setHeader);
        if (!SecuritySupport.isSystemAdmin(request) || !SecuritySupport.hasValidCsrfToken(request)
                || SecuritySupport.getPrincipal(request).getUserId() == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên thao tác không hợp lệ.");
            return;
        }
        SystemAdminPrincipal principal = SecuritySupport.getPrincipal(request);
        String action = request.getParameter("action");
        try {
            if ("toggle".equals(action)) {
                int planId = parsePositiveInt(request.getParameter("planId"), "Mã gói không hợp lệ.");
                boolean active = "true".equalsIgnoreCase(request.getParameter("active"));
                planDAO.setActive(planId, active, principal.getUserId(), request.getRemoteAddr());
                request.getSession(true).setAttribute(FLASH_MESSAGE,
                        active ? "Đã kích hoạt lại gói dịch vụ." : "Đã tạm ẩn gói dịch vụ.");
            } else {
                String planName = request.getParameter("planName");
                if (planName == null || planName.trim().isEmpty() || planName.trim().length() > 100) {
                    throw new IllegalArgumentException("Tên gói dịch vụ phải từ 1 đến 100 ký tự.");
                }
                int maxAccounts = parsePositiveInt(request.getParameter("maxAccounts"), "Số tài khoản tối đa phải lớn hơn 0.");
                int maxManagers = Math.min(3, maxAccounts);
                BigDecimal price;
                try {
                    price = new BigDecimal(request.getParameter("price").trim());
                    if (price.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Giá gói dịch vụ phải là số >= 0.");
                }
                String billingCycle = "YEARLY".equalsIgnoreCase(request.getParameter("billingCycle")) ? "YEARLY" : "MONTHLY";
                if ("update".equals(action)) {
                    int planId = parsePositiveInt(request.getParameter("planId"), "Mã gói không hợp lệ.");
                    planDAO.update(planId, planName.trim(), maxAccounts, maxManagers, price, billingCycle,
                            principal.getUserId(), request.getRemoteAddr());
                    request.getSession(true).setAttribute(FLASH_MESSAGE, "Đã cập nhật gói dịch vụ " + planName.trim() + ".");
                } else {
                    planDAO.create(planName.trim(), maxAccounts, maxManagers, price, billingCycle,
                            principal.getUserId(), request.getRemoteAddr());
                    request.getSession(true).setAttribute(FLASH_MESSAGE, "Đã tạo gói dịch vụ mới " + planName.trim() + ".");
                }
            }
        } catch (IllegalArgumentException ex) {
            request.getSession(true).setAttribute(FLASH_ERROR, ex.getMessage());
        } catch (SQLException ex) {
            String msg = (ex.getErrorCode() == 2601 || ex.getErrorCode() == 2627)
                    ? "Tên gói dịch vụ đã tồn tại."
                    : "Không thể lưu gói dịch vụ. Vui lòng thử lại.";
            request.getSession(true).setAttribute(FLASH_ERROR, msg);
        }
        response.sendRedirect(request.getContextPath() + "/systemadmin/plans");
    }

    private static int parsePositiveInt(String raw, String errorMessage) {
        try {
            int val = Integer.parseInt(raw == null ? "" : raw.trim());
            if (val <= 0) throw new IllegalArgumentException(errorMessage);
            return val;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static void moveFlash(HttpServletRequest request, String sessionKey, String requestKey) {
        if (request.getSession(false) == null) return;
        Object val = request.getSession(false).getAttribute(sessionKey);
        if (val != null) {
            request.setAttribute(requestKey, val);
            request.getSession(false).removeAttribute(sessionKey);
        }
    }
}
