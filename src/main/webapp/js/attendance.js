const AttendanceApi = {
    async get(url) {
        const response = await fetch(withContext(url), { headers: { 'Accept': 'application/json' } });
        return handleResponse(response);
    },

    async post(url, data) {
        const parameters = new URLSearchParams(data);
        parameters.set('csrfToken', document.querySelector('meta[name="organization-csrf-token"]')?.content || '');
        const response = await fetch(withContext(url), {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            },
            body: parameters
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
    } else if (page === 'attendance-list') {
        await initTimesheetPage();
    }
});

/* =========================================================================
   UC31: Employee Personal Attendance (Read-Only)
   ========================================================================= */
async function initEmployeeAttendance() {
    const meta = await loadMeta();
    populateEmployeeSelect('employeeSelect', meta.employees, meta.currentEmployeeId);

    const today = new Date();
    let defEmpMonth = today.getMonth() + 1;
    let defEmpYear = today.getFullYear();
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('month')) {
        defEmpMonth = parseInt(urlParams.get('month'), 10);
    } else if (today.getDate() <= 10) {
        defEmpMonth--;
        if (defEmpMonth < 1) { defEmpMonth = 12; defEmpYear--; }
    }
    if (urlParams.has('year')) {
        defEmpYear = parseInt(urlParams.get('year'), 10);
    }
    const mSelect = document.getElementById('monthSelect');
    if (mSelect) mSelect.value = defEmpMonth;
    const ySelect = document.getElementById('yearSelect');
    if (ySelect) ySelect.value = defEmpYear;

    const btnFilter = document.getElementById('btnFilter');
    if (btnFilter) btnFilter.addEventListener('click', loadPersonalAttendance);
    const empSelect = document.getElementById('employeeSelect');
    if (empSelect) empSelect.addEventListener('change', loadPersonalAttendance);

    document.getElementById('btnMyExplanations')?.addEventListener('click', openMyExplanationsModal);
    document.getElementById('btnCloseSubmitExp')?.addEventListener('click', closeSubmitExplanationModal);
    document.getElementById('btnCancelSubmitExp')?.addEventListener('click', closeSubmitExplanationModal);
    document.getElementById('btnCloseMyExp')?.addEventListener('click', closeMyExplanationsModal);
    document.getElementById('btnCloseMyExpBottom')?.addEventListener('click', closeMyExplanationsModal);
    document.getElementById('formSubmitExplanation')?.addEventListener('submit', handleExplanationSubmit);

    await loadPersonalAttendance();
}

async function loadPersonalAttendance() {
    const empSelect = document.getElementById('employeeSelect');
    const employeeId = empSelect ? empSelect.value : null;
    const mSelect = document.getElementById('monthSelect');
    const month = mSelect ? mSelect.value : (new Date().getMonth() + 1);
    const ySelect = document.getElementById('yearSelect');
    const year = ySelect ? ySelect.value : new Date().getFullYear();
    const tbody = document.getElementById('attendanceRows');

    if (!employeeId) {
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;color:#667085;padding:32px;">Vui lòng chọn nhân viên để xem bảng công.</td></tr>';
        }
        renderPersonalSummary(null);
        showMessage('Vui lòng chọn nhân viên để xem bảng công.', 'error');
        return;
    }

    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;color:#667085;padding:24px;">Đang tải dữ liệu...</td></tr>';
    }

    try {
        const payload = await AttendanceApi.get(`/attendance/personal?employeeId=${employeeId}&month=${month}&year=${year}`);
        renderPersonalSummary(payload.summary, payload.data);
        renderPersonalRows(payload.data);
        showMessage(`Đã tải dữ liệu chấm công tháng ${month}/${year}.`, 'success');
    } catch (err) {
        showMessage(err.message, 'error');
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;color:#d92d20;padding:32px;">Lỗi: ${safe(err.message)}</td></tr>`;
        }
    }
}

function calculateWorkCredit(hours) {
    const h = parseFloat(hours) || 0;
    if (h >= 7.0) return 1.0;
    if (h >= 3.5) return 0.5;
    return 0.0;
}

function renderPersonalSummary(summary, records) {
    const s = summary || {};
    const elWork = document.getElementById('totalWorkingDays');
    if (elWork) elWork.textContent = s.totalWorkingDays || 0;

    const elCredits = document.getElementById('totalWorkCredits');
    if (elCredits) {
        let credits = 0;
        if (s.totalWorkCredits !== undefined && s.totalWorkCredits !== null) {
            credits = parseFloat(s.totalWorkCredits) || 0;
        } else if (records && Array.isArray(records)) {
            credits = records.reduce((sum, r) => sum + (r.workCredit !== undefined && r.workCredit !== null ? parseFloat(r.workCredit) : calculateWorkCredit(r.totalHours)), 0);
        }
        elCredits.textContent = (credits % 1 === 0 ? credits : credits.toFixed(1)) + ' công';
    }

    const elHours = document.getElementById('totalHours');
    if (elHours) elHours.textContent = (s.totalHours || 0) + ' hrs';
    const elPres = document.getElementById('presentDays');
    if (elPres) elPres.textContent = s.presentDays || 0;
    const elLate = document.getElementById('lateDays');
    if (elLate) elLate.textContent = s.lateDays || 0;
    const elEarly = document.getElementById('earlyLeaveDays');
    if (elEarly) elEarly.textContent = s.earlyLeaveDays || 0;
    const elAbsent = document.getElementById('absentDays');
    if (elAbsent) elAbsent.textContent = (s.absentDays || 0) + (s.onLeaveDays ? ` (${s.onLeaveDays} phép)` : '');

    // Hiển thị thông báo hạn mức 5 buổi đi muộn/về sớm và cảnh báo khi > 3 buổi
    renderViolationNoticeBanner(summary, records);
}

function renderViolationNoticeBanner(summary, records) {
    const banner = document.getElementById('violationNoticeBanner');
    if (!banner) return;

    if (!summary) {
        banner.style.display = 'none';
        banner.innerHTML = '';
        return;
    }

    const lateDays = summary.lateDays !== undefined && summary.lateDays !== null ? Number(summary.lateDays) : 0;
    const earlyLeaveDays = summary.earlyLeaveDays !== undefined && summary.earlyLeaveDays !== null ? Number(summary.earlyLeaveDays) : 0;
    const totalViolations = lateDays + earlyLeaveDays;
    const maxAllowed = 5;

    let bannerClass = 'safe';
    let iconSvg = '';
    let titleHtml = '';
    let reminderText = '';

    if (totalViolations >= maxAllowed) {
        // >= 5 buổi: Báo động đỏ - Đã chạm hoặc vượt ngưỡng cho phép
        bannerClass = 'danger';
        iconSvg = `
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
        `;
        titleHtml = `🚨 CẢNH BÁO VI PHẠM: ĐÃ ĐẠT/VƯỢT QUÁ GIỚI HẠN ${maxAllowed} BUỔI (${totalViolations}/${maxAllowed} buổi)`;
        reminderText = `Bạn đã đi muộn / về sớm <strong>${totalViolations}/${maxAllowed} buổi</strong> (${lateDays} lần đi muộn, ${earlyLeaveDays} lần về sớm). Theo quy định phòng ban, bạn đã chạm mức tối đa 5 buổi vi phạm trong tháng! Báo cáo chuyên cần này sẽ được gửi tới Trưởng phòng ban để xem xét xử lý kỷ luật.`;
    } else if (totalViolations > 3) {
        // > 3 buổi (cụ thể là 4 buổi): Cảnh báo vàng/cam nổi bật kèm hình cảnh báo và nhắc nhở
        bannerClass = 'warning';
        iconSvg = `
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                <line x1="12" y1="9" x2="12" y2="13"></line>
                <line x1="12" y1="17" x2="12.01" y2="17"></line>
            </svg>
        `;
        titleHtml = `⚠️ CẢNH BÁO GIỜ GIẤC: SỐ BUỔI ĐI MUỘN / VỀ SỚM ĐÃ TRÊN 3 BUỔI (${totalViolations}/${maxAllowed} buổi)`;
        reminderText = `Bạn đã ghi nhận <strong>${totalViolations}/${maxAllowed} buổi</strong> đi muộn hoặc về sớm (${lateDays} lần đi muộn, ${earlyLeaveDays} lần về sớm). Bạn <strong>chỉ còn lại duy nhất ${maxAllowed - totalViolations} buổi</strong> trước khi bị xử lý kỷ luật phòng ban. Vui lòng nghiêm túc tuân thủ đúng giờ giấc làm việc để tránh bị phạt chuyên cần!`;
    } else {
        // <= 3 buổi: Trạng thái bình thường / an toàn kèm thông báo nhắc nhở hạn mức
        bannerClass = 'safe';
        iconSvg = `
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </svg>
        `;
        titleHtml = `Tình trạng giờ giấc phòng ban: ${totalViolations}/${maxAllowed} buổi vi phạm`;
        reminderText = `Quy định phòng ban cho phép tối đa <strong>${maxAllowed} buổi</strong> đi muộn hoặc về sớm mỗi tháng. Hiện tại bạn đã ghi nhận <strong>${totalViolations}/${maxAllowed} buổi</strong> (${lateDays} lần đi muộn, ${earlyLeaveDays} lần về sớm) - Còn lại <strong>${maxAllowed - totalViolations} buổi</strong>. Hãy tiếp tục duy trì tác phong làm việc đúng giờ!`;
    }

    const percent = Math.min(100, Math.round((totalViolations / maxAllowed) * 100));

    banner.className = `attendance-quota-banner ${bannerClass}`;
    banner.style.display = 'flex';
    banner.innerHTML = `
        <div class="attendance-quota-icon">
            ${iconSvg}
        </div>
        <div class="attendance-quota-content">
            <div class="attendance-quota-title">
                ${titleHtml}
            </div>
            <div class="attendance-quota-desc">
                ${reminderText}
            </div>
            <div class="attendance-quota-progress-track" title="Tỷ lệ sử dụng hạn mức vi phạm: ${percent}%">
                <div class="attendance-quota-progress-bar" style="width: ${percent}%;"></div>
            </div>
        </div>
    `;
}

