<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Registration Requests | HRMs.Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/system-admin.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/views/sysadmin/admin-shell.jspf" %>
<main class="admin-main" id="main-content">
    <c:if test="${not empty actionMessage}"><div class="alert alert-success" role="status"><c:out value="${actionMessage}"/></div></c:if>
    <c:if test="${not empty actionWarning}"><div class="alert alert-warning" role="alert"><c:out value="${actionWarning}"/></div></c:if>
    <nav class="breadcrumb" aria-label="Đường dẫn"><a href="${pageContext.request.contextPath}/systemadmin/dashboard">Quản lý tenant</a><svg class="icon" aria-hidden="true"><use href="${icons}#chevron"/></svg><span>Yêu cầu đăng ký</span></nav>
    <div class="page-heading"><h1>Danh sách yêu cầu đăng ký tenant</h1><a class="button" href="${pageContext.request.contextPath}/systemadmin/registration-request"><svg class="icon" aria-hidden="true"><use href="${icons}#refresh"/></svg>Làm mới dữ liệu</a></div>
    <section class="stat-grid stat-grid-compact" aria-label="Thống kê toàn bộ yêu cầu, không áp dụng bộ lọc">
        <article class="stat-card"><span class="stat-icon"><svg class="icon" aria-hidden="true"><use href="${icons}#clock"/></svg></span><div><h2>Mới tiếp nhận</h2><strong class="stat-value"><c:out value="${stats.newCount}"/></strong><span class="stat-note">NEW</span></div></article>
        <article class="stat-card"><span class="stat-icon stat-icon-amber"><svg class="icon" aria-hidden="true"><use href="${icons}#search"/></svg></span><div><h2>Đang xem xét</h2><strong class="stat-value"><c:out value="${stats.reviewCount}"/></strong><span class="stat-note">UNDER_REVIEW</span></div></article>
        <article class="stat-card"><span class="stat-icon"><svg class="icon" aria-hidden="true"><use href="${icons}#refresh"/></svg></span><div><h2>Đang khởi tạo</h2><strong class="stat-value"><c:out value="${stats.convertingCount}"/></strong><span class="stat-note">CONVERTING</span></div></article>
        <article class="stat-card"><span class="stat-icon stat-icon-teal"><svg class="icon" aria-hidden="true"><use href="${icons}#shield"/></svg></span><div><h2>Đã chuyển đổi</h2><strong class="stat-value"><c:out value="${stats.convertedCount}"/></strong><span class="stat-note">CONVERTED</span></div></article>
    </section>
    <section class="panel filter-panel" aria-label="Bộ lọc yêu cầu">
        <form method="get" action="${pageContext.request.contextPath}/systemadmin/registration-request" class="filter-form">
            <div class="filter-field filter-search"><label for="request-search">Tìm yêu cầu</label><input id="request-search" type="search" name="q" maxlength="150" placeholder="Tên công ty, email, mã yêu cầu..." value="<c:out value='${filter.q}'/>"></div>
            <div class="filter-field"><label for="request-status">Trạng thái</label><select id="request-status" name="status">
                <option value="">Tất cả trạng thái</option>
                <option value="PENDING" ${filter.status == 'PENDING' ? 'selected' : ''}>Cần xử lý</option>
                <option value="NEW" ${filter.status == 'NEW' ? 'selected' : ''}>Mới tiếp nhận</option>
                <option value="UNDER_REVIEW" ${filter.status == 'UNDER_REVIEW' ? 'selected' : ''}>Đang xem xét</option>
                <option value="CONVERTING" ${filter.status == 'CONVERTING' ? 'selected' : ''}>Đang khởi tạo</option>
                <option value="CONVERTED" ${filter.status == 'CONVERTED' ? 'selected' : ''}>Đã chuyển đổi</option>
            </select></div>
            <div class="filter-field"><label for="request-region">Khu vực</label><select id="request-region" name="region"><option value="">Tất cả khu vực</option><option value="NORTH" ${filter.region == 'NORTH' ? 'selected' : ''}>Miền Bắc</option><option value="CENTRAL" ${filter.region == 'CENTRAL' ? 'selected' : ''}>Miền Trung</option><option value="SOUTH" ${filter.region == 'SOUTH' ? 'selected' : ''}>Miền Nam</option></select></div>
            <div class="filter-field"><label for="request-date">Ngày tiếp nhận</label><input id="request-date" name="date" type="date" value="<c:out value='${filter.date}'/>"></div>
            <div class="filter-field"><label for="page-size">Số dòng</label><select id="page-size" name="size"><option value="10" ${requestPage.size == 10 ? 'selected' : ''}>10</option><option value="20" ${requestPage.size == 20 ? 'selected' : ''}>20</option><option value="50" ${requestPage.size == 50 ? 'selected' : ''}>50</option></select></div>
            <button class="button button-primary" type="submit">Lọc dữ liệu</button>
        </form>
        <div class="filter-summary"><span>Thống kê phía trên tính trên toàn bộ yêu cầu chưa xóa.</span><a href="${pageContext.request.contextPath}/systemadmin/registration-request">Xóa toàn bộ bộ lọc</a></div>
    </section>
    <section class="panel" aria-label="Kết quả yêu cầu đăng ký">
        <%@ include file="/WEB-INF/views/sysadmin/request-table.jspf" %>
        <div class="table-footer"><span>Hiển thị <strong>${requestPage.from}–${requestPage.to}</strong> trong số <strong>${requestPage.total}</strong> yêu cầu</span>
            <nav class="pagination" aria-label="Phân trang">
                <c:url var="previousUrl" value="/systemadmin/registration-request"><c:param name="q" value="${filter.q}"/><c:param name="status" value="${filter.status}"/><c:param name="region" value="${filter.region}"/><c:param name="date" value="${filter.date}"/><c:param name="size" value="${requestPage.size}"/><c:param name="page" value="${requestPage.page - 1}"/></c:url>
                <c:url var="nextUrl" value="/systemadmin/registration-request"><c:param name="q" value="${filter.q}"/><c:param name="status" value="${filter.status}"/><c:param name="region" value="${filter.region}"/><c:param name="date" value="${filter.date}"/><c:param name="size" value="${requestPage.size}"/><c:param name="page" value="${requestPage.page + 1}"/></c:url>
                <c:choose><c:when test="${requestPage.hasPrevious}"><a class="icon-button" href="<c:out value='${previousUrl}'/>" aria-label="Trang trước"><svg class="icon" aria-hidden="true"><use href="${icons}#arrow-left"/></svg></a></c:when><c:otherwise><button class="icon-button" disabled aria-label="Trang trước"><svg class="icon" aria-hidden="true"><use href="${icons}#arrow-left"/></svg></button></c:otherwise></c:choose>
                <span class="page-number" aria-current="page">${requestPage.page}</span><span>/ ${requestPage.totalPages}</span>
                <c:choose><c:when test="${requestPage.hasNext}"><a class="icon-button" href="<c:out value='${nextUrl}'/>" aria-label="Trang tiếp theo"><svg class="icon" aria-hidden="true"><use href="${icons}#arrow-right"/></svg></a></c:when><c:otherwise><button class="icon-button" disabled aria-label="Trang tiếp theo"><svg class="icon" aria-hidden="true"><use href="${icons}#arrow-right"/></svg></button></c:otherwise></c:choose>
            </nav>
        </div>
    </section>
</main>
</body>
</html>
