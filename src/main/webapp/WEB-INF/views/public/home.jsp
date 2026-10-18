<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>
            HRMs Cloud | Quản trị nhân sự thông minh
        </title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/public-portal.css">
    </head>
    <body>
        <%@ include file="public-header.jspf" %>
        <main>
            <section class="hero public-container">
                <div class="hero-copy">
                    <span class="eyebrow">
                        NỀN TẢNG HR SaaS CHO DOANH NGHIỆP
                    </span>
                    <h1>
                        Vận hành nhân sự rõ ràng hơn cùng
                        <span>
                            HRMs Cloud
                        </span>
                    </h1>
                    <p>
                        Số hóa hồ sơ, tự động hóa chấm công và tinh gọn quy trình nhân sự trong một workspace dành riêng cho doanh nghiệp.
                    </p>
                    <div class="hero-actions">
                        <a class="button button-primary" href="${pageContext.request.contextPath}/consultation-request">
                            Đăng ký tư vấn miễn phí
                        </a>
                        <a class="button button-outline" href="${pageContext.request.contextPath}/features-solutions">
                            Khám phá giải pháp
                        </a>
                    </div>
                </div>
                <div class="hero-card">
                    <span class="hero-card-label">
                        CẤU HÌNH THAM CHIẾU
                    </span>
                    <strong>
                        59.000
                        <span>
                            đ
                        </span>
                    </strong>
                    <small>
                        / công ty / tháng
                    </small>
                    <ul>
                        <li>
                            Tối đa 20 tài khoản
                        </li>
                        <li>
                            1 Company Admin và tối đa 3 Manager
                        </li>
                        <li>
                            Đầy đủ e-HRM, chấm công, tính lương, ESS
                        </li>
                    </ul>
                </div>
            </section>
            <section class="section section-subtle">
                <div class="public-container">
                    <div class="section-heading">
                        <span class="eyebrow">
                            GIẢI PHÁP TOÀN DIỆN
                        </span>
                        <h2>
                            Một nền tảng cho toàn bộ vòng đời nhân sự
                        </h2>
                        <p>
                            Từ dữ liệu nhân viên đến trải nghiệm tự phục vụ, HRMs Cloud giúp đội ngũ tập trung vào những công việc tạo giá trị.
                        </p>
                    </div>
                    <div class="feature-grid">
                        <article class="feature-card">
                            <span class="icon-circle">
                                01
                            </span>
                            <h3>
                                Hồ sơ nhân sự số
                            </h3>
                            <p>
                                Tập trung hồ sơ, phòng ban, vị trí và tài liệu trong một nguồn dữ liệu có cấu trúc.
                            </p>
                        </article>
                        <article class="feature-card">
                            <span class="icon-circle">
                                02
                            </span>
                            <h3>
                                Chấm công thông minh
                            </h3>
                            <p>
                                Quản lý ca kíp, ngày công và các quy trình phê duyệt minh bạch.
                            </p>
                        </article>
                        <article class="feature-card">
                            <span class="icon-circle">
                                03
                            </span>
                            <h3>
                                Tính lương chính xác
                            </h3>
                            <p>
                                Chuẩn hóa dữ liệu đầu vào để giảm thao tác thủ công và sai lệch.
                            </p>
                        </article>
                        <article class="feature-card">
                            <span class="icon-circle">
                                04
                            </span>
                            <h3>
                                Employee Self-Service
                            </h3>
                            <p>
                                Nhân viên chủ động theo dõi thông tin và tương tác với doanh nghiệp.
                            </p>
                        </article>
                    </div>
                </div>
            </section>
            <section class="section">
                <div class="public-container callout">
                    <div>
                        <span class="eyebrow">
                            BẮT ĐẦU ĐƠN GIẢN
                        </span>
                        <h2>
                            Trao đổi với chuyên viên HRMs Cloud
                        </h2>
                        <p>
                            Gửi 5 thông tin cơ bản. Hệ thống tiếp nhận yêu cầu ở trạng thái NEW để đội ngũ tư vấn liên hệ.
                        </p>
                    </div>
                    <a class="button button-primary" href="${pageContext.request.contextPath}/consultation-request">
                        Gửi yêu cầu tư vấn
                    </a>
                </div>
            </section>
        </main>
        <%@ include file="public-footer.jspf" %>
        <script src="${pageContext.request.contextPath}/js/public-portal.js?v=<%= application.getResource("/js/public-portal.js").openConnection().getLastModified() %>" defer>
        </script>
    </body>
</html>
