document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-password-toggle]').forEach(function (button) {
        button.addEventListener('click', function () {
            const input = document.getElementById(button.dataset.passwordToggle);
            if (!input) return;
            const show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            button.setAttribute('aria-pressed', String(show));
            button.setAttribute('aria-label', show ? HrmI18n.t('Ẩn mật khẩu') : HrmI18n.t('Hiện mật khẩu'));
        });
    });

    document.querySelectorAll('[data-dialog-open]').forEach(function (button) {
        button.addEventListener('click', function () {
            const dialog = document.getElementById(button.dataset.dialogOpen);
            if (!dialog) return;
            dialog.showModal();
        });
    });
    document.querySelectorAll('[data-dialog-close]').forEach(function (button) {
        button.addEventListener('click', function () { button.closest('dialog').close(); });
    });
    document.querySelectorAll('.action-dialog form').forEach(function (form) {
        form.addEventListener('submit', function () {
            form.querySelectorAll('button').forEach(function (button) { button.disabled = true; });
        });
    });

    const toggle = document.querySelector('[data-menu-toggle]');
    const sidebar = document.getElementById('admin-navigation');
    const backdrop = document.querySelector('.sidebar-backdrop');
    if (!toggle || !sidebar || !backdrop) return;
    const mobile = window.matchMedia('(max-width: 1024px)');
    function closeDrawer() {
        document.body.classList.remove('sidebar-open');
        backdrop.hidden = true;
        sidebar.inert = mobile.matches;
        toggle.setAttribute('aria-expanded', String(!mobile.matches && !document.body.classList.contains('sidebar-collapsed')));
        toggle.setAttribute('aria-label', mobile.matches ? HrmI18n.t('Mở menu') : HrmI18n.t('Thu gọn / mở rộng menu'));
    }
    closeDrawer();
    toggle.addEventListener('click', function () {
        if (!mobile.matches) {
            document.body.classList.toggle('sidebar-collapsed');
            closeDrawer();
            return;
        }
        if (document.body.classList.contains('sidebar-open')) {
            closeDrawer();
        } else {
            document.body.classList.add('sidebar-open');
            sidebar.inert = false;
            backdrop.hidden = false;
            toggle.setAttribute('aria-expanded', 'true');
            toggle.setAttribute('aria-label', HrmI18n.t('Đóng menu'));
            sidebar.querySelector('a').focus();
        }
    });
    backdrop.addEventListener('click', function () { closeDrawer(); toggle.focus(); });
    document.addEventListener('keydown', function (event) {
        if (!document.body.classList.contains('sidebar-open')) return;
        if (event.key === 'Escape') { closeDrawer(); toggle.focus(); }
        if (event.key === 'Tab') {
            const links = sidebar.querySelectorAll('a, button:not(:disabled)');
            const first = links[0], last = links[links.length - 1];
            if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
            if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
        }
    });
    mobile.addEventListener('change', closeDrawer);
});
