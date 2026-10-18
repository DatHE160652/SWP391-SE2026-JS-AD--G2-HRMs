<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Employee Detail</title>
    <link rel="stylesheet" href="../css/employee.css">
</head>
<body data-page="detail">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>TechNova JSC</span></div>
        <div class="nav-label">Phân hệ quản trị</div>
        <a class="nav-link active" href="employee-list">Quản lý Nhân sự</a>
        <a class="nav-link" href="../department/department-list">Quản lý Phòng ban</a>
        <a class="nav-link" href="../position/position-list">Quản lý Chức danh</a>
        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>

    <main class="content">
        <div class="page-header">
            <div>
                <h1 id="detailTitle" class="page-title">Employee Profile</h1>
            </div>
            <a class="button secondary" href="employee-list">Cancel</a>
        </div>

        <div id="message" class="message"></div>
        <section class="panel">
            <div class="tabs">
                <button type="button" class="tab-button active" data-tab="profile">Profile</button>
                <button type="button" class="tab-button" data-tab="employment">Employment</button>
                <button type="button" class="tab-button" data-tab="organization">Organization Information</button>
                <button type="button" class="tab-button" data-tab="documents">Documents</button>
                <button type="button" class="tab-button" data-tab="history">History</button>
            </div>
            <div id="profileTab" class="tab-panel detail-grid"></div>
            <div id="employmentTab" class="tab-panel detail-grid hidden"></div>
            <div id="organizationTab" class="tab-panel hidden">
                <form id="organizationForm" novalidate>
                    <input type="hidden" name="employeeId">
                    <div class="form-grid">
                        <div>
                            <label>Employee Name</label>
                            <input id="organizationEmployeeName" readonly>
                        </div>
                        <div>
                            <label>Department</label>
                            <select name="departmentId" required></select>
                        </div>
                        <div>
                            <label>Position</label>
                            <select name="positionId"></select>
                        </div>
                        <div>
                            <label id="managerFieldLabel">Manager</label>
                            <select name="managerId" required></select>
                        </div>
                    </div>
                    <div id="leaderEmployeesPanel" class="hidden">
                        <label>Employee do Leader quản lý</label>
                        <div id="leaderEmployeeOptions" class="member-picker"></div>
                        <table style="margin-top: 18px;">
                            <thead>
                            <tr>
                                <th>Employee Code</th>
                                <th>Full Name</th>
                                <th>Position</th>
                                <th>Role</th>
                            </tr>
                            </thead>
                            <tbody id="leaderEmployeeRows"></tbody>
                        </table>
                    </div>
                    <div class="form-actions">
                        <button type="submit" formnovalidate>Save Organization Information</button>
                    </div>
                </form>
            </div>
            <div id="documentsTab" class="tab-panel hidden">
                <div class="document-toolbar">
                    <h2>Employee Documents</h2>
                    <a id="uploadDocumentLink" class="button" href="#">Upload Document</a>
                </div>
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
            </div>
            <div id="historyTab" class="tab-panel hidden">
                <table>
                    <thead>
                    <tr>
                        <th>Date</th>
                        <th>Action</th>
                        <th>Changed By</th>
                        <th>Old Value</th>
                        <th>New Value</th>
                    </tr>
                    </thead>
                    <tbody id="historyRows"></tbody>
                </table>
            </div>
        </section>
    </main>
</div>
<script src="../js/employee.js?v=<%= application.getResource("/js/employee.js").openConnection().getLastModified() %>"></script>
<script src="../js/document.js"></script>
</body>
</html>
