// One account control for every service, using the existing shared auth API.
const account = window.MCWWS_AUTH;
const button = document.getElementById('authButton');
const modal = document.getElementById('authModal');
if (account && button && modal) {
    const refreshPanels = () => {
        for (const panel of document.querySelectorAll('.service-track > iframe[src]')) {
            panel.contentWindow?.MCWWS_AUTH?.refresh();
        }
    };
    account.onChange(refreshPanels);
    window.addEventListener('storage', event => {
        if (event.key === account.TOKEN_KEY) void account.refresh();
    });
    void account.init();
    let opened = false;
    new MutationObserver(() => {
        const visible = modal.classList.contains('active');
        if (visible === opened) return;
        opened = visible;
        if (visible) document.getElementById('authUsername')?.focus();
        else button.focus({ preventScroll: true });
    }).observe(modal, { attributes: true, attributeFilter: ['class'] });
    document.addEventListener('keydown', event => {
        if (!opened) return;
        if (event.key === 'Escape') { account.closeModal(); return; }
        if (event.key !== 'Tab') return;
        const controls = [...modal.querySelectorAll('button, input, a[href]')].filter(el => !el.disabled && el.getClientRects().length);
        const first = controls[0], last = controls.at(-1);
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
    });
}
