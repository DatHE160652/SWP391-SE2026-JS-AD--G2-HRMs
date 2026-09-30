<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>HRMs Cloud</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/employee.css">
</head>
<body>
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>TechNova JSC</span></div>
        <div class="nav-label">Phân hệ quản trị</div>
        <a class="nav-link active" href="${pageContext.request.contextPath}/employee/employee-list.html">Quản lý Nhân sự</a>
        <a class="nav-link" href="${pageContext.request.contextPath}/department/department-list.html">Quản lý Phòng ban</a>
        <a class="nav-link" href="${pageContext.request.contextPath}/position/position-list.html">Quản lý Chức danh</a>
        <div class="plan-card"><strong>Gói Pro SME</strong><br>Iteration 1: Employee Lifecycle</div>
    </aside>

    <main class="content">
        <div class="topbar">
            <div class="search-box">Employee Lifecycle Data Management</div>
            <div class="today">Iteration 1</div>
            <div class="user">HR Director / Admin</div>
        </div>

        <section class="hero-panel">
            <div>
                <span class="badge Active">BF-01 Employee Lifecycle</span>
                <h1 class="page-title">HRMs Cloud</h1>
                <p class="page-subtitle">
                    Quản lý hồ sơ nhân viên, phòng ban, chức danh, tài khoản và tài liệu cá nhân trong một luồng đơn giản.
                </p>
            </div>
            <a class="button" href="${pageContext.request.contextPath}/employee/employee-list.html">Mở Employee Management</a>
        </section>

        <section class="summary-grid">
            <div class="summary-card">
                <div class="summary-label">UC01</div>
                <div class="summary-number">Directory</div>
                <p class="page-subtitle">Xem danh sách nhân viên.</p>
            </div>
            <div class="summary-card">
                <div class="summary-label">UC04 / UC05</div>
                <div class="summary-number">CRUD</div>
                <p class="page-subtitle">Thêm và cập nhật hồ sơ nhân viên.</p>
            </div>
            <div class="summary-card">
                <div class="summary-label">UC07</div>
                <div class="summary-number">Departments</div>
                <p class="page-subtitle">CRUD phòng ban trong công ty.</p>
            </div>
            <div class="summary-card">
                <div class="summary-label">UC08</div>
                <div class="summary-number">Positions</div>
                <p class="page-subtitle">CRUD chức danh/vị trí trong công ty.</p>
            </div>
            <div class="summary-card">
                <div class="summary-label">UC16 / UC27 / UC28</div>
                <div class="summary-number">Documents</div>
                <p class="page-subtitle">Quản lý tài liệu nhân viên.</p>
            </div>
        </section>

        <section class="panel">
            <div class="document-toolbar">
                <div>
                    <h2>Luồng đang triển khai</h2>
                    <p class="page-subtitle">Validate employee data -> Create employee record -> Manage departments and positions -> Manage documents.</p>
                </div>
                <a class="button secondary" href="${pageContext.request.contextPath}/position/position-list.html">Vào Position Management</a>
            </div>
        </section>
    </main>
</div>
</body>
</html>
