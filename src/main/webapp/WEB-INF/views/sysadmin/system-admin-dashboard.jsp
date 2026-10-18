<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
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
        <a class="button button-primary" href="${pageContext.request.contextPath}/systemadmin/registration-request?status=PENDING"><svg class="icon" aria-hidden="true"><use href="${icons}#shield"/></svg>Xem yêu cầu cần xử lý (<c:out value="${stats.pendingCount}"/>)</a>
    </div>
    <section class="stat-grid" aria-label="Tổng quan nền tảng">
        <article class="stat-card"><div class="stat-heading"><h2>Tổng doanh nghiệp<br>(Tenants)</h2><span class="stat-icon"><svg class="icon" aria-hidden="true"><use href="${icons}#building"/></svg></span></div><strong class="stat-value"><c:out value="${stats.companyCount}"/></strong><p class="stat-note">Doanh nghiệp trên nền tảng</p></article>
        <article class="stat-card"><div class="stat-heading"><h2>Yêu cầu cần xử lý</h2><span class="stat-icon"><svg class="icon" aria-hidden="true"><use href="${icons}#requests"/></svg></span></div><strong class="stat-value"><c:out value="${stats.pendingCount}"/></strong><p class="stat-note">Mới tiếp nhận &amp; đang xem xét</p></article>
        <article class="stat-card"><div class="stat-heading"><h2>Tổng tài khoản<br>tenant</h2><span class="stat-icon stat-icon-teal"><svg class="icon" aria-hidden="true"><use href="${icons}#users"/></svg></span></div><strong class="stat-value"><c:out value="${stats.tenantUserCount}"/></strong><p class="stat-note">Không gồm System Admin</p></article>
        <article class="stat-card"><div class="stat-heading"><h2>SQL &amp; Servlet<br>Monitoring</h2><span class="stat-icon"><svg class="icon" aria-hidden="true"><use href="${icons}#server"/></svg></span></div><strong class="stat-value stat-unavailable">—</strong><p class="stat-note">Chưa tích hợp giám sát uptime</p></article>
    </section>
    <section class="panel queue-panel" aria-labelledby="queue-title">
        <div class="panel-heading"><div><h2 id="queue-title">Yêu cầu đăng ký cần xử lý</h2><p class="muted">5 yêu cầu mới nhất đang chờ xử lý</p></div><a class="button button-secondary button-small" href="${pageContext.request.contextPath}/systemadmin/registration-request?status=PENDING">Xem tất cả <c:out value="${stats.pendingCount}"/> yêu cầu<svg class="icon" aria-hidden="true"><use href="${icons}#arrow-right"/></svg></a></div>
        <%@ include file="/WEB-INF/views/sysadmin/request-table.jspf" %>
        <div class="table-footer"><span>Hiển thị <strong><c:out value="${registrationRequests.size()}"/></strong> trên <strong><c:out value="${stats.pendingCount}"/></strong> yêu cầu cần xử lý</span></div>
    </section>
</main>
</body>
</html>
