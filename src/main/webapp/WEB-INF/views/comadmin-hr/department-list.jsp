<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_5fa6138e70c3", "Quản lý phòng ban") %></title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc" data-page="department-list">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content">
        <div class="lc-breadcrumb"><%= com.hrms.i18n.Language.text(request, "ui_6e3d81c992f7", "Quản trị nhân sự / Cơ cấu tổ chức") %></div>

        <div class="page-header" style="display:flex;justify-content:space-between;align-items:center;">
            <div>
                <h1 class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_bc217c0a7c21", "Quản lý Phòng ban") %></h1>
            </div>
            <div style="display:flex;gap:10px;">
                <a id="btnExportDepartments" class="button secondary" href="/departments/export" style="text-decoration:none;display:inline-flex;align-items:center;gap:6px;cursor:pointer;">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                    <%= com.hrms.i18n.Language.text(request, "ui_1721265dcbc2", "Xuất Excel") %>
                </a>
                <button type="button" id="btnImportDepartments" class="button secondary" style="display:inline-flex;align-items:center;gap:6px;cursor:pointer;">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="8" x2="12" y2="21"/></svg>
                    <%= "en".equals(com.hrms.i18n.Language.selected(request)) ? "Import Excel" : "Nhập Excel" %>
                </button>
                <a class="button" href="department-form"><%= com.hrms.i18n.Language.text(request, "ui_122fc9e2b66c", "+ Thêm phòng ban") %></a>
            </div>
        </div>

        <section class="summary-grid">
            <div class="summary-card">
                <div class="summary-label"><%= com.hrms.i18n.Language.text(request, "ui_bc39d783e13e", "Tổng phòng ban") %></div>
                <div id="totalDepartments" class="summary-number">0</div>
            </div>
        </section>

        <div id="message" class="message"></div>

        <label for="statusFilter"><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></label>
        <select id="statusFilter">
            <option value=""><%= com.hrms.i18n.Language.text(request, "ui_f7a578dcbdca", "Tất cả") %></option>
            <option value="Active"><%= com.hrms.i18n.Language.text(request, "ui_9a616fcdadd0", "Đang hoạt động") %></option>
            <option value="Inactive"><%= com.hrms.i18n.Language.text(request, "ui_1ae508f9aaf1", "Ngừng hoạt động") %></option>
        </select>

        <section class="panel">
            <table data-pagination="departments">
                <thead>
                <tr>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_20ed2ad71052", "Mã phòng ban") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_f2e8d8bc0044", "Tên phòng ban") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_8b2d9e8a26a5", "Ngày tạo") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th>
                </tr>
                </thead>
                <tbody id="departmentRows"></tbody>
            </table>
        </section>
    </main>


<script src="../js/department.js?v=<%= application.getResource("/js/department.js").openConnection().getLastModified() %>"></script>
<%@ include file="/WEB-INF/views/common/initial-assignment.jspf" %>
<%@ include file="/WEB-INF/views/common/import-modal.jspf" %>
<script src="${pageContext.request.contextPath}/js/import-modal.js"></script>
<script>
    document.getElementById('btnImportDepartments')?.addEventListener('click', function() {
        DataImporter.open('DEPARTMENT', {
            onSuccess: function() {
                if (typeof loadDepartments === 'function') {
                    loadDepartments();
                } else {
                    location.reload();
                }
            }
        });
    });
</script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js?v=<%= application.getResource("/js/organization-shell.js").openConnection().getLastModified() %>" defer></script>
</body>
</html>
