const AttendanceApi = {
    async get(url) {
        const response = await fetch(withContext(url), { headers: { 'Accept': 'application/json' } });
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

document.addEventListener('DOMContentLoaded', async () => {
    const page = document.body.dataset.page;
    if (page === 'employee-attendance') {
        await initEmployeeAttendance();
    } else if (page === 'manager-attendance') {
        await initManagerAttendance();
    } else if (page === 'company-attendance') {
        await initCompanyAttendance();
    } else if (page === 'attendance-report') {
        await initAttendanceReport();
    }
});

/* =========================================================================
   UC31: Employee Personal Attendance (Read-Only)
   ========================================================================= */
async function initEmployeeAttendance() {
    const meta = await loadMeta();
    populateEmployeeSelect('employeeSelect', meta.employees);

    const today = new Date();
    document.getElementById('monthSelect').value = today.getMonth() + 1;
    document.getElementById('yearSelect').value = today.getFullYear();

    document.getElementById('btnFilter').addEventListener('click', loadPersonalAttendance);
    document.getElementById('employeeSelect').addEventListener('change', loadPersonalAttendance);

    await loadPersonalAttendance();
}

async function loadPersonalAttendance() {
    const employeeId = document.getElementById('employeeSelect').value;
    const month = document.getElementById('monthSelect').value;
    const year = document.getElementById('yearSelect').value;

    if (!employeeId) {
        showMessage('Vui lòng chọn nhân viên để xem bảng công.', 'error');
        return;
    }

    try {
        const payload = await AttendanceApi.get(`/attendance/personal?employeeId=${employeeId}&month=${month}&year=${year}`);
        renderPersonalSummary(payload.summary);
        renderPersonalRows(payload.data);
        showMessage(`Đã tải dữ liệu chấm công tháng ${month}/${year}.`, 'success');
    } catch (err) {
        showMessage(err.message, 'error');
    }
}

function renderPersonalSummary(summary) {
    if (!summary) return;
    document.getElementById('totalWorkingDays').textContent = summary.totalWorkingDays || 0;
    document.getElementById('totalHours').textContent = (summary.totalHours || 0) + ' hrs';
    document.getElementById('presentDays').textContent = summary.presentDays || 0;
    document.getElementById('lateDays').textContent = summary.lateDays || 0;
    document.getElementById('earlyLeaveDays').textContent = summary.earlyLeaveDays || 0;
    document.getElementById('absentDays').textContent = (summary.absentDays || 0) + (summary.onLeaveDays ? ` (${summary.onLeaveDays} phép)` : '');
}

function renderPersonalRows(records) {
    const tbody = document.getElementById('attendanceRows');
    if (!records || records.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:#667085;padding:32px;">Không có bản ghi chấm công nào trong tháng này.</td></tr>';
        return;
    }

    tbody.innerHTML = records.map(r => {
        const dateObj = new Date(r.attendanceDate);
        const dayOfWeek = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'][dateObj.getDay()];
        const statusClass = (r.status || '').replace(/\s+/g, '');
        return `
            <tr>
                <td><strong>${safe(r.attendanceDate)}</strong></td>
                <td><span class="text-muted">${dayOfWeek}</span></td>
                <td>${r.checkInTime ? `<strong>${r.checkInTime}</strong>` : '<span class="text-muted">--:--:--</span>'}</td>
                <td>${r.checkOutTime ? `<strong>${r.checkOutTime}</strong>` : '<span class="text-muted">--:--:--</span>'}</td>
                <td><strong>${r.totalHours || '0.00'}</strong> hrs</td>
                <td><span class="badge ${statusClass}">${safe(r.status)}</span></td>
                <td><span class="text-muted">${safe(r.note)}</span></td>
            </tr>
        `;
    }).join('');
}

/* =========================================================================
   Điểm danh Nhân sự: Hiển thị toàn bộ user, Navbar role tabs, Check-in/Check-out, Hoàn tất điểm danh
   ========================================================================= */
let currentAttendanceList = [];
let currentRoleFilter = 'ALL';
let currentSearchKeyword = '';

async function initManagerAttendance() {
    const today = new Date();
    const todayStr = today.toISOString().split('T')[0];

    const dateInput = document.getElementById('teamDate');
    if (dateInput) {
        dateInput.value = todayStr;
    }

    const dateDisplay = document.getElementById('currentDateDisplay');
    if (dateDisplay) {
        const dd = String(today.getDate()).padStart(2, '0');
        const mm = String(today.getMonth() + 1).padStart(2, '0');
        const yyyy = today.getFullYear();
        dateDisplay.textContent = `${dd}/${mm}/${yyyy}`;
    }

    // Role tabs switching
    const roleTabs = document.getElementById('roleTabs');
    if (roleTabs) {
        roleTabs.addEventListener('click', (e) => {
            const btn = e.target.closest('.role-tab');
            if (!btn) return;
            document.querySelectorAll('.role-tab').forEach(t => t.classList.remove('active'));
            btn.classList.add('active');
            currentRoleFilter = btn.dataset.role || 'ALL';
            
            const labelMap = {
                'ALL': 'Tất cả nhân sự',
                'MANAGER': 'Cấp Quản lý (Manager)',
                'EMPLOYEE': 'Nhân viên (Employee)'
            };
            const statusLabel = document.getElementById('filterStatusText');
            if (statusLabel) {
                statusLabel.textContent = labelMap[currentRoleFilter] || 'Tất cả nhân sự';
            }
            renderAllAttendanceTable();
        });
    }

    // Keyword search
    const keywordInput = document.getElementById('keywordSearch');
    if (keywordInput) {
        keywordInput.addEventListener('input', (e) => {
            currentSearchKeyword = (e.target.value || '').trim().toLowerCase();
            renderAllAttendanceTable();
        });
    }

    // Button Today & Filter
    const btnSetToday = document.getElementById('btnSetToday');
    if (btnSetToday) {
        btnSetToday.addEventListener('click', () => {
            dateInput.value = todayStr;
            loadAllAttendance();
        });
    }

    const btnFilter = document.getElementById('btnFilterTeam');
    if (btnFilter) {
        btnFilter.addEventListener('click', loadAllAttendance);
    }

    const btnRefresh = document.getElementById('btnRefreshList');
    if (btnRefresh) {
        btnRefresh.addEventListener('click', loadAllAttendance);
    }

    // Select all checkbox
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', (e) => {
            const checked = e.target.checked;
            document.querySelectorAll('.emp-checkbox').forEach(cb => {
                cb.checked = checked;
            });
            updateBatchButtonState();
        });
    }

    // Batch Complete Button: "Hoàn tất việc check điểm danh"
    const btnBatch = document.getElementById('btnCompleteAttendance');
    if (btnBatch) {
        btnBatch.addEventListener('click', performBatchCompleteAttendance);
    }

    // Tải dữ liệu toàn bộ nhân sự ngay lập tức khi vào trang
    await loadAllAttendance();
}

