<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tài liệu nhân viên</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/employee-assignment.css">
</head>
<body class="lc" data-page="documents">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content ea-details">
        <div class="lc-breadcrumb">Quản trị nhân sự / Hồ sơ nhân viên / Tài liệu</div>

        <div class="page-header">
            <div>
                <h1 class="page-title">Tài liệu nhân viên</h1>
                <p class="page-subtitle">Xem, tải xuống và quản lý tài liệu của nhân viên.</p>
            </div>
            <a id="uploadDocumentLink" class="button" href="#">Tải tài liệu lên</a>
        </div>

        <a id="documentEmployeeLink" class="ea-profile-link" href="../employee/employee-list">← Hồ sơ nhân viên</a>
        <div id="message" class="message" role="status"></div>
        <label for="statusFilter">Trạng thái</label>
        <select id="statusFilter">
            <option value="">Tất cả</option>
            <option value="Active">Đang sử dụng</option>
            <option value="Inactive">Đã lưu trữ</option>
        </select>

        <section class="panel">
            <table data-pagination="employee-documents">
                <thead>
                <tr>
                    <th>Tên tài liệu</th>
                    <th>Loại tài liệu</th>
                    <th>Ngày tải lên</th>
                    <th>Người tải lên</th>
                    <th>Thao tác</th>
                </tr>
                </thead>
                <tbody id="documentRows"></tbody>
            </table>
        </section>
    </main>
<script src="../js/document.js?v=<%= application.getResource("/js/document.js").openConnection().getLastModified() %>"></script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js" defer></script>
</body>
</html>
