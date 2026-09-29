package com.hrms.model;

public class Lead {
	private Integer leadId;
	private String fullName;
	private String workEmail;
	private String phoneNumber;
	private String companyName;
	private String companySize;
	private String message;

	public Integer getLeadId() { return leadId; }
	public void setLeadId(Integer leadId) { this.leadId = leadId; }
	public String getFullName() { return fullName; }
	public void setFullName(String fullName) { this.fullName = fullName; }
	public String getWorkEmail() { return workEmail; }
	public void setWorkEmail(String workEmail) { this.workEmail = workEmail; }
	public String getPhoneNumber() { return phoneNumber; }
	public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
	public String getCompanyName() { return companyName; }
	public void setCompanyName(String companyName) { this.companyName = companyName; }
	public String getCompanySize() { return companySize; }
	public void setCompanySize(String companySize) { this.companySize = companySize; }
	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }
}
