<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Lỗi | HRMs.Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/system-admin.css">
</head>
<body class="auth-page">
<main class="error-card">
    <img src="${pageContext.request.contextPath}/images/hrms-admin.svg" alt="HRMs.Admin">
    <h1>Không thể hoàn tất yêu cầu</h1>
    <p class="muted"><c:out value="${errorMessage}"/></p>
    <a class="button button-primary" href="${pageContext.request.contextPath}/systemadmin/registration-request">Quay lại danh sách</a>
</main>
</body>
</html>
