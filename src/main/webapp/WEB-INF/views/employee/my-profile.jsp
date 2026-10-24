<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hồ sơ cá nhân - HRMs Cloud</title>
    <link rel="stylesheet" href="../css/lifecycle.css">
    <link rel="stylesheet" href="../css/organization.css">
    <link rel="stylesheet" href="../css/profile-update.css">
    <link rel="stylesheet" href="../css/employee-navigation.css?v=<%= application.getResource("/css/employee-navigation.css").openConnection().getLastModified() %>">
</head>
<body class="lc" data-page="my-profile" data-profile-role="<c:out value='${sessionScope.tenantPrincipal.roleCode}'/>">
    <c:set var="employeeNavPage" value="profile"/>
    <%@ include file="/WEB-INF/views/common/employee-navigation.jspf" %>

    <aside class="lc-sidebar">
        <a class="lc-brand" href="my-profile"><span class="lc-logo">H</span><span class="lc-brand-name">HRMs Cloud</span></a>
        <div class="lc-nav-label">CỔNG NHÂN VIÊN</div><nav aria-label="Chức năng cá nhân">
        <a class="active" href="my-profile">Hồ sơ cá nhân</a>
        <a  href="../attendance/employee-attendance">Chấm công cá nhân</a>
        </nav><a class="lc-signout" href="../logout">↪ <span>Đăng xuất</span></a>
    </aside>

    <header class="lc-topbar"><span>Cổng nhân viên <span class="lc-muted">/ Hồ sơ cá nhân</span></span><span class="lc-workspace">Workspace · <c:out value="${sessionScope.tenantPrincipal.workspaceCode}" default="—"/></span></header>
