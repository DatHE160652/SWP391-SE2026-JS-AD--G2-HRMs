<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= com.hrms.i18n.Language.text(request, "ui_7a13b3a21066", "Chi tiết Company | HRMs.Admin") %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/system-admin.css?v=20261020-company-toolbar">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc workspace-admin admin-page company-page company-detail-page">
<%@ include file="/WEB-INF/views/sysadmin/admin-shell.jspf" %>
<main class="lc-main admin-main" id="main-content">
    <a class="company-back-link" href="${pageContext.request.contextPath}/systemadmin/company"><svg class="icon" aria-hidden="true"><use href="${icons}#arrow-left"/></svg><%= com.hrms.i18n.Language.text(request, "ui_13fbcf84a5b5", "Quay lại danh sách Doanh nghiệp") %></a>

    <section class="company-hero" aria-labelledby="company-title">
        <div class="company-hero-copy">
            <div class="company-hero-title"><h1 id="company-title"><c:out value="${company.companyName}"/></h1><span class="company-hero-status"><i></i><c:set var="i18nDisplayLabel" value="${company.subscriptionStatus}"/><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("i18nDisplayLabel")) %>'/> · <c:set var="i18nDisplayLabel" value="${company.subscriptionActive ? 'Đang hoạt động' : 'Đã tạm ngưng'}"/><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("i18nDisplayLabel")) %>'/></span><span class="region-label"><c:set var="i18nDisplayLabel" value="${company.regionLabel}"/><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("i18nDisplayLabel")) %>'/></span></div>
            <p><%= com.hrms.i18n.Language.text(request, "ui_01834dd867de", "Ngày tạo:") %> <strong><c:out value="${company.companyCreatedAtDisplay}"/></strong> <span class="workspace-code">Workspace: <c:out value="${company.workspaceCode}"/></span></p>
        </div>
        <div class="company-hero-actions">
            <c:choose>
                <c:when test="${company.subscriptionActive}"><button class="button button-activate" type="button" disabled aria-disabled="true" title="<%= com.hrms.i18n.Language.text(request, "ui_d6d7257196c1", "Subscription đang hoạt động") %>"><svg class="icon" aria-hidden="true"><use href="${icons}#check"/></svg><%= com.hrms.i18n.Language.text(request, "ui_c29d2e125612", "Kích hoạt Subscription") %></button><button class="button button-suspend" type="button" data-dialog-open="suspend-subscription-dialog"><svg class="icon" aria-hidden="true"><use href="${icons}#close"/></svg><%= com.hrms.i18n.Language.text(request, "ui_2883189c64c9", "Tạm ngưng Subscription") %></button></c:when>
                <c:when test="${company.subscriptionSuspended}"><button class="button button-activate" type="button" data-dialog-open="activate-subscription-dialog"><svg class="icon" aria-hidden="true"><use href="${icons}#check"/></svg><%= com.hrms.i18n.Language.text(request, "ui_c29d2e125612", "Kích hoạt Subscription") %></button><button class="button button-suspend" type="button" disabled aria-disabled="true" title="<%= com.hrms.i18n.Language.text(request, "ui_eb931602db13", "Subscription đã tạm ngưng") %>"><svg class="icon" aria-hidden="true"><use href="${icons}#close"/></svg><%= com.hrms.i18n.Language.text(request, "ui_2883189c64c9", "Tạm ngưng Subscription") %></button></c:when>
            </c:choose>
        </div>
    </section>
    <c:if test="${not empty actionMessage}"><div class="alert alert-success" role="status"><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("actionMessage")) %>'/></div></c:if>

    <div class="company-detail-row company-detail-row-top">
        <section class="company-info-card" aria-labelledby="company-info-title">
            <header class="company-card-heading"><span class="company-card-icon icon-blue"><svg class="icon" aria-hidden="true"><use href="${icons}#building"/></svg></span><h2 id="company-info-title"><%= com.hrms.i18n.Language.text(request, "ui_96be95a06809", "Thông tin Doanh nghiệp &amp; Workspace") %></h2><span class="tenant-id">Tenant ID: #<c:out value="${company.companyId}"/></span></header>
            <div class="company-info-grid">
                <div><span><%= com.hrms.i18n.Language.text(request, "ui_24db39d3bf97", "Tên công ty") %></span><strong><c:out value="${company.companyName}"/></strong></div>
                <div><span><%= com.hrms.i18n.Language.text(request, "ui_2cfb589c3008", "Mã định danh Workspace") %></span><strong class="workspace-code workspace-code-box"><c:out value="${company.workspaceCode}"/></strong></div>
                <div><span><%= com.hrms.i18n.Language.text(request, "ui_bd9db06383e6", "Khu vực phân vùng") %></span><strong><span class="region-label"><c:set var="i18nDisplayLabel" value="${company.regionLabel}"/><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("i18nDisplayLabel")) %>'/></span></strong></div>
                <div><span><%= com.hrms.i18n.Language.text(request, "ui_e26dfbe9dc12", "Thời gian phê duyệt &amp; tạo") %></span><strong><c:out value="${company.companyCreatedAtDisplay}"/></strong></div>
            </div>
        </section>

        <section class="company-info-card" aria-labelledby="admin-info-title">
            <header class="company-card-heading"><span class="company-card-icon icon-indigo"><svg class="icon" aria-hidden="true"><use href="${icons}#user"/></svg></span><h2 id="admin-info-title"><%= com.hrms.i18n.Language.text(request, "ui_90313706876c", "Thông tin Người đại diện (Company Admin)") %></h2></header>
            <div class="company-admin-profile"><span class="company-admin-avatar"><c:out value="${company.companyAdminInitial}"/></span><div><strong><c:out value="${company.companyAdminName}" default='<%= com.hrms.i18n.Language.text(request, "ui_8e3f7f9adb87", "Chưa có Company Admin") %>'/></strong><span><%= com.hrms.i18n.Language.text(request, "ui_11ce57032cc5", "Đại diện pháp lý &amp; quản trị viên của Tenant") %></span></div></div>
            <div class="company-admin-contact"><div><span><%= com.hrms.i18n.Language.text(request, "ui_49b6521a4634", "Email đăng nhập &amp; liên hệ") %></span><strong><c:out value="${company.companyAdminEmail}" default="—"/></strong></div><div><span><%= com.hrms.i18n.Language.text(request, "ui_58f52301a801", "Số điện thoại") %></span><strong><c:out value="${company.contactPhone}" default="—"/></strong></div><div><span><%= com.hrms.i18n.Language.text(request, "ui_87ac4f1bf9ae", "Thời gian kích hoạt") %></span><strong><c:out value="${company.companyAdminActivatedAtDisplay}"/></strong></div></div>
        </section>
    </div>

    <div class="company-detail-row company-detail-row-bottom">
        <section class="company-info-card company-quota-card" aria-labelledby="quota-title">
            <header class="company-card-heading quota-heading"><span class="company-card-icon icon-teal"><svg class="icon" aria-hidden="true"><use href="${icons}#users"/></svg></span><div><h2 id="quota-title"><%= com.hrms.i18n.Language.text(request, "ui_b1beb1a90fed", "Định mức &amp; Phân bổ Tài khoản") %></h2><p><%= com.hrms.i18n.Language.text(request, "ui_d75f1680ec85", "Giới hạn tối đa") %> <c:out value="${company.maxAccounts}"/> <%= com.hrms.i18n.Language.text(request, "ui_49d75d4ce01d", "User theo gói dịch vụ") %></p></div><div class="quota-summary"><span><%= com.hrms.i18n.Language.text(request, "ui_de94eef9992e", "Đã sử dụng") %></span><strong><c:out value="${company.accountCount}"/> <em>/ <c:out value="${company.maxAccounts}"/></em></strong></div></header>
            <div class="quota-progress" role="img" aria-label="Đã sử dụng ${company.accountUsagePercent}% hạn mức"><span style="width:${company.accountUsagePercent}%"></span></div>
            <div class="quota-legend"><span><i></i><c:out value="${company.accountCount}"/> <%= com.hrms.i18n.Language.text(request, "ui_f672fdc909b3", "tài khoản đang dùng") %></span><strong><%= com.hrms.i18n.Language.text(request, "ui_1f4a25c6dee1", "Còn trống:") %> <c:out value="${company.availableAccounts}"/> <%= com.hrms.i18n.Language.text(request, "ui_b3c66a8c6770", "vị trí") %></strong></div>
            <div class="core-members-section">
                <h3><%= com.hrms.i18n.Language.text(request, "ui_89f297e05a5e", "THÀNH VIÊN CỐT CÁN TRONG COMPANY") %></h3>
                <c:choose>
                    <c:when test="${empty company.coreMembers}">
                        <p class="core-members-empty"><%= com.hrms.i18n.Language.text(request, "ui_dd8f9b015c67", "Chưa có tài khoản thành viên trong Company.") %></p>
                    </c:when>
                    <c:otherwise>
                        <div class="table-wrap core-members-wrap">
                            <table class="request-table core-members-table">
                                <thead><tr><th scope="col"><%= com.hrms.i18n.Language.text(request, "ui_f3d81e4e1b96", "Nhân sự") %></th><th scope="col"><%= com.hrms.i18n.Language.text(request, "ui_60c91e5a60ac", "Email nội bộ") %></th><th scope="col"><%= com.hrms.i18n.Language.text(request, "ui_13f858e6a912", "Vai trò (Role)") %></th><th scope="col"><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></th></tr></thead>
                                <tbody><c:forEach var="member" items="${company.coreMembers}"><tr><td><span class="avatar member-avatar"><c:out value="${member.initials}"/></span><strong><c:out value="${member.displayName}" default="—"/></strong></td><td class="member-email"><c:out value="${member.email}" default="—"/></td><td><span class="status ${member.roleCode == 'MANAGER' ? 'status-CONVERTING' : 'status-NEW'}"><c:set var="i18nDisplayLabel" value="${member.roleLabel}"/><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("i18nDisplayLabel")) %>'/></span></td><td><span class="status ${member.active ? 'status-CONVERTED' : 'status-UNDER_REVIEW'}"><span class="status-dot" aria-hidden="true"></span><c:out value="${member.active ? 'Active' : 'Inactive'}"/></span></td></tr></c:forEach></tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
            <p class="quota-readonly-note"><%= com.hrms.i18n.Language.text(request, "ui_d1378dd1d413", "System Admin chỉ xem số lượng tài khoản; quản lý người dùng thuộc phạm vi Company Admin.") %></p>
        </section>

        <section class="company-info-card company-subscription-card" aria-labelledby="subscription-title">
            <header class="company-card-heading"><span class="company-card-icon icon-orange"><svg class="icon" aria-hidden="true"><use href="${icons}#info"/></svg></span><h2 id="subscription-title"><%= com.hrms.i18n.Language.text(request, "ui_870a3550ccb1", "Gói dịch vụ (Subscription)") %></h2><span class="subscription-status ${company.subscriptionActive ? 'is-active' : 'is-suspended'}"><i></i><c:set var="i18nDisplayLabel" value="${company.subscriptionStatus}"/><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("i18nDisplayLabel")) %>'/></span></header>
            <div class="subscription-price"><span><%= com.hrms.i18n.Language.text(request, "ui_5ee98fd590e9", "Mức phí áp dụng") %></span><strong><fmt:formatNumber value="${company.price}" type="number" maxFractionDigits="0"/> <small><%= com.hrms.i18n.Language.text(request, "ui_7495bb1ec8ee", "đ / tháng") %></small></strong><p><%= com.hrms.i18n.Language.text(request, "ui_dc5410fcfa1f", "Gói dịch vụ mặc định của doanh nghiệp.") %></p></div>
            <dl class="subscription-data"><div><dt><%= com.hrms.i18n.Language.text(request, "ui_4523f67ff820", "Gói cấu hình:") %></dt><dd><c:out value="${company.planName}"/></dd></div><div><dt><%= com.hrms.i18n.Language.text(request, "ui_e18c02837795", "Ngày bắt đầu:") %></dt><dd><c:out value="${company.subscriptionStartDateDisplay}"/></dd></div><div><dt><%= com.hrms.i18n.Language.text(request, "ui_ea3213ee56cf", "Ngày kết thúc:") %></dt><dd><c:out value="${company.subscriptionEndDateDisplay}"/></dd></div><c:if test="${company.subscriptionSuspended}"><div><dt><%= com.hrms.i18n.Language.text(request, "ui_39d30a18c65b", "Tạm ngưng lúc:") %></dt><dd><c:out value="${company.suspendedAtDisplay}"/></dd></div><div class="subscription-reason"><dt><%= com.hrms.i18n.Language.text(request, "ui_5bb5c802eea4", "Lý do tạm ngưng:") %></dt><dd><c:out value="${company.suspensionReason}"/></dd></div></c:if></dl>
        </section>
    </div>

    <c:if test="${company.subscriptionActive}"><dialog class="action-dialog" id="suspend-subscription-dialog" aria-labelledby="suspend-subscription-title"><form method="post" action="${pageContext.request.contextPath}/systemadmin/company/suspend"><input type="hidden" name="id" value="<c:out value='${company.companyId}'/>"><input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.systemAdminCsrfToken}'/>"><h2 id="suspend-subscription-title"><%= com.hrms.i18n.Language.text(request, "ui_db381a7248ed", "Xác nhận Tạm ngưng Subscription") %></h2><p>Workspace <strong><c:out value="${company.workspaceCode}"/></strong> <%= com.hrms.i18n.Language.text(request, "ui_8857ddac5c69", "sẽ bị chặn truy cập, kể cả các phiên tenant đang đăng nhập. Company và tài khoản vẫn được giữ nguyên.") %></p><label class="dialog-field" for="suspension-reason"><%= com.hrms.i18n.Language.text(request, "ui_e0e820b261f4", "Lý do tạm ngưng dịch vụ") %> <span aria-hidden="true">*</span></label><textarea id="suspension-reason" name="reason" maxlength="500" required rows="3" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_6d6b4aebe683", "Nhập lý do tạm ngưng") %>"></textarea><div class="dialog-actions"><button class="button" type="button" data-dialog-close><%= com.hrms.i18n.Language.text(request, "ui_247d4b1efe9c", "Hủy bỏ") %></button><button class="button button-danger" type="submit"><%= com.hrms.i18n.Language.text(request, "ui_6cdc712ea0c8", "Xác nhận tạm ngưng") %></button></div></form></dialog></c:if>
    <c:if test="${company.subscriptionSuspended}"><dialog class="action-dialog" id="activate-subscription-dialog" aria-labelledby="activate-subscription-title"><form method="post" action="${pageContext.request.contextPath}/systemadmin/company/activate"><input type="hidden" name="id" value="<c:out value='${company.companyId}'/>"><input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.systemAdminCsrfToken}'/>"><h2 id="activate-subscription-title"><%= com.hrms.i18n.Language.text(request, "ui_85a4da3fb635", "Kích hoạt lại Subscription?") %></h2><p><%= com.hrms.i18n.Language.text(request, "ui_01ec9e32467e", "Kích hoạt lại Subscription hiện có cho") %> <strong><c:out value="${company.companyName}"/></strong><%= com.hrms.i18n.Language.text(request, "ui_28099819f0a4", ". Tài khoản bị khóa hoặc vô hiệu hóa sẽ không tự được mở lại.") %></p><label class="dialog-field" for="activation-reason"><%= com.hrms.i18n.Language.text(request, "ui_1ab8ccefe801", "Lý do kích hoạt lại dịch vụ") %> <span aria-hidden="true">*</span></label><textarea id="activation-reason" name="reason" maxlength="500" required rows="3" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_185c5edfad26", "Nhập lý do kích hoạt lại") %>"></textarea><div class="dialog-actions"><button class="button" type="button" data-dialog-close><%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %></button><button class="button button-primary" type="submit"><%= com.hrms.i18n.Language.text(request, "ui_5f452ce1b293", "Xác nhận kích hoạt") %></button></div></form></dialog></c:if>
</main>
</body>
</html>
