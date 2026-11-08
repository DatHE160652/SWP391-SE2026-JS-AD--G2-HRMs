<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= com.hrms.i18n.Language.text(request, "ui_8119b26bb800", "Đăng nhập | HRMs.Admin") %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/system-admin.css">
</head>
<body class="auth-page">
<c:set var="icons" value="${pageContext.request.contextPath}/images/admin-icons.svg"/>
<main class="auth-card" aria-labelledby="login-title">
    <div class="accent-line" aria-hidden="true"></div>
    <div class="brand"><img src="${pageContext.request.contextPath}/images/hrms-admin.svg" alt="HRMs.Admin" width="144" height="36"></div>
    <h1 id="login-title"><%= com.hrms.i18n.Language.text(request, "ui_401e469bc821", "Đăng nhập Cổng Quản trị Hệ thống") %></h1>
    <c:if test="${not empty errorMessage}"><div class="alert alert-error" role="alert"><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("errorMessage")) %>'/></div></c:if>
    <form method="post" action="${pageContext.request.contextPath}/systemadmin/login" class="auth-form">
        <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.systemAdminCsrfToken}'/>">
        <div class="label-row"><label for="email"><%= com.hrms.i18n.Language.text(request, "ui_0fc4b12bcff0", "Email quản trị") %></label><span>SysAdmin Role</span></div>
        <div class="input-icon">
            <svg class="icon" aria-hidden="true"><use href="${icons}#user"/></svg>
            <input id="email" name="email" type="email" autocomplete="username" maxlength="150" required value="<c:out value='${param.email}'/>" placeholder="admin@example.com">
        </div>
        <label for="password"><%= com.hrms.i18n.Language.text(request, "ui_888c9c47cfe0", "Mật khẩu") %></label>
        <div class="input-icon password-field">
            <svg class="icon" aria-hidden="true"><use href="${icons}#lock"/></svg>
            <input id="password" name="password" type="password" autocomplete="current-password" required>
            <button type="button" class="password-toggle icon-button" data-password-toggle="password" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_b56a27bfee69", "Hiện mật khẩu") %>" aria-pressed="false"><svg class="icon" aria-hidden="true"><use href="${icons}#eye"/></svg></button>
        </div>
        <button class="button button-primary button-wide" type="submit"><svg class="icon" aria-hidden="true"><use href="${icons}#login"/></svg><%= com.hrms.i18n.Language.text(request, "ui_459ffaae7805", "Đăng nhập Quản trị Hệ thống") %></button>
    </form>
</main>
<script src="${pageContext.request.contextPath}/js/system-admin.js?v=<%= application.getResource("/js/system-admin.js").openConnection().getLastModified() %>" defer></script>
</body>
</html>
