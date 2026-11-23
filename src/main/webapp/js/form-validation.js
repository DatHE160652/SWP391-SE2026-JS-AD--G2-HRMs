window.FormValidation = (() => {
    const records = new WeakMap();
    const initializedForms = new WeakSet();
    // Pages that do not load the i18n runtime keep the Vietnamese text as-is.
    const i18n = window.HrmI18n;
    const t = text => i18n ? i18n.t(text) : text;
    const fragment = text => i18n ? i18n.fragment(text) : text;
    let errorSequence = 0;
    const labels = {
        fullName: t('họ và tên'), phone: t('số điện thoại'), email: 'email',
        employeeCode: t('mã nhân viên'), identityNumber: t('mã định danh (CCCD)'),
        departmentCode: t('mã phòng ban'), departmentName: t('tên phòng ban'),
        positionTitle: t('tên chức danh'), departmentId: t('phòng ban'), managerId: t('quản lý'),
        documentName: t('tên tài liệu'), file: t('tài liệu'), dateOfBirth: t('ngày sinh'),
        gender: t('giới tính'), status: t('trạng thái'), tenantDomain: 'workspace',
        tenantEmail: 'email', tenantPassword: t('mật khẩu'), identifier: t('tên đăng nhập / email'),
        password: t('mật khẩu'), newPassword: t('mật khẩu mới'), confirmPassword: t('mật khẩu xác nhận'),
        companyName: t('tên công ty'), region: t('khu vực'), reason: t('lý do'),
        startDate: t('ngày bắt đầu'), endDate: t('ngày kết thúc'), dates: t('ngày nghỉ'),
        quotaDays: t('số ngày phép'), maxDaysPerRequest: t('giới hạn mỗi đơn'),
        title: t('tiêu đề'), content: t('nội dung'),
        grossSalary: t("lương trước khấu trừ"), travelAllowance: t('phụ cấp đi lại'),
        positionId: t('chức danh'), employeeId: t('nhân viên'), contractCode: t('mã hợp đồng'),
        contractType: t('loại hợp đồng'), effectiveDate: t('ngày hiệu lực'),
        startsAt: t('thời gian bắt đầu'), endsAt: t('thời gian kết thúc')
    };

    function fields(root) {
        return Array.from(root.querySelectorAll('input, select, textarea'))
            .filter(field => !['hidden', 'button', 'submit', 'reset', 'image'].includes(field.type));
    }

    function group(field) {
        if (field.type !== 'radio' || !field.form || !field.name) {
            return [field];
        }
        return fields(field.form).filter(candidate => candidate.name === field.name && candidate.type === 'radio');
    }

    function ensure(field) {
        if (records.has(field)) {
            return records.get(field);
        }
        const members = group(field);
        const anchor = field.closest('.radio-row, .input-icon, .domain-input') || field;
        let error = anchor.nextElementSibling;
        if (!error || !error.classList.contains('field-error')) {
            error = document.createElement('small');
            error.className = 'field-error';
            anchor.insertAdjacentElement('afterend', error);
        }
        error.id = error.id || `validation-error-${++errorSequence}`;
        error.setAttribute('aria-live', 'polite');
        members.forEach(member => {
            records.set(member, error);
            const describedBy = new Set((member.getAttribute('aria-describedby') || '').split(/\s+/).filter(Boolean));
            describedBy.add(error.id);
            member.setAttribute('aria-describedby', Array.from(describedBy).join(' '));
            if (error.textContent.trim()) {
                member.setAttribute('aria-invalid', 'true');
            }
        });
        return error;
    }

    function resolve(root, name) {
        if (name instanceof HTMLElement) {
            return name;
        }
        return fields(root).find(field => field.name === name || field.id === name);
    }

    function setError(root, name, message) {
        const field = resolve(root, name);
        if (!field) {
            return false;
        }
        ensure(field).textContent = message || '';
        group(field).forEach(member => member.setAttribute('aria-invalid', message ? 'true' : 'false'));
        return true;
    }

    function messageFor(field) {
        if (field.disabled || field.readOnly) {
            return '';
        }
        const members = group(field);
        const value = field.value.trim();
        const labelNode = field.labels && field.labels[0] && field.labels[0].cloneNode(true);
        if (labelNode) labelNode.querySelectorAll('.field-error, input, select, textarea').forEach(node => node.remove());
        const label = field.dataset.validationLabel?.trim() || labels[field.name] || labels[field.id]
            || (labelNode && labelNode.textContent.replace(/\*/g, '').trim())
            || t('thông tin');
        if (field.validity.badInput) {
            return `${label}${fragment(": giá trị không hợp lệ.")}`;
        }
        const missing = field.type === 'radio' ? !members.some(member => member.checked)
            : field.type === 'checkbox' ? !field.checked
            : field.type === 'file' ? !field.files.length : !value;
        if (members.some(member => member.required) && missing) {
            const selection = field.tagName === 'SELECT' || ['radio', 'checkbox', 'file'].includes(field.type);
            return `${fragment("Vui lòng ")}${selection ? t('chọn') : t('nhập')} ${label}.`;
        }
        if (missing) {
            return '';
        }
        if (field.name === 'fullName' && field.form?.dataset.validationOrder && !/^[\p{L}\p{M}\s]+$/u.test(value)) {
            return t('Họ tên chỉ được chứa chữ cái và khoảng trắng.');
        }
        if (field.type === 'email' && (field.validity.typeMismatch || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value))) {
            return field.form?.id === 'lc-employee-form'
                ? t('Vui lòng nhập địa chỉ email công ty hợp lệ.')
                : t('Vui lòng nhập địa chỉ email hợp lệ.');
        }
        if (field.name === 'identityNumber' && !/^[0-9]{12}$/.test(value)) {
            return t('Mã định danh (CCCD) phải gồm đúng 12 chữ số.');
        }
        if (field.name === 'phone' && !/^0[0-9]{9}$/.test(value)) {
            return t('Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.');
        }
        if (field.name !== 'phone' && field.validity.patternMismatch) {
            return `${label}${fragment(": không đúng định dạng yêu cầu.")}`;
        }
        if (field.maxLength > 0 && value.length > field.maxLength) {
            return `${label}${fragment(": tối đa ")}${field.maxLength}${fragment(" ký tự.")}`;
        }
        if (field.minLength > 0 && value.length < field.minLength) {
            return `${label}${fragment(": tối thiểu ")}${field.minLength}${fragment(" ký tự.")}`;
        }
        if (field.validity.rangeUnderflow) {
            return `${label}${fragment(": giá trị phải từ ")}${field.min}${fragment(" trở lên.")}`;
        }
        if (field.validity.rangeOverflow) {
            return `${label}${fragment(": giá trị không được vượt quá ")}${field.max}.`;
        }
        if (field.validity.stepMismatch || field.validity.typeMismatch) {
            return `${label}${fragment(": giá trị không hợp lệ.")}`;
        }
        const form = field.form;
        if (field.name === 'confirmPassword' && form && form.elements.namedItem('newPassword')
            && field.value !== form.elements.namedItem('newPassword').value) {
            return t('Mật khẩu xác nhận không khớp.');
        }
        if (field.name === 'newPassword' && field.value === '1') {
            return t('Vui lòng chọn mật khẩu khác mật khẩu tạm.');
        }
        const startDate = form && form.elements.namedItem('startDate');
        if (field.name === 'endDate' && startDate && startDate.value && value < startDate.value) {
            return t('Ngày kết thúc không được trước ngày bắt đầu.');
        }
        const startsAt = form && form.elements.namedItem('startsAt');
        if (field.name === 'endsAt' && startsAt && startsAt.value && value <= startsAt.value) {
            return t('Thời gian kết thúc phải sau thời gian bắt đầu.');
        }
        if (field.type === 'file') {
            const extensions = field.accept.split(',').map(extension => extension.trim().toLowerCase()).filter(extension => extension.startsWith('.'));
            for (const file of field.files) {
                if (file.size > 10 * 1024 * 1024) {
                    return t('Dung lượng tài liệu không được vượt quá 10 MB.');
                }
                if (extensions.length && !extensions.some(extension => file.name.toLowerCase().endsWith(extension))) {
                    return t('Định dạng tài liệu không được hỗ trợ.');
                }
            }
        }
        return '';
    }

    function validateField(field) {
        const message = messageFor(field);
        setError(field.form || document, field, message);
        return !message;
    }

    function focusInvalid(field) {
        const panel = field.closest('[data-form-panel]');
        if (panel && panel.hidden && field.form) {
            const tab = Array.from(field.form.querySelectorAll('[data-form-tab]'))
                .find(button => button.dataset.formTab === panel.dataset.formPanel);
            if (tab) tab.click();
        }
        field.focus();
    }

    function validate(root) {
        clear(root);
        const order = root.dataset?.validationOrder?.split(',');
        const allFields = fields(root);
        const ordered = order ? order.map(name => allFields.find(field => field.name === name)).filter(Boolean) : [];
        const alert = order && root.querySelector('[role="alert"]');
        if (alert) alert.textContent = '';
        if (ordered.length && ordered.every(field => !field.value.trim() && !field.validity.badInput)) {
            if (alert) alert.textContent = t('Vui lòng nhập đầy đủ thông tin bắt buộc.');
            focusInvalid(ordered[0]);
            return false;
        }
        for (const field of [...ordered, ...allFields.filter(field => !ordered.includes(field))]) {
            if (!validateField(field)) {
                focusInvalid(field);
                return false;
            }
        }
        return true;
    }

    function clear(root) {
        fields(root).forEach(field => setError(root, field, ''));
    }

    function init(form) {
        form.noValidate = true;
        fields(form).forEach(ensure);
        if (!initializedForms.has(form)) {
            initializedForms.add(form);
            form.addEventListener('reset', () => setTimeout(() => clear(form), 0));
        }
    }

    function showErrors(root, failure) {
        const errors = new Map();
        Object.entries(failure.fieldErrors || {}).forEach(([name, message]) => {
            const field = resolve(root, name);
            if (field && message) errors.set(field, message);
        });
        if (!errors.size) return false;
        clear(root);
        for (const field of fields(root)) {
            if (field.disabled || field.readOnly) continue;
            // Check earlier inputs before displaying a server error further down.
            const message = messageFor(field) || errors.get(field);
            if (message) {
                setError(root, field, message);
                focusInvalid(field);
                return true;
            }
        }
        return false;
    }

    function responseError(payload) {
        const error = new Error(payload.message || t('Không thể hoàn tất thao tác.'));
        error.fieldErrors = payload.fieldErrors;
        return error;
    }

    document.addEventListener('submit', event => {
        init(event.target);
        if (!validate(event.target)) {
            event.preventDefault();
            event.stopImmediatePropagation();
        }
    }, true);
    document.addEventListener('invalid', event => {
        event.preventDefault();
        validateField(event.target);
    }, true);
    document.addEventListener('focusout', event => {
        if (event.target.matches('input:not([type="hidden"]), select, textarea')) {
            validateField(event.target);
        }
    });
    document.addEventListener('input', event => {
        if (records.has(event.target) && records.get(event.target).textContent) {
            validateField(event.target);
        }
    });
    document.addEventListener('change', event => {
        if (event.target.matches('input:not([type="hidden"]), select, textarea')) {
            validateField(event.target);
            const dependent = { startDate: 'endDate', startsAt: 'endsAt', newPassword: 'confirmPassword' }[event.target.name];
            const target = dependent && event.target.form && event.target.form.elements.namedItem(dependent);
            if (target && target.value) validateField(target);
        }
    });
    document.addEventListener('DOMContentLoaded', () => document.querySelectorAll('form').forEach(init));
    return { init, validate, validateField, setError, clear, showErrors, responseError };
})();
