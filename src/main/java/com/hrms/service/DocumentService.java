package com.hrms.service;

import com.hrms.dao.DocumentDAO;
import com.hrms.dao.EmployeeDAO;
import com.hrms.model.EmployeeDocument;
import com.hrms.util.FileUploadUtil;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DocumentService {
    private final DocumentDAO documentDAO = new DocumentDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    public List<EmployeeDocument> getDocumentsByEmployee(int employeeId) throws SQLException {
        ensureEmployeeExists(employeeId);
        return documentDAO.getDocumentsByEmployee(employeeId);
    }

    public boolean uploadDocument(int employeeId, String documentName, String documentType, Part filePart,
            ServletContext servletContext) throws SQLException, IOException {
        return uploadDocument(employeeId, documentName, documentType, filePart, servletContext, null);
    }

    public boolean uploadDocument(int employeeId, String documentName, String documentType, Part filePart,
            ServletContext servletContext, Integer uploadedBy) throws SQLException, IOException {
        ensureEmployeeExists(employeeId);
        if (documentName == null || documentName.isBlank()) {
            throw new FieldValidationException("documentName", "Document name is required");
        }

        FileUploadUtil.SavedFile savedFile = FileUploadUtil.saveUploadedFile(filePart, employeeId, servletContext);

        EmployeeDocument document = new EmployeeDocument();
        document.setEmployeeId(employeeId);
        document.setDocumentName(documentName.trim());
        document.setDocumentType(documentType == null || documentType.isBlank() ? null : documentType.trim());
        document.setFileName(savedFile.getOriginalFileName());
        document.setFilePath(savedFile.getRelativePath());
        document.setUploadedBy(uploadedBy);

        return documentDAO.insertDocument(document);
    }

    public EmployeeDocument downloadDocument(int documentId) throws SQLException {
        EmployeeDocument document = documentDAO.getDocumentById(documentId);
        if (document == null || isDeleted(document)) {
            throw new IllegalArgumentException("Document not found");
        }
        return document;
    }

    public File resolveDownloadFile(EmployeeDocument document, ServletContext servletContext) {
        File file = FileUploadUtil.resolveStoredFile(document.getFilePath(), servletContext);
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Document file not found on server");
        }
        return file;
    }

    public boolean deleteDocument(int documentId) throws SQLException {
        return documentDAO.deleteDocument(documentId);
    }

    private void ensureEmployeeExists(int employeeId) throws SQLException {
        if (employeeDAO.getEmployeeById(employeeId) == null) {
            throw new IllegalArgumentException("Employee not found");
        }
    }

    private boolean isDeleted(EmployeeDocument document) {
        return document.getStatus() != null && "Deleted".equalsIgnoreCase(document.getStatus());
    }
}
