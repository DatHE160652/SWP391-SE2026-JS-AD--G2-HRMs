<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Báo cáo Chấm công - HRMs Cloud</title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
</head>
<body data-page="attendance-report">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>TechNova JSC</span></div>
        <div class="nav-label">Phân hệ Chấm công</div>
        <a class="nav-link" href="../employee/my-profile?nav=manager">Hồ sơ cá nhân</a>
        <a class="nav-link" href="manager-attendance">Điểm danh Nhân sự</a>
        <a class="nav-link" href="manager-personal-attendance">Chấm công Cá nhân</a>
        <a class="nav-link active" href="attendance-report">Báo cáo Chấm công</a>
        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title">Báo cáo Tổng hợp Chấm công</h1>
            </div>
            <div class="actions">
                <button class="button" id="btnExportCsv" style="background:#00866b;border-color:#00866b;">
                    <svg width="16" height="16" fill="currentColor" viewBox="0 0 20 20" style="margin-right:6px;">
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
                    <button class="button" id="btnLoadReport">Xem Báo Cáo</button>
                </div>
            </div>
        </section>

        <!-- Summary Grid -->
        <section class="summary-grid" style="grid-template-columns: repeat(3, minmax(180px, 1fr));">
            <div class="summary-card">
                <div class="summary-label">Tổng số nhân viên báo cáo</div>
                <div id="reportTotalEmployees" class="summary-number">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Tổng giờ làm việc tích lũy</div>
                <div id="reportTotalHours" class="summary-number text-success">0 giờ</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Tỉ lệ đúng giờ trung bình</div>
                <div id="reportAvgPunctuality" class="summary-number" style="color:var(--navy, #062579);">0%</div>
            </div>
        </section>

        <div id="message" class="message" style="margin: 0 28px 16px;"></div>

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
</div>
<script src="../js/attendance.js?v=<%= application.getResource("/js/attendance.js").openConnection().getLastModified() %>"></script>
</body>
</html>
