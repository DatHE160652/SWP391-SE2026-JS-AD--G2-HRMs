(() => {
    const dialog = document.querySelector('#initialAssignmentDialog');
    const form = document.querySelector('#initialAssignmentForm');
    if (!dialog || !form) return;
    const error = document.querySelector('#initialAssignmentError');
    const save = document.querySelector('#initialAssignmentSave');
    const picker = document.querySelector('#initialAssignmentEmployees');
    const hint = document.querySelector('#initialAssignmentSelectionHint');
    const managerField = document.querySelector('#initialAssignmentManagerField');
    const managerSelect = form.elements.managerId;
    const managerHint = document.querySelector('#initialAssignmentManagerHint');
    let revision = 0;
    let positions = [];
    let levels = [];
    let candidates = [];
    let managers = [];
    let submitting = false;
    function renderEmployees() {
        picker.replaceChildren();
        FormValidation.clear(form);
        error.textContent = '';
        managerField.hidden = true;
        managerSelect.disabled = true;
        managerSelect.replaceChildren(new Option('Chưa chọn quản lý', ''));
        managerHint.textContent = '';
        const position = positions.find(item => String(item.positionId) === form.elements.positionId.value);
        const roleBadge = document.querySelector('#initialAssignmentRole');
        if (roleBadge) roleBadge.textContent = 'Chưa chọn chức danh';
        save.disabled = !position || !candidates.length || submitting;
        if (!position) { hint.textContent = 'Chọn chức danh trước để chọn nhân viên.'; return; }
        const level = levels.find(item => String(item.responsibilityLevelId) === String(position.responsibilityLevelId));
        if (!level) {
            hint.textContent = 'Không xác định được cấp trách nhiệm. Vui lòng tải lại danh sách.';
            save.disabled = true;
            return;
        }
        const employeeLevel = ['EMP','EMPLOYEE'].includes(String(level.levelCode).trim().toUpperCase())
            || String(level.levelName).trim().toUpperCase() === 'EMPLOYEE';
        const single = !employeeLevel && (String(level.levelCode).trim().toUpperCase() === 'MANAGER'
            || String(level.levelName).trim().toUpperCase() === 'MANAGER' || position.canManageEmployees === true);
        if (roleBadge) roleBadge.textContent = single ? 'Manager' : 'Employee';
        if (!single) {
            managerField.hidden = false;
            managerSelect.disabled = false;
            const matching = position.reportsToPositionId == null ? [] : managers.filter(manager=>String(manager.positionId)===String(position.reportsToPositionId));
            for (const manager of matching) managerSelect.add(new Option(manager.employeeCode + ' · ' + manager.fullName + ' · ' + (manager.positionTitle || ''), manager.employeeId));
            if (matching.length === 1) {
                managerSelect.value = matching[0].employeeId;
                managerHint.textContent = 'Gợi ý người đang đảm nhiệm đúng chức danh quản lý đã liên kết. Có thể chọn Chưa chọn quản lý.';
            } else managerHint.textContent = position.reportsToPositionId == null
                ? 'Chưa liên kết chức danh quản lý. Hãy cấu hình tại Chức danh; hệ thống không tự chọn quản lý khác team.'
                : matching.length ? 'Chọn người đang đảm nhiệm chức danh quản lý đã liên kết.' : 'Chưa có quản lý đang hoạt động đảm nhiệm chức danh đã liên kết. Có thể phân công quản lý sau.';
        }
        hint.textContent = single ? 'Chức danh cấp Manager: chỉ chọn một nhân viên.' : 'Có thể chọn nhiều nhân viên cho chức danh này.';
        for (const employee of candidates) {
            const label = document.createElement('label');
            label.className = 'ia-choice';
            const input = document.createElement('input');
            input.type = single ? 'radio' : 'checkbox';
            input.name = 'employeeIds';
            input.value = employee.EmployeeId;
            input.required = single;
            const text = document.createElement('span');
            text.textContent = employee.EmployeeCode + ' · ' + employee.FullName
                + (employee.DepartmentName ? ' · ' + employee.DepartmentName + ' / ' + (employee.PositionTitle || 'Chưa có chức danh') : ' · Chưa phân công');
            label.append(input, text);
            picker.append(label);
        }
        FormValidation.init(form);
    }
    async function open(departmentId) {
        const requestRevision = ++revision;
        form.reset();
        FormValidation.clear(form);
        form.elements.departmentId.value = departmentId;
        positions = []; levels = []; candidates = []; managers = [];
        managerField.hidden = true;
        managerSelect.disabled = true;
        managerSelect.replaceChildren(new Option('Chưa chọn quản lý', ''));
        picker.replaceChildren();
        error.textContent = '';
        hint.textContent = 'Chọn chức danh trước để chọn nhân viên.';
        document.querySelector('#initialAssignmentDepartment').textContent = 'Đang tải phòng ban…';
        document.querySelector('#initialAssignmentEmpty').hidden = true;
        save.disabled = true;
        form.elements.positionId.replaceChildren(new Option('Đang tải chức danh…', ''));
        document.querySelector('#initialAssignmentPositionLink').href = '../position/position-form?departmentId=' + encodeURIComponent(departmentId);
        dialog.showModal();
        try {
            const [department, positionData, candidateData, levelData, memberData] = await Promise.all([
                DepartmentApi.get('/departments/detail?id=' + encodeURIComponent(departmentId)),
                DepartmentApi.get('/positions?activeOnly=true&departmentId=' + encodeURIComponent(departmentId)),
                DepartmentApi.get('/departments/assignment-options?departmentId=' + encodeURIComponent(departmentId)),
                DepartmentApi.get('/responsibility-levels'),
                DepartmentApi.get('/departments/members?departmentId=' + encodeURIComponent(departmentId))
            ]);
            if (requestRevision !== revision || !dialog.open) return;
            positions = positionData.data; candidates = candidateData.data; levels = levelData.data;
            managers = memberData.data.filter(member => member.canManageTeam === true && String(member.departmentId) === String(departmentId));
            const availablePositions = positions.filter(position=>{
                const level=levels.find(item=>String(item.responsibilityLevelId)===String(position.responsibilityLevelId));
                const employee=['EMP','EMPLOYEE'].includes(String(level?.levelCode || '').trim().toUpperCase())
                    || String(level?.levelName || '').trim().toUpperCase()==='EMPLOYEE';
                const manager=!employee && (position.canManageEmployees===true
                    || String(level?.levelCode || '').trim().toUpperCase()==='MANAGER'
                    || String(level?.levelName || '').trim().toUpperCase()==='MANAGER');
                return !manager || position.assignmentOccupied!==true;
            });
            document.querySelector('#initialAssignmentDepartment').textContent = department.data.departmentName;
            form.elements.positionId.replaceChildren(new Option('Chọn chức danh', ''));
            for (const position of availablePositions) form.elements.positionId.add(new Option(position.positionTitle + ' · ' + (position.responsibilityLevelName || ''), position.positionId));
            const empty = document.querySelector('#initialAssignmentEmpty');
            empty.hidden = Boolean(candidates.length && availablePositions.length);
            empty.textContent = !positions.length ? 'Phòng ban chưa có chức danh đang hoạt động. Hãy tạo chức danh trước khi phân công.'
                : !availablePositions.length ? 'Không có chức danh còn chỗ để phân công. Các chức danh Manager đã có người được ẩn khỏi danh sách.'
                : 'Không có nhân viên chưa phân công. Để chuyển phòng ban/chức danh, hãy mở hồ sơ nhân viên và chọn Phân công / Chuyển chức danh.';
            FormValidation.init(form);
            renderEmployees();
        } catch (exception) {
            if (requestRevision === revision && dialog.open) error.textContent = exception.message;
        }
    }
    form.elements.positionId.addEventListener('change', renderEmployees);
    document.addEventListener('click', event => {
        const button = event.target.closest('[data-initial-department]');
        if (button && !submitting) open(button.dataset.initialDepartment);
        if (event.target.closest('[data-initial-close]') && !submitting) dialog.close();
    });
    dialog.addEventListener('cancel', event => { if (submitting) event.preventDefault(); });
    dialog.addEventListener('close', () => { revision++; });
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (save.disabled || submitting) return;
        error.textContent = '';
        const employeeIds = [...picker.querySelectorAll('input[name="employeeIds"]:checked')].map(input => input.value);
        if (!employeeIds.length) {
            error.textContent = 'Vui lòng chọn ít nhất một nhân viên.';
            picker.querySelector('input')?.focus();
            return;
        }
        submitting = true;
        save.disabled = true;
        form.elements.positionId.disabled = true;
        managerSelect.disabled = true;
        picker.querySelectorAll('input').forEach(input => { input.disabled = true; });
        try {
            const payload = await DepartmentApi.post('/departments/initial-assignment', {
                departmentId: form.elements.departmentId.value,
                positionId: form.elements.positionId.value,
                employeeIds: employeeIds.join(','),
                managerId: managerField.hidden ? '' : managerSelect.value
            });
            dialog.close();
            showDepartmentMessage(payload.message);
            document.dispatchEvent(new CustomEvent('department-assignment-saved'));
            if (document.body.dataset.page === 'department-form') await loadDepartmentMembers(form.elements.departmentId.value);
        } catch (exception) {
            if (!FormValidation.showErrors(form, exception)) error.textContent = exception.message;
        } finally {
            submitting = false;
            form.elements.positionId.disabled = false;
            managerSelect.disabled = managerField.hidden;
            picker.querySelectorAll('input').forEach(input => { input.disabled = false; });
            save.disabled = false;
        }
    });
})();
