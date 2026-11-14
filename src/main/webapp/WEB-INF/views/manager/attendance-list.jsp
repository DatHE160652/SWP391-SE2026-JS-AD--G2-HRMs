<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_193f5f95b418", "Bảng chấm công - HRMs Cloud") %></title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/attendance.css?v=<%= application.getResource("/css/attendance.css").openConnection().getLastModified() %>">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc" data-page="attendance-list">
<c:set var="managerNavPage" value="attendance-list"/>
<c:set var="managerNavTitle" value="Bảng chấm công"/>
<%@ include file="/WEB-INF/views/common/manager-shell.jspf" %>

    <main class="lc-main content">
        <div class="lc-breadcrumb"><%= com.hrms.i18n.Language.text(request, "ui_f3139a92e13e", "Cổng quản lý &nbsp; / &nbsp; Bảng chấm công") %></div>

        <!-- Page Header with Month Navigation & Excel Export -->
        <div class="page-header" style="align-items: center; margin-bottom: 18px;">
            <div>
                <h1 class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_b4d20486e1c3", "Bảng chấm công") %></h1>
                <div id="timesheetHeaderSubtitle" class="page-subtitle" style="margin-top: 4px;"><%= com.hrms.i18n.Language.text(request, "ui_f0bffa82d5ff", "Đang tải danh sách nhân sự...") %></div>
            </div>
            <div class="header-actions" style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap;">
                <!-- Modern Month/Year Selector & Navigator (UC36 / UC83 / UC88) -->
                <div class="month-navigator" style="display: inline-flex; align-items: center; gap: 6px; background: #ffffff; border: 1px solid #d0d5dd; border-radius: 8px; padding: 4px 8px; box-shadow: 0 1px 2px rgba(16, 24, 40, 0.05);">
                    <button type="button" class="month-nav-btn" id="btnPrevMonth" title="<%= com.hrms.i18n.Language.text(request, "ui_0cfd503bec52", "Tháng trước") %>" style="display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; min-width: 32px; border: 1px solid #d0d5dd; background: #f9fafb; border-radius: 6px; color: #344054; cursor: pointer; font-size: 18px; font-weight: 800; line-height: 1; padding: 0;">
                        &#8249;
                    </button>
                    <div class="month-select-wrapper" style="display: inline-flex; align-items: center; gap: 6px;">
                        <select id="timesheetMonthSelect" class="timesheet-period-select" title="<%= com.hrms.i18n.Language.text(request, "ui_5abb0b42265c", "Chọn tháng xem chấm công") %>" style="width: 125px; min-width: 125px; height: 34px; font-weight: 700; font-size: 14px; color: #101828; border: 1px solid #d0d5dd; background: #ffffff; padding: 4px 8px; border-radius: 6px; cursor: pointer; outline: none; box-sizing: border-box;">
                            <option value="1"><%= com.hrms.i18n.Language.text(request, "ui_b3f5dfcfeb77", "Tháng 01") %></option>
                            <option value="2"><%= com.hrms.i18n.Language.text(request, "ui_f5f8b83a33d5", "Tháng 02") %></option>
                            <option value="3"><%= com.hrms.i18n.Language.text(request, "ui_db3f5827fb6d", "Tháng 03") %></option>
                            <option value="4"><%= com.hrms.i18n.Language.text(request, "ui_4318e3f2987d", "Tháng 04") %></option>
                            <option value="5"><%= com.hrms.i18n.Language.text(request, "ui_89ed4194b292", "Tháng 05") %></option>
                            <option value="6"><%= com.hrms.i18n.Language.text(request, "ui_a3bd71d699ce", "Tháng 06") %></option>
                            <option value="7"><%= com.hrms.i18n.Language.text(request, "ui_8d46817cecac", "Tháng 07") %></option>
                            <option value="8"><%= com.hrms.i18n.Language.text(request, "ui_5dc73aeecd7c", "Tháng 08") %></option>
                            <option value="9"><%= com.hrms.i18n.Language.text(request, "ui_758bea7142cc", "Tháng 09") %></option>
                            <option value="10"><%= com.hrms.i18n.Language.text(request, "ui_1293573088a5", "Tháng 10") %></option>
                            <option value="11"><%= com.hrms.i18n.Language.text(request, "ui_e2e2ffeea726", "Tháng 11") %></option>
                            <option value="12"><%= com.hrms.i18n.Language.text(request, "ui_59de2e03dd04", "Tháng 12") %></option>
                        </select>
                        <span style="color: #98a2b3; font-weight: 700; font-size: 15px;">/</span>
                        <select id="timesheetYearSelect" class="timesheet-period-select" title="<%= com.hrms.i18n.Language.text(request, "ui_67337229dff7", "Chọn năm") %>" style="width: 90px; min-width: 90px; height: 34px; font-weight: 700; font-size: 14px; color: #101828; border: 1px solid #d0d5dd; background: #ffffff; padding: 4px 8px; border-radius: 6px; cursor: pointer; outline: none; box-sizing: border-box;">
                            <option value="2024">2024</option>
                            <option value="2025">2025</option>
                            <option value="2026">2026</option>
                            <option value="2027">2027</option>
                        </select>
                    </div>
                    <button type="button" class="month-nav-btn" id="btnNextMonth" title="<%= com.hrms.i18n.Language.text(request, "ui_8b4549577c2d", "Tháng tiếp theo") %>" style="display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; min-width: 32px; border: 1px solid #d0d5dd; background: #f9fafb; border-radius: 6px; color: #344054; cursor: pointer; font-size: 18px; font-weight: 800; line-height: 1; padding: 0;">
                        &#8250;
                    </button>
                    <span class="month-badge-display" id="currentMonthBadge" style="display:none;">T10/2026</span>
                </div>

                <!-- Quick Button to view Previous Month worked -->
                <button type="button" class="button secondary" id="btnTimesheetQuickPrev" title="<%= com.hrms.i18n.Language.text(request, "ui_3d681467c8af", "Xem nhanh tháng làm việc trước") %>" style="font-size: 13px; font-weight: 600; padding: 8px 12px; border-radius: 8px; cursor: pointer; display: inline-flex; align-items: center; gap: 4px; height: 42px;">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><polyline points="11 17 6 12 11 7"></polyline><polyline points="18 17 13 12 18 7"></polyline></svg>
                    <%= com.hrms.i18n.Language.text(request, "ui_0cfd503bec52", "Tháng trước") %>
                </button>

                <button class="button btn-export-excel" id="btnExportTimesheetExcel" type="button" style="background:#027a48; border-color:#027a48; color:#fff; display:inline-flex; align-items:center; gap:6px;">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                        <polyline points="7 10 12 15 17 10"></polyline>
                        <line x1="12" y1="15" x2="12" y2="3"></line>
                    </svg>
                    <%= com.hrms.i18n.Language.text(request, "ui_1721265dcbc2", "Xuất Excel") %>
                </button>
                <span id="lockStatusBadge" style="display:none; align-items:center; gap:4px; padding:6px 12px; border-radius:6px; font-size:13px; font-weight:700;"></span>
            </div>
        </div>

        <div id="message" class="message"></div>

        <!-- 2 Navigation Tabs: Bảng chấm công & Phân tích theo phòng ban -->
        <div class="timesheet-tabs-bar">
            <button class="timesheet-nav-tab active" id="tabGrid" type="button"><%= com.hrms.i18n.Language.text(request, "ui_b4d20486e1c3", "Bảng chấm công") %></button>
            <button class="timesheet-nav-tab" id="tabDept" type="button"><%= com.hrms.i18n.Language.text(request, "ui_795f13d743e4", "Phân tích theo phòng ban") %></button>
        </div>

        <!-- Filter Bar with Search, Department Select, and Legends -->
        <div class="timesheet-filter-card">
            <div class="timesheet-filters-left">
                <div class="search-input-wrapper">
                    <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                    <input type="text" id="timesheetSearchInput" class="timesheet-search" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_588f0688a716", "Tìm nhân viên...") %>">
                </div>

                <select id="timesheetDeptSelect" class="timesheet-select">
                    <option value=""><%= com.hrms.i18n.Language.text(request, "ui_1f6697b3badc", "Tất cả phòng ban") %></option>
                </select>
            </div>

            <div class="timesheet-legends">
                <span class="legend-item"><span class="legend-dot dot-present"></span> <%= com.hrms.i18n.Language.text(request, "ui_ce231f767048", "Đi làm") %></span>
                <span class="legend-item"><span class="legend-dot dot-late"></span> <%= com.hrms.i18n.Language.text(request, "ui_227a2bff02a9", "Đi trễ") %></span>
                <span class="legend-item"><span class="legend-dot dot-early"></span> <%= com.hrms.i18n.Language.text(request, "ui_fc9579195fc6", "Về sớm") %></span>
                <span class="legend-item"><span class="legend-dot dot-onleave"></span> <%= com.hrms.i18n.Language.text(request, "ui_9d21ce1caf72", "Nghỉ phép") %></span>
                <span class="legend-item"><span class="legend-dot dot-absent"></span> <%= com.hrms.i18n.Language.text(request, "ui_a159e7ec7c91", "Vắng") %></span>
            </div>
        </div>

        <!-- TAB 1: Bảng chấm công Matrix Table -->
        <div class="timesheet-panel" id="sectionGrid">
            <div class="timesheet-scroll-wrapper">
                <table class="timesheet-matrix-table">
                    <thead>
                        <tr id="timesheetHeaderDays">
                            <th class="col-sticky-emp"><%= com.hrms.i18n.Language.text(request, "ui_20030fd51f7c", "NHÂN VIÊN") %></th>
                            <!-- Dynamic day columns 1..N will be injected here -->
                        </tr>
                    </thead>
                    <tbody id="timesheetRows">
                        <tr><td colspan="32" style="text-align:center; padding:36px; color:#667085;"><%= com.hrms.i18n.Language.text(request, "ui_be6448be219b", "Đang tải bảng chấm công...") %></td></tr>
                    </tbody>
                </table>
            </div>
        </div>

        <!-- TAB 2: Phân tích theo phòng ban -->
        <div class="panel" id="sectionDepartment" style="display:none; padding:0; overflow:hidden;">
            <table class="department-stats-table">
                <thead>
                    <tr>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_6f358f848e2e", "Phòng ban") %></th>
                        <th style="text-align:center;"><%= com.hrms.i18n.Language.text(request, "ui_ddca6f455ed4", "Số nhân sự") %></th>
                        <th style="text-align:center;"><%= com.hrms.i18n.Language.text(request, "ui_f775f7bbaf1b", "Tổng ngày công") %></th>
                        <th style="text-align:center; min-width:180px;"><%= com.hrms.i18n.Language.text(request, "ui_1d6a90db155c", "Tỷ lệ đúng giờ") %></th>
                        <th style="text-align:center;"><%= com.hrms.i18n.Language.text(request, "ui_227a2bff02a9", "Đi trễ") %></th>
                        <th style="text-align:center;"><%= com.hrms.i18n.Language.text(request, "ui_fc9579195fc6", "Về sớm") %></th>
                        <th style="text-align:center;"><%= com.hrms.i18n.Language.text(request, "ui_9d21ce1caf72", "Nghỉ phép") %></th>
                        <th style="text-align:center;"><%= com.hrms.i18n.Language.text(request, "ui_833a955ae662", "Vắng mặt") %></th>
                    </tr>
                </thead>
                <tbody id="departmentStatsRows">
                    <tr><td colspan="8" style="text-align:center; padding:32px; color:#667085;"><%= com.hrms.i18n.Language.text(request, "ui_65f42031743f", "Đang tải dữ liệu thống kê...") %></td></tr>
                </tbody>
            </table>
        </div>
    </main>

<script src="../js/attendance.js?v=<%= application.getResource("/js/attendance.js").openConnection().getLastModified() %>"></script>
</body>
</html>
