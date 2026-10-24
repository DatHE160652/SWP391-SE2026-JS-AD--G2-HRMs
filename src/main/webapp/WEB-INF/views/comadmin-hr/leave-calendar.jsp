<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lịch nghỉ phép Toàn công ty - HRMs Cloud</title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
    <link rel="stylesheet" href="../css/leave.css">
</head>
<body data-page="leave-calendar-company">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>TechNova JSC</span></div>
        <div class="nav-label">Phân hệ Quản trị</div>
        <a class="nav-link" href="../employee/employee-list">Quản lý Nhân sự</a>
        <a class="nav-link" href="../department/department-list">Quản lý Phòng ban</a>
        <a class="nav-link" href="../position/position-list">Quản lý Chức danh</a>

        <div class="nav-label" style="margin-top: 18px;">Phân hệ Chấm công</div>
        <a class="nav-link" href="../attendance/manager-attendance">Điểm danh Nhân sự</a>
        <a class="nav-link" href="../attendance/manager-personal-attendance">Chấm công Cá nhân</a>
        <a class="nav-link" href="../attendance/company-attendance">Giám sát Công ty</a>
        <a class="nav-link" href="../attendance/attendance-report">Báo cáo Chấm công</a>

        <div class="nav-label" style="margin-top: 18px;">Phân hệ Nghỉ phép</div>
        <a class="nav-link" href="types">Cấu hình loại phép</a>
        <a class="nav-link" href="all-requests">Tất cả đơn nghỉ</a>
        <a class="nav-link" href="pending-approvals">Duyệt đơn nghỉ phép</a>
        <a class="nav-link active" href="calendar">Lịch nghỉ công ty</a>
        <a class="nav-link" href="report">Báo cáo nghỉ phép</a>

        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title">Lịch nghỉ phép Toàn công ty</h1>
                <p class="page-subtitle">Theo dõi lịch nghỉ tập trung của các phòng ban để chủ động điều phối ca trực và nhân sự.</p>
            </div>
            <div class="header-actions">
                <div class="filter-group" style="min-width: 200px;">
                    <select id="deptFilter">
                        <option value="">-- Tất cả phòng ban --</option>
                    </select>
                </div>
            </div>
        </div>

        <!-- Interactive Calendar Component -->
        <div class="calendar-container">
            <div class="calendar-header">
                <div id="calendarMonthTitle" class="calendar-month-title">Tháng -- / ----</div>
                <div style="display: flex; gap: 8px;">
                    <button id="btnPrevMonth" class="calendar-nav-btn">&larr; Tháng trước</button>
                    <button id="btnCurrentMonth" class="calendar-nav-btn">Hôm nay</button>
                    <button id="btnNextMonth" class="calendar-nav-btn">Tháng sau &rarr;</button>
                </div>
            </div>

            <!-- Calendar Days of Week Header (Mon to Sun) -->
            <div class="calendar-grid-header">
                <div>Thứ 2</div>
                <div>Thứ 3</div>
                <div>Thứ 4</div>
                <div>Thứ 5</div>
                <div>Thứ 6</div>
                <div>Thứ 7</div>
                <div>Chủ Nhật</div>
            </div>

            <!-- Calendar Days Grid -->
            <div id="calendarDaysContainer" class="calendar-grid-days">
                <div style="grid-column: 1/-1; text-align: center; padding: 32px; background: #fff; color: #667085;">
                    Đang tải lịch...
                </div>
            </div>
        </div>
    </main>
</div>

<!-- Details Modal -->
<div id="modalDetail" class="modal-overlay">
    <div class="modal-box">
        <div class="modal-header">
            <h3 class="modal-title">Chi tiết lịch nghỉ nhân sự</h3>
            <button class="modal-close" onclick="LeaveApp.closeModal('modalDetail')">&times;</button>
        </div>
        <div class="modal-body" id="detailModalBody"></div>
        <div class="modal-footer">
            <button class="button secondary" onclick="LeaveApp.closeModal('modalDetail')">Đóng</button>
        </div>
    </div>
</div>

<script src="../js/leave.js?v=<%= application.getResource("/js/leave.js").openConnection().getLastModified() %>"></script>
<script>
    document.addEventListener("DOMContentLoaded", () => {
        LeaveApp.initCalendarView(false); // false = company-wide scope
    });
</script>
</body>
</html>
