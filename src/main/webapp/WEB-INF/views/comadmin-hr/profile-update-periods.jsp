<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${updatePeriodHeading}"/></title>

    <link rel="stylesheet" href="../css/organization.css">
    <link rel="stylesheet" href="../css/profile-update.css?v=<%= System.currentTimeMillis() %>">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc" data-page="profile-update-periods" data-period-type="${updatePeriodType}">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content profile-update-page">
        <div class="lc-breadcrumb"><c:out value="${updatePeriodBreadcrumb}"/></div>
        <div class="page-header">
            <div>
                <h1 class="page-title"><c:out value="${updatePeriodHeading}"/></h1>
                <p class="page-subtitle"><c:out value="${updatePeriodSubtitle}"/></p>
            </div>
        </div>
        <div id="message" class="message" role="status"></div>

        <c:if test="${documentUpdatePeriods}">
            <section class="panel doc-type-manager-panel">
                <div class="profile-section-heading">
                    <span class="profile-section-icon">▤</span>
                    <div>
                        <h2>Danh mục loại tài liệu của công ty</h2>
                        <p>Tùy chỉnh các loại tài liệu mà công ty quản lý (ví dụ: CCCD / CMND, Bảng điểm, Hợp đồng lao động, Bằng đại học, Bằng thạc sĩ...).</p>
                    </div>
                </div>
                <div id="companyDocumentTypeList" class="doc-type-chip-list"></div>
                <div class="doc-type-add-bar">
                    <div class="doc-type-input-wrap">
                        <input type="text" id="newDocumentTypeName" name="documentTypeName" data-validation-label="tên loại tài liệu" maxlength="100" placeholder="Nhập tên loại tài liệu mới (VD: Bằng tiến sĩ, Chứng chỉ IELTS...)">
                        <small id="newDocumentTypeError" class="field-error"></small>
                    </div>
                    <button type="button" id="addDocumentTypeBtn">+ Thêm loại tài liệu</button>
                </div>
            </section>
        </c:if>

        <form id="profileUpdatePeriodForm" class="panel" novalidate>
            <input type="hidden" name="csrfToken">
            <div class="profile-section-heading"><span class="profile-section-icon">＋</span><div><h2><c:out value="${updatePeriodCreateHeading}"/></h2><p><c:out value="${updatePeriodCreateDescription}"/></p></div></div>
            <div class="form-grid">
                <div>
                    <label for="periodTitle"><%= com.hrms.i18n.Language.text(request, "ui_f46ca070b6f6", "Tên đợt cập nhật") %></label>
                    <input id="periodTitle" name="title" maxlength="150" placeholder="${documentUpdatePeriods ? 'VD: Bổ sung bằng cấp & CCCD đợt 1' : 'VD: Cập nhật thông tin liên hệ Q4'}" required>
                </div>
                <div>
                    <label for="periodStartsAt"><%= com.hrms.i18n.Language.text(request, "ui_95c3f9f5965f", "Bắt đầu") %></label>
                    <input id="periodStartsAt" name="startsAt" type="datetime-local" required>
                </div>
                <div>
                    <label for="periodEndsAt"><%= com.hrms.i18n.Language.text(request, "ui_9c1ae4f57bf2", "Kết thúc") %></label>
                    <input id="periodEndsAt" name="endsAt" type="datetime-local" required>
                </div>
            </div>

            <div class="doc-period-config-block">
                <div class="doc-config-header">
                    <label>${documentUpdatePeriods ? 'Loại tài liệu cần cập nhật trong đợt này *' : 'Loại thông tin nhân viên được phép cập nhật trong đợt này *'}</label>
                    <div class="doc-quick-links">
                        <button type="button" class="link-btn" id="selectAllDocTypesBtn">Chọn tất cả</button>
                        <span>·</span>
                        <button type="button" class="link-btn" id="clearAllDocTypesBtn">Bỏ chọn</button>
                    </div>
                </div>
                <div id="periodDocumentTypesSelector" class="doc-type-selector-grid"></div>
            </div>

            <div class="doc-period-config-block">
                <label>Phạm vi nhân viên áp dụng *</label>
                <div class="doc-scope-radios">
                    <label class="doc-scope-option">
                        <input type="radio" name="scopeType" value="ALL" checked>
                        <span><strong>Toàn công ty</strong> (Tất cả nhân viên đang làm việc)</span>
                    </label>
                    <label class="doc-scope-option">
                        <input type="radio" name="scopeType" value="SELECTED">
                        <span><strong>Cá nhân / Nhóm cụ thể</strong> (Chọn 1 hoặc một số nhân viên cập nhật riêng)</span>
                    </label>
                </div>
                <div id="selectedEmployeesPicker" class="doc-employee-picker hidden">
                    <div class="doc-employee-picker-toolbar">
                        <input type="search" id="employeePickerSearch" placeholder="Tìm theo tên, mã nhân viên, phòng ban...">
                        <div class="doc-quick-links">
                            <span id="selectedEmployeesCounter" class="lc-badge active">Đã chọn: 0 nhân viên</span>
                            <button type="button" class="link-btn" id="selectVisibleEmployeesBtn">Chọn danh sách đang lọc</button>
                            <span>·</span>
                            <button type="button" class="link-btn" id="clearSelectedEmployeesBtn">Bỏ chọn tất cả</button>
                        </div>
                    </div>
                    <div id="employeePickerList" class="doc-employee-picker-list"></div>
                </div>
            </div>

            <c:if test="${!documentUpdatePeriods}">
                <p>Có thể tạo đợt cập nhật hồ sơ cho toàn công ty hoặc tạo đợt riêng cho từng cá nhân. Nhân viên thuộc đợt đang mở chỉ được chỉnh sửa đúng các trường thông tin được chọn ở trên.</p>
            </c:if>
            <c:if test="${documentUpdatePeriods}">
                <p>Có thể tạo đợt cập nhật tài liệu cho toàn công ty hoặc tạo đợt cập nhật riêng cho từng cá nhân. Nhân viên thuộc đợt đang mở sẽ được tải lên đúng các loại tài liệu được chọn.</p>
            </c:if>
            <div class="form-actions"><button type="submit" id="createProfilePeriodButton"><%= com.hrms.i18n.Language.text(request, "ui_3fbd9d29d460", "Tạo đợt cập nhật") %></button></div>
        </form>
        <section class="panel">
            <div class="profile-section-heading"><div><h2><%= com.hrms.i18n.Language.text(request, "ui_367ca500e1f4", "Lịch sử các đợt cập nhật") %></h2><p><%= com.hrms.i18n.Language.text(request, "ui_88884717af34", "Theo dõi trạng thái và quản lý các đợt của công ty.") %></p></div></div>
            <div class="lc-table-wrap"><table data-pagination="${updatePeriodType}-update-periods">
                <thead>
                <tr>
                    <th>Tên đợt</th>
                    <th>${documentUpdatePeriods ? 'Loại tài liệu yêu cầu' : 'Loại thông tin cho phép'}</th>
                    <th>Phạm vi áp dụng</th>
                    <th>Bắt đầu</th>
                    <th>Kết thúc</th>
                    <th>Tiến độ cập nhật</th>
                    <th>Trạng thái</th>
                    <th>Thao tác</th>
                </tr>
                </thead>
                <tbody id="profilePeriodRows"></tbody>
            </table></div>
        </section>

        <dialog id="documentPeriodDetailModal" class="lc-modal doc-period-detail-modal">
            <div class="lc-modal-head">
                <div>
                    <h3 id="detailModalTitle">${documentUpdatePeriods ? 'Chi tiết đợt cập nhật tài liệu' : 'Chi tiết đợt cập nhật hồ sơ'}</h3>
                    <p id="detailModalSubtitle" class="page-subtitle" style="margin:4px 0 0"></p>
                </div>
                <button type="button" class="lc-close" id="closeDetailModalBtn" aria-label="Đóng">×</button>
            </div>
            <div class="doc-detail-body">
                <div id="detailRequiredTypesBar" class="doc-detail-types-bar"></div>
                <div id="detailSummaryCards" class="doc-detail-summary-grid"></div>

                <section class="doc-detail-section">
                    <h4>${documentUpdatePeriods ? 'Tài liệu nhân viên đã cập nhật trong đợt' : 'Chi tiết thông tin nhân viên đã cập nhật trong đợt'}</h4>
                    <div class="lc-table-wrap">
                        <table>
                            <thead>
                            <c:choose>
                                <c:when test="${documentUpdatePeriods}">
                                    <tr>
                                        <th>Nhân viên</th>
                                        <th>Phòng ban</th>
                                        <th>Tên tài liệu</th>
                                        <th>Loại tài liệu</th>
                                        <th>Thời gian tải lên</th>
                                        <th>Thao tác</th>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <tr>
                                        <th>Nhân viên</th>
                                        <th>Phòng ban</th>
                                        <th>Loại thông tin</th>
                                        <th>Giá trị trước</th>
                                        <th>Giá trị sau</th>
                                        <th>Thời gian cập nhật</th>
                                    </tr>
                                </c:otherwise>
                            </c:choose>
                            </thead>
                            <tbody id="detailUploadedDocsRows"></tbody>
                        </table>
                    </div>
                </section>

                <section class="doc-detail-section">
                    <div class="doc-detail-section-head">
                        <h4>Tiến độ theo nhân viên thuộc đợt</h4>
                        <div class="doc-detail-filter-bar">
                            <button type="button" class="small secondary active" data-emp-filter="ALL">Tất cả</button>
                            <button type="button" class="small secondary" data-emp-filter="UPDATED">Đã cập nhật</button>
                            <button type="button" class="small secondary" data-emp-filter="PENDING">Chưa cập nhật</button>
                        </div>
                    </div>
                    <div class="lc-table-wrap">
                        <table>
                            <thead>
                            <tr>
                                <th>Mã NV</th>
                                <th>Họ và tên</th>
                                <th>Phòng ban</th>
                                <th>${documentUpdatePeriods ? 'Đã cập nhật' : 'Thông tin đã cập nhật'}</th>
                                <th>${documentUpdatePeriods ? 'Còn thiếu' : 'Chưa thay đổi'}</th>
                                <th>Trạng thái</th>
                            </tr>
                            </thead>
                            <tbody id="detailEmployeeStatusRows"></tbody>
                        </table>
                    </div>
                </section>
            </div>
            <div class="lc-modal-foot">
                <button type="button" class="secondary" id="closeDetailModalFootBtn">Đóng</button>
            </div>
        </dialog>
    </main>


<script src="../js/employee.js?v=<%= application.getResource("/js/employee.js").openConnection().getLastModified() %>"></script>
<script src="../js/profile-update-periods.js?v=<%= System.currentTimeMillis() %>"></script>
</body>
</html>
