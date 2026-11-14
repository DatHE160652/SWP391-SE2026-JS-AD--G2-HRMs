package com.hrms.model;

import java.sql.Date;

public class Employee {
    private int employeeId;
    private Integer companyId;
    private Integer departmentId;
    private Integer positionId;
    private Integer managerId;
    private String email;
    private Date identityIssuedDate;
    private String identityIssuedPlace;
    private String permanentAddress;
    private String hometown;
    private String major;
    private String intendedContractType;
    private String contractType;
    private String contractCode;
    private String contractStatus;
    private String departmentName;
    private String positionTitle;
    private String managerName;
    private String updatedAt;

    public Integer getManagerId() { return managerId; }
    public void setManagerId(Integer value) { managerId = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public Date getIdentityIssuedDate() { return identityIssuedDate; }
    public void setIdentityIssuedDate(Date value) { identityIssuedDate = value; }
    public String getIdentityIssuedPlace() { return identityIssuedPlace; }
    public void setIdentityIssuedPlace(String value) { identityIssuedPlace = value; }
    public String getPermanentAddress() { return permanentAddress; }
    public void setPermanentAddress(String value) { permanentAddress = value; }
    public String getHometown() { return hometown; }
    public void setHometown(String value) { hometown = value; }
    public String getMajor() { return major; }
    public void setMajor(String value) { major = value; }
    public String getIntendedContractType() { return intendedContractType; }
    public void setIntendedContractType(String value) { intendedContractType = value; }
    public String getContractType() { return contractType; }
    public void setContractType(String value) { contractType = value; }
    public String getContractCode() { return contractCode; }
    public void setContractCode(String value) { contractCode = value; }
    public String getContractStatus() { return contractStatus; }
    public void setContractStatus(String value) { contractStatus = value; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String value) { departmentName = value; }
    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String value) { positionTitle = value; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String value) { managerName = value; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String value) { updatedAt = value; }

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }
    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
    public Integer getPositionId() { return positionId; }
    public void setPositionId(Integer positionId) { this.positionId = positionId; }
    private String employeeCode;
    private String fullName;
    private String phone;
    private String gender;
    private Date dateOfBirth;
    private String identityNumber;
    private String status;
    private Integer userId;
    private String username;
    private String accountEmail;
    private Boolean accountActive;

    public Employee() {
    }

    public Employee(int employeeId, String employeeCode, String fullName, String phone, String gender,
            Date dateOfBirth, String identityNumber, String status) {
        this.employeeId = employeeId;
        this.employeeCode = employeeCode;
        this.fullName = fullName;
        this.phone = phone;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.identityNumber = identityNumber;
        this.status = status;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getIdentityNumber() {
        return identityNumber;
    }

    public void setIdentityNumber(String identityNumber) {
        this.identityNumber = identityNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(String accountEmail) {
        this.accountEmail = accountEmail;
    }

    public Boolean getAccountActive() {
        return accountActive;
    }

    public void setAccountActive(Boolean accountActive) {
        this.accountActive = accountActive;
    }

    @Override
    public String toString() {
        return "Employee{"
                + "employeeId=" + employeeId
                + ", employeeCode='" + employeeCode + '\''
                + ", fullName='" + fullName + '\''
                + ", phone='" + phone + '\''
                + ", gender='" + gender + '\''
                + ", dateOfBirth=" + dateOfBirth
                + ", status='" + status + '\''
                + ", userId=" + userId
                + ", username='" + username + '\''
                + ", accountEmail='" + accountEmail + '\''
                + ", accountActive=" + accountActive
                + '}';
    }
}