async function loadAllAttendance() {
    const dateInput = document.getElementById('teamDate');
    const date = dateInput ? dateInput.value : new Date().toISOString().split('T')[0];

    try {
        const payload = await AttendanceApi.get(`/attendance/all?date=${date}`);
        currentAttendanceList = payload.data || [];
        updateAllCounters(currentAttendanceList);
        updateRoleTabCounts(currentAttendanceList);
        renderAllAttendanceTable();
        showMessage(`Đã tải danh sách điểm danh toàn bộ nhân sự ngày ${date}.`, 'success');
    } catch (err) {
        showMessage(err.message, 'error');
    }
}

function updateAllCounters(records) {
    if (!records) return;
    const total = records.length;
    let present = 0, late = 0, early = 0, notIn = 0;

    records.forEach(m => {
        if (m.status === 'Present') present++;
        else if (m.status === 'Late') late++;
        else if (m.status === 'Early Leave') early++;
        else if (!m.checkInTime || m.status === 'Absent') notIn++;
    });

    const elTotal = document.getElementById('totalTeamMembers');
    if (elTotal) elTotal.textContent = total;
    const elPres = document.getElementById('teamPresent');
    if (elPres) elPres.textContent = present;
    const elLate = document.getElementById('teamLate');
    if (elLate) elLate.textContent = late;
    const elEarly = document.getElementById('teamEarly');
    if (elEarly) elEarly.textContent = early;
    const elNotIn = document.getElementById('teamNotIn');
    if (elNotIn) elNotIn.textContent = notIn;
}

