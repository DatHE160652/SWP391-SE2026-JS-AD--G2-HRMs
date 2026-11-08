const updatePeriodApi = document.body.dataset.periodType === 'DOCUMENT' ? '/employees/document-update-periods' : '/employees/profile-update-periods';
const documentUpdatePeriods = document.body.dataset.periodType === 'DOCUMENT';
document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('#profileUpdatePeriodForm');
    setDefaultProfilePeriodDates(form);
    loadProfileUpdatePeriods();
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (!FormValidation.validate(form)) return;
        const start = new Date(form.elements.namedItem('startsAt').value);
        const end = new Date(form.elements.namedItem('endsAt').value);
        if (!Number.isFinite(start.getTime()) || !Number.isFinite(end.getTime()) || end <= start) {
            FormValidation.setError(form, 'endsAt', HrmI18n.t('Thời gian kết thúc phải sau thời gian bắt đầu.'));
            form.elements.namedItem('endsAt').focus();
            return;
        }
        const button = document.querySelector('#createProfilePeriodButton');
        button.disabled = true;
        try {
            const data = Object.fromEntries(new FormData(form).entries());
            data.startsAt = new Date(data.startsAt).toISOString();
            data.endsAt = new Date(data.endsAt).toISOString();
            const payload = await EmployeeApi.post(updatePeriodApi+'/create', data);
            showMessage(payload.message);
            form.reset();
            setDefaultProfilePeriodDates(form);
            await loadProfileUpdatePeriods();
        } catch (error) {
            if (!FormValidation.showErrors(form, error)) {
                showMessage(error.message, 'error');
            }
        } finally {
            button.disabled = false;
        }
    });
});

function setDefaultProfilePeriodDates(form) {
    const localInput = date => new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
    form.elements.namedItem('startsAt').value = localInput(new Date());
    form.elements.namedItem('endsAt').value = localInput(new Date(Date.now() + 24 * 60 * 60 * 1000));
}

async function loadProfileUpdatePeriods() {
    try {
        const payload = await EmployeeApi.get(updatePeriodApi);
        document.querySelector('#profileUpdatePeriodForm').elements.namedItem('csrfToken').value = payload.csrfToken;
        const rows = document.querySelector('#profilePeriodRows');
        rows.replaceChildren();
        if (!payload.data.length) {
            const row = rows.insertRow();
            const cell = row.insertCell();
            cell.colSpan = 5;
            cell.textContent = HrmI18n.t(documentUpdatePeriods ? 'Chưa có đợt cập nhật tài liệu.' : 'Chưa có đợt cập nhật hồ sơ.');
        }
        payload.data.forEach(period => {
            const row = rows.insertRow();
            const expired = Date.parse(period.endsAt) <= Date.now();
            const stateCode = period.closed ? 'closed' : expired ? 'ended'
                : Date.parse(period.startsAt) <= Date.now() ? 'open' : 'scheduled';
            const state = {closed:HrmI18n.t('Đã đóng'),ended:HrmI18n.t('Đã kết thúc'),
                open:HrmI18n.t('Đang mở'),scheduled:HrmI18n.t('Chưa bắt đầu')}[stateCode];
            [period.title, new Date(period.startsAt).toLocaleString(HrmI18n.locale),
                new Date(period.endsAt).toLocaleString(HrmI18n.locale), state].forEach(value => {
                row.insertCell().textContent = value;
            });
            const statusCell = row.cells[3];
            const badge = document.createElement('span');
            badge.className = 'lc-badge' + (stateCode === 'open' ? ' active' : stateCode === 'scheduled' ? ' warning' : '');
            badge.textContent = state;
            statusCell.replaceChildren(badge);
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'danger';
            button.textContent = HrmI18n.t('Đóng đợt');
            button.disabled = period.closed || expired;
            button.addEventListener('click', () => closeProfileUpdatePeriod(period.periodId, button));
            row.insertCell().appendChild(button);
        });
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

async function closeProfileUpdatePeriod(periodId, button) {
    const confirmation = HrmI18n.t(documentUpdatePeriods
        ? 'Đóng đợt cập nhật tài liệu này? Chỉ quyền cập nhật tài liệu của đợt này sẽ bị khóa.'
        : 'Đóng đợt cập nhật hồ sơ này? Chỉ quyền cập nhật hồ sơ của đợt này sẽ bị khóa.');
    if (!(await AppDialog.confirm(confirmation))) {
        return;
    }
    button.disabled = true;
    try {
        const form = document.querySelector('#profileUpdatePeriodForm');
        const payload = await EmployeeApi.post(updatePeriodApi+'/close', {
            periodId,
            csrfToken: form.elements.namedItem('csrfToken').value
        });
        showMessage(payload.message);
        await loadProfileUpdatePeriods();
    } catch (error) {
        showMessage(error.message, 'error');
        button.disabled = false;
    }
}
