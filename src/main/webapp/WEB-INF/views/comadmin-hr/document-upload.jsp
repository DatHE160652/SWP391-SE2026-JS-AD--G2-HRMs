<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_498f1d88e704", "Tải tài liệu lên") %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/employee-assignment.css">
</head>
<body class="lc" data-page="document-upload">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content ea-details">
        <div class="lc-breadcrumb"><%= com.hrms.i18n.Language.text(request, "ui_c45a1e44b24c", "Quản trị nhân sự / Hồ sơ nhân viên / Tài liệu") %></div>
        <div class="page-header">
            <div>
                <h1 class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_498f1d88e704", "Tải tài liệu lên") %></h1>
                <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_2cc6c9bf11ec", "Hỗ trợ PDF, DOC, DOCX, JPG, PNG. Dung lượng tối đa 10 MB.") %></p>
            </div>
            <a id="cancelDocumentLink" class="button secondary" href="#"><%= com.hrms.i18n.Language.text(request, "ui_8e6abdbed5ad", "Quay lại tài liệu") %></a>
        </div>

        <a id="documentEmployeeLink" class="ea-profile-link" href="../employee/employee-list"><%= com.hrms.i18n.Language.text(request, "ui_4c17d3c326e1", "← Hồ sơ nhân viên") %></a>
        <div id="message" class="message" role="status"></div>

        <form id="documentUploadForm" class="panel" enctype="multipart/form-data">
            <input type="hidden" name="employeeId">
            <div class="form-grid">
                <div>
                    <label><%= com.hrms.i18n.Language.text(request, "ui_bb295b750c07", "Tên tài liệu") %></label>
                    <input name="documentName" required>
                </div>
                <div>
                    <label><%= com.hrms.i18n.Language.text(request, "ui_7c8dd9841819", "Loại tài liệu") %></label>
                    <input name="documentType" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_042495f1a795", "Hợp đồng, giấy tờ định danh, bằng cấp…") %>">
                </div>
                <div>
                    <label><%= com.hrms.i18n.Language.text(request, "ui_c7e9f7266ee2", "Chọn tệp") %></label>
                    <input name="file" type="file" accept=".pdf,.doc,.docx,.jpg,.png" required>
                </div>
            </div>
            <div class="form-actions">
                <button type="submit"><%= com.hrms.i18n.Language.text(request, "ui_2ca7a184867a", "Tải lên") %></button>
                <a id="cancelDocumentButton" class="button secondary" href="#"><%= com.hrms.i18n.Language.text(request, "ui_8e6abdbed5ad", "Quay lại tài liệu") %></a>
            </div>
        </form>
    </main>


<script src="../js/document.js?v=<%= application.getResource("/js/document.js").openConnection().getLastModified() %>"></script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js?v=<%= application.getResource("/js/organization-shell.js").openConnection().getLastModified() %>" defer></script>
</body>
</html>
