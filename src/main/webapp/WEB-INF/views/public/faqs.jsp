<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
    <head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title><%= com.hrms.i18n.Language.text(request, "ui_df8535767e42", "Hỏi đáp | HRMs Cloud") %></title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/public-portal.css">
    </head>
    <body>
        <%@ include file="public-header.jspf" %>
        <main>
            <section class="page-hero public-container faq-page-hero">
                <span class="eyebrow"><%= com.hrms.i18n.Language.text(request, "ui_c4ef4fee930b", "TRUNG TÂM HỎI ĐÁP") %></span>
                <h1><%= com.hrms.i18n.Language.text(request, "ui_b74208ae72a9", "Thông tin rõ ràng trước khi bắt đầu") %></h1>
                <p>
                    <%= com.hrms.i18n.Language.text(request, "ui_c14a4274937b", "Khách truy cập có thể xem các câu trả lời thường gặp về HRMs Cloud và dịch vụ của chúng tôi.") %>
                </p>
            </section>
            <section class="section section-subtle faq-section">
                <div class="public-container faq-layout">
                    <div class="faq-list" aria-label="<%= com.hrms.i18n.Language.text(request, "ui_1e03bd2b0356", "Các câu hỏi thường gặp") %>">
                        <article class="faq-topic">
                            <button class="faq-question" type="button" aria-expanded="false" aria-controls="faq-answer-1">
                                <span><%= com.hrms.i18n.Language.text(request, "ui_48d8a7566bd1", "HRMs Cloud cung cấp những phân hệ nào?") %></span>
                                <span class="faq-chevron" aria-hidden="true">⌄</span>
                            </button>
                            <div class="faq-answer" id="faq-answer-1" hidden>
                                <p><%= com.hrms.i18n.Language.text(request, "ui_9dda9b006851", "Nền tảng gồm hồ sơ nhân sự số, chấm công, tính lương và cổng tự phục vụ nhân viên.") %></p>
                            </div>
                        </article>
                        <article class="faq-topic">
                            <button class="faq-question" type="button" aria-expanded="false" aria-controls="faq-answer-2">
                                <span><%= com.hrms.i18n.Language.text(request, "ui_870f87969933", "Gói tham chiếu có giá bao nhiêu?") %></span>
                                <span class="faq-chevron" aria-hidden="true">⌄</span>
                            </button>
                            <div class="faq-answer" id="faq-answer-2" hidden>
                                <p><%= com.hrms.i18n.Language.text(request, "ui_1a15efc4f1c3", "Cấu hình tham chiếu là 59.000 đồng cho mỗi công ty mỗi tháng, không yêu cầu chọn nhiều plan.") %></p>
                            </div>
                        </article>
                        <article class="faq-topic">
                            <button class="faq-question" type="button" aria-expanded="false" aria-controls="faq-answer-3">
                                <span><%= com.hrms.i18n.Language.text(request, "ui_b7d2558058f6", "Gửi yêu cầu tư vấn có tạo tài khoản ngay không?") %></span>
                                <span class="faq-chevron" aria-hidden="true">⌄</span>
                            </button>
                            <div class="faq-answer" id="faq-answer-3" hidden>
                                <p><%= com.hrms.i18n.Language.text(request, "ui_5120840bc725", "Không. Form chỉ tạo Registration Request ở trạng thái NEW; Company, User và Subscription chỉ được tạo sau khi System Admin confirm.") %></p>
                            </div>
                        </article>
                        <article class="faq-topic">
                            <button class="faq-question" type="button" aria-expanded="false" aria-controls="faq-answer-4">
                                <span><%= com.hrms.i18n.Language.text(request, "ui_e39ccf32158a", "Khách chưa đăng nhập có thể làm gì trên FAQ?") %></span>
                                <span class="faq-chevron" aria-hidden="true">⌄</span>
                            </button>
                            <div class="faq-answer" id="faq-answer-4" hidden>
                                <p><%= com.hrms.i18n.Language.text(request, "ui_4ac8916a1e7d", "Khách có thể xem các câu trả lời FAQ mà không cần đăng nhập. Để được tư vấn, khách có thể gửi yêu cầu qua biểu mẫu liên hệ.") %></p>
                            </div>
                        </article>
                        <article class="faq-topic">
                            <button class="faq-question" type="button" aria-expanded="false" aria-controls="faq-answer-5">
                                <span><%= com.hrms.i18n.Language.text(request, "ui_d001897beeb9", "Một Company có tối đa bao nhiêu tài khoản?") %></span>
                                <span class="faq-chevron" aria-hidden="true">⌄</span>
                            </button>
                            <div class="faq-answer" id="faq-answer-5" hidden>
                                <p><%= com.hrms.i18n.Language.text(request, "ui_6b50648a9b11", "Cấu hình tham chiếu hỗ trợ tối đa 20 tài khoản, gồm một Company Admin và tối đa ba Manager theo chính sách nền tảng.") %></p>
                            </div>
                        </article>
                    </div>
                    <aside class="faq-aside">
                        <div class="info-card">
                            <span class="icon-circle" aria-hidden="true">?</span>
                            <h2><%= com.hrms.i18n.Language.text(request, "ui_9ba9a066cdf2", "Khách có thể làm gì?") %></h2>
                            <ul class="check-list">
                                <li><%= com.hrms.i18n.Language.text(request, "ui_fcfcce9488b1", "Xem các câu trả lời FAQ công khai") %></li>
                                <li><%= com.hrms.i18n.Language.text(request, "ui_f78990a5bb46", "Truy cập Features và Pricing") %></li>
                                <li><%= com.hrms.i18n.Language.text(request, "ui_35408fa869e5", "Gửi yêu cầu tư vấn không cần đăng nhập") %></li>
                                <li><%= com.hrms.i18n.Language.text(request, "ui_96113688d2e2", "Đăng nhập Tenant khi đã được cấp tài khoản") %></li>
                            </ul>
                            <a class="button button-outline button-wide" href="${pageContext.request.contextPath}/login">
                                <%= com.hrms.i18n.Language.text(request, "ui_5ab2fb9bebcd", "Đăng nhập Tenant") %>
                            </a>
                        </div>
                    </aside>
                </div>
            </section>
        </main>
        <%@ include file="public-footer.jspf" %>
        <script src="${pageContext.request.contextPath}/js/public-portal.js?v=<%= application.getResource("/js/public-portal.js").openConnection().getLastModified() %>" defer></script>
    </body>
</html>
