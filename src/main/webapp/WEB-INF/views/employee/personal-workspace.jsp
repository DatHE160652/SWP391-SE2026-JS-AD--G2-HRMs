<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
<title>${personalView == 'contracts' ? 'Hợp đồng của tôi' : 'Sơ đồ tổ chức'} · HRMs Cloud</title>
<link rel="stylesheet" href="../css/lifecycle.css"><link rel="stylesheet" href="../css/organization.css">
<link rel="stylesheet" href="../css/profile-update.css"><link rel="stylesheet" href="../css/organization-explorer.css">
<link rel="stylesheet" href="../css/employee-navigation.css?v=<%= application.getResource("/css/employee-navigation.css").openConnection().getLastModified() %>">
<link rel="stylesheet" href="../css/personal-workspace.css?v=<%= application.getResource("/css/personal-workspace.css").openConnection().getLastModified() %>">
</head><body class="lc" data-page="personal-workspace" data-personal-view="${personalView}" data-profile-role="<c:out value='${sessionScope.tenantPrincipal.roleCode}'/>">
<c:set var="employeeNavPage" value="${personalView}"/>
<%@ include file="/WEB-INF/views/common/employee-navigation.jspf" %>
<aside class="lc-sidebar"><a class="lc-brand" href="my-profile"><span class="lc-logo">H</span><span class="lc-brand-name">HRMs Cloud</span></a><nav aria-label="Chức năng cá nhân"></nav><a class="lc-signout" href="../logout">↪ <span>Đăng xuất</span></a></aside>
<header class="lc-topbar"><span>Cổng cá nhân</span><span class="lc-workspace">Workspace · <c:out value="${sessionScope.tenantPrincipal.workspaceCode}" default="—"/></span></header>
<%@ include file="/WEB-INF/views/common/appearance.jspf" %>
<main class="lc-main"><div class="lc-breadcrumb">Cổng nhân viên / <span id="personalBreadcrumb"></span></div>
<div class="lc-heading"><div><h1 id="personalPageTitle"></h1><p id="personalPageDescription"></p></div></div>
<div id="personalPageError" class="lc-alert" role="alert"></div><div id="personalWorkspaceContent" aria-live="polite"><div class="lc-spinner">Đang tải dữ liệu…</div></div></main>
<dialog id="personalEmployeeDialog" class="lc-modal" aria-labelledby="personalEmployeeTitle"><header class="lc-modal-header"><h2 id="personalEmployeeTitle">Hồ sơ nhân viên</h2><button type="button" class="lc-close" aria-label="Đóng">×</button></header><div class="lc-panel-body" id="personalEmployeeBody"></div></dialog>
<script src="../js/employee.js?v=<%= application.getResource("/js/employee.js").openConnection().getLastModified() %>"></script>
<script src="../js/organization-chart.js?v=<%= application.getResource("/js/organization-chart.js").openConnection().getLastModified() %>"></script>
<script src="../js/organization-explorer.js?v=<%= application.getResource("/js/organization-explorer.js").openConnection().getLastModified() %>"></script>
<script src="../js/personal-workspace.js?v=<%= application.getResource("/js/personal-workspace.js").openConnection().getLastModified() %>"></script>
</body></html>
