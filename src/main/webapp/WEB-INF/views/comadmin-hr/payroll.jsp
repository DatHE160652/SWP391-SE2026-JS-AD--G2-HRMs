<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="csrf-token" content="<c:out value='${payrollCsrf}'/>">
    <title><%= com.hrms.i18n.Language.text(request, "ui_78f95e303c84", "Quản lý lương · HRMs Cloud") %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/payroll.css?v=20261026-payslips-1">
<%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
</head>
<body class="lc workspace-payroll" data-page="${salaryPage ? 'salaries' : 'payroll'}" data-context="${pageContext.request.contextPath}">
<div class="pay-shell">
    <%@ include file="/WEB-INF/views/common/tenant-shell.jspf" %>
    <main class="lc-main pay-main">

        <div class="pay-content">
            <div class="page-heading"><div><div class="eyebrow">PAYROLL MANAGEMENT</div><h1><c:set var="i18nConditionalLabel" value="${salaryPage ? 'Lương cơ bản' : 'Quản lý kỳ lương'}"/><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("i18nConditionalLabel")) %>'/></h1><p><c:set var="i18nConditionalLabel" value="${salaryPage ? 'Theo dõi mức lương cơ bản, phụ cấp trách nhiệm và phụ cấp đi lại của nhân viên theo hợp đồng lao động đã kích hoạt.' : 'Từ dữ liệu lương đến phiếu lương — mọi bước trong cùng một không gian.'}"/><c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("i18nConditionalLabel")) %>'/></p></div><c:if test="${not salaryPage}"><div class="toolbar-fields"><button class="btn secondary" id="openFormulaSettings" type="button">⚙ Cấu hình công thức lương</button><button class="btn primary" id="newPeriod"><%= com.hrms.i18n.Language.text(request, "ui_efeac4fb7566", "＋ Tạo kỳ lương") %></button></div></c:if></div>
            <div id="feedback" class="feedback" role="status" aria-live="polite" hidden></div>
            <c:choose><c:when test="${salaryPage}">
                <div class="info-note"><%= com.hrms.i18n.Language.text(request, "ui_65457966627b", "Mức lương cơ bản, phụ cấp trách nhiệm và phụ cấp đi lại đang có hiệu lực hôm nay. Lương cơ bản không cập nhật thủ công mà chỉ được đồng bộ tự động từ hợp đồng lao động đã kích hoạt.") %></div>
                <section class="pay-card"><div class="card-toolbar"><h2><%= com.hrms.i18n.Language.text(request, "ui_ef489d24a6a3", "Danh sách mức lương") %></h2><label class="search-field"><span>⌕</span><input id="salarySearch" type="search" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_a43bbd25194e", "Tìm tên, mã nhân viên hoặc phòng ban") %>" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_be33b65f40bc", "Tìm nhân viên") %>"></label></div><div class="table-scroll"><table><thead><tr><th><%= com.hrms.i18n.Language.text(request, "ui_14ddd70d2564", "Nhân viên") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_6f358f848e2e", "Phòng ban") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></th><th class="number"><%= com.hrms.i18n.Language.text(request, "ui_832b5c6af3ff", "Lương cơ bản (VND)") %></th><th class="number"><%= com.hrms.i18n.Language.text(request, "ui_ac5d493eaa5e", "Phụ cấp trách nhiệm") %></th><th class="number">Phụ cấp đi lại</th><th><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th></tr></thead><tbody id="salaryRows"><tr><td colspan="7" class="empty"><%= com.hrms.i18n.Language.text(request, "ui_e6b584854879", "Đang tải dữ liệu…") %></td></tr></tbody></table></div><div class="pagination" id="salaryPagination"></div></section>
            </c:when><c:otherwise>
                <div id="formulaSummaryBanner" class="info-note" style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;margin-bottom:14px;">
                    <div><strong>Công thức tính lương của công ty:</strong> <span id="formulaSummaryText">Thực nhận = ((Lương cơ bản × Công thực) / Số công tiêu chuẩn) + Phụ cấp trách nhiệm + Phụ cấp đi lại + Thưởng</span></div>
                </div>
                <section class="workflow-strip" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_b1b59e711b5a", "Quy trình tính lương") %>"><div><span>01</span><strong><%= com.hrms.i18n.Language.text(request, "ui_021c6e1c4d74", "Chuẩn bị") %></strong><small><%= com.hrms.i18n.Language.text(request, "ui_e7351d02e30c", "Lương & điều chỉnh") %></small></div><i>→</i><div><span>02</span><strong><%= com.hrms.i18n.Language.text(request, "ui_937bcc18133e", "Tính lương") %></strong><small><%= com.hrms.i18n.Language.text(request, "ui_aa88baac3881", "Kết quả tự động") %></small></div><i>→</i><div><span>03</span><strong><%= com.hrms.i18n.Language.text(request, "ui_ec0a446ca1fc", "Rà soát & duyệt") %></strong><small><%= com.hrms.i18n.Language.text(request, "ui_8e29bdbfc767", "Xác nhận bảng lương") %></small></div><i>→</i><div><span>04</span><strong><%= com.hrms.i18n.Language.text(request, "ui_3bec228bfe1a", "Công bố") %></strong><small><%= com.hrms.i18n.Language.text(request, "ui_f169d1a87a72", "Phiếu lương & thông báo") %></small></div></section>
                <section class="pay-card periods-card"><div class="card-toolbar"><h2><%= com.hrms.i18n.Language.text(request, "ui_1f22d2859e01", "Kỳ lương") %> <span id="periodCount" class="count" aria-live="polite">0</span></h2><div class="toolbar-fields"><select id="yearFilter" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_a89c934bbacb", "Lọc năm") %>"><option value=""><%= com.hrms.i18n.Language.text(request, "ui_b97f711f0b89", "Tất cả năm") %></option></select><select id="statusFilter" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_fa369313d3b4", "Lọc trạng thái") %>"><option value=""><%= com.hrms.i18n.Language.text(request, "ui_872e04b463ff", "Tất cả trạng thái") %></option><option>Draft</option><option>Calculated</option><option>Reviewed</option><option>Approved</option></select><button class="btn secondary" id="refreshPeriods" type="button"><%= com.hrms.i18n.Language.text(request, "ui_9452d72a84cf", "↻ Tải lại") %></button></div></div><div id="periodList" class="period-list" role="group" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_5e96d41d7641", "Danh sách kỳ lương") %>" aria-busy="true"><div class="empty"><%= com.hrms.i18n.Language.text(request, "ui_64f660fad581", "Đang tải kỳ lương…") %></div></div></section>
                <div id="periodDetailState" class="info-note" role="status" aria-live="polite" hidden></div>
                <section id="periodWorkspace" hidden>
                    <div class="period-heading"><div><div class="eyebrow"><%= com.hrms.i18n.Language.text(request, "ui_e8e259c18dc0", "KỲ LƯƠNG ĐANG CHỌN") %></div><h2 id="periodTitle"></h2><div id="periodBadges" class="badge-row"></div></div><div id="periodActions" class="action-row"></div></div>
                    <div id="periodNotice" class="info-note"></div>
                    <div id="periodReviewHistory" class="info-note" hidden></div>
                    <section class="pay-card adjustments-card" aria-labelledby="adjustmentsTitle">
                        <div class="card-toolbar"><div><h2 id="adjustmentsTitle">Điều chỉnh thưởng</h2><p class="subtle">Danh sách nhân viên trong công ty. Bạn có thể thiết lập đợt thưởng (Lương tháng 13 / Lễ Tết) tự động hoặc điều chỉnh riêng cho từng nhân viên.</p></div><div class="toolbar-fields"><button class="btn primary" id="openBonusCampaign" type="button">🎁 Thiết lập đợt thưởng (Tháng 13 / Lễ Tết)</button></div><span id="adjustmentLock" class="badge closed" hidden><%= com.hrms.i18n.Language.text(request, "ui_bcc435973804", "Chỉ xem · Kỳ đã khóa") %></span></div>
                        <div class="table-filter"><label class="search-field"><span>⌕</span><input id="adjustmentSearch" type="search" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_c80238f61d7b", "Tìm tên hoặc mã nhân viên") %>" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_db845fbaf56b", "Tìm nhân viên để điều chỉnh") %>"></label><span id="adjustmentCount" class="subtle"></span></div>
                        <div class="table-scroll"><table><thead><tr><th><%= com.hrms.i18n.Language.text(request, "ui_84979604c762", "Mã nhân viên") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_3da3c8c2738b", "Họ tên") %></th><th class="number"><%= com.hrms.i18n.Language.text(request, "ui_f6195db41766", "Lương cơ bản") %></th><th class="number"><%= com.hrms.i18n.Language.text(request, "ui_1dff4c8c3481", "Thưởng") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_02f03f68e75d", "Ghi chú") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th></tr></thead><tbody id="adjustmentRows"></tbody></table></div>
                        <div class="pagination" id="adjustmentPagination"></div>
                        <p class="adjustment-help subtle"><%= com.hrms.i18n.Language.text(request, "ui_bbcc23100aa1", "Nhân viên chưa có hợp đồng hoặc mức lương hợp lệ vẫn có thể lưu điều chỉnh. Kết quả tính lương chỉ bao gồm nhân viên đủ điều kiện trong kỳ.") %></p>
                    </section>
                    <section class="pay-metrics"><div class="metric"><small><%= com.hrms.i18n.Language.text(request, "ui_d798a23f9959", "Nhân viên trong kỳ") %></small><strong id="metricEmployees">—</strong><span><%= com.hrms.i18n.Language.text(request, "ui_1c5cd70dab50", "Hồ sơ thuộc công ty") %></span></div><div class="metric"><small><%= com.hrms.i18n.Language.text(request, "ui_835bbc822b95", "Tổng thu nhập") %></small><strong id="metricIncome">—</strong><span><%= com.hrms.i18n.Language.text(request, "ui_49b9b0092cb4", "Lương + Phụ cấp + Thưởng · VND") %></span></div><div class="metric"><small>Tổng tiền thưởng</small><strong id="metricBonus">—</strong><span>Thưởng kỳ & tháng 13 · VND</span></div><div class="metric highlight"><small><%= com.hrms.i18n.Language.text(request, "ui_4e66b7d05033", "Tổng thực nhận") %></small><strong id="metricNet">—</strong><span id="metricNetNote"><%= com.hrms.i18n.Language.text(request, "ui_3a80f5c6f33c", "Chưa tính lương") %></span></div></section>
                    <section class="pay-card"><div class="card-toolbar"><div><h2><%= com.hrms.i18n.Language.text(request, "ui_8a8ce94e2e7c", "Bảng lương nhân viên") %></h2><p class="subtle" id="payrollFormulaSubtle">((Lương cơ bản × Công thực) / Số công tiêu chuẩn) + Phụ cấp trách nhiệm + Phụ cấp đi lại + Thưởng = Thực nhận</p></div><div class="toolbar-fields"><button class="btn secondary" id="templateButton"><%= com.hrms.i18n.Language.text(request, "ui_50bacaa8f160", "↓ File mẫu Excel") %></button><button class="btn secondary" id="importButton"><%= com.hrms.i18n.Language.text(request, "ui_d5f91186d645", "↑ Nhập Excel") %></button></div></div><div class="table-filter"><label class="search-field"><span>⌕</span><input type="search" id="rowSearch" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_c80238f61d7b", "Tìm tên hoặc mã nhân viên") %>" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_608ea43940cf", "Tìm nhân viên trong kỳ") %>"></label><span id="rowCount" class="subtle"></span></div><div class="table-scroll"><table><thead><tr><th><%= com.hrms.i18n.Language.text(request, "ui_84979604c762", "Mã nhân viên") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_3da3c8c2738b", "Họ tên") %></th><th class="number"><%= com.hrms.i18n.Language.text(request, "ui_f6195db41766", "Lương cơ bản") %></th><th class="number">Công thực / Chuẩn</th><th class="number">Lương theo công</th><th class="number"><%= com.hrms.i18n.Language.text(request, "ui_ac5d493eaa5e", "Phụ cấp trách nhiệm") %></th><th class="number">Phụ cấp đi lại</th><th class="number"><%= com.hrms.i18n.Language.text(request, "ui_1dff4c8c3481", "Thưởng") %></th><th class="number"><%= com.hrms.i18n.Language.text(request, "ui_bda3b5abe5e4", "Thực nhận") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th></tr></thead><tbody id="payrollRows"></tbody></table></div><div class="pagination" id="rowPagination"></div></section>
                </section>
            </c:otherwise></c:choose>
            <footer class="pay-footer">HRMs Cloud <span>•</span> <%= com.hrms.i18n.Language.text(request, "ui_72fe236bc647", "Quản lý lương trong không gian công ty của bạn") %></footer>
        </div>
    </main>
