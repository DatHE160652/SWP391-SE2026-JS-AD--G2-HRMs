<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="csrf-token" content="<c:out value='${payrollCsrf}'/>">
    <title>Quản lý lương · HRMs Cloud</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/payroll.css?v=20261026-payslips-1">
</head>
<body data-page="${salaryPage ? 'salaries' : 'payroll'}" data-context="${pageContext.request.contextPath}">
<div class="pay-shell">
    <aside class="pay-sidebar">
        <a class="pay-brand" href="${pageContext.request.contextPath}/payroll"><span class="brand-mark">H</span><span>HRMs <b>Cloud</b><small>Không gian doanh nghiệp</small></span></a>
        <div class="workspace-name"><span class="workspace-icon">▦</span><div><strong><c:out value="${payrollPrincipal.companyName}"/></strong><small>Quản lý lương</small></div></div>
        <c:if test="${payrollPrincipal.companyAdministrator}">
        <div class="nav-caption">QUẢN TRỊ NHÂN SỰ</div>
        <a class="pay-nav" href="${pageContext.request.contextPath}/employee/employee-list"><span>♙</span> Nhân viên</a>
        <a class="pay-nav" href="${pageContext.request.contextPath}/department/department-list"><span>▦</span> Phòng ban</a>
        <a class="pay-nav" href="${pageContext.request.contextPath}/attendance/company-attendance"><span>◷</span> Chấm công</a>
        </c:if>
        <c:if test="${payrollPrincipal.roleCode == 'MANAGER'}">
        <div class="nav-caption">QUẢN LÝ NHÂN SỰ</div>
        <a class="pay-nav" href="${pageContext.request.contextPath}/attendance/manager-attendance"><span>◷</span> Điểm danh nhân sự</a>
        <a class="pay-nav" href="${pageContext.request.contextPath}/employee/my-profile?nav=manager"><span>♙</span> Hồ sơ cá nhân</a>
        </c:if>
        <div class="nav-caption">LƯƠNG & PHÚC LỢI</div>
        <a class="pay-nav ${salaryPage ? '' : 'selected'}" href="${pageContext.request.contextPath}/payroll"><span>▤</span> Kỳ tính lương</a>
        <a class="pay-nav ${salaryPage ? 'selected' : ''}" href="${pageContext.request.contextPath}/payroll/salaries"><span>₫</span> Lương cơ bản</a>
        <a class="pay-nav" href="${pageContext.request.contextPath}/payroll?view=payslips"><span>▧</span> Quản lý phiếu lương</a>
        <c:if test="${not empty payrollPrincipal.employeeId}"><a class="pay-nav" href="${pageContext.request.contextPath}/payslips"><span>▧</span> Phiếu lương của tôi</a></c:if>
        <div class="sidebar-note"><strong>Lương chính xác, minh bạch</strong><p>Rà soát và phê duyệt trước khi gửi phiếu lương đến nhân viên.</p></div>
        <a class="pay-nav logout" href="${pageContext.request.contextPath}/logout">↪ Đăng xuất</a>
    </aside>
    <main class="pay-main">
        <header class="pay-topbar"><span>Không gian công ty <span class="crumb">/</span> <strong>${salaryPage ? 'Lương cơ bản' : 'Quản lý lương'}</strong></span><div class="user-chip"><span class="user-avatar">HR</span><c:out value="${payrollPrincipal.displayName}"/></div></header>
        <div class="pay-content">
            <div class="page-heading"><div><div class="eyebrow">PAYROLL MANAGEMENT</div><h1>${salaryPage ? 'Thiết lập lương cơ bản' : 'Quản lý kỳ lương'}</h1><p>${salaryPage ? 'Thiết lập mức lương cho từng nhân viên, làm cơ sở cho các kỳ tính lương.' : 'Từ dữ liệu lương đến phiếu lương — mọi bước trong cùng một không gian.'}</p></div><c:if test="${not salaryPage}"><button class="btn primary" id="newPeriod">＋ Tạo kỳ lương</button></c:if></div>
            <div id="feedback" class="feedback" role="status" aria-live="polite" hidden></div>
            <c:choose><c:when test="${salaryPage}">
                <div class="info-note">Mức lương và phụ cấp đang có hiệu lực hôm nay. Cập nhật theo ngày hiệu lực; không cho sửa nguồn lương của kỳ đã duyệt/công bố. Các kỳ chưa duyệt bị ảnh hưởng sẽ cần tính lại.</div>
                <section class="pay-card"><div class="card-toolbar"><h2>Danh sách mức lương</h2><label class="search-field"><span>⌕</span><input id="salarySearch" type="search" placeholder="Tìm tên, mã nhân viên hoặc phòng ban" aria-label="Tìm nhân viên"></label></div><div class="table-scroll"><table><thead><tr><th>Nhân viên</th><th>Phòng ban</th><th>Trạng thái</th><th class="number">Lương cơ bản (VND)</th><th class="number">Phụ cấp trách nhiệm</th><th>Thao tác</th></tr></thead><tbody id="salaryRows"><tr><td colspan="6" class="empty">Đang tải dữ liệu…</td></tr></tbody></table></div><div class="pagination" id="salaryPagination"></div></section>
            </c:when><c:otherwise>
                <section class="workflow-strip" aria-label="Quy trình tính lương"><div><span>01</span><strong>Chuẩn bị</strong><small>Lương & điều chỉnh</small></div><i>→</i><div><span>02</span><strong>Tính lương</strong><small>Kết quả tự động</small></div><i>→</i><div><span>03</span><strong>Rà soát & duyệt</strong><small>Xác nhận bảng lương</small></div><i>→</i><div><span>04</span><strong>Công bố</strong><small>Phiếu lương & thông báo</small></div></section>
                <section class="pay-card periods-card"><div class="card-toolbar"><h2>Kỳ lương <span id="periodCount" class="count" aria-live="polite">0</span></h2><div class="toolbar-fields"><select id="yearFilter" aria-label="Lọc năm"><option value="">Tất cả năm</option></select><select id="statusFilter" aria-label="Lọc trạng thái"><option value="">Tất cả trạng thái</option><option>Draft</option><option>Calculated</option><option>Reviewed</option><option>Approved</option></select><button class="btn secondary" id="refreshPeriods" type="button">↻ Tải lại</button></div></div><div id="periodList" class="period-list" role="group" aria-label="Danh sách kỳ lương" aria-busy="true"><div class="empty">Đang tải kỳ lương…</div></div></section>
                <div id="periodDetailState" class="info-note" role="status" aria-live="polite" hidden></div>
                <section id="periodWorkspace" hidden>
                    <div class="period-heading"><div><div class="eyebrow">KỲ LƯƠNG ĐANG CHỌN</div><h2 id="periodTitle"></h2><div id="periodBadges" class="badge-row"></div></div><div id="periodActions" class="action-row"></div></div>
                    <div id="periodNotice" class="info-note"></div>
                    <div id="periodReviewHistory" class="info-note" hidden></div>
                    <section class="pay-card adjustments-card" aria-labelledby="adjustmentsTitle">
                        <div class="card-toolbar"><div><h2 id="adjustmentsTitle">Điều chỉnh thưởng / khấu trừ</h2><p class="subtle">Danh sách nhân viên trong công ty. Chọn Điều chỉnh để thêm hoặc sửa thưởng, khấu trừ và ghi chú.</p></div><span id="adjustmentLock" class="badge closed" hidden>Chỉ xem · Kỳ đã khóa</span></div>
                        <div class="table-filter"><label class="search-field"><span>⌕</span><input id="adjustmentSearch" type="search" placeholder="Tìm tên hoặc mã nhân viên" aria-label="Tìm nhân viên để điều chỉnh"></label><span id="adjustmentCount" class="subtle"></span></div>
                        <div class="table-scroll"><table><thead><tr><th>Mã nhân viên</th><th>Họ tên</th><th class="number">Lương cơ bản</th><th class="number">Thưởng</th><th class="number">Khấu trừ</th><th>Ghi chú</th><th>Thao tác</th></tr></thead><tbody id="adjustmentRows"></tbody></table></div>
                        <div class="pagination" id="adjustmentPagination"></div>
                        <p class="adjustment-help subtle">Nhân viên chưa có hợp đồng hoặc mức lương hợp lệ vẫn có thể lưu điều chỉnh. Kết quả tính lương chỉ bao gồm nhân viên đủ điều kiện trong kỳ.</p>
                    </section>
                    <section class="pay-metrics"><div class="metric"><small>Nhân viên trong kỳ</small><strong id="metricEmployees">—</strong><span>Hồ sơ thuộc công ty</span></div><div class="metric"><small>Tổng thu nhập</small><strong id="metricIncome">—</strong><span>Lương + Phụ cấp + Thưởng · VND</span></div><div class="metric"><small>Tổng khấu trừ</small><strong id="metricDeduction">—</strong><span>VND</span></div><div class="metric highlight"><small>Tổng thực nhận</small><strong id="metricNet">—</strong><span id="metricNetNote">Chưa tính lương</span></div></section>
                    <section class="pay-card"><div class="card-toolbar"><div><h2>Bảng lương nhân viên</h2><p class="subtle">Lương cơ bản + Phụ cấp trách nhiệm + Thưởng − Khấu trừ = Thực nhận</p></div><div class="toolbar-fields"><button class="btn secondary" id="templateButton">↓ File mẫu Excel</button><button class="btn secondary" id="importButton">↑ Nhập Excel</button></div></div><div class="table-filter"><label class="search-field"><span>⌕</span><input type="search" id="rowSearch" placeholder="Tìm tên hoặc mã nhân viên" aria-label="Tìm nhân viên trong kỳ"></label><span id="rowCount" class="subtle"></span></div><div class="table-scroll"><table><thead><tr><th>Mã nhân viên</th><th>Họ tên</th><th class="number">Lương cơ bản</th><th class="number">Phụ cấp trách nhiệm</th><th class="number">Thưởng</th><th class="number">Khấu trừ</th><th class="number">Thực nhận</th><th>Thao tác</th></tr></thead><tbody id="payrollRows"></tbody></table></div><div class="pagination" id="rowPagination"></div></section>
                </section>
            </c:otherwise></c:choose>
            <footer class="pay-footer">HRMs Cloud <span>•</span> Quản lý lương trong không gian công ty của bạn</footer>
        </div>
    </main>
