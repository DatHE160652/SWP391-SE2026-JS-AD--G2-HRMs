<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>
            Đăng ký tư vấn | HRMs Cloud
        </title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/public-portal.css">
    </head>
    <body>
        <%@ include file="public-header.jspf" %>
        <main>
            <section class="consultation-hero public-container">
                <div class="consultation-benefits">
                    <span class="eyebrow eyebrow-light">
                        TƯ VẤN &amp; TRẢI NGHIỆM MIỄN PHÍ
                    </span>
                    <h1>
                        Đồng hành chuyển đổi số nhân sự thông minh cùng HRMs Cloud
                    </h1>
                    <p>
                        Giải pháp SaaS chuyên sâu chuẩn hóa nghiệp vụ quản trị nguồn nhân lực dành cho doanh nghiệp tăng trưởng tại Việt Nam.
                    </p>
                    <div class="benefit-list">
                        <div>
                            <strong>
                                59.000đ/công ty/tháng
                            </strong>
                            <span>
                                Cấu hình tham chiếu minh bạch, đầy đủ các phân hệ nền tảng.
                            </span>
                        </div>
                        <div>
                            <strong>
                                Quy mô chuẩn 20 tài khoản
                            </strong>
                            <span>
                                1 Company Admin, tối đa 3 Manager và các tài khoản tác nghiệp.
                            </span>
                        </div>
                        <div>
                            <strong>
                                Workspace theo Company
                            </strong>
                            <span>
                                Phân tách quyền truy cập và dữ liệu theo từng doanh nghiệp.
                            </span>
                        </div>
                        <div>
                            <strong>
                                Tiếp nhận 24/7
                            </strong>
                            <span>
                                Gửi thông tin để đội ngũ tư vấn liên hệ theo nhu cầu.
                            </span>
                        </div>
                    </div>
                </div>
                <div class="consultation-card">
                    <div class="form-heading">
                        <span class="eyebrow">
                            BIỂU MẪU TIẾP NHẬN
                        </span>
                        <h2>
                            Đăng ký tư vấn giải pháp
                        </h2>
                        <p>
                            Vui lòng cung cấp đúng 5 thông tin cơ bản. Việc gửi form chưa tạo tài khoản hoặc workspace.
                        </p>
                    </div>
                    <c:if test="${submitted}">
                        <div class="alert alert-success" role="status">
                            <strong>
                                Đã tiếp nhận yêu cầu.
                            </strong>
                            <span>
                                Yêu cầu của bạn đã được lưu ở trạng thái NEW. Đội ngũ HRMs Cloud sẽ liên hệ để tư vấn.
                            </span>
                        </div>
                    </c:if>
                    <c:if test="${not empty formError}">
                        <div class="alert alert-error" role="alert">
                            <c:out value="${formError}"/>
                        </div>
                    </c:if>
                    <form method="post" action="${pageContext.request.contextPath}/consultation-request" class="consultation-form" novalidate>
                        <div class="form-field">
                            <label for="fullName">
                                Họ và tên
                                <span>
                                    *
                                </span>
                            </label>
                            <input id="fullName" name="fullName" type="text" maxlength="150" required value="<c:out value='${formFullName}'/>" placeholder="Ví dụ: Nguyễn Văn An">
                            <c:if test="${not empty fieldErrors.fullName}">
                                <p class="field-error">
                                    <c:out value="${fieldErrors.fullName}"/>
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
                                    <c:out value="${fieldErrors.email}"/>
                                </p>
                            </c:if>
                        </div>
                        <div class="form-field">
                            <label for="phone">
                                Số điện thoại
                                <span>
                                    *
                                </span>
                            </label>
                            <input id="phone" name="phone" type="tel" maxlength="10" pattern="^0[0-9]{9}$" required value="<c:out value='${formPhone}'/>" placeholder="09xxxxxxxx">
                            <c:if test="${not empty fieldErrors.phone}">
                                <p class="field-error">
                                    <c:out value="${fieldErrors.phone}"/>
                                </p>
                            </c:if>
                        </div>
                        <div class="form-field">
                            <label for="companyName">
                                Tên công ty
                                <span>
                                    *
                                </span>
                            </label>
                            <input id="companyName" name="companyName" type="text" maxlength="200" required value="<c:out value='${formCompanyName}'/>" placeholder="Ví dụ: Công ty Cổ phần Công nghệ Anpha">
                            <c:if test="${not empty fieldErrors.companyName}">
                                <p class="field-error">
                                    <c:out value="${fieldErrors.companyName}"/>
                                </p>
                            </c:if>
                        </div>
                        <div class="form-field">
                            <label for="region">
                                Khu vực
                                <span>
                                    *
                                </span>
                            </label>
                            <select id="region" name="region" required>
                                <option value="">
                                    Chọn khu vực
                                </option>
                                <option value="NORTH" ${formRegion == 'NORTH' ? 'selected' : ''}>
                                    Khu vực miền Bắc
                                </option>
                                <option value="CENTRAL" ${formRegion == 'CENTRAL' ? 'selected' : ''}>
                                    Khu vực miền Trung
                                </option>
                                <option value="SOUTH" ${formRegion == 'SOUTH' ? 'selected' : ''}>
                                    Khu vực miền Nam
                                </option>
                            </select>
                            <c:if test="${not empty fieldErrors.region}">
                                <p class="field-error">
                                    <c:out value="${fieldErrors.region}"/>
                                </p>
                            </c:if>
                        </div>
                        <p class="form-note">
                            Sau khi gửi, hồ sơ được lưu ở trạng thái
                            <strong>
                                NEW
                            </strong>
                            . System Admin sẽ xem xét trước khi tạo Company, User hoặc Subscription.
                        </p>
                        <button class="button button-primary button-wide submit-button" type="submit">
                            Gửi yêu cầu đăng ký tư vấn
                            <span aria-hidden="true">
                                →
                            </span>
                        </button>
                    </form>
                </div>
            </section>
        </main>
        <%@ include file="public-footer.jspf" %>
        <script src="${pageContext.request.contextPath}/js/public-portal.js" defer>
        </script>
    </body>
</html>