</div>
<dialog id="createDialog" class="pay-dialog"><form id="createForm"><div class="dialog-heading"><div><div class="eyebrow"><%= com.hrms.i18n.Language.text(request, "ui_436b6784623d", "KỲ TÍNH LƯƠNG MỚI") %></div><h2><%= com.hrms.i18n.Language.text(request, "ui_fab76f892682", "Tạo kỳ lương") %></h2></div><button type="button" class="icon-button" data-dismiss aria-label="<%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %>">×</button></div>
<p class="subtle"><%= com.hrms.i18n.Language.text(request, "ui_e3f8da69a504", "Mỗi tháng chỉ có một kỳ lương trong công ty. Kỳ mới bắt đầu ở Draft.") %></p>
<div class="info-note" style="margin-bottom: 12px; font-size: 12px; background: #eff6ff; border-color: #bfdbfe; color: #1e3a8a;">
    💡 <strong>Quy định:</strong> Chỉ tạo kỳ lương khi tháng làm việc đã kết thúc và đã chốt xong dữ liệu chấm công (chỉ tạo từ tháng trước trở về trước).
</div>
<label><%= com.hrms.i18n.Language.text(request, "ui_3b8d69c9f892", "Tên kỳ lương") %><input name="periodName" id="createPeriodName" maxlength="100" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_5c8b0d0e47cc", "Tự đặt theo tháng và năm nếu để trống") %>"></label>
<div class="form-pair">
    <label><%= com.hrms.i18n.Language.text(request, "ui_0e2b309955fe", "Tháng") %>
        <select name="month" id="createPeriodMonth" required>
            <c:forEach begin="1" end="12" var="m">
                <option value="${m}"><%= com.hrms.i18n.Language.text(request, "ui_0e2b309955fe", "Tháng") %> ${m}</option>
            </c:forEach>
        </select>
    </label>
    <label><%= com.hrms.i18n.Language.text(request, "ui_98926c7e765d", "Năm") %>
        <input name="year" id="createPeriodYear" type="number" min="2000" max="9999" required>
    </label>
