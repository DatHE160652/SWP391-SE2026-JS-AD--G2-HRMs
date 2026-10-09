<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Upload Document</title>
    <link rel="stylesheet" href="../css/employee.css">
</head>
<body data-page="document-upload">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>TechNova JSC</span></div>
        <div class="nav-label">Phân hệ quản trị</div>
        <a class="nav-link active" href="../employee/employee-list">Quản lý Nhân sự</a>
        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title">Upload Document</h1>
                <p class="page-subtitle">Allowed: PDF, DOC, DOCX, JPG, PNG. Maximum size: 10MB.</p>
            </div>
            <a id="cancelDocumentLink" class="button secondary" href="#">Cancel</a>
        </div>

        <div id="message" class="message"></div>

        <form id="documentUploadForm" class="panel" enctype="multipart/form-data">
            <input type="hidden" name="employeeId">
            <div class="form-grid">
                <div>
                    <label>Document Name</label>
                    <input name="documentName" required>
                </div>
                <div>
                    <label>Document Type</label>
                    <input name="documentType" placeholder="Contract, ID Card, Certificate...">
                </div>
                <div>
                    <label>Choose File</label>
                    <input name="file" type="file" accept=".pdf,.doc,.docx,.jpg,.png" required>
                </div>
            </div>
            <div class="form-actions">
                <button type="submit">Upload</button>
                <a id="cancelDocumentButton" class="button secondary" href="#">Cancel</a>
            </div>
        </form>
    </main>
</div>
<script src="../js/document.js"></script>
</body>
</html>
