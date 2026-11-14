<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>
            <%= com.hrms.i18n.Language.text(request, "ui_3630ae0ab07e", "Bảng giá | HRMs Cloud") %>
        </title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/public-portal.css">
    </head>
    <body>
        <%@ include file="public-header.jspf" %>
        <main>
            <section class="page-hero public-container">
                <span class="eyebrow">
                    <%= com.hrms.i18n.Language.text(request, "ui_70dbd1498ef5", "BẢNG GIÁ MINH BẠCH") %>
                </span>
                <h1>
                    <%= com.hrms.i18n.Language.text(request, "ui_5337fd70c5bd", "Một cấu hình đơn giản, đủ cho doanh nghiệp tăng trưởng") %>
                </h1>
                <p>
                    <%= com.hrms.i18n.Language.text(request, "ui_34b284108f30", "Không cần chọn giữa nhiều gói phức tạp. Gói tham chiếu của HRMs Cloud cung cấp đầy đủ nền tảng quản trị nhân sự.") %>
                </p>
            </section>
            <section class="section section-subtle">
                <div class="public-container pricing-layout">
                    <article class="pricing-card">
                        <span class="pricing-badge">
                            <%= com.hrms.i18n.Language.text(request, "ui_91c4a729f564", "GÓI CHUẨN DOANH NGHIỆP") %>
                        </span>
                        <h2>
                            <%= com.hrms.i18n.Language.text(request, "ui_881fd42a03e6", "HRMs Cloud · Gói tiêu chuẩn") %>
                        </h2>
                        <div class="price">
                            59.000
                            <span>
                                <%= com.hrms.i18n.Language.text(request, "ui_c5f95801dfa6", "đ") %>
                            </span>
                        </div>
                        <div class="price-unit">
                            <%= com.hrms.i18n.Language.text(request, "ui_bbf6b9e933bc", "/ công ty / tháng") %>
                        </div>
                        <p class="pricing-intro">
                            <%= com.hrms.i18n.Language.text(request, "ui_eaf77dfe5937", "Cấu hình tham chiếu cố định, không yêu cầu chọn gói dịch vụ khi gửi yêu cầu tư vấn.") %>
                        </p>
                        <a class="button button-primary button-wide" href="${pageContext.request.contextPath}/consultation-request">
                            <%= com.hrms.i18n.Language.text(request, "ui_704751ca32bd", "Đăng ký tư vấn") %>
                        </a>
                    </article>
                    <div class="pricing-details">
                        <h2>
                            <%= com.hrms.i18n.Language.text(request, "ui_4b006bd63e0d", "Đã bao gồm") %>
                        </h2>
                        <ul class="check-list large">
                            <li>
                                <%= com.hrms.i18n.Language.text(request, "ui_b77e004241a5", "Đầy đủ e-HRM, chấm công, tính lương và ESS") %>
                            </li>
                            <li>
                                <%= com.hrms.i18n.Language.text(request, "ui_587b165db8ac", "Tối đa 20 tài khoản trong một công ty") %>
                            </li>
                            <li>
                                <%= com.hrms.i18n.Language.text(request, "ui_cbad3dc3bec3", "1 Quản trị viên công ty và tối đa 3 quản lý") %>
                            </li>
                            <li>
                                <%= com.hrms.i18n.Language.text(request, "ui_44337cdc207a", "Workspace và dữ liệu được phân tách theo công ty") %>
                            </li>
                            <li>
                                <%= com.hrms.i18n.Language.text(request, "ui_952674e51bb7", "Hỗ trợ quy trình kích hoạt sau khi Quản trị viên hệ thống xác nhận") %>
                            </li>
                        </ul>
                        <div class="pricing-note">
                            <strong>
                                <%= com.hrms.i18n.Language.text(request, "ui_7a62ab00d8cf", "Lưu ý") %>
                            </strong>
                            <p>
                                <%= com.hrms.i18n.Language.text(request, "ui_1dd7e1fdae60", "Gửi biểu mẫu tư vấn chỉ tạo yêu cầu đăng ký. Hệ thống chưa tạo công ty tại bước này.") %>
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
