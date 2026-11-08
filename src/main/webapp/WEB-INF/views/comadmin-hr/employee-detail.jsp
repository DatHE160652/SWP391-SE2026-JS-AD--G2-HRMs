<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_c1984a2d94d3", "Phân công và cấp dưới") %></title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/employee-assignment.css">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc" data-page="detail">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content ea-details">
        <div class="lc-breadcrumb"><%= com.hrms.i18n.Language.text(request, "ui_f13fdabc3453", "Quản trị nhân sự / Hồ sơ nhân viên / Phân công và cấp dưới") %></div>
        <div class="page-header">
            <div>
                <h1 id="detailTitle" class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_c1984a2d94d3", "Phân công và cấp dưới") %></h1>
            </div>
            <a id="detailBackLink" class="button secondary" href="employee-list"><%= com.hrms.i18n.Language.text(request, "ui_19c90ec517eb", "Quay lại hồ sơ") %></a>
        </div>

        <div class="panel ea-summary"><span id="detailAvatar" class="lc-avatar large" aria-hidden="true">…</span><div><strong id="detailEmployeeCode"><%= com.hrms.i18n.Language.text(request, "ui_23dcfd5155ec", "Đang tải thông tin nhân viên…") %></strong><p id="detailSubtitle" class="page-subtitle"></p></div><a id="detailOrgChartLink" class="button secondary" href="${pageContext.request.contextPath}/lifecycle/organization"><%= com.hrms.i18n.Language.text(request, "ui_96f1e29be0e5", "Xem sơ đồ tổ chức") %></a></div>
        <div id="message" class="message" role="status"></div>
        <section class="panel">
            <div class="tabs lc-tabs" role="tablist" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_25447f03ac1e", "Thông tin nhân viên") %>">
                <button type="button" class="tab-button" data-tab="profile"><%= com.hrms.i18n.Language.text(request, "ui_6465033443d8", "Thông tin") %></button>
                <button type="button" class="tab-button" data-tab="employment"><%= com.hrms.i18n.Language.text(request, "ui_f5638104066e", "Việc làm & tài khoản nhân viên") %></button>
                <button type="button" class="tab-button active" data-tab="organization"><%= com.hrms.i18n.Language.text(request, "ui_5a88aa96584d", "Phân công & Cấp dưới") %></button>
                <button type="button" class="tab-button" data-tab="documents"><%= com.hrms.i18n.Language.text(request, "ui_e87eb38806a6", "Tài liệu") %></button>
                <button type="button" class="tab-button" data-tab="history"><%= com.hrms.i18n.Language.text(request, "ui_ff72062b26de", "Lịch sử") %></button>
            </div>
            <div id="profileTab" class="tab-panel detail-grid hidden"></div>
            <div id="employmentTab" class="tab-panel detail-grid hidden"></div>
            <div id="organizationTab" class="tab-panel">
                <%@ include file="/WEB-INF/views/common/employee-organization.jspf" %>
            </div>
            <div id="documentsTab" class="tab-panel hidden">
                <div class="document-toolbar">
                    <h2><%= com.hrms.i18n.Language.text(request, "ui_63e905f01f81", "Tài liệu nhân viên") %></h2>
                    <a id="uploadDocumentLink" class="button" href="#"><%= com.hrms.i18n.Language.text(request, "ui_498f1d88e704", "Tải tài liệu lên") %></a>
                </div>
                <table data-pagination="legacy-documents">
                    <thead>
                    <tr>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_bb295b750c07", "Tên tài liệu") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_7c8dd9841819", "Loại tài liệu") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_9c0863b0dcfc", "Ngày tải lên") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_04dcb5fbde87", "Người tải lên") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th>
                    </tr>
                    </thead>
                    <tbody id="documentRows"></tbody>
                </table>
            </div>
            <div id="historyTab" class="tab-panel hidden">
                <h2><%= com.hrms.i18n.Language.text(request, "ui_dee8308e373a", "Lịch sử nghiệp vụ nhân sự") %></h2>
                <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_d25b2cc95a2f", "100 sự kiện gần nhất: phân công người quản lý, chuyển vị trí/phòng ban, thăng cấp, hạ cấp và hiệu chỉnh. Bản ghi hiệu chỉnh trong ngày được giữ đúng loại CORRECTION.") %></p>
                <div style="overflow-x: auto;">
                    <table data-pagination="legacy-career-history">
                        <thead><tr><th><%= com.hrms.i18n.Language.text(request, "ui_651d62aeac55", "Ngày hiệu lực") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_d5ea59943f84", "Nghiệp vụ") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_cc7a9e410e67", "Trước thay đổi") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_a27c5dda3b3e", "Sau thay đổi") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_ecdb468a6da0", "Lý do") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_382efb78e209", "Người thực hiện") %></th></tr></thead>
                        <tbody id="careerHistoryRows"></tbody>
                    </table>
                </div>
                <h2><%= com.hrms.i18n.Language.text(request, "ui_241dba3b0b83", "Nhật ký thay đổi") %></h2>
                <table data-pagination="legacy-audit-history">
                    <thead>
                    <tr>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_020b0562702a", "Thời điểm") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_382efb78e209", "Người thực hiện") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_cc7a9e410e67", "Trước thay đổi") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_a27c5dda3b3e", "Sau thay đổi") %></th>
                    </tr>
                    </thead>
                    <tbody id="historyRows"></tbody>
                </table>
            </div>
        </section>
    </main>


<script src="../js/employee.js?v=<%= application.getResource("/js/employee.js").openConnection().getLastModified() %>"></script>
<script src="../js/document.js?v=<%= application.getResource("/js/document.js").openConnection().getLastModified() %>"></script>
<script src="${pageContext.request.contextPath}/js/organization-shell.js?v=<%= application.getResource("/js/organization-shell.js").openConnection().getLastModified() %>" defer></script>
</body>
</html>
