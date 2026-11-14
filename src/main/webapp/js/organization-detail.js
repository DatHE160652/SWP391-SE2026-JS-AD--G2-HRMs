(() => {
    const type = location.pathname.split('/').slice(-2,-1)[0];
    const configs = {
        department: {title:HrmI18n.t('phòng ban'), api:'departments', fields:[['departmentCode',HrmI18n.t('Mã phòng ban')],['departmentName',HrmI18n.t('Tên phòng ban')],['isActive',HrmI18n.t('Trạng thái')]]},
        position: {title:HrmI18n.t('chức danh'), api:'positions', fields:[['departmentName',HrmI18n.t('Phòng ban')],['positionTitle',HrmI18n.t('Tên chức danh')],['description',HrmI18n.t('Mô tả')],['responsibilityLevelName',HrmI18n.t('Cấp trách nhiệm')],['effectiveAllowance',HrmI18n.t('Phụ cấp theo cấp trách nhiệm')],['reportsToPositionId',HrmI18n.t('Chức danh quản lý trực tiếp')],['isActive',HrmI18n.t('Trạng thái')],['canManageEmployees',HrmI18n.t('Cho phép quản lý nhân viên')]]},
        'responsibility-level': {title:HrmI18n.t('cấp trách nhiệm'), api:'responsibility-levels', fields:[['levelCode',HrmI18n.t('Mã cấp trách nhiệm')],['levelName',HrmI18n.t('Tên cấp trách nhiệm')],['rankOrder',HrmI18n.t('Thứ bậc')],['defaultAllowance',HrmI18n.t('Phụ cấp mặc định')],['isActive',HrmI18n.t('Trạng thái')]]}
    };
    const config = configs[type];
    const id = new URLSearchParams(location.search).get('id');
    const context = document.body.dataset.context || '';
    document.querySelector('#detailTitle').textContent = document.title = `${HrmI18n.fragment("Chi tiết ")}${config.title}`;
    document.querySelector('#detailBack').href = `${type}-list`;
    async function get(path) {
        const response = await fetch(context+path,{headers:{Accept:'application/json'}});
        const payload = await response.json().then(HrmI18n.response);
        if (!response.ok || !payload.success) throw new Error(payload.message || HrmI18n.t('Không tải được thông tin.'));
        return payload.data;
    }
    async function load() {
        if (!id || !/^[1-9][0-9]*$/.test(id)) throw new Error(HrmI18n.t('Vui lòng chọn bản ghi hợp lệ.'));
        const record = await get(`/${config.api}/detail?id=${encodeURIComponent(id)}`);
        if (!record) throw new Error(HrmI18n.t('Không tìm thấy bản ghi.'));
        let reportingTitle = HrmI18n.t('Chưa cấu hình');
        if (type === 'position' && record.reportsToPositionId) {
            const reporting = await get(`/positions/detail?id=${encodeURIComponent(record.reportsToPositionId)}`);
            reportingTitle = reporting.positionTitle;
        }
        const nodes = config.fields.map(([name,label]) => {
            const group = document.createElement('div');
            const term = document.createElement('dt'); term.textContent = label;
            const value = document.createElement('dd'); value.style.margin = '8px 0 0';
            value.style.whiteSpace = 'pre-wrap';
            const raw = record[name];
            value.textContent = name === 'isActive' ? (raw ? HrmI18n.t('Đang hoạt động') : HrmI18n.t('Ngừng hoạt động'))
                : name === 'canManageEmployees' ? (raw ? HrmI18n.t('Có') : HrmI18n.t('Không'))
                : name === 'reportsToPositionId' ? reportingTitle
                : /Allowance$/.test(name) && raw != null ? new Intl.NumberFormat(HrmI18n.locale,{style:'currency',currency:'VND'}).format(raw)
                : raw == null || raw === '' ? HrmI18n.t('Chưa cập nhật') : String(raw);
            group.append(term,value); return group;
        });
        document.querySelector('#detailFields').replaceChildren(...nodes);
        const edit = document.querySelector('#detailEdit');
        edit.href = `${type}-form?id=${encodeURIComponent(id)}`; edit.hidden = false;
        const messageKey = {department:'departmentMessage',position:'organizationMessage','responsibility-level':'responsibilityLevelMessage'}[type];
        const savedMessage = sessionStorage.getItem(messageKey);
        if (savedMessage) {
            const message = document.querySelector('#message');
            message.textContent = savedMessage; message.className = 'message show success';
            sessionStorage.removeItem(messageKey);
        }
        if (type === 'department') {
            const links = document.querySelector('#departmentPositionLinks');
            for (const [path,label] of [['position-list',HrmI18n.t('Xem chức danh')],['position-form',HrmI18n.t('Thêm chức danh')]]) {
                const link = document.createElement('a');
                link.href = `../position/${path}?departmentId=${encodeURIComponent(id)}`;
                link.textContent = label; links.append(link);
            }
            const button = document.querySelector('#departmentInitialAssignment');
            button.hidden = !record.isActive; button.dataset.initialDepartment = id;
            await loadDepartmentMembers(id);
        }
    }
    load().catch(error => {
        document.querySelector('#detailFields').replaceChildren();
        const message = document.querySelector('#message');
        message.textContent = error.message; message.className = 'message show error';
    });
})();
