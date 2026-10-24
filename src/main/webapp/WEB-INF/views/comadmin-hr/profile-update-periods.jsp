<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đợt cập nhật ${updatePeriodLabel}</title>
    <link rel="stylesheet" href="../css/lifecycle.css">
    <link rel="stylesheet" href="../css/organization.css">
    <link rel="stylesheet" href="../css/profile-update.css">
</head>
<body class="lc" data-page="profile-update-periods" data-period-type="${updatePeriodType}">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content profile-update-page">
        <div class="lc-breadcrumb">Quản trị nhân sự &nbsp; / &nbsp; Cập nhật ${updatePeriodLabel}</div>
        <div class="page-header">
            <div>
                <h1 class="page-title">Đợt cập nhật ${updatePeriodLabel}</h1>
                <p class="page-subtitle">Quản lý thời gian cập nhật ${updatePeriodLabel} độc lập cho nhân viên trong công ty.</p>
            </div>
        </div>
        <div id="message" class="message" role="status"></div>
        <form id="profileUpdatePeriodForm" class="panel" novalidate>
            <input type="hidden" name="csrfToken">
            <div class="profile-section-heading"><span class="profile-section-icon">＋</span><div><h2>Tạo đợt cập nhật ${updatePeriodLabel}</h2><p>Thiết lập khoảng thời gian cho phép nhân viên cập nhật ${updatePeriodLabel}.</p></div></div>
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
            <div class="profile-section-heading"><div><h2>Lịch sử các đợt cập nhật</h2><p>Theo dõi trạng thái và quản lý các đợt của công ty.</p></div></div>
            <div class="lc-table-wrap"><table data-pagination="${updatePeriodType}-update-periods">
                <thead><tr><th>Tên đợt</th><th>Bắt đầu</th><th>Kết thúc</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
                <tbody id="profilePeriodRows"></tbody>
            </table></div>
        </section>
    </main>
<script src="../js/employee.js?v=<%= application.getResource("/js/employee.js").openConnection().getLastModified() %>"></script>
<script src="../js/profile-update-periods.js?v=<%= application.getResource("/js/profile-update-periods.js").openConnection().getLastModified() %>"></script>
</body>
</html>
