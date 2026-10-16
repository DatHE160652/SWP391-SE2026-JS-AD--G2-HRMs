<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Số dư ngày phép - HRMs Cloud</title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
    <link rel="stylesheet" href="../css/leave.css">
</head>
<body data-page="leave-balance">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>Employee Portal</span></div>
        <div class="nav-label">Chức năng cá nhân</div>
        <a class="nav-link" href="../employee/my-profile">Hồ sơ cá nhân</a>
        <a class="nav-link" href="../attendance/employee-attendance">Chấm công cá nhân</a>

        <div class="nav-label" style="margin-top: 18px;">Nghỉ phép</div>
        <a class="nav-link active" href="my-balances">Số dư phép</a>
        <a class="nav-link" href="submit">Tạo đơn xin nghỉ</a>
        <a class="nav-link" href="my-requests">Lịch sử đơn nghỉ</a>
        <a class="nav-link" href="calendar">Lịch nghỉ cá nhân</a>

        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title">Hạn mức & Số dư ngày phép</h1>
                <p class="page-subtitle">Theo dõi số ngày nghỉ được cấp, số ngày đã sử dụng và quỹ ngày phép còn lại theo từng năm.</p>
            </div>
            <div class="header-actions">
                <a href="submit" class="button" style="text-decoration: none;">+ Tạo đơn xin nghỉ mới</a>
            </div>
        </div>

        <section class="panel" style="margin-bottom: 24px;">
            <div class="filter-bar" style="margin-bottom: 0; background: none; border: none; padding: 0;">
                <div class="filter-group">
                    <label for="yearSelect">Năm theo dõi:</label>
                    <select id="yearSelect">
                        <option value="2025">2025</option>
                        <option value="2026" selected>2026</option>
                        <option value="2027">2027</option>
                    </select>
                </div>
            </div>
        </section>

        <!-- Balance Quota Cards Container -->
        <section id="balanceGridContainer" class="balance-grid">
            <div style="grid-column: 1/-1; text-align: center; color: #667085; padding: 32px;">Đang tải số dư ngày phép...</div>
        </section>

        <!-- Quick Guideline Card -->
        <div class="info-banner" style="margin: 0 0 24px;">
            <svg width="20" height="20" fill="currentColor" viewBox="0 0 20 20" style="flex-shrink:0;">
                <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"></path>
            </svg>
            <div>
                <strong>Quy định tính ngày nghỉ:</strong> Số ngày nghỉ thực tế chỉ tính trên các ngày làm việc tiêu chuẩn (từ Thứ 2 đến Thứ 6). Các ngày Thứ 7, Chủ Nhật không bị khấu trừ vào số dư phép. Khi nộp đơn, hệ thống sẽ tự động kiểm tra quỹ ngày còn lại.
            </div>
        </div>
    </main>
</div>

<script src="../js/leave.js"></script>
<script>
    document.addEventListener("DOMContentLoaded", () => {
        LeaveApp.initBalancesView();
    });
</script>
</body>
</html>