function updateRoleTabCounts(records) {
    if (!records) return;
    const total = records.length;
    let mgrCount = 0;
    let empCount = 0;

    records.forEach(r => {
        const role = (r.roleCode || '').toUpperCase();
        const pos = (r.positionTitle || '').toLowerCase();
        if (role === 'MANAGER' || role === 'COMPANY_ADMIN' || pos.includes('quản lý') || pos.includes('trưởng')) {
            mgrCount++;
        } else {
            empCount++;
        }
    });

    const cAll = document.getElementById('countAll');
    if (cAll) cAll.textContent = total;
    const cMgr = document.getElementById('countManager');
    if (cMgr) cMgr.textContent = mgrCount;
    const cEmp = document.getElementById('countEmployee');
    if (cEmp) cEmp.textContent = empCount;
}

function renderAllAttendanceTable() {
    const tbody = document.getElementById('teamRows');
    if (!tbody) return;

    const dateInput = document.getElementById('teamDate');
    const date = dateInput ? dateInput.value : '';

    // Filter by role
    let filtered = currentAttendanceList.filter(m => {
        if (currentRoleFilter === 'ALL') return true;
        const role = (m.roleCode || '').toUpperCase();
        const pos = (m.positionTitle || '').toLowerCase();
        const isMgr = role === 'MANAGER' || role === 'COMPANY_ADMIN' || pos.includes('quản lý') || pos.includes('trưởng');
        if (currentRoleFilter === 'MANAGER') return isMgr;
        if (currentRoleFilter === 'EMPLOYEE') return !isMgr;
        return true;
    });

    // Filter by search keyword
    if (currentSearchKeyword) {
        filtered = filtered.filter(m => {
            const name = (m.fullName || '').toLowerCase();
            const code = (m.employeeCode || '').toLowerCase();
            const dept = (m.departmentName || '').toLowerCase();
            const pos = (m.positionTitle || '').toLowerCase();
            return name.includes(currentSearchKeyword) || code.includes(currentSearchKeyword)
                || dept.includes(currentSearchKeyword) || pos.includes(currentSearchKeyword)
                || (m.employeeStatus || '').toLowerCase().includes(currentSearchKeyword);
        });
    }

    if (filtered.length === 0) {
        tbody.innerHTML = '<tr><td colspan="10" style="text-align:center;color:#667085;padding:36px;">Không tìm thấy nhân sự nào phù hợp với bộ lọc hiện tại.</td></tr>';
        resetBatchState();
        return;
    }

    tbody.innerHTML = filtered.map(m => {
        const hasCheckIn = !!m.checkInTime;
        const hasCheckOut = !!m.checkOutTime;

        // Distinct status styling
        let statusBadge = '';
        const st = m.status || '';
        if (st === 'Present') {
            statusBadge = '<span class="badge badge-present">Có mặt</span>';
        } else if (st === 'Late') {
            statusBadge = '<span class="badge badge-late">Đi muộn</span>';
        } else if (st === 'Early Leave') {
            statusBadge = '<span class="badge badge-early">Về sớm</span>';
        } else if (st === 'On Leave') {
            statusBadge = '<span class="badge badge-onleave">Nghỉ phép</span>';
        } else {
            statusBadge = '<span class="badge badge-absent">Chưa vào ca</span>';
        }

        // Role badge
        const role = (m.roleCode || '').toUpperCase();
        const pos = (m.positionTitle || '').toLowerCase();
        const isMgr = role === 'MANAGER' || role === 'COMPANY_ADMIN' || pos.includes('quản lý') || pos.includes('trưởng');
        const roleBadge = isMgr
            ? '<span class="badge RoleBadge Manager">Quản lý</span>'
            : '<span class="badge RoleBadge Employee">Nhân viên</span>';

        // Action buttons per row
        let actionHtml = '';
        if (!hasCheckIn) {
            actionHtml = `
                <button class="button btn-checkin" style="padding:6px 12px; font-size:12px;" onclick="performSingleCheckIn(${m.employeeId}, '${date}', '${escapeAttr(m.fullName)}')">
                    + Vào ca
                </button>
            `;
        } else if (!hasCheckOut) {
            actionHtml = `
                <button class="button btn-checkout" style="padding:6px 12px; font-size:12px;" onclick="performSingleCheckOut(${m.employeeId}, '${date}', '${escapeAttr(m.fullName)}')">
                    Tan ca
                </button>
            `;
        } else {
            actionHtml = `<span class="badge badge-present" style="font-size:11px; padding:4px 8px;">✓ Đã hoàn tất</span>`;
        }

        const empStatusBadge = m.employeeStatus && m.employeeStatus !== 'Active'
            ? `<span class="badge" style="background:#fee4e2; color:#d92d20; font-size:10px; padding:2px 6px; font-weight:600;">${safe(m.employeeStatus)}</span>`
            : '';

        return `
            <tr>
                <td class="checkbox-col">
                    <input type="checkbox" class="emp-checkbox" value="${m.employeeId}" onchange="updateBatchButtonState()">
                </td>
                <td><strong>${safe(m.employeeCode)}</strong></td>
                <td>
                    <div style="display:flex; align-items:center; gap:6px;">
                        <span style="font-weight:700; color:#101828;">${safe(m.fullName)}</span>
                        ${empStatusBadge}
                    </div>
                    <div style="font-size:12px; color:#667085;">${safe(m.positionTitle || '-')} &bull; ${safe(m.departmentName || '-')}</div>
                </td>
                <td>${roleBadge}</td>
                <td>${m.checkInTime ? `<strong style="color:#027a48;">${m.checkInTime}</strong>` : '<span class="text-muted">--:--:--</span>'}</td>
                <td>${m.checkOutTime ? `<strong style="color:#b54708;">${m.checkOutTime}</strong>` : '<span class="text-muted">--:--:--</span>'}</td>
                <td><strong>${m.totalHours || '0.00'}</strong> hrs</td>
                <td>${statusBadge}</td>
                <td><span class="text-muted" style="font-size:12px;">${safe(m.note)}</span></td>
                <td style="text-align:center;"><div class="actions" style="justify-content:center;">${actionHtml}</div></td>
            </tr>
        `;
    }).join('');

    resetBatchState();
}

