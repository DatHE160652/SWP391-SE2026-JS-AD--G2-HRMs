<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>"><head>
<%@ include file="/WEB-INF/views/common/language.jspf" %><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
<meta name="csrf-token" content="<c:out value='${organizationCsrfToken}'/>">
<title><%= com.hrms.i18n.Language.text(request, "ui_202f75f34e4c", "Hồ sơ nhân viên · HRMs Cloud") %></title><link rel="stylesheet" href="${pageContext.request.contextPath}/css/employee-assignment.css"><%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc" data-lifecycle="employees" data-context="${pageContext.request.contextPath}">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
<main class="lc-main">
<div class="lc-breadcrumb"><%= com.hrms.i18n.Language.text(request, "ui_60d4755edd25", "Quản trị nhân sự &nbsp; / &nbsp; Hồ sơ nhân viên") %></div>
<div class="lc-heading"><div><h1><%= com.hrms.i18n.Language.text(request, "ui_56a31d8a7ea2", "Hồ sơ nhân viên") %></h1><p><%= com.hrms.i18n.Language.text(request, "ui_e61381ea1835", "Quản lý thông tin và quá trình công tác của nhân viên") %></p></div>
<div class="lc-actions"><button id="lc-add"><%= com.hrms.i18n.Language.text(request, "ui_8a5334416960", "＋ Thêm nhân viên") %></button><button id="lc-export-employees" type="button" class="secondary" style="display:inline-flex;align-items:center;gap:6px;cursor:pointer;"><%= com.hrms.i18n.Language.text(request, "ui_1721265dcbc2", "Xuất Excel") %></button><button id="lc-import-employees" type="button" class="secondary" style="display:inline-flex;align-items:center;gap:6px;cursor:pointer;"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="8" x2="12" y2="21"/></svg> <%= "en".equals(com.hrms.i18n.Language.selected(request)) ? "Import Excel" : "Nhập Excel" %></button></div></div>
<div id="lc-error" class="lc-alert" role="alert"></div>
<div class="lc-master-detail">
<section class="lc-card"><div class="lc-list-head"><h3><%= com.hrms.i18n.Language.text(request, "ui_0cfe7659f82e", "Danh sách nhân viên") %> <span id="lc-count" class="lc-count">—</span></h3>
<input id="lc-search" type="search" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_41e697588ee0", "Tìm tên hoặc mã nhân viên…") %>" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_be33b65f40bc", "Tìm nhân viên") %>">
<select id="lc-department-filter" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_ff78fac533ca", "Lọc phòng ban") %>"><option value=""><%= com.hrms.i18n.Language.text(request, "ui_1f6697b3badc", "Tất cả phòng ban") %></option></select>
</div><div id="lc-employees" class="lc-list" aria-live="polite"><div class="lc-spinner"><%= com.hrms.i18n.Language.text(request, "ui_0b6748c9e625", "Đang tải nhân viên…") %></div></div>
<nav id="lc-employee-pagination" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_ac2f9cecbe63", "Phân trang danh sách nhân viên") %>"></nav></section>
<section id="lc-profile" class="lc-card"><div class="lc-empty"><%= com.hrms.i18n.Language.text(request, "ui_0e6e85687f18", "Chọn nhân viên để xem hồ sơ") %></div></section>
</div></main>
<dialog id="lc-employee-modal" class="lc-modal" aria-labelledby="lc-modal-title">
<form id="lc-employee-form" novalidate>
<div class="lc-modal-header"><h2 id="lc-modal-title"><%= com.hrms.i18n.Language.text(request, "ui_e528ea19daee", "Thêm nhân viên") %></h2><button type="button" class="lc-close" data-close aria-label="<%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %>"><%= com.hrms.i18n.Language.text(request, "ui_67fba2f357af", "×") %></button></div>
<input type="hidden" name="employeeId"><input type="hidden" name="updatedAt">
<div class="lc-form-section"><div id="lc-form-error" class="lc-alert" role="alert"></div>
<div class="lc-form-grid">
<label><%= com.hrms.i18n.Language.text(request, "ui_84979604c762", "Mã nhân viên") %><input name="employeeCode" required maxlength="50" autocomplete="off"></label>
<label><%= com.hrms.i18n.Language.text(request, "ui_c7cce3468e79", "Họ tên *") %><input name="fullName" required maxlength="150" autocomplete="name"></label>
<label>Email *<input type="email" name="email" required maxlength="150" autocomplete="email"></label>
<label><%= com.hrms.i18n.Language.text(request, "ui_3f7da8d142d0", "Điện thoại *") %><input name="phone" required type="tel" inputmode="numeric" minlength="10" maxlength="10" pattern="0[0-9]{9}" autocomplete="tel"></label>
<label><%= com.hrms.i18n.Language.text(request, "ui_b33375f2d469", "Trạng thái *") %><select name="status" required><option value="Active"><%= com.hrms.i18n.Language.text(request, "ui_a3efad5dad72", "Đang làm việc") %></option><option value="Inactive"><%= com.hrms.i18n.Language.text(request, "ui_ba2e6227ab5f", "Chưa hoạt động") %></option></select></label>
<label><%= com.hrms.i18n.Language.text(request, "ui_ec52cbb131a3", "Ngày sinh *") %><input type="date" name="dateOfBirth" required></label>
<label><%= com.hrms.i18n.Language.text(request, "ui_d7c5567c4a80", "Giới tính *") %><select name="gender" required><option value=""><%= com.hrms.i18n.Language.text(request, "ui_3adbd997d224", "Chọn giới tính") %></option><option value="Male"><%= com.hrms.i18n.Language.text(request, "ui_b580ba1b67f1", "Nam") %></option><option value="Female"><%= com.hrms.i18n.Language.text(request, "ui_feb92ccdb958", "Nữ") %></option><option value="Other"><%= com.hrms.i18n.Language.text(request, "ui_5b8a28abf85c", "Khác") %></option></select></label>
<label><%= com.hrms.i18n.Language.text(request, "ui_7f093927f5a5", "Số CCCD/CMND *") %><input name="identityNumber" required inputmode="numeric" pattern="[0-9]{12}" maxlength="12"></label>
<label><%= com.hrms.i18n.Language.text(request, "ui_002c1cf07b2f", "Ngày cấp CCCD") %><input type="date" name="identityIssuedDate"></label>
<label class="full"><%= com.hrms.i18n.Language.text(request, "ui_a08c3cfb5b79", "Nơi cấp") %><input name="identityIssuedPlace" maxlength="200"></label>
<label class="full"><%= com.hrms.i18n.Language.text(request, "ui_8eddf059a536", "Địa chỉ thường trú") %><input name="permanentAddress" maxlength="500" autocomplete="street-address"></label>
<label><%= com.hrms.i18n.Language.text(request, "ui_f80fa6db5941", "Quê quán") %><input name="hometown" maxlength="200"></label>
<label><%= com.hrms.i18n.Language.text(request, "ui_c19d76692baf", "Ngành học") %><input name="major" maxlength="200"></label>
</div>
</div><div class="lc-modal-footer"><button type="button" class="secondary" data-close><%= com.hrms.i18n.Language.text(request, "ui_34ca764caf22", "Hủy") %></button><button id="lc-save" type="submit"><%= com.hrms.i18n.Language.text(request, "ui_e528ea19daee", "Thêm nhân viên") %></button></div>
</form></dialog>
<template id="lc-organization-template"><%@ include file="/WEB-INF/views/common/employee-organization.jspf" %></template>
<%@ include file="/WEB-INF/views/common/lifecycle-action.jspf" %>
<div id="lc-toast" class="lc-toast" role="status"></div>
<script src="${pageContext.request.contextPath}/js/lifecycle-http.js?v=<%= application.getResource("/js/lifecycle-http.js").openConnection().getLastModified() %>" defer></script>


<%@ include file="/WEB-INF/views/common/import-modal.jspf" %>
<script src="${pageContext.request.contextPath}/js/import-modal.js"></script>
<script>
    document.addEventListener('DOMContentLoaded', function() {
        document.getElementById('lc-import-employees')?.addEventListener('click', function() {
            DataImporter.open('EMPLOYEE', {
                onSuccess: function() {
                    location.reload();
                }
            });
        });
    });
</script>
<script src="${pageContext.request.contextPath}/js/employee.js?v=<%= application.getResource("/js/employee.js").openConnection().getLastModified() %>" defer></script>
<script src="${pageContext.request.contextPath}/js/lifecycle.js?v=<%= application.getResource("/js/lifecycle.js").openConnection().getLastModified() %>" defer></script></body></html>
