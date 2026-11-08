<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_4825415f7fc6", "Lịch sử đơn nghỉ phép - HRMs Cloud") %></title>
    <link rel="stylesheet" href="../css/employee.css">
    <link rel="stylesheet" href="../css/attendance.css">
    <link rel="stylesheet" href="../css/leave.css">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc" data-page="leave-my-requests">
<div class="workspace-shell">
    <%@ include file="/WEB-INF/views/common/tenant-shell.jspf" %>

    <main class="lc-main content">
        <div class="page-header">
            <div>
                <h1 class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_87f09fe51ba5", "Lịch sử đơn xin nghỉ phép") %></h1>
                <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_45125febe979", "Theo dõi tiến độ duyệt đơn, phản hồi của cấp trên và lịch sử các đợt nghỉ đã đăng ký.") %></p>
            </div>
            <div class="header-actions">
                <a href="submit" class="button" style="text-decoration: none;"><%= com.hrms.i18n.Language.text(request, "ui_3fe7cc4d4d30", "+ Tạo đơn mới") %></a>
            </div>
        </div>

        <!-- Filter Bar -->
        <section class="panel" style="margin-bottom: 20px;">
            <div class="filter-bar" style="margin-bottom: 0; background: none; border: none; padding: 0;">
                <div class="filter-group" style="min-width: 160px;">
                    <label for="statusFilter"><%= com.hrms.i18n.Language.text(request, "ui_98267f115412", "Trạng thái duyệt:") %></label>
                    <select id="statusFilter">
                        <option value="ALL"><%= com.hrms.i18n.Language.text(request, "ui_045c53831ced", "-- Tất cả trạng thái --") %></option>
                        <option value="Pending"><%= com.hrms.i18n.Language.text(request, "ui_3352b356a4c7", "Chờ duyệt") %></option>
                        <option value="Approved"><%= com.hrms.i18n.Language.text(request, "ui_1873ea0bf839", "Đã duyệt (Approved)") %></option>
                        <option value="Rejected"><%= com.hrms.i18n.Language.text(request, "ui_b148008cb7c1", "Đã từ chối") %></option>
                        <option value="Cancelled"><%= com.hrms.i18n.Language.text(request, "ui_7c7fb2f1c0ef", "Đã hủy (Cancelled)") %></option>
                    </select>
                </div>
                <div class="filter-group" style="min-width: 120px;">
                    <label for="yearFilter"><%= com.hrms.i18n.Language.text(request, "ui_3731977b06d7", "Năm:") %></label>
                    <select id="yearFilter">
                        <option value=""><%= com.hrms.i18n.Language.text(request, "ui_033595c1c234", "-- Tất cả các năm --") %></option>
                        <option value="2025">2025</option>
                        <option value="2026" selected>2026</option>
                        <option value="2027">2027</option>
                    </select>
                </div>
            </div>
        </section>

        <!-- Requests Table -->
        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th style="width: 70px;"><%= com.hrms.i18n.Language.text(request, "ui_e67a0987de5b", "Mã đơn") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_1389e04eccd7", "Loại nghỉ phép") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_f3aae68524b6", "Khoảng thời gian") %></th>
                    <th style="width: 100px;"><%= com.hrms.i18n.Language.text(request, "ui_c10472ab6cfb", "Số ngày") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_ecdb468a6da0", "Lý do") %></th>
                    <th style="width: 120px;"><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></th>
                    <th><%= com.hrms.i18n.Language.text(request, "ui_b692587c98ef", "Phản hồi người duyệt") %></th>
                    <th style="width: 130px; text-align: center;"><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th>
                </tr>
                </thead>
                <tbody id="requestsTableBody">
                    <tr><td colspan="8" style="text-align: center; color: #667085; padding: 24px;"><%= com.hrms.i18n.Language.text(request, "ui_d37b2a68bf44", "Đang tải danh sách...") %></td></tr>
                </tbody>
            </table>

            <div id="paginationBar" class="pagination-bar"></div>
        </section>
    </main>
</div>

<!-- Details Modal -->
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



<script src="../js/leave.js?v=<%= application.getResource("/js/leave.js").openConnection().getLastModified() %>"></script>
<script>
    document.addEventListener("DOMContentLoaded", () => {
        LeaveApp.initMyRequestsView();
    });
</script>
</body>
</html>
