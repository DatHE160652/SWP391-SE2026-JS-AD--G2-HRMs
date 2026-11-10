package com.hrms.model;

import java.sql.Timestamp;

public class EmployeeDocument {
    private int documentId;
    private int employeeId;
    private String documentName;
    private String documentType;
    private String fileName;
    private String filePath;
    private Integer uploadedBy;
    private Timestamp uploadedDate;
    private String status;
    private String mimeType;
    private Long fileSizeBytes;
    private Long documentPeriodId;

    public String getMimeType() { return mimeType; }
    public void setMimeType(String value) { mimeType=value; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long value) { fileSizeBytes=value; }
    public Long getDocumentPeriodId() { return documentPeriodId; }
    public void setDocumentPeriodId(Long value) { documentPeriodId=value; }

    public EmployeeDocument() {
    }

    public EmployeeDocument(int documentId, int employeeId, String documentName, String documentType,
            String fileName, String filePath, Integer uploadedBy, Timestamp uploadedDate, String status) {
        this.documentId = documentId;
        this.employeeId = employeeId;
        this.documentName = documentName;
        this.documentType = documentType;
        this.fileName = fileName;
        this.filePath = filePath;
        this.uploadedBy = uploadedBy;
        this.uploadedDate = uploadedDate;
        this.status = status;
    }

    public int getDocumentId() {
        return documentId;
    }

    public void setDocumentId(int documentId) {
        this.documentId = documentId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Integer getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Integer uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Timestamp getUploadedDate() {
        return uploadedDate;
    }

    public void setUploadedDate(Timestamp uploadedDate) {
        this.uploadedDate = uploadedDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "EmployeeDocument{"
                + "documentId=" + documentId
                + ", employeeId=" + employeeId
                + ", documentName='" + documentName + '\''
                + ", documentType='" + documentType + '\''
                + ", fileName='" + fileName + '\''
                + ", filePath='" + filePath + '\''
                + ", uploadedBy=" + uploadedBy
                + ", uploadedDate=" + uploadedDate
                + ", status='" + status + '\''
                + '}';
    }
}
