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

        <div class="page-header">
            <div>
                <h1 class="page-title">Quản lý Phòng ban</h1>
            </div>
            <a class="button" href="department-form">+ Thêm phòng ban</a>
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
