<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>
            <%= com.hrms.i18n.Language.text(request, "ui_599096630a97", "Tính năng &amp; Giải pháp | HRMs Cloud") %>
        </title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/public-portal.css">
    </head>
    <body>
        <%@ include file="public-header.jspf" %>
        <main>
            <section class="page-hero public-container">
                <span class="eyebrow">
                    <%= com.hrms.i18n.Language.text(request, "ui_2f75e9a5ee76", "TÍNH NĂNG &amp; GIẢI PHÁP") %>
                </span>
                <h1>
                    <%= com.hrms.i18n.Language.text(request, "ui_40f3fdfdd751", "Thiết kế để đội ngũ nhân sự vận hành nhẹ nhàng hơn") %>
                </h1>
                <p>
                    <%= com.hrms.i18n.Language.text(request, "ui_4926f6a74a96", "HRMs Cloud kết nối dữ liệu và quy trình nhân sự trong một trải nghiệm thống nhất, dễ bắt đầu cho doanh nghiệp Việt Nam.") %>
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
                                <%= com.hrms.i18n.Language.text(request, "ui_c9c55114a63c", "Hồ sơ nhân sự số") %>
                            </h2>
                            <p>
                                <%= com.hrms.i18n.Language.text(request, "ui_e8fb6a0f50d9", "Quản lý thông tin nhân viên, tài liệu, phòng ban và chức danh tập trung. Dữ liệu được chuẩn hóa để nhân sự dễ tra cứu và cập nhật.") %>
                            </p>
                            <ul class="check-list">
                                <li>
                                    <%= com.hrms.i18n.Language.text(request, "ui_136c953fd683", "Hồ sơ và tài liệu theo nhân viên") %>
                                </li>
                                <li>
                                    <%= com.hrms.i18n.Language.text(request, "ui_53e224f9655d", "Cấu trúc phòng ban, vị trí, người quản lý") %>
                                </li>
                                <li>
                                    <%= com.hrms.i18n.Language.text(request, "ui_3dc7a397baf6", "Phân quyền theo workspace") %>
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
                                <%= com.hrms.i18n.Language.text(request, "ui_9190b3c34e5e", "Chấm công &amp; ca kíp") %>
                            </h2>
                            <p>
                                <%= com.hrms.i18n.Language.text(request, "ui_ab3223e799eb", "Đơn giản hóa việc theo dõi thời gian làm việc, ca làm và các quy trình phê duyệt liên quan.") %>
                            </p>
                            <ul class="check-list">
                                <li>
                                    <%= com.hrms.i18n.Language.text(request, "ui_2d3782c11dae", "Quản lý ngày công và lịch làm việc") %>
                                </li>
                                <li>
                                    <%= com.hrms.i18n.Language.text(request, "ui_710e1e6aabb6", "Hỗ trợ quy trình điều chỉnh minh bạch") %>
                                </li>
                                <li>
                                    <%= com.hrms.i18n.Language.text(request, "ui_378fbcb09770", "Dữ liệu sẵn sàng cho tính lương") %>
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
                                <%= com.hrms.i18n.Language.text(request, "ui_98bfbe83503c", "Tính lương &amp; bảo hiểm") %>
                            </h2>
                            <p>
                                <%= com.hrms.i18n.Language.text(request, "ui_834eee34bb46", "Chuẩn hóa dữ liệu đầu vào để giảm thao tác lặp lại và giúp đội ngũ kiểm tra bảng lương có hệ thống.") %>
                            </p>
                            <ul class="check-list">
                                <li>
                                    <%= com.hrms.i18n.Language.text(request, "ui_8ed4cde00268", "Cấu trúc dữ liệu lương rõ ràng") %>
                                </li>
                                <li>
                                    <%= com.hrms.i18n.Language.text(request, "ui_27ca8e794473", "Kiểm tra trước khi phê duyệt") %>
                                </li>
                                <li>
                                    <%= com.hrms.i18n.Language.text(request, "ui_00fb78f2a9e4", "Lưu vết theo kỳ xử lý") %>
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
                                <%= com.hrms.i18n.Language.text(request, "ui_e8da4828ec75", "Cổng tự phục vụ nhân viên") %>
                            </h2>
                            <p>
                                <%= com.hrms.i18n.Language.text(request, "ui_38628d7285b4", "Đưa những thông tin và thao tác thường ngày đến gần nhân viên hơn, đồng thời giảm tải cho nhân sự.") %>
                            </p>
                            <ul class="check-list">
                                <li>
                                    <%= com.hrms.i18n.Language.text(request, "ui_a53c62d42491", "Trải nghiệm theo vai trò") %>
                                </li>
                                <li>
                                    <%= com.hrms.i18n.Language.text(request, "ui_857f6228adbd", "Thông tin thống nhất trong workspace") %>
                                </li>
                                <li>
                                    <%= com.hrms.i18n.Language.text(request, "ui_2b653f67d07c", "Quy trình tương tác có kiểm soát") %>
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
                            <%= com.hrms.i18n.Language.text(request, "ui_a2c6d69e5cd4", "WORKSPACE RIÊNG BIỆT") %>
                        </span>
                        <h2>
                            <%= com.hrms.i18n.Language.text(request, "ui_045441cd73d4", "Kiểm soát dữ liệu theo đúng ranh giới doanh nghiệp") %>
                        </h2>
                        <p>
                            <%= com.hrms.i18n.Language.text(request, "ui_cf73bf9a6b39", "Quản trị viên công ty quản lý workspace của mình; Quản trị viên hệ thống quản trị nền tảng nhưng không truy cập dữ liệu nhân sự chi tiết của tenant.") %>
                        </p>
                    </div>
                    <a class="button button-primary" href="${pageContext.request.contextPath}/consultation-request">
                        <%= com.hrms.i18n.Language.text(request, "ui_16a3c62b7420", "Trao đổi nhu cầu") %>
                    </a>
                </div>
            </section>
        </main>
        <%@ include file="public-footer.jspf" %>
        <script src="${pageContext.request.contextPath}/js/public-portal.js?v=<%= application.getResource("/js/public-portal.js").openConnection().getLastModified() %>" defer>
        </script>
    </body>
</html>
