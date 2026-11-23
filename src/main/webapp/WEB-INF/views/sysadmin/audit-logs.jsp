<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Lịch sử gửi Email | HRMs.Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/system-admin.css?v=20261020-company-toolbar">
</head>
<body class="admin-page company-page company-list-page">
    <%@ include file="/WEB-INF/views/sysadmin/admin-shell.jspf" %>
    <main class="admin-main" id="main-content">
        <header class="company-page-title">
            <nav class="breadcrumb" aria-label="Đường dẫn">
                <a href="${pageContext.request.contextPath}/systemadmin/dashboard">Giám sát nền tảng</a>
                <svg class="icon" aria-hidden="true"><use href="${icons}#chevron" /></svg>
                <span>Lịch sử gửi Email</span>
            </nav>
            <h1>Lịch sử gửi Email</h1>
        </header>

        <section class="company-toolbar" aria-label="Bộ lọc lịch sử gửi email">
            <form class="company-toolbar-fields" method="get" action="${pageContext.request.contextPath}/systemadmin/audit-logs">
                <label for="email-status">Trạng thái:
                    <select id="email-status" name="statusFilter" onchange="this.form.submit()">
                        <option value="">Tất cả trạng thái</option>
                        <option value="ACCEPTED" ${statusFilter == 'ACCEPTED' ? 'selected' : ''}>ACCEPTED</option>
                        <option value="FAILED" ${statusFilter == 'FAILED' ? 'selected' : ''}>FAILED</option>
                        <option value="UNKNOWN" ${statusFilter == 'UNKNOWN' ? 'selected' : ''}>UNKNOWN</option>
                        <option value="PENDING" ${statusFilter == 'PENDING' ? 'selected' : ''}>PENDING</option>
                    </select>
                </label>
                <button class="button button-small button-primary" type="submit">Lọc</button>
            </form>
        </section>

        <section class="company-list-panel" aria-label="Bảng lịch sử gửi email">
            <div class="table-wrap" tabindex="0">
                <table class="company-table">
                    <thead>
                        <tr>
                            <th scope="col">ID</th>
                            <th scope="col">Loại thư</th>
                            <th scope="col">Người nhận</th>
                            <th scope="col">Doanh nghiệp / Request</th>
                            <th scope="col">Trạng thái</th>
                            <th scope="col">Mã lỗi</th>
                            <th scope="col">Thời gian tạo</th>
                            <th scope="col">Hoàn tất</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="ea" items="${emailAttempts}">
                            <tr>
                                <td><strong class="workspace-code">#<c:out value="${ea.emailAttemptId()}" /></strong></td>
                                <td><code><c:out value="${ea.emailType()}" /></code></td>
                                <td><strong><c:out value="${ea.recipientEmail()}" /></strong></td>
                                <td>
                                    <c:out value="${ea.companyName()}" default="—" />
                                    <c:if test="${ea.registrationRequestId() != null}">
                                        <a href="${pageContext.request.contextPath}/systemadmin/registration-request-details?id=${ea.registrationRequestId()}">(#REQ-<c:out value="${ea.registrationRequestId()}" />)</a>
                                    </c:if>
                                </td>
                                <td>
                                    <span class="subscription-status ${ea.status() == 'ACCEPTED' ? 'is-active' : (ea.status() == 'FAILED' ? 'is-suspended' : '')}">
                                        <i></i><c:out value="${ea.status()}" />
                                    </span>
                                </td>
                                <td><code><c:out value="${ea.errorCode()}" default="—" /></code></td>
                                <td class="date-cell"><c:out value="${ea.createdAtDisplay()}" /></td>
                                <td class="date-cell"><c:out value="${ea.completedAtDisplay()}" /></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty emailAttempts}">
                            <tr>
                                <td colspan="8" class="muted" style="text-align:center; padding:24px;">Không có lịch sử gửi email nào phù hợp.</td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </section>
    </main>
</body>
</html>
