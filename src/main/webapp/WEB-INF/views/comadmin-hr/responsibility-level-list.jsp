<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_31bf9078d724", "Quản lý Cấp trách nhiệm") %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
</head>
<body class="lc" data-page="responsibility-level-list">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content">
        <div class="lc-breadcrumb"><%= com.hrms.i18n.Language.text(request, "ui_6e3d81c992f7", "Quản trị nhân sự / Cơ cấu tổ chức") %></div>
        <div class="page-header">
            <div><h1 class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_31bf9078d724", "Quản lý Cấp trách nhiệm") %></h1><p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_8d991cdf1752", "Thứ bậc và phụ cấp mặc định dùng chung cho các chức danh trong công ty.") %></p></div>
            <a class="button" href="responsibility-level-form"><%= com.hrms.i18n.Language.text(request, "ui_6c76cdc70cca", "+ Thêm cấp trách nhiệm") %></a>
        </div>
        <section class="summary-grid">
            <div class="summary-card"><div class="summary-label"><%= com.hrms.i18n.Language.text(request, "ui_b2b6c8eadaf2", "Tổng cấp trách nhiệm") %></div><div id="totalLevels" class="summary-number">0</div></div>
        </section>
        <div id="message" class="message" role="status"></div>
        <label for="statusFilter"><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></label>
        <select id="statusFilter">
            <option value=""><%= com.hrms.i18n.Language.text(request, "ui_f7a578dcbdca", "Tất cả") %></option>
            <option value="Active"><%= com.hrms.i18n.Language.text(request, "ui_9a616fcdadd0", "Đang hoạt động") %></option>
            <option value="Inactive"><%= com.hrms.i18n.Language.text(request, "ui_1ae508f9aaf1", "Ngừng hoạt động") %></option>
        </select>

        <section class="panel">
            <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_3d2fa6b6b539", "thứ bậc xác định thứ bậc trách nhiệm, không cấp quyền quản lý nhân viên hoặc quyền truy cập hệ thống.") %></p>
            <table data-pagination="responsibility-levels">
                <thead><tr><th><%= com.hrms.i18n.Language.text(request, "ui_1780821b4a40", "Mã cấp") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_e36d4695d33b", "Tên cấp trách nhiệm") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_33939051c309", "Thứ bậc") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_42fbfed4a5dd", "Phụ cấp mặc định") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th></tr></thead>
                <tbody id="levelRows"></tbody>
            </table>
        </section>
    </main>


<script src="../js/responsibility-level.js?v=<%= application.getResource("/js/responsibility-level.js").openConnection().getLastModified() %>"></script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js?v=<%= application.getResource("/js/organization-shell.js").openConnection().getLastModified() %>" defer></script>
</body>
</html>
