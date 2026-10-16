<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lịch nghỉ cá nhân - HRMs Cloud</title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
    <link rel="stylesheet" href="../css/leave.css">
</head>
<body data-page="leave-calendar-employee">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>Employee Portal</span></div>
        <div class="nav-label">Chức năng cá nhân</div>
        <a class="nav-link" href="../employee/my-profile">Hồ sơ cá nhân</a>
        <a class="nav-link" href="../attendance/employee-attendance">Chấm công cá nhân</a>

        <div class="nav-label" style="margin-top: 18px;">Nghỉ phép</div>
        <a class="nav-link" href="my-balances">Số dư phép</a>
        <a class="nav-link" href="submit">Tạo đơn xin nghỉ</a>
        <a class="nav-link" href="my-requests">Lịch sử đơn nghỉ</a>
        <a class="nav-link active" href="calendar">Lịch nghỉ cá nhân</a>

        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title">Lịch nghỉ phép cá nhân</h1>
                <p class="page-subtitle">Xem trực quan các đợt nghỉ phép đã được phê duyệt trong tháng theo dạng lịch biểu.</p>
            </div>
            <div class="header-actions">
                <a href="submit" class="button" style="text-decoration: none;">+ Tạo đơn mới</a>
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
            <h3 class="modal-title">Chi tiết lịch nghỉ</h3>
            <button class="modal-close" onclick="LeaveApp.closeModal('modalDetail')">&times;</button>
        </div>
        <div class="modal-body" id="detailModalBody"></div>
        <div class="modal-footer">
            <button class="button secondary" onclick="LeaveApp.closeModal('modalDetail')">Đóng</button>
        </div>
    </div>
</div>

<script src="../js/leave.js"></script>
<script>
    document.addEventListener("DOMContentLoaded", () => {
        LeaveApp.initCalendarView(true); // true = employee personal scope only
    });
</script>
</body>
</html>
