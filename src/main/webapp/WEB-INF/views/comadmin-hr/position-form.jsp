<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_2ad24d69874d", "Thông tin chức danh") %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
</head>
<body class="lc" data-page="position-form">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content">
        <div class="lc-breadcrumb"><%= com.hrms.i18n.Language.text(request, "ui_6e3d81c992f7", "Quản trị nhân sự / Cơ cấu tổ chức") %></div>
        <div class="page-header">
            <div>
                <h1 id="positionFormTitle" class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_cf15e184382a", "Thêm chức danh") %></h1>
                <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_00de21cbc9d1", "Mỗi chức danh thuộc một phòng ban và một cấp trách nhiệm. Chức danh đã sử dụng không được đổi phòng ban hoặc cấp; hãy tạo chức danh mới và điều chuyển có ngày hiệu lực.") %></p>
            </div>
            <a class="button secondary" href="position-list"><%= com.hrms.i18n.Language.text(request, "ui_34ca764caf22", "Hủy") %></a>
        </div>

        <div id="message" class="message"></div>

        <form id="positionForm" class="panel">
            <input type="hidden" name="positionId">
            <div class="form-grid">
                <div>
                    <label><%= com.hrms.i18n.Language.text(request, "ui_6f358f848e2e", "Phòng ban") %></label>
                    <select name="departmentId" required></select>
                </div>
                <div>
                    <label><%= com.hrms.i18n.Language.text(request, "ui_1369837fdad0", "Tên chức danh") %></label>
                    <input name="positionTitle" maxlength="150" required>
                </div>
                <div>
                    <label><%= com.hrms.i18n.Language.text(request, "ui_7ee34367c03d", "Mô tả") %></label>
                    <textarea name="description" rows="3" maxlength="255"></textarea>
                </div>
                <div>
                    <label><%= com.hrms.i18n.Language.text(request, "ui_a77b71180722", "Cấp trách nhiệm") %></label>
                    <select name="responsibilityLevelId" required></select>
                    <a href="../responsibility-level/responsibility-level-list"><%= com.hrms.i18n.Language.text(request, "ui_f6f0a9ae741b", "Quản lý cấp trách nhiệm") %></a>
                    <small id="responsibilityAllowance"></small>
                </div>
                <div>
                    <label><%= com.hrms.i18n.Language.text(request, "ui_d86432294ca8", "Chức danh quản lý trực tiếp") %></label>
                    <select name="reportsToPositionId"><option value=""><%= com.hrms.i18n.Language.text(request, "ui_c5c68c2a1d9b", "Chưa cấu hình") %></option></select>
                    <small><%= com.hrms.i18n.Language.text(request, "ui_eaf2cbc6c739", "Liên kết chức danh nhân viên với chức danh quản lý đúng team trong cùng phòng ban. Không suy đoán team từ tên chức danh.") %></small>
                </div>
                <div>
                    <label><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></label>
                    <select name="isActive"><option value="true"><%= com.hrms.i18n.Language.text(request, "ui_9a616fcdadd0", "Đang hoạt động") %></option><option value="false"><%= com.hrms.i18n.Language.text(request, "ui_1ae508f9aaf1", "Ngừng hoạt động") %></option></select>
                </div>
                <div>
                    <label for="canManageEmployees"><%= com.hrms.i18n.Language.text(request, "ui_fb9ad585defc", "Cho phép quản lý nhân viên") %></label>
                    <input id="canManageEmployees" name="canManageEmployees" type="checkbox" value="true">
                    <small><%= com.hrms.i18n.Language.text(request, "ui_f2bc0e26f551", "Cấp trách nhiệm Employee không được phép quản lý nhân viên. Các cấp khác cần được bật quyền này để làm quản lý trực tiếp.") %></small>
                </div>
            </div>
            <div class="form-actions">
                <button type="submit"><%= com.hrms.i18n.Language.text(request, "ui_a306970e8b81", "Lưu") %></button>
                <a class="button secondary" href="position-list"><%= com.hrms.i18n.Language.text(request, "ui_34ca764caf22", "Hủy") %></a>
            </div>
        </form>
    </main>


<script src="../js/position.js?v=<%= application.getResource("/js/position.js").openConnection().getLastModified() %>"></script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js?v=<%= application.getResource("/js/organization-shell.js").openConnection().getLastModified() %>" defer></script>
</body>
</html>
