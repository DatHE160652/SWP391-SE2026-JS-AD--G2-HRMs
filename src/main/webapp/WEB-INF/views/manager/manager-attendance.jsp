<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_e22cba8ce2ed", "Điểm danh Nhân sự - HRMs Cloud") %></title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/attendance.css?v=<%= application.getResource("/css/attendance.css").openConnection().getLastModified() %>">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc" data-page="manager-attendance">
<c:set var="managerNavPage" value="team-attendance"/>
<c:set var="managerNavTitle" value="Điểm danh Nhân sự"/>
<%@ include file="/WEB-INF/views/common/manager-shell.jspf" %>

    <main class="lc-main content">
        <div class="lc-breadcrumb"><%= com.hrms.i18n.Language.text(request, "ui_0b07ea439ad0", "Cổng quản lý &nbsp; / &nbsp; Điểm danh Nhân sự") %></div>

        <div class="page-header">
            <div>
                <h1 class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_cc34d4e67fde", "Điểm danh Nhân sự") %></h1>
                <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_14e42ec301ec", "Quản lý và theo dõi điểm danh hàng ngày của nhân sự phòng ban, thực hiện chấm công bổ sung và duyệt giải trình công.") %></p>
            </div>
            <div class="header-actions" style="display:flex; align-items:center; gap:8px;">
                <button class="button secondary" id="btnRefreshList" type="button">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;vertical-align:middle;">
                        <polyline points="23 4 23 10 17 10"></polyline>
                        <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                    </svg>
                    <%= com.hrms.i18n.Language.text(request, "ui_61b0561227b1", "Làm mới") %>
                </button>
                <button class="button" id="btnOpenManualModal" type="button" style="background:#4338ca; border-color:#4338ca; color:#fff; display:inline-flex; align-items:center; gap:6px;">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                    <%= com.hrms.i18n.Language.text(request, "ui_63cab52471c3", "Điểm danh bổ sung") %>
                </button>
                <button class="button" id="btnOpenExplanationsModal" type="button" style="background:#059669; border-color:#059669; color:#fff; display:inline-flex; align-items:center; gap:6px;">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                        <polyline points="14 2 14 8 20 8"></polyline>
                        <line x1="16" y1="13" x2="8" y2="13"></line>
                        <line x1="16" y1="17" x2="8" y2="17"></line>
                    </svg>
                    <%= com.hrms.i18n.Language.text(request, "ui_cf34cf9988c2", "Duyệt giải trình công") %>
                    <span id="badgePendingExplanations" style="display:none; background:#ef4444; color:#fff; font-size:11px; font-weight:700; padding:2px 6px; border-radius:10px;">0</span>
                </button>
            </div>
        </div>

        <div class="info-banner">
            <svg width="20" height="20" fill="currentColor" viewBox="0 0 20 20" style="flex-shrink:0;">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"></path>
            </svg>
            <div id="shiftInfoBannerText">
                <strong><%= com.hrms.i18n.Language.text(request, "ui_1450eda07b53", "Quy định chấm công công ty:") %></strong> <%= com.hrms.i18n.Language.text(request, "ui_983a50cfcfb4", "Vào ca sau") %> <strong id="bannerShiftStart">08:30</strong><span id="bannerGraceText"></span> <%= com.hrms.i18n.Language.text(request, "ui_1c410cabb769", "tự động tính là") %> <strong><%= com.hrms.i18n.Language.text(request, "ui_3b8c8406a25a", "Đi muộn (Late)") %></strong><%= com.hrms.i18n.Language.text(request, "ui_5bedd1151f48", ". Tan ca trước") %> <strong id="bannerShiftEnd">17:30</strong> <%= com.hrms.i18n.Language.text(request, "ui_48782f077672", "ghi nhận là") %> <strong><%= com.hrms.i18n.Language.text(request, "ui_0999ea936c8e", "Về sớm (Early Leave)") %></strong><%= com.hrms.i18n.Language.text(request, "ui_6dd2633baeb6", ". Tự động khấu trừ") %> <span id="bannerLunchBreak">60</span> <%= com.hrms.i18n.Language.text(request, "ui_0cff4147668d", "phút ăn trưa khi ca làm việc từ 5 tiếng trở lên.") %>
            </div>
        </div>

        <!-- Date & Search Control Bar -->
        <section class="panel" style="margin-bottom: 20px;">
            <div class="filter-bar" style="margin-bottom: 0; background: none; border: none; padding: 0;">
                <div class="filter-group">
                    <label for="teamDate"><%= com.hrms.i18n.Language.text(request, "ui_7be80fabfaa1", "Ngày điểm danh:") %></label>
                    <input type="date" id="teamDate">
                </div>
                <div class="filter-actions">
                    <button class="button secondary" id="btnSetToday" type="button"><%= com.hrms.i18n.Language.text(request, "ui_0048b6a408e3", "Hôm nay") %></button>
                    <button class="button" id="btnFilterTeam" type="button"><%= com.hrms.i18n.Language.text(request, "ui_6197dfe37f8a", "Tải dữ liệu") %></button>
                </div>
                <div class="filter-group" style="flex: 1; min-width: 220px; margin-left: auto;">
                    <label for="keywordSearch"><%= com.hrms.i18n.Language.text(request, "ui_b1f3aa77cfda", "Tìm kiếm nhanh nhân sự:") %></label>
                    <input type="text" id="keywordSearch" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_a1460f76f881", "Tìm theo tên, mã NV hoặc phòng ban...") %>">
                </div>
            </div>
        </section>

        <!-- Summary Grid -->
        <section class="summary-grid" style="grid-template-columns: repeat(5, minmax(130px, 1fr)); margin-bottom: 20px;">
            <div class="summary-card">
                <div class="summary-label"><%= com.hrms.i18n.Language.text(request, "ui_635e8a4e6a93", "Tổng nhân sự") %></div>
                <div id="totalTeamMembers" class="summary-number">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label"><%= com.hrms.i18n.Language.text(request, "ui_b4d36be65371", "Có mặt (Đúng giờ)") %></div>
                <div id="teamPresent" class="summary-number text-success">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label"><%= com.hrms.i18n.Language.text(request, "ui_d46cffe2fec0", "Đi muộn") %></div>
                <div id="teamLate" class="summary-number text-warning">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label"><%= com.hrms.i18n.Language.text(request, "ui_fc9579195fc6", "Về sớm") %></div>
                <div id="teamEarly" class="summary-number" style="color:#3538cd;">0</div>
            </div>
            <div class="summary-card">
                <div class="summary-label"><%= com.hrms.i18n.Language.text(request, "ui_acdcb3a006fa", "Chưa vào ca / Vắng") %></div>
                <div id="teamNotIn" class="summary-number text-danger">0</div>
            </div>
        </section>

        <div id="message" class="message"></div>

        <!-- Main Panel with Role Navbar & Batch Completion -->
        <section class="panel">
            <!-- Small Role Navbar Tabs -->
            <div class="role-tabs-container">
                <div class="role-tabs" id="roleTabs">
                    <button class="role-tab active" data-role="ALL" type="button">
                        <%= com.hrms.i18n.Language.text(request, "ui_f7a578dcbdca", "Tất cả") %>
                        <span class="count-badge" id="countAll">0</span>
                    </button>
                    <button class="role-tab" data-role="MANAGER" type="button">
                        <%= com.hrms.i18n.Language.text(request, "ui_320c0d21d79a", "Quản lý") %>
                        <span class="count-badge" id="countManager">0</span>
                    </button>
                    <button class="role-tab" data-role="EMPLOYEE" type="button">
                        <%= com.hrms.i18n.Language.text(request, "ui_8e5b2d01c5dd", "Nhân viên (Employee)") %>
                        <span class="count-badge" id="countEmployee">0</span>
                    </button>
                </div>

                <div style="font-size: 13px; color: #475467;">
                    <%= com.hrms.i18n.Language.text(request, "ui_6aea424ced86", "Đang hiển thị:") %> <strong id="filterStatusText" style="color: #062579;"><%= com.hrms.i18n.Language.text(request, "ui_1bba50e3b189", "Tất cả nhân sự") %></strong>
                </div>
            </div>

            <!-- Batch Action Toolbar -->
            <div class="batch-toolbar">
                <div class="batch-controls">
                    <label class="batch-select-all">
                        <input type="checkbox" id="selectAllCheckbox">
                        <span><%= com.hrms.i18n.Language.text(request, "ui_1a6ff135ac78", "Chọn tất cả (") %><span id="selectedCountDisplay">0</span> <%= com.hrms.i18n.Language.text(request, "ui_9f4e58d9643d", "đã chọn)") %></span>
                    </label>
                </div>

                <div class="batch-action-buttons" style="display:flex; align-items:center; gap:8px;">
                    <!-- Nút 1: Vào ca hàng loạt (Check-in) -->
                    <button class="button btn-batch-checkin" id="btnBatchCheckIn" type="button" disabled
                            title="<%= com.hrms.i18n.Language.text(request, "ui_39a7c05adabb", "Điểm danh Vào ca cho các nhân sự chưa vào ca trong danh sách đã chọn") %>">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path>
                            <polyline points="10 17 15 12 10 7"></polyline>
                            <line x1="15" y1="12" x2="3" y2="12"></line>
                        </svg>
                        <%= com.hrms.i18n.Language.text(request, "ui_2c87404562d3", "Vào ca hàng loạt") %>
                    </button>

                    <!-- Nút 2: Tan ca hàng loạt (Check-out) -->
                    <button class="button btn-batch-checkout" id="btnBatchCheckOut" type="button" disabled
                            title="<%= com.hrms.i18n.Language.text(request, "ui_ae84cf3e8835", "Điểm danh Tan ca cho các nhân sự đang làm việc trong danh sách đã chọn") %>">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                            <polyline points="16 17 21 12 16 7"></polyline>
                            <line x1="21" y1="12" x2="9" y2="12"></line>
                        </svg>
                        <%= com.hrms.i18n.Language.text(request, "ui_cbbabff49ae4", "Tan ca hàng loạt") %>
                    </button>
                </div>
            </div>

            <!-- Team / All Employees Table -->
            <div style="overflow-x:auto;">
                <table>
                    <thead>
                    <tr>
                        <th class="checkbox-col">#</th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_c6f0f81cc2b0", "Mã NV") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_14ddd70d2564", "Nhân viên") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_29535363a737", "Vai trò") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_3d8042db8e05", "Vào ca") %></th>
                        <th>Tan ca</th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_bf5e3203822b", "Tổng giờ") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_02f03f68e75d", "Ghi chú") %></th>
                        <th style="min-width: 230px; text-align: center;"><%= com.hrms.i18n.Language.text(request, "ui_0532ff109b15", "Thao tác Điểm danh") %></th>
                    </tr>
                    </thead>
                    <tbody id="teamRows">
                        <tr><td colspan="10" style="text-align:center;color:#667085;padding:32px;"><%= com.hrms.i18n.Language.text(request, "ui_0f9657ad71ee", "Đang tải danh sách toàn bộ nhân sự...") %></td></tr>
                    </tbody>
                </table>
            </div>
        </section>

        <!-- Modal Điểm danh bổ sung cho bất kỳ ngày nào -->
        <div id="modalManualAttendance" class="shift-modal-overlay" style="display:none;">
            <div class="shift-modal-box" style="max-width: 600px;">
                <div class="shift-modal-header">
                    <h3 class="shift-modal-title" style="color: #4338ca;">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                        </svg>
                        <%= com.hrms.i18n.Language.text(request, "ui_d6d605917ba6", "Điểm danh bổ sung / Điều chỉnh công") %>
                    </h3>
                    <button class="shift-modal-close" id="btnCloseManualModal" type="button" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %>">&times;</button>
                </div>
                <div class="shift-modal-body">
                    <p style="font-size: 13px; color: #475467; margin-top: 0; margin-bottom: 16px; line-height: 1.5;">
                        <%= com.hrms.i18n.Language.text(request, "ui_b52946ba30b5", "Quản lý ghi nhận hoặc điều chỉnh giờ làm việc cho nhân sự trong ngày hôm nay. Để bổ sung công cho các ngày trước đó, nhân viên cần gửi") %> <strong><%= com.hrms.i18n.Language.text(request, "ui_d51751d9bda3", "Đơn giải trình chấm công") %></strong> <%= com.hrms.i18n.Language.text(request, "ui_7911759325a8", "để được xét duyệt.") %>
                    </p>
                    <div id="manualModalMessage" class="message" style="display:none; margin: 0 0 16px 0;"></div>
                    <form id="formManualAttendance" onsubmit="return false;">
                        <input type="hidden" name="organizationCsrfToken" value="${organizationCsrfToken}">

                        <div class="shift-input-card">
                            <div class="shift-grid-2">
                                <div class="shift-field-group">
                                    <label><%= com.hrms.i18n.Language.text(request, "ui_46a74d2ebd4b", "Ngày điểm danh bổ sung:") %></label>
                                    <div class="fixed-date-badge" style="display:flex; align-items:center; gap:8px; height:40px; padding:0 12px; background:#f8f9fa; border:1.5px solid #d0d5dd; border-radius:8px; font-weight:700; color:#1e293b;">
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#4338ca" stroke-width="2.2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                                        <span id="manualDateDisplay"><%= com.hrms.i18n.Language.text(request, "ui_0048b6a408e3", "Hôm nay") %></span>
                                        <span class="badge" style="background:#e0e7ff; color:#3730a3; font-size:11px; padding:2px 8px; border-radius:6px; margin-left:auto;"><%= com.hrms.i18n.Language.text(request, "ui_39a64b01f527", "Cố định trong ngày") %></span>
                                    </div>
                                    <input type="hidden" id="manualDate" name="date" value="">
                                    <div class="shift-field-hint"><%= com.hrms.i18n.Language.text(request, "ui_8d428b34ba1f", "Áp dụng trực tiếp cho ngày hôm nay. Ngày quá khứ vui lòng duyệt Đơn giải trình.") %></div>
                                </div>
                                <div class="shift-field-group">
                                    <label for="manualEmployeeSelect"><%= com.hrms.i18n.Language.text(request, "ui_67565ef6fe0a", "Nhân sự:") %> <span style="color:#d92d20;">*</span></label>
                                    <select id="manualEmployeeSelect" name="employeeId" required style="width:100%; height:40px; padding:8px 12px; border:1.5px solid #d0d5dd; border-radius:8px; font-size:14px; font-weight:600; color:#101828; background:#fff;">
                                        <option value=""><%= com.hrms.i18n.Language.text(request, "ui_b716defd2991", "-- Đang tải nhân sự... --") %></option>
                                    </select>
                                    <div class="shift-field-hint"><%= com.hrms.i18n.Language.text(request, "ui_7dd83129517e", "Chọn nhân viên cần bổ sung / sửa công.") %></div>
                                </div>
                            </div>
                        </div>

                        <div class="shift-input-card">
                            <div class="shift-grid-2">
                                <div class="shift-field-group">
                                    <label for="manualCheckInTime"><%= com.hrms.i18n.Language.text(request, "ui_cf5fe78772db", "Giờ vào ca (Check-in):") %></label>
                                    <input type="time" id="manualCheckInTime" name="checkInTime" step="1">
                                    <div class="shift-field-hint"><%= com.hrms.i18n.Language.text(request, "ui_be93958b7543", "Giờ nhân viên bắt đầu làm việc.") %></div>
                                </div>
                                <div class="shift-field-group">
                                    <label for="manualCheckOutTime"><%= com.hrms.i18n.Language.text(request, "ui_e4307032e392", "Giờ tan ca (Check-out):") %></label>
                                    <input type="time" id="manualCheckOutTime" name="checkOutTime" step="1">
                                    <div class="shift-field-hint"><%= com.hrms.i18n.Language.text(request, "ui_5740605c7a30", "Giờ nhân viên kết thúc làm việc.") %></div>
                                </div>
                            </div>
                        </div>

                        <!-- Preview tính toán công tự động -->
                        <div class="shift-summary-box" id="manualLivePreview" style="background:#f4ebff; border-color:#d6bbfb; color:#5925dc;">
                            <div style="font-weight:700; margin-bottom:4px;"><%= com.hrms.i18n.Language.text(request, "ui_999d70bbc0a4", "📊 Dự tính công & trạng thái:") %></div>
                            <div id="manualPreviewDetails" style="font-size:13px; line-height:1.6;">
                                <%= com.hrms.i18n.Language.text(request, "ui_5f1957ad6650", "Vui lòng nhập giờ vào ca hoặc giờ tan ca để xem dự tính.") %>
                            </div>
                        </div>

                        <div class="shift-field-group" style="margin-top:14px;">
                            <label for="manualNote"><%= com.hrms.i18n.Language.text(request, "ui_565ad306045e", "Lý do / Ghi chú bổ sung:") %></label>
                            <input type="text" id="manualNote" name="note" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_2ada64798f13", "Ví dụ: Quên quẹt thẻ sáng, Được duyệt bổ sung, Đi công tác ngoại kiểm...") %>" style="width:100%; height:40px; padding:8px 12px; border:1.5px solid #d0d5dd; border-radius:8px; font-size:14px;">
                        </div>
                    </form>
                </div>
                <div class="shift-modal-footer">
                    <button type="button" class="btn-shift-cancel" id="btnCancelManualModal"><%= com.hrms.i18n.Language.text(request, "ui_247d4b1efe9c", "Hủy bỏ") %></button>
                    <button type="button" class="button" id="btnSaveManualAttendance" style="background:#4338ca; border-color:#4338ca; color:#fff; font-weight:700; padding:9px 18px; border-radius:8px; display:inline-flex; align-items:center; gap:6px;">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                            <polyline points="20 6 9 17 4 12"></polyline>
                        </svg>
                        <%= com.hrms.i18n.Language.text(request, "ui_482f5f9c20a3", "Lưu điểm danh bổ sung") %>
                    </button>
                </div>
            </div>
        </div>

        <!-- Modal UC87: Quản lý Duyệt Đơn Giải Trình Chấm Công -->
        <div id="modalExplanations" class="shift-modal-overlay" style="display:none;">
            <div class="shift-modal-box" style="max-width: 900px; width: 95%;">
                <div class="shift-modal-header" style="display:flex; justify-content:space-between; align-items:center;">
                    <div>
                        <h3 class="shift-modal-title" style="margin:0 0 4px; font-size:18px; font-weight:700; color:#059669;">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" style="vertical-align:middle;margin-right:4px;">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                <polyline points="14 2 14 8 20 8"></polyline>
                                <line x1="16" y1="13" x2="8" y2="13"></line>
                                <line x1="16" y1="17" x2="8" y2="17"></line>
                            </svg>
                            <%= com.hrms.i18n.Language.text(request, "ui_b9393e2552a0", "Duyệt đơn giải trình chấm công") %>
                        </h3>
                        <div style="font-size:13px; color:#475467;"><%= com.hrms.i18n.Language.text(request, "ui_ae7836495182", "Xem xét và phê duyệt các yêu cầu xin bổ sung/giải trình công từ nhân viên cấp dưới.") %></div>
                    </div>
                    <button type="button" class="btn-close" id="btnCloseExplanationsModal" style="background:none; border:none; font-size:24px; cursor:pointer; color:#667085;">&times;</button>
                </div>
                <div class="shift-modal-body" style="padding: 20px; max-height: 65vh; overflow-y: auto;">
                    <div id="explanationAlertBox" style="display:none; padding:10px 14px; border-radius:8px; margin-bottom:14px;"></div>
                    <table style="width:100%; border-collapse:collapse;">
                        <thead>
                            <tr style="border-bottom:2px solid #eaecf0; text-align:left; font-size:13px; color:#475467;">
                                <th style="padding:10px 8px;"><%= com.hrms.i18n.Language.text(request, "ui_14ddd70d2564", "Nhân viên") %></th>
                                <th style="padding:10px 8px;"><%= com.hrms.i18n.Language.text(request, "ui_e2caeb5a707b", "Ngày giải trình") %></th>
                                <th style="padding:10px 8px;"><%= com.hrms.i18n.Language.text(request, "ui_ecdb468a6da0", "Lý do") %></th>
                                <th style="padding:10px 8px;"><%= com.hrms.i18n.Language.text(request, "ui_34747a0262eb", "Giờ đề xuất") %></th>
                                <th style="padding:10px 8px;"><%= com.hrms.i18n.Language.text(request, "ui_6e743f1007b9", "Chi tiết lý do") %></th>
                                <th style="padding:10px 8px; text-align:center;"><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th>
                            </tr>
                        </thead>
                        <tbody id="pendingExplanationsRows">
                            <tr><td colspan="6" style="text-align:center; color:#667085; padding:24px;"><%= com.hrms.i18n.Language.text(request, "ui_5d570fda5c5f", "Đang tải danh sách chờ duyệt...") %></td></tr>
                        </tbody>
                    </table>
                </div>
                <div class="shift-modal-footer" style="padding: 14px 20px; background:#f9fafb; border-top:1px solid #eaecf0; display:flex; justify-content:flex-end;">
                    <button type="button" class="btn-shift-cancel" id="btnCloseExplanationsBottom"><%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %></button>
                </div>
            </div>
        </div>
    </main>

<script src="../js/attendance.js?v=<%= application.getResource("/js/attendance.js").openConnection().getLastModified() %>"></script>
<script src="../js/payroll-notifications.js?v=<%= application.getResource("/js/payroll-notifications.js").openConnection().getLastModified() %>"></script>
</body>
</html>
