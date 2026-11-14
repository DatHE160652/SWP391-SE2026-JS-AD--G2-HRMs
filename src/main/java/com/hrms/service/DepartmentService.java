package com.hrms.service;

import com.hrms.dao.DepartmentDAO;
import com.hrms.model.Department;

import java.sql.SQLException;
import java.util.List;

public class DepartmentService {
    private final DepartmentDAO departmentDAO = new DepartmentDAO();

    public List<Department> getDepartments(int companyId) throws SQLException {
        return departmentDAO.getAllDepartments(companyId);
    }

    public Department getDepartmentDetail(int companyId, int id) throws SQLException {
        return departmentDAO.getDepartmentById(companyId, id);
    }

    public boolean createDepartment(int companyId, Department department) throws SQLException {
        department.setCompanyId(companyId);
        validate(department, null);
        return departmentDAO.insertDepartment(department);
    }

    public boolean updateDepartment(int companyId, Department department) throws SQLException {
        if (getDepartmentDetail(companyId, department.getDepartmentId()) == null) {
            throw new IllegalArgumentException("Không tìm thấy phòng ban.");
        }
        department.setCompanyId(companyId);
        validate(department, department.getDepartmentId());
        return departmentDAO.updateDepartment(department);
    }

    public boolean setDepartmentActive(int companyId, int id, boolean active) throws SQLException {
        if (!departmentDAO.setDepartmentActive(companyId, id, active)) {
            throw new IllegalArgumentException("Không tìm thấy phòng ban trong công ty của bạn.");
        }
        return true;
    }

    private void validate(Department department, Integer excludeId) throws SQLException {
        department.setDepartmentCode(required("departmentCode", department.getDepartmentCode(), 50));
        department.setDepartmentName(required("departmentName", department.getDepartmentName(), 150));
        if (!departmentDAO.companyExists(department.getCompanyId())) {
            throw new IllegalArgumentException("Công ty không hợp lệ hoặc đã ngừng hoạt động.");
        }
        if (departmentDAO.departmentCodeExists(department.getCompanyId(), department.getDepartmentCode(), excludeId)) {
            throw new FieldValidationException("departmentCode", "Mã phòng ban đã tồn tại trong công ty.");
        }
        if (new com.hrms.dao.OrganizationNameDAO().exists(com.hrms.dao.OrganizationNameDAO.Catalog.DEPARTMENT,
                department.getCompanyId(), department.getDepartmentName(), excludeId)) {
            throw new FieldValidationException("departmentName", "Tên phòng ban đã tồn tại trong công ty.");
        }
    }

    static String required(String field, String value, int maximum) {
        if (value == null || value.isBlank()) throw new FieldValidationException(field, "Thông tin này là bắt buộc.");
        String trimmed = value.strip();
        if (trimmed.length() > maximum) throw new FieldValidationException(field, "Độ dài tối đa là " + maximum + " characters.");
        return trimmed;
    }
}
