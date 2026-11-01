const PositionApi = {
    async get(url) {
        const response = await fetch(positionApiUrl(url), { headers: { Accept: 'application/json' } });
        return handlePositionResponse(response);
    },

    async post(url, data) {
        const response = await fetch(positionApiUrl(url), {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            },
            body: new URLSearchParams({ ...data, csrfToken: data.csrfToken || document.querySelector('meta[name="organization-csrf-token"]')?.content || '' })
        });
        return handlePositionResponse(response);
    }
};

document.addEventListener('DOMContentLoaded', () => {
    document.querySelector('#statusFilter')?.addEventListener('change', loadPositions);
    document.querySelector('#btnExportPositions')?.addEventListener('click', (e) => {
        e.preventDefault();
        const status = document.querySelector('#statusFilter')?.value || '';
        const dept = document.querySelector('#departmentFilter')?.value || '';
        const params = new URLSearchParams();
        if (status) params.set('status', status);
        if (dept) params.set('departmentId', dept);
        window.location.href = positionApiUrl(`/positions/export?${params.toString()}`);
    });
    const page = document.body.dataset.page;
    if (page === 'position-list') {
        setupPositionList().catch(error => showPositionMessage(error.message, 'error'));
    }
    if (page === 'position-form') {
        setupPositionForm().catch(error => showPositionMessage(error.message, 'error'));
    }
});

async function loadPositions() {
    try {
        const departmentId = document.querySelector('#departmentFilter')?.value || '';
        const payload = await PositionApi.get('/positions' + (departmentId ? '?departmentId=' + encodeURIComponent(departmentId) : ''));
        renderPositionRows(payload.data);
        document.querySelector('#totalPositions').textContent = payload.data.length;
    } catch (error) {
        showPositionMessage(error.message, 'error');
    }
}

async function setupPositionForm() {
    const positionId = new URLSearchParams(location.search).get('id');
    const form = document.querySelector('#positionForm');
    await loadDepartmentOptions(form);
    const catalog = await PositionApi.get('/positions?action=catalogs');
    form.dataset.catalog = JSON.stringify(catalog.data);
    const reportingPositions = await PositionApi.get('/positions?activeOnly=true');
    form.dataset.reportingPositions = JSON.stringify(reportingPositions.data);
    form.departmentId.value = new URLSearchParams(location.search).get('departmentId') || '';
    renderPositionCatalog(form);

    if (positionId) {
        document.querySelector('#positionFormTitle').textContent = 'Cập nhật chức danh';
        const payload = await PositionApi.get(`/positions/detail?id=${positionId}`);
        await loadDepartmentOptions(form, payload.data.departmentId);
        renderPositionCatalog(form, payload.data);
        fillPositionForm(form, payload.data);
        renderReportingPositionOptions(form,payload.data.reportsToPositionId);
        if (new URLSearchParams(location.search).get('view') === '1') {
            document.querySelector('#positionFormTitle').textContent = 'Chi tiết chức danh';
            form.querySelectorAll('input, select, textarea, button').forEach(control => control.disabled = true);
        }
    }

    form.responsibilityLevelId.addEventListener('change', () => showResponsibilityAllowance(form));
    form.departmentId.addEventListener('change',()=>renderReportingPositionOptions(form));
    form.canManageEmployees.addEventListener('change',()=>renderReportingPositionOptions(form));
    showResponsibilityAllowance(form);
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (positionId) {
            await updatePosition(form);
        } else {
            await createPosition(form);
        }
    });
}

async function createPosition(form) {
    try {
        const payload = await PositionApi.post('/positions/create', readPositionForm(form));
        sessionStorage.setItem('organizationMessage', payload.message);
        location.assign('position-list');
    } catch (error) {
        if (!FormValidation.showErrors(form, error)) {
            showPositionMessage(error.message, 'error');
        }
    }
}

async function updatePosition(form) {
    try {
        const payload = await PositionApi.post('/positions/update', readPositionForm(form));
        sessionStorage.setItem('organizationMessage', payload.message);
        location.assign('position-list');
    } catch (error) {
        if (!FormValidation.showErrors(form, error)) {
            showPositionMessage(error.message, 'error');
        }
    }
}

