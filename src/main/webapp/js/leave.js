/* Leave policy, discrete-date requests and decisions. User data is rendered as text. */
const LeaveApp = (() => {
    const $ = id => document.getElementById(id);
    const t = text => window.HrmI18n ? HrmI18n.t(text) : text;
    const statusText = value => t(({Pending: 'Chờ duyệt', Approved: 'Đã duyệt', Rejected: 'Từ chối', Cancelled: 'Đã hủy'})[value] || value);
    const vnDate = date => date ? date.split('-').reverse().join('/') : '—';
    const today = () => new Intl.DateTimeFormat('en-CA', {timeZone: 'Asia/Ho_Chi_Minh', year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
    let reloadList = () => {};

    function message(id, text) { if ($(id)) $(id).textContent = t(text || ''); }
    function button(text, action, className = 'button secondary') {
        const btn = document.createElement('button');
        btn.type = 'button'; btn.className = className; btn.textContent = t(text);
        btn.addEventListener('click', action);
        return btn;
    }
    async function api(path, data) {
        const options = {headers: {'Accept':'application/json'}};
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
        const form = $('leaveSubmitForm'), input = $('leaveDate'), submit = $('btnSubmitLeave');
        let earliest, policy;
        function render() {
            $('selectedDates').replaceChildren();
            [...selected].sort().forEach(date => {
                const li = document.createElement('li');
                li.textContent = vnDate(date) + ' ';
                li.appendChild(button('Bỏ ngày', () => { selected.delete(date); render(); }));
                $('selectedDates').appendChild(li);
            });
            message('selectedDayCount', `Đã chọn ${selected.size} ngày`);
            submit.disabled = !policy || selected.size === 0;
        }
        $('addLeaveDate').addEventListener('click', () => {
            input.setCustomValidity('');
            const date = input.value;
            const weekday = date ? new Date(date + 'T12:00:00').getDay() : -1;
            if (!date || date < earliest || weekday === 0 || weekday === 6) {
                input.setCustomValidity(t('Chỉ chọn ngày làm việc từ ngày mai, không chọn Thứ Bảy/Chủ Nhật.'));
                input.reportValidity(); return;
            }
            if (selected.has(date)) { message('submitMessage','Ngày nghỉ đã được chọn.'); return; }
            if (selected.size >= policy.maxDaysPerRequest) { message('submitMessage','Đã đạt giới hạn số ngày mỗi đơn.'); return; }
            selected.add(date); input.value = ''; message('submitMessage',''); render();
        });
        input.addEventListener('input', () => input.setCustomValidity(''));
        form.addEventListener('submit', async event => {
            event.preventDefault();
            const data = new URLSearchParams();
            [...selected].sort().forEach(date => data.append('dates',date));
            data.set('reason',$('reason').value);
            submit.disabled = true;
            try { await api('submit',data); window.location.href = 'my-requests'; }
            catch (error) { message('submitMessage',error.message); render(); }
        });
        try {
            const result = await api('policy'); earliest = result.earliestDate; policy = result.policy;
            input.min = earliest;
            if (!policy) { message('submitMessage','Công ty chưa cấu hình quota nghỉ phép.'); return; }
            message('policyNotice',`Tối đa ${policy.maxDaysPerRequest} ngày làm việc mỗi đơn. Quota ${policy.quotaDays} ngày/${policy.quotaMode === 'ANNUAL' ? 'năm' : 'tháng'}.`);
            $('addLeaveDate').disabled = false;
        } catch (error) { message('submitMessage',error.message); }
    }

    async function viewRequestDetail(id) {
        try {
            const {data: r} = await api(`detail?leaveRequestId=${id}`);
            const body = $('detailModalBody'); body.replaceChildren(); message('detailMessage','');
            const fields = [
                ['Tên đơn',r.title], ['Người tạo',r.employeeFullName], ['Mã nhân viên',r.employeeCode],
                ['Chức danh',r.positionTitle], ['Phòng ban',r.departmentName], ['Số ngày nghỉ',r.totalDays],
                ['Ngày nghỉ',r.legacy ? `${vnDate(r.startDate)} – ${vnDate(r.endDate)} (lịch sử theo khoảng ngày)` : r.dates.map(vnDate).join(', ')],
                ['Lý do',r.reason], ['Trạng thái',statusText(r.status)], ['Người duyệt',r.approverDisplayName],
                ['Phản hồi',r.approverComment], ['Thời gian gửi',r.createdAt], ['Thời gian duyệt',r.reviewedAt]
            ];
            const dl = document.createElement('dl');
            fields.forEach(([label,value]) => {
                const dt = document.createElement('dt'), dd = document.createElement('dd');
                dt.textContent = t(label); dt.style.fontWeight = '600'; dd.textContent = value ?? '—'; dd.style.whiteSpace = 'pre-wrap';
                dl.append(dt,dd);
            }); body.appendChild(dl);
            const actions = $('detailActions'); actions.replaceChildren();
            if (r.canReview) {
                const label = document.createElement('label'); label.textContent = t('Phản hồi (bắt buộc nếu từ chối)');
                const comment = document.createElement('textarea'); comment.maxLength = 300; comment.className = 'form-control'; comment.id = 'reviewComment';
                body.append(label,comment);
                async function decide(approved) {
                    const buttons = [...actions.querySelectorAll('button')]; buttons.forEach(b => b.disabled = true);
                    try {
                        if (!approved && !comment.value.trim()) throw new Error(t('Vui lòng nhập lý do từ chối.'));
                        await api(approved ? 'approve' : 'reject',new URLSearchParams({leaveRequestId:id,comment:comment.value}));
                        $('leaveDetailDialog').close(); reloadList();
                    } catch (error) { message('detailMessage',error.message); }
                    finally { buttons.forEach(b => b.disabled = false); }
                }
                if (!r.legacy) actions.appendChild(button('Duyệt',() => decide(true),'button'));
                actions.appendChild(button('Từ chối',() => decide(false)));
            }
            if (r.canCancel) actions.appendChild(button('Hủy đơn',async () => {
                if (!(await AppDialog.confirm(t('Bạn có chắc chắn muốn hủy đơn đang chờ duyệt?')))) return;
                try { await api('cancel',new URLSearchParams({leaveRequestId:id})); $('leaveDetailDialog').close(); reloadList(); }
                catch(error) { message('detailMessage',error.message); }
            }));
            $('leaveDetailDialog').showModal();
        } catch(error) { message('listMessage',error.message); }
    }

    function initRequests(own) {
        const table = own ? $('requestsTableBody') : $('pendingTableBody');
        let page = 1;
        const pendingMode = !own && !$('btnHistory');

        function currentQuery() {
            const params = new URLSearchParams({page: String(page)});
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
            const params = new URLSearchParams({page: String(page)});
            params.set('status', $('statusFilter')?.value || 'ALL');
            return params.toString();
        }

        function renderRows(rows) {
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
            });
        }

        async function load() {
            message('listMessage', 'Đang tải...');
            try {
                const query = own ? ownQuery() : currentQuery();
                let result;
                if (own) result = await api(`my-requests?${query}`);
                else if (pendingMode) result = await api(`pending-approvals?${query}`);
                else result = await api(`all-requests?${query}`);
                renderRows(result.data);
                message('listMessage', result.data.length ? '' : (pendingMode ? 'Không có đơn chờ duyệt.' : 'Chưa có đơn nghỉ phép.'));
                renderPager(result);
            } catch (error) { message('listMessage', error.message); }
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

        reloadList = load;
        load();
    }

    async function initPolicy() {
        const form = $('leavePolicyForm');
        function preview() {
            const days = Number($('quotaDays').value), mode = $('quotaMode').value;
            message('allocationPreview',mode === 'ANNUAL' ? `Tháng 1: ${Math.floor(days/12)+days%12} ngày; tháng 2–12: ${Math.floor(days/12)} ngày/tháng.` : `Mỗi tháng: ${days} ngày.`);
        }
        form.addEventListener('input',preview);
        form.addEventListener('submit',async event => {
            event.preventDefault(); const btn=form.querySelector('button[type="submit"]'); btn.disabled=true;
            try { await api('policy',new URLSearchParams(new FormData(form))); message('policyMessage','Đã lưu cấu hình.'); }
            catch(error) { message('policyMessage',error.message); }
            finally {btn.disabled=false;}
        });
        try {
            const {policy} = await api('policy');
            if (policy) { ['quotaMode','quotaDays','maxDaysPerRequest'].forEach(key => $(key).value=policy[key]); preview(); }
            else message('policyMessage','Chưa có cấu hình. Vui lòng nhập quota chung.');
        } catch(error) { message('policyMessage',error.message); }
    }

    document.addEventListener('DOMContentLoaded',() => {
        if ($('leaveSubmitForm')) initSubmitForm();
        if ($('leavePolicyForm')) initPolicy();
        if ($('requestsTableBody')) initRequests(true);
        if ($('pendingTableBody')) initRequests(false);
    });
    return {viewRequestDetail};
})();
