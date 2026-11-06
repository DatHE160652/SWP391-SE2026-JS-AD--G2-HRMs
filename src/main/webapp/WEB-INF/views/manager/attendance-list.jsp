<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bảng chấm công - HRMs Cloud</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/attendance.css?v=<%= application.getResource("/css/attendance.css").openConnection().getLastModified() %>">
</head>
<body class="lc" data-page="attendance-list">
<c:set var="managerNavPage" value="attendance-list"/>
<c:set var="managerNavTitle" value="Bảng chấm công"/>
<%@ include file="/WEB-INF/views/common/manager-shell.jspf" %>

    <main class="lc-main content">
        <div class="lc-breadcrumb">Cổng quản lý &nbsp; / &nbsp; Bảng chấm công</div>

        <!-- Page Header with Month Navigation & Excel Export -->
        <div class="page-header" style="align-items: center; margin-bottom: 18px;">
            <div>
                <h1 class="page-title">Bảng chấm công</h1>
                <div id="timesheetHeaderSubtitle" class="page-subtitle" style="margin-top: 4px;">Đang tải danh sách nhân sự...</div>
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
                <button type="button" class="button secondary" id="btnTimesheetQuickPrev" title="Xem nhanh tháng làm việc trước" style="font-size: 13px; font-weight: 600; padding: 8px 12px; border-radius: 8px; cursor: pointer; display: inline-flex; align-items: center; gap: 4px; height: 42px;">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><polyline points="11 17 6 12 11 7"></polyline><polyline points="18 17 13 12 18 7"></polyline></svg>
                    Tháng trước
                </button>

                <button class="button btn-export-excel" id="btnExportTimesheetExcel" type="button" style="background:#027a48; border-color:#027a48; color:#fff; display:inline-flex; align-items:center; gap:6px;">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                        <polyline points="7 10 12 15 17 10"></polyline>
                        <line x1="12" y1="15" x2="12" y2="3"></line>
                    </svg>
                    Xuất Excel
                </button>
                <span id="lockStatusBadge" style="display:none; align-items:center; gap:4px; padding:6px 12px; border-radius:6px; font-size:13px; font-weight:700;"></span>
            </div>
        </div>

        <div id="message" class="message"></div>

        <!-- 2 Navigation Tabs: Bảng chấm công & Phân tích theo phòng ban -->
        <div class="timesheet-tabs-bar">
            <button class="timesheet-nav-tab active" id="tabGrid" type="button">Bảng chấm công</button>
            <button class="timesheet-nav-tab" id="tabDept" type="button">Phân tích theo phòng ban</button>
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

        <!-- TAB 1: Bảng chấm công Matrix Table -->
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
    </main>

<script src="../js/attendance.js?v=<%= application.getResource("/js/attendance.js").openConnection().getLastModified() %>"></script>
</body>
</html>
