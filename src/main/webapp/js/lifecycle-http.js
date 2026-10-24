(function (root) {
    'use strict';
    function formBody(values, csrfToken) {
        const containsFiles = values instanceof FormData && Array.from(values.values()).some(value => typeof value !== 'string');
        const body = containsFiles ? values : new URLSearchParams(values);
        body.set('csrfToken', csrfToken);
        return body;
    }
    const helpers = {formBody};
    if (typeof module !== 'undefined' && module.exports) module.exports = helpers;
    else root.HrmLifecycleHttp = helpers;
})(globalThis);
