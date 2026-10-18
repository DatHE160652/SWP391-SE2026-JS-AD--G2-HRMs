document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('#profileUpdatePeriodForm');
    setDefaultProfilePeriodDates(form);
    loadProfileUpdatePeriods();
    document.querySelector('#refreshPeriodsButton').addEventListener('click', loadProfileUpdatePeriods);
    form.addEventListener('submit', async event => {
        event.preventDefault();
        const button = document.querySelector('#createProfilePeriodButton');
        button.disabled = true;
        try {
            const data = Object.fromEntries(new FormData(form).entries());
            data.startsAt = new Date(data.startsAt).toISOString();
            data.endsAt = new Date(data.endsAt).toISOString();
            const payload = await EmployeeApi.post('/employees/profile-update-periods/create', data);
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
        const payload = await EmployeeApi.get('/employees/profile-update-periods');
        document.querySelector('#profileUpdatePeriodForm').elements.namedItem('csrfToken').value = payload.csrfToken;
        const rows = document.querySelector('#profilePeriodRows');
        rows.replaceChildren();
        if (!payload.data.length) {
            const row = rows.insertRow();
            const cell = row.insertCell();
            cell.colSpan = 5;
            cell.textContent = 'Chưa có đợt cập nhật hồ sơ.';
        }
        payload.data.forEach(period => {
            const row = rows.insertRow();
            const expired = Date.parse(period.endsAt) <= Date.now();
            const state = period.closed ? 'Đã đóng' : expired ? 'Đã kết thúc'
                : Date.parse(period.startsAt) <= Date.now() ? 'Đang mở' : 'Chưa bắt đầu';
            [period.title, new Date(period.startsAt).toLocaleString('vi-VN'),
                new Date(period.endsAt).toLocaleString('vi-VN'), state].forEach(value => {
                row.insertCell().textContent = value;
            });
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'danger';
            button.textContent = 'Đóng đợt';
            button.disabled = period.closed || expired;
            button.addEventListener('click', () => closeProfileUpdatePeriod(period.periodId, button));
            row.insertCell().appendChild(button);
        });
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

async function closeProfileUpdatePeriod(periodId, button) {
    if (!confirm('Đóng đợt cập nhật này? Employee và Manager sẽ không thể cập nhật số điện thoại trong đợt này nữa.')) {
        return;
    }
    button.disabled = true;
    try {
        const form = document.querySelector('#profileUpdatePeriodForm');
        const payload = await EmployeeApi.post('/employees/profile-update-periods/close', {
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
