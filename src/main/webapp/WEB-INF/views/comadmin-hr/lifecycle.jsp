<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
<meta name="csrf-token" content="<c:out value='${organizationCsrfToken}'/>">
<title>Quản trị nhân sự · HRMs Cloud</title><link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css?v=<%= application.getResource("/css/lifecycle.css").openConnection().getLastModified() %>"><link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization-explorer.css?v=<%= application.getResource("/css/organization-explorer.css").openConnection().getLastModified() %>"><link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css?v=<%= application.getResource("/css/dashboard.css").openConnection().getLastModified() %>"></head>
<body class="lc" data-lifecycle="<c:out value='${lifecycleResource}'/>" data-context="${pageContext.request.contextPath}" data-role="<c:out value='${sessionScope.tenantPrincipal.roleCode}'/>">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
<main class="lc-main"><div class="lc-breadcrumb">Quản trị nhân sự &nbsp; / &nbsp; <span id="lc-page-label"></span></div>
<div class="lc-heading"><div><h1 id="lc-page-title">Đang tải…</h1><p id="lc-page-subtitle"></p></div><div class="lc-actions"><button id="lc-page-add" hidden>＋ Tạo hợp đồng</button><button id="lc-page-export" class="secondary" hidden style="display:inline-flex;align-items:center;gap:6px;cursor:pointer;">Xuất Excel</button></div></div>
<div id="lc-error" class="lc-alert" role="alert"></div><div id="lc-page-content"><div class="lc-spinner">Đang tải dữ liệu…</div></div></main>
<dialog id="lc-employee-contracts" class="lc-modal lc-employee-contracts" aria-labelledby="lc-employee-contracts-title">
<div class="lc-modal-header"><div><h2 id="lc-employee-contracts-title">Hợp đồng của nhân viên</h2><p id="lc-employee-contracts-summary" class="lc-muted"></p></div><button class="lc-close" type="button" data-close aria-label="Đóng danh sách hợp đồng">×</button></div>
<div id="lc-employee-contracts-list"></div>
<div class="lc-modal-footer"><button type="button" data-close class="secondary">Đóng</button></div>
</dialog>
<dialog id="lc-action-modal" class="lc-modal" aria-labelledby="lc-action-title"><form id="lc-action-form">
<div class="lc-modal-header"><h2 id="lc-action-title"></h2><button class="lc-close" type="button" data-close aria-label="Đóng">×</button></div>
<div class="lc-form-section"><div id="lc-action-error" class="lc-alert" role="alert"></div><div id="lc-action-fields" class="lc-form-grid"></div></div>
<div class="lc-modal-footer"><button type="button" data-close class="secondary">Đóng</button><button id="lc-action-save" type="submit">Lưu</button></div>
</form></dialog><div id="lc-toast" class="lc-toast" role="status"></div>
<script src="${pageContext.request.contextPath}/js/lifecycle-http.js?v=<%= application.getResource("/js/lifecycle-http.js").openConnection().getLastModified() %>" defer></script>
<script src="${pageContext.request.contextPath}/js/organization-chart.js?v=<%= application.getResource("/js/organization-chart.js").openConnection().getLastModified() %>" defer></script>
<script src="${pageContext.request.contextPath}/js/organization-explorer.js?v=<%= application.getResource("/js/organization-explorer.js").openConnection().getLastModified() %>" defer></script>
<script src="${pageContext.request.contextPath}/js/dashboard.js?v=<%= application.getResource("/js/dashboard.js").openConnection().getLastModified() %>" defer></script>
<script src="${pageContext.request.contextPath}/js/lifecycle.js?v=<%= application.getResource("/js/lifecycle.js").openConnection().getLastModified() %>" defer></script></body></html>
