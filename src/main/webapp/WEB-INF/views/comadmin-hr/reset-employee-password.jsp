<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="resetEnglish" value='<%= "en".equals(com.hrms.i18n.Language.selected(request)) %>'/>
<!DOCTYPE html>
<html lang="<%= com.hrms.i18n.Language.selected(request) %>">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="<c:out value='${organizationCsrfToken}'/>">
    <meta name="reset-context-token" content="<c:out value='${resetContextToken}'/>">
    <%@ include file="/WEB-INF/views/common/language.jspf" %>
    <%@ include file="/WEB-INF/views/common/workspace-theme.jspf" %>
    <title>${resetEnglish ? 'Password reset' : 'Đặt lại mật khẩu'} · HRMs Cloud</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/reset-employee-password.css?v=<%= application.getResource("/css/reset-employee-password.css").openConnection().getLastModified() %>">
</head>
<body class="lc" data-context="${pageContext.request.contextPath}">
<%@ include file="/WEB-INF/views/common/lifecycle-shell.jspf" %>
<main class="lc-main reset-page">
    <div class="lc-breadcrumb">${resetEnglish ? 'HR administration / Password reset' : 'Quản trị nhân sự / Đặt lại mật khẩu'}</div>
    <div class="lc-heading"><div>
        <h1>${resetEnglish ? 'Password reset' : 'Đặt lại mật khẩu'}</h1>
        <p>${resetEnglish ? 'Find an Employee or Manager and email a new temporary password.' : 'Tìm Employee hoặc Manager và gửi mật khẩu tạm mới qua email.'}</p>
    </div></div>
    <section class="lc-card reset-search" aria-labelledby="reset-search-heading">
        <h2 id="reset-search-heading">${resetEnglish ? 'Find employee' : 'Tìm nhân viên'}</h2>
        <p class="lc-muted" id="reset-search-help">${resetEnglish ? 'Enter the exact login email or identity number. If both are entered, both must match.' : 'Nhập chính xác email đăng nhập hoặc CCCD/CMND.'}</p>
        <form id="reset-search-form" autocomplete="off" aria-describedby="reset-search-help">
            <fieldset id="reset-search-fields" class="reset-filters">
                <label for="reset-email">${resetEnglish ? 'Login email' : 'Email đăng nhập'}
                    <input id="reset-email" name="email" type="email" maxlength="150" placeholder="name@company.com">
                </label>
                <label for="reset-identity">${resetEnglish ? 'Identity number' : 'CCCD/CMND'}
                    <input id="reset-identity" name="identityNumber" type="text" inputmode="numeric" pattern="[0-9]+" maxlength="50">
                </label>
                <button type="submit" id="reset-search-button">${resetEnglish ? 'Search' : 'Tìm kiếm'}</button>
            </fieldset>
        </form>
    </section>
    <div id="reset-notice" class="reset-notice" role="status" aria-live="polite" tabindex="-1" hidden></div>
    <section id="reset-results" aria-label="${resetEnglish ? 'Search results' : 'Kết quả tìm kiếm'}" aria-busy="false">
        <p class="lc-empty">${resetEnglish ? 'Enter search information to find an employee in your company.' : 'Nhập thông tin tìm kiếm để tìm nhân viên trong công ty của bạn.'}</p>
    </section>
</main>
<dialog id="reset-confirm" class="lc-modal" aria-labelledby="reset-confirm-title" aria-describedby="reset-confirm-warning">
    <form id="reset-confirm-form">
        <div class="lc-modal-header"><h2 id="reset-confirm-title">${resetEnglish ? 'Confirm password reset' : 'Xác nhận đặt lại mật khẩu'}</h2></div>
        <div class="lc-form-section">
            <p id="reset-confirm-person"></p>
            <p id="reset-confirm-warning">${resetEnglish ? 'The current password will stop working. Existing sessions will be rejected on their next protected request. A new temporary password will be emailed to this account, and the employee must change it after signing in.' : 'Mật khẩu hiện tại sẽ mất hiệu lực. Phiên đăng nhập cũ sẽ bị chặn ở lần truy cập được bảo vệ tiếp theo. Mật khẩu tạm mới sẽ được gửi đến email tài khoản; nhân viên phải đổi mật khẩu sau khi đăng nhập.'}</p>
            <p class="lc-muted">${resetEnglish ? 'Resetting does not unlock an account. A later reset invalidates every earlier temporary password, even if emails arrive out of order.' : 'Đặt lại mật khẩu không mở khóa tài khoản. Nếu reset lại, mật khẩu tạm trước sẽ mất hiệu lực kể cả khi email đến không đúng thứ tự.'}</p>
        </div>
        <div class="lc-modal-footer">
            <button type="button" class="secondary" id="reset-cancel">${resetEnglish ? 'Cancel' : 'Hủy'}</button>
            <button type="submit" id="reset-submit">${resetEnglish ? 'Reset and send email' : 'Đặt lại mật khẩu và gửi email'}</button>
        </div>
    </form>
</dialog>
<script src="${pageContext.request.contextPath}/js/reset-employee-password.js?v=<%= application.getResource("/js/reset-employee-password.js").openConnection().getLastModified() %>" defer></script>
</body>
</html>
