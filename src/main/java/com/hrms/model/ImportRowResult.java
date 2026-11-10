package com.hrms.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Result of validating a single row during Excel import preview.
 */
public class ImportRowResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private int rowNumber;
    private Map<String, String> data;
    private boolean valid;
    private String errorMessage;

    public ImportRowResult() {
    }

    public ImportRowResult(int rowNumber, Map<String, String> data, boolean valid, String errorMessage) {
        this.rowNumber = rowNumber;
        this.data = data;
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public Map<String, String> getData() {
        return data != null ? data : Collections.emptyMap();
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

