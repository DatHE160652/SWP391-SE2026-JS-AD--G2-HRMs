package com.hrms.service;

import com.hrms.dao.EmployeeAssignmentDAO;
import com.hrms.dao.OrganizationAssignmentDAO;
import com.hrms.model.EmployeeAssignment;
import java.sql.SQLException;
import java.util.List;

public class EmployeeAssignmentService {
    public boolean assignDepartmentEmployees(int companyId, int[] employeeIds, int departmentId, int positionId,
            Integer managerId, Integer actorId) throws SQLException {
        if (employeeIds == null || employeeIds.length == 0 || employeeIds.length > 500)
            throw new FieldValidationException("employeeIds", "Vui lòng chọn từ 1 đến 500 nhân viên.");
        java.util.Set<Integer> selected = new java.util.HashSet<>();
        for (int employeeId : employeeIds) if (employeeId <= 0 || !selected.add(employeeId))
            throw new FieldValidationException("employeeIds", "Danh sách nhân viên không hợp lệ hoặc bị trùng.");
        if (positionId <= 0) throw new FieldValidationException("positionId", "Vui lòng chọn chức danh.");
        return organizationDAO.assignDepartmentEmployees(companyId, employeeIds, departmentId, positionId, managerId, actorId);
    }

    public boolean recordInitialAssignments(int companyId, int[] employeeIds, int departmentId, int positionId,
            Integer actorId) throws SQLException {
        return recordInitialAssignments(companyId, employeeIds, departmentId, positionId, null, actorId);
    }

    public boolean recordInitialAssignments(int companyId, int[] employeeIds, int departmentId, int positionId,
            Integer managerId, Integer actorId) throws SQLException {
        if (employeeIds == null || employeeIds.length == 0)
            throw new FieldValidationException("employeeIds", "Vui lòng chọn ít nhất một nhân viên chưa phân công.");
        if (employeeIds.length > 500) throw new FieldValidationException("employeeIds", "Chỉ phân công tối đa 500 nhân viên mỗi lần.");
        java.util.Set<Integer> selected = new java.util.HashSet<>();
        for (int employeeId : employeeIds) if (employeeId <= 0 || !selected.add(employeeId))
            throw new FieldValidationException("employeeIds", "Danh sách nhân viên không hợp lệ hoặc bị trùng.");
        if (positionId <= 0) throw new FieldValidationException("positionId", "Vui lòng chọn chức danh.");
        if (managerId != null && managerId <= 0) throw new FieldValidationException("managerId", "Quản lý trực tiếp không hợp lệ.");
        return organizationDAO.recordInitialAssignments(companyId, employeeIds, departmentId, positionId, managerId, actorId);
    }

    public java.util.List<java.util.Map<String,Object>> getInitialAssignmentCandidates(int companyId, int departmentId) throws SQLException {
        return organizationDAO.initialAssignmentCandidates(companyId, departmentId);
    }

    public boolean recordInitialAssignment(int companyId, int employeeId, int departmentId, int positionId,
            java.time.LocalDate effectiveFrom, Integer actorId, String reason) throws SQLException {
        if (employeeId <= 0) throw new FieldValidationException("employeeId", "Vui lòng chọn nhân viên chưa phân công.");
        if (positionId <= 0) throw new FieldValidationException("positionId", "Vui lòng chọn chức danh.");
        String explanation = reason == null || reason.isBlank() ? "Ghi nhận phân công ban đầu lên hệ thống" : reason.strip();
        if (explanation.length() > 500) throw new FieldValidationException("reason", "Độ dài tối đa là 500 ký tự.");
        return organizationDAO.recordInitialAssignment(companyId, employeeId, departmentId, positionId, effectiveFrom, actorId, explanation);
    }

    private final EmployeeAssignmentDAO employeeAssignmentDAO = new EmployeeAssignmentDAO();
    private final OrganizationAssignmentDAO organizationDAO = new OrganizationAssignmentDAO();

    public boolean assignDepartment(int companyId, int employeeId, int departmentId) throws SQLException {
        EmployeeAssignment existing = getEmployeeOrganization(companyId, employeeId);
        return assignOrganization(companyId, employeeId, departmentId, existing.getPositionId());
    }

    public boolean assignPosition(int companyId, int employeeId, int positionId) throws SQLException {
        EmployeeAssignment existing = getEmployeeOrganization(companyId, employeeId);
        return assignOrganization(companyId, employeeId, existing.getDepartmentId(), positionId);
    }

    public boolean clearPosition(int companyId, int employeeId) throws SQLException {
        EmployeeAssignment existing = getEmployeeOrganization(companyId, employeeId);
        return assignOrganization(companyId, employeeId, existing.getDepartmentId(), null);
    }

