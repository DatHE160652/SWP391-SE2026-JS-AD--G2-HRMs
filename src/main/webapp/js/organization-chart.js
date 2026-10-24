(function(root) {
    'use strict';
    const escape=value => String(value ?? '').replace(/[&<>"']/g,character => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]));
    const initials=name => String(name || '?').trim().split(/\s+/).slice(-2).map(part => part[0]).join('').toUpperCase();
    const collator=new Intl.Collator('vi',{numeric:true,sensitivity:'base'});
    const departmentKey=member => member.DepartmentId!=null?'id:'+member.DepartmentId:member.DepartmentName?'name:'+member.DepartmentName:'unassigned';
    const comparePeople=(first,second) => collator.compare(first.DepartmentName || '',second.DepartmentName || '')
        || collator.compare(first.FullName || '',second.FullName || '')
        || collator.compare(first.EmployeeCode || '',second.EmployeeCode || '')
        || collator.compare(String(first.EmployeeId),String(second.EmployeeId));
    function availableDepth(members,employeeId,includeAdministration=false) {
        const byId=new Map(members.map(member => [String(member.EmployeeId),member]));
        const selected=employeeId?byId.get(String(employeeId)):null;
        if (employeeId && !selected) return 0;
        const roots=selected?[selected]:members.filter(member => !byId.has(String(member.ManagerId)));
        const children=new Map();
        members.forEach(member => {
            const manager=String(member.ManagerId);
            if (!children.has(manager)) children.set(manager,[]);
            children.get(manager).push(member);
        });
        const pending=roots.map(member => ({member,depth:includeAdministration && !selected?1:0}));
        const visited=new Set();
        let maximum=0;
        while (pending.length) {
            const current=pending.pop(),id=String(current.member.EmployeeId);
            if (visited.has(id)) continue;
            visited.add(id);
            maximum=Math.max(maximum,current.depth);
            pending.push(...(children.get(id)||[]).map(member => ({member,depth:current.depth+1})));
        }
        return maximum;
    }
    function render(members,employeeId,context,options={}) {
        const byId=new Map(members.map(member => [String(member.EmployeeId),member]));
        if (!members.length) return (options.administrationHtml || '')+'<div class="lc-empty">Chưa có nhân viên được phân công có hiệu lực.</div>';
        const selected=employeeId ? byId.get(String(employeeId)) : null;
        if (employeeId && !selected) return '<div class="lc-empty">Nhân viên không thuộc phạm vi đang xem.</div>';
        const children=new Map();
        members.forEach(member => {
            const managerId=String(member.ManagerId);
            if (!children.has(managerId)) children.set(managerId,[]);
            children.get(managerId).push(member);
        });
        children.forEach(reports => reports.sort(comparePeople));
        const collapsed=options.collapsed || new Set();
        const depth=Math.max(1,Math.min(6,Number(options.depth) || 1));
        const ancestors=new Set();
        let cursor=selected;
        while(cursor) {
            const id=String(cursor.EmployeeId);
            if (ancestors.has(id)) return '<div class="lc-alert">Phát hiện vòng lặp quản lý. Vui lòng kiểm tra phân công.</div>';
            ancestors.add(id); cursor=byId.get(String(cursor.ManagerId));
        }
        const branch=(member,level,path) => {
            const id=String(member.EmployeeId);
            if (path.has(id)) return '<li><div class="lc-alert">Phát hiện vòng lặp quản lý.</div></li>';
            const nextPath=new Set(path).add(id),reports=children.get(id) || [];
            const expanded=reports.length && level<depth && !collapsed.has(id);
            return '<li class="oe-branch"><article class="oe-person" data-org-person="'+escape(id)+'">'
                +'<span class="oe-avatar" aria-hidden="true">'+escape(initials(member.FullName))+'</span>'
                +'<a class="oe-person-name" href="'+escape(options.profileUrl?options.profileUrl(member):context+'/employee/employee-list?id='+encodeURIComponent(id))+'">'+escape(member.FullName)+'</a>'
                +'<span class="oe-person-title" title="'+escape(member.PositionTitle)+'">'+escape(member.PositionTitle || 'Chưa có chức danh')+'</span>'
                +'<small>'+escape(member.DepartmentName)+'</small>'
                +'<div class="oe-person-footer"><span>'+escape(member.EmployeeCode)+'</span>'
                +(reports.length?'<button class="oe-branch-focus" type="button" data-org-focus="'+escape(id)+'" aria-label="Tập trung vào '+escape(member.FullName)+'">◎</button><button type="button" data-org-toggle="'+escape(id)+'" aria-expanded="'+Boolean(expanded)+'" aria-label="'+(expanded?'Thu gọn':'Mở rộng')+' cấp dưới của '+escape(member.FullName)+'">'+reports.length+' '+(expanded?'⌃':'⌄')+'</button>':'')
                +'</div></article>'+(expanded?'<ul class="oe-children">'+reports.map(report => branch(report,level+1,nextPath)).join('')+'</ul>':'')+'</li>';
        };
        const roots=selected?[selected]:members.filter(member => !byId.has(String(member.ManagerId)));
        const initialLevel=options.administrationHtml && !selected?1:0;
        const departments=new Map();
        if (!selected) roots.forEach(member => {
            const key=departmentKey(member);
            if (!departments.has(key)) departments.set(key,{key,name:member.DepartmentName || 'Chưa xác định phòng ban',roots:[]});
            departments.get(key).roots.push(member);
        });
        const tree=selected?branch(selected,initialLevel,new Set()):[...departments.values()]
            .sort((first,second) => collator.compare(first.name,second.name) || collator.compare(first.key,second.key))
            .map(department => '<li class="oe-branch oe-department-branch" data-org-department="'+escape(department.key)+'">'
                +'<div class="oe-department-heading"><strong>'+escape(department.name)+'</strong><small>'+department.roots.length+' nhánh trực thuộc</small></div>'
                +'<ul class="oe-children oe-department-roots" aria-label="'+escape('Nhân sự '+department.name)+'">'
                +department.roots.slice().sort(comparePeople).map(member => branch(member,initialLevel,new Set())).join('')+'</ul></li>').join('');
        const reachable=new Set(),pending=[...roots];
        while(pending.length) {
            const member=pending.pop(),id=String(member.EmployeeId);
            if (reachable.has(id)) continue;
            reachable.add(id); pending.push(...(children.get(id) || []));
        }
        const warning=!selected && reachable.size<members.length?'<div class="lc-alert">Có quan hệ quản lý bị vòng lặp hoặc chưa hợp lệ; không thể hiển thị đầy đủ.</div>':'';
        const parent=selected && byId.get(String(selected.ManagerId));
        const chart=options.administrationHtml
            ? '<ul class="oe-tree oe-company-tree"><li class="oe-company-root">'+options.administrationHtml+'<ul class="oe-children oe-company-branches" aria-label="Các nhánh thuộc phạm vi quản trị công ty">'+tree+'</ul></li></ul>'
            : '<ul class="oe-tree">'+tree+'</ul>';
        return (parent?'<button class="small secondary oe-parent" data-org-focus="'+escape(parent.EmployeeId)+'">↑ '+escape(parent.FullName)+'</button>':'')+chart+warning;
    }
    root.HrmOrganizationChart={render,availableDepth};
    if (typeof module!=='undefined') module.exports={render,availableDepth};
})(globalThis);
