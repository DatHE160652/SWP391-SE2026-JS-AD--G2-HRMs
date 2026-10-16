<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Duyệt đơn nghỉ phép - HRMs Cloud</title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
    <link rel="stylesheet" href="../css/leave.css">
</head>
<body data-page="leave-pending">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>Manager Portal</span></div>
        <div class="nav-label">Phân hệ Chấm công</div>
        <a class="nav-link" href="../employee/my-profile?nav=manager">Hồ sơ cá nhân</a>
        <a class="nav-link" href="../attendance/manager-attendance">Điểm danh Nhân sự</a>
        <a class="nav-link" href="../attendance/manager-personal-attendance">Chấm công Cá nhân</a>
        <a class="nav-link" href="../attendance/attendance-report">Báo cáo Chấm công</a>

        <div class="nav-label" style="margin-top: 18px;">Phân hệ Nghỉ phép</div>
        <a class="nav-link active" href="pending-approvals">Duyệt đơn nghỉ phép</a>
        <a class="nav-link" href="calendar">Lịch nghỉ nhân sự</a>
        <a class="nav-link" href="my-requests">Đơn nghỉ cá nhân</a>

        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title">Duyệt đơn xin nghỉ phép</h1>
                <p class="page-subtitle">Danh sách các yêu cầu nghỉ phép đang chờ phê duyệt từ nhân sự trực thuộc quản lý.</p>
            </div>
            <div class="header-actions">
                <button class="button secondary" onclick="window.LeaveAppReloadPending()">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;vertical-align:middle;">
                        <polyline points="23 4 23 10 17 10"></polyline>
                        <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                    </svg>
                    Làm mới
                </button>
            </div>
        </div>

        <div class="info-banner" style="margin: 0 0 20px;">
            <svg width="20" height="20" fill="currentColor" viewBox="0 0 20 20" style="flex-shrink:0;">
                <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"></path>
            </svg>
            <div>
                <strong>Lưu ý quy trình duyệt:</strong> Khi phê duyệt đơn, hệ thống sẽ tự động trừ số ngày nghỉ tương ứng vào quỹ phép của nhân viên và đồng bộ trạng thái <strong>On Leave</strong> vào bảng chấm công cho các ngày làm việc đã duyệt.
            </div>
        </div>

        <!-- Pending Requests Table -->
        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th style="width: 70px;">Mã đơn</th>
                    <th>Nhân sự</th>
                    <th>Loại nghỉ phép</th>
                    <th>Khoảng thời gian</th>
                    <th style="width: 100px;">Số ngày</th>
                    <th>Lý do xin nghỉ</th>
                    <th style="width: 140px;">Thời gian nộp</th>
                    <th style="width: 160px; text-align: center;">Thao tác duyệt</th>
                </tr>
                </thead>
                <tbody id="pendingTableBody">
                    <tr><td colspan="8" style="text-align: center; color: #667085; padding: 24px;">Đang tải danh sách chờ duyệt...</td></tr>
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
            <h3 class="modal-title">Xác nhận phê duyệt đơn nghỉ phép</h3>
            <button class="modal-close" onclick="LeaveApp.closeModal('modalApprove')">&times;</button>
        </div>
        <div class="modal-body">
            <input type="hidden" id="approveReqId">
            <p>Bạn có chắc chắn muốn phê duyệt đơn nghỉ phép của nhân sự <strong id="approveEmpName"></strong>?</p>
            <div style="margin-top: 14px;">
                <label class="form-label" for="approveComment">Ý kiến phê duyệt (tùy chọn):</label>
                <textarea id="approveComment" class="form-control" placeholder="Ghi chú thêm nếu có..."></textarea>
            </div>
        </div>
        <div class="modal-footer">
            <button class="button secondary" onclick="LeaveApp.closeModal('modalApprove')">Hủy</button>
            <button id="btnConfirmApprove" class="button" style="background:#027a48;border-color:#027a48;" onclick="LeaveApp.confirmApprove()">Xác nhận duyệt</button>
        </div>
    </div>
</div>

<!-- Modal Reject Confirmation -->
<div id="modalReject" class="modal-overlay">
    <div class="modal-box">
        <div class="modal-header">
            <h3 class="modal-title">Từ chối đơn xin nghỉ phép</h3>
            <button class="modal-close" onclick="LeaveApp.closeModal('modalReject')">&times;</button>
        </div>
        <div class="modal-body">
            <input type="hidden" id="rejectReqId">
            <p>Từ chối đơn nghỉ phép của nhân sự <strong id="rejectEmpName"></strong>.</p>
            <div style="margin-top: 14px;">
                <label class="form-label" for="rejectComment">Lý do từ chối <span style="color:#b42318;">*</span>:</label>
                <textarea id="rejectComment" class="form-control" placeholder="Nhập lý do không phê duyệt đơn..." required></textarea>
            </div>
        </div>
        <div class="modal-footer">
            <button class="button secondary" onclick="LeaveApp.closeModal('modalReject')">Hủy</button>
            <button id="btnConfirmReject" class="button" style="background:#b42318;border-color:#b42318;" onclick="LeaveApp.confirmReject()">Từ chối đơn</button>
        </div>
    </div>
</div>

<script src="../js/leave.js"></script>
<script>
    document.addEventListener("DOMContentLoaded", () => {
        LeaveApp.initPendingApprovalsView();
    });
</script>
</body>
</html>
