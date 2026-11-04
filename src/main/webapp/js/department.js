const DepartmentApi = {
    async get(url) {
        const response = await fetch(departmentApiUrl(url), { headers: { Accept: 'application/json' } });
        return handleDepartmentResponse(response);
    },

    async post(url, data) {
        const response = await fetch(departmentApiUrl(url), {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            },
            body: new URLSearchParams({ ...data, csrfToken: data.csrfToken || document.querySelector('meta[name="organization-csrf-token"]')?.content || '' })
        });
        return handleDepartmentResponse(response);
    }
};

document.addEventListener('DOMContentLoaded', () => {
    document.querySelector('#statusFilter')?.addEventListener('change', loadDepartments);
    document.querySelector('#btnExportDepartments')?.addEventListener('click', (e) => {
        e.preventDefault();
        const status = document.querySelector('#statusFilter')?.value || '';
        window.location.href = departmentApiUrl(`/departments/export?status=${encodeURIComponent(status)}`);
    });
    const page = document.body.dataset.page;
    if (page === 'department-list') {
        loadDepartments();
    }
    if (page === 'department-form') {
        setupDepartmentForm().catch(error => showDepartmentMessage(error.message, 'error'));
    }
});

async function loadDepartments() {
    const message = sessionStorage.getItem('departmentMessage');
    if (message) { showDepartmentMessage(message); sessionStorage.removeItem('departmentMessage'); }
    try {
        const payload = await DepartmentApi.get('/departments');
        renderDepartmentRows(payload.data);
        document.querySelector('#totalDepartments').textContent = payload.data.length;
    } catch (error) {
        showDepartmentMessage(error.message, 'error');
    }
}

async function setupDepartmentForm() {
    const departmentId = new URLSearchParams(location.search).get('id');
    const form = document.querySelector('#departmentForm');

    if (departmentId) {
        document.querySelector('#departmentFormTitle').textContent = 'Cập nhật phòng ban';
        const payload = await DepartmentApi.get(`/departments/detail?id=${departmentId}`);
        fillDepartmentForm(form, payload.data);
        const assignButton = document.querySelector('#departmentInitialAssignment');
        if (assignButton && payload.data.isActive) { assignButton.hidden = false; assignButton.dataset.initialDepartment = departmentId; }
        const links = document.querySelector('#departmentPositionLinks');
        const view = document.createElement('a');
        view.href = '../position/position-list?departmentId=' + encodeURIComponent(departmentId);
        view.textContent = 'Xem chức danh';
        const create = document.createElement('a');
        create.href = '../position/position-form?departmentId=' + encodeURIComponent(departmentId);
        create.textContent = ' Thêm chức danh';
        links.replaceChildren(view, create);
        await loadDepartmentMembers(departmentId);
    }

    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (departmentId) {
            await updateDepartment(form);
        } else {
            await createDepartment(form);
        }
    });
}

async function loadDepartmentMembers(departmentId) {
    const panel = document.querySelector('#departmentMembersPanel');
    const list = document.querySelector('#departmentMemberList');
    if (!panel || !list) return;
    panel.classList.remove('hidden');
    list.innerHTML = '<div class="lc-spinner">Đang tải nhân viên…</div>';
    document.querySelector('#departmentMemberCount').textContent = '—';
    try {
        const payload = await DepartmentApi.get('/departments/members?departmentId=' + encodeURIComponent(departmentId));
        const employees = (payload.data || []).filter(employee => String(employee.departmentId) === String(departmentId));
        document.querySelector('#departmentMemberCount').textContent = String(employees.length);
        list.innerHTML = employees.length ? '<div class="lc-table-wrap"><table data-pagination="department-members"><thead><tr>'
            + '<th>Mã nhân viên</th><th>Họ tên</th><th>Chức danh</th><th>Quản lý trực tiếp</th></tr></thead><tbody>'
            + employees.map(employee => '<tr><td>' + safeDepartmentValue(employee.employeeCode) + '</td><td>'
                + safeDepartmentValue(employee.fullName) + '</td><td>' + safeDepartmentValue(employee.positionTitle || 'Chưa phân công')
                + '</td><td>' + safeDepartmentValue(employee.managerName || 'Chưa có quản lý') + '</td></tr>').join('')
            + '</tbody></table></div>' : '<div class="lc-empty">Phòng ban chưa có nhân viên.</div>';
    } catch (error) {
        document.querySelector('#departmentMemberCount').textContent = '—';
        list.textContent = error.message;
    }
}

