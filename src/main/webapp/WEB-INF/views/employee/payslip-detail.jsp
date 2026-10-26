<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Phiếu lương · <c:out value="${slip.periodName()}"/></title><link rel="stylesheet" href="${pageContext.request.contextPath}/css/payroll.css?v=20261026-payslips-1"></head>
<body><main class="slip-page">
    <div class="slip-actions">
        <c:url var="slipListUrl" value="${adminView ? '/payroll' : '/payslips'}"><c:if test="${adminView}"><c:param name="view" value="payslips"/><c:param name="periodId" value="${slip.payrollPeriodId()}"/></c:if></c:url>
        <a class="btn secondary" href="<c:out value='${slipListUrl}'/>">← Quay lại</a><button class="btn primary" type="button" onclick="window.print()">In / Lưu PDF</button>
    </div>
    <article class="slip-paper">
        <header class="slip-header"><div><div class="eyebrow">HRMs CLOUD</div><h1>PHIẾU LƯƠNG</h1><p><c:out value="${slip.companyName()}"/></p></div><div class="slip-period">Tháng <fmt:formatNumber value="${slip.month()}" pattern="00"/> / ${slip.year()}</div></header>
        <section class="slip-info"><div><small>Nhân viên</small><strong><c:out value="${slip.fullName()}"/></strong></div><div><small>Mã nhân viên</small><strong><c:out value="${slip.employeeCode()}"/></strong></div><div><small>Kỳ tính lương</small><strong><c:out value="${slip.periodName()}"/></strong></div><div><small>Mã phiếu lương</small><strong>PS-${slip.payslipId()}</strong></div></section>
        <div class="info-note">Trạng thái kỳ: <strong><c:out value="${slip.periodStatus()}"/></strong> · ${empty slip.publishedAt() ? 'Chưa công bố' : 'Đã công bố'}<c:if test="${empty slip.publishedAt()}"><br>Bản xem trước dành cho người quản lý. Nhân viên chưa thể xem phiếu này.</c:if></div>
        <table><thead><tr><th>Khoản mục</th><th class="number">Số tiền (VND)</th></tr></thead><tbody>
            <tr><td>Lương cơ bản</td><td class="number"><fmt:formatNumber value="${slip.baseSalary()}" minFractionDigits="0" maxFractionDigits="2"/></td></tr>
            <tr><td>Phụ cấp trách nhiệm</td><td class="number">+ <fmt:formatNumber value="${slip.responsibilityAllowance()}" minFractionDigits="0" maxFractionDigits="2"/></td></tr>
            <tr><td>Thưởng / Bonus</td><td class="number">+ <fmt:formatNumber value="${slip.bonus()}" minFractionDigits="0" maxFractionDigits="2"/></td></tr>
            <tr><td>Khấu trừ / Deduction</td><td class="number">− <fmt:formatNumber value="${slip.deduction()}" minFractionDigits="0" maxFractionDigits="2"/></td></tr>
            <tr class="slip-total"><td>THỰC NHẬN</td><td class="number"><fmt:formatNumber value="${slip.netSalary()}" minFractionDigits="0" maxFractionDigits="2"/> ₫</td></tr>
        </tbody></table>
        <div class="slip-foot">Thực nhận = Lương cơ bản + Phụ cấp trách nhiệm + Thưởng − Khấu trừ.<br>Ngày tạo (UTC): <c:out value="${slip.generatedAt()}"/><c:if test="${not empty slip.publishedAt()}"><br>Ngày công bố (UTC): <c:out value="${slip.publishedAt()}"/></c:if><br>Vui lòng liên hệ bộ phận nhân sự nếu cần đối chiếu thông tin.</div>
    </article>
    <footer class="pay-footer">Phiếu lương được tạo từ HRMs Cloud.</footer>
</main></body></html>