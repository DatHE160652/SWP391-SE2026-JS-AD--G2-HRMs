<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Dashboard | HRMs.Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/system-admin.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/views/sysadmin/admin-shell.jspf" %>
<main class="admin-main" id="main-content">
    <div class="page-heading">
        <h1>System Admin Overview</h1>
        <div class="dashboard-actions"><a class="button" href="${pageContext.request.contextPath}/systemadmin/company"><svg class="icon" aria-hidden="true"><use href="${icons}#building"/></svg><%= com.hrms.i18n.Language.text(request, "ui_47414364e415", "Xem danh sách Company") %></a><a class="button button-primary" href="${pageContext.request.contextPath}/systemadmin/registration-request?status=PENDING"><svg class="icon" aria-hidden="true"><use href="${icons}#shield"/></svg><%= com.hrms.i18n.Language.text(request, "ui_c809e872dcb8", "Xem yêu cầu cần xử lý (") %><c:out value="${stats.pendingCount}"/>)</a></div>
    </div>
    <section class="stat-grid" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_b48e56206b64", "Tổng quan nền tảng") %>">
        <article class="stat-card"><div class="stat-heading"><h2><%= com.hrms.i18n.Language.text(request, "ui_cba9134dc8a9", "Tổng doanh nghiệp") %><br>(Tenants)</h2><span class="stat-icon"><svg class="icon" aria-hidden="true"><use href="${icons}#building"/></svg></span></div><strong class="stat-value"><c:out value="${stats.companyCount}"/></strong><p class="stat-note"><%= com.hrms.i18n.Language.text(request, "ui_d5237cedc868", "Doanh nghiệp trên nền tảng") %></p></article>
        <article class="stat-card"><div class="stat-heading"><h2><%= com.hrms.i18n.Language.text(request, "ui_7ada1b678cbe", "Yêu cầu cần xử lý") %></h2><span class="stat-icon"><svg class="icon" aria-hidden="true"><use href="${icons}#requests"/></svg></span></div><strong class="stat-value"><c:out value="${stats.pendingCount}"/></strong><p class="stat-note"><%= com.hrms.i18n.Language.text(request, "ui_edf31457f101", "Mới tiếp nhận &amp; đang xem xét") %></p></article>
        <article class="stat-card"><div class="stat-heading"><h2><%= com.hrms.i18n.Language.text(request, "ui_942a62687504", "Tổng tài khoản") %><br>tenant</h2><span class="stat-icon stat-icon-teal"><svg class="icon" aria-hidden="true"><use href="${icons}#users"/></svg></span></div><strong class="stat-value"><c:out value="${stats.tenantUserCount}"/></strong><p class="stat-note"><%= com.hrms.i18n.Language.text(request, "ui_21300b107475", "Không gồm System Admin") %></p></article>
        <article class="stat-card"><div class="stat-heading"><h2>SQL &amp; Servlet<br>Monitoring</h2><span class="stat-icon"><svg class="icon" aria-hidden="true"><use href="${icons}#server"/></svg></span></div><strong class="stat-value stat-unavailable">—</strong><p class="stat-note"><%= com.hrms.i18n.Language.text(request, "ui_478194c57d0f", "Chưa tích hợp giám sát uptime") %></p></article>
    </section>
    <section class="panel queue-panel" aria-labelledby="queue-title">
        <div class="panel-heading"><div><h2 id="queue-title"><%= com.hrms.i18n.Language.text(request, "ui_8201219860e5", "Yêu cầu đăng ký cần xử lý") %></h2><p class="muted"><%= com.hrms.i18n.Language.text(request, "ui_f5f7112a0434", "5 yêu cầu mới nhất đang chờ xử lý") %></p></div><a class="button button-secondary button-small" href="${pageContext.request.contextPath}/systemadmin/registration-request?status=PENDING"><%= com.hrms.i18n.Language.text(request, "ui_7e040254521e", "Xem tất cả") %> <c:out value="${stats.pendingCount}"/> <%= com.hrms.i18n.Language.text(request, "ui_f970f45deb01", "yêu cầu") %><svg class="icon" aria-hidden="true"><use href="${icons}#arrow-right"/></svg></a></div>
        <%@ include file="/WEB-INF/views/sysadmin/request-table.jspf" %>
        <div class="table-footer"><span><%= com.hrms.i18n.Language.text(request, "ui_2a48ea973ecf", "Hiển thị") %> <strong><c:out value="${registrationRequests.size()}"/></strong> <%= com.hrms.i18n.Language.text(request, "ui_3f257ea5dc0a", "trên") %> <strong><c:out value="${stats.pendingCount}"/></strong> <%= com.hrms.i18n.Language.text(request, "ui_d97f4bcd91e3", "yêu cầu cần xử lý") %></span></div>
    </section>
</main>
</body>
</html>