async function setPositionActive(positionId, active) {
    if (!confirm(active ? 'Kích hoạt lại chức danh này?' : 'Vô hiệu hóa chức danh này?')) {
        return;
    }

    try {
        const payload = await PositionApi.post(active ? '/positions/activate' : '/positions/deactivate', { positionId });
        showPositionMessage(payload.message);
        await loadPositions();
    } catch (error) {
        showPositionMessage(error.message, 'error');
    }
}

function renderPositionRows(positions) {
    const status = document.querySelector('#statusFilter')?.value || '';
    const level = document.querySelector('#levelFilter')?.value || '';
    positions = positions.filter(item => !level || String(item.responsibilityLevelId) === level);
    positions = positions.filter(item => !status || (item.isActive) === (status === 'Active'));
    const tbody = document.querySelector('#positionRows');
    if (!positions.length) {
        tbody.innerHTML = '<tr><td colspan="6">Chưa có chức danh.</td></tr>';
        return;
    }

    tbody.innerHTML = positions.map(position => `
        <tr>
            <td><strong>${safePositionValue(position.positionTitle)}</strong>${position.canManageEmployees ? '<small>Được quản lý nhân viên</small>' : ''}</td>
            <td>${safePositionValue(position.departmentName)}</td>
            <td><a href="../responsibility-level/responsibility-level-form?id=${position.responsibilityLevelId}">${safePositionValue(position.responsibilityLevelName)}</a></td>
            <td>${position.effectiveAllowance == null ? 'Chưa cấu hình' : Number(position.effectiveAllowance).toLocaleString('vi-VN') + ' ₫'}<small>Theo cấp trách nhiệm</small></td>
            <td>${position.isActive ? 'Đang hoạt động' : 'Ngừng hoạt động'}</td>
            <td class="actions">
                <a href="position-form?id=${position.positionId}&view=1">Chi tiết</a>
                <a href="position-form?id=${position.positionId}">Sửa</a>
                <button class="danger" onclick="setPositionActive(${position.positionId}, ${!position.isActive})">${position.isActive ? 'Vô hiệu hóa' : 'Kích hoạt lại'}</button>
            </td>
        </tr>
    `).join('');
}

function fillPositionForm(form, position) {
    Object.keys(position).forEach(key => {
        if (form[key] && position[key] !== null) {
            if (form[key].type === 'checkbox') form[key].checked = position[key] === true;
            else form[key].value = position[key];
        }
    });
}

function readPositionForm(form) {
    return {...Object.fromEntries(new FormData(form).entries()), canManageEmployees: String(form.canManageEmployees.checked),
        reportsToPositionId: form.reportsToPositionId?.value || ''};
}

async function loadDepartmentOptions(form, selectedDepartment = form.departmentId.value) {
    const payload = await PositionApi.get('/departments');
    const departments = (payload.data || []).filter(department => department.isActive !== false || String(department.departmentId) === String(selectedDepartment));
    form.departmentId.innerHTML = '<option value="">Chọn phòng ban</option>' + departments.map(department => `
        <option value="${department.departmentId}">${safePositionValue(department.departmentName)}</option>
    `).join('');
}

function positionApiUrl(url) {
    const first = location.pathname.split('/').filter(Boolean)[0] || '';
    const contextPath = first === 'position' ? '' : `/${first}`;
    return `${contextPath}${url}`;
}

async function handlePositionResponse(response) {
    const payload = await response.json();
    if (!response.ok || payload.success === false) {
        throw FormValidation.responseError(payload);
    }
    return payload;
}

function showPositionMessage(message, type = 'success') {
    const element = document.querySelector('#message');
    element.textContent = message;
    element.className = `message show ${type}`;
}