</div>
<div class="dialog-error" role="alert" hidden></div>
<div class="dialog-actions"><button type="button" class="btn secondary" data-dismiss><%= com.hrms.i18n.Language.text(request, "ui_34ca764caf22", "Hủy") %></button><button class="btn primary" type="submit"><%= com.hrms.i18n.Language.text(request, "ui_fab76f892682", "Tạo kỳ lương") %></button></div></form></dialog>
<dialog id="salaryDialog" class="pay-dialog"><form id="salaryForm">
    <div class="dialog-heading"><h2><%= com.hrms.i18n.Language.text(request, "ui_9b3a291b86eb", "Cập nhật lương cơ bản") %></h2><button type="button" class="icon-button" data-dismiss aria-label="<%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %>">×</button></div>
    <p id="salaryEmployee" class="employee-label"></p><input name="employeeId" type="hidden">
    <div class="salary-amount-field">
        <label for="salaryAmount"><%= com.hrms.i18n.Language.text(request, "ui_832b5c6af3ff", "Lương cơ bản (VND)") %></label>
        <div class="salary-amount-control">
            <input id="salaryAmount" name="baseSalary" type="number" min="0" max="9999999999999999.99" step="0.01" data-salary-step="100000" required aria-describedby="salaryStepHint">
            <div class="salary-amount-buttons">
                <button type="button" data-salary-direction="1" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_bf83a0c13a43", "Tăng 100.000 đồng") %>" title="<%= com.hrms.i18n.Language.text(request, "ui_bf83a0c13a43", "Tăng 100.000 đồng") %>">▴</button>
                <button type="button" data-salary-direction="-1" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_64d0a77203b5", "Giảm 100.000 đồng") %>" title="<%= com.hrms.i18n.Language.text(request, "ui_64d0a77203b5", "Giảm 100.000 đồng") %>">▾</button>
            </div>
        </div>
        <small id="salaryStepHint" class="subtle"><%= com.hrms.i18n.Language.text(request, "ui_948fe1647d0e", "Mỗi lần bấm tăng/giảm 100.000đ. Bạn có thể gõ trực tiếp mức lương.") %></small>
    </div>
    <label><%= com.hrms.i18n.Language.text(request, "ui_651d62aeac55", "Ngày hiệu lực") %><input name="effectiveFrom" type="date" min="2000-01-01" max="9999-12-31" required></label>
    <p class="subtle"><%= com.hrms.i18n.Language.text(request, "ui_16c7f53c5f50", "Lưu thành lịch sử lương, giữ các mức lương trước ngày này và lịch lương đã hẹn sau đó. Thay đổi giữa tháng cần có quy tắc được xác nhận trước khi tính kỳ đó.") %></p>
    <div class="dialog-error" role="alert" hidden></div><div class="dialog-actions"><button class="btn secondary" type="button" data-dismiss><%= com.hrms.i18n.Language.text(request, "ui_34ca764caf22", "Hủy") %></button><button class="btn primary" type="submit"><%= com.hrms.i18n.Language.text(request, "ui_ca8b15656335", "Lưu mức lương") %></button></div>
