<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Điểm danh Nhân sự - HRMs Cloud</title>
    <link rel="stylesheet" href="../css/employee.css?v=<%= application.getResource("/css/employee.css").openConnection().getLastModified() %>">
    <link rel="stylesheet" href="../css/attendance.css?v=<%= application.getResource("/css/attendance.css").openConnection().getLastModified() %>">
    <style>
        /* Scoped styles for Company Shift Settings Modal */
        #modalShiftSetting {
            position: fixed !important;
            top: 0 !important;
            left: 0 !important;
            right: 0 !important;
            bottom: 0 !important;
            width: 100vw !important;
            height: 100vh !important;
            background: rgba(15, 23, 42, 0.65) !important;
            backdrop-filter: blur(4px) !important;
            -webkit-backdrop-filter: blur(4px) !important;
            z-index: 99999 !important;
            display: none;
            align-items: center !important;
            justify-content: center !important;
            padding: 16px !important;
            box-sizing: border-box !important;
        }

        #modalShiftSetting.active,
        #modalManualAttendance.active {
            display: flex !important;
        }

        .shift-modal-overlay .shift-modal-box {
            background: #ffffff !important;
            border: 1px solid #d5dbeb !important;
            border-radius: 16px !important;
            width: 100% !important;
            max-width: 560px !important;
            box-shadow: 0 25px 50px -12px rgba(15, 23, 42, 0.35), 0 0 0 1px rgba(15, 23, 42, 0.05) !important;
            overflow: hidden !important;
            margin: auto !important;
            transform: translateY(0) !important;
            animation: shiftModalIn 0.2s cubic-bezier(0.16, 1, 0.3, 1) !important;
        }

        @keyframes shiftModalIn {
            from { opacity: 0; transform: scale(0.96) translateY(10px); }
            to { opacity: 1; transform: scale(1) translateY(0); }
        }

        .shift-modal-overlay .shift-modal-header {
            display: flex !important;
            align-items: center !important;
            justify-content: space-between !important;
            padding: 18px 24px !important;
            border-bottom: 1px solid #eaecf0 !important;
            background: #ffffff !important;
        }

        .shift-modal-overlay .shift-modal-title {
            margin: 0 !important;
            font-size: 17px !important;
            font-weight: 800 !important;
            color: #062579 !important;
            display: flex !important;
            align-items: center !important;
            gap: 8px !important;
        }

        .shift-modal-overlay .shift-modal-close {
            background: transparent !important;
            border: none !important;
            color: #98a2b3 !important;
            font-size: 26px !important;
            line-height: 1 !important;
            cursor: pointer !important;
            padding: 4px 8px !important;
            border-radius: 6px !important;
            width: auto !important;
            height: auto !important;
            min-width: 0 !important;
            min-height: 0 !important;
            box-shadow: none !important;
            transition: all 0.15s ease !important;
        }

        .shift-modal-overlay .shift-modal-close:hover {
            color: #1d2939 !important;
            background: #f2f4f7 !important;
        }

        .shift-modal-overlay .shift-modal-body {
            padding: 22px 24px !important;
            background: #ffffff !important;
            max-height: calc(85vh - 140px) !important;
            overflow-y: auto !important;
        }

        .shift-modal-overlay .shift-modal-footer {
            display: flex !important;
            align-items: center !important;
            justify-content: flex-end !important;
            gap: 12px !important;
            padding: 16px 24px !important;
            border-top: 1px solid #eaecf0 !important;
            background: #f8fafc !important;
        }

        .shift-modal-overlay .shift-section-title {
            font-size: 12px !important;
            font-weight: 800 !important;
            text-transform: uppercase !important;
            letter-spacing: 0.5px !important;
            color: #475467 !important;
            margin-bottom: 10px !important;
            display: flex !important;
            align-items: center !important;
            gap: 6px !important;
        }

        .shift-modal-overlay .shift-input-card {
            background: #f8faff !important;
            border: 1px solid #e2e8f0 !important;
            border-radius: 10px !important;
            padding: 14px !important;
            margin-bottom: 16px !important;
        }

        .shift-modal-overlay .shift-grid-2 {
            display: grid !important;
            grid-template-columns: 1fr 1fr !important;
            gap: 14px !important;
        }

        .shift-modal-overlay .shift-field-group {
            display: flex !important;
            flex-direction: column !important;
        }

        .shift-modal-overlay .shift-field-group label {
            font-size: 13px !important;
            font-weight: 700 !important;
            color: #344054 !important;
            margin-bottom: 6px !important;
            display: block !important;
        }

        .shift-modal-overlay .shift-field-group input,
        .shift-modal-overlay .shift-field-group select {
            width: 100% !important;
            box-sizing: border-box !important;
            height: 40px !important;
            padding: 8px 12px !important;
            border: 1.5px solid #d0d5dd !important;
            border-radius: 8px !important;
            font-size: 14px !important;
            font-weight: 600 !important;
            color: #101828 !important;
            background: #ffffff !important;
            transition: all 0.15s ease !important;
        }

        .shift-modal-overlay .shift-field-group input:focus,
        .shift-modal-overlay .shift-field-group select:focus {
            border-color: #062579 !important;
            outline: none !important;
            box-shadow: 0 0 0 3px rgba(6, 37, 121, 0.12) !important;
        }

        .shift-modal-overlay .shift-field-hint {
            font-size: 11.5px !important;
            color: #667085 !important;
            margin-top: 5px !important;
            line-height: 1.4 !important;
        }

        .shift-modal-overlay .shift-summary-box {
            background: #eff8ff !important;
            border: 1px solid #b2ddff !important;
            border-radius: 8px !important;
            padding: 12px 14px !important;
            font-size: 12.5px !important;
            color: #175cd3 !important;
            line-height: 1.5 !important;
            margin-top: 4px !important;
        }

        .shift-modal-overlay .shift-meta-box {
            font-size: 12px !important;
            color: #475467 !important;
            background: #f8fafc !important;
            border: 1px solid #eaecf0 !important;
            border-radius: 8px !important;
            padding: 10px 14px !important;
            margin-top: 14px !important;
        }

        #modalShiftSetting .btn-shift-cancel {
            background: #ffffff !important;
            border: 1px solid #d0d5dd !important;
            color: #344054 !important;
            font-size: 13.5px !important;
            font-weight: 600 !important;
            padding: 9px 18px !important;
            border-radius: 8px !important;
            cursor: pointer !important;
            box-shadow: 0 1px 2px rgba(16, 24, 40, 0.05) !important;
            transition: all 0.15s ease !important;
            width: auto !important;
            height: auto !important;
        }

        #modalShiftSetting .btn-shift-cancel:hover {
            background: #f9fafb !important;
            color: #1d2939 !important;
        }

        #modalShiftSetting .btn-shift-save {
            background: #062579 !important;
            border: 1px solid #062579 !important;
            color: #ffffff !important;
            font-size: 13.5px !important;
            font-weight: 700 !important;
            padding: 9px 22px !important;
            border-radius: 8px !important;
            cursor: pointer !important;
            box-shadow: 0 1px 3px rgba(6, 37, 121, 0.25) !important;
            transition: all 0.15s ease !important;
            width: auto !important;
            height: auto !important;
        }

        #modalShiftSetting .btn-shift-save:hover {
            background: #082f9c !important;
            border-color: #082f9c !important;
        }

        #modalShiftSetting .btn-shift-save:disabled {
            opacity: 0.65 !important;
            cursor: not-allowed !important;
        }
    </style>
