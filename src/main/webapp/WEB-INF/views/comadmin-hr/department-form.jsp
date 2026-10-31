<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thông tin phòng ban</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
</head>
<body class="lc" data-page="department-form">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content">
        <div class="lc-breadcrumb">Quản trị nhân sự / Cơ cấu tổ chức</div>

        <div class="page-header">
            <div>
                <h1 id="departmentFormTitle" class="page-title">Thêm phòng ban</h1>
            </div>
            <a class="button secondary" href="department-list">Hủy</a>
        </div>

        <div id="message" class="message"></div>
        <p class="page-subtitle">Phòng ban chứa các chức danh. Ngừng hoạt động sẽ chặn phân công mới, không xóa nhân viên hoặc lịch sử hiện có.</p>
        <div id="departmentPositionLinks"></div>
        <button type="button" id="departmentInitialAssignment" hidden>Phân công nhân viên</button>

        <form id="departmentForm" class="panel">
            <input type="hidden" name="departmentId">
            <div class="form-grid">
                <div>
                    <label>Mã phòng ban</label>
                    <input name="departmentCode" maxlength="50" required>
                </div>
                <div>
                    <label>Tên phòng ban</label>
                    <input name="departmentName" maxlength="150" required>
                </div>
            </div>
            <div class="form-actions">
                <button type="submit">Lưu</button>
                <a class="button secondary" href="department-list">Hủy</a>
            </div>
        </form>

        <section id="departmentMembersPanel" class="panel hidden">
            <div class="document-toolbar">
                <h2>Nhân viên trong phòng ban <span id="departmentMemberCount" class="lc-count">0</span></h2>
            </div>
            <div id="departmentMemberList" aria-live="polite"></div>
        </section>
    </main>
<script src="../js/department.js?v=<%= application.getResource("/js/department.js").openConnection().getLastModified() %>"></script>
<%@ include file="/WEB-INF/views/common/initial-assignment.jspf" %>
<script src="${pageContext.request.contextPath}/js/organization-shell.js" defer></script>
</body>
</html>
