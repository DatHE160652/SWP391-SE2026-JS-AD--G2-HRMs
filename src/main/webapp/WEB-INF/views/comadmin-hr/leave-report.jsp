<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_8de6e40eb889", "Báo cáo sử dụng nghỉ phép - HRMs Cloud") %></title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
    <link rel="stylesheet" href="../css/leave.css">
</head>
<body data-page="leave-report">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span><c:out value="${not empty sessionScope.tenantPrincipal.companyName ? sessionScope.tenantPrincipal.companyName : 'Doanh nghiệp'}"/></span></div>
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
        <a class="nav-link" href="calendar"><%= com.hrms.i18n.Language.text(request, "ui_0dd83ebf282c", "Lịch nghỉ công ty") %></a>
        <a class="nav-link active" href="report"><%= com.hrms.i18n.Language.text(request, "ui_e198a77b4338", "Báo cáo nghỉ phép") %></a>

        <a class="nav-link logout-link" href="../logout"><%= com.hrms.i18n.Language.text(request, "ui_d513aac0be87", "Đăng xuất") %></a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_b785164dd4b0", "Báo cáo tình hình sử dụng nghỉ phép") %></h1>
                <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_4f813165bdb5", "Tổng hợp tỷ lệ sử dụng ngày phép, hạn mức còn lại và phân loại theo chính sách tính lương của nhân sự toàn công ty.") %></p>
            </div>
            <div class="header-actions">
                <button id="btnExportExcel" class="button" style="background:#027a48;border-color:#027a48;">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:6px;vertical-align:middle;">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                        <polyline points="7 10 12 15 17 10"></polyline>
                        <line x1="12" y1="15" x2="12" y2="3"></line>
                    </svg>
                    <%= com.hrms.i18n.Language.text(request, "ui_c8f65c17c3f8", "Xuất file Excel (.xls)") %>
                </button>
            </div>
        </div>

        <!-- Filter Bar -->
        <section class="panel" style="margin-bottom: 20px;">
            <div class="filter-bar" style="margin-bottom: 0; background: none; border: none; padding: 0;">
                <div class="filter-group" style="min-width: 140px;">
                    <label for="reportYearSelect"><%= com.hrms.i18n.Language.text(request, "ui_d86c9441c101", "Năm báo cáo:") %></label>
                    <select id="reportYearSelect">
                        <option value="2025">2025</option>
                        <option value="2026" selected>2026</option>
                        <option value="2027">2027</option>
                    </select>
                </div>
                <div class="filter-group" style="min-width: 200px;">
                    <label for="reportDeptFilter"><%= com.hrms.i18n.Language.text(request, "ui_325e08e78a74", "Phòng ban:") %></label>
                    <select id="reportDeptFilter">
                        <option value=""><%= com.hrms.i18n.Language.text(request, "ui_5bf8b98ac9a0", "-- Tất cả phòng ban --") %></option>
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
                    <th style="width: 85px;"><%= com.hrms.i18n.Language.text(request, "ui_c6f0f81cc2b0", "Mã NV") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_ad9f29940174", "Họ và tên") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_6f358f848e2e", "Phòng ban") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_285d666f18b5", "Chức danh") %></th>
                    <th style="width: 90px; text-align: right;"><%= com.hrms.i18n.Language.text(request, "ui_beed8c92d480", "Được cấp") %></th>
                    <th style="width: 90px; text-align: right;"><%= com.hrms.i18n.Language.text(request, "ui_2cc668ec6579", "Đã dùng") %></th>
                    <th style="width: 90px; text-align: right;"><%= com.hrms.i18n.Language.text(request, "ui_dac73a26d36f", "Còn lại") %></th>
                    <th style="width: 95px; text-align: right;"><%= com.hrms.i18n.Language.text(request, "ui_b44e4de07f2c", "Có lương") %></th>
                    <th style="width: 95px; text-align: right;"><%= com.hrms.i18n.Language.text(request, "ui_2300c982f24c", "Không lương") %></th>
                    <th style="width: 75px; text-align: center;"><%= com.hrms.i18n.Language.text(request, "ui_edca2522e25d", "Tổng đơn") %></th>
                    <th style="width: 75px; text-align: center;"><%= com.hrms.i18n.Language.text(request, "ui_1165d8820535", "Đã duyệt") %></th>
                </tr>
                </thead>
                <tbody id="reportTableBody">
                    <tr><td colspan="12" style="text-align: center; color: #667085; padding: 24px;"><%= com.hrms.i18n.Language.text(request, "ui_6a578200c996", "Đang tải báo cáo...") %></td></tr>
                </tbody>
            </table>
        </section>
    </main>
</div>



<script src="../js/leave.js?v=<%= application.getResource("/js/leave.js").openConnection().getLastModified() %>"></script>
<script>
    document.addEventListener("DOMContentLoaded", () => {
        LeaveApp.initReportView();
    });
</script>
</body>
</html>
