<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_63e905f01f81", "Tài liệu nhân viên") %></title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/employee-assignment.css">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc" data-page="documents">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content ea-details">
        <div class="lc-breadcrumb"><%= com.hrms.i18n.Language.text(request, "ui_c45a1e44b24c", "Quản trị nhân sự / Hồ sơ nhân viên / Tài liệu") %></div>

        <div class="page-header">
            <div>
                <h1 class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_63e905f01f81", "Tài liệu nhân viên") %></h1>
                <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_99da4f5e368b", "Xem, tải xuống và quản lý tài liệu của nhân viên.") %></p>
            </div>
            <a id="uploadDocumentLink" class="button" href="#"><%= com.hrms.i18n.Language.text(request, "ui_498f1d88e704", "Tải tài liệu lên") %></a>
        </div>

        <a id="documentEmployeeLink" class="ea-profile-link" href="../employee/employee-list"><%= com.hrms.i18n.Language.text(request, "ui_4c17d3c326e1", "← Hồ sơ nhân viên") %></a>
        <div id="message" class="message" role="status"></div>
        <label for="statusFilter"><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></label>
        <select id="statusFilter">
            <option value=""><%= com.hrms.i18n.Language.text(request, "ui_f7a578dcbdca", "Tất cả") %></option>
            <option value="Active"><%= com.hrms.i18n.Language.text(request, "ui_c7a67c3b6211", "Đang sử dụng") %></option>
            <option value="Inactive"><%= com.hrms.i18n.Language.text(request, "ui_46552989a0ef", "Đã lưu trữ") %></option>
        </select>

        <section class="panel">
            <table data-pagination="employee-documents">
                <thead>
                <tr>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_bb295b750c07", "Tên tài liệu") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_7c8dd9841819", "Loại tài liệu") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_9c0863b0dcfc", "Ngày tải lên") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_04dcb5fbde87", "Người tải lên") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th>
                </tr>
                </thead>
                <tbody id="documentRows"></tbody>
            </table>
        </section>
    </main>


<script src="../js/document.js?v=<%= application.getResource("/js/document.js").openConnection().getLastModified() %>"></script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js?v=<%= application.getResource("/js/organization-shell.js").openConnection().getLastModified() %>" defer></script>
</body>
</html>