</form></dialog>
<dialog id="calculationPreviewDialog" class="pay-dialog wide" aria-labelledby="calculationPreviewTitle"><div class="dialog-heading"><h2 id="calculationPreviewTitle"><%= com.hrms.i18n.Language.text(request, "ui_2d9d31d8c93c", "Xem trước bảng lương") %></h2><button type="button" class="icon-button" data-dismiss aria-label="<%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %>">×</button></div><div id="calculationPreviewContent" aria-live="polite"></div><div class="dialog-error" role="alert" hidden></div><div class="dialog-actions"><button type="button" class="btn secondary" data-dismiss><%= com.hrms.i18n.Language.text(request, "ui_79294ac84b35", "Đóng xem trước") %></button></div></dialog>
<dialog id="adjustmentDialog" class="pay-dialog" aria-labelledby="adjustmentTitle"><form id="adjustmentForm" novalidate>
    <div class="dialog-heading"><h2 id="adjustmentTitle">Điều chỉnh thưởng</h2><button type="button" class="icon-button" data-dismiss aria-label="<%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %>">×</button></div>
    <p id="adjustmentEmployee" class="employee-label"></p><input name="employeeId" type="hidden">
    <label><%= com.hrms.i18n.Language.text(request, "ui_a8c17a9c5995", "Thưởng / Bonus (VND)") %><input name="bonus" type="number" min="0" max="9999999999999999.99" step="0.01" required aria-describedby="bonusError"><small id="bonusError" class="adjustment-field-error" data-adjustment-error="bonus" role="alert" hidden></small></label>
    <label><%= com.hrms.i18n.Language.text(request, "ui_02f03f68e75d", "Ghi chú") %><textarea name="note" maxlength="300" rows="3" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_9b490133cc84", "Lý do thưởng (ví dụ: thưởng hiệu quả công việc, thưởng dự án...)") %>" aria-describedby="noteError"></textarea><small id="noteError" class="adjustment-field-error" data-adjustment-error="note" role="alert" hidden></small></label>
    <div class="info-note"><%= com.hrms.i18n.Language.text(request, "ui_eaf3ded7db16", "Thay đổi điều chỉnh đưa kỳ về Draft và xóa kết quả tính cũ để tính, rà soát và duyệt lại.") %></div>
    <p id="adjustmentSaveState" class="subtle" role="status" aria-live="polite"><%= com.hrms.i18n.Language.text(request, "ui_9a6788cf37de", "Chưa có thay đổi.") %></p>
    <div class="dialog-error" role="alert" hidden></div>
    <div class="dialog-actions"><button class="btn secondary" type="button" data-dismiss><%= com.hrms.i18n.Language.text(request, "ui_34ca764caf22", "Hủy") %></button><button class="btn primary" type="submit" disabled><%= com.hrms.i18n.Language.text(request, "ui_46419bee0b3f", "Lưu điều chỉnh") %></button></div>