function updateBatchButtonState() {
    const checked = document.querySelectorAll('.emp-checkbox:checked');
    const count = checked.length;
    const btn = document.getElementById('btnCompleteAttendance');
    const countDisplay = document.getElementById('selectedCountDisplay');
    
    if (countDisplay) countDisplay.textContent = count;
    if (btn) {
        btn.disabled = (count === 0);
    }
}

function resetBatchState() {
    const selectAll = document.getElementById('selectAllCheckbox');
    if (selectAll) selectAll.checked = false;
    updateBatchButtonState();
}

// Batch action: Hoàn tất việc check điểm danh cho các nhân sự đã tích chọn
async function performBatchCompleteAttendance() {
    const checked = Array.from(document.querySelectorAll('.emp-checkbox:checked')).map(cb => cb.value);
    if (checked.length === 0) {
        showMessage('Vui lòng tích chọn ít nhất một nhân sự để hoàn tất điểm danh.', 'error');
        return;
    }

    const dateInput = document.getElementById('teamDate');
    const date = dateInput ? dateInput.value : '';
    const actionSelect = document.getElementById('batchActionSelect');
    const action = actionSelect ? actionSelect.value : 'auto';

    const actionTextMap = {
        'auto': 'Tự động (Vào ca / Tan ca)',
        'check-in': 'Điểm danh Vào ca (Check-in)',
        'check-out': 'Điểm danh Tan ca (Check-out)'
    };

    if (!confirm(`Xác nhận HOÀN TẤT VIỆC CHECK ĐIỂM DANH cho [${checked.length} nhân sự] đã chọn (${actionTextMap[action]}) vào ngày ${date}?`)) {
        return;
    }

    try {
        const payload = await AttendanceApi.post('/attendance/batch-complete', {
            employeeIds: checked.join(','),
            action: action,
            date: date
        });

        showMessage(payload.message || 'Hoàn tất điểm danh thành công!', 'success');
        if (payload.data) {
            currentAttendanceList = payload.data;
            updateAllCounters(currentAttendanceList);
            updateRoleTabCounts(currentAttendanceList);
            renderAllAttendanceTable();
        } else {
            await loadAllAttendance();
        }
    } catch (err) {
        showMessage(err.message, 'error');
    }
}

// Single Check-In Action
async function performSingleCheckIn(employeeId, date, name) {
    if (!confirm(`Xác nhận ĐIỂM DANH VÀO CA (Check-in) cho [${name}] vào ngày ${date}?`)) {
        return;
    }
    try {
        const payload = await AttendanceApi.post('/attendance/check-in', { employeeId, date });
        showMessage(payload.message || 'Vào ca thành công!', 'success');
        await loadAllAttendance();
    } catch (err) {
        showMessage(err.message, 'error');
    }
}

// Single Check-Out Action
async function performSingleCheckOut(employeeId, date, name) {
    if (!confirm(`Xác nhận ĐIỂM DANH TAN CA (Check-out) cho [${name}] vào ngày ${date}?`)) {
        return;
    }
    try {
        const payload = await AttendanceApi.post('/attendance/check-out', { employeeId, date });
        showMessage(payload.message || 'Tan ca thành công!', 'success');
        await loadAllAttendance();
    } catch (err) {
        showMessage(err.message, 'error');
    }
}

