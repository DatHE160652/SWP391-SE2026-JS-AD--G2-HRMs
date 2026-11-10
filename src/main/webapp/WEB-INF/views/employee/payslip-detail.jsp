<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
            <fmt:setLocale value='<%= "en".equals(com.hrms.i18n.Language.selected(request)) ? "en_US" : "vi_VN" %>' />
            <!DOCTYPE html>
            <html lang="<%= com.hrms.i18n.Language.selected(request) %>">

            <head>
                <%@ include file="/WEB-INF/views/common/language.jspf" %>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width,initial-scale=1">
                    <title>
                        <%= com.hrms.i18n.Language.text(request, "ui_7283a20e7ce7" , "Phiếu lương ·" ) %>
                            <c:out value="${slip.periodName()}" />
                    </title>
                    <link rel="stylesheet"
                        href="${pageContext.request.contextPath}/css/payroll.css?v=20261026-payslips-1">
                    <%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
            </head>

            <body class="lc workspace-document">
                <main class="slip-page">
                    <div class="slip-actions">
                        <c:url var="slipListUrl" value="${adminView ? '/payroll' : '/payslips'}">
                            <c:if test="${adminView}">
                                <c:param name="view" value="payslips" />
                                <c:param name="periodId" value="${slip.payrollPeriodId()}" />
                            </c:if>
                        </c:url>
                        <a class="btn secondary" href="<c:out value='${slipListUrl}'/>">
                            <%= com.hrms.i18n.Language.text(request, "ui_4a436befc12b" , "← Quay lại" ) %>
                        </a><button class="btn primary" type="button" onclick="window.print()">
                            <%= com.hrms.i18n.Language.text(request, "ui_c3e2d23e53b2" , "In / Lưu PDF" ) %>
                        </button>
                    </div>
                    <article class="slip-paper">
                        <header class="slip-header">
                            <div>
                                <div class="eyebrow">HRMs CLOUD</div>
                                <h1>
                                    <%= com.hrms.i18n.Language.text(request, "ui_c0e6353aff6a" , "PHIẾU LƯƠNG" ) %>
                                </h1>
                                <p>
                                    <c:out value="${slip.companyName()}" />
                                </p>
                            </div>
                            <div class="slip-period">
                                <%= com.hrms.i18n.Language.text(request, "ui_0e2b309955fe" , "Tháng" ) %>
                                    <fmt:formatNumber value="${slip.month()}" pattern="00" /> / ${slip.year()}
                            </div>
                        </header>
                        <section class="slip-info">
                            <div><small>
                                    <%= com.hrms.i18n.Language.text(request, "ui_14ddd70d2564" , "Nhân viên" ) %>
                                </small><strong>
                                    <c:out value="${slip.fullName()}" />
                                </strong></div>
                            <div><small>
                                    <%= com.hrms.i18n.Language.text(request, "ui_84979604c762" , "Mã nhân viên" ) %>
                                </small><strong>
                                    <c:out value="${slip.employeeCode()}" />
                                </strong></div>
                            <div><small>
                                    <%= com.hrms.i18n.Language.text(request, "ui_4a9c54be5faf" , "Kỳ tính lương" ) %>
                                </small><strong>
                                    <c:out value="${slip.periodName()}" />
                                </strong></div>
                            <div><small>
                                    <%= com.hrms.i18n.Language.text(request, "ui_4adc22325708" , "Mã phiếu lương" ) %>
                                </small><strong>PS-${slip.payslipId()}</strong></div>
                        </section>
                        <div class="info-note">
                            <%= com.hrms.i18n.Language.text(request, "ui_1c16408f4209" , "Trạng thái kỳ:" ) %> <strong>
                                    <c:set var="i18nPeriodStatus" value="${slip.periodStatus()}" />
                                    <c:out
                                        value='<%= com.hrms.i18n.Language.label(request, pageContext.findAttribute("i18nPeriodStatus")) %>' />
                                </strong> ·
                                <c:set var="i18nConditionalLabel"
                                    value="${empty slip.publishedAt() ? 'Chưa công bố' : 'Đã công bố'}" />
                                <c:out
                                    value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("i18nConditionalLabel")) %>' />
                                <c:if test="${empty slip.publishedAt()}"><br>
                                    <%= com.hrms.i18n.Language.text(request, "ui_86d4b88d28f4"
                                        , "Bản xem trước dành cho người quản lý. Nhân viên chưa thể xem phiếu này." ) %>
                                </c:if>
                        </div>
                        <table>
                            <thead>
                                <tr>
                                    <th>
                                        <%= com.hrms.i18n.Language.text(request, "ui_87ffb6e1ac37" , "Khoản mục" ) %>
                                    </th>
                                    <th class="number">
                                        <%= com.hrms.i18n.Language.text(request, "ui_33eb57284b68" , "Số tiền (VND)" )
                                            %>
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td>Lương cơ bản theo hợp đồng</td>
                                    <td class="number">
                                        <fmt:formatNumber value="${slip.contractBaseSalary()}" minFractionDigits="0"
                                            maxFractionDigits="2" />
                                    </td>
                                </tr>
                                <tr>
                                    <td>Ngày công thực tế / Công tiêu chuẩn</td>
                                    <td class="number">
                                        <fmt:formatNumber value="${slip.actualWorkDays()}" minFractionDigits="0"
                                            maxFractionDigits="2" /> /
                                        <fmt:formatNumber value="${slip.standardWorkDays()}" minFractionDigits="0"
                                            maxFractionDigits="2" /> ngày
                                    </td>
                                </tr>
                                <tr>
                                    <td><strong>Lương chính tính theo công</strong></td>
                                    <td class="number"><strong>
                                            <fmt:formatNumber value="${slip.baseSalary()}" minFractionDigits="0"
                                                maxFractionDigits="2" />
                                        </strong></td>
                                </tr>
                                <tr>
                                    <td>
                                        <%= com.hrms.i18n.Language.text(request, "ui_ac5d493eaa5e"
                                            , "Phụ cấp trách nhiệm" ) %>
                                    </td>
                                    <td class="number">+
                                        <fmt:formatNumber value="${slip.responsibilityAllowance()}"
                                            minFractionDigits="0" maxFractionDigits="2" />
                                    </td>
                                </tr>
                                <tr>
                                    <td>Phụ cấp đi lại</td>
                                    <td class="number">+
                                        <fmt:formatNumber value="${slip.travelAllowance()}" minFractionDigits="0"
                                            maxFractionDigits="2" />
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <%= com.hrms.i18n.Language.text(request, "ui_cd98f5fe0006" , "Thưởng / Bonus" )
                                            %>
                                    </td>
                                    <td class="number">+
                                        <fmt:formatNumber value="${slip.bonus()}" minFractionDigits="0"
                                            maxFractionDigits="2" />
                                    </td>
                                </tr>
                                <tr class="slip-total">
                                    <td>
                                        <%= com.hrms.i18n.Language.text(request, "ui_f32076b949d2" , "THỰC NHẬN" ) %>
                                    </td>
                                    <td class="number">
                                        <fmt:formatNumber value="${slip.netSalary()}" minFractionDigits="0"
                                            maxFractionDigits="2" /> ₫
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                        <div class="slip-foot">Thực nhận = ((Lương cơ bản × Công thực) / Số công tiêu chuẩn) + Phụ cấp
                            trách nhiệm + Phụ cấp đi lại + Thưởng.<br>
                            <%= com.hrms.i18n.Language.text(request, "ui_c3ec71043aa8" , "Ngày tạo (UTC):" ) %>
                                <c:out value="${slip.generatedAt()}" />
                                <c:if test="${not empty slip.publishedAt()}"><br>
                                    <%= com.hrms.i18n.Language.text(request, "ui_3d79d6a0c8e3" , "Ngày công bố (UTC):" )
                                        %>
                                        <c:out value="${slip.publishedAt()}" />
                                </c:if><br>
                                <%= com.hrms.i18n.Language.text(request, "ui_e283fbe50000"
                                    , "Vui lòng liên hệ bộ phận nhân sự nếu cần đối chiếu thông tin." ) %>
                        </div>
                    </article>
                    <footer class="pay-footer">
                        <%= com.hrms.i18n.Language.text(request, "ui_828bd433a031"
                            , "Phiếu lương được tạo từ HRMs Cloud." ) %>
                    </footer>
                </main>
            </body>

            </html>