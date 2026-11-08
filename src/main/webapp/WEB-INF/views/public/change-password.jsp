<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= com.hrms.i18n.Language.text(request, "ui_7d46aacfa2da", "Đổi mật khẩu | HRMs Cloud") %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/public-portal.css">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc workspace-auth tenant-login-page">
<main class="tenant-login-main">
    <section class="tenant-login-card" aria-labelledby="change-password-title">
        <div class="tenant-login-heading">
            <span class="eyebrow">WORKSPACE</span>
            <h1 id="change-password-title"><%= com.hrms.i18n.Language.text(request, "ui_d4e1de23305e", "Đổi mật khẩu") %></h1>
            <p><%= com.hrms.i18n.Language.text(request, "ui_101fec028366", "Tài khoản đang dùng mật khẩu tạm. Vui lòng đặt mật khẩu mới để tiếp tục.") %></p>
        </div>
        <c:if test="${not empty changePasswordError and empty changePasswordField}">
            <div class="alert alert-warning" role="alert"><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("changePasswordError")) %>'/></div>
        </c:if>
        <form method="post" action="${pageContext.request.contextPath}/change-password" class="tenant-login-form">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.tenantPasswordCsrfToken}'/>">
            <div class="form-field">
                <label for="newPassword"><%= com.hrms.i18n.Language.text(request, "ui_4267a600ceea", "Mật khẩu mới") %> <span>*</span></label>
                <input id="newPassword" name="newPassword" type="password" required autocomplete="new-password">
                <c:if test="${changePasswordField == 'newPassword'}"><small class="field-error"><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("changePasswordError")) %>'/></small></c:if>
            </div>
            <div class="form-field">
                <label for="confirmPassword"><%= com.hrms.i18n.Language.text(request, "ui_7386c6b17384", "Xác nhận mật khẩu") %> <span>*</span></label>
                <input id="confirmPassword" name="confirmPassword" type="password" required autocomplete="new-password">
                <c:if test="${changePasswordField == 'confirmPassword'}"><small class="field-error"><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("changePasswordError")) %>'/></small></c:if>
            </div>
            <button class="button button-primary button-wide" type="submit"><%= com.hrms.i18n.Language.text(request, "ui_790b8a6b0727", "Cập nhật mật khẩu") %></button>
        </form>
    </section>
</main>
</body>
</html>
