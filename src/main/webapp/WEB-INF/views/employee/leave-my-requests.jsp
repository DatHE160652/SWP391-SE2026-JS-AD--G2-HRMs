<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lịch sử đơn nghỉ phép - HRMs Cloud</title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
    <link rel="stylesheet" href="../css/leave.css">
</head>
<body data-page="leave-my-requests">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>Employee Portal</span></div>
        <div class="nav-label">Chức năng cá nhân</div>
        <a class="nav-link" href="../employee/my-profile">Hồ sơ cá nhân</a>
        <a class="nav-link" href="../attendance/employee-attendance">Chấm công cá nhân</a>

        <div class="nav-label" style="margin-top: 18px;">Nghỉ phép</div>
        <a class="nav-link" href="my-balances">Số dư phép</a>
        <a class="nav-link" href="submit">Tạo đơn xin nghỉ</a>
        <a class="nav-link active" href="my-requests">Lịch sử đơn nghỉ</a>
        <a class="nav-link" href="calendar">Lịch nghỉ cá nhân</a>

        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title">Lịch sử đơn xin nghỉ phép</h1>
                <p class="page-subtitle">Theo dõi tiến độ duyệt đơn, phản hồi của cấp trên và lịch sử các đợt nghỉ đã đăng ký.</p>
            </div>
            <div class="header-actions">
                <a href="submit" class="button" style="text-decoration: none;">+ Tạo đơn mới</a>
            </div>
        </div>

        <!-- Filter Bar -->
        <section class="panel" style="margin-bottom: 20px;">
            <div class="filter-bar" style="margin-bottom: 0; background: none; border: none; padding: 0;">
                <div class="filter-group" style="min-width: 160px;">
                    <label for="statusFilter">Trạng thái duyệt:</label>
                    <select id="statusFilter">
                        <option value="ALL">-- Tất cả trạng thái --</option>
                        <option value="Pending">Chờ duyệt (Pending)</option>
                        <option value="Approved">Đã duyệt (Approved)</option>
                        <option value="Rejected">Bị từ chối (Rejected)</option>
                        <option value="Cancelled">Đã hủy (Cancelled)</option>
                    </select>
                </div>
                <div class="filter-group" style="min-width: 120px;">
                    <label for="yearFilter">Năm:</label>
                    <select id="yearFilter">
                        <option value="">-- Tất cả các năm --</option>
                        <option value="2025">2025</option>
                        <option value="2026" selected>2026</option>
                        <option value="2027">2027</option>
                    </select>
                </div>
            </div>
        </section>

        <!-- Requests Table -->
        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th style="width: 70px;">Mã đơn</th>
                    <th>Loại nghỉ phép</th>
                    <th>Khoảng thời gian</th>
                    <th style="width: 100px;">Số ngày</th>
                    <th>Lý do</th>
                    <th style="width: 120px;">Trạng thái</th>
                    <th>Phản hồi người duyệt</th>
                    <th style="width: 130px; text-align: center;">Thao tác</th>
                </tr>
                </thead>
                <tbody id="requestsTableBody">
                    <tr><td colspan="8" style="text-align: center; color: #667085; padding: 24px;">Đang tải danh sách...</td></tr>
                </tbody>
            </table>

            <div id="paginationBar" class="pagination-bar"></div>
        </section>
    </main>
</div>

<!-- Details Modal -->
<div id="modalDetail" class="modal-overlay">
    <div class="modal-box">
        <div class="modal-header">
            <h3 class="modal-title">Chi tiết đơn xin nghỉ phép</h3>
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
        LeaveApp.initMyRequestsView();
    });
</script>
</body>
</html>
