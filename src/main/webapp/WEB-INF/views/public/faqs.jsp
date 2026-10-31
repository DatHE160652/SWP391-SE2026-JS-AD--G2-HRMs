<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Hỏi đáp | HRMs Cloud</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/public-portal.css">
    </head>
    <body>
        <%@ include file="public-header.jspf" %>
        <main>
            <section class="page-hero public-container faq-page-hero">
                <span class="eyebrow">TRUNG TÂM HỎI ĐÁP</span>
                <h1>Thông tin rõ ràng trước khi bắt đầu</h1>
                <p>
                    Khách truy cập có thể xem các câu trả lời thường gặp về HRMs Cloud và dịch vụ của chúng tôi.
                </p>
            </section>
            <section class="section section-subtle faq-section">
                <div class="public-container faq-layout">
                    <div class="faq-list" aria-label="Các câu hỏi thường gặp">
                        <article class="faq-topic">
                            <button class="faq-question" type="button" aria-expanded="false" aria-controls="faq-answer-1">
                                <span>HRMs Cloud cung cấp những phân hệ nào?</span>
                                <span class="faq-chevron" aria-hidden="true">⌄</span>
                            </button>
                            <div class="faq-answer" id="faq-answer-1" hidden>
                                <p>Nền tảng gồm hồ sơ nhân sự số, chấm công, tính lương và cổng tự phục vụ nhân viên.</p>
                            </div>
                        </article>
                        <article class="faq-topic">
                            <button class="faq-question" type="button" aria-expanded="false" aria-controls="faq-answer-2">
                                <span>Gói tham chiếu có giá bao nhiêu?</span>
                                <span class="faq-chevron" aria-hidden="true">⌄</span>
                            </button>
                            <div class="faq-answer" id="faq-answer-2" hidden>
                                <p>Cấu hình tham chiếu là 59.000 đồng cho mỗi công ty mỗi tháng, không yêu cầu chọn nhiều plan.</p>
                            </div>
                        </article>
                        <article class="faq-topic">
                            <button class="faq-question" type="button" aria-expanded="false" aria-controls="faq-answer-3">
                                <span>Gửi yêu cầu tư vấn có tạo tài khoản ngay không?</span>
                                <span class="faq-chevron" aria-hidden="true">⌄</span>
                            </button>
                            <div class="faq-answer" id="faq-answer-3" hidden>
                                <p>Không. Form chỉ tạo Registration Request ở trạng thái NEW; Company, User và Subscription chỉ được tạo sau khi System Admin confirm.</p>
                            </div>
                        </article>
                        <article class="faq-topic">
                            <button class="faq-question" type="button" aria-expanded="false" aria-controls="faq-answer-4">
                                <span>Khách chưa đăng nhập có thể làm gì trên FAQ?</span>
                                <span class="faq-chevron" aria-hidden="true">⌄</span>
                            </button>
                            <div class="faq-answer" id="faq-answer-4" hidden>
                                <p>Khách có thể xem các câu trả lời FAQ mà không cần đăng nhập. Để được tư vấn, khách có thể gửi yêu cầu qua biểu mẫu liên hệ.</p>
                            </div>
                        </article>
                        <article class="faq-topic">
                            <button class="faq-question" type="button" aria-expanded="false" aria-controls="faq-answer-5">
                                <span>Một Company có tối đa bao nhiêu tài khoản?</span>
                                <span class="faq-chevron" aria-hidden="true">⌄</span>
                            </button>
                            <div class="faq-answer" id="faq-answer-5" hidden>
                                <p>Cấu hình tham chiếu hỗ trợ tối đa 20 tài khoản, gồm một Company Admin và tối đa ba Manager theo chính sách nền tảng.</p>
                            </div>
                        </article>
                    </div>
                    <aside class="faq-aside">
                        <div class="info-card">
                            <span class="icon-circle" aria-hidden="true">?</span>
                            <h2>Khách có thể làm gì?</h2>
                            <ul class="check-list">
                                <li>Xem các câu trả lời FAQ công khai</li>
                                <li>Truy cập Features và Pricing</li>
                                <li>Gửi yêu cầu tư vấn không cần đăng nhập</li>
                                <li>Đăng nhập Tenant khi đã được cấp tài khoản</li>
                            </ul>
                            <a class="button button-outline button-wide" href="${pageContext.request.contextPath}/login">
                                Đăng nhập Tenant
                            </a>
                        </div>
                    </aside>
                </div>
            </section>
        </main>
        <%@ include file="public-footer.jspf" %>
        <script src="${pageContext.request.contextPath}/js/public-portal.js" defer></script>
    </body>
</html>
