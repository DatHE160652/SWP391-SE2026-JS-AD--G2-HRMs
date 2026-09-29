package com.hrms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Subscription {
	private Integer subscriptionId;
	private Integer companyId;
	private Integer planId;
	private LocalDate startDate;
	private LocalDate endDate;
	private String status;
	private String paymentMethod;
	private LocalDateTime createdAt;

	public Integer getSubscriptionId() { return subscriptionId; }
	public void setSubscriptionId(Integer subscriptionId) { this.subscriptionId = subscriptionId; }
	public Integer getCompanyId() { return companyId; }
	public void setCompanyId(Integer companyId) { this.companyId = companyId; }
	public Integer getPlanId() { return planId; }
	public void setPlanId(Integer planId) { this.planId = planId; }
	public LocalDate getStartDate() { return startDate; }
	public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
	public LocalDate getEndDate() { return endDate; }
	public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public String getPaymentMethod() { return paymentMethod; }
	public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
