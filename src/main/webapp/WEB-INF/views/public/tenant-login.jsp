<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng nhập công ty | HRMs Cloud</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/public-portal.css">
</head>
<body class="tenant-login-page">
<main class="tenant-login-main">
    <section class="tenant-login-card" aria-labelledby="tenant-login-title">
        <div class="tenant-login-heading">
            <span class="eyebrow">WORKSPACE</span>
            <h1 id="tenant-login-title">Đăng nhập</h1>
            <p>Nhập thông tin workspace và tài khoản tenant của doanh nghiệp.</p>
        </div>
        <c:if test="${not empty loginError}">
            <div class="alert alert-warning" role="alert"><c:out value="${loginError}"/></div>
        </c:if>
        <form method="post" action="${pageContext.request.contextPath}/login" class="tenant-login-form">
            <div class="form-field">
                <label for="tenantDomain">Workspace Domain <span>*</span></label>
                <div class="domain-input">
                    <span>https://</span>
                    <input id="tenantDomain" name="tenantDomain" type="text" maxlength="50" required value="<c:out value='${tenantDomain}'/>" placeholder="ten-cong-ty">
                    <span>.hrms.com</span>
                </div>
            </div>
            <div class="form-field">
                <label for="tenantEmail">Email tài khoản <span>*</span></label>
                <input id="tenantEmail" name="tenantEmail" type="email" maxlength="150" required value="<c:out value='${tenantEmail}'/>" placeholder="nhanvien@congty.vn">
            </div>
            <div class="form-field">
                <label for="tenantPassword">Mật khẩu <span>*</span></label>
                <input id="tenantPassword" name="tenantPassword" type="password" required placeholder="••••••••">
            </div>
            <button class="button button-primary button-wide" type="submit">Đăng nhập vào workspace</button>
        </form>
        <div class="tenant-login-links">
            <a href="${pageContext.request.contextPath}/consultation-request">Chưa có workspace? Đăng ký tư vấn</a>
            <a href="${pageContext.request.contextPath}/homepage">Về trang chủ</a>
        </div>
    </section>
</main>
</body>
</html>
