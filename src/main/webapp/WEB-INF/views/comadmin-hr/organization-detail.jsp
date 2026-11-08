<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>"><head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
<title><%= com.hrms.i18n.Language.text(request, "ui_4087262b773c", "Chi tiết cơ cấu tổ chức") %></title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css?v=<%= application.getResource("/css/organization.css").openConnection().getLastModified() %>">
</head><body class="lc" data-page="organization-detail" data-context="${pageContext.request.contextPath}">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
<main class="lc-main content">
<div class="lc-breadcrumb"><%= com.hrms.i18n.Language.text(request, "ui_a84e3aeecd55", "Quản trị nhân sự / Cơ cấu tổ chức / Chi tiết") %></div>
<div class="page-header"><h1 id="detailTitle" class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_d72a6317ec65", "Chi tiết") %></h1><div class="detail-actions"><a id="detailBack" class="button secondary"><%= com.hrms.i18n.Language.text(request, "ui_8a09e03d2052", "Quay lại") %></a><a id="detailEdit" class="button" hidden><%= com.hrms.i18n.Language.text(request, "ui_b59e6523fc38", "Chỉnh sửa") %></a></div></div>
<div id="message" class="message" role="status"></div>
<section class="panel"><dl id="detailFields" class="form-grid"><div><%= com.hrms.i18n.Language.text(request, "ui_577a1d591051", "Đang tải…") %></div></dl></section>
<div id="departmentPositionLinks" class="actions"></div>
<button type="button" id="departmentInitialAssignment" hidden><%= com.hrms.i18n.Language.text(request, "ui_965d995c503c", "Phân công nhân viên") %></button>
<section id="departmentMembersPanel" class="panel hidden"><h2><%= com.hrms.i18n.Language.text(request, "ui_8bcbf1c445d1", "Nhân viên trong phòng ban") %> <span id="departmentMemberCount" class="lc-count">0</span></h2><div id="departmentMemberList" aria-live="polite"></div></section>
</main>
<script src="${pageContext.request.contextPath}/js/department.js?v=<%= application.getResource("/js/department.js").openConnection().getLastModified() %>"></script>
<%@ include file="/WEB-INF/views/common/initial-assignment.jspf" %>
<script src="${pageContext.request.contextPath}/js/organization-detail.js?v=<%= application.getResource("/js/organization-detail.js").openConnection().getLastModified() %>" defer></script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js?v=<%= application.getResource("/js/organization-shell.js").openConnection().getLastModified() %>" defer></script>
</body></html>
