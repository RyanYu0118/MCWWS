const services = ['shop', 'map', 'manage'];
const workspace = document.querySelector('.service-workspace');
const track = workspace.querySelector('.service-track');
const panels = [...track.querySelectorAll('iframe')];
const dockLinks = [...document.querySelectorAll('.dock [data-service]')];
const homeLink = document.querySelector('.wordmark[data-service="home"]');
const main = document.querySelector('main');
const shopPanel = panels[0];
const mapPanel = panels[1];
const shopNav = document.querySelector('.shop-workspace-nav');
const shopActions = document.querySelector('.shop-workspace-actions');
const shopStatus = shopActions.querySelector('[data-shop-status]');
const cartCount = shopActions.querySelector('[data-shop-cart-count]');
const accountButton = document.getElementById('authButton');
const brand = document.querySelector('[data-workspace-brand]');
const lightControls = document.querySelector('.top-inner [aria-label="光感强度"]');
const threshold = document.querySelector('.top-inner .threshold');
const header = document.querySelector('.top');
const dock = document.querySelector('.dock');
let shopObserver;
let active;

function syncShopInsets() {
    const panel = panels[services.indexOf(active)];
    const root = panel?.contentDocument?.documentElement;
    if (!root) return;
    const top = header.hidden ? 0 : Math.ceil(header.getBoundingClientRect().bottom) + 8;
    const bottom = dock.getClientRects().length ? Math.ceil(innerHeight - dock.getBoundingClientRect().top) + 8 : 0;
    root.style.setProperty('--shop-header-space', `${top}px`);
    root.style.setProperty('--shop-dock-space', `${bottom}px`);
    if (panel === mapPanel) {
        const frame = panel.contentDocument.getElementById('bluemapFrame');
        if (frame?.getAttribute('src')) frame.contentWindow?.postMessage({ type: 'mcwws-workspace-insets', top, bottom }, new URL(frame.src).origin);
    }
}

const overlayResize = new ResizeObserver(syncShopInsets);
overlayResize.observe(header);
overlayResize.observe(dock);
window.addEventListener('resize', syncShopInsets);

function styleEmbeddedPanel(panel, name) {
    const doc = panel.contentDocument;
    if (!doc?.body) return;
    if (!doc.getElementById('mcwws-embedded-shop-style')) {
        const style = doc.createElement('link');
        style.id = 'mcwws-embedded-shop-style';
        style.rel = 'stylesheet';
        style.href = new URL('services-workspace.css?v=10', location.href);
        doc.head.append(style);
    }
    doc.body.classList.add('mcwws-embedded-' + name);
    doc.documentElement.classList.add('mcwws-embedded-' + name + '-root');
    const mapFrame = doc.getElementById('bluemapFrame');
    mapFrame?.addEventListener('load', syncShopInsets);
    syncShopInsets();
}

function activePanel() { return panels[services.indexOf(active)]; }


function syncShopHeader() {
    shopObserver?.disconnect();
    const doc = activePanel()?.contentDocument;
    const nav = doc?.querySelector('.navbar');
    if (!nav) {
        for (const button of shopActions.querySelectorAll('button')) button.disabled = true;
        shopStatus.textContent = '';
        cartCount.hidden = true;
        return;
    }
    const dashboard = active === 'shop' && doc.location.pathname.endsWith('/index.html');
    for (const link of shopNav.querySelectorAll('[data-shop-view]')) {
        if (link.dataset.shopView === (active === 'shop' ? (dashboard ? 'dashboard' : 'catalog') : active)) link.setAttribute('aria-current', 'page');
        else link.removeAttribute('aria-current');
    }
    for (const button of shopActions.querySelectorAll('button')) {
        button.hidden = active !== 'shop' || dashboard && button.dataset.shopAction !== 'refresh';
        button.disabled = false;
    }
    const updateStatus = () => {
        shopStatus.textContent = active === 'shop' ? nav?.querySelector('.item-count, .last-updated')?.textContent.trim() || '' : '';
        const badge = nav?.querySelector('#cartBadge');
        cartCount.hidden = !badge || badge.hidden;
        cartCount.textContent = badge?.textContent || '';
    };
    updateStatus();
    shopObserver = new MutationObserver(updateStatus);
    shopObserver.observe(nav, { subtree: true, childList: true, characterData: true, attributes: true, attributeFilter: ['hidden', 'src', 'style', 'aria-expanded'] });
}

