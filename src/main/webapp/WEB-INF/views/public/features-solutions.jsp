<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>
            Tính năng &amp; Giải pháp | HRMs Cloud
        </title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/public-portal.css">
    </head>
    <body>
        <%@ include file="public-header.jspf" %>
        <main>
            <section class="page-hero public-container">
                <span class="eyebrow">
                    TÍNH NĂNG &amp; GIẢI PHÁP
                </span>
                <h1>
                    Thiết kế để đội ngũ nhân sự vận hành nhẹ nhàng hơn
                </h1>
                <p>
                    HRMs Cloud kết nối dữ liệu và quy trình nhân sự trong một trải nghiệm thống nhất, dễ bắt đầu cho doanh nghiệp Việt Nam.
                </p>
            </section>
            <section class="section section-subtle">
                <div class="public-container solution-list">
                    <article class="solution-row">
                        <div class="solution-number">
                            01
                        </div>
                        <div>
                            <h2>
                                Hồ sơ nhân sự số
                            </h2>
                            <p>
                                Quản lý thông tin nhân viên, tài liệu, phòng ban và chức danh tập trung. Dữ liệu được chuẩn hóa để HR dễ tra cứu và cập nhật.
                            </p>
                            <ul class="check-list">
                                <li>
                                    Hồ sơ và tài liệu theo nhân viên
                                </li>
                                <li>
                                    Cấu trúc phòng ban, vị trí, người quản lý
                                </li>
                                <li>
                                    Phân quyền theo workspace
                                </li>
                            </ul>
                        </div>
                    </article>
                    <article class="solution-row">
                        <div class="solution-number">
                            02
                        </div>
                        <div>
                            <h2>
                                Chấm công &amp; ca kíp
                            </h2>
                            <p>
                                Đơn giản hóa việc theo dõi thời gian làm việc, ca làm và các quy trình phê duyệt liên quan.
                            </p>
                            <ul class="check-list">
                                <li>
                                    Quản lý ngày công và lịch làm việc
                                </li>
                                <li>
                                    Hỗ trợ quy trình điều chỉnh minh bạch
                                </li>
                                <li>
                                    Dữ liệu sẵn sàng cho tính lương
                                </li>
                            </ul>
                        </div>
                    </article>
                    <article class="solution-row">
                        <div class="solution-number">
                            03
                        </div>
                        <div>
                            <h2>
                                Tính lương &amp; bảo hiểm
                            </h2>
                            <p>
                                Chuẩn hóa dữ liệu đầu vào để giảm thao tác lặp lại và giúp đội ngũ kiểm tra bảng lương có hệ thống.
                            </p>
                            <ul class="check-list">
                                <li>
                                    Cấu trúc dữ liệu lương rõ ràng
                                </li>
                                <li>
                                    Kiểm tra trước khi phê duyệt
                                </li>
                                <li>
                                    Lưu vết theo kỳ xử lý
                                </li>
                            </ul>
                        </div>
                    </article>
                    <article class="solution-row">
                        <div class="solution-number">
                            04
                        </div>
                        <div>
                            <h2>
                                Cổng tự phục vụ nhân viên
                            </h2>
                            <p>
                                Đưa những thông tin và thao tác thường ngày đến gần nhân viên hơn, đồng thời giảm tải cho HR.
                            </p>
                            <ul class="check-list">
                                <li>
                                    Trải nghiệm theo vai trò
                                </li>
                                <li>
                                    Thông tin thống nhất trong workspace
                                </li>
                                <li>
                                    Quy trình tương tác có kiểm soát
                                </li>
                            </ul>
                        </div>
                    </article>
                </div>
            </section>
            <section class="section">
                <div class="public-container trust-banner">
                    <div>
                        <span class="eyebrow">
                            WORKSPACE RIÊNG BIỆT
                        </span>
                        <h2>
                            Kiểm soát dữ liệu theo đúng ranh giới doanh nghiệp
                        </h2>
                        <p>
                            Company Admin quản lý workspace của mình; System Admin quản trị nền tảng nhưng không truy cập dữ liệu HR chi tiết của tenant.
                        </p>
                    </div>
                    <a class="button button-primary" href="${pageContext.request.contextPath}/consultation-request">
                        Trao đổi nhu cầu
                    </a>
                </div>
            </section>
        </main>
        <%@ include file="public-footer.jspf" %>
        <script src="${pageContext.request.contextPath}/js/public-portal.js" defer>
        </script>
    </body>
</html>
