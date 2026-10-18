window.FormValidation = (() => {
    const records = new WeakMap();
    const initializedForms = new WeakSet();
    let errorSequence = 0;
    const labels = {
        fullName: 'họ và tên', phone: 'số điện thoại', email: 'email',
        employeeCode: 'mã nhân viên', identityNumber: 'mã định danh (CCCD)',
        departmentCode: 'mã phòng ban', departmentName: 'tên phòng ban',
        positionTitle: 'tên chức danh', departmentId: 'phòng ban', managerId: 'quản lý',
        documentName: 'tên tài liệu', file: 'tài liệu', dateOfBirth: 'ngày sinh',
        gender: 'giới tính', status: 'trạng thái', tenantDomain: 'workspace',
        tenantEmail: 'email', tenantPassword: 'mật khẩu', identifier: 'tên đăng nhập / email',
        password: 'mật khẩu', newPassword: 'mật khẩu mới', confirmPassword: 'mật khẩu xác nhận',
        companyName: 'tên công ty', region: 'khu vực', reason: 'lý do nghỉ phép',
        startDate: 'ngày bắt đầu', endDate: 'ngày kết thúc', leaveTypeId: 'loại nghỉ phép',
        typeCode: 'mã loại nghỉ phép', typeName: 'tên loại nghỉ phép',
        defaultDaysPerYear: 'số ngày phép', title: 'tiêu đề', content: 'nội dung'
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
        const label = labels[field.name] || labels[field.id]
            || (field.labels && field.labels[0] && field.labels[0].textContent.replace(/\*/g, '').trim())
            || 'thông tin';
        if (field.validity.badInput) {
            return `${label}: giá trị không hợp lệ.`;
        }
        const missing = field.type === 'radio' ? !members.some(member => member.checked)
            : field.type === 'checkbox' ? !field.checked
            : field.type === 'file' ? !field.files.length : !value;
        if (members.some(member => member.required) && missing) {
            const selection = field.tagName === 'SELECT' || ['radio', 'checkbox', 'file'].includes(field.type);
            return `Vui lòng ${selection ? 'chọn' : 'nhập'} ${label}.`;
        }
        if (missing) {
            return '';
        }
        if (field.type === 'email' && field.validity.typeMismatch) {
            return 'Vui lòng nhập địa chỉ email hợp lệ.';
        }
        if (field.name === 'identityNumber' && !/^[0-9]{12}$/.test(value)) {
            return 'Mã định danh (CCCD) phải gồm đúng 12 chữ số.';
        }
        if (field.validity.patternMismatch) {
            return field.name === 'phone' ? 'Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.'
                : `${label}: không đúng định dạng yêu cầu.`;
        }
        if (field.maxLength > 0 && value.length > field.maxLength) {
            return `${label}: tối đa ${field.maxLength} ký tự.`;
        }
        if (field.minLength > 0 && value.length < field.minLength) {
            return `${label}: tối thiểu ${field.minLength} ký tự.`;
        }
        if (field.validity.rangeUnderflow) {
            return `${label}: giá trị phải từ ${field.min} trở lên.`;
        }
        if (field.validity.rangeOverflow) {
            return `${label}: giá trị không được vượt quá ${field.max}.`;
        }
        if (field.validity.stepMismatch || field.validity.typeMismatch) {
            return `${label}: giá trị không hợp lệ.`;
        }
        const form = field.form;
        if (field.name === 'confirmPassword' && form && form.elements.namedItem('newPassword')
            && field.value !== form.elements.namedItem('newPassword').value) {
            return 'Mật khẩu xác nhận không khớp.';
        }
        if (field.name === 'newPassword' && field.value === '1') {
            return 'Vui lòng chọn mật khẩu khác mật khẩu tạm.';
        }
        const startDate = form && form.elements.namedItem('startDate');
        if (field.name === 'endDate' && startDate && startDate.value && value < startDate.value) {
            return 'Ngày kết thúc không được trước ngày bắt đầu.';
        }
        if (field.type === 'file') {
            const extensions = field.accept.split(',').map(extension => extension.trim().toLowerCase()).filter(extension => extension.startsWith('.'));
            for (const file of field.files) {
                if (file.size > 10 * 1024 * 1024) {
                    return 'Dung lượng tài liệu không được vượt quá 10 MB.';
                }
                if (extensions.length && !extensions.some(extension => file.name.toLowerCase().endsWith(extension))) {
                    return 'Định dạng tài liệu không được hỗ trợ.';
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

    function validate(root) {
        let firstInvalid;
        fields(root).forEach(field => {
            if (!validateField(field) && !firstInvalid) {
                firstInvalid = field;
            }
        });
        if (firstInvalid) {
            firstInvalid.focus();
        }
        return !firstInvalid;
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
        let firstInvalid;
        Object.entries(failure.fieldErrors || {}).forEach(([name, message]) => {
            const field = resolve(root, name);
            if (field && setError(root, field, message)) {
                firstInvalid = firstInvalid || field;
            }
        });
        if (firstInvalid) {
            firstInvalid.focus();
        }
        return Boolean(firstInvalid);
    }

    function responseError(payload) {
        const error = new Error(payload.message || 'Không thể hoàn tất thao tác.');
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
        }
    });
    document.addEventListener('DOMContentLoaded', () => document.querySelectorAll('form').forEach(init));
    return { init, validate, validateField, setError, clear, showErrors, responseError };
})();
