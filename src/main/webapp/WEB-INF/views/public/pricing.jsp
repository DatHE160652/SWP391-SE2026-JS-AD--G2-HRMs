<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>
            Bảng giá | HRMs Cloud
        </title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/public-portal.css">
    </head>
    <body>
        <%@ include file="public-header.jspf" %>
        <main>
            <section class="page-hero public-container">
                <span class="eyebrow">
                    BẢNG GIÁ MINH BẠCH
                </span>
                <h1>
                    Một cấu hình đơn giản, đủ cho doanh nghiệp tăng trưởng
                </h1>
                <p>
                    Không cần chọn giữa nhiều gói phức tạp. Gói tham chiếu của HRMs Cloud cung cấp đầy đủ nền tảng quản trị nhân sự.
                </p>
            </section>
            <section class="section section-subtle">
                <div class="public-container pricing-layout">
                    <article class="pricing-card">
                        <span class="pricing-badge">
                            GÓI CHUẨN DOANH NGHIỆP
                        </span>
                        <h2>
                            HRMs Cloud Standard
                        </h2>
                        <div class="price">
                            59.000
                            <span>
                                đ
                            </span>
                        </div>
                        <div class="price-unit">
                            / công ty / tháng
                        </div>
                        <p class="pricing-intro">
                            Cấu hình tham chiếu cố định, không yêu cầu chọn plan khi gửi yêu cầu tư vấn.
                        </p>
                        <a class="button button-primary button-wide" href="${pageContext.request.contextPath}/consultation-request">
                            Đăng ký tư vấn
                        </a>
                    </article>
                    <div class="pricing-details">
                        <h2>
                            Đã bao gồm
                        </h2>
                        <ul class="check-list large">
                            <li>
                                Đầy đủ e-HRM, chấm công, tính lương và ESS
                            </li>
                            <li>
                                Tối đa 20 tài khoản trong một Company
                            </li>
                            <li>
                                1 Company Admin và tối đa 3 Manager
                            </li>
                            <li>
                                Workspace và dữ liệu được phân tách theo Company
                            </li>
                            <li>
                                Hỗ trợ quy trình kích hoạt sau khi System Admin xác nhận
                            </li>
                        </ul>
                        <div class="pricing-note">
                            <strong>
                                Lưu ý
                            </strong>
                            <p>
                                Gửi form tư vấn chỉ tạo Registration Request. Hệ thống chưa tạo Company tại bước này.
                            </p>
                        </div>
                    </div>
                </div>
            </section>
        </main>
        <%@ include file="public-footer.jspf" %>
        <script src="${pageContext.request.contextPath}/js/public-portal.js?v=<%= application.getResource("/js/public-portal.js").openConnection().getLastModified() %>" defer>
        </script>
    </body>
</html>
