const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const { JSDOM } = require('jsdom');

const script = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/js/form-validation.js'), 'utf8');
const stylesheet = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/css/form-validation.css'), 'utf8');
const employeeScript = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/js/employee.js'), 'utf8');

function page(markup) {
    const dom = new JSDOM(`<html><head><style>${stylesheet}</style></head><body>${markup}</body></html>`, {
        runScripts: 'outside-only', url: 'http://localhost:8081/HRMs/test'
    });
    dom.window.eval(script);
    dom.window.document.dispatchEvent(new dom.window.Event('DOMContentLoaded'));
    return dom.window;
}

test('required profile fields show red italic text and block the submit handler', () => {
    const window = page('<form><input name="fullName" required><input name="phone" required></form>');
    const form = window.document.querySelector('form');
    let saves = 0;
    form.addEventListener('submit', () => saves++);
    const submission = new window.Event('submit', { bubbles: true, cancelable: true });
    form.dispatchEvent(submission);
    assert.equal(form.noValidate, true);
    assert.equal(saves, 0);
    assert.equal(submission.defaultPrevented, true);
    const errors = form.querySelectorAll('.field-error');
    assert.equal(errors.length, 2);
    errors.forEach(error => {
        assert.ok(error.textContent);
        assert.equal(window.getComputedStyle(error).fontStyle, 'italic');
        assert.equal(window.getComputedStyle(error).color, 'rgb(180, 35, 24)');
    });
    assert.equal(window.document.activeElement.name, 'fullName');
    window.close();
});

test('valid submissions keep existing handlers and readonly/disabled values do not block', () => {
    const window = page('<form><input name="fullName" value="Name" required><input readonly required><input disabled required></form>');
    let saves = 0;
    const form = window.document.querySelector('form');
    form.addEventListener('submit', () => saves++);
    form.dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }));
    assert.equal(saves, 1);
    window.close();
});

test('radio groups share one error below the group and clear on selection', () => {
    const window = page('<form><div class="radio-row"><label><input type="radio" name="gender" value="Male" required>Male</label><label><input type="radio" name="gender" value="Female">Female</label></div></form>');
    const form = window.document.querySelector('form');
    assert.equal(window.FormValidation.validate(form), false);
    assert.equal(form.querySelectorAll('.field-error').length, 1);
    const selected = form.querySelector('[value="Female"]');
    selected.checked = true;
    selected.dispatchEvent(new window.Event('change', { bubbles: true }));
    assert.equal(form.querySelector('.field-error').textContent, '');
    window.close();
});

test('CCCD, email, numeric ranges and whitespace are validated inline', () => {
    const window = page('<form><input name="identityNumber" value="00123456789A"><input name="email" type="email" value="bad"><input name="defaultDaysPerYear" type="number" min="0" max="365" value="366"><input name="fullName" required value="   "></form>');
    const form = window.document.querySelector('form');
    assert.equal(window.FormValidation.validate(form), false);
    assert.equal(form.querySelectorAll('[aria-invalid="true"]').length, 4);
    const identity = form.elements.namedItem('identityNumber');
    identity.value = '001234567890';
    identity.dispatchEvent(new window.Event('input', { bubbles: true }));
    assert.equal(identity.getAttribute('aria-invalid'), 'false');
    assert.equal(identity.value, '001234567890');
    window.close();
});

test('server errors stay in the correct form without interpreting HTML', () => {
    const window = page('<form id="first"><input name="phone"></form><form id="second"><input name="phone"></form>');
    const form = window.document.getElementById('second');
    const failure = window.FormValidation.responseError({ message: 'Invalid', fieldErrors: { phone: '<b>Duplicate phone</b>' } });
    assert.equal(window.FormValidation.showErrors(form, failure), true);
    assert.equal(form.querySelector('.field-error').textContent, '<b>Duplicate phone</b>');
    assert.equal(form.querySelector('.field-error b'), null);
    assert.equal(window.document.getElementById('first').querySelector('.field-error').textContent, '');
    assert.equal(window.FormValidation.showErrors(form, new Error('Network error')), false);
    window.close();
});

test('existing server-rendered field messages are reused', () => {
    const window = page('<form><div><input name="email"><p class="field-error">Server error</p></div></form>');
    const form = window.document.querySelector('form');
    window.FormValidation.init(form);
    assert.equal(form.querySelectorAll('.field-error').length, 1);
    assert.equal(form.querySelector('.field-error').textContent, 'Server error');
    assert.equal(form.elements.namedItem('email').getAttribute('aria-invalid'), 'true');
    window.close();
});

test('date order, password confirmation and standalone modal inputs use field errors', () => {
    const window = page('<form><input name="startDate" type="date" value="2026-10-20"><input name="endDate" type="date" value="2026-10-19"><input name="newPassword" value="abc"><input name="confirmPassword" value="xyz"></form><textarea id="rejectComment" required></textarea>');
    const form = window.document.querySelector('form');
    assert.equal(window.FormValidation.validate(form), false);
    assert.equal(window.FormValidation.validateField(window.document.getElementById('rejectComment')), false);
    assert.equal(form.elements.namedItem('endDate').getAttribute('aria-invalid'), 'true');
    assert.equal(form.elements.namedItem('confirmPassword').getAttribute('aria-invalid'), 'true');
    assert.ok(window.document.getElementById('rejectComment').nextElementSibling.textContent);
    window.close();
});