<%@ include file="/WEB-INF/views/common/appearance.jspf" %>
    <main class="lc-main content profile-update-page">
        <div class="lc-breadcrumb">Cổng nhân viên &nbsp; / &nbsp; Hồ sơ cá nhân</div>
        <div class="page-header">
            <div>
                <h1 id="profileName" class="page-title">Hồ sơ cá nhân</h1>
                <p class="page-subtitle">Mã nhân viên: <span id="profileCode">-</span></p>
            </div>
        </div>

        <div id="message" class="message"></div>

        <section class="panel personal-profile-layout">
            <nav class="tabs lc-tabs personal-profile-navigation" aria-label="Các mục hồ sơ cá nhân">
                <button type="button" class="tab-button active" data-tab="profile">Thông tin cá nhân</button>
                <button type="button" class="tab-button" data-tab="employment">Công việc</button>
                <button type="button" class="tab-button" data-tab="salary">Lương & phụ cấp</button>
                <button type="button" class="tab-button" data-tab="account">Tài khoản</button>
                <button type="button" class="tab-button" data-tab="documents">Tài liệu</button>
                <button type="button" class="tab-button" data-tab="history">Lịch sử</button>
            </nav>

            <div class="personal-profile-content">
            <div id="profileTab" class="tab-panel">
                <form id="myProfileForm">
                    <input type="hidden" name="csrfToken">
                    <p id="profileUpdateStatus" role="status">Đang kiểm tra đợt cập nhật hồ sơ...</p>
                    <div class="form-grid">
                        <div>
                            <label>Mã nhân viên</label>
                            <input name="employeeCode" readonly>
                        </div>
                        <div>
                            <label>Họ và tên</label>
                            <input name="fullName" readonly>
                        </div>
                        <div>
                            <label>Giới tính</label>
                            <div class="radio-row">
                                <label><input type="radio" name="gender" value="Male" disabled> Nam</label>
                                <label><input type="radio" name="gender" value="Female" disabled> Nữ</label>
                                <label><input type="radio" name="gender" value="Other" disabled> Khác</label>
                            </div>
                        </div>
                        <div>
                            <label>Ngày sinh</label>
                            <input name="dateOfBirth" type="date" readonly>
                        </div>
                        <div>
                            <label>Điện thoại</label>
                            <input name="phone" type="tel" maxlength="10" pattern="0[0-9]{9}" required readonly>
                        </div>
                        <div>
                            <label>Trạng thái làm việc</label>
                            <input name="status" readonly>
                        </div>
                        <div>
                            <label for="profileEmail">Email liên hệ</label>
                            <input id="profileEmail" name="email" type="email" maxlength="150" readonly placeholder="Chưa cập nhật">
                        </div>
                        <div>
                            <label for="profileIdentityNumber">Số CCCD/CMND</label>
                            <input id="profileIdentityNumber" name="identityNumber" readonly autocomplete="off" placeholder="Chưa cập nhật">
                        </div>
                        <div>
                            <label for="profileIdentityIssuedDate">Ngày cấp CCCD/CMND</label>
                            <input id="profileIdentityIssuedDate" name="identityIssuedDate" type="date" readonly>
                        </div>
                        <div>
                            <label for="profileIdentityIssuedPlace">Nơi cấp CCCD/CMND</label>
                            <input id="profileIdentityIssuedPlace" name="identityIssuedPlace" maxlength="200" readonly placeholder="Chưa cập nhật">
                        </div>
                        <div>
                            <label for="profilePermanentAddress">Địa chỉ thường trú</label>
                            <textarea id="profilePermanentAddress" name="permanentAddress" rows="2" maxlength="500" readonly placeholder="Chưa cập nhật"></textarea>
                        </div>
                        <div>
                            <label for="profileHometown">Quê quán</label>
                            <input id="profileHometown" name="hometown" maxlength="200" readonly placeholder="Chưa cập nhật">
                        </div>
                        <div>
                            <label for="profileMajor">Ngành học</label>
                            <input id="profileMajor" name="major" readonly placeholder="Chưa cập nhật">
                        </div>
                    </div>
                    <div class="form-actions">
                        <button type="button" id="editProfileButton" class="hidden" disabled>Cập nhật thông tin cá nhân</button>
                        <button type="submit" id="submitProfileButton" class="hidden">Lưu thay đổi</button>
                        <button type="button" class="secondary hidden" id="discardProfileButton">Hủy thay đổi</button>
                    </div>
                </form>
            </div>
            <div id="employmentTab" class="tab-panel detail-grid hidden"></div>
            <div id="salaryTab" class="tab-panel hidden"></div>
            <div id="accountTab" class="tab-panel detail-grid hidden"></div>
            <div id="documentsTab" class="tab-panel hidden">
                <p id="documentUpdateStatus" class="page-subtitle" role="status" aria-live="polite">Chỉ được thay đổi tài liệu khi có đợt cập nhật tài liệu đang mở.</p>
                <form id="myDocumentForm" class="form-grid" enctype="multipart/form-data">
                    <div>
                        <label>Tên tài liệu</label>
                        <input name="documentName" required disabled>
                    </div>
                    <div>
                        <label>Loại tài liệu</label>
                        <input name="documentType" placeholder="Hợp đồng, căn cước, chứng chỉ…" disabled>
                    </div>
                    <div>
                        <label>Chọn tệp</label>
                        <input name="file" type="file" accept=".pdf,.doc,.docx,.jpg,.png" required disabled>
                    </div>
                    <div class="form-actions">
                        <button type="submit" disabled>Tải tài liệu lên</button>
                    </div>
                </form>
                <table data-pagination="my-documents">
                    <thead>
                    <tr>
                        <th>Tên tài liệu</th>
                        <th>Loại tài liệu</th>
                        <th>Ngày tải lên</th>
                        <th>Người tải lên</th>
                        <th>Thao tác</th>
                    </tr>
                    </thead>
                    <tbody id="myDocumentRows">
                    <tr><td colspan="5">Chưa có tài liệu.</td></tr>
                    </tbody>
                </table>
            </div>
            <div id="historyTab" class="tab-panel hidden">
                <table data-pagination="my-profile-history">
                    <thead>
                    <tr>
                        <th>Ngày</th>
                        <th>Thao tác</th>
                        <th>Người thay đổi</th>
                        <th>Giá trị trước</th>
                        <th>Giá trị sau</th>
                    </tr>
                    </thead>
                    <tbody id="profileHistoryRows"></tbody>
                </table>
            </div>
            </div>
        </section>
    </main>
<script src="../js/employee.js?v=<%= application.getResource("/js/employee.js").openConnection().getLastModified() %>"></script>
</body>
</html>
