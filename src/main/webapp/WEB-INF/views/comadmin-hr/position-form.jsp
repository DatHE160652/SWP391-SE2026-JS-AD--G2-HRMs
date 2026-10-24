<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thông tin chức danh</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
</head>
<body class="lc" data-page="position-form">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content">
        <div class="lc-breadcrumb">Quản trị nhân sự / Cơ cấu tổ chức</div>
        <div class="page-header">
            <div>
                <h1 id="positionFormTitle" class="page-title">Thêm chức danh</h1>
                <p class="page-subtitle">Mỗi chức danh thuộc một phòng ban và một cấp trách nhiệm. Chức danh đã sử dụng không được đổi phòng ban hoặc cấp; hãy tạo chức danh mới và điều chuyển có ngày hiệu lực.</p>
            </div>
            <a class="button secondary" href="position-list">Hủy</a>
        </div>

        <div id="message" class="message"></div>

        <form id="positionForm" class="panel">
            <input type="hidden" name="positionId">
            <div class="form-grid">
                <div>
                    <label>Phòng ban</label>
                    <select name="departmentId" required></select>
                </div>
                <div>
                    <label>Tên chức danh</label>
                    <input name="positionTitle" maxlength="150" required>
                </div>
                <div>
                    <label>Mô tả</label>
                    <textarea name="description" rows="3" maxlength="255"></textarea>
                </div>
                <div>
                    <label>Cấp trách nhiệm</label>
                    <select name="responsibilityLevelId" required></select>
                    <a href="../responsibility-level/responsibility-level-list">Quản lý cấp trách nhiệm</a>
                    <small id="responsibilityAllowance"></small>
                </div>
                <div>
                    <label>Chức danh quản lý trực tiếp</label>
                    <select name="reportsToPositionId"><option value="">Chưa cấu hình</option></select>
                    <small>Liên kết chức danh nhân viên với chức danh quản lý đúng team trong cùng phòng ban. Không suy đoán team từ tên chức danh.</small>
                </div>
                <div>
                    <label>Trạng thái</label>
                    <select name="isActive"><option value="true">Đang hoạt động</option><option value="false">Ngừng hoạt động</option></select>
                </div>
                <div>
                    <label for="canManageEmployees">Cho phép quản lý nhân viên</label>
                    <input id="canManageEmployees" name="canManageEmployees" type="checkbox" value="true">
                    <small>Cấp trách nhiệm Employee không được phép quản lý nhân viên. Các cấp khác cần được bật quyền này để làm quản lý trực tiếp.</small>
                </div>
            </div>
            <div class="form-actions">
                <button type="submit">Lưu</button>
                <a class="button secondary" href="position-list">Hủy</a>
            </div>
        </form>
    </main>
<script src="../js/position.js?v=<%= application.getResource("/js/position.js").openConnection().getLastModified() %>"></script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js" defer></script>
</body>
</html>
