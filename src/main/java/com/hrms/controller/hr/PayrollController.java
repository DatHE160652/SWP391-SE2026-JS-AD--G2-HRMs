package com.hrms.controller.hr;

import com.hrms.dao.PayrollDAO;
import com.hrms.model.PayrollPeriod;
import com.hrms.service.*;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.PayrollJson;
import com.hrms.util.SecuritySupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

import static com.hrms.util.PayrollJson.object;

/** Payroll management and personal payslips share scoped services, never client tenant IDs. */
public class PayrollController extends HttpServlet {
    private final PayrollDAO dao;
    private final PayrollService service;
    private final PayrollExcelService excel=new PayrollExcelService();

    public PayrollController() { this(new PayrollDAO()); }

    PayrollController(PayrollDAO dao) {
        this.dao=dao;
        this.service=new PayrollService(dao);
    }

    private String path(HttpServletRequest request) {
        return request.getServletPath()+(request.getPathInfo()==null?"":request.getPathInfo());
    }

    @Override
    protected void doGet(HttpServletRequest request,HttpServletResponse response) throws ServletException,IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        TenantPrincipal p=SecuritySupport.getTenantPrincipal(request);
        String path=path(request);
        try {
            if(path.startsWith("/payroll")) PayrollService.requirePayrollAccess(p);
            else PayrollService.ownEmployee(p);
            switch(path) {
                case "/payroll", "/payroll/salaries" -> {
                    preparePage(request,p);request.setAttribute("salaryPage",path.endsWith("/salaries"));
                    boolean slips=path.equals("/payroll") && "payslips".equals(request.getParameter("view"));
                    request.setAttribute("adminView",slips);
                    request.getRequestDispatcher(slips?"/WEB-INF/views/employee/payslips.jsp":
                            "/WEB-INF/views/comadmin-hr/payroll.jsp").forward(request,response);
                }
                case "/payroll/api/periods" -> ok(response,service.periods(p).stream().map(this::periodJson).toList());
                case "/payroll/api/period" -> {
                    int id=id(request,"periodId");
                    if ("payslips".equals(request.getParameter("view")))
                        ok(response,object("period",periodJson(service.period(p,id)),"slips",service.periodPayslips(p,id)));
                    else ok(response,object("period",periodJson(service.period(p,id)),"rows",dao.periodRows(p.getCompanyId(),id),
                            "adjustments",dao.adjustmentRows(p.getCompanyId(),id)));
                }
                case "/payroll/api/salaries" -> ok(response,dao.salaries(p.getCompanyId()));
                case "/payroll/api/preview" -> ok(response,service.preview(p,id(request,"periodId")));
                case "/payroll/api/template" -> {
                    int id=id(request,"periodId");
                    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    response.setHeader("Content-Disposition","attachment; filename=payroll-adjustments-"+id+".xlsx");
                    excel.template(dao.adjustmentRows(p.getCompanyId(),id),response.getOutputStream());
                }
                case "/payroll/payslip" -> {
                    request.setAttribute("slip",service.payslip(p,longId(request,"id"),true));
                    preparePage(request,p);request.setAttribute("adminView",true);
                    request.getRequestDispatcher("/WEB-INF/views/employee/payslip-detail.jsp").forward(request,response);
                }
                case "/payslips" -> {
                    preparePage(request,p);request.setAttribute("adminView",false);
                    request.getRequestDispatcher("/WEB-INF/views/employee/payslips.jsp").forward(request,response);
                }
                case "/payslips/view" -> {
                    request.setAttribute("slip",service.payslip(p,longId(request,"id"),false));
                    preparePage(request,p);request.setAttribute("adminView",false);
                    request.getRequestDispatcher("/WEB-INF/views/employee/payslip-detail.jsp").forward(request,response);
                }
                case "/payslips/api/slips" -> ok(response,service.ownPayslips(p));
                case "/payslips/api/notifications" -> ok(response,dao.notifications(p.getCompanyId(),PayrollService.ownEmployee(p)));
                default -> throw new PayrollException(404,"Không tìm thấy trang.");
            }
        } catch(PayrollException e) { failure(request,response,e); }
        catch(SQLException e) { databaseFailure(request,response,e); }
    }

    @Override
    protected void doPost(HttpServletRequest request,HttpServletResponse response) throws ServletException,IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        TenantPrincipal p=SecuritySupport.getTenantPrincipal(request);
        try {
            PayrollService.requireTenant(p);
            if(!SecuritySupport.hasValidPayrollCsrfToken(request)) throw new PayrollException(403,"Phiên thao tác không hợp lệ. Hãy tải lại trang.");
            String path=path(request);
            if(path.equals("/payslips/api/read")) {
                dao.readNotification(p.getCompanyId(),PayrollService.ownEmployee(p),longId(request,"notificationId"));
                message(response,"Đã đánh dấu thông báo là đã đọc."); return;
            }
            PayrollService.requirePayrollAccess(p);
            switch(path) {
                case "/payroll/api/create" -> {
                    int id=service.create(p,request.getParameter("periodName"),id(request,"month"),id(request,"year"));
                    response.setStatus(HttpServletResponse.SC_CREATED);
                    json(response,object("success",true,"message","Đã tạo kỳ lương Draft.","periodId",id));
                }
                case "/payroll/api/salary" -> {
                    service.setSalary(p,id(request,"employeeId"),request.getParameter("baseSalary"),request.getParameter("effectiveFrom"));
                    message(response,"Đã lưu lịch sử lương theo ngày hiệu lực. Các kỳ chưa duyệt bị ảnh hưởng đã về Draft để tính lại.");
                }
                case "/payroll/api/adjustment" -> {
                    boolean changed=service.adjustment(p,id(request,"periodId"),id(request,"employeeId"),request.getParameter("bonus"),request.getParameter("deduction"),request.getParameter("note"));
                    json(response,object("success",true,"changed",changed,"message",changed?
                            "Đã lưu điều chỉnh. Kỳ đã về Draft; kết quả cũ được xóa để tính và duyệt lại.":
                            "Dữ liệu điều chỉnh không thay đổi. Trạng thái kỳ lương được giữ nguyên."));
                }
                case "/payroll/api/import", "/payroll/api/import-preview" -> {
                    Part file=request.getPart("file");
                    if(file==null || file.getSize()==0 || file.getSize()>5*1024*1024)
                        throw new PayrollException(400,"Chọn file Excel tối đa 5 MB.");
                    String name=file.getSubmittedFileName()==null?"":file.getSubmittedFileName().toLowerCase(Locale.ROOT);
                    if(!name.endsWith(".xlsx") && !name.endsWith(".xls")) throw new PayrollException(400,"Chỉ hỗ trợ Excel .xlsx hoặc .xls.");
                    java.util.List<com.hrms.model.PayrollAdjustmentInput> rows;
                    try(var input=file.getInputStream()) { rows=excel.read(input); }
                    int periodId=id(request,"periodId");
                    if(path.endsWith("import-preview")) {
                        PayrollPeriod period=dao.findPeriod(p.getCompanyId(),periodId);
                        if(period.isClosed() || "Approved".equals(period.getStatus()) || period.getPublishedAt()!=null)
                            throw new PayrollException(409,"Kỳ đã đóng hoặc phê duyệt. Hãy mở lại kỳ chưa công bố để chỉnh sửa.");
                        java.util.Set<String> codes=dao.adjustmentRows(p.getCompanyId(),periodId).stream()
                                .map(row->row.employeeCode().toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
                        java.util.List<String> errors=rows.stream().filter(row->!codes.contains(row.employeeCode().toUpperCase(Locale.ROOT)))
                                .map(row->"Dòng "+row.rowNumber()+": mã "+row.employeeCode()+" không thuộc công ty hoặc không có trong kỳ lương.").toList();
                        if(!errors.isEmpty()) throw new PayrollException(400,"Chưa nhập dữ liệu. Hãy sửa các dòng lỗi.",errors);
                        ok(response,rows);
                    } else {
                        int count=service.importRows(p,periodId,rows);
                        message(response,"Đã nhập "+count+" dòng điều chỉnh. Hãy tính lại bảng lương.");
                    }
                }
                case "/payroll/api/calculate" -> {
                    int count=service.calculate(p,id(request,"periodId"));message(response,"Đã tính lương cho "+count+" nhân viên.");
                }
                case "/payroll/api/review", "/payroll/api/approve", "/payroll/api/close", "/payroll/api/reopen" -> {
                    String action=path.substring(path.lastIndexOf('/')+1);
                    service.transition(p,id(request,"periodId"),action);
                    message(response,switch(action) {
                        case "review" -> "Đã hoàn tất rà soát bảng lương.";
                        case "approve" -> "Đã phê duyệt bảng lương. Bạn có thể công bố phiếu lương.";
                        case "close" -> "Đã đóng kỳ lương.";
                        default -> "Đã mở lại kỳ về Draft. Hãy tính và duyệt lại.";
                    });
                }
                case "/payroll/api/publish" -> {
                    int count=service.publish(p,id(request,"periodId"));
                    message(response,count==0?"Kỳ này đã công bố; không tạo thêm phiếu hoặc thông báo.":"Đã công bố "+count+" phiếu lương và gửi thông báo trong hệ thống.");
                }
                default -> throw new PayrollException(404,"Không tìm thấy thao tác.");
            }
        } catch(PayrollException e) { failure(request,response,e); }
        catch(SQLException e) { databaseFailure(request,response,e); }
        catch(IllegalStateException e) { failure(request,response,new PayrollException(400,"File hoặc yêu cầu vượt quá giới hạn cho phép.")); }
        catch(IOException e) {
            if(response.isCommitted()) throw e;
            failure(request,response,new PayrollException(400,"Không đọc được file Excel. Hãy kiểm tra file và thử lại."));
        }
    }

    private void preparePage(HttpServletRequest request,TenantPrincipal principal) {
        request.setAttribute("payrollPrincipal",principal);
        request.setAttribute("payrollCsrf",SecuritySupport.ensurePayrollCsrfToken(request.getSession()));
    }
    private Object periodJson(PayrollPeriod p) {
        return object("payrollPeriodId",p.getPayrollPeriodId(),"periodName",p.getPeriodName(),"month",p.getMonth(),"year",p.getYear(),
                "status",p.getStatus(),"closed",p.isClosed(),"publishedAt",p.getPublishedAt(),"approvedAt",p.getApprovedAt(),
                "reviewedByUserId",p.getReviewedByUserId(),"reviewedAt",p.getReviewedAt(),"reviewedByName",p.getReviewedByName(),
                "approvedByUserId",p.getApprovedByUserId(),"approvedByName",p.getApprovedByName(),"createdAt",p.getCreatedAt(),
                "employeeCount",p.getEmployeeCount(),"totalIncome",p.getTotalIncome(),"totalDeduction",p.getTotalDeduction(),"totalNetSalary",p.getTotalNetSalary());
    }
    private int id(HttpServletRequest request,String name) {
        long value=longId(request,name);
        if(value>Integer.MAX_VALUE) throw new PayrollException(400,"Giá trị "+name+" không hợp lệ.");return (int)value;
    }
    private long longId(HttpServletRequest request,String name) {
        try { long value=Long.parseLong(request.getParameter(name));if(value<1) throw new NumberFormatException();return value; }
        catch(NumberFormatException e) { throw new PayrollException(400,"Giá trị "+name+" không hợp lệ."); }
    }
    private void ok(HttpServletResponse response,Object data) throws IOException { json(response,object("success",true,"data",data)); }
    private void message(HttpServletResponse response,String message) throws IOException { json(response,object("success",true,"message",message)); }
    private void json(HttpServletResponse response,Object value) throws IOException {
        response.setCharacterEncoding("UTF-8");response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(PayrollJson.encode(value));
    }
    private void failure(HttpServletRequest request,HttpServletResponse response,PayrollException e) throws IOException {
        if(path(request).contains("/api/")) {
            response.setStatus(e.getStatus());json(response,object("success",false,"message",e.getMessage(),"errors",e.getErrors()));
        } else response.sendError(e.getStatus(),e.getMessage());
    }
    private void databaseFailure(HttpServletRequest request,HttpServletResponse response,SQLException e) throws IOException {
        getServletContext().log("Payroll database failure",e);
        failure(request,response,new PayrollException(500,"Chưa thể xử lý bảng lương. Vui lòng kiểm tra kết nối và cấu hình cơ sở dữ liệu."));
    }
}
