<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_df12fb74e968", "Quản lý Đơn nghỉ phép Toàn công ty - HRMs Cloud") %></title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
    <link rel="stylesheet" href="../css/leave.css">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc" data-page="leave-all-requests">
<div class="workspace-shell">
    <%@ include file="/WEB-INF/views/common/tenant-shell.jspf" %>

    <main class="lc-main content">
        <div class="page-header">
            <div>
                <h1 class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_abe194b53e34", "Quản lý Đơn nghỉ phép Toàn công ty") %></h1>
                <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_f09197972c59", "Giám sát toàn bộ các đơn xin nghỉ phép trong doanh nghiệp, tìm kiếm và phân loại theo trạng thái hoặc phòng ban.") %></p>
            </div>
            <div class="header-actions" style="display:flex;gap:10px;">
                <button class="button secondary" onclick="window.LeaveAppReloadAll()"><%= com.hrms.i18n.Language.text(request, "ui_61b0561227b1", "Làm mới") %></button>
                <button class="button" style="background:#107c41;color:#fff;display:inline-flex;align-items:center;gap:6px;border:none;cursor:pointer;" onclick="window.exportLeaveRequests()">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                    <%= com.hrms.i18n.Language.text(request, "ui_1721265dcbc2", "Xuất Excel") %>
                </button>
            </div>
        </div>

        <!-- Filter Bar -->
        <section class="panel" style="margin-bottom: 20px;">
            <div class="filter-bar" style="margin-bottom: 0; background: none; border: none; padding: 0;">
                <div class="filter-group" style="min-width: 160px;">
                    <label for="statusFilter"><%= com.hrms.i18n.Language.text(request, "ui_d7b8829cb1d0", "Trạng thái:") %></label>
                    <select id="statusFilter">
                        <option value="ALL"><%= com.hrms.i18n.Language.text(request, "ui_045c53831ced", "-- Tất cả trạng thái --") %></option>
                        <option value="Pending"><%= com.hrms.i18n.Language.text(request, "ui_98429b58be9b", "Pending (Chờ duyệt)") %></option>
                        <option value="Approved"><%= com.hrms.i18n.Language.text(request, "ui_1165d8820535", "Đã duyệt") %></option>
                        <option value="Rejected"><%= com.hrms.i18n.Language.text(request, "ui_39d4bd690bde", "Rejected (Bị từ chối)") %></option>
                        <option value="Cancelled"><%= com.hrms.i18n.Language.text(request, "ui_fe90dc3bca71", "Đã hủy") %></option>
                    </select>
                </div>
                <div class="filter-group" style="min-width: 180px;">
                    <label for="deptFilter"><%= com.hrms.i18n.Language.text(request, "ui_325e08e78a74", "Phòng ban:") %></label>
                    <select id="deptFilter">
                        <option value=""><%= com.hrms.i18n.Language.text(request, "ui_5bf8b98ac9a0", "-- Tất cả phòng ban --") %></option>
                    </select>
                </div>
                <div class="filter-group">
                    <label for="fromDate"><%= com.hrms.i18n.Language.text(request, "ui_5688d659d36a", "Từ ngày:") %></label>
                    <input type="date" id="fromDate">
                </div>
                <div class="filter-group">
                    <label for="toDate"><%= com.hrms.i18n.Language.text(request, "ui_cb6416e9abf5", "Đến ngày:") %></label>
                    <input type="date" id="toDate">
                </div>
                <div class="filter-group" style="flex: 1; min-width: 200px;">
                    <label for="keywordInput"><%= com.hrms.i18n.Language.text(request, "ui_796185cae8a4", "Tìm kiếm nhân viên:") %></label>
                    <input type="text" id="keywordInput" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_be4a48c96275", "Nhập tên hoặc mã nhân viên...") %>">
                </div>
                <div class="filter-actions" style="margin-bottom: 1px;">
                    <button class="button" id="btnFilter" style="padding: 8px 16px;"><%= com.hrms.i18n.Language.text(request, "ui_85b9732b1d5b", "Lọc dữ liệu") %></button>
                </div>
            </div>
        </section>

        <!-- All Requests Table -->
        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th style="width: 70px;"><%= com.hrms.i18n.Language.text(request, "ui_e67a0987de5b", "Mã đơn") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_f3d81e4e1b96", "Nhân sự") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_1389e04eccd7", "Loại nghỉ phép") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_f3aae68524b6", "Khoảng thời gian") %></th>
                    <th style="width: 100px;"><%= com.hrms.i18n.Language.text(request, "ui_c10472ab6cfb", "Số ngày") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_ecdb468a6da0", "Lý do") %></th>
                    <th style="width: 120px;"><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_2dbc1871b054", "Người duyệt") %></th>
                    <th style="width: 140px; text-align: center;"><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th>
                </tr>
                </thead>
                <tbody id="allRequestsTableBody">
                    <tr><td colspan="9" style="text-align: center; color: #667085; padding: 24px;"><%= com.hrms.i18n.Language.text(request, "ui_d37b2a68bf44", "Đang tải danh sách...") %></td></tr>
                </tbody>
            </table>

        </section>
    </main>
