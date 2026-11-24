const embeddedModule = new Map([
    ['workspace-v8', 'shop'],
    ['workspace-manage', 'manage'],
    ['workspace-map', 'map'],
]).get(new URLSearchParams(location.search).get('embed'));
if (window.parent !== window && embeddedModule) {
    document.documentElement.classList.add('mcwws-embedded-' + embeddedModule + '-root');
    document.addEventListener('DOMContentLoaded', () => document.body.classList.add('mcwws-embedded-' + embeddedModule), { once: true });
}
