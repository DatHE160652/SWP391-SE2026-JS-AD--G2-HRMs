(() => {
    'use strict';
    const context = document.body.dataset.context || '';
    const csrf = document.querySelector('meta[name="csrf-token"]')?.content || '';
    const $ = selector => document.querySelector(selector);

    const safe = value => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    const money = value => {
        if (value == null) return '—';
        const num = Math.round(Number(value));
        if (Number.isNaN(num)) return '—';
        return new Intl.NumberFormat(window.HrmI18n?.locale || 'vi-VN').format(num) + ' ₫';
    };

    let rawCandidates = [];
    try {
        const jsonEl = document.getElementById('candidatesData');
        if (jsonEl && jsonEl.textContent.trim()) {
            rawCandidates = JSON.parse(jsonEl.textContent.trim());
        }
    } catch (_) {
        rawCandidates = [];
    }

    const state = {
        candidates: [],
        periodYear: new Date().getFullYear(),
        periodId: document.getElementById('periodId')?.value || '',
        searchQuery: '',
        busy: false
    };

    function parseYearFromPage() {
        const periodText = document.querySelector('.page-heading p')?.textContent || '';
        const match = periodText.match(/(\d{4})/);
        return match ? parseInt(match[1], 10) : new Date().getFullYear();
    }
    state.periodYear = parseYearFromPage();

    function feedback(message, isError = false) {
        const el = $('#feedback');
        if (!el) return;
        el.textContent = message;
        el.className = 'feedback ' + (isError ? 'error' : 'success');
        el.hidden = false;
        el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    function calculateMonthsWorked(startDateStr, year) {
        if (!startDateStr) return 12;
        const parts = startDateStr.split('-');
        if (parts.length < 3) return 12;
        const sYear = parseInt(parts[0], 10);
        const sMonth = parseInt(parts[1], 10);
        const sDay = parseInt(parts[2], 10);

        if (sYear < year) return 12;
        if (sYear === year) {
            let months = 12 - sMonth + (sDay <= 15 ? 1 : 0);
            return Math.max(1, Math.min(12, months));
        }
        return 0;
    }

    function initCandidates() {
        state.candidates = rawCandidates.map(c => {
            const monthsWorked = calculateMonthsWorked(c.startDate, state.periodYear);
            return {
                ...c,
                monthsWorked,
                calculatedBonus: 0,
                finalBonus: 0,
                finalNote: '',
                isManuallyEdited: false
            };
        });
    }

    function computeFormulaForCandidate(c, config) {
        const base = Number(c.contractBaseSalary || 0);
        let bonusAmt = 0;
        let noteText = '';

        if (config.calcType === 'CONTRACT_SALARY') {
            if (base <= 0) {
                bonusAmt = 0;
                noteText = config.campaignName + ' (Chưa có lương cơ bản hợp đồng)';
            } else if (config.prorateByTenure) {
                const ratio = c.monthsWorked / 12;
                bonusAmt = Math.round(base * config.monthsMultiplier * ratio);
                noteText = config.campaignName + ' (' + config.monthsMultiplier + ' tháng lương - làm ' + c.monthsWorked + '/12 tháng năm ' + state.periodYear + ')';
            } else {
                bonusAmt = Math.round(base * config.monthsMultiplier);
                noteText = config.campaignName + ' (' + config.monthsMultiplier + ' tháng lương)';
            }
        } else {
            bonusAmt = Math.round(config.fixedAmount);
            noteText = config.campaignName;
        }

        let finalBonus = bonusAmt;
        let finalNote = noteText;
        if (config.applyMode === 'ADD') {
            const cur = Number(c.currentBonus || 0);
            finalBonus = cur + bonusAmt;
            finalNote = c.currentNote ? (c.currentNote + '; ' + noteText) : noteText;
        }

        return { bonusAmt: finalBonus, noteText: finalNote };
    }

    function getConfig() {
        const form = $('#bonusCampaignForm');
        return {
            campaignName: form?.elements.campaignName?.value?.trim() || 'Lương tháng 13',
            applyMode: form?.elements.applyMode?.value || 'REPLACE',
            calcType: form?.elements.calcType?.value || 'CONTRACT_SALARY',
            monthsMultiplier: parseFloat(form?.elements.monthsMultiplier?.value) || 1.0,
            fixedAmount: parseFloat(form?.elements.fixedAmount?.value) || 0,
            prorateByTenure: !!form?.elements.prorateByTenure?.checked
        };
    }

    function recalculateAll(forceResetManual = false) {
        const config = getConfig();

        // Update form visibility
        const isContract = config.calcType === 'CONTRACT_SALARY';
        const monthsContainer = $('#monthsMultiplierContainer');
        const fixedContainer = $('#fixedAmountContainer');
        const tenureBox = $('#tenureProrateBox');

        if (monthsContainer) monthsContainer.style.display = isContract ? 'block' : 'none';
        if (fixedContainer) fixedContainer.style.display = isContract ? 'none' : 'block';
        if (tenureBox) tenureBox.style.display = isContract ? 'block' : 'none';

        // Calculate
        state.candidates.forEach(c => {
            const computed = computeFormulaForCandidate(c, config);
            c.calculatedBonus = computed.bonusAmt;
            if (forceResetManual || !c.isManuallyEdited) {
                c.finalBonus = computed.bonusAmt;
                c.finalNote = computed.noteText;
            }
        });

        renderRows();
        updateMetrics();
    }

    function updateMetrics() {
        const count = state.candidates.filter(c => Number(c.finalBonus) > 0).length;
        const total = state.candidates.reduce((sum, c) => sum + (Number(c.finalBonus) || 0), 0);
        const avg = count > 0 ? Math.round(total / count) : 0;

        const countEl = $('#metricCandidateCount');
        const totalEl = $('#metricTotalBonus');
        const avgEl = $('#metricAvgBonus');

        if (countEl) countEl.textContent = count + ' nhân viên';
        if (totalEl) totalEl.textContent = money(total);
        if (avgEl) avgEl.textContent = money(avg);
    }

    function renderRows() {
        const tbody = $('#candidateRows');
        if (!tbody) return;

        const q = (state.searchQuery || '').trim().toLowerCase();
        const filtered = state.candidates.filter(c => {
            if (!q) return true;
            return (c.fullName + ' ' + c.employeeCode + ' ' + (c.departmentName || '')).toLowerCase().includes(q);
        });

        if (!filtered.length) {
            tbody.innerHTML = '<tr><td colspan="8" class="empty">Không tìm thấy nhân viên phù hợp.</td></tr>';
            return;
        }

        tbody.innerHTML = filtered.map(c => {
            const isFull = c.monthsWorked >= 12;
            const tenurePercent = Math.round((c.monthsWorked / 12) * 100);
            const tenureBadge = `<span class="tenure-pill ${isFull ? 'full' : ''}">${c.monthsWorked}/12 tháng (${tenurePercent}%)</span>`;
            const baseSalaryFormatted = c.contractBaseSalary ? money(c.contractBaseSalary) : '<span class="subtle">Chưa có HĐ</span>';

            return `<tr>
                <td><strong>${safe(c.employeeCode)}</strong></td>
                <td>${safe(c.fullName)}</td>
                <td>${safe(c.departmentName || 'Chưa phân công')}</td>
                <td><small class="subtle">${safe(c.startDate || '—')}</small></td>
                <td style="text-align:center;">${tenureBadge}</td>
                <td class="number">${baseSalaryFormatted}</td>
                <td class="number">
                    <input type="number" min="0" step="10000" class="bonus-table-input"
                           data-emp-bonus="${c.employeeId}"
                           value="${c.finalBonus}"
                           title="Nhập số tiền thưởng (VND)">
                </td>
                <td>
                    <input type="text" maxlength="300" class="bonus-note-input"
                           data-emp-note="${c.employeeId}"
                           value="${safe(c.finalNote)}"
                           placeholder="Ghi chú thưởng...">
                </td>
            </tr>`;
        }).join('');
    }

    // Wiring up Form interactions
    const form = $('#bonusCampaignForm');
    if (form) {
        form.addEventListener('input', e => {
            if (e.target.matches('.bonus-table-input')) {
                const empId = parseInt(e.target.dataset.empBonus, 10);
                const candidate = state.candidates.find(c => c.employeeId === empId);
                if (candidate) {
                    candidate.finalBonus = Math.max(0, parseFloat(e.target.value) || 0);
                    candidate.isManuallyEdited = true;
                    updateMetrics();
                }
                return;
            }
            if (e.target.matches('.bonus-note-input')) {
                const empId = parseInt(e.target.dataset.empNote, 10);
                const candidate = state.candidates.find(c => c.employeeId === empId);
                if (candidate) {
                    candidate.finalNote = e.target.value;
                    candidate.isManuallyEdited = true;
                }
                return;
            }

            // Top form parameters change
            if (['campaignName', 'applyMode', 'calcType', 'monthsMultiplier', 'fixedAmount'].includes(e.target.name)) {
                recalculateAll(false);
            }
        });

        form.addEventListener('change', e => {
            if (['applyMode', 'calcType', 'prorateByTenure'].includes(e.target.name)) {
                recalculateAll(false);
            }
        });
    }

    // Preset buttons
    document.querySelectorAll('.preset-badge-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.preset-badge-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            const multiplierInput = $('#monthsMultiplier');
            if (multiplierInput) {
                multiplierInput.value = btn.dataset.preset;
                recalculateAll(false);
            }
        });
    });

    // Reset button
    $('#resetToFormulaBtn')?.addEventListener('click', () => {
        recalculateAll(true);
        feedback('Đã đặt lại mức thưởng của toàn bộ nhân viên theo công thức.');
    });

    // Search filter
    $('#candidateSearch')?.addEventListener('input', e => {
        state.searchQuery = e.target.value;
        renderRows();
    });

    const confirmDialog = $('#bonusConfirmDialog');
    const confirmForm = $('#bonusConfirmForm');

    function setConfirmError(message) {
        const box = confirmDialog?.querySelector('.dialog-error');
        if (!box) return;
        box.textContent = message || '';
        box.hidden = !message;
    }

    function openConfirmDialog() {
        if (!confirmDialog) return;
        const total = state.candidates.reduce((sum, c) => sum + (Number(c.finalBonus) || 0), 0);
        const count = state.candidates.filter(c => Number(c.finalBonus) > 0).length;
        const config = getConfig();

        $('#bonusConfirmText').textContent = `Xác nhận áp dụng đợt thưởng cho ${count} nhân viên? Bảng lương kỳ sẽ được cập nhật và đưa về trạng thái chờ tính toán.`;
        $('#bonusConfirmSummary').innerHTML =
            `Đợt thưởng: <strong>${safe(config.campaignName)}</strong><br>` +
            `Số nhân viên nhận thưởng: <strong>${count} nhân viên</strong><br>` +
            `Tổng ngân sách thưởng: <strong>${money(total)}</strong>`;
        setConfirmError('');
        if (typeof confirmDialog.showModal === 'function') {
            confirmDialog.showModal();
        }
    }

    confirmDialog?.querySelectorAll('[data-dismiss]').forEach(btn => {
        btn.addEventListener('click', () => {
            if (!state.busy) confirmDialog.close();
        });
    });

    confirmDialog?.addEventListener('cancel', e => {
        if (state.busy) e.preventDefault();
    });

    confirmDialog?.addEventListener('click', e => {
        if (e.target === confirmDialog && !state.busy) {
            confirmDialog.close();
        }
    });

    // Submit form: Open confirmation popup
    form?.addEventListener('submit', e => {
        e.preventDefault();
        if (state.busy) return;
        openConfirmDialog();
    });

    // Confirm popup submit: Apply bonus campaign
    confirmForm?.addEventListener('submit', async e => {
        e.preventDefault();
        if (state.busy) return;

        state.busy = true;
        setConfirmError('');
        const submitBtn = $('#applyBonusBtn');
        const confirmSubmitBtn = $('#bonusConfirmSubmit');
        const saveHint = $('#saveHint');
        const originalConfirmLabel = confirmSubmitBtn?.textContent || 'Xác nhận áp dụng';
        if (submitBtn) submitBtn.disabled = true;
        if (confirmSubmitBtn) {
            confirmSubmitBtn.disabled = true;
            confirmSubmitBtn.textContent = 'Đang áp dụng…';
        }
        if (saveHint) saveHint.textContent = 'Đang lưu đợt thưởng vào hệ thống…';

        try {
            const formData = new FormData();
            formData.set('periodId', state.periodId);

            // Append each employee's bonus and note
            state.candidates.forEach(c => {
                formData.append('employeeId', String(c.employeeId));
                formData.append('bonus', String(c.finalBonus || 0));
                formData.append('note', c.finalNote || '');
            });

            const res = await fetch(context + '/payroll/api/bonus-campaign', {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'X-CSRF-Token': csrf
                },
                body: formData
            });

            const payload = await res.json();
            if (!res.ok || !payload.success) {
                throw new Error(payload.message || 'Không thể lưu đợt thưởng. Vui lòng thử lại.');
            }

            confirmDialog?.close();
            feedback('Đã áp dụng đợt thưởng thành công! Đang chuyển hướng về kỳ lương…');
            setTimeout(() => {
                location.href = context + '/payroll?periodId=' + encodeURIComponent(state.periodId);
            }, 1000);

        } catch (error) {
            setConfirmError(error.message);
            feedback(error.message, true);
            state.busy = false;
            if (submitBtn) submitBtn.disabled = false;
            if (confirmSubmitBtn) {
                confirmSubmitBtn.disabled = false;
                confirmSubmitBtn.textContent = originalConfirmLabel;
            }
            if (saveHint) saveHint.textContent = 'Lưu thất bại. Hãy kiểm tra lại.';
        }
    });

    // Load initial
    initCandidates();
    recalculateAll(true);
})();

