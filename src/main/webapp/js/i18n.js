/* The catalogue contains UI copy only. User-entered data is never scanned or rewritten. */
(function (root) {
    'use strict';
    const supported = value => value === 'en' ? 'en' : 'vi';
    const cookieLanguage = () => typeof document === 'undefined' ? 'vi'
        : supported(String(document.cookie || '').split(';').map(value => value.trim()).find(value => value.startsWith('hrms.language='))?.split('=')[1]);
    let language = cookieLanguage();
    const dictionary = root.HrmTranslations || {};
    const templates = Object.entries(dictionary).filter(([source]) => /\{\d+\}/.test(source)).map(([source, translation]) => {
        const fields = [];
        const expression = source.split(/(\{\d+\})/g).map(part => {
            if (/^\{\d+\}$/.test(part)) {fields.push(part); return '([\\s\\S]*?)';}
            return part.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/\s+/g, '\\s+');
        }).join('');
        return {pattern: new RegExp('^'+expression+'$'), fields, translation};
    });
    function t(source) {
        if (source == null || typeof source !== 'string' || language === 'vi') return source;
        const key = source.trim().replace(/\s+/g, ' ');
        let translated = dictionary[key];
        if (translated === undefined) for (const template of templates) {
            const match = source.trim().match(template.pattern);
            if (match) {
                translated = template.translation.replace(/\{\d+\}/g, field => match[template.fields.indexOf(field)+1] ?? field);
                break;
            }
        }
        return translated === undefined ? source : source.slice(0, source.indexOf(source.trim())) + translated + source.slice(source.trimEnd().length);
    }
    function fragment(source) {
        // Called only with static source-code fragments, before interpolating record values.
        return source.split(/(<[^>]*(?:>|$)|^[^<]*>)/g).map(part => {
            if (!(part.startsWith('<') || /^[^<]*>/.test(part))) return t(part);
            const escapeAttribute = value => value.replace(/&/g,'&amp;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
            return part.replace(/\b(placeholder|title|aria-label|alt)=(['"])(.*?)(\2|$)/g,
                (_,name,quote,value,end) => name+'='+quote+escapeAttribute(t(value))+end)
                .replace(/^([^"'<]*)(['"][^>]*>)/, (_,value,tail) => escapeAttribute(t(value))+tail);
        }).join('');
    }
    function label(code) {
        // These are system status codes; record names and free text never enter this map.
        const labels = {Draft:'Bản nháp', Calculated:'Đã tính', Reviewed:'Đã rà soát', Approved:'Đã phê duyệt',
            Active:'Đang làm việc', Inactive:'Ngừng làm việc', 'On Leave':'Nghỉ phép',
            Present:'Đúng giờ', Late:'Đi muộn', 'Early Leave':'Về sớm', Absent:'Vắng mặt'};
        return Object.hasOwn(labels, code) ? t(labels[code]) : t(code);
    }
    function response(payload) {
        if (!payload || typeof payload !== 'object') return payload;
        if (typeof payload.message === 'string') payload.message = t(payload.message);
        if (Array.isArray(payload.errors)) payload.errors = payload.errors.map(t);
        if (payload.fieldErrors && typeof payload.fieldErrors === 'object') {
            payload.fieldErrors = Object.fromEntries(Object.entries(payload.fieldErrors).map(([key,value]) => [key,t(value)]));
        }
        return payload;
    }
    function setLanguage(value) {
        const next = supported(value);
        if (typeof document !== 'undefined') {
            document.cookie = 'hrms.language='+next+'; Path=/; Max-Age=31536000; SameSite=Lax'+(location.protocol === 'https:' ? '; Secure' : '');
            try { localStorage.setItem('hrms.language.v1',next); } catch (_) {}
            if (next !== language) location.reload();
        }
    }
    const api = {t, fragment, label, response, setLanguage, get language(){return language;}, get locale(){return language === 'en' ? 'en-US' : 'vi-VN';}};
    root.HrmI18n = api;
    if (typeof module !== 'undefined') module.exports = api;
    if (typeof document === 'undefined') return;
    document.documentElement.lang = language;
    document.addEventListener('DOMContentLoaded', () => {
        const selector = document.querySelector('#appearance-language');
        if (selector) {
            selector.value = language;
            selector.addEventListener('change', event => setLanguage(event.target.value));
        }
    });
    root.addEventListener('storage', event => {
        if (event.key === 'hrms.language.v1' && supported(event.newValue) !== language) location.reload();
    });
})(globalThis);
