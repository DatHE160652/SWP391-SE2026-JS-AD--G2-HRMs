<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
<%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/form-validation.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${updatePeriodHeading}"/></title>
    <link rel="stylesheet" href="../css/lifecycle.css">
    <link rel="stylesheet" href="../css/organization.css">
    <link rel="stylesheet" href="../css/profile-update.css">
</head>
<body class="lc" data-page="profile-update-periods" data-period-type="${updatePeriodType}">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
    <main class="lc-main content profile-update-page">
        <div class="lc-breadcrumb"><c:out value="${updatePeriodBreadcrumb}"/></div>
        <div class="page-header">
            <div>
                <h1 class="page-title"><c:out value="${updatePeriodHeading}"/></h1>
                <p class="page-subtitle"><c:out value="${updatePeriodSubtitle}"/></p>
            </div>
        </div>
        <div id="message" class="message" role="status"></div>
        <form id="profileUpdatePeriodForm" class="panel" novalidate>
            <input type="hidden" name="csrfToken">
            <div class="profile-section-heading"><span class="profile-section-icon">＋</span><div><h2><c:out value="${updatePeriodCreateHeading}"/></h2><p><c:out value="${updatePeriodCreateDescription}"/></p></div></div>
            <div class="form-grid">
                <div>
                    <label for="periodTitle"><%= com.hrms.i18n.Language.text(request, "ui_f46ca070b6f6", "Tên đợt cập nhật") %></label>
                    <input id="periodTitle" name="title" maxlength="150" required>
                </div>
                <div>
                    <label for="periodStartsAt"><%= com.hrms.i18n.Language.text(request, "ui_95c3f9f5965f", "Bắt đầu") %></label>
                    <input id="periodStartsAt" name="startsAt" type="datetime-local" required>
                </div>
                <div>
                    <label for="periodEndsAt"><%= com.hrms.i18n.Language.text(request, "ui_9c1ae4f57bf2", "Kết thúc") %></label>
                    <input id="periodEndsAt" name="endsAt" type="datetime-local" required>
                </div>
            </div>
            <p><%= com.hrms.i18n.Language.text(request, "ui_6543c203ab80", "Đợt áp dụng cho toàn công ty. Thời gian hiển thị theo múi giờ của trình duyệt. Các đợt chưa đóng không được trùng thời gian.") %></p>
            <div class="form-actions"><button type="submit" id="createProfilePeriodButton"><%= com.hrms.i18n.Language.text(request, "ui_3fbd9d29d460", "Tạo đợt cập nhật") %></button></div>
        </form>
        <section class="panel">
            <div class="profile-section-heading"><div><h2><%= com.hrms.i18n.Language.text(request, "ui_367ca500e1f4", "Lịch sử các đợt cập nhật") %></h2><p><%= com.hrms.i18n.Language.text(request, "ui_88884717af34", "Theo dõi trạng thái và quản lý các đợt của công ty.") %></p></div></div>
            <div class="lc-table-wrap"><table data-pagination="${updatePeriodType}-update-periods">
                <thead><tr><th><%= com.hrms.i18n.Language.text(request, "ui_3e5782a708ab", "Tên đợt") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_95c3f9f5965f", "Bắt đầu") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_9c1ae4f57bf2", "Kết thúc") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_e03c1401e314", "Trạng thái") %></th><th><%= com.hrms.i18n.Language.text(request, "ui_fed024b82c0a", "Thao tác") %></th></tr></thead>
                <tbody id="profilePeriodRows"></tbody>
            </table></div>
        </section>
    </main>


<script src="../js/employee.js?v=<%= application.getResource("/js/employee.js").openConnection().getLastModified() %>"></script>
<script src="../js/profile-update-periods.js?v=<%= application.getResource("/js/profile-update-periods.js").openConnection().getLastModified() %>"></script>
</body>
</html>
