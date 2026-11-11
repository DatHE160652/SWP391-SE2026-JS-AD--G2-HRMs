package com.hrms.controller.hr;

import com.hrms.dao.EmployeePasswordResetDAO;
import com.hrms.service.EmployeePasswordResetService;
import com.hrms.util.Json;
import com.hrms.util.SecuritySupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class EmployeePasswordResetController extends HttpServlet {
    public static final String PAGE="/comadmin-hr/reset-employee-password";
    public static final String SEARCH="/comadmin-hr/reset-employee-password/search";
    public static final String RESET="/comadmin-hr/reset-employee-password/reset";
    private static final String PAGE_CONTEXT_ATTRIBUTE = "employeePasswordResetContext";
    private record PageContext(Integer userId, Integer companyId, String workspaceCode,
                               String credentialVersion, String token) {
        boolean matches(com.hrms.service.security.TenantPrincipal principal) {
            return java.util.Objects.equals(userId, principal.getUserId())
                    && java.util.Objects.equals(companyId, principal.getCompanyId())
                    && java.util.Objects.equals(workspaceCode, principal.getWorkspaceCode())
                    && java.util.Objects.equals(credentialVersion, principal.getCredentialVersion());
        }
    }
    private final EmployeePasswordResetService service;
    public EmployeePasswordResetController() { this(new EmployeePasswordResetService()); }
    public EmployeePasswordResetController(EmployeePasswordResetService service) { this.service=service; }
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response) throws ServletException,IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            var principal=SecuritySupport.getTenantPrincipal(request);
            service.requireEditor(principal);
            if(!PAGE.equals(request.getServletPath())) { error(response,400,"Thao tác này yêu cầu POST."); return; }
            HttpSession session=request.getSession();
            synchronized(session) {
                Object existing=session.getAttribute(PAGE_CONTEXT_ATTRIBUTE);
                PageContext context=existing instanceof PageContext saved && saved.matches(principal) ? saved
                        : new PageContext(principal.getUserId(),principal.getCompanyId(),principal.getWorkspaceCode(),
                                principal.getCredentialVersion(),java.util.UUID.randomUUID().toString());
                session.setAttribute(PAGE_CONTEXT_ATTRIBUTE,context);
                request.setAttribute("resetContextToken",context.token());
            }
            request.setAttribute("organizationCsrfToken",SecuritySupport.ensurePublicCsrfToken(session));
            request.getRequestDispatcher("/WEB-INF/views/comadmin-hr/reset-employee-password.jsp").forward(request,response);
        } catch(SecurityException exception) { error(response,403,"Bạn không có quyền truy cập trang này."); }
    }
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response) throws IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            var principal=SecuritySupport.getTenantPrincipal(request);
            service.requireEditor(principal);
            if(!SecuritySupport.hasValidPublicCsrfToken(request)) throw new SecurityException();
            HttpSession session=request.getSession(false);
            Object saved=session==null?null:session.getAttribute(PAGE_CONTEXT_ATTRIBUTE);
            String token=request.getParameter("resetContextToken");
            if(!(saved instanceof PageContext context) || !context.matches(principal) || token==null
                    || !java.security.MessageDigest.isEqual(context.token().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            token.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                error(response,409,"Tài khoản hoặc workspace đã thay đổi. Vui lòng tải lại trang Đặt lại mật khẩu trước khi tiếp tục.");
                return;
            }
            Object data;
            if(SEARCH.equals(request.getServletPath())) {
                data=service.search(principal,request.getParameter("email"),request.getParameter("identityNumber"));
            } else if(RESET.equals(request.getServletPath())) {
                String id=request.getParameter("employeeId");
                if(id==null || !id.matches("[1-9][0-9]{0,9}")) throw new IllegalArgumentException();
                data=service.reset(principal,Integer.parseInt(id),request.getParameter("version"));
            } else { error(response,400,"Thao tác không hợp lệ."); return; }
            write(response,200,Map.of("success",true,"data",data));
        } catch(SecurityException exception) { error(response,403,"Không có quyền hoặc phiên thao tác không hợp lệ. Vui lòng tải lại trang."); }
        catch(EmployeePasswordResetDAO.ConflictException exception) { error(response,409,exception.getMessage()); }
        catch(EmployeePasswordResetService.ConfigurationException exception) { error(response,500,exception.getMessage()); }
        catch(IllegalArgumentException exception) { error(response,400,"Dữ liệu không hợp lệ. Nhập email hoặc CCCD/CMND hợp lệ và tìm lại nhân viên."); }
        catch(SQLException exception) {
            boolean searching=SEARCH.equals(request.getServletPath());
            String sqlState=exception.getSQLState();
            if(sqlState==null || !sqlState.matches("[A-Za-z0-9]{5}")) sqlState="unknown";
            java.util.logging.Logger.getLogger(EmployeePasswordResetController.class.getName()).warning(
                    "Employee password reset " + (searching ? "search" : "mutation")
                    + " failed; SQLState=" + sqlState + "; errorCode=" + exception.getErrorCode());
            error(response,500,searching
                    ? "Không tìm kiếm được do lỗi cơ sở dữ liệu. Thao tác tìm kiếm chưa thay đổi mật khẩu. Vui lòng kiểm tra log máy chủ."
                    : "Không xác nhận được kết quả thao tác. Vui lòng tìm lại nhân viên trước khi tiếp tục; không tự động gửi lại yêu cầu.");
        }
        catch(RuntimeException exception) { error(response,500,"Không xử lý được yêu cầu. Vui lòng tìm lại nhân viên trước khi tiếp tục."); }
    }
    private static void error(HttpServletResponse response,int status,String message) throws IOException {
        write(response,status,Map.of("success",false,"message",message));
    }
    private static void write(HttpServletResponse response,int status,Object data) throws IOException {
        response.setStatus(status); response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8"); response.getWriter().write(Json.encode(data));
    }
}