</div>

<!-- Modal Details -->
<div id="modalDetail" class="modal-overlay">
    <div class="modal-box">
        <div class="modal-header">
            <h3 class="modal-title"><%= com.hrms.i18n.Language.text(request, "ui_da3b9bc3801a", "Chi tiết đơn xin nghỉ phép") %></h3>
            <button class="modal-close" onclick="LeaveApp.closeModal('modalDetail')">&times;</button>
        </div>
        <div class="modal-body" id="detailModalBody"></div>
        <div class="modal-footer">
            <button class="button secondary" onclick="LeaveApp.closeModal('modalDetail')"><%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %></button>
        </div>
    </div>
</div>

<!-- Modal Approve Confirmation -->
<div id="modalApprove" class="modal-overlay">
    <div class="modal-box">
        <div class="modal-header">
            <h3 class="modal-title"><%= com.hrms.i18n.Language.text(request, "ui_535d279f720e", "Xác nhận phê duyệt đơn nghỉ phép") %></h3>
            <button class="modal-close" onclick="LeaveApp.closeModal('modalApprove')">&times;</button>
        </div>
        <div class="modal-body">
            <input type="hidden" id="approveReqId">
            <p><%= com.hrms.i18n.Language.text(request, "ui_9f88ef38e638", "Bạn có chắc chắn muốn phê duyệt đơn nghỉ phép của nhân sự") %> <strong id="approveEmpName"></strong>?</p>
            <div style="margin-top: 14px;">
                <label class="form-label" for="approveComment"><%= com.hrms.i18n.Language.text(request, "ui_4fe3b63adcab", "Ý kiến phê duyệt (tùy chọn):") %></label>
                <textarea id="approveComment" class="form-control" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_8b290a5daf8a", "Ghi chú thêm nếu có...") %>"></textarea>
            </div>
        </div>
        <div class="modal-footer">
            <button class="button secondary" onclick="LeaveApp.closeModal('modalApprove')"><%= com.hrms.i18n.Language.text(request, "ui_34ca764caf22", "Hủy") %></button>
            <button id="btnConfirmApprove" class="button" style="background:#027a48;border-color:#027a48;" onclick="LeaveApp.confirmApprove()"><%= com.hrms.i18n.Language.text(request, "ui_21234cc570e8", "Xác nhận duyệt") %></button>
        </div>
    </div>
</div>

<!-- Modal Reject Confirmation -->
<div id="modalReject" class="modal-overlay">
    <div class="modal-box">
        <div class="modal-header">
            <h3 class="modal-title"><%= com.hrms.i18n.Language.text(request, "ui_36d6f60b591e", "Từ chối đơn xin nghỉ phép") %></h3>
            <button class="modal-close" onclick="LeaveApp.closeModal('modalReject')">&times;</button>
        </div>
        <div class="modal-body">
            <input type="hidden" id="rejectReqId">
            <p><%= com.hrms.i18n.Language.text(request, "ui_e74140937c6b", "Từ chối đơn nghỉ phép của nhân sự") %> <strong id="rejectEmpName"></strong>.</p>
            <div style="margin-top: 14px;">
                <label class="form-label" for="rejectComment"><%= com.hrms.i18n.Language.text(request, "ui_206d4a030b9c", "Lý do từ chối") %> <span style="color:#b42318;">*</span>:</label>
                <textarea id="rejectComment" class="form-control" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_a2470e7cbfa1", "Nhập lý do không phê duyệt...") %>" required></textarea>
            </div>
        </div>
        <div class="modal-footer">
            <button class="button secondary" onclick="LeaveApp.closeModal('modalReject')"><%= com.hrms.i18n.Language.text(request, "ui_34ca764caf22", "Hủy") %></button>
            <button id="btnConfirmReject" class="button" style="background:#b42318;border-color:#b42318;" onclick="LeaveApp.confirmReject()"><%= com.hrms.i18n.Language.text(request, "ui_011a2c70d7de", "Từ chối đơn") %></button>
        </div>
    </div>
</div>



<script src="../js/leave.js?v=<%= application.getResource("/js/leave.js").openConnection().getLastModified() %>"></script>
<script>
    document.addEventListener("DOMContentLoaded", () => {
        LeaveApp.initAllRequestsView();
    });
</script>
</body>
</html>
