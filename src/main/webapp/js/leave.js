/**
 * ==========================================================================
 * Leave Management Client Scripts (leave.js)
 * Manages dynamic working-day calculations, quota balance views, request tables,
 * paginated filters, review modals, and calendar grids.
 * ==========================================================================
 */

const LeaveApp = (() => {
    // Utility: calculate working days locally (Monday to Friday)
    function computeWorkingDays(startStr, endStr) {
        if (!startStr || !endStr) return 0;
        const start = new Date(startStr);
        const end = new Date(endStr);
        if (end < start) return 0;

        let count = 0;
        const cur = new Date(start);
        while (cur <= end) {
            const day = cur.getDay(); // 0 is Sunday, 6 is Saturday
            if (day !== 0 && day !== 6) {
                count++;
            }
            cur.setDate(cur.getDate() + 1);
        }
        return count;
    }

    // Utility: show toast or banner message
    function showMessage(elId, text, isError = false) {
        const el = document.getElementById(elId);
        if (!el) return;
        el.textContent = text;
        el.className = isError ? "calc-banner error" : "calc-banner";
        el.style.display = "block";
    }

    function clearMessage(elId) {
        const el = document.getElementById(elId);
        if (!el) return;
        el.textContent = "";
        el.style.display = "none";
    }

    // Modal controls
    function openModal(modalId) {
        const m = document.getElementById(modalId);
        if (m) m.classList.add("active");
    }

    function closeModal(modalId) {
        const m = document.getElementById(modalId);
        if (m) m.classList.remove("active");
    }

    // --------------------------------------------------------------------------
    // 1. Employee: Submit Leave Request
    // --------------------------------------------------------------------------
    function initSubmitForm() {
        const form = document.getElementById("leaveSubmitForm");
        const typeSelect = document.getElementById("leaveTypeId");
        const startDateInput = document.getElementById("startDate");
        const endDateInput = document.getElementById("endDate");
        const daysDisplay = document.getElementById("calcWorkingDays");
        const banner = document.getElementById("calcBanner");
        const balanceNotice = document.getElementById("balanceNotice");
        const btnSubmit = document.getElementById("btnSubmitLeave");

        let cachedBalances = {};

        // Load employee balances to show available quota
        fetch("../leave/api/my-balances")
            .then(res => res.json())
            .then(data => {
                if (data.success && data.data) {
                    typeSelect.innerHTML = '<option value="">-- Chọn loại nghỉ phép --</option>';
                    data.data.forEach(b => {
                        cachedBalances[b.leaveTypeId] = b;
                        const opt = document.createElement("option");
                        opt.value = b.leaveTypeId;
                        opt.textContent = `${b.leaveTypeName} (Còn lại: ${b.remainingDays} ngày)`;
                        typeSelect.appendChild(opt);
                    });
                }
            })
            .catch(() => {});

        function updateCalculation() {
            const sVal = startDateInput.value;
            const eVal = endDateInput.value;
            const tId = typeSelect.value;

            if (!sVal || !eVal) {
                FormValidation.setError(form, 'endDate', '');
                banner.style.display = "none";
                balanceNotice.textContent = "";
                return;
            }

            const days = computeWorkingDays(sVal, eVal);
            daysDisplay.textContent = days;
            banner.style.display = "flex";

            if (days <= 0) {
                FormValidation.setError(form, 'endDate', 'Khoảng thời gian đã chọn không có ngày làm việc.');
                banner.className = "calc-banner error";
                banner.querySelector(".calc-text").textContent = "Khoảng thời gian đã chọn không có ngày làm việc (rơi vào T7, CN).";
                btnSubmit.disabled = true;
                return;
            }

            banner.className = "calc-banner";
            FormValidation.setError(form, 'endDate', '');
            banner.querySelector(".calc-text").innerHTML = `Số ngày làm việc tính toán: <strong>${days} ngày</strong> (đã trừ Thứ 7, Chủ Nhật)`;

            if (tId && cachedBalances[tId]) {
                const rem = parseFloat(cachedBalances[tId].remainingDays || 0);
                if (days > rem) {
                    FormValidation.setError(form, 'endDate', `Số ngày nghỉ vượt quá số dư hiện tại (${rem} ngày).`);
                    banner.className = "calc-banner error";
                    balanceNotice.innerHTML = `<span style="color:#b42318;font-weight:700;">Vượt quá số dư hiện tại (${rem} ngày còn lại)!</span>`;
                    btnSubmit.disabled = true;
                    return;
                } else {
                    FormValidation.setError(form, 'endDate', '');
                    balanceNotice.innerHTML = `<span style="color:#027a48;">Đủ hạn mức (${rem - days} ngày còn lại sau khi trừ).</span>`;
                    btnSubmit.disabled = false;
                }
            } else {
                balanceNotice.textContent = "";
                btnSubmit.disabled = false;
            }
        }

        startDateInput.addEventListener("change", () => {
            if (!endDateInput.value || endDateInput.value < startDateInput.value) {
                endDateInput.value = startDateInput.value;
            }
            updateCalculation();
        });
        endDateInput.addEventListener("change", updateCalculation);
        typeSelect.addEventListener("change", updateCalculation);

        form.addEventListener("submit", e => {
            e.preventDefault();
            const formData = new URLSearchParams(new FormData(form));

            btnSubmit.disabled = true;
            btnSubmit.textContent = "Đang gửi đơn...";

            fetch("../leave/api/submit", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: formData.toString()
            })
                .then(res => res.json())
                .then(data => {
                    if (data.success) {
                        alert(data.message || "Nộp đơn xin nghỉ phép thành công!");
                        window.location.href = "my-requests";
                    } else {
                        if (!FormValidation.showErrors(form, data)) {
                            showMessage('submitMessage', data.message || 'Không thể nộp đơn.', true);
                        }
                        btnSubmit.disabled = false;
                        btnSubmit.textContent = "Nộp đơn xin nghỉ";
                    }
                })
                .catch(err => {
                    alert("Lỗi kết nối máy chủ: " + err);
                    btnSubmit.disabled = false;
                    btnSubmit.textContent = "Nộp đơn xin nghỉ";
                });
        });
    }

    // --------------------------------------------------------------------------
    // 2. Employee: Leave Balances View
    // --------------------------------------------------------------------------
    function initBalancesView() {
        const container = document.getElementById("balanceGridContainer");
        const yearSelect = document.getElementById("yearSelect");

        function loadBalances(year) {
            container.innerHTML = '<div style="grid-column: 1/-1; text-align: center; color: #667085; padding: 32px;">Đang tải số dư ngày phép...</div>';

            fetch(`../leave/api/my-balances?year=${year}`)
                .then(res => res.json())
                .then(data => {
                    if (!data.success || !data.data || data.data.length === 0) {
                        container.innerHTML = '<div style="grid-column: 1/-1; text-align: center; color: #667085; padding: 32px;">Chưa có chính sách ngày phép cho năm này.</div>';
                        return;
                    }

                    container.innerHTML = "";
                    data.data.forEach(b => {
                        const card = document.createElement("div");
                        card.className = "balance-card";

                        const paidBadge = b.isPaid
                            ? '<span class="badge badge-paid">Có hưởng lương</span>'
                            : '<span class="badge badge-unpaid">Không lương</span>';

                        card.innerHTML = `
                            <div>
                                <div class="balance-card-header">
                                    <div>
                                        <h3 class="balance-card-title">${b.leaveTypeName}</h3>
                                        <div class="balance-card-code">${b.leaveTypeCode}</div>
                                    </div>
                                    ${paidBadge}
                                </div>
                                <div class="balance-stat-primary">
                                    <div class="balance-number-lg">${b.remainingDays}</div>
                                    <div class="balance-number-label">ngày còn lại</div>
                                </div>
                            </div>
                            <div class="balance-details-row">
                                <div class="balance-details-item">
                                    <span>Được cấp:</span>
                                    <span>${b.entitledDays} ngày</span>
                                </div>
                                <div class="balance-details-item">
                                    <span>Đã dùng:</span>
                                    <span>${b.usedDays} ngày</span>
                                </div>
                            </div>
                        `;
                        container.appendChild(card);
                    });
                })
                .catch(() => {
                    container.innerHTML = '<div style="grid-column: 1/-1; text-align: center; color: #b42318; padding: 32px;">Lỗi tải dữ liệu số dư.</div>';
                });
        }

        if (yearSelect) {
            yearSelect.addEventListener("change", () => loadBalances(yearSelect.value));
            loadBalances(yearSelect.value || new Date().getFullYear());
        }
    }

    // --------------------------------------------------------------------------
    // 3. Employee: My Requests History
    // --------------------------------------------------------------------------
    function initMyRequestsView() {
        const tableBody = document.getElementById("requestsTableBody");
        const statusFilter = document.getElementById("statusFilter");
        const yearFilter = document.getElementById("yearFilter");
        const paginationContainer = document.getElementById("paginationBar");
        let currentPage = 1;

        function loadRequests(page = 1) {
            currentPage = page;
            const status = statusFilter ? statusFilter.value : "ALL";
            const year = yearFilter ? yearFilter.value : "";

            tableBody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: #667085; padding: 24px;">Đang tải danh sách đơn...</td></tr>';

            fetch(`../leave/api/my-requests?status=${encodeURIComponent(status)}&year=${encodeURIComponent(year)}&page=${page}`)
                .then(res => res.json())
                .then(data => {
                    if (!data.success || !data.data || data.data.length === 0) {
                        tableBody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: #667085; padding: 24px;">Không tìm thấy đơn nghỉ phép nào.</td></tr>';
                        renderPagination(paginationContainer, 0, 1, 20, loadRequests);
                        return;
                    }

                    tableBody.innerHTML = "";
                    data.data.forEach(r => {
                        const tr = document.createElement("tr");

                        let cancelBtn = "";
                        if (r.status === "Pending") {
                            cancelBtn = `<button class="btn-action cancel" onclick="LeaveApp.cancelRequest(${r.leaveRequestId})">Hủy đơn</button>`;
                        }

                        tr.innerHTML = `
                            <td>#${r.leaveRequestId}</td>
                            <td><strong>${r.leaveTypeName}</strong></td>
                            <td>${formatDate(r.startDate)} - ${formatDate(r.endDate)}</td>
                            <td style="font-weight:700; color:var(--navy,#062579);">${r.totalDays} ngày</td>
                            <td><span style="max-width:200px; display:inline-block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;" title="${escapeHtml(r.reason)}">${escapeHtml(r.reason)}</span></td>
                            <td><span class="badge ${r.status}">${translateStatus(r.status)}</span></td>
                            <td>
                                ${r.approverComment ? `<span style="font-size:12px; color:#475467;" title="${escapeHtml(r.approverComment)}">${escapeHtml(r.approverComment)}</span>` : '<span style="color:#98a2b3;">—</span>'}
                            </td>
                            <td>
                                <div class="btn-action-group">
                                    <button class="btn-action view" onclick='LeaveApp.viewRequestDetail(${JSON.stringify(r)})'>Chi tiết</button>
                                    ${cancelBtn}
                                </div>
                            </td>
                        `;
                        tableBody.appendChild(tr);
                    });

                    renderPagination(paginationContainer, data.total, data.page, data.pageSize, loadRequests);
                })
                .catch(() => {
                    tableBody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: #b42318; padding: 24px;">Lỗi khi tải dữ liệu đơn.</td></tr>';
                });
        }

        if (statusFilter) statusFilter.addEventListener("change", () => loadRequests(1));
        if (yearFilter) yearFilter.addEventListener("change", () => loadRequests(1));
        loadRequests(1);
    }

    // --------------------------------------------------------------------------
    // 4. Manager: Pending Approvals
    // --------------------------------------------------------------------------
    function initPendingApprovalsView() {
        const tableBody = document.getElementById("pendingTableBody");
        const paginationContainer = document.getElementById("paginationBar");
        let currentPage = 1;

        function loadPending(page = 1) {
            currentPage = page;
            tableBody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: #667085; padding: 24px;">Đang tải danh sách chờ duyệt...</td></tr>';

            fetch(`../leave/api/pending-approvals?page=${page}`)
                .then(res => res.json())
                .then(data => {
                    if (!data.success || !data.data || data.data.length === 0) {
                        tableBody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: #667085; padding: 24px;">Không có đơn xin nghỉ phép nào đang chờ duyệt.</td></tr>';
                        renderPagination(paginationContainer, 0, 1, 20, loadPending);
                        return;
                    }

                    tableBody.innerHTML = "";
                    data.data.forEach(r => {
                        const tr = document.createElement("tr");
                        tr.innerHTML = `
                            <td>#${r.leaveRequestId}</td>
                            <td>
                                <strong>${escapeHtml(r.employeeFullName)}</strong>
                                <div style="font-size:11px;color:#667085;">${escapeHtml(r.employeeCode)} • ${escapeHtml(r.departmentName || "—")}</div>
                            </td>
                            <td><strong>${r.leaveTypeName}</strong></td>
                            <td>${formatDate(r.startDate)} - ${formatDate(r.endDate)}</td>
                            <td style="font-weight:700; color:var(--navy,#062579);">${r.totalDays} ngày</td>
                            <td style="max-width:220px;"><div style="overflow:hidden; text-overflow:ellipsis; white-space:nowrap;" title="${escapeHtml(r.reason)}">${escapeHtml(r.reason)}</div></td>
                            <td><span style="font-size:12px;color:#667085;">${r.createdAt}</span></td>
                            <td>
                                <div class="btn-action-group">
                                    <button class="btn-action approve" onclick="LeaveApp.openApproveModal(${r.leaveRequestId}, '${escapeHtml(r.employeeFullName)}')">Duyệt</button>
                                    <button class="btn-action reject" onclick="LeaveApp.openRejectModal(${r.leaveRequestId}, '${escapeHtml(r.employeeFullName)}')">Từ chối</button>
                                </div>
                            </td>
                        `;
                        tableBody.appendChild(tr);
                    });

                    renderPagination(paginationContainer, data.total, data.page, data.pageSize, loadPending);
                })
                .catch(() => {
                    tableBody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: #b42318; padding: 24px;">Lỗi khi tải danh sách chờ duyệt.</td></tr>';
                });
        }

        window.LeaveAppReloadPending = () => loadPending(currentPage);
        loadPending(1);
    }

    // --------------------------------------------------------------------------
    // 5. Admin / HR: Company-wide Requests
    // --------------------------------------------------------------------------
    function initAllRequestsView() {
        const tableBody = document.getElementById("allRequestsTableBody");
        const paginationContainer = document.getElementById("paginationBar");
        const statusFilter = document.getElementById("statusFilter");
        const deptFilter = document.getElementById("deptFilter");
        const fromDate = document.getElementById("fromDate");
        const toDate = document.getElementById("toDate");
        const keywordInput = document.getElementById("keywordInput");
        const btnFilter = document.getElementById("btnFilter");
        let currentPage = 1;

        // Load departments dropdown
        fetch("../attendance/meta")
            .then(res => res.json())
            .then(data => {
                if (data.departments && deptFilter) {
                    data.departments.forEach(d => {
                        const opt = document.createElement("option");
                        opt.value = d.departmentId;
                        opt.textContent = d.departmentName;
                        deptFilter.appendChild(opt);
                    });
                }
            })
            .catch(() => {});

        function loadAll(page = 1) {
            currentPage = page;
            const params = new URLSearchParams({
                status: statusFilter ? statusFilter.value : "ALL",
                departmentId: deptFilter ? deptFilter.value : "",
                fromDate: fromDate ? fromDate.value : "",
                toDate: toDate ? toDate.value : "",
                keyword: keywordInput ? keywordInput.value : "",
                page: page
            });

            tableBody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: #667085; padding: 24px;">Đang tải danh sách đơn toàn công ty...</td></tr>';

            fetch(`../leave/api/all-requests?${params.toString()}`)
                .then(res => res.json())
                .then(data => {
                    if (!data.success || !data.data || data.data.length === 0) {
                        tableBody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: #667085; padding: 24px;">Không tìm thấy đơn nghỉ phép phù hợp.</td></tr>';
                        renderPagination(paginationContainer, 0, 1, 20, loadAll);
                        return;
                    }

                    tableBody.innerHTML = "";
                    data.data.forEach(r => {
                        const tr = document.createElement("tr");

                        let actions = `<button class="btn-action view" onclick='LeaveApp.viewRequestDetail(${JSON.stringify(r)})'>Xem</button>`;
                        if (r.status === "Pending") {
                            actions += `
                                <button class="btn-action approve" onclick="LeaveApp.openApproveModal(${r.leaveRequestId}, '${escapeHtml(r.employeeFullName)}')">Duyệt</button>
                                <button class="btn-action reject" onclick="LeaveApp.openRejectModal(${r.leaveRequestId}, '${escapeHtml(r.employeeFullName)}')">Từ chối</button>
                            `;
                        }

                        tr.innerHTML = `
                            <td>#${r.leaveRequestId}</td>
                            <td>
                                <strong>${escapeHtml(r.employeeFullName)}</strong>
                                <div style="font-size:11px;color:#667085;">${escapeHtml(r.employeeCode)} • ${escapeHtml(r.departmentName || "—")}</div>
                            </td>
                            <td><strong>${r.leaveTypeName}</strong></td>
                            <td>${formatDate(r.startDate)} - ${formatDate(r.endDate)}</td>
                            <td style="font-weight:700; color:var(--navy,#062579);">${r.totalDays} ngày</td>
                            <td style="max-width:200px;"><div style="overflow:hidden; text-overflow:ellipsis; white-space:nowrap;" title="${escapeHtml(r.reason)}">${escapeHtml(r.reason)}</div></td>
                            <td><span class="badge ${r.status}">${translateStatus(r.status)}</span></td>
                            <td><span style="font-size:12px;color:#667085;">${r.approverDisplayName || "—"}</span></td>
                            <td><div class="btn-action-group">${actions}</div></td>
                        `;
                        tableBody.appendChild(tr);
                    });

                    renderPagination(paginationContainer, data.total, data.page, data.pageSize, loadAll);
                })
                .catch(() => {
                    tableBody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: #b42318; padding: 24px;">Lỗi tải danh sách đơn.</td></tr>';
                });
        }

        if (btnFilter) btnFilter.addEventListener("click", () => loadAll(1));
        if (statusFilter) statusFilter.addEventListener("change", () => loadAll(1));
        if (deptFilter) deptFilter.addEventListener("change", () => loadAll(1));
        window.LeaveAppReloadAll = () => loadAll(currentPage);
        loadAll(1);
    }

    // --------------------------------------------------------------------------
    // 6. Admin / HR: Leave Policies & Types Configuration
    // --------------------------------------------------------------------------
    function initLeaveTypesView() {
        const tableBody = document.getElementById("typesTableBody");
        const form = document.getElementById("leaveTypeForm");

        function loadTypes() {
            tableBody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:#667085;padding:24px;">Đang tải danh mục...</td></tr>';

            fetch("../leave/api/types")
                .then(res => res.json())
                .then(data => {
                    if (!data.success || !data.data || data.data.length === 0) {
                        tableBody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:#667085;padding:24px;">Chưa có loại nghỉ phép nào. Hãy tạo mới.</td></tr>';
                        return;
                    }

                    tableBody.innerHTML = "";
                    data.data.forEach(t => {
                        const tr = document.createElement("tr");
                        const paidBadge = t.isPaid
                            ? '<span class="badge badge-paid">Có hưởng lương</span>'
                            : '<span class="badge badge-unpaid">Không lương</span>';

                        tr.innerHTML = `
                            <td>#${t.leaveTypeId}</td>
                            <td><span style="font-family:monospace;font-weight:700;color:var(--navy,#062579);">${t.typeCode}</span></td>
                            <td><strong>${escapeHtml(t.typeName)}</strong></td>
                            <td style="font-weight:700;">${t.defaultDaysPerYear} ngày</td>
                            <td>${paidBadge}</td>
                            <td>
                                <div class="btn-action-group">
                                    <button class="btn-action view" onclick='LeaveApp.editLeaveType(${JSON.stringify(t)})'>Sửa</button>
                                    <button class="btn-action reject" onclick="LeaveApp.deleteLeaveType(${t.leaveTypeId}, '${escapeHtml(t.typeName)}')">Xóa</button>
                                </div>
                            </td>
                        `;
                        tableBody.appendChild(tr);
                    });
                })
                .catch(() => {
                    tableBody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:#b42318;padding:24px;">Lỗi tải danh mục.</td></tr>';
                });
        }

        if (form) {
            form.addEventListener("submit", e => {
                e.preventDefault();
                const formData = new URLSearchParams(new FormData(form));

                fetch("../leave/api/types", {
                    method: "POST",
                    headers: { "Content-Type": "application/x-www-form-urlencoded" },
                    body: formData.toString()
                })
                    .then(res => res.json())
                    .then(data => {
                        if (data.success) {
                            alert(data.message || "Lưu loại nghỉ phép thành công!");
                            closeModal("modalLeaveType");
                            form.reset();
                            loadTypes();
                        } else {
                            if (!FormValidation.showErrors(form, data)) {
                                showMessage('typeMessage', data.message || 'Không thể lưu loại nghỉ phép.', true);
                            }
                        }
                    })
                    .catch(err => alert("Lỗi kết nối: " + err));
            });
        }

        loadTypes();
    }

    // --------------------------------------------------------------------------
    // 7. Interactive Calendar View (Employee & Company)
    // --------------------------------------------------------------------------
    function initCalendarView(isEmployeeOnly = false) {
        let currentYear = new Date().getFullYear();
        let currentMonth = new Date().getMonth() + 1; // 1-indexed

        const monthTitle = document.getElementById("calendarMonthTitle");
        const daysContainer = document.getElementById("calendarDaysContainer");
        const btnPrev = document.getElementById("btnPrevMonth");
        const btnNext = document.getElementById("btnNextMonth");
        const btnToday = document.getElementById("btnCurrentMonth");
        const deptFilter = document.getElementById("deptFilter");

        if (deptFilter && !isEmployeeOnly) {
            fetch("../attendance/meta")
                .then(res => res.json())
                .then(data => {
                    if (data.departments) {
                        data.departments.forEach(d => {
                            const opt = document.createElement("option");
                            opt.value = d.departmentId;
                            opt.textContent = d.departmentName;
                            deptFilter.appendChild(opt);
                        });
                    }
                })
                .catch(() => {});

            deptFilter.addEventListener("change", () => renderMonth(currentYear, currentMonth));
        }

        function renderMonth(year, month) {
            monthTitle.textContent = `Tháng ${month} / ${year}`;
            daysContainer.innerHTML = '<div style="grid-column:1/-1;text-align:center;padding:32px;background:#fff;color:#667085;">Đang tải lịch...</div>';

            const deptId = (deptFilter && !isEmployeeOnly) ? deptFilter.value : "";
            const url = `../leave/api/calendar-events?year=${year}&month=${month}&departmentId=${encodeURIComponent(deptId)}`;

            fetch(url)
                .then(res => res.json())
                .then(data => {
                    const events = (data.success && data.data) ? data.data : [];
                    buildCalendarGrid(year, month, events);
                })
                .catch(() => {
                    daysContainer.innerHTML = '<div style="grid-column:1/-1;text-align:center;padding:32px;background:#fff;color:#b42318;">Lỗi khi tải lịch nghỉ phép.</div>';
                });
        }

        function buildCalendarGrid(year, month, events) {
            daysContainer.innerHTML = "";

            const firstDayOfMonth = new Date(year, month - 1, 1);
            const lastDayOfMonth = new Date(year, month, 0);
            const numDays = lastDayOfMonth.getDate();

            // Day of week: 0 is Sun, 1 is Mon ... we want Monday to be 0
            let startDay = firstDayOfMonth.getDay() - 1;
            if (startDay < 0) startDay = 6; // Sunday becomes 6

            const prevMonthLastDay = new Date(year, month - 1, 0).getDate();

            // Previous month cells
            for (let i = startDay - 1; i >= 0; i--) {
                const cell = document.createElement("div");
                cell.className = "calendar-day-cell other-month";
                cell.innerHTML = `<div class="day-header"><span class="day-number">${prevMonthLastDay - i}</span></div>`;
                daysContainer.appendChild(cell);
            }

            const today = new Date();
            const isThisMonth = today.getFullYear() === year && (today.getMonth() + 1) === month;

            // Current month cells
            for (let d = 1; d <= numDays; d++) {
                const curDate = new Date(year, month - 1, d);
                const dayOfWeek = curDate.getDay();
                const isWeekend = (dayOfWeek === 0 || dayOfWeek === 6);
                const isToday = isThisMonth && today.getDate() === d;

                const cell = document.createElement("div");
                cell.className = "calendar-day-cell" + (isWeekend ? " weekend" : "") + (isToday ? " today" : "");

                const dayStr = `${year}-${String(month).padStart(2, "0")}-${String(d).padStart(2, "0")}`;

                // Filter events that cover this day
                const dayEvents = events.filter(e => {
                    return dayStr >= e.startDate && dayStr <= e.endDate;
                });

                let chipsHtml = "";
                dayEvents.forEach(e => {
                    let chipClass = "other";
                    const code = (e.leaveTypeCode || "").toUpperCase();
                    if (code.includes("ANNUAL")) chipClass = "annual";
                    else if (code.includes("SICK")) chipClass = "sick";
                    else if (code.includes("UNPAID")) chipClass = "unpaid";
                    else if (code.includes("MATERNITY")) chipClass = "maternity";

                    const title = isEmployeeOnly
                        ? `${e.leaveTypeName} (${e.totalDays}d)`
                        : `${e.employeeFullName} - ${e.leaveTypeName}`;

                    chipsHtml += `
                        <div class="calendar-event-chip ${chipClass}" 
                             onclick='LeaveApp.viewCalendarEventDetail(${JSON.stringify(e)})' 
                             title="${escapeHtml(title)}: ${escapeHtml(e.reason)}">
                            ${escapeHtml(title)}
                        </div>
                    `;
                });

                cell.innerHTML = `
                    <div class="day-header">
                        <span class="day-number">${d}</span>
                    </div>
                    <div style="flex:1;overflow-y:auto;">${chipsHtml}</div>
                `;
                daysContainer.appendChild(cell);
            }

            // Fill trailing cells to complete grid row (7 columns)
            const totalCells = startDay + numDays;
            const remaining = (7 - (totalCells % 7)) % 7;
            for (let i = 1; i <= remaining; i++) {
                const cell = document.createElement("div");
                cell.className = "calendar-day-cell other-month";
                cell.innerHTML = `<div class="day-header"><span class="day-number">${i}</span></div>`;
                daysContainer.appendChild(cell);
            }
        }

        btnPrev.addEventListener("click", () => {
            currentMonth--;
            if (currentMonth < 1) {
                currentMonth = 12;
                currentYear--;
            }
            renderMonth(currentYear, currentMonth);
        });

        btnNext.addEventListener("click", () => {
            currentMonth++;
            if (currentMonth > 12) {
                currentMonth = 1;
                currentYear++;
            }
            renderMonth(currentYear, currentMonth);
        });

        btnToday.addEventListener("click", () => {
            currentYear = new Date().getFullYear();
            currentMonth = new Date().getMonth() + 1;
            renderMonth(currentYear, currentMonth);
        });

        renderMonth(currentYear, currentMonth);
    }

    // --------------------------------------------------------------------------
    // 8. Admin / HR: Leave Utilization Report
    // --------------------------------------------------------------------------
    function initReportView() {
        const tableBody = document.getElementById("reportTableBody");
        const yearSelect = document.getElementById("reportYearSelect");
        const deptFilter = document.getElementById("reportDeptFilter");
        const btnExport = document.getElementById("btnExportExcel");

        // Load departments
        fetch("../attendance/meta")
            .then(res => res.json())
            .then(data => {
                if (data.departments && deptFilter) {
                    data.departments.forEach(d => {
                        const opt = document.createElement("option");
                        opt.value = d.departmentId;
                        opt.textContent = d.departmentName;
                        deptFilter.appendChild(opt);
                    });
                }
            })
            .catch(() => {});

        function loadReport() {
            const year = yearSelect ? yearSelect.value : new Date().getFullYear();
            const deptId = deptFilter ? deptFilter.value : "";

            tableBody.innerHTML = '<tr><td colspan="12" style="text-align:center;color:#667085;padding:24px;">Đang tổng hợp báo cáo...</td></tr>';

            fetch(`../leave/api/report?year=${year}&departmentId=${encodeURIComponent(deptId)}`)
                .then(res => res.json())
                .then(data => {
                    if (!data.success || !data.data || data.data.length === 0) {
                        tableBody.innerHTML = '<tr><td colspan="12" style="text-align:center;color:#667085;padding:24px;">Không có dữ liệu báo cáo cho năm này.</td></tr>';
                        return;
                    }

                    tableBody.innerHTML = "";
                    let stt = 1;
                    data.data.forEach(r => {
                        const tr = document.createElement("tr");
                        tr.innerHTML = `
                            <td style="text-align:center;">${stt++}</td>
                            <td><span style="font-family:monospace;font-weight:700;">${escapeHtml(r.employeeCode)}</span></td>
                            <td><strong>${escapeHtml(r.fullName)}</strong></td>
                            <td>${escapeHtml(r.departmentName || "—")}</td>
                            <td>${escapeHtml(r.positionTitle || "—")}</td>
                            <td style="text-align:right;font-weight:700;">${r.entitledDays}</td>
                            <td style="text-align:right;color:#b54708;font-weight:700;">${r.usedDays}</td>
                            <td style="text-align:right;color:#027a48;font-weight:700;">${r.remainingDays}</td>
                            <td style="text-align:right;">${r.paidDays}</td>
                            <td style="text-align:right;">${r.unpaidDays}</td>
                            <td style="text-align:center;">${r.totalRequests}</td>
                            <td style="text-align:center;font-weight:700;color:#027a48;">${r.approvedRequests}</td>
                        `;
                        tableBody.appendChild(tr);
                    });
                })
                .catch(() => {
                    tableBody.innerHTML = '<tr><td colspan="12" style="text-align:center;color:#b42318;padding:24px;">Lỗi khi tổng hợp báo cáo.</td></tr>';
                });
        }

        if (yearSelect) yearSelect.addEventListener("change", loadReport);
        if (deptFilter) deptFilter.addEventListener("change", loadReport);

        if (btnExport) {
            btnExport.addEventListener("click", () => {
                const year = yearSelect ? yearSelect.value : new Date().getFullYear();
                const deptId = deptFilter ? deptFilter.value : "";
                window.location.href = `../leave/export-excel?year=${year}&departmentId=${encodeURIComponent(deptId)}`;
            });
        }

        loadReport();
    }

    // --------------------------------------------------------------------------
    // Pagination Generator Helper
    // --------------------------------------------------------------------------
    function renderPagination(container, totalRecords, curPage, pageSize, onPageChange) {
        if (!container) return;
        if (totalRecords <= 0) {
            container.innerHTML = '<span style="color:#667085;">Tổng cộng: 0 kết quả</span>';
            return;
        }

        const totalPages = Math.ceil(totalRecords / pageSize);
        const startRecord = (curPage - 1) * pageSize + 1;
        const endRecord = Math.min(curPage * pageSize, totalRecords);

        let buttonsHtml = `
            <button class="page-btn" ${curPage <= 1 ? "disabled" : ""} onclick="(${onPageChange})(${curPage - 1})">Trang trước</button>
        `;

        for (let p = 1; p <= totalPages; p++) {
            if (p === 1 || p === totalPages || (p >= curPage - 1 && p <= curPage + 1)) {
                buttonsHtml += `
                    <button class="page-btn ${p === curPage ? "active" : ""}" onclick="(${onPageChange})(${p})">${p}</button>
                `;
            } else if (p === curPage - 2 || p === curPage + 2) {
                buttonsHtml += '<span style="color:#98a2b3;padding:0 4px;">...</span>';
            }
        }

        buttonsHtml += `
            <button class="page-btn" ${curPage >= totalPages ? "disabled" : ""} onclick="(${onPageChange})(${curPage + 1})">Trang sau</button>
        `;

        container.innerHTML = `
            <div>Hiển thị <strong>${startRecord}</strong> - <strong>${endRecord}</strong> trong <strong>${totalRecords}</strong> bản ghi</div>
            <div class="pagination-controls">${buttonsHtml}</div>
        `;
    }

    // --------------------------------------------------------------------------
    // Shared Actions (Approve, Reject, Cancel, Details)
    // --------------------------------------------------------------------------
    function openApproveModal(reqId, empName) {
        document.getElementById("approveReqId").value = reqId;
        document.getElementById("approveEmpName").textContent = empName;
        document.getElementById("approveComment").value = "";
        openModal("modalApprove");
    }

    function confirmApprove() {
        const reqId = document.getElementById("approveReqId").value;
        const comment = document.getElementById("approveComment").value;
        const btn = document.getElementById("btnConfirmApprove");

        btn.disabled = true;
        btn.textContent = "Đang xử lý...";

        fetch("../leave/api/approve", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams({ leaveRequestId: reqId, comment: comment }).toString()
        })
            .then(res => res.json())
            .then(data => {
                btn.disabled = false;
                btn.textContent = "Xác nhận duyệt";
                if (data.success) {
                    alert(data.message || "Phê duyệt thành công!");
                    closeModal("modalApprove");
                    if (window.LeaveAppReloadPending) window.LeaveAppReloadPending();
                    if (window.LeaveAppReloadAll) window.LeaveAppReloadAll();
                } else {
                    alert(data.message || "Không thể phê duyệt đơn.");
                }
            })
            .catch(err => {
                btn.disabled = false;
                btn.textContent = "Xác nhận duyệt";
                alert("Lỗi: " + err);
            });
    }

    function openRejectModal(reqId, empName) {
        document.getElementById("rejectReqId").value = reqId;
        document.getElementById("rejectEmpName").textContent = empName;
        document.getElementById("rejectComment").value = "";
        openModal("modalReject");
    }

    function confirmReject() {
        const reqId = document.getElementById("rejectReqId").value;
        const comment = document.getElementById("rejectComment").value;
        const btn = document.getElementById("btnConfirmReject");

        if (!comment || comment.trim().length === 0) {
            FormValidation.setError(document, 'rejectComment', 'Vui lòng nhập lý do từ chối đơn xin nghỉ phép.');
            document.getElementById('rejectComment').focus();
            return;
        }
        FormValidation.setError(document, 'rejectComment', '');

        btn.disabled = true;
        btn.textContent = "Đang xử lý...";

        fetch("../leave/api/reject", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams({ leaveRequestId: reqId, comment: comment.trim() }).toString()
        })
            .then(res => res.json())
            .then(data => {
                btn.disabled = false;
                btn.textContent = "Từ chối đơn";
                if (data.success) {
                    alert(data.message || "Đã từ chối đơn.");
                    closeModal("modalReject");
                    if (window.LeaveAppReloadPending) window.LeaveAppReloadPending();
                    if (window.LeaveAppReloadAll) window.LeaveAppReloadAll();
                } else {
                    if (!FormValidation.showErrors(document, data)) {
                        alert(data.message || "Lỗi khi từ chối đơn.");
                    }
                }
            })
            .catch(err => {
                btn.disabled = false;
                btn.textContent = "Từ chối đơn";
                alert("Lỗi: " + err);
            });
    }

    function cancelRequest(reqId) {
        if (!confirm("Bạn có chắc chắn muốn hủy đơn xin nghỉ phép này không?")) {
            return;
        }

        fetch("../leave/api/cancel", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams({ leaveRequestId: reqId }).toString()
        })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    alert(data.message || "Đã hủy đơn thành công.");
                    window.location.reload();
                } else {
                    alert(data.message || "Không thể hủy đơn.");
                }
            })
            .catch(err => alert("Lỗi: " + err));
    }

    function viewRequestDetail(r) {
        const b = document.getElementById("detailModalBody");
        if (!b) return;

        b.innerHTML = `
            <div style="display:grid; grid-template-columns: 140px 1fr; gap: 10px; font-size: 13px;">
                <div style="color:#667085;">Mã đơn:</div>
                <div><strong>#${r.leaveRequestId}</strong></div>

                <div style="color:#667085;">Nhân viên:</div>
                <div><strong>${escapeHtml(r.employeeFullName || "")}</strong> (${escapeHtml(r.employeeCode || "")})</div>

                <div style="color:#667085;">Phòng ban:</div>
                <div>${escapeHtml(r.departmentName || "—")}</div>

                <div style="color:#667085;">Loại nghỉ phép:</div>
                <div><strong>${escapeHtml(r.leaveTypeName || "")}</strong> (${r.isPaid ? "Có lương" : "Không lương"})</div>

                <div style="color:#667085;">Khoảng thời gian:</div>
                <div>${formatDate(r.startDate)} đến ${formatDate(r.endDate)}</div>

                <div style="color:#667085;">Số ngày làm việc:</div>
                <div style="font-weight:800; color:var(--navy,#062579);">${r.totalDays} ngày</div>

                <div style="color:#667085;">Trạng thái:</div>
                <div><span class="badge ${r.status}">${translateStatus(r.status)}</span></div>

                <div style="color:#667085;">Lý do nghỉ:</div>
                <div style="background:#f8f9fc; padding: 8px 12px; border-radius:6px; border:1px solid #e5e9f5;">${escapeHtml(r.reason || "")}</div>

                <div style="color:#667085;">Người duyệt:</div>
                <div>${escapeHtml(r.approverDisplayName || "—")}</div>

                <div style="color:#667085;">Phản hồi người duyệt:</div>
                <div>${r.approverComment ? escapeHtml(r.approverComment) : '<span style="color:#98a2b3;">—</span>'}</div>

                <div style="color:#667085;">Thời gian nộp:</div>
                <div style="color:#667085;">${r.createdAt || "—"}</div>
            </div>
        `;
        openModal("modalDetail");
    }

    function viewCalendarEventDetail(e) {
        viewRequestDetail(e);
    }

    function editLeaveType(t) {
        document.getElementById("formLeaveTypeId").value = t.leaveTypeId;
        document.getElementById("formTypeCode").value = t.typeCode;
        document.getElementById("formTypeCode").readOnly = true;
        document.getElementById("formTypeName").value = t.typeName;
        document.getElementById("formDefaultDays").value = t.defaultDaysPerYear;
        document.getElementById("formIsPaid").checked = t.isPaid;
        document.getElementById("modalLeaveTypeTitle").textContent = "Chỉnh sửa loại nghỉ phép";
        openModal("modalLeaveType");
    }

    function openCreateLeaveTypeModal() {
        const form = document.getElementById("leaveTypeForm");
        if (form) form.reset();
        document.getElementById("formLeaveTypeId").value = "";
        document.getElementById("formTypeCode").readOnly = false;
        document.getElementById("modalLeaveTypeTitle").textContent = "Thêm loại nghỉ phép mới";
        openModal("modalLeaveType");
    }

    function deleteLeaveType(id, name) {
        if (!confirm(`Bạn có chắc chắn muốn xóa chính sách nghỉ phép "${name}" không?`)) {
            return;
        }

        fetch("../leave/api/types", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams({ action: "delete", leaveTypeId: id }).toString()
        })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    alert(data.message || "Đã xóa thành công!");
                    window.location.reload();
                } else {
                    alert(data.message || "Không thể xóa chính sách này.");
                }
            })
            .catch(err => alert("Lỗi: " + err));
    }

    // Helpers
    function formatDate(dateStr) {
        if (!dateStr) return "—";
        const parts = dateStr.split("-");
        if (parts.length === 3) {
            return `${parts[2]}/${parts[1]}/${parts[0]}`;
        }
        return dateStr;
    }

    function translateStatus(s) {
        switch (s) {
            case "Pending": return "Chờ duyệt";
            case "Approved": return "Đã duyệt";
            case "Rejected": return "Từ chối";
            case "Cancelled": return "Đã hủy";
            default: return s || "—";
        }
    }

    function escapeHtml(str) {
        if (!str) return "";
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    return {
        initSubmitForm,
        initBalancesView,
        initMyRequestsView,
        initPendingApprovalsView,
        initAllRequestsView,
        initLeaveTypesView,
        initCalendarView,
        initReportView,
        openModal,
        closeModal,
        openApproveModal,
        confirmApprove,
        openRejectModal,
        confirmReject,
        cancelRequest,
        viewRequestDetail,
        viewCalendarEventDetail,
        editLeaveType,
        openCreateLeaveTypeModal,
        deleteLeaveType
    };
})();
