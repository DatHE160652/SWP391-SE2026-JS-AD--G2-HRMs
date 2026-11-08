(function () {
    'use strict';
    const defaults = { accent: '#4f46e5', dark: false, density: 'normal', radius: 14 };
    const accents = ['#4f46e5', '#2563eb', '#0d9488', '#9333ea'];
    const normalize = value => ({
        accent: accents.includes(value?.accent) ? value.accent : defaults.accent,
        dark: value?.dark === true,
        density: ['compact', 'normal', 'comfortable'].includes(value?.density) ? value.density : defaults.density,
        radius: Number.isInteger(value?.radius) && value.radius >= 0 && value.radius <= 24 ? value.radius : defaults.radius
    });
    if (typeof module !== 'undefined') module.exports = { normalize, defaults };
    if (typeof document === 'undefined') return;
    const key = 'hrms.appearance.v1';
    let preferences;
    try { preferences = normalize(JSON.parse(localStorage.getItem(key))); } catch (_) { preferences = normalize(null); }
    const apply = () => {
        const root = document.documentElement;
        root.style.setProperty('--lc-purple', preferences.accent);
        root.style.setProperty('--lc-radius', preferences.radius + 'px');
        root.dataset.theme = preferences.dark ? 'dark' : 'light';
        root.dataset.density = preferences.density;
    };
    apply();
    document.addEventListener('DOMContentLoaded', () => {
        const panel = document.querySelector('#appearance-panel');
        const trigger = document.querySelector('#appearance-trigger');
        if (!panel || !trigger) return;
        const sync = () => {
            panel.querySelectorAll('[data-accent]').forEach(button => button.setAttribute('aria-pressed', String(button.dataset.accent === preferences.accent)));
            panel.querySelectorAll('[data-density]').forEach(button => button.setAttribute('aria-pressed', String(button.dataset.density === preferences.density)));
            panel.querySelector('#appearance-dark').checked = preferences.dark;
            panel.querySelector('#appearance-radius').value = preferences.radius;
            panel.querySelector('output').textContent = preferences.radius + 'px';
        };
        const save = () => {
            apply(); sync();
            try { localStorage.setItem(key, JSON.stringify(preferences)); } catch (_) {}
        };
        const close = () => { panel.hidden = true; trigger.setAttribute('aria-expanded', 'false'); };
        trigger.addEventListener('click', () => {
            panel.hidden = !panel.hidden;
            trigger.setAttribute('aria-expanded', String(!panel.hidden));
            if (!panel.hidden) panel.querySelector('[data-appearance-close]').focus();
        });
        panel.querySelector('[data-appearance-close]').addEventListener('click', () => { close(); trigger.focus(); });
        document.addEventListener('keydown', event => { if (event.key === 'Escape' && !panel.hidden) { close(); trigger.focus(); } });
        document.addEventListener('click', event => { if (!panel.contains(event.target) && !trigger.contains(event.target)) close(); });
        panel.querySelectorAll('[data-accent]').forEach(button => button.addEventListener('click', () => { preferences.accent = button.dataset.accent; save(); }));
        panel.querySelectorAll('[data-density]').forEach(button => button.addEventListener('click', () => { preferences.density = button.dataset.density; save(); }));
        panel.querySelector('#appearance-dark').addEventListener('change', event => { preferences.dark = event.target.checked; save(); });
        panel.querySelector('#appearance-radius').addEventListener('input', event => { preferences.radius = Number(event.target.value); save(); });
        panel.querySelector('[data-appearance-reset]').addEventListener('click', () => { preferences = normalize(null); save(); });
        sync();
    });
})();
