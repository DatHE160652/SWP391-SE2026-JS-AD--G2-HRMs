<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Giám sát Chấm công Toàn Công ty - HRMs Cloud</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/attendance.css?v=<%= application.getResource("/css/attendance.css").openConnection().getLastModified() %>">
</head>
<body class="lc" data-page="company-attendance">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>

    <main class="lc-main content">
        <div class="lc-breadcrumb">Quản trị nhân sự &nbsp; / &nbsp; Nghiệp vụ khác &nbsp; / &nbsp; Giám sát chấm công</div>

        <div class="page-header">
            <div>
                <h1 class="page-title">Giám sát Chấm công Toàn Công ty</h1>
                <p class="page-subtitle">Theo dõi tình hình điểm danh, thời gian vào ca, tan ca và trạng thái làm việc của toàn thể nhân sự theo thời gian thực.</p>
            </div>
            <div class="header-actions">
                <button class="button" id="btnOpenShiftSetting" type="button" style="background:#062579; border-color:#062579; color:#fff; display:inline-flex; align-items:center; gap:6px;">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="3"></circle>
                        <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                    </svg>
                    Cài đặt ca & Chốt công
                </button>
                <a class="button secondary" href="attendance-report" style="text-decoration:none;display:inline-flex;align-items:center;gap:6px;">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="18" y1="20" x2="18" y2="10"></line>
                        <line x1="12" y1="20" x2="12" y2="4"></line>
                        <line x1="6" y1="20" x2="6" y2="14"></line>
                    </svg>
                    Báo cáo Chấm công
                </a>
            </div>
        </div>

        <!-- Filter Bar -->
        <section class="panel" style="margin-bottom: 20px;">
            <div class="filter-bar" style="margin-bottom: 0; background: none; border: none; padding: 0;">
                <div class="filter-group" style="min-width: 180px;">
                    <label for="deptFilter">Phòng ban:</label>
                    <select id="deptFilter"></select>
                </div>
                <div class="filter-group" style="min-width: 140px;">
                    <label for="statusFilter">Trạng thái:</label>
                    <select id="statusFilter">
                        <option value="ALL">-- Tất cả trạng thái --</option>
                        <option value="Present">Có mặt</option>
                        <option value="Late">Đi muộn</option>
                        <option value="Early Leave">Về sớm</option>
                        <option value="Absent">Vắng mặt</option>
                        <option value="On Leave">On Leave (Nghỉ phép)</option>
                    </select>
                </div>
                <div class="filter-group">
                    <label for="fromDate">Từ ngày:</label>
                    <input type="date" id="fromDate">
                </div>
                <div class="filter-group">
                    <label for="toDate">Đến ngày:</label>
                    <input type="date" id="toDate">
                </div>
                <div class="filter-group" style="min-width: 200px; flex: 1;">
                    <label for="keyword">Từ khóa tìm kiếm:</label>
                    <input type="text" id="keyword" placeholder="Mã NV hoặc Họ tên...">
                </div>
                <div class="filter-actions">
                    <button class="button" id="btnSearch" type="button">Tìm kiếm</button>
                    <button class="button secondary" id="btnReset" type="button">Đặt lại</button>
                </div>
            </div>
        </section>

        <!-- Summary Grid -->
        <section class="summary-grid" style="grid-template-columns: repeat(1, minmax(200px, 260px));">
            <div class="summary-card">
                <div class="summary-label">Tổng bản ghi tìm thấy</div>
                <div id="totalRecords" class="summary-number text-success">0</div>
            </div>
        </section>

        <div id="message" class="message"></div>

        <!-- Attendance Table -->
        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th>Mã NV</th>
                    <th>Họ và Tên</th>
                    <th>Phòng ban</th>
                    <th>Ngày</th>
                    <th>Vào ca</th>
                    <th>Tan ca</th>
                    <th>Tổng giờ</th>
                    <th>Trạng thái</th>
                    <th>Ghi chú</th>
                </tr>
                </thead>
                <tbody id="companyAttendanceRows">
                    <tr><td colspan="9" style="text-align:center;color:#667085;padding:24px;">Đang tải dữ liệu chấm công...</td></tr>
                </tbody>
            </table>
        </section>
        <!-- Modal Cài đặt Giờ giấc làm việc & Ngày chốt công cho Công ty (CompanyShiftSettings) -->
        <div id="modalShiftSetting" class="shift-modal-overlay">
            <div class="shift-modal-box">
                <div class="shift-modal-header">
                    <h3 class="shift-modal-title">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#062579" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="12" cy="12" r="10"></circle>
                            <polyline points="12 6 12 12 16 14"></polyline>
                        </svg>
                        Cài đặt giờ giấc làm việc & Chốt công công ty
                    </h3>
                    <button class="shift-modal-close" id="btnCloseShiftModal" type="button" aria-label="Đóng">&times;</button>
                </div>
                <div class="shift-modal-body">
                    <p style="font-size: 13px; color: #475467; margin-top: 0; margin-bottom: 16px; line-height: 1.5;">
                        Cấu hình khung giờ chuẩn và quy tắc tự động khóa bảng chấm công cho toàn công ty. Các thiết lập này sẽ <strong>chạy ngầm tự động</strong> trong toàn hệ thống.
                    </p>
                    <div id="shiftModalMessage" class="message" style="display:none; margin: 0 0 16px 0;"></div>
                    <form id="formShiftSetting" onsubmit="return false;">
                        <input type="hidden" name="organizationCsrfToken" value="${organizationCsrfToken}">

                        <!-- Section 1: Khung giờ ca làm việc -->
                        <div class="shift-section-title">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                            Khung giờ làm việc chuẩn
                        </div>
                        <div class="shift-input-card">
                            <div class="shift-grid-2">
                                <div class="shift-field-group">
                                    <label for="inputShiftStartTime">Giờ vào ca (Bắt đầu): <span style="color:#d92d20;">*</span></label>
                                    <input type="time" id="inputShiftStartTime" name="shiftStartTime" value="08:30" required>
                                    <div class="shift-field-hint">Giờ quy định nhân viên bắt đầu làm việc.</div>
                                </div>
                                <div class="shift-field-group">
                                    <label for="inputShiftEndTime">Giờ tan ca (Kết thúc): <span style="color:#d92d20;">*</span></label>
                                    <input type="time" id="inputShiftEndTime" name="shiftEndTime" value="17:30" required>
                                    <div class="shift-field-hint">Tan ca trước giờ này tính là Về sớm.</div>
                                </div>
                            </div>
                        </div>

                        <!-- Section 2: Quy tắc ân hạn & nghỉ trưa -->
                        <div class="shift-section-title">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
                            Quy tắc tính trễ & Khấu trừ nghỉ trưa
                        </div>
                        <div class="shift-input-card">
                            <div class="shift-grid-2">
                                <div class="shift-field-group">
                                    <label for="inputGracePeriodMinutes">Ân hạn vào muộn (phút):</label>
                                    <input type="number" id="inputGracePeriodMinutes" name="gracePeriodMinutes" min="0" max="120" value="0">
                                    <div class="shift-field-hint">Số phút cho phép trễ mà chưa bị tính Đi muộn.</div>
                                </div>
                                <div class="shift-field-group">
                                    <label for="inputLunchBreakMinutes">Nghỉ trưa (phút):</label>
                                    <input type="number" id="inputLunchBreakMinutes" name="lunchBreakMinutes" min="0" max="240" value="60">
                                    <div class="shift-field-hint">Tự động trừ vào tổng giờ công nếu ca làm &ge; 5 tiếng.</div>
                                </div>
                            </div>
                        </div>

                        <!-- Section 3: Quy tắc tính công theo giờ làm việc -->
                        <div class="shift-section-title">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                            Quy tắc tính công theo số giờ làm việc
                        </div>
                        <div class="shift-input-card">
                            <div class="shift-grid-2">
                                <div class="shift-field-group">
                                    <label for="inputMinHoursHalfDay">Mốc tính 0.5 công (tối thiểu): <span style="color:#d92d20;">*</span></label>
                                    <div style="display:flex; align-items:center; gap:8px;">
                                        <input type="number" id="inputMinHoursHalfDay" name="minHoursHalfDay" min="0.5" max="24" step="0.5" value="4.0" style="width:110px; font-weight:700; text-align:center;">
                                        <span style="font-weight:600; color:#344054; font-size:13.5px;">tiếng</span>
                                    </div>
                                    <div class="shift-field-hint">Số giờ tối thiểu để được tính 0.5 ngày công (nửa công).</div>
                                </div>
                                <div class="shift-field-group">
                                    <label for="inputMinHoursFullDay">Mốc tính 1.0 công (đủ công): <span style="color:#d92d20;">*</span></label>
                                    <div style="display:flex; align-items:center; gap:8px;">
                                        <input type="number" id="inputMinHoursFullDay" name="minHoursFullDay" min="1" max="24" step="0.5" value="6.0" style="width:110px; font-weight:700; text-align:center;">
                                        <span style="font-weight:600; color:#344054; font-size:13.5px;">tiếng</span>
                                    </div>
                                    <div class="shift-field-hint">Số giờ tối thiểu để được tính đủ 1.0 ngày công.</div>
                                </div>
                            </div>
                            <div class="shift-field-hint" style="margin-top:10px; color:#344054; background:#f0f4fe; padding:8px 12px; border-radius:6px; border-left:3px solid #062579; font-size:12.5px; line-height:1.5;">
                                🎯 <strong>Quy tắc áp dụng:</strong> Làm &ge; <strong id="hintFullDay">6.0</strong>h &rarr; <span style="color:#027a48; font-weight:700;">1.0 công</span> | Làm từ <strong id="hintHalfDay">4.0</strong>h đến &lt; <strong id="hintFullDay2">6.0</strong>h &rarr; <span style="color:#b54708; font-weight:700;">0.5 công</span> | Dưới <strong id="hintHalfDay2">4.0</strong>h &rarr; <span style="color:#667085; font-weight:700;">0 công</span>. Thiết lập này <strong>chỉ áp dụng riêng cho công ty hiện tại</strong>.
                            </div>
                        </div>

                        <!-- Section 4: Quy tắc tự động chốt & khóa công hàng tháng -->
                        <div class="shift-section-title">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                            Quy tắc tự động chốt & khóa công hàng tháng
                        </div>
                        <div class="shift-input-card">
                            <div class="shift-field-group">
                                <label for="inputCutoffDay">Ngày chốt bảng công hàng tháng: <span style="color:#d92d20;">*</span></label>
                                <div style="display:flex; align-items:center; gap:8px;">
                                    <span style="font-weight:600; color:#344054; font-size:13.5px;">Ngày</span>
                                    <input type="number" id="inputCutoffDay" name="timesheetCutoffDay" min="1" max="31" value="5" style="width:100px; text-align:center; font-weight:700;">
                                    <span style="font-weight:600; color:#344054; font-size:13.5px;">hàng tháng (Mặc định: Ngày 05)</span>
                                </div>
                                <div class="shift-field-hint" style="margin-top:6px;">
                                    Hệ thống chạy ngầm tự động: Khi thời gian thực tế bước sang ngày này, bảng chấm công của tháng trước sẽ tự động <strong>Khóa hoàn toàn</strong> để kế toán tính lương. Nhân viên sẽ không thể gửi giải trình bù và Quản lý không thể chỉnh sửa điểm danh của tháng đó nữa.
                                </div>
                            </div>
                        </div>

                        <!-- Box tóm tắt trực quan -->
                        <div class="shift-summary-box" id="shiftLivePreview">
                            💡 <strong>Áp dụng thực tế:</strong> Vào ca sau <strong id="previewLateThreshold">08:30</strong> tính là <strong>Đi muộn (Late)</strong>. Tan ca trước <strong id="previewShiftEnd">17:30</strong> tính là <strong>Về sớm (Early Leave)</strong>. Tính <strong>0.5 công</strong> từ <strong id="previewMinHalf">4.0</strong>h, tính <strong>1.0 công</strong> từ <strong id="previewMinFull">6.0</strong>h. Tự động chốt công tháng trước vào <strong>ngày <span id="previewCutoffDay">05</span></strong> hàng tháng.
                        </div>

                        <!-- Lịch sử cập nhật -->
                        <div id="shiftSettingMetaInfo" class="shift-meta-box">
                            Đang tải thông tin cấu hình hiện tại...
                        </div>
                    </form>
                </div>
                <div class="shift-modal-footer">
                    <button type="button" class="btn-shift-cancel" id="btnCancelShiftModal">Hủy bỏ</button>
                    <button type="button" class="btn-shift-save" id="btnSaveShiftSetting">
                        Lưu cài đặt
                    </button>
                </div>
            </div>
        </div>
    </main>

<script src="../js/attendance.js?v=<%= application.getResource("/js/attendance.js").openConnection().getLastModified() %>"></script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js" defer></script>
</body>
</html>
