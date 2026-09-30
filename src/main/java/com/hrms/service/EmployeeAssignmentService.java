package com.hrms.service;

import com.hrms.dao.EmployeeAssignmentDAO;
import com.hrms.model.EmployeeAssignment;

import java.sql.SQLException;

public class EmployeeAssignmentService {
    private final EmployeeAssignmentDAO employeeAssignmentDAO = new EmployeeAssignmentDAO();

    public boolean assignDepartment(int employeeId, int departmentId) throws SQLException {
        validateEmployee(employeeId);
        if (!employeeAssignmentDAO.departmentExists(departmentId)) {
            throw new IllegalArgumentException("Department does not exist.");
        }
        return employeeAssignmentDAO.assignDepartment(employeeId, departmentId);
    }

    public boolean assignPosition(int employeeId, int positionId) throws SQLException {
        validateEmployee(employeeId);
        if (!employeeAssignmentDAO.positionExists(positionId)) {
            throw new IllegalArgumentException("Position does not exist.");
        }
        return employeeAssignmentDAO.assignPosition(employeeId, positionId);
    }

    public boolean assignManager(int employeeId, int managerId) throws SQLException {
        validateEmployee(employeeId);
        if (employeeId == managerId) {
            throw new IllegalArgumentException("Employee cannot be assigned as their own manager.");
        }
        if (!employeeAssignmentDAO.employeeExists(managerId)) {
            throw new IllegalArgumentException("Manager does not exist.");
        }
        return employeeAssignmentDAO.assignManager(employeeId, managerId);
    }

    public EmployeeAssignment getEmployeeOrganization(int employeeId) throws SQLException {
        EmployeeAssignment assignment = employeeAssignmentDAO.getEmployeeOrganization(employeeId);
        if (assignment == null) {
            throw new IllegalArgumentException("Employee not found.");
        }
        return assignment;
    }

    private void validateEmployee(int employeeId) throws SQLException {
        if (!employeeAssignmentDAO.employeeExists(employeeId)) {
            throw new IllegalArgumentException("Employee not found.");
        }
    }
}
