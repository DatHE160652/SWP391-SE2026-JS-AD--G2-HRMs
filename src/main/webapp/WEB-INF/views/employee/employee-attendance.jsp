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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/employee-navigation.css?v=<%= application.getResource("/css/employee-navigation.css").openConnection().getLastModified() %>">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/attendance.css?v=<%= application.getResource("/css/attendance.css").openConnection().getLastModified() %>">
</head>
<body class="lc" data-page="employee-attendance" data-profile-role="<c:out value='${sessionScope.tenantPrincipal.roleCode}'/>">
<c:set var="employeeNavPage" value="attendance"/>
<c:set var="employeeNavTitle" value="Chấm công cá nhân"/>
<%@ include file="/WEB-INF/views/common/employee-shell.jspf" %>

    <main class="lc-main content">
        <div class="lc-breadcrumb">Cổng nhân viên &nbsp; / &nbsp; Chấm công cá nhân</div>

        <div class="page-header" style="display:flex; justify-content:space-between; align-items:center;">
            <div>
                <h1 class="page-title">Bảng chấm công cá nhân</h1>
                <p class="page-subtitle">Xem lịch sử chấm công theo tháng, số công làm việc và gửi đơn giải trình nếu có sai sót hoặc quên quẹt thẻ.</p>
            </div>
            <div class="header-actions">
                <button class="button" id="btnMyExplanations" type="button" style="background:#4338ca; border-color:#4338ca; color:#fff; display:inline-flex; align-items:center; gap:6px;">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                        <polyline points="14 2 14 8 20 8"></polyline>
                        <line x1="16" y1="13" x2="8" y2="13"></line>
                        <line x1="16" y1="17" x2="8" y2="17"></line>
                    </svg>
                    Đơn giải trình của tôi
                </button>
            </div>
        </div>

        <div class="info-banner">
            <svg width="20" height="20" fill="currentColor" viewBox="0 0 20 20" style="flex-shrink:0;">
                <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"></path>
            </svg>
            <div>
                <strong>Lưu ý:</strong> Chế độ xem dành riêng cho nhân viên. Nếu bạn quên chấm công hoặc đi công tác bên ngoài, vui lòng bấm nút <strong>"Giải trình"</strong> tại dòng ngày tương ứng để gửi yêu cầu cho quản lý duyệt bù công.
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
            <div class="summary-card">
                <div class="summary-label">Số ngày đi làm</div>
                <div id="totalWorkingDays" class="summary-number">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Số ngày công</div>
                <div id="totalWorkCredits" class="summary-number text-success" style="font-size: 24px;">0 công</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Tổng giờ làm</div>
                <div id="totalHours" class="summary-number text-success" style="font-size: 24px;">0 giờ</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Đúng giờ</div>
                <div id="presentDays" class="summary-number text-success">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Vắng / Nghỉ phép</div>
                <div id="absentDays" class="summary-number text-danger" style="font-size: 22px;">0</div>
            </div>
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
                    <th style="text-align:center;">Thao tác</th>
                </tr>
                </thead>
                <tbody id="attendanceRows">
                    <tr><td colspan="9" style="text-align:center;color:#667085;padding:24px;">Đang tải dữ liệu...</td></tr>
                </tbody>
            </table>
        </section>
    </main>

