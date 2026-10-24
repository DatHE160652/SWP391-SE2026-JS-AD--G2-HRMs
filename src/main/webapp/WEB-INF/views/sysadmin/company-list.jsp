<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý Doanh nghiệp | HRMs.Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/system-admin.css?v=20261020-company-toolbar">
</head>
<body class="admin-page company-page company-list-page">
<%@ include file="/WEB-INF/views/sysadmin/admin-shell.jspf" %>
<main class="admin-main" id="main-content">
    <header class="company-page-title">
        <nav class="breadcrumb" aria-label="Đường dẫn"><a href="${pageContext.request.contextPath}/systemadmin/dashboard">Quản lý tenant</a><svg class="icon" aria-hidden="true"><use href="${icons}#chevron"/></svg><span>Quản lý công ty</span></nav>
        <h1>Quản lý Doanh nghiệp </h1>
    </header>

    <c:set var="activeCount" value="0"/>
    <c:set var="suspendedCount" value="0"/>
    <c:set var="accountTotal" value="0"/>
    <c:set var="quotaTotal" value="0"/>
    <c:forEach var="item" items="${allCompanies}">
        <c:if test="${item.subscriptionActive}"><c:set var="activeCount" value="${activeCount + 1}"/></c:if>
        <c:if test="${item.subscriptionSuspended}"><c:set var="suspendedCount" value="${suspendedCount + 1}"/></c:if>
        <c:set var="accountTotal" value="${accountTotal + item.accountCount}"/>
        <c:set var="quotaTotal" value="${quotaTotal + item.maxAccounts}"/>
    </c:forEach>

    <section class="company-stat-grid" aria-label="Tổng quan Company và Subscription">
        <article class="company-stat-card">
            <div><h2>TỔNG DOANH NGHIỆP<br><span>(TENANTS)</span></h2><strong><c:out value="${allCompanies.size()}"/></strong></div>
            <span class="company-stat-icon icon-blue"><svg class="icon" aria-hidden="true"><use href="${icons}#building"/></svg></span>
            <p>Doanh nghiệp trên nền tảng</p>
        </article>
        <article class="company-stat-card">
            <div><h2>SUBSCRIPTION ACTIVE</h2><strong class="text-success"><c:out value="${activeCount}"/></strong></div>
            <span class="company-stat-icon icon-success"><svg class="icon" aria-hidden="true"><use href="${icons}#check"/></svg></span>
            <p>Đang trả phí 59.000 đ/tháng</p>
        </article>
        <article class="company-stat-card">
            <div><h2>TẠM NGƯNG (SUSPENDED)</h2><strong class="text-warning"><c:out value="${suspendedCount}"/></strong></div>
            <span class="company-stat-icon icon-warning"><svg class="icon" aria-hidden="true"><use href="${icons}#info"/></svg></span>
            <p>Khóa truy cập workspace</p>
        </article>
        <article class="company-stat-card">
            <div><h2>TỔNG TÀI KHOẢN<br><span>TENANT</span></h2><strong><c:out value="${accountTotal}"/> <em>/ <c:out value="${quotaTotal}"/></em></strong></div>
            <span class="company-stat-icon icon-teal"><svg class="icon" aria-hidden="true"><use href="${icons}#users"/></svg></span>
            <p>Định mức tối đa 20 acc/doanh nghiệp</p>
        </article>
    </section>

    <section class="company-toolbar" aria-label="Bộ lọc hiển thị Company">
        <div class="company-tabs" role="group" aria-label="Lọc Subscription">
            <c:url var="allCompaniesUrl" value="/systemadmin/company"><c:param name="region" value="${filter.region}"/><c:param name="sort" value="${filter.sort}"/></c:url>
            <c:url var="activeCompaniesUrl" value="/systemadmin/company"><c:param name="status" value="ACTIVE"/><c:param name="region" value="${filter.region}"/><c:param name="sort" value="${filter.sort}"/></c:url>
            <c:url var="suspendedCompaniesUrl" value="/systemadmin/company"><c:param name="status" value="SUSPENDED"/><c:param name="region" value="${filter.region}"/><c:param name="sort" value="${filter.sort}"/></c:url>
            <a class="${filter.allStatuses ? 'selected' : ''}" aria-current="${filter.allStatuses ? 'page' : 'false'}" href="<c:out value='${allCompaniesUrl}'/>">Tất cả (<c:out value="${allCompanies.size()}"/>)</a>
            <a class="${filter.active ? 'selected' : ''}" aria-current="${filter.active ? 'page' : 'false'}" href="<c:out value='${activeCompaniesUrl}'/>">Đang hoạt động (<c:out value="${activeCount}"/>)</a>
            <a class="${filter.suspended ? 'selected' : ''}" aria-current="${filter.suspended ? 'page' : 'false'}" href="<c:out value='${suspendedCompaniesUrl}'/>">Đã tạm ngưng (<c:out value="${suspendedCount}"/>)</a>
        </div>
        <form class="company-toolbar-fields" method="get" action="${pageContext.request.contextPath}/systemadmin/company">
            <input type="hidden" name="status" value="<c:out value='${filter.status}'/>">
            <label for="company-region">Khu vực:<select id="company-region" name="region" onchange="this.form.submit()"><option value="" ${empty filter.region ? 'selected' : ''}>Tất cả vùng</option><option value="NORTH" ${filter.region == 'NORTH' ? 'selected' : ''}>Miền Bắc</option><option value="CENTRAL" ${filter.region == 'CENTRAL' ? 'selected' : ''}>Miền Trung</option><option value="SOUTH" ${filter.region == 'SOUTH' ? 'selected' : ''}>Miền Nam</option></select></label>
            <label for="company-sort">Sắp xếp:<select id="company-sort" name="sort" onchange="this.form.submit()"><option value="NEWEST" ${filter.sort == 'NEWEST' ? 'selected' : ''}>Mới nhất</option><option value="ACCOUNT_DESC" ${filter.sort == 'ACCOUNT_DESC' ? 'selected' : ''}>Số lượng User giảm dần</option><option value="NAME_ASC" ${filter.sort == 'NAME_ASC' ? 'selected' : ''}>Tên doanh nghiệp (A-Z)</option></select></label>
            <noscript><button class="button button-small" type="submit">Áp dụng</button></noscript>
        </form>
    </section>

    <section class="company-list-panel" aria-label="Danh sách Company">
        <c:choose>
            <c:when test="${empty companies}">
                <div class="empty-state"><svg class="icon" aria-hidden="true"><use href="${icons}#building"/></svg><h3><c:choose><c:when test="${empty allCompanies}">Chưa có Company</c:when><c:otherwise>Không có Công ty phù hợp</c:otherwise></c:choose></h3><p><c:choose><c:when test="${empty allCompanies}">Công ty sẽ xuất hiện sau khi Registration Request được phê duyệt.</c:when><c:otherwise>Hãy thay đổi trạng thái Subscription hoặc khu vực để xem Công ty khác.</c:otherwise></c:choose></p></div>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="company-table">
                        <thead><tr><th scope="col">Mã workspace</th><th scope="col">Doanh nghiệp SME</th><th scope="col">Đại diện (Company Admin)</th><th scope="col">Khu vực</th><th scope="col">Tài khoản</th><th scope="col">Subscription</th><th scope="col" class="company-action-heading">Thao tác</th></tr></thead>
                        <tbody>
                        <c:forEach var="item" items="${companies}">
                            <tr class="${item.subscriptionSuspended ? 'subscription-suspended-row' : ''}">
                                <td><strong class="workspace-code ${item.subscriptionSuspended ? 'workspace-warning' : ''}"><c:out value="${item.workspaceCode}"/></strong></td>
                                <td class="company-name"><c:out value="${item.companyName}"/></td>
                                <td><strong class="company-representative"><c:out value="${item.companyAdminName}" default="—"/></strong><span class="company-email"><c:out value="${item.companyAdminEmail}" default="—"/></span></td>
                                <td><span class="region-label"><c:out value="${item.regionLabel}"/></span></td>
                                <td><div class="account-usage"><span><strong><c:out value="${item.accountCount}"/> / <c:out value="${item.maxAccounts}"/></strong><em class="${item.accountUsagePercent >= 90 ? 'near-limit' : ''}"><c:out value="${item.accountUsagePercent}"/>%</em></span><i><b style="width:${item.accountUsagePercent}%"></b></i></div></td>
                                <td><span class="subscription-status ${item.subscriptionActive ? 'is-active' : 'is-suspended'}"><i></i><c:out value="${item.subscriptionStatus}"/></span><c:if test="${item.subscriptionSuspended}"><small class="suspension-reason"><c:out value="${item.suspensionReason}" default="Đã tạm ngưng dịch vụ"/></small></c:if></td>
                                <td class="company-action"><a class="company-details-button" href="${pageContext.request.contextPath}/systemadmin/company-details?id=${item.companyId}">Chi tiết <svg class="icon" aria-hidden="true"><use href="${icons}#arrow-right"/></svg></a></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <footer class="company-table-footer"><span>Hiển thị <strong><c:out value="${companies.size()}"/></strong> trên tổng số <strong><c:out value="${allCompanies.size()}"/></strong> doanh nghiệp SME đã tạo</span><nav class="company-pagination" aria-label="Phân trang"><button type="button" disabled aria-label="Trang trước"><svg class="icon" aria-hidden="true"><use href="${icons}#arrow-left"/></svg></button><strong>1</strong><span>/ 1</span><button type="button" disabled aria-label="Trang sau"><svg class="icon" aria-hidden="true"><use href="${icons}#arrow-right"/></svg></button></nav></footer>
            </c:otherwise>
        </c:choose>
    </section>
</main>
</body>
</html>
