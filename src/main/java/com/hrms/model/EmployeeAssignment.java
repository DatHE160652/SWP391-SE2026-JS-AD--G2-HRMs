package com.hrms.model;

public class EmployeeAssignment {
    private int employeeId;
    private Integer departmentId;
    private Integer positionId;
    private Integer managerId;

    public EmployeeAssignment() {
    }

    public EmployeeAssignment(int employeeId, Integer departmentId, Integer positionId, Integer managerId) {
        this.employeeId = employeeId;
        this.departmentId = departmentId;
        this.positionId = positionId;
        this.managerId = managerId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getPositionId() {
        return positionId;
    }

    public void setPositionId(Integer positionId) {
        this.positionId = positionId;
    }

    public Integer getManagerId() {
        return managerId;
    }

    public void setManagerId(Integer managerId) {
        this.managerId = managerId;
    }
}
