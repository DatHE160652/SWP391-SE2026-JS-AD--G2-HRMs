const ResponsibilityLevelApi = {
    async request(url, data) {
        const first = location.pathname.split('/').filter(Boolean)[0] || '';
        const contextPath = first === 'responsibility-level' ? '' : '/' + first;
        const options = {headers: {Accept: 'application/json'}};
        if (data) {
            options.method = 'POST';
            options.headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
            options.body = new URLSearchParams({...data, csrfToken: document.querySelector('meta[name="organization-csrf-token"]').content});
        }
        const response = await fetch(contextPath + url, options);
        const payload = await response.json().then(HrmI18n.response);
        if (!response.ok || payload.success === false) throw FormValidation.responseError(payload);
        return payload;
    }
};

document.addEventListener('DOMContentLoaded', () => {
    document.querySelector('#statusFilter')?.addEventListener('change', loadResponsibilityLevels);
    const page = document.body.dataset.page;
    if (page === 'responsibility-level-list') {
        const message = sessionStorage.getItem('responsibilityLevelMessage');
        if (message) { showLevelMessage(message); sessionStorage.removeItem('responsibilityLevelMessage'); }
        loadResponsibilityLevels().catch(error => showLevelMessage(error.message, 'error'));
    }
    if (page === 'responsibility-level-form') setupResponsibilityLevelForm().catch(error => showLevelMessage(error.message, 'error'));
});

async function loadResponsibilityLevels() {
    const payload = await ResponsibilityLevelApi.request('/responsibility-levels');
    renderResponsibilityLevelRows(payload.data);
    document.querySelector('#totalLevels').textContent = String(payload.data.length);
}

function renderResponsibilityLevelRows(levels) {
    const status = document.querySelector('#statusFilter')?.value || '';
    levels = levels.filter(item => !status || (item.isActive) === (status === 'Active'));
    const rows = document.querySelector('#levelRows');
    rows.replaceChildren();
    if (!levels.length) {
        const cell = rows.insertRow().insertCell();
        cell.colSpan = 6;
        cell.textContent = HrmI18n.t('Chưa có cấp trách nhiệm.');
    }
    levels.forEach(level => {
        const row = rows.insertRow();
        [level.levelCode, level.levelName, String(level.rankOrder), level.defaultAllowance == null ? HrmI18n.t('Chưa cấu hình') : Number(level.defaultAllowance).toLocaleString(HrmI18n.locale) + ' ₫', level.isActive ? HrmI18n.t('Đang hoạt động') : HrmI18n.t('Ngừng hoạt động')]
            .forEach(value => { row.insertCell().textContent = value; });
        const actions = row.insertCell();
        actions.className = 'actions';
        const view = document.createElement('a');
        view.href = 'responsibility-level-detail?id=' + encodeURIComponent(level.responsibilityLevelId);
        view.textContent = HrmI18n.t('Chi tiết');
        actions.append(view);
        const edit = document.createElement('a');
        edit.href = 'responsibility-level-form?id=' + encodeURIComponent(level.responsibilityLevelId);
        edit.textContent = HrmI18n.t('Sửa');
        actions.append(edit);
        const positions=document.createElement('a');
        positions.href='../position/position-list?responsibilityLevelId='+encodeURIComponent(level.responsibilityLevelId);
        positions.textContent=HrmI18n.t('Xem chức danh');
        actions.append(positions);
        {

            const deactivate = document.createElement('button');
            deactivate.type = 'button'; deactivate.className = 'danger'; deactivate.textContent = level.isActive ? HrmI18n.t('Vô hiệu hóa') : HrmI18n.t('Kích hoạt lại');
            deactivate.addEventListener('click', () => setResponsibilityLevelActive(level.responsibilityLevelId, !level.isActive));
            actions.append(deactivate);
        }
    });
}

async function setupResponsibilityLevelForm() {
    const form = document.querySelector('#levelForm');
    const id = new URLSearchParams(location.search).get('id');
    if (id) {
        const payload = await ResponsibilityLevelApi.request('/responsibility-levels/detail?id=' + encodeURIComponent(id));
        Object.entries(payload.data).forEach(([key, value]) => {
            const control = form.elements.namedItem(key);
            if (control) control.value = value === null ? (key === 'defaultAllowance' ? '0' : '') : String(value);
        });
        document.querySelector('#levelFormTitle').textContent = HrmI18n.t('Cập nhật cấp trách nhiệm');
        document.querySelectorAll('a[href="responsibility-level-list"]').forEach(link => {
            link.href = 'responsibility-level-detail?id=' + encodeURIComponent(id);
        });
    }
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (!FormValidation.validate(form)) return;
        const button = form.querySelector('button[type="submit"]');
        button.disabled = true;
        try {
            const payload = await ResponsibilityLevelApi.request(id ? '/responsibility-levels/update' : '/responsibility-levels/create',
                Object.fromEntries(new FormData(form).entries()));
            sessionStorage.setItem('responsibilityLevelMessage', payload.message);
            location.assign(id ? 'responsibility-level-detail?id=' + encodeURIComponent(id) : 'responsibility-level-list');
        } catch (error) {
            if (!FormValidation.showErrors(form, error)) showLevelMessage(error.message, 'error');
        } finally { button.disabled = false; }
    });
}

async function setResponsibilityLevelActive(id, active) {
    if (!(await AppDialog.confirm(active ? HrmI18n.t('Kích hoạt lại bản ghi này?') : HrmI18n.t('Vô hiệu hóa bản ghi này?')))) return;
    try {
        const payload = await ResponsibilityLevelApi.request(active ? '/responsibility-levels/activate' : '/responsibility-levels/deactivate', {responsibilityLevelId: id});
        showLevelMessage(payload.message);
        await loadResponsibilityLevels();
    } catch (error) { showLevelMessage(error.message, 'error'); }
}

function showLevelMessage(message, type = 'success') {
    const element = document.querySelector('#message');
    element.textContent = HrmI18n.t(message);
    element.className = 'message show ' + type;
}