panels.forEach((panel, index) => panel.addEventListener('load', () => {
    styleEmbeddedPanel(panel, services[index]);
    if (panel === activePanel()) syncShopHeader();
}));
shopNav.addEventListener('click', event => {
    const link = event.target.closest('[data-shop-view]');
    if (!link || event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    if (services.includes(link.dataset.shopView)) activate(link.dataset.shopView, true);
    else if (!link.hasAttribute('aria-current') || !shopPanel.contentDocument?.querySelector('.navbar')) {
        for (const button of shopActions.querySelectorAll('button')) button.disabled = true;
        const url = new URL(link.href);
        url.searchParams.set('embed', 'workspace-v8');
        url.searchParams.set('glass', '8');
        shopPanel.src = url.href;
    }
});

const actionSelectors = { refresh: '.navbar .refresh-btn', cart: '#cartToggleBtn' };
function closeShopAccount() {
    const widget = document.getElementById('mcwwsAuthWidget');
    widget?.classList.remove('is-popover-open', 'is-popover-hover');
    document.getElementById('authPopover')?.setAttribute('aria-hidden', 'true');
    accountButton.setAttribute('aria-expanded', 'false');
}
shopActions.addEventListener('click', event => {
    const action = event.target.closest('[data-shop-action]')?.dataset.shopAction;
    if (action) activePanel()?.contentDocument?.querySelector(actionSelectors[action])?.click();
});

function activate(service, updateHistory = false) {
    const index = services.indexOf(service);
    const next = index < 0 ? 'home' : service;
    if (next === active) return;
    if (updateHistory) {
        const url = new URL(location.href);
        if (index < 0) url.searchParams.delete('service');
        else url.searchParams.set('service', next);
        url.hash = '';
        history.pushState(null, '', url);
    }
    const animate = !matchMedia('(prefers-reduced-motion: reduce)').matches;
    const returningHome = animate && next === 'home' && active && active !== 'home';
    const enteringService = animate && active === 'home' && index >= 0;
    closeShopAccount();
    active = next;
    document.body.dataset.activeService = next;
    const inShop = next === 'shop';
    workspace.classList.toggle('is-shop-workspace', index >= 0);
    header.hidden = next === 'map';
    brand.textContent = ({ shop: '流浪世界游戏商店系统', manage: '流浪世界服务管理系统' })[next] || '流浪世界';
    lightControls.hidden = index >= 0;
    threshold.hidden = index >= 0;
    shopNav.hidden = index < 0 || next === 'map';
    shopActions.hidden = shopNav.hidden;
    for (const link of shopNav.querySelectorAll('[data-shop-view]')) {
        const view = link.dataset.shopView;
        link.hidden = view === 'catalog' || view === 'dashboard' ? !inShop : view !== next;
    }
    for (const button of shopActions.querySelectorAll('button')) {
        button.hidden = !inShop;
    }
    if (index >= 0 && !panels[index].hasAttribute('src')) {
        panels[index].src = panels[index].dataset.serviceSrc;
    }
    if (index >= 0) track.style.setProperty('--service-offset', `${-index * 100}%`);
    panels.forEach((panel, panelIndex) => {
        panel.inert = panelIndex !== index;
        panel.setAttribute('aria-hidden', String(panel.inert));
    });
    for (const link of [homeLink, ...dockLinks]) {
        if (link.dataset.service === next) link.setAttribute('aria-current', 'page');
        else link.removeAttribute('aria-current');
    }
    main.inert = index >= 0;
    workspace.hidden = index < 0 && !returningHome;
    workspace.classList.toggle('is-returning-home', Boolean(returningHome));
    workspace.classList.toggle('is-entering-service', enteringService);
    workspace.inert = index < 0;
    main.classList.toggle('is-returning-home', Boolean(returningHome));
    document.body.toggleAttribute('data-service-open', index >= 0);
    syncShopHeader();
    syncShopInsets();
}

for (const link of [homeLink, ...dockLinks]) {
    link.addEventListener('click', event => {
        if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
        event.preventDefault();
        activate(link.dataset.service, true);
    });
}

function syncFromUrl() {
    activate(new URLSearchParams(location.search).get('service'));
}

if (new URLSearchParams(location.search).get('shell') === 'android') {
    document.body.setAttribute('data-native-service-nav', '');
}
window.MCWWS_SERVICES_WORKSPACE = { open: service => activate(service, true) };
window.addEventListener('popstate', syncFromUrl);
workspace.addEventListener('animationend', event => {
    if (event.target !== workspace) return;
    if (active === 'home') workspace.hidden = true;
    workspace.classList.remove('is-entering-service');
});
syncFromUrl();
