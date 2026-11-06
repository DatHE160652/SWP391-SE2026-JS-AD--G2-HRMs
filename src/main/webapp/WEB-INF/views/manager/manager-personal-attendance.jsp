<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chấm công cá nhân - HRMs Cloud</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/attendance.css?v=<%= application.getResource("/css/attendance.css").openConnection().getLastModified() %>">
</head>
<body class="lc" data-page="employee-attendance" data-personal-scope="self">
<c:set var="managerNavPage" value="personal-attendance"/>
<c:set var="managerNavTitle" value="Chấm công Cá nhân"/>
<%@ include file="/WEB-INF/views/common/manager-shell.jspf" %>

    <main class="lc-main content">
        <div class="lc-breadcrumb">Cổng quản lý &nbsp; / &nbsp; Chấm công Cá nhân</div>

        <div class="page-header">
            <div>
                <h1 class="page-title">Bảng chấm công cá nhân</h1>
                <p class="page-subtitle">Quản lý xem lịch sử chấm công cá nhân theo từng tháng.</p>
            </div>
        </div>

        <section class="panel" style="margin-bottom: 20px;">
            <div class="filter-bar" style="margin-bottom: 0; background: none; border: none; padding: 0;">
                <div class="filter-group" style="min-width: 240px;">
                    <label for="employeeSelect">Nhân viên:</label>
                    <select id="employeeSelect"></select>
                </div>
                <div class="filter-group">
                    <label for="monthSelect">Tháng:</label>
                    <select id="monthSelect">
                        <option value="1">Tháng 1</option>
                        <option value="2">Tháng 2</option>
                        <option value="3">Tháng 3</option>
                        <option value="4">Tháng 4</option>
                        <option value="5">Tháng 5</option>
                        <option value="6">Tháng 6</option>
                        <option value="7">Tháng 7</option>
                        <option value="8">Tháng 8</option>
                        <option value="9">Tháng 9</option>
                        <option value="10">Tháng 10</option>
                        <option value="11">Tháng 11</option>
                        <option value="12">Tháng 12</option>
                    </select>
                </div>
                <div class="filter-group">
                    <label for="yearSelect">Năm:</label>
                    <select id="yearSelect">
                        <option value="2025">2025</option>
                        <option value="2026" selected>2026</option>
                        <option value="2027">2027</option>
                    </select>
                </div>
                <div class="filter-actions">
                    <button class="button" id="btnFilter" type="button">Xem bảng công</button>
                </div>
            </div>
        </section>

        <section class="summary-grid" style="grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));">
            <div class="summary-card"><div class="summary-label">Số ngày đi làm</div><div id="totalWorkingDays" class="summary-number">0</div></div>
            <div class="summary-card"><div class="summary-label">Số ngày công</div><div id="totalWorkCredits" class="summary-number text-success" style="font-size: 24px;">0 công</div></div>
            <div class="summary-card"><div class="summary-label">Tổng giờ làm</div><div id="totalHours" class="summary-number text-success" style="font-size: 24px;">0 giờ</div></div>
            <div class="summary-card"><div class="summary-label">Đúng giờ</div><div id="presentDays" class="summary-number text-success">0</div></div>
            <div class="summary-card"><div class="summary-label">Vắng / Nghỉ phép</div><div id="absentDays" class="summary-number text-danger" style="font-size: 22px;">0</div></div>
        </section>

        <div id="message" class="message"></div>

        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th>Ngày</th>
                    <th>Thứ</th>
                    <th>Giờ vào</th>
                    <th>Giờ ra</th>
                    <th>Tổng giờ</th>
                    <th>Số công</th>
                    <th>Trạng thái</th>
                    <th>Ghi chú</th>
                </tr>
                </thead>
                <tbody id="attendanceRows">
                    <tr><td colspan="8" style="text-align:center;color:#667085;padding:24px;">Đang tải dữ liệu...</td></tr>
                </tbody>
            </table>
        </section>
    </main>

<script src="../js/attendance.js?v=<%= application.getResource("/js/attendance.js").openConnection().getLastModified() %>"></script>
<script src="../js/payroll-notifications.js"></script>
</body>
</html>