/* =========================================================================
   UC33: Company Attendance Monitoring & Search (HR / Admin)
   ========================================================================= */
async function initCompanyAttendance() {
    const meta = await loadMeta();
    populateDepartmentSelect('deptFilter', meta.departments);

    document.getElementById('btnSearch').addEventListener('click', searchCompanyAttendance);
    document.getElementById('btnReset').addEventListener('click', () => {
        document.getElementById('deptFilter').value = '';
        document.getElementById('statusFilter').value = 'ALL';
        document.getElementById('fromDate').value = '';
        document.getElementById('toDate').value = '';
        document.getElementById('keyword').value = '';
        searchCompanyAttendance();
    });

    await searchCompanyAttendance();
}

async function searchCompanyAttendance() {
    const departmentId = document.getElementById('deptFilter').value;
    const status = document.getElementById('statusFilter').value;
    const fromDate = document.getElementById('fromDate').value;
    const toDate = document.getElementById('toDate').value;
    const keyword = document.getElementById('keyword').value;

    const params = new URLSearchParams();
    if (departmentId) params.append('departmentId', departmentId);
    if (status && status !== 'ALL') params.append('status', status);
    if (fromDate) params.append('fromDate', fromDate);
    if (toDate) params.append('toDate', toDate);
    if (keyword) params.append('keyword', keyword);

    try {
        const payload = await AttendanceApi.get(`/attendance/company?${params.toString()}`);
        renderCompanyRows(payload.data);
        document.getElementById('totalRecords').textContent = payload.data ? payload.data.length : 0;
        showMessage(`Tìm thấy ${payload.data ? payload.data.length : 0} bản ghi chấm công.`, 'success');
    } catch (err) {
        showMessage(err.message, 'error');
    }
}

function renderCompanyRows(records) {
    const tbody = document.getElementById('companyAttendanceRows');
    if (!records || records.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;color:#667085;padding:32px;">Không tìm thấy bản ghi chấm công nào phù hợp với bộ lọc.</td></tr>';
        return;
    }

    tbody.innerHTML = records.map(r => {
        const statusClass = (r.status || '').replace(/\s+/g, '');
        return `
            <tr>
                <td><strong>${safe(r.employeeCode)}</strong></td>
                <td><strong>${safe(r.fullName)}</strong></td>
                <td>${safe(r.departmentName)}</td>
                <td>${safe(r.attendanceDate)}</td>
                <td>${r.checkInTime ? `<strong>${r.checkInTime}</strong>` : '<span class="text-muted">--:--:--</span>'}</td>
                <td>${r.checkOutTime ? `<strong>${r.checkOutTime}</strong>` : '<span class="text-muted">--:--:--</span>'}</td>
                <td><strong>${r.totalHours || '0.00'}</strong> hrs</td>
                <td><span class="badge ${statusClass}">${safe(r.status)}</span></td>
                <td><span class="text-muted">${safe(r.note)}</span></td>
            </tr>
        `;
    }).join('');
}

/* =========================================================================
   UC36: Attendance Reports & CSV Export (HR / Manager)
   ========================================================================= */
async function initAttendanceReport() {
    const meta = await loadMeta();
    populateDepartmentSelect('reportDept', meta.departments);

    const today = new Date();
    document.getElementById('reportMonth').value = today.getMonth() + 1;
    document.getElementById('reportYear').value = today.getFullYear();

    document.getElementById('btnLoadReport').addEventListener('click', loadAttendanceReport);
    document.getElementById('btnExportCsv').addEventListener('click', exportReportCsv);

    await loadAttendanceReport();
}

async function loadAttendanceReport() {
    const departmentId = document.getElementById('reportDept').value;
    const month = document.getElementById('reportMonth').value;
    const year = document.getElementById('reportYear').value;

    const params = new URLSearchParams({ month, year });
    if (departmentId) params.append('departmentId', departmentId);

    try {
        const payload = await AttendanceApi.get(`/attendance/report?${params.toString()}`);
        renderReportRows(payload.data);
        renderReportStats(payload.data);
        showMessage(`Đã tải báo cáo chấm công tháng ${month}/${year}.`, 'success');
    } catch (err) {
        showMessage(err.message, 'error');
    }
}

