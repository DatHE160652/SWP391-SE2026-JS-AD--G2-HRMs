<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= com.hrms.i18n.Language.text(request, "ui_8f12ad371250", "Lỗi | HRMs.Admin") %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/system-admin.css">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc workspace-auth auth-page">
<main class="error-card">
    <img src="${pageContext.request.contextPath}/images/hrms-admin.svg" alt="HRMs.Admin">
    <h1><%= com.hrms.i18n.Language.text(request, "ui_b6ff00c42b42", "Không thể hoàn tất yêu cầu") %></h1>
    <p class="muted"><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("errorMessage")) %>'/></p>
    <a class="button button-primary" href="${pageContext.request.contextPath}/systemadmin/registration-request"><%= com.hrms.i18n.Language.text(request, "ui_325426ab3168", "Quay lại danh sách") %></a>
</main>
</body>
</html>
