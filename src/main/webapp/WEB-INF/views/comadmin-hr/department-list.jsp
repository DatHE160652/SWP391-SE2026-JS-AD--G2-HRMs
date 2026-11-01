<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý phòng ban</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
</head>
<body class="lc" data-page="department-list">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content">
        <div class="lc-breadcrumb">Quản trị nhân sự / Cơ cấu tổ chức</div>

        <div class="page-header" style="display:flex;justify-content:space-between;align-items:center;">
            <div>
                <h1 class="page-title">Quản lý Phòng ban</h1>
            </div>
            <div style="display:flex;gap:10px;">
                <a id="btnExportDepartments" class="button secondary" href="/departments/export" style="text-decoration:none;display:inline-flex;align-items:center;gap:6px;cursor:pointer;">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                    Xuất Excel
                </a>
                <a class="button" href="department-form">+ Thêm phòng ban</a>
            </div>
        </div>

        <section class="summary-grid">
            <div class="summary-card">
                <div class="summary-label">Tổng phòng ban</div>
                <div id="totalDepartments" class="summary-number">0</div>
            </div>
        </section>

        <div id="message" class="message"></div>

        <label for="statusFilter">Trạng thái</label>
        <select id="statusFilter">
            <option value="">Tất cả</option>
            <option value="Active">Đang hoạt động</option>
            <option value="Inactive">Ngừng hoạt động</option>
        </select>

        <section class="panel">
            <table data-pagination="departments">
                <thead>
                <tr>
                    <th>Mã phòng ban</th>
                    <th>Tên phòng ban</th>
                    <th>Trạng thái</th>
                    <th>Ngày tạo</th>
                    <th>Thao tác</th>
                </tr>
                </thead>
                <tbody id="departmentRows"></tbody>
            </table>
        </section>
    </main>
<script src="../js/department.js?v=<%= application.getResource("/js/department.js").openConnection().getLastModified() %>"></script>
<%@ include file="/WEB-INF/views/common/initial-assignment.jspf" %>
<script src="${pageContext.request.contextPath}/js/organization-shell.js" defer></script>
</body>
</html>
