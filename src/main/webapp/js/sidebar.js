(function () {
    'use strict';
    const scriptRoot = (() => {
        try {
            const src = document.currentScript?.src || '';
            if (src) return new URL('../', src).pathname.replace(/\/$/, '');
        } catch (_) {}
        const seg = location.pathname.split('/').filter(Boolean);
        return seg.length > 0 && seg[0] === 'HRMs' ? '/HRMs' : '';
    })();
    const key = 'hrms.sidebar.v1';
    const media = window.matchMedia('(max-width: 820px)');
    let saved;
    try { saved = JSON.parse(localStorage.getItem(key)); } catch (_) {}
    let preference = typeof saved?.collapsed === 'boolean' ? saved.collapsed : null;
    let collapsed = media.matches ? true : preference ?? false;
    const closedGroups = new Set(Array.isArray(saved?.closedGroups) ? saved.closedGroups.filter(value => typeof value === 'string') : []);
    document.documentElement.dataset.sidebar = collapsed ? 'collapsed' : 'expanded';
    document.addEventListener('DOMContentLoaded', () => {
        initTopbarNotifications();
        const sidebar = document.querySelector('.lc-sidebar');
        if (!sidebar) return;
        sidebar.id = sidebar.id || 'lc-sidebar';
        const brand = sidebar.querySelector('.lc-brand');
        if (!brand) return;
        const current = new URL(location.href);
        const navigationPath = current.pathname
            .replace(/\/(employee|department|position|responsibility-level)\/\1-(?:detail|form)$/, '/$1/$1-list')
            .replace(/\/employee\/(?:employee-document-list|document-upload)$/, '/employee/employee-list');
        const links = Array.from(sidebar.querySelectorAll('nav a[href]'));
        const matches = links.filter(link => {
            const url = new URL(link.href);
            return url.pathname === navigationPath && Array.from(url.searchParams).every(([key, value]) => current.searchParams.get(key) === value);
        });
        const specificity = Math.max(0, ...matches.map(link => Array.from(new URL(link.href).searchParams).length));
        if (matches.length) links.forEach(link => {
            const active = matches.includes(link) && Array.from(new URL(link.href).searchParams).length === specificity;
            link.classList.toggle('active', active);
            if (active) link.setAttribute('aria-current', 'page');
            else link.removeAttribute('aria-current');
        });
        const activeLink = sidebar.querySelector('nav a.active');
        if (activeLink) {
            const currentGroup = activeLink.closest('[data-nav-group]');
            if (currentGroup) closedGroups.delete(currentGroup.dataset.navGroup);
            const breadcrumb = document.querySelector('.lc-topbar .lc-muted');
            if (breadcrumb) breadcrumb.textContent = '/ ' + (activeLink.querySelector('span:not(.lc-nav-icon)')?.textContent.trim() || activeLink.textContent.trim());
        }
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

    function initTopbarNotifications() {
        const topbar = document.querySelector('.lc-topbar');
        const workspaceBadge = topbar?.querySelector('.lc-workspace');
        if (!topbar || !workspaceBadge || topbar.querySelector('#lc-notif-bell')) return;

        const actionsWrap = document.createElement('div');
        actionsWrap.className = 'lc-topbar-actions';
        workspaceBadge.before(actionsWrap);

        const notifWrap = document.createElement('div');
        notifWrap.className = 'lc-notif-wrap';
        notifWrap.innerHTML = `
            <button type="button" class="lc-notif-bell" id="lc-notif-bell" aria-label="Thông báo" aria-expanded="false" title="Thông báo">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9"/>
                    <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                </svg>
                <span class="lc-notif-badge" id="lc-notif-badge" hidden>0</span>
            </button>
            <div class="lc-notif-dropdown" id="lc-notif-dropdown" hidden>
                <div class="lc-notif-head">
                    <div>
                        <strong>Thông báo</strong>
                        <span class="lc-notif-head-count" id="lc-notif-head-count" hidden>0 mới</span>
                    </div>
                    <button type="button" class="lc-notif-mark-all" id="lc-notif-mark-all">Đánh dấu đã đọc tất cả</button>
                </div>
                <div class="lc-notif-filters" id="lc-notif-filters">
                    <button type="button" class="active" data-notif-filter="ALL">Tất cả</button>
                    <button type="button" data-notif-filter="UNREAD">Chưa đọc</button>
                    <button type="button" data-notif-filter="UPDATE_PERIOD">Đợt cập nhật</button>
                    <button type="button" data-notif-filter="ATTENDANCE">Điểm danh</button>
                    <button type="button" data-notif-filter="LEAVE_PAYROLL">Nghỉ phép &amp; Lương</button>
                </div>
                <div class="lc-notif-list" id="lc-notif-list">
                    <div class="lc-notif-empty">Đang tải thông báo...</div>
                </div>
            </div>
        `;
        actionsWrap.appendChild(notifWrap);
        actionsWrap.appendChild(workspaceBadge);

        const bellBtn = notifWrap.querySelector('#lc-notif-bell');
        const badgeEl = notifWrap.querySelector('#lc-notif-badge');
        const headCountEl = notifWrap.querySelector('#lc-notif-head-count');
        const dropdownEl = notifWrap.querySelector('#lc-notif-dropdown');
        const listEl = notifWrap.querySelector('#lc-notif-list');
        const markAllBtn = notifWrap.querySelector('#lc-notif-mark-all');
        const filterBtns = Array.from(notifWrap.querySelectorAll('[data-notif-filter]'));

        let notifications = [];
        let activeFilter = 'ALL';

        const escapeHtml = val => String(val ?? '').replace(/[&<>"']/g, ch => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
        }[ch]));

        const categoryIcon = cat => {
            if (cat === 'ATTENDANCE') return '◷';
            if (cat === 'LEAVE_PAYROLL') return '▤';
            return '↻';
        };

        const renderList = () => {
            const unreadCount = notifications.filter(n => !n.read).length;
            if (unreadCount > 0) {
                badgeEl.textContent = unreadCount > 99 ? '99+' : String(unreadCount);
                badgeEl.hidden = false;
                headCountEl.textContent = `${unreadCount} chưa đọc`;
                headCountEl.hidden = false;
                markAllBtn.hidden = false;
            } else {
                badgeEl.hidden = true;
                headCountEl.hidden = true;
                markAllBtn.hidden = true;
            }

            let filtered = notifications;
            if (activeFilter === 'UNREAD') {
                filtered = notifications.filter(n => !n.read);
            } else if (activeFilter !== 'ALL') {
                filtered = notifications.filter(n => n.category === activeFilter);
            }

            if (!filtered.length) {
                listEl.innerHTML = '<div class="lc-notif-empty">Không có thông báo nào trong mục này.</div>';
                return;
            }

            listEl.innerHTML = filtered.map(item => `
                <a class="lc-notif-item ${item.read ? '' : 'unread'}" href="${escapeHtml(scriptRoot + (item.link || '/lifecycle/dashboard'))}" data-notif-key="${escapeHtml(item.key)}">
                    <span class="lc-notif-icon" data-cat="${escapeHtml(item.category)}">${categoryIcon(item.category)}</span>
                    <div class="lc-notif-body">
                        <div class="lc-notif-title">${escapeHtml(item.title)}</div>
                        <div class="lc-notif-msg">${escapeHtml(item.message)}</div>
                        ${item.timeLabel ? `<div class="lc-notif-time">${escapeHtml(item.timeLabel)}</div>` : ''}
                    </div>
                    ${item.read ? '' : '<span class="lc-notif-dot" title="Chưa đọc"></span>'}
                </a>
            `).join('');

            listEl.querySelectorAll('.lc-notif-item').forEach(anchor => {
                anchor.addEventListener('click', event => {
                    const notifKey = anchor.dataset.notifKey;
                    const targetHref = anchor.getAttribute('href');
                    if (notifKey) {
                        event.preventDefault();
                        const body = new URLSearchParams({ key: notifKey });
                        fetch(scriptRoot + '/notifications/api/read', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8', 'Accept': 'application/json' },
                            body: body.toString(),
                            credentials: 'same-origin'
                        }).finally(() => {
                            location.href = targetHref;
                        });
                    }
                });
            });
        };

        const loadNotifications = async () => {
            try {
                const res = await fetch(scriptRoot + '/notifications/api', {
                    headers: { 'Accept': 'application/json' },
                    credentials: 'same-origin'
                });
                if (!res.ok) return;
                const payload = await res.json();
                if (payload?.success && payload.data) {
                    notifications = Array.isArray(payload.data.items) ? payload.data.items : [];
                    renderList();
                }
            } catch (_) {
                // Keep page functional if network is temporarily unavailable
            }
        };

        bellBtn.addEventListener('click', event => {
            event.stopPropagation();
            const isOpen = !dropdownEl.hidden;
            dropdownEl.hidden = isOpen;
            bellBtn.setAttribute('aria-expanded', String(!isOpen));
            if (!isOpen) {
                loadNotifications();
            }
        });

        filterBtns.forEach(btn => {
            btn.addEventListener('click', event => {
                event.stopPropagation();
                activeFilter = btn.dataset.notifFilter || 'ALL';
                filterBtns.forEach(b => b.classList.toggle('active', b === btn));
                renderList();
            });
        });

        markAllBtn.addEventListener('click', async event => {
            event.stopPropagation();
            markAllBtn.disabled = true;
            try {
                const body = new URLSearchParams({ markAll: 'true' });
                const res = await fetch(scriptRoot + '/notifications/api/read', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8', 'Accept': 'application/json' },
                    body: body.toString(),
                    credentials: 'same-origin'
                });
                if (res.ok) {
                    const payload = await res.json();
                    if (payload?.success && payload.data) {
                        notifications = Array.isArray(payload.data.items) ? payload.data.items : [];
                        renderList();
                    }
                }
            } catch (_) {
            } finally {
                markAllBtn.disabled = false;
            }
        });

        dropdownEl.addEventListener('click', event => event.stopPropagation());

        document.addEventListener('click', () => {
            if (!dropdownEl.hidden) {
                dropdownEl.hidden = true;
                bellBtn.setAttribute('aria-expanded', 'false');
            }
        });

        document.addEventListener('keydown', event => {
            if (event.key === 'Escape' && !dropdownEl.hidden) {
                dropdownEl.hidden = true;
                bellBtn.setAttribute('aria-expanded', 'false');
                bellBtn.focus();
            }
        });

        loadNotifications();
        setInterval(() => {
            if (!document.hidden) loadNotifications();
        }, 45000);
    }
})();
