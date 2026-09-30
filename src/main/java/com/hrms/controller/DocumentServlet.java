package com.hrms.controller;

import com.hrms.model.EmployeeDocument;
import com.hrms.service.DocumentService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;

public class DocumentServlet extends HttpServlet {
    private final DocumentService documentService = new DocumentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if ("/documents/download".equals(request.getServletPath())) {
                downloadDocument(request, response);
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
                success = documentService.deleteDocument(parseRequiredInt(request.getParameter("documentId")));
                message = success ? "Document deleted successfully." : "No document record was changed.";
            } else {
                Part filePart = request.getPart("file");
                success = documentService.uploadDocument(
                        parseRequiredInt(request.getParameter("employeeId")),
                        request.getParameter("documentName"),
                        request.getParameter("documentType"),
                        filePart,
                        getServletContext());
                message = success ? "Document uploaded successfully." : "Document upload failed.";
            }

            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":" + success + ",\"message\":" + quote(message) + "}");
        } catch (SQLException | IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        }
    }

    private void downloadDocument(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        EmployeeDocument document = documentService.downloadDocument(parseRequiredInt(request.getParameter("id")));
        File file = documentService.resolveDownloadFile(document, getServletContext());

        String contentType = getServletContext().getMimeType(file.getName());
        response.setContentType(contentType == null ? "application/octet-stream" : contentType);
        String disposition = "true".equalsIgnoreCase(request.getParameter("inline")) ? "inline" : "attachment";
        response.setHeader("Content-Disposition", disposition + "; filename=\"" + document.getFileName() + "\"");
        response.setContentLengthLong(file.length());

        try (FileInputStream inputStream = new FileInputStream(file);
             OutputStream outputStream = response.getOutputStream()) {
            inputStream.transferTo(outputStream);
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
