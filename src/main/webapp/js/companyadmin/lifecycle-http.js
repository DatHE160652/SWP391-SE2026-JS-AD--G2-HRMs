(function (root) {
    'use strict';
    function formBody(values, csrfToken) {
        const containsFiles = values instanceof FormData && Array.from(values.values()).some(value => typeof value !== 'string' && (value.size > 0 || value.name));
        const body = containsFiles ? values : new URLSearchParams(values instanceof FormData
            ? Array.from(values.entries()).filter(([,value]) => typeof value === 'string') : values);
        body.set('csrfToken', csrfToken);
        return body;
    }
    const helpers = {formBody};
    if (typeof module !== 'undefined' && module.exports) module.exports = helpers;
    else root.HrmLifecycleHttp = helpers;
})(globalThis);
