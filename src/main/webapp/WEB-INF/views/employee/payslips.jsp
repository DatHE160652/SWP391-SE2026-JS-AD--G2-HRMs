<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="csrf-token" content="<c:out value='${payrollCsrf}'/>">
    <title>${adminView ? 'Quản lý phiếu lương' : 'Phiếu lương của tôi'} · HRMs Cloud</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/payroll.css?v=20261026-payslips-1">
</head>
<body data-page="payslips" data-management="${adminView}" data-context="${pageContext.request.contextPath}">
<div class="pay-shell">
    <aside class="pay-sidebar">
        <a class="pay-brand" href="${pageContext.request.contextPath}/${adminView ? 'payroll?view=payslips' : 'payslips'}"><span class="brand-mark">H</span><span>HRMs <b>Cloud</b><small>${adminView ? 'Không gian doanh nghiệp' : 'Không gian nhân viên'}</small></span></a>
        <div class="workspace-name"><span class="workspace-icon">▦</span><div><strong><c:out value="${payrollPrincipal.companyName}"/></strong><small>${adminView ? 'Quản lý phiếu lương' : 'Hồ sơ cá nhân'}</small></div></div>
        <c:if test="${payrollPrincipal.roleCode == 'MANAGER'}"><a class="pay-nav" href="${pageContext.request.contextPath}/attendance/manager-attendance">Điểm danh nhân sự</a></c:if>
        <c:if test="${payrollPrincipal.canManagePayroll()}">
            <div class="nav-caption">QUẢN LÝ LƯƠNG</div>
            <a class="pay-nav" href="${pageContext.request.contextPath}/payroll">Quản lý kỳ lương</a>
            <a class="pay-nav" href="${pageContext.request.contextPath}/payroll/salaries">Lương cơ bản</a>
            <a class="pay-nav ${adminView ? 'selected' : ''}" href="${pageContext.request.contextPath}/payroll?view=payslips">Quản lý phiếu lương</a>
        </c:if>
        <c:if test="${not empty payrollPrincipal.employeeId}">
            <div class="nav-caption">CHỨC NĂNG CÁ NHÂN</div>
            <a class="pay-nav" href="${pageContext.request.contextPath}/employee/my-profile">Hồ sơ cá nhân</a>
            <a class="pay-nav" href="${pageContext.request.contextPath}/attendance/employee-attendance">Chấm công cá nhân</a>
            <a class="pay-nav ${adminView ? '' : 'selected'}" href="${pageContext.request.contextPath}/payslips">Phiếu lương của tôi</a>
        </c:if>
        <div class="sidebar-note"><strong>Thông tin phiếu lương</strong><p>Nhân viên xem phiếu lương cá nhân sau khi kỳ lương được duyệt và công bố.</p></div>
        <a class="pay-nav logout" href="${pageContext.request.contextPath}/logout">↪ Đăng xuất</a>
    </aside>
    <main class="pay-main">
        <header class="pay-topbar"><span>${adminView ? 'Không gian công ty' : 'Không gian nhân viên'} <span class="crumb">/</span> <strong>Phiếu lương</strong></span><div class="user-chip"><span class="user-avatar">${adminView ? 'QL' : 'NV'}</span><c:out value="${payrollPrincipal.displayName}"/></div></header>
        <div class="pay-content">
            <div class="page-heading"><div><div class="eyebrow">${adminView ? 'PAYSLIP MANAGEMENT' : 'MY PAYSLIPS'}</div><h1>${adminView ? 'Quản lý phiếu lương' : 'Phiếu lương của tôi'}</h1><p>${adminView ? 'Tra cứu phiếu lương theo kỳ và theo nhân viên trong công ty.' : 'Xem thu nhập và các khoản điều chỉnh theo từng kỳ lương.'}</p></div><button class="btn secondary" id="refreshSlips" type="button">↻ Làm mới</button></div>
            <div id="feedback" class="feedback" role="status" aria-live="polite" hidden></div>
            <c:if test="${not adminView}">
                <section class="personal-hero"><div><h2>Thu nhập rõ ràng, dễ theo dõi</h2><p>Lương cơ bản, phụ cấp trách nhiệm, thưởng và khấu trừ trên từng phiếu.<br>Bạn có thể in hoặc lưu phiếu lương thành PDF.</p></div><span class="hero-icon" aria-hidden="true">▧</span></section>
                <section id="notifications" class="notification-list" aria-label="Thông báo phiếu lương"></section>
            </c:if>
            <c:if test="${adminView}"><div id="slipPeriodStatus" class="info-note" role="status" aria-live="polite">Chọn kỳ lương để xem phiếu.</div></c:if>
            <section class="pay-card">
                <div class="card-toolbar"><h2>${adminView ? 'Phiếu lương trong kỳ' : 'Lịch sử phiếu lương'}</h2><div class="toolbar-fields">
                    <c:choose><c:when test="${adminView}">
                        <select id="slipPeriod" aria-label="Kỳ lương"><option value="">Đang tải kỳ lương…</option></select>
                        <label class="search-field"><span>⌕</span><input id="slipSearch" type="search" placeholder="Tìm mã hoặc họ tên nhân viên" aria-label="Tìm phiếu lương theo mã hoặc họ tên nhân viên"></label>
                    </c:when><c:otherwise><select id="slipYear" aria-label="Lọc năm"><option value="">Tất cả năm</option></select></c:otherwise></c:choose>
                </div></div>
                <div class="table-scroll"><table><thead><tr><c:choose><c:when test="${adminView}"><th>Nhân viên</th></c:when><c:otherwise><th>Kỳ lương</th></c:otherwise></c:choose><th class="number">Lương cơ bản</th><th class="number">Phụ cấp trách nhiệm</th><th class="number">Thưởng</th><th class="number">Khấu trừ</th><th class="number">Thực nhận</th><c:if test="${adminView}"><th>Công bố</th></c:if><th>Chi tiết</th></tr></thead><tbody id="slipRows" aria-live="polite" aria-busy="true"><tr><td colspan="${adminView ? 8 : 7}" class="empty">Đang tải phiếu lương…</td></tr></tbody></table></div>
                <div class="pagination" id="slipPagination"></div>
            </section>
            <footer class="pay-footer">HRMs Cloud <span>•</span> ${adminView ? 'Quản lý phiếu lương' : 'Phiếu lương cá nhân'}</footer>
        </div>
    </main>
</div>
<script src="${pageContext.request.contextPath}/js/payroll.js?v=20261026-payslips-1"></script>
</body></html>
