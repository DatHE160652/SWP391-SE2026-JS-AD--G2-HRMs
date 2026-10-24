package com.hrms.controller;

import com.hrms.model.ResponsibilityLevel;
import com.hrms.service.ResponsibilityLevelService;
import com.hrms.service.FieldValidationException;
import com.hrms.util.SecuritySupport;
import com.hrms.util.ValidationSupport;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.stream.Collectors;

public class ResponsibilityLevelController extends HttpServlet {
    private final ResponsibilityLevelService service=new ResponsibilityLevelService();

    @Override
    protected void doGet(HttpServletRequest request,HttpServletResponse response) throws IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            SecuritySupport.requireOrganizationEditor(request);
            int companyId=SecuritySupport.requireCompanyId(request);
            if ("/responsibility-levels/detail".equals(request.getServletPath())) {
                ResponsibilityLevel level=service.getLevel(companyId,id(request.getParameter("id")));
                if (level==null) { write(response,404,error("Không tìm thấy cấp trách nhiệm.")); return; }
                write(response,200,"{\"success\":true,\"data\":"+json(level)+"}");
            } else {
                write(response,200,"{\"success\":true,\"data\":"+service.getLevels(companyId).stream()
                        .map(this::json).collect(Collectors.joining(",","[","]"))+"}");
            }
        } catch (SecurityException exception) { write(response,403,error(exception.getMessage())); }
        catch (IllegalArgumentException exception) { write(response,400,ValidationSupport.errorJson(exception)); }
        catch (SQLException exception) { databaseError(response,exception); }
    }

    @Override
    protected void doPost(HttpServletRequest request,HttpServletResponse response) throws IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            SecuritySupport.requireOrganizationEditor(request);
            int companyId=SecuritySupport.requireCompanyId(request);
            if (!SecuritySupport.hasValidPublicCsrfToken(request)) throw new SecurityException("Phiên thao tác không hợp lệ. Vui lòng tải lại trang.");
            switch(request.getServletPath()) {
                case "/responsibility-levels/create" -> service.createLevel(companyId,read(request,false));
                case "/responsibility-levels/update" -> service.updateLevel(companyId,read(request,true));
                case "/responsibility-levels/activate" -> {
                    ResponsibilityLevel level = service.getLevel(companyId, id(request.getParameter("responsibilityLevelId")));
                    if (level == null) throw new IllegalArgumentException("Không tìm thấy cấp trách nhiệm trong công ty của bạn.");
                    level.setActive(true);
                    service.updateLevel(companyId, level);
                }
                case "/responsibility-levels/deactivate" -> service.deactivateLevel(companyId,id(request.getParameter("responsibilityLevelId")));
                default -> throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
            write(response,200,"{\"success\":true,\"message\":\"Đã lưu cấp trách nhiệm.\"}");
        } catch (SecurityException exception) { write(response,403,error(exception.getMessage())); }
        catch (IllegalArgumentException exception) { write(response,400,ValidationSupport.errorJson(exception)); }
        catch (SQLException exception) { databaseError(response,exception); }
    }

    private ResponsibilityLevel read(HttpServletRequest request,boolean updating) {
        ResponsibilityLevel level=new ResponsibilityLevel();
        if (updating) level.setResponsibilityLevelId(id(request.getParameter("responsibilityLevelId")));
        level.setLevelCode(request.getParameter("levelCode")); level.setLevelName(request.getParameter("levelName"));
        try { level.setRankOrder(Integer.parseInt(request.getParameter("rankOrder"))); }
        catch (NumberFormatException exception) { throw new FieldValidationException("rankOrder","Thứ bậc phải là số nguyên không âm."); }
        String active=request.getParameter("isActive");
        if (active!=null && !"true".equals(active) && !"false".equals(active)) throw new FieldValidationException("isActive","Trạng thái không hợp lệ.");
        level.setActive(!"false".equals(active));
        if (request.getParameter("defaultAllowance")!=null) {
            if (!SecuritySupport.getTenantPrincipal(request).isCompanyAdministrator())
                throw new SecurityException("Chỉ quản trị viên công ty được cấu hình phụ cấp.");
            try { level.setDefaultAllowance(new java.math.BigDecimal(request.getParameter("defaultAllowance"))); }
            catch (NumberFormatException exception) { throw new FieldValidationException("defaultAllowance","Nhập phụ cấp không âm; dùng 0 nếu không có."); }
        }
        return level;
    }

    private int id(String value) {
        try { int parsed=Integer.parseInt(value); if (parsed<=0) throw new NumberFormatException(); return parsed; }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Mã cấp trách nhiệm không hợp lệ."); }
    }

    private String json(ResponsibilityLevel level) {
        return "{\"responsibilityLevelId\":"+level.getResponsibilityLevelId()+",\"levelCode\":"+ValidationSupport.quote(level.getLevelCode())
                +",\"levelName\":"+ValidationSupport.quote(level.getLevelName())+",\"rankOrder\":"+level.getRankOrder()
                +",\"defaultAllowance\":"+level.getDefaultAllowance()+",\"isActive\":"+level.isActive()+"}";
    }

    private String error(String message) { return "{\"success\":false,\"message\":"+ValidationSupport.quote(message)+"}"; }
    private void databaseError(HttpServletResponse response,SQLException exception) throws IOException {
        log("Responsibility level operation failed",exception);
        write(response,400,error("Không thể lưu cấp trách nhiệm. Kiểm tra mã, thứ bậc bị trùng hoặc tải lại trang."));
    }
    private void write(HttpServletResponse response,int status,String json) throws IOException {
        response.setStatus(status); response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8"); response.getWriter().write(json);
    }
}