function renderPersonalRows(records) {
    const tbody = document.getElementById('attendanceRows');
    if (!tbody) return;
    if (!records || records.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;color:#667085;padding:32px;">Không có bản ghi chấm công nào trong tháng này.</td></tr>';
        return;
    }

    tbody.innerHTML = records.map(r => {
        const dateObj = new Date(r.attendanceDate);
        const dayOfWeek = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'][dateObj.getDay()];
        const st = (r.status || '').toLowerCase();
        let badgeHtml = '';
        if (st.includes('late') || st.includes('muộn') || st.includes('trễ')) {
            badgeHtml = '<span class="badge badge-late">Đi muộn</span>';
        } else if (st.includes('early') || st.includes('sớm')) {
            badgeHtml = '<span class="badge badge-early">Về sớm</span>';
        } else if (st.includes('leave') || st.includes('phép')) {
            badgeHtml = '<span class="badge badge-onleave">Nghỉ phép</span>';
        } else if (st.includes('absent') || st.includes('vắng')) {
            badgeHtml = '<span class="badge badge-absent">Vắng mặt</span>';
        } else {
            badgeHtml = '<span class="badge badge-present">Đúng giờ</span>';
        }

        const credit = r.workCredit !== undefined && r.workCredit !== null
            ? parseFloat(r.workCredit)
            : calculateWorkCredit(r.totalHours);

        let creditBadgeHtml = '';
        if (credit >= 1.0) {
            creditBadgeHtml = '<span class="badge" style="background:#ecfdf3;color:#027a48;font-weight:700;">1 công</span>';
        } else if (credit >= 0.5) {
            creditBadgeHtml = '<span class="badge" style="background:#fffaeb;color:#b54708;font-weight:700;">0.5 công</span>';
        } else {
            creditBadgeHtml = '<span class="badge" style="background:#f2f4f7;color:#667085;font-weight:600;">0 công</span>';
        }

        return `
            <tr>
                <td><strong>${safe(r.attendanceDate)}</strong></td>
                <td><span class="text-muted">${dayOfWeek}</span></td>
                <td>${r.checkInTime ? `<strong>${r.checkInTime}</strong>` : '<span class="text-muted">--:--:--</span>'}</td>
                <td>${r.checkOutTime ? `<strong>${r.checkOutTime}</strong>` : '<span class="text-muted">--:--:--</span>'}</td>
                <td><strong>${r.totalHours || '0.00'}</strong> hrs</td>
                <td>${creditBadgeHtml}</td>
                <td>${badgeHtml}</td>
                <td><span class="text-muted">${safe(r.note)}</span></td>
                <td style="text-align:center;">
                    <button class="button small secondary btn-open-exp-modal" data-date="${safe(r.attendanceDate)}" data-in="${r.checkInTime || '08:30:00'}" data-out="${r.checkOutTime || '17:30:00'}" style="padding:4px 8px;font-size:12px;display:inline-flex;align-items:center;gap:4px;">
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"></path><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path></svg>
                        Giải trình
                    </button>
                </td>
            </tr>
        `;
    }).join('');

    document.querySelectorAll('.btn-open-exp-modal').forEach(btn => {
        btn.addEventListener('click', () => {
            const date = btn.dataset.date;
            const inTime = (btn.dataset.in || '08:30:00').substring(0, 5);
            const outTime = (btn.dataset.out || '17:30:00').substring(0, 5);
            openSubmitExplanationModal(date, inTime, outTime);
        });
    });
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

    // Complete batch attendance button ("Hoàn tất việc check điểm danh")
    const btnComplete = document.getElementById('btnCompleteAttendance');
    if (btnComplete) {
        btnComplete.addEventListener('click', () => {
            performBatchAttendanceAction('auto');
        });
    }

    // Batch Check-in & Check-out Buttons (nếu có nút riêng)
    const btnBatchIn = document.getElementById('btnBatchCheckIn');
    if (btnBatchIn) {
        btnBatchIn.addEventListener('click', () => performBatchAttendanceAction('check-in'));
    }

    const btnBatchOut = document.getElementById('btnBatchCheckOut');
    if (btnBatchOut) {
        btnBatchOut.addEventListener('click', () => performBatchAttendanceAction('check-out'));
    }

    // Modal Duyệt giải trình công (UC87)
    document.getElementById('btnOpenExplanationsModal')?.addEventListener('click', openManagerExplanationsModal);
    document.getElementById('btnCloseExplanationsModal')?.addEventListener('click', closeManagerExplanationsModal);
    document.getElementById('btnCloseExplanationsBottom')?.addEventListener('click', closeManagerExplanationsModal);
    loadPendingExplanationsCount();

    // Modal Cài đặt ca làm việc công ty
    document.getElementById('btnOpenShiftSetting')?.addEventListener('click', openShiftModal);
    document.getElementById('btnCloseShiftModal')?.addEventListener('click', closeShiftModal);
    document.getElementById('btnCancelShiftModal')?.addEventListener('click', closeShiftModal);
    document.getElementById('btnSaveShiftSetting')?.addEventListener('click', saveShiftSetting);
    document.getElementById('inputShiftStartTime')?.addEventListener('input', updateShiftLivePreview);
    document.getElementById('inputShiftEndTime')?.addEventListener('input', updateShiftLivePreview);
    document.getElementById('inputGracePeriodMinutes')?.addEventListener('input', updateShiftLivePreview);
    document.getElementById('inputLunchBreakMinutes')?.addEventListener('input', updateShiftLivePreview);

    // Modal Điểm danh bổ sung / Điều chỉnh công cho bất kỳ ngày nào
    document.getElementById('btnOpenManualModal')?.addEventListener('click', openManualModal);
    document.getElementById('btnCloseManualModal')?.addEventListener('click', closeManualModal);
    document.getElementById('btnCancelManualModal')?.addEventListener('click', closeManualModal);
    document.getElementById('btnSaveManualAttendance')?.addEventListener('click', saveManualAttendance);
    document.getElementById('manualCheckInTime')?.addEventListener('input', updateManualPreview);
    document.getElementById('manualCheckOutTime')?.addEventListener('input', updateManualPreview);

    // Tải thông tin ca làm việc và dữ liệu toàn bộ nhân sự
    await loadShiftSetting();
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

        // Action buttons per row: Tách riêng nút Vào ca và nút Tan ca
        let btnCheckInHtml = '';
        if (!hasCheckIn) {
            btnCheckInHtml = `
                <button class="button btn-checkin" style="padding:5px 9px; font-size:12px; display:inline-flex; align-items:center; gap:3px;" onclick="performSingleCheckIn(${m.employeeId}, '${date}', '${escapeAttr(m.fullName)}')">
                    <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path><polyline points="10 17 15 12 10 7"></polyline><line x1="15" y1="12" x2="3" y2="12"></line></svg>
                    Vào ca
                </button>
            `;
        } else {
            btnCheckInHtml = `
                <button class="button" disabled style="padding:5px 8px; font-size:11px; font-weight:600; background:#ecfdf3; color:#027a48; border:1px solid #abefc6; cursor:default;" title="Đã ghi nhận vào ca: ${m.checkInTime}">
                    ✓ Đã vào
                </button>
            `;
        }

        let btnCheckOutHtml = '';
        if (!hasCheckIn) {
            btnCheckOutHtml = `
                <button class="button" disabled style="padding:5px 8px; font-size:11px; background:#f2f4f7; color:#98a2b3; border:1px solid #eaecf0; cursor:not-allowed;" title="Chưa vào ca, không thể tan ca">
                    Tan ca
                </button>
            `;
        } else if (!hasCheckOut) {
            btnCheckOutHtml = `
                <button class="button btn-checkout" style="padding:5px 9px; font-size:12px; display:inline-flex; align-items:center; gap:3px;" onclick="performSingleCheckOut(${m.employeeId}, '${date}', '${escapeAttr(m.fullName)}')">
                    <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
                    Tan ca
                </button>
            `;
        } else {
            btnCheckOutHtml = `
                <button class="button" disabled style="padding:5px 8px; font-size:11px; font-weight:600; background:#fef3f2; color:#b42318; border:1px solid #fee4e2; cursor:default;" title="Đã ghi nhận tan ca: ${m.checkOutTime}">
                    ✓ Đã tan
                </button>
            `;
        }

        const manualBadge = m.manualAdjusted
            ? '<span class="badge badge-manual" title="Điểm danh bổ sung thủ công" style="font-size:10.5px; padding:2px 5px;">Bổ sung</span>'
            : '';

        const actionHtml = `
            <div style="display:flex; align-items:center; justify-content:center; gap:5px;">
                ${btnCheckInHtml}
                ${btnCheckOutHtml}
                <button class="button btn-adjust" style="padding:5px 8px; font-size:11.5px; display:inline-flex; align-items:center; gap:3px;" onclick="openManualModalFor(${m.employeeId}, '${escapeAttr(m.fullName)}', '${escapeAttr(m.employeeCode)}', '${m.checkInTime || ''}', '${m.checkOutTime || ''}', '${escapeAttr(m.note || '')}')" title="Điểm danh bổ sung / Sửa công cho nhân sự này">
                    <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                    Bổ sung
                </button>
            </div>
        `;

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
                </td>
                <td>${roleBadge}</td>
                <td>${m.checkInTime ? `<strong style="color:#027a48;">${m.checkInTime}</strong>` : '<span class="text-muted">--:--:--</span>'}</td>
                <td>${m.checkOutTime ? `<strong style="color:#b54708;">${m.checkOutTime}</strong>` : '<span class="text-muted">--:--:--</span>'}</td>
                <td><strong>${m.totalHours || '0.00'}</strong> hrs</td>
                <td><div style="display:flex; align-items:center; gap:4px; flex-wrap:wrap;">${statusBadge}${manualBadge}</div></td>
                <td><span class="text-muted" style="font-size:12px;">${safe(m.note)}</span></td>
                <td style="text-align:center;"><div class="actions" style="justify-content:center;">${actionHtml}</div></td>
            </tr>
        `;
    }).join('');

    resetBatchState();
}

function updateBatchButtonState() {
    const allCheckboxes = document.querySelectorAll('.emp-checkbox');
    const checked = document.querySelectorAll('.emp-checkbox:checked');
    const count = checked.length;
    const btnComplete = document.getElementById('btnCompleteAttendance');
    const btnIn = document.getElementById('btnBatchCheckIn');
    const btnOut = document.getElementById('btnBatchCheckOut');
    const countDisplay = document.getElementById('selectedCountDisplay');
    const selectAll = document.getElementById('selectAllCheckbox');
    
    if (countDisplay) countDisplay.textContent = count;
    if (btnComplete) {
        btnComplete.disabled = (count === 0);
    }
    if (btnIn) {
        btnIn.disabled = (count === 0);
    }
    if (btnOut) {
        btnOut.disabled = (count === 0);
    }
    if (selectAll && allCheckboxes.length > 0) {
        selectAll.checked = (count === allCheckboxes.length);
        selectAll.indeterminate = (count > 0 && count < allCheckboxes.length);
    }
}

function resetBatchState() {
    const selectAll = document.getElementById('selectAllCheckbox');
    if (selectAll) {
        selectAll.checked = false;
        selectAll.indeterminate = false;
    }
    updateBatchButtonState();
}

// Batch action: Điểm danh cho danh sách nhân sự đã chọn
async function performBatchAttendanceAction(action) {
    const checked = Array.from(document.querySelectorAll('.emp-checkbox:checked')).map(cb => cb.value);
    if (checked.length === 0) {
        showMessage(`Vui lòng tích chọn ít nhất một nhân sự để thực hiện điểm danh.`, 'error');
        return;
    }

    const dateInput = document.getElementById('teamDate');
    const date = dateInput ? dateInput.value : '';

    // Kiểm tra tính hợp lệ trước khi gửi yêu cầu
    const checkedIdSet = new Set(checked.map(id => Number(id)));
    const selectedMembers = currentAttendanceList.filter(m => checkedIdSet.has(m.employeeId));

    let actionLabel = 'ĐIỂM DANH TỰ ĐỘNG';
    if (action === 'check-in') {
        actionLabel = 'VÀO CA';
        const eligible = selectedMembers.filter(m => !m.checkInTime);
        if (eligible.length === 0) {
            showMessage('Tất cả các nhân sự được chọn đã được điểm danh Vào ca trước đó.', 'warning');
            return;
        }
    } else if (action === 'check-out') {
        actionLabel = 'TAN CA';
        const eligible = selectedMembers.filter(m => m.checkInTime && !m.checkOutTime);
        if (eligible.length === 0) {
            const noneCheckedIn = selectedMembers.every(m => !m.checkInTime);
            if (noneCheckedIn) {
                showMessage('Các nhân sự được chọn chưa điểm danh Vào ca nên chưa thể thực hiện Tan ca.', 'warning');
            } else {
                showMessage('Tất cả các nhân sự được chọn đã hoàn tất điểm danh Tan ca trước đó.', 'warning');
            }
            return;
        }
    } else { // 'auto'
        action = 'auto';
        actionLabel = 'HOÀN TẤT ĐIỂM DANH';
        const eligible = selectedMembers.filter(m => !m.checkInTime || !m.checkOutTime);
        if (eligible.length === 0) {
            showMessage('Tất cả các nhân sự được chọn đã hoàn thành cả Vào ca và Tan ca hôm nay.', 'warning');
            return;
        }
    }

    if (!confirm(`Xác nhận ${actionLabel} cho [${checked.length} nhân sự] đã chọn vào ngày ${date}?`)) {
        return;
    }

    try {
        const payload = await AttendanceApi.post('/attendance/batch-complete', {
            employeeIds: checked.join(','),
            action: action,
            date: date
        });

        showMessage(payload.message || `Điểm danh thành công!`, 'success');
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
   Cài đặt giờ giấc làm việc công ty (Shift Settings)
   ========================================================================= */
let currentShiftSetting = null;

async function loadShiftSetting() {
    try {
        const payload = await AttendanceApi.get('/attendance/shift-setting');
        if (payload && payload.data) {
            currentShiftSetting = payload.data;
            updateShiftBanner(currentShiftSetting);
        }
    } catch (err) {
        console.warn('Could not load shift settings:', err);
    }
}

function updateShiftBanner(setting) {
    if (!setting) return;
    const startEl = document.getElementById('bannerShiftStart');
    const endEl = document.getElementById('bannerShiftEnd');
    const graceEl = document.getElementById('bannerGraceText');
    const lunchEl = document.getElementById('bannerLunchBreak');

    if (startEl && setting.shiftStartTime) startEl.textContent = setting.shiftStartTime.substring(0, 5);
    if (endEl && setting.shiftEndTime) endEl.textContent = setting.shiftEndTime.substring(0, 5);
    if (lunchEl && setting.lunchBreakMinutes !== undefined) lunchEl.textContent = setting.lunchBreakMinutes;
    if (graceEl) {
        graceEl.textContent = (setting.gracePeriodMinutes > 0) ? ` (ân hạn ${setting.gracePeriodMinutes} phút)` : '';
    }
}

function openShiftModal() {
    const modal = document.getElementById('modalShiftSetting');
    if (!modal) return;

    if (currentShiftSetting) {
        if (document.getElementById('inputShiftStartTime') && currentShiftSetting.shiftStartTime) {
            document.getElementById('inputShiftStartTime').value = currentShiftSetting.shiftStartTime.substring(0, 5);
        }
        if (document.getElementById('inputShiftEndTime') && currentShiftSetting.shiftEndTime) {
            document.getElementById('inputShiftEndTime').value = currentShiftSetting.shiftEndTime.substring(0, 5);
        }
        if (document.getElementById('inputGracePeriodMinutes')) {
            document.getElementById('inputGracePeriodMinutes').value = currentShiftSetting.gracePeriodMinutes ?? 0;
        }
        if (document.getElementById('inputLunchBreakMinutes')) {
            document.getElementById('inputLunchBreakMinutes').value = currentShiftSetting.lunchBreakMinutes ?? 60;
        }
        const metaEl = document.getElementById('shiftSettingMetaInfo');
        if (metaEl) {
            metaEl.textContent = currentShiftSetting.updatedAt
                ? `Lần cập nhật gần nhất: ${currentShiftSetting.updatedAt} bởi ${currentShiftSetting.updatedByName || 'Hệ thống'}.`
                : 'Đang áp dụng thiết lập mặc định của công ty.';
        }
    }

    const msgEl = document.getElementById('shiftModalMessage');
    if (msgEl) {
        msgEl.style.display = 'none';
        msgEl.textContent = '';
    }

    updateShiftLivePreview();
    modal.classList.add('active');
    modal.style.display = 'flex';
}

function closeShiftModal() {
    const modal = document.getElementById('modalShiftSetting');
    if (modal) {
        modal.classList.remove('active');
        modal.style.display = 'none';
    }
}

function updateShiftLivePreview() {
    const startVal = document.getElementById('inputShiftStartTime')?.value || '08:30';
    const endVal = document.getElementById('inputShiftEndTime')?.value || '17:30';
    const graceVal = parseInt(document.getElementById('inputGracePeriodMinutes')?.value || '0', 10);

    const [h, m] = startVal.split(':').map(Number);
    const totalM = (h * 60 + m) + (isNaN(graceVal) ? 0 : graceVal);
    const lateThresholdStr = String(Math.floor(totalM / 60)).padStart(2, '0') + ':' + String(totalM % 60).padStart(2, '0');

    const previewStart = document.getElementById('previewLateThreshold');
    const previewEnd = document.getElementById('previewShiftEnd');
    if (previewStart) previewStart.textContent = lateThresholdStr;
    if (previewEnd) previewEnd.textContent = endVal;
}

async function saveShiftSetting() {
    const startTime = document.getElementById('inputShiftStartTime')?.value;
    const endTime = document.getElementById('inputShiftEndTime')?.value;
    const gracePeriodMinutes = document.getElementById('inputGracePeriodMinutes')?.value;
    const lunchBreakMinutes = document.getElementById('inputLunchBreakMinutes')?.value;

    if (!startTime || !endTime) {
        showShiftModalMessage('Vui lòng nhập đầy đủ giờ vào ca và giờ tan ca.', 'error');
        return;
    }
    if (endTime <= startTime) {
        showShiftModalMessage('Giờ kết thúc ca làm việc phải sau giờ bắt đầu ca.', 'error');
        return;
    }

    const saveBtn = document.getElementById('btnSaveShiftSetting');
    if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.textContent = 'Đang lưu...';
    }

    try {
        const payload = await AttendanceApi.post('/attendance/shift-setting', {
            shiftStartTime: startTime.length === 5 ? startTime + ':00' : startTime,
            shiftEndTime: endTime.length === 5 ? endTime + ':00' : endTime,
            gracePeriodMinutes: gracePeriodMinutes || 0,
            lunchBreakMinutes: lunchBreakMinutes || 60
        });

        if (payload && payload.data) {
            currentShiftSetting = payload.data;
            updateShiftBanner(currentShiftSetting);
        }
        showMessage(payload.message || 'Cập nhật giờ giấc làm việc thành công!', 'success');
        closeShiftModal();
        await loadAllAttendance();
    } catch (err) {
        showShiftModalMessage(err.message || 'Lỗi khi lưu cài đặt.', 'error');
    } finally {
        if (saveBtn) {
            saveBtn.disabled = false;
            saveBtn.textContent = 'Lưu cài đặt';
        }
    }
}

function showShiftModalMessage(text, type) {
    const msgEl = document.getElementById('shiftModalMessage');
    if (!msgEl) return;
    msgEl.className = `message ${type}`;
    msgEl.textContent = text;
    msgEl.style.display = 'block';
}

/* =========================================================================
   Điểm danh bổ sung / Điều chỉnh công cho bất kỳ ngày nào (Manual Adjust)
   ========================================================================= */
function openManualModal() {
    const modal = document.getElementById('modalManualAttendance');
    if (!modal) return;

    const dateInput = document.getElementById('teamDate');
    const manualDateInput = document.getElementById('manualDate');
    if (manualDateInput) {
        manualDateInput.value = (dateInput && dateInput.value) ? dateInput.value : new Date().toISOString().split('T')[0];
    }

    const select = document.getElementById('manualEmployeeSelect');
    if (select) {
        if (currentAttendanceList && currentAttendanceList.length > 0) {
            select.innerHTML = '<option value="">-- Chọn nhân sự --</option>' + currentAttendanceList.map(m => {
                return `<option value="${m.employeeId}">${safe(m.employeeCode)} - ${safe(m.fullName)}</option>`;
            }).join('');
        }
    }

    const inInput = document.getElementById('manualCheckInTime');
    const outInput = document.getElementById('manualCheckOutTime');
    const noteInput = document.getElementById('manualNote');
    const msgEl = document.getElementById('manualModalMessage');

    if (inInput) inInput.value = '';
    if (outInput) outInput.value = '';
    if (noteInput) noteInput.value = '';
    if (msgEl) {
        msgEl.style.display = 'none';
        msgEl.textContent = '';
    }

    updateManualPreview();
    modal.classList.add('active');
    modal.style.display = 'flex';
}

function openManualModalFor(employeeId, fullName, employeeCode, checkInTime, checkOutTime, note) {
    openManualModal();
    const select = document.getElementById('manualEmployeeSelect');
    if (select && employeeId) {
        select.value = employeeId;
    }
    if (checkInTime) {
        const inInput = document.getElementById('manualCheckInTime');
        if (inInput) inInput.value = checkInTime.substring(0, 5);
    }
    if (checkOutTime) {
        const outInput = document.getElementById('manualCheckOutTime');
        if (outInput) outInput.value = checkOutTime.substring(0, 5);
    }
    if (note) {
        const noteInput = document.getElementById('manualNote');
        if (noteInput) noteInput.value = note;
    }
    updateManualPreview();
}

function closeManualModal() {
    const modal = document.getElementById('modalManualAttendance');
    if (modal) {
        modal.classList.remove('active');
        modal.style.display = 'none';
    }
}

function updateManualPreview() {
    const previewEl = document.getElementById('manualPreviewDetails');
    if (!previewEl) return;

    const inVal = document.getElementById('manualCheckInTime')?.value;
    const outVal = document.getElementById('manualCheckOutTime')?.value;

    const shiftStart = currentShiftSetting?.shiftStartTime ? currentShiftSetting.shiftStartTime.substring(0, 5) : '08:30';
    const shiftEnd = currentShiftSetting?.shiftEndTime ? currentShiftSetting.shiftEndTime.substring(0, 5) : '17:30';
    const graceMin = currentShiftSetting?.gracePeriodMinutes || 0;
    const lunchMin = currentShiftSetting?.lunchBreakMinutes || 60;

    const [startH, startM] = shiftStart.split(':').map(Number);
    const startTotalMin = startH * 60 + startM + graceMin;
    const lateThresholdStr = String(Math.floor(startTotalMin / 60)).padStart(2, '0') + ':' + String(startTotalMin % 60).padStart(2, '0');

    if (!inVal && !outVal) {
        previewEl.innerHTML = '<span class="text-muted">Vui lòng nhập giờ vào ca hoặc giờ tan ca để xem dự tính.</span>';
        return;
    }

    if (inVal && outVal) {
        const [inH, inM] = inVal.split(':').map(Number);
        const [outH, outM] = outVal.split(':').map(Number);
        const inMinutes = inH * 60 + inM;
        const outMinutes = outH * 60 + outM;

        if (outMinutes <= inMinutes) {
            previewEl.innerHTML = '<span style="color:#d92d20;font-weight:700;">⚠️ Giờ tan ca phải sau giờ vào ca!</span>';
            return;
        }

        let totalDuration = outMinutes - inMinutes;
        let lunchDeducted = false;
        if (totalDuration >= 300 && lunchMin > 0) {
            totalDuration = Math.max(0, totalDuration - lunchMin);
            lunchDeducted = true;
        }

        const totalHours = (totalDuration / 60).toFixed(2);
        const hoursNum = parseFloat(totalHours);
        let workCredit = 0;
        if (hoursNum >= 7.0) {
            workCredit = 1.0;
        } else if (hoursNum >= 3.5) {
            workCredit = 0.5;
        }

        const isLate = inVal > lateThresholdStr;
        const isEarly = outVal < shiftEnd;

        let statusBadge = '<span class="badge badge-present">Có mặt</span>';
        if (isLate) {
            statusBadge = '<span class="badge badge-late">Đi muộn</span>';
        } else if (isEarly) {
            statusBadge = '<span class="badge badge-early">Về sớm</span>';
        }

        let creditBadge = '';
        if (workCredit === 1.0) {
            creditBadge = '<strong style="color:#027a48;">1.0 ngày công</strong>';
        } else if (workCredit === 0.5) {
            creditBadge = '<strong style="color:#b54708;">0.5 ngày công (nửa công)</strong>';
        } else {
            creditBadge = '<strong style="color:#667085;">0 công (&lt; 3.5 tiếng)</strong>';
        }

        previewEl.innerHTML = `
            <div style="display:flex; flex-direction:column; gap:4px;">
                <div>⏱️ <strong>Tổng giờ làm:</strong> <strong style="color:#062579;">${totalHours} hrs</strong> ${lunchDeducted ? `<span style="font-size:11.5px;color:#475467;">(đã trừ ${lunchMin}p nghỉ trưa)</span>` : ''}</div>
                <div>💼 <strong>Số công tính được:</strong> ${creditBadge}</div>
                <div>🏷️ <strong>Trạng thái ghi nhận:</strong> ${statusBadge} ${isLate ? `<span style="font-size:11.5px;color:#b54708;">(vào sau ${lateThresholdStr})</span>` : ''} ${isEarly ? `<span style="font-size:11.5px;color:#3538cd;">(tan trước ${shiftEnd})</span>` : ''}</div>
            </div>
        `;
    } else if (inVal) {
        const isLate = inVal > lateThresholdStr;
        previewEl.innerHTML = `
            <div>⏱️ Đã nhập giờ vào: <strong>${inVal}</strong> | Trạng thái: ${isLate ? '<span class="badge badge-late">Đi muộn</span>' : '<span class="badge badge-present">Đúng giờ</span>'}</div>
            <div style="font-size:11.5px; color:#667085; margin-top:2px;">(Chưa nhập giờ tan ca - cần cả 2 mốc để tính tổng giờ và số ngày công)</div>
        `;
    } else {
        const isEarly = outVal < shiftEnd;
        previewEl.innerHTML = `
            <div>⏱️ Đã nhập giờ tan: <strong>${outVal}</strong> | Trạng thái: ${isEarly ? '<span class="badge badge-early">Về sớm</span>' : '<span class="badge badge-present">Đúng giờ</span>'}</div>
            <div style="font-size:11.5px; color:#667085; margin-top:2px;">(Chưa nhập giờ vào ca - cần cả 2 mốc để tính tổng giờ và số ngày công)</div>
        `;
    }
}

async function saveManualAttendance() {
    const employeeId = document.getElementById('manualEmployeeSelect')?.value;
    const date = document.getElementById('manualDate')?.value;
    const checkInTime = document.getElementById('manualCheckInTime')?.value;
    const checkOutTime = document.getElementById('manualCheckOutTime')?.value;
    const note = document.getElementById('manualNote')?.value;

    if (!date) {
        showManualModalMessage('Vui lòng chọn ngày cần điểm danh bổ sung.', 'error');
        return;
    }
    if (!employeeId) {
        showManualModalMessage('Vui lòng chọn nhân sự cần điểm danh bổ sung.', 'error');
        return;
    }
    if (!checkInTime && !checkOutTime) {
        showManualModalMessage('Vui lòng nhập giờ vào ca hoặc giờ tan ca.', 'error');
        return;
    }
    if (checkInTime && checkOutTime && checkOutTime <= checkInTime) {
        showManualModalMessage('Thời gian tan ca không thể sớm hơn hoặc bằng thời gian vào ca.', 'error');
        return;
    }

    const saveBtn = document.getElementById('btnSaveManualAttendance');
    if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.textContent = 'Đang lưu...';
    }

    try {
        const payload = await AttendanceApi.post('/attendance/manual-adjust', {
            employeeId,
            date,
            checkInTime: checkInTime ? (checkInTime.length === 5 ? checkInTime + ':00' : checkInTime) : '',
            checkOutTime: checkOutTime ? (checkOutTime.length === 5 ? checkOutTime + ':00' : checkOutTime) : '',
            note: note || ''
        });

        showMessage(payload.message || 'Điểm danh bổ sung thành công!', 'success');
        closeManualModal();

        const currentDate = document.getElementById('teamDate')?.value;
        if (currentDate === date) {
            await loadAllAttendance();
        }
    } catch (err) {
        showManualModalMessage(err.message || 'Lỗi khi điểm danh bổ sung.', 'error');
    } finally {
        if (saveBtn) {
            saveBtn.disabled = false;
            saveBtn.innerHTML = `
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                Lưu điểm danh bổ sung
            `;
        }
    }
}

function showManualModalMessage(text, type) {
    const msgEl = document.getElementById('manualModalMessage');
    if (!msgEl) return;
    msgEl.className = `message ${type}`;
    msgEl.textContent = text;
    msgEl.style.display = 'block';
}

// Window click & Escape key listener to close modals
window.addEventListener('click', (e) => {
    const shiftModal = document.getElementById('modalShiftSetting');
    if (e.target === shiftModal) closeShiftModal();
    const manualModal = document.getElementById('modalManualAttendance');
    if (e.target === manualModal) closeManualModal();
});
window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        closeShiftModal();
        closeManualModal();
    }
});


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
        const manualBadge = (r.manualAdjusted || r.isManualAdjusted)
            ? '<span class="badge" style="background:#e0e7ff;color:#4338ca;border:1px solid #c7d2fe;font-size:11px;margin-left:4px;">Bổ sung</span>'
            : '';
        return `
            <tr>
                <td><strong>${safe(r.employeeCode)}</strong></td>
                <td><strong>${safe(r.fullName)}</strong></td>
                <td>${safe(r.departmentName)}</td>
                <td>${safe(r.attendanceDate)}</td>
                <td>${r.checkInTime ? `<strong>${r.checkInTime}</strong>` : '<span class="text-muted">--:--:--</span>'}</td>
                <td>${r.checkOutTime ? `<strong>${r.checkOutTime}</strong>` : '<span class="text-muted">--:--:--</span>'}</td>
                <td><strong>${r.totalHours || '0.00'}</strong> hrs</td>
                <td><span class="badge ${statusClass}">${safe(r.status)}</span>${manualBadge}</td>
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
    let repMonth = today.getMonth() + 1;
    let repYear = today.getFullYear();
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('month')) {
        repMonth = parseInt(urlParams.get('month'), 10);
    } else if (today.getDate() <= 10) {
        repMonth--;
        if (repMonth < 1) { repMonth = 12; repYear--; }
    }
    if (urlParams.has('year')) {
        repYear = parseInt(urlParams.get('year'), 10);
    }
    const rMonth = document.getElementById('reportMonth');
    if (rMonth) rMonth.value = repMonth;
    const rYear = document.getElementById('reportYear');
    if (rYear) rYear.value = repYear;

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

