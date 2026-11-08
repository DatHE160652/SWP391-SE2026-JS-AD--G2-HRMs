/* Personal payslip badge on existing tenant pages, including dynamic sidebars. */
(() => {
    'use strict';
    const root=new URL('../',document.currentScript.src).pathname.replace(/\/$/,'');
    let unread=0;
    function badge() {
        document.querySelectorAll('a[href]').forEach(link=>{
            if(new URL(link.href,location.href).pathname!==root+'/payslips') return;
            let count=link.querySelector('.payslip-count');
            if(!count) { count=document.createElement('span');count.className='payslip-count';count.style.cssText='margin-left:8px;border-radius:12px;padding:2px 7px;background:#dce8ff;color:#2357b0;font-size:11px';link.append(count); }
            const label=unread?String(unread):'';
            if(count.textContent!==label) count.textContent=label;
            count.hidden=!unread;
            link.title=unread?`${unread}${HrmI18n.fragment(" phiếu lương mới chưa đọc")}`:HrmI18n.t('Phiếu lương của tôi');
        });
    }
    async function refresh() {
        try {
            const response=await fetch(root+'/payslips/api/notifications',{headers:{Accept:'application/json'},credentials:'same-origin'});
            if(!response.ok) return;
            const payload=await response.json().then(HrmI18n.response);
            if(payload.success) { unread=payload.data.filter(item=>!item.readAt).length;badge(); }
        } catch (_) { /* Other tenant features remain usable during a network outage. */ }
    }
    const sidebar=document.querySelector('.sidebar');
    if(sidebar) new MutationObserver(badge).observe(sidebar,{childList:true,subtree:true});
    refresh();setInterval(()=>{if(!document.hidden) refresh();},60000);
})();
