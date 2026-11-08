<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_48a0068b5616", "Cấu hình loại nghỉ phép - HRMs Cloud") %></title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
    <link rel="stylesheet" href="../css/leave.css">
</head>
<body data-page="leave-type-list">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span><c:out value="${not empty sessionScope.tenantPrincipal.companyName ? sessionScope.tenantPrincipal.companyName : 'Doanh nghiệp'}"/></span></div>
        <div class="nav-label"><%= com.hrms.i18n.Language.text(request, "ui_0dec7fd51c40", "Phân hệ Quản trị") %></div>
        <a class="nav-link" href="../employee/employee-list"><%= com.hrms.i18n.Language.text(request, "ui_5ed96e7249d6", "Quản lý Nhân sự") %></a>
        <a class="nav-link" href="../department/department-list"><%= com.hrms.i18n.Language.text(request, "ui_bc217c0a7c21", "Quản lý Phòng ban") %></a>
        <a class="nav-link" href="../position/position-list"><%= com.hrms.i18n.Language.text(request, "ui_45243efa4b09", "Quản lý Chức danh") %></a>

        <div class="nav-label" style="margin-top: 18px;"><%= com.hrms.i18n.Language.text(request, "ui_bb8e1c6f085e", "Phân hệ Chấm công") %></div>
        <a class="nav-link" href="../attendance/manager-attendance"><%= com.hrms.i18n.Language.text(request, "ui_cc34d4e67fde", "Điểm danh Nhân sự") %></a>
        <a class="nav-link" href="../attendance/manager-personal-attendance"><%= com.hrms.i18n.Language.text(request, "ui_e846c0844ceb", "Chấm công Cá nhân") %></a>
        <a class="nav-link" href="../attendance/company-attendance"><%= com.hrms.i18n.Language.text(request, "ui_6b91b52ab71d", "Giám sát Công ty") %></a>
        <a class="nav-link" href="../attendance/attendance-report"><%= com.hrms.i18n.Language.text(request, "ui_5cfe0baad77f", "Báo cáo Chấm công") %></a>

        <div class="nav-label" style="margin-top: 18px;"><%= com.hrms.i18n.Language.text(request, "ui_b036712357df", "Phân hệ Nghỉ phép") %></div>
        <a class="nav-link active" href="types"><%= com.hrms.i18n.Language.text(request, "ui_611245ff0fb6", "Cấu hình loại phép") %></a>
        <a class="nav-link" href="all-requests"><%= com.hrms.i18n.Language.text(request, "ui_b26df14c4e69", "Tất cả đơn nghỉ") %></a>
        <a class="nav-link" href="pending-approvals"><%= com.hrms.i18n.Language.text(request, "ui_7b0a878a5449", "Duyệt đơn nghỉ phép") %></a>
        <a class="nav-link" href="calendar"><%= com.hrms.i18n.Language.text(request, "ui_0dd83ebf282c", "Lịch nghỉ công ty") %></a>
        <a class="nav-link" href="report"><%= com.hrms.i18n.Language.text(request, "ui_e198a77b4338", "Báo cáo nghỉ phép") %></a>

        <a class="nav-link logout-link" href="../logout"><%= com.hrms.i18n.Language.text(request, "ui_d513aac0be87", "Đăng xuất") %></a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_aae460efe903", "Cấu hình Loại & Chính sách Nghỉ phép") %></h1>
                <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_f3db1f659953", "Thiết lập các danh mục ngày nghỉ, số ngày định mức hàng năm và chế độ tính lương áp dụng trong công ty.") %></p>
            </div>
            <div class="header-actions">
                <button class="button" onclick="LeaveApp.openCreateLeaveTypeModal()"><%= com.hrms.i18n.Language.text(request, "ui_326e8e8102dc", "+ Thêm loại nghỉ phép mới") %></button>
            </div>
        </div>

        <label for="statusFilter"><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></label>
        <select id="statusFilter">
            <option value=""><%= com.hrms.i18n.Language.text(request, "ui_f7a578dcbdca", "Tất cả") %></option>
            <option value="Active"><%= com.hrms.i18n.Language.text(request, "ui_9a616fcdadd0", "Đang hoạt động") %></option>
            <option value="Inactive"><%= com.hrms.i18n.Language.text(request, "ui_1ae508f9aaf1", "Ngừng hoạt động") %></option>
        </select>

        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th style="width: 70px;">ID</th>
                    <th style="width: 140px;"><%= com.hrms.i18n.Language.text(request, "ui_49ac23841059", "Mã loại (Code)") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_53c78f5aaee3", "Tên chính sách nghỉ phép") %></th>
                    <th style="width: 150px;"><%= com.hrms.i18n.Language.text(request, "ui_6a469916c910", "Số ngày cấp mặc định") %></th>
                    <th style="width: 150px;"><%= com.hrms.i18n.Language.text(request, "ui_5b3554e23818", "Chế độ lương") %></th>
                    <th style="width: 130px; text-align: center;"><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th>
                </tr>
                </thead>
                <tbody id="typesTableBody">
                    <tr><td colspan="6" style="text-align: center; color: #667085; padding: 24px;"><%= com.hrms.i18n.Language.text(request, "ui_c757c90d9451", "Đang tải danh mục...") %></td></tr>
                </tbody>
            </table>
        </section>
    </main>