<!-- Modal UC86: Gửi đơn giải trình chấm công -->
<div id="modalSubmitExplanation" class="exp-modal-overlay">
    <div class="exp-modal-box">
        <div class="exp-modal-header">
            <h3>Đơn giải trình / Xin bù công</h3>
            <button type="button" class="btn-close" id="btnCloseSubmitExp" style="background:none;border:none;font-size:20px;cursor:pointer;">&times;</button>
        </div>
        <form id="formSubmitExplanation">
            <div class="exp-modal-body">
                <div class="form-group" style="margin-bottom:16px;">
                    <label style="font-weight:600;display:block;margin-bottom:6px;">Ngày giải trình <span class="text-danger">*</span></label>
                    <input type="date" id="expAttendanceDate" class="form-control" required style="width:100%;padding:8px 12px;border:1px solid #d0d5dd;border-radius:6px;">
                </div>
                <div class="form-group" style="margin-bottom:16px;">
                    <label style="font-weight:600;display:block;margin-bottom:6px;">Loại lý do <span class="text-danger">*</span></label>
                    <select id="expReasonType" class="form-control" required style="width:100%;padding:8px 12px;border:1px solid #d0d5dd;border-radius:6px;">
                        <option value="FORGOT_PUNCH">Quên bấm chấm công</option>
                        <option value="BUSINESS_TRIP">Đi công tác / Ra ngoài gặp đối tác</option>
                        <option value="TECHNICAL_ISSUE">Lỗi máy chấm công / Lỗi mạng</option>
                        <option value="OTHER">Lý do chính đáng khác</option>
                    </select>
                </div>
                <div style="display:flex;gap:16px;margin-bottom:16px;">
                    <div style="flex:1;">
                        <label style="font-weight:600;display:block;margin-bottom:6px;">Giờ vào thực tế đề xuất</label>
                        <input type="time" id="expProposedCheckIn" value="08:30" class="form-control" style="width:100%;padding:8px 12px;border:1px solid #d0d5dd;border-radius:6px;">
                    </div>
                    <div style="flex:1;">
                        <label style="font-weight:600;display:block;margin-bottom:6px;">Giờ tan ca thực tế đề xuất</label>
                        <input type="time" id="expProposedCheckOut" value="17:30" class="form-control" style="width:100%;padding:8px 12px;border:1px solid #d0d5dd;border-radius:6px;">
                    </div>
                </div>
                <div class="form-group">
                    <label style="font-weight:600;display:block;margin-bottom:6px;">Lý do giải trình chi tiết <span class="text-danger">*</span></label>
                    <textarea id="expReasonDetail" rows="3" required placeholder="Ghi rõ lý do tại sao không bấm công đúng giờ hoặc vắng mặt..." style="width:100%;padding:8px 12px;border:1px solid #d0d5dd;border-radius:6px;"></textarea>
                </div>
            </div>
            <div class="exp-modal-footer">
                <button type="button" class="button secondary" id="btnCancelSubmitExp">Đóng</button>
                <button type="submit" class="button" style="background:#4338ca;border-color:#4338ca;color:#fff;">Gửi đơn duyệt</button>
            </div>
        </form>
    </div>
</div>

<!-- Modal UC86: Danh sách đơn giải trình của tôi -->
<div id="modalMyExplanations" class="exp-modal-overlay">
    <div class="exp-modal-box wide">
        <div class="exp-modal-header">
            <h3>Danh sách đơn giải trình của tôi</h3>
            <button type="button" class="btn-close" id="btnCloseMyExp" style="background:none;border:none;font-size:20px;cursor:pointer;">&times;</button>
        </div>
        <div class="exp-modal-body">
            <table style="width:100%;border-collapse:collapse;">
                <thead>
                    <tr style="border-bottom:2px solid #eaecf0;text-align:left;font-size:13px;color:#475467;">
                        <th style="padding:10px 8px;">Ngày công</th>
                        <th style="padding:10px 8px;">Loại lý do</th>
                        <th style="padding:10px 8px;">Giờ đề xuất</th>
                        <th style="padding:10px 8px;">Trạng thái</th>
                        <th style="padding:10px 8px;">Phản hồi</th>
                        <th style="padding:10px 8px;text-align:center;">Thao tác</th>
                    </tr>
                </thead>
                <tbody id="myExplanationsRows">
                    <tr><td colspan="6" style="text-align:center;color:#667085;padding:20px;">Đang tải danh sách...</td></tr>
                </tbody>
            </table>
        </div>
        <div class="exp-modal-footer">
            <button type="button" class="button secondary" id="btnCloseMyExpBottom">Đóng</button>
        </div>
    </div>
</div>

<script src="../js/attendance.js?v=<%= application.getResource("/js/attendance.js").openConnection().getLastModified() %>"></script>
<script src="../js/payroll-notifications.js"></script>
</body>
</html>
