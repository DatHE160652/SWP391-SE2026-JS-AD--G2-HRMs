(function () {
    'use strict';
    const key = 'hrms.sidebar.v1';
    const media = window.matchMedia('(max-width: 820px)');
    let saved;
    try { saved = JSON.parse(localStorage.getItem(key)); } catch (_) {}
    let preference = typeof saved?.collapsed === 'boolean' ? saved.collapsed : null;
    let collapsed = media.matches ? true : preference ?? false;
    const closedGroups = new Set(Array.isArray(saved?.closedGroups) ? saved.closedGroups.filter(value => typeof value === 'string') : []);
    document.documentElement.dataset.sidebar = collapsed ? 'collapsed' : 'expanded';
    document.addEventListener('DOMContentLoaded', () => {
        const sidebar = document.querySelector('.lc-sidebar');
        if (!sidebar) return;
        sidebar.id = sidebar.id || 'lc-sidebar';
        const brand = sidebar.querySelector('.lc-brand');
        if (!brand) return;
        const header = document.createElement('div');
        header.className = 'lc-sidebar-header';
        brand.before(header);
        header.append(brand);
        const toggle = document.createElement('button');
        toggle.type = 'button';
        toggle.id = 'lc-sidebar-toggle';
        toggle.setAttribute('aria-controls', sidebar.id);
        toggle.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><path d="m14 6-6 6 6 6"/></svg>';
        header.append(toggle);
        const backdrop = document.createElement('button');
        backdrop.type = 'button';
        backdrop.id = 'lc-sidebar-backdrop';
        backdrop.setAttribute('aria-label', HrmI18n.t('Đóng menu điều hướng'));
        backdrop.tabIndex = -1;
        document.body.append(backdrop);
        const groups = Array.from(sidebar.querySelectorAll('[data-nav-group]'));
        sidebar.querySelectorAll('nav a, .lc-signout').forEach(link => {
            const label = link.querySelector('span:not(.lc-nav-icon)')?.textContent.trim() || link.textContent.trim();
            link.title = label;
            link.setAttribute('aria-label', label);
        });
        const apply = () => {
            document.documentElement.dataset.sidebar = collapsed ? 'collapsed' : 'expanded';
            toggle.setAttribute('aria-expanded', String(!collapsed));
            toggle.setAttribute('aria-label', collapsed ? HrmI18n.t('Mở rộng menu') : HrmI18n.t('Thu gọn menu'));
            toggle.title = collapsed ? HrmI18n.t('Mở rộng menu') : HrmI18n.t('Thu gọn menu');
            backdrop.hidden = collapsed || !media.matches;
            groups.forEach(group => {
                const button = group.querySelector('.lc-group-toggle');
                const links = group.querySelector('.lc-nav-links');
                links.hidden = !collapsed && closedGroups.has(group.dataset.navGroup);
                button.setAttribute('aria-expanded', String(!links.hidden));
            });
        };
        const persist = () => {
            try { localStorage.setItem(key, JSON.stringify({collapsed:preference, closedGroups:[...closedGroups]})); } catch (_) {}
        };
        const setCollapsed = value => { collapsed = value; preference = value; apply(); persist(); };
        toggle.addEventListener('click', () => setCollapsed(!collapsed));
        backdrop.addEventListener('click', () => { setCollapsed(true); toggle.focus(); });
        sidebar.addEventListener('click', event => {
            if (media.matches && event.target.closest('nav a')) setCollapsed(true);
        });
        groups.forEach(group => group.querySelector('.lc-group-toggle').addEventListener('click', () => {
            const name = group.dataset.navGroup;
            if (closedGroups.has(name)) closedGroups.delete(name); else closedGroups.add(name);
            apply(); persist();
        }));
        document.addEventListener('keydown', event => {
            if (event.key === 'Escape' && media.matches && !collapsed) { setCollapsed(true); toggle.focus(); }
        });
        media.addEventListener('change', () => { collapsed = media.matches ? true : preference ?? false; apply(); });
        apply();
    });
})();
