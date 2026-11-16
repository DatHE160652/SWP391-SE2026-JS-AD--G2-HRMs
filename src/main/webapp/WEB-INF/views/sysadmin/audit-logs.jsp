<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <!DOCTYPE html>
        <html lang="<%= com.hrms.i18n.Language.selected(request) %>">

        <head>
            <%@ include file="/WEB-INF/views/common/language.jspf" %>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Nhật ký hệ thống &amp; Email | HRMs.Admin</title>
                <link rel="stylesheet"
                    href="${pageContext.request.contextPath}/css/system-admin.css?v=20261020-company-toolbar">
        </head>

        <body class="admin-page company-page company-list-page">
            <%@ include file="/WEB-INF/views/sysadmin/admin-shell.jspf" %>
                <main class="admin-main" id="main-content">
                    <header class="company-page-title">
                        <nav class="breadcrumb" aria-label="Đường dẫn">
                            <a href="${pageContext.request.contextPath}/systemadmin/dashboard">Giám sát nền tảng</a>
                            <svg class="icon" aria-hidden="true">
                                <use href="${icons}#chevron" />
                            </svg>
                            <span>Nhật ký &amp; Email</span>
                        </nav>
                        <h1>Nhật ký hệ thống &amp; Lịch sử gửi Email</h1>
                    </header>

                    <c:if test="${not empty actionMessage}">
                        <div class="alert alert-success" role="status">
                            <c:out value="${actionMessage}" />
                        </div>
                    </c:if>
                    <c:if test="${not empty actionError}">
                        <div class="alert alert-error" role="alert">
                            <c:out value="${actionError}" />
                        </div>
                    </c:if>

                    <section class="company-toolbar" aria-label="Bộ lọc nhật ký và email">
                        <div class="company-tabs" role="group" aria-label="Chuyển tab giám sát">
                            <a class="${tab == 'audit' ? 'selected' : ''}"
                                aria-current="${tab == 'audit' ? 'page' : 'false'}"
                                href="${pageContext.request.contextPath}/systemadmin/audit-logs?tab=audit">
                                Nhật ký thao tác (
                                <c:out value="${auditLogs.size()}" />)
                            </a>
                            <a class="${tab == 'email' ? 'selected' : ''}"
                                aria-current="${tab == 'email' ? 'page' : 'false'}"
                                href="${pageContext.request.contextPath}/systemadmin/audit-logs?tab=email">
                                Lịch sử gửi Email (
                                <c:out value="${emailAttempts.size()}" />)
                            </a>
                        </div>
                        <form class="company-toolbar-fields" method="get"
                            action="${pageContext.request.contextPath}/systemadmin/audit-logs">
                            <input type="hidden" name="tab" value="<c:out value='${tab}'/>">
                            <label for="audit-q">Tìm kiếm:
                                <input id="audit-q" type="search" name="q" placeholder="Email, công ty, action..."
                                    value="<c:out value='${q}'/>"
                                    style="height:36px; padding:0 10px; border:1px solid var(--border); border-radius:var(--radius-sm); background:#fff;">
                            </label>
                            <c:if test="${tab == 'audit'}">
                                <label for="audit-action">Hành động:
                                    <select id="audit-action" name="actionFilter" onchange="this.form.submit()">
                                        <option value="">Tất cả hành động</option>
                                        <option value="CONFIRM_REQUEST" ${actionFilter=='CONFIRM_REQUEST' ? 'selected'
                                            : '' }>Phê duyệt &amp; khởi tạo doanh nghiệp</option>
                                        <option value="RESEND_ACTIVATION" ${actionFilter=='RESEND_ACTIVATION'
                                            ? 'selected' : '' }>Gửi lại email kích hoạt</option>
                                        <option value="DELETE_REQUEST" ${actionFilter=='DELETE_REQUEST' ? 'selected'
                                            : '' }>Từ chối / Xóa yêu cầu</option>
                                        <option value="RENEW_SUBSCRIPTION" ${actionFilter=='RENEW_SUBSCRIPTION'
                                            ? 'selected' : '' }>Gia hạn gói dịch vụ</option>
                                        <option value="ROLLBACK_RENEW_SUBSCRIPTION"
                                            ${actionFilter=='ROLLBACK_RENEW_SUBSCRIPTION' ? 'selected' : '' }>
                                            Khôi phục lại</option>
                                        <option value="CHANGE_PLAN" ${actionFilter=='CHANGE_PLAN' ? 'selected' : '' }>
                                            Đổi gói dịch vụ</option>
                                        <option value="CREATE_PLAN" ${actionFilter=='CREATE_PLAN' ? 'selected' : '' }>
                                            Tạo gói dịch vụ</option>
                                        <option value="UPDATE_PLAN" ${actionFilter=='UPDATE_PLAN' ? 'selected' : '' }>
                                            Cập nhật gói dịch vụ</option>
                                    </select>
                                </label>
                            </c:if>
                            <c:if test="${tab == 'email'}">
                                <label for="email-status">Trạng thái:
                                    <select id="email-status" name="statusFilter" onchange="this.form.submit()">
                                        <option value="">Tất cả trạng thái</option>
                                        <option value="ACCEPTED" ${statusFilter=='ACCEPTED' ? 'selected' : '' }>ACCEPTED
                                        </option>
                                        <option value="FAILED" ${statusFilter=='FAILED' ? 'selected' : '' }>FAILED
                                        </option>
                                        <option value="UNKNOWN" ${statusFilter=='UNKNOWN' ? 'selected' : '' }>UNKNOWN
                                        </option>
                                        <option value="PENDING" ${statusFilter=='PENDING' ? 'selected' : '' }>PENDING
                                        </option>
                                    </select>
                                </label>
                            </c:if>
                            <button class="button button-small button-primary" type="submit">Lọc</button>
                        </form>
                    </section>

                    <c:if test="${tab == 'audit'}">
                        <section class="company-list-panel" aria-label="Bảng nhật ký thao tác">
                            <div class="table-wrap" tabindex="0">
                                <table class="company-table">
                                    <thead>
                                        <tr>
                                            <th scope="col">ID</th>
                                            <th scope="col">Thời gian</th>
                                            <th scope="col">Người thực hiện</th>
                                            <th scope="col">Hành động</th>
                                            <th scope="col">Doanh nghiệp</th>
                                            <th scope="col" class="company-action-heading">Thao tác</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="log" items="${auditLogs}">
                                            <tr>
                                                <td><strong class="workspace-code">#
                                                        <c:out value="${log.logId()}" />
                                                    </strong></td>
                                                <td class="date-cell">
                                                    <c:out value="${log.createdAtDisplay()}" />
                                                </td>
                                                <td>
                                                    <strong class="company-representative">
                                                        <c:out value="${log.actorName()}" default="System" />
                                                    </strong>
                                                    <span class="company-email">
                                                        <c:out value="${log.actorEmail()}" />
                                                    </span>
                                                </td>
                                                <td><span class="region-label">
                                                        <c:out value="${log.actionLabel()}" />
                                                    </span></td>
                                                <td class="company-name">
                                                    <c:out value="${log.companyName()}" default="—" />
                                                </td>
                                                <td class="company-action">
                                                    <button class="company-details-button" type="button"
                                                        data-dialog-open="audit-detail-${log.logId()}">
                                                        Chi tiết <svg class="icon" aria-hidden="true">
                                                            <use href="${icons}#arrow-right" />
                                                        </svg>
                                                    </button>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                        <c:if test="${empty auditLogs}">
                                            <tr>
                                                <td colspan="6" class="muted" style="text-align:center; padding:24px;">
                                                    Không có nhật ký nào phù hợp.</td>
                                            </tr>
                                        </c:if>
                                    </tbody>
                                </table>
                            </div>
                        </section>

                        <c:forEach var="log" items="${auditLogs}">
                            <dialog class="action-dialog" id="audit-detail-${log.logId()}"
                                aria-labelledby="audit-detail-title-${log.logId()}">
                                <h2 id="audit-detail-title-${log.logId()}">Chi tiết Nhật ký #
                                    <c:out value="${log.logId()}" />
                                </h2>
                                <p><strong>Hành động:</strong>
                                    <c:out value="${log.actionLabel()}" />
                                </p>
                                <dl class="subscription-data" style="margin:12px 0;">
                                    <div>
                                        <dt>Thời gian:</dt>
                                        <dd>
                                            <c:out value="${log.createdAtDisplay()}" />
                                        </dd>
                                    </div>
                                    <div>
                                        <dt>Người thực hiện:</dt>
                                        <dd>
                                            <c:out value="${log.actorName()}" default="System" /> (
                                            <c:out value="${log.actorEmail()}" default="—" />)
                                        </dd>
                                    </div>
                                    <div>
                                        <dt>Đối tượng:</dt>
                                        <dd>
                                            <c:out value="${log.tableName()}" /> #
                                            <c:out value="${log.recordId()}" />
                                        </dd>
                                    </div>
                                    <div>
                                        <dt>Doanh nghiệp:</dt>
                                        <dd>
                                            <c:out value="${log.companyName()}" default="—" />
                                        </dd>
                                    </div>
                                    <c:if test="${not empty log.oldValues()}">
                                        <div>
                                            <dt>Trước thay đổi:</dt>
                                            <dd style="word-break:break-word;">
                                                <c:out value="${log.oldValuesDisplay()}" />
                                            </dd>
                                        </div>
                                    </c:if>
                                    <c:if test="${not empty log.newValues()}">
                                        <div>
                                            <dt>Sau thay đổi:</dt>
                                            <dd style="word-break:break-word;">
                                                <c:out value="${log.newValuesDisplay()}" />
                                            </dd>
                                        </div>
                                    </c:if>
                                </dl>
                                <div class="dialog-actions">
                                    <c:if
                                        test="${log.canRollbackRenew() and log.actorUserId() == sessionScope.systemAdminPrincipal.userId}">
                                        <form method="post"
                                            action="${pageContext.request.contextPath}/systemadmin/audit-logs/rollback-renew"
                                            style="display:inline;">
                                            <input type="hidden" name="csrfToken"
                                                value="<c:out value='${sessionScope.systemAdminCsrfToken}'/>">
                                            <input type="hidden" name="logId" value="${log.logId()}">
                                            <button class="button button-danger" type="submit">Khôi phục lại</button>
                                        </form>
                                    </c:if>
                                    <button class="button button-primary" type="button" data-dialog-close>Đóng</button>
                                </div>
                            </dialog>
                        </c:forEach>
                    </c:if>

                    <c:if test="${tab == 'email'}">
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
                                                <td><strong class="workspace-code">#
                                                        <c:out value="${ea.emailAttemptId()}" />
                                                    </strong></td>
                                                <td><code><c:out value="${ea.emailType()}"/></code></td>
                                                <td><strong>
                                                        <c:out value="${ea.recipientEmail()}" />
                                                    </strong></td>
                                                <td>
                                                    <c:out value="${ea.companyName()}" default="—" />
                                                    <c:if test="${ea.registrationRequestId() != null}">
                                                        <a
                                                            href="${pageContext.request.contextPath}/systemadmin/registration-request-details?id=${ea.registrationRequestId()}">(#REQ-${ea.registrationRequestId()})</a>
                                                    </c:if>
                                                </td>
                                                <td>
                                                    <span
                                                        class="subscription-status ${ea.status() == 'ACCEPTED' ? 'is-active' : (ea.status() == 'FAILED' ? 'is-suspended' : '')}">
                                                        <i></i>
                                                        <c:out value="${ea.status()}" />
                                                    </span>
                                                </td>
                                                <td><code><c:out value="${ea.errorCode()}" default="—"/></code></td>
                                                <td class="date-cell">
                                                    <c:out value="${ea.createdAtDisplay()}" />
                                                </td>
                                                <td class="date-cell">
                                                    <c:out value="${ea.completedAtDisplay()}" />
                                                </td>
                                            </tr>
                                        </c:forEach>
                                        <c:if test="${empty emailAttempts}">
                                            <tr>
                                                <td colspan="8" class="muted" style="text-align:center; padding:24px;">
                                                    Không có lịch sử gửi email nào phù hợp.</td>
                                            </tr>
                                        </c:if>
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    </c:if>
                </main>
        </body>

        </html>