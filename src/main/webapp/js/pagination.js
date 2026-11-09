(() => {
    'use strict';
    const sizes = [10,20,50];
    const states = new Map();
    const targets = new Map();
    function pageNumbers(page, total, compact) {
        if (total < 1) return [];
        if (total <= 5) return Array.from({length:total},(_,index) => index+1);
        const radius = compact ? 0 : 1;
        const pages = new Set([1,total]);
        for (let index = Math.max(1,page-radius); index <= Math.min(total,page+radius); index++) pages.add(index);
        return [...pages].filter(value => value > 0).sort((first,second) => first-second);
    }
    function render(host, options) {
        const total = Math.max(0,Number(options.total) || 0);
        const size = sizes.includes(Number(options.pageSize)) ? Number(options.pageSize) : 10;
        const pages = Math.ceil(total/size);
        const page = Math.max(1,Math.min(Number(options.page) || 1,pages || 1));
        const prefix = options.idPrefix;
        const create = (tag,className,text) => {
            const element = document.createElement(tag);
            if (className) element.className = className;
            if (text != null) element.textContent = text;
            return element;
        };
        host.classList.add('hrm-pagination');
        host.classList.toggle('hrm-pagination--compact',Boolean(options.compact));
        host.setAttribute('aria-label',options.label || HrmI18n.t('Phân trang'));
        host.replaceChildren();
        const meta = create('div','hrm-pagination__meta');
        const sizeLabel = create('label','hrm-pagination__size',HrmI18n.t('Số dòng'));
        const sizeSelect = create('select');
        sizeSelect.setAttribute('aria-label',HrmI18n.t('Số dòng mỗi trang'));
        if (prefix) sizeSelect.id = prefix+'-size';
        for (const value of sizes) sizeSelect.add(new Option(String(value),String(value)));
        sizeSelect.value = String(size);
        sizeSelect.addEventListener('change',() => {
            options.onChange(1,Number(sizeSelect.value));
            host.querySelector('select')?.focus({preventScroll:true});
        });
        sizeLabel.append(sizeSelect);
        const range = create('span','hrm-pagination__range',total ? `${(page-1)*size+1}–${Math.min(page*size,total)} / ${total}` : HrmI18n.t('0 nhân viên'));
        if (!total && !options.compact) range.textContent = HrmI18n.t('0 kết quả');
        range.setAttribute('aria-live','polite');
        if (prefix) range.id = prefix+'-range';
        meta.append(sizeLabel,range);
        const controls = create('div','hrm-pagination__controls');
        const addButton = (text,destination,label,disabled,id) => {
            const button = create('button','hrm-pagination__button',text);
            button.type = 'button'; button.disabled = disabled;
            button.setAttribute('aria-label',label);
            if (id) button.id = id;
            if (destination === page && !id) {
                button.classList.add('is-current');
                button.setAttribute('aria-current','page');
            }
            button.addEventListener('click',() => {
                options.onChange(destination,size);
                const next = [...host.querySelectorAll('button')].find(candidate => candidate.getAttribute('aria-label') === label && !candidate.disabled);
                next?.focus({preventScroll:true});
            });
            controls.append(button);
        };
        addButton('‹',page-1,HrmI18n.t('Trang trước'),page<=1,prefix ? prefix+'-prev' : '');
        let previous = 0;
        for (const number of pageNumbers(page,pages,options.compact)) {
            if (previous && number-previous>1) controls.append(create('span','hrm-pagination__ellipsis','…'));
            addButton(String(number),number,'Trang '+number,false,'');
            previous = number;
        }
        addButton('›',page+1,'Trang sau',page>=pages,prefix ? prefix+'-next' : '');
        const status = create('span','hrm-pagination__status',`Trang ${pages ? page : 0} / ${pages}`);
        if (prefix) status.id = prefix+'-status';
        status.setAttribute('aria-live','polite');
        host.append(meta,controls,status);
    }
    function rowsOf(target) {
        if (target.tagName === 'TABLE') {
            const rows = [...(target.tBodies[0]?.rows || [])];
            return rows.length === 1 && rows[0].cells.length === 1 && rows[0].cells[0].colSpan > 1 ? [] : rows;
        }
        return [...target.children].filter(element => element.matches('article'));
    }
    function refresh(target, force = false) {
        const instance = targets.get(target);
        const rows = rowsOf(target);
        if (!force && rows.length === instance.rows.length && rows.every((row,index) => row === instance.rows[index])) return;
        instance.rows = rows;
        const state = instance.state;
        state.page = Math.max(1,Math.min(state.page,Math.ceil(rows.length/state.pageSize) || 1));
        const start = (state.page-1)*state.pageSize;
        rows.forEach((row,index) => row.toggleAttribute('data-page-hidden',index<start || index>=start+state.pageSize));
        render(instance.host,{total:rows.length,page:state.page,pageSize:state.pageSize,label:HrmI18n.t('Phân trang ')+(target.dataset.pageLabel || HrmI18n.t('danh sách')),onChange(page,pageSize) {
            state.page = page; state.pageSize = pageSize;
            refresh(target,true);
        }});
    }
    function scan() {
        for (const [target,instance] of targets) if (!target.isConnected) {
            instance.host.remove(); targets.delete(target);
        }
        for (const target of document.querySelectorAll('[data-pagination]')) {
            if (!targets.has(target)) {
                const key = location.pathname+'|'+(new URLSearchParams(location.search).get('id') || '')+'|'+target.dataset.pagination;
                if (!states.has(key)) states.set(key,{page:1,pageSize:10});
                const host = document.createElement('nav');
                const anchor = target.parentElement?.classList.contains('lc-table-wrap') ? target.parentElement : target;
                anchor.after(host);
                targets.set(target,{host,state:states.get(key),rows:[]});
                refresh(target,true);
            } else refresh(target);
        }
    }
    function start() {
        scan();
        let scheduled = false;
        new MutationObserver(() => {
            if (scheduled) return;
            scheduled = true;
            queueMicrotask(() => { scheduled = false; scan(); });
        }).observe(document.body,{childList:true,subtree:true});
        for (const event of ['input','change']) document.addEventListener(event,change => {
            if (!change.target.matches('#statusFilter,#departmentFilter,#levelFilter,#lc-resource-search,#lc-resource-status,#lc-resource-type,#lc-account-approval-search')) return;
            for (const state of states.values()) state.page = 1;
            for (const target of targets.keys()) refresh(target,true);
        },true);
    }
    globalThis.HrmPagination = {render,scan};
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded',start,{once:true});
    else start();
})();
