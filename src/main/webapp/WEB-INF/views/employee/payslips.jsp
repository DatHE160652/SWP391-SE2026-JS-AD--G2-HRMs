<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="csrf-token" content="<c:out value='${payrollCsrf}'/>">
    <title><c:set var="i18nConditionalLabel" value="${adminView ? 'Quản lý phiếu lương' : 'Phiếu lương của tôi'}"/><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("i18nConditionalLabel")) %>'/> · HRMs Cloud</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/payroll.css?v=<%= application.getResource("/css/payroll.css").openConnection().getLastModified() %>">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc workspace-payroll" data-page="payslips" data-management="${adminView}" data-context="${pageContext.request.contextPath}">
<div class="pay-shell">
    <%@ include file="/WEB-INF/views/common/tenant-shell.jspf" %>
    <main class="lc-main pay-main">

        <div class="pay-content">
            <div class="page-heading"><div><div class="eyebrow">${adminView ? 'PAYSLIP MANAGEMENT' : 'MY PAYSLIPS'}</div><h1><c:set var="i18nConditionalLabel" value="${adminView ? 'Quản lý phiếu lương' : 'Phiếu lương của tôi'}"/><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("i18nConditionalLabel")) %>'/></h1><p><c:set var="i18nConditionalLabel" value="${adminView ? 'Tra cứu phiếu lương theo kỳ và theo nhân viên trong công ty.' : 'Xem thu nhập và các khoản điều chỉnh theo từng kỳ lương.'}"/><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("i18nConditionalLabel")) %>'/></p></div><button class="btn secondary" id="refreshSlips" type="button"><%= com.hrms.i18n.Language.text(request, "ui_7e659027e37b", "↻ Làm mới") %></button></div>
            <div id="feedback" class="feedback" role="status" aria-live="polite" hidden></div>
            <c:if test="${not adminView}">
                <section class="personal-hero"><div><h2><%= com.hrms.i18n.Language.text(request, "ui_18d6ad0a2468", "Thu nhập rõ ràng, dễ theo dõi") %></h2><p><%= com.hrms.i18n.Language.text(request, "ui_2b94e5cae631", "Lương cơ bản, phụ cấp trách nhiệm, thưởng và khấu trừ trên từng phiếu.") %><br><%= com.hrms.i18n.Language.text(request, "ui_0da060930347", "Bạn có thể in hoặc lưu phiếu lương thành PDF.") %></p></div><span class="hero-icon" aria-hidden="true">▧</span></section>
                <section id="notifications" class="notification-list" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_0414fc2da6ab", "Thông báo phiếu lương") %>"></section>
            </c:if>
            <c:if test="${adminView}"><div id="slipPeriodStatus" class="info-note" role="status" aria-live="polite"><%= com.hrms.i18n.Language.text(request, "ui_e1346b663f09", "Chọn kỳ lương để xem phiếu.") %></div></c:if>
            <section class="pay-card">
                <div class="card-toolbar"><h2><c:set var="i18nConditionalLabel" value="${adminView ? 'Phiếu lương trong kỳ' : 'Lịch sử phiếu lương'}"/><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("i18nConditionalLabel")) %>'/></h2><div class="toolbar-fields">
                    <c:choose><c:when test="${adminView}">
                        <select id="slipPeriod" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_1f22d2859e01", "Kỳ lương") %>"><option value=""><%= com.hrms.i18n.Language.text(request, "ui_64f660fad581", "Đang tải kỳ lương…") %></option></select>
                        <label class="search-field"><span>⌕</span><input id="slipSearch" type="search" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_96e4db4c4c88", "Tìm mã hoặc họ tên nhân viên") %>" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_23a25e1b2313", "Tìm phiếu lương theo mã hoặc họ tên nhân viên") %>"></label>
                    </c:when><c:otherwise><select id="slipYear" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_a89c934bbacb", "Lọc năm") %>"><option value=""><%= com.hrms.i18n.Language.text(request, "ui_b97f711f0b89", "Tất cả năm") %></option></select></c:otherwise></c:choose>
                </div></div>
                <div class="table-scroll"><table><thead><tr><c:choose><c:when test="${adminView}"><th><%= com.hrms.i18n.Language.text(request, "ui_14ddd70d2564", "Nhân viên") %></th></c:when><c:otherwise><th><%= com.hrms.i18n.Language.text(request, "ui_1f22d2859e01", "Kỳ lương") %></th></c:otherwise></c:choose><th class="number">Lương theo công</th><th class="number"><%= com.hrms.i18n.Language.text(request, "ui_ac5d493eaa5e", "Phụ cấp trách nhiệm") %></th><th class="number">Phụ cấp đi lại</th><th class="number"><%= com.hrms.i18n.Language.text(request, "ui_1dff4c8c3481", "Thưởng") %></th><th class="number"><%= com.hrms.i18n.Language.text(request, "ui_bda3b5abe5e4", "Thực nhận") %></th><c:if test="${adminView}"><th><%= com.hrms.i18n.Language.text(request, "ui_3bec228bfe1a", "Công bố") %></th></c:if><th><%= com.hrms.i18n.Language.text(request, "ui_d72a6317ec65", "Chi tiết") %></th></tr></thead><tbody id="slipRows" aria-live="polite" aria-busy="true"><tr><td colspan="${adminView ? 8 : 7}" class="empty"><%= com.hrms.i18n.Language.text(request, "ui_86fc79ccd3ef", "Đang tải phiếu lương…") %></td></tr></tbody></table></div>
                <div class="pagination" id="slipPagination"></div>
            </section>
            <footer class="pay-footer">HRMs Cloud <span>•</span> <c:set var="i18nConditionalLabel" value="${adminView ? 'Quản lý phiếu lương' : 'Phiếu lương cá nhân'}"/><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("i18nConditionalLabel")) %>'/></footer>
        </div>
    </main>
</div>
<script src="${pageContext.request.contextPath}/js/payroll.js?v=<%= application.getResource("/js/payroll.js").openConnection().getLastModified() %>"></script>
</body></html>
