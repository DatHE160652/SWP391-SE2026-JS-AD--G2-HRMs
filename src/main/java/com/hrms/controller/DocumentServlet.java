package com.hrms.controller;

import com.hrms.model.EmployeeDocument;
import com.hrms.service.DocumentService;
import com.hrms.service.EmployeeHistoryService;
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

public class DocumentServlet extends HttpServlet {
    private final DocumentService documentService = new DocumentService();
    private final EmployeeHistoryService employeeHistoryService = new EmployeeHistoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
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
        } catch (SQLException | IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            boolean success;
            String message;
            if ("/documents/delete".equals(request.getServletPath())) {
                EmployeeDocument document = documentService.downloadDocument(parseRequiredInt(request.getParameter("documentId")));
                success = documentService.deleteDocument(document.getDocumentId());
                if (success) {
                    TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
                    logDocumentChange(principal, document.getEmployeeId(), "DELETE_DOCUMENT",
                            documentSummary(document), "-", request);
                }
                message = success ? "Document deleted successfully." : "No document record was changed.";
            } else if ("/documents/me/delete".equals(request.getServletPath())) {
                TenantPrincipal principal = requireEmployeePrincipal(request);
                EmployeeDocument document = documentService.downloadDocument(parseRequiredInt(request.getParameter("documentId")));
                ensureOwnDocument(principal, document);
                success = documentService.deleteDocument(document.getDocumentId());
                if (success) {
                    logDocumentChange(principal, document.getEmployeeId(), "DELETE_DOCUMENT",
                            documentSummary(document), "-", request);
                }
                message = success ? "Document deleted successfully." : "No document record was changed.";
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
                if (success) {
                    logDocumentChange(principal, principal.getEmployeeId(), "UPLOAD_DOCUMENT",
                            "-", request.getParameter("documentName"), request);
                }
                message = success ? "Document uploaded successfully." : "Document upload failed.";
            } else {
                TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
                int employeeId = parseRequiredInt(request.getParameter("employeeId"));
                Part filePart = request.getPart("file");
                success = documentService.uploadDocument(
                        employeeId,
                        request.getParameter("documentName"),
                        request.getParameter("documentType"),
                        filePart,
                        getServletContext());
                if (success) {
                    logDocumentChange(principal, employeeId, "UPLOAD_DOCUMENT",
                            "-", request.getParameter("documentName"), request);
                }
                message = success ? "Document uploaded successfully." : "Document upload failed.";
            }

            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":" + success + ",\"message\":" + quote(message) + "}");
        } catch (SQLException | IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
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

        String contentType = getServletContext().getMimeType(file.getName());
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
        String asciiFileName = safeFileName.replace("\\", "_").replace("\"", "_");
        String encodedFileName = URLEncoder.encode(safeFileName, StandardCharsets.UTF_8).replace("+", "%20");
        return disposition + "; filename=\"" + asciiFileName + "\"; filename*=UTF-8''" + encodedFileName;
    }

    private TenantPrincipal requireEmployeePrincipal(HttpServletRequest request) {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null || principal.getEmployeeId() == null) {
            throw new IllegalArgumentException("Employee session is required.");
        }
        return principal;
    }

    private void ensureOwnDocument(TenantPrincipal principal, EmployeeDocument document) {
        if (principal.getEmployeeId() == null || document.getEmployeeId() != principal.getEmployeeId()) {
            throw new IllegalArgumentException("Document not found");
        }
    }

    private void logDocumentChange(TenantPrincipal principal, int employeeId, String action,
                                   String oldValue, String newValue, HttpServletRequest request) throws SQLException {
        employeeHistoryService.logEmployeeChange(
                principal == null ? null : principal.getCompanyId(),
                principal == null ? null : principal.getUserId(),
                employeeId,
                action,
                oldValue,
                newValue,
                request.getRemoteAddr());
    }

    private String documentSummary(EmployeeDocument document) {
        return "Document: " + nullToDash(document.getDocumentName())
                + "; Type: " + nullToDash(document.getDocumentType())
                + "; File: " + nullToDash(document.getFileName());
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
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
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String nullableInteger(Integer value) {
        return value == null ? "null" : value.toString();
    }

    private int parseRequiredInt(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required numeric value is missing.");
        }
        return Integer.parseInt(value);
    }
}
