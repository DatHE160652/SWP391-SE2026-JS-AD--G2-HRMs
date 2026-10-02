package com.hrms.model;

import java.math.BigDecimal;
import java.sql.Date;

public class Employee {
    private int employeeId;
    private String employeeCode;
    private String fullName;
    private String phone;
    private String gender;
    private Date dateOfBirth;
    private Date startDate;
    private BigDecimal baseSalary;
    private String status;
    private Integer userId;
    private String username;
    private String accountEmail;
    private Boolean accountActive;

    public Employee() {
    }

    public Employee(int employeeId, String employeeCode, String fullName, String phone, String gender,
            Date dateOfBirth, Date startDate, BigDecimal baseSalary, String status) {
        this.employeeId = employeeId;
        this.employeeCode = employeeCode;
        this.fullName = fullName;
        this.phone = phone;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.startDate = startDate;
        this.baseSalary = baseSalary;
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

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
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
                + ", startDate=" + startDate
                + ", baseSalary=" + baseSalary
                + ", status='" + status + '\''
                + ", userId=" + userId
                + ", username='" + username + '\''
                + ", accountEmail='" + accountEmail + '\''
                + ", accountActive=" + accountActive
                + '}';
    }
}
