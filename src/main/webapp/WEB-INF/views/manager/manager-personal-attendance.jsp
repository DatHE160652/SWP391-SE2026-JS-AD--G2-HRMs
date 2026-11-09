<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_d7794a1b7f80", "Chấm công cá nhân - HRMs Cloud") %></title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/attendance.css?v=<%= application.getResource("/css/attendance.css").openConnection().getLastModified() %>">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc" data-page="employee-attendance" data-personal-scope="self">
<c:set var="managerNavPage" value="personal-attendance"/>
<c:set var="managerNavTitle" value="Chấm công Cá nhân"/>
<%@ include file="/WEB-INF/views/common/manager-shell.jspf" %>

    <main class="lc-main content">
        <div class="lc-breadcrumb"><%= com.hrms.i18n.Language.text(request, "ui_46fe7662da03", "Cổng quản lý &nbsp; / &nbsp; Chấm công Cá nhân") %></div>

        <div class="page-header">
            <div>
                <h1 class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_b7670a343e0f", "Bảng chấm công cá nhân") %></h1>
                <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_e68533fe6b58", "Quản lý xem lịch sử chấm công cá nhân theo từng tháng.") %></p>
            </div>
        </div>

        <section class="panel" style="margin-bottom: 20px;">
            <div class="filter-bar" style="margin-bottom: 0; background: none; border: none; padding: 0;">
                <div class="filter-group" style="min-width: 240px;">
                    <label for="employeeSelect"><%= com.hrms.i18n.Language.text(request, "ui_083724684294", "Nhân viên:") %></label>
                    <select id="employeeSelect"></select>
                </div>
                <div class="filter-group">
                    <label for="monthSelect"><%= com.hrms.i18n.Language.text(request, "ui_10168731cfaa", "Tháng:") %></label>
                    <select id="monthSelect">
                        <option value="1"><%= com.hrms.i18n.Language.text(request, "ui_b216054d0ed2", "Tháng 1") %></option>
                        <option value="2"><%= com.hrms.i18n.Language.text(request, "ui_516d1076c554", "Tháng 2") %></option>
                        <option value="3"><%= com.hrms.i18n.Language.text(request, "ui_ffadeda83dc4", "Tháng 3") %></option>
                        <option value="4"><%= com.hrms.i18n.Language.text(request, "ui_e17ca884dc27", "Tháng 4") %></option>
                        <option value="5"><%= com.hrms.i18n.Language.text(request, "ui_cac48fd56c48", "Tháng 5") %></option>
                        <option value="6"><%= com.hrms.i18n.Language.text(request, "ui_ba86b1edac83", "Tháng 6") %></option>
                        <option value="7"><%= com.hrms.i18n.Language.text(request, "ui_9d6c2d27b25e", "Tháng 7") %></option>
                        <option value="8"><%= com.hrms.i18n.Language.text(request, "ui_90318b387a39", "Tháng 8") %></option>
                        <option value="9"><%= com.hrms.i18n.Language.text(request, "ui_d90a79daca16", "Tháng 9") %></option>
                        <option value="10"><%= com.hrms.i18n.Language.text(request, "ui_1293573088a5", "Tháng 10") %></option>
                        <option value="11"><%= com.hrms.i18n.Language.text(request, "ui_e2e2ffeea726", "Tháng 11") %></option>
                        <option value="12"><%= com.hrms.i18n.Language.text(request, "ui_59de2e03dd04", "Tháng 12") %></option>
                    </select>
                </div>
                <div class="filter-group">
                    <label for="yearSelect"><%= com.hrms.i18n.Language.text(request, "ui_3731977b06d7", "Năm:") %></label>
                    <select id="yearSelect">
                        <option value="2025">2025</option>
                        <option value="2026" selected>2026</option>
                        <option value="2027">2027</option>
                    </select>
                </div>
                <div class="filter-actions">
                    <button class="button" id="btnFilter" type="button"><%= com.hrms.i18n.Language.text(request, "ui_d866f4958610", "Xem bảng công") %></button>
                </div>
            </div>
        </section>

        <section class="summary-grid" style="grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));">
            <div class="summary-card"><div class="summary-label"><%= com.hrms.i18n.Language.text(request, "ui_fa1a0311252f", "Số ngày đi làm") %></div><div id="totalWorkingDays" class="summary-number">0</div></div>
            <div class="summary-card"><div class="summary-label"><%= com.hrms.i18n.Language.text(request, "ui_1ba1fa4b2b12", "Số ngày công") %></div><div id="totalWorkCredits" class="summary-number text-success" style="font-size: 24px;"><%= com.hrms.i18n.Language.text(request, "ui_ad40299b9b92", "0 công") %></div></div>
            <div class="summary-card"><div class="summary-label"><%= com.hrms.i18n.Language.text(request, "ui_fae054de5cea", "Tổng giờ làm") %></div><div id="totalHours" class="summary-number text-success" style="font-size: 24px;"><%= com.hrms.i18n.Language.text(request, "ui_d245a9a1d25c", "0 giờ") %></div></div>
            <div class="summary-card"><div class="summary-label"><%= com.hrms.i18n.Language.text(request, "ui_eeb85b25d751", "Đúng giờ") %></div><div id="presentDays" class="summary-number text-success">0</div></div>
            <div class="summary-card"><div class="summary-label"><%= com.hrms.i18n.Language.text(request, "ui_605a2078868b", "Vắng / Nghỉ phép") %></div><div id="absentDays" class="summary-number text-danger" style="font-size: 22px;">0</div></div>
        </section>

        <div id="message" class="message"></div>

        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_368c4575744a", "Ngày") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_f445a0520061", "Thứ") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_4754e55645c1", "Giờ vào") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_7c51594b39a3", "Giờ ra") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_bf5e3203822b", "Tổng giờ") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_e988aba3e0d7", "Số công") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_02f03f68e75d", "Ghi chú") %></th>
                </tr>
                </thead>
                <tbody id="attendanceRows">
                    <tr><td colspan="8" style="text-align:center;color:#667085;padding:24px;"><%= com.hrms.i18n.Language.text(request, "ui_de6849168004", "Đang tải dữ liệu...") %></td></tr>
                </tbody>
            </table>
        </section>
    </main>

<script src="../js/attendance.js?v=<%= application.getResource("/js/attendance.js").openConnection().getLastModified() %>"></script>
<script src="../js/payroll-notifications.js?v=<%= application.getResource("/js/payroll-notifications.js").openConnection().getLastModified() %>"></script>
</body>
</html>