function populateEmployeeSelect(selectId, employees, preferredId) {
    const select = document.getElementById(selectId);
    if (!select || !employees) return;
    if (employees.length === 0) {
        select.innerHTML = '<option value="">-- Chưa có nhân viên --</option>';
        return;
    }
    select.innerHTML = employees.map(e => {
        return `<option value="${e.employeeId}">${safe(e.employeeCode)} - ${safe(e.fullName)}</option>`;
    }).join('');

    if (preferredId && employees.some(e => String(e.employeeId) === String(preferredId))) {
        select.value = preferredId;
    } else {
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

/* =========================================================================
   Danh sách Chấm công (Monthly Timesheet Matrix) - Matching Image 2
   ========================================================================= */
function getInitialTimesheetPeriod() {
    const urlParams = new URLSearchParams(window.location.search);
    const pMonth = parseInt(urlParams.get('month'), 10);
    const pYear = parseInt(urlParams.get('year'), 10);
    if (!isNaN(pMonth) && pMonth >= 1 && pMonth <= 12 && !isNaN(pYear) && pYear >= 2000 && pYear <= 2100) {
        return { month: pMonth, year: pYear };
    }

    const sMonth = parseInt(sessionStorage.getItem('hr_timesheet_month'), 10);
    const sYear = parseInt(sessionStorage.getItem('hr_timesheet_year'), 10);
    if (!isNaN(sMonth) && sMonth >= 1 && sMonth <= 12 && !isNaN(sYear) && sYear >= 2000 && sYear <= 2100) {
        return { month: sMonth, year: sYear };
    }

    const now = new Date();
    let m = now.getMonth() + 1;
    let y = now.getFullYear();
    // Trong quản lý nhân sự, vào những ngày đầu tháng (<= 10),
    // quản lý thường cần xem, chốt công, khóa công và xuất bảng công của THÁNG TRƯỚC đã hoàn thành.
    if (now.getDate() <= 10) {
        m--;
        if (m < 1) {
            m = 12;
            y--;
        }
    }
    return { month: m, year: y };
}

function persistTimesheetPeriod() {
    try {
        sessionStorage.setItem('hr_timesheet_month', currentTimesheetMonth);
        sessionStorage.setItem('hr_timesheet_year', currentTimesheetYear);
        if (window.location && window.location.href) {
            const url = new URL(window.location.href);
            url.searchParams.set('view', 'list');
            url.searchParams.set('month', currentTimesheetMonth);
            url.searchParams.set('year', currentTimesheetYear);
            window.history.replaceState({}, '', url.toString());
        }
    } catch (e) {
        // Safe fallback in test or headless env
    }
}

let currentTimesheetMonth = 10;
let currentTimesheetYear = 2026;
let currentTimesheetData = null;
let currentTimesheetActiveTab = 'grid'; // 'grid' | 'department'
let currentTimesheetSearch = '';
let currentTimesheetDept = '';

async function initTimesheetPage() {
    const initPeriod = getInitialTimesheetPeriod();
    currentTimesheetMonth = initPeriod.month;
    currentTimesheetYear = initPeriod.year;
    persistTimesheetPeriod();
    updateMonthBadgeUI();

    // Month & Year Dropdown Selectors
    const monthSelect = document.getElementById('timesheetMonthSelect');
    if (monthSelect) {
        monthSelect.value = String(currentTimesheetMonth);
        monthSelect.addEventListener('change', async (e) => {
            currentTimesheetMonth = parseInt(e.target.value, 10);
            persistTimesheetPeriod();
            await loadTimesheetData();
        });
    }

    const yearSelect = document.getElementById('timesheetYearSelect');
    if (yearSelect) {
        yearSelect.value = String(currentTimesheetYear);
        yearSelect.addEventListener('change', async (e) => {
            currentTimesheetYear = parseInt(e.target.value, 10);
            persistTimesheetPeriod();
            await loadTimesheetData();
        });
    }

    // Quick jump to previous month button
    const btnQuickPrev = document.getElementById('btnTimesheetQuickPrev');
    if (btnQuickPrev) {
        btnQuickPrev.addEventListener('click', async () => {
            const now = new Date();
            let m = now.getMonth(); // 0..11, so current month index is actually previous month number!
            let y = now.getFullYear();
            if (m === 0) {
                m = 12;
                y--;
            }
            currentTimesheetMonth = m;
            currentTimesheetYear = y;
            persistTimesheetPeriod();
            await loadTimesheetData();
        });
    }

    // Previous month button
    const btnPrev = document.getElementById('btnPrevMonth');
    if (btnPrev) {
        btnPrev.addEventListener('click', async () => {
            currentTimesheetMonth--;
            if (currentTimesheetMonth < 1) {
                currentTimesheetMonth = 12;
                currentTimesheetYear--;
            }
            persistTimesheetPeriod();
            await loadTimesheetData();
        });
    }

    // Next month button
    const btnNext = document.getElementById('btnNextMonth');
    if (btnNext) {
        btnNext.addEventListener('click', async () => {
            currentTimesheetMonth++;
            if (currentTimesheetMonth > 12) {
                currentTimesheetMonth = 1;
                currentTimesheetYear++;
            }
            persistTimesheetPeriod();
            await loadTimesheetData();
        });
    }

    // Tab switching (Bảng chấm công vs Phân tích theo phòng ban)
    const tabGrid = document.getElementById('tabGrid');
    const tabDept = document.getElementById('tabDept');
    const secGrid = document.getElementById('sectionGrid');
    const secDept = document.getElementById('sectionDepartment');

    if (tabGrid && tabDept) {
        tabGrid.addEventListener('click', () => {
            tabGrid.classList.add('active');
            tabDept.classList.remove('active');
            currentTimesheetActiveTab = 'grid';
            if (secGrid) secGrid.style.display = 'block';
            if (secDept) secDept.style.display = 'none';
        });

        tabDept.addEventListener('click', () => {
            tabDept.classList.add('active');
            tabGrid.classList.remove('active');
            currentTimesheetActiveTab = 'department';
            if (secGrid) secGrid.style.display = 'none';
            if (secDept) secDept.style.display = 'block';
        });
    }

    // Search filter input
    const searchInput = document.getElementById('timesheetSearchInput');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            currentTimesheetSearch = (e.target.value || '').trim();
            if (currentTimesheetData) {
                renderTimesheetGrid(currentTimesheetData);
            }
        });
    }

    // Department filter select
    const deptSelect = document.getElementById('timesheetDeptSelect');
    if (deptSelect) {
        deptSelect.addEventListener('change', (e) => {
            currentTimesheetDept = e.target.value;
            if (currentTimesheetData) {
                renderTimesheetGrid(currentTimesheetData);
            }
        });
    }

    // Export Excel button
    const btnExport = document.getElementById('btnExportTimesheetExcel');
    if (btnExport) {
        btnExport.addEventListener('click', exportTimesheetToExcel);
    }

    // Toggle Timesheet Lock button (UC88)
    const btnToggleLock = document.getElementById('btnToggleTimesheetLock');
    if (btnToggleLock) {
        btnToggleLock.addEventListener('click', toggleTimesheetLock);
    }

    // Load initial data
    await loadTimesheetData();
}

