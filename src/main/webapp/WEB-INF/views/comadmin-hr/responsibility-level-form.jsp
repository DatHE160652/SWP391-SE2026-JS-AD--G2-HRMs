<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_31bf9078d724", "Quản lý Cấp trách nhiệm") %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
</head>
<body class="lc" data-page="responsibility-level-form">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content">
        <div class="lc-breadcrumb"><%= com.hrms.i18n.Language.text(request, "ui_6e3d81c992f7", "Quản trị nhân sự / Cơ cấu tổ chức") %></div>
        <div class="page-header">
            <div><h1 id="levelFormTitle" class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_4ad795ec58aa", "Thêm cấp trách nhiệm") %></h1><p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_36cc03984595", "Cấp dùng chung trong công ty, được gắn vào từng chức danh.") %></p></div>
            <a class="button secondary" href="responsibility-level-list"><%= com.hrms.i18n.Language.text(request, "ui_8a09e03d2052", "Quay lại") %></a>
        </div>
        <div id="message" class="message" role="status"></div>
        <form id="levelForm" class="panel">
            <input type="hidden" name="responsibilityLevelId">
            <div class="form-grid">
                <div><label for="levelCode"><%= com.hrms.i18n.Language.text(request, "ui_46b5503a8b63", "Mã cấp trách nhiệm") %></label><input id="levelCode" name="levelCode" maxlength="50" required></div>
                <div><label for="levelName"><%= com.hrms.i18n.Language.text(request, "ui_e36d4695d33b", "Tên cấp trách nhiệm") %></label><input id="levelName" name="levelName" maxlength="100" required></div>
                <div><label for="rankOrder"><%= com.hrms.i18n.Language.text(request, "ui_8d15d4130ec2", "Thứ bậc (thứ bậc)") %></label><input id="rankOrder" name="rankOrder" type="number" min="0" max="2147483647" step="1" required></div>
                <div><label for="defaultAllowance"><%= com.hrms.i18n.Language.text(request, "ui_fe3f11a4da46", "Phụ cấp mặc định (VNĐ)") %></label><input id="defaultAllowance" name="defaultAllowance" type="number" min="0" step="0.01" required value="0"><small><%= com.hrms.i18n.Language.text(request, "ui_104be4c1a2c3", "Chức danh kế thừa mức này nếu chưa đặt mức riêng. Không thay đổi lịch sử lương đã lưu.") %></small></div>
                <div><label for="isActive"><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></label><select id="isActive" name="isActive"><option value="true"><%= com.hrms.i18n.Language.text(request, "ui_9a616fcdadd0", "Đang hoạt động") %></option><option value="false"><%= com.hrms.i18n.Language.text(request, "ui_1ae508f9aaf1", "Ngừng hoạt động") %></option></select></div>
            </div>
            <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_d44d2d866e25", "Mã và thứ bậc phải duy nhất trong công ty. Khi cấp đã được chức danh hoặc chính sách phụ cấp tham chiếu, chỉ sửa tên hoặc trạng thái; tạo cấp mới nếu cần đổi thứ bậc.") %></p>
            <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_4502e6b4ef6d", "thứ bậc càng lớn, cấp càng cao. Không tự cấp quyền quản lý hoặc tài khoản nhân viên. Cấu hình phụ cấp chỉ áp dụng khi tạo bản ghi lương mới.") %></p>
            <div class="form-actions"><button type="submit"><%= com.hrms.i18n.Language.text(request, "ui_a306970e8b81", "Lưu") %></button><a class="button secondary" href="responsibility-level-list"><%= com.hrms.i18n.Language.text(request, "ui_34ca764caf22", "Hủy") %></a></div>
        </form>
    </main>


<script src="../js/responsibility-level.js?v=<%= application.getResource("/js/responsibility-level.js").openConnection().getLastModified() %>"></script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js?v=<%= application.getResource("/js/organization-shell.js").openConnection().getLastModified() %>" defer></script>
</body>
</html>
