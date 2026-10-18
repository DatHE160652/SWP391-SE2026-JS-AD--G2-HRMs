package com.hrms.model;

import java.sql.Date;

public class Employee {
    private int employeeId;
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
