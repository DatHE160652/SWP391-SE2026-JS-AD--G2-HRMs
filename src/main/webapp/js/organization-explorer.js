(function(root) {
    'use strict';
    const escape=value => String(value ?? '').replace(/[&<>"']/g,character => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]));
    const initials=name => String(name || '?').trim().split(/\s+/).slice(-2).map(part => part[0]).join('').toUpperCase();
    const normalize=value => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D').toLowerCase();
    function combine(assignments,employees) {
        const assigned=new Map(assignments.filter(member => member.Status==='Active').map(member => [String(member.EmployeeId),member]));
        const members=employees.filter(employee => employee.status==='Active').map(employee => {
            const assignment=assigned.get(String(employee.employeeId));
            assigned.delete(String(employee.employeeId));
            return {EmployeeId:employee.employeeId,EmployeeCode:employee.employeeCode,FullName:employee.fullName,
                Email:employee.email,Phone:employee.phone,DepartmentId:null,DepartmentName:null,
                PositionId:null,PositionTitle:null,ManagerId:null,...assignment,HasAssignment:Boolean(assignment)};
        });
        return members.concat([...assigned.values()].map(member => ({...member,HasAssignment:true})));
    }
    function filter(members,department,query) {
        const term=normalize(query);
        return members.filter(member => (!department || String(member.DepartmentId)===String(department))
            && normalize([member.FullName,member.EmployeeCode,member.DepartmentName,member.PositionTitle,member.Email,member.ManagerName].join(' ')).includes(term));
    }
    function chartContext(members,matches,department) {
        const scoped=new Map(members.filter(member => member.HasAssignment && (!department || String(member.DepartmentId)===String(department))).map(member => [String(member.EmployeeId),member]));
        const result=new Map();
        matches.forEach(member => {
            let current=scoped.get(String(member.EmployeeId));
            const path=new Set();
            while(current && !path.has(String(current.EmployeeId))) {
                path.add(String(current.EmployeeId));
                result.set(String(current.EmployeeId),current);
                current=scoped.get(String(current.ManagerId));
            }
        });
        return [...result.values()];
    }
    function directory(members,context,selected) {
        if (!members.length) return HrmI18n.fragment('<div class="lc-empty">Không có nhân viên phù hợp.</div>');
        return '<div class="oe-directory">'+members.map(member => '<article class="oe-directory-card '+(String(selected)===String(member.EmployeeId)?'selected':'')+'">'
            +'<span class="oe-directory-avatar" aria-hidden="true">'+escape(initials(member.FullName))+'</span>'
            +'<a href="'+escape(context)+'/employee/employee-list?id='+escape(member.EmployeeId)+'"><strong>'+escape(member.FullName)+'</strong></a>'
            +'<span>'+escape(member.PositionTitle || HrmI18n.t('Chưa phân công chức danh'))+'</span>'
            +'<small>'+escape(member.DepartmentName || HrmI18n.t('Chưa phân công phòng ban'))+' · '+escape(member.EmployeeCode)+'</small>'
            +'<div class="oe-directory-contact"><span>'+escape(member.Email || HrmI18n.t('Chưa có email'))+'</span><span>'+escape(member.Phone || HrmI18n.t('Chưa có số điện thoại'))+'</span></div>'
            +(member.HasAssignment?'<button type="button" class="small secondary" data-org-focus="'+escape(member.EmployeeId)+HrmI18n.fragment('">Xem trong sơ đồ</button>'):HrmI18n.fragment('<span class="lc-badge">Chưa có phân công hiệu lực</span>'))+'</article>').join('')+'</div>';
    }
    function list(members,context,selected) {
        if (!members.length) return HrmI18n.fragment('<div class="lc-empty">Không có nhân viên phù hợp.</div>');
        return HrmI18n.fragment('<div class="lc-table-wrap"><table><thead><tr><th>Nhân viên</th><th>Chức danh / Phòng ban</th><th>Cấp trách nhiệm</th><th>Quản lý</th><th>Liên hệ</th><th></th></tr></thead><tbody>')
            +members.map(member => '<tr class="'+(String(selected)===String(member.EmployeeId)?'oe-selected-row':'')+'">'
            +'<td><a href="'+escape(context)+'/employee/employee-list?id='+escape(member.EmployeeId)+'"><strong>'+escape(member.FullName)+'</strong></a><small class="oe-block">'+escape(member.EmployeeCode)+'</small></td>'
            +'<td>'+escape(member.PositionTitle || HrmI18n.t('Chưa phân công'))+'<small class="oe-block">'+escape(member.DepartmentName)+'</small></td>'
            +'<td>'+escape(member.LevelName || '—')+'</td><td>'+escape(member.ManagerName || '—')+'</td>'
            +'<td>'+escape(member.Email || '—')+'<small class="oe-block">'+escape(member.Phone)+'</small></td>'
            +'<td>'+(member.HasAssignment?'<button class="small secondary" data-org-focus="'+escape(member.EmployeeId)+HrmI18n.fragment('">Sơ đồ</button>'):HrmI18n.fragment('<span class="lc-badge">Chưa phân công</span>'))+'</td></tr>').join('')+'</tbody></table></div>';
    }
    function csv(members) {
        const safe=value => {
            let text=String(value ?? '');
            if (/^[\s]*[=+\-@]/.test(text) || /^[\t\r\n]/.test(text)) text="'"+text;
            return '"'+text.replace(/"/g,'""')+'"';
        };
        const rows=[[HrmI18n.t('Mã nhân viên'),HrmI18n.t('Họ tên'),HrmI18n.t('Phòng ban'),HrmI18n.t('Chức danh'),HrmI18n.t('Cấp trách nhiệm'),HrmI18n.t('Quản lý'),'Email',HrmI18n.t('Điện thoại')],
            ...members.map(member => [member.EmployeeCode,member.FullName,member.DepartmentName,member.PositionTitle,member.LevelName,member.ManagerName,member.Email,member.Phone])];
        return '\uFEFF'+rows.map(row => row.map(safe).join(',')).join('\r\n');
    }
    function administratorsPanel(administrators,context,profileUrl) {
        const accounts=administrators.filter(account => ['COMPANY_ADMIN','HR'].includes(account.RoleCode));
        return HrmI18n.fragment('<section class="oe-administration" aria-labelledby="oe-administration-title"><div class="oe-administration-heading"><div><h3 id="oe-administration-title">Quản trị công ty</h3><small>')+escape(accounts[0]?.CompanyName || '')+'</small></div><span class="lc-badge">'+accounts.length+HrmI18n.fragment(' tài khoản</span></div>')
            +'<div class="oe-administrators">'+(accounts.length?accounts.map(account => '<article class="oe-admin-card"><span class="oe-admin-avatar" aria-hidden="true">'+escape(initials(account.DisplayName))+'</span><div><strong>'+escape(account.DisplayName || HrmI18n.t('Tài khoản #')+account.UserId)+'</strong><span class="oe-admin-role">'+(account.RoleCode==='COMPANY_ADMIN'?HrmI18n.t('Quản trị viên công ty'):HrmI18n.t('Nhân sự · Quản trị công ty'))+'</span>'
                +(account.EmployeeId?'<a href="'+escape(profileUrl?profileUrl(account):context+'/employee/employee-list?id='+encodeURIComponent(account.EmployeeId))+HrmI18n.fragment('">Xem hồ sơ →</a>'):HrmI18n.fragment('<small>Tài khoản quản trị · chưa liên kết hồ sơ nhân viên</small>'))+'</div></article>').join(''):HrmI18n.fragment('<p class="lc-muted">Chưa có tài khoản quản trị viên công ty / nhân sự đang hoạt động.</p>'))+HrmI18n.fragment('</div><p class="oe-administration-note">Quản trị viên công ty / Nhân sự quản lý toàn bộ công ty, bao gồm các quản lý và nhân viên trong các nhánh bên dưới.</p></section>');
    }
    function mount(element,members,context,administrators=[]) {
        const parameters=new URLSearchParams(location.search);
        const views=new Set(['list','directory','chart']);
        const state={view:views.has(parameters.get('view'))?parameters.get('view'):'chart',
            department:parameters.get('departmentId') || '',query:parameters.get('q') || '',
            selected:parameters.get('employeeId') || '',depth:6,zoom:1,collapsed:new Set()};
        const departments=[...new Map(members.filter(member => member.DepartmentId).map(member => [String(member.DepartmentId),member.DepartmentName])).entries()];
        if (!departments.some(([id]) => id===state.department)) state.department='';
        if (!members.some(member => String(member.EmployeeId)===state.selected)) state.selected='';
        const option=(value,name) => '<option value="'+escape(value)+'">'+escape(name)+'</option>';
        element.innerHTML='<section class="oe-workspace"><div class="oe-topline"><a class="oe-new" href="'+escape(context)+HrmI18n.fragment('/employee/employee-list?add=1">＋ Thêm nhân viên</a>')
            +HrmI18n.fragment('<div class="oe-view-tabs" role="tablist" aria-label="Chế độ xem cơ cấu"><button type="button" role="tab" data-org-view="list">☷ Danh sách</button><button type="button" role="tab" data-org-view="directory">▦ Danh bạ</button><button type="button" role="tab" data-org-view="chart">♧ Sơ đồ tổ chức</button></div></div>')
            +HrmI18n.fragment('<div class="oe-toolbar"><input id="oe-search" type="search" placeholder="Tìm nhân viên, mã, chức danh…" aria-label="Tìm nhân viên">')
            +HrmI18n.fragment('<select id="oe-department" aria-label="Lọc phòng ban">')+option('',HrmI18n.t('Tất cả phòng ban'))+departments.map(([id,name]) => option(id,name)).join('')+'</select>'
            +HrmI18n.fragment('<select id="oe-jump" aria-label="Đi đến nhân viên">')+option('',HrmI18n.t('Toàn bộ cơ cấu'))+members.map(member => option(member.EmployeeId,member.FullName+' · '+member.EmployeeCode)).join('')+'</select>'
            +HrmI18n.fragment('<div class="oe-chart-tools"><button type="button" class="secondary small" data-org-action="collapse">Thu gọn</button><button type="button" class="secondary small" data-org-action="expand">Mở rộng</button></div>')
            +HrmI18n.fragment('<button type="button" class="secondary small oe-export" data-org-action="export">↧ Xuất Excel</button></div>')
            +'<div class="oe-status" role="status" aria-live="polite"></div>'
            +'<div class="oe-administration-summary"></div>'
            +'<div class="oe-panel" role="tabpanel" id="oe-panel"><div class="oe-results"></div></div>'
            +HrmI18n.fragment('<div class="oe-chart-stage" role="tabpanel" id="oe-chart-panel" hidden><div class="oe-zoom"><button class="secondary small" type="button" data-org-action="zoom-in" aria-label="Phóng to">＋</button><button class="secondary small" type="button" data-org-action="zoom-out" aria-label="Thu nhỏ">−</button><button class="secondary small" type="button" data-org-action="zoom-reset" aria-label="Đặt lại mức phóng">100%</button></div><div class="oe-viewport" tabindex="0" aria-label="Sơ đồ tổ chức; cuộn để di chuyển"><div class="oe-canvas"></div></div></div></section>');
        const get=selector => element.querySelector(selector);
        get('#oe-search').value=state.query; get('#oe-department').value=state.department; get('#oe-jump').value=state.selected;
        const updateUrl=() => {
            const url=new URL(location.href);
            for (const [name,value] of Object.entries({view:state.view,departmentId:state.department,q:state.query,employeeId:state.selected})) {
                if (value) url.searchParams.set(name,value); else url.searchParams.delete(name);
            }
            history.replaceState(null,'',url);
        };
        const updateZoom=() => {
            get('.oe-canvas').style.zoom=String(state.zoom);
            get('[data-org-action=zoom-reset]').textContent=Math.round(state.zoom*100)+'%';
        };
        const render=() => {
            const matches=filter(members,state.department,state.query);
            const selected=members.find(member => String(member.EmployeeId)===state.selected);
            const isChart=state.view==='chart';
            element.querySelectorAll('[data-org-view]').forEach(button => {
                const active=button.dataset.orgView===state.view;
                button.classList.toggle('active',active); button.setAttribute('aria-selected',String(active));
                button.id='oe-tab-'+button.dataset.orgView;
                button.setAttribute('aria-controls',button.dataset.orgView==='chart'?'oe-chart-panel':'oe-panel'); button.tabIndex=active?0:-1;
            });
            get(isChart?'.oe-chart-stage':'.oe-panel').setAttribute('aria-labelledby','oe-tab-'+state.view);
            get('.oe-chart-tools').hidden=!isChart; get('.oe-panel').hidden=isChart; get('.oe-chart-stage').hidden=!isChart;
            get('.oe-administration-summary').hidden=isChart;
            get('.oe-administration-summary').innerHTML=isChart?'':administratorsPanel(administrators,context);
            const unassigned=matches.filter(member => !member.HasAssignment).length;
            get('.oe-status').textContent=matches.length+' / '+members.length+HrmI18n.t(' nhân viên')+(selected?HrmI18n.t(' · Đang chọn: ')+selected.FullName:'')+(isChart && unassigned?' · '+unassigned+HrmI18n.t(' chưa phân công (xem tại Danh sách / Danh bạ)'):'');
            if (isChart) {
                const chartMembers=chartContext(members,matches,state.department);
                const focus=chartMembers.some(member => String(member.EmployeeId)===state.selected)?state.selected:'';
                get('.oe-canvas').innerHTML=selected && !selected.HasAssignment?HrmI18n.fragment('<div class="lc-empty">Nhân viên đã chọn chưa có phân công hiệu lực. Xem hồ sơ tại Danh sách / Danh bạ.</div>'):state.selected && !focus?HrmI18n.fragment('<div class="lc-empty">Nhân viên đã chọn nằm ngoài phạm vi bộ lọc hiện tại.</div>'):root.HrmOrganizationChart.render(chartMembers,focus,context,{depth:state.depth,collapsed:state.collapsed,administrationHtml:administrators.length?administratorsPanel(administrators,context):''});
                updateZoom();
                const viewport=get('.oe-viewport');
                viewport.scrollLeft=Math.max(0,((viewport.scrollWidth || 0)-(viewport.clientWidth || 0))/2);
            } else {
                get('.oe-canvas').innerHTML='';
                get('.oe-results').innerHTML=state.view==='list'?list(matches,context,state.selected):directory(matches,context,state.selected);
            }
            updateUrl();
        };
        element.addEventListener('click',event => {
            const view=event.target.closest('[data-org-view]');
            if (view) { state.view=view.dataset.orgView; render(); return; }
            const focus=event.target.closest('[data-org-focus]');
            if (focus) {
                state.selected=focus.dataset.orgFocus; state.view='chart'; state.query='';
                const selected=members.find(member => String(member.EmployeeId)===state.selected);
                if (state.department && String(selected?.DepartmentId)!==state.department) state.department='';
                get('#oe-search').value=''; get('#oe-department').value=state.department; get('#oe-jump').value=state.selected;
                render(); return;
            }
            const toggle=event.target.closest('[data-org-toggle]');
            if (toggle) {
                const id=toggle.dataset.orgToggle;
                if (toggle.getAttribute('aria-expanded')==='true') state.collapsed.add(id);
                else { state.collapsed.delete(id); state.depth=6; }
                render(); return;
            }
            const action=event.target.closest('[data-org-action]')?.dataset.orgAction;
            if (!action) return;
            if (action==='collapse') { state.depth=1; state.collapsed.clear(); render(); }
            if (action==='expand') { state.depth=6; state.collapsed.clear(); render(); }
            if (action==='zoom-in') { state.zoom=Math.min(1.6,Math.round((state.zoom+.1)*10)/10); updateZoom(); }
            if (action==='zoom-out') { state.zoom=Math.max(.5,Math.round((state.zoom-.1)*10)/10); updateZoom(); }
            if (action==='zoom-reset') { state.zoom=1; updateZoom(); }
            if (action==='export') {
                const params=new URLSearchParams();
                if (state.department) params.set('departmentId',state.department);
                if (state.query) params.set('keyword',state.query);
                window.location.href=context+'/lifecycle/organization/export'+(params.toString()?'?'+params.toString():'');
            }
        });
        get('.oe-view-tabs').addEventListener('keydown',event => {
            if (!['ArrowLeft','ArrowRight','Home','End'].includes(event.key)) return;
            event.preventDefault();
            const tabs=[...element.querySelectorAll('[data-org-view]')],current=tabs.findIndex(tab => tab.dataset.orgView===state.view);
            const index=event.key==='Home'?0:event.key==='End'?tabs.length-1:(current+(event.key==='ArrowRight'?1:-1)+tabs.length)%tabs.length;
            tabs[index].click(); tabs[index].focus();
        });
        get('#oe-search').addEventListener('input',event => { state.query=event.target.value; render(); });
        get('#oe-department').addEventListener('change',event => { state.department=event.target.value; render(); });
        get('#oe-jump').addEventListener('change',event => {
            state.selected=event.target.value;
            if (state.selected) {
                state.query=''; get('#oe-search').value='';
                const selected=members.find(member => String(member.EmployeeId)===state.selected);
                if (state.department && String(selected?.DepartmentId)!==state.department) { state.department=''; get('#oe-department').value=''; }
            }
            render();
        });
        const viewport=get('.oe-viewport');
        let drag=null;
        viewport.addEventListener('pointerdown',event => {
            if (event.pointerType!=='mouse' || event.button!==0 || event.target.closest('a,button,input,select')) return;
            drag={left:viewport.scrollLeft,top:viewport.scrollTop,x:event.clientX,y:event.clientY};
            viewport.setPointerCapture(event.pointerId);
            viewport.classList.add('is-dragging');
            event.preventDefault();
        });
        viewport.addEventListener('pointermove',event => {
            if (!drag) return;
            viewport.scrollLeft=drag.left-(event.clientX-drag.x);
            viewport.scrollTop=drag.top-(event.clientY-drag.y);
        });
        const stopDrag=() => { drag=null; viewport.classList.remove('is-dragging'); };
        viewport.addEventListener('pointerup',stopDrag);
        viewport.addEventListener('pointercancel',stopDrag);
        viewport.addEventListener('lostpointercapture',stopDrag);
        render();
        return {state,render};
    }
    root.HrmOrganizationExplorer={combine,filter,chartContext,directory,list,csv,administratorsPanel,mount};
    if (typeof module!=='undefined') module.exports=root.HrmOrganizationExplorer;
})(globalThis);