function renderReportStats(reports) {
    if (!reports) return;
    const totalEmployees = reports.length;
    let totalWorkHours = 0;
    let sumRate = 0;

    reports.forEach(r => {
        totalWorkHours += Number(r.totalHours || 0);
        sumRate += Number(r.punctualityRate || 0);
    });

    const avgRate = totalEmployees > 0 ? (sumRate / totalEmployees).toFixed(1) : 0;

    document.getElementById('reportTotalEmployees').textContent = totalEmployees;
    document.getElementById('reportTotalHours').textContent = totalWorkHours.toFixed(1) + ' hrs';
    document.getElementById('reportAvgPunctuality').textContent = avgRate + '%';
}

function renderReportRows(reports) {
    const tbody = document.getElementById('reportRows');
    if (!reports || reports.length === 0) {
        tbody.innerHTML = '<tr><td colspan="10" style="text-align:center;color:#667085;padding:32px;">Không có dữ liệu báo cáo nào trong kỳ được chọn.</td></tr>';
        return;
    }

    tbody.innerHTML = reports.map(r => {
        const rate = Number(r.punctualityRate || 0);
        return `
            <tr>
                <td><strong>${safe(r.employeeCode)}</strong></td>
                <td><strong>${safe(r.fullName)}</strong></td>
                <td>${safe(r.departmentName)}</td>
                <td><strong>${r.totalWorkDays}</strong> ngày</td>
                <td><strong>${r.totalHours}</strong> hrs</td>
                <td><span class="text-success">${r.presentCount}</span></td>
                <td><span class="text-warning">${r.lateCount}</span></td>
                <td><span class="text-muted">${r.earlyLeaveCount}</span></td>
                <td><span class="text-danger">${r.absentCount}</span></td>
                <td>
                    <div class="rate-bar-container">
                        <div class="rate-bar-fill" style="width: ${rate}%;"></div>
                    </div>
                    <strong>${rate}%</strong>
                </td>
            </tr>
        `;
    }).join('');
}

function exportReportCsv() {
    const departmentId = document.getElementById('reportDept').value;
    const month = document.getElementById('reportMonth').value;
    const year = document.getElementById('reportYear').value;

    const params = new URLSearchParams({ month, year });
    if (departmentId) params.append('departmentId', departmentId);

    window.location.href = withContext(`/attendance/export-report?${params.toString()}`);
}

/* =========================================================================
   Helpers & Shared Utilities
   ========================================================================= */
async function loadMeta() {
    try {
        const scope = document.body.dataset.personalScope === 'self' ? '?scope=self' : '';
        const payload = await AttendanceApi.get(`/attendance/meta${scope}`);
        return payload;
    } catch (e) {
        console.warn('Could not load metadata:', e);
        return { employees: [], departments: [] };
    }
}

function populateEmployeeSelect(selectId, employees) {
    const select = document.getElementById(selectId);
    if (!select || !employees) return;
    select.innerHTML = '<option value="">-- Chọn nhân viên --</option>' + employees.map(e => {
        return `<option value="${e.employeeId}">${safe(e.employeeCode)} - ${safe(e.fullName)}</option>`;
    }).join('');
    if (employees.length > 0) {
        select.value = employees[0].employeeId;
    }
}

function populateDepartmentSelect(selectId, departments) {
    const select = document.getElementById(selectId);
    if (!select || !departments) return;
    select.innerHTML = '<option value="">-- Tất cả phòng ban --</option>' + departments.map(d => {
        return `<option value="${d.departmentId}">${safe(d.departmentName)}</option>`;
    }).join('');
}

function withContext(url) {
    const first = location.pathname.split('/').filter(Boolean)[0] || '';
    const contextPath = (first === 'attendance' || first === 'employee' || first === 'department' || first === 'position') ? '' : `/${first}`;
    return `${contextPath}${url}`;
}

async function handleResponse(response) {
    const payload = await response.json();
    if (!response.ok || payload.success === false) {
        throw new Error(payload.message || 'Yêu cầu không thành công.');
    }
    return payload;
}

function showMessage(message, type = 'success') {
    const el = document.getElementById('message');
    if (!el) return;
    el.textContent = message;
    el.className = `message show ${type}`;
    setTimeout(() => {
        if (el.className.includes('show')) {
            el.className = 'message';
        }
    }, 4500);
}

function safe(val) {
    return val === null || val === undefined || val === '' ? '-' : String(val);
}

function escapeAttr(val) {
    return (val || '').replace(/'/g, "\\'");
}
