package com.hrms.dao.companyadmin;

import com.hrms.service.companyadmin.ContractHardCopy;
import com.hrms.util.FileUploadUtil;
import java.sql.Connection;
import java.sql.SQLException;

public class ContractDocumentDAO {
    public java.util.Map<String,Object> missingHardCopyTarget(Connection connection,int companyId,long contractId) throws SQLException {
        var dao=new EmployeeLifecycleDAO();
        var contracts=dao.query(connection,"SELECT ContractId,EmployeeId,ContractCode,Status FROM EmploymentContracts WHERE CompanyId=? AND ContractId=?",companyId,contractId);
        if (contracts.isEmpty()) throw new IllegalArgumentException("Không tìm thấy hợp đồng trong công ty.");
        var contract=contracts.get(0);
        if (!java.util.Set.of("ACTIVE","EXPIRED","TERMINATED").contains(String.valueOf(contract.get("Status"))))
            throw new IllegalArgumentException("Chỉ bổ sung bản cứng cho hợp đồng đã kích hoạt. Hợp đồng nháp có thể tải tệp khi chỉnh sửa.");
        if (!dao.query(connection,"SELECT 1 FROM EmployeeDocuments WHERE ContractId=? AND EmployeeId=? AND Status='Active'",contractId,contract.get("EmployeeId")).isEmpty())
            throw new IllegalArgumentException("Hợp đồng đã có bản cứng. Vui lòng tải lại danh sách.");
        return contract;
    }

    /** The caller holds the company lock through commit, preventing duplicate uploads. */
    public java.util.Map<String,Object> addMissingHardCopy(Connection connection,int companyId,long contractId,int actorId,ContractHardCopy copy) throws SQLException {
        if (copy==null) throw new com.hrms.service.FieldValidationException("contractFile","Vui lòng chọn hợp đồng bản cứng.");
        var contract=missingHardCopyTarget(connection,companyId,contractId);
        insert(connection,companyId,((Number)contract.get("EmployeeId")).intValue(),contractId,String.valueOf(contract.get("ContractCode")),actorId,copy);
        return contract;
    }

    public void insert(Connection connection,int companyId,int employeeId,long contractId,String contractCode,int actorId,ContractHardCopy copy) throws SQLException {
        if (copy==null) return;
        int inserted=new EmployeeLifecycleDAO().execute(connection,"""
                INSERT EmployeeDocuments(EmployeeId,DocumentName,DocumentType,FileName,FilePath,UploadedBy,Status,MimeType,FileSizeBytes,ContractId)
                SELECT c.EmployeeId,?,N'Hợp đồng lao động',?,?,?,'Active',?,?,c.ContractId
                FROM EmploymentContracts c WHERE c.CompanyId=? AND c.EmployeeId=? AND c.ContractId=?
                ""","Hợp đồng bản cứng - "+contractCode,copy.file().getOriginalFileName(),copy.file().getRelativePath(),actorId,
                FileUploadUtil.contentType(copy.file().getOriginalFileName()),copy.size(),companyId,employeeId,contractId);
        if (inserted!=1) throw new SQLException("Không lưu được bản cứng của hợp đồng trong công ty.");
    }
}
