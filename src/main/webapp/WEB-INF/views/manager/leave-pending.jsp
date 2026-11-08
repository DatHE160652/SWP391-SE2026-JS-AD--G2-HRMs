<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_8c1fef9b7b35", "Duyệt đơn nghỉ phép - HRMs Cloud") %></title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
    <link rel="stylesheet" href="../css/leave.css">
</head>
<body data-page="leave-pending">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span><%= com.hrms.i18n.Language.text(request, "ui_3d11fc48b64c", "Cổng quản lý") %></span></div>
        <div class="nav-label"><%= com.hrms.i18n.Language.text(request, "ui_bb8e1c6f085e", "Phân hệ Chấm công") %></div>
        <a class="nav-link" href="../employee/my-profile?nav=manager"><%= com.hrms.i18n.Language.text(request, "ui_b70d1e2772e9", "Hồ sơ cá nhân") %></a>
        <a class="nav-link" href="../attendance/manager-attendance"><%= com.hrms.i18n.Language.text(request, "ui_cc34d4e67fde", "Điểm danh Nhân sự") %></a>
        <a class="nav-link" href="../attendance/manager-personal-attendance"><%= com.hrms.i18n.Language.text(request, "ui_e846c0844ceb", "Chấm công Cá nhân") %></a>
        <a class="nav-link" href="../attendance/attendance-report"><%= com.hrms.i18n.Language.text(request, "ui_5cfe0baad77f", "Báo cáo Chấm công") %></a>

        <div class="nav-label" style="margin-top: 18px;"><%= com.hrms.i18n.Language.text(request, "ui_b036712357df", "Phân hệ Nghỉ phép") %></div>
        <a class="nav-link active" href="pending-approvals"><%= com.hrms.i18n.Language.text(request, "ui_7b0a878a5449", "Duyệt đơn nghỉ phép") %></a>
        <a class="nav-link" href="calendar"><%= com.hrms.i18n.Language.text(request, "ui_55ed5216ef49", "Lịch nghỉ nhân sự") %></a>
        <a class="nav-link" href="my-requests"><%= com.hrms.i18n.Language.text(request, "ui_f88e8ff3ad6b", "Đơn nghỉ cá nhân") %></a>

        <a class="nav-link logout-link" href="../logout"><%= com.hrms.i18n.Language.text(request, "ui_d513aac0be87", "Đăng xuất") %></a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_9721d2df1381", "Duyệt đơn xin nghỉ phép") %></h1>
                <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_97cb10713ce3", "Danh sách các yêu cầu nghỉ phép đang chờ phê duyệt từ nhân sự trực thuộc quản lý.") %></p>
            </div>
            <div class="header-actions">
                <button class="button secondary" onclick="window.LeaveAppReloadPending()">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;vertical-align:middle;">
                        <polyline points="23 4 23 10 17 10"></polyline>
                        <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                    </svg>
                    <%= com.hrms.i18n.Language.text(request, "ui_61b0561227b1", "Làm mới") %>
                </button>
            </div>
        </div>

        <div class="info-banner" style="margin: 0 0 20px;">
            <svg width="20" height="20" fill="currentColor" viewBox="0 0 20 20" style="flex-shrink:0;">
                <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"></path>
            </svg>
            <div>
                <strong><%= com.hrms.i18n.Language.text(request, "ui_92c0237396f3", "Lưu ý quy trình duyệt:") %></strong> <%= com.hrms.i18n.Language.text(request, "ui_6380b3844ced", "Khi phê duyệt đơn, hệ thống sẽ tự động trừ số ngày nghỉ tương ứng vào quỹ phép của nhân viên và đồng bộ trạng thái") %> <strong><%= com.hrms.i18n.Language.text(request, "ui_9d21ce1caf72", "Nghỉ phép") %></strong> <%= com.hrms.i18n.Language.text(request, "ui_fb60f7c90c9c", "vào bảng chấm công cho các ngày làm việc đã duyệt.") %>
            </div>
        </div>

        <!-- Pending Requests Table -->
        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th style="width: 70px;"><%= com.hrms.i18n.Language.text(request, "ui_e67a0987de5b", "Mã đơn") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_f3d81e4e1b96", "Nhân sự") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_1389e04eccd7", "Loại nghỉ phép") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_f3aae68524b6", "Khoảng thời gian") %></th>
                    <th style="width: 100px;"><%= com.hrms.i18n.Language.text(request, "ui_c10472ab6cfb", "Số ngày") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_c60a93281be1", "Lý do xin nghỉ") %></th>
                    <th style="width: 140px;"><%= com.hrms.i18n.Language.text(request, "ui_8e9e61118077", "Thời gian nộp") %></th>
                    <th style="width: 160px; text-align: center;"><%= com.hrms.i18n.Language.text(request, "ui_4d0ec006ac2e", "Thao tác duyệt") %></th>
                </tr>
                </thead>
                <tbody id="pendingTableBody">
                    <tr><td colspan="8" style="text-align: center; color: #667085; padding: 24px;"><%= com.hrms.i18n.Language.text(request, "ui_5d570fda5c5f", "Đang tải danh sách chờ duyệt...") %></td></tr>
                </tbody>
            </table>

            <div id="paginationBar" class="pagination-bar"></div>
        </section>
    </main>
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
                <textarea id="rejectComment" class="form-control" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_a0f58bc97471", "Nhập lý do không phê duyệt đơn...") %>" required></textarea>
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
        LeaveApp.initPendingApprovalsView();
    });
</script>
</body>
</html>
