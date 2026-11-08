document.addEventListener('DOMContentLoaded',async()=>{
    if(document.body.dataset.page!=='personal-workspace') return;
    renderMyProfileSidebar(document.body.dataset.profileRole);
    const content=document.querySelector('#personalWorkspaceContent');
    const dialog=document.querySelector('#personalEmployeeDialog');
    const dialogBody=document.querySelector('#personalEmployeeBody');
    const title=document.querySelector('#personalEmployeeTitle');
    const contractsPage=document.body.dataset.personalView==='contracts';
    const label=contractsPage?HrmI18n.t('Hợp đồng của tôi'):HrmI18n.t('Sơ đồ tổ chức');
    document.querySelector('#personalPageTitle').textContent=label;
    document.querySelector('#personalBreadcrumb').textContent=label;
    document.querySelector('#personalPageDescription').textContent=contractsPage?HrmI18n.t('Chỉ hiển thị các hợp đồng đã và đang ký của chính bạn.'):document.body.dataset.profileRole==='EMPLOYEE'?HrmI18n.t('Quản lý trực tiếp và Admin Company/HR của công ty bạn.'):HrmI18n.t('Nhóm phụ trách và Admin Company/HR của công ty bạn.');
    let revision=0;
    dialog.querySelector('button').addEventListener('click',()=>dialog.close());
    dialog.addEventListener('close',()=>{
        revision++;
        dialogBody.replaceChildren();
        const url=new URL(location.href);
        url.searchParams.delete('employeeId');
        history.replaceState(null,'',url);
    });
    const facts=items=>'<div class="personal-contract-facts">'+items.map(([name,value])=>detailItem(name,value ?? HrmI18n.t('Chưa ghi nhận'))).join('')+'</div>';
    const money=value=>value==null?HrmI18n.t('Chưa ghi nhận'):new Intl.NumberFormat(HrmI18n.locale,{style:'currency',currency:'VND'}).format(value);
    const contractFacts=contract=>facts([
        [HrmI18n.t('Mã hợp đồng'),contract.ContractCode],[HrmI18n.t('Loại hợp đồng'),contract.ContractType],[HrmI18n.t('Trạng thái'),contract.Status],
        [HrmI18n.t('Ngày bắt đầu'),contract.StartDate],[HrmI18n.t('Ngày hết hạn'),contract.EndDate || HrmI18n.t('Không xác định thời hạn')],
        [HrmI18n.t('Ngày ký'),contract.SignedDate],[HrmI18n.t('Lương Gross'),money(contract.GrossSalary)],[HrmI18n.t('Phụ cấp đi lại'),money(contract.TravelAllowance)],
        [HrmI18n.t('Ngày chấm dứt'),contract.TerminatedDate],[HrmI18n.t('Lý do chấm dứt'),contract.TerminationReason],[HrmI18n.t('Ghi chú'),contract.Notes]
    ]);
    async function openEmployee(employeeId){
        const current=++revision;
        title.textContent=HrmI18n.t('Hồ sơ nhân viên');
        dialogBody.textContent=HrmI18n.t('Đang tải hồ sơ…');
        if(!dialog.open) dialog.showModal();
        try {
            const response=await EmployeeApi.get('/employees/me/colleague?employeeId='+encodeURIComponent(employeeId));
            if(current!==revision || !dialog.open) return;
            const result=response.data,person=result.profile;
            title.textContent=person.FullName;
            dialogBody.innerHTML=facts([
                [HrmI18n.t('Mã nhân viên'),person.EmployeeCode],[HrmI18n.t('Họ tên'),person.FullName],[HrmI18n.t('Phòng ban'),person.DepartmentName],
                [HrmI18n.t('Chức danh / Chức vụ'),person.PositionTitle],[HrmI18n.t('Cấp bậc'),person.LevelName],['Manager',person.ManagerName],[HrmI18n.t('Email liên lạc'),person.Email]
            ])+HrmI18n.fragment('<h3 class="personal-private-title">Thông tin cá nhân</h3>')+facts([
                [HrmI18n.t('Số điện thoại'),person.Phone],[HrmI18n.t('Số CCCD/CMND'),person.IdentityNumber],[HrmI18n.t('Địa chỉ thường trú'),person.PermanentAddress],
                [HrmI18n.t('Quê quán'),person.Hometown],[HrmI18n.t('Lương Gross'),person.GrossSalary],[HrmI18n.t('Phụ cấp chức vụ'),person.PositionAllowance],[HrmI18n.t('Phụ cấp đi lại'),person.TravelAllowance]
            ])+HrmI18n.fragment('<p class="personal-protected">Các trường được che không thuộc quyền xem của tài khoản này.</p>')
                +(result.canViewContracts?HrmI18n.fragment('<h3 class="personal-private-title">Hợp đồng lao động</h3>')+personalTable('colleague-contracts-'+employeeId,
                    [HrmI18n.t('Loại hợp đồng'),HrmI18n.t('Ngày bắt đầu'),HrmI18n.t('Ngày hết hạn')],result.contracts.map(contract=>[HrmVietnamese.label(contract.ContractType),contract.StartDate,contract.EndDate || HrmI18n.t('Không xác định thời hạn')])):'');
            const url=new URL(location.href);
            url.searchParams.set('employeeId',employeeId);
            history.replaceState(null,'',url);
        } catch(error){if(current===revision && dialog.open) dialogBody.textContent=error.message;}
    }
    try {
        if(contractsPage){
            const [response,personal]=await Promise.all([EmployeeApi.get('/employees/me/contracts'),EmployeeApi.get('/employees/me')]);
            const employee=personal.data,contracts=response.data;
            content.innerHTML='<section class="lc-card personal-contract-summary">'+facts([
                [HrmI18n.t('Họ tên'),employee.fullName],[HrmI18n.t('Mã nhân viên'),employee.employeeCode],[HrmI18n.t('Phòng ban'),employee.departmentName],[HrmI18n.t('Chức danh'),employee.positionTitle]
            ])+personalTable('own-contracts',[HrmI18n.t('Mã hợp đồng'),HrmI18n.t('Loại hợp đồng'),HrmI18n.t('Ngày bắt đầu'),HrmI18n.t('Ngày hết hạn'),HrmI18n.t('Trạng thái')],contracts.map(contract=>[
                contract.ContractCode,HrmVietnamese.label(contract.ContractType),contract.StartDate,contract.EndDate || HrmI18n.t('Không xác định thời hạn'),HrmVietnamese.label(contract.Status)
            ]))+'</section>';
            const table=content.querySelector('table');
            table.querySelector('thead tr').insertAdjacentHTML('beforeend',HrmI18n.fragment('<th>Chi tiết</th>'));
            table.querySelectorAll('tbody tr').forEach((row,index)=>{
                if(!contracts[index]) {row.firstElementChild.colSpan=6;return;}
                const cell=row.insertCell();
                const button=document.createElement('button');
                button.className='small secondary';button.type='button';button.textContent=HrmI18n.t('Xem hợp đồng');
                button.addEventListener('click',()=>{
                    title.textContent=contracts[index].ContractCode;
                    dialogBody.innerHTML=contractFacts(contracts[index]);
                    dialog.showModal();
                });
                cell.append(button);
            });
            return;
        }
        const response=await EmployeeApi.get('/employees/me/organization-chart');
        const graph=response.data;
        const members=graph.members;
        let zoom=1;
        const collapsed=new Set();
        content.innerHTML=HrmI18n.fragment('<div class="personal-workspace-toolbar"><input id="personalOrgSearch" type="search" placeholder="Tìm tên, mã nhân viên, chức danh…" aria-label="Tìm trong sơ đồ"><select id="personalOrgDepartment" aria-label="Lọc phòng ban"><option value="">Tất cả phòng ban</option></select></div>')
            +HrmI18n.fragment('<section class="oe-chart-stage"><div class="oe-zoom"><button class="small secondary" data-zoom="in" aria-label="Phóng to">＋</button><button class="small secondary" data-zoom="out" aria-label="Thu nhỏ">−</button><button class="small secondary" data-zoom="reset">100%</button></div><div class="oe-viewport" tabindex="0" aria-label="Sơ đồ tổ chức; cuộn để di chuyển"><div class="oe-canvas"></div></div></section>');
        const departments=new Map(members.filter(member=>member.DepartmentId!=null).map(member=>[String(member.DepartmentId),member.DepartmentName]));
        const department=document.querySelector('#personalOrgDepartment');
        departments.forEach((name,id)=>department.add(new Option(name,id)));
        const profileUrl=member=>withContext(Number(member.EmployeeId)===Number(graph.selfEmployeeId)?'/employee/my-profile':'/employee/organization?employeeId='+encodeURIComponent(member.EmployeeId));
        const canvas=content.querySelector('.oe-canvas');
        function draw(){
            const term=document.querySelector('#personalOrgSearch').value.trim().toLocaleLowerCase('vi');
            const matches=members.filter(member=>(!department.value || String(member.DepartmentId)===department.value)
                &&[member.FullName,member.EmployeeCode,member.PositionTitle].join(' ').toLocaleLowerCase('vi').includes(term));
            const scoped=term?HrmOrganizationExplorer.chartContext(members.map(member=>({...member,HasAssignment:true})),matches,department.value):matches;
            canvas.innerHTML=HrmOrganizationChart.render(scoped,null,withContext(''),{depth:members.length+1,collapsed,profileUrl,
                administrationHtml:HrmOrganizationExplorer.administratorsPanel(graph.administrators,withContext(''),profileUrl)});
            const viewport=content.querySelector('.oe-viewport');
            viewport.scrollLeft=Math.max(0,(viewport.scrollWidth-viewport.clientWidth)/2);
        }
        content.addEventListener('click',event=>{
            const link=event.target.closest('.oe-person-name,.oe-admin-card a');
            if(link && new URL(link.href).pathname.endsWith('/employee/organization')){
                event.preventDefault();openEmployee(new URL(link.href).searchParams.get('employeeId'));return;
            }
            const toggle=event.target.closest('[data-org-toggle]');
            if(toggle){const id=toggle.dataset.orgToggle;collapsed.has(id)?collapsed.delete(id):collapsed.add(id);draw();}
            const focus=event.target.closest('[data-org-focus]');
            if(focus) openEmployee(focus.dataset.orgFocus);
            const button=event.target.closest('[data-zoom]');
            if(button){zoom=button.dataset.zoom==='reset'?1:Math.max(.4,Math.min(1.6,Math.round((zoom+(button.dataset.zoom==='in'?.1:-.1))*10)/10));canvas.style.zoom=String(zoom);content.querySelector('[data-zoom="reset"]').textContent=Math.round(zoom*100)+'%';}
        });
        document.querySelector('#personalOrgSearch').addEventListener('input',draw);
        department.addEventListener('change',draw);
        draw();
        const requested=new URLSearchParams(location.search).get('employeeId');
        if(requested) await openEmployee(requested);
    } catch(error){document.querySelector('#personalPageError').textContent=error.message;content.replaceChildren();}
});
