<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Employee Management</title>
    <link rel="stylesheet" href="../css/employee.css">
</head>
<body data-page="list">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>TechNova JSC</span></div>
        <div class="nav-label">Phân hệ quản trị</div>
        <a class="nav-link active" href="employee-list">Quản lý Nhân sự</a>
        <a class="nav-link" href="../department/department-list">Quản lý Phòng ban</a>
        <a class="nav-link" href="../position/position-list">Quản lý Chức danh</a>
        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title">Danh bạ Nhân sự</h1>
            </div>
            <a class="button" href="employee-form">+ Add Employee</a>
        </div>

        <section class="summary-grid">
            <div class="summary-card">
                <div class="summary-label">Tổng hồ sơ</div>
                <div id="totalEmployees" class="summary-number">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Active</div>
                <div id="activeEmployees" class="summary-number">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Inactive / Other</div>
                <div id="otherEmployees" class="summary-number">0</div>
            </div>
        </section>

        <div id="message" class="message"></div>

        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th>Employee Code</th>
                    <th>Full Name</th>
                    <th>Phone</th>
                    <th>Gender</th>
                    <th>Mã định danh</th>
                    <th>Status</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody id="employeeRows"></tbody>
            </table>
        </section>
    </main>
</div>
<script src="../js/employee.js?v=<%= application.getResource("/js/employee.js").openConnection().getLastModified() %>"></script>
</body>
</html>
