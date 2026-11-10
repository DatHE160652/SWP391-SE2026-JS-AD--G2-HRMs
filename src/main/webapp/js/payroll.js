(() => {
    'use strict';
    const context = document.body.dataset.context || '';
    const page = document.body.dataset.page;
    const manageSlips = page === 'payslips' && document.body.dataset.management === 'true';
    const csrf = document.querySelector('meta[name="csrf-token"]').content;
    const $ = selector => document.querySelector(selector);
    const safe = value => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    const money = value => {
        if (value == null) return '—';
        const text = String(value); const negative = text.startsWith('-');
        const [whole, fraction = ''] = text.replace(/^-/, '').split('.');
        const decimals = fraction.replace(/0+$/, '');
        return (negative ? '-' : '') + new Intl.NumberFormat(HrmI18n.locale).format(BigInt(whole)) + (decimals ? (HrmI18n.language === 'en' ? '.' : ',') + decimals : '');
    };
    const cents = value => {
        const text = String(value ?? '0'); const negative = text.startsWith('-');
        const [whole, fraction = ''] = text.replace(/^-/, '').split('.');
        return BigInt(whole + fraction.padEnd(2, '0')) * (negative ? -1n : 1n);
    };
    const decimal = value => {
        const negative = value < 0n; const absolute = negative ? -value : value;
        return (negative ? '-' : '') + String(absolute / 100n) + '.' + String(absolute % 100n).padStart(2, '0');
    };
    const days = value => {
        if (value == null) return '—';
        const num = Number(value);
        return Number.isNaN(num) ? '—' : num.toLocaleString(HrmI18n.locale, { minimumFractionDigits: 0, maximumFractionDigits: 2 });
    };
    const state = {
        periods: [], selected: null, rows: [], adjustments: [], salaries: [], slips: [], rowPage: 1, adjustmentPage: 1, salaryPage: 1,
        adjustmentDraft: null, action: null, busy: false, request: 0, listRequest: 0, failedPeriodId: null,
        slipPage: 1, slipRequest: 0, slipListRequest: 0, settings: null
    };

    async function api(path, data) {
        const options = { credentials: 'same-origin', headers: { Accept: 'application/json' } };
        if (data !== undefined) {
            options.method = 'POST'; options.headers['X-CSRF-Token'] = csrf;
            options.body = data instanceof FormData ? data : new URLSearchParams(data);
        }
        let response;
        try { response = await fetch(context + path, options); }
        catch (_) { throw new Error(HrmI18n.t('Không thể kết nối máy chủ. Vui lòng thử lại.')); }
        let payload;
        try { payload = await response.json().then(HrmI18n.response); } catch (_) { throw new Error(HrmI18n.t('Không đọc được phản hồi. Hãy kiểm tra kết nối hoặc đăng nhập lại.')); }
        if (!response.ok || !payload.success) {
            const error = new Error([payload.message, ...(payload.errors || [])].join('\n')); error.status = response.status; throw error;
        }
        return payload;
    }
    function feedback(message, error = false) {
        const box = $('#feedback'); box.textContent = message; box.className = 'feedback' + (error ? ' error' : ''); box.hidden = false;
    }
    function dialogError(dialog, error) { const box = dialog.querySelector('.dialog-error'); box.textContent = error.message; box.hidden = false; }
    function openDialog(id) { const dialog = $(id); dialog.querySelector('.dialog-error').hidden = true; dialog.showModal(); }
    function employee(row, showCode = true) {
        const initials = row.fullName.trim().split(/\s+/).slice(-2).map(word => word[0]).join('').toUpperCase();
        return `<div class="employee-cell"><span class="employee-avatar">${safe(initials)}</span><div><strong>${safe(row.fullName)}</strong>${showCode ? `<small>${safe(row.employeeCode)}</small>` : ''}${row.salaryIssue ? `<small class="negative">${safe(HrmI18n.t(row.salaryIssue))}</small>` : ""}</div></div>`;
    }
    function editable() { return state.selected && !state.selected.closed && state.selected.status !== 'Approved' && !state.selected.publishedAt; }
    const total = key => state.rows.reduce((sum, row) => sum + cents(row[key] || '0'), 0n);
    function savedTotals() {
        return {
            employeeCount: state.rows.length,
            totalIncome: decimal(total('baseSalary') + total('responsibilityAllowance') + total('travelAllowance') + total('bonus')),
            totalDeduction: '0.00',
            totalNetSalary: decimal(total('baseSalary') + total('responsibilityAllowance') + total('travelAllowance') + total('bonus'))
        };
    }
    function validSavedResults() {
        try {
            if (!state.rows.length || !state.rows.every(row => row.payslipId && !row.salaryIssue &&
                ['baseSalary', 'responsibilityAllowance', 'bonus', 'netSalary'].every(key => row[key] != null) &&
                ['baseSalary', 'responsibilityAllowance', 'bonus'].every(key => cents(row[key]) >= 0n) &&
                cents(row.travelAllowance ?? '0') >= 0n &&
                cents(row.netSalary) === cents(row.baseSalary) + cents(row.responsibilityAllowance) + cents(row.travelAllowance ?? '0') + cents(row.bonus))) return false;
            const p = state.selected, totals = savedTotals();
            return (p.employeeCount == null || p.employeeCount === totals.employeeCount) &&
                ['totalIncome', 'totalNetSalary'].every(key => p[key] == null || cents(p[key]) === cents(totals[key]));
        } catch (_) { return false; }
    }
    function canConfirm(action) {
        const p = state.selected; if (!p || p.publishedAt) return false;
        if (action === 'review' || action === 'approve') return !p.closed && validSavedResults() &&
            (action === 'review' ? p.status === 'Calculated' : p.status === 'Reviewed' && p.reviewedByUserId != null && !!p.reviewedAt);
        return !!confirmations[action];
    }
    function actorTime(name, userId, time) {
        return `${name || '#' + userId} · ${new Date(time + 'Z').toLocaleString(HrmI18n.locale)}`;
    }
    const badges = p => `<span class="badge ${safe(p.status)}">${safe(HrmI18n.label(p.status))}</span>${p.publishedAt ? HrmI18n.fragment('<span class="badge Approved">Đã công bố</span>') : p.closed ? HrmI18n.fragment('<span class="badge closed">Đã đóng</span>') : ''}`;

    function adjustmentValues() {
        const form = $('#adjustmentForm');
        return { bonus: form.elements.bonus.value, deduction: '0', note: form.elements.note.value };
    }
    function adjustmentDirty() {
        if (!state.adjustmentDraft) return false;
        const current = adjustmentValues();
        return Object.keys(current).some(key => current[key] !== state.adjustmentDraft.original[key]);
    }
    function clearAdjustmentDraft() {
        state.adjustmentDraft = null;
        $('#adjustmentDialog')?.close();
    }
    async function discardAdjustment() {
        if (state.adjustmentDraft && state.busy) return false;
        if (adjustmentDirty() && !(await AppDialog.confirm(HrmI18n.t('Có thay đổi điều chỉnh chưa được lưu. Bạn có muốn bỏ các thay đổi này?')))) return false;
        clearAdjustmentDraft(); return true;
    }
    function updateAdjustmentState() {
        const dirty = adjustmentDirty(), saving = !!state.adjustmentDraft && state.busy;
        const status = $('#adjustmentSaveState');
        status.textContent = saving ? HrmI18n.t('Đang lưu điều chỉnh…') : dirty ? HrmI18n.t('Có thay đổi chưa lưu.') : HrmI18n.t('Chưa có thay đổi.');
        status.className = 'subtle' + (saving ? ' saving' : dirty ? ' dirty' : '');
        $('#adjustmentForm [type="submit"]').disabled = saving || !dirty || !editable();
    }
    function validateAdjustment() {
        const form = $('#adjustmentForm'), values = adjustmentValues(), errors = {};
        for (const [key, label] of [['bonus', HrmI18n.t('Thưởng')]]) {
            const value = values[key];
            if (!/^(?:\d+(?:\.\d{0,2})?|\.\d{1,2})$/.test(value) || cents(value) > 999999999999999999n)
                errors[key] = `${label}${HrmI18n.fragment(" phải là số không âm, tối đa 2 chữ số thập phân và không vượt 9999999999999999.99.")}`;
        }
        if (values.note.trim().length > 300) errors.note = HrmI18n.t('Ghi chú không được vượt quá 300 ký tự.');
        for (const key of ['bonus', 'note']) {
            const message = form.querySelector(`[data-adjustment-error="${key}"]`);
            if (message) { message.textContent = errors[key] || ''; message.hidden = !errors[key]; }
            if (form.elements[key]) form.elements[key].setAttribute('aria-invalid', String(!!errors[key]));
        }
        if (Object.keys(errors).length) form.elements[Object.keys(errors)[0]].focus();
        return !Object.keys(errors).length;
    }
    async function saveAdjustment() {
        if (state.busy || !state.adjustmentDraft || !adjustmentDirty()) return;
        if (!editable()) { dialogError($('#adjustmentDialog'), new Error(HrmI18n.t('Kỳ lương đã khóa hoặc phê duyệt. Không thể lưu điều chỉnh.'))); return; }
        if (!validateAdjustment()) return;
        const draft = state.adjustmentDraft, form = $('#adjustmentForm'), dialog = $('#adjustmentDialog');
        const data = { ...adjustmentValues(), periodId: draft.periodId, employeeId: draft.employeeId };
        const controls = [...form.querySelectorAll('input,textarea,button')];
        state.busy = true; controls.forEach(control => control.disabled = true);
        form.setAttribute('aria-busy', 'true'); form.querySelector('.dialog-error').hidden = true;
        const button = form.querySelector('[type="submit"]'); button.textContent = HrmI18n.t('Đang lưu…'); updateAdjustmentState();
        let saved = false;
        try {
            const result = await api('/payroll/api/adjustment', data); saved = true;
            clearAdjustmentDraft(); feedback(result.message);
            await loadPeriods(draft.periodId);
        } catch (error) {
            if (saved) feedback(HrmI18n.t('Đã lưu điều chỉnh nhưng chưa tải lại được kỳ lương. Hãy chọn Tải lại. ') + error.message, true);
            else {
                dialogError(dialog, error);
                $('#adjustmentSaveState').textContent = HrmI18n.t('Chưa lưu được. Các giá trị vừa nhập vẫn được giữ để bạn kiểm tra và thử lại.');
            }
        } finally {
            state.busy = false; controls.forEach(control => control.disabled = false); form.setAttribute('aria-busy', 'false'); button.textContent = HrmI18n.t('Lưu điều chỉnh');
            if (saved) updateAdjustmentState();
            else button.disabled = !adjustmentDirty();
        }
    }

    function clearPeriodSelection(updateUrl = true) {
        ++state.request; // Ignore any detail response for a selection that is no longer visible.
        state.selected = null; state.rows = []; state.adjustments = []; state.failedPeriodId = null;
        $('#periodWorkspace').hidden = true; $('#periodDetailState').hidden = true;
        if (updateUrl) {
            const url = new URL(location.href); url.searchParams.delete('periodId'); history.replaceState(null, '', url);
        }
    }
    function clearPeriodFailure() {
        if ($('#feedback').classList.contains('error')) $('#feedback').hidden = true;
    }
    function periodListBusy(busy) {
        $('#periodList').setAttribute('aria-busy', String(busy));
        $('#yearFilter').disabled = busy; $('#statusFilter').disabled = busy; $('#refreshPeriods').disabled = busy;
    }
    function filteredPeriods() {
        return state.periods.filter(p => (!$('#yearFilter').value || p.year === Number($('#yearFilter').value)) && (!$('#statusFilter').value || p.status === $('#statusFilter').value));
    }
    async function loadPeriods(preferred) {
        if (!(await discardAdjustment())) return;
        const sequence = ++state.listRequest;
        const requested = preferred ?? state.failedPeriodId ?? new URLSearchParams(location.search).get('periodId') ?? state.selected?.payrollPeriodId;
        clearPeriodSelection(false); periodListBusy(true);
        $('#periodList').innerHTML = HrmI18n.fragment('<div class="empty">Đang tải kỳ lương…</div>');
        try {
            const result = await api('/payroll/api/periods');
            if (sequence !== state.listRequest) return;
            state.periods = result.data;
        } catch (error) {
            if (sequence !== state.listRequest) return;
            state.periods = []; $('#periodCount').textContent = '0';
            $('#periodList').innerHTML = HrmI18n.fragment('<div class="empty"><strong>Không tải được danh sách kỳ lương</strong>Kiểm tra kết nối hoặc đăng nhập lại, sau đó chọn Tải lại.</div>');
            throw error;
        } finally { if (sequence === state.listRequest) periodListBusy(false); }
        clearPeriodFailure();
        const year = $('#yearFilter').value;
        const years = [...new Set(state.periods.map(p => p.year))].sort((a, b) => b - a);
        $('#yearFilter').innerHTML = HrmI18n.fragment('<option value="">Tất cả năm</option>') + years.map(y => `<option>${y}</option>`).join('');
        $('#yearFilter').value = years.includes(Number(year)) ? year : '';
        // A newly created or updated period must remain visible after a successful action.
        if (preferred != null && state.periods.some(p => p.payrollPeriodId === Number(preferred)) && !filteredPeriods().some(p => p.payrollPeriodId === Number(preferred))) {
            $('#yearFilter').value = ''; $('#statusFilter').value = '';
        }
        const items = renderPeriods();
        if (requested != null && !state.periods.some(p => p.payrollPeriodId === Number(requested))) {
            await selectPeriod(requested); return; // Let the existing API report an invalid or foreign ID.
        }
        const chosen = items.find(p => p.payrollPeriodId === Number(requested)) || items[0];
        if (chosen) await selectPeriod(chosen.payrollPeriodId);
        else clearPeriodSelection();
    }
    function renderPeriods() {
        state.filters = { year: $('#yearFilter').value, status: $('#statusFilter').value };
        const items = filteredPeriods();
        $('#periodCount').textContent = items.length;
        $('#periodCount').title = `${items.length} / ${state.periods.length}${HrmI18n.fragment(" kỳ lương")}`;
        $('#periodList').innerHTML = items.length ? items.map(p => `<button class="period-tile ${state.selected?.payrollPeriodId === p.payrollPeriodId ? 'active' : ''}" data-period="${p.payrollPeriodId}" aria-pressed="${state.selected?.payrollPeriodId === p.payrollPeriodId}${HrmI18n.fragment("\"><strong>Tháng ")}${String(p.month).padStart(2, '0')} / ${p.year}</strong>${badges(p)}<small>${p.employeeCount ? `${p.employeeCount}${HrmI18n.fragment(" nhân viên · ")}${money(p.totalNetSalary)} ₫` : HrmI18n.t('Chưa có kết quả tính lương')}</small></button>`).join('') : `<div class="empty"><strong>${state.periods.length ? HrmI18n.t('Không có kỳ phù hợp') : HrmI18n.t('Bắt đầu kỳ lương đầu tiên')}</strong>${state.periods.length ? HrmI18n.t('Thử thay đổi bộ lọc.') : HrmI18n.t('Tạo kỳ lương để nhập thưởng, khấu trừ và tính thu nhập cho nhân viên.')}</div>`;
        return items;
    }
    async function changePeriodFilters() {
        if (!(await discardAdjustment())) {
            if (state.filters) { $('#yearFilter').value = state.filters.year; $('#statusFilter').value = state.filters.status; }
            return;
        }
        clearPeriodFailure();
        const items = renderPeriods();
        if (!items.length) { clearPeriodSelection(); return; }
        if (items.some(p => p.payrollPeriodId === state.selected?.payrollPeriodId)) return;
        await selectPeriod(items[0].payrollPeriodId);
    }
    async function selectPeriod(id) {
        if (!(await discardAdjustment())) return;
        const sequence = ++state.request;
        clearPeriodFailure(); state.selected = null; state.rows = []; state.adjustments = []; state.failedPeriodId = id;
        $('#periodWorkspace').hidden = true;
        const message = $('#periodDetailState'); message.className = 'info-note'; message.textContent = HrmI18n.t('Đang tải chi tiết kỳ lương…'); message.hidden = false;
        renderPeriods();
        try {
            const result = (await api('/payroll/api/period?periodId=' + encodeURIComponent(id))).data;
            if (sequence !== state.request) return;
            state.selected = result.period; state.rows = result.rows; state.adjustments = result.adjustments || result.rows;
            state.rowPage = 1; state.adjustmentPage = 1; state.failedPeriodId = null;
            $('#adjustmentSearch').value = '';
            $('#rowSearch').value = ''; message.hidden = true; $('#periodWorkspace').hidden = false;
            const url = new URL(location.href); url.searchParams.set('periodId', result.period.payrollPeriodId); history.replaceState(null, '', url);
            renderPeriods(); renderWorkspace();
        } catch (error) {
            if (sequence !== state.request) return;
            message.className = 'feedback error';
            message.innerHTML = `${HrmI18n.fragment("Không tải được chi tiết kỳ lương. ")}${safe(error.message)}${HrmI18n.fragment(" <button class=\"text-button\" type=\"button\" data-retry-period>Thử lại</button>")}`;
            throw error;
        }
    }
    function renderWorkspace() {
        const p = state.selected;
        $('#periodTitle').textContent = p.periodName; $('#periodBadges').innerHTML = badges(p);
        const buttons = [];
        if (!p.publishedAt) {
            if (!p.closed && p.status !== 'Approved') buttons.push(['calculate', p.status === 'Draft' ? HrmI18n.t('Tính lương') : HrmI18n.t('Tính lại'), 'primary']);
            if (!p.closed && p.status === 'Calculated') buttons.push(['review', HrmI18n.t('Hoàn tất rà soát'), 'secondary']);
            if (!p.closed && p.status === 'Reviewed') buttons.push(['approve', HrmI18n.t('Phê duyệt'), 'success']);
            if (!p.closed && p.status === 'Approved') buttons.push(['publish', HrmI18n.t('Công bố phiếu lương'), 'primary']);
            if (p.closed || p.status === 'Approved' || p.status === 'Reviewed') buttons.push(['reopen', HrmI18n.t('Mở lại về Draft'), 'secondary']);
        }
        $('#periodActions').innerHTML = (editable() ? HrmI18n.fragment('<button class="btn secondary" type="button" data-preview-payroll>Xem trước</button>') : '') + buttons.map(([action, label, style]) => `<button class="btn ${style}" data-action="${action}" ${!canConfirm(action) ? 'disabled' : ''}>${label}</button>`).join('');
        $('#periodNotice').textContent = p.publishedAt ? HrmI18n.t('Phiếu lương đã công bố và nhân viên đã nhận thông báo trong hệ thống. Kỳ được khóa để bảo toàn kết quả.') : p.closed ? HrmI18n.t('Kỳ đang đóng. Mở lại sẽ đưa kỳ về Draft để tính và duyệt lại.') : ({ Draft: HrmI18n.t('Chuẩn bị lương cơ bản và các khoản điều chỉnh, sau đó chọn Tính lương. Chấm công và nghỉ phép không tự thay đổi mức lương.'), Calculated: HrmI18n.t('Kết quả đã được tính. Kiểm tra các khoản thu nhập rồi hoàn tất rà soát. Chỉnh sửa điều chỉnh sẽ yêu cầu tính lại.'), Reviewed: HrmI18n.t('Bảng lương đã được rà soát. Phê duyệt để chuẩn bị công bố phiếu lương.'), Approved: HrmI18n.t('Bảng lương đã phê duyệt. Công bố để nhân viên xem phiếu lương và nhận thông báo.') }[p.status]);
        $('#metricEmployees').textContent = state.rows.length;
        const total = key => state.rows.reduce((sum, row) => sum + cents(row[key]), 0n);
        const validIncome = state.rows.every(row => !row.salaryIssue && row.baseSalary != null && row.responsibilityAllowance != null);
        $('#metricIncome').textContent = validIncome ? money(decimal(total('baseSalary') + total('responsibilityAllowance') + total('travelAllowance') + total('bonus'))) : '—';
        if ($('#metricBonus')) $('#metricBonus').textContent = money(decimal(total('bonus')));
        $('#metricNet').textContent = p.status === 'Draft' ? '—' : money(decimal(total('netSalary')));
        $('#metricNetNote').textContent = p.status === 'Draft' ? HrmI18n.t('Chưa tính lương') : HrmI18n.t('VND · Kết quả tính lương');
        const history = [];
        if (p.reviewedAt && p.reviewedByUserId != null) history.push(HrmI18n.t('Đã rà soát: ') + actorTime(p.reviewedByName, p.reviewedByUserId, p.reviewedAt));
        if (p.approvedAt && p.approvedByUserId != null) history.push(HrmI18n.t('Đã phê duyệt: ') + actorTime(p.approvedByName, p.approvedByUserId, p.approvedAt));
        $('#periodReviewHistory').textContent = history.join(' | '); $('#periodReviewHistory').hidden = !history.length;
        if (!p.closed && !p.publishedAt && ['Calculated', 'Reviewed'].includes(p.status)) {
            if (!validSavedResults()) $('#periodNotice').textContent += HrmI18n.t(' Kết quả chưa đầy đủ hoặc chưa hợp lệ. Hãy tải lại hoặc tính lại trước khi rà soát và duyệt.');
            else if (p.status === 'Reviewed' && (p.reviewedByUserId == null || !p.reviewedAt))
                $('#periodNotice').textContent += HrmI18n.t(' Kỳ cũ chưa ghi nhận người/thời điểm rà soát. Hãy tính lại và hoàn tất rà soát trước khi duyệt.');
        }
        $('#importButton').disabled = !editable(); $('#templateButton').disabled = !state.selected;
        $('#adjustmentLock').hidden = editable();
        renderRows(); renderAdjustmentRows();
    }
    function pagination(id, current, count, kind, label = HrmI18n.t('nhân viên')) {
        const pages = Math.max(1, Math.ceil(count / 25));
        $(id).innerHTML = `<span>${count} ${label} · ${HrmI18n.t("Trang")} ${current}/${pages}</span><button class="btn secondary" data-page-kind="${kind}" data-page-step="-1" ${current <= 1 ? 'disabled' : ''}${HrmI18n.fragment(" aria-label=\"Trang trước\">←</button><button class=\"btn secondary\" data-page-kind=\"")}${kind}" data-page-step="1" ${current >= pages ? 'disabled' : ''} aria-label="${HrmI18n.t("Trang sau")}">→</button>`;
    }
    function renderRows() {
        const q = $('#rowSearch').value.trim().toLocaleLowerCase('vi');
        const rows = state.rows.filter(row => (row.fullName + ' ' + row.employeeCode).toLocaleLowerCase('vi').includes(q));
        $('#rowCount').textContent = `${rows.length}${HrmI18n.fragment(" nhân viên")}`;
        $('#payrollRows').innerHTML = rows.slice((state.rowPage - 1) * 25, state.rowPage * 25).map(row => `<tr>
            <td>${safe(row.employeeCode)}</td><td>${employee(row, false)}</td>
            <td class="number">${money(row.contractBaseSalary ?? row.baseSalary)}</td>
            <td class="number">${days(row.actualWorkDays)} / ${days(row.standardWorkDays)}</td>
            <td class="number">${money(row.baseSalary)}</td><td class="number">${money(row.responsibilityAllowance)}</td>
            <td class="number">${money(row.travelAllowance ?? '0')}</td>
            <td class="number">${money(row.bonus)}</td>
            <td class="number net-value ${row.netSalary != null && cents(row.netSalary) < 0n ? 'negative' : ''}">${money(row.netSalary)}</td>
            <td>${row.payslipId ? `<a class="text-button" href="${context}/payroll/payslip?id=${row.payslipId}${HrmI18n.fragment("\">Xem phiếu</a>")}` :
                `<span class="subtle">${editable() ? HrmI18n.t('Chưa tính lương') : HrmI18n.t('Đã khóa')}</span>`}</td>
            </tr>`).join('') || HrmI18n.fragment('<tr><td colspan="10" class="empty">Không có nhân viên phù hợp. Kiểm tra thời gian làm việc và trạng thái hồ sơ.</td></tr>');
        pagination('#rowPagination', state.rowPage, rows.length, 'rows');
    }
    function renderAdjustmentRows() {
        const q = $('#adjustmentSearch').value.trim().toLocaleLowerCase('vi');
        const rows = state.adjustments.filter(row => (row.fullName + ' ' + row.employeeCode).toLocaleLowerCase('vi').includes(q));
        $('#adjustmentCount').textContent = `${rows.length}${HrmI18n.fragment(" nhân viên")}`;
        $('#adjustmentRows').innerHTML = rows.slice((state.adjustmentPage - 1) * 25, state.adjustmentPage * 25).map(row => {
            const inPeriod = row.baseSalary != null || (!row.salaryIssue && row.contractBaseSalary != null);
            const badge = !inPeriod ? `<span class="badge" style="background:#f1f5f9;color:#64748b;font-size:11px;font-weight:normal;margin-left:6px;">Chưa/hết HĐ trong kỳ</span>` : '';
            return `<tr>
                <td><strong>${safe(row.employeeCode)}</strong></td>
                <td>${safe(row.fullName)}${badge}</td>
                <td class="number">${row.baseSalary != null ? money(row.baseSalary) : `<span class="subtle" title="${safe(row.salaryIssue || '')}">—</span>`}</td>
                <td class="number">${money(row.bonus)}</td>
                <td class="adjustment-note">${safe(row.note || '—')}</td>
                <td>${editable() ? `<button type="button" class="text-button" data-adjust="${row.employeeId}${HrmI18n.fragment("\">Điều chỉnh</button>")}` : HrmI18n.fragment('<span class="subtle">Chỉ xem</span>')}</td>
            </tr>`;
        }).join('') || HrmI18n.fragment('<tr><td colspan="6" class="empty">Không có nhân viên phù hợp trong công ty.</td></tr>');
        pagination('#adjustmentPagination', state.adjustmentPage, rows.length, 'adjustments');
    }
    async function loadSalaries() { state.salaries = (await api('/payroll/api/salaries')).data; renderSalaries(); }
    function renderSalaries() {
        const q = $('#salarySearch').value.trim().toLocaleLowerCase('vi');
        const rows = state.salaries.filter(row => (row.fullName + ' ' + row.employeeCode + ' ' + (row.departmentName || '')).toLocaleLowerCase('vi').includes(q));
        $('#salaryRows').innerHTML = rows.slice((state.salaryPage - 1) * 25, state.salaryPage * 25).map(row => `<tr><td>${employee(row)}</td><td>${safe(row.departmentName || HrmI18n.t('Chưa phân công'))}</td><td><span class="badge ${row.status === 'Active' ? 'Approved' : ''}">${safe(HrmI18n.label(row.status))}</span></td><td class="number net-value">${money(row.baseSalary)}${row.salaryEffectiveFrom ? `${HrmI18n.fragment("<small class=\"subtle\">Từ ")}${safe(row.salaryEffectiveFrom)}${row.salaryEffectiveTo ? `${HrmI18n.fragment(" đến trước ")}${safe(row.salaryEffectiveTo)}` : ""}</small>` : ""}</td><td class="number">${money(row.responsibilityAllowance)}</td><td class="number">${money(row.travelAllowance ?? '0')}</td><td><a class="text-button" href="${context}/lifecycle/contracts?employeeId=${row.employeeId}">${HrmI18n.t('Xem hợp đồng')}</a></td></tr>`).join('') || HrmI18n.fragment('<tr><td colspan="7" class="empty">Không có nhân viên phù hợp.</td></tr>');
        pagination('#salaryPagination', state.salaryPage, rows.length, 'salaries');
    }
    async function openAdjustment(id) {
        if (!editable() || !(await discardAdjustment())) return;
        const row = state.adjustments.find(r => r.employeeId === id); if (!row) return;
        const form = $('#adjustmentForm'); form.reset();
        form.elements.employeeId.value = id; form.elements.bonus.value = row.bonus; if (form.elements.deduction) form.elements.deduction.value = '0'; form.elements.note.value = row.note || '';
        state.adjustmentDraft = { employeeId: id, periodId: state.selected.payrollPeriodId, original: adjustmentValues() };
        form.querySelectorAll('[data-adjustment-error]').forEach(message => { message.textContent = ''; message.hidden = true; });
        form.querySelectorAll('[aria-invalid]').forEach(input => input.removeAttribute('aria-invalid'));
        $('#adjustmentEmployee').textContent = row.fullName + ' · ' + row.employeeCode; openDialog('#adjustmentDialog'); updateAdjustmentState();
    }
    function openSalary(id) {
        const row = state.salaries.find(r => r.employeeId === id); const form = $('#salaryForm');
        form.elements.employeeId.value = id; form.elements.baseSalary.value = row.baseSalary ?? ""; const today = new Date(); form.elements.effectiveFrom.value = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
        $('#salaryEmployee').textContent = row.fullName + ' · ' + row.employeeCode; openDialog('#salaryDialog');
    }
    function stepSalary(direction) {
        const input = $('#salaryForm').elements.baseSalary;
        if (state.busy || input.disabled || input.readOnly) return;
        if (input.validity.badInput || input.value && !/^(?:\d+(?:\.\d{0,2})?|\.\d{1,2})$/.test(input.value)) {
            input.reportValidity(); return;
        }
        // The button increment is independent of the accepted decimal precision.
        const next = cents(input.value || '0') + BigInt(direction) * cents(input.dataset.salaryStep);
        const minimum = cents(input.min), maximum = cents(input.max);
        input.value = decimal(next < minimum ? minimum : next > maximum ? maximum : next);
        input.dispatchEvent(new Event('input', { bubbles: true }));
    }
    const confirmations = {
        calculate: [HrmI18n.t('Tính bảng lương?'), HrmI18n.t('Hệ thống kiểm tra lịch sử lương, chính sách phụ cấp và điều chỉnh của toàn bộ nhân viên trước khi lưu. Thay đổi giữa tháng cần có quy tắc được xác nhận.')],
        review: [HrmI18n.t('Hoàn tất rà soát?'), HrmI18n.t('Xác nhận bạn đã kiểm tra lương cơ bản, phụ cấp trách nhiệm, phụ cấp đi lại, thưởng và thực nhận. Kỳ sẽ chuyển sang Reviewed.')],
        approve: [HrmI18n.t('Phê duyệt bảng lương?'), HrmI18n.t('Bảng lương sẽ chuyển sang Approved và khóa chỉnh sửa. Nếu cần sửa trước khi công bố, hãy mở lại về Draft và duyệt lại.')],
        publish: [HrmI18n.t('Công bố phiếu lương?'), HrmI18n.t('Phiếu lương sẽ hiển thị cho từng nhân viên và tạo thông báo trong hệ thống. Kỳ sẽ được khóa sau khi công bố.')],
        close: [HrmI18n.t('Đóng kỳ lương?'), HrmI18n.t('Kỳ sẽ tạm khóa các thao tác. Bạn có thể mở lại kỳ chưa công bố về Draft.')],
        reopen: [HrmI18n.t('Mở lại kỳ về Draft?'), HrmI18n.t('Kết quả tính và phê duyệt hiện tại sẽ được xóa. Các khoản điều chỉnh được giữ để tính lại.')]
    };
    function confirmAction(action) {
        if (!canConfirm(action)) return;
        state.action = { action, periodId: state.selected.payrollPeriodId };
        $('#confirmTitle').textContent = confirmations[action][0]; $('#confirmText').textContent = confirmations[action][1];
        const summary = $('#confirmSummary'); summary.hidden = !['review', 'approve'].includes(action);
        if (!summary.hidden) {
            const totals = savedTotals();
            summary.innerHTML = `<strong>${safe(state.selected.periodName)}${HrmI18n.fragment("</strong><br>Tổng nhân viên: <strong>")}${totals.employeeCount}${HrmI18n.fragment("</strong><br>Tổng thu nhập: <strong>")}${money(totals.totalIncome)}${HrmI18n.fragment(" ₫</strong><br>Tổng tiền thưởng: <strong>")}${money(decimal(total('bonus')))}${HrmI18n.fragment(" ₫</strong><br>Tổng thực nhận: <strong>")}${money(totals.totalNetSalary)} ₫</strong>`;
        }
        $('#confirmSubmit').textContent = action === 'review' ? HrmI18n.t('Xác nhận rà soát') : action === 'approve' ? HrmI18n.t('Xác nhận phê duyệt') : HrmI18n.t('Xác nhận');
        openDialog('#confirmDialog');
    }
    async function previewPayroll(button) {
        if (state.busy || !editable() || !(await discardAdjustment())) return;
        const id = state.selected.payrollPeriodId, sequence = state.request;
        const dialog = $('#calculationPreviewDialog');
        $('#calculationPreviewTitle').textContent = HrmI18n.t('Xem trước · ') + state.selected.periodName;
        $('#calculationPreviewContent').textContent = HrmI18n.t('Đang kiểm tra dữ liệu và tính lương…');
        openDialog('#calculationPreviewDialog'); state.busy = true; button.disabled = true;
        try {
            const result = (await api('/payroll/api/preview?periodId=' + encodeURIComponent(id))).data;
            if (sequence !== state.request || state.selected?.payrollPeriodId !== id) { dialog.close(); return; }
            $('#calculationPreviewContent').innerHTML = `<p>${result.rows.length}${HrmI18n.fragment(" nhân viên · Kết quả xem trước chưa được lưu.</p>\n                <div class=\"info-note\">Tổng thu nhập: <strong>")}${money(result.totalIncome)}${HrmI18n.fragment(" ₫</strong><br>Tổng tiền thưởng: <strong>")}${money(decimal(result.rows.reduce((sum, r) => sum + cents(r.bonus), 0n)))}${HrmI18n.fragment(" ₫</strong><br>Tổng thực nhận: <strong>")}${money(result.totalNetSalary)}${HrmI18n.fragment(" ₫</strong></div>\n                <div class=\"table-scroll\"><table><thead><tr><th>Mã nhân viên</th><th>Họ tên</th><th class=\"number\">Lương cơ bản</th><th class=\"number\">Công thực / Chuẩn</th><th class=\"number\">Lương theo công</th><th class=\"number\">Phụ cấp trách nhiệm</th><th class=\"number\">Phụ cấp đi lại</th><th class=\"number\">Thưởng</th><th class=\"number\">Thực nhận</th></tr></thead><tbody>")}${result.rows.map(row => `<tr><td>${safe(row.employeeCode)}</td><td>${safe(row.fullName)}</td><td class="number">${money(row.contractBaseSalary ?? row.baseSalary)}</td><td class="number">${days(row.actualWorkDays)} / ${days(row.standardWorkDays)}</td><td class="number">${money(row.baseSalary)}</td><td class="number">${money(row.responsibilityAllowance)}</td><td class="number">${money(row.travelAllowance ?? '0')}</td><td class="number">${money(row.bonus)}</td><td class="number net-value">${money(row.netSalary)}</td></tr>`).join('')}</tbody></table></div>`;
        } catch (error) { $('#calculationPreviewContent').textContent = HrmI18n.t('Chưa có kết quả xem trước.'); dialogError(dialog, error); }
        finally { state.busy = false; button.disabled = false; }
    }
    async function submit(form, path, data, after) {
        if (state.busy) return;
        state.busy = true; const dialog = form.closest('dialog'); const button = form.querySelector('[type="submit"]'); button.disabled = true;
        const label = button.textContent; button.textContent = HrmI18n.t('Đang xử lý…'); form.setAttribute('aria-busy', 'true');
        try {
            const result = await api(path, data); dialog.close(); feedback(result.message);
            await after(result);
        } catch (error) { if (dialog.open) dialogError(dialog, error); else feedback(error.message, true); }
        finally { state.busy = false; button.disabled = false; button.textContent = label; form.setAttribute('aria-busy', 'false'); }
    }
    function setupDialogs() {
        document.querySelectorAll('[data-salary-direction]').forEach(button => button.addEventListener('click', () => stepSalary(button.dataset.salaryDirection)));
        $('#salaryForm').elements.baseSalary.addEventListener('keydown', event => {
            if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
                event.preventDefault(); stepSalary(event.key === 'ArrowUp' ? 1 : -1);
            }
        });
        document.querySelectorAll('[data-dismiss]').forEach(button => button.addEventListener('click', () => {
            if (state.busy) return;
            const dialog = button.closest('dialog'); if (dialog.id === 'adjustmentDialog') discardAdjustment(); else dialog.close();
        }));
        document.querySelectorAll('dialog').forEach(dialog => dialog.addEventListener('cancel', event => {
            if (state.busy) event.preventDefault();
            else if (dialog.id === 'adjustmentDialog') { event.preventDefault(); discardAdjustment(); }
        }));
        $('#adjustmentForm').addEventListener('input', event => {
            const key = event.target.name, message = event.currentTarget.querySelector(`[data-adjustment-error="${key}"]`);
            if (message) { message.hidden = true; event.target.removeAttribute('aria-invalid'); }
            event.currentTarget.querySelector('.dialog-error').hidden = true; updateAdjustmentState();
        });
        $('#createForm').addEventListener('submit', event => {
            event.preventDefault();
            const form = event.currentTarget;
            const now = new Date();
            const currentYear = now.getFullYear();
            const currentMonth = now.getMonth() + 1;
            const y = parseInt(form.elements.year.value, 10);
            const m = parseInt(form.elements.month.value, 10);
            if (y > currentYear || (y === currentYear && m >= currentMonth)) {
                dialogError(form.closest('dialog'), new Error('Chỉ được tạo kỳ lương cho các tháng đã kết thúc (từ tháng trước trở về trước).'));
                return;
            }
            submit(event.currentTarget, '/payroll/api/create', Object.fromEntries(new FormData(event.currentTarget)), result => loadPeriods(result.periodId));
        });
        $('#salaryForm').addEventListener('submit', event => { event.preventDefault(); submit(event.currentTarget, '/payroll/api/salary', Object.fromEntries(new FormData(event.currentTarget)), loadSalaries); });
        $('#adjustmentForm').addEventListener('submit', event => {
            event.preventDefault(); saveAdjustment();
        });
        $('#confirmForm').addEventListener('submit', event => {
            event.preventDefault(); const pending = state.action;
            if (!pending || !$('#confirmDialog').open || state.selected?.payrollPeriodId !== pending.periodId || !canConfirm(pending.action)) return;
            submit(event.currentTarget, '/payroll/api/' + pending.action, { periodId: pending.periodId }, () => loadPeriods(pending.periodId));
        });
        $('#importButton')?.addEventListener('click', () => {
            $('#importForm').reset(); $('#importPreview').hidden = true; $('#saveImport').disabled = true;
            $('#saveImport').textContent = 'Nhập bảng lương';
            openDialog('#importDialog');
        });
        $('#downloadTemplateInDialog')?.addEventListener('click', () => {
            if (state.selected?.payrollPeriodId) {
                location.href = context + '/payroll/api/template?periodId=' + state.selected.payrollPeriodId;
            }
        });
        $('#importFile').addEventListener('change', () => { $('#saveImport').disabled = true; $('#importPreview').hidden = true; });
        $('#previewImport').addEventListener('click', async () => {
            if (state.busy || !$('#importForm').reportValidity()) return;
            state.busy = true; $('#previewImport').disabled = true; $('#saveImport').disabled = true; $('#importDialog .dialog-error').hidden = true;
            try {
                const data = new FormData($('#importForm')); data.set('periodId', state.selected.payrollPeriodId);
                const rows = (await api('/payroll/api/import-preview', data)).data;
                const totalContractBase = rows.reduce((s, r) => s + cents(r.contractBaseSalary || '0'), 0n);
                const totalBonus = rows.reduce((s, r) => s + cents(r.bonus || '0'), 0n);
                const totalNet = rows.reduce((s, r) => s + cents(r.netSalary || '0'), 0n);

                $('#importPreview').innerHTML = `
                    <div class="info-note" style="margin-bottom:12px;background:#f0fdf4;border-color:#bbf7d0;color:#166534;">
                        <strong>✓ ${rows.length} nhân viên hợp lệ trong file Excel.</strong><br>
                        Tổng lương cơ bản: <strong>${money(decimal(totalContractBase))} ₫</strong> | 
                        Tổng thưởng: <strong>${money(decimal(totalBonus))} ₫</strong> | 
                        Tổng thực nhận: <strong style="color:#15803d;">${money(decimal(totalNet))} ₫</strong>
                    </div>
                    <p class="subtle" style="margin-bottom:6px;">Bản xem trước dữ liệu (tối đa 30 dòng):</p>
                    <div class="preview-table table-scroll" style="max-height:260px;overflow-y:auto;border:1px solid #e2e8f0;border-radius:6px;">
                        <table style="width:100%;font-size:12px;">
                            <thead>
                                <tr>
                                    <th>Mã NV</th>
                                    <th>Họ tên</th>
                                    <th class="number">Lương CB</th>
                                    <th class="number">Công thực/Chuẩn</th>
                                    <th class="number">Lương theo công</th>
                                    <th class="number">PC TN</th>
                                    <th class="number">PC đi lại</th>
                                    <th class="number">Thưởng</th>
                                    <th class="number">Thực nhận</th>
                                    <th>Ghi chú</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${rows.slice(0, 30).map(row => `
                                    <tr>
                                        <td><strong>${safe(row.employeeCode)}</strong></td>
                                        <td>${safe(row.fullName || '—')}</td>
                                        <td class="number">${money(row.contractBaseSalary)}</td>
                                        <td class="number">${days(row.actualWorkDays)} / ${days(row.standardWorkDays)}</td>
                                        <td class="number">${money(row.baseSalary)}</td>
                                        <td class="number">${money(row.responsibilityAllowance)}</td>
                                        <td class="number">${money(row.travelAllowance)}</td>
                                        <td class="number">${money(row.bonus)}</td>
                                        <td class="number net-value" style="font-weight:700;color:#1e40af;">${money(row.netSalary)}</td>
                                        <td><small>${safe(row.note || '—')}</small></td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>`;
                $('#importPreview').hidden = false;
                $('#saveImport').disabled = false;
                $('#saveImport').textContent = `Nhập bảng lương (${rows.length} nhân viên)`;
            } catch (error) { dialogError($('#importDialog'), error); }
            finally { state.busy = false; $('#previewImport').disabled = false; }
        });
        $('#importForm').addEventListener('submit', event => {
            event.preventDefault(); if ($('#saveImport').disabled) return;
            const data = new FormData(event.currentTarget); data.set('periodId', state.selected.payrollPeriodId);
            submit(event.currentTarget, '/payroll/api/import', data, () => loadPeriods(state.selected.payrollPeriodId));
        });
    }
    function slipPlaceholder(message, busy = false) {
        $('#slipRows').setAttribute('aria-busy', String(busy));
        $('#slipRows').innerHTML = `<tr><td colspan="${manageSlips ? 8 : 7}" class="empty">${safe(message)}</td></tr>`;
        $('#slipPagination').innerHTML = '';
    }
    async function loadSlips() {
        const request = ++state.slipRequest;
        state.slips = []; slipPlaceholder(HrmI18n.t('Đang tải phiếu lương…'), true); $('#notifications').innerHTML = '';
        try {
            const [slips, notifications] = await Promise.all([api('/payslips/api/slips'), api('/payslips/api/notifications')]);
            if (request !== state.slipRequest) return;
            state.slips = slips.data; state.slipPage = 1; const year = $('#slipYear').value;
            $('#slipYear').innerHTML = HrmI18n.fragment('<option value="">Tất cả năm</option>') + [...new Set(state.slips.map(s => s.year))].map(y => `<option>${safe(y)}</option>`).join('');
            $('#slipYear').value = state.slips.some(s => String(s.year) === year) ? year : '';
            $('#feedback').hidden = true; renderSlips();
            $('#notifications').innerHTML = notifications.data.filter(n => !n.readAt).map(n => `${HrmI18n.fragment("<div class=\"notification-item\"><p><strong>Phiếu lương mới đã sẵn sàng</strong><br>")}${safe(n.periodName)}<br><small>${safe(new Date(n.createdAt + 'Z').toLocaleString(HrmI18n.locale))}</small></p><div><a class="btn secondary" href="${context}/payslips/view?id=${safe(n.payslipId)}${HrmI18n.fragment("\">Xem phiếu</a><button class=\"text-button\" data-read=\"")}${safe(n.notificationId)}${HrmI18n.fragment("\">Đã đọc</button></div></div>")}`).join('');
        } catch (error) {
            if (request !== state.slipRequest) return;
            slipPlaceholder(HrmI18n.t('Không tải được phiếu lương. Chọn Làm mới để thử lại.')); throw error;
        }
    }
    async function loadSlipPeriods() {
        const request = ++state.slipListRequest;
        ++state.slipRequest; state.slips = []; state.selected = null;
        slipPlaceholder(HrmI18n.t('Đang tải kỳ lương…'), true); $('#slipPeriod').disabled = true;
        $('#slipPeriodStatus').textContent = HrmI18n.t('Đang tải kỳ lương…');
        try {
            const data = await api('/payroll/api/periods');
            if (request !== state.slipListRequest) return;
            state.periods = data.data;
            $('#slipPeriod').innerHTML = state.periods.map(p => `<option value="${safe(p.payrollPeriodId)}">${safe(p.periodName)} · ${p.month}/${p.year}</option>`).join('');
            const requested = new URLSearchParams(location.search).get('periodId');
            const selected = requested ?? String(state.periods[0]?.payrollPeriodId ?? '');
            if (requested !== null && !state.periods.some(p => String(p.payrollPeriodId) === requested))
                $('#slipPeriod').insertAdjacentHTML('afterbegin', `<option value="${safe(requested)}${HrmI18n.fragment("\">Kỳ lương không khả dụng</option>")}`);
            $('#slipPeriod').value = selected; $('#slipPeriod').disabled = false;
            if (!selected && requested === null) {
                $('#slipPeriodStatus').textContent = HrmI18n.t('Công ty chưa có kỳ lương.'); slipPlaceholder(HrmI18n.t('Chưa có phiếu lương.')); return;
            }
            await loadPeriodSlips(selected);
        } catch (error) {
            if (request !== state.slipListRequest) return;
            $('#slipPeriodStatus').textContent = HrmI18n.t('Không tải được kỳ lương.');
            slipPlaceholder(HrmI18n.t('Không tải được phiếu lương. Chọn Làm mới để thử lại.')); throw error;
        }
    }
    async function loadPeriodSlips(periodId) {
        const request = ++state.slipRequest;
        state.selected = null; state.slips = []; state.slipPage = 1;
        slipPlaceholder(HrmI18n.t('Đang tải phiếu lương…'), true); $('#slipPeriodStatus').textContent = HrmI18n.t('Đang tải phiếu lương…');
        const url = new URL(location.href); url.searchParams.set('periodId', periodId); history.replaceState(null, '', url);
        try {
            const result = await api('/payroll/api/period?view=payslips&periodId=' + encodeURIComponent(periodId));
            if (request !== state.slipRequest) return;
            state.selected = result.data.period; state.slips = result.data.slips;
            const p = state.selected;
            $('#slipPeriodStatus').innerHTML = `${badges(p)} <span>${p.publishedAt ? HrmI18n.t('Nhân viên đã có thể xem phiếu lương.') : HrmI18n.t('Chưa công bố cho nhân viên.')}</span> <a class="text-button" href="${context}/payroll?periodId=${p.payrollPeriodId}${HrmI18n.fragment("\">Mở kỳ lương →</a>")}`;
            $('#feedback').hidden = true; renderSlips();
        } catch (error) {
            if (request !== state.slipRequest) return;
            $('#slipPeriodStatus').textContent = HrmI18n.t('Không xem được phiếu lương của kỳ đã chọn.');
            slipPlaceholder(HrmI18n.t('Không tải được phiếu lương. Chọn Làm mới để thử lại.')); throw error;
        }
    }
    function renderSlips() {
        const normalize = value => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase();
        const q = manageSlips ? normalize($('#slipSearch').value.trim()) : '';
        const slips = state.slips.filter(s => manageSlips ? normalize(s.employeeCode + ' ' + s.fullName).includes(q) :
            !$('#slipYear').value || s.year === Number($('#slipYear').value));
        state.slipPage = Math.min(state.slipPage, Math.max(1, Math.ceil(slips.length / 25)));
        $('#slipRows').setAttribute('aria-busy', 'false');
        $('#slipRows').innerHTML = slips.slice((state.slipPage - 1) * 25, state.slipPage * 25).map(s => `<tr>${manageSlips ? `<td>${employee(s)}</td>` : `<td><strong>${safe(s.periodName)}${HrmI18n.fragment("</strong><br><small class=\"subtle\">Tháng ")}${s.month}/${s.year}</small></td>`}<td class="number">${money(s.baseSalary)}</td><td class="number">${money(s.responsibilityAllowance)}</td><td class="number">${money(s.travelAllowance ?? '0')}</td><td class="number">${money(s.bonus)}</td><td class="number net-value ${cents(s.netSalary) < 0n ? 'negative' : ''}">${money(s.netSalary)} ₫</td>${manageSlips ? `<td><span class="badge ${s.publishedAt ? 'Approved' : 'closed'}">${s.publishedAt ? HrmI18n.t('Đã công bố') : HrmI18n.t('Chưa công bố')}</span></td>` : ''}<td><a class="text-button" href="${context}/${manageSlips ? 'payroll/payslip' : 'payslips/view'}?id=${safe(s.payslipId)}${HrmI18n.fragment("\">Xem chi tiết →</a></td></tr>")}`).join('') || `<tr><td colspan="${manageSlips ? 8 : 7}" class="empty"><strong>${q ? HrmI18n.t('Không tìm thấy nhân viên') : HrmI18n.t('Chưa có phiếu lương')}</strong>${manageSlips ? HrmI18n.t('Chọn kỳ khác hoặc kiểm tra kết quả tính lương.') : HrmI18n.t('Phiếu sẽ xuất hiện sau khi công ty công bố kỳ lương.')}</td></tr>`;
        pagination('#slipPagination', state.slipPage, slips.length, 'slips', HrmI18n.t('phiếu'));
    }
    document.addEventListener('click', event => {
        const button = event.target.closest('button'); if (!button || state.busy) return;
        if (button.dataset.period) selectPeriod(Number(button.dataset.period)).catch(error => feedback(error.message, true));
        if (button.hasAttribute('data-retry-period') && state.failedPeriodId != null) selectPeriod(state.failedPeriodId).catch(error => feedback(error.message, true));
        if (button.dataset.action) confirmAction(button.dataset.action);
        if (button.hasAttribute('data-preview-payroll')) previewPayroll(button);
        if (button.dataset.adjust) openAdjustment(Number(button.dataset.adjust));
        if (button.dataset.salary) openSalary(Number(button.dataset.salary));
        if (button.dataset.pageKind) {
            if (button.dataset.pageKind === 'rows') { state.rowPage += Number(button.dataset.pageStep); renderRows(); }
            else if (button.dataset.pageKind === 'adjustments') { state.adjustmentPage += Number(button.dataset.pageStep); renderAdjustmentRows(); }
            else if (button.dataset.pageKind === 'slips') { state.slipPage += Number(button.dataset.pageStep); renderSlips(); }
            else { state.salaryPage += Number(button.dataset.pageStep); renderSalaries(); }
        }
        if (button.dataset.read) {
            button.disabled = true; api('/payslips/api/read', { notificationId: button.dataset.read }).then(loadSlips).catch(error => { feedback(error.message, true); button.disabled = false; });
        }
    });
    window.addEventListener('beforeunload', event => {
        if (adjustmentDirty()) { event.preventDefault(); event.returnValue = ''; }
    });
    document.addEventListener('click', event => {
        const link = event.target.closest('a[href]');
        if (!link || link.target === '_blank' || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button !== 0) return;
        if (state.adjustmentDraft) { event.preventDefault(); const href = link.href; discardAdjustment().then(confirmed => { if (confirmed) location.assign(href); }); }
    });
    if (page === 'payslips') {
        if (manageSlips) {
            $('#slipPeriod').addEventListener('change', () => loadPeriodSlips($('#slipPeriod').value).catch(error => feedback(error.message, true)));
            $('#slipSearch').addEventListener('input', () => { state.slipPage = 1; renderSlips(); });
        } else $('#slipYear').addEventListener('change', () => { state.slipPage = 1; renderSlips(); });
        const load = manageSlips ? loadSlipPeriods : loadSlips;
        $('#refreshSlips').addEventListener('click', () => load().catch(error => feedback(error.message, true)));
        load().catch(error => feedback(error.message, true)); return;
    }
    setupDialogs();
    if (page === 'salaries') {
        $('#salarySearch').addEventListener('input', () => { state.salaryPage = 1; renderSalaries(); });
        loadSalaries().catch(error => feedback(error.message, true)); return;
    }
    function parseWeekdayWeights(raw, mode) {
        const fallback = mode === 'MON_SAT' ? [1, 1, 1, 1, 1, 1, 0]
            : mode === 'MON_SAT_HALF' ? [1, 1, 1, 1, 1, 0.5, 0]
            : [1, 1, 1, 1, 1, 0, 0];
        if (!raw) return fallback;
        const parts = String(raw).split(',').map(v => Number(v.trim()));
        if (parts.length !== 7 || parts.some(v => Number.isNaN(v) || v < 0 || v > 1)) return fallback;
        return parts;
    }
    function setWeekdaySelects(weights) {
        const form = $('#formulaSettingsForm'); if (!form) return;
        form.querySelectorAll('[data-weekday-index]').forEach(sel => {
            const idx = Number(sel.dataset.weekdayIndex);
            const val = weights[idx] ?? 0;
            sel.value = val === 0.5 ? '0.5' : val === 1 ? '1' : '0';
        });
    }
    function getWeekdayWeightsFromForm() {
        const form = $('#formulaSettingsForm'); if (!form) return [1, 1, 1, 1, 1, 0, 0];
        return [0, 1, 2, 3, 4, 5, 6].map(idx => {
            const sel = form.querySelector(`[data-weekday-index="${idx}"]`);
            return sel ? Number(sel.value) : 0;
        });
    }
    function countStandardDaysInMonth(year, month, weights) {
        const daysInMonth = new Date(year, month, 0).getDate();
        let total = 0;
        for (let d = 1; d <= daysInMonth; d++) {
            const jsDay = new Date(year, month - 1, d).getDay(); // 0=Sun, 1=Mon..6=Sat
            const idx = jsDay === 0 ? 6 : jsDay - 1; // 0=Mon..6=Sun
            total += weights[idx] || 0;
        }
        return total;
    }
    function formatWeekdayShortSummary(weights) {
        const names = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
        const active = weights.map((w, i) => w === 1 ? names[i] : w === 0.5 ? `${names[i]}(0.5)` : null).filter(Boolean);
        return active.length ? active.join(', ') : '0 công';
    }
    function buildFormulaDescription(cfg) {
        if (!cfg) return 'Thực nhận = ((Lương cơ bản × Công thực) / Số công tiêu chuẩn) + Phụ cấp trách nhiệm + Phụ cấp đi lại + Thưởng';
        const parts = [];
        if (cfg.salaryCalculationMode === 'FIXED_MONTHLY') {
            parts.push('Lương cơ bản');
        } else {
            const weights = parseWeekdayWeights(cfg.weekdayWeights, cfg.standardDaysMode);
            const stdText = cfg.standardDaysMode === 'MON_SAT' ? 'Số công tiêu chuẩn (T2–T7)'
                : cfg.standardDaysMode === 'MON_SAT_HALF' ? 'Số công tiêu chuẩn (T2–T6 + 0.5 T7)'
                : cfg.standardDaysMode === 'CUSTOM_WEEKDAYS' ? `Số công tiêu chuẩn (${formatWeekdayShortSummary(weights)})`
                : cfg.standardDaysMode === 'FIXED' ? `Số công tiêu chuẩn (${days(cfg.fixedStandardDays || 22)} công cố định)`
                : 'Số công tiêu chuẩn (T2–T6)';
            parts.push(`((Lương cơ bản × Công thực${cfg.includePaidLeave !== false ? ' [gồm phép hưởng lương]' : ''}) / ${stdText})`);
        }
        if (cfg.includeResponsibilityAllowance !== false) parts.push('Phụ cấp trách nhiệm');
        if (cfg.includeTravelAllowance !== false) parts.push('Phụ cấp đi lại');
        parts.push('Thưởng');
        return 'Thực nhận = ' + parts.join(' + ');
    }
    function renderFormulaSummary() {
        const text = buildFormulaDescription(state.settings);
        const banner = $('#formulaSummaryText'); if (banner) banner.textContent = text;
        const subtle = $('#payrollFormulaSubtle'); if (subtle) subtle.textContent = text;
    }
    function updateFormulaDialogPreview(event) {
        const form = $('#formulaSettingsForm'); if (!form) return;
        const target = event?.target;
        let mode = form.elements.standardDaysMode.value;
        if (target && target.name === 'standardDaysMode') {
            if (mode === 'MON_FRI') setWeekdaySelects([1, 1, 1, 1, 1, 0, 0]);
            else if (mode === 'MON_SAT_HALF') setWeekdaySelects([1, 1, 1, 1, 1, 0.5, 0]);
            else if (mode === 'MON_SAT') setWeekdaySelects([1, 1, 1, 1, 1, 1, 0]);
        } else if (target && target.hasAttribute('data-weekday-index')) {
            const currentWeights = getWeekdayWeightsFromForm().join(',');
            if (currentWeights === '1,1,1,1,1,0,0') mode = 'MON_FRI';
            else if (currentWeights === '1,1,1,1,1,0.5,0') mode = 'MON_SAT_HALF';
            else if (currentWeights === '1,1,1,1,1,1,0') mode = 'MON_SAT';
            else mode = 'CUSTOM_WEEKDAYS';
            form.elements.standardDaysMode.value = mode;
        }
        const weights = getWeekdayWeightsFromForm();
        const weeklyTotal = weights.reduce((a, b) => a + b, 0);
        const refMonth = state.selected?.month || (new Date().getMonth() + 1);
        const refYear = state.selected?.year || new Date().getFullYear();
        const monthStdDays = countStandardDaysInMonth(refYear, refMonth, weights);
        const badge = $('#weekdaySummaryBadge');
        if (badge) {
            badge.textContent = `${days(weeklyTotal)} công/tuần · Tháng ${String(refMonth).padStart(2, '0')}/${refYear} = ${days(monthStdDays)} công chuẩn`;
        }
        const isFixed = mode === 'FIXED';
        const weekdayBox = $('#weekdayCustomizeBox');
        if (weekdayBox) weekdayBox.style.display = isFixed ? 'none' : 'block';
        const fixedField = $('#fixedStandardDaysField');
        if (fixedField) fixedField.style.display = isFixed ? 'block' : 'none';
        const cfg = {
            salaryCalculationMode: form.elements.salaryCalculationMode.value,
            standardDaysMode: mode,
            fixedStandardDays: form.elements.fixedStandardDays.value,
            weekdayWeights: weights.join(','),
            includePaidLeave: form.elements.includePaidLeave.checked,
            includeResponsibilityAllowance: form.elements.includeResponsibilityAllowance.checked,
            includeTravelAllowance: form.elements.includeTravelAllowance.checked
        };
        const preview = $('#formulaDialogPreview');
        if (preview) preview.innerHTML = '<strong>Công thức áp dụng:</strong><br>' + safe(buildFormulaDescription(cfg));
    }
    async function loadFormulaSettings() {
        try {
            const res = await api('/payroll/api/settings');
            state.settings = res.data;
            renderFormulaSummary();
        } catch (_) {}
    }
    function openFormulaSettings() {
        const form = $('#formulaSettingsForm'); if (!form) return;
        const s = state.settings || {
            salaryCalculationMode: 'WORKDAYS', standardDaysMode: 'MON_FRI', fixedStandardDays: '22',
            weekdayWeights: '1,1,1,1,1,0,0',
            includePaidLeave: true, includeResponsibilityAllowance: true, includeTravelAllowance: true
        };
        form.elements.salaryCalculationMode.value = s.salaryCalculationMode || 'WORKDAYS';
        form.elements.standardDaysMode.value = s.standardDaysMode || 'MON_FRI';
        form.elements.fixedStandardDays.value = s.fixedStandardDays ?? '22';
        setWeekdaySelects(parseWeekdayWeights(s.weekdayWeights, s.standardDaysMode));
        form.elements.includePaidLeave.checked = s.includePaidLeave !== false;
        form.elements.includeResponsibilityAllowance.checked = s.includeResponsibilityAllowance !== false;
        form.elements.includeTravelAllowance.checked = s.includeTravelAllowance !== false;
        updateFormulaDialogPreview();
        openDialog('#formulaSettingsDialog');
    }
    $('#openFormulaSettings')?.addEventListener('click', openFormulaSettings);
    $('#formulaSettingsForm')?.addEventListener('input', updateFormulaDialogPreview);
    $('#formulaSettingsForm')?.addEventListener('change', updateFormulaDialogPreview);
    document.querySelectorAll('[data-fixed-days-preset]').forEach(btn => {
        btn.addEventListener('click', () => {
            const form = $('#formulaSettingsForm'); if (!form) return;
            form.elements.fixedStandardDays.value = btn.dataset.fixedDaysPreset;
            updateFormulaDialogPreview();
        });
    });
    $('#formulaSettingsForm')?.addEventListener('submit', event => {
        event.preventDefault();
        const form = event.currentTarget;
        const payload = {
            salaryCalculationMode: form.elements.salaryCalculationMode.value,
            standardDaysMode: form.elements.standardDaysMode.value,
            fixedStandardDays: form.elements.fixedStandardDays.value || '22',
            weekdayWeights: getWeekdayWeightsFromForm().join(','),
            includePaidLeave: String(form.elements.includePaidLeave.checked),
            includeResponsibilityAllowance: String(form.elements.includeResponsibilityAllowance.checked),
            includeTravelAllowance: String(form.elements.includeTravelAllowance.checked)
        };
        submit(form, '/payroll/api/settings', payload, async result => {
            state.settings = result.data;
            renderFormulaSummary();
            if (state.selected) await selectPeriod(state.selected.payrollPeriodId);
        });
    });
    $('#openBonusCampaign')?.addEventListener('click', () => {
        if (!state.selected) {
            feedback(HrmI18n.t('Vui lòng chọn kỳ lương trước.'), true);
            return;
        }
        location.href = context + '/payroll/bonus-campaign?periodId=' + state.selected.payrollPeriodId;
    });
    function getPreviousMonthAndYear() {
        const now = new Date();
        let month = now.getMonth();
        let year = now.getFullYear();
        if (month === 0) {
            month = 12;
            year -= 1;
        }
        return { month, year };
    }
    function updateCreateMonthOptions(form) {
        if (!form) return;
        const now = new Date();
        const currentYear = now.getFullYear();
        const currentMonth = now.getMonth() + 1;
        const yearInput = form.elements.year;
        const selectedYear = parseInt(yearInput?.value, 10) || currentYear;
        const monthSelect = form.elements.month;
        if (!monthSelect) return;

        Array.from(monthSelect.options).forEach(opt => {
            const m = parseInt(opt.value, 10);
            const isFuture = (selectedYear > currentYear) || (selectedYear === currentYear && m >= currentMonth);
            opt.disabled = isFuture;
            opt.textContent = isFuture ? `Tháng ${m} (Chưa kết thúc)` : `Tháng ${m}`;
        });

        if (monthSelect.selectedOptions[0]?.disabled) {
            const enabledOptions = Array.from(monthSelect.options).filter(o => !o.disabled);
            if (enabledOptions.length) {
                monthSelect.value = enabledOptions[enabledOptions.length - 1].value;
            }
        }
    }
    $('#newPeriod').addEventListener('click', () => {
        const form = $('#createForm');
        form.reset();
        const prev = getPreviousMonthAndYear();
        form.elements.year.value = prev.year;
        updateCreateMonthOptions(form);
        form.elements.month.value = prev.month;
        form.elements.periodName.value = `Bảng lương tháng ${String(prev.month).padStart(2, '0')}/${prev.year}`;
        openDialog('#createDialog');
    });
    $('#createForm')?.elements.year?.addEventListener('input', () => {
        updateCreateMonthOptions($('#createForm'));
    });
    $('#yearFilter').addEventListener('change', () => changePeriodFilters().catch(error => feedback(error.message, true)));
    $('#statusFilter').addEventListener('change', () => changePeriodFilters().catch(error => feedback(error.message, true)));
    $('#refreshPeriods').addEventListener('click', () => loadPeriods().catch(error => feedback(error.message, true)));
    $('#rowSearch').addEventListener('input', () => { state.rowPage = 1; renderRows(); });
    $('#adjustmentSearch').addEventListener('input', () => { state.adjustmentPage = 1; renderAdjustmentRows(); });
    $('#templateButton').addEventListener('click', () => { location.href = context + '/payroll/api/template?periodId=' + state.selected.payrollPeriodId; });
    loadFormulaSettings();
    loadPeriods().catch(error => feedback(error.message, true));
})();
