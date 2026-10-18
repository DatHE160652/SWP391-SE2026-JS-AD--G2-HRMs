<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chấm công cá nhân - HRMs Cloud</title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
</head>
<body data-page="employee-attendance">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>Employee Portal</span></div>
        <div class="nav-label">Chức năng cá nhân</div>
        <a class="nav-link" href="../employee/my-profile">Hồ sơ cá nhân</a>
        <a class="nav-link active" href="employee-attendance">Chấm công cá nhân</a>

        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">

        <div class="page-header">
            <div>
                <h1 class="page-title">Bảng chấm công cá nhân</h1>
                <p class="page-subtitle">Nhân viên xem lịch sử chấm công theo tháng. Điểm danh vào ca/tan ca do Manager phụ trách.</p>
            </div>
        </div>

        <div class="info-banner" style="margin: 0 28px 16px;">
            <svg width="20" height="20" fill="currentColor" viewBox="0 0 20 20" style="flex-shrink:0;">
                <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"></path>
            </svg>
            <div>
                <strong>Lưu ý:</strong> Chế độ xem dành riêng cho nhân viên. Employee chỉ được xem dữ liệu chấm công của chính mình.
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
                    <button class="button" id="btnFilter">Xem bảng công</button>
                </div>
            </div>
        </section>

        <section class="summary-grid" style="grid-template-columns: repeat(6, minmax(130px, 1fr));">
            <div class="summary-card">
                <div class="summary-label">Số ngày đi làm</div>
                <div id="totalWorkingDays" class="summary-number">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Tổng giờ làm</div>
                <div id="totalHours" class="summary-number text-success" style="font-size: 24px;">0 hrs</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Đúng giờ</div>
                <div id="presentDays" class="summary-number text-success">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Đi muộn</div>
                <div id="lateDays" class="summary-number text-warning">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Về sớm</div>
                <div id="earlyLeaveDays" class="summary-number" style="color: #3538cd;">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Vắng / Nghỉ phép</div>
                <div id="absentDays" class="summary-number text-danger" style="font-size: 22px;">0</div>
            </div>
        </section>

        <div id="message" class="message" style="margin: 0 28px 16px;"></div>

        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th>Ngày</th>
                    <th>Thứ</th>
                    <th>Giờ vào (Check-in)</th>
                    <th>Giờ ra (Check-out)</th>
                    <th>Tổng giờ</th>
                    <th>Trạng thái</th>
                    <th>Ghi chú</th>
                </tr>
                </thead>
                <tbody id="attendanceRows">
                    <tr><td colspan="7" style="text-align:center;color:#667085;padding:24px;">Đang tải dữ liệu...</td></tr>
                </tbody>
            </table>
        </section>
    </main>
</div>
<script src="../js/attendance.js?v=<%= application.getResource("/js/attendance.js").openConnection().getLastModified() %>"></script>
</body>
</html>
