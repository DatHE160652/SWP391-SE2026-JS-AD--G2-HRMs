package com.hrms.service;

import com.hrms.dao.AuditLogDAO;
import com.hrms.model.AuditLog;

import java.sql.SQLException;
import java.util.List;

public class EmployeeHistoryService {
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public void logEmployeeChange(Integer companyId, Integer actorUserId, int employeeId, String action,
                                  String oldValue, String newValue, String ipAddress) throws SQLException {
        AuditLog log = new AuditLog();
        log.setCompanyId(companyId);
        log.setUserId(actorUserId);
        log.setAction(action);
        log.setTableName("Employees");
        log.setRecordId(String.valueOf(employeeId));
        log.setOldValues(oldValue);
        log.setNewValues(newValue);
        log.setIpAddress(ipAddress);
        auditLogDAO.insert(log);
    }

    public List<AuditLogDAO.HistoryEntry> getEmployeeHistory(int employeeId) throws SQLException {
        return auditLogDAO.getEmployeeHistory(employeeId);
    }
}
