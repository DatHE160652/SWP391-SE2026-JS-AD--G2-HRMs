<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Position Management</title>
    <link rel="stylesheet" href="../css/employee.css">
</head>
<body data-page="position-list">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>TechNova JSC</span></div>
        <div class="nav-label">Phân hệ quản trị</div>
        <a class="nav-link" href="../employee/employee-list">Quản lý Nhân sự</a>
        <a class="nav-link" href="../department/department-list">Quản lý Phòng ban</a>
        <a class="nav-link active" href="position-list">Quản lý Chức danh</a>
        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title">Quản lý Chức danh</h1>
            </div>
            <a class="button" href="position-form">+ Add Position</a>
        </div>

        <section class="summary-grid">
            <div class="summary-card">
                <div class="summary-label">Total Positions</div>
                <div id="totalPositions" class="summary-number">0</div>
            </div>
        </section>

        <div id="message" class="message"></div>

        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th>Position Title</th>
                    <th>Department</th>
                    <th>Description</th>
                    <th>Created At</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody id="positionRows"></tbody>
            </table>
        </section>
    </main>
</div>
<script src="../js/position.js?v=<%= application.getResource("/js/position.js").openConnection().getLastModified() %>"></script>
</body>
</html>
