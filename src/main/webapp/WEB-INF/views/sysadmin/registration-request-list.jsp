<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= com.hrms.i18n.Language.text(request, "ui_4aafdd49263f", "Yêu cầu đăng ký | HRMs.Admin") %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/system-admin.css">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc workspace-admin admin-page">
<%@ include file="/WEB-INF/views/sysadmin/admin-shell.jspf" %>
<main class="lc-main admin-main" id="main-content">
    <c:if test="${not empty actionMessage}"><div class="alert alert-success" role="status"><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("actionMessage")) %>'/></div></c:if>
    <c:if test="${not empty actionWarning}"><div class="alert alert-warning" role="alert"><c:out value="${actionWarning}"/></div></c:if>
    <nav class="breadcrumb" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_50de7d02dcaa", "Đường dẫn") %>"><a href="${pageContext.request.contextPath}/systemadmin/dashboard"><%= com.hrms.i18n.Language.text(request, "ui_8201002cd9e8", "Quản lý tenant") %></a><svg class="icon" aria-hidden="true"><use href="${icons}#chevron"/></svg><span><%= com.hrms.i18n.Language.text(request, "ui_a262f997f733", "Yêu cầu đăng ký") %></span></nav>
    <div class="page-heading"><h1><%= com.hrms.i18n.Language.text(request, "ui_619d06c4ef54", "Danh sách yêu cầu đăng ký doanh nghiệp") %></h1><a class="button" href="${pageContext.request.contextPath}/systemadmin/registration-request"><svg class="icon" aria-hidden="true"><use href="${icons}#refresh"/></svg><%= com.hrms.i18n.Language.text(request, "ui_e90fa4eefd44", "Làm mới dữ liệu") %></a></div>
    <section class="stat-grid stat-grid-compact" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_c71bd2adc387", "Thống kê toàn bộ yêu cầu, không áp dụng bộ lọc") %>">
        <article class="stat-card"><span class="stat-icon"><svg class="icon" aria-hidden="true"><use href="${icons}#clock"/></svg></span><div><h2><%= com.hrms.i18n.Language.text(request, "ui_add2d9210361", "Mới tiếp nhận") %></h2><strong class="stat-value"><c:out value="${stats.newCount}"/></strong><span class="stat-note"><%= com.hrms.i18n.Language.text(request, "ui_add2d9210361", "Mới tiếp nhận") %></span></div></article>
        <article class="stat-card"><span class="stat-icon stat-icon-amber"><svg class="icon" aria-hidden="true"><use href="${icons}#search"/></svg></span><div><h2><%= com.hrms.i18n.Language.text(request, "ui_1c10b631753a", "Đang xem xét") %></h2><strong class="stat-value"><c:out value="${stats.reviewCount}"/></strong><span class="stat-note"><%= com.hrms.i18n.Language.text(request, "ui_1c10b631753a", "Đang xem xét") %></span></div></article>
        <article class="stat-card"><span class="stat-icon"><svg class="icon" aria-hidden="true"><use href="${icons}#refresh"/></svg></span><div><h2><%= com.hrms.i18n.Language.text(request, "ui_334176c72d9a", "Đang khởi tạo") %></h2><strong class="stat-value"><c:out value="${stats.convertingCount}"/></strong><span class="stat-note"><%= com.hrms.i18n.Language.text(request, "ui_334176c72d9a", "Đang khởi tạo") %></span></div></article>
        <article class="stat-card"><span class="stat-icon stat-icon-teal"><svg class="icon" aria-hidden="true"><use href="${icons}#shield"/></svg></span><div><h2><%= com.hrms.i18n.Language.text(request, "ui_33bdefd63607", "Đã chuyển đổi") %></h2><strong class="stat-value"><c:out value="${stats.convertedCount}"/></strong><span class="stat-note"><%= com.hrms.i18n.Language.text(request, "ui_57a21dd39691", "Đã khởi tạo") %></span></div></article>
    </section>
    <section class="panel filter-panel" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_03e3eda5aa7b", "Bộ lọc yêu cầu") %>">
        <form method="get" action="${pageContext.request.contextPath}/systemadmin/registration-request" class="filter-form">
            <div class="filter-field filter-search"><label for="request-search"><%= com.hrms.i18n.Language.text(request, "ui_f2755ef6fb36", "Tìm yêu cầu") %></label><input id="request-search" type="search" name="q" maxlength="150" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_5dd36d9054d3", "Tên công ty, email, mã yêu cầu...") %>" value="<c:out value='${filter.q}'/>"></div>
            <div class="filter-field"><label for="request-status"><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></label><select id="request-status" name="status">
                <option value=""><%= com.hrms.i18n.Language.text(request, "ui_872e04b463ff", "Tất cả trạng thái") %></option>
                <option value="PENDING" ${filter.status == 'PENDING' ? 'selected' : ''}><%= com.hrms.i18n.Language.text(request, "ui_22f4bd8f0659", "Cần xử lý") %></option>
                <option value="NEW" ${filter.status == 'NEW' ? 'selected' : ''}><%= com.hrms.i18n.Language.text(request, "ui_add2d9210361", "Mới tiếp nhận") %></option>
                <option value="UNDER_REVIEW" ${filter.status == 'UNDER_REVIEW' ? 'selected' : ''}><%= com.hrms.i18n.Language.text(request, "ui_1c10b631753a", "Đang xem xét") %></option>
                <option value="CONVERTING" ${filter.status == 'CONVERTING' ? 'selected' : ''}><%= com.hrms.i18n.Language.text(request, "ui_334176c72d9a", "Đang khởi tạo") %></option>
                <option value="CONVERTED" ${filter.status == 'CONVERTED' ? 'selected' : ''}><%= com.hrms.i18n.Language.text(request, "ui_33bdefd63607", "Đã chuyển đổi") %></option>
            </select></div>
            <div class="filter-field"><label for="request-region"><%= com.hrms.i18n.Language.text(request, "ui_c12e4bb02923", "Khu vực") %></label><select id="request-region" name="region"><option value=""><%= com.hrms.i18n.Language.text(request, "ui_8581f79d89d7", "Tất cả khu vực") %></option><option value="NORTH" ${filter.region == 'NORTH' ? 'selected' : ''}><%= com.hrms.i18n.Language.text(request, "ui_c60e9ff86ff4", "Miền Bắc") %></option><option value="CENTRAL" ${filter.region == 'CENTRAL' ? 'selected' : ''}><%= com.hrms.i18n.Language.text(request, "ui_70b3957f87c7", "Miền Trung") %></option><option value="SOUTH" ${filter.region == 'SOUTH' ? 'selected' : ''}><%= com.hrms.i18n.Language.text(request, "ui_76103d806bb2", "Miền Nam") %></option></select></div>
            <div class="filter-field"><label for="request-date"><%= com.hrms.i18n.Language.text(request, "ui_69ee5b88256e", "Ngày tiếp nhận") %></label><input id="request-date" name="date" type="date" value="<c:out value='${filter.date}'/>"></div>
            <div class="filter-field"><label for="page-size"><%= com.hrms.i18n.Language.text(request, "ui_3b801f21e6c4", "Số dòng") %></label><select id="page-size" name="size"><option value="10" ${requestPage.size == 10 ? 'selected' : ''}>10</option><option value="20" ${requestPage.size == 20 ? 'selected' : ''}>20</option><option value="50" ${requestPage.size == 50 ? 'selected' : ''}>50</option></select></div>
            <button class="button button-primary" type="submit"><%= com.hrms.i18n.Language.text(request, "ui_85b9732b1d5b", "Lọc dữ liệu") %></button>
        </form>
        <div class="filter-summary"><span><%= com.hrms.i18n.Language.text(request, "ui_979288a30d93", "Thống kê phía trên tính trên toàn bộ yêu cầu chưa xóa.") %></span><a href="${pageContext.request.contextPath}/systemadmin/registration-request"><%= com.hrms.i18n.Language.text(request, "ui_4a47a4788d52", "Xóa toàn bộ bộ lọc") %></a></div>
    </section>
    <section class="panel" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_4a78caadfad9", "Kết quả yêu cầu đăng ký") %>">
        <%@ include file="/WEB-INF/views/sysadmin/request-table.jspf" %>
        <div class="table-footer"><span><%= com.hrms.i18n.Language.text(request, "ui_2a48ea973ecf", "Hiển thị") %> <strong>${requestPage.from}–${requestPage.to}</strong> <%= com.hrms.i18n.Language.text(request, "ui_787f939abe7a", "trong số") %> <strong>${requestPage.total}</strong> <%= com.hrms.i18n.Language.text(request, "ui_f970f45deb01", "yêu cầu") %></span>
            <nav class="pagination" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_c429cfc55fdd", "Phân trang") %>">
                <c:url var="previousUrl" value="/systemadmin/registration-request"><c:param name="q" value="${filter.q}"/><c:param name="status" value="${filter.status}"/><c:param name="region" value="${filter.region}"/><c:param name="date" value="${filter.date}"/><c:param name="size" value="${requestPage.size}"/><c:param name="page" value="${requestPage.page - 1}"/></c:url>
                <c:url var="nextUrl" value="/systemadmin/registration-request"><c:param name="q" value="${filter.q}"/><c:param name="status" value="${filter.status}"/><c:param name="region" value="${filter.region}"/><c:param name="date" value="${filter.date}"/><c:param name="size" value="${requestPage.size}"/><c:param name="page" value="${requestPage.page + 1}"/></c:url>
                <c:choose><c:when test="${requestPage.hasPrevious}"><a class="icon-button" href="<c:out value='${previousUrl}'/>" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_d676ae254158", "Trang trước") %>"><svg class="icon" aria-hidden="true"><use href="${icons}#arrow-left"/></svg></a></c:when><c:otherwise><button class="icon-button" disabled aria-label="<%= com.hrms.i18n.Language.text(request, "ui_d676ae254158", "Trang trước") %>"><svg class="icon" aria-hidden="true"><use href="${icons}#arrow-left"/></svg></button></c:otherwise></c:choose>
                <span class="page-number" aria-current="page">${requestPage.page}</span><span>/ ${requestPage.totalPages}</span>
                <c:choose><c:when test="${requestPage.hasNext}"><a class="icon-button" href="<c:out value='${nextUrl}'/>" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_9ea8e8d44871", "Trang tiếp theo") %>"><svg class="icon" aria-hidden="true"><use href="${icons}#arrow-right"/></svg></a></c:when><c:otherwise><button class="icon-button" disabled aria-label="<%= com.hrms.i18n.Language.text(request, "ui_9ea8e8d44871", "Trang tiếp theo") %>"><svg class="icon" aria-hidden="true"><use href="${icons}#arrow-right"/></svg></button></c:otherwise></c:choose>
            </nav>
        </div>
    </section>
</main>
</body>
</html>