</div>
<dialog id="createDialog" class="pay-dialog"><form id="createForm"><div class="dialog-heading"><div><div class="eyebrow">KỲ TÍNH LƯƠNG MỚI</div><h2>Tạo kỳ lương</h2></div><button type="button" class="icon-button" data-dismiss aria-label="Đóng">×</button></div><p class="subtle">Mỗi tháng chỉ có một kỳ lương trong công ty. Kỳ mới bắt đầu ở Draft.</p><label>Tên kỳ lương<input name="periodName" maxlength="100" placeholder="Tự đặt theo tháng và năm nếu để trống"></label><div class="form-pair"><label>Tháng<select name="month" required><c:forEach begin="1" end="12" var="month"><option value="${month}">Tháng ${month}</option></c:forEach></select></label><label>Năm<input name="year" type="number" min="2000" max="9999" required></label></div><div class="dialog-error" role="alert" hidden></div><div class="dialog-actions"><button type="button" class="btn secondary" data-dismiss>Hủy</button><button class="btn primary" type="submit">Tạo kỳ lương</button></div></form></dialog>
<dialog id="salaryDialog" class="pay-dialog"><form id="salaryForm">
    <div class="dialog-heading"><h2>Cập nhật lương cơ bản</h2><button type="button" class="icon-button" data-dismiss aria-label="Đóng">×</button></div>
    <p id="salaryEmployee" class="employee-label"></p><input name="employeeId" type="hidden">
    <div class="salary-amount-field">
        <label for="salaryAmount">Lương cơ bản (VND)</label>
        <div class="salary-amount-control">
            <input id="salaryAmount" name="baseSalary" type="number" min="0" max="9999999999999999.99" step="0.01" data-salary-step="100000" required aria-describedby="salaryStepHint">
            <div class="salary-amount-buttons">
                <button type="button" data-salary-direction="1" aria-label="Tăng 100.000 đồng" title="Tăng 100.000 đồng">▴</button>
                <button type="button" data-salary-direction="-1" aria-label="Giảm 100.000 đồng" title="Giảm 100.000 đồng">▾</button>
            </div>
        </div>
        <small id="salaryStepHint" class="subtle">Mỗi lần bấm tăng/giảm 100.000đ. Bạn có thể gõ trực tiếp mức lương.</small>
    </div>
    <label>Ngày hiệu lực<input name="effectiveFrom" type="date" min="2000-01-01" max="9999-12-31" required></label>
    <p class="subtle">Lưu thành lịch sử lương, giữ các mức lương trước ngày này và lịch lương đã hẹn sau đó. Thay đổi giữa tháng cần có quy tắc được xác nhận trước khi tính kỳ đó.</p>
    <div class="dialog-error" role="alert" hidden></div><div class="dialog-actions"><button class="btn secondary" type="button" data-dismiss>Hủy</button><button class="btn primary" type="submit">Lưu mức lương</button></div>
