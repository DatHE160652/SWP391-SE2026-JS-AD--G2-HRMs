package com.hrms.model;

import java.time.LocalDateTime;

public class BulkImportLog {
    private Integer importId;
    private Integer companyId;
    private String importType;
    private String fileName;
    private Integer totalRows;
    private Integer successRows;
    private Integer failedRows;
    private String errorDetails;
    private Integer importedByUserId;
    private LocalDateTime createdAt;

    public Integer getImportId() { return importId; }
    public void setImportId(Integer importId) { this.importId = importId; }
    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }
    public String getImportType() { return importType; }
    public void setImportType(String importType) { this.importType = importType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }
    public Integer getSuccessRows() { return successRows; }
    public void setSuccessRows(Integer successRows) { this.successRows = successRows; }
    public Integer getFailedRows() { return failedRows; }
    public void setFailedRows(Integer failedRows) { this.failedRows = failedRows; }
    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
    public Integer getImportedByUserId() { return importedByUserId; }
    public void setImportedByUserId(Integer importedByUserId) { this.importedByUserId = importedByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}