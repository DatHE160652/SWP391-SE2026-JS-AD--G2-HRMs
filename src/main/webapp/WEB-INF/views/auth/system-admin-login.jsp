<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng nhập | HRMs.Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/system-admin.css">
</head>
<body class="auth-page">
<c:set var="icons" value="${pageContext.request.contextPath}/images/admin-icons.svg"/>
<main class="auth-card" aria-labelledby="login-title">
    <div class="accent-line" aria-hidden="true"></div>
    <div class="brand"><img src="${pageContext.request.contextPath}/images/hrms-admin.svg" alt="HRMs.Admin" width="144" height="36"></div>
    <h1 id="login-title">Đăng nhập Cổng Quản trị Hệ thống</h1>
    <c:if test="${not empty errorMessage}"><div class="alert alert-error" role="alert"><c:out value="${errorMessage}"/></div></c:if>
    <form method="post" action="${pageContext.request.contextPath}/systemadmin/login" class="auth-form">
        <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.systemAdminCsrfToken}'/>">
        <div class="label-row"><label for="identifier">Tên đăng nhập / Email quản trị</label><span>SysAdmin Role</span></div>
        <div class="input-icon">
            <svg class="icon" aria-hidden="true"><use href="${icons}#user"/></svg>
            <input id="identifier" name="identifier" type="text" autocomplete="username" required value="<c:out value='${param.identifier}'/>" placeholder="admin@example.com">
        </div>
        <label for="password">Mật khẩu</label>
        <div class="input-icon password-field">
            <svg class="icon" aria-hidden="true"><use href="${icons}#lock"/></svg>
            <input id="password" name="password" type="password" autocomplete="current-password" required>
            <button type="button" class="password-toggle icon-button" data-password-toggle="password" aria-label="Hiện mật khẩu" aria-pressed="false"><svg class="icon" aria-hidden="true"><use href="${icons}#eye"/></svg></button>
        </div>
        <button class="button button-primary button-wide" type="submit"><svg class="icon" aria-hidden="true"><use href="${icons}#login"/></svg>Đăng nhập Quản trị Hệ thống</button>
    </form>
</main>
<script src="${pageContext.request.contextPath}/js/system-admin.js" defer></script>
</body>
</html>
