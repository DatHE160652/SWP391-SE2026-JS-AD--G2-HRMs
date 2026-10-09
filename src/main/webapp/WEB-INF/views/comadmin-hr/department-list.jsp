<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Department Management</title>
    <link rel="stylesheet" href="../css/employee.css">
</head>
<body data-page="department-list">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>TechNova JSC</span></div>
        <div class="nav-label">Phân hệ quản trị</div>
        <a class="nav-link" href="../employee/employee-list">Quản lý Nhân sự</a>
        <a class="nav-link active" href="department-list">Quản lý Phòng ban</a>
        <a class="nav-link" href="../position/position-list">Quản lý Chức danh</a>
        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">

        <div class="page-header">
            <div>
                <h1 class="page-title">Quản lý Phòng ban</h1>
            </div>
            <a class="button" href="department-form">+ Add Department</a>
        </div>

        <section class="summary-grid">
            <div class="summary-card">
                <div class="summary-label">Total Departments</div>
                <div id="totalDepartments" class="summary-number">0</div>
            </div>
        </section>

        <div id="message" class="message"></div>

        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th>Department Code</th>
                    <th>Department Name</th>
                    <th>Created At</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody id="departmentRows"></tbody>
            </table>
        </section>
    </main>
</div>
<script src="../js/department.js"></script>
</body>
</html>
