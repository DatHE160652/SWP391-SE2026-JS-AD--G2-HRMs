<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý Cấp trách nhiệm</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
</head>
<body class="lc" data-page="responsibility-level-form">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content">
        <div class="lc-breadcrumb">Quản trị nhân sự / Cơ cấu tổ chức</div>
        <div class="page-header">
            <div><h1 id="levelFormTitle" class="page-title">Thêm cấp trách nhiệm</h1><p class="page-subtitle">Cấp dùng chung trong công ty, được gắn vào từng chức danh.</p></div>
            <a class="button secondary" href="responsibility-level-list">Quay lại</a>
        </div>
        <div id="message" class="message" role="status"></div>
        <form id="levelForm" class="panel">
            <input type="hidden" name="responsibilityLevelId">
            <div class="form-grid">
                <div><label for="levelCode">Mã cấp trách nhiệm</label><input id="levelCode" name="levelCode" maxlength="50" required></div>
                <div><label for="levelName">Tên cấp trách nhiệm</label><input id="levelName" name="levelName" maxlength="100" required></div>
                <div><label for="rankOrder">Thứ bậc (thứ bậc)</label><input id="rankOrder" name="rankOrder" type="number" min="0" max="2147483647" step="1" required></div>
                <div><label for="defaultAllowance">Phụ cấp mặc định (VNĐ)</label><input id="defaultAllowance" name="defaultAllowance" type="number" min="0" step="0.01" required value="0"><small>Chức danh kế thừa mức này nếu chưa đặt mức riêng. Không thay đổi lịch sử lương đã lưu.</small></div>
                <div><label for="isActive">Trạng thái</label><select id="isActive" name="isActive"><option value="true">Đang hoạt động</option><option value="false">Ngừng hoạt động</option></select></div>
            </div>
            <p class="page-subtitle">Mã và thứ bậc phải duy nhất trong công ty. Khi cấp đã được chức danh hoặc chính sách phụ cấp tham chiếu, chỉ sửa tên hoặc trạng thái; tạo cấp mới nếu cần đổi thứ bậc.</p>
            <p class="page-subtitle">thứ bậc càng lớn, cấp càng cao. Không tự cấp quyền quản lý hoặc tài khoản nhân viên. Cấu hình phụ cấp chỉ áp dụng khi tạo bản ghi lương mới.</p>
            <div class="form-actions"><button type="submit">Lưu</button><a class="button secondary" href="responsibility-level-list">Hủy</a></div>
        </form>
    </main>
<script src="../js/responsibility-level.js?v=<%= application.getResource("/js/responsibility-level.js").openConnection().getLastModified() %>"></script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js" defer></script>
</body>
</html>