function safePositionValue(value) {
    return value === null || value === undefined || value === '' ? '-' : String(value).replace(/[&<>"']/g, character => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]));
}
async function setupPositionList() {
    const payload = await PositionApi.get('/departments');
    const filter = document.querySelector('#departmentFilter');
    filter.replaceChildren(new Option('Tất cả phòng ban', ''));
    payload.data.forEach(department => filter.add(new Option(department.departmentName, department.departmentId)));
    filter.value = new URLSearchParams(location.search).get('departmentId') || '';
    filter.addEventListener('change', loadPositions);
    const levels=await PositionApi.get('/positions?action=catalogs');
    const levelFilter=document.querySelector('#levelFilter');
    levelFilter.replaceChildren(new Option('Tất cả cấp trách nhiệm',''));
    levels.data.responsibilityLevels.forEach(level => levelFilter.add(new Option(level.name,level.id)));
    levelFilter.value=new URLSearchParams(location.search).get('responsibilityLevelId') || '';
    levelFilter.addEventListener('change',loadPositions);
    const message = sessionStorage.getItem('organizationMessage');
    if (message) { showPositionMessage(message); sessionStorage.removeItem('organizationMessage'); }
    await loadPositions();
}

function renderPositionCatalog(form, selected = {}) {
    const catalog = JSON.parse(form.dataset.catalog);
    for (const [field, entries] of [['responsibilityLevelId', catalog.responsibilityLevels]]) {
        form[field].replaceChildren(new Option('Chọn cấp trách nhiệm', ''));
        entries.filter(entry => entry.active || String(entry.id) === String(selected[field]))
            .forEach(entry => form[field].add(new Option(entry.name + (entry.active ? '' : ' (ngừng hoạt động)'), entry.id)));
    }
}

function showResponsibilityAllowance(form) {
    const catalog = JSON.parse(form.dataset.catalog);
    const selected = catalog.responsibilityLevels.find(entry => String(entry.id) === form.responsibilityLevelId.value);
    if (form.canManageEmployees) {
        const employeeLevel = ['EMP', 'EMPLOYEE'].includes(String(selected?.code || '').trim().toUpperCase())
            || String(selected?.name || '').trim().toUpperCase() === 'EMPLOYEE';
        form.canManageEmployees.disabled = !selected || employeeLevel || new URLSearchParams(location.search).get('view') === '1';
        if (!selected || employeeLevel) form.canManageEmployees.checked = false;
    }
    document.querySelector('#responsibilityAllowance').textContent = selected && selected.allowance != null
        ? 'Phụ cấp theo cấp trách nhiệm: ' + Number(selected.allowance).toLocaleString('vi-VN') + ' ₫ (chỉ xem, cấu hình tại cấp trách nhiệm)'
        : 'Chưa có một chính sách mặc định hợp lệ; hãy cấu hình tại cấp trách nhiệm.';
    renderReportingPositionOptions(form);
}

function renderReportingPositionOptions(form, selected = form.reportsToPositionId?.value) {
    if (!form.reportsToPositionId) return;
    const levels=JSON.parse(form.dataset.catalog).responsibilityLevels;
    const level=levels.find(item=>String(item.id)===form.responsibilityLevelId.value);
    const employee=['EMP','EMPLOYEE'].includes(String(level?.code || '').trim().toUpperCase())
        || String(level?.name || '').trim().toUpperCase()==='EMPLOYEE';
    const manager=!employee && (form.canManageEmployees.checked || String(level?.code || '').trim().toUpperCase()==='MANAGER'
        || String(level?.name || '').trim().toUpperCase()==='MANAGER');
    const positions=JSON.parse(form.dataset.reportingPositions || '[]');
    form.reportsToPositionId.replaceChildren(new Option('Chưa cấu hình',''));
    if (!manager) for (const position of positions.filter(item=>item.canManageEmployees===true && item.isActive!==false && item.reportsToPositionId==null
        && String(item.departmentId)===form.departmentId.value && String(item.positionId)!==String(form.positionId?.value)))
        form.reportsToPositionId.add(new Option(position.positionTitle,position.positionId));
    form.reportsToPositionId.value=manager?'':selected || '';
    form.reportsToPositionId.disabled=manager || new URLSearchParams(location.search).get('view')==='1';
}
