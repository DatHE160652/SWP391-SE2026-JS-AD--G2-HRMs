/* Shared asynchronous application dialogs. Never call browser alert/confirm/prompt. */
(() => {
    let queue = Promise.resolve();
    function show(kind, message, options = {}) {
        const result = queue.then(() => new Promise(resolve => {
            const previous = document.activeElement;
            const dialog = document.createElement('dialog');
            dialog.className = 'app-dialog';
            dialog.setAttribute('aria-labelledby', 'app-dialog-title');
            dialog.setAttribute('aria-describedby', 'app-dialog-message');
            dialog.innerHTML = HrmI18n.fragment('<form><h2 id="app-dialog-title"></h2><p id="app-dialog-message"></p><label hidden for="app-dialog-input">Lý do ngừng làm việc <span aria-hidden="true">*</span></label><textarea hidden id="app-dialog-input" rows="4" placeholder="Nhập lý do ngừng làm việc..."></textarea><div class="app-dialog-actions"><button type="button" data-cancel>Hủy</button><button type="submit" data-submit>Xác nhận</button></div></form>');
            dialog.querySelector('h2').textContent = kind === 'prompt' ? HrmI18n.t('Ngừng làm việc') : kind === 'confirm' ? HrmI18n.t('Xác nhận thao tác') : HrmI18n.t('Thông báo');
            dialog.querySelector('p').textContent = String(message);
            const input = dialog.querySelector('textarea');
            const cancel = dialog.querySelector('[data-cancel]');
            if (kind === 'prompt') {
                input.hidden = false;
                input.required = true;
                input.name = 'reason';
                input.maxLength = 500;
                input.dataset.validationLabel = options.label || HrmI18n.t('lý do ngừng làm việc');
                const label = dialog.querySelector('label');
                label.hidden = false;
                label.textContent = (options.label || HrmI18n.t('Lý do ngừng làm việc')) + ' *';
                dialog.querySelector('h2').textContent = options.title || HrmI18n.t('Ngừng làm việc');
                input.placeholder = options.placeholder || HrmI18n.t('Nhập lý do ngừng làm việc...');
                input.addEventListener('input', () => input.setCustomValidity(''));
            }
            if (kind === 'alert') {
                cancel.hidden = true;
                dialog.querySelector('[data-submit]').textContent = HrmI18n.t('Đóng');
            }
            function finish(value) {
                dialog.close();
                dialog.remove();
                if (previous?.isConnected) previous.focus();
                resolve(value);
            }
            cancel.addEventListener('click', () => finish(kind === 'prompt' ? null : false));
            dialog.addEventListener('cancel', event => {
                event.preventDefault();
                finish(kind === 'prompt' ? null : false);
            });
            dialog.querySelector('form').addEventListener('submit', event => {
                event.preventDefault();
                if (kind === 'prompt' && !input.value.trim()) {
                    input.setCustomValidity(HrmI18n.t('Vui lòng nhập ') + (options.label || HrmI18n.t('lý do ngừng làm việc')).toLocaleLowerCase('vi') + '.');
                    input.reportValidity();
                    return;
                }
                finish(kind === 'prompt' ? input.value.trim() : true);
            });
            document.body.append(dialog);
            dialog.showModal();
            (kind === 'prompt' ? input : kind === 'confirm' ? cancel : dialog.querySelector('[data-submit]')).focus();
        }));
        queue = result.catch(() => {});
        return result;
    }
    window.AppDialog = {
        alert: message => show('alert', message),
        confirm: message => show('confirm', message),
        prompt: (message, options) => show('prompt', message, options)
    };
})();
