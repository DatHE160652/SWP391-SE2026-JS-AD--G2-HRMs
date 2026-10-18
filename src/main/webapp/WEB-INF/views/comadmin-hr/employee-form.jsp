<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Employee Form</title>
    <link rel="stylesheet" href="../css/employee.css">
</head>
<body data-page="form">
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
                <h1 id="formTitle" class="page-title">Add Employee</h1>
            </div>
            <a class="button secondary" href="employee-list">Cancel</a>
        </div>

        <div id="message" class="message"></div>

        <form id="employeeForm" class="panel">
            <input type="hidden" name="employeeId">
            <div class="form-grid">
                <div id="employeeCodeModeField">
                    <label for="employeeCodeMode">Cách cấp mã nhân viên</label>
                    <select id="employeeCodeMode" name="employeeCodeMode">
                        <option value="MANUAL">Nhập mã thủ công</option>
                        <option value="AUTO">Tự động tạo mã</option>
                    </select>
                </div>
                <div>
                    <label for="employeeCode">Employee Code</label>
                    <input id="employeeCode" name="employeeCode" maxlength="50" required
                           aria-describedby="employeeCodeHint">
                    <small id="employeeCodeHint">Nhập mã đang dùng tại công ty, không bắt buộc định dạng.</small>
                </div>
                <div>
                    <label>Full Name</label>
                    <input name="fullName" required>
                </div>
                <div>
                    <label>Phone</label>
                    <input name="phone" required>
                </div>
                <div>
                    <label>Gender</label>
                    <select name="gender" required>
                        <option value="">Select gender</option>
                        <option value="Male">Male</option>
                        <option value="Female">Female</option>
                        <option value="Other">Other</option>
                    </select>
                </div>
                <div>
                    <label>Date Of Birth</label>
                    <input name="dateOfBirth" type="date" required>
                </div>
                <div>
                    <label>Start Date</label>
                    <input name="startDate" type="date" required>
                </div>
                <div>
                    <label>Base Salary</label>
                    <input name="baseSalary" type="number" min="0" step="0.01" value="0" required>
                </div>
                <div>
                    <label>Status</label>
                    <select name="status" required>
                        <option value="Active">Active</option>
                        <option value="Inactive">Inactive</option>
                    </select>
                </div>
            </div>
            <div class="form-actions">
                <button type="submit">Save</button>
                <a class="button secondary" href="employee-list">Cancel</a>
            </div>
        </form>
    </main>
</div>
<script src="../js/employee.js?v=<%= application.getResource("/js/employee.js").openConnection().getLastModified() %>"></script>
</body>
</html>
