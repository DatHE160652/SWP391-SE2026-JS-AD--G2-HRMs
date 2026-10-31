<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Giám sát Chấm công Toàn Công ty - HRMs Cloud</title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
</head>
<body data-page="company-attendance">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>TechNova JSC</span></div>
        <div class="nav-label">Phân hệ Quản trị</div>
        <a class="nav-link" href="../employee/employee-list">Quản lý Nhân sự</a>
        <a class="nav-link" href="../department/department-list">Quản lý Phòng ban</a>
        <a class="nav-link" href="../position/position-list">Quản lý Chức danh</a>

        <div class="nav-label" style="margin-top: 18px;">Phân hệ Chấm công</div>
        <a class="nav-link" href="manager-attendance">Điểm danh Nhân sự</a>
        <a class="nav-link" href="manager-personal-attendance">Chấm công Cá nhân</a>
        <a class="nav-link active" href="company-attendance">Giám sát Công ty</a>
        <a class="nav-link" href="attendance-report">Báo cáo Chấm công</a>

        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
        <a class="nav-link" href="../employee/my-profile">Hồ sơ cá nhân</a>
    </aside>

    <main class="content">

        <div class="page-header">
            <div>
                <h1 class="page-title">Giám sát Chấm công Toàn Công ty</h1>
            </div>
        </div>

        <!-- Filter Bar -->
        <section class="panel" style="margin-bottom: 20px;">
            <div class="filter-bar" style="margin-bottom: 0; background: none; border: none; padding: 0;">
                <div class="filter-group" style="min-width: 180px;">
                    <label for="deptFilter">Phòng ban:</label>
                    <select id="deptFilter"></select>
                </div>
                <div class="filter-group" style="min-width: 140px;">
                    <label for="statusFilter">Trạng thái:</label>
                    <select id="statusFilter">
                        <option value="ALL">-- Tất cả trạng thái --</option>
                        <option value="Present">Có mặt</option>
                        <option value="Late">Đi muộn</option>
                        <option value="Early Leave">Về sớm</option>
                        <option value="Absent">Vắng mặt</option>
                        <option value="On Leave">On Leave (Nghỉ phép)</option>
                    </select>
                </div>
                <div class="filter-group">
                    <label for="fromDate">Từ ngày:</label>
                    <input type="date" id="fromDate">
                </div>
                <div class="filter-group">
                    <label for="toDate">Đến ngày:</label>
                    <input type="date" id="toDate">
                </div>
                <div class="filter-group" style="min-width: 200px;">
                    <label for="keyword">Từ khóa tìm kiếm:</label>
                    <input type="text" id="keyword" placeholder="Mã NV hoặc Họ tên...">
                </div>
                <div class="filter-actions">
                    <button class="button" id="btnSearch">Tìm kiếm</button>
                    <button class="button secondary" id="btnReset">Đặt lại</button>
                </div>
            </div>
        </section>

        <!-- Summary Grid -->
        <section class="summary-grid" style="grid-template-columns: repeat(1, minmax(200px, 260px));">
            <div class="summary-card">
                <div class="summary-label">Tổng bản ghi tìm thấy</div>
                <div id="totalRecords" class="summary-number text-success">0</div>
            </div>
        </section>

        <div id="message" class="message" style="margin: 0 28px 16px;"></div>

        <!-- Attendance Table -->
        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th>Mã NV</th>
                    <th>Họ và Tên</th>
                    <th>Phòng ban</th>
                    <th>Ngày</th>
                    <th>Vào ca</th>
                    <th>Tan ca</th>
                    <th>Tổng giờ</th>
                    <th>Trạng thái</th>
                    <th>Ghi chú</th>
                </tr>
                </thead>
                <tbody id="companyAttendanceRows">
                    <tr><td colspan="9" style="text-align:center;color:#667085;padding:24px;">Đang tải dữ liệu chấm công...</td></tr>
                </tbody>
            </table>
        </section>
    </main>
</div>
<script src="../js/attendance.js?v=<%= application.getResource("/js/attendance.js").openConnection().getLastModified() %>"></script>
</body>
</html>
