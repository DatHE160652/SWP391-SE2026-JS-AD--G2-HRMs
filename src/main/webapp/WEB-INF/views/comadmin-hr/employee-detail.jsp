<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Phân công và cấp dưới</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/lifecycle.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/employee-assignment.css">
</head>
<body class="lc" data-page="detail">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content ea-details">
        <div class="lc-breadcrumb">Quản trị nhân sự / Hồ sơ nhân viên / Phân công và cấp dưới</div>
        <div class="page-header">
            <div>
                <h1 id="detailTitle" class="page-title">Phân công và cấp dưới</h1>
            </div>
            <a id="detailBackLink" class="button secondary" href="employee-list">Quay lại hồ sơ</a>
        </div>

        <div class="panel ea-summary"><span id="detailAvatar" class="lc-avatar large" aria-hidden="true">…</span><div><strong id="detailEmployeeCode">Đang tải thông tin nhân viên…</strong><p id="detailSubtitle" class="page-subtitle"></p></div><a id="detailOrgChartLink" class="button secondary" href="${pageContext.request.contextPath}/lifecycle/organization">Xem sơ đồ tổ chức</a></div>
        <div id="message" class="message" role="status"></div>
        <section class="panel">
            <div class="tabs lc-tabs" role="tablist" aria-label="Thông tin nhân viên">
                <button type="button" class="tab-button" data-tab="profile">Thông tin</button>
                <button type="button" class="tab-button" data-tab="employment">Việc làm & tài khoản nhân viên</button>
                <button type="button" class="tab-button active" data-tab="organization">Phân công & Cấp dưới</button>
                <button type="button" class="tab-button" data-tab="documents">Tài liệu</button>
                <button type="button" class="tab-button" data-tab="history">Lịch sử</button>
            </div>
            <div id="profileTab" class="tab-panel detail-grid hidden"></div>
            <div id="employmentTab" class="tab-panel detail-grid hidden"></div>
            <div id="organizationTab" class="tab-panel">
                <%@ include file="/WEB-INF/views/common/employee-organization.jspf" %>
            </div>
            <div id="documentsTab" class="tab-panel hidden">
                <div class="document-toolbar">
                    <h2>Tài liệu nhân viên</h2>
                    <a id="uploadDocumentLink" class="button" href="#">Tải tài liệu lên</a>
                </div>
                <table data-pagination="legacy-documents">
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
            </div>
            <div id="historyTab" class="tab-panel hidden">
                <h2>Lịch sử nghiệp vụ nhân sự</h2>
                <p class="page-subtitle">100 sự kiện gần nhất: phân công người quản lý, chuyển vị trí/phòng ban, thăng cấp, hạ cấp và hiệu chỉnh. Bản ghi hiệu chỉnh trong ngày được giữ đúng loại CORRECTION.</p>
                <div style="overflow-x: auto;">
                    <table data-pagination="legacy-career-history">
                        <thead><tr><th>Ngày hiệu lực</th><th>Nghiệp vụ</th><th>Trước thay đổi</th><th>Sau thay đổi</th><th>Lý do</th><th>Trạng thái</th><th>Người thực hiện</th></tr></thead>
                        <tbody id="careerHistoryRows"></tbody>
                    </table>
                </div>
                <h2>Nhật ký thay đổi</h2>
                <table data-pagination="legacy-audit-history">
                    <thead>
                    <tr>
                        <th>Thời điểm</th>
                        <th>Thao tác</th>
                        <th>Người thực hiện</th>
                        <th>Trước thay đổi</th>
                        <th>Sau thay đổi</th>
                    </tr>
                    </thead>
                    <tbody id="historyRows"></tbody>
                </table>
            </div>
        </section>
    </main>
<script src="../js/employee.js?v=<%= application.getResource("/js/employee.js").openConnection().getLastModified() %>"></script>
<script src="../js/document.js?v=<%= application.getResource("/js/document.js").openConnection().getLastModified() %>"></script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js" defer></script>
</body>
</html>