</form></dialog>
<dialog id="calculationPreviewDialog" class="pay-dialog wide" aria-labelledby="calculationPreviewTitle"><div class="dialog-heading"><h2 id="calculationPreviewTitle">Xem trước bảng lương</h2><button type="button" class="icon-button" data-dismiss aria-label="Đóng">×</button></div><div id="calculationPreviewContent" aria-live="polite"></div><div class="dialog-error" role="alert" hidden></div><div class="dialog-actions"><button type="button" class="btn secondary" data-dismiss>Đóng xem trước</button></div></dialog>
<dialog id="adjustmentDialog" class="pay-dialog" aria-labelledby="adjustmentTitle"><form id="adjustmentForm" novalidate>
    <div class="dialog-heading"><h2 id="adjustmentTitle">Điều chỉnh lương</h2><button type="button" class="icon-button" data-dismiss aria-label="Đóng">×</button></div>
    <p id="adjustmentEmployee" class="employee-label"></p><input name="employeeId" type="hidden">
    <div class="form-pair">
        <label>Thưởng / Bonus (VND)<input name="bonus" type="number" min="0" max="9999999999999999.99" step="0.01" required aria-describedby="bonusError"><small id="bonusError" class="adjustment-field-error" data-adjustment-error="bonus" role="alert" hidden></small></label>
        <label>Khấu trừ / Deduction (VND)<input name="deduction" type="number" min="0" max="9999999999999999.99" step="0.01" required aria-describedby="deductionError"><small id="deductionError" class="adjustment-field-error" data-adjustment-error="deduction" role="alert" hidden></small></label>
    </div>
    <label>Ghi chú<textarea name="note" maxlength="300" rows="3" placeholder="Lý do thưởng hoặc khấu trừ" aria-describedby="noteError"></textarea><small id="noteError" class="adjustment-field-error" data-adjustment-error="note" role="alert" hidden></small></label>
    <div class="info-note">Thay đổi điều chỉnh đưa kỳ về Draft và xóa kết quả tính cũ để tính, rà soát và duyệt lại.</div>
    <p id="adjustmentSaveState" class="subtle" role="status" aria-live="polite">Chưa có thay đổi.</p>
    <div class="dialog-error" role="alert" hidden></div>
    <div class="dialog-actions"><button class="btn secondary" type="button" data-dismiss>Hủy</button><button class="btn primary" type="submit" disabled>Lưu điều chỉnh</button></div>