function updateMonthBadgeUI() {
    const badge = document.getElementById('currentMonthBadge');
    if (badge) {
        badge.textContent = `T${currentTimesheetMonth}/${currentTimesheetYear}`;
    }
    const monthSelect = document.getElementById('timesheetMonthSelect');
    if (monthSelect) {
        monthSelect.value = String(currentTimesheetMonth);
    }
    const yearSelect = document.getElementById('timesheetYearSelect');
    if (yearSelect) {
        yearSelect.value = String(currentTimesheetYear);
    }
}

async function loadTimesheetData() {
    updateMonthBadgeUI();
    checkTimesheetLockStatus(currentTimesheetMonth, currentTimesheetYear);
    const tbody = document.getElementById('timesheetRows');
    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="32" style="text-align:center; padding:36px; color:#667085;">Đang tải bảng chấm công...</td></tr>';
    }

    try {
        const payload = await AttendanceApi.get(`/attendance/report?type=timesheet&month=${currentTimesheetMonth}&year=${currentTimesheetYear}`);
        currentTimesheetData = payload;

        // Header Subtitle: "Tháng 10/2026 · 10 nhân sự"
        const sub = document.getElementById('timesheetHeaderSubtitle');
        if (sub) {
            sub.textContent = `Tháng ${payload.month}/${payload.year} · ${payload.totalEmployees || (payload.employees ? payload.employees.length : 0)} nhân sự`;
        }

        // Populate department dropdown
        populateTimesheetDeptSelect(payload.departments || []);

        // Render Table & Department Stats
        renderTimesheetGrid(payload);
        renderDepartmentStats(payload.departmentStats || []);

        showMessage(`Đã tải bảng chấm công tháng ${payload.month}/${payload.year}.`, 'success');
    } catch (err) {
        showMessage(err.message, 'error');
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="32" style="text-align:center; padding:36px; color:#d92d20;">Lỗi: ${safe(err.message)}</td></tr>`;
        }
    }
}

function populateTimesheetDeptSelect(departments) {
    const select = document.getElementById('timesheetDeptSelect');
    if (!select || select.dataset.loaded) return;
    select.innerHTML = '<option value="">Tất cả phòng ban</option>' + departments.map(d => {
        return `<option value="${d.departmentId}">${safe(d.departmentName)}</option>`;
    }).join('');
    select.dataset.loaded = 'true';
}

function renderTimesheetGrid(data) {
    const headerRow = document.getElementById('timesheetHeaderDays');
    const tbody = document.getElementById('timesheetRows');
    if (!headerRow || !tbody) return;

    const daysInMonth = data.daysInMonth || 30;
    const year = data.year;
    const month = data.month;

    // 1. Render Header columns
    const dayNames = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
    let headerHtml = '<th class="col-sticky-emp">NHÂN VIÊN</th>';

    for (let d = 1; d <= daysInMonth; d++) {
        const dateObj = new Date(year, month - 1, d);
        const dayOfWeek = dateObj.getDay();
        const wkText = dayNames[dayOfWeek];
        const isSunday = (dayOfWeek === 0);
        const isSaturday = (dayOfWeek === 6);

        let cls = '';
        if (isSunday) cls = 'is-weekend-sunday';
        else if (isSaturday) cls = 'is-weekend-saturday';

        headerHtml += `
            <th class="${cls}">
                <div class="day-num">${d}</div>
                <div class="day-wk">${wkText}</div>
            </th>
        `;
    }
    headerRow.innerHTML = headerHtml;

    // 2. Filter employees
    let employees = data.employees || [];
    if (currentTimesheetDept) {
        employees = employees.filter(e => String(e.departmentId) === String(currentTimesheetDept));
    }
    if (currentTimesheetSearch) {
        const kw = currentTimesheetSearch.toLowerCase();
        employees = employees.filter(e =>
            (e.fullName || '').toLowerCase().includes(kw) ||
            (e.employeeCode || '').toLowerCase().includes(kw)
        );
    }

    if (employees.length === 0) {
        tbody.innerHTML = `<tr><td colspan="${daysInMonth + 1}" style="text-align:center; padding:36px; color:#667085;">Không tìm thấy nhân sự nào phù hợp với bộ lọc.</td></tr>`;
        return;
    }

    // Color palette for avatars
    const avatarBgs = ['#fee4e2', '#fef0c7', '#d1fadf', '#d1e9ff', '#f4ebff', '#e0eaef'];
    const avatarColors = ['#b42318', '#b54708', '#027a48', '#175cd3', '#6941c6', '#344054'];

    // 3. Render Employee Rows
    tbody.innerHTML = employees.map((emp, empIdx) => {
        const bg = avatarBgs[empIdx % avatarBgs.length];
        const tc = avatarColors[empIdx % avatarColors.length];
        const initials = getInitials(emp.fullName || emp.employeeCode || 'NV');
        const records = emp.records || {};

        let cellsHtml = '';
        for (let d = 1; d <= daysInMonth; d++) {
            const dateObj = new Date(year, month - 1, d);
            const dayOfWeek = dateObj.getDay();
            const isSunday = (dayOfWeek === 0);
            const isSaturday = (dayOfWeek === 6);

            let cellClass = '';
            if (isSunday) cellClass = 'is-weekend-sunday';
            else if (isSaturday) cellClass = 'is-weekend-saturday';

            const rec = records[String(d)];
            let pillHtml = '';

            if (rec) {
                const st = (rec.status || '').toLowerCase();
                if (st.includes('late') || st.includes('muộn') || st.includes('trễ')) {
                    // Orange pill L (matching Image 2)
                    pillHtml = `<span class="cell-pill pill-late" title="Ngày ${d}/${month}: Vào ${rec.checkIn || '--'} - Tan ${rec.checkOut || '--'} (Đi trễ)">L</span>`;
                } else if (st.includes('early') || st.includes('sớm')) {
                    pillHtml = `<span class="cell-pill pill-early" title="Ngày ${d}/${month}: Vào ${rec.checkIn || '--'} - Tan ${rec.checkOut || '--'} (Về sớm)">S</span>`;
                } else if (st.includes('leave') || st.includes('phép')) {
                    pillHtml = `<span class="cell-pill pill-onleave" title="Ngày ${d}/${month}: Nghỉ phép">P</span>`;
                } else if (st.includes('absent') || st.includes('vắng')) {
                    pillHtml = `<span class="cell-pill pill-absent" title="Ngày ${d}/${month}: Vắng mặt">V</span>`;
                } else {
                    // Present (Đúng giờ / Có mặt)
                    pillHtml = `<span class="cell-pill pill-present" title="Ngày ${d}/${month}: Vào ${rec.checkIn || '--'} - Tan ${rec.checkOut || '--'} (Đúng giờ)">✓</span>`;
                }
            } else {
                // Day without attendance
                if (isSunday || isSaturday) {
                    pillHtml = `<span class="cell-pill pill-muted" title="Ngày ${d}/${month} (Nghỉ cuối tuần)">-</span>`;
                } else {
                    pillHtml = `<span class="cell-pill pill-muted" title="Ngày ${d}/${month} (Không có dữ liệu)">-</span>`;
                }
            }

            cellsHtml += `<td class="${cellClass}">${pillHtml}</td>`;
        }

        return `
            <tr>
                <td class="col-sticky-emp">
                    <div class="emp-cell-content">
                        <div class="emp-avatar-circle" style="background:${bg}; color:${tc};">${initials}</div>
                        <div class="emp-name-title">
                            <span class="emp-name" title="${safe(emp.fullName)}">${safe(emp.fullName)}</span>
                            <span class="emp-code">${safe(emp.employeeCode)}</span>
                        </div>
                    </div>
                </td>
                ${cellsHtml}
            </tr>
        `;
    }).join('');
}

function getInitials(name) {
    if (!name) return 'NV';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

function renderDepartmentStats(stats) {
    const tbody = document.getElementById('departmentStatsRows');
    if (!tbody) return;

    if (!stats || stats.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; padding:32px; color:#667085;">Chưa có dữ liệu thống kê phòng ban cho tháng này.</td></tr>';
        return;
    }

    tbody.innerHTML = stats.map(st => {
        const rate = Number(st.punctualityRate || 0).toFixed(1);
        let rateColor = '#00866b';
        if (rate < 70) rateColor = '#c1121f';
        else if (rate < 85) rateColor = '#b54708';

        return `
            <tr>
                <td><strong>${safe(st.departmentName || 'Chung')}</strong></td>
                <td style="text-align:center;"><strong>${st.totalWorkDays ? '1' : '0'}</strong></td>
                <td style="text-align:center;"><strong>${st.totalHours || '0.00'}</strong> hrs</td>
                <td style="text-align:center;">
                    <div style="display:flex; align-items:center; justify-content:center; gap:8px;">
                        <div class="rate-bar-container" style="max-width:100px;">
                            <div class="rate-bar-fill" style="width:${rate}%; background:${rateColor};"></div>
                        </div>
                        <strong style="color:${rateColor}; font-size:12px;">${rate}%</strong>
                    </div>
                </td>
                <td style="text-align:center;"><span class="badge badge-late" style="font-size:11px;">${st.lateCount || 0}</span></td>
                <td style="text-align:center;"><span class="badge badge-early" style="font-size:11px;">${st.earlyLeaveCount || 0}</span></td>
                <td style="text-align:center;"><span class="badge badge-onleave" style="font-size:11px;">${st.onLeaveCount || 0}</span></td>
                <td style="text-align:center;"><span class="badge badge-absent" style="font-size:11px;">${st.absentCount || 0}</span></td>
            </tr>
        `;
    }).join('');
}

function exportTimesheetToExcel() {
    const deptSelect = document.getElementById('timesheetDeptSelect');
    const searchInput = document.getElementById('timesheetSearchInput');
    const departmentId = deptSelect ? deptSelect.value : '';
    const keyword = searchInput ? searchInput.value.trim() : '';

    const params = new URLSearchParams({
        month: currentTimesheetMonth,
        year: currentTimesheetYear
    });
    if (departmentId) params.append('departmentId', departmentId);
    if (keyword) params.append('keyword', keyword);

    window.location.href = withContext(`/attendance/export-timesheet?${params.toString()}`);
}

/* =========================================================================
   UC86 & UC87: Attendance Explanation Functions
   ========================================================================= */
function openSubmitExplanationModal(date, inTime, outTime) {
    const modal = document.getElementById('modalSubmitExplanation');
    if (!modal) return;
    const dateInput = document.getElementById('expAttendanceDate');
    if (dateInput && date) dateInput.value = date;
    const inInput = document.getElementById('expProposedCheckIn');
    if (inInput) inInput.value = inTime || '08:30';
    const outInput = document.getElementById('expProposedCheckOut');
    if (outInput) outInput.value = outTime || '17:30';
    modal.classList.add('active');
}

function closeSubmitExplanationModal() {
    const modal = document.getElementById('modalSubmitExplanation');
    if (modal) modal.classList.remove('active');
}

async function handleExplanationSubmit(e) {
    e.preventDefault();
    const date = document.getElementById('expAttendanceDate')?.value;
    const reasonType = document.getElementById('expReasonType')?.value;
    const inTime = document.getElementById('expProposedCheckIn')?.value;
    const outTime = document.getElementById('expProposedCheckOut')?.value;
    const detail = document.getElementById('expReasonDetail')?.value;

    if (!date) {
        alert('Vui lòng chọn ngày cần giải trình.');
        return;
    }
    if (!detail || !detail.trim()) {
        alert('Vui lòng nhập lý do chi tiết.');
        return;
    }

    try {
        const payload = await AttendanceApi.post('/attendance/explanation/submit', {
            attendanceDate: date,
            reasonType: reasonType || 'OTHER',
            proposedCheckIn: inTime || '',
            proposedCheckOut: outTime || '',
            reasonDetail: detail.trim()
        });
        if (payload && payload.success) {
            alert(payload.message || 'Gửi đơn giải trình chấm công thành công!');
            closeSubmitExplanationModal();
            document.getElementById('formSubmitExplanation')?.reset();
        } else {
            alert((payload && payload.message) ? payload.message : 'Không thể gửi đơn giải trình.');
        }
    } catch (err) {
        alert('Lỗi: ' + (err.message || 'Không thể kết nối đến máy chủ.'));
    }
}

async function openMyExplanationsModal() {
    const modal = document.getElementById('modalMyExplanations');
    if (!modal) return;
    modal.classList.add('active');
    await loadMyExplanations();
}

function closeMyExplanationsModal() {
    const modal = document.getElementById('modalMyExplanations');
    if (modal) modal.classList.remove('active');
}

async function loadMyExplanations() {
    const tbody = document.getElementById('myExplanationsRows');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:24px;color:#667085;">Đang tải danh sách...</td></tr>';

    try {
        const payload = await AttendanceApi.get('/attendance/explanation/my-requests');
        const list = (payload && payload.data) ? payload.data : [];
        if (list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:24px;color:#667085;">Bạn chưa có đơn giải trình nào.</td></tr>';
            return;
        }

        tbody.innerHTML = list.map(item => {
            let statusBadge = '';
            if (item.status === 'PENDING') {
                statusBadge = '<span class="badge" style="background:#fffaeb;color:#b54708;font-weight:700;">Chờ duyệt</span>';
            } else if (item.status === 'APPROVED') {
                statusBadge = '<span class="badge" style="background:#ecfdf3;color:#027a48;font-weight:700;">Đã duyệt</span>';
            } else if (item.status === 'REJECTED') {
                statusBadge = '<span class="badge" style="background:#fef3f2;color:#b42318;font-weight:700;">Từ chối</span>';
            } else {
                statusBadge = '<span class="badge" style="background:#f2f4f7;color:#667085;">Đã hủy</span>';
            }

            const inOut = (item.proposedCheckIn || '--:--') + ' - ' + (item.proposedCheckOut || '--:--');
            const cancelBtn = item.status === 'PENDING'
                ? `<button class="button small secondary" onclick="cancelMyExplanation(${item.explanationId})" style="padding:3px 8px;font-size:12px;color:#b42318;">Hủy đơn</button>`
                : '-';

            return `
                <tr style="border-bottom:1px solid #f2f4f7;">
                    <td style="padding:10px 8px;"><strong>${safe(item.attendanceDate)}</strong></td>
                    <td style="padding:10px 8px;">${safe(item.reasonTypeDisplay || item.reasonType)}</td>
                    <td style="padding:10px 8px;">${safe(inOut)}</td>
                    <td style="padding:10px 8px;">${statusBadge}</td>
                    <td style="padding:10px 8px;font-size:12px;color:#475467;">${item.reviewerComment ? safe(item.reviewerComment) : '-'}</td>
                    <td style="padding:10px 8px;text-align:center;">${cancelBtn}</td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:#b42318;padding:20px;">Lỗi tải dữ liệu: ${safe(err.message)}</td></tr>`;
    }
}