</head>
<body data-page="manager-attendance">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span><c:out value="${not empty sessionScope.tenantPrincipal.companyName ? sessionScope.tenantPrincipal.companyName : 'TechNova JSC'}"/></span></div>
        <div class="nav-label">Phân hệ Chấm công</div>
        <a class="nav-link" href="../employee/my-profile?nav=manager">Hồ sơ cá nhân</a>
        <a class="nav-link active" href="manager-attendance">Điểm danh Nhân sự</a>
        <a class="nav-link" href="manager-attendance?view=list">Danh sách chấm công</a>
        <a class="nav-link" href="manager-personal-attendance">Chấm công Cá nhân</a>
        <a class="nav-link" href="attendance-report">Báo cáo Chấm công</a>
        <a class="nav-link" href="../payslips">Phiếu lương của tôi</a>
        <a class="nav-link" href="${pageContext.request.contextPath}/payroll">Quản lý lương</a>
        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title">Điểm danh Nhân sự</h1>
            </div>
            <div class="header-actions" style="display:flex; align-items:center; gap:8px;">
                <button class="button secondary" id="btnRefreshList">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;vertical-align:middle;">
                        <polyline points="23 4 23 10 17 10"></polyline>
                        <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                    </svg>
                    Làm mới
                </button>
                <button class="button" id="btnOpenManualModal" style="background:#4338ca; border-color:#4338ca; color:#fff; display:inline-flex; align-items:center; gap:6px;">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                    Điểm danh bổ sung
                </button>
                <button class="button" id="btnOpenShiftSetting" style="background:#0c4a6e; border-color:#0c4a6e; color:#fff; display:inline-flex; align-items:center; gap:6px;">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="3"></circle>
                        <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                    </svg>
                    Cài đặt ca làm việc
                </button>
            </div>
        </div>

        <div class="info-banner" style="margin: 0 28px 16px;">
            <svg width="20" height="20" fill="currentColor" viewBox="0 0 20 20" style="flex-shrink:0;">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"></path>
            </svg>
            <div id="shiftInfoBannerText">
                <strong>Quy định chấm công công ty:</strong> Vào ca sau <strong id="bannerShiftStart">08:30</strong><span id="bannerGraceText"></span> tự động tính là <strong>Đi muộn (Late)</strong>. Tan ca trước <strong id="bannerShiftEnd">17:30</strong> ghi nhận là <strong>Về sớm (Early Leave)</strong>. Tự động khấu trừ <span id="bannerLunchBreak">60</span> phút ăn trưa khi ca làm việc từ 5 tiếng trở lên.
            </div>
        </div>

        <!-- Date & Search Control Bar -->
        <section class="panel" style="margin-bottom: 20px;">
            <div class="filter-bar" style="margin-bottom: 0; background: none; border: none; padding: 0;">
                <div class="filter-group">
                    <label for="teamDate">Ngày điểm danh:</label>
                    <input type="date" id="teamDate">
                </div>
                <div class="filter-actions" style="margin-bottom: 1px;">
                    <button class="button secondary" id="btnSetToday" style="padding: 8px 12px; font-size: 13px;">Hôm nay</button>
                    <button class="button" id="btnFilterTeam" style="padding: 8px 16px; font-size: 13px;">Tải dữ liệu</button>
                </div>
                <div class="filter-group" style="flex: 1; min-width: 220px; margin-left: auto;">
                    <label for="keywordSearch">Tìm kiếm nhanh nhân sự:</label>
                    <input type="text" id="keywordSearch" placeholder="Tìm theo tên, mã NV hoặc phòng ban...">
                </div>
            </div>
        </section>

        <!-- Summary Grid -->
        <section class="summary-grid" style="grid-template-columns: repeat(5, minmax(130px, 1fr)); margin-bottom: 20px;">
            <div class="summary-card">
                <div class="summary-label">Tổng nhân sự</div>
                <div id="totalTeamMembers" class="summary-number">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Có mặt (Đúng giờ)</div>
                <div id="teamPresent" class="summary-number text-success">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Đi muộn</div>
                <div id="teamLate" class="summary-number text-warning">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Về sớm</div>
                <div id="teamEarly" class="summary-number" style="color:#3538cd;">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label">Chưa vào ca / Vắng</div>
                <div id="teamNotIn" class="summary-number text-danger">0</div>
            </div>
        </section>

        <div id="message" class="message" style="margin: 0 28px 16px;"></div>

        <!-- Main Panel with Role Navbar & Batch Completion -->
        <section class="panel">
            <!-- Small Role Navbar Tabs -->
            <div class="role-tabs-container">
                <div class="role-tabs" id="roleTabs">
                    <button class="role-tab active" data-role="ALL">
                        Tất cả
                        <span class="count-badge" id="countAll">0</span>
                    </button>
                    <button class="role-tab" data-role="MANAGER">
                        Quản lý
                        <span class="count-badge" id="countManager">0</span>
                    </button>
                    <button class="role-tab" data-role="EMPLOYEE">
                        Nhân viên (Employee)
                        <span class="count-badge" id="countEmployee">0</span>
                    </button>
                </div>

                <div style="font-size: 13px; color: #475467;">
                    Đang hiển thị: <strong id="filterStatusText" style="color: #062579;">Tất cả nhân sự</strong>
                </div>
            </div>

            <!-- Batch Action Toolbar -->
            <div class="batch-toolbar">
                <div class="batch-controls">
                    <label class="batch-select-all">
                        <input type="checkbox" id="selectAllCheckbox">
                        <span>Chọn tất cả (<span id="selectedCountDisplay">0</span> đã chọn)</span>
                    </label>
                </div>

                <button class="button btn-complete-batch" id="btnCompleteAttendance" disabled>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                        <polyline points="22 4 12 14.01 9 11.01"></polyline>
                    </svg>
                    Hoàn tất việc check điểm danh
                </button>
            </div>

            <!-- Team / All Employees Table -->
            <div style="overflow-x:auto;">
                <table>
                    <thead>
                    <tr>
                        <th class="checkbox-col">#</th>
                        <th>Mã NV</th>
                        <th>Nhân viên</th>
                        <th>Vai trò</th>
                        <th>Vào ca</th>
                        <th>Tan ca</th>
                        <th>Tổng giờ</th>
                        <th>Trạng thái</th>
                        <th>Ghi chú</th>
                        <th style="min-width: 230px; text-align: center;">Thao tác Điểm danh</th>
                    </tr>
                    </thead>
                    <tbody id="teamRows">
                        <tr><td colspan="10" style="text-align:center;color:#667085;padding:32px;">Đang tải danh sách toàn bộ nhân sự...</td></tr>
                    </tbody>
                </table>
            </div>
        </section>

        <!-- Modal Cài đặt Giờ giấc làm việc cho Công ty (CompanyShiftSettings) -->
        <div id="modalShiftSetting" class="shift-modal-overlay">
            <div class="shift-modal-box">
                <div class="shift-modal-header">
                    <h3 class="shift-modal-title">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#062579" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="12" cy="12" r="10"></circle>
                            <polyline points="12 6 12 12 16 14"></polyline>
                        </svg>
                        Cài đặt giờ giấc làm việc công ty
                    </h3>
                    <button class="shift-modal-close" id="btnCloseShiftModal" type="button" aria-label="Đóng">&times;</button>
                </div>
                <div class="shift-modal-body">
                    <p style="font-size: 13px; color: #475467; margin-top: 0; margin-bottom: 16px; line-height: 1.5;">
                        Cấu hình khung giờ chuẩn cho toàn bộ nhân sự công ty. Hệ thống sẽ tự động đối chiếu giờ quẹt thẻ với các mốc này để tính <strong>Đi muộn</strong>, <strong>Về sớm</strong> và <strong>Khấu trừ nghỉ trưa</strong>.
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

                        <!-- Box tóm tắt trực quan -->
                        <div class="shift-summary-box" id="shiftLivePreview">
                            💡 <strong>Áp dụng thực tế:</strong> Vào ca sau <strong id="previewLateThreshold">08:30</strong> tính là <strong>Đi muộn (Late)</strong>. Tan ca trước <strong id="previewShiftEnd">17:30</strong> tính là <strong>Về sớm (Early Leave)</strong>.
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

        <!-- Modal Điểm danh bổ sung cho bất kỳ ngày nào -->
        <div id="modalManualAttendance" class="shift-modal-overlay" style="display:none;">
            <div class="shift-modal-box" style="max-width: 600px;">
                <div class="shift-modal-header">
                    <h3 class="shift-modal-title" style="color: #4338ca;">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                        </svg>
                        Điểm danh bổ sung / Điều chỉnh công
                    </h3>
                    <button class="shift-modal-close" id="btnCloseManualModal" type="button" aria-label="Đóng">&times;</button>
                </div>
                <div class="shift-modal-body">
                    <p style="font-size: 13px; color: #475467; margin-top: 0; margin-bottom: 16px; line-height: 1.5;">
                        Quản lý có thể ghi nhận hoặc điều chỉnh giờ vào ca, tan ca cho nhân sự vào <strong>bất kỳ ngày nào</strong> (quên quẹt thẻ, công tác ngoài, sửa công).
                    </p>
                    <div id="manualModalMessage" class="message" style="display:none; margin: 0 0 16px 0;"></div>
                    <form id="formManualAttendance" onsubmit="return false;">
                        <input type="hidden" name="organizationCsrfToken" value="${organizationCsrfToken}">

                        <div class="shift-input-card">
                            <div class="shift-grid-2">
                                <div class="shift-field-group">
                                    <label for="manualDate">Ngày điểm danh bổ sung: <span style="color:#d92d20;">*</span></label>
                                    <input type="date" id="manualDate" name="date" required>
                                    <div class="shift-field-hint">Chọn bất kỳ ngày nào cần ghi nhận công.</div>
                                </div>
                                <div class="shift-field-group">
                                    <label for="manualEmployeeSelect">Nhân sự: <span style="color:#d92d20;">*</span></label>
                                    <select id="manualEmployeeSelect" name="employeeId" required style="width:100%; height:40px; padding:8px 12px; border:1.5px solid #d0d5dd; border-radius:8px; font-size:14px; font-weight:600; color:#101828; background:#fff;">
                                        <option value="">-- Đang tải nhân sự... --</option>
                                    </select>
                                    <div class="shift-field-hint">Chọn nhân viên cần bổ sung / sửa công.</div>
                                </div>
                            </div>
                        </div>

                        <div class="shift-input-card">
                            <div class="shift-grid-2">
                                <div class="shift-field-group">
                                    <label for="manualCheckInTime">Giờ vào ca (Check-in):</label>
                                    <input type="time" id="manualCheckInTime" name="checkInTime" step="1">
                                    <div class="shift-field-hint">Giờ nhân viên bắt đầu làm việc.</div>
                                </div>
                                <div class="shift-field-group">
                                    <label for="manualCheckOutTime">Giờ tan ca (Check-out):</label>
                                    <input type="time" id="manualCheckOutTime" name="checkOutTime" step="1">
                                    <div class="shift-field-hint">Giờ nhân viên kết thúc làm việc.</div>
                                </div>
                            </div>
                        </div>

                        <!-- Preview tính toán công tự động -->
                        <div class="shift-summary-box" id="manualLivePreview" style="background:#f4ebff; border-color:#d6bbfb; color:#5925dc;">
                            <div style="font-weight:700; margin-bottom:4px;">📊 Dự tính công & trạng thái:</div>
                            <div id="manualPreviewDetails" style="font-size:13px; line-height:1.6;">
                                Vui lòng nhập giờ vào ca hoặc giờ tan ca để xem dự tính.
                            </div>
                        </div>

                        <div class="shift-field-group" style="margin-top:14px;">
                            <label for="manualNote">Lý do / Ghi chú bổ sung:</label>
                            <input type="text" id="manualNote" name="note" placeholder="Ví dụ: Quên quẹt thẻ sáng, Được duyệt bổ sung, Đi công tác ngoại kiểm..." style="width:100%; height:40px; padding:8px 12px; border:1.5px solid #d0d5dd; border-radius:8px; font-size:14px;">
                        </div>
                    </form>
                </div>
                <div class="shift-modal-footer">
                    <button type="button" class="btn-shift-cancel" id="btnCancelManualModal">Hủy bỏ</button>
                    <button type="button" class="button" id="btnSaveManualAttendance" style="background:#4338ca; border-color:#4338ca; color:#fff; font-weight:700; padding:9px 18px; border-radius:8px; display:inline-flex; align-items:center; gap:6px;">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                            <polyline points="20 6 9 17 4 12"></polyline>
                        </svg>
                        Lưu điểm danh bổ sung
                    </button>
                </div>
            </div>
        </div>
    </main>
</div>
<script src="../js/attendance.js?v=<%= application.getResource("/js/attendance.js").openConnection().getLastModified() %>"></script>
<script src="../js/payroll-notifications.js"></script>
</body>
</html>