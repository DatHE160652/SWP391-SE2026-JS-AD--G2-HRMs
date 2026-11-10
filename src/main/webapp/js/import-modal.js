/**
 * Reusable Data Importer JavaScript Component for HRMs SME Platform
 * Usage:
 *    DataImporter.open('EMPLOYEE', {
 *        onSuccess: function(result) {
 *            alert(result.message);
 *            location.reload();
 *        }
 *    });
 */
(function(window) {
    'use strict';

    let currentType = 'EMPLOYEE';
    let currentOptions = {};
    let selectedFile = null;
    let isProcessing = false;

    const typeNamesVi = {
        'DEPARTMENT': 'Phòng ban',
        'POSITION': 'Chức danh',
        'EMPLOYEE': 'Hồ sơ nhân sự',
        'ATTENDANCE': 'Điểm danh chấm công',
        'CONTRACT': 'Hợp đồng lao động'
    };

    function getContextPath() {
        const path = window.location.pathname;
        const secondSlash = path.indexOf('/', 1);
        return secondSlash > 0 ? path.substring(0, secondSlash) : '';
    }

    function open(type, options) {
        currentType = (type || 'EMPLOYEE').toUpperCase();
        currentOptions = options || {};
        selectedFile = null;
        isProcessing = false;

        const modal = document.getElementById('modalDataImport');
        if (!modal) {
            console.error('DataImporter: modalDataImport element not found in DOM.');
            return;
        }

        // Setup Title & Template download URL
        const titleText = document.getElementById('importModalTitleText');
        if (titleText) {
            titleText.textContent = `Nhập dữ liệu Excel - ${typeNamesVi[currentType] || currentType}`;
        }

        const btnDownload = document.getElementById('btnDownloadImportTemplate');
        if (btnDownload) {
            const ctx = getContextPath();
            btnDownload.href = `${ctx}/data/import/template?type=${currentType}`;
            btnDownload.setAttribute('download', `mau_nhap_${currentType.toLowerCase()}.xlsx`);
        }

        // Reset UI states
        resetUI();

        // Bind events if not already done
        bindEvents();

        modal.style.display = 'flex';
    }

    function close() {
        const modal = document.getElementById('modalDataImport');
        if (modal) {
            modal.style.display = 'none';
        }
        resetUI();
    }

    function resetUI() {
        selectedFile = null;
        const fileInput = document.getElementById('inputImportFile');
        if (fileInput) fileInput.value = '';

        const fileNameEl = document.getElementById('importSelectedFileName');
        if (fileNameEl) {
            fileNameEl.style.display = 'none';
            fileNameEl.textContent = '';
        }

        const alertEl = document.getElementById('importGlobalAlert');
        if (alertEl) {
            alertEl.style.display = 'none';
            alertEl.className = 'import-alert';
            alertEl.textContent = '';
        }

        const previewSection = document.getElementById('importPreviewSection');
        if (previewSection) previewSection.style.display = 'none';

        const spinner = document.getElementById('importLoadingSpinner');
        if (spinner) spinner.style.display = 'none';

        const btnConfirm = document.getElementById('btnConfirmImport');
        if (btnConfirm) btnConfirm.disabled = true;
    }

    function showAlert(message, type) {
        const alertEl = document.getElementById('importGlobalAlert');
        if (!alertEl) return;
        alertEl.className = `import-alert ${type || 'error'}`;
        alertEl.textContent = message;
        alertEl.style.display = 'block';
    }

    function bindEvents() {
        const btnClose = document.getElementById('btnCloseImportModal');
        const btnCancel = document.getElementById('btnCancelImportModal');
        const dropZone = document.getElementById('importDropZone');
        const fileInput = document.getElementById('inputImportFile');
        const btnBrowse = document.getElementById('btnBrowseFile');
        const btnConfirm = document.getElementById('btnConfirmImport');

        if (btnClose) btnClose.onclick = close;
        if (btnCancel) btnCancel.onclick = close;

        if (btnBrowse && fileInput) {
            btnBrowse.onclick = (e) => {
                e.stopPropagation();
                fileInput.click();
            };
        }

        if (dropZone && fileInput) {
            dropZone.onclick = () => fileInput.click();

            dropZone.ondragover = (e) => {
                e.preventDefault();
                dropZone.classList.add('dragover');
            };
            dropZone.ondragleave = () => dropZone.classList.remove('dragover');
            dropZone.ondrop = (e) => {
                e.preventDefault();
                dropZone.classList.remove('dragover');
                if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files.length > 0) {
                    handleFileSelected(e.dataTransfer.files[0]);
                }
            };

            fileInput.onchange = () => {
                if (fileInput.files && fileInput.files.length > 0) {
                    handleFileSelected(fileInput.files[0]);
                }
            };
        }

        if (btnConfirm) {
            btnConfirm.onclick = executeImport;
        }
    }

    async function handleFileSelected(file) {
        if (!file) return;
        const name = file.name.toLowerCase();
        if (!name.endsWith('.xlsx') && !name.endsWith('.xls')) {
            showAlert('Vui lòng chỉ chọn tệp định dạng Excel (.xlsx hoặc .xls).', 'error');
            return;
        }

        selectedFile = file;

        const fileNameEl = document.getElementById('importSelectedFileName');
        if (fileNameEl) {
            fileNameEl.textContent = `📁 Tệp đã chọn: ${file.name} (${(file.size / 1024).toFixed(1)} KB)`;
            fileNameEl.style.display = 'inline-block';
        }

        await runPreview();
    }

    async function runPreview() {
        if (!selectedFile) return;

        const spinner = document.getElementById('importLoadingSpinner');
        const previewSection = document.getElementById('importPreviewSection');
        const btnConfirm = document.getElementById('btnConfirmImport');
        const alertEl = document.getElementById('importGlobalAlert');

        if (alertEl) alertEl.style.display = 'none';
        if (spinner) spinner.style.display = 'block';
        if (previewSection) previewSection.style.display = 'none';
        if (btnConfirm) btnConfirm.disabled = true;

        const formData = new FormData();
        formData.append('file', selectedFile);
        formData.append('type', currentType);

        try {
            const ctx = getContextPath();
            const response = await fetch(`${ctx}/data/import/preview?type=${currentType}`, {
                method: 'POST',
                body: formData
            });

            const data = await response.json();
            if (spinner) spinner.style.display = 'none';

            if (data.type) {
                currentType = data.type;
                const titleText = document.getElementById('importModalTitleText');
                if (titleText) {
                    titleText.textContent = `Nhập dữ liệu Excel - ${typeNamesVi[currentType] || currentType}`;
                }
            }

            if (!response.ok || (data.globalErrors && data.globalErrors.length > 0)) {
                const msg = (data.globalErrors && data.globalErrors.length > 0)
                    ? data.globalErrors.join('; ')
                    : (data.message || 'Lỗi khi kiểm tra tệp Excel.');
                showAlert(msg, 'error');
                return;
            }

            renderPreviewTable(data);

            if (previewSection) previewSection.style.display = 'block';
            if (btnConfirm) {
                btnConfirm.disabled = !data.canProceed;
            }

            if (data.errorRows > 0) {
                showAlert(`Tệp có ${data.errorRows} dòng dữ liệu lỗi. Vui lòng xem chi tiết cột lỗi bên dưới hoặc sửa lại tệp trước khi xác nhận.`, 'error');
            } else if (data.validRows > 0) {
                showAlert(`Tất cả ${data.validRows} dòng dữ liệu đều hợp lệ! Bạn có thể nhấn 'Xác nhận Nhập dữ liệu' ngay.`, 'success');
            }
        } catch (err) {
            if (spinner) spinner.style.display = 'none';
            showAlert('Lỗi kết nối máy chủ: ' + err.message, 'error');
        }
    }

    function renderPreviewTable(data) {
        const badgeTotal = document.getElementById('badgeTotalRows');
        const badgeValid = document.getElementById('badgeValidRows');
        const badgeError = document.getElementById('badgeErrorRows');

        if (badgeTotal) badgeTotal.textContent = `Tổng: ${data.totalRows} dòng`;
        if (badgeValid) badgeValid.textContent = `✓ ${data.validRows} hợp lệ`;
        if (badgeError) badgeError.textContent = `✗ ${data.errorRows} lỗi`;

        const thead = document.getElementById('importTableHead');
        const tbody = document.getElementById('importTableBody');
        if (!thead || !tbody) return;

        // Render Head
        let headHtml = '<tr><th style="width:40px; text-align:center;">STT</th><th style="width:90px; text-align:center;">Trạng thái</th>';
        (data.columnHeaders || []).forEach(h => {
            headHtml += `<th>${escapeHtml(h)}</th>`;
        });
        headHtml += '<th>Ghi chú kiểm tra</th></tr>';
        thead.innerHTML = headHtml;

        // Render Body
        let bodyHtml = '';
        (data.previewRows || []).forEach(row => {
            const trClass = row.valid ? '' : 'row-error';
            const statusBadge = row.valid
                ? '<span style="color:#027a48; font-weight:700;">✓ Chuẩn</span>'
                : '<span style="color:#b42318; font-weight:700;">✗ Lỗi</span>';

            bodyHtml += `<tr class="${trClass}">`;
            bodyHtml += `<td style="text-align:center;">${row.rowNumber}</td>`;
            bodyHtml += `<td style="text-align:center;">${statusBadge}</td>`;

            (data.columnHeaders || []).forEach(h => {
                const cellVal = (row.data && row.data[h]) ? row.data[h] : '';
                bodyHtml += `<td>${escapeHtml(cellVal)}</td>`;
            });

            const errNote = row.errorMessage ? `<span style="color:#b42318;">${escapeHtml(row.errorMessage)}</span>` : '<span style="color:#027a48;">Sẵn sàng nhập</span>';
            bodyHtml += `<td>${errNote}</td>`;
            bodyHtml += '</tr>';
        });

        tbody.innerHTML = bodyHtml;
    }

    async function executeImport() {
        if (!selectedFile || isProcessing) return;

        const btnConfirm = document.getElementById('btnConfirmImport');
        if (btnConfirm) {
            btnConfirm.disabled = true;
            btnConfirm.innerHTML = '<span>Đang nhập dữ liệu...</span>';
        }
        isProcessing = true;

        const formData = new FormData();
        formData.append('file', selectedFile);
        formData.append('type', currentType);

        try {
            const ctx = getContextPath();
            const response = await fetch(`${ctx}/data/import/execute?type=${currentType}`, {
                method: 'POST',
                body: formData
            });

            const data = await response.json();
            isProcessing = false;

            if (response.ok && data.success) {
                showAlert(data.message || 'Nhập dữ liệu thành công!', 'success');
                if (btnConfirm) {
                    btnConfirm.innerHTML = '<span>✓ Đã nhập xong</span>';
                }
                setTimeout(() => {
                    close();
                    if (typeof currentOptions.onSuccess === 'function') {
                        currentOptions.onSuccess(data);
                    } else {
                        window.location.reload();
                    }
                }, 1200);
            } else {
                if (btnConfirm) {
                    btnConfirm.disabled = false;
                    btnConfirm.innerHTML = '<span>Xác nhận Nhập dữ liệu</span>';
                }
                const msg = data.message || 'Lỗi khi nhập dữ liệu vào cơ sở dữ liệu.';
                showAlert(msg, 'error');
            }
        } catch (err) {
            isProcessing = false;
            if (btnConfirm) {
                btnConfirm.disabled = false;
                btnConfirm.innerHTML = '<span>Xác nhận Nhập dữ liệu</span>';
            }
            showAlert('Lỗi kết nối máy chủ: ' + err.message, 'error');
        }
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    window.DataImporter = {
        open: open,
        close: close
    };

})(window);

