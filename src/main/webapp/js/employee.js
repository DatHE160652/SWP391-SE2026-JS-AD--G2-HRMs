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
    },

    async upload(url, formData) {
        const response = await fetch(withContext(url), {
            method: 'POST',
            headers: { Accept: 'application/json' },
            body: formData
        });
        return handleResponse(response);
    }
};

document.addEventListener('DOMContentLoaded', () => {
    const page = document.body.dataset.page;
    setupTabs();

    if (page === 'list') {
        loadEmployees();
    }
    if (page === 'detail') {
        getEmployeeDetail();
    }
    if (page === 'form') {
        setupEmployeeForm();
    }
    if (page === 'my-profile') {
        setupMyProfile();
    }
});

function setupTabs() {
    document.querySelectorAll('.tab-button').forEach(button => {
        button.addEventListener('click', event => {
            event.preventDefault();
            const target = document.querySelector(`#${button.dataset.tab}Tab`);
            if (!target) {
                return;
            }
            document.querySelectorAll('.tab-button').forEach(item => item.classList.remove('active'));
            document.querySelectorAll('.tab-panel').forEach(panel => panel.classList.add('hidden'));
            button.classList.add('active');
            target.classList.remove('hidden');
        });
    });
}

async function loadEmployees() {
    try {
        const payload = await EmployeeApi.get('/employees');
        const employees = payload.data || [];
        renderEmployeeRows(employees);
        renderSummary(employees);
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
        await loadEmployeeHistory(employeeId, '#historyRows');
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

async function setupEmployeeForm() {
    const employeeId = new URLSearchParams(location.search).get('id');
    const form = document.querySelector('#employeeForm');
    setupEmployeeFieldValidation(form);
    form.dataset.editing = employeeId ? 'true' : 'false';
    form.employeeCodeMode.addEventListener('change', () => {
        updateEmployeeCodeInput(form);
        setEmployeeFieldError(form, 'employeeCode', '');
    });
    form.addEventListener('reset', () => {
        setTimeout(() => updateEmployeeCodeInput(form), 0);
    });
    updateEmployeeCodeInput(form);

    if (employeeId) {
        document.querySelector('#formTitle').textContent = 'Update Employee';
        const payload = await EmployeeApi.get(`/employees/detail?id=${employeeId}`);
        fillForm(form, payload.data);
    }

    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (!validateEmployeeForm(form)) {
            return;
        }
        if (employeeId) {
            await updateEmployee(form);
        } else {
            await createEmployee(form);
        }
    });
}

function setupEmployeeFieldValidation(form) {
    FormValidation.init(form);
}

function setEmployeeFieldError(form, name, message) {
    FormValidation.setError(form, name, message);
}

function validateEmployeeField(form, input) {
    return FormValidation.validateField(input);
}

function validateEmployeeForm(form) {
    return FormValidation.validate(form);
}

function showEmployeeFormError(form, error) {
    if (!FormValidation.showErrors(form, error)) {
        showMessage(error.message, 'error');
    }
}

function updateEmployeeCodeInput(form) {
    const editing = form.dataset.editing === 'true';
    const automatic = !editing && form.employeeCodeMode.value === 'AUTO';
    document.querySelector('#employeeCodeModeField').classList.toggle('hidden', editing);
    form.employeeCodeMode.disabled = editing;
    form.employeeCode.disabled = automatic;
    form.employeeCode.readOnly = editing;
    form.employeeCode.required = !editing && !automatic;
    form.employeeCode.placeholder = automatic ? 'Mã sẽ được tạo khi lưu nhân viên' : '';
    document.querySelector('#employeeCodeHint').textContent = editing
        ? 'Mã nhân viên hiện tại được giữ nguyên.'
        : automatic
            ? 'Hệ thống tự động tạo mã nhân viên duy nhất khi lưu.'
            : 'Nhập mã đang dùng tại công ty, không bắt buộc định dạng.';
}