</form></dialog>
<dialog id="importDialog" class="pay-dialog wide" style="max-width:960px;" aria-labelledby="importTitle"><form id="importForm">
    <div class="dialog-heading">
        <div>
            <div class="eyebrow">BẢNG LƯƠNG TỪ EXCEL</div>
            <h2 id="importTitle">Nhập bảng lương nhân viên từ Excel</h2>
        </div>
        <button type="button" class="icon-button" data-dismiss aria-label="<%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %>">×</button>
    </div>
    <p class="subtle">Nhập bảng lương đầy đủ cho nhân viên (đặc biệt hữu ích khi nhập các tháng cũ chưa có dữ liệu chấm công). Tải file mẫu bên dưới để xem danh sách nhân viên và mức lương cơ bản/phụ cấp đã chuẩn bị sẵn.</p>
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:12px;">
        <button type="button" class="btn secondary" id="downloadTemplateInDialog" style="font-size:13px;">↓ Tải file mẫu Excel kỳ này</button>
        <span class="subtle" style="font-size:12px;">Đã điền sẵn danh sách nhân viên, công chuẩn (26), lương cơ bản & phụ cấp.</span>
    </div>
    <label class="upload-field"><%= com.hrms.i18n.Language.text(request, "ui_30752557c3ec", "Chọn file .xlsx hoặc .xls") %><input id="importFile" name="file" type="file" accept=".xlsx,.xls" required><small><%= com.hrms.i18n.Language.text(request, "ui_3475b0603658", "Tối đa 5 MB / 5.000 dòng.") %></small></label>
    <div id="importPreview" hidden></div>
    <div class="dialog-error" role="alert" hidden></div>
    <div class="dialog-actions"><button type="button" class="btn secondary" data-dismiss><%= com.hrms.i18n.Language.text(request, "ui_34ca764caf22", "Hủy") %></button><button id="previewImport" type="button" class="btn secondary"><%= com.hrms.i18n.Language.text(request, "ui_6f65b67fd503", "Kiểm tra file") %></button><button id="saveImport" type="submit" class="btn primary" disabled>Nhập bảng lương</button></div>
