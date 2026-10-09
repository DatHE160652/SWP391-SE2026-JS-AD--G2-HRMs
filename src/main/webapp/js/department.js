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
            body: new URLSearchParams(data)
        });
        return handleDepartmentResponse(response);
    }
};

document.addEventListener('DOMContentLoaded', () => {
    const page = document.body.dataset.page;
    if (page === 'department-list') {
        loadDepartments();
    }
    if (page === 'department-form') {
        setupDepartmentForm();
    }
});

async function loadDepartments() {
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
        document.querySelector('#departmentFormTitle').textContent = 'Update Department';
        const payload = await DepartmentApi.get(`/departments/detail?id=${departmentId}`);
        fillDepartmentForm(form, payload.data);
        await setupDepartmentMembersForm(departmentId);
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

async function setupDepartmentMembersForm(departmentId) {
    const panel = document.querySelector('#departmentMembersPanel');
    const form = document.querySelector('#departmentMembersForm');
    if (!panel || !form) {
        return;
    }
    panel.classList.remove('hidden');
    form.departmentId.value = departmentId;

    const payload = await DepartmentApi.get(`/departments/members?departmentId=${departmentId}`);
    renderDepartmentMemberOptions(form, payload.data || []);

    if (form.dataset.bound !== 'true') {
        form.dataset.bound = 'true';
        form.addEventListener('submit', async event => {
            event.preventDefault();
            await saveDepartmentMembers(form);
        });
    }
}

function renderDepartmentMemberOptions(form, employees) {
    const managerSelect = form.managerId;
    const departmentId = String(form.departmentId.value);
    const managers = employees.filter(employee =>
        String(employee.departmentId || '') === departmentId
        && String(employee.roleCode || '').toUpperCase() === 'MANAGER'
    );
    managerSelect.innerHTML = '<option value="">Select Department Manager</option>' + managers.map(employee => `
        <option value="${employee.employeeId}">${safeDepartmentValue(employee.employeeCode)} - ${safeDepartmentValue(employee.fullName)}</option>
    `).join('');

    const currentManager = employees.find(employee =>
        employee.roleCode === 'MANAGER' && employee.selected && (!employee.managerId || employee.managerId === employee.employeeId));
    if (currentManager) {
        managerSelect.value = currentManager.employeeId;
    }

    const picker = document.querySelector('#departmentEmployeeOptions');
    if (!employees.length) {
        picker.innerHTML = '<p class="page-subtitle">No employees found.</p>';
        return;
    }

    picker.innerHTML = employees.map(employee => {
        const belongsOtherDepartment = employee.departmentId && String(employee.departmentId) !== departmentId;
        const disabled = belongsOtherDepartment ? 'disabled' : '';
        const checked = employee.selected && !belongsOtherDepartment ? 'checked' : '';
        const note = belongsOtherDepartment ? ` | Đang thuộc phòng #${employee.departmentId}` : '';
        return `
        <label class="member-option">
            <input type="checkbox" name="employeeIds" value="${employee.employeeId}" ${checked} ${disabled}>
            <span>
                <strong>${safeDepartmentValue(employee.employeeCode)} - ${safeDepartmentValue(employee.fullName)}</strong>
                <span>${safeDepartmentValue(employee.positionTitle)} | Role: ${safeDepartmentValue(employee.roleCode)}${note}</span>
            </span>
        </label>
    `;
    }).join('');

    managerSelect.addEventListener('change', () => {
        picker.querySelectorAll('input[name="employeeIds"]').forEach(input => {
            if (input.value === managerSelect.value) {
                input.checked = false;
                input.disabled = true;
            } else {
                input.disabled = false;
            }
        });
    });
    managerSelect.dispatchEvent(new Event('change'));
}

async function saveDepartmentMembers(form) {
    const employeeIds = Array.from(form.querySelectorAll('input[name="employeeIds"]:checked'))
        .map(input => input.value)
        .join(',');

    try {
        const payload = await DepartmentApi.post('/departments/assign-members', {
            departmentId: form.departmentId.value,
            managerId: form.managerId.value,
            employeeIds
        });
        showDepartmentMessage(payload.message);
        await setupDepartmentMembersForm(form.departmentId.value);
    } catch (error) {
        showDepartmentMessage(error.message, 'error');
    }
}

async function createDepartment(form) {
    try {
        const payload = await DepartmentApi.post('/departments/create', readDepartmentForm(form));
        showDepartmentMessage(payload.message);
        form.reset();
    } catch (error) {
        showDepartmentMessage(error.message, 'error');
    }
}

async function updateDepartment(form) {
    try {
        const payload = await DepartmentApi.post('/departments/update', readDepartmentForm(form));
        showDepartmentMessage(payload.message);
    } catch (error) {
        showDepartmentMessage(error.message, 'error');
    }
}

async function deleteDepartment(departmentId) {
    if (!confirm('Delete this department?')) {
        return;
    }

    try {
        const payload = await DepartmentApi.post('/departments/delete', { departmentId });
        showDepartmentMessage(payload.message);
        await loadDepartments();
    } catch (error) {
        showDepartmentMessage(error.message, 'error');
    }
}

function renderDepartmentRows(departments) {
    const tbody = document.querySelector('#departmentRows');
    if (!departments.length) {
        tbody.innerHTML = '<tr><td colspan="4">No departments found.</td></tr>';
        return;
    }

    tbody.innerHTML = departments.map(department => `
        <tr>
            <td><strong>${safeDepartmentValue(department.departmentCode)}</strong></td>
            <td>${safeDepartmentValue(department.departmentName)}</td>
            <td>${safeDepartmentValue(department.createdAt)}</td>
            <td class="actions">
                <a href="department-form?id=${department.departmentId}">Edit</a>
                <button class="danger" onclick="deleteDepartment(${department.departmentId})">Delete</button>
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
        throw new Error(payload.message || 'Request failed.');
    }
    return payload;
}

function showDepartmentMessage(message, type = 'success') {
    const element = document.querySelector('#message');
    element.textContent = message;
    element.className = `message show ${type}`;
}

function safeDepartmentValue(value) {
    return value === null || value === undefined || value === '' ? '-' : String(value);
}