test('native validation popups are prevented and dynamic forms are supported', () => {
    const window = page('<main></main>');
    window.document.querySelector('main').innerHTML = '<form><input name="title" required></form>';
    const form = window.document.querySelector('form');
    form.dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }));
    assert.equal(form.noValidate, true);
    const invalid = new window.Event('invalid', { cancelable: true });
    form.elements.namedItem('title').dispatchEvent(invalid);
    assert.equal(invalid.defaultPrevented, true);
    assert.ok(form.querySelector('.field-error').textContent);
    window.close();
});

function profilePage() {
    const window = page('<div id="message"></div><div id="profileName"></div><div id="profileCode"></div>'
        + '<form id="myProfileForm"><input name="csrfToken" type="hidden"><p id="profileUpdateStatus"></p>'
        + '<input name="employeeCode" readonly><input name="fullName" readonly><input name="phone" readonly>'
        + '<input name="dateOfBirth" readonly><input name="status" readonly><input type="radio" name="gender" value="Male" disabled>'
        + '<button id="editProfileButton" type="button"></button><button id="submitProfileButton"></button>'
        + '<button id="discardProfileButton" type="button"></button></form>'
        + '<div id="employmentTab"></div><div id="salaryTab"></div><div id="accountTab"></div>');
    const form = window.document.getElementById('myProfileForm');
    Array.from(form.elements).filter(field => field.name).forEach(field => {
        Object.defineProperty(form, field.name, { value: field, configurable: true });
    });
    window.eval(employeeScript);
    return window;
}

function profileData(available) {
    return { employeeId: 1, employeeCode: 'EMP-001', fullName: 'Protected Name', phone: '0901234567',
        dateOfBirth: '2000-01-01', gender: 'Male', status: 'Active', csrfToken: 'test-token',
        canUpdatePhone: available, profileUpdatePeriod: available
            ? { periodId: 1, title: 'Phone update', endsAt: new Date(Date.now() + 60000).toISOString() } : null };
}

test('profile stays locked without an open period even if edit is invoked directly', () => {
    const window = profilePage();
    window.renderMyProfile(profileData(false));
    window.setProfileEditMode(true);
    const form = window.document.getElementById('myProfileForm');
    assert.equal(form.phone.readOnly, true);
    assert.equal(form.fullName.readOnly, true);
    assert.equal(form.dateOfBirth.readOnly, true);
    assert.equal(window.document.getElementById('editProfileButton').disabled, true);
    window.close();
});

test('an open period unlocks only phone and expiry locks it again', () => {
    const window = profilePage();
    window.renderMyProfile(profileData(true));
    window.setProfileEditMode(true);
    const form = window.document.getElementById('myProfileForm');
    assert.equal(form.phone.readOnly, false);
    assert.equal(form.fullName.readOnly, true);
    assert.equal(form.dateOfBirth.readOnly, true);
    assert.equal(form.querySelector('[name="gender"]').disabled, true);
    form.dataset.updateDeadline = new Date(Date.now() - 1000).toISOString();
    window.scheduleProfileUpdateExpiry(form);
    assert.equal(form.phone.readOnly, true);
    assert.equal(window.document.getElementById('submitProfileButton').classList.contains('hidden'), true);
    window.close();
});

test('profile save submits only phone and CSRF even if protected inputs are tampered with', async () => {
    const window = profilePage();
    window.renderMyProfile(profileData(true));
    const requests = [];
    window.fetch = async (url, options) => {
        if (options.method === 'POST') {
            requests.push(Object.fromEntries(options.body.entries()));
        }
        return { ok: true, status: 200, json: async () => ({ success: true, message: 'Saved', data: profileData(true) }) };
    };
    const form = window.document.getElementById('myProfileForm');
    form.fullName.value = 'Tampered';
    form.phone.value = '0902000001';
    await window.updateMyProfile(form);
    assert.deepEqual(requests, [{ phone: '0902000001', csrfToken: 'test-token' }]);
    window.close();
});

test('a period closed by the administrator locks the profile after server rejection', async () => {
    const window = profilePage();
    window.renderMyProfile(profileData(true));
    window.setProfileEditMode(true);
    window.fetch = async () => ({ ok: false, status: 403,
        json: async () => ({ success: false, message: 'Period closed' }) });
    const form = window.document.getElementById('myProfileForm');
    await window.updateMyProfile(form);
    assert.equal(form.phone.readOnly, true);
    assert.equal(window.document.getElementById('editProfileButton').disabled, true);
    assert.equal(window.document.getElementById('profileUpdateStatus').textContent, 'Period closed');
    window.close();
});
