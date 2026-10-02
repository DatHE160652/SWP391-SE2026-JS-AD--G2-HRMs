const EmployeeApi = {
    async get(url) {
        const response = await fetch(withContext(url), { headers: { Accept: 'application/json' } });
        return handleResponse(response);
    },

    async post(url, data) {
        const response = await fetch(withContext(url), {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            },
            body: new URLSearchParams(data)
        });
        return handleResponse(response);
    }
};

document.addEventListener('DOMContentLoaded', () => {
    const page = document.body.dataset.page;
    if (page === 'list') {
        loadEmployees();
    }
    if (page === 'detail') {
        getEmployeeDetail();
    }
    if (page === 'form') {
        setupEmployeeForm();
    }
});

async function loadEmployees() {
    try {
        const payload = await EmployeeApi.get('/employees');
        renderEmployeeRows(payload.data);
        renderSummary(payload.data);
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

async function getEmployeeDetail() {
    const employeeId = new URLSearchParams(location.search).get('id');
    if (!employeeId) {
        showMessage('Employee id is required.', 'error');
        return;
    }

    try {
        const payload = await EmployeeApi.get(`/employees/detail?id=${employeeId}`);
        renderEmployeeDetail(payload.data);
        await setupOrganizationForm(payload.data);
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

async function setupEmployeeForm() {
    const employeeId = new URLSearchParams(location.search).get('id');
    const form = document.querySelector('#employeeForm');

    if (employeeId) {
        document.querySelector('#formTitle').textContent = 'Update Employee';
        const payload = await EmployeeApi.get(`/employees/detail?id=${employeeId}`);
        fillForm(form, payload.data);
        form.employeeCode.readOnly = true;
    }

    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (employeeId) {
            await updateEmployee(form);
        } else {
            await createEmployee(form);
        }
    });
}

async function createEmployee(form) {
    try {
        const payload = await EmployeeApi.post('/employees/create', readForm(form));
        showMessage(payload.message);
        form.reset();
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

async function updateEmployee(form) {
    try {
        const payload = await EmployeeApi.post('/employees/update', readForm(form));
        showMessage(payload.message);
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

async function deleteEmployee(employeeId) {
    if (!confirm('Delete this employee record?')) {
        return;
    }

    try {
        const payload = await EmployeeApi.post('/employees/delete', { employeeId });
        showMessage(payload.message);
        await loadEmployees();
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

function renderEmployeeRows(employees) {
    const tbody = document.querySelector('#employeeRows');
    if (!employees.length) {
        tbody.innerHTML = '<tr><td colspan="8">No employees found.</td></tr>';
        return;
    }

    tbody.innerHTML = employees.map(employee => `
        <tr>
            <td><strong>${safe(employee.employeeCode)}</strong></td>
            <td>${safe(employee.fullName)}</td>
            <td>${safe(employee.phone)}</td>
            <td>${safe(employee.gender)}</td>
            <td>${safe(employee.startDate)}</td>
            <td>${formatMoney(employee.baseSalary)}</td>
            <td><span class="badge ${safe(employee.status)}">${safe(employee.status)}</span></td>
            <td class="actions">
                <a href="employee-detail.html?id=${employee.employeeId}">View</a>
                <a href="employee-form.html?id=${employee.employeeId}">Edit</a>
                <button class="danger" onclick="deleteEmployee(${employee.employeeId})">Delete</button>
            </td>
        </tr>
    `).join('');
}

function renderSummary(employees) {
    document.querySelector('#totalEmployees').textContent = employees.length;
    document.querySelector('#activeEmployees').textContent = employees
        .filter(employee => employee.status === 'Active').length;
    document.querySelector('#otherEmployees').textContent = employees
        .filter(employee => employee.status !== 'Active').length;
}

function renderEmployeeDetail(employee) {
    document.querySelector('#detailTitle').textContent = employee.fullName || 'Employee Profile';
    document.querySelector('#profileTab').innerHTML = `
        ${detailItem('Employee Code', employee.employeeCode)}
        ${detailItem('Full Name', employee.fullName)}
        ${detailItem('Phone', employee.phone)}
        ${detailItem('Gender', employee.gender)}
        ${detailItem('Date Of Birth', employee.dateOfBirth)}
        ${detailItem('Start Date', employee.startDate)}
        ${detailItem('Base Salary', formatMoney(employee.baseSalary))}
        ${detailItem('Status', employee.status)}
    `;

    document.querySelector('#employmentTab').innerHTML = `
        ${detailItem('Account Username', employee.username)}
        ${detailItem('Account Email', employee.accountEmail)}
        ${detailItem('Account Status', employee.accountActive === true ? 'Active' : employee.accountActive === false ? 'Inactive' : null)}
        ${detailItem('UserId', employee.userId)}
    `;
}

async function setupOrganizationForm(employee) {
    const form = document.querySelector('#organizationForm');
    if (!form) {
        return;
    }

    form.employeeId.value = employee.employeeId;
    document.querySelector('#organizationEmployeeName').value = employee.fullName || '';

    const [organizationPayload, departmentPayload, positionPayload, employeePayload] = await Promise.all([
        EmployeeApi.get(`/employees/organization?id=${employee.employeeId}`),
        EmployeeApi.get('/departments'),
        EmployeeApi.get('/positions'),
        EmployeeApi.get('/employees')
    ]);

    renderSelect(
        form.departmentId,
        departmentPayload.data,
        'departmentId',
        'departmentName',
        'Select Department',
        organizationPayload.data.departmentId
    );
    renderSelect(
        form.positionId,
        positionPayload.data,
        'positionId',
        'positionTitle',
        'Select Position',
        organizationPayload.data.positionId
    );
    renderManagerSelect(form.managerId, employeePayload.data, employee.employeeId, organizationPayload.data.managerId);

    form.addEventListener('submit', async event => {
        event.preventDefault();
        await saveOrganization(form);
    });
}

async function saveOrganization(form) {
    const data = readForm(form);
    if (data.employeeId === data.managerId) {
        showMessage('Employee cannot be assigned as their own manager.', 'error');
        return;
    }

    try {
        await EmployeeApi.post('/employees/assign-department', {
            employeeId: data.employeeId,
            departmentId: data.departmentId
        });
        await EmployeeApi.post('/employees/assign-position', {
            employeeId: data.employeeId,
            positionId: data.positionId
        });
        await EmployeeApi.post('/employees/assign-manager', {
            employeeId: data.employeeId,
            managerId: data.managerId
        });
        showMessage('Organization information updated successfully.');
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

function renderSelect(select, items, valueKey, labelKey, placeholder, selectedValue) {
    select.innerHTML = `<option value="">${placeholder}</option>` + items.map(item => `
        <option value="${item[valueKey]}">${safe(item[labelKey])}</option>
    `).join('');
    select.value = selectedValue || '';
}

function renderManagerSelect(select, employees, currentEmployeeId, selectedValue) {
    select.innerHTML = '<option value="">Select Manager</option>' + employees.map(employee => {
        const disabled = employee.employeeId === currentEmployeeId ? ' disabled' : '';
        const label = `${safe(employee.employeeCode)} - ${safe(employee.fullName)}`;
        return `<option value="${employee.employeeId}"${disabled}>${label}</option>`;
    }).join('');
    select.value = selectedValue || '';
}

function detailItem(label, value) {
    return `<div class="detail-item"><div class="detail-label">${label}</div><strong>${safe(value)}</strong></div>`;
}

function fillForm(form, employee) {
    Object.keys(employee).forEach(key => {
        if (form[key] && employee[key] !== null) {
            form[key].value = employee[key];
        }
    });
}

function readForm(form) {
    return Object.fromEntries(new FormData(form).entries());
}

function withContext(url) {
    const first = location.pathname.split('/').filter(Boolean)[0] || '';
    const contextPath = first === 'employee' ? '' : `/${first}`;
    return `${contextPath}${url}`;
}

async function handleResponse(response) {
    const payload = await response.json();
    if (!response.ok || payload.success === false) {
        throw new Error(payload.message || 'Request failed.');
    }
    return payload;
}

function showMessage(message, type = 'success') {
    const element = document.querySelector('#message');
    element.textContent = message;
    element.className = `message show ${type}`;
}

function formatMoney(value) {
    return Number(value || 0).toLocaleString('vi-VN') + ' đ';
}

function safe(value) {
    return value === null || value === undefined || value === '' ? '-' : String(value);
}