    public boolean assignOrganization(int companyId, int employeeId, Integer departmentId, Integer positionId) throws SQLException {
        return organizationDAO.assignOrganization(companyId, employeeId, departmentId, positionId);
    }

    public boolean assignDepartmentMembers(int companyId, int departmentId, int managerId, int[] employeeIds) throws SQLException {
        return employeeAssignmentDAO.assignDepartmentMembers(companyId, departmentId, managerId, employeeIds);
    }

    public boolean assignManager(int companyId, int employeeId, Integer managerId) throws SQLException {
        return employeeAssignmentDAO.assignManager(companyId, employeeId, managerId);
    }

    public boolean assignManager(int companyId,int employeeId,Integer managerId,Integer actorId,String reason) throws SQLException {
        return employeeAssignmentDAO.assignManager(companyId,employeeId,managerId,actorId,validateReason(reason));
    }

    public boolean assignDepartmentMembers(int companyId,int departmentId,int managerId,int[] employeeIds,Integer actorId) throws SQLException {
        return employeeAssignmentDAO.assignDepartmentMembers(companyId,departmentId,managerId,employeeIds,actorId);
    }

    public boolean saveOrganization(int companyId, int employeeId, Integer departmentId, Integer positionId,
                                     Integer managerId, Integer actorUserId, String reason) throws SQLException {
        if (departmentId == null) throw new FieldValidationException("departmentId", "Vui lòng chọn phòng ban.");
        String changeReason = reason == null || reason.isBlank() ? "Thông tin phân công được bộ phận nhân sự cập nhật" : reason.trim();
        if (changeReason.length() > 500) throw new FieldValidationException("reason", "Độ dài tối đa là 500 ký tự.");
        return organizationDAO.saveOrganization(companyId, employeeId, departmentId, positionId, managerId, actorUserId, changeReason);
    }

    public List<EmployeeAssignmentDAO.DepartmentMemberOption> getEligibleManagers(int companyId, Integer departmentId, int employeeId) throws SQLException {
        return employeeAssignmentDAO.getEligibleManagers(companyId, departmentId, employeeId);
    }

    public List<EmployeeAssignmentDAO.DepartmentMemberOption> getTeamOptions(int companyId, int managerId) throws SQLException {
        return employeeAssignmentDAO.getTeamOptions(companyId, managerId);
    }

    public EmployeeAssignmentDAO.DepartmentMemberOption getMember(int companyId, int employeeId) throws SQLException {
        return employeeAssignmentDAO.getMember(companyId, employeeId);
    }

    public boolean manageTeam(int companyId, int managerId, int[] additions, int[] removals,
                              java.util.Map<Integer,Integer> expectedManagers, boolean confirmTransfers, Integer actorId) throws SQLException {
        return employeeAssignmentDAO.manageTeam(companyId, managerId, additions, removals, expectedManagers, confirmTransfers, actorId);
    }

    public boolean manageTeam(int companyId,int managerId,int[] additions,int[] removals,
                              java.util.Map<Integer,Integer> expectedManagers,boolean confirmTransfers,Integer actorId,String reason) throws SQLException {
        return employeeAssignmentDAO.manageTeam(companyId,managerId,additions,removals,expectedManagers,confirmTransfers,actorId,validateReason(reason));
    }

    private String validateReason(String reason) {
        if (reason!=null && reason.length()>500) throw new FieldValidationException("reason","Độ dài tối đa là 500 ký tự.");
        return reason==null?null:reason.strip();
    }

    public List<EmployeeAssignmentDAO.DepartmentMemberOption> getDepartmentMemberOptions(int companyId, int departmentId) throws SQLException {
        if (!employeeAssignmentDAO.departmentExists(companyId, departmentId)) {
            throw new FieldValidationException("departmentId", "Không tìm thấy phòng ban trong công ty của bạn.");
        }
        return employeeAssignmentDAO.getDepartmentMemberOptions(companyId, departmentId);
    }

    public List<EmployeeAssignmentDAO.DepartmentMemberOption> getDirectReports(int companyId, int employeeId) throws SQLException {
        return employeeAssignmentDAO.getDirectReports(companyId, employeeId);
    }

    public EmployeeAssignment getEmployeeOrganization(int companyId, int employeeId) throws SQLException {
        EmployeeAssignment assignment = employeeAssignmentDAO.getEmployeeOrganization(companyId, employeeId);
        if (assignment == null) throw new IllegalArgumentException("Không tìm thấy nhân viên trong công ty của bạn.");
        return assignment;
    }
}