async function cancelMyExplanation(id) {
    if (!confirm('Bạn có chắc muốn hủy đơn giải trình này không?')) return;
    try {
        const payload = await AttendanceApi.post('/attendance/explanation/cancel', { explanationId: id });
        if (payload && payload.success) {
            alert('Hủy đơn thành công!');
            await loadMyExplanations();
        } else {
            alert((payload && payload.message) ? payload.message : 'Không thể hủy đơn.');
        }
    } catch (err) {
        alert('Lỗi: ' + (err.message || 'Không thể hủy đơn.'));
    }
}

async function openManagerExplanationsModal() {
    const modal = document.getElementById('modalExplanations');
    if (!modal) return;
    modal.style.display = 'flex';
    modal.classList.add('active');
    await loadPendingExplanations();
}

function closeManagerExplanationsModal() {
    const modal = document.getElementById('modalExplanations');
    if (!modal) return;
    modal.style.display = 'none';
    modal.classList.remove('active');
}

async function loadPendingExplanationsCount() {
    try {
        const payload = await AttendanceApi.get('/attendance/explanation/pending');
        const count = (payload && payload.data) ? payload.data.length : 0;
        const badge = document.getElementById('badgePendingExplanations');
        if (badge) {
            if (count > 0) {
                badge.textContent = count;
                badge.style.display = 'inline-block';
            } else {
                badge.style.display = 'none';
            }
        }
    } catch (err) {
        console.warn('Could not load pending explanations count:', err);
    }
}

