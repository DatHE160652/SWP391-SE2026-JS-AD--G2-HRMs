package com.hrms.model;

import java.time.LocalDateTime;

public class Company {
	private Integer companyId;
	private String workspaceCode;
	private String companyName;
	private String taxCode;
	private String contactEmail;
	private String contactPhone;
	private String address;
	private Boolean active;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public Integer getCompanyId() { return companyId; }
	public void setCompanyId(Integer companyId) { this.companyId = companyId; }
	public String getWorkspaceCode() { return workspaceCode; }
	public void setWorkspaceCode(String workspaceCode) { this.workspaceCode = workspaceCode; }
	public String getCompanyName() { return companyName; }
	public void setCompanyName(String companyName) { this.companyName = companyName; }
	public String getTaxCode() { return taxCode; }
	public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
	public String getContactEmail() { return contactEmail; }
	public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
	public String getContactPhone() { return contactPhone; }
	public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
	public String getAddress() { return address; }
	public void setAddress(String address) { this.address = address; }
	public Boolean getActive() { return active; }
	public void setActive(Boolean active) { this.active = active; }
	public Boolean getIsActive() { return active; }
	public void setIsActive(Boolean active) { this.active = active; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
