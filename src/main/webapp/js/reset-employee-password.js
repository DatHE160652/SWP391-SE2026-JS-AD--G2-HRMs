(() => {
    'use strict';
    const text = (vi, en) => document.documentElement.lang === 'en' ? en : vi;
    const form = document.querySelector('#reset-search-form');
    const fields = document.querySelector('#reset-search-fields');
    const results = document.querySelector('#reset-results');
    const notice = document.querySelector('#reset-notice');
    const dialog = document.querySelector('#reset-confirm');
    const submit = document.querySelector('#reset-submit');
    const cancel = document.querySelector('#reset-cancel');
    const context = document.body.dataset.context || '';
    const endpoint = `${context}/comadmin-hr/reset-employee-password`;
    let accounts = [];
    let selected = null;
    let busy = false;

    function notify(message, kind = 'info') {
        notice.textContent = message;
        notice.dataset.kind = kind;
        notice.hidden = !message;
    }

    function setBusy(value) {
        busy = value;
        fields.disabled = value;
        submit.disabled = value;
        cancel.disabled = value;
        results.setAttribute('aria-busy', String(value));
        results.querySelectorAll('button').forEach(button => {
            button.disabled = value || !accounts[Number(button.dataset.account)]?.canReset;
        });
    }

    async function post(action, values) {
        const body = new URLSearchParams(values);
        body.set('csrfToken', document.querySelector('meta[name="csrf-token"]').content);
        body.set('resetContextToken', document.querySelector('meta[name="reset-context-token"]').content);
        const response = await fetch(`${endpoint}/${action}`, {
            method: 'POST', headers: {Accept: 'application/json'}, body, cache: 'no-store'
        });
        const payload = await response.json().catch(() => null);
        if (!payload || response.redirected) {
            throw new Error(text('Phiên làm việc đã hết hạn hoặc chưa nhận được kết quả. Vui lòng đăng nhập lại và kiểm tra trước khi reset tiếp.', 'Session expired or no result was received. Sign in again and verify before resetting again.'));
        }
        if (!response.ok || payload.success !== true) {
            throw new Error(payload.message || text('Không thực hiện được yêu cầu.', 'The request could not be completed.'));
        }
        return payload.data;
    }

    function field(label, value) {
        const group = document.createElement('div');
        const term = document.createElement('dt');
        const description = document.createElement('dd');
        term.textContent = label;
        description.textContent = value == null || value === '' ? '—' : String(value);
        group.append(term, description);
        return group;
    }

    function render() {
        results.replaceChildren();
        if (!accounts.length) {
            const empty = document.createElement('p');
            empty.className = 'lc-empty';
            empty.textContent = text('Không tìm thấy nhân viên phù hợp trong công ty.', 'No matching employee was found in your company.');
            results.append(empty);
            return;
        }
        accounts.forEach((account, index) => {
            const card = document.createElement('article');
            card.className = 'lc-card reset-result';
            const heading = document.createElement('h2');
            heading.textContent = account.fullName;
            const details = document.createElement('dl');
            details.append(
                field(text('Mã nhân viên', 'Employee code'), account.employeeCode),
                field(text('Email đăng nhập', 'Login email'), account.email),
                field('CCCD/CMND', account.identityNumber),
                field(text('Số điện thoại', 'Phone'), account.phone),
                field(text('Vai trò', 'Role'), account.roleCode),
                field(text('Trạng thái tài khoản', 'Account status'), !account.userId
                    ? text('Chưa có tài khoản', 'No account') : account.isLocked
                    ? text('Bị khóa', 'Locked') : account.isActive
                    ? text('Đang hoạt động', 'Active') : text('Ngừng hoạt động', 'Inactive'))
            );
            const actions = document.createElement('div');
            actions.className = 'lc-actions';
            const button = document.createElement('button');
            button.type = 'button';
            button.dataset.account = String(index);
            button.textContent = text('Đặt lại mật khẩu và gửi email', 'Reset and send email');
            button.disabled = busy || !account.canReset;
            button.addEventListener('click', () => {
                if (busy || !account.canReset) return;
                selected = account;
                document.querySelector('#reset-confirm-person').textContent = `${account.fullName} · ${account.employeeCode} · ${account.email}`;
                dialog.showModal();
                cancel.focus();
            });
            actions.append(button);
            const hint = document.createElement('p');
            hint.className = 'lc-form-hint';
            hint.textContent = account.unavailableReason || (account.isLocked
                ? text('Tài khoản đang khóa. Reset không tự mở khóa.', 'This account is locked. Resetting does not unlock it.')
                : account.mustChangePassword ? text('Nhân viên đang được yêu cầu đổi mật khẩu.', 'This employee must change their password.') : '');
            actions.append(hint);
            card.append(heading, details, actions);
            results.append(card);
        });
    }

    form.addEventListener('input', () => {
        if (busy) return;
        accounts = [];
        results.replaceChildren();
        notify('');
    });

    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (busy) return;
        const values = new FormData(form);
        if (!String(values.get('email') || '').trim() && !String(values.get('identityNumber') || '').trim()) {
            notify(text('Vui lòng nhập email hoặc CCCD/CMND.', 'Enter an email or identity number.'), 'error');
            return;
        }
        accounts = [];
        results.replaceChildren();
        setBusy(true);
        notify(text('Đang tìm kiếm…', 'Searching…'));
        try {
            accounts = await post('search', values);
            render();
            notify('');
        } catch (error) {
            notify(error.message, 'error');
        } finally {
            setBusy(false);
        }
    });

    cancel.addEventListener('click', () => { if (!busy) dialog.close(); });
    dialog.addEventListener('cancel', event => { if (busy) event.preventDefault(); });
    dialog.addEventListener('close', () => { selected = null; });
    document.querySelector('#reset-confirm-form').addEventListener('submit', async event => {
        event.preventDefault();
        if (busy || !selected) return;
        const target = selected;
        setBusy(true);
        submit.textContent = text('Đang xử lý…', 'Processing…');
        try {
            const result = await post('reset', {employeeId: target.employeeId, version: target.version});
            notify(`${result.message}\n${text('Tìm kiếm lại nếu cần kiểm tra tài khoản hoặc thực hiện một lần reset mới.', 'Search again to check the account or perform a new reset.')}`, result.deliveryStatus === 'ACCEPTED' && result.emailMetadataRecorded !== false ? 'info' : 'warning');
        } catch (error) {
            notify(`${error.message}\n${text('Nếu kết nối bị gián đoạn, mật khẩu có thể đã được đặt lại. Hãy kiểm tra email và tìm kiếm lại trước khi xác nhận reset mới.', 'If the connection was interrupted, the password may already have been reset. Check email and search again before confirming another reset.')}`, 'error');
        } finally {
            // A submitted version must never be reused, even when delivery or the connection fails.
            accounts = [];
            results.replaceChildren();
            setBusy(false);
            dialog.close();
            submit.textContent = text('Đặt lại mật khẩu và gửi email', 'Reset and send email');
            notice.focus();
        }
    });
})();
