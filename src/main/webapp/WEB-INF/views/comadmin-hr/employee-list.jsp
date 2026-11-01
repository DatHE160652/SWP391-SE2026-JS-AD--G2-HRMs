<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
<meta name="csrf-token" content="<c:out value='${organizationCsrfToken}'/>">
<title>Hồ sơ nhân viên · HRMs Cloud</title><link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/css/employee-assignment.css"></head>
<body class="lc" data-lifecycle="employees" data-context="${pageContext.request.contextPath}">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
<main class="lc-main">
<div class="lc-breadcrumb">Quản trị nhân sự &nbsp; / &nbsp; Hồ sơ nhân viên</div>
<div class="lc-heading"><div><h1>Hồ sơ nhân viên</h1><p>Quản lý thông tin và quá trình công tác của nhân viên</p></div>
<div class="lc-actions"><button id="lc-add">＋ Thêm nhân viên</button><button id="lc-export-employees" type="button" class="secondary" style="display:inline-flex;align-items:center;gap:6px;cursor:pointer;">Xuất Excel</button></div></div>
<div id="lc-error" class="lc-alert" role="alert"></div>
<div class="lc-master-detail">
<section class="lc-card"><div class="lc-list-head"><h3>Danh sách nhân viên <span id="lc-count" class="lc-count">—</span></h3>
<input id="lc-search" type="search" placeholder="Tìm tên hoặc mã nhân viên…" aria-label="Tìm nhân viên">
<select id="lc-department-filter" aria-label="Lọc phòng ban"><option value="">Tất cả phòng ban</option></select>
</div><div id="lc-employees" class="lc-list" aria-live="polite"><div class="lc-spinner">Đang tải nhân viên…</div></div>
<nav id="lc-employee-pagination" aria-label="Phân trang danh sách nhân viên"></nav></section>
<section id="lc-profile" class="lc-card"><div class="lc-empty">Chọn nhân viên để xem hồ sơ</div></section>
</div></main>
<dialog id="lc-employee-modal" class="lc-modal" aria-labelledby="lc-modal-title">
<form id="lc-employee-form" novalidate>
<div class="lc-modal-header"><h2 id="lc-modal-title">Thêm nhân viên</h2><button type="button" class="lc-close" data-close aria-label="Đóng">×</button></div>
<input type="hidden" name="employeeId"><input type="hidden" name="updatedAt">
<div class="lc-form-section"><div id="lc-form-error" class="lc-alert" role="alert"></div>
<div class="lc-form-grid">
<label>Mã nhân viên<input name="employeeCode" required maxlength="50" autocomplete="off"></label>
<label>Họ tên *<input name="fullName" required maxlength="150" autocomplete="name"></label>
<label>Email<input type="email" name="email" maxlength="150" autocomplete="email"></label>
<label>Điện thoại *<input name="phone" required type="tel" inputmode="numeric" minlength="10" maxlength="10" pattern="0[0-9]{9}" autocomplete="tel"></label>
<label>Trạng thái *<select name="status" required><option value="Active">Đang làm việc</option><option value="Inactive">Chưa hoạt động</option></select></label>
<label>Ngày sinh *<input type="date" name="dateOfBirth" required></label>
<label>Giới tính *<select name="gender" required><option value="">Chọn giới tính</option><option value="Male">Nam</option><option value="Female">Nữ</option><option value="Other">Khác</option></select></label>
<label>Số CCCD/CMND *<input name="identityNumber" required inputmode="numeric" pattern="[0-9]{12}" maxlength="12"></label>
<label>Ngày cấp CCCD<input type="date" name="identityIssuedDate"></label>
<label class="full">Nơi cấp<input name="identityIssuedPlace" maxlength="200"></label>
<label class="full">Địa chỉ thường trú<input name="permanentAddress" maxlength="500" autocomplete="street-address"></label>
<label>Quê quán<input name="hometown" maxlength="200"></label>
<label>Ngành học<input name="major" maxlength="200"></label>
</div>
</div><div class="lc-modal-footer"><button type="button" class="secondary" data-close>Hủy</button><button id="lc-save" type="submit">Thêm nhân viên</button></div>
</form></dialog>
<template id="lc-organization-template"><%@ include file="/WEB-INF/views/common/employee-organization.jspf" %></template>
<%@ include file="/WEB-INF/views/common/lifecycle-action.jspf" %>
<div id="lc-toast" class="lc-toast" role="status"></div>
<script src="${pageContext.request.contextPath}/js/lifecycle-http.js?v=<%= application.getResource("/js/lifecycle-http.js").openConnection().getLastModified() %>" defer></script>
<script src="${pageContext.request.contextPath}/js/employee.js?v=<%= application.getResource("/js/employee.js").openConnection().getLastModified() %>" defer></script>
<script src="${pageContext.request.contextPath}/js/lifecycle.js?v=<%= application.getResource("/js/lifecycle.js").openConnection().getLastModified() %>" defer></script></body></html>
