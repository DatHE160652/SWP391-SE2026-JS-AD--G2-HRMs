<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_45243efa4b09", "Quản lý Chức danh") %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
</head>
<body class="lc" data-page="position-list">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content">
        <div class="lc-breadcrumb"><%= com.hrms.i18n.Language.text(request, "ui_6e3d81c992f7", "Quản trị nhân sự / Cơ cấu tổ chức") %></div>
        <div class="page-header" style="display:flex;justify-content:space-between;align-items:center;">
            <div>
                <h1 class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_45243efa4b09", "Quản lý Chức danh") %></h1>
            </div>
            <div style="display:flex;gap:10px;">
                <a id="btnExportPositions" class="button secondary" href="/positions/export" style="text-decoration:none;display:inline-flex;align-items:center;gap:6px;cursor:pointer;">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                    <%= com.hrms.i18n.Language.text(request, "ui_1721265dcbc2", "Xuất Excel") %>
                </a>
                <a class="button" href="position-form"><%= com.hrms.i18n.Language.text(request, "ui_799509a7bfca", "+ Thêm chức danh") %></a>
            </div>
        </div>

        <section class="summary-grid">
            <div class="summary-card">
                <div class="summary-label"><%= com.hrms.i18n.Language.text(request, "ui_c33bc1ef4673", "Tổng chức danh") %></div>
                <div id="totalPositions" class="summary-number">0</div>
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
            <label for="departmentFilter"><%= com.hrms.i18n.Language.text(request, "ui_6f358f848e2e", "Phòng ban") %></label>
            <select id="departmentFilter"></select>
            <label for="levelFilter"><%= com.hrms.i18n.Language.text(request, "ui_a77b71180722", "Cấp trách nhiệm") %></label><select id="levelFilter"></select>
            <table data-pagination="positions">
                <thead>
                <tr>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_1369837fdad0", "Tên chức danh") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_6f358f848e2e", "Phòng ban") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_a77b71180722", "Cấp trách nhiệm") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_8de0b001ab79", "Phụ cấp áp dụng") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th>
                </tr>
                </thead>
                <tbody id="positionRows"></tbody>
            </table>
        </section>
    </main>


<script src="../js/position.js?v=<%= application.getResource("/js/position.js").openConnection().getLastModified() %>"></script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js?v=<%= application.getResource("/js/organization-shell.js").openConnection().getLastModified() %>" defer></script>
</body>
</html>
