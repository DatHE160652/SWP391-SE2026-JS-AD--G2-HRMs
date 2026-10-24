package com.hrms.controller;

import com.hrms.util.ValidationSupport;

import com.hrms.model.EmployeeDocument;
import com.hrms.service.DocumentService;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.SecuritySupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DocumentController extends HttpServlet {
    private final DocumentService documentService = new DocumentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            authorizeDocument(request);
            if ("/documents/me/download".equals(request.getServletPath())) {
                downloadOwnDocument(request, response);
                return;
            }

            if ("/documents/download".equals(request.getServletPath())) {
                downloadDocument(request, response);
                return;
            }

            if ("/documents/me".equals(request.getServletPath())) {
                TenantPrincipal principal = requireEmployeePrincipal(request);
                List<EmployeeDocument> documents = documentService.getDocumentsByEmployee(principal.getEmployeeId());
                writeJson(response, HttpServletResponse.SC_OK,
                        "{\"success\":true,\"data\":" + toJsonArray(documents) + "}");
                return;
            }

            int employeeId = parseRequiredInt(request.getParameter("employeeId"));
            List<EmployeeDocument> documents = documentService.getDocumentsByEmployee(employeeId);
            writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJsonArray(documents) + "}");
        } catch (SecurityException exception) {
            writeJson(response, 403, error(exception.getMessage()));
        } catch (SQLException exception) {
            log("Document read failed",exception);
            writeJson(response,500,error("Không tải được danh sách tài liệu."));
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, ValidationSupport.errorJson(e));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            authorizeDocument(request);
            if (!SecuritySupport.hasValidPublicCsrfToken(request)) throw new SecurityException("Phiên thao tác không hợp lệ. Vui lòng tải lại trang.");
            boolean success;
            String message;
            if (java.util.Set.of("/documents/deactivate", "/documents/activate",
                    "/documents/me/deactivate", "/documents/me/activate").contains(request.getServletPath())) {
                if (!SecuritySupport.hasValidPublicCsrfToken(request)) throw new SecurityException("Phiên thao tác không hợp lệ. Vui lòng tải lại trang.");
                TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
                EmployeeDocument document = documentService.getDocument(parseRequiredInt(request.getParameter("documentId")));
                if (request.getServletPath().startsWith("/documents/me/")) ensureOwnDocument(requireEmployeePrincipal(request), document);
                com.hrms.model.Employee owner = new com.hrms.service.EmployeeService()
                        .getEmployeeById(SecuritySupport.requireCompanyId(request), document.getEmployeeId());
                if (owner == null) throw new SecurityException("Không tìm thấy tài liệu trong công ty của bạn.");
                boolean active = request.getServletPath().endsWith("/activate");
                success = documentService.setDocumentActive(document.getDocumentId(), active,SecuritySupport.requireCompanyId(request),principal.getUserId());
                message = active ? "Đã kích hoạt tài liệu." : "Đã lưu trữ tài liệu.";
            } else if ("/documents/me/upload".equals(request.getServletPath())) {
                TenantPrincipal principal = requireEmployeePrincipal(request);
                Part filePart = request.getPart("file");
                success = documentService.uploadDocument(
                        principal.getEmployeeId(),
                        request.getParameter("documentName"),
                        request.getParameter("documentType"),
                        filePart,
                        getServletContext(),
                        principal.getUserId());
                message = success ? "Đã tải tài liệu lên." : "Không tải được tài liệu lên.";
            } else {
                TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
                int employeeId = parseRequiredInt(request.getParameter("employeeId"));
                Part filePart = request.getPart("file");
                success = documentService.uploadDocument(
                        employeeId,
                        request.getParameter("documentName"),
                        request.getParameter("documentType"),
                        filePart,
                        getServletContext(), principal.getUserId());
                message = success ? "Đã tải tài liệu lên." : "Không tải được tài liệu lên.";
            }

            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":" + success + ",\"message\":" + quote(message) + "}");
        } catch (SecurityException exception) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, "{\"success\":false,\"message\":" + quote(exception.getMessage()) + "}");
        } catch (SQLException exception) {
            log("Document write failed",exception);
            writeJson(response,500,error("Không lưu được thông tin tài liệu."));
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, ValidationSupport.errorJson(e));
        }
    }

    private void downloadOwnDocument(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        TenantPrincipal principal = requireEmployeePrincipal(request);
        EmployeeDocument document = documentService.downloadDocument(parseRequiredInt(request.getParameter("id")));
        ensureOwnDocument(principal, document);
        streamDocument(document, request, response);
    }

    private void downloadDocument(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        EmployeeDocument document = documentService.downloadDocument(parseRequiredInt(request.getParameter("id")));
        streamDocument(document, request, response);
    }

    private void streamDocument(EmployeeDocument document, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        File file = documentService.resolveDownloadFile(document, getServletContext());

        String contentType = com.hrms.util.FileUploadUtil.contentType(document.getFileName());
        response.setContentType(contentType == null ? "application/octet-stream" : contentType);
        String disposition = "true".equalsIgnoreCase(request.getParameter("inline")) ? "inline" : "attachment";
        response.setHeader("Content-Disposition", buildContentDisposition(disposition, document.getFileName()));
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentLengthLong(file.length());

        try (FileInputStream inputStream = new FileInputStream(file);
             OutputStream outputStream = response.getOutputStream()) {
            inputStream.transferTo(outputStream);
        }
    }

    private String buildContentDisposition(String disposition, String fileName) {
        String safeFileName = fileName == null || fileName.isBlank() ? "document" : fileName;
        String asciiFileName = safeFileName.replaceAll("[\\\\\\\"\\r\\n]", "_");
        String encodedFileName = URLEncoder.encode(safeFileName, StandardCharsets.UTF_8).replace("+", "%20");
        return disposition + "; filename=\"" + asciiFileName + "\"; filename*=UTF-8''" + encodedFileName;
    }

    private TenantPrincipal requireEmployeePrincipal(HttpServletRequest request) {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null || principal.getEmployeeId() == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập bằng tài khoản nhân viên.");
        }
        return principal;
    }

    private void ensureOwnDocument(TenantPrincipal principal, EmployeeDocument document) {
        if (principal.getEmployeeId() == null || document.getEmployeeId() != principal.getEmployeeId()) {
            throw new IllegalArgumentException("Không tìm thấy tài liệu.");
        }
    }

    private String toJsonArray(List<EmployeeDocument> documents) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < documents.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(toJson(documents.get(i)));
        }
        return json.append("]").toString();
    }

    private String toJson(EmployeeDocument document) {
        return "{"
                + "\"documentId\":" + document.getDocumentId() + ","
                + "\"employeeId\":" + document.getEmployeeId() + ","
                + "\"documentName\":" + quote(document.getDocumentName()) + ","
                + "\"documentType\":" + quote(document.getDocumentType()) + ","
                + "\"fileName\":" + quote(document.getFileName()) + ","
                + "\"mimeType\":" + quote(document.getMimeType()) + ","
                + "\"fileSizeBytes\":" + (document.getFileSizeBytes()==null?"null":document.getFileSizeBytes()) + ","
                + "\"uploadedBy\":" + nullableInteger(document.getUploadedBy()) + ","
                + "\"uploadedDate\":" + quote(document.getUploadedDate() == null ? null : document.getUploadedDate().toString()) + ","
                + "\"status\":" + quote(document.getStatus())
                + "}";
    }

    private void writeJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(json);
    }

    private String error(String message) {
        return "{\"success\":false,\"message\":" + quote(message) + "}";
    }

    private String quote(String value) {
        return ValidationSupport.quote(value);
    }

    private void authorizeDocument(HttpServletRequest request) throws SQLException {
        if (request.getServletPath().startsWith("/documents/me")) {
            requireEmployeePrincipal(request);
            return;
        }
        SecuritySupport.requireOrganizationEditor(request);
        int companyId = SecuritySupport.requireCompanyId(request);
        String employeeId = request.getParameter("employeeId");
        if (employeeId != null && new com.hrms.service.EmployeeService().getEmployeeById(companyId, parseRequiredInt(employeeId)) == null)
            throw new SecurityException("Không tìm thấy nhân viên trong công ty của bạn.");
        if (request.getServletPath().equals("/documents/download")) {
            EmployeeDocument document = documentService.getDocument(parseRequiredInt(request.getParameter("id")));
            if (new com.hrms.service.EmployeeService().getEmployeeById(companyId, document.getEmployeeId()) == null)
                throw new SecurityException("Không tìm thấy tài liệu trong công ty của bạn.");
        }
    }

    private String nullableInteger(Integer value) {
        return value == null ? "null" : value.toString();
    }

    private int parseRequiredInt(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập giá trị số bắt buộc.");
        }
        return Integer.parseInt(value);
    }
}
