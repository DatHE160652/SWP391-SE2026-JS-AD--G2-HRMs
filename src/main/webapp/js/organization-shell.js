'use strict';
document.querySelectorAll('.lc-sidebar nav a').forEach(link => {
    const section=location.pathname.split('/').slice(-2,-1)[0];
    link.classList.toggle('active',link.pathname.split('/').slice(-2,-1)[0]===(section==='document'?'employee':section));
});
