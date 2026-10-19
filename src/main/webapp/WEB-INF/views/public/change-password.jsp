<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đổi mật khẩu | HRMs Cloud</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/public-portal.css">
</head>
<body class="tenant-login-page">
<main class="tenant-login-main">
    <section class="tenant-login-card" aria-labelledby="change-password-title">
        <div class="tenant-login-heading">
            <span class="eyebrow">TENANT WORKSPACE</span>
            <h1 id="change-password-title">Đổi mật khẩu</h1>
            <p>Tài khoản đang dùng mật khẩu tạm. Vui lòng đặt mật khẩu mới để tiếp tục.</p>
        </div>
        <c:if test="${not empty changePasswordError}">
            <div class="alert alert-warning" role="alert"><c:out value="${changePasswordError}"/></div>
        </c:if>
        <form method="post" action="${pageContext.request.contextPath}/change-password" class="tenant-login-form">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.tenantPasswordCsrfToken}'/>">
            <div class="form-field">
                <label for="newPassword">Mật khẩu mới <span>*</span></label>
                <input id="newPassword" name="newPassword" type="password" required autocomplete="new-password">
            </div>
            <div class="form-field">
                <label for="confirmPassword">Xác nhận mật khẩu <span>*</span></label>
                <input id="confirmPassword" name="confirmPassword" type="password" required autocomplete="new-password">
            </div>
            <button class="button button-primary button-wide" type="submit">Cập nhật mật khẩu</button>
        </form>
    </section>
</main>
</body>
</html>
