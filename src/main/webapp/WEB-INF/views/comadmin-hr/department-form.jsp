<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_61224ffd829b", "Thông tin phòng ban") %></title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc" data-page="department-form">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content">
        <div class="lc-breadcrumb"><%= com.hrms.i18n.Language.text(request, "ui_6e3d81c992f7", "Quản trị nhân sự / Cơ cấu tổ chức") %></div>

        <div class="page-header">
            <div>
                <h1 id="departmentFormTitle" class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_6fa830e07714", "Thêm phòng ban") %></h1>
            </div>
            <a class="button secondary" href="department-list"><%= com.hrms.i18n.Language.text(request, "ui_34ca764caf22", "Hủy") %></a>
        </div>

        <div id="message" class="message"></div>
        <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_ad043c42fcb3", "Phòng ban chứa các chức danh. Ngừng hoạt động sẽ chặn phân công mới, không xóa nhân viên hoặc lịch sử hiện có.") %></p>

        <form id="departmentForm" class="panel">
            <input type="hidden" name="departmentId">
            <div class="form-grid">
                <div>
                    <label><%= com.hrms.i18n.Language.text(request, "ui_20ed2ad71052", "Mã phòng ban") %></label>
                    <input name="departmentCode" maxlength="50" required>
                </div>
                <div>
                    <label><%= com.hrms.i18n.Language.text(request, "ui_f2e8d8bc0044", "Tên phòng ban") %></label>
                    <input name="departmentName" maxlength="150" required>
                </div>
            </div>
            <div class="form-actions">
                <button type="submit"><%= com.hrms.i18n.Language.text(request, "ui_a306970e8b81", "Lưu") %></button>
                <a class="button secondary" href="department-list"><%= com.hrms.i18n.Language.text(request, "ui_34ca764caf22", "Hủy") %></a>
            </div>
        </form>

    </main>


<script src="../js/department.js?v=<%= application.getResource("/js/department.js").openConnection().getLastModified() %>"></script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js?v=<%= application.getResource("/js/organization-shell.js").openConnection().getLastModified() %>" defer></script>
</body>
</html>
