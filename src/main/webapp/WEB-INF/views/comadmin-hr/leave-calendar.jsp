<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_7ca24fa7e261", "Lịch nghỉ phép Toàn công ty - HRMs Cloud") %></title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
    <link rel="stylesheet" href="../css/leave.css">
</head>
<body data-page="leave-calendar-company">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>TechNova JSC</span></div>
        <div class="nav-label"><%= com.hrms.i18n.Language.text(request, "ui_0dec7fd51c40", "Phân hệ Quản trị") %></div>
        <a class="nav-link" href="../employee/employee-list"><%= com.hrms.i18n.Language.text(request, "ui_5ed96e7249d6", "Quản lý Nhân sự") %></a>
        <a class="nav-link" href="../department/department-list"><%= com.hrms.i18n.Language.text(request, "ui_bc217c0a7c21", "Quản lý Phòng ban") %></a>
        <a class="nav-link" href="../position/position-list"><%= com.hrms.i18n.Language.text(request, "ui_45243efa4b09", "Quản lý Chức danh") %></a>

        <div class="nav-label" style="margin-top: 18px;"><%= com.hrms.i18n.Language.text(request, "ui_bb8e1c6f085e", "Phân hệ Chấm công") %></div>
        <a class="nav-link" href="../attendance/manager-attendance"><%= com.hrms.i18n.Language.text(request, "ui_cc34d4e67fde", "Điểm danh Nhân sự") %></a>
        <a class="nav-link" href="../attendance/manager-personal-attendance"><%= com.hrms.i18n.Language.text(request, "ui_e846c0844ceb", "Chấm công Cá nhân") %></a>
        <a class="nav-link" href="../attendance/company-attendance"><%= com.hrms.i18n.Language.text(request, "ui_6b91b52ab71d", "Giám sát Công ty") %></a>
        <a class="nav-link" href="../attendance/attendance-report"><%= com.hrms.i18n.Language.text(request, "ui_5cfe0baad77f", "Báo cáo Chấm công") %></a>

        <div class="nav-label" style="margin-top: 18px;"><%= com.hrms.i18n.Language.text(request, "ui_b036712357df", "Phân hệ Nghỉ phép") %></div>
        <a class="nav-link" href="types"><%= com.hrms.i18n.Language.text(request, "ui_611245ff0fb6", "Cấu hình loại phép") %></a>
        <a class="nav-link" href="all-requests"><%= com.hrms.i18n.Language.text(request, "ui_b26df14c4e69", "Tất cả đơn nghỉ") %></a>
        <a class="nav-link" href="pending-approvals"><%= com.hrms.i18n.Language.text(request, "ui_7b0a878a5449", "Duyệt đơn nghỉ phép") %></a>
        <a class="nav-link active" href="calendar"><%= com.hrms.i18n.Language.text(request, "ui_0dd83ebf282c", "Lịch nghỉ công ty") %></a>
        <a class="nav-link" href="report"><%= com.hrms.i18n.Language.text(request, "ui_e198a77b4338", "Báo cáo nghỉ phép") %></a>

        <a class="nav-link logout-link" href="../logout"><%= com.hrms.i18n.Language.text(request, "ui_d513aac0be87", "Đăng xuất") %></a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_3738ab5b9eb5", "Lịch nghỉ phép Toàn công ty") %></h1>
                <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_21d4ae621794", "Theo dõi lịch nghỉ tập trung của các phòng ban để chủ động điều phối ca trực và nhân sự.") %></p>
            </div>
            <div class="header-actions">
                <div class="filter-group" style="min-width: 200px;">
                    <select id="deptFilter">
                        <option value=""><%= com.hrms.i18n.Language.text(request, "ui_5bf8b98ac9a0", "-- Tất cả phòng ban --") %></option>
                    </select>
                </div>
            </div>
        </div>

        <!-- Interactive Calendar Component -->
        <div class="calendar-container">
            <div class="calendar-header">
                <div id="calendarMonthTitle" class="calendar-month-title"><%= com.hrms.i18n.Language.text(request, "ui_5cf8447ec515", "Tháng -- / ----") %></div>
                <div style="display: flex; gap: 8px;">
                    <button id="btnPrevMonth" class="calendar-nav-btn"><%= com.hrms.i18n.Language.text(request, "ui_a44a3d6046ab", "&larr; Tháng trước") %></button>
                    <button id="btnCurrentMonth" class="calendar-nav-btn"><%= com.hrms.i18n.Language.text(request, "ui_0048b6a408e3", "Hôm nay") %></button>
                    <button id="btnNextMonth" class="calendar-nav-btn"><%= com.hrms.i18n.Language.text(request, "ui_38b4f3627eb5", "Tháng sau &rarr;") %></button>
                </div>
            </div>

            <!-- Calendar Days of Week Header (Mon to Sun) -->
            <div class="calendar-grid-header">
                <div><%= com.hrms.i18n.Language.text(request, "ui_97dbd47ac906", "Thứ 2") %></div>
                <div><%= com.hrms.i18n.Language.text(request, "ui_591b5d0df78a", "Thứ 3") %></div>
                <div><%= com.hrms.i18n.Language.text(request, "ui_dde473c80589", "Thứ 4") %></div>
                <div><%= com.hrms.i18n.Language.text(request, "ui_30e2ba2157b4", "Thứ 5") %></div>
                <div><%= com.hrms.i18n.Language.text(request, "ui_cc66d50d2654", "Thứ 6") %></div>
                <div><%= com.hrms.i18n.Language.text(request, "ui_4aacb4481107", "Thứ 7") %></div>
                <div><%= com.hrms.i18n.Language.text(request, "ui_5f78bbed3928", "Chủ Nhật") %></div>
            </div>

            <!-- Calendar Days Grid -->
            <div id="calendarDaysContainer" class="calendar-grid-days">
                <div style="grid-column: 1/-1; text-align: center; padding: 32px; background: #fff; color: #667085;">
                    <%= com.hrms.i18n.Language.text(request, "ui_1a44080116ca", "Đang tải lịch...") %>
                </div>
            </div>
        </div>
    </main>
</div>

<!-- Details Modal -->
<div id="modalDetail" class="modal-overlay">
    <div class="modal-box">
        <div class="modal-header">
            <h3 class="modal-title"><%= com.hrms.i18n.Language.text(request, "ui_f5f4e2b42cdc", "Chi tiết lịch nghỉ nhân sự") %></h3>
            <button class="modal-close" onclick="LeaveApp.closeModal('modalDetail')">&times;</button>
        </div>
        <div class="modal-body" id="detailModalBody"></div>
        <div class="modal-footer">
            <button class="button secondary" onclick="LeaveApp.closeModal('modalDetail')"><%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %></button>
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
