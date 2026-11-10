package com.hrms.controller.importdata;

import com.hrms.model.ImportExecuteResult;
import com.hrms.model.ImportPreviewResult;
import com.hrms.model.ImportRowResult;
import com.hrms.service.DataImportExcelService;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.SecuritySupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Controller for Excel Data Import:
 * - GET  /data/import/template : Download template Excel
 * - POST /data/import/preview  : Upload & validate file, returning preview JSON
 * - POST /data/import/execute  : Commit validated data into database
 */
@WebServlet(name = "DataImportController", urlPatterns = {
        "/data/import/template",
        "/data/import/preview",
        "/data/import/execute"
})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1 MB
        maxFileSize = 10 * 1024 * 1024,       // 10 MB
        maxRequestSize = 20 * 1024 * 1024     // 20 MB
)
public class DataImportController extends HttpServlet {

    private final DataImportExcelService importService;

    public DataImportController() {
        this.importService = new DataImportExcelService();
    }

    public DataImportController(DataImportExcelService importService) {
        this.importService = importService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if ("/data/import/template".equals(path)) {
            handleDownloadTemplate(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        try {
            ensureAuthorized(request);
            switch (path) {
                case "/data/import/preview" -> handlePreview(request, response);
                case "/data/import/execute" -> handleExecute(request, response);
                default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SecurityException e) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                    "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Lỗi máy chủ: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    // =========================================================================
    // HANDLERS
    // =========================================================================

    private void handleDownloadTemplate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        int companyId = principal != null && principal.getCompanyId() != null ? principal.getCompanyId() : 0;
        String companyName = principal != null ? principal.getCompanyName() : "HRMs Cloud";

        String type = request.getParameter("type");
        String normType = DataImportExcelService.normalizeType(type);
        String filename = "mau_nhap_" + normType.toLowerCase() + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        importService.generateTemplate(normType, companyName, response.getOutputStream());
    }

    private void handlePreview(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        int companyId = principal.getCompanyId();

        String type = request.getParameter("type");
        Part filePart = request.getPart("file");
        validateFilePart(filePart);

        try (InputStream is = filePart.getInputStream()) {
            ImportPreviewResult result = importService.previewImport(type, companyId, is);
            writeJson(response, HttpServletResponse.SC_OK, toJson(result));
        }
    }

    private void handleExecute(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        int companyId = principal.getCompanyId();
        Integer userId = principal.getUserId();

        String type = request.getParameter("type");
        Part filePart = request.getPart("file");
        validateFilePart(filePart);

        try (InputStream is = filePart.getInputStream()) {
            ImportExecuteResult result = importService.executeImport(type, companyId, userId, is);
            int status = result.isSuccess() ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST;
            writeJson(response, status, toJson(result));
        }
    }

    // =========================================================================
    // SECURITY & VALIDATION
    // =========================================================================

    private void ensureAuthorized(HttpServletRequest request) {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null || principal.getCompanyId() == null) {
            throw new SecurityException("Vui lòng đăng nhập với tài khoản công ty để sử dụng tính năng này.");
        }
        if (!principal.isCompanyAdministrator() && !"MANAGER".equalsIgnoreCase(principal.getRoleCode())) {
            throw new SecurityException("Chỉ Quản trị viên công ty hoặc Quản lý mới có quyền nhập dữ liệu Excel.");
        }
    }

    private void validateFilePart(Part filePart) {
        if (filePart == null || filePart.getSize() == 0) {
            throw new IllegalArgumentException("Vui lòng chọn một tệp Excel để tải lên.");
        }
        if (filePart.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Dung lượng tệp vượt quá giới hạn 10 MB.");
        }
        String fileName = filePart.getSubmittedFileName();
        if (fileName == null || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
            throw new IllegalArgumentException("Định dạng tệp không hợp lệ. Vui lòng chỉ tải lên tệp .xlsx hoặc .xls.");
        }
    }

    // =========================================================================
    // JSON SERIALIZATION
    // =========================================================================

    private void writeJson(HttpServletResponse response, int statusCode, String json) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(json);
    }

    private String toJson(ImportPreviewResult r) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"success\":").append(r.getGlobalErrors().isEmpty()).append(",");
        sb.append("\"type\":\"").append(escapeJson(r.getType())).append("\",");
        sb.append("\"totalRows\":").append(r.getTotalRows()).append(",");
        sb.append("\"validRows\":").append(r.getValidRows()).append(",");
        sb.append("\"errorRows\":").append(r.getErrorRows()).append(",");
        sb.append("\"canProceed\":").append(r.isCanProceed()).append(",");

        sb.append("\"globalErrors\":[");
        for (int i = 0; i < r.getGlobalErrors().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(r.getGlobalErrors().get(i))).append("\"");
        }
        sb.append("],");

        sb.append("\"columnHeaders\":[");
        for (int i = 0; i < r.getColumnHeaders().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(r.getColumnHeaders().get(i))).append("\"");
        }
        sb.append("],");

        sb.append("\"previewRows\":[");
        int limit = Math.min(r.getPreviewRows().size(), 50);
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(",");
            ImportRowResult row = r.getPreviewRows().get(i);
            sb.append("{");
            sb.append("\"rowNumber\":").append(row.getRowNumber()).append(",");
            sb.append("\"valid\":").append(row.isValid()).append(",");
            sb.append("\"errorMessage\":").append(row.getErrorMessage() != null ? "\"" + escapeJson(row.getErrorMessage()) + "\"" : "null").append(",");
            sb.append("\"data\":{");
            int c = 0;
            for (Map.Entry<String, String> e : row.getData().entrySet()) {
                if (c++ > 0) sb.append(",");
                sb.append("\"").append(escapeJson(e.getKey())).append("\":\"").append(escapeJson(e.getValue())).append("\"");
            }
            sb.append("}}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String toJson(ImportExecuteResult r) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"success\":").append(r.isSuccess()).append(",");
        sb.append("\"message\":\"").append(escapeJson(r.getMessage())).append("\",");
        sb.append("\"totalProcessed\":").append(r.getTotalProcessed()).append(",");
        sb.append("\"insertedCount\":").append(r.getInsertedCount()).append(",");
        sb.append("\"updatedCount\":").append(r.getUpdatedCount()).append(",");
        sb.append("\"failedCount\":").append(r.getFailedCount()).append(",");
        sb.append("\"errorMessages\":[");
        for (int i = 0; i < r.getErrorMessages().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(r.getErrorMessages().get(i))).append("\"");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

