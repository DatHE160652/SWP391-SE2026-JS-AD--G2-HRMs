<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Employee Documents</title>
    <link rel="stylesheet" href="../css/employee.css">
</head>
<body data-page="documents">
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
                <h1 class="page-title">Employee Documents</h1>
                <p class="page-subtitle">View, download, and delete employee documents.</p>
            </div>
            <a id="uploadDocumentLink" class="button" href="#">Upload Document</a>
        </div>

        <div id="message" class="message"></div>
        <section class="panel">
            <table>
                <thead>
                <tr>
                    <th>Document Name</th>
                    <th>Document Type</th>
                    <th>Uploaded Date</th>
                    <th>Uploaded By</th>
                    <th>Action</th>
                </tr>
                </thead>
                <tbody id="documentRows"></tbody>
            </table>
        </section>
    </main>
</div>
<script src="../js/document.js"></script>
</body>
</html>
