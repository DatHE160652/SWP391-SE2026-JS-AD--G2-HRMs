(() => {
    'use strict';
    const context = document.body.dataset.context || '';
    const page = document.body.dataset.lifecycle;
    const csrf = document.querySelector('meta[name="csrf-token"]').content;
    const select = selector => document.querySelector(selector);
    const escape = value => String(value ?? '').replace(/[&<>"']/g, character => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]));
    const vietnamese = value => globalThis.HrmVietnamese?.label(value) ?? value;
    const money = value => value == null ? 'Chưa có dữ liệu' : new Intl.NumberFormat('vi-VN', {style:'currency',currency:'VND'}).format(value);
    const initials = name => String(name || '?').trim().split(/\s+/).slice(-2).map(part => part[0]).join('').toUpperCase();
    const empty = message => `<div class="lc-empty">${escape(message)}</div>`;
    const badge = status => `<span class="lc-badge ${['Active','ACTIVE'].includes(status) ? 'active' : ''}">${escape(vietnamese(status))}</span>`;
    let toastTimer;
    let employees = [];
    let departments = [];
    let currentEmployee = null;
    const requestedTab = new URLSearchParams(location.search).get('tab');
    let activeTab = ['info','organization','salary','history','audit','documents'].includes(requestedTab) ? requestedTab : 'info';
    let detailRevision = 0;
    let employeePage = 1;
    let employeePageSize = 10;
    const error = exception => { select('#lc-error').textContent = exception.message || String(exception); };
    const toast = message => {
        clearTimeout(toastTimer);
        select('#lc-toast').textContent = message;
        toastTimer = setTimeout(() => { select('#lc-toast').textContent = ''; }, 4500);
    };
    async function api(path, values) {
        const options = {headers:{Accept:'application/json'},credentials:'same-origin'};
        if (values) {
            options.method = 'POST';
            options.body = globalThis.HrmLifecycleHttp.formBody(values, csrf);
        }
        const response = await fetch(context + path, options);
        const result = await response.json();
        if (!response.ok || !result.success) {
            const exception = new Error(result.message || 'Không thể xử lý yêu cầu.');
            exception.fieldErrors = result.fieldErrors;
            throw exception;
        }
        return result.data;
    }
    document.querySelectorAll('.lc-sidebar nav a').forEach(link => link.classList.toggle('active', link.pathname === location.pathname));
    document.addEventListener('click', event => {
        const button = event.target.closest('[data-close]');
        if (button) button.closest('dialog').close();
    });
    document.addEventListener('click', async event => {
        const button = event.target.closest('[data-copy-password]');
        if (!button) return;
        const dialog = button.closest('dialog');
        const password = dialog.querySelector('.lc-password')?.textContent;
        if (!dialog.open || !password) return;
        const status = dialog.querySelector('.lc-copy-status');
        button.disabled = true;
        status.textContent = '';
        try {
            try {
                if (!navigator.clipboard?.writeText) throw new Error("Không có quyền sử dụng bộ nhớ tạm");
                await navigator.clipboard.writeText(password);
            } catch (_) {
                const field = document.createElement('textarea');
                field.value = password;
                field.readOnly = true;
                field.className = 'lc-clipboard-fallback';
                dialog.append(field);
                try {
                    field.select();
                    if (!document.execCommand('copy')) throw new Error("Không sao chép được");
                } finally { field.remove(); }
            }
            status.textContent = 'Đã sao chép mật khẩu.';
        } catch (_) {
            status.textContent = 'Không thể sao chép tự động. Bạn có thể chọn mật khẩu và sao chép thủ công.';
        } finally {
            button.disabled = false;
            if (dialog.open && button.isConnected) button.focus();
        }
    });
    document.querySelectorAll('dialog').forEach(dialog => dialog.addEventListener('close', () => {
        const password = dialog.querySelector('.lc-password');
        if (password && !dialog.open) password.textContent = '';
    }));
    function historyActorLabel(value) {
        if (!value || ['Không ghi nhận','Chưa ghi nhận người thực hiện'].includes(value)) return 'Chưa ghi nhận người thực hiện';
        if (['System','SYSTEM'].includes(value)) return 'Hệ thống';
        return value;
    }
    function option(value, label) { return `<option value="${escape(value)}">${escape(vietnamese(label))}</option>`; }
    function filteredEmployees() {
        const query = select('#lc-search').value.trim().toLocaleLowerCase('vi');
        const department = select('#lc-department-filter').value;
        return employees.filter(employee => (!department || String(employee.departmentId) === department)
            && `${employee.fullName} ${employee.employeeCode}`.toLocaleLowerCase('vi').includes(query));
    }
    function renderEmployees() {
        const filtered = filteredEmployees();
        const totalPages = Math.ceil(filtered.length / employeePageSize);
        employeePage = Math.max(1,Math.min(employeePage,totalPages || 1));
        const start = (employeePage - 1) * employeePageSize;
        const visible = filtered.slice(start,start + employeePageSize);
        select('#lc-count').textContent = `${filtered.length} / ${employees.length}`;
        HrmPagination.render(select('#lc-employee-pagination'),{
            total:filtered.length,page:employeePage,pageSize:employeePageSize,compact:true,idPrefix:'lc-page',
            label:'Phân trang danh sách nhân viên',onChange(page,pageSize) {
                employeePage=page; employeePageSize=pageSize;
                renderEmployees();
                select('#lc-employees').scrollTop=0;
            }
        });
        select('#lc-employees').innerHTML = visible.length ? visible.map(employee => `<div class="lc-employee ${currentEmployee?.employeeId === employee.employeeId ? 'selected' : ''}">
            <button type="button" class="lc-select-employee" data-employee="${employee.employeeId}"><span class="lc-avatar">${escape(initials(employee.fullName))}</span>
            <span><span class="lc-employee-name">${escape(employee.fullName)}</span><br><span class="lc-employee-code">${escape(employee.employeeCode)} · ${escape(employee.departmentName || 'Chưa phân công')}</span><br>
            <span class="lc-employee-code"><i class="lc-status-dot ${employee.status === 'Active' ? 'active' : ''}"></i>${escape(vietnamese(employee.status))}</span></span></button></div>`).join('') : empty('Không tìm thấy nhân viên');
    }
    async function loadEmployees(preferredId) {
        employees = await api('/employees');
        const target = employees.find(employee => employee.employeeId === Number(preferredId)) || employees[0];
        if (target) await chooseEmployee(target.employeeId);
        else { currentEmployee = null; select('#lc-profile').innerHTML = empty('Chưa có nhân viên. Thêm hồ sơ đầu tiên.'); }
        renderEmployees();
    }
    async function chooseEmployee(employeeId) {
        const revision = ++detailRevision;
        const employee = await api(`/employees/detail?id=${employeeId}`);
        if (revision !== detailRevision) return;
        currentEmployee = employee;
        const selectedIndex = filteredEmployees().findIndex(item => item.employeeId === employee.employeeId);
        if (selectedIndex >= 0) employeePage = Math.floor(selectedIndex / employeePageSize) + 1;
        const url = new URL(location.href);
        url.searchParams.set('id', employeeId);
        url.searchParams.set('tab', activeTab);
        history.replaceState(null, '', url);
        renderEmployees();
        await renderProfile();
    }
    function fields(items) {
        return `<div class="lc-fields">${items.map(([label,value]) => `<div><div class="lc-field-label">${escape(vietnamese(label))}</div><div class="lc-field-value">${escape(value == null || value === '' ? 'Chưa có dữ liệu' : vietnamese(value))}</div></div>`).join('')}</div>`;
    }
    async function renderProfile() {
        const employee = currentEmployee;
        const revision = detailRevision;
        select('#lc-profile').innerHTML = `<div class="lc-profile-head"><span class="lc-avatar large">${escape(initials(employee.fullName))}</span>
            <div><h2>${escape(employee.fullName)}</h2><div class="lc-muted">${escape(employee.employeeCode)} · ${escape(employee.positionTitle || 'Chưa có vị trí')}</div><div class="lc-muted">${escape(employee.departmentName || 'Chưa phân công')}</div></div>
            <div class="lc-profile-actions">${badge(employee.status)}<div class="lc-actions" style="margin-top:12px"><button class="small secondary" data-edit>Sửa hồ sơ</button><button class="small danger" data-status>${employee.status === 'Active' ? 'Ngừng làm việc' : 'Kích hoạt'}</button></div></div></div>
            <div class="lc-tabs" role="tablist" aria-label="Hồ sơ nhân viên">${[['info','Thông tin'],['organization','Phân công & Cấp dưới'],['salary','Lương & Phụ cấp'],['history','Quá trình công tác'],['audit','Lịch sử cập nhật'],['documents','Tài liệu']].map(([key,label]) => `<button role="tab" aria-selected="${activeTab === key}" data-profile-tab="${key}" class="${activeTab === key ? 'active' : ''}">${label}</button>`).join('')}</div><div class="lc-panel-body" id="lc-tab-content" role="tabpanel"></div>`;
        const container = select('#lc-tab-content');
        if (activeTab === 'info') {
            container.innerHTML = `<section class="lc-info-section"><h3>Thông tin liên hệ</h3>${fields([['Email',employee.email],['Điện thoại',employee.phone]])}</section>
                <section class="lc-info-section"><h3>Thông tin cá nhân</h3>${fields([['Ngày sinh',employee.dateOfBirth],['Giới tính',employee.gender],['Số CCCD/CMND',employee.identityNumber],['Ngày cấp',employee.identityIssuedDate],['Nơi cấp',employee.identityIssuedPlace],['Địa chỉ thường trú',employee.permanentAddress],['Quê quán',employee.hometown],['Ngành học',employee.major]])}</section>
                <section class="lc-info-section"><h3>Phân công tổ chức</h3>${fields([['Phòng ban',employee.departmentName],['Chức danh hiện tại',employee.positionTitle],['Quản lý trực tiếp',employee.managerName],['Loại hợp đồng',employeeContractLabel(employee)],['Mã hợp đồng',employee.contractCode || 'Chưa có hợp đồng']])}<p><button type="button" class="secondary small" data-profile-tab="organization">Quản lý phân công và cấp dưới →</button></p><a href="${context}/lifecycle/contracts?employeeId=${employee.employeeId}">Xem hợp đồng lao động →</a></section>`;
            return;
        }
        container.innerHTML = '<div class="lc-spinner">Đang tải dữ liệu…</div>';
        try {
            if (activeTab === 'organization') {
                container.classList.add('ea-details','lc-embedded-organization');
                container.innerHTML = '<div id="message" class="message" role="status"></div>' + select('#lc-organization-template').innerHTML;
                if (employee.status === 'Active') container.insertAdjacentHTML('afterbegin','<div class="lc-actions" style="margin-bottom:20px"><button type="button" class="secondary small" data-assign-organization>Phân công / Chuyển chức danh</button></div>');
                const form = container.querySelector('#organizationForm');
                form.dataset.embedded = 'true';
                FormValidation.init(form);
                FormValidation.init(container.querySelector('#manageTeamForm'));
                await setupOrganizationForm(employee);
                if (revision !== detailRevision || activeTab !== 'organization' || !form.isConnected) return;
            } else if (activeTab === 'salary') {
                const [records,allowances] = await Promise.all([
                    api(`/lifecycle/api/compensation?employeeId=${employee.employeeId}`),
                    api(`/lifecycle/api/allowance?employeeId=${employee.employeeId}`)
                ]);
                if (revision !== detailRevision || activeTab !== 'salary') return;
                const effective = records.find(record => record.IsCurrent === 1);
                const allowance=allowances[0];
                container.innerHTML = (effective ? fields([['Lương trước khấu trừ',money(effective.BaseSalary)],['Phụ cấp đi lại',money(effective.TravelAllowance)]]) : empty('Chưa có lương có hiệu lực'))
                    + fields([['Phụ cấp chức vụ (theo cấp trách nhiệm)',allowance?.Amount != null ? money(allowance.Amount) : allowance ? 'Chưa cấu hình phụ cấp có hiệu lực' : 'Chưa có phân công tổ chức có hiệu lực']]);
                if (effective?.SourceContractId) container.innerHTML += `<p class="lc-form-hint">Lương trước khấu trừ được đồng bộ từ <a href="${context}/lifecycle/contracts?employeeId=${employee.employeeId}">hợp đồng ${escape(effective.ContractCode || effective.SourceContractId)}</a>.</p>`;
                container.innerHTML += `<section class="lc-info-section"><div class="lc-heading"><h3>Lịch sử lương</h3><a class="small secondary" href="${context}/lifecycle/contracts?employeeId=${employee.employeeId}">Quản lý trong hợp đồng</a></div>${table(['Hiệu lực từ','Đến ngày','Lương trước khấu trừ','Phụ cấp chức vụ (lịch sử)','Nguồn','Lý do'], records.map(record => [record.EffectiveFrom,record.IsSuperseded ? 'Thay thế trong ngày' : record.EffectiveTo || 'Hiện tại',money(record.BaseSalary),record.PositionAllowance == null ? {html:'<span title="Bản lương này chưa lưu mức phụ cấp tại thời điểm ghi lương; mức hiện hành nằm ở phía trên.">Không có dữ liệu lịch sử</span>'} : money(record.PositionAllowance),record.ContractCode || 'Khởi tạo / Điều chỉnh',vietnamese(record.Reason)]))}</section>`;
            } else if (activeTab === 'history') {
                const records = await api(`/employees/career-history?id=${employee.employeeId}`);
                if (revision !== detailRevision || activeTab !== 'history') return;
                container.innerHTML = records.length ? `<div class="lc-timeline" data-pagination="career-timeline">${records.map(record => `<article class="lc-event"><small>${escape(record.effectiveDate || record.recordedAt)}</small><h3>${escape(vietnamese(record.eventType))} ${badge(record.status)}</h3><p>${escape(record.newValue)}</p><small>${record.reason ? `Lý do: ${escape(vietnamese(record.reason))}<br>` : ''}Người thực hiện: ${escape(historyActorLabel(record.changedBy))}</small></article>`).join('')}</div>` : empty('Chưa có lịch sử công tác');
                if (employee.status === 'Active') container.insertAdjacentHTML('afterbegin','<div class="lc-actions" style="margin-bottom:20px"><button type="button" class="secondary small" data-assign-organization>Phân công / Chuyển chức danh</button></div>');
            } else if (activeTab === 'audit') {
                const audit = await api(`/employees/history?id=${employee.employeeId}`);
                if (revision !== detailRevision || activeTab !== 'audit') return;
                container.innerHTML = '<section><h3>Lịch sử cập nhật</h3>' + table(['Thời điểm','Thao tác','Người thực hiện','Trước thay đổi','Sau thay đổi'],audit.map(record => [record.createdAt,historyActionLabel(record.action),historyActorLabel(record.changedBy),{html:formatHistoryValue(record.oldValue)},{html:formatHistoryValue(record.newValue)}])) + '</section>';
            } else {
                const records = await api(`/documents?employeeId=${employee.employeeId}`);
                if (revision !== detailRevision || activeTab !== 'documents') return;
                container.innerHTML = `<form id="lc-upload" class="lc-form-grid"><input type="hidden" name="employeeId" value="${employee.employeeId}"><label>Tên tài liệu *<input name="documentName" required maxlength="255"></label><label>Loại tài liệu<input name="documentType" maxlength="100"></label><label class="full">Tệp PDF, Word, JPG, PNG (tối đa 10 MB)<input type="file" name="file" accept=".pdf,.doc,.docx,.jpg,.png" required></label><div><button type="submit">↑ Tải tài liệu lên</button></div></form><div class="lc-info-section" style="margin-top:24px">${table(['Tài liệu','Ngày tải','Trạng thái','Thao tác'], records.map(record => [record.documentName,record.uploadedDate,vietnamese(record.status),{html:`${record.status === 'Active' ? `<a href="${context}/documents/download?id=${record.documentId}">Tải xuống</a>` : ''} <button type="button" class="small secondary" data-document="${record.documentId}" data-document-action="${record.status === 'Active' ? 'deactivate' : 'activate'}">${record.status === 'Active' ? 'Lưu trữ' : 'Khôi phục'}</button>`}]))}</div>`;
            }
        } catch (exception) { if (revision === detailRevision && container.isConnected) container.innerHTML = empty(exception.message); }
    }
    function employeeCodeFields(form) {
        const editing = Boolean(form.elements.employeeId.value);
        form.elements.employeeCode.disabled = editing;
        form.elements.employeeCode.required = !editing;
        form.elements.employeeCode.placeholder = 'Nhập mã đang dùng tại công ty';
        FormValidation.setError(form, 'employeeCode', '');
    }
    async function employeeModal(editing) {
        const form = select('#lc-employee-form');
        form.reset();
        for (const element of form.elements) element.disabled = false;
        select('#lc-form-error').textContent = '';
        select('#lc-modal-title').textContent = editing ? 'Chỉnh sửa hồ sơ' : 'Thêm nhân viên';
        select('#lc-save').textContent = editing ? 'Lưu thay đổi' : 'Thêm nhân viên';
        if (editing) for (const [key,value] of Object.entries(currentEmployee)) if (form.elements.namedItem(key)) form.elements.namedItem(key).value = value ?? '';
        if (editing) {
            ['employeeCode','status'].forEach(name => { form.elements[name].disabled = true; });
        }
        employeeCodeFields(form);
        const today = new Date().toLocaleDateString('en-CA');
        form.elements.dateOfBirth.max = today;
        form.elements.identityIssuedDate.max = today;
        form.elements.identityNumber.required = !editing;
        select('#lc-employee-modal').showModal();
    }
    async function initEmployees() {
        bindActionForm();
        departments = await api('/departments');
        select('#lc-department-filter').innerHTML += departments.map(department => option(department.departmentId,department.departmentName)).join('');
        const resetEmployeePage = () => { employeePage = 1; renderEmployees(); select('#lc-employees').scrollTop = 0; };
        select('#lc-search').addEventListener('input',resetEmployeePage);
        select('#lc-department-filter').addEventListener('change',resetEmployeePage);
        select('#lc-employees').addEventListener('click',event => { const target = event.target.closest('[data-employee]'); if (target) chooseEmployee(Number(target.dataset.employee)).catch(error); });
        select('#lc-add').addEventListener('click',() => employeeModal(false).catch(error));
        select('#lc-export-employees')?.addEventListener('click', () => {
            const dept = select('#lc-department-filter')?.value || '';
            const q = select('#lc-search')?.value || '';
            const params = new URLSearchParams();
            if (dept) params.set('departmentId', dept);
            if (q) params.set('keyword', q);
            window.location.href = `${context}/employees/export?${params.toString()}`;
        });
        select('#lc-employee-form').addEventListener('submit',async event => {
            event.preventDefault();
            const form = event.currentTarget;
            if (!FormValidation.validate(form)) return;
            const values = new FormData(form);
            const editing = Boolean(values.get('employeeId'));
            if (editing) ['employeeCode','departmentId','positionId','status'].forEach(name => values.set(name,currentEmployee[name] ?? ''));
            const button = select('#lc-save'); button.disabled = true;
            try {
                const employee = await api(editing ? '/employees/update' : '/employees/create',values);
                select('#lc-employee-modal').close();
                select('#lc-search').value = ''; select('#lc-department-filter').value = '';
                await loadEmployees(employee.employeeId);
                toast(editing ? 'Đã cập nhật hồ sơ' : 'Đã thêm nhân viên');
            } catch (exception) {
                select('#lc-form-error').textContent = FormValidation.showErrors(form,exception) ? '' : exception.message;
            } finally { button.disabled = false; }
        });
        select('#lc-profile').addEventListener('click',async event => {
            try {
                const tab = event.target.closest('[data-profile-tab]');
                if (tab) {
                    activeTab = tab.dataset.profileTab;
                    const url = new URL(location.href);
                    url.searchParams.set('tab',activeTab);
                    history.replaceState(null,'',url);
                    await renderProfile();
                }
                if (event.target.closest('[data-edit]')) await employeeModal(true);
                if (event.target.closest('[data-assign-organization]')) await organizationAssignmentModal();
                if (event.target.closest('[data-status]')) {
                    if (currentEmployee.status === 'Active') {
                        const reason = window.prompt('Lý do ngừng làm việc (sẽ kết thúc phân công, xử lý hợp đồng và khóa tài khoản):');
                        if (!reason?.trim()) return;
                        await api('/lifecycle/api/terminate',{employeeId:currentEmployee.employeeId,reason});
                    } else {
                        if (!window.confirm("Kích hoạt lại hồ sơ? Tài khoản nhân viên và phân công được quản lý riêng.")) return;
                        await api('/employees/activate',{employeeId:currentEmployee.employeeId});
                    }
                    await loadEmployees(currentEmployee.employeeId); toast('Đã cập nhật trạng thái');
                }
                const documentButton = event.target.closest('[data-document]');
                if (documentButton) { await api(`/documents/${documentButton.dataset.documentAction}`,{documentId:documentButton.dataset.document}); await renderProfile(); toast('Đã cập nhật tài liệu'); }
            } catch (exception) { error(exception); }
        });
        select('#lc-profile').addEventListener('submit',async event => {
            if (event.target.id !== 'lc-upload') return;
            event.preventDefault();
            const button = event.target.querySelector('button'); button.disabled = true;
            try {
                const values = new FormData(event.target);
                const file = values.get('file');
                if (file.size>10*1024*1024) throw new Error('Tệp không được vượt quá 10 MB.');
                await api('/documents/upload',values); await renderProfile(); toast('Đã tải tài liệu lên');
            } catch (exception) { error(exception); button.disabled = false; }
        });
        document.addEventListener('employee-organization-updated', async event => {
            if (Number(event.detail.employeeId) !== currentEmployee?.employeeId) return;
            try { await loadEmployees(currentEmployee.employeeId); toast('Đã cập nhật phân công'); } catch (exception) { error(exception); }
        });
        const requested = new URLSearchParams(location.search).get('id');
        await loadEmployees(requested);
        if (new URLSearchParams(location.search).get('add')==='1') await employeeModal(false);
        if (location.pathname.endsWith('/employee-form')) await employeeModal(Boolean(requested));
    }
    function table(headers, rows, paginationKey = headers.join('|')) {
        if (!rows.length) return empty('Chưa có dữ liệu');
        return `<div class="lc-table-wrap"><table data-pagination="${escape(paginationKey)}"><thead><tr>${headers.map(header => `<th>${escape(header)}</th>`).join('')}</tr></thead><tbody>${rows.map(row => `<tr>${row.map(value => `<td>${value && typeof value === 'object' && 'html' in value ? value.html : escape(value ?? '—')}</td>`).join('')}</tr>`).join('')}</tbody></table></div>`;
    }
    let actionHandler;
    function bindActionForm() {
        select('#lc-action-form').addEventListener('submit',async event => {
            event.preventDefault();
            if (!actionHandler) return;
            const form = event.currentTarget;
            const button = select('#lc-action-save'); button.disabled = true;
            try { await actionHandler(new FormData(form)); select('#lc-action-modal').close(); }
            catch (exception) { select('#lc-action-error').textContent = FormValidation.showErrors(form,exception) ? '' : exception.message; }
            finally { button.disabled = false; }
        });
    }
    async function organizationAssignmentModal() {
        const [positions,levels] = await Promise.all([api('/positions?activeOnly=true'),api('/responsibility-levels')]);
        const availableForAssignment=position=>{
            if (String(position.positionId)===String(currentEmployee.positionId)) return true;
            const level=levels.find(item=>String(item.responsibilityLevelId)===String(position.responsibilityLevelId));
            const employee=['EMP','EMPLOYEE'].includes(String(level?.levelCode || '').trim().toUpperCase())
                || String(level?.levelName || '').trim().toUpperCase()==='EMPLOYEE';
            const manager=!employee && (position.canManageEmployees===true || String(level?.levelCode || '').trim().toUpperCase()==='MANAGER'
                || String(level?.levelName || '').trim().toUpperCase()==='MANAGER');
            return !manager || position.assignmentOccupied!==true;
        };
        const fieldsHtml = '<label class="full">Phòng ban *<select name="departmentId" required>'
            + option('','Chọn phòng ban') + departments.filter(item=>item.isActive!==false).map(item=>option(item.departmentId,item.departmentName)).join('')+'</select></label>'
            + '<label class="full">Chức danh *<select name="positionId" required></select></label>'
            + '<div class="full"><label>Vai trò theo chức danh</label><span data-assignment-role class="lc-badge"></span></div>'
            + '<label class="full">Quản lý trực tiếp<select name="managerId"></select></label>'
            + '<p class="full lc-muted">Lưu sẽ áp dụng ngay, đồng bộ vai trò Employee/Manager và giữ lịch sử. Đề xuất cũ chưa áp dụng (nếu có) sẽ được ghi nhận là đã bị thay thế. Tài khoản Admin Company/HR được giữ nguyên quyền.</p>';
        actionModal('Phân công / Chuyển chức danh',fieldsHtml,async values=>{
            values.set('employeeId',currentEmployee.employeeId);
            values.set('managerAction',values.get('managerId')?'ASSIGN':'REMOVE');
            await api('/employees/assign-organization',values);
            await loadEmployees(currentEmployee.employeeId);
            toast('Đã cập nhật phân công và vai trò theo chức danh.');
        });
        const form=select('#lc-action-form');
        let departmentRevision=0;
        let availableManagers=[];
        const syncPosition=()=>{
            const position=positions.find(item=>String(item.positionId)===form.elements.positionId.value);
            const level=levels.find(item=>String(item.responsibilityLevelId)===String(position?.responsibilityLevelId));
            const employeeLevel=['EMP','EMPLOYEE'].includes(String(level?.levelCode || '').trim().toUpperCase())
                || String(level?.levelName || '').trim().toUpperCase()==='EMPLOYEE';
            const manager=position && !employeeLevel && (position.canManageEmployees===true
                || String(level?.levelCode || '').trim().toUpperCase()==='MANAGER'
                || String(level?.levelName || '').trim().toUpperCase()==='MANAGER');
            form.querySelector('[data-assignment-role]').textContent=position?(manager?'Manager':'Employee'):'Chưa chọn chức danh';
            form.elements.managerId.replaceChildren(new Option('Không có quản lý trực tiếp',''));
            const matching=position?.reportsToPositionId == null ? [] : availableManagers.filter(member=>String(member.positionId)===String(position.reportsToPositionId));
            if (!manager) for (const member of matching)
                form.elements.managerId.add(new Option(member.fullName+' · '+member.positionTitle,member.employeeId));
            form.elements.managerId.disabled=Boolean(manager);
            if (!manager && matching.some(member=>member.employeeId===currentEmployee.managerId))
                form.elements.managerId.value=currentEmployee.managerId;
        };
        const syncDepartment=async()=>{
            const revision=++departmentRevision;
            select('#lc-action-save').disabled=true;
            form.elements.positionId.innerHTML=option('','Chọn chức danh')+positions.filter(item=>String(item.departmentId)===form.elements.departmentId.value && availableForAssignment(item))
                .map(item=>option(item.positionId,item.positionTitle+' · '+item.responsibilityLevelName)).join('');
            if (String(currentEmployee.departmentId)===form.elements.departmentId.value)
                form.elements.positionId.value=currentEmployee.positionId || '';
            availableManagers=[]; syncPosition();
            try {
                const members=form.elements.departmentId.value?await api('/departments/members?departmentId='+form.elements.departmentId.value):[];
                if (revision!==departmentRevision) return;
                availableManagers=members.filter(member=>member.canManageTeam===true && member.employeeId!==currentEmployee.employeeId
                    && String(member.departmentId)===form.elements.departmentId.value);
                syncPosition();
                select('#lc-action-error').textContent='';
            } catch(exception) {
                if (revision===departmentRevision) select('#lc-action-error').textContent=exception.message;
                return;
            }
            if (revision===departmentRevision) select('#lc-action-save').disabled=false;
        };
        form.elements.departmentId.value=currentEmployee.departmentId || '';
        form.elements.departmentId.addEventListener('change',syncDepartment);
        form.elements.positionId.addEventListener('change',syncPosition);
        await syncDepartment();
    }
    function actionModal(title, fieldsHtml, handler) {
        select('#lc-action-title').textContent = title;
        select('#lc-action-fields').innerHTML = fieldsHtml;
        FormValidation.init(select('#lc-action-form'));
        select('#lc-action-error').textContent = '';
        select('#lc-action-save').hidden = !handler;
        select('#lc-action-save').disabled = false;
        select('#lc-action-save').textContent = 'Lưu';
        actionHandler = handler;
        select('#lc-action-modal').showModal();
    }
    function input(label, name, type, value, required = false) {
        return `<label>${escape(label)}<input name="${name}" type="${type}" value="${escape(value ?? '')}" ${required ? 'required' : ''} ${type === 'number' ? 'min="0" step="0.01"' : ''}></label>`;
    }
    let records = [];
    let contractEmployeeId = null;
    const contractTypes = option('PROBATION','Thử việc') + option('FIXED_TERM','Có thời hạn') + option('INDEFINITE','Không xác định thời hạn');
    async function contractModal(record, renewal = false, signedExisting = false) {
        const employeeOptions = employees.filter(employee => signedExisting || employee.status === 'Active' || employee.employeeId === record?.EmployeeId).map(employee => option(employee.employeeId,`${employee.fullName} · ${employee.employeeCode}`)).join('');
        const fieldsHtml = `<label class="full">Nhân viên *<select name="employeeId" required>${option('','Chọn nhân viên')}${employeeOptions}</select></label>
            ${input('Mã hợp đồng *','contractCode','text',renewal ? '' : record?.ContractCode,true)}<label>Loại hợp đồng *<select name="contractType">${contractTypes}</select></label>
            ${input('Ngày bắt đầu *','startDate','date',renewal && record?.EndDate ? nextDay(record.EndDate) : record?.StartDate,true)}${input('Ngày kết thúc','endDate','date',renewal ? '' : record?.EndDate)}
            <h3 class="full">Lương &amp; Phụ cấp theo hợp đồng</h3>${input('Lương trước khấu trừ *','grossSalary','number',record?.GrossSalary,true)}${input('Phụ cấp đi lại','travelAllowance','number',record ? record.TravelAllowance : '0')}<p class="full lc-form-hint">Phụ cấp chức vụ được quản lý riêng theo chính sách cấp trách nhiệm, không nhập trong hợp đồng. Phụ cấp đi lại bằng 0 nghĩa là không có; để trống sẽ giữ mức đang có khi đồng bộ lương (dành cho hợp đồng cũ chưa khai báo).</p>${signedExisting ? `${input('Ngày ký thực tế *','signedDate','date','',true)}<label class="full">Xử lý lương<select name="synchronizeSalary">${option('false','Giữ nguyên lịch sử lương hiện có (khuyến nghị)')}${option('true','Đồng bộ lương từ ngày bắt đầu hợp đồng')}</select></label><p class="full lc-form-hint">Ghi nhận hợp đồng đã ký ngoài hệ thống, không phải ký mới hay gia hạn. Trạng thái tự xác định theo thời hạn. Mặc định chỉ lưu lương và phụ cấp đi lại trên hợp đồng, không sửa lịch sử lương. Chỉ chọn đồng bộ khi cần khởi tạo lương: lịch lương phát sinh sau đó hoặc kỳ lương đã duyệt sẽ chặn thao tác. Hợp đồng hết hạn không được đồng bộ lương. Nếu hợp đồng đã có trên hệ thống, hãy dùng bản ghi hiện có. Bản scan có thể tải tại tab Tài liệu của nhân viên.</p>` : ''}<label class="full">Ghi chú<textarea name="notes" maxlength="1000">${escape(renewal ? `Gia hạn hợp đồng ${record.ContractCode}` : record?.Notes || '')}</textarea></label>`;
        actionModal(signedExisting ? 'Ghi nhận hợp đồng đã ký' : renewal ? 'Gia hạn bằng hợp đồng mới' : record ? 'Chỉnh sửa hợp đồng nháp' : 'Tạo hợp đồng', fieldsHtml, async values => {
            if (record && !renewal) { values.set('contractId',record.ContractId); values.set('version',record.Version); }
            values.set('creationMode',signedExisting ? 'SIGNED_EXISTING' : 'DRAFT');
            await api('/lifecycle/api/contracts/save',values);
            await refreshResource();
            toast(signedExisting ? 'Đã ghi nhận hợp đồng đã ký theo thời hạn thực tế' : 'Đã lưu hợp đồng nháp; Lương trước khấu trừ sẽ đồng bộ khi kích hoạt');
        });
        select('#lc-action-form').elements.employeeId.value = record?.EmployeeId || '';
        select('#lc-action-form').elements.contractType.value = record?.ContractType || 'FIXED_TERM';
        select('#lc-action-form').elements.contractCode.maxLength = 60;
        if (signedExisting) {
            const today = new Date();
            const dateLimit = `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}-${String(today.getDate()).padStart(2,'0')}`;
            select('#lc-action-form').elements.startDate.max = dateLimit;
            select('#lc-action-form').elements.signedDate.max = dateLimit;
        }
    }
    function nextDay(value) { const date = new Date(`${value}T12:00:00Z`); date.setUTCDate(date.getUTCDate()+1); return date.toISOString().slice(0,10); }
    function filteredRecords() {
        const query = (select('#lc-resource-search')?.value || '').toLocaleLowerCase('vi');
        const status = select('#lc-resource-status')?.value || '';
        const type = select('#lc-resource-type')?.value || '';
        const employeeId = new URLSearchParams(location.search).get('employeeId');
        return records.filter(record => JSON.stringify(record).toLocaleLowerCase('vi').includes(query)
            && (!status || (record.DisplayStatus || accountStatus(record)) === status)
            && (!type || record.ContractType === type) && (!employeeId || String(record.EmployeeId) === employeeId));
    }
    function accountStatus(record) { return record.UserId == null ? 'NONE' : record.AdministrativeIsActive === false ? 'REVOKED' : record.ContractAccessAllowed === false ? 'INACTIVE' : !record.IsActive ? 'REVOKED' : record.IsLocked ? 'LOCKED' : 'ACTIVE'; }
    function renderContractEmployees(visible) {
        const matchingEmployees = new Set(visible.map(record => String(record.EmployeeId)));
        const groups = new Map();
        for (const record of records) {
            const employeeId = String(record.EmployeeId);
            if (!matchingEmployees.has(employeeId)) continue;
            if (!groups.has(employeeId)) groups.set(employeeId,[]);
            groups.get(employeeId).push(record);
        }
        select('#lc-resource-table').innerHTML = groups.size ? table(['Mã nhân viên','Nhân viên','Số hợp đồng','Trạng thái các hợp đồng','Thao tác'],[...groups.values()].map(contracts => {
            const employee = contracts[0];
            const statuses = [...new Set(contracts.map(record => record.DisplayStatus))];
            return [employee.EmployeeCode,employee.FullName,contracts.length,
                {html:statuses.map(status => badge(status)).join(' ') + (contracts.some(record => record.Expiring) ? ' <span class="lc-badge warning">Sắp hết hạn</span>' : '')},
                {html:`<button type="button" class="small secondary" data-contract-employee="${escape(employee.EmployeeId)}">Xem hợp đồng</button>`}];
        }),'contract-employees') : empty('Không có nhân viên có hợp đồng phù hợp');
    }
    function renderEmployeeContracts() {
        const contracts = records.filter(record => String(record.EmployeeId) === contractEmployeeId);
        const employee = contracts[0];
        select('#lc-employee-contracts-title').textContent = employee ? `Hợp đồng · ${employee.FullName}` : 'Hợp đồng của nhân viên';
        select('#lc-employee-contracts-summary').textContent = employee ? `${employee.EmployeeCode} · ${contracts.length} hợp đồng · Toàn bộ lịch sử đã ghi nhận` : 'Không có hợp đồng';
        select('#lc-employee-contracts-list').innerHTML = table(['Mã hợp đồng','Loại','Thời hạn','Lương trước khấu trừ','Phụ cấp đi lại','Trạng thái','Thao tác'],contracts.map(record => [
            record.ContractCode,vietnamese(record.ContractType),`${record.StartDate} → ${record.EndDate || 'Không thời hạn'}`,money(record.GrossSalary),record.TravelAllowance == null ? 'Chưa khai báo' : money(record.TravelAllowance),{html:badge(record.DisplayStatus)+(record.Expiring ? ' <span class="lc-badge warning">Sắp hết hạn</span>' : '')},
            {html:`<div class="lc-actions"><button class="small secondary" data-contract="${record.ContractId}" data-action="view">Chi tiết</button>${record.Status === 'DRAFT' ? `<button class="small secondary" data-contract="${record.ContractId}" data-action="edit">Sửa</button><button class="small" data-contract="${record.ContractId}" data-action="activate">Kích hoạt</button>` : `<button class="small secondary" data-contract="${record.ContractId}" data-action="renew">Gia hạn</button>`}${record.DisplayStatus === 'ACTIVE' ? `<button class="small danger" data-contract="${record.ContractId}" data-action="terminate">Chấm dứt</button>` : ''}</div>`}
        ]),`employee-contracts-${contractEmployeeId}`);
    }
    function openEmployeeContracts(employeeId) {
        contractEmployeeId = String(employeeId);
        renderEmployeeContracts();
        select('#lc-employee-contracts').showModal();
    }
    function renderResourceTable() {
        const visible = filteredRecords();
        if (page === 'contracts') renderContractEmployees(visible);
        else select('#lc-resource-table').innerHTML = table(['Nhân viên','Chức danh','Email đăng nhập','Vai trò','Trạng thái','Thao tác'],visible.map(record => [record.FullName,record.PositionTitle || 'Chưa phân công',record.Email,vietnamese(record.RoleCode),{html:badge(accountStatus(record))+(record.ContractAccessAllowed === false ? '<small class="lc-form-hint">Hợp đồng đã kết thúc; chờ hợp đồng mới có hiệu lực.</small>' : '')},{html:record.UserId == null ? `<button class="small" data-account="${record.EmployeeId}" data-action="create">Tạo tài khoản</button>` : `<div class="lc-actions">${[['lock','Khóa'],['unlock','Mở khóa'],['reset','Đặt lại mật khẩu'],['revoke','Thu hồi']].map(([action,label]) => `<button class="small secondary" data-account="${record.EmployeeId}" data-action="${action}">${label}</button>`).join('')}</div>`}]));
    }
    async function refreshResource() {
        records = await api(`/lifecycle/api/${page}`);
        renderResourceTable();
        if (page === 'contracts' && select('#lc-employee-contracts').open) renderEmployeeContracts();
    }
    async function accountModal(record, action) {
        const content = `<div class="full">${escape(record.FullName)} · ${escape(record.EmployeeCode)}<br><small>Chức danh: ${escape(record.PositionTitle || 'Chưa phân công')}</small></div>`
            + (action === 'create' ? '<div class="full"><label>Email đăng nhập</label><div class="lc-field-value">' + escape(record.ContactEmail || 'Chưa có email liên hệ') + '</div></div><p class="full lc-form-hint">Tài khoản dùng email liên hệ đã lưu trong hồ sơ, không tự sinh tên đăng nhập. Cần bổ sung email và mã định danh trước khi cấp tài khoản. Mật khẩu ban đầu là mã định danh cá nhân; nhân viên bắt buộc đổi ở lần đăng nhập đầu.</p>' : '')
            + (action === 'create' ? '<div class="full"><label>Vai trò theo phân công</label><div class="lc-field-value">'+escape(vietnamese(record.AssignmentRoleCode || 'EMPLOYEE'))+'</div></div><p class="full lc-form-hint">Vai trò Employee/Manager được đồng bộ tại Phòng ban khi phân công chức danh.</p>' : '<p class="full lc-muted">Xác nhận thay đổi quyền truy cập hệ thống của nhân viên này.</p>');
        actionModal(action === 'create' ? 'Cấp tài khoản nhân viên' : "Tài khoản nhân viên · "+action,content,async values => {
            values.set('employeeId',record.EmployeeId); values.set('action',action);
            const result = await api('/lifecycle/api/accounts/action',values);
            await refreshResource();
            if (result.temporaryPassword) {
                const saved=records.find(account => account.EmployeeId===record.EmployeeId);
                setTimeout(() => actionModal('Mật khẩu tạm thời',`<p class="full">Email đăng nhập: <strong>${escape(saved?.Email)}</strong></p><p class="full">Gửi mật khẩu qua kênh riêng cho nhân viên. Mật khẩu chỉ hiển thị lần này và phải đổi ở lần đăng nhập đầu.</p><div class="lc-password-copy full"><div class="lc-password">${escape(result.temporaryPassword)}</div><button type="button" class="secondary" data-copy-password aria-label="Sao chép mật khẩu tạm thời">Sao chép</button></div><small class="lc-copy-status full" role="status" aria-live="polite"></small>`,null),0);
            } else toast(result.message || 'Đã cập nhật tài khoản nhân viên');
        });
        if (action === 'create') select('#lc-action-save').textContent = 'Cấp tài khoản';
    }
    async function initResource() {
        const labels = {contracts:['Hợp đồng lao động','Theo dõi hợp đồng và thời hạn ký kết'],accounts:['Tài khoản nhân viên','Quản lý quyền truy cập hệ thống của nhân viên'],organization:['Sơ đồ tổ chức','Cơ cấu từ các phân công có hiệu lực'],dashboard:['Bảng điều khiển','Tổng quan nhân sự & lương · '+new Intl.DateTimeFormat('vi-VN',{weekday:'long',day:'2-digit',month:'2-digit',year:'numeric'}).format(new Date())]};
        select('#lc-page-title').textContent = labels[page][0]; select('#lc-page-label').textContent = labels[page][0]; select('#lc-page-subtitle').textContent = labels[page][1];
        if (page === 'dashboard') { await renderDashboard(); return; }
        if (page === 'organization') { await renderOrganization(); return; }
        employees = await api('/employees');
        select('#lc-page-content').innerHTML = `<section class="lc-card"><div class="lc-filterbar"><input type="search" id="lc-resource-search" placeholder="Tìm nhân viên, mã, email…" aria-label="Tìm kiếm"><select id="lc-resource-status" aria-label="Trạng thái">${option('','Tất cả trạng thái')}${(page === 'contracts' ? ['DRAFT','ACTIVE','EXPIRED','TERMINATED','CANCELLED'] : ['NONE','ACTIVE','INACTIVE','LOCKED','REVOKED']).map(status => option(status,status)).join('')}</select>${page === 'contracts' ? `<select id="lc-resource-type" aria-label="Loại hợp đồng">${option('','Tất cả loại')}${contractTypes}</select>` : ''}</div><div id="lc-resource-table"></div></section>`;
        select('#lc-resource-search').addEventListener('input',renderResourceTable); select('#lc-resource-status').addEventListener('change',renderResourceTable); select('#lc-resource-type')?.addEventListener('change',renderResourceTable);
        if (page === 'contracts') {
            select('#lc-page-add').hidden = false;
            select('#lc-page-add').textContent = 'Tạo hợp đồng mới';
            select('#lc-page-add').addEventListener('click',() => contractModal(null).catch(error));
            if (select('#lc-page-export')) {
                select('#lc-page-export').hidden = false;
                select('#lc-page-export').addEventListener('click', () => {
                    const status = select('#lc-resource-status')?.value || '';
                    const type = select('#lc-resource-type')?.value || '';
                    const q = select('#lc-resource-search')?.value || '';
                    const requestedEmployeeId = new URLSearchParams(location.search).get('employeeId') || '';
                    const params = new URLSearchParams();
                    if (status) params.set('status', status);
                    if (type) params.set('type', type);
                    if (q) params.set('keyword', q);
                    if (requestedEmployeeId) params.set('employeeId', requestedEmployeeId);
                    window.location.href = `${context}/lifecycle/contracts/export?${params.toString()}`;
                });
            }
            const signedButton = document.createElement('button');
            signedButton.type = 'button'; signedButton.className = 'secondary';
            signedButton.textContent = 'Ghi nhận hợp đồng đã ký';
            signedButton.addEventListener('click',() => contractModal(null,false,true).catch(error));
            select('.lc-filterbar').append(signedButton);
        }
        bindActionForm();
        const handleResourceClick = async event => {
            try {
                const employeeButton = event.target.closest('[data-contract-employee]');
                if (employeeButton) { openEmployeeContracts(employeeButton.dataset.contractEmployee); return; }
                const contractButton = event.target.closest('[data-contract]');
                if (contractButton) {
                    const record = records.find(item => item.ContractId === Number(contractButton.dataset.contract));
                    const action = contractButton.dataset.action;
                    if (action === 'edit' || action === 'renew') await contractModal(record,action === 'renew');
                    else if (action === 'view') actionModal('Chi tiết hợp đồng',`<div class="full">${fields([['Nhân viên',record.FullName],['Mã nhân viên',record.EmployeeCode],['Mã hợp đồng',record.ContractCode],['Loại hợp đồng',vietnamese(record.ContractType)],['Ngày bắt đầu',record.StartDate],['Ngày kết thúc',record.EndDate || 'Không xác định thời hạn'],['Lương trước khấu trừ',money(record.GrossSalary)],['Phụ cấp đi lại',record.TravelAllowance == null ? 'Chưa khai báo' : money(record.TravelAllowance)],['Trạng thái',vietnamese(record.DisplayStatus)],['Ngày ghi nhận ký kết',record.SignedDate],['Ngày chấm dứt',record.TerminatedDate],['Lý do chấm dứt',record.TerminationReason],['Ghi chú',record.Notes]])}</div>`,null);
                    else actionModal(action === 'activate' ? 'Kích hoạt hợp đồng' : 'Chấm dứt hợp đồng',`<p class="full">${escape(record.ContractCode)} · ${escape(record.FullName)}</p>${action === 'terminate' ? '<label class="full">Lý do *<textarea name="reason" maxlength="500" required></textarea></label>' : '<p class="full lc-muted">Kích hoạt sẽ lưu lịch sử ký kết và đồng bộ lương trước khấu trừ từ ngày bắt đầu hợp đồng. Lương cũ được giữ trong lịch sử; kỳ lương đã duyệt hoặc lịch lương tương lai xung đột sẽ chặn thao tác.</p>'}`,async values => {
                        values.set('contractId',record.ContractId); values.set('action',action); values.set('version',record.Version);
                        await api('/lifecycle/api/contracts/transition',values); await refreshResource(); toast('Đã cập nhật hợp đồng');
                    });
                }
                const accountButton = event.target.closest('[data-account]');
                if (accountButton) await accountModal(records.find(record => record.EmployeeId === Number(accountButton.dataset.account)),accountButton.dataset.action);
            } catch (exception) { error(exception); }
        };
        select('#lc-page-content').addEventListener('click',handleResourceClick);
        if (page === 'contracts') select('#lc-employee-contracts-list').addEventListener('click',handleResourceClick);
        await refreshResource();
        const requestedEmployeeId = new URLSearchParams(location.search).get('employeeId');
        if (page === 'contracts' && requestedEmployeeId && records.some(record => String(record.EmployeeId) === requestedEmployeeId)) openEmployeeContracts(requestedEmployeeId);
    }
    async function renderOrganization() {
        const [assignments,profiles,administrators]=await Promise.all([api('/lifecycle/api/organization'),api('/employees'),api('/lifecycle/api/organization-admins')]);
        const members=globalThis.HrmOrganizationExplorer.combine(assignments,profiles);
        globalThis.HrmOrganizationExplorer.mount(select('#lc-page-content'),members,context,administrators);
        select('#lc-page-title').textContent='Cơ cấu tổ chức ('+members.length+')';
        select('#lc-page-subtitle').textContent='Danh sách · Danh bạ · Sơ đồ tổ chức — dữ liệu phân công có hiệu lực';
    }
    async function renderDashboard() {
        const data = await api('/lifecycle/api/dashboard');
        select('#lc-page-content').innerHTML = globalThis.HrmDashboard.render(data,context);
    }
    (page === 'employees' ? initEmployees() : initResource()).catch(error);
})();
