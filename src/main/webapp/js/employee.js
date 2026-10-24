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
            body: new URLSearchParams({ ...data, csrfToken: data.csrfToken || document.querySelector('meta[name="organization-csrf-token"]')?.content || '' })
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
    document.querySelector('#statusFilter')?.addEventListener('change', loadEmployees);
    const page = document.body.dataset.page;
    setupTabs();

    if (page === 'list') {
        loadEmployees();
    }
    if (page === 'detail') {
        getEmployeeDetail();
    }
    if (page === 'form') {
        setupEmployeeForm().catch(error => showMessage(error.message, 'error'));
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
            if (document.body.dataset.page==='detail') {
                document.querySelectorAll('.tab-button').forEach(item => {
                    item.setAttribute('role','tab');
                    item.setAttribute('aria-selected',String(item===button));
                    item.setAttribute('aria-controls',item.dataset.tab+'Tab');
                });
                target.setAttribute('role','tabpanel');
                const url=new URL(location.href);
                url.searchParams.set('tab',button.dataset.tab);
                history.replaceState(null,'',url);
            }
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
        showMessage("Vui lòng chọn nhân viên.", 'error');
        return;
    }

    try {
        const payload = await EmployeeApi.get(`/employees/detail?id=${employeeId}`);
        renderEmployeeDetail(payload.data);
        await setupOrganizationForm(payload.data);
        const fields=document.querySelector('#assignmentFields');
        if (fields) fields.disabled=false;
        openEmployeeDetailTab();
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
    form.addEventListener('reset', () => {
        setTimeout(() => updateEmployeeCodeInput(form), 0);
    });
    updateEmployeeCodeInput(form);

    if (employeeId) {
        document.querySelector('#formTitle').textContent = "Cập nhật nhân viên";
        const payload = await EmployeeApi.get(`/employees/detail?id=${employeeId}`);
        fillForm(form, payload.data);
        form.dataset.departmentId = payload.data.departmentId || '';
        form.dataset.positionId = payload.data.positionId || '';
    }

    await setupEmployeePositionFields(form);

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
    form.employeeCode.disabled = false;
    form.employeeCode.readOnly = editing;
    form.employeeCode.required = !editing;
    form.employeeCode.placeholder = 'Nhập mã đang dùng tại công ty';
    const hint = document.querySelector('#employeeCodeHint');
    if (hint) hint.textContent = editing
        ? 'Mã nhân viên hiện tại được giữ nguyên.'
        : 'Nhập mã đang dùng tại công ty, không bắt buộc định dạng.';
}

