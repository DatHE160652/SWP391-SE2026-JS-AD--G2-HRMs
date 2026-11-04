<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý Đơn nghỉ phép Toàn công ty - HRMs Cloud</title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
    <link rel="stylesheet" href="../css/leave.css">
</head>
<body data-page="leave-all-requests">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>TechNova JSC</span></div>
        <div class="nav-label">Phân hệ Quản trị</div>
        <a class="nav-link" href="../employee/employee-list">Quản lý Nhân sự</a>
        <a class="nav-link" href="../department/department-list">Quản lý Phòng ban</a>
        <a class="nav-link" href="../position/position-list">Quản lý Chức danh</a>

        <div class="nav-label" style="margin-top: 18px;">Phân hệ Chấm công</div>
        <a class="nav-link" href="../attendance/manager-attendance">Điểm danh Nhân sự</a>
        <a class="nav-link" href="../attendance/manager-personal-attendance">Chấm công Cá nhân</a>
        <a class="nav-link" href="../attendance/company-attendance">Giám sát Công ty</a>
        <a class="nav-link" href="../attendance/attendance-report">Báo cáo Chấm công</a>

        <div class="nav-label" style="margin-top: 18px;">Phân hệ Nghỉ phép</div>
        <a class="nav-link" href="types">Cấu hình loại phép</a>
        <a class="nav-link active" href="all-requests">Tất cả đơn nghỉ</a>
        <a class="nav-link" href="pending-approvals">Duyệt đơn nghỉ phép</a>
        <a class="nav-link" href="calendar">Lịch nghỉ công ty</a>
        <a class="nav-link" href="report">Báo cáo nghỉ phép</a>

        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title">Quản lý Đơn nghỉ phép Toàn công ty</h1>
                <p class="page-subtitle">Giám sát toàn bộ các đơn xin nghỉ phép trong doanh nghiệp, tìm kiếm và phân loại theo trạng thái hoặc phòng ban.</p>
            </div>
            <div class="header-actions" style="display:flex;gap:10px;">
                <button class="button secondary" onclick="window.LeaveAppReloadAll()">Làm mới</button>
                <button class="button" style="background:#107c41;color:#fff;display:inline-flex;align-items:center;gap:6px;border:none;cursor:pointer;" onclick="window.exportLeaveRequests()">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                    Xuất Excel
                </button>
            </div>
        </div>

        <!-- Filter Bar -->
        <section class="panel" style="margin-bottom: 20px;">
            <div class="filter-bar" style="margin-bottom: 0; background: none; border: none; padding: 0;">
                <div class="filter-group" style="min-width: 160px;">
                    <label for="statusFilter">Trạng thái:</label>
                    <select id="statusFilter">
                        <option value="ALL">-- Tất cả trạng thái --</option>
                        <option value="Pending">Pending (Chờ duyệt)</option>
                        <option value="Approved">Đã duyệt</option>
                        <option value="Rejected">Rejected (Bị từ chối)</option>
                        <option value="Cancelled">Đã hủy</option>
                    </select>
                </div>
                <div class="filter-group" style="min-width: 180px;">
                    <label for="deptFilter">Phòng ban:</label>
                    <select id="deptFilter">
                        <option value="">-- Tất cả phòng ban --</option>
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
                <div class="filter-group" style="flex: 1; min-width: 200px;">
                    <label for="keywordInput">Tìm kiếm nhân viên:</label>
                    <input type="text" id="keywordInput" placeholder="Nhập tên hoặc mã nhân viên...">
                </div>
                <div class="filter-actions" style="margin-bottom: 1px;">
                    <button class="button" id="btnFilter" style="padding: 8px 16px;">Lọc dữ liệu</button>
                </div>
            </div>
        </section>

        <!-- All Requests Table -->
        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th style="width: 70px;">Mã đơn</th>
                    <th>Nhân sự</th>
                    <th>Loại nghỉ phép</th>
                    <th>Khoảng thời gian</th>
                    <th style="width: 100px;">Số ngày</th>
                    <th>Lý do</th>
                    <th style="width: 120px;">Trạng thái</th>
                    <th>Người duyệt</th>
                    <th style="width: 140px; text-align: center;">Thao tác</th>
                </tr>
                </thead>
                <tbody id="allRequestsTableBody">
                    <tr><td colspan="9" style="text-align: center; color: #667085; padding: 24px;">Đang tải danh sách...</td></tr>
                </tbody>
            </table>

        </section>
    </main>
</div>

<!-- Modal Details -->
<div id="modalDetail" class="modal-overlay">
    <div class="modal-box">
        <div class="modal-header">
            <h3 class="modal-title">Chi tiết đơn xin nghỉ phép</h3>
            <button class="modal-close" onclick="LeaveApp.closeModal('modalDetail')">&times;</button>
        </div>
        <div class="modal-body" id="detailModalBody"></div>
        <div class="modal-footer">
            <button class="button secondary" onclick="LeaveApp.closeModal('modalDetail')">Đóng</button>
        </div>
    </div>
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
                <textarea id="rejectComment" class="form-control" placeholder="Nhập lý do không phê duyệt..." required></textarea>
            </div>
        </div>
        <div class="modal-footer">
            <button class="button secondary" onclick="LeaveApp.closeModal('modalReject')">Hủy</button>
            <button id="btnConfirmReject" class="button" style="background:#b42318;border-color:#b42318;" onclick="LeaveApp.confirmReject()">Từ chối đơn</button>
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
