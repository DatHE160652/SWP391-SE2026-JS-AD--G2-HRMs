package com.hrms.service.companyadmin;

import com.hrms.service.FieldValidationException;
import com.hrms.util.FileUploadUtil;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Deletes a staged upload unless its owning database transaction has committed. */
public final class ContractHardCopy implements AutoCloseable {
    private final FileUploadUtil.SavedFile file;
    private final long size;
    private boolean retained;

    private ContractHardCopy(FileUploadUtil.SavedFile file,long size) {
        this.file=file;
        this.size=size;
    }

    public static ContractHardCopy receive(Part part,int employeeId,ServletContext context) throws IOException {
        if (part==null || (part.getSize()==0 && (part.getSubmittedFileName()==null || part.getSubmittedFileName().isBlank()))) return null;
        try {
            return new ContractHardCopy(FileUploadUtil.saveUploadedFile(part,employeeId,context),part.getSize());
        } catch (FieldValidationException e) {
            throw new FieldValidationException("contractFile",e.getMessage());
        }
    }

    public FileUploadUtil.SavedFile file() { return file; }
    public long size() { return size; }
    public void retain() { retained=true; }

    @Override public void close() throws IOException {
        if (!retained) Files.deleteIfExists(Path.of(file.getAbsolutePath()));
    }
}
