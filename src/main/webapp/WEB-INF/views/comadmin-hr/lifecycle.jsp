<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="<%= com.hrms.i18n.Language.selected(request) %>"><head>
<%@ include file="/WEB-INF/views/common/language.jspf" %><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
<meta name="csrf-token" content="<c:out value='${organizationCsrfToken}'/>">
<title><%= com.hrms.i18n.Language.text(request, "ui_e2fae15bcdca", "Quản trị nhân sự · HRMs Cloud") %></title><link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css?v=<%= application.getResource("/css/lifecycle.css").openConnection().getLastModified() %>"><link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization-explorer.css?v=<%= application.getResource("/css/organization-explorer.css").openConnection().getLastModified() %>"><link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css?v=<%= application.getResource("/css/dashboard.css").openConnection().getLastModified() %>"></head>
<body class="lc" data-lifecycle="<c:out value='${lifecycleResource}'/>" data-context="${pageContext.request.contextPath}" data-role="<c:out value='${sessionScope.tenantPrincipal.roleCode}'/>">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
<main class="lc-main"><div class="lc-breadcrumb"><%= com.hrms.i18n.Language.text(request, "ui_19a324d31030", "Quản trị nhân sự &nbsp; / &nbsp;") %> <span id="lc-page-label"></span></div>
<div class="lc-heading"><div><h1 id="lc-page-title"><%= com.hrms.i18n.Language.text(request, "ui_577a1d591051", "Đang tải…") %></h1><p id="lc-page-subtitle"></p></div><div class="lc-actions"><button type="button" id="lc-unregistered-accounts-open" hidden aria-haspopup="dialog" aria-controls="lc-unregistered-accounts"><%= com.hrms.i18n.Language.text(request, "ui_99262203c916", "＋ Tạo tài khoản") %></button><button id="lc-page-add" hidden><%= com.hrms.i18n.Language.text(request, "ui_7125f4067a16", "＋ Tạo hợp đồng") %></button><button id="lc-page-export" class="secondary" hidden style="display:inline-flex;align-items:center;gap:6px;cursor:pointer;"><%= com.hrms.i18n.Language.text(request, "ui_1721265dcbc2", "Xuất Excel") %></button></div></div>
<div id="lc-error" class="lc-alert" role="alert"></div><div id="lc-page-content"><div class="lc-spinner"><%= com.hrms.i18n.Language.text(request, "ui_e6b584854879", "Đang tải dữ liệu…") %></div></div></main>
<dialog id="lc-employee-contracts" class="lc-modal lc-employee-contracts" aria-labelledby="lc-employee-contracts-title">
<div class="lc-modal-header"><div><h2 id="lc-employee-contracts-title"><%= com.hrms.i18n.Language.text(request, "ui_0fa5602ac885", "Hợp đồng của nhân viên") %></h2><p id="lc-employee-contracts-summary" class="lc-muted"></p></div><button class="lc-close" type="button" data-close aria-label="<%= com.hrms.i18n.Language.text(request, "ui_2a52667f673a", "Đóng danh sách hợp đồng") %>"><%= com.hrms.i18n.Language.text(request, "ui_67fba2f357af", "×") %></button></div>
<div id="lc-employee-contracts-list"></div>
<div class="lc-modal-footer"><button type="button" data-close class="secondary"><%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %></button></div>
</dialog>
<dialog id="lc-unregistered-accounts" class="lc-modal lc-unregistered-accounts" aria-labelledby="lc-unregistered-accounts-title">
<div class="lc-modal-header"><div><h2 id="lc-unregistered-accounts-title"><%= com.hrms.i18n.Language.text(request, "ui_e1fd1bd23dc3", "Nhân viên chưa có tài khoản") %></h2><p id="lc-unregistered-accounts-summary" class="lc-muted" role="status"></p></div><button class="lc-close" type="button" data-close aria-label="<%= com.hrms.i18n.Language.text(request, "ui_5a6e84b06722", "Đóng danh sách nhân viên") %>"><%= com.hrms.i18n.Language.text(request, "ui_67fba2f357af", "×") %></button></div>
<div class="lc-filterbar"><input type="search" id="lc-unregistered-accounts-search" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_c777992d746e", "Tìm tên, mã nhân viên, email…") %>" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_a7096aec1730", "Tìm nhân viên chưa có tài khoản") %>"></div>
<div id="lc-unregistered-accounts-list"></div>
<div class="lc-modal-footer"><button type="button" data-close class="secondary"><%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %></button></div>
</dialog>
<dialog id="lc-action-modal" class="lc-modal" aria-labelledby="lc-action-title"><form id="lc-action-form">
<div class="lc-modal-header"><h2 id="lc-action-title"></h2><button class="lc-close" type="button" data-close aria-label="<%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %>"><%= com.hrms.i18n.Language.text(request, "ui_67fba2f357af", "×") %></button></div>
<div class="lc-form-section"><div id="lc-action-error" class="lc-alert" role="alert"></div><div id="lc-action-fields" class="lc-form-grid"></div></div>
<div class="lc-modal-footer"><button type="button" data-close class="secondary"><%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %></button><button id="lc-action-save" type="submit"><%= com.hrms.i18n.Language.text(request, "ui_a306970e8b81", "Lưu") %></button></div>
</form></dialog><div id="lc-toast" class="lc-toast" role="status"></div>
<script src="${pageContext.request.contextPath}/js/lifecycle-http.js?v=<%= application.getResource("/js/lifecycle-http.js").openConnection().getLastModified() %>" defer></script>
<script src="${pageContext.request.contextPath}/js/organization-chart.js?v=<%= application.getResource("/js/organization-chart.js").openConnection().getLastModified() %>" defer></script>
<script src="${pageContext.request.contextPath}/js/organization-explorer.js?v=<%= application.getResource("/js/organization-explorer.js").openConnection().getLastModified() %>" defer></script>
<script src="${pageContext.request.contextPath}/js/dashboard.js?v=<%= application.getResource("/js/dashboard.js").openConnection().getLastModified() %>" defer></script>


<script src="${pageContext.request.contextPath}/js/lifecycle.js?v=<%= application.getResource("/js/lifecycle.js").openConnection().getLastModified() %>" defer></script></body></html>