</div>

<!-- Modal Create / Edit Leave Type -->
<div id="modalLeaveType" class="modal-overlay">
    <div class="modal-box">
        <div class="modal-header">
            <h3 id="modalLeaveTypeTitle" class="modal-title"><%= com.hrms.i18n.Language.text(request, "ui_1a0314a45c00", "Thêm loại nghỉ phép mới") %></h3>
            <button class="modal-close" onclick="LeaveApp.closeModal('modalLeaveType')">&times;</button>
        </div>
        <form id="leaveTypeForm">
            <div id="typeMessage" role="alert" style="display:none"></div>
            <input type="hidden" id="formLeaveTypeId" name="leaveTypeId">
            <div class="modal-body">
                <div style="margin-bottom: 14px;">
                    <label class="form-label" for="formTypeCode"><%= com.hrms.i18n.Language.text(request, "ui_b58b0b99069c", "Mã loại phép (duy nhất)") %> <span style="color:#b42318;">*</span></label>
                    <input type="text" id="formTypeCode" name="typeCode" class="form-control" placeholder="VD: ANNUAL, SICK, UNPAID, MATERNITY" required>
                </div>
                <div style="margin-bottom: 14px;">
                    <label class="form-label" for="formTypeName"><%= com.hrms.i18n.Language.text(request, "ui_9bff20b16de2", "Tên loại nghỉ phép") %> <span style="color:#b42318;">*</span></label>
                    <input type="text" id="formTypeName" name="typeName" class="form-control" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_dd1bbcde575b", "VD: Nghỉ phép năm, Nghỉ ốm đau...") %>" required>
                </div>
                <div style="margin-bottom: 14px;">
                    <label class="form-label" for="formDefaultDays"><%= com.hrms.i18n.Language.text(request, "ui_52aab2ea0960", "Số ngày được cấp hàng năm") %> <span style="color:#b42318;">*</span></label>
                    <input type="number" id="formDefaultDays" name="defaultDaysPerYear" class="form-control" min="0" max="365" value="12" required>
                </div>
                <div style="margin-top: 16px;">
                    <label style="display: inline-flex; align-items: center; gap: 8px; cursor: pointer; font-size: 13px; font-weight: 700; color: #344054;">
                        <input type="checkbox" id="formIsPaid" name="isPaid" value="true" checked style="width: 16px; height: 16px;">
                        <%= com.hrms.i18n.Language.text(request, "ui_89dd758909a5", "Nghỉ có hưởng lương (Paid Leave)") %>
                    </label>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="button secondary" onclick="LeaveApp.closeModal('modalLeaveType')"><%= com.hrms.i18n.Language.text(request, "ui_247d4b1efe9c", "Hủy bỏ") %></button>
                <button type="submit" class="button"><%= com.hrms.i18n.Language.text(request, "ui_32ccece336e3", "Lưu chính sách") %></button>
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
