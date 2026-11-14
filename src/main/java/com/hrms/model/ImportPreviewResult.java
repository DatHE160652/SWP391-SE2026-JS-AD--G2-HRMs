package com.hrms.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of validating an entire Excel file during import preview.
 */
public class ImportPreviewResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private int totalRows;
    private int validRows;
    private int errorRows;
    private List<String> globalErrors = new ArrayList<>();
    private List<ImportRowResult> previewRows = new ArrayList<>();
    private List<String> columnHeaders = new ArrayList<>();
    private boolean canProceed;

    public ImportPreviewResult() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getValidRows() {
        return validRows;
    }

    public void setValidRows(int validRows) {
        this.validRows = validRows;
    }

    public int getErrorRows() {
        return errorRows;
    }

    public void setErrorRows(int errorRows) {
        this.errorRows = errorRows;
    }

    public List<String> getGlobalErrors() {
        return globalErrors;
    }

    public void setGlobalErrors(List<String> globalErrors) {
        this.globalErrors = globalErrors != null ? globalErrors : new ArrayList<>();
    }

    public List<ImportRowResult> getPreviewRows() {
        return previewRows;
    }

    public void setPreviewRows(List<ImportRowResult> previewRows) {
        this.previewRows = previewRows != null ? previewRows : new ArrayList<>();
    }

    public List<String> getColumnHeaders() {
        return columnHeaders;
    }

    public void setColumnHeaders(List<String> columnHeaders) {
        this.columnHeaders = columnHeaders != null ? columnHeaders : new ArrayList<>();
    }

    public boolean isCanProceed() {
        return canProceed;
    }

    public void setCanProceed(boolean canProceed) {
        this.canProceed = canProceed;
    }
}

