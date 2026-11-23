document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.faq-question').forEach((button) => {
    button.addEventListener('click', () => {
      const expanded = button.getAttribute('aria-expanded') === 'true';
      button.setAttribute('aria-expanded', String(!expanded));
      const answer = button.nextElementSibling;
      if (answer) answer.hidden = expanded;
    });
  });

  document.querySelectorAll('.consultation-form').forEach((form) => {
    form.addEventListener('submit', () => {
      const button = form.querySelector('.submit-button');
      if (button && form.checkValidity()) {
        button.disabled = true;
        button.innerHTML = 'Đang lưu yêu cầu...';
      }
    });
  });
});
