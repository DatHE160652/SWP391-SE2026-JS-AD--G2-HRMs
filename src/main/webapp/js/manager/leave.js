/* Leave policy, discrete-date requests and decisions. User data is rendered as text. */
const LeaveApp = (() => {
    const $ = id => document.getElementById(id);
    const t = text => window.HrmI18n ? HrmI18n.t(text) : text;
    const statusText = value => t(({ Pending: 'Chờ duyệt', Approved: 'Đã duyệt', Rejected: 'Từ chối', Cancelled: 'Đã hủy' })[value] || value);
    const vnDate = date => date ? date.split('-').reverse().join('/') : '—';
    const today = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
    let reloadList = () => { };

    function message(id, text, type = 'success') {
        const el = $(id);
        if (!el) return;
        el.textContent = t(text || '');
        if (el.classList.contains('message')) {
            el.className = text ? `message show ${type}`.trim() : 'message';
        }
    }
    function button(text, action, className = 'button secondary') {
        const btn = document.createElement('button');
        btn.type = 'button'; btn.className = className; btn.textContent = t(text);
        btn.addEventListener('click', action);
        return btn;
    }
    async function api(path, data) {
        const options = { headers: { 'Accept': 'application/json' } };
        if (data) {
            data.set('csrfToken', document.querySelector('meta[name="organization-csrf-token"]')?.content || '');
            options.method = 'POST'; options.body = data;
        }
        const response = await fetch(`../leave/api/${path}`, options);
        const result = await response.json();
        if (!response.ok || !result.success) throw new Error(result.message || t('Không thể xử lý yêu cầu.'));
        return result;
    }
    function cell(row, value) { const td = row.insertCell(); td.textContent = value ?? '—'; return td; }

    // A native date input adds one working date at a time; no span is inferred.
    async function initSubmitForm() {
        const selected = new Set();
        const approvedDates = new Set();
        const pendingDates = new Set();
        const form = $('leaveSubmitForm'), input = $('leaveDate'), submit = $('btnSubmitLeave');
        let earliest, policy, usedByMonth = {};

        function expandDates(r) {
            if (Array.isArray(r.dates) && r.dates.length) return r.dates;
            if (!r.startDate || !r.endDate) return [];
            const list = [];
            const cur = new Date(r.startDate + 'T12:00:00');
            const end = new Date(r.endDate + 'T12:00:00');
            while (cur <= end) {
                const wd = cur.getDay();
                if (wd !== 0 && wd !== 6) {
                    const y = cur.getFullYear();
                    const m = String(cur.getMonth() + 1).padStart(2, '0');
                    const d = String(cur.getDate()).padStart(2, '0');
                    list.push(`${y}-${m}-${d}`);
                }
                cur.setDate(cur.getDate() + 1);
            }
            return list;
        }

        async function syncExistingRequests() {
            try {
                for (let p = 1; p <= 5; p++) {
                    const res = await api(`my-requests?status=ALL&page=${p}`);
                    (res.data || []).forEach(r => {
                        if (r.status === 'Approved') {
                            expandDates(r).forEach(d => approvedDates.add(d));
                        } else if (r.status === 'Pending') {
                            expandDates(r).forEach(d => pendingDates.add(d));
                        }
                    });
                    if (!res.hasMore) break;
                }
                const countByMonth = {};
                approvedDates.forEach(d => {
                    const ym = d.slice(0, 7);
                    countByMonth[ym] = (countByMonth[ym] || 0) + 1;
                });
                Object.entries(countByMonth).forEach(([ym, cnt]) => {
                    usedByMonth[ym] = Math.max(Number(usedByMonth[ym] || 0), cnt);
                });
            } catch (_) { }
        }

        function quotaForMonth(ym) {
            if (!policy || !ym) return 0;
            const monthIdx = Number(ym.split('-')[1]) - 1;
            return Array.isArray(policy.monthlyDays) && policy.monthlyDays[monthIdx] !== undefined
                ? Number(policy.monthlyDays[monthIdx])
                : Number(policy.quotaDays || 0);
        }

        function usedInMonth(ym) {
            return Number(usedByMonth[ym] || 0);
        }

        function selectedInMonth(ym) {
            let count = 0;
            selected.forEach(d => { if (d.startsWith(ym)) count++; });
            return count;
        }

        function updateQuotaSummary() {
            const notice = $('policyNotice');
            if (!notice || !policy) return;
            const activeDate = input.value || ([...selected].sort()[0]) || earliest || today();
            const ym = activeDate.slice(0, 7);
            const [y, m] = ym.split('-');
            const monthQuota = quotaForMonth(ym);
            const used = usedInMonth(ym);
            const picking = selectedInMonth(ym);
            const remainBeforePick = Math.max(0, monthQuota - used);
            const remainAfterPick = Math.max(0, remainBeforePick - picking);
            const monthTag = `${t('Tháng')} ${Number(m)}/${y}`;

            notice.replaceChildren();
            const cards = [
                ['max', 'Số ngày nghỉ tối đa trong tháng', monthTag, monthQuota],
                ['used', 'Số ngày nghỉ đã được duyệt', monthTag, used],
                ['remaining' + (remainAfterPick === 0 ? ' exhausted' : ''), 'Số ngày nghỉ còn lại', picking > 0 ? `${monthTag} (${t('Đang chọn')}: ${picking})` : monthTag, remainAfterPick]
            ];
            cards.forEach(([cls, label, sub, val]) => {
                const card = document.createElement('div');
                card.className = `quota-stat-card ${cls}`;
                const info = document.createElement('div');
                info.className = 'quota-stat-info';
                const lbl = document.createElement('span');
                lbl.className = 'quota-stat-label';
                lbl.textContent = t(label);
                const subEl = document.createElement('span');
                subEl.className = 'quota-stat-sub';
                subEl.textContent = sub;
                info.append(lbl, subEl);

                const badge = document.createElement('div');
                badge.className = 'quota-stat-value';
                badge.textContent = String(val) + ' ';
                const unit = document.createElement('small');
                unit.textContent = t('ngày');
                badge.appendChild(unit);

                card.append(info, badge);
                notice.appendChild(card);
            });
        }

        function render() {
            $('selectedDates').replaceChildren();
            [...selected].sort().forEach(date => {
                const li = document.createElement('li');
                li.textContent = vnDate(date) + ' ';
                li.appendChild(button('Bỏ ngày', () => { selected.delete(date); setDateError(''); render(); }));
                $('selectedDates').appendChild(li);
            });
            message('selectedDayCount', `Đã chọn ${selected.size} ngày`);
            updateQuotaSummary();
            submit.disabled = !policy || selected.size === 0;
        }
        function setDateError(errText) {
            const err = t(errText || '');
            if ($('leaveDateError')) $('leaveDateError').textContent = err;
            input.setAttribute('aria-invalid', err ? 'true' : 'false');
        }
        function validateCandidateDate(date) {
            if (!date) return 'Vui lòng chọn ngày nghỉ.';
            if (earliest && date < earliest) return 'Chỉ được chọn ngày nghỉ từ ngày mai.';
            const weekday = new Date(date + 'T12:00:00').getDay();
            if (weekday === 0 || weekday === 6) return 'Không được chọn Thứ Bảy hoặc Chủ Nhật.';
            if (selected.has(date)) return 'Ngày nghỉ đã được chọn.';
            if (approvedDates.has(date)) return `Ngày ${vnDate(date)} đã có đơn được duyệt.`;
            if (pendingDates.has(date)) return `Ngày ${vnDate(date)} đã có đơn đang chờ duyệt.`;
            if (policy) {
                const ym = date.slice(0, 7);
                const [y, m] = ym.split('-');
                const monthQuota = quotaForMonth(ym);
                const used = usedInMonth(ym);
                const picking = selectedInMonth(ym);
                const remainBeforePick = Math.max(0, monthQuota - used);
                if (used + picking + 1 > monthQuota) {
                    return `Vượt quá hạn mức tháng ${Number(m)}/${y} (Được cấp ${monthQuota} ngày, đã duyệt ${used} ngày, còn lại ${remainBeforePick} ngày).`;
                }
            }
            return '';
        }
        $('addLeaveDate').addEventListener('click', () => {
            const date = input.value;
            const err = validateCandidateDate(date);
            if (err) {
                setDateError(err);
                return;
            }
            selected.add(date); input.value = ''; setDateError(''); render();
        });
        input.addEventListener('input', () => {
            const date = input.value;
            updateQuotaSummary();
            if (date) {
                const err = validateCandidateDate(date);
                if (err) {
                    setDateError(err);
                    return;
                }
            }
            setDateError('');
        });
        form.addEventListener('submit', async event => {
            event.preventDefault();
            const data = new URLSearchParams();
            [...selected].sort().forEach(date => data.append('dates', date));
            data.set('reason', $('reason').value);
            submit.disabled = true;
            try {
                await api('submit', data);
                sessionStorage.setItem('leaveMessage', 'Đã tạo đơn nghỉ phép.');
                window.location.href = 'my-requests';
            }
            catch (error) { message('submitMessage', error.message, 'error'); render(); }
        });
        try {
            const result = await api('policy');
            earliest = result.earliestDate;
            policy = result.policy;
            usedByMonth = result.usedByMonth || {};
            input.min = earliest;
            if (!policy) { message('submitMessage', 'Công ty chưa cấu hình quota nghỉ phép.', 'error'); return; }
            await syncExistingRequests();
            updateQuotaSummary();
            $('addLeaveDate').disabled = false;
        } catch (error) { message('submitMessage', error.message, 'error'); }
    }

    async function viewRequestDetail(id) {
        try {
            const { data: r } = await api(`detail?leaveRequestId=${id}`);
            const body = $('detailModalBody'); body.replaceChildren(); message('detailMessage', '');
            const fields = [
                ['Tên đơn', r.title], ['Người tạo', r.employeeFullName], ['Mã nhân viên', r.employeeCode],
                ['Chức danh', r.positionTitle], ['Phòng ban', r.departmentName], ['Số ngày nghỉ', r.totalDays],
                ['Ngày nghỉ', r.legacy ? `${vnDate(r.startDate)} – ${vnDate(r.endDate)} (lịch sử theo khoảng ngày)` : r.dates.map(vnDate).join(', ')],
                ['Lý do', r.reason], ['Trạng thái', statusText(r.status)], ['Người duyệt', r.approverDisplayName],
                ['Phản hồi', r.approverComment], ['Thời gian gửi', r.createdAt], ['Thời gian duyệt', r.reviewedAt]
            ];
            const dl = document.createElement('dl');
            fields.forEach(([label, value]) => {
                const dt = document.createElement('dt'), dd = document.createElement('dd');
                dt.textContent = t(label); dt.style.fontWeight = '600'; dd.textContent = value ?? '—'; dd.style.whiteSpace = 'pre-wrap';
                dl.append(dt, dd);
            }); body.appendChild(dl);
            const actions = $('detailActions'); actions.replaceChildren();
            if (r.canReview) {
                const label = document.createElement('label'); label.textContent = t('Phản hồi (bắt buộc nếu từ chối)');
                const comment = document.createElement('textarea'); comment.maxLength = 300; comment.className = 'form-control'; comment.id = 'reviewComment';
                body.append(label, comment);
                async function decide(approved) {
                    if (!approved && !comment.value.trim()) {
                        message('detailMessage', 'Vui lòng nhập lý do từ chối.');
                        comment.focus();
                        return;
                    }
                    const confirmText = approved
                        ? `${t('Bạn có chắc chắn muốn duyệt đơn nghỉ phép của')} ${r.employeeFullName || ''}?`
                        : `${t('Bạn có chắc chắn muốn từ chối đơn nghỉ phép của')} ${r.employeeFullName || ''}?`;
                    if (!(await AppDialog.confirm(confirmText))) return;
                    const buttons = [...actions.querySelectorAll('button')]; buttons.forEach(b => b.disabled = true);
                    try {
                        await api(approved ? 'approve' : 'reject', new URLSearchParams({ leaveRequestId: id, comment: comment.value }));
                        $('leaveDetailDialog').close();
                        message('actionMessage', approved ? 'Đã duyệt đơn nghỉ phép.' : 'Đã từ chối đơn nghỉ phép.');
                        reloadList();
                    } catch (error) { message('detailMessage', error.message); }
                    finally { buttons.forEach(b => b.disabled = false); }
                }
                if (!r.legacy) actions.appendChild(button('Duyệt', () => decide(true), 'button'));
                actions.appendChild(button('Từ chối', () => decide(false)));
            }
            if (r.canCancel) actions.appendChild(button('Hủy đơn', async () => {
                if (!(await AppDialog.confirm(t('Bạn có chắc chắn muốn hủy đơn đang chờ duyệt?')))) return;
                try {
                    await api('cancel', new URLSearchParams({ leaveRequestId: id }));
                    $('leaveDetailDialog').close();
                    message('actionMessage', 'Đã hủy đơn nghỉ phép.');
                    reloadList();
                }
                catch (error) { message('detailMessage', error.message); }
            }));
            $('leaveDetailDialog').showModal();
        } catch (error) { message('actionMessage', error.message, 'error'); }
    }

    function initRequests(own) {
        const table = own ? $('requestsTableBody') : $('pendingTableBody');
        let page = 1;
        const pendingMode = !own && !$('btnHistory');

        function currentQuery() {
            const params = new URLSearchParams({ page: String(page) });
            params.set('status', pendingMode ? 'Pending' : ($('filterStatus')?.value || 'ALL'));
            const date = $('filterDate')?.value;
            const month = $('filterMonth')?.value;
            const year = $('filterYear')?.value;
            if (date && month && year) params.set('date', `${year}-${String(month).padStart(2, '0')}-${String(date).padStart(2, '0')}`);
            else {
                if (month) params.set('month', month);
                if (year) params.set('year', year);
            }
            return params.toString();
        }

        function ownQuery() {
            const params = new URLSearchParams({ page: String(page) });
            params.set('status', $('statusFilter')?.value || 'ALL');
            return params.toString();
        }

        function renderEmptyRow(text, isError = false) {
            table.replaceChildren();
            const row = table.insertRow();
            const td = row.insertCell();
            td.colSpan = 6;
            td.style.textAlign = 'center';
            td.style.padding = '24px 16px';
            td.style.color = isError ? '#b42318' : 'var(--lc-muted, #667085)';
            td.textContent = t(text);
        }

        async function inlineApprove(r, btn) {
            if (!(await AppDialog.confirm(`${t('Bạn có chắc chắn muốn duyệt đơn nghỉ phép của')} ${r.employeeFullName || ''}?`))) return;
            btn.disabled = true;
            try {
                await api('approve', new URLSearchParams({ leaveRequestId: r.leaveRequestId, comment: '' }));
                message('actionMessage', 'Đã duyệt đơn nghỉ phép.');
                load();
            } catch (error) {
                message('actionMessage', error.message, 'error');
                btn.disabled = false;
            }
        }

        async function inlineReject(r, btn) {
            const reason = await AppDialog.prompt(
                `${t('Bạn có chắc chắn muốn từ chối đơn nghỉ phép của')} ${r.employeeFullName || ''}?`,
                { title: t('Xác nhận từ chối đơn nghỉ phép'), label: t('Lý do từ chối'), placeholder: t('Nhập lý do từ chối...') }
            );
            if (!reason) return;
            btn.disabled = true;
            try {
                await api('reject', new URLSearchParams({ leaveRequestId: r.leaveRequestId, comment: reason }));
                message('actionMessage', 'Đã từ chối đơn nghỉ phép.');
                load();
            } catch (error) {
                message('actionMessage', error.message, 'error');
                btn.disabled = false;
            }
        }

        async function inlineCancel(r, btn) {
            if (!(await AppDialog.confirm(t('Bạn có chắc chắn muốn hủy đơn đang chờ duyệt?')))) return;
            btn.disabled = true;
            try {
                await api('cancel', new URLSearchParams({ leaveRequestId: r.leaveRequestId }));
                message('actionMessage', 'Đã hủy đơn nghỉ phép.');
                load();
            } catch (error) {
                message('actionMessage', error.message, 'error');
                btn.disabled = false;
            }
        }

        function renderRows(rows) {
            if (!rows || !rows.length) {
                renderEmptyRow(pendingMode ? 'Không có đơn chờ duyệt.' : 'Chưa có đơn nghỉ phép.');
                return;
            }
            table.replaceChildren();
            rows.forEach(r => {
                const row = table.insertRow();
                const nameCell = row.insertCell();
                nameCell.appendChild(button(r.title, () => viewRequestDetail(r.leaveRequestId), 'btn-action view'));
                cell(row, r.employeeFullName);
                cell(row, r.legacy
                    ? `${vnDate(r.startDate)} – ${vnDate(r.endDate)}`
                    : (r.dates && r.dates.length ? r.dates.map(vnDate).join(', ') : `${vnDate(r.startDate)} – ${vnDate(r.endDate)}`));
                cell(row, r.totalDays);
                const statusCell = row.insertCell();
                const statusBadge = document.createElement('span');
                statusBadge.textContent = statusText(r.status);
                statusBadge.className = 'leave-status status-' + String(r.status || '').toLowerCase();
                statusCell.appendChild(statusBadge);

                const actionCell = row.insertCell();
                actionCell.style.textAlign = 'center';
                const group = document.createElement('div');
                group.className = 'btn-action-group';
                if (r.canReview) {
                    if (!r.legacy) {
                        const approveBtn = button('Duyệt', () => inlineApprove(r, approveBtn), 'button small');
                        group.appendChild(approveBtn);
                    }
                    const rejectBtn = button('Từ chối', () => inlineReject(r, rejectBtn), 'button secondary small');
                    group.appendChild(rejectBtn);
                } else if (r.canCancel) {
                    const cancelBtn = button('Hủy đơn', () => inlineCancel(r, cancelBtn), 'button secondary small');
                    group.appendChild(cancelBtn);
                }
                if (group.childElementCount > 0) {
                    actionCell.appendChild(group);
                } else {
                    actionCell.textContent = '—';
                }
            });
        }

        async function load() {
            message('listMessage', '');
            renderEmptyRow('Đang tải...');
            try {
                const query = own ? ownQuery() : currentQuery();
                let result;
                if (own) result = await api(`my-requests?${query}`);
                else if (pendingMode) result = await api(`pending-approvals?${query}`);
                else result = await api(`all-requests?${query}`);
                renderRows(result.data);
                renderPager(result);
            } catch (error) { renderEmptyRow(error.message, true); }
        }

        function renderPager(result) {
            const pager = $('paginationBar');
            if (!pager) return;
            pager.replaceChildren();
            const prev = button('Trang trước', () => { page--; load(); });
            prev.disabled = page <= 1;
            const next = button('Trang sau', () => { page++; load(); });
            next.disabled = !result.hasMore;
            const label = document.createElement('span');
            label.textContent = ` ${t('Trang')} ${page} `;
            pager.append(prev, label, next);
        }

        function resetPage() { page = 1; load(); }

        // Year -> Month -> Day cascade: later selects stay disabled until the previous one is chosen.
        function fillYears() {
            const yearSelect = $('filterYear');
            if (!yearSelect) return;
            const current = new Date().getFullYear();
            for (let y = current + 1; y >= current - 5; y--) {
                const option = document.createElement('option');
                option.value = String(y);
                option.textContent = String(y);
                yearSelect.appendChild(option);
            }
        }

        function fillDays() {
            const daySelect = $('filterDate');
            if (!daySelect) return;
            const year = Number($('filterYear')?.value || 0);
            const month = Number($('filterMonth')?.value || 0);
            daySelect.replaceChildren(new Option('Tất cả', ''));
            if (!year || !month) { daySelect.disabled = true; return; }
            daySelect.disabled = false;
            const days = new Date(year, month, 0).getDate();
            for (let d = 1; d <= days; d++) daySelect.appendChild(new Option(String(d), String(d)));
        }

        function syncCascade() {
            const monthSelect = $('filterMonth');
            const yearSelect = $('filterYear');
            if (monthSelect) monthSelect.disabled = !yearSelect?.value;
            fillDays();
        }

        fillYears();
        syncCascade();

        $('statusFilter')?.addEventListener('change', resetPage);

        $('filterYear')?.addEventListener('change', () => {
            if ($('filterMonth')) $('filterMonth').value = '';
            syncCascade();
            resetPage();
        });

        $('filterMonth')?.addEventListener('change', () => {
            fillDays();
            resetPage();
        });

        $('filterDate')?.addEventListener('change', resetPage);
        $('filterStatus')?.addEventListener('change', resetPage);

        $('btnResetFilter')?.addEventListener('click', () => {
            if ($('filterYear')) $('filterYear').value = '';
            if ($('filterMonth')) $('filterMonth').value = '';
            if ($('filterDate')) $('filterDate').value = '';
            if ($('filterStatus')) $('filterStatus').value = 'ALL';
            syncCascade();
            resetPage();
        });

        $('btnPendingOnly')?.addEventListener('click', () => {
            if ($('filterStatus')) $('filterStatus').value = 'Pending';
            resetPage();
        });

        $('btnHistory')?.addEventListener('click', () => {
            if ($('filterStatus')) $('filterStatus').value = 'ALL';
            resetPage();
        });

        const savedMessage = sessionStorage.getItem('leaveMessage');
        if (savedMessage) {
            message('actionMessage', savedMessage);
            sessionStorage.removeItem('leaveMessage');
        }

        reloadList = load;
        load();
    }

    async function initPolicy() {
        const form = $('leavePolicyForm');
        function syncQuotaLimit() {
            const mode = $('quotaMode').value;
            $('quotaDays').max = mode === 'MONTHLY' ? '29' : '366';
        }
        function preview() {
            syncQuotaLimit();
            const rawDays = Number($('quotaDays').value);
            const days = Number.isFinite(rawDays) && rawDays >= 0 ? Math.floor(rawDays) : 0;
            const mode = $('quotaMode').value;
            if (mode !== 'ANNUAL') {
                message('allocationPreview', `Mỗi tháng: ${days} ngày.`);
                return;
            }
            const base = Math.floor(days / 12), rem = days % 12;
            if (rem === 0) {
                message('allocationPreview', `Tháng 1–12: ${base} ngày/tháng.`);
            } else if (rem === 1) {
                message('allocationPreview', `Tháng 1: ${base + 1} ngày; tháng 2–12: ${base} ngày/tháng.`);
            } else if (rem === 11) {
                message('allocationPreview', `Tháng 1–11: ${base + 1} ngày/tháng; tháng 12: ${base} ngày.`);
            } else {
                message('allocationPreview', `Tháng 1–${rem}: ${base + 1} ngày/tháng; tháng ${rem + 1}–12: ${base} ngày/tháng.`);
            }
        }
        form.addEventListener('input', preview);
        form.addEventListener('submit', async event => {
            event.preventDefault();
            const mode = $('quotaMode').value;
            const rawDays = Number($('quotaDays').value);
            if (mode === 'MONTHLY' && (!Number.isInteger(rawDays) || rawDays < 0 || rawDays >= 30)) {
                message('policyMessage', 'Quota theo tháng phải dưới 30 ngày (0–29 ngày).', 'error');
                return;
            }
            const btn = form.querySelector('button[type="submit"]'); btn.disabled = true;
            try { await api('policy', new URLSearchParams(new FormData(form))); message('policyMessage', 'Đã lưu cấu hình.'); }
            catch (error) { message('policyMessage', error.message, 'error'); }
            finally { btn.disabled = false; }
        });
        try {
            const { policy } = await api('policy');
            if (policy) { ['quotaMode', 'quotaDays'].forEach(key => $(key).value = policy[key]); preview(); }
            else message('policyMessage', 'Chưa có cấu hình. Vui lòng nhập quota chung.', 'error');
        } catch (error) { message('policyMessage', error.message, 'error'); }
    }

    document.addEventListener('DOMContentLoaded', () => {
        if ($('leaveSubmitForm')) initSubmitForm();
        if ($('leavePolicyForm')) initPolicy();
        if ($('requestsTableBody')) initRequests(true);
        if ($('pendingTableBody')) initRequests(false);
    });
    return { viewRequestDetail };
})();
