const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const jsPath = path.join(root, 'bluemap/web/js/mcwws-gis.js');
const cssPath = path.join(root, 'bluemap/web/css/mcwws-gis.css');
const settingsPath = path.join(root, 'bluemap/web/settings.json');
const webappPath = path.join(root, 'plugins/BlueMap/webapp.conf');

let js = fs.readFileSync(jsPath, 'utf8');

js = js.replace("const MCWWS_GIS_BUILD = '20260602-23';", "const MCWWS_GIS_BUILD = '20260602-25';");

const oldBlocking = `    function isLayerDialogBlockingMapVertexUi() {
        return layerDialogOpen;
    }

    function shouldShowVertexHandles() {
        return isGisSelectMode() && hasGisSelection() && !isLayerDialogBlockingMapVertexUi();
    }`;

const newBlocking = `    function isPointerOverLayerDialog(clientX, clientY) {
        if (!layerDialogOpen) {
            return false;
        }
        const dialog = document.querySelector('.mcwws-layer-dialog:not([hidden])');
        if (!dialog) {
            return false;
        }
        const rect = dialog.getBoundingClientRect();
        if (!rect.width || !rect.height) {
            return false;
        }
        return clientX >= rect.left
            && clientX <= rect.right
            && clientY >= rect.top
            && clientY <= rect.bottom;
    }

    function shouldShowVertexHandles() {
        return isGisSelectMode() && hasGisSelection();
    }`;

if (!js.includes(oldBlocking)) {
    console.error('blocking block not found');
    process.exit(1);
}
js = js.replace(oldBlocking, newBlocking);

js = js.replace(
    '        if (!shouldShowVertexHandles() || isLayerDialogBlockingMapVertexUi()) {\n            return null;\n        }',
    '        if (!shouldShowVertexHandles()) {\n            return null;\n        }\n        if (isPointerOverLayerDialog(clientX, clientY)) {\n            return null;\n        }'
);

js = js.replace(
    `        if (isLayerDialogBlockingMapVertexUi()) {
            clearGisSelectHover();
            return;
        }
        if (!isGisSelectMode()) {`,
    `        if (isPointerOverLayerDialog(clientX, clientY)) {
            clearGisSelectHover();
            return;
        }
        if (!isGisSelectMode()) {`
);

js = js.replace(
    `        if (event.target?.closest?.('.mcwws-ctrl-gis-wrap, .mcwws-layer-dialog')) {
            return;
        }`,
    `        if (event.target?.closest?.('.mcwws-ctrl-gis-wrap, .mcwws-layer-dialog, .mcwws-map-controls')) {
            return;
        }
        if (isPointerOverLayerDialog(event.clientX, event.clientY)) {
            return;
        }`
);

js = js.replace(
    `        if (isGisSelectMode()) {
            if (isLayerDialogBlockingMapVertexUi()) {
                return;
            }
            const vtx = pickVertexAtScreen(event.clientX, event.clientY);`,
    `        if (isGisSelectMode()) {
            const vtx = isPointerOverLayerDialog(event.clientX, event.clientY)
                ? null
                : pickVertexAtScreen(event.clientX, event.clientY);`
);

const oldRenderDialog = `        if (layerDialogOpen !== layerDialogVertexUiSuppressed) {
            layerDialogVertexUiSuppressed = layerDialogOpen;
            if (layerDialogOpen) {
                hideVertexGizmo();
                clearGisHoverSegmentInsert();
            }
            renderOverlay();
        }

        dialog.innerHTML = `;

const newRenderDialog = `        dialog.innerHTML = `;

if (js.includes(oldRenderDialog)) {
    js = js.replace(oldRenderDialog, newRenderDialog);
} else if (!js.includes('layerDialogVertexUiSuppressed')) {
    // already patched
} else {
    console.error('renderLayerDialog block not found');
    process.exit(1);
}

js = js.replace('    let layerDialogVertexUiSuppressed = false;\n', '');

const oldHandleOff = `                handle.classList.toggle('is-offscreen', off);
                if (!off) {
                    handle.style.transform = \`translate3d(\${projected.x}px, \${projected.y}px, 0) translate(-50%, -50%)\`;
                }`;

const newHandleOff = `                const underDialog = layerDialogOpen
                    && isPointerOverLayerDialog(projected.x, projected.y);
                handle.classList.toggle('is-offscreen', off || underDialog);
                if (!off && !underDialog) {
                    handle.style.transform = \`translate3d(\${projected.x}px, \${projected.y}px, 0) translate(-50%, -50%)\`;
                }`;

if (js.includes(oldHandleOff)) {
    js = js.replace(oldHandleOff, newHandleOff);
}

fs.writeFileSync(jsPath, js, 'utf8');

let css = fs.readFileSync(cssPath, 'utf8');
css = css.replace(
    `body.mcwws-gis-layer-dialog-open #mcwws-gis-vertex-layer,
body.mcwws-gis-layer-dialog-open #mcwws-gis-vertex-gizmo {
    visibility: hidden !important;
    pointer-events: none !important;
    opacity: 0 !important;
}

`,
    ''
);
fs.writeFileSync(cssPath, css, 'utf8');

let settings = fs.readFileSync(settingsPath, 'utf8');
settings = settings
    .replace('mcwws-gis.js?v\\u003d20260602-24', 'mcwws-gis.js?v\\u003d20260602-25')
    .replace('mcwws-gis.css?v\\u003d20260602-16', 'mcwws-gis.css?v\\u003d20260602-17');
fs.writeFileSync(settingsPath, settings, 'utf8');

let webapp = fs.readFileSync(webappPath, 'utf8');
webapp = webapp
    .replace('mcwws-gis.js?v=20260602-24', 'mcwws-gis.js?v=20260602-25')
    .replace('mcwws-gis.css?v=20260602-16', 'mcwws-gis.css?v=20260602-17');
fs.writeFileSync(webappPath, webapp, 'utf8');

console.log('patched ok');
