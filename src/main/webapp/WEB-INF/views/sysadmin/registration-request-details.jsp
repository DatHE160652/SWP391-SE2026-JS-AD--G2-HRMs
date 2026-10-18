<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
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
        <div class="detail-title"><h1 id="detail-title">Chi tiết đăng ký tenant</h1><p class="detail-meta"><span class="request-code">#REQ-<c:out value="${item.registrationRequestId}"/></span><span class="status status-${item.status}"><span class="status-dot" aria-hidden="true"></span><c:out value="${item.statusLabel}"/></span></p><p class="muted">Tiếp nhận: <c:out value="${item.createdAtDisplay}"/> UTC</p></div>
        <div class="detail-actions"><button class="button button-danger" type="button" disabled aria-describedby="action-help"><svg class="icon" aria-hidden="true"><use href="${icons}#close"/></svg>Từ chối đăng ký</button><button class="button button-primary" type="button" disabled aria-describedby="action-help"><svg class="icon" aria-hidden="true"><use href="${icons}#rocket"/></svg>Phê duyệt &amp; khởi tạo tenant</button></div>
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
    <p class="read-only-note"><svg class="icon" aria-hidden="true"><use href="${icons}#info"/></svg>Mở chi tiết không làm thay đổi trạng thái yêu cầu hoặc tạo tenant.</p>
</main>
</body>
</html>
