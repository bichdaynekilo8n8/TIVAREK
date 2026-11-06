/* Week 23 — client-side progressive enhancement only. Validation is still server-owned. */
(() => {
  'use strict';
  // Delivery is the only fulfillment method that needs an address.
  for (const fulfillment of document.querySelectorAll('[data-w23-fulfillment]')) {
    const form = fulfillment.closest('form');
    if (!form) continue;
    const address = form.querySelector('[data-w23-address]');
    const requiredMarker = form.querySelector('.w23-delivery-required');
    if (!address) continue;
    const synchronize = () => {
      const delivery = fulfillment.value === 'DELIVERY';
      address.required = delivery;
      address.setAttribute('aria-required', String(delivery));
      if (requiredMarker) requiredMarker.hidden = !delivery;
    };
    fulfillment.addEventListener('change', synchronize);
    synchronize();
  }
  const forms = document.querySelectorAll('form[data-w23-submit-lock]');
  for (const form of forms) {
    const submit = form.querySelector('button[type="submit"], input[type="submit"]');
    if (!submit) continue;
    const label = submit.textContent;
    form.addEventListener('submit', (event) => {
      if (form.dataset.w23Submitting === 'true') {
        event.preventDefault();
        return;
      }
      if (!form.checkValidity()) return;
      form.dataset.w23Submitting = 'true';
      // Defer disabling until the browser has accepted the submit event.
      window.setTimeout(() => {
        submit.disabled = true;
        submit.setAttribute('aria-busy', 'true');
        if (submit.tagName === 'BUTTON') submit.textContent = 'Đang gửi, vui lòng chờ…';
      }, 0);
    });
    window.addEventListener('pageshow', () => {
      form.dataset.w23Submitting = 'false';
      submit.disabled = false;
      submit.removeAttribute('aria-busy');
      if (submit.tagName === 'BUTTON') submit.textContent = label;
    });
  }
})();
