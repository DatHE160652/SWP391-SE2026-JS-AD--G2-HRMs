<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chi tiết yêu cầu | HRMs.Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/system-admin.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/views/sysadmin/admin-shell.jspf" %>
<main class="admin-main" id="main-content">
    <a class="back-link" href="${pageContext.request.contextPath}/systemadmin/registration-request"><svg class="icon" aria-hidden="true"><use href="${icons}#arrow-left"/></svg>Quay lại danh sách yêu cầu</a>
    <c:set var="item" value="${registrationRequest}"/>
    <section class="panel detail-heading" aria-labelledby="detail-title">
        <span class="detail-symbol"><svg class="icon" aria-hidden="true"><use href="${icons}#document"/></svg></span>
        <div class="detail-title"><h1 id="detail-title">Chi tiết yêu cầu đăng ký công ty</h1><p class="detail-meta"><span class="request-code">#REQ-<c:out value="${item.registrationRequestId}"/></span><span class="status status-${item.status}"><span class="status-dot" aria-hidden="true"></span><c:out value="${item.statusLabel}"/></span></p><p class="muted">Tiếp nhận: <c:out value="${item.createdAtDisplay}"/></p></div>
        <div class="detail-actions">
            <c:choose>
                <c:when test="${item.status == 'NEW' or item.status == 'UNDER_REVIEW'}">
                    <button class="button button-danger" type="button" data-dialog-open="delete-request-dialog"><svg class="icon" aria-hidden="true"><use href="${icons}#close"/></svg>Từ chối đăng ký</button>
                    <button class="button button-primary" type="button" data-dialog-open="confirm-request-dialog"><svg class="icon" aria-hidden="true"><use href="${icons}#rocket"/></svg>Phê duyệt &amp; khởi tạo tenant</button>
                </c:when>
                <c:when test="${item.status == 'CONVERTING'}"><p class="muted">Đang khởi tạo công ty; chưa thể thực hiện thao tác khác.</p></c:when>
                <c:otherwise><p class="muted">Yêu cầu đã được xử lý; không thể phê duyệt hoặc xóa mềm.</p></c:otherwise>
            </c:choose>
        </div>
    </section>
    <section class="panel profile-panel" aria-labelledby="details-title">
        <div class="panel-heading"><h2 id="details-title"><svg class="icon" aria-hidden="true"><use href="${icons}#document"/></svg>1. Hồ sơ đăng ký</h2><span class="read-only-badge"><svg class="icon" aria-hidden="true"><use href="${icons}#lock"/></svg>Chế độ chỉ đọc</span></div>
        <div class="detail-grid">
            <div class="detail-card"><span class="detail-label">1. Họ và tên</span><strong><svg class="icon" aria-hidden="true"><use href="${icons}#user"/></svg><c:out value="${item.fullName}"/></strong><span class="muted">Người liên hệ đăng ký</span></div>
            <div class="detail-card"><span class="detail-label">2. Email liên hệ</span><strong><svg class="icon" aria-hidden="true"><use href="${icons}#mail"/></svg><c:out value="${item.email}"/></strong></div>
            <div class="detail-card"><span class="detail-label">3. Số điện thoại</span><strong><svg class="icon" aria-hidden="true"><use href="${icons}#phone"/></svg><c:out value="${item.phone}"/></strong></div>
            <div class="detail-card"><span class="detail-label">4. Khu vực</span><strong><svg class="icon" aria-hidden="true"><use href="${icons}#map"/></svg><c:out value="${item.regionLabel}"/></strong></div>
            <div class="detail-card detail-card-wide"><span class="detail-label">5. Tên công ty / Doanh nghiệp đầy đủ</span><strong><svg class="icon" aria-hidden="true"><use href="${icons}#building"/></svg><c:out value="${item.companyName}"/></strong></div>
        </div>
    </section>
    <c:choose>
        <c:when test="${item.status == 'NEW' or item.status == 'UNDER_REVIEW'}">
            <p class="read-only-note"><svg class="icon" aria-hidden="true"><use href="${icons}#info"/></svg>Yêu cầu đang chờ xử lý. Chưa có công ty, workspace hoặc tài khoản quản trị được tạo. Xem chi tiết không làm thay đổi trạng thái.</p>
        </c:when>
        <c:when test="${item.status == 'CONVERTING'}">
            <p class="read-only-note"><svg class="icon" aria-hidden="true"><use href="${icons}#clock"/></svg>Hệ thống đang khởi tạo công ty. Vui lòng tải lại trang để xem kết quả; không gửi lại yêu cầu phê duyệt.</p>
        </c:when>
    </c:choose>

    <c:if test="${item.status == 'CONVERTED'}">
        <section class="panel profile-panel" aria-labelledby="conversion-title">
            <div class="panel-heading">
                <h2 id="conversion-title"><svg class="icon" aria-hidden="true"><use href="${icons}#check"/></svg>2. Kết quả phê duyệt công ty</h2>
            </div>
            <div class="detail-grid">
                <div class="detail-card">
                    <span class="detail-label">Mã workspace</span>
                    <strong><c:out value="${item.workspaceCode}" default="—"/></strong>
                    <span class="muted">Mã không gian làm việc của công ty, dùng cùng email khi đăng nhập.</span>
                </div>
                <div class="detail-card">
                    <span class="detail-label">Thời gian phê duyệt</span>
                    <strong><c:out value="${item.convertedAtDisplay}"/></strong>
                </div>
                <div class="detail-card">
                    <span class="detail-label">Người phê duyệt</span>
                    <strong><c:out value="${item.convertedByName}" default="—"/></strong>
                </div>
                <div class="detail-card">
                    <span class="detail-label">Kết quả</span>
                    <strong>Đã khởi tạo công ty</strong>
                    <span class="muted">Đã tạo workspace, Company Admin và thuê bao. Kết quả gửi email được theo dõi riêng bên dưới.</span>
                </div>
            </div>
        </section>

        <section class="panel profile-panel" aria-labelledby="admin-account-title">
            <div class="panel-heading">
                <h2 id="admin-account-title"><svg class="icon" aria-hidden="true"><use href="${icons}#shield"/></svg>3. Tài khoản quản trị công ty</h2>
            </div>
            <div class="detail-grid">
                <div class="detail-card">
                    <span class="detail-label">Email đăng nhập</span>
                    <strong><c:out value="${item.companyAdminEmail}" default="Chưa có thông tin"/></strong>
                    <span class="muted">Chỉ đăng nhập bằng email, không dùng tên tài khoản.</span>
                </div>
                <div class="detail-card">
                    <span class="detail-label">Vai trò</span>
                    <strong>Company Admin</strong>
                    <span class="muted">Quản trị trong phạm vi công ty.</span>
                </div>
                <div class="detail-card">
                    <span class="detail-label">Trạng thái kích hoạt</span>
                    <strong>
                        <c:choose>
                            <c:when test="${empty item.companyAdminEmail}">Chưa có thông tin tài khoản</c:when>
                            <c:when test="${item.companyAdminActivated}">Đã kích hoạt</c:when>
                            <c:otherwise>Chưa hoàn tất đổi mật khẩu tạm</c:otherwise>
                        </c:choose>
                    </strong>
                    <span class="muted">Kích hoạt hoàn tất khi người dùng đổi mật khẩu tạm, không phải khi email được gửi.</span>
                </div>
                <div class="detail-card">
                    <span class="detail-label">Thời gian kích hoạt</span>
                    <strong><c:out value="${item.companyAdminActivatedAtDisplay}"/></strong>
                </div>
            </div>
        </section>

        <section class="panel profile-panel" aria-labelledby="activation-email-title">
            <div class="panel-heading">
                <h2 id="activation-email-title"><svg class="icon" aria-hidden="true"><use href="${icons}#mail"/></svg>4. Email kích hoạt — lần gửi gần nhất</h2>
            </div>
            <div class="detail-grid">
                <div class="detail-card">
                    <span class="detail-label">Kết quả gửi</span>
                    <strong><c:out value="${item.activationEmailStatusLabel}"/></strong>
                    <span class="muted">Bao gồm email kích hoạt ban đầu và các lần gửi lại.</span>
                </div>
                <div class="detail-card">
                    <span class="detail-label">Thời gian ghi nhận kết quả</span>
                    <strong><c:out value="${item.activationEmailCompletedAtDisplay}"/></strong>
                </div>
            </div>
            <p class="read-only-note">Nhà cung cấp tiếp nhận email không có nghĩa email đã đến hộp thư. Gửi thất bại không hủy công ty đã khởi tạo. Mật khẩu không hiển thị trên trang này.</p>
        </section>
    </c:if>

    <c:if test="${not empty actionMessage}"><div class="alert alert-success" role="status"><c:out value="${actionMessage}"/></div></c:if>
    <c:if test="${not empty actionWarning}"><div class="alert alert-warning" role="alert"><c:out value="${actionWarning}"/></div></c:if>

    <c:if test="${item.status == 'NEW' or item.status == 'UNDER_REVIEW'}">
        <dialog class="action-dialog" id="confirm-request-dialog" aria-labelledby="confirm-request-title">
            <form method="post" action="${pageContext.request.contextPath}/systemadmin/registration-request/confirm">
                <input type="hidden" name="id" value="<c:out value='${item.registrationRequestId}'/>">
                <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.systemAdminCsrfToken}'/>">
                <h2 id="confirm-request-title">Phê duyệt và khởi tạo tenant?</h2>
                <p>Hệ thống sẽ tạo công ty, workspace, Company Admin và thuê bao ACTIVE. Sau khi lưu thành công, hệ thống sẽ thử gửi mật khẩu tạm tới email liên hệ. Gửi email thất bại không hủy công ty đã tạo; người dùng phải đổi mật khẩu tạm để hoàn tất kích hoạt.</p>
                <div class="dialog-actions"><button class="button" type="button" data-dialog-close>Hủy</button><button class="button button-primary" type="submit">Xác nhận phê duyệt</button></div>
            </form>
        </dialog>
        <dialog class="action-dialog" id="delete-request-dialog" aria-labelledby="delete-request-title">
            <form method="post" action="${pageContext.request.contextPath}/systemadmin/registration-request/delete">
                <input type="hidden" name="id" value="<c:out value='${item.registrationRequestId}'/>">
                <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.systemAdminCsrfToken}'/>">
                <h2 id="delete-request-title">Từ chối đăng ký?</h2>
                <p>Hồ sơ gốc vẫn được lưu để Sale tiếp tục trao đổi. Request này sẽ bị ẩn khỏi danh sách và không thể đăng ký lại bằng email hoặc tên công ty này.</p>
                <div class="dialog-actions"><button class="button" type="button" data-dialog-close>Hủy</button><button class="button button-danger" type="submit">Xác nhận từ chối</button></div>
            </form>
        </dialog>
    </c:if>
</main>
</body>
</html>
