<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= com.hrms.i18n.Language.text(request, "ui_6bf97329bdf0", "Hồ sơ cá nhân - HRMs Cloud") %></title>

    <link rel="stylesheet" href="../css/organization.css">
    <link rel="stylesheet" href="../css/profile-update.css">

<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc" data-page="my-profile" data-profile-role="<c:out value='${sessionScope.tenantPrincipal.roleCode}'/>">
<%@ include file="/WEB-INF/views/common/tenant-shell.jspf" %>
<main class="lc-main content profile-update-page">
        <div class="lc-breadcrumb"><%= com.hrms.i18n.Language.text(request, "ui_e53a24994284", "Cổng nhân viên &nbsp; / &nbsp; Hồ sơ cá nhân") %></div>
        <div class="page-header">
            <div>
                <h1 id="profileName" class="page-title"><%= com.hrms.i18n.Language.text(request, "ui_b70d1e2772e9", "Hồ sơ cá nhân") %></h1>
                <p class="page-subtitle"><%= com.hrms.i18n.Language.text(request, "ui_2bb9e42b9f82", "Mã nhân viên:") %> <span id="profileCode">-</span></p>
            </div>
        </div>

        <div id="message" class="message"></div>

        <section class="panel personal-profile-layout">
            <nav class="tabs lc-tabs personal-profile-navigation" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_d94ee7c335a3", "Các mục hồ sơ cá nhân") %>">
                <button type="button" class="tab-button active" data-tab="profile"><%= com.hrms.i18n.Language.text(request, "ui_5de3e5eb0281", "Thông tin cá nhân") %></button>
                <button type="button" class="tab-button" data-tab="employment"><%= com.hrms.i18n.Language.text(request, "ui_4b7e9cc7b859", "Công việc") %></button>
                <button type="button" class="tab-button" data-tab="salary"><%= com.hrms.i18n.Language.text(request, "ui_287294c55fa6", "Lương & phụ cấp") %></button>
                <button type="button" class="tab-button" data-tab="account"><%= com.hrms.i18n.Language.text(request, "ui_4a60abe52524", "Tài khoản") %></button>
                <button type="button" class="tab-button" data-tab="documents"><%= com.hrms.i18n.Language.text(request, "ui_e87eb38806a6", "Tài liệu") %></button>
                <button type="button" class="tab-button" data-tab="history"><%= com.hrms.i18n.Language.text(request, "ui_ff72062b26de", "Lịch sử") %></button>
            </nav>

            <div class="personal-profile-content">
            <div id="profileTab" class="tab-panel">
                <form id="myProfileForm">
                    <input type="hidden" name="csrfToken">
                    <p id="profileUpdateStatus" role="status"><%= com.hrms.i18n.Language.text(request, "ui_876481552761", "Đang kiểm tra đợt cập nhật hồ sơ...") %></p>
                    <div class="form-grid">
                        <div>
                            <label><%= com.hrms.i18n.Language.text(request, "ui_84979604c762", "Mã nhân viên") %></label>
                            <input name="employeeCode" readonly>
                        </div>
                        <div>
                            <label><%= com.hrms.i18n.Language.text(request, "ui_ad9f29940174", "Họ và tên") %></label>
                            <input name="fullName" readonly>
                        </div>
                        <div>
                            <label><%= com.hrms.i18n.Language.text(request, "ui_80bbce559524", "Giới tính") %></label>
                            <div class="radio-row">
                                <label><input type="radio" name="gender" value="Male" disabled> <%= com.hrms.i18n.Language.text(request, "ui_b580ba1b67f1", "Nam") %></label>
                                <label><input type="radio" name="gender" value="Female" disabled> <%= com.hrms.i18n.Language.text(request, "ui_feb92ccdb958", "Nữ") %></label>
                                <label><input type="radio" name="gender" value="Other" disabled> <%= com.hrms.i18n.Language.text(request, "ui_5b8a28abf85c", "Khác") %></label>
                            </div>
                        </div>
                        <div>
                            <label><%= com.hrms.i18n.Language.text(request, "ui_6f8de3085b7b", "Ngày sinh") %></label>
                            <input name="dateOfBirth" type="date" readonly>
                        </div>
                        <div>
                            <label><%= com.hrms.i18n.Language.text(request, "ui_08fc3ada4760", "Điện thoại") %></label>
                            <input name="phone" type="tel" inputmode="numeric" minlength="10" maxlength="10" pattern="0[0-9]{9}" required readonly>
                        </div>
                        <div>
                            <label><%= com.hrms.i18n.Language.text(request, "ui_5e9c13e10f68", "Trạng thái làm việc") %></label>
                            <input name="status" readonly>
                        </div>
                        <div>
                            <label for="profileEmail"><%= com.hrms.i18n.Language.text(request, "ui_faaf5491ea05", "Email liên hệ *") %></label>
                            <input id="profileEmail" name="email" type="email" required maxlength="150" readonly placeholder="<%= com.hrms.i18n.Language.text(request, "ui_443f1c4da0d2", "Chưa cập nhật") %>">
                            <small class="lc-form-hint" style="display:block;margin-top:4px;font-size:11.5px;color:var(--lc-muted)">Email dùng để liên hệ trong hồ sơ nhân sự. Khi đăng nhập hệ thống, vui lòng dùng <strong>Email đăng nhập</strong> tại tab Tài khoản.</small>
                        </div>
                        <div>
                            <label for="profileIdentityNumber"><%= com.hrms.i18n.Language.text(request, "ui_5dfa5a768186", "Số CCCD/CMND") %></label>
                            <input id="profileIdentityNumber" name="identityNumber" readonly autocomplete="off" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_443f1c4da0d2", "Chưa cập nhật") %>">
                        </div>
                        <div>
                            <label for="profileIdentityIssuedDate"><%= com.hrms.i18n.Language.text(request, "ui_b010b9a1b3e8", "Ngày cấp CCCD/CMND") %></label>
                            <input id="profileIdentityIssuedDate" name="identityIssuedDate" type="date" readonly>
                        </div>
                        <div>
                            <label for="profileIdentityIssuedPlace"><%= com.hrms.i18n.Language.text(request, "ui_14722ec2fffb", "Nơi cấp CCCD/CMND") %></label>
                            <input id="profileIdentityIssuedPlace" name="identityIssuedPlace" maxlength="200" readonly placeholder="<%= com.hrms.i18n.Language.text(request, "ui_443f1c4da0d2", "Chưa cập nhật") %>">
                        </div>
                        <div>
                            <label for="profilePermanentAddress"><%= com.hrms.i18n.Language.text(request, "ui_8eddf059a536", "Địa chỉ thường trú") %></label>
                            <textarea id="profilePermanentAddress" name="permanentAddress" rows="2" maxlength="500" readonly placeholder="<%= com.hrms.i18n.Language.text(request, "ui_443f1c4da0d2", "Chưa cập nhật") %>"></textarea>
                        </div>
                        <div>
                            <label for="profileHometown"><%= com.hrms.i18n.Language.text(request, "ui_f80fa6db5941", "Quê quán") %></label>
                            <input id="profileHometown" name="hometown" maxlength="200" readonly placeholder="<%= com.hrms.i18n.Language.text(request, "ui_443f1c4da0d2", "Chưa cập nhật") %>">
                        </div>
                        <div>
                            <label for="profileMajor"><%= com.hrms.i18n.Language.text(request, "ui_c19d76692baf", "Ngành học") %></label>
                            <input id="profileMajor" name="major" readonly placeholder="<%= com.hrms.i18n.Language.text(request, "ui_443f1c4da0d2", "Chưa cập nhật") %>">
                        </div>
                    </div>
                    <div class="form-actions">
                        <button type="button" id="editProfileButton" class="hidden" disabled><%= com.hrms.i18n.Language.text(request, "ui_605002477abd", "Cập nhật thông tin cá nhân") %></button>
                        <button type="submit" id="submitProfileButton" class="hidden"><%= com.hrms.i18n.Language.text(request, "ui_c05f72bd8c78", "Lưu thay đổi") %></button>
                        <button type="button" class="secondary hidden" id="discardProfileButton"><%= com.hrms.i18n.Language.text(request, "ui_043c52159fa5", "Hủy thay đổi") %></button>
                    </div>
                </form>
            </div>
            <div id="employmentTab" class="tab-panel detail-grid hidden"></div>
            <div id="salaryTab" class="tab-panel hidden"></div>
            <div id="accountTab" class="tab-panel detail-grid hidden"></div>
            <div id="documentsTab" class="tab-panel hidden">
                <p id="documentUpdateStatus" class="page-subtitle" role="status" aria-live="polite"><%= com.hrms.i18n.Language.text(request, "ui_e668337508c3", "Chỉ được thay đổi tài liệu khi có đợt cập nhật tài liệu đang mở.") %></p>
                <form id="myDocumentForm" class="form-grid" enctype="multipart/form-data">
                    <div>
                        <label><%= com.hrms.i18n.Language.text(request, "ui_bb295b750c07", "Tên tài liệu") %></label>
                        <input name="documentName" required disabled>
                    </div>
                    <div>
                        <label><%= com.hrms.i18n.Language.text(request, "ui_7c8dd9841819", "Loại tài liệu") %></label>
                        <select name="documentType" required disabled>
                            <option value=""><%= com.hrms.i18n.Language.text(request, "ui_8c2460772e05", "-- Chọn loại tài liệu --") %></option>
                        </select>
                    </div>
                    <div>
                        <label><%= com.hrms.i18n.Language.text(request, "ui_c7e9f7266ee2", "Chọn tệp") %></label>
                        <input name="file" type="file" accept=".pdf,.doc,.docx,.jpg,.png" required disabled>
                    </div>
                    <div class="form-actions">
                        <button type="submit" disabled><%= com.hrms.i18n.Language.text(request, "ui_498f1d88e704", "Tải tài liệu lên") %></button>
                    </div>
                </form>
                <table data-pagination="my-documents">
                    <thead>
                    <tr>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_bb295b750c07", "Tên tài liệu") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_7c8dd9841819", "Loại tài liệu") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_9c0863b0dcfc", "Ngày tải lên") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_04dcb5fbde87", "Người tải lên") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th>
                    </tr>
                    </thead>
                    <tbody id="myDocumentRows">
                    <tr><td colspan="5"><%= com.hrms.i18n.Language.text(request, "ui_5ce54c5a5e0c", "Chưa có tài liệu.") %></td></tr>
                    </tbody>
                </table>
            </div>
            <div id="historyTab" class="tab-panel hidden">
                <table data-pagination="my-profile-history">
                    <thead>
                    <tr>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_368c4575744a", "Ngày") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_bee3922ac0cb", "Người thay đổi") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_bd652e6ba1c0", "Giá trị trước") %></th>
                        <th><%= com.hrms.i18n.Language.text(request, "ui_41111d7751be", "Giá trị sau") %></th>
                    </tr>
                    </thead>
                    <tbody id="profileHistoryRows"></tbody>
                </table>
            </div>
            </div>
        </section>
    </main>


<script src="../js/employee.js?v=<%= application.getResource("/js/employee.js").openConnection().getLastModified() %>"></script>
<script src="../js/payroll-notifications.js?v=<%= application.getResource("/js/payroll-notifications.js").openConnection().getLastModified() %>"></script>
</body>
</html>