async function createDepartment(form) {
    try {
        const payload = await DepartmentApi.post('/departments/create', readDepartmentForm(form));
        sessionStorage.setItem('departmentMessage', payload.message);
        location.assign('department-list');
    } catch (error) {
        if (!FormValidation.showErrors(form, error)) {
            showDepartmentMessage(error.message, 'error');
        }
    }
}

async function updateDepartment(form) {
    try {
        const payload = await DepartmentApi.post('/departments/update', readDepartmentForm(form));
        sessionStorage.setItem('departmentMessage', payload.message);
        location.assign('department-list');
    } catch (error) {
        if (!FormValidation.showErrors(form, error)) {
            showDepartmentMessage(error.message, 'error');
        }
    }
}

async function setDepartmentActive(departmentId, active) {
    if (!confirm(active ? 'Kích hoạt lại bản ghi này?' : 'Vô hiệu hóa bản ghi này?')) {
        return;
    }

    try {
        const payload = await DepartmentApi.post(active ? '/departments/activate' : '/departments/deactivate', { departmentId });
        showDepartmentMessage(payload.message);
        await loadDepartments();
    } catch (error) {
        showDepartmentMessage(error.message, 'error');
    }
}

function renderDepartmentRows(departments) {
    const status = document.querySelector('#statusFilter')?.value || '';
    departments = departments.filter(item => !status || (item.isActive) === (status === 'Active'));
    const tbody = document.querySelector('#departmentRows');
    if (!departments.length) {
        tbody.innerHTML = '<tr><td colspan="5">Chưa có phòng ban.</td></tr>';
        return;
    }

    tbody.innerHTML = departments.map(department => `
        <tr>
            <td><strong>${safeDepartmentValue(department.departmentCode)}</strong></td>
            <td>${safeDepartmentValue(department.departmentName)}</td>
            <td>${department.isActive ? 'Đang hoạt động' : 'Ngừng hoạt động'}</td>
            <td>${department.createdAt ? safeDepartmentValue(new Date(department.createdAt).toLocaleDateString('vi-VN')) : '-'}</td>
            <td class="actions">
                ${department.isActive ? `<button type="button" data-initial-department="${department.departmentId}">Phân công</button>` : ''}
                <a href="department-form?id=${department.departmentId}">Xem / Sửa</a>
                <a href="../position/position-list?departmentId=${department.departmentId}">Chức danh</a>
                <a href="../position/position-form?departmentId=${department.departmentId}">Thêm chức danh</a>
                <button class="danger" onclick="setDepartmentActive(${department.departmentId}, ${!department.isActive})">${department.isActive ? 'Vô hiệu hóa' : 'Kích hoạt lại'}</button>
            </td>
        </tr>
    `).join('');
}

function fillDepartmentForm(form, department) {
    Object.keys(department).forEach(key => {
        if (form[key] && department[key] !== null) {
            form[key].value = department[key];
        }
    });
}

function readDepartmentForm(form) {
    return Object.fromEntries(new FormData(form).entries());
}

function departmentApiUrl(url) {
    const first = location.pathname.split('/').filter(Boolean)[0] || '';
    const contextPath = first === 'department' ? '' : `/${first}`;
    return `${contextPath}${url}`;
}

async function handleDepartmentResponse(response) {
    const payload = await response.json();
    if (!response.ok || payload.success === false) {
        throw FormValidation.responseError(payload);
    }
    return payload;
}

function showDepartmentMessage(message, type = 'success') {
    const element = document.querySelector('#message');
    element.textContent = message;
    element.className = `message show ${type}`;
}

function safeDepartmentValue(value) {
    return value === null || value === undefined || value === '' ? '-' : String(value).replace(/[&<>"']/g, character => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]));
}
