package com.hrms.controller.hr;

import com.hrms.api.ApiRoutes;
import com.hrms.dao.LeaveRequestDAO;
import com.hrms.model.LeavePolicy;
import com.hrms.model.LeaveRequest;
import com.hrms.service.LeaveService;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.Json;
import com.hrms.util.SecuritySupport;
import com.hrms.util.ValidationSupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@WebServlet(name="LeaveController", urlPatterns={
    "/leave/types", "/leave/policy", "/leave/my-requests", "/leave/submit",
    "/leave/pending-approvals", "/leave/all-requests",
    "/leave/api/types", "/leave/api/policy", "/leave/api/my-requests",
    "/leave/api/submit", "/leave/api/cancel", "/leave/api/pending-approvals", "/leave/api/approve",
    "/leave/api/reject", "/leave/api/all-requests", "/leave/api/detail"
})
public class LeaveController extends HttpServlet {
    private final LeaveService service = new LeaveService();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        handle(req, res, false);
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        handle(req, res, true);
    }
    private void handle(HttpServletRequest req, HttpServletResponse res, boolean post) throws ServletException, IOException {
        try {
            TenantPrincipal actor = SecuritySupport.getTenantPrincipal(req);
            if (actor == null) { res.sendError(401); return; }
            String path = req.getServletPath();
            if (post && !SecuritySupport.hasValidPublicCsrfToken(req)) throw new SecurityException("Phiên thao tác không hợp lệ. Vui lòng tải lại trang.");
            if (!path.contains("/api/")) {
                if (post) { res.sendError(405); return; }
                page(req, res, actor, path);
                return;
            }
            if (post) {
                switch (path) {
                    case "/leave/api/policy", "/leave/api/types" -> service.savePolicy(actor, req.getParameter("quotaMode"), number(req,"quotaDays",-1), number(req,"maxDaysPerRequest",-1));
                    case "/leave/api/submit" -> {
                        String[] dates = req.getParameterValues("dates");
                        List<LocalDate> parsed = dates == null ? List.of() : Arrays.stream(dates).map(LocalDate::parse).toList();
                        LeaveRequest result = service.submit(actor, parsed, req.getParameter("reason"));
                        json(res, Map.of("success",true,"leaveRequestId",result.getLeaveRequestId())); return;
                    }
                    case "/leave/api/approve", "/leave/api/reject" -> service.review(actor, number(req,"leaveRequestId",0), path.endsWith("approve"), req.getParameter("comment"));
                    case "/leave/api/cancel" -> service.cancel(actor, number(req,"leaveRequestId",0));
                    default -> { res.sendError(405); return; }
                }
                json(res, Map.of("success",true,"message","Đã cập nhật thành công."));
                return;
            }
            switch (path) {
                case "/leave/api/policy", "/leave/api/types" -> {
                    LeavePolicy policy = service.getPolicy(actor);
                    Map<String,Object> data = new LinkedHashMap<>();
                    data.put("success",true); data.put("today",service.tomorrow().minusDays(1)); data.put("earliestDate",service.tomorrow());
                    data.put("policy",policy == null ? null : Map.of("quotaMode",policy.quotaMode(),"quotaDays",policy.quotaDays(),"maxDaysPerRequest",policy.maxDaysPerRequest(),
                        "monthlyDays",java.util.stream.IntStream.rangeClosed(1,12).map(policy::daysForMonth).boxed().toList()));
                    json(res,data);
                }
                case "/leave/api/detail" -> json(res, Map.of("success",true,"data",toMap(actor,service.detail(actor,number(req,"leaveRequestId",0)))));
                case "/leave/api/my-requests", "/leave/api/pending-approvals", "/leave/api/all-requests" -> {
                    boolean own = path.endsWith("my-requests"), pending = path.endsWith("pending-approvals");
                    int page = Math.max(1,Math.min(100000,number(req,"page",1)));
                    List<LeaveRequest> requests = service.list(actor,own,pending,req.getParameter("status"),dateFilter(req),page);
                    json(res,Map.of("success",true,"page",page,"hasMore",requests.size()>20,
                        "data",requests.stream().limit(20).map(r -> toMap(actor,r)).toList()));
                }
                default -> res.sendError(404);
            }
        } catch (SecurityException ex) {
            res.setStatus(403); json(res,Map.of("success",false,"message",ex.getMessage()));
        } catch (IllegalArgumentException | java.time.DateTimeException ex) {
            res.setStatus(400); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write(ValidationSupport.errorJson(ex));
        } catch (SQLException ex) {
            getServletContext().log("Leave operation failed",ex);
            res.setStatus(500); json(res,Map.of("success",false,"message","Không thể xử lý dữ liệu nghỉ phép. Kiểm tra migration và thử lại."));
        }
    }

    private void page(HttpServletRequest req, HttpServletResponse res, TenantPrincipal actor, String path) throws ServletException, IOException {
        if (path.equals("/leave/types")) { res.sendRedirect(req.getContextPath()+"/leave/policy"); return; }
        if (path.equals("/leave/policy") && !"COMPANY_ADMIN".equals(actor.getRoleCode())) throw new SecurityException("Chỉ Company Admin được cấu hình quota.");
        if (Set.of("/leave/submit","/leave/my-requests").contains(path)
                && !Set.of("EMPLOYEE","MANAGER").contains(actor.getRoleCode())) throw new SecurityException("Chỉ nhân viên và Manager được dùng chức năng này.");
        if (Set.of("/leave/all-requests","/leave/pending-approvals").contains(path)
                && !Set.of("COMPANY_ADMIN","MANAGER").contains(actor.getRoleCode())) throw new SecurityException("Không có quyền duyệt đơn.");
        String view = switch(path) {
            case "/leave/policy" -> "/WEB-INF/views/comadmin-hr/leave-policy.jsp";
            case "/leave/submit" -> ApiRoutes.VIEW_LEAVE_SUBMIT;
            case "/leave/my-requests" -> ApiRoutes.VIEW_LEAVE_MY_REQUESTS;
            case "/leave/all-requests" -> "/WEB-INF/views/comadmin-hr/leave-all-requests.jsp";
            case "/leave/pending-approvals" -> ApiRoutes.VIEW_LEAVE_PENDING_APPROVALS;
            default -> null;
        };
        if (view == null) { res.sendError(404); return; }
        req.setAttribute("currentUser",actor);
        req.setAttribute("organizationCsrfToken",SecuritySupport.ensurePublicCsrfToken(req.getSession(true)));
        req.getRequestDispatcher(view).forward(req,res);
    }

    private Map<String,Object> toMap(TenantPrincipal actor, LeaveRequest r) {
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("leaveRequestId",r.getLeaveRequestId()); data.put("title",r.getTitle());
        data.put("employeeId",r.getEmployeeId()); data.put("employeeFullName",r.getEmployeeFullName());
        data.put("employeeCode",r.getEmployeeCode()); data.put("departmentName",r.getDepartmentName());
        data.put("positionTitle",r.getPositionTitle()); data.put("startDate",r.getStartDate()); data.put("endDate",r.getEndDate());
        data.put("dates",r.getDates()); data.put("totalDays",r.getTotalDays()); data.put("reason",r.getReason());
        data.put("status",r.getStatus()); data.put("createdAt",r.getCreatedAt()); data.put("reviewedAt",r.getReviewedAt());
        data.put("approverDisplayName",r.getApproverDisplayName()); data.put("approverComment",r.getApproverComment());
        data.put("legacy",r.isLegacy()); data.put("canReview",LeaveService.canReview(actor,r)&&"Pending".equals(r.getStatus()));
        data.put("canCancel",Objects.equals(actor.getEmployeeId(),r.getEmployeeId())&&"Pending".equals(r.getStatus()));
        return data;
    }
    private int number(HttpServletRequest req,String key,int fallback) { String s=req.getParameter(key); return s==null||s.isBlank()?fallback:Integer.parseInt(s); }
    private LeaveRequestDAO.DateFilter dateFilter(HttpServletRequest req) {
        String date = req.getParameter("date");
        if (date != null && !date.isBlank()) return new LeaveRequestDAO.DateFilter(LocalDate.parse(date), null, null);
        Integer year = req.getParameter("year") == null || req.getParameter("year").isBlank() ? null : number(req, "year", 0);
        Integer month = req.getParameter("month") == null || req.getParameter("month").isBlank() ? null : number(req, "month", 0);
        if (year != null && (year < 2000 || year > 9998)) throw new IllegalArgumentException("Năm không hợp lệ.");
        if (month != null && (month < 1 || month > 12)) throw new IllegalArgumentException("Tháng không hợp lệ.");
        return new LeaveRequestDAO.DateFilter(null, month, year);
    }
    private void json(HttpServletResponse res,Object data) throws IOException { res.setContentType("application/json;charset=UTF-8"); res.getWriter().write(Json.encode(data)); }
}
