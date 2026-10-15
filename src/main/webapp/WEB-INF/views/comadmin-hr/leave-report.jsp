<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Báo cáo sử dụng nghỉ phép - HRMs Cloud</title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
    <link rel="stylesheet" href="../css/leave.css">
</head>
<body data-page="leave-report">
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
        <a class="nav-link" href="calendar">Lịch nghỉ công ty</a>
        <a class="nav-link active" href="report">Báo cáo nghỉ phép</a>

        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title">Báo cáo tình hình sử dụng nghỉ phép</h1>
                <p class="page-subtitle">Tổng hợp tỷ lệ sử dụng ngày phép, hạn mức còn lại và phân loại theo chính sách tính lương của nhân sự toàn công ty.</p>
            </div>
            <div class="header-actions">
                <button id="btnExportExcel" class="button" style="background:#027a48;border-color:#027a48;">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:6px;vertical-align:middle;">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                        <polyline points="7 10 12 15 17 10"></polyline>
                        <line x1="12" y1="15" x2="12" y2="3"></line>
                    </svg>
                    Xuất file Excel (.xls)
                </button>
            </div>
        </div>

        <!-- Filter Bar -->
        <section class="panel" style="margin-bottom: 20px;">
            <div class="filter-bar" style="margin-bottom: 0; background: none; border: none; padding: 0;">
                <div class="filter-group" style="min-width: 140px;">
                    <label for="reportYearSelect">Năm báo cáo:</label>
                    <select id="reportYearSelect">
                        <option value="2025">2025</option>
                        <option value="2026" selected>2026</option>
                        <option value="2027">2027</option>
                    </select>
                </div>
                <div class="filter-group" style="min-width: 200px;">
                    <label for="reportDeptFilter">Phòng ban:</label>
                    <select id="reportDeptFilter">
                        <option value="">-- Tất cả phòng ban --</option>
                    </select>
                </div>
            </div>
        </section>

        <!-- Report Table -->
        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th style="width: 45px; text-align: center;">STT</th>
                    <th style="width: 85px;">Mã NV</th>
                    <th>Họ và tên</th>
                    <th>Phòng ban</th>
                    <th>Chức danh</th>
                    <th style="width: 90px; text-align: right;">Được cấp</th>
                    <th style="width: 90px; text-align: right;">Đã dùng</th>
                    <th style="width: 90px; text-align: right;">Còn lại</th>
                    <th style="width: 95px; text-align: right;">Có lương</th>
                    <th style="width: 95px; text-align: right;">Không lương</th>
                    <th style="width: 75px; text-align: center;">Tổng đơn</th>
                    <th style="width: 75px; text-align: center;">Đã duyệt</th>
                </tr>
                </thead>
                <tbody id="reportTableBody">
                    <tr><td colspan="12" style="text-align: center; color: #667085; padding: 24px;">Đang tải báo cáo...</td></tr>
                </tbody>
            </table>
        </section>
    </main>
</div>

<script src="../js/leave.js"></script>
<script>
    document.addEventListener("DOMContentLoaded", () => {
        LeaveApp.initReportView();
    });
</script>
</body>
</html>
