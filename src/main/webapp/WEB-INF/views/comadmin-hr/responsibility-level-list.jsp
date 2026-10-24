<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý Cấp trách nhiệm</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
</head>
<body class="lc" data-page="responsibility-level-list">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content">
        <div class="lc-breadcrumb">Quản trị nhân sự / Cơ cấu tổ chức</div>
        <div class="page-header">
            <div><h1 class="page-title">Quản lý Cấp trách nhiệm</h1><p class="page-subtitle">Thứ bậc và phụ cấp mặc định dùng chung cho các chức danh trong công ty.</p></div>
            <a class="button" href="responsibility-level-form">+ Thêm cấp trách nhiệm</a>
        </div>
        <section class="summary-grid">
            <div class="summary-card"><div class="summary-label">Tổng cấp trách nhiệm</div><div id="totalLevels" class="summary-number">0</div></div>
        </section>
        <div id="message" class="message" role="status"></div>
        <label for="statusFilter">Trạng thái</label>
        <select id="statusFilter">
            <option value="">Tất cả</option>
            <option value="Active">Đang hoạt động</option>
            <option value="Inactive">Ngừng hoạt động</option>
        </select>

        <section class="panel">
            <p class="page-subtitle">thứ bậc xác định thứ bậc trách nhiệm, không cấp quyền quản lý nhân viên hoặc quyền truy cập hệ thống.</p>
            <table data-pagination="responsibility-levels">
                <thead><tr><th>Mã cấp</th><th>Tên cấp trách nhiệm</th><th>Thứ bậc</th><th>Phụ cấp mặc định</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
                <tbody id="levelRows"></tbody>
            </table>
        </section>
    </main>
<script src="../js/responsibility-level.js?v=<%= application.getResource("/js/responsibility-level.js").openConnection().getLastModified() %>"></script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js" defer></script>
</body>
</html>