</form></dialog>
<dialog id="importDialog" class="pay-dialog wide"><form id="importForm"><div class="dialog-heading"><div><div class="eyebrow">BONUS & DEDUCTION</div><h2>Nhập điều chỉnh từ Excel</h2></div><button type="button" class="icon-button" data-dismiss aria-label="Đóng">×</button></div><p class="subtle">Dùng file mẫu với bốn cột EmployeeCode, Bonus, Deduction, Note. Mỗi mã xuất hiện một lần; số tiền không âm, tối đa 2 chữ số thập phân.</p><label class="upload-field">Chọn file .xlsx hoặc .xls<input id="importFile" name="file" type="file" accept=".xlsx,.xls" required><small>Tối đa 5 MB / 5.000 dòng. Không dùng công thức Excel.</small></label><div id="importPreview" hidden></div><div class="dialog-error" role="alert" hidden></div><div class="dialog-actions"><button type="button" class="btn secondary" data-dismiss>Hủy</button><button id="previewImport" type="button" class="btn secondary">Kiểm tra file</button><button id="saveImport" type="submit" class="btn primary" disabled>Nhập dữ liệu</button></div></form></dialog>
<dialog id="confirmDialog" class="pay-dialog" aria-labelledby="confirmTitle" aria-describedby="confirmText"><form id="confirmForm"><div class="dialog-heading"><h2 id="confirmTitle"></h2><button type="button" class="icon-button" data-dismiss aria-label="Đóng">×</button></div><p id="confirmText"></p><div id="confirmSummary" class="info-note" hidden></div><div class="dialog-error" role="alert" hidden></div><div class="dialog-actions"><button class="btn secondary" type="button" data-dismiss>Hủy</button><button class="btn primary" type="submit" id="confirmSubmit">Xác nhận</button></div></form></dialog>
<script src="${pageContext.request.contextPath}/js/payroll.js?v=20261026-payslips-1"></script>
</body></html>
