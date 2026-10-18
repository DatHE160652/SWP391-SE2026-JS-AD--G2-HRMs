<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đợt cập nhật hồ sơ</title>
    <link rel="stylesheet" href="../css/employee.css">
</head>
<body data-page="profile-update-periods">
<div class="shell">
    <aside class="sidebar">
        <div class="brand">HRMs Cloud<span>Quản trị nhân sự</span></div>
        <c:if test="${canManageEmployees}"><a class="nav-link" href="employee-list">Quản lý Nhân sự</a></c:if>
        <a class="nav-link active" href="profile-update-periods">Đợt cập nhật hồ sơ</a>
        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    </aside>
    <main class="content">
        <div class="page-header">
            <div>
                <h1 class="page-title">Đợt cập nhật hồ sơ</h1>
                <p class="page-subtitle">Employee và Manager chỉ được sửa số điện thoại trong thời gian đợt cập nhật của công ty đang mở.</p>
            </div>
            <button type="button" class="secondary" id="refreshPeriodsButton">Tải lại</button>
        </div>
        <div id="message" class="message" role="status"></div>
        <form id="profileUpdatePeriodForm" class="panel" novalidate>
            <input type="hidden" name="csrfToken">
            <h2>Tạo yêu cầu cập nhật</h2>
            <div class="form-grid">
                <div>
                    <label for="periodTitle">Tên đợt cập nhật</label>
                    <input id="periodTitle" name="title" maxlength="150" required>
                </div>
                <div>
                    <label for="periodStartsAt">Bắt đầu</label>
                    <input id="periodStartsAt" name="startsAt" type="datetime-local" required>
                </div>
                <div>
                    <label for="periodEndsAt">Kết thúc</label>
                    <input id="periodEndsAt" name="endsAt" type="datetime-local" required>
                </div>
            </div>
            <p>Đợt áp dụng cho toàn công ty. Thời gian hiển thị theo múi giờ của trình duyệt. Các đợt chưa đóng không được trùng thời gian.</p>
            <div class="form-actions"><button type="submit" id="createProfilePeriodButton">Tạo đợt cập nhật</button></div>
        </form>
        <section class="panel">
            <h2>Các đợt cập nhật</h2>
            <table>
                <thead><tr><th>Tên đợt</th><th>Bắt đầu</th><th>Kết thúc</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
                <tbody id="profilePeriodRows"></tbody>
            </table>
        </section>
    </main>
</div>
<script src="../js/employee.js?v=<%= application.getResource("/js/employee.js").openConnection().getLastModified() %>"></script>
<script src="../js/profile-update-periods.js?v=<%= application.getResource("/js/profile-update-periods.js").openConnection().getLastModified() %>"></script>
</body>
</html>
