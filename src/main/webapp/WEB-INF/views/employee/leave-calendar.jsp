<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_ca884148d77f", "Lịch nghỉ cá nhân - HRMs Cloud") %></title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
    <link rel="stylesheet" href="../css/leave.css">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc" data-page="leave-calendar-employee">
<div class="workspace-shell">
    <%@ include file="/WEB-INF/views/common/tenant-shell.jspf" %>

    <main class="lc-main content">
        <div class="page-header">
            <div>
                <h1 class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_ef7a5eded78e", "Lịch nghỉ phép cá nhân") %></h1>
                <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_9bfcfe703f8b", "Xem trực quan các đợt nghỉ phép đã được phê duyệt trong tháng theo dạng lịch biểu.") %></p>
            </div>
            <div class="header-actions">
                <a href="submit" class="button" style="text-decoration: none;"><%= com.hrms.i18n.Language.text(request, "ui_3fe7cc4d4d30", "+ Tạo đơn mới") %></a>
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
            <h3 class="modal-title"><%= com.hrms.i18n.Language.text(request, "ui_d7fe5111ad88", "Chi tiết lịch nghỉ") %></h3>
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
        LeaveApp.initCalendarView(true); // true = employee personal scope only
    });
</script>
</body>
</html>
