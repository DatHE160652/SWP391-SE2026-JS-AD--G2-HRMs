<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Danh sách Chấm công - HRMs Cloud</title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
</head>
<body data-page="attendance-list">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>TechNova JSC</span></div>
        <div class="nav-label">Phân hệ Chấm công</div>
        <a class="nav-link" href="../employee/my-profile?nav=manager">Hồ sơ cá nhân</a>
        <a class="nav-link" href="manager-attendance">Điểm danh Nhân sự</a>
        <a class="nav-link active" href="manager-attendance?view=list">Danh sách chấm công</a>
        <a class="nav-link" href="manager-personal-attendance">Chấm công Cá nhân</a>
        <a class="nav-link" href="attendance-report">Báo cáo Chấm công</a>
        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">
        <!-- Top Breadcrumb matching Image 2 -->
        <div class="timesheet-breadcrumb" style="margin: 0 28px 12px;">
            <span class="crumb-link">NHÂN SỰ</span>
            <span class="crumb-sep">&rsaquo;</span>
            <span class="crumb-current">Chấm công</span>
        </div>

        <!-- Page Header with Month Navigation & Excel Export -->
        <div class="page-header" style="align-items: center; margin-bottom: 18px;">
            <div>
                <h1 class="page-title" style="font-size: 26px; font-weight: 800; color: #101828; margin: 0 0 4px;">Chấm công</h1>
                <div id="timesheetHeaderSubtitle" style="font-size: 14px; color: #475467;">Đang tải danh sách nhân sự...</div>
            </div>
            <div class="header-actions" style="display: flex; align-items: center; gap: 12px;">
                <div class="month-navigator">
                    <button class="month-nav-btn" id="btnPrevMonth" title="Tháng trước">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15 18 9 12 15 6"></polyline></svg>
                    </button>
                    <span class="month-badge-display" id="currentMonthBadge">T10/2026</span>
                    <button class="month-nav-btn" id="btnNextMonth" title="Tháng tiếp theo">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"></polyline></svg>
                    </button>
                </div>

                <button class="button btn-export-excel" id="btnExportTimesheetExcel">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" style="margin-right:4px;vertical-align:middle;">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                        <polyline points="7 10 12 15 17 10"></polyline>
                        <line x1="12" y1="15" x2="12" y2="3"></line>
                    </svg>
                    Xuất Excel
                </button>
            </div>
        </div>

        <div id="message" class="message" style="margin: 0 28px 16px;"></div>

        <!-- 2 Navigation Tabs: Bảng chấm công & Phân tích theo phòng ban -->
        <div style="padding: 0 28px;">
            <div class="timesheet-tabs-bar">
                <button class="timesheet-nav-tab active" id="tabGrid">Bảng chấm công</button>
                <button class="timesheet-nav-tab" id="tabDept">Phân tích theo phòng ban</button>
            </div>

            <!-- Filter Bar with Search, Department Select, and Legends -->
            <div class="timesheet-filter-card">
                <div class="timesheet-filters-left">
                    <div class="search-input-wrapper">
                        <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                        <input type="text" id="timesheetSearchInput" class="timesheet-search" placeholder="Tìm nhân viên...">
                    </div>

                    <select id="timesheetDeptSelect" class="timesheet-select">
                        <option value="">Tất cả phòng ban</option>
                    </select>
                </div>

                <div class="timesheet-legends">
                    <span class="legend-item"><span class="legend-dot dot-present"></span> Đi làm</span>
                    <span class="legend-item"><span class="legend-dot dot-late"></span> Đi trễ</span>
                    <span class="legend-item"><span class="legend-dot dot-early"></span> Về sớm</span>
                    <span class="legend-item"><span class="legend-dot dot-onleave"></span> Nghỉ phép</span>
                    <span class="legend-item"><span class="legend-dot dot-absent"></span> Vắng</span>
                </div>
            </div>

            <!-- TAB 1: Bảng chấm công Matrix Table (Matching Image 2) -->
            <div class="timesheet-panel" id="sectionGrid">
                <div class="timesheet-scroll-wrapper">
                    <table class="timesheet-matrix-table">
                        <thead>
                            <tr id="timesheetHeaderDays">
                                <th class="col-sticky-emp">NHÂN VIÊN</th>
                                <!-- Dynamic day columns 1..N will be injected here -->
                            </tr>
                        </thead>
                        <tbody id="timesheetRows">
                            <tr><td colspan="32" style="text-align:center; padding:36px; color:#667085;">Đang tải bảng chấm công...</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- TAB 2: Phân tích theo phòng ban -->
            <div class="panel" id="sectionDepartment" style="display:none; padding:0; overflow:hidden;">
                <table class="department-stats-table">
                    <thead>
                        <tr>
                            <th>Phòng ban</th>
                            <th style="text-align:center;">Số nhân sự</th>
                            <th style="text-align:center;">Tổng ngày công</th>
                            <th style="text-align:center; min-width:180px;">Tỷ lệ đúng giờ</th>
                            <th style="text-align:center;">Đi trễ</th>
                            <th style="text-align:center;">Về sớm</th>
                            <th style="text-align:center;">Nghỉ phép</th>
                            <th style="text-align:center;">Vắng mặt</th>
                        </tr>
                    </thead>
                    <tbody id="departmentStatsRows">
                        <tr><td colspan="8" style="text-align:center; padding:32px; color:#667085;">Đang tải dữ liệu thống kê...</td></tr>
                    </tbody>
                </table>
            </div>
        </div>
    </main>
</div>
<script src="../js/attendance.js?v=20261024_02"></script>
</body>
</html>
