window.FormValidation = (() => {
    const records = new WeakMap();
    const initializedForms = new WeakSet();
    let errorSequence = 0;
    const labels = {
        fullName: HrmI18n.t('họ và tên'), phone: HrmI18n.t('số điện thoại'), email: 'email',
        employeeCode: HrmI18n.t('mã nhân viên'), identityNumber: HrmI18n.t('mã định danh (CCCD)'),
        departmentCode: HrmI18n.t('mã phòng ban'), departmentName: HrmI18n.t('tên phòng ban'),
        positionTitle: HrmI18n.t('tên chức danh'), departmentId: HrmI18n.t('phòng ban'), managerId: HrmI18n.t('quản lý'),
        documentName: HrmI18n.t('tên tài liệu'), file: HrmI18n.t('tài liệu'), dateOfBirth: HrmI18n.t('ngày sinh'),
        gender: HrmI18n.t('giới tính'), status: HrmI18n.t('trạng thái'), tenantDomain: 'workspace',
        tenantEmail: 'email', tenantPassword: HrmI18n.t('mật khẩu'), identifier: HrmI18n.t('tên đăng nhập / email'),
        password: HrmI18n.t('mật khẩu'), newPassword: HrmI18n.t('mật khẩu mới'), confirmPassword: HrmI18n.t('mật khẩu xác nhận'),
        companyName: HrmI18n.t('tên công ty'), region: HrmI18n.t('khu vực'), reason: HrmI18n.t('lý do'),
        startDate: HrmI18n.t('ngày bắt đầu'), endDate: HrmI18n.t('ngày kết thúc'), dates: HrmI18n.t('ngày nghỉ'),
        quotaDays: HrmI18n.t('số ngày phép'), maxDaysPerRequest: HrmI18n.t('giới hạn mỗi đơn'),
        title: HrmI18n.t('tiêu đề'), content: HrmI18n.t('nội dung'),
        grossSalary: HrmI18n.t("lương trước khấu trừ"), travelAllowance: HrmI18n.t('phụ cấp đi lại'),
        positionId: HrmI18n.t('chức danh'), employeeId: HrmI18n.t('nhân viên'), contractCode: HrmI18n.t('mã hợp đồng'),
        contractType: HrmI18n.t('loại hợp đồng'), effectiveDate: HrmI18n.t('ngày hiệu lực'),
        startsAt: HrmI18n.t('thời gian bắt đầu'), endsAt: HrmI18n.t('thời gian kết thúc')
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
            || HrmI18n.t('thông tin');
        if (field.validity.badInput) {
            return `${label}${HrmI18n.fragment(": giá trị không hợp lệ.")}`;
        }
        const missing = field.type === 'radio' ? !members.some(member => member.checked)
            : field.type === 'checkbox' ? !field.checked
            : field.type === 'file' ? !field.files.length : !value;
        if (members.some(member => member.required) && missing) {
            const selection = field.tagName === 'SELECT' || ['radio', 'checkbox', 'file'].includes(field.type);
            return `${HrmI18n.fragment("Vui lòng ")}${selection ? HrmI18n.t('chọn') : HrmI18n.t('nhập')} ${label}.`;
        }
        if (missing) {
            return '';
        }
        if (field.type === 'email' && (field.validity.typeMismatch || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value))) {
            return HrmI18n.t('Vui lòng nhập địa chỉ email hợp lệ.');
        }
        if (field.name === 'identityNumber' && !/^[0-9]{12}$/.test(value)) {
            return HrmI18n.t('Mã định danh (CCCD) phải gồm đúng 12 chữ số.');
        }
        if (field.name === 'phone' && !/^0[0-9]{9}$/.test(value)) {
            return HrmI18n.t('Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.');
        }
        if (field.name !== 'phone' && field.validity.patternMismatch) {
            return `${label}${HrmI18n.fragment(": không đúng định dạng yêu cầu.")}`;
        }
        if (field.maxLength > 0 && value.length > field.maxLength) {
            return `${label}${HrmI18n.fragment(": tối đa ")}${field.maxLength}${HrmI18n.fragment(" ký tự.")}`;
        }
        if (field.minLength > 0 && value.length < field.minLength) {
            return `${label}${HrmI18n.fragment(": tối thiểu ")}${field.minLength}${HrmI18n.fragment(" ký tự.")}`;
        }
        if (field.validity.rangeUnderflow) {
            return `${label}${HrmI18n.fragment(": giá trị phải từ ")}${field.min}${HrmI18n.fragment(" trở lên.")}`;
        }
        if (field.validity.rangeOverflow) {
            return `${label}${HrmI18n.fragment(": giá trị không được vượt quá ")}${field.max}.`;
        }
        if (field.validity.stepMismatch || field.validity.typeMismatch) {
            return `${label}${HrmI18n.fragment(": giá trị không hợp lệ.")}`;
        }
        const form = field.form;
        if (field.name === 'confirmPassword' && form && form.elements.namedItem('newPassword')
            && field.value !== form.elements.namedItem('newPassword').value) {
            return HrmI18n.t('Mật khẩu xác nhận không khớp.');
        }
        if (field.name === 'newPassword' && field.value === '1') {
            return HrmI18n.t('Vui lòng chọn mật khẩu khác mật khẩu tạm.');
        }
        const startDate = form && form.elements.namedItem('startDate');
        if (field.name === 'endDate' && startDate && startDate.value && value < startDate.value) {
            return HrmI18n.t('Ngày kết thúc không được trước ngày bắt đầu.');
        }
        const startsAt = form && form.elements.namedItem('startsAt');
        if (field.name === 'endsAt' && startsAt && startsAt.value && value <= startsAt.value) {
            return HrmI18n.t('Thời gian kết thúc phải sau thời gian bắt đầu.');
        }
        if (field.type === 'file') {
            const extensions = field.accept.split(',').map(extension => extension.trim().toLowerCase()).filter(extension => extension.startsWith('.'));
            for (const file of field.files) {
                if (file.size > 10 * 1024 * 1024) {
                    return HrmI18n.t('Dung lượng tài liệu không được vượt quá 10 MB.');
                }
                if (extensions.length && !extensions.some(extension => file.name.toLowerCase().endsWith(extension))) {
                    return HrmI18n.t('Định dạng tài liệu không được hỗ trợ.');
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
        // DOM order follows the form from top to bottom (left to right per row).
        for (const field of fields(root)) {
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
        const error = new Error(payload.message || HrmI18n.t('Không thể hoàn tất thao tác.'));
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
