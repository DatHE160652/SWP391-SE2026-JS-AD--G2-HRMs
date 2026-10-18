<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Department Form</title>
    <link rel="stylesheet" href="../css/employee.css">
</head>
<body data-page="department-form">
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
                <h1 id="departmentFormTitle" class="page-title">Add Department</h1>
            </div>
            <a class="button secondary" href="department-list">Cancel</a>
        </div>

        <div id="message" class="message"></div>

        <form id="departmentForm" class="panel">
            <input type="hidden" name="departmentId">
            <div class="form-grid">
                <div>
                    <label>Department Code</label>
                    <input name="departmentCode" required>
                </div>
                <div>
                    <label>Department Name</label>
                    <input name="departmentName" required>
                </div>
            </div>
            <div class="form-actions">
                <button type="submit">Save</button>
                <a class="button secondary" href="department-list">Cancel</a>
            </div>
        </form>

        <section id="departmentMembersPanel" class="panel hidden">
            <div class="document-toolbar">
                <div>
                    <h2>Department Members</h2>
                    <p class="page-subtitle">Chọn trưởng phòng và nhiều nhân viên cho phòng ban này.</p>
                </div>
            </div>
            <form id="departmentMembersForm">
                <input type="hidden" name="departmentId">
                <div class="form-grid">
                    <div>
                        <label>Trưởng phòng</label>
                        <select name="managerId" required></select>
                    </div>
                </div>
                <div id="departmentEmployeeOptions" class="member-picker"></div>
                <div class="form-actions">
                    <button type="submit">Save Department Members</button>
                </div>
            </form>
        </section>
    </main>
</div>
<script src="../js/department.js?v=<%= application.getResource("/js/department.js").openConnection().getLastModified() %>"></script>
</body>
</html>
