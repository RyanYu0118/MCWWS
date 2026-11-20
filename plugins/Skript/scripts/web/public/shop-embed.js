const embeddedModule = { 'workspace-v8': 'shop', 'workspace-manage': 'manage' }[new URLSearchParams(location.search).get('embed')];
if (window.parent !== window && (embeddedModule === 'shop' || embeddedModule === 'manage')) {
    document.documentElement.classList.add('mcwws-embedded-' + embeddedModule + '-root');
    document.addEventListener('DOMContentLoaded', () => document.body.classList.add('mcwws-embedded-' + embeddedModule), { once: true });
}
