<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tải tài liệu lên</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/employee-assignment.css">
</head>
<body class="lc" data-page="document-upload">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content ea-details">
        <div class="lc-breadcrumb">Quản trị nhân sự / Hồ sơ nhân viên / Tài liệu</div>
        <div class="page-header">
            <div>
                <h1 class="page-title">Tải tài liệu lên</h1>
                <p class="page-subtitle">Hỗ trợ PDF, DOC, DOCX, JPG, PNG. Dung lượng tối đa 10 MB.</p>
            </div>
            <a id="cancelDocumentLink" class="button secondary" href="#">Quay lại tài liệu</a>
        </div>

        <a id="documentEmployeeLink" class="ea-profile-link" href="../employee/employee-list">← Hồ sơ nhân viên</a>
        <div id="message" class="message" role="status"></div>

        <form id="documentUploadForm" class="panel" enctype="multipart/form-data">
            <input type="hidden" name="employeeId">
            <div class="form-grid">
                <div>
                    <label>Tên tài liệu</label>
                    <input name="documentName" required>
                </div>
                <div>
                    <label>Loại tài liệu</label>
                    <input name="documentType" placeholder="Hợp đồng, giấy tờ định danh, bằng cấp…">
                </div>
                <div>
                    <label>Chọn tệp</label>
                    <input name="file" type="file" accept=".pdf,.doc,.docx,.jpg,.png" required>
                </div>
            </div>
            <div class="form-actions">
                <button type="submit">Tải lên</button>
                <a id="cancelDocumentButton" class="button secondary" href="#">Quay lại tài liệu</a>
            </div>
        </form>
    </main>
<script src="../js/document.js?v=<%= application.getResource("/js/document.js").openConnection().getLastModified() %>"></script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js" defer></script>
</body>
</html>
