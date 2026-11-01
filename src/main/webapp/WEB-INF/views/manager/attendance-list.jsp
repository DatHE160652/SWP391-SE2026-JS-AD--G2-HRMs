<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Danh sách Chấm công - HRMs Cloud</title>
    <link rel="stylesheet" href="../css/employee.css?v=20261101_05">
    <link rel="stylesheet" href="../css/attendance.css?v=20261101_05">
</head>
<body data-page="attendance-list">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span><c:out value="${not empty sessionScope.tenantPrincipal.companyName ? sessionScope.tenantPrincipal.companyName : 'TechNova JSC'}"/></span></div>
        <div class="nav-label">Phân hệ Chấm công</div>
        <a class="nav-link" href="../employee/my-profile?nav=manager">Hồ sơ cá nhân</a>
        <a class="nav-link" href="manager-attendance">Điểm danh Nhân sự</a>
        <a class="nav-link active" href="manager-attendance?view=list">Danh sách chấm công</a>
        <a class="nav-link" href="manager-personal-attendance">Chấm công Cá nhân</a>
        <a class="nav-link" href="attendance-report">Báo cáo Chấm công</a>
        <a class="nav-link" href="${pageContext.request.contextPath}/payroll">Quản lý lương</a>
        <a class="nav-link" href="${pageContext.request.contextPath}/payslips">Phiếu lương của tôi</a>
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
            <div class="header-actions" style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap;">
                <!-- Modern Month/Year Selector & Navigator (UC36 / UC83 / UC88) -->
                <div class="month-navigator" style="display: inline-flex; align-items: center; gap: 6px; background: #ffffff; border: 1px solid #d0d5dd; border-radius: 8px; padding: 4px 8px; box-shadow: 0 1px 2px rgba(16, 24, 40, 0.05);">
                    <button type="button" class="month-nav-btn" id="btnPrevMonth" title="Tháng trước" style="display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; min-width: 32px; border: 1px solid #d0d5dd; background: #f9fafb; border-radius: 6px; color: #344054; cursor: pointer; font-size: 18px; font-weight: 800; line-height: 1; padding: 0;">
                        &#8249;
                    </button>
                    <div class="month-select-wrapper" style="display: inline-flex; align-items: center; gap: 6px;">
                        <select id="timesheetMonthSelect" class="timesheet-period-select" title="Chọn tháng xem chấm công" style="width: 125px; min-width: 125px; height: 34px; font-weight: 700; font-size: 14px; color: #101828; border: 1px solid #d0d5dd; background: #ffffff; padding: 4px 8px; border-radius: 6px; cursor: pointer; outline: none; box-sizing: border-box;">
                            <option value="1">Tháng 01</option>
                            <option value="2">Tháng 02</option>
                            <option value="3">Tháng 03</option>
                            <option value="4">Tháng 04</option>
                            <option value="5">Tháng 05</option>
                            <option value="6">Tháng 06</option>
                            <option value="7">Tháng 07</option>
                            <option value="8">Tháng 08</option>
                            <option value="9">Tháng 09</option>
                            <option value="10">Tháng 10</option>
                            <option value="11">Tháng 11</option>
                            <option value="12">Tháng 12</option>
                        </select>
                        <span style="color: #98a2b3; font-weight: 700; font-size: 15px;">/</span>
                        <select id="timesheetYearSelect" class="timesheet-period-select" title="Chọn năm" style="width: 90px; min-width: 90px; height: 34px; font-weight: 700; font-size: 14px; color: #101828; border: 1px solid #d0d5dd; background: #ffffff; padding: 4px 8px; border-radius: 6px; cursor: pointer; outline: none; box-sizing: border-box;">
                            <option value="2024">2024</option>
                            <option value="2025">2025</option>
                            <option value="2026">2026</option>
                            <option value="2027">2027</option>
                        </select>
                    </div>
                    <button type="button" class="month-nav-btn" id="btnNextMonth" title="Tháng tiếp theo" style="display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; min-width: 32px; border: 1px solid #d0d5dd; background: #f9fafb; border-radius: 6px; color: #344054; cursor: pointer; font-size: 18px; font-weight: 800; line-height: 1; padding: 0;">
                        &#8250;
                    </button>
                    <span class="month-badge-display" id="currentMonthBadge" style="display:none;">T10/2026</span>
                </div>

                <!-- Quick Button to view Previous Month worked -->
                <button type="button" class="button" id="btnTimesheetQuickPrev" title="Xem nhanh tháng làm việc trước (Tháng 10/2026)" style="background: #f8f9fc; border: 1px solid #d0d5dd; color: #344054; font-size: 13px; font-weight: 600; padding: 8px 12px; border-radius: 8px; cursor: pointer; display: inline-flex; align-items: center; gap: 4px; height: 42px;">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><polyline points="11 17 6 12 11 7"></polyline><polyline points="18 17 13 12 18 7"></polyline></svg>
                    Tháng trước
                </button>

                <button class="button btn-export-excel" id="btnExportTimesheetExcel">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" style="margin-right:4px;vertical-align:middle;">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                        <polyline points="7 10 12 15 17 10"></polyline>
                        <line x1="12" y1="15" x2="12" y2="3"></line>
                    </svg>
                    Xuất Excel
                </button>
                <span id="lockStatusBadge" style="display:none; align-items:center; gap:4px; padding:6px 12px; border-radius:6px; font-size:13px; font-weight:700;"></span>
                <button class="button" id="btnToggleTimesheetLock" style="display:none; align-items:center; gap:6px; font-weight:600; padding:8px 14px; border-radius:8px;">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                        <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                    </svg>
                    <span id="btnLockText">Khóa công tháng</span>
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
<script src="../js/attendance.js?v=20261101_05"></script>
</body>
</html>
