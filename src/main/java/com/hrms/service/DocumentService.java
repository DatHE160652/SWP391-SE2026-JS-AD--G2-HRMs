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
import java.sql.Connection;
import com.hrms.util.DBConnection;
import com.hrms.dao.OrganizationTransaction;
import com.hrms.dao.EmployeeLifecycleDAO;
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
        if(uploadedBy==null) throw new SecurityException("Vui lòng đăng nhập trước khi tải tài liệu lên.");
        var employee=employeeDAO.getEmployeeById(employeeId);
        if (documentName == null || documentName.isBlank()) {
            throw new FieldValidationException("documentName", "Vui lòng nhập tên tài liệu.");
        }
        try(Connection connection=DBConnection.getConnection()) {
            requireUploader(connection,employeeId,employee.getCompanyId(),uploadedBy,documentType,true);
        }

        FileUploadUtil.SavedFile savedFile = FileUploadUtil.saveUploadedFile(filePart, employeeId, servletContext);

        EmployeeDocument document = new EmployeeDocument();
        document.setEmployeeId(employeeId);
        document.setDocumentName(documentName.trim());
        document.setDocumentType(documentType == null || documentType.isBlank() ? null : documentType.trim());
        document.setFileName(savedFile.getOriginalFileName());
        document.setFilePath(savedFile.getRelativePath());
        document.setUploadedBy(uploadedBy);

        try {
            long storedSize=java.nio.file.Files.size(java.nio.file.Path.of(savedFile.getAbsolutePath()));
            boolean saved=OrganizationTransaction.execute(employee.getCompanyId(),connection -> {
                var period = requireUploader(connection,employeeId,employee.getCompanyId(),uploadedBy,document.getDocumentType(),true);
                if (period != null) {
                    document.setDocumentPeriodId(period.periodId());
                }
                if(!documentDAO.insertDocument(connection,document,FileUploadUtil.contentType(savedFile.getOriginalFileName()),storedSize)) throw new SQLException("Không lưu được tài liệu.");
                OrganizationTransaction.audit(connection,employee.getCompanyId(),uploadedBy,employeeId,"UPLOAD_DOCUMENT",null,"DocumentId="+document.getDocumentId()+"; "+document.getDocumentName()+periodDescription(period));
                requirePeriodStillOpen(connection,employee.getCompanyId(),employeeId,document.getDocumentType(),period);
                return true;
            });
            if(!saved) java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(savedFile.getAbsolutePath()));
            return saved;
        } catch (SQLException | IOException | RuntimeException exception) {
            java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(savedFile.getAbsolutePath()));
            throw exception;
        }
    }

    public EmployeeDocument downloadDocument(int documentId) throws SQLException {
        EmployeeDocument document = documentDAO.getDocumentById(documentId);
        if (document == null || isInactive(document)) {
            throw new IllegalArgumentException("Không tìm thấy tài liệu.");
        }
        return document;
    }

    public File resolveDownloadFile(EmployeeDocument document, ServletContext servletContext) {
        File file = FileUploadUtil.resolveStoredFile(document.getFilePath(), servletContext);
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Không tìm thấy tệp tài liệu trên máy chủ.");
        }
        return file;
    }

    public EmployeeDocument getDocument(int documentId) throws SQLException {
        EmployeeDocument document = documentDAO.getDocumentById(documentId);
        if (document == null) throw new IllegalArgumentException("Không tìm thấy tài liệu.");
        return document;
    }

    public boolean setDocumentActive(int documentId,boolean active,int companyId,Integer actorId) throws SQLException {
        var document=getDocument(documentId);
        return OrganizationTransaction.execute(companyId,connection -> {
            var period = requireUploader(connection,document.getEmployeeId(),companyId,actorId,null,false);
            if(new EmployeeLifecycleDAO().execute(connection,"UPDATE EmployeeDocuments SET Status=? WHERE DocumentId=? AND EmployeeId=?",
                    active?"Active":"Inactive",documentId,document.getEmployeeId())!=1) throw new IllegalArgumentException("Tài liệu đã thay đổi. Vui lòng tải lại trang.");
            OrganizationTransaction.audit(connection,companyId,actorId,document.getEmployeeId(),active?"ACTIVATE_DOCUMENT":"DEACTIVATE_DOCUMENT",document.getStatus(),"DocumentId="+documentId+"; "+(active?"Active":"Inactive")+periodDescription(period));
            requirePeriodStillOpen(connection,companyId,document.getEmployeeId(),null,period);
            return true;
        });
    }

    private void ensureEmployeeExists(int employeeId) throws SQLException {
        if (employeeDAO.getEmployeeById(employeeId) == null) {
            throw new IllegalArgumentException("Không tìm thấy nhân viên.");
        }
    }

    private com.hrms.dao.DocumentUpdatePeriodDAO.ActiveEmployeeDocumentContext requireUploader(Connection connection,int employeeId,int companyId,Integer actorId,String documentType,boolean isUpload) throws SQLException {
        var actors = new EmployeeLifecycleDAO().query(connection,"""
                SELECT r.RoleCode FROM Users u JOIN Roles r ON r.RoleId=u.RoleId
                JOIN Employees e ON e.CompanyId=u.CompanyId AND e.EmployeeId=?
                WHERE u.UserId=? AND u.CompanyId=? AND u.IsActive=1 AND u.IsLocked=0
                    AND (r.RoleCode IN ('HR','COMPANY_ADMIN') OR (r.RoleCode IN ('EMPLOYEE','MANAGER')
                        AND u.UserId=e.UserId AND e.Status='Active' AND u.MustChangePassword=0 AND
                """ + com.hrms.dao.ContractAccessPolicy.ALLOWED + "))",employeeId,actorId,companyId);
        if (actors.isEmpty()) throw new SecurityException("Bạn không có quyền thay đổi tài liệu của nhân viên này.");
        if (java.util.Set.of("HR","COMPANY_ADMIN").contains(actors.get(0).get("RoleCode"))) {
            new com.hrms.dao.DocumentUpdatePeriodDAO().ensureSchemaAndCompanyDefaults(connection, companyId);
            return null;
        }
        if (isUpload && (documentType == null || documentType.isBlank())) {
            throw new FieldValidationException("documentType", "Vui lòng chọn loại tài liệu cần cập nhật.");
        }
        return new com.hrms.dao.DocumentUpdatePeriodDAO().requireAvailablePeriodForUpload(connection,companyId,employeeId,documentType);
    }

    private String periodDescription(com.hrms.dao.DocumentUpdatePeriodDAO.ActiveEmployeeDocumentContext period) {
        return period == null ? "" : "\nĐợt cập nhật: " + period.title();
    }

    private void requirePeriodStillOpen(Connection connection,int companyId,int employeeId,String documentType,com.hrms.dao.DocumentUpdatePeriodDAO.ActiveEmployeeDocumentContext period) throws SQLException {
        if (period != null && new com.hrms.dao.DocumentUpdatePeriodDAO().requireAvailablePeriodForUpload(connection,companyId,employeeId,documentType) == null)
            throw new SecurityException("Đợt cập nhật đã kết thúc hoặc thay đổi. Vui lòng tải lại trang.");
    }

    private boolean isInactive(EmployeeDocument document) {
        return document.getStatus() != null && !"Active".equalsIgnoreCase(document.getStatus());
    }
}
