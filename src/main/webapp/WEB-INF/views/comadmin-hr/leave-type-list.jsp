<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cấu hình loại nghỉ phép - HRMs Cloud</title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
    <link rel="stylesheet" href="../css/leave.css">
</head>
<body data-page="leave-type-list">
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
        <a class="nav-link active" href="types">Cấu hình loại phép</a>
        <a class="nav-link" href="all-requests">Tất cả đơn nghỉ</a>
        <a class="nav-link" href="pending-approvals">Duyệt đơn nghỉ phép</a>
        <a class="nav-link" href="calendar">Lịch nghỉ công ty</a>
        <a class="nav-link" href="report">Báo cáo nghỉ phép</a>

        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title">Cấu hình Loại & Chính sách Nghỉ phép</h1>
                <p class="page-subtitle">Thiết lập các danh mục ngày nghỉ, số ngày định mức hàng năm và chế độ tính lương áp dụng trong công ty.</p>
            </div>
            <div class="header-actions">
                <button class="button" onclick="LeaveApp.openCreateLeaveTypeModal()">+ Thêm loại nghỉ phép mới</button>
            </div>
        </div>

        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th style="width: 70px;">ID</th>
                    <th style="width: 140px;">Mã loại (Code)</th>
                    <th>Tên chính sách nghỉ phép</th>
                    <th style="width: 150px;">Số ngày cấp mặc định</th>
                    <th style="width: 150px;">Chế độ lương</th>
                    <th style="width: 130px; text-align: center;">Thao tác</th>
                </tr>
                </thead>
                <tbody id="typesTableBody">
                    <tr><td colspan="6" style="text-align: center; color: #667085; padding: 24px;">Đang tải danh mục...</td></tr>
                </tbody>
            </table>
        </section>
    </main>
</div>

<!-- Modal Create / Edit Leave Type -->
<div id="modalLeaveType" class="modal-overlay">
    <div class="modal-box">
        <div class="modal-header">
            <h3 id="modalLeaveTypeTitle" class="modal-title">Thêm loại nghỉ phép mới</h3>
            <button class="modal-close" onclick="LeaveApp.closeModal('modalLeaveType')">&times;</button>
        </div>
        <form id="leaveTypeForm">
            <div id="typeMessage" role="alert" style="display:none"></div>
            <input type="hidden" id="formLeaveTypeId" name="leaveTypeId">
            <div class="modal-body">
                <div style="margin-bottom: 14px;">
                    <label class="form-label" for="formTypeCode">Mã loại phép (duy nhất) <span style="color:#b42318;">*</span></label>
                    <input type="text" id="formTypeCode" name="typeCode" class="form-control" placeholder="VD: ANNUAL, SICK, UNPAID, MATERNITY" required>
                </div>
                <div style="margin-bottom: 14px;">
                    <label class="form-label" for="formTypeName">Tên loại nghỉ phép <span style="color:#b42318;">*</span></label>
                    <input type="text" id="formTypeName" name="typeName" class="form-control" placeholder="VD: Nghỉ phép năm, Nghỉ ốm đau..." required>
                </div>
                <div style="margin-bottom: 14px;">
                    <label class="form-label" for="formDefaultDays">Số ngày được cấp hàng năm <span style="color:#b42318;">*</span></label>
                    <input type="number" id="formDefaultDays" name="defaultDaysPerYear" class="form-control" min="0" max="365" value="12" required>
                </div>
                <div style="margin-top: 16px;">
                    <label style="display: inline-flex; align-items: center; gap: 8px; cursor: pointer; font-size: 13px; font-weight: 700; color: #344054;">
                        <input type="checkbox" id="formIsPaid" name="isPaid" value="true" checked style="width: 16px; height: 16px;">
                        Nghỉ có hưởng lương (Paid Leave)
                    </label>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="button secondary" onclick="LeaveApp.closeModal('modalLeaveType')">Hủy bỏ</button>
                <button type="submit" class="button">Lưu chính sách</button>
            </div>
        </form>
    </div>
</div>

<script src="../js/leave.js?v=<%= application.getResource("/js/leave.js").openConnection().getLastModified() %>"></script>
<script>
    document.addEventListener("DOMContentLoaded", () => {
        LeaveApp.initLeaveTypesView();
    });
</script>
</body>
</html>
