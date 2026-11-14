package com.hrms.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Result returned after committing imported Excel records into the database.
 */
public class ImportExecuteResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private int totalProcessed;
    private int insertedCount;
    private int updatedCount;
    private int failedCount;
    private List<String> errorMessages = new ArrayList<>();

    public ImportExecuteResult() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTotalProcessed() {
        return totalProcessed;
    }

    public void setTotalProcessed(int totalProcessed) {
        this.totalProcessed = totalProcessed;
    }

    public int getInsertedCount() {
        return insertedCount;
    }

    public void setInsertedCount(int insertedCount) {
        this.insertedCount = insertedCount;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages != null ? errorMessages : new ArrayList<>();
    }
}

