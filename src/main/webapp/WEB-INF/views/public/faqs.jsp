<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>
            Hỏi đáp | HRMs Cloud
        </title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/public-portal.css">
    </head>
    <body>
        <%@ include file="public-header.jspf" %>
        <main>
            <section class="page-hero public-container">
                <span class="eyebrow">
                    TRUNG TÂM HỎI ĐÁP
                </span>
                <h1>
                    Thông tin rõ ràng trước khi bắt đầu
                </h1>
                <p>
                    Khách truy cập có thể xem chủ đề và bình luận. Đăng nhập tenant để tạo chủ đề hoặc bình luận dưới chủ đề.
                </p>
            </section>
            <section class="section section-subtle">
                <div class="public-container faq-layout">
                    <div class="faq-list">
                        <c:if test="${not empty faqReadError}">
                            <div class="alert alert-error">
                                <c:out value="${faqReadError}"/>
                            </div>
                        </c:if>
                        <c:if test="${not empty sessionScope.tenantPrincipal}">
                            <article class="faq-create-card">
                                <h2>
                                    Tạo chủ đề mới
                                </h2>
                                <form method="post" action="${pageContext.request.contextPath}/faqs/topics" class="faq-form">
                                    <input type="hidden" name="csrfToken" value="<c:out value='${faqCsrfToken}'/>
                                    ">
                                    <label for="topicTitle">
                                        Tiêu đề
                                    </label>
                                    <input id="topicTitle" name="title" type="text" maxlength="200" required value="<c:out value='${faqTitle}'/>">
                                    <c:if test="${empty faqErrorTopicId and faqErrorField == 'title'}"><small class="field-error"><c:out value="${faqErrorMessage}"/></small></c:if>
                                    <label for="topicContent">
                                        Nội dung
                                    </label>
                                    <textarea id="topicContent" name="content" maxlength="10000" required>
                                        <c:if test="${empty faqErrorTopicId}"><c:out value="${faqContent}"/></c:if>
                                    </textarea>
                                    <c:if test="${empty faqErrorTopicId and faqErrorField == 'content'}"><small class="field-error"><c:out value="${faqErrorMessage}"/></small></c:if>
                                    <button class="button button-primary" type="submit">
                                        Đăng chủ đề
                                    </button>
                                </form>
                            </article>
                        </c:if>
                        <c:choose>
                            <c:when test="${empty faqTopics}">
                                <div class="empty-state">
                                    <h2>
                                        Chưa có chủ đề thảo luận
                                    </h2>
                                    <p>
                                        Các câu hỏi thường gặp sẽ được cập nhật tại đây.
                                    </p>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="topic" items="${faqTopics}">
                                    <article class="faq-topic">
                                        <button class="faq-question" type="button" aria-expanded="false">
                                            <span>
                                                <c:out value="${topic.title}"/>
                                            </span>
                                            <span class="faq-chevron" aria-hidden="true">
                                                ⌄
                                            </span>
                                        </button>
                                        <div class="faq-answer" hidden>
                                            <p>
                                                <c:out value="${topic.content}"/>
                                            </p>
                                            <p class="faq-meta">
                                                Đăng bởi
                                                <c:out value="${topic.authorName}"/>
                                                ·
                                                <c:out value="${topic.createdAtDisplay}"/>
                                            </p>
                                            <div class="comments">
                                                <c:choose>
                                                    <c:when test="${empty topic.comments}">
                                                        <p class="muted">
                                                            Chưa có bình luận.
                                                        </p>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <c:forEach var="comment" items="${topic.comments}">
                                                            <div class="comment">
                                                                <p>
                                                                    <c:out value="${comment.content}"/>
                                                                </p>
                                                                <span>
                                                                    <c:out value="${comment.authorName}"/>
                                                                    ·
                                                                    <c:out value="${comment.createdAtDisplay}"/>
                                                                </span>
                                                            </div>
                                                        </c:forEach>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                            <c:choose>
                                                <c:when test="${not empty sessionScope.tenantPrincipal and topic.topicId >
                                                    0}">
                                                    <form method="post" action="${pageContext.request.contextPath}/faqs/comments" class="faq-comment-form">
                                                        <input type="hidden" name="csrfToken" value="<c:out value='${faqCsrfToken}'/>
                                                        ">
                                                        <input type="hidden" name="topicId" value="<c:out value='${topic.topicId}'/>
                                                        ">
                                                        <label class="sr-only" for="comment-${topic.topicId}">
                                                            Bình luận
                                                        </label>
                                                        <textarea id="comment-${topic.topicId}" name="content" maxlength="10000" required placeholder="Viết bình luận của bạn">
                                                            <c:if test="${faqErrorTopicId == topic.topicId}"><c:out value="${faqContent}"/></c:if>
                                                        </textarea>
                                                        <c:if test="${faqErrorTopicId == topic.topicId and faqErrorField == 'content'}"><small class="field-error"><c:out value="${faqErrorMessage}"/></small></c:if>
                                                        <button class="button button-outline" type="submit">
                                                            Gửi bình luận
                                                        </button>
                                                    </form>
                                                </c:when>
                                                <c:otherwise>
                                                    <div class="login-notice">
                                                        Muốn tạo chủ đề hoặc bình luận? Hãy đăng nhập vào cổng tenant.
                                                    </div>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </article>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <aside class="faq-aside">
                        <div class="info-card">
                            <span class="icon-circle">
                                ?
                            </span>
                            <h2>
                                Khách có thể làm gì?
                            </h2>
                            <ul class="check-list">
                                <li>
                                    Xem các chủ đề công khai
                                </li>
                                <li>
                                    Đọc bình luận dưới chủ đề
                                </li>
                                <li>
                                    Truy cập Features và Pricing
                                </li>
                                <li>
                                    Gửi yêu cầu tư vấn không cần đăng nhập
                                </li>
                            </ul>
                            <a class="button button-outline button-wide" href="${pageContext.request.contextPath}/login">
                                Đăng nhập tenant
                            </a>
                        </div>
                    </aside>
                </div>
            </section>
        </main>
        <%@ include file="public-footer.jspf" %>
        <script src="${pageContext.request.contextPath}/js/public-portal.js?v=<%= application.getResource("/js/public-portal.js").openConnection().getLastModified() %>" defer>
        </script>
    </body>
</html>