async function loadPendingExplanations() {
    const tbody = document.getElementById('pendingExplanationsRows');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:24px;color:#667085;">Đang tải danh sách chờ duyệt...</td></tr>';

    try {
        const payload = await AttendanceApi.get('/attendance/explanation/pending');
        const list = (payload && payload.data) ? payload.data : [];
        const badge = document.getElementById('badgePendingExplanations');
        if (badge) {
            badge.textContent = list.length;
            badge.style.display = list.length > 0 ? 'inline-block' : 'none';
        }

        if (list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:24px;color:#667085;">Không có đơn giải trình nào đang chờ duyệt.</td></tr>';
            return;
        }

        tbody.innerHTML = list.map(item => {
            const inOut = (item.proposedCheckIn || '--:--') + ' - ' + (item.proposedCheckOut || '--:--');
            return `
                <tr style="border-bottom:1px solid #f2f4f7;">
                    <td style="padding:10px 8px;">
                        <strong>${safe(item.employeeFullName)}</strong>
                        <div style="font-size:12px;color:#667085;">${safe(item.employeeCode)} · ${safe(item.departmentName || 'Chung')}</div>
                    </td>
                    <td style="padding:10px 8px;"><strong>${safe(item.attendanceDate)}</strong></td>
                    <td style="padding:10px 8px;"><span class="badge" style="background:#f4ebff;color:#6941c6;font-size:11px;">${safe(item.reasonTypeDisplay || item.reasonType)}</span></td>
                    <td style="padding:10px 8px;">${safe(inOut)}</td>
                    <td style="padding:10px 8px;max-width:240px;font-size:12px;color:#344054;">${safe(item.reasonDetail)}</td>
                    <td style="padding:10px 8px;text-align:center;">
                        <button class="button small" onclick="reviewExplanation(${item.explanationId}, 'APPROVE')" style="background:#059669;border-color:#059669;color:#fff;padding:4px 8px;font-size:12px;margin-right:4px;">Duyệt</button>
                        <button class="button small secondary" onclick="reviewExplanation(${item.explanationId}, 'REJECT')" style="color:#b42318;padding:4px 8px;font-size:12px;">Từ chối</button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:#b42318;padding:20px;">Lỗi tải dữ liệu: ${safe(err.message)}</td></tr>`;
    }
}

async function reviewExplanation(id, decision) {
    const isApprove = decision === 'APPROVE';
    const actionText = isApprove ? 'Duyệt đơn giải trình và tự động cập nhật ngày công' : 'Từ chối đơn giải trình';
    const comment = prompt(`Nhập phản hồi/ghi chú cho nhân viên (${actionText}):`, isApprove ? 'Đồng ý bổ sung công' : 'Không đủ căn cứ giải trình');
    if (comment === null) return;

    try {
        const payload = await AttendanceApi.post('/attendance/explanation/review', {
            explanationId: id,
            decision: decision,
            comment: comment
        });
        if (payload && payload.success) {
            alert(payload.message || 'Đã xử lý đơn thành công!');
            await loadPendingExplanations();
            if (typeof loadTeamAttendanceData === 'function') {
                await loadTeamAttendanceData();
            }
        } else {
            alert((payload && payload.message) ? payload.message : 'Không thể xử lý đơn giải trình.');
        }
    } catch (err) {
        alert('Lỗi: ' + (err.message || 'Không thể xử lý đơn.'));
    }
}

/* =========================================================================
   UC88: Timesheet Locking Functions
   ========================================================================= */
let isCurrentTimesheetLocked = false;

async function checkTimesheetLockStatus(month, year) {
    const badge = document.getElementById('lockStatusBadge');
    const btn = document.getElementById('btnToggleTimesheetLock');
    const btnText = document.getElementById('btnLockText');
    if (!badge || !btn) return;

    try {
        const payload = await AttendanceApi.get(`/attendance/timesheet-lock?month=${month}&year=${year}`);
        isCurrentTimesheetLocked = payload && payload.isLocked;

        badge.style.display = 'inline-flex';
        btn.style.display = 'inline-flex';

        if (isCurrentTimesheetLocked) {
            badge.style.background = '#fef3f2';
            badge.style.color = '#b42318';
            badge.style.border = '1px solid #fecdca';
            badge.innerHTML = `🔒 Đã khóa chốt lương`;

            btn.style.background = '#f9fafb';
            btn.style.borderColor = '#d0d5dd';
            btn.style.color = '#344054';
            if (btnText) btnText.textContent = 'Mở khóa công';
        } else {
            badge.style.background = '#ecfdf3';
            badge.style.color = '#027a48';
            badge.style.border = '1px solid #a6f4c5';
            badge.innerHTML = `🔓 Chưa khóa`;

            btn.style.background = '#f79009';
            btn.style.borderColor = '#f79009';
            btn.style.color = '#ffffff';
            if (btnText) btnText.textContent = 'Khóa công tháng';
        }
    } catch (err) {
        console.warn('Could not check timesheet lock status:', err);
    }
}

async function toggleTimesheetLock() {
    const action = isCurrentTimesheetLocked ? 'UNLOCK' : 'LOCK';
    const actionText = isCurrentTimesheetLocked
        ? `Mở khóa bảng chấm công tháng ${currentTimesheetMonth}/${currentTimesheetYear}? (Sau khi mở khóa, quản lý có thể sửa đổi hoặc duyệt công bù)`
        : `Khóa bảng chấm công tháng ${currentTimesheetMonth}/${currentTimesheetYear}? (Sau khi khóa, mọi thao tác sửa công hoặc duyệt giải trình của tháng này sẽ bị chặn để chốt lương)`;

    if (!confirm(actionText)) return;

    const notes = isCurrentTimesheetLocked ? '' : (prompt('Nhập ghi chú chốt kỳ công (không bắt buộc):', 'Chốt công tháng phục vụ tính lương') || '');

    try {
        const payload = await AttendanceApi.post('/attendance/timesheet-lock', {
            month: currentTimesheetMonth,
            year: currentTimesheetYear,
            action: action,
            notes: notes
        });

        if (payload && payload.success) {
            alert(payload.message || 'Thao tác thành công!');
            await checkTimesheetLockStatus(currentTimesheetMonth, currentTimesheetYear);
        } else {
            alert((payload && payload.message) ? payload.message : 'Thao tác không thành công.');
        }
    } catch (err) {
        alert('Lỗi: ' + (err.message || 'Không thể thực hiện khóa/mở khóa.'));
    }
}
