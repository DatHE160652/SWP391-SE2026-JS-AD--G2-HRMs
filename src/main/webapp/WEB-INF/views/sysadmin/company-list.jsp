<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= com.hrms.i18n.Language.text(request, "ui_932ff9068f27", "Quản lý Doanh nghiệp | HRMs.Admin") %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/system-admin.css?v=20261020-company-toolbar">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc workspace-admin admin-page company-page company-list-page">
<%@ include file="/WEB-INF/views/sysadmin/admin-shell.jspf" %>
<main class="lc-main admin-main" id="main-content">
    <header class="company-page-title">
        <nav class="breadcrumb" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_50de7d02dcaa", "Đường dẫn") %>"><a href="${pageContext.request.contextPath}/systemadmin/dashboard"><%= com.hrms.i18n.Language.text(request, "ui_8201002cd9e8", "Quản lý tenant") %></a><svg class="icon" aria-hidden="true"><use href="${icons}#chevron"/></svg><span><%= com.hrms.i18n.Language.text(request, "ui_2093bd912e40", "Quản lý công ty") %></span></nav>
        <h1><%= com.hrms.i18n.Language.text(request, "ui_f202a77ef7d0", "Quản lý Doanh nghiệp") %> </h1>
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

    <section class="company-stat-grid" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_350313380897", "Tổng quan Company và Subscription") %>">
        <article class="company-stat-card">
            <div><h2><%= com.hrms.i18n.Language.text(request, "ui_59094e812194", "TỔNG DOANH NGHIỆP") %><br><span>(TENANTS)</span></h2><strong><c:out value="${allCompanies.size()}"/></strong></div>
            <span class="company-stat-icon icon-blue"><svg class="icon" aria-hidden="true"><use href="${icons}#building"/></svg></span>
            <p><%= com.hrms.i18n.Language.text(request, "ui_d5237cedc868", "Doanh nghiệp trên nền tảng") %></p>
        </article>
        <article class="company-stat-card">
            <div><h2>SUBSCRIPTION ACTIVE</h2><strong class="text-success"><c:out value="${activeCount}"/></strong></div>
            <span class="company-stat-icon icon-success"><svg class="icon" aria-hidden="true"><use href="${icons}#check"/></svg></span>
            <p><%= com.hrms.i18n.Language.text(request, "ui_d069de924b74", "Đang trả phí 59.000 đ/tháng") %></p>
        </article>
        <article class="company-stat-card">
            <div><h2><%= com.hrms.i18n.Language.text(request, "ui_355fdeb4a49f", "TẠM NGƯNG (SUSPENDED)") %></h2><strong class="text-warning"><c:out value="${suspendedCount}"/></strong></div>
            <span class="company-stat-icon icon-warning"><svg class="icon" aria-hidden="true"><use href="${icons}#info"/></svg></span>
            <p><%= com.hrms.i18n.Language.text(request, "ui_9fa6e0a6487e", "Khóa truy cập workspace") %></p>
        </article>
        <article class="company-stat-card">
            <div><h2><%= com.hrms.i18n.Language.text(request, "ui_4e2682a92251", "TỔNG TÀI KHOẢN") %><br><span>TENANT</span></h2><strong><c:out value="${accountTotal}"/> <em>/ <c:out value="${quotaTotal}"/></em></strong></div>
            <span class="company-stat-icon icon-teal"><svg class="icon" aria-hidden="true"><use href="${icons}#users"/></svg></span>
            <p><%= com.hrms.i18n.Language.text(request, "ui_3943f92c7403", "Định mức tối đa 20 acc/doanh nghiệp") %></p>
        </article>
    </section>

    <section class="company-toolbar" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_695ea656a152", "Bộ lọc hiển thị Company") %>">
        <div class="company-tabs" role="group" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_bc10468c4920", "Lọc Subscription") %>">
            <c:url var="allCompaniesUrl" value="/systemadmin/company"><c:param name="region" value="${filter.region}"/><c:param name="sort" value="${filter.sort}"/></c:url>
            <c:url var="activeCompaniesUrl" value="/systemadmin/company"><c:param name="status" value="ACTIVE"/><c:param name="region" value="${filter.region}"/><c:param name="sort" value="${filter.sort}"/></c:url>
            <c:url var="suspendedCompaniesUrl" value="/systemadmin/company"><c:param name="status" value="SUSPENDED"/><c:param name="region" value="${filter.region}"/><c:param name="sort" value="${filter.sort}"/></c:url>
            <a class="${filter.allStatuses ? 'selected' : ''}" aria-current="${filter.allStatuses ? 'page' : 'false'}" href="<c:out value='${allCompaniesUrl}'/>"><%= com.hrms.i18n.Language.text(request, "ui_5587303546bd", "Tất cả (") %><c:out value="${allCompanies.size()}"/>)</a>
            <a class="${filter.active ? 'selected' : ''}" aria-current="${filter.active ? 'page' : 'false'}" href="<c:out value='${activeCompaniesUrl}'/>"><%= com.hrms.i18n.Language.text(request, "ui_b244095cd844", "Đang hoạt động (") %><c:out value="${activeCount}"/>)</a>
            <a class="${filter.suspended ? 'selected' : ''}" aria-current="${filter.suspended ? 'page' : 'false'}" href="<c:out value='${suspendedCompaniesUrl}'/>"><%= com.hrms.i18n.Language.text(request, "ui_d6e46c3043bc", "Đã tạm ngưng (") %><c:out value="${suspendedCount}"/>)</a>
        </div>
        <form class="company-toolbar-fields" method="get" action="${pageContext.request.contextPath}/systemadmin/company">
            <input type="hidden" name="status" value="<c:out value='${filter.status}'/>">
            <label for="company-region"><%= com.hrms.i18n.Language.text(request, "ui_4fdaff02c8b0", "Khu vực:") %><select id="company-region" name="region" onchange="this.form.submit()"><option value="" ${empty filter.region ? 'selected' : ''}><%= com.hrms.i18n.Language.text(request, "ui_11a8e26dbae7", "Tất cả vùng") %></option><option value="NORTH" ${filter.region == 'NORTH' ? 'selected' : ''}><%= com.hrms.i18n.Language.text(request, "ui_c60e9ff86ff4", "Miền Bắc") %></option><option value="CENTRAL" ${filter.region == 'CENTRAL' ? 'selected' : ''}><%= com.hrms.i18n.Language.text(request, "ui_70b3957f87c7", "Miền Trung") %></option><option value="SOUTH" ${filter.region == 'SOUTH' ? 'selected' : ''}><%= com.hrms.i18n.Language.text(request, "ui_76103d806bb2", "Miền Nam") %></option></select></label>
            <label for="company-sort"><%= com.hrms.i18n.Language.text(request, "ui_1d983d7cf030", "Sắp xếp:") %><select id="company-sort" name="sort" onchange="this.form.submit()"><option value="NEWEST" ${filter.sort == 'NEWEST' ? 'selected' : ''}><%= com.hrms.i18n.Language.text(request, "ui_41fd3e53f99f", "Mới nhất") %></option><option value="ACCOUNT_DESC" ${filter.sort == 'ACCOUNT_DESC' ? 'selected' : ''}><%= com.hrms.i18n.Language.text(request, "ui_634acae343a6", "Số lượng User giảm dần") %></option><option value="NAME_ASC" ${filter.sort == 'NAME_ASC' ? 'selected' : ''}><%= com.hrms.i18n.Language.text(request, "ui_f46a1f41e52c", "Tên doanh nghiệp (A-Z)") %></option></select></label>
            <noscript><button class="button button-small" type="submit"><%= com.hrms.i18n.Language.text(request, "ui_1e948c591368", "Áp dụng") %></button></noscript>
        </form>
    </section>

    <section class="company-list-panel" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_eb221db1b16b", "Danh sách Company") %>">
        <c:choose>
            <c:when test="${empty companies}">
                <div class="empty-state"><svg class="icon" aria-hidden="true"><use href="${icons}#building"/></svg><h3><c:choose><c:when test="${empty allCompanies}"><%= com.hrms.i18n.Language.text(request, "ui_4dac9ceb9ae6", "Chưa có Company") %></c:when><c:otherwise><%= com.hrms.i18n.Language.text(request, "ui_5d047610a4a3", "Không có Công ty phù hợp") %></c:otherwise></c:choose></h3><p><c:choose><c:when test="${empty allCompanies}"><%= com.hrms.i18n.Language.text(request, "ui_1579117b367a", "Công ty sẽ xuất hiện sau khi Registration Request được phê duyệt.") %></c:when><c:otherwise><%= com.hrms.i18n.Language.text(request, "ui_cd12c5d3c6e2", "Hãy thay đổi trạng thái Subscription hoặc khu vực để xem Công ty khác.") %></c:otherwise></c:choose></p></div>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="company-table">
                        <thead><tr><th scope="col"><%= com.hrms.i18n.Language.text(request, "ui_9d6821753834", "Mã workspace") %></th><th scope="col"><%= com.hrms.i18n.Language.text(request, "ui_ff89a728e65f", "Doanh nghiệp SME") %></th><th scope="col"><%= com.hrms.i18n.Language.text(request, "ui_a3d0f0edfff1", "Đại diện (Company Admin)") %></th><th scope="col"><%= com.hrms.i18n.Language.text(request, "ui_c12e4bb02923", "Khu vực") %></th><th scope="col"><%= com.hrms.i18n.Language.text(request, "ui_4a60abe52524", "Tài khoản") %></th><th scope="col">Subscription</th><th scope="col" class="company-action-heading"><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th></tr></thead>
                        <tbody>
                        <c:forEach var="item" items="${companies}">
                            <tr class="${item.subscriptionSuspended ? 'subscription-suspended-row' : ''}">
                                <td><strong class="workspace-code ${item.subscriptionSuspended ? 'workspace-warning' : ''}"><c:out value="${item.workspaceCode}"/></strong></td>
                                <td class="company-name"><c:out value="${item.companyName}"/></td>
                                <td><strong class="company-representative"><c:out value="${item.companyAdminName}" default="—"/></strong><span class="company-email"><c:out value="${item.companyAdminEmail}" default="—"/></span></td>
                                <td><span class="region-label"><c:set var="i18nDisplayLabel" value="${item.regionLabel}"/><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("i18nDisplayLabel")) %>'/></span></td>
                                <td><div class="account-usage"><span><strong><c:out value="${item.accountCount}"/> / <c:out value="${item.maxAccounts}"/></strong><em class="${item.accountUsagePercent >= 90 ? 'near-limit' : ''}"><c:out value="${item.accountUsagePercent}"/>%</em></span><i><b style="width:${item.accountUsagePercent}%"></b></i></div></td>
                                <td><span class="subscription-status ${item.subscriptionActive ? 'is-active' : 'is-suspended'}"><i></i><c:set var="i18nDisplayLabel" value="${item.subscriptionStatus}"/><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("i18nDisplayLabel")) %>'/></span><c:if test="${item.subscriptionSuspended}"><small class="suspension-reason"><c:out value="${item.suspensionReason}" default='<%= com.hrms.i18n.Language.text(request, "ui_0d79b58ac18b", "Đã tạm ngưng dịch vụ") %>'/></small></c:if></td>
                                <td class="company-action"><a class="company-details-button" href="${pageContext.request.contextPath}/systemadmin/company-details?id=${item.companyId}"><%= com.hrms.i18n.Language.text(request, "ui_d72a6317ec65", "Chi tiết") %> <svg class="icon" aria-hidden="true"><use href="${icons}#arrow-right"/></svg></a></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <footer class="company-table-footer"><span><%= com.hrms.i18n.Language.text(request, "ui_2a48ea973ecf", "Hiển thị") %> <strong><c:out value="${companies.size()}"/></strong> <%= com.hrms.i18n.Language.text(request, "ui_8d53c654fbab", "trên tổng số") %> <strong><c:out value="${allCompanies.size()}"/></strong> <%= com.hrms.i18n.Language.text(request, "ui_5e13ed2ed50a", "doanh nghiệp SME đã tạo") %></span><nav class="company-pagination" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_c429cfc55fdd", "Phân trang") %>"><button type="button" disabled aria-label="<%= com.hrms.i18n.Language.text(request, "ui_d676ae254158", "Trang trước") %>"><svg class="icon" aria-hidden="true"><use href="${icons}#arrow-left"/></svg></button><strong>1</strong><span>/ 1</span><button type="button" disabled aria-label="Trang sau"><svg class="icon" aria-hidden="true"><use href="${icons}#arrow-right"/></svg></button></nav></footer>
            </c:otherwise>
        </c:choose>
    </section>
</main>
</body>
</html>
