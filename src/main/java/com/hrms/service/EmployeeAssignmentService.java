package com.hrms.service;

import com.hrms.dao.EmployeeAssignmentDAO;
import com.hrms.dao.UserDAO;
import com.hrms.model.EmployeeAssignment;
import com.hrms.model.Position;
import com.hrms.dao.PositionDAO;

import java.sql.SQLException;

public class EmployeeAssignmentService {
    private final EmployeeAssignmentDAO employeeAssignmentDAO = new EmployeeAssignmentDAO();
    private final UserDAO userDAO = new UserDAO();
    private final PositionDAO positionDAO = new PositionDAO();

    public boolean assignDepartment(int employeeId, int departmentId) throws SQLException {
        validateEmployee(employeeId);
        if (!employeeAssignmentDAO.departmentExists(departmentId)) {
            throw new FieldValidationException("departmentId", "Department does not exist.");
        }
        return employeeAssignmentDAO.assignDepartment(employeeId, departmentId);
    }

    public boolean assignPosition(int employeeId, int positionId) throws SQLException {
        validateEmployee(employeeId);
        if (!employeeAssignmentDAO.positionExists(positionId)) {
            throw new FieldValidationException("positionId", "Position does not exist.");
        }
        Position position = positionDAO.getPositionById(positionId);
        EmployeeAssignment assignment = getEmployeeOrganization(employeeId);
        if (assignment.getDepartmentId() == null) {
            throw new FieldValidationException("departmentId", "Please assign a department before selecting a position.");
        }
        if (position.getDepartmentId() == null || !position.getDepartmentId().equals(assignment.getDepartmentId())) {
            throw new FieldValidationException("positionId", "Selected position must belong to the employee's department.");
        }
        boolean assigned = employeeAssignmentDAO.assignPosition(employeeId, positionId);
        if (assigned) {
            userDAO.updateTenantUserRoleByEmployeeId(employeeId, isLeaderPosition(position) ? "MANAGER" : "EMPLOYEE");
        }
        return assigned;
    }

    public boolean clearPosition(int employeeId) throws SQLException {
        validateEmployee(employeeId);
        boolean updated = employeeAssignmentDAO.clearPosition(employeeId);
        if (updated) {
            userDAO.updateTenantUserRoleByEmployeeId(employeeId, "EMPLOYEE");
        }
        return updated;
    }

    public boolean assignDepartmentMembers(int departmentId, int managerId, int[] employeeIds) throws SQLException {
        if (!employeeAssignmentDAO.departmentExists(departmentId)) {
            throw new FieldValidationException("departmentId", "Department does not exist.");
        }
        validateEmployee(managerId);
        if (!employeeAssignmentDAO.isManagerInDepartment(managerId, departmentId)) {
            throw new FieldValidationException("managerId", "Department manager must have MANAGER role and belong to this department.");
        }
        for (int employeeId : employeeIds) {
            validateEmployee(employeeId);
            if (employeeId == managerId) {
                throw new FieldValidationException("employeeIds", "Manager cannot be selected as their own team member.");
            }
            if (employeeAssignmentDAO.hasDifferentDepartment(employeeId, departmentId)) {
                throw new FieldValidationException("employeeIds", "Employee already belongs to another department. Update that employee's department first.");
            }
        }
        userDAO.updateTenantUserRoleByEmployeeId(managerId, "MANAGER");
        return employeeAssignmentDAO.assignDepartmentMembers(departmentId, managerId, employeeIds);
    }

    public java.util.List<EmployeeAssignmentDAO.DepartmentMemberOption> getDepartmentMemberOptions(int departmentId)
            throws SQLException {
        if (!employeeAssignmentDAO.departmentExists(departmentId)) {
            throw new FieldValidationException("departmentId", "Department does not exist.");
        }
        return employeeAssignmentDAO.getDepartmentMemberOptions(departmentId);
    }

    public boolean assignManager(int employeeId, int managerId) throws SQLException {
        validateEmployee(employeeId);
        if (employeeId == managerId) {
            throw new FieldValidationException("managerId", "Employee cannot be assigned as their own manager.");
        }
        if (!employeeAssignmentDAO.employeeExists(managerId)) {
            throw new FieldValidationException("managerId", "Manager does not exist.");
        }
        EmployeeAssignment assignment = getEmployeeOrganization(employeeId);
        if (assignment.getDepartmentId() == null) {
            throw new FieldValidationException("departmentId", "Please assign a department before selecting a manager.");
        }
        if (!employeeAssignmentDAO.isManagerInDepartment(managerId, assignment.getDepartmentId())) {
            throw new FieldValidationException("managerId", "Selected manager must have MANAGER role and belong to the same department.");
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

    private boolean isLeaderPosition(Position position) {
        if (position == null || position.getPositionTitle() == null) {
            return false;
        }
        String title = position.getPositionTitle().toLowerCase();
        return title.contains("leader") || title.contains("manager") || title.contains("trưởng") || title.contains("quan ly") || title.contains("quản lý");
    }
}
