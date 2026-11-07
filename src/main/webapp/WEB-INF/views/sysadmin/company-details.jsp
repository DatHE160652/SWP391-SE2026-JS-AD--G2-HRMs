<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chi tiết Company | HRMs.Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/system-admin.css?v=20261020-company-toolbar">
</head>
<body class="admin-page company-page company-detail-page">
<%@ include file="/WEB-INF/views/sysadmin/admin-shell.jspf" %>
<main class="admin-main" id="main-content">
    <a class="company-back-link" href="${pageContext.request.contextPath}/systemadmin/company"><svg class="icon" aria-hidden="true"><use href="${icons}#arrow-left"/></svg>Quay lại danh sách Doanh nghiệp</a>

    <section class="company-hero" aria-labelledby="company-title">
        <div class="company-hero-copy">
            <div class="company-hero-title"><h1 id="company-title"><c:out value="${company.companyName}"/></h1><span class="company-hero-status"><i></i><c:out value="${company.subscriptionStatus}"/> · <c:out value="${company.subscriptionActive ? 'Đang hoạt động' : 'Đã tạm ngưng'}"/></span><span class="region-label"><c:out value="${company.regionLabel}"/></span></div>
            <p>Ngày tạo: <strong><c:out value="${company.companyCreatedAtDisplay}"/></strong> <span class="workspace-code">Workspace: <c:out value="${company.workspaceCode}"/></span></p>
        </div>
        <div class="company-hero-actions">
            <c:choose>
                <c:when test="${company.subscriptionActive}"><button class="button button-activate" type="button" disabled aria-disabled="true" title="Subscription đang hoạt động"><svg class="icon" aria-hidden="true"><use href="${icons}#check"/></svg>Kích hoạt Subscription</button><button class="button button-suspend" type="button" data-dialog-open="suspend-subscription-dialog"><svg class="icon" aria-hidden="true"><use href="${icons}#close"/></svg>Tạm ngưng Subscription</button></c:when>
                <c:when test="${company.subscriptionSuspended}"><button class="button button-activate" type="button" data-dialog-open="activate-subscription-dialog"><svg class="icon" aria-hidden="true"><use href="${icons}#check"/></svg>Kích hoạt Subscription</button><button class="button button-suspend" type="button" disabled aria-disabled="true" title="Subscription đã tạm ngưng"><svg class="icon" aria-hidden="true"><use href="${icons}#close"/></svg>Tạm ngưng Subscription</button></c:when>
            </c:choose>
        </div>
    </section>
    <c:if test="${not empty actionMessage}"><div class="alert alert-success" role="status"><c:out value="${actionMessage}"/></div></c:if>

    <div class="company-detail-row company-detail-row-top">
        <section class="company-info-card" aria-labelledby="company-info-title">
            <header class="company-card-heading"><span class="company-card-icon icon-blue"><svg class="icon" aria-hidden="true"><use href="${icons}#building"/></svg></span><h2 id="company-info-title">Thông tin Doanh nghiệp &amp; Workspace</h2><span class="tenant-id">Tenant ID: #<c:out value="${company.companyId}"/></span></header>
            <div class="company-info-grid">
                <div><span>Tên công ty</span><strong><c:out value="${company.companyName}"/></strong></div>
                <div><span>Mã định danh Workspace</span><strong class="workspace-code workspace-code-box"><c:out value="${company.workspaceCode}"/></strong></div>
                <div><span>Khu vực phân vùng</span><strong><span class="region-label"><c:out value="${company.regionLabel}"/></span></strong></div>
                <div><span>Thời gian phê duyệt &amp; tạo</span><strong><c:out value="${company.companyCreatedAtDisplay}"/></strong></div>
            </div>
        </section>

        <section class="company-info-card" aria-labelledby="admin-info-title">
            <header class="company-card-heading"><span class="company-card-icon icon-indigo"><svg class="icon" aria-hidden="true"><use href="${icons}#user"/></svg></span><h2 id="admin-info-title">Thông tin Người đại diện (Company Admin)</h2></header>
            <div class="company-admin-profile"><span class="company-admin-avatar"><c:out value="${company.companyAdminInitial}"/></span><div><strong><c:out value="${company.companyAdminName}" default="Chưa có Company Admin"/></strong><span>Đại diện pháp lý &amp; quản trị viên của Tenant</span></div></div>
            <div class="company-admin-contact"><div><span>Email đăng nhập &amp; liên hệ</span><strong><c:out value="${company.companyAdminEmail}" default="—"/></strong></div><div><span>Số điện thoại</span><strong><c:out value="${company.contactPhone}" default="—"/></strong></div><div><span>Thời gian kích hoạt</span><strong><c:out value="${company.companyAdminActivatedAtDisplay}"/></strong></div></div>
        </section>
    </div>

    <div class="company-detail-row company-detail-row-bottom">
        <section class="company-info-card company-quota-card" aria-labelledby="quota-title">
            <header class="company-card-heading quota-heading"><span class="company-card-icon icon-teal"><svg class="icon" aria-hidden="true"><use href="${icons}#users"/></svg></span><div><h2 id="quota-title">Định mức &amp; Phân bổ Tài khoản</h2><p>Giới hạn tối đa <c:out value="${company.maxAccounts}"/> User theo gói dịch vụ</p></div><div class="quota-summary"><span>Đã sử dụng</span><strong><c:out value="${company.accountCount}"/> <em>/ <c:out value="${company.maxAccounts}"/></em></strong></div></header>
            <div class="quota-progress" role="img" aria-label="Đã sử dụng ${company.accountUsagePercent}% hạn mức"><span style="width:${company.accountUsagePercent}%"></span></div>
            <div class="quota-legend"><span><i></i><c:out value="${company.accountCount}"/> tài khoản đang dùng</span><strong>Còn trống: <c:out value="${company.availableAccounts}"/> vị trí</strong></div>
            <div class="core-members-section">
                <h3>THÀNH VIÊN CỐT CÁN TRONG COMPANY</h3>
                <c:choose>
                    <c:when test="${empty company.coreMembers}">
                        <p class="core-members-empty">Chưa có tài khoản thành viên trong Company.</p>
                    </c:when>
                    <c:otherwise>
                        <div class="table-wrap core-members-wrap">
                            <table class="request-table core-members-table">
                                <thead><tr><th scope="col">Nhân sự</th><th scope="col">Email nội bộ</th><th scope="col">Vai trò (Role)</th><th scope="col">Trạng thái</th></tr></thead>
                                <tbody><c:forEach var="member" items="${company.coreMembers}"><tr><td><span class="avatar member-avatar"><c:out value="${member.initials}"/></span><strong><c:out value="${member.displayName}" default="—"/></strong></td><td class="member-email"><c:out value="${member.email}" default="—"/></td><td><span class="status ${member.roleCode == 'MANAGER' ? 'status-CONVERTING' : 'status-NEW'}"><c:out value="${member.roleLabel}"/></span></td><td><span class="status ${member.active ? 'status-CONVERTED' : 'status-UNDER_REVIEW'}"><span class="status-dot" aria-hidden="true"></span><c:out value="${member.active ? 'Active' : 'Inactive'}"/></span></td></tr></c:forEach></tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
            <p class="quota-readonly-note">System Admin chỉ xem số lượng tài khoản; quản lý người dùng thuộc phạm vi Company Admin.</p>
        </section>

        <section class="company-info-card company-subscription-card" aria-labelledby="subscription-title">
            <header class="company-card-heading"><span class="company-card-icon icon-orange"><svg class="icon" aria-hidden="true"><use href="${icons}#info"/></svg></span><h2 id="subscription-title">Gói dịch vụ (Subscription)</h2><span class="subscription-status ${company.subscriptionActive ? 'is-active' : 'is-suspended'}"><i></i><c:out value="${company.subscriptionStatus}"/></span></header>
            <div class="subscription-price"><span>Mức phí áp dụng</span><strong><fmt:formatNumber value="${company.price}" type="number" maxFractionDigits="0"/> <small>đ / tháng</small></strong><p>Gói dịch vụ mặc định của doanh nghiệp.</p></div>
            <dl class="subscription-data"><div><dt>Gói cấu hình:</dt><dd><c:out value="${company.planName}"/></dd></div><div><dt>Ngày bắt đầu:</dt><dd><c:out value="${company.subscriptionStartDateDisplay}"/></dd></div><div><dt>Ngày kết thúc:</dt><dd><c:out value="${company.subscriptionEndDateDisplay}"/></dd></div><c:if test="${company.subscriptionSuspended}"><div><dt>Tạm ngưng lúc:</dt><dd><c:out value="${company.suspendedAtDisplay}"/></dd></div><div class="subscription-reason"><dt>Lý do tạm ngưng:</dt><dd><c:out value="${company.suspensionReason}"/></dd></div></c:if></dl>
        </section>
    </div>

    <c:if test="${company.subscriptionActive}"><dialog class="action-dialog" id="suspend-subscription-dialog" aria-labelledby="suspend-subscription-title"><form method="post" action="${pageContext.request.contextPath}/systemadmin/company/suspend"><input type="hidden" name="id" value="<c:out value='${company.companyId}'/>"><input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.systemAdminCsrfToken}'/>"><h2 id="suspend-subscription-title">Xác nhận Tạm ngưng Subscription</h2><p>Workspace <strong><c:out value="${company.workspaceCode}"/></strong> sẽ bị chặn truy cập, kể cả các phiên tenant đang đăng nhập. Company và tài khoản vẫn được giữ nguyên.</p><label class="dialog-field" for="suspension-reason">Lý do tạm ngưng dịch vụ <span aria-hidden="true">*</span></label><textarea id="suspension-reason" name="reason" maxlength="500" required rows="3" placeholder="Nhập lý do tạm ngưng"></textarea><div class="dialog-actions"><button class="button" type="button" data-dialog-close>Hủy bỏ</button><button class="button button-danger" type="submit">Xác nhận tạm ngưng</button></div></form></dialog></c:if>
    <c:if test="${company.subscriptionSuspended}"><dialog class="action-dialog" id="activate-subscription-dialog" aria-labelledby="activate-subscription-title"><form method="post" action="${pageContext.request.contextPath}/systemadmin/company/activate"><input type="hidden" name="id" value="<c:out value='${company.companyId}'/>"><input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.systemAdminCsrfToken}'/>"><h2 id="activate-subscription-title">Kích hoạt lại Subscription?</h2><p>Kích hoạt lại Subscription hiện có cho <strong><c:out value="${company.companyName}"/></strong>. Tài khoản bị khóa hoặc vô hiệu hóa sẽ không tự được mở lại.</p><div class="dialog-context"><span class="dialog-field">Lý do tạm ngưng trước đó</span><p class="dialog-context-value"><c:out value="${company.suspensionReason}" default="Không có thông tin"/></p></div><label class="dialog-field" for="activation-reason">Lý do kích hoạt lại dịch vụ <span aria-hidden="true">*</span></label><textarea id="activation-reason" name="reason" maxlength="500" required rows="3" placeholder="Nhập lý do kích hoạt lại"></textarea><div class="dialog-actions"><button class="button" type="button" data-dialog-close>Đóng</button><button class="button button-primary" type="submit">Xác nhận kích hoạt</button></div></form></dialog></c:if>
</main>
</body>
</html>
