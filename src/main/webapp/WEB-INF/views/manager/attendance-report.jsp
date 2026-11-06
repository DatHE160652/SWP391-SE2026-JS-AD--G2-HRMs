<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Báo cáo Chấm công - HRMs Cloud</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/attendance.css?v=<%= application.getResource("/css/attendance.css").openConnection().getLastModified() %>">
</head>
<body class="lc" data-page="attendance-report">
<c:choose>
    <c:when test="${sessionScope.tenantPrincipal.companyAdministrator || sessionScope.tenantPrincipal.roleCode == 'HR'}">
        <%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    </c:when>
    <c:otherwise>
        <c:set var="managerNavPage" value="attendance-report"/>
        <c:set var="managerNavTitle" value="Báo cáo Chấm công"/>
        <%@ include file="/WEB-INF/views/common/manager-shell.jspf" %>
    </c:otherwise>
</c:choose>

    <main class="lc-main content">
        <c:choose>
            <c:when test="${sessionScope.tenantPrincipal.companyAdministrator || sessionScope.tenantPrincipal.roleCode == 'HR'}">
                <div class="lc-breadcrumb">Quản trị nhân sự &nbsp; / &nbsp; Nghiệp vụ khác &nbsp; / &nbsp; Báo cáo Chấm công</div>
            </c:when>
            <c:otherwise>
                <div class="lc-breadcrumb">Cổng quản lý &nbsp; / &nbsp; Báo cáo Chấm công</div>
            </c:otherwise>
        </c:choose>

        <div class="page-header">
            <div>
                <h1 class="page-title">Báo cáo Tổng hợp Chấm công</h1>
                <p class="page-subtitle">Tổng hợp chỉ số chuyên cần, tỷ lệ đi đúng giờ của nhân viên theo phòng ban và theo tháng.</p>
            </div>
            <div class="header-actions">
                <button class="button" id="btnExportCsv" type="button" style="background:#00866b; border-color:#00866b; color:#fff; display:inline-flex; align-items:center; gap:6px;">
                    <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clip-rule="evenodd"></path>
                    </svg>
                    Xuất Báo cáo CSV (Excel)
                </button>
            </div>
        </div>

        <!-- Filter Bar -->
        <section class="panel" style="margin-bottom: 20px;">
            <div class="filter-bar" style="margin-bottom: 0; background: none; border: none; padding: 0;">
                <div class="filter-group" style="min-width: 200px;">
                    <label for="reportDept">Phòng ban:</label>
                    <select id="reportDept"></select>
                </div>
                <div class="filter-group">
                    <label for="reportMonth">Tháng:</label>
                    <select id="reportMonth">
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
                    <label for="reportYear">Năm:</label>
                    <select id="reportYear">
                        <option value="2025">2025</option>
                        <option value="2026" selected>2026</option>
                        <option value="2027">2027</option>
                    </select>
                </div>
                <div class="filter-actions">
                    <button class="button" id="btnLoadReport" type="button">Xem Báo Cáo</button>
                </div>
            </div>
        </section>

        <!-- Summary Grid -->
        <section class="summary-grid" style="grid-template-columns: repeat(2, minmax(200px, 1fr));">
            <div class="summary-card">
                <div class="summary-label">Tổng số nhân viên báo cáo</div>
                <div id="reportTotalEmployees" class="summary-number">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Tỉ lệ đúng giờ trung bình</div>
                <div id="reportAvgPunctuality" class="summary-number" style="color:var(--navy, #062579);">0%</div>
            </div>
        </section>

        <div id="message" class="message"></div>

        <!-- Report Table -->
        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th>Mã NV</th>
                    <th>Họ và Tên</th>
                    <th>Phòng ban</th>
                    <th>Số ngày làm</th>
                    <th>Tổng giờ làm</th>
                    <th>Đúng giờ</th>
                    <th>Đi muộn</th>
                    <th>Về sớm</th>
                    <th>Vắng</th>
                    <th>Tỉ lệ đúng giờ</th>
                </tr>
                </thead>
                <tbody id="reportRows">
                    <tr><td colspan="10" style="text-align:center;color:#667085;padding:24px;">Đang tải dữ liệu báo cáo...</td></tr>
                </tbody>
            </table>
        </section>
    </main>

<script src="../js/attendance.js?v=<%= application.getResource("/js/attendance.js").openConnection().getLastModified() %>"></script>
<script src="../js/payroll-notifications.js"></script>
</body>
</html>
