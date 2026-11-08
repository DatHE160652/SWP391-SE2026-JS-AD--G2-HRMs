<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
    <head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>
            <%= com.hrms.i18n.Language.text(request, "ui_6376097230e7", "Đăng ký tư vấn | HRMs Cloud") %>
        </title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/public-portal.css">
    </head>
    <body>
        <%@ include file="public-header.jspf" %>
        <main>
            <section class="consultation-hero public-container">
                <div class="consultation-benefits">
                    <span class="eyebrow eyebrow-light">
                        <%= com.hrms.i18n.Language.text(request, "ui_e36c9f3373da", "TƯ VẤN &amp; TRẢI NGHIỆM MIỄN PHÍ") %>
                    </span>
                    <h1>
                        <%= com.hrms.i18n.Language.text(request, "ui_e1a2fa21f22a", "Đồng hành chuyển đổi số nhân sự thông minh cùng HRMs Cloud") %>
                    </h1>
                    <p>
                        <%= com.hrms.i18n.Language.text(request, "ui_6f4b7663d056", "Giải pháp SaaS chuyên sâu chuẩn hóa nghiệp vụ quản trị nguồn nhân lực dành cho doanh nghiệp tăng trưởng tại Việt Nam.") %>
                    </p>
                    <div class="benefit-list">
                        <div>
                            <strong>
                                <%= com.hrms.i18n.Language.text(request, "ui_6335a41f14c9", "59.000đ/công ty/tháng") %>
                            </strong>
                            <span>
                                <%= com.hrms.i18n.Language.text(request, "ui_20cab5270b2b", "Cấu hình tham chiếu minh bạch, đầy đủ các phân hệ nền tảng.") %>
                            </span>
                        </div>
                        <div>
                            <strong>
                                <%= com.hrms.i18n.Language.text(request, "ui_f2ea2fe71b96", "Quy mô chuẩn 20 tài khoản") %>
                            </strong>
                            <span>
                                <%= com.hrms.i18n.Language.text(request, "ui_14607526cc07", "1 Company Admin, tối đa 3 Manager và các tài khoản tác nghiệp.") %>
                            </span>
                        </div>
                        <div>
                            <strong>
                                <%= com.hrms.i18n.Language.text(request, "ui_e97b884b4e1e", "Workspace theo Company") %>
                            </strong>
                            <span>
                                <%= com.hrms.i18n.Language.text(request, "ui_428bd6a6a6c9", "Phân tách quyền truy cập và dữ liệu theo từng doanh nghiệp.") %>
                            </span>
                        </div>
                        <div>
                            <strong>
                                <%= com.hrms.i18n.Language.text(request, "ui_bc594fb41bc4", "Tiếp nhận 24/7") %>
                            </strong>
                            <span>
                                <%= com.hrms.i18n.Language.text(request, "ui_da83163b4aa2", "Gửi thông tin để đội ngũ tư vấn liên hệ theo nhu cầu.") %>
                            </span>
                        </div>
                    </div>
                </div>
                <div class="consultation-card">
                    <div class="form-heading">
                        <span class="eyebrow">
                            <%= com.hrms.i18n.Language.text(request, "ui_074456b69cbd", "BIỂU MẪU TIẾP NHẬN") %>
                        </span>
                        <h2>
                            <%= com.hrms.i18n.Language.text(request, "ui_343f4c8102b1", "Đăng ký tư vấn giải pháp") %>
                        </h2>
                        <p>
                            <%= com.hrms.i18n.Language.text(request, "ui_9154698d2ff9", "Vui lòng cung cấp đúng 5 thông tin cơ bản. Việc gửi form chưa tạo tài khoản hoặc workspace.") %>
                        </p>
                    </div>
                    <c:if test="${submitted}">
                        <div class="alert alert-success" role="status">
                            <strong>
                                <%= com.hrms.i18n.Language.text(request, "ui_b5daeaa4e399", "Đã tiếp nhận yêu cầu.") %>
                            </strong>
                            <span>
                                <%= com.hrms.i18n.Language.text(request, "ui_96989984c167", "Yêu cầu của bạn đã được lưu. Đội ngũ HRMs Cloud sẽ liên hệ để tư vấn.") %>
                            </span>
                        </div>
                    </c:if>
                    <c:if test="${not empty formError}">
                        <div class="alert alert-error" role="alert">
                            <c:out value='<%= com.hrms.i18n.Language.message(request, pageContext.findAttribute("formError")) %>'/>
                        </div>
                    </c:if>
                    <form method="post" action="${pageContext.request.contextPath}/consultation-request" class="consultation-form" novalidate>
                        <div class="form-field">
                            <label for="fullName">
                                <%= com.hrms.i18n.Language.text(request, "ui_ad9f29940174", "Họ và tên") %>
                                <span>
                                    *
                                </span>
                            </label>
                            <input id="fullName" name="fullName" type="text" maxlength="150" required value="<c:out value='${formFullName}'/>" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_b0404417e6ac", "Ví dụ: Nguyễn Văn An") %>">
                            <c:if test="${not empty fieldErrors.fullName}">
                                <p class="field-error">
                                    <c:out value='<%= com.hrms.i18n.Language.fieldMessage(request, pageContext.findAttribute("fieldErrors"), "fullName") %>'/>
                                </p>
                            </c:if>
                        </div>
                        <div class="form-field">
                            <label for="email">
                                Email
                                <span>
                                    *
                                </span>
                            </label>
                            <input id="email" name="email" type="email" maxlength="150" required value="<c:out value='${formEmail}'/>" placeholder="an.nguyen@company.vn">
                            <c:if test="${not empty fieldErrors.email}">
                                <p class="field-error">
                                    <c:out value='<%= com.hrms.i18n.Language.fieldMessage(request, pageContext.findAttribute("fieldErrors"), "email") %>'/>
                                </p>
                            </c:if>
                        </div>
                        <div class="form-field">
                            <label for="phone">
                                <%= com.hrms.i18n.Language.text(request, "ui_58f52301a801", "Số điện thoại") %>
                                <span>
                                    *
                                </span>
                            </label>
                            <input id="phone" name="phone" type="tel" maxlength="10" pattern="^0[0-9]{9}$" required value="<c:out value='${formPhone}'/>" placeholder="09xxxxxxxx">
                            <c:if test="${not empty fieldErrors.phone}">
                                <p class="field-error">
                                    <c:out value='<%= com.hrms.i18n.Language.fieldMessage(request, pageContext.findAttribute("fieldErrors"), "phone") %>'/>
                                </p>
                            </c:if>
                        </div>
                        <div class="form-field">
                            <label for="companyName">
                                <%= com.hrms.i18n.Language.text(request, "ui_24db39d3bf97", "Tên công ty") %>
                                <span>
                                    *
                                </span>
                            </label>
                            <input id="companyName" name="companyName" type="text" maxlength="200" required value="<c:out value='${formCompanyName}'/>" placeholder="<%= com.hrms.i18n.Language.text(request, "ui_3dafa4b0e116", "Ví dụ: Công ty Cổ phần Công nghệ Anpha") %>">
                            <c:if test="${not empty fieldErrors.companyName}">
                                <p class="field-error">
                                    <c:out value='<%= com.hrms.i18n.Language.fieldMessage(request, pageContext.findAttribute("fieldErrors"), "companyName") %>'/>
                                </p>
                            </c:if>
                        </div>
                        <div class="form-field">
                            <label for="region">
                                <%= com.hrms.i18n.Language.text(request, "ui_c12e4bb02923", "Khu vực") %>
                                <span>
                                    *
                                </span>
                            </label>
                            <select id="region" name="region" required>
                                <option value="">
                                    <%= com.hrms.i18n.Language.text(request, "ui_d20a2faa65c4", "Chọn khu vực") %>
                                </option>
                                <option value="NORTH" ${formRegion == 'NORTH' ? 'selected' : ''}>
                                    <%= com.hrms.i18n.Language.text(request, "ui_78cdd44a7022", "Khu vực miền Bắc") %>
                                </option>
                                <option value="CENTRAL" ${formRegion == 'CENTRAL' ? 'selected' : ''}>
                                    <%= com.hrms.i18n.Language.text(request, "ui_a7bf2d986da3", "Khu vực miền Trung") %>
                                </option>
                                <option value="SOUTH" ${formRegion == 'SOUTH' ? 'selected' : ''}>
                                    <%= com.hrms.i18n.Language.text(request, "ui_4a8c2e15abd5", "Khu vực miền Nam") %>
                                </option>
                            </select>
                            <c:if test="${not empty fieldErrors.region}">
                                <p class="field-error">
                                    <c:out value='<%= com.hrms.i18n.Language.fieldMessage(request, pageContext.findAttribute("fieldErrors"), "region") %>'/>
                                </p>
                            </c:if>
                        </div>
                        <p class="form-note">
                            <%= com.hrms.i18n.Language.text(request, "ui_21287808fdbb", "Sau khi gửi, Yêu cầu của bạn đã được lưu. Đội ngũ HRMs Cloud sẽ liên hệ để tư vấn.") %>
                        </p>
                        <button class="button button-primary button-wide submit-button" type="submit">
                            <%= com.hrms.i18n.Language.text(request, "ui_e268249ff400", "Gửi yêu cầu đăng ký tư vấn") %>
                            <span aria-hidden="true">
                                →
                            </span>
                        </button>
                    </form>
                </div>
            </section>
        </main>
        <%@ include file="public-footer.jspf" %>
        <script src="${pageContext.request.contextPath}/js/public-portal.js?v=<%= application.getResource("/js/public-portal.js").openConnection().getLastModified() %>" defer>
        </script>
    </body>
</html>