async function setupMyProfile() {
    const form = document.querySelector('#myProfileForm');
    const navRole = getProfileNavRole();
    if (navRole) {
        renderMyProfileSidebar(navRole);
    }
    try {
        const payload = await EmployeeApi.get('/employees/me');
        renderMyProfileSidebar(navRole || payload.data.roleCode);
        renderMyProfile(payload.data);
        setProfileEditMode(false);
        if (form) {
            form.addEventListener('submit', async event => {
                event.preventDefault();
                await updateMyProfile(form);
            });
            document.querySelector('#editProfileButton').addEventListener('click', () => setProfileEditMode(true));
            document.querySelector('#discardProfileButton').addEventListener('click', async () => {
                const latest = await EmployeeApi.get('/employees/me');
                renderMyProfile(latest.data);
                setProfileEditMode(false);
            });
        }
        setupMyDocumentForm();
        await loadMyDocuments();
        await loadMyHistory();
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

function getProfileNavRole() {
    const nav = new URLSearchParams(location.search).get('nav');
    if (nav === 'manager') {
        return 'MANAGER';
    }
    if (nav === 'admin') {
        return 'COMPANY_ADMIN';
    }
    return '';
}

function renderMyProfileSidebar(roleCode) {
    const sidebar = document.querySelector('.sidebar');
    if (!sidebar) {
        return;
    }
    const role = String(roleCode || '').toUpperCase();
    if (role === 'MANAGER') {
        sidebar.innerHTML = `
            <div class="brand">HRMs Cloud<span>TechNova JSC</span></div>
            <div class="nav-label">Phân hệ Chấm công</div>
            <a class="nav-link active" href="../employee/my-profile">Hồ sơ cá nhân</a>
            <a class="nav-link" href="../attendance/manager-attendance">Điểm danh Nhân sự</a>
            <a class="nav-link" href="../attendance/manager-personal-attendance">Chấm công Cá nhân</a>
            <a class="nav-link" href="../attendance/attendance-report">Báo cáo Chấm công</a>
            <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
        `;
        return;
    }
    if (role === 'COMPANY_ADMIN') {
        sidebar.innerHTML = `
            <div class="brand">HRMs Cloud<span>TechNova JSC</span></div>
            <div class="nav-label">Phân hệ quản trị</div>
            <a class="nav-link active" href="../employee/my-profile">Hồ sơ cá nhân</a>
            <a class="nav-link" href="../employee/employee-list">Quản lý Nhân sự</a>
            <a class="nav-link" href="../department/department-list">Quản lý Phòng ban</a>
            <a class="nav-link" href="../position/position-list">Quản lý Chức danh</a>
            <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
        `;
        return;
    }
    sidebar.innerHTML = `
        <div class="brand">HRMs Cloud<span>Employee Portal</span></div>
        <div class="nav-label">Chức năng cá nhân</div>
        <a class="nav-link active" href="../employee/my-profile">Hồ sơ cá nhân</a>
        <a class="nav-link" href="../attendance/employee-attendance">Chấm công cá nhân</a>
        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    `;
}

async function updateMyProfile(form) {
    try {
        const payload = await EmployeeApi.post('/employees/me/update-profile', readForm(form));
        showMessage(payload.message);
        const latest = await EmployeeApi.get('/employees/me');
        renderMyProfile(latest.data);
        setProfileEditMode(false);
        await loadMyHistory();
    } catch (error) {
        showEmployeeFormError(form, error);
    }
}

function setupMyDocumentForm() {
    const form = document.querySelector('#myDocumentForm');
    if (!form || form.dataset.bound === 'true') {
        return;
    }
    form.dataset.bound = 'true';
    form.addEventListener('submit', async event => {
        event.preventDefault();
        await uploadMyDocument(form);
    });
}

async function loadMyDocuments() {
    const tbody = document.querySelector('#myDocumentRows');
    if (!tbody) {
        return;
    }
    try {
        const payload = await EmployeeApi.get('/documents/me');
        renderMyDocumentRows(payload.data || []);
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

async function uploadMyDocument(form) {
    try {
        const payload = await EmployeeApi.upload('/documents/me/upload', new FormData(form));
        showMessage(payload.message);
        form.reset();
        await loadMyDocuments();
        await loadMyHistory();
    } catch (error) {
        showEmployeeFormError(form, error);
    }
}

async function loadMyHistory() {
    await loadHistory('/employees/me/history', '#profileHistoryRows');
}

async function loadEmployeeHistory(employeeId, selector) {
    await loadHistory(`/employees/history?id=${employeeId}`, selector);
}

async function loadHistory(url, selector) {
    const tbody = document.querySelector(selector);
    if (!tbody) {
        return;
    }
    try {
        const payload = await EmployeeApi.get(url);
        renderHistoryRows(tbody, payload.data || []);
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="5">${safe(error.message)}</td></tr>`;
    }
}

async function deleteMyDocument(documentId) {
    if (!confirm('Delete this document?')) {
        return;
    }

    try {
        const payload = await EmployeeApi.post('/documents/me/delete', { documentId });
        showMessage(payload.message);
        await loadMyDocuments();
        await loadMyHistory();
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

async function createEmployee(form) {
    try {
        await EmployeeApi.post('/employees/create', readForm(form));
        location.assign('employee-list');
    } catch (error) {
        showEmployeeFormError(form, error);
    }
}

async function updateEmployee(form) {
    try {
        await EmployeeApi.post('/employees/update', readForm(form));
        location.assign('employee-list');
    } catch (error) {
        showEmployeeFormError(form, error);
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
        tbody.innerHTML = '<tr><td colspan="7">No employees found.</td></tr>';
        return;
    }

    tbody.innerHTML = employees.map(employee => `
        <tr>
            <td><strong>${safe(employee.employeeCode)}</strong></td>
            <td>${safe(employee.fullName)}</td>
            <td>${safe(employee.phone)}</td>
            <td>${safe(employee.gender)}</td>
            <td>${safe(employee.identityNumber)}</td>
            <td><span class="badge ${safe(employee.status)}">${safe(employee.status)}</span></td>
            <td class="actions">
                <a href="employee-detail?id=${employee.employeeId}">View</a>
                <a href="employee-form?id=${employee.employeeId}">Edit</a>
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
        ${detailItem('Mã định danh', employee.identityNumber)}
        ${detailItem('Status', employee.status)}
    `;

    document.querySelector('#employmentTab').innerHTML = `
        ${detailItem('Account Username', employee.username)}
        ${detailItem('Account Email', employee.accountEmail)}
        ${detailItem('Account Status', employee.accountActive === true ? 'Active' : employee.accountActive === false ? 'Inactive' : null)}
    `;
}

function renderMyProfile(employee) {
    document.querySelector('#profileName').textContent =
        `Employee Details - ${employee.employeeCode || 'N/A'} (${employee.fullName || 'Employee'})`;
    document.querySelector('#profileCode').textContent = employee.employeeCode || '-';
    const form = document.querySelector('#myProfileForm');
    if (form) {
        form.employeeCode.value = employee.employeeCode || '';
        form.fullName.value = employee.fullName || '';
        form.phone.value = employee.phone || '';
        form.dateOfBirth.value = employee.dateOfBirth || '';
        form.status.value = employee.status || '';
        const gender = employee.gender || '';
        form.querySelectorAll('input[name="gender"]').forEach(input => {
            input.checked = input.value === gender;
        });
    }

    document.querySelector('#employmentTab').innerHTML = `
        ${detailItem('Employee Code', employee.employeeCode)}
        ${detailItem('Full Name', employee.fullName)}
        ${detailItem('Mã định danh', employee.identityNumber)}
        ${detailItem('Employment Status', employee.status)}
    `;
    document.querySelector('#salaryTab').innerHTML = `
        ${detailItem('Salary Currency', 'VND')}
        ${detailItem('Payroll Status', employee.status)}
    `;
    document.querySelector('#accountTab').innerHTML = `
        ${detailItem('Account Username', employee.username)}
        ${detailItem('Account Email', employee.accountEmail)}
        ${detailItem('Account Status', employee.accountActive === true ? 'Active' : employee.accountActive === false ? 'Inactive' : null)}
    `;
}

function setProfileEditMode(editing) {
    const form = document.querySelector('#myProfileForm');
    if (!form) {
        return;
    }
    ['fullName', 'phone', 'dateOfBirth'].forEach(name => {
        if (form[name]) {
            form[name].readOnly = !editing;
        }
    });
    form.querySelectorAll('input[name="gender"]').forEach(input => {
        input.disabled = !editing;
    });
    document.querySelector('#editProfileButton').classList.toggle('hidden', editing);
    document.querySelector('#submitProfileButton').classList.toggle('hidden', !editing);
    document.querySelector('#discardProfileButton').classList.toggle('hidden', !editing);
}

function renderMyDocumentRows(documents) {
    const tbody = document.querySelector('#myDocumentRows');
    if (!tbody) {
        return;
    }
    if (!documents.length) {
        tbody.innerHTML = '<tr><td colspan="5">No documents found.</td></tr>';
        return;
    }

    tbody.innerHTML = documents.map(document => `
        <tr>
            <td>${safe(document.documentName)}</td>
            <td>${safe(document.documentType)}</td>
            <td>${safe(document.uploadedDate)}</td>
            <td>${safe(document.uploadedBy)}</td>
            <td class="actions">
                <a href="${withContext(`/documents/me/download?id=${document.documentId}&inline=true`)}" target="_blank" rel="noopener">View</a>
                <a href="${withContext(`/documents/me/download?id=${document.documentId}`)}" download="${safe(document.fileName || document.documentName || 'document')}">Download</a>
                <button class="danger" onclick="deleteMyDocument(${document.documentId})">Delete</button>
            </td>
        </tr>
    `).join('');
}

function renderHistoryRows(tbody, history) {
    if (!history.length) {
        tbody.innerHTML = '<tr><td colspan="5">No history found.</td></tr>';
        return;
    }

    tbody.innerHTML = history.map(item => `
        <tr>
            <td>${safe(item.createdAt)}</td>
            <td>${safe(item.action)}</td>
            <td>${safe(item.changedBy)}</td>
            <td>${safe(item.oldValue)}</td>
            <td>${safe(item.newValue)}</td>
        </tr>
    `).join('');
}

async function setupOrganizationForm(employee) {
    const form = document.querySelector('#organizationForm');
    if (!form) {
        return;
    }

    form.employeeId.value = employee.employeeId;
    document.querySelector('#organizationEmployeeName').value = employee.fullName || '';

    const [organizationPayload, departmentPayload, positionPayload] = await Promise.all([
        EmployeeApi.get(`/employees/organization?id=${employee.employeeId}`),
        EmployeeApi.get('/departments'),
        EmployeeApi.get('/positions')
    ]);
    form.dataset.employeeId = employee.employeeId;
    form.dataset.positions = JSON.stringify(positionPayload.data || []);
    form.dataset.managerId = organizationPayload.data.managerId || '';

    renderSelect(
        form.departmentId,
        departmentPayload.data,
        'departmentId',
        'departmentName',
        'Select Department',
        organizationPayload.data.departmentId
    );
    renderPositionSelect(form, organizationPayload.data.positionId);
    form.positionId.required = false;
    form.positionId.removeAttribute('required');
    form.positionId.addEventListener('change', () => updateOrganizationMode(form));
    form.departmentId.addEventListener('change', async () => {
        renderPositionSelect(form, '');
        await updateOrganizationMode(form);
    });
    await updateOrganizationMode(form);

    form.addEventListener('submit', async event => {
        event.preventDefault();
        await saveOrganization(form);
    });
}

async function saveOrganization(form) {
    const data = readForm(form);
    const leaderMode = isSelectedLeaderPosition(form);
    if (!data.departmentId) {
        FormValidation.setError(form, 'departmentId', 'Vui lòng chọn phòng ban.');
        return;
    }
    if (!leaderMode && data.employeeId === data.managerId) {
        FormValidation.setError(form, 'managerId', 'Nhân viên không thể tự quản lý chính mình.');
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
        if (leaderMode) {
            const employeeIds = Array.from(document.querySelectorAll('#leaderEmployeeOptions input[name="leaderEmployeeIds"]:checked'))
                .map(input => input.value)
                .join(',');
            await EmployeeApi.post('/departments/assign-members', {
                departmentId: data.departmentId,
                managerId: data.employeeId,
                employeeIds
            });
            await updateOrganizationMode(form);
        } else if (data.managerId) {
            await EmployeeApi.post('/employees/assign-manager', {
                employeeId: data.employeeId,
                managerId: data.managerId
            });
        }
        showMessage('Organization information updated successfully.');
        await loadEmployeeHistory(data.employeeId, '#historyRows');
    } catch (error) {
        showEmployeeFormError(form, error);
    }
}

async function updateOrganizationMode(form) {
    const leaderMode = isSelectedLeaderPosition(form);
    const panel = document.querySelector('#leaderEmployeesPanel');
    const label = document.querySelector('#managerFieldLabel');
    form.managerId.closest('div').classList.toggle('hidden', leaderMode);
    form.managerId.required = !leaderMode;
    panel.classList.toggle('hidden', !leaderMode);
    label.textContent = leaderMode ? 'Employee' : 'Manager';
    form.positionId.disabled = !form.departmentId.value;
    if (!form.departmentId.value) {
        document.querySelector('#leaderEmployeeOptions').innerHTML = '';
        document.querySelector('#leaderEmployeeRows').innerHTML = '<tr><td colspan="4">Select department first.</td></tr>';
        form.managerId.innerHTML = '<option value="">Select Manager</option>';
        return;
    }
    if (leaderMode) {
        await loadLeaderEmployeeOptions(form);
    } else {
        await loadDepartmentManagers(form);
    }
}

async function loadDepartmentManagers(form) {
    const select = form.managerId;
    if (!form.departmentId.value) {
        select.innerHTML = '<option value="">Select Manager</option>';
        return;
    }
    const payload = await EmployeeApi.get(`/departments/members?departmentId=${form.departmentId.value}`);
    const currentEmployeeId = String(form.dataset.employeeId);
    const managers = (payload.data || []).filter(employee =>
        String(employee.employeeId) !== currentEmployeeId
        && String(employee.departmentId || '') === String(form.departmentId.value)
        && String(employee.roleCode || '').toUpperCase() === 'MANAGER'
    );
    renderManagerSelect(select, managers, currentEmployeeId, form.dataset.managerId);
}

function isSelectedLeaderPosition(form) {
    const positions = JSON.parse(form.dataset.positions || '[]');
    const selected = positions.find(position => String(position.positionId) === String(form.positionId.value));
    if (!selected) {
        return false;
    }
    const title = String(selected ? selected.positionTitle : '').toLowerCase();
    return title.includes('leader') || title.includes('manager') || title.includes('trưởng') || title.includes('quan ly') || title.includes('quản lý');
}

async function loadLeaderEmployeeOptions(form) {
    const payload = await EmployeeApi.get(`/departments/members?departmentId=${form.departmentId.value}`);
    const currentEmployeeId = String(form.dataset.employeeId);
    const employees = (payload.data || []).filter(employee =>
        String(employee.employeeId) !== currentEmployeeId
        && (!employee.departmentId || String(employee.departmentId) === String(form.departmentId.value))
    );
    const picker = document.querySelector('#leaderEmployeeOptions');
    picker.innerHTML = employees.map(employee => {
        const checked = String(employee.managerId || '') === currentEmployeeId ? 'checked' : '';
        return `
            <label class="member-option">
                <input type="checkbox" name="leaderEmployeeIds" value="${employee.employeeId}" ${checked}>
                <span>
                    <strong>${safe(employee.employeeCode)} - ${safe(employee.fullName)}</strong>
                    <span>${safe(employee.positionTitle)} | Role: ${safe(employee.roleCode)}</span>
                </span>
            </label>
        `;
    }).join('');
    renderLeaderEmployeeRows(employees.filter(employee => String(employee.managerId || '') === currentEmployeeId));
}

function renderPositionSelect(form, selectedValue) {
    const selectedDepartmentId = String(form.departmentId.value || '');
    const positions = selectedDepartmentId
        ? JSON.parse(form.dataset.positions || '[]').filter(position =>
            String(position.departmentId || '') === selectedDepartmentId
        )
        : [];
    renderSelect(form.positionId, positions, 'positionId', 'positionTitle', 'Select Position', selectedValue);
    form.positionId.disabled = !form.departmentId.value;
    form.positionId.required = false;
    form.positionId.removeAttribute('required');
}

function renderLeaderEmployeeRows(employees) {
    const tbody = document.querySelector('#leaderEmployeeRows');
    if (!employees.length) {
        tbody.innerHTML = '<tr><td colspan="4">No managed employees found.</td></tr>';
        return;
    }
    tbody.innerHTML = employees.map(employee => `
        <tr>
            <td>${safe(employee.employeeCode)}</td>
            <td>${safe(employee.fullName)}</td>
            <td>${safe(employee.positionTitle)}</td>
            <td>${safe(employee.roleCode)}</td>
        </tr>
    `).join('');
}

function renderSelect(select, items, valueKey, labelKey, placeholder, selectedValue) {
    select.innerHTML = `<option value="">${placeholder}</option>` + items.map(item => `
        <option value="${item[valueKey]}">${safe(item[labelKey])}</option>
    `).join('');
    select.value = selectedValue || '';
}

function renderManagerSelect(select, employees, currentEmployeeId, selectedValue) {
    select.innerHTML = '<option value="">Select Manager</option>' + employees.map(employee => {
        const label = `${safe(employee.employeeCode)} - ${safe(employee.fullName)}`;
        return `<option value="${employee.employeeId}">${label}</option>`;
    }).join('');
    select.value = selectedValue || '';
    if (select.value !== String(selectedValue || '')) {
        select.value = '';
    }
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
    const rootFolders = ['employee', 'document'];
    const contextPath = rootFolders.includes(first) ? '' : `/${first}`;
    return `${contextPath}${url}`;
}

async function handleResponse(response) {
    const payload = await response.json();
    if (!response.ok || payload.success === false) {
        const error = new Error(payload.message || 'Request failed.');
        error.fieldErrors = payload.fieldErrors;
        throw error;
    }
    return payload;
}

function showMessage(message, type = 'success') {
    const element = document.querySelector('#message');
    if (!element) {
        return;
    }
    element.textContent = message;
    element.className = `message show ${type}`;
}

function formatMoney(value) {
    return Number(value || 0).toLocaleString('vi-VN') + ' VND';
}

function safe(value) {
    return value === null || value === undefined || value === '' ? '-' : String(value);
}