async function setupMyProfile() {
    const form = document.querySelector('#myProfileForm');
    const navRole = document.body.dataset.profileRole;
    if (navRole) {
        renderMyProfileSidebar(navRole);
    }
    try {
        const payload = await EmployeeApi.get('/employees/me');
        renderMyProfileSidebar(payload.data.roleCode);
        renderMyProfile(payload.data);
        setProfileEditMode(false);
        if (form) {
            form.addEventListener('submit', async event => {
                event.preventDefault();
                await updateMyProfile(form);
            });
            document.querySelector('#editProfileButton').addEventListener('click', async () => {
                try {
                    const latest = await EmployeeApi.get('/employees/me');
                    renderMyProfile(latest.data);
                    setProfileEditMode(true);
                } catch (error) {
                    showMessage(error.message, 'error');
                    setProfileEditMode(false);
                }
            });
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

function renderMyProfileSidebar(roleCode) {
    const modernSidebar = document.querySelector('.lc-sidebar');
    if (modernSidebar) {
        const links = [['/employee/my-profile', 'Hồ sơ cá nhân']];
        if (roleCode==='MANAGER') links.push(['/employee/organization','Sơ đồ tổ chức'],['/employee/my-contracts','Hợp đồng của tôi']);
        if (roleCode === 'MANAGER') links.push(['/attendance/manager-attendance', 'Điểm danh nhân sự'], ['/attendance/manager-personal-attendance', 'Chấm công cá nhân'], ['/attendance/attendance-report', 'Báo cáo chấm công']);
        else if (['COMPANY_ADMIN','HR'].includes(roleCode)) links.push(['/lifecycle/dashboard', 'Bảng điều khiển'], ['/employee/employee-list', 'Hồ sơ nhân viên'], ['/employee/profile-update-periods', 'Cập nhật hồ sơ'], ['/department/department-list', 'Phòng ban'], ['/position/position-list', 'Chức danh'], ['/responsibility-level/responsibility-level-list', 'Cấp trách nhiệm']);
        else links.push(['/attendance/employee-attendance', 'Chấm công cá nhân']);
        modernSidebar.querySelector('nav').innerHTML = links.map(([url, label]) => '<a href="' + withContext(url) + '"' + (location.pathname === withContext(url) ? ' class="active"' : '') + ' aria-label="' + label + '"><span class="lc-nav-icon" aria-hidden="true">◉</span><span>' + label + '</span></a>').join('');
        return;
    }
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
    if (['COMPANY_ADMIN', 'HR'].includes(role)) {
        sidebar.innerHTML = `
            <div class="brand">HRMs Cloud<span>TechNova JSC</span></div>
            <div class="nav-label">Phân hệ quản trị</div>
            <a class="nav-link active" href="../employee/my-profile">Hồ sơ cá nhân</a>
            <a class="nav-link" href="../employee/employee-list">Quản lý Nhân sự</a>
            <a class="nav-link" href="../employee/profile-update-periods">Đợt cập nhật hồ sơ</a>
            <a class="nav-link" href="../department/department-list">Quản lý Phòng ban</a>
            <a class="nav-link" href="../position/position-list">Quản lý Chức danh</a>
            <a class="nav-link" href="../responsibility-level/responsibility-level-list">Cấp trách nhiệm</a>
            <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
        `;
        return;
    }
    sidebar.innerHTML = `
        <div class="brand">HRMs Cloud<span>Cổng nhân viên</span></div>
        <div class="nav-label">Chức năng cá nhân</div>
        <a class="nav-link active" href="../employee/my-profile">Hồ sơ cá nhân</a>
        <a class="nav-link" href="../attendance/employee-attendance">Chấm công cá nhân</a>
        <a class="nav-link logout-link" href="../logout">Đăng xuất</a>
    `;
}

async function updateMyProfile(form) {
    try {
        if (!FormValidation.validate(form)) return;
        const changes = {csrfToken:form.csrfToken.value};
        if (form.dataset.profileVersion) changes.updatedAt = form.dataset.profileVersion;
        ['email','phone','identityIssuedDate','identityIssuedPlace','permanentAddress','hometown'].forEach(name => {
            const field = form.elements.namedItem(name);
            if (field) changes[name] = field.value;
        });
        const payload = await EmployeeApi.post('/employees/me/update-profile', changes);
        showMessage(payload.message);
        const latest = await EmployeeApi.get('/employees/me');
        renderMyProfile(latest.data);
        setProfileEditMode(false);
        await loadMyHistory();
    } catch (error) {
        showEmployeeFormError(form, error);
        if (error.status === 403) {
            form.dataset.canUpdateProfile = 'false';
            form.dataset.canUpdatePhone = 'false';
            setProfileEditMode(false);
            document.querySelector('#profileUpdateStatus').textContent = error.message;
        }
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
    if (!canUpdateMyDocuments()) {
        updateMyDocumentPermissions();
        showMessage('Chỉ được thay đổi tài liệu khi có đợt cập nhật tài liệu đang mở.', 'error');
        return;
    }
    try {
        if (!FormValidation.validate(form)) return;
        const values = new FormData(form);
        values.set('csrfToken', document.querySelector('#myProfileForm')?.elements.namedItem('csrfToken')?.value || document.querySelector('meta[name="organization-csrf-token"]')?.content || '');
        const payload = await EmployeeApi.upload('/documents/me/upload', values);
        showMessage(payload.message);
        form.reset();
        await loadMyDocuments();
        await loadMyHistory();
    } catch (error) {
        showEmployeeFormError(form, error);
        lockMyDocumentUpdates(error);
    }
}

async function loadMyHistory() {
    await loadHistory('/employees/me/history', '#profileHistoryRows');
}

async function loadEmployeeHistory(employeeId, selector) {
    await Promise.all([loadHistory(`/employees/history?id=${employeeId}`, selector), loadCareerHistory(employeeId)]);
}

async function loadCareerHistory(employeeId) {
    const rows=document.querySelector('#careerHistoryRows');
    if (!rows) return;
    try {
        const payload=await EmployeeApi.get('/employees/career-history?id='+encodeURIComponent(employeeId));
        renderCareerHistoryRows(rows,payload.data||[]);
    } catch (error) { rows.innerHTML=`<tr><td colspan="7">${safe(error.message)}</td></tr>`; }
}

function renderCareerHistoryRows(rows,events) {
    if (!events.length) { rows.innerHTML='<tr><td colspan="7">Chưa có lịch sử nghiệp vụ nhân sự.</td></tr>'; return; }
    rows.innerHTML=events.map(event=>`<tr>
        <td>${safe(event.effectiveDate)}<br><small>Ghi nhận (UTC): ${safe(event.recordedAt?.replace('T',' '))}</small></td>
        <td>${safe(careerEventLabel(event.eventType))}</td>
        <td>${formatHistoryValue(event.oldValue)}</td><td>${formatHistoryValue(event.newValue)}</td>
        <td>${formatHistoryValue(event.reason)}</td><td>${safe(careerStatusLabel(event.status))}</td>
        <td>${safe(event.changedBy)}</td></tr>`).join('');
}

function careerEventLabel(type) {
    if (globalThis.HrmVietnamese?.labels[type]) return globalThis.HrmVietnamese.label(type);
    return ({HIRE:'Phân công vị trí ban đầu',BASELINE:'Ghi nhận vị trí ban đầu',TRANSFER:'Chuyển vị trí / phòng ban',
        PROMOTION:'Thăng cấp',DEMOTION:'Hạ cấp',CORRECTION:'Hiệu chỉnh vị trí',
        DEPARTMENT_ASSIGNED:'Phân công phòng ban',DEPARTMENT_TRANSFERRED:'Chuyển phòng ban',
        MANAGER_ASSIGNED:'Gán người quản lý',MANAGER_TRANSFERRED:'Chuyển người quản lý',MANAGER_REMOVED:'Bỏ người quản lý',
        MANAGER_CHANGED:'Thay đổi người quản lý',TEAM_MEMBER_ASSIGNED:'Thêm nhân viên vào nhóm',
        TEAM_MEMBER_TRANSFERRED:'Chuyển nhân viên giữa các nhóm',TEAM_MEMBER_REMOVED:'Bỏ nhân viên khỏi nhóm'})[type]||type;
}

function careerStatusLabel(status) {
    return ({APPLIED:'Đã áp dụng',PENDING:'Chờ duyệt',APPROVED:'Đã duyệt',REJECTED:'Đã từ chối',CANCELLED:'Đã hủy',
        ACTIVE:'Đang hiệu lực',ENDED:'Đã kết thúc',SCHEDULED:'Chờ hiệu lực',CORRECTED:'Đã hiệu chỉnh trong ngày'})[status]||status;
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

async function setMyDocumentActive(documentId, active) {
    if (!canUpdateMyDocuments()) {
        updateMyDocumentPermissions();
        showMessage('Chỉ được thay đổi tài liệu khi có đợt cập nhật tài liệu đang mở.', 'error');
        return;
    }
    if (!confirm(active ? 'Kích hoạt lại tài liệu này?' : 'Vô hiệu hóa tài liệu này?')) {
        return;
    }

    try {
        const payload = await EmployeeApi.post(active ? '/documents/me/activate' : '/documents/me/deactivate', { documentId, csrfToken: document.querySelector('#myProfileForm')?.elements.namedItem('csrfToken')?.value || '' });
        showMessage(payload.message);
        await loadMyDocuments();
        await loadMyHistory();
    } catch (error) {
        showMessage(error.message, 'error');
        lockMyDocumentUpdates(error);
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

async function setEmployeeActive(employeeId, active) {
    if (!confirm(active ? 'Kích hoạt lại bản ghi này?' : 'Vô hiệu hóa bản ghi này?')) {
        return;
    }

    const reason = active ? '' : prompt('Lý do ngừng làm việc (phân công và hợp đồng sẽ kết thúc, tài khoản sẽ bị khóa):');
    if (!active && !reason?.trim()) return;

    try {
        const payload = await EmployeeApi.post(active ? '/employees/activate' : '/employees/deactivate', { employeeId, reason });
        showMessage(payload.message);
        await loadEmployees();
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

function renderEmployeeRows(employees) {
    const status = document.querySelector('#statusFilter')?.value || '';
    employees = employees.filter(item => !status || (item.status === 'Active') === (status === 'Active'));
    const tbody = document.querySelector('#employeeRows');
    if (!employees.length) {
        tbody.innerHTML = '<tr><td colspan="7">Không có nhân viên phù hợp.</td></tr>';
        return;
    }

    tbody.innerHTML = employees.map(employee => `
        <tr>
            <td><strong>${safe(employee.employeeCode)}</strong></td>
            <td>${safe(employee.fullName)}</td>
            <td>${safe(employee.phone)}</td>
            <td>${safe(employee.gender)}</td>
            <td>${safe(employee.identityNumber)}</td>
            <td><span class="badge ${safe(employee.status)}">${safe(globalThis.HrmVietnamese?.label(employee.status) ?? employee.status)}</span></td>
            <td class="actions">
                <a href="employee-detail?id=${employee.employeeId}">Xem</a>
                <a href="employee-form?id=${employee.employeeId}">Chỉnh sửa</a>
                <button class="danger" onclick="setEmployeeActive(${employee.employeeId}, ${employee.status !== 'Active'})">${employee.status === 'Active' ? 'Vô hiệu hóa' : 'Kích hoạt lại'}</button>
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

function openEmployeeDetailTab() {
    const requested=new URLSearchParams(location.search).get('tab') || 'organization';
    const tabs=Array.from(document.querySelectorAll('.tab-button'));
    const selected=tabs.find(button => button.dataset.tab===requested)
        || tabs.find(button => button.dataset.tab==='organization');
    if (selected) selected.click();
}

function employeeContractLabel(employee) {
    if (!employee.contractType) return 'Chưa có hợp đồng';
    const type=({PROBATION:'Thử việc',FIXED_TERM:'Có thời hạn',INDEFINITE:'Không xác định thời hạn'})[employee.contractType] || employee.contractType;
    const status=({DRAFT:'Bản nháp',UPCOMING:'Chưa đến ngày hiệu lực',ACTIVE:'Đang hiệu lực',EXPIRED:'Hết hạn',TERMINATED:'Đã chấm dứt'})[employee.contractStatus] || employee.contractStatus;
    return status ? type+' · '+status : type;
}

function renderEmployeeDetail(employee) {
    document.querySelector('#detailTitle').textContent=employee.fullName || 'Phân công và cấp dưới';
    const id=encodeURIComponent(employee.employeeId);
    const back=document.querySelector('#detailBackLink'),chart=document.querySelector('#detailOrgChartLink');
    if (back) back.href=withContext('/employee/employee-list')+'?id='+id;
    if (chart) chart.href=withContext('/lifecycle/organization')+'?view=chart&employeeId='+id;
    const code=document.querySelector('#detailEmployeeCode'),avatar=document.querySelector('#detailAvatar');
    if (code) code.textContent=employee.employeeCode || 'Chưa có mã nhân viên';
    if (avatar) avatar.textContent=String(employee.fullName || '?').trim().split(/\s+/).slice(-2).map(part => part[0]).join('').toUpperCase();
    const subtitle=document.querySelector('#detailSubtitle');
    if (subtitle) subtitle.textContent=[employee.departmentName,employee.positionTitle].filter(Boolean).join(' · ') || 'Chưa phân công tổ chức';
    const gender=({Male:'Nam',Female:'Nữ',Other:'Khác'})[employee.gender] || employee.gender;
    const status=employee.status==='Active'?'Đang làm việc':employee.status==='Inactive'?'Ngừng làm việc':employee.status;
    document.querySelector('#profileTab').innerHTML=[
        ['Mã nhân viên',employee.employeeCode],['Họ tên',employee.fullName],['Email',employee.email],
        ['Điện thoại',employee.phone],['Giới tính',gender],['Ngày sinh',employee.dateOfBirth],
        ['Số CCCD/CMND',employee.identityNumber],['Trạng thái',status]
    ].map(([label,value]) => detailItem(label,value)).join('');
    document.querySelector('#employmentTab').innerHTML=[
        ['Trạng thái làm việc',status],['Phòng ban',employee.departmentName],
        ['Chức danh',employee.positionTitle],['Loại hợp đồng',employeeContractLabel(employee)]
    ].map(([label,value]) => detailItem(label,value)).join('')
        +'<div class="ea-employment-links">'
        +'<a href="'+withContext('/lifecycle/contracts')+'?employeeId='+id+'">Xem hợp đồng lao động →</a>'
        +'<a href="'+withContext('/lifecycle/accounts')+'?employeeId='+id+'">Xem tài khoản nhân viên →</a></div>';
}

function renderMyProfile(employee) {
    if (employee.roleCode) document.body.dataset.profileRole = employee.roleCode;
    document.querySelector('#profileName').textContent =
        employee.fullName || 'Hồ sơ cá nhân';
    document.querySelector('#profileCode').textContent = employee.employeeCode || '-';
    const form = document.querySelector('#myProfileForm');
    if (form) {
        form.dataset.canUpdateProfile = String((employee.canUpdateProfile ?? employee.canUpdatePhone) === true);
        form.dataset.canUpdatePhone = form.dataset.canUpdateProfile;
        form.dataset.updateDeadline = employee.profileUpdatePeriod ? employee.profileUpdatePeriod.endsAt : '';
        form.dataset.canUpdateDocuments = String(employee.canUpdateDocuments === true);
        form.dataset.documentUpdateDeadline = employee.documentUpdatePeriod ? employee.documentUpdatePeriod.endsAt : '';
        form.dataset.profileVersion = employee.updatedAt || '';
        globalThis.FormValidation?.clear(form);
        form.csrfToken.value = employee.csrfToken || '';
        form.employeeCode.value = employee.employeeCode || '';
        form.fullName.value = employee.fullName || '';
        form.phone.value = employee.phone || '';
        form.dateOfBirth.value = employee.dateOfBirth || '';
        form.status.value = globalThis.HrmVietnamese?.label(employee.status) || employee.status || '';
        ['email','identityNumber','identityIssuedDate','identityIssuedPlace','permanentAddress','hometown','major'].forEach(name => {
            const field = form.elements.namedItem(name);
            if (field) field.value = employee[name] || '';
        });
        const issuedDate = form.elements.namedItem('identityIssuedDate');
        if (issuedDate) {
            issuedDate.min = employee.dateOfBirth || '';
            issuedDate.max = new Date().toLocaleDateString('en-CA');
        }
        const gender = employee.gender || '';
        form.querySelectorAll('input[name="gender"]').forEach(input => {
            input.checked = input.value === gender;
        });
        const period = employee.profileUpdatePeriod;
        document.querySelector('#profileUpdateStatus').textContent = (employee.canUpdateProfile ?? employee.canUpdatePhone) && period
            ? `Đợt "${period.title}" đang mở đến ${new Date(period.endsAt).toLocaleString('vi-VN')}. Bạn được cập nhật email liên hệ, số điện thoại, ngày/nơi cấp căn cước, địa chỉ thường trú và quê quán.`
            : 'Không có đợt cập nhật hồ sơ đang mở. Thông tin cá nhân hiện không thể chỉnh sửa.';
        setProfileEditMode(false);
        scheduleProfileUpdateExpiry(form);
        scheduleDocumentUpdateExpiry(form);
    }

    document.querySelector('#employmentTab').innerHTML = `
        ${detailItem('Mã nhân viên', employee.employeeCode)}
        ${detailItem('Họ và tên', employee.fullName)}
        ${detailItem('Phòng ban', employee.departmentName)}
        ${detailItem('Chức danh', employee.positionTitle)}
        ${detailItem('Quản lý trực tiếp', employee.managerName)}
        ${detailItem('Loại hợp đồng', employeeContractLabel(employee))}
        ${detailItem('Trạng thái làm việc', employee.status)}
    `;
    renderMyCompensation(employee.compensation);
    document.querySelector('#accountTab').innerHTML = `
        ${detailItem('Tên đăng nhập', employee.username)}
        ${detailItem('Email đăng nhập', employee.accountEmail)}
        ${detailItem('Trạng thái tài khoản', employee.accountActive === true ? 'Hoạt động' : employee.accountActive === false ? 'Ngừng hoạt động' : null)}
    `;
}

function personalTable(key,labels,rows) {
    return '<div class="lc-table-wrap"><table data-pagination="'+key+'"><thead><tr>'
        + labels.map(label=>'<th>'+safe(label)+'</th>').join('')+'</tr></thead><tbody>'
        + (rows.length ? rows.map(row=>'<tr>'+row.map(value=>'<td>'+safe(value ?? 'Chưa ghi nhận')+'</td>').join('')+'</tr>').join('')
            : '<tr><td colspan="'+labels.length+'">Chưa có dữ liệu.</td></tr>')+'</tbody></table></div>';
}


function renderMyCompensation(compensation) {
    const history = compensation?.history || [];
    const current = history.find(record => Number(record.IsCurrent) === 1);
    const allowance = compensation?.allowances?.[0];
    const money = value => value == null ? 'Chưa ghi nhận' : new Intl.NumberFormat('vi-VN', {style:'currency',currency:'VND'}).format(value);
    document.querySelector('#salaryTab').innerHTML = '<div class="personal-salary-heading"><h2>Lương & phụ cấp của tôi</h2>'
        + '<p>Lương theo hợp đồng đã có hiệu lực; phụ cấp chức vụ theo cấu hình cấp trách nhiệm hiện hành.</p></div>'
        + '<div class="detail-grid">'
        + detailItem('Lương Gross',current ? money(current.BaseSalary) : 'Chưa có lương có hiệu lực')
        + detailItem('Phụ cấp đi lại',money(current?.TravelAllowance))
        + detailItem('Phụ cấp chức vụ',allowance?.Amount != null ? money(allowance.Amount)
            : allowance ? 'Chưa cấu hình phụ cấp có hiệu lực' : 'Chưa có phân công tổ chức có hiệu lực')
        + detailItem('Cấp trách nhiệm',allowance?.LevelName || 'Chưa phân công')
        + detailItem('Hiệu lực lương từ',current?.EffectiveFrom || 'Chưa ghi nhận')
        + detailItem('Hợp đồng nguồn',current?.ContractCode || 'Chưa ghi nhận') + '</div>'
        + '<section class="personal-salary-history"><h3>Lịch sử lương</h3><div class="lc-table-wrap">'
        + '<table data-pagination="my-compensation-history"><thead><tr><th>Hiệu lực từ</th><th>Đến ngày</th>'
        + '<th>Lương Gross</th><th>Phụ cấp đi lại</th><th>Hợp đồng</th></tr></thead><tbody>'
        + (history.length ? history.map(record => '<tr><td>' + safe(record.EffectiveFrom) + '</td><td>'
            + safe(Number(record.IsSuperseded) === 1 ? 'Thay thế trong ngày' : record.EffectiveTo || 'Hiện tại')
            + '</td><td>' + safe(money(record.BaseSalary)) + '</td><td>' + safe(money(record.TravelAllowance))
            + '</td><td>' + safe(record.ContractCode || 'Chưa ghi nhận') + '</td></tr>').join('')
            : '<tr><td colspan="5">Chưa có lịch sử lương.</td></tr>')
        + '</tbody></table></div></section>';
}

function setProfileEditMode(editing) {
    const form = document.querySelector('#myProfileForm');
    if (!form) {
        return;
    }
    const available = form.dataset.canUpdateProfile === 'true'
        && Date.parse(form.dataset.updateDeadline) > Date.now();
    const profileEditing = editing && available;
    ['employeeCode','fullName','dateOfBirth','identityNumber','major','status'].forEach(name => {
        const field = form.elements.namedItem(name);
        if (field) field.readOnly = true;
    });
    form.querySelectorAll('input[name="gender"]').forEach(input => {
        input.disabled = true;
    });
    ['email','phone','identityIssuedDate','identityIssuedPlace','permanentAddress','hometown'].forEach(name => {
        const field = form.elements.namedItem(name);
        if (field) field.readOnly = !profileEditing;
    });
    document.querySelector('#editProfileButton').disabled = !available;
    document.querySelector('#editProfileButton').classList.toggle('hidden', profileEditing || !available);
    document.querySelector('#submitProfileButton').classList.toggle('hidden', !profileEditing);
    document.querySelector('#discardProfileButton').classList.toggle('hidden', !profileEditing);
    updateMyDocumentPermissions();
}

function canUpdateMyDocuments() {
    const profile = document.querySelector('#myProfileForm');
    return profile?.dataset.canUpdateDocuments === 'true' && Date.parse(profile.dataset.documentUpdateDeadline) > Date.now();
}

function updateMyDocumentPermissions() {
    const available = canUpdateMyDocuments();
    const form = document.querySelector('#myDocumentForm');
    if (form) Array.from(form.elements).forEach(field => { field.disabled = !available; });
    document.querySelectorAll('[data-my-document-write]').forEach(button => { button.disabled = !available; });
    const status = document.querySelector('#documentUpdateStatus');
    if (status) status.textContent = available
        ? 'Đợt cập nhật tài liệu đang mở. Bạn có thể tải lên, lưu trữ hoặc kích hoạt lại tài liệu của mình.'
        : 'Tài liệu đang được khóa cập nhật. Bạn vẫn có thể xem và tải xuống; chỉ được thay đổi khi có đợt cập nhật tài liệu đang mở.';
}

function lockMyDocumentUpdates(error) {
    if (error.status !== 403) return;
    const profile = document.querySelector('#myProfileForm');
    if (profile) {
        profile.dataset.canUpdateDocuments = 'false';
    }
    updateMyDocumentPermissions();
    const status = document.querySelector('#documentUpdateStatus');
    if (status) status.textContent = error.message;
}

let documentUpdateExpiryTimer;
function scheduleDocumentUpdateExpiry(form) {
    clearTimeout(documentUpdateExpiryTimer);
    if (form.dataset.canUpdateDocuments !== 'true') return;
    const remaining = Date.parse(form.dataset.documentUpdateDeadline)-Date.now();
    if (!Number.isFinite(remaining) || remaining<=0) {
        form.dataset.canUpdateDocuments = 'false';
        updateMyDocumentPermissions();
        return;
    }
    documentUpdateExpiryTimer=setTimeout(()=>scheduleDocumentUpdateExpiry(form),Math.min(remaining+1,2147483647));
}

let profileUpdateExpiryTimer;

function scheduleProfileUpdateExpiry(form) {
    clearTimeout(profileUpdateExpiryTimer);
    if (form.dataset.canUpdateProfile !== 'true') {
        return;
    }
    const remaining = Date.parse(form.dataset.updateDeadline) - Date.now();
    if (!Number.isFinite(remaining) || remaining <= 0) {
        form.dataset.canUpdateProfile = 'false';
        form.dataset.canUpdatePhone = 'false';
        setProfileEditMode(false);
        document.querySelector('#profileUpdateStatus').textContent = 'Đợt cập nhật đã kết thúc. Các trường thông tin cá nhân đã được khóa.';
        return;
    }
    profileUpdateExpiryTimer = setTimeout(() => scheduleProfileUpdateExpiry(form), Math.min(remaining + 1, 2147483647));
}

function renderMyDocumentRows(documents) {
    const tbody = document.querySelector('#myDocumentRows');
    if (!tbody) {
        return;
    }
    if (!documents.length) {
        tbody.innerHTML = '<tr><td colspan="5">Chưa có tài liệu.</td></tr>';
        return;
    }

    tbody.innerHTML = documents.map(document => `
        <tr>
            <td>${safe(document.documentName)}</td>
            <td>${safe(document.documentType)}</td>
            <td>${safe(document.uploadedDate)}</td>
            <td>${safe(document.uploadedBy)}</td>
            <td class="actions">
                ${document.status && document.status !== 'Active' ? '' : `
                <a href="${withContext(`/documents/me/download?id=${document.documentId}&inline=true`)}" target="_blank" rel="noopener">Xem</a>
                <a href="${withContext(`/documents/me/download?id=${document.documentId}`)}" download="${safe(document.fileName || document.documentName || 'document')}">Tải xuống</a>
                `}
                <span>${safe(globalThis.HrmVietnamese?.label(document.status || 'Active') ?? document.status ?? 'Đang hoạt động')}</span>
                <button class="danger" data-my-document-write ${canUpdateMyDocuments() ? '' : 'disabled'} onclick="setMyDocumentActive(${document.documentId}, ${document.status !== null && document.status !== 'Active'})">${document.status === null || document.status === 'Active' ? 'Vô hiệu hóa' : 'Kích hoạt lại'}</button>
            </td>
        </tr>
    `).join('');
}

function renderHistoryRows(tbody, history) {
    if (!history.length) {
        tbody.innerHTML = '<tr><td colspan="5">Chưa có lịch sử.</td></tr>';
        return;
    }

    tbody.innerHTML = history.map(item => `
        <tr>
            <td>${safe(item.createdAt)}</td>
            <td>${safe(historyActionLabel(item.action))}</td>
            <td>${safe(item.changedBy)}</td>
            <td>${formatHistoryValue(item.oldValue)}</td>
            <td>${formatHistoryValue(item.newValue)}</td>
        </tr>
    `).join('');
}

function formatHistoryValue(value) {
    return value === null || value === undefined || value === '' ? '-'
        : String(value).split(/\r?\n/).map(line => safe(globalThis.HrmVietnamese?.label(line) ?? line)).join('<br>');
}

function historyActionLabel(action) {
    if (globalThis.HrmVietnamese?.labels[action]) return globalThis.HrmVietnamese.label(action);
    if (action?.startsWith('MANAGER_') || action?.startsWith('TEAM_MEMBER_')) return careerEventLabel(action);
    return ({CHANGE_ORGANIZATION: 'Cập nhật cơ cấu tổ chức', CHANGE_MANAGER: 'Đổi người quản lý trực tiếp',
        CHANGE_DEPARTMENT: 'Đổi phòng ban', CHANGE_POSITION: 'Đổi chức danh',
        ASSIGN_TO_MANAGER: 'Phân công người quản lý', ASSIGN_TEAM: 'Phân công nhóm',
        POSITION_CHANGE_APPLIED: 'Áp dụng thay đổi chức danh'})[action] || action;
}

async function setupOrganizationForm(employee) {
    const form = document.querySelector('#organizationForm');
    if (!form) return;

    form.employeeId.value = employee.employeeId;
    document.querySelector('#organizationEmployeeName').value = employee.fullName || '';
    if (form.dataset.readOnly === 'true') {
        form.addEventListener('submit', event => event.preventDefault());
        const payload = await EmployeeApi.get(`/employees/organization?id=${employee.employeeId}`);
        if (form.dataset.embedded === 'true' && !form.isConnected) return;
        form.dataset.employeeId = String(employee.employeeId);
        document.querySelector('#organizationDepartmentName').value = payload.data.departmentName || employee.departmentName || 'Chưa phân công';
        document.querySelector('#organizationPositionTitle').value = payload.data.positionTitle || employee.positionTitle || 'Chưa phân công';
        document.querySelector('#directManager').value = payload.data.managerName || 'Không có quản lý trực tiếp';
        await updateOrganizationMode(form);
        if (form.dataset.embedded === 'true' && !form.isConnected) return;
        bindOrganizationTeam(form);
        return;
    }

    const [organizationPayload, departmentPayload] = await Promise.all([
        EmployeeApi.get(`/employees/organization?id=${employee.employeeId}`),
        EmployeeApi.get('/departments')
    ]);
    if (form.dataset.embedded === 'true' && !form.isConnected) return;
    form.dataset.employeeId = String(employee.employeeId);
    form.dataset.managerId = String(organizationPayload.data.managerId || '');
    form.dataset.roleCode = organizationPayload.data.roleCode || '';
    form.dataset.managerName = organizationPayload.data.managerName || '';
    renderSelect(form.departmentId, departmentPayload.data.filter(department => department.isActive !== false || department.departmentId === organizationPayload.data.departmentId), 'departmentId', 'departmentName',
        'Chọn phòng ban', organizationPayload.data.departmentId);
    await loadOrganizationPositions(form, organizationPayload.data.positionId);
    if (form.dataset.embedded === 'true' && !form.isConnected) return;
    form.departmentId.addEventListener('change', async () => {
        const oldPosition = form.positionId.value;
        await Promise.all([loadOrganizationPositions(form, oldPosition), loadEligibleManagers(form, form.managerId.value)])
            .catch(error => showMessage(error.message, 'error'));
    });
    await loadEligibleManagers(form, form.dataset.managerId);
    if (form.dataset.embedded === 'true' && !form.isConnected) return;
    form.managerId.addEventListener('change', () => {
        document.querySelector('#managerAssignmentWarning').textContent = form.managerId.value
            && form.managerId.value === form.dataset.invalidManagerId ? 'Chọn quản lý hợp lệ hoặc chọn Không có quản lý trực tiếp trước khi lưu.' : '';
    });
    await updateOrganizationMode(form);
    if (form.dataset.embedded === 'true' && !form.isConnected) return;
    bindOrganizationTeam(form);

    form.addEventListener('submit', async event => {
        event.preventDefault();
        await saveOrganization(form);
    });
}

function bindOrganizationTeam(form) {
    document.querySelector('#manageTeamButton').addEventListener('click', () => openManageTeam(form).catch(error => showMessage(error.message, 'error')));
    document.querySelector('#closeTeamDialog').addEventListener('click', () => document.querySelector('#manageTeamDialog').close());
    document.querySelector('#manageTeamForm').addEventListener('submit', event => {
        event.preventDefault();
        saveTeamMembers(form).catch(error => { document.querySelector('#teamMessage').textContent = error.message; });
    });
}

async function saveOrganization(form) {
    if (form.dataset.readOnly === 'true') return;
    if (form.managerId.disabled || form.dataset.positionLoadFailed === 'true' || form.positionId.disabled) {
        showMessage('Vui lòng đợi danh sách chức danh và quản lý tải xong.', 'error');
        return;
    }
    const data = readForm(form);
    if (!data.departmentId) {
        FormValidation.setError(form, 'departmentId', 'Vui lòng chọn phòng ban.');
        return;
    }
    if (data.managerId && data.managerId === form.dataset.invalidManagerId) {
        FormValidation.setError(form, 'managerId', 'Quản lý hiện tại không còn hợp lệ. Chọn quản lý khác hoặc Không có quản lý trực tiếp.');
        return;
    }
    if (form.dataset.managerId && form.dataset.managerId !== (data.managerId || '')
            && !confirm('Xác nhận thay đổi hoặc bỏ quản lý trực tiếp hiện tại?')) return;

    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    try {
        const payload = await EmployeeApi.post('/employees/assign-organization', {
            employeeId: data.employeeId,
            departmentId: data.departmentId,
            positionId: data.positionId || '',
            managerId: data.managerId || '',
            reason: data.reason || ''
        });
        if (form.dataset.embedded === 'true') {
            document.dispatchEvent(new CustomEvent('employee-organization-updated', {detail:{employeeId:data.employeeId}}));
            return;
        }
        const saved = (await EmployeeApi.get('/employees/organization?id=' + encodeURIComponent(data.employeeId))).data;
        form.departmentId.value = saved.departmentId || '';
        form.dataset.managerId = String(saved.managerId || '');
        form.dataset.managerName = saved.managerName || '';
        await loadOrganizationPositions(form, saved.positionId);
        await loadEligibleManagers(form, form.dataset.managerId);
        await updateOrganizationMode(form);
        showMessage(payload.message || 'Đã cập nhật phòng ban và chức danh.');
        await loadEmployeeHistory(data.employeeId, '#historyRows');
    } catch (error) {
        showEmployeeFormError(form, error);
    } finally {
        button.disabled = false;
    }
}

async function updateOrganizationMode(form) {
    const panel = document.querySelector('#directReportsPanel');
    if (!panel) return;
    const requestRevision = String(Number(form.dataset.teamRevision || 0) + 1);
    form.dataset.teamRevision = requestRevision;
    const [payload, organization] = await Promise.all([
        EmployeeApi.get('/employees/direct-reports?id=' + encodeURIComponent(form.dataset.employeeId)),
        EmployeeApi.get('/employees/organization?id=' + encodeURIComponent(form.dataset.employeeId))
    ]);
    if (form.dataset.embedded === 'true' && !form.isConnected) return;
    if (form.dataset.teamRevision !== requestRevision) return;
    const employees = payload.data || [];
    document.querySelector('#directReportsCount').textContent = String(employees.length);
    const canManage = organization.data.canManageTeam;
    form.dataset.savedDepartmentName = organization.data.departmentName || '';
    document.querySelector('#directReportsWarning').textContent = !canManage && employees.length
        ? 'Chức danh hiện tại không được nhận thêm cấp dưới. Cần phân công lại hoặc gỡ các quan hệ cũ trước khi đổi tổ chức.' : '';
    const teamButton = document.querySelector('#manageTeamButton');
    teamButton.hidden = !canManage && !employees.length;
    teamButton.textContent = canManage ? 'Quản lý thành viên nhóm' : 'Xử lý quan hệ cấp dưới';
    renderLeaderEmployeeRows(employees);
}

async function loadOrganizationPositions(form, selectedValue) {
    const departmentId = form.departmentId.value;
    const revision = String(Number(form.dataset.positionRevision || 0) + 1);
    form.dataset.positionRevision = revision;
    form.positionId.disabled = true;
    form.positionId.innerHTML = '<option value="">Đang tải chức danh…</option>';
    try {
        const payload = departmentId ? await EmployeeApi.get('/positions?activeOnly=true&departmentId=' + encodeURIComponent(departmentId)) : {data: []};
        if (revision !== form.dataset.positionRevision || departmentId !== form.departmentId.value) return;
        form.dataset.positions = JSON.stringify(payload.data || []);
        renderPositionSelect(form, selectedValue);
        delete form.dataset.positionLoadFailed;
    } catch (error) {
        if (revision === form.dataset.positionRevision) form.dataset.positionLoadFailed = 'true';
        throw error;
    }
}

async function loadEligibleManagers(form, selectedValue) {
    const departmentId = form.departmentId.value;
    const revision = String(Number(form.dataset.managerRevision || 0) + 1);
    form.dataset.managerRevision = revision;
    form.managerId.disabled = true;
    try {
        const payload = await EmployeeApi.get('/employees/eligible-managers?id=' + encodeURIComponent(form.dataset.employeeId)
            + '&departmentId=' + encodeURIComponent(departmentId));
        if (form.dataset.embedded === 'true' && !form.isConnected) return;
        if (revision !== form.dataset.managerRevision || departmentId !== form.departmentId.value) return;
        const managers = payload.data || [];
        renderSelect(form.managerId, managers, 'employeeId', 'fullName', 'Không có quản lý trực tiếp', selectedValue);
        const invalid = selectedValue && !managers.some(manager => String(manager.employeeId) === String(selectedValue));
        form.dataset.invalidManagerId = invalid ? String(selectedValue) : '';
        if (invalid) {
            form.managerId.add(new Option((form.dataset.managerName || 'Quản lý mã ' + selectedValue) + ' (không còn đủ điều kiện)', selectedValue));
            form.managerId.value = String(selectedValue);
        }
        document.querySelector('#managerAssignmentWarning').textContent = form.dataset.roleCode === 'MANAGER'
            ? "Tài khoản có quyền quản lý không nhận quản lý trực tiếp." : invalid
            ? 'Quản lý hiện tại không còn đủ điều kiện trong phòng ban đã chọn. Chọn người khác hoặc Không có quản lý trực tiếp.'
            : managers.length ? '' : 'Chưa có quản lý đủ điều kiện trong phòng ban. Có thể chọn Không có quản lý trực tiếp.';
        form.managerId.disabled = false;
    } catch (error) {
        document.querySelector('#managerAssignmentWarning').textContent = 'Không tải được danh sách quản lý. Vui lòng tải lại trước khi lưu.';
        throw error;
    }
}

async function openManageTeam(form) {
    const payload = await EmployeeApi.get('/employees/team-options?id=' + encodeURIComponent(form.dataset.employeeId));
    form.dataset.teamOptions = JSON.stringify(payload.data || []);
    document.querySelector('#teamManagerName').textContent = document.querySelector('#organizationEmployeeName').value
        + ' — ' + form.dataset.savedDepartmentName;
    document.querySelector('#teamMessage').textContent = 'Chỉ được chọn nhân viên chưa có Manager. Chuyển hoặc gỡ quản lý tại Phòng ban → Phân công nhân viên.';
    document.querySelector('#teamChangeReason').value = '';
    document.querySelector('#teamMemberOptions').innerHTML = payload.data.map(member => `
        <label class="member-option">
            <input type="checkbox" name="teamEmployeeIds" value="${Number(member.employeeId)}"
                ${member.selected ? 'checked' : ''} ${member.selected || member.managerId != null || !member.assignable ? 'disabled' : ''}>
            <span><strong>${safe(member.employeeCode)} - ${safe(member.fullName)}</strong>
                <span>${safe(member.departmentName)} | ${safe(member.positionTitle)} | Quản lý hiện tại: ${safe(member.managerName)}
                ${member.managerId != null ? ' | Đã có Manager — thay đổi tại Phòng ban' : member.assignable ? '' : ' | Không thể phân công cho quản lý này'}</span></span>
        </label>`).join('') || '<p>Chưa có nhân viên phù hợp.</p>';
    document.querySelector('#manageTeamDialog').showModal();
}

async function saveTeamMembers(form) {
    const options = JSON.parse(form.dataset.teamOptions || '[]');
    const selected = new Set(Array.from(document.querySelectorAll('input[name="teamEmployeeIds"]:checked')).map(input => Number(input.value)));
    const added = options.filter(member => !member.selected && selected.has(member.employeeId));
    const removed = options.filter(member => member.selected && !selected.has(member.employeeId));
    const transfers = added.filter(member => member.managerId && String(member.managerId) !== form.dataset.employeeId);
    if (transfers.length || removed.length) {
        document.querySelector('#teamMessage').textContent='Để chuyển hoặc gỡ quản lý, vui lòng dùng phân công trực tiếp tại Phòng ban.';
        return;
    }
    const button = document.querySelector('#manageTeamForm button[type="submit"]');
    button.disabled = true;
    try {
        await EmployeeApi.post('/employees/manage-team', {
            managerId: form.dataset.employeeId,
            addEmployeeIds: added.map(member => member.employeeId).join(','),
            removeEmployeeIds: removed.map(member => member.employeeId).join(','),
            expectedManagers: added.map(member => member.employeeId + ':' + (member.managerId || 0)).join(','),
            confirmTransfers: String(transfers.length > 0),
            confirmRemovals: String(removed.length > 0),
            reason: document.querySelector('#teamChangeReason').value
        });
        if (form.dataset.embedded === 'true') {
            if (form.isConnected) document.querySelector('#manageTeamDialog').close();
            document.dispatchEvent(new CustomEvent('employee-organization-updated', {detail:{employeeId:form.dataset.employeeId}}));
            return;
        }
        document.querySelector('#manageTeamDialog').close();
        await updateOrganizationMode(form);
        await loadEmployeeHistory(form.dataset.employeeId,'#historyRows');
        showMessage('Đã cập nhật thành viên nhóm.');
    } finally { button.disabled = false; }
}

function renderLeaderEmployeeRows(employees) {
    const rows = document.querySelector('#directReportRows');
    rows.innerHTML = employees.length ? employees.map(employee => `
        <tr>
            <td>${safe(employee.employeeCode)}</td>
            <td>${safe(employee.fullName)}</td>
            <td>${safe(employee.departmentName)}</td>
            <td>${safe(employee.positionTitle)}</td>
            <td><a href="${withContext('/employee/employee-list')}?id=${Number(employee.employeeId)}">Xem hồ sơ</a></td>
        </tr>
    `).join('') : '<tr><td colspan="5">Chưa có nhân viên báo cáo trực tiếp cho quản lý này.</td></tr>';
}

function renderPositionSelect(form, selectedValue) {
    const selectedDepartmentId = String(form.departmentId.value || '');
    const positions = selectedDepartmentId
        ? JSON.parse(form.dataset.positions || '[]').filter(position =>
            position.isActive && String(position.departmentId || '') === selectedDepartmentId
        )
        : [];
    renderSelect(form.positionId, positions, 'positionId', 'positionTitle', 'Chọn chức danh', selectedValue);
    form.positionId.disabled = !form.departmentId.value;
    form.positionId.required = false;
    form.positionId.removeAttribute('required');
}

function renderSelect(select, items, valueKey, labelKey, placeholder, selectedValue) {
    select.innerHTML = `<option value="">${placeholder}</option>` + items.map(item => `
        <option value="${item[valueKey]}">${safe(item[labelKey])}</option>
    `).join('');
    select.value = selectedValue || '';
}

function detailItem(label, value) {
    return `<div class="detail-item"><div class="detail-label">${label}</div><strong>${safe(globalThis.HrmVietnamese?.label(value) ?? value)}</strong></div>`;
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
        const error = new Error(payload.message || "Không thực hiện được yêu cầu.");
        error.status = response.status;
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
    return value === null || value === undefined || value === '' ? '-' : String(value).replace(/[&<>"']/g, character => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]));
}
async function setupEmployeePositionFields(form) {
    const departments = await EmployeeApi.get('/departments');
    const selectedDepartment = form.dataset.departmentId || '';
    const selectedPosition = form.dataset.positionId || '';
    renderSelect(form.departmentId, departments.data.filter(department => department.isActive !== false || String(department.departmentId) === String(selectedDepartment)), 'departmentId', 'departmentName', "Chọn phòng ban", selectedDepartment);
    let revision = 0;
    async function load(selected = '') {
        const requestRevision = ++revision;
        form.positionId.replaceChildren(new Option("Chọn chức danh", ''));
        form.positionId.disabled = true;
        if (!form.departmentId.value) return;
        const payload = await EmployeeApi.get('/positions?activeOnly=true&departmentId=' + encodeURIComponent(form.departmentId.value));
        if (requestRevision !== revision) return;
        renderSelect(form.positionId, payload.data, 'positionId', 'positionTitle', "Chọn chức danh", selected);
        form.positionId.disabled = false;
    }
    if (selectedPosition) {
        document.querySelector('#employeePositionHint').textContent =
            'Nhân viên đã có vị trí. Thay đổi phòng ban/vị trí cần quyết định có ngày hiệu lực.';
        form.departmentId.disabled = true;
        form.positionId.disabled = true;
        const detail = await EmployeeApi.get('/positions/detail?id=' + encodeURIComponent(selectedPosition));
        form.positionId.replaceChildren(new Option(detail.data.positionTitle, selectedPosition));
        for (const [name, value] of [['departmentId', selectedDepartment], ['positionId', selectedPosition]]) {
            const hidden = document.createElement('input');
            hidden.type = 'hidden'; hidden.name = name; hidden.value = value; form.append(hidden);
        }
        return;
    }
    form.departmentId.addEventListener('change', () => load().catch(error => showMessage(error.message, 'error')));
    await load(selectedPosition);
}