</form></dialog>
<dialog id="confirmDialog" class="pay-dialog" aria-labelledby="confirmTitle" aria-describedby="confirmText"><form id="confirmForm"><div class="dialog-heading"><h2 id="confirmTitle"></h2><button type="button" class="icon-button" data-dismiss aria-label="<%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %>">×</button></div><p id="confirmText"></p><div id="confirmSummary" class="info-note" hidden></div><div class="dialog-error" role="alert" hidden></div><div class="dialog-actions"><button class="btn secondary" type="button" data-dismiss><%= com.hrms.i18n.Language.text(request, "ui_34ca764caf22", "Hủy") %></button><button class="btn primary" type="submit" id="confirmSubmit"><%= com.hrms.i18n.Language.text(request, "ui_d53daf2f4654", "Xác nhận") %></button></div></form></dialog>
<dialog id="formulaSettingsDialog" class="pay-dialog wide"><form id="formulaSettingsForm">
    <div class="dialog-heading">
        <div>
            <div class="eyebrow">CẤU HÌNH CÔNG TY</div>
            <h2>Cấu hình công thức tính lương</h2>
        </div>
        <button type="button" class="icon-button" data-dismiss aria-label="<%= com.hrms.i18n.Language.text(request, "ui_d2b73ab2ada1", "Đóng") %>">×</button>
    </div>
    <p class="subtle">Thiết lập công thức tính lương riêng cho công ty của bạn. Thay đổi sẽ áp dụng cho các kỳ lương được tính hoặc tính lại từ thời điểm này.</p>
    <div id="formulaDialogPreview" class="info-note" style="margin-bottom:14px;background:#eff6ff;border-color:#bfdbfe;color:#1e3a8a;"></div>
    <div class="form-pair">
        <label>Cách tính lương chính
            <select name="salaryCalculationMode">
                <option value="WORKDAYS">Theo ngày công: (Lương cơ bản × Công thực) / Số công tiêu chuẩn</option>
                <option value="FIXED_MONTHLY">Cố định theo tháng (100% Lương cơ bản hợp đồng)</option>
            </select>
        </label>
        <label>Cách tính số công tiêu chuẩn / tháng
            <select name="standardDaysMode" id="standardDaysModeSelect">
                <option value="MON_FRI">Tự động đếm Thứ 2 – Thứ 6 trong tháng (Nghỉ T7, CN)</option>
                <option value="MON_SAT_HALF">Tự động đếm Thứ 2 – Sáng Thứ 7 (Thứ 7 = 0.5 công)</option>
                <option value="MON_SAT">Tự động đếm Thứ 2 – Thứ 7 trong tháng (Nghỉ CN)</option>
                <option value="CUSTOM_WEEKDAYS">Tùy chỉnh hệ số công từng thứ trong tuần (T2 → CN)</option>
                <option value="FIXED">Tự nhập số ngày công chuẩn cố định mỗi tháng</option>
            </select>
        </label>
    </div>
    <div id="weekdayCustomizeBox" style="margin-top:10px;padding:12px;border:1px solid #cbd5e1;border-radius:10px;background:#f8fafc;">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap;margin-bottom:8px;">
            <strong style="font-size:13px;color:#1e293b;">Tùy chỉnh hệ số ngày công theo từng thứ trong tuần:</strong>
            <span id="weekdaySummaryBadge" class="subtle" style="font-weight:600;color:#2563eb;"></span>
        </div>
        <div style="display:grid;grid-template-columns:repeat(7,minmax(78px,1fr));gap:8px;">
            <label style="font-size:12px;text-align:center;">Thứ 2
                <select data-weekday-index="0" style="margin-top:4px;padding:6px 4px;font-size:12px;">
                    <option value="1">1.0 công</option><option value="0.5">0.5 công</option><option value="0">Nghỉ (0)</option>
                </select>
            </label>
            <label style="font-size:12px;text-align:center;">Thứ 3
                <select data-weekday-index="1" style="margin-top:4px;padding:6px 4px;font-size:12px;">
                    <option value="1">1.0 công</option><option value="0.5">0.5 công</option><option value="0">Nghỉ (0)</option>
                </select>
            </label>
            <label style="font-size:12px;text-align:center;">Thứ 4
                <select data-weekday-index="2" style="margin-top:4px;padding:6px 4px;font-size:12px;">
                    <option value="1">1.0 công</option><option value="0.5">0.5 công</option><option value="0">Nghỉ (0)</option>
                </select>
            </label>
            <label style="font-size:12px;text-align:center;">Thứ 5
                <select data-weekday-index="3" style="margin-top:4px;padding:6px 4px;font-size:12px;">
                    <option value="1">1.0 công</option><option value="0.5">0.5 công</option><option value="0">Nghỉ (0)</option>
                </select>
            </label>
            <label style="font-size:12px;text-align:center;">Thứ 6
                <select data-weekday-index="4" style="margin-top:4px;padding:6px 4px;font-size:12px;">
                    <option value="1">1.0 công</option><option value="0.5">0.5 công</option><option value="0">Nghỉ (0)</option>
                </select>
            </label>
            <label style="font-size:12px;text-align:center;">Thứ 7
                <select data-weekday-index="5" style="margin-top:4px;padding:6px 4px;font-size:12px;">
                    <option value="1">1.0 công</option><option value="0.5">0.5 công</option><option value="0">Nghỉ (0)</option>
                </select>
            </label>
            <label style="font-size:12px;text-align:center;">Chủ nhật
                <select data-weekday-index="6" style="margin-top:4px;padding:6px 4px;font-size:12px;">
                    <option value="1">1.0 công</option><option value="0.5">0.5 công</option><option value="0">Nghỉ (0)</option>
                </select>
            </label>
        </div>
        <small class="subtle" style="display:block;margin-top:8px;">Bạn có thể chỉnh trực tiếp hệ số công của bất kỳ thứ nào trong tuần (1.0 công = cả ngày, 0.5 công = nửa ngày, 0 = ngày nghỉ).</small>
    </div>
    <div id="fixedStandardDaysField" style="margin-top:10px;padding:12px;border:1px solid #cbd5e1;border-radius:10px;background:#f8fafc;">
        <label style="font-weight:600;color:#1e293b;">Nhập số ngày công tiêu chuẩn cố định / tháng (tùy chỉnh)
            <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;margin-top:6px;">
                <input name="fixedStandardDays" type="number" min="1" max="31" step="0.5" value="22" style="max-width:160px;font-weight:600;">
                <span class="subtle">ngày công / tháng · Chọn nhanh:</span>
                <button type="button" class="btn secondary" data-fixed-days-preset="20" style="padding:4px 10px;font-size:12px;">20 công</button>
                <button type="button" class="btn secondary" data-fixed-days-preset="22" style="padding:4px 10px;font-size:12px;">22 công</button>
                <button type="button" class="btn secondary" data-fixed-days-preset="24" style="padding:4px 10px;font-size:12px;">24 công</button>
                <button type="button" class="btn secondary" data-fixed-days-preset="26" style="padding:4px 10px;font-size:12px;">26 công</button>
            </div>
        </label>
    </div>
    <div style="display:grid;gap:10px;margin:14px 0;padding:12px;border:1px solid #e2e8f0;border-radius:10px;background:#f8fafc;">
        <strong style="font-size:13px;color:#334155;">Các thành phần đưa vào công thức lương thực nhận:</strong>
        <label style="display:flex;align-items:center;gap:8px;flex-direction:row;font-weight:500;cursor:pointer;">
            <input type="checkbox" name="includePaidLeave" value="true" checked style="width:16px;height:16px;">
            <span>Cộng ngày nghỉ phép có lương đã duyệt vào <strong>Công thực</strong></span>
        </label>
        <label style="display:flex;align-items:center;gap:8px;flex-direction:row;font-weight:500;cursor:pointer;">
            <input type="checkbox" name="includeResponsibilityAllowance" value="true" checked style="width:16px;height:16px;">
            <span>Cộng <strong>Phụ cấp trách nhiệm</strong> (theo Cấp trách nhiệm / Job Level)</span>
        </label>
        <label style="display:flex;align-items:center;gap:8px;flex-direction:row;font-weight:500;cursor:pointer;">
            <input type="checkbox" name="includeTravelAllowance" value="true" checked style="width:16px;height:16px;">
            <span>Cộng <strong>Phụ cấp đi lại</strong> (theo Hợp đồng lao động đang hiệu lực)</span>
        </label>
    </div>
    <div class="dialog-error" role="alert" hidden></div>
    <div class="dialog-actions">
        <button class="btn secondary" type="button" data-dismiss><%= com.hrms.i18n.Language.text(request, "ui_34ca764caf22", "Hủy") %></button>
        <button class="btn primary" type="submit">Lưu cấu hình công thức</button>
    </div>
</form></dialog>
<script src="${pageContext.request.contextPath}/js/payroll.js?v=<%= application.getResource("/js/payroll.js").openConnection().getLastModified() %>"></script>
</body></html>
