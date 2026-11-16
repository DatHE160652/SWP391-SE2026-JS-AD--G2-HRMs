const updatePeriodApi = document.body.dataset.periodType === 'DOCUMENT' ? '/employees/document-update-periods' : '/employees/profile-update-periods';
const documentUpdatePeriods = document.body.dataset.periodType === 'DOCUMENT';

let cachedDocumentTypes = [];
let cachedProfileFields = [];
let cachedCompanyEmployees = [];
let selectedEmployeeIds = new Set();
let selectedDocTypeIds = new Set();
let selectedProfileFieldKeys = new Set();
let currentDetailData = null;
let currentEmpFilter = 'ALL';

document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('#profileUpdatePeriodForm');
    setDefaultProfilePeriodDates(form);
    setupPeriodExtensions(form);
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
        if (documentUpdatePeriods) {
            if (selectedDocTypeIds.size === 0) {
                showMessage('Vui lòng chọn ít nhất 1 loại tài liệu cần cập nhật trong đợt này.', 'error');
                return;
            }
        } else {
            if (selectedProfileFieldKeys.size === 0) {
                showMessage('Vui lòng chọn ít nhất 1 loại thông tin hồ sơ được phép cập nhật trong đợt này.', 'error');
                return;
            }
        }
        const scopeType = form.querySelector('input[name="scopeType"]:checked')?.value || 'ALL';
        if (scopeType === 'SELECTED' && selectedEmployeeIds.size === 0) {
            showMessage('Vui lòng chọn ít nhất 1 nhân viên cho đợt cập nhật cá nhân.', 'error');
            return;
        }
        const button = document.querySelector('#createProfilePeriodButton');
        button.disabled = true;
        try {
            const data = Object.fromEntries(new FormData(form).entries());
            data.startsAt = new Date(data.startsAt).toISOString();
            data.endsAt = new Date(data.endsAt).toISOString();
            data.scopeType = scopeType;
            data.employeeIds = scopeType === 'SELECTED' ? Array.from(selectedEmployeeIds).join(',') : '';
            if (documentUpdatePeriods) {
                data.documentTypeIds = Array.from(selectedDocTypeIds).join(',');
            } else {
                data.allowedFields = Array.from(selectedProfileFieldKeys).join(',');
            }
            const payload = await EmployeeApi.post(updatePeriodApi + '/create', data);
            showMessage(payload.message);
            form.reset();
            setDefaultProfilePeriodDates(form);
            selectedEmployeeIds.clear();
            document.querySelector('#selectedEmployeesPicker')?.classList.add('hidden');
            if (documentUpdatePeriods) {
                selectedDocTypeIds = new Set(cachedDocumentTypes.map(t => Number(t.documentTypeId)));
            } else {
                selectedProfileFieldKeys = new Set(cachedProfileFields.filter(f => f.defaultChecked).map(f => f.fieldKey));
            }
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

function setupPeriodExtensions(form) {
    if (documentUpdatePeriods) {
        const input = document.querySelector('#newDocumentTypeName');
        const errorEl = document.querySelector('#newDocumentTypeError');
        const addBtn = document.querySelector('#addDocumentTypeBtn');

        const submitAddDocumentType = async () => {
            const typeName = input?.value?.trim() || '';
            if (errorEl) errorEl.textContent = '';
            if (!typeName) {
                if (errorEl) errorEl.textContent = 'Vui lòng nhập tên loại tài liệu mới.';
                input?.focus();
                return;
            }
            if (addBtn) addBtn.disabled = true;
            try {
                const payload = await EmployeeApi.post(updatePeriodApi + '/create', {
                    action: 'addType',
                    typeName,
                    csrfToken: form.elements.namedItem('csrfToken').value
                });
                input.value = '';
                if (errorEl) errorEl.textContent = '';
                showMessage(payload.message);
                if (payload.data?.documentTypeId) {
                    selectedDocTypeIds.add(Number(payload.data.documentTypeId));
                }
                await loadProfileUpdatePeriods();
            } catch (error) {
                if (errorEl) errorEl.textContent = error.message;
                else showMessage(error.message, 'error');
            } finally {
                if (addBtn) addBtn.disabled = false;
            }
        };

        input?.addEventListener('input', () => {
            if (errorEl) errorEl.textContent = '';
        });
        input?.addEventListener('keydown', event => {
            if (event.key === 'Enter') {
                event.preventDefault();
                submitAddDocumentType();
            }
        });
        addBtn?.addEventListener('click', submitAddDocumentType);
    }

    document.querySelector('#selectAllDocTypesBtn')?.addEventListener('click', () => {
        if (documentUpdatePeriods) {
            selectedDocTypeIds = new Set(cachedDocumentTypes.map(t => Number(t.documentTypeId)));
            renderDocumentTypesSelector();
        } else {
            selectedProfileFieldKeys = new Set(cachedProfileFields.map(f => f.fieldKey));
            renderProfileFieldsSelector();
        }
    });
    document.querySelector('#selectDefaultProfileFieldsBtn')?.addEventListener('click', () => {
        selectedProfileFieldKeys = new Set(cachedProfileFields.filter(f => f.defaultChecked).map(f => f.fieldKey));
        renderProfileFieldsSelector();
    });
    document.querySelector('#clearAllDocTypesBtn')?.addEventListener('click', () => {
        if (documentUpdatePeriods) {
            selectedDocTypeIds.clear();
            renderDocumentTypesSelector();
        } else {
            selectedProfileFieldKeys.clear();
            renderProfileFieldsSelector();
        }
    });

    form.querySelectorAll('input[name="scopeType"]').forEach(radio => {
        radio.addEventListener('change', () => {
            const isSelected = form.querySelector('input[name="scopeType"]:checked')?.value === 'SELECTED';
            document.querySelector('#selectedEmployeesPicker')?.classList.toggle('hidden', !isSelected);
            if (isSelected) {
                renderEmployeePickerList();
            }
        });
    });

    document.querySelector('#employeePickerSearch')?.addEventListener('input', () => {
        renderEmployeePickerList();
    });

    document.querySelector('#selectVisibleEmployeesBtn')?.addEventListener('click', () => {
        getFilteredPickerEmployees().forEach(emp => selectedEmployeeIds.add(Number(emp.employeeId)));
        renderEmployeePickerList();
    });

    document.querySelector('#clearSelectedEmployeesBtn')?.addEventListener('click', () => {
        selectedEmployeeIds.clear();
        renderEmployeePickerList();
    });

    const modal = document.querySelector('#documentPeriodDetailModal');
    document.querySelector('#closeDetailModalBtn')?.addEventListener('click', () => modal?.close());
    document.querySelector('#closeDetailModalFootBtn')?.addEventListener('click', () => modal?.close());
    modal?.querySelectorAll('[data-emp-filter]').forEach(btn => {
        btn.addEventListener('click', () => {
            currentEmpFilter = btn.dataset.empFilter;
            modal.querySelectorAll('[data-emp-filter]').forEach(b => b.classList.toggle('active', b === btn));
            renderDetailEmployeeStatusRows();
        });
    });
}

function setDefaultProfilePeriodDates(form) {
    const localInput = date => new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
    form.elements.namedItem('startsAt').value = localInput(new Date());
    form.elements.namedItem('endsAt').value = localInput(new Date(Date.now() + 24 * 60 * 60 * 1000));
}

async function loadProfileUpdatePeriods() {
    try {
        const payload = await EmployeeApi.get(updatePeriodApi);
        const form = document.querySelector('#profileUpdatePeriodForm');
        form.elements.namedItem('csrfToken').value = payload.csrfToken;
        cachedCompanyEmployees = payload.employees || [];

        if (documentUpdatePeriods) {
            cachedDocumentTypes = payload.documentTypes || [];
            if (selectedDocTypeIds.size === 0 && cachedDocumentTypes.length > 0) {
                selectedDocTypeIds = new Set(cachedDocumentTypes.map(t => Number(t.documentTypeId)));
            } else {
                const validIds = new Set(cachedDocumentTypes.map(t => Number(t.documentTypeId)));
                selectedDocTypeIds = new Set(Array.from(selectedDocTypeIds).filter(id => validIds.has(id)));
            }
            renderCompanyDocumentTypeManager();
            renderDocumentTypesSelector();
        } else {
            cachedProfileFields = payload.profileFields || [];
            if (selectedProfileFieldKeys.size === 0 && cachedProfileFields.length > 0) {
                selectedProfileFieldKeys = new Set(cachedProfileFields.filter(f => f.defaultChecked).map(f => f.fieldKey));
            }
            renderProfileFieldsSelector();
        }

        renderEmployeePickerList();
        renderPeriodRows(payload.data || []);
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

function renderCompanyDocumentTypeManager() {
    const container = document.querySelector('#companyDocumentTypeList');
    if (!container) return;
    if (!cachedDocumentTypes.length) {
        container.innerHTML = '<span class="lc-muted">Chưa có loại tài liệu nào. Hãy thêm loại tài liệu mới bên dưới.</span>';
        return;
    }
    container.innerHTML = cachedDocumentTypes.map(type => `
        <span class="doc-type-chip">
            <span>${escapeHtml(type.typeName)}</span>
            <button type="button" class="doc-type-remove-btn" data-remove-type="${type.documentTypeId}" data-type-name="${escapeHtml(type.typeName)}" title="Xóa loại tài liệu này">×</button>
        </span>
    `).join('');

    container.querySelectorAll('[data-remove-type]').forEach(btn => {
        btn.addEventListener('click', async () => {
            const typeId = Number(btn.dataset.removeType);
            const typeName = btn.dataset.typeName;
            if (!(await AppDialog.confirm(`Xóa loại tài liệu "${typeName}" khỏi danh mục của công ty?`))) {
                return;
            }
            btn.disabled = true;
            try {
                const csrfToken = document.querySelector('#profileUpdatePeriodForm').elements.namedItem('csrfToken').value;
                const payload = await EmployeeApi.post(updatePeriodApi + '/create', {
                    action: 'deleteType',
                    documentTypeId: typeId,
                    csrfToken
                });
                selectedDocTypeIds.delete(typeId);
                showMessage(payload.message);
                await loadProfileUpdatePeriods();
            } catch (error) {
                showMessage(error.message, 'error');
                btn.disabled = false;
            }
        });
    });
}

function renderDocumentTypesSelector() {
    const container = document.querySelector('#periodDocumentTypesSelector');
    if (!container) return;
    if (!cachedDocumentTypes.length) {
        container.innerHTML = '<p class="lc-muted">Vui lòng thêm ít nhất 1 loại tài liệu ở mục Danh mục loại tài liệu phía trên.</p>';
        return;
    }
    container.innerHTML = cachedDocumentTypes.map(type => {
        const id = Number(type.documentTypeId);
        const checked = selectedDocTypeIds.has(id);
        return `
            <label class="doc-type-check-card ${checked ? 'is-checked' : ''}">
                <input type="checkbox" value="${id}" ${checked ? 'checked' : ''}>
                <span>${escapeHtml(type.typeName)}</span>
            </label>
        `;
    }).join('');

    container.querySelectorAll('input[type="checkbox"]').forEach(cb => {
        cb.addEventListener('change', () => {
            const id = Number(cb.value);
            if (cb.checked) selectedDocTypeIds.add(id);
            else selectedDocTypeIds.delete(id);
            cb.closest('.doc-type-check-card')?.classList.toggle('is-checked', cb.checked);
        });
    });
}

function renderProfileFieldsSelector() {
    const container = document.querySelector('#periodDocumentTypesSelector');
    if (!container) return;
    if (!cachedProfileFields.length) {
        container.innerHTML = '<p class="lc-muted">Đang tải danh mục trường thông tin...</p>';
        return;
    }
    container.innerHTML = cachedProfileFields.map(field => {
        const key = String(field.fieldKey);
        const checked = selectedProfileFieldKeys.has(key);
        return `
            <label class="doc-type-check-card ${checked ? 'is-checked' : ''}">
                <input type="checkbox" value="${escapeHtml(key)}" ${checked ? 'checked' : ''}>
                <span>${escapeHtml(field.fieldLabel)}</span>
            </label>
        `;
    }).join('');

    container.querySelectorAll('input[type="checkbox"]').forEach(cb => {
        cb.addEventListener('change', () => {
            const key = cb.value;
            if (cb.checked) selectedProfileFieldKeys.add(key);
            else selectedProfileFieldKeys.delete(key);
            cb.closest('.doc-type-check-card')?.classList.toggle('is-checked', cb.checked);
        });
    });
}

function getFilteredPickerEmployees() {
    const keyword = (document.querySelector('#employeePickerSearch')?.value || '').trim().toLowerCase();
    if (!keyword) return cachedCompanyEmployees;
    return cachedCompanyEmployees.filter(emp =>
        String(emp.fullName || '').toLowerCase().includes(keyword)
        || String(emp.employeeCode || '').toLowerCase().includes(keyword)
        || String(emp.departmentName || '').toLowerCase().includes(keyword)
        || String(emp.positionTitle || '').toLowerCase().includes(keyword)
    );
}

function renderEmployeePickerList() {
    const list = document.querySelector('#employeePickerList');
    const counter = document.querySelector('#selectedEmployeesCounter');
    if (counter) {
        counter.textContent = `Đã chọn: ${selectedEmployeeIds.size} nhân viên`;
    }
    if (!list) return;
    const filtered = getFilteredPickerEmployees();
    if (!filtered.length) {
        list.innerHTML = '<div class="lc-muted" style="padding:12px">Không tìm thấy nhân viên phù hợp.</div>';
        return;
    }
    list.innerHTML = filtered.map(emp => {
        const id = Number(emp.employeeId);
        const checked = selectedEmployeeIds.has(id);
        return `
            <label class="doc-emp-check-row ${checked ? 'is-checked' : ''}">
                <input type="checkbox" value="${id}" ${checked ? 'checked' : ''}>
                <div class="doc-emp-info">
                    <strong>${escapeHtml(emp.fullName)}</strong>
                    <small>${escapeHtml(emp.employeeCode)} · ${escapeHtml(emp.departmentName || 'Chưa phân công')}${emp.positionTitle ? ' · ' + escapeHtml(emp.positionTitle) : ''}</small>
                </div>
            </label>
        `;
    }).join('');

    list.querySelectorAll('input[type="checkbox"]').forEach(cb => {
        cb.addEventListener('change', () => {
            const id = Number(cb.value);
            if (cb.checked) selectedEmployeeIds.add(id);
            else selectedEmployeeIds.delete(id);
            cb.closest('.doc-emp-check-row')?.classList.toggle('is-checked', cb.checked);
            if (counter) counter.textContent = `Đã chọn: ${selectedEmployeeIds.size} nhân viên`;
        });
    });
}

function renderPeriodRows(periods) {
    const rows = document.querySelector('#profilePeriodRows');
    rows.replaceChildren();
    if (!periods.length) {
        const row = rows.insertRow();
        const cell = row.insertCell();
        cell.colSpan = 8;
        cell.textContent = documentUpdatePeriods ? 'Chưa có đợt cập nhật tài liệu.' : 'Chưa có đợt cập nhật hồ sơ.';
        return;
    }

    periods.forEach(period => {
        const row = rows.insertRow();
        const expired = Date.parse(period.endsAt) <= Date.now();
        const stateCode = period.closed ? 'closed' : expired ? 'ended'
            : Date.parse(period.startsAt) <= Date.now() ? 'open' : 'scheduled';
        const state = {
            closed: 'Đã đóng',
            ended: 'Đã kết thúc',
            open: 'Đang mở',
            scheduled: 'Chưa bắt đầu'
        }[stateCode];

        // 1. Tên đợt
        const titleCell = row.insertCell();
        const titleStrong = document.createElement('strong');
        titleStrong.textContent = period.title;
        titleCell.appendChild(titleStrong);

        // 2. Loại tài liệu / Loại thông tin cho phép
        const typesCell = row.insertCell();
        if (documentUpdatePeriods) {
            const docTypes = period.documentTypes || [];
            if (docTypes.length) {
                typesCell.innerHTML = `<div class="doc-mini-tags">${docTypes.map(t => `<span class="doc-mini-tag">${escapeHtml(t.typeName)}</span>`).join('')}</div>`;
            } else {
                typesCell.textContent = 'Tất cả loại tài liệu';
            }
        } else {
            const allowedFields = period.allowedFields || [];
            if (allowedFields.length) {
                typesCell.innerHTML = `<div class="doc-mini-tags">${allowedFields.map(f => `<span class="doc-mini-tag">${escapeHtml(f.fieldLabel)}</span>`).join('')}</div>`;
            } else {
                typesCell.textContent = 'Thông tin cá nhân mặc định';
            }
        }

        // 3. Phạm vi áp dụng
        const scopeCell = row.insertCell();
        const targetCount = Number(period.targetEmployeeCount || 0);
        if (String(period.scopeType || 'ALL').toUpperCase() === 'SELECTED') {
            const names = (period.targetEmployees || []).map(e => e.fullName).slice(0, 2).join(', ');
            const extra = (period.targetEmployees || []).length > 2 ? ` (+${period.targetEmployees.length - 2})` : '';
            scopeCell.innerHTML = `<span class="lc-badge warning">Cá nhân (${targetCount} NV)</span><br><small class="lc-muted">${escapeHtml(names + extra)}</small>`;
        } else {
            scopeCell.innerHTML = `<span class="lc-badge">Toàn công ty (${targetCount} NV)</span>`;
        }

        // 4. Bắt đầu
        row.insertCell().textContent = new Date(period.startsAt).toLocaleString(HrmI18n.locale);

        // 5. Kết thúc
        row.insertCell().textContent = new Date(period.endsAt).toLocaleString(HrmI18n.locale);

        // 6. Tiến độ cập nhật
        const progressCell = row.insertCell();
        const updatedEmp = Number(period.updatedEmployeeCount || 0);
        if (documentUpdatePeriods) {
            const uploadedDocs = Number(period.uploadedDocumentCount || 0);
            progressCell.innerHTML = `<strong>${updatedEmp} / ${targetCount}</strong> nhân viên<br><small class="lc-muted">${uploadedDocs} tài liệu đã nộp</small>`;
        } else {
            const updatedFieldsCount = Number(period.updatedFieldCount || 0);
            progressCell.innerHTML = `<strong>${updatedEmp} / ${targetCount}</strong> nhân viên<br><small class="lc-muted">${updatedFieldsCount} thông tin đã cập nhật</small>`;
        }

        // 7. Trạng thái
        const statusCell = row.insertCell();
        const badge = document.createElement('span');
        badge.className = 'lc-badge' + (stateCode === 'open' ? ' active' : stateCode === 'scheduled' ? ' warning' : '');
        badge.textContent = state;
        statusCell.appendChild(badge);

        // 8. Thao tác
        const actionCell = row.insertCell();
        const actionWrap = document.createElement('div');
        actionWrap.className = 'doc-period-actions';

        const detailBtn = document.createElement('button');
        detailBtn.type = 'button';
        detailBtn.className = 'secondary';
        detailBtn.textContent = 'Xem chi tiết';
        detailBtn.addEventListener('click', () => openPeriodDetail(period.periodId));
        actionWrap.appendChild(detailBtn);

        const closeBtn = document.createElement('button');
        closeBtn.type = 'button';
        closeBtn.className = 'danger';
        closeBtn.textContent = 'Đóng đợt';
        closeBtn.disabled = period.closed || expired;
        closeBtn.addEventListener('click', () => closeProfileUpdatePeriod(period.periodId, closeBtn));
        actionWrap.appendChild(closeBtn);

        actionCell.appendChild(actionWrap);
    });
}

async function openPeriodDetail(periodId) {
    const modal = document.querySelector('#documentPeriodDetailModal');
    if (!modal) return;
    try {
        const payload = await EmployeeApi.get(`${updatePeriodApi}?periodId=${encodeURIComponent(periodId)}`);
        currentDetailData = payload.data;
        currentEmpFilter = 'ALL';
        modal.querySelectorAll('[data-emp-filter]').forEach(b => b.classList.toggle('active', b.dataset.empFilter === 'ALL'));

        const d = currentDetailData;
        document.querySelector('#detailModalTitle').textContent = `Chi tiết đợt: ${d.title}`;
        const scopeLabel = String(d.scopeType || 'ALL').toUpperCase() === 'SELECTED'
            ? `Cá nhân cụ thể (${d.targetEmployeeCount} nhân viên)`
            : `Toàn công ty (${d.targetEmployeeCount} nhân viên)`;
        document.querySelector('#detailModalSubtitle').textContent =
            `${new Date(d.startsAt).toLocaleString(HrmI18n.locale)} → ${new Date(d.endsAt).toLocaleString(HrmI18n.locale)} · Phạm vi: ${scopeLabel}`;

        const typesBar = document.querySelector('#detailRequiredTypesBar');
        const pendingCount = Math.max(0, Number(d.targetEmployeeCount || 0) - Number(d.updatedEmployeeCount || 0));

        if (documentUpdatePeriods) {
            const reqTypes = d.documentTypes || [];
            typesBar.innerHTML = `<strong>Loại tài liệu yêu cầu cập nhật:</strong> ` + (reqTypes.length
                ? reqTypes.map(t => `<span class="doc-mini-tag">${escapeHtml(t.typeName)}</span>`).join(' ')
                : '<span>Tất cả loại tài liệu</span>');

            document.querySelector('#detailSummaryCards').innerHTML = `
                <div class="doc-summary-card">
                    <div class="doc-summary-label">Nhân viên thuộc đợt</div>
                    <div class="doc-summary-value">${Number(d.targetEmployeeCount || 0)}</div>
                </div>
                <div class="doc-summary-card success">
                    <div class="doc-summary-label">Đã cập nhật</div>
                    <div class="doc-summary-value">${Number(d.updatedEmployeeCount || 0)}</div>
                </div>
                <div class="doc-summary-card warning">
                    <div class="doc-summary-label">Chưa cập nhật</div>
                    <div class="doc-summary-value">${pendingCount}</div>
                </div>
                <div class="doc-summary-card">
                    <div class="doc-summary-label">Tổng tài liệu đã tải lên</div>
                    <div class="doc-summary-value">${Number(d.uploadedDocumentCount || 0)}</div>
                </div>
            `;

            const docRows = document.querySelector('#detailUploadedDocsRows');
            const docs = d.uploadedDocuments || [];
            if (!docs.length) {
                docRows.innerHTML = '<tr><td colspan="6" class="lc-muted">Chưa có nhân viên nào tải tài liệu lên trong đợt cập nhật này.</td></tr>';
            } else {
                docRows.innerHTML = docs.map(doc => `
                    <tr>
                        <td><strong>${escapeHtml(doc.fullName)}</strong><br><small class="lc-muted">${escapeHtml(doc.employeeCode)}</small></td>
                        <td>${escapeHtml(doc.departmentName || 'Chưa phân công')}</td>
                        <td><strong>${escapeHtml(doc.documentName)}</strong><br><small class="lc-muted">${escapeHtml(doc.fileName || '')}</small></td>
                        <td><span class="doc-mini-tag">${escapeHtml(doc.documentType || 'Chưa phân loại')}</span></td>
                        <td>${escapeHtml(doc.uploadedDate || '—')}</td>
                        <td>
                            <div class="doc-period-actions">
                                <a class="small secondary" href="${withContext(`/documents/download?id=${doc.documentId}&inline=true`)}" target="_blank" rel="noopener">Xem tệp</a>
                                <a class="small secondary" href="${withContext(`/documents/download?id=${doc.documentId}`)}" download="${escapeHtml(doc.fileName || doc.documentName || 'document')}">Tải xuống</a>
                            </div>
                        </td>
                    </tr>
                `).join('');
            }
        } else {
            const allowedFields = d.allowedFields || [];
            typesBar.innerHTML = `<strong>Loại thông tin cho phép cập nhật:</strong> ` + (allowedFields.length
                ? allowedFields.map(f => `<span class="doc-mini-tag">${escapeHtml(f.fieldLabel)}</span>`).join(' ')
                : '<span>Thông tin cá nhân mặc định</span>');

            document.querySelector('#detailSummaryCards').innerHTML = `
                <div class="doc-summary-card">
                    <div class="doc-summary-label">Nhân viên thuộc đợt</div>
                    <div class="doc-summary-value">${Number(d.targetEmployeeCount || 0)}</div>
                </div>
                <div class="doc-summary-card success">
                    <div class="doc-summary-label">Nhân viên đã cập nhật</div>
                    <div class="doc-summary-value">${Number(d.updatedEmployeeCount || 0)}</div>
                </div>
                <div class="doc-summary-card warning">
                    <div class="doc-summary-label">Chưa cập nhật</div>
                    <div class="doc-summary-value">${pendingCount}</div>
                </div>
                <div class="doc-summary-card">
                    <div class="doc-summary-label">Tổng thông tin đã thay đổi</div>
                    <div class="doc-summary-value">${Number(d.updatedFieldCount || 0)}</div>
                </div>
            `;

            const changeRows = document.querySelector('#detailUploadedDocsRows');
            const changes = d.fieldChanges || [];
            if (!changes.length) {
                changeRows.innerHTML = '<tr><td colspan="6" class="lc-muted">Chưa có nhân viên nào cập nhật thông tin hồ sơ trong đợt này.</td></tr>';
            } else {
                changeRows.innerHTML = changes.map(ch => `
                    <tr>
                        <td><strong>${escapeHtml(ch.fullName)}</strong><br><small class="lc-muted">${escapeHtml(ch.employeeCode)}</small></td>
                        <td>${escapeHtml(ch.departmentName || 'Chưa phân công')}</td>
                        <td><span class="doc-mini-tag">${escapeHtml(ch.fieldLabel)}</span></td>
                        <td class="lc-muted">${escapeHtml(ch.oldValue || 'Chưa cập nhật')}</td>
                        <td><strong>${escapeHtml(ch.newValue || 'Chưa cập nhật')}</strong></td>
                        <td>${escapeHtml(ch.updatedAt || '—')}</td>
                    </tr>
                `).join('');
            }
        }

        renderDetailEmployeeStatusRows();
        modal.showModal();
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

function renderDetailEmployeeStatusRows() {
    const tbody = document.querySelector('#detailEmployeeStatusRows');
    if (!tbody || !currentDetailData) return;
    let list = currentDetailData.employeeStatuses || [];
    const countKey = documentUpdatePeriods ? 'uploadedCount' : 'updatedCount';
    if (currentEmpFilter === 'UPDATED') {
        list = list.filter(emp => Number(emp[countKey] || 0) > 0);
    } else if (currentEmpFilter === 'PENDING') {
        list = list.filter(emp => Number(emp[countKey] || 0) === 0);
    }

    if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="lc-muted">Không có nhân viên phù hợp với bộ lọc.</td></tr>';
        return;
    }

    if (documentUpdatePeriods) {
        tbody.innerHTML = list.map(emp => {
            const uploadedTypes = emp.uploadedTypes || [];
            const missingTypes = emp.missingTypes || [];
            const statusBadge = emp.completionStatus === 'FULL'
                ? '<span class="lc-badge active">Đã cập nhật đủ</span>'
                : emp.completionStatus === 'PARTIAL'
                    ? '<span class="lc-badge warning">Đã nộp một phần</span>'
                    : '<span class="lc-badge">Chưa cập nhật</span>';

            return `
                <tr>
                    <td><strong>${escapeHtml(emp.employeeCode)}</strong></td>
                    <td>${escapeHtml(emp.fullName)}${emp.positionTitle ? `<br><small class="lc-muted">${escapeHtml(emp.positionTitle)}</small>` : ''}</td>
                    <td>${escapeHtml(emp.departmentName || 'Chưa phân công')}</td>
                    <td>${uploadedTypes.length
                        ? `<div class="doc-mini-tags">${uploadedTypes.map(t => `<span class="doc-mini-tag">${escapeHtml(t)}</span>`).join('')}</div>`
                        : '<span class="lc-muted">Chưa nộp</span>'}</td>
                    <td>${missingTypes.length
                        ? `<div class="doc-mini-tags">${missingTypes.map(t => `<span class="doc-mini-tag missing">${escapeHtml(t)}</span>`).join('')}</div>`
                        : '<span class="lc-muted">Không thiếu</span>'}</td>
                    <td>${statusBadge}</td>
                </tr>
            `;
        }).join('');
    } else {
        tbody.innerHTML = list.map(emp => {
            const updatedFields = emp.updatedFields || [];
            const missingFields = emp.missingFields || [];
            const hasUpdated = Number(emp.updatedCount || 0) > 0;
            const statusBadge = hasUpdated
                ? `<span class="lc-badge active">Đã cập nhật</span>${emp.lastUpdatedAt ? `<br><small class="lc-muted">${escapeHtml(emp.lastUpdatedAt)}</small>` : ''}`
                : '<span class="lc-badge">Chưa cập nhật</span>';

            return `
                <tr>
                    <td><strong>${escapeHtml(emp.employeeCode)}</strong></td>
                    <td>${escapeHtml(emp.fullName)}${emp.positionTitle ? `<br><small class="lc-muted">${escapeHtml(emp.positionTitle)}</small>` : ''}</td>
                    <td>${escapeHtml(emp.departmentName || 'Chưa phân công')}</td>
                    <td>${updatedFields.length
                        ? `<div class="doc-mini-tags">${updatedFields.map(f => `<span class="doc-mini-tag">${escapeHtml(f)}</span>`).join('')}</div>`
                        : '<span class="lc-muted">Chưa cập nhật</span>'}</td>
                    <td>${missingFields.length
                        ? `<div class="doc-mini-tags">${missingFields.map(f => `<span class="doc-mini-tag missing">${escapeHtml(f)}</span>`).join('')}</div>`
                        : '<span class="lc-muted">—</span>'}</td>
                    <td>${statusBadge}</td>
                </tr>
            `;
        }).join('');
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
        const payload = await EmployeeApi.post(updatePeriodApi + '/close', {
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

function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, ch => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }[ch]));
}
