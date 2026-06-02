(function () {
    const API_PORT = 8002;
    const NODE_API = `${window.location.protocol}//${window.location.hostname}:${API_PORT}`;
    console.info('[mcwws-gis] loaded', { ts: '2026-06-02T17:45+08:00' });
    const GIS_WRAP_ID = 'mcwws-gis-wrap';
    const MAP_CONTROLS_STACK_SEL = '.mcwws-map-controls-stack';
    const SVG_LAYER_ID = 'mcwws-gis-svg-layer';
    const PIN_LAYER_ID = 'mcwws-gis-pin-layer';
    const VERTEX_LAYER_ID = 'mcwws-gis-vertex-layer';
    const VERTEX_GIZMO_ID = 'mcwws-gis-vertex-gizmo';
    const LASSO_LAYER_ID = 'mcwws-gis-lasso-layer';
    const GIS_LASSO_POINT_MIN_DIST_PX = 5;
    const GIS_LASSO_MIN_POINTS = 4;
    const GIS_LASSO_MIN_DIAG_PX = 12;
    const GIS_DEFAULT_Y = 64;
    const GIS_CLIP_W_EPS = 1e-4;
    const GIS_NDC_LIMIT = 1.001;
    const GIS_SCREEN_CLIP_PAD = 8000;
    const GIS_CHAIN_JOIN_EPS = 6;
    const GIS_DRAG_THRESHOLD_PX = 8;
    const GIS_SELECT_DRAG_THRESHOLD_PX = 14;
    const GIS_SELECT_HIT_PX = 16;
    const GIS_SELECT_HIT_PX_3D = 22;
    const GIS_VERTEX_HIT_PX = 14;
    const GIS_SEGMENT_HIT_PX = 14;
    const GIS_SEGMENT_MIN_SCREEN_LEN = 10;
    const GIS_SEGMENT_VERTEX_CLEAR_PX = 10;
    const GIS_AXIS_DRAG_THRESHOLD_PX = 3;
    const GIS_GIZMO_AXES_SIZE = 80;
    const GIS_GIZMO_HUB = GIS_GIZMO_AXES_SIZE / 2;
    const GIS_GIZMO_AXIS_WORLD_SPAN = 4;
    const GIS_GIZMO_AXIS_MIN_WIDTH_PX = 42;
    const GIS_GIZMO_AXIS_MAX_WIDTH_PX = 72;
    const GIS_GIZMO_COORDS_OFFSET_Y = 34;
    const GIS_HISTORY_MAX = 100;
    const GIS_ROAD_DUAL_DEFAULT_SPLIT_HEIGHT = 80;
    const GIS_ROAD_DUAL_DEFAULT_LANE_OFFSET = 3;
    const GIS_ROAD_MAX_LANES_PER_SIDE = 6;
    const GIS_LANE_ARROW_SPACING_PX = 88;
    /** 指向 +X 的实心箭头（右箭头）；左车道通过 rotate(180°) 得到左箭头 */
    const GIS_LANE_ARROW_SHAPE_D = 'M -6 -3.5 L 2 0 L -6 3.5 L -3.5 0 Z';

    let mapAuthToken = null;
    let mapAuthUser = null;
    let gisCanEdit = false;
    let gisEditMode = false;
    let activeTool = 'select';
    let activeLayerId = 'roads';
    let project = null;
    /** @type {Set<string>} */
    const selectedFeatureIds = new Set();
    let draftPoints = [];
    let draftHover = null;
    let layerDialogOpen = false;
    let mapRenderMode = 'original';
    let gisInfoEnabled = true;
    let gisEditorOpen = false;
    let gisControlsBound = false;
    const STORAGE_RENDER_MODE = 'mcwws-map-render-mode';
    const STORAGE_GIS_ENABLED = 'mcwws-gis-info-enabled';
    let dirty = false;
    let saving = false;
    let statusMessage = '';
    let statusKind = '';
    let animationId = 0;
    let started = false;
    let mapClickBound = false;
    let canvasClickBound = false;
    let pinElements = new Map();
    /** @type {Map<string, SVGPathElement>} */
    const svgPathElements = new Map();
    /** @type {Map<string, SVGGElement>} */
    const svgLaneArrowGroups = new Map();
    /** @type {SVGPathElement | null} */
    let svgDraftPathEl = null;
    let gisHoverFeatureId = null;
    let lastPickAt = 0;
    let lastPickKey = '';
    let gisCachedCamera = null;
    /** @type {{ startX: number, startY: number, moved: boolean, pointerId: number } | null} */
    let gisCanvasPointer = null;
    let gisLastMapDragAt = 0;
    /** @type {Array<{ project: object, selectedFeatureIds: string[], activeLayerId: string }>} */
    let gisUndoStack = [];
    /** @type {Array<{ project: object, selectedFeatureIds: string[], activeLayerId: string }>} */
    let gisRedoStack = [];
    let gisHistoryApplying = false;
    /** @type {Set<string>} 特征点多选，键为 featureId:vertexIndex */
    const selectedVertices = new Set();
    /** @type {{ axis: string, featureId: string, vertexIndex: number, startWorld: object, startClientX: number, startClientY: number, pointerId: number, historyRecorded: boolean, moved: boolean, screenAxis: object | null, cleanup: (() => void) | null } | null} */
    let gisVertexDrag = null;
    /** @type {Map<string, HTMLButtonElement>} */
    const vertexHandleElements = new Map();
    /** @type {{ featureId: string, segmentIndex: number, insertIndex: number, world: object, screenX: number, screenY: number, clientX: number, clientY: number } | null} */
    let gisHoverSegmentInsert = null;
    let segmentInsertHandleEl = null;
    let gisVertexGizmoEl = null;
    let gisVertexGizmoBound = false;
    let gisVertexCoordHistoryPending = false;
    /** @type {{ points: { x: number, y: number }[], pointerId: number, captureEl: Element | null } | null } */
    let gisLassoPointer = null;
    /** @type {SVGPathElement | null} */
    let gisLassoPathEl = null;
    let gisLassoCaptureBound = false;

    const TOOLS = [
        { id: 'select', label: '选择', icon: '↖' },
        { id: 'point', label: '点', icon: '📍' },
        { id: 'line', label: '道路', icon: '〰' },
        { id: 'polygon', label: '区域', icon: '▢' },
        { id: 'label', label: '标注', icon: '🏷' }
    ];

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function newFeatureId() {
        if (typeof crypto !== 'undefined' && crypto.randomUUID) {
            return crypto.randomUUID().replace(/-/g, '').slice(0, 16);
        }
        return `f_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
    }

    function authHeaders() {
        if (!mapAuthToken) return {};
        return { Authorization: `Bearer ${mapAuthToken}` };
    }

    function setStatus(msg, kind) {
        statusMessage = msg || '';
        statusKind = kind || '';
        renderPanel();
    }

    function getBlueMapApp() {
        const app = document.getElementById('app');
        return app?.__vue_app__?.config?.globalProperties?.$bluemap
            || app?.__vueParentComponent?.appContext?.config?.globalProperties?.$bluemap
            || null;
    }

    function getControlsManager() {
        return getBlueMapApp()?.mapViewer?.controlsManager || null;
    }

    function getMapViewState() {
        return getBlueMapApp()?.appState?.controls?.state || parseHash()?.mode || 'perspective';
    }

    function getCurrentMapId() {
        return getBlueMapApp()?.mapViewer?.data?.map?.id || parseHash()?.map || 'world';
    }

    function loadLayerPrefs() {
        try {
            const mode = localStorage.getItem(STORAGE_RENDER_MODE);
            if (mode === 'original' || mode === 'simplified') {
                mapRenderMode = mode;
            }
            const gis = localStorage.getItem(STORAGE_GIS_ENABLED);
            gisInfoEnabled = mapRenderMode === 'simplified' || gis !== '0';
        } catch {
            /* ignore */
        }
    }

    function saveLayerPrefs() {
        try {
            localStorage.setItem(STORAGE_RENDER_MODE, mapRenderMode);
            localStorage.setItem(STORAGE_GIS_ENABLED, gisInfoEnabled ? '1' : '0');
        } catch {
            /* ignore */
        }
    }

    function isSimplifiedMapMode() {
        return mapRenderMode === 'simplified';
    }

    function roundViewNumber(value) {
        return Number(value).toFixed(4).replace(/\.?0+$/, '');
    }

    function formatViewHash(view) {
        const distance = view.distance ?? view.height ?? 128;
        return [
            view.map,
            roundViewNumber(view.x),
            roundViewNumber(view.y),
            roundViewNumber(view.z),
            roundViewNumber(distance),
            roundViewNumber(view.rotation ?? view.yaw ?? 0),
            roundViewNumber(view.angle ?? view.pitch ?? 0),
            roundViewNumber(view.tilt ?? 0),
            roundViewNumber(view.ortho ?? view.fov ?? 1),
            view.mode || 'flat'
        ].join(':');
    }

    function replaceLocationHash(hash) {
        const clean = String(hash || '').replace(/^#/, '');
        const url = `${window.location.pathname}${window.location.search}#${clean}`;
        window.history.replaceState(null, '', url);
        if (window.parent !== window) {
            try {
                const parentUrl = new URL(window.parent.location.href);
                parentUrl.hash = clean;
                window.parent.history.replaceState(null, '', parentUrl.toString());
            } catch {
                /* ignore */
            }
        }
    }

    function applyViewState(view) {
        const current = getViewForProjection() || parseHash() || {
            map: getCurrentMapId(),
            x: 0,
            y: 64,
            z: 0,
            distance: 128,
            height: 128,
            rotation: 0,
            angle: 0,
            pitch: 0,
            mode: 'flat'
        };
        const next = {
            ...current,
            ...(view || {}),
            map: view?.map || current.map || getCurrentMapId()
        };
        const dist = next.distance ?? next.height ?? current.distance ?? 128;
        next.distance = dist;
        next.height = dist;
        replaceLocationHash(formatViewHash(next));
        const cm = getControlsManager();
        if (cm) {
            cm.position.x = next.x;
            cm.position.y = next.y;
            cm.position.z = next.z;
            cm.distance = dist;
            if (Number.isFinite(next.rotation)) {
                cm.rotation = next.rotation;
            }
            const pitch = next.pitch ?? next.angle;
            if (Number.isFinite(pitch)) {
                cm.angle = pitch;
            }
        }
        const bm = getBlueMapApp();
        bm?.mapViewer?.updateLoadedMapArea?.();
    }

    function restoreOriginalMapRendering() {
        const bm = getBlueMapApp();
        const data = bm?.mapViewer?.data;
        if (!data) {
            return;
        }
        const settings = bm.settings || {};
        const hiresDefault = Number(settings.hiresSliderDefault);
        const lowresDefault = Number(settings.lowresSliderDefault);
        data.loadedHiresViewDistance = Number.isFinite(hiresDefault) ? hiresDefault : 100;
        data.loadedLowresViewDistance = Number.isFinite(lowresDefault) ? lowresDefault : 2000;
        bm.mapViewer?.updateLoadedMapArea?.();
        bm.saveUserSettings?.();
    }

    const MAP_BG_OPACITY_SELECTED = '0.1';

    /** 选中 GIS 要素时压低 BlueMap 底图（含简化地图模式） */
    function syncMapBackgroundOpacity(selectionActive) {
        const active = !!selectionActive && gisInfoEnabled;
        document.body.classList.toggle('mcwws-gis-has-selection', active);
        const mapContainer = document.getElementById('map-container');
        if (!mapContainer) {
            return;
        }
        if (active) {
            mapContainer.style.opacity = MAP_BG_OPACITY_SELECTED;
        } else {
            mapContainer.style.removeProperty('opacity');
        }
    }

    function syncMapRenderModeVisual() {
        document.body.classList.toggle('mcwws-map-simplified-mode', isSimplifiedMapMode());
        document.body.classList.toggle('mcwws-gis-overlay-visible', gisInfoEnabled);
        const mapContainer = document.getElementById('map-container');
        if (mapContainer) {
            mapContainer.style.display = '';
        }
        gisCachedCamera = null;
        renderOverlay();
    }

    function applyMapRenderMode(mode, persist = true) {
        const next = mode === 'simplified' ? 'simplified' : 'original';
        const changed = next !== mapRenderMode;
        mapRenderMode = next;
        if (changed) {
            if (mapRenderMode === 'original') {
                restoreOriginalMapRendering();
            } else {
                setGisInfoEnabled(true, false);
            }
        }
        syncMapRenderModeVisual();
        if (persist) {
            saveLayerPrefs();
        }
        renderPanel();
    }

    function setGisInfoEnabled(enabled, persist = true) {
        if (!enabled && isSimplifiedMapMode()) {
            enabled = true;
        }
        gisInfoEnabled = !!enabled;
        document.body.classList.toggle('mcwws-gis-overlay-visible', gisInfoEnabled);
        if (!gisInfoEnabled && gisEditorOpen) {
            closeGisEditorPanel();
        }
        if (persist) {
            saveLayerPrefs();
        }
        renderOverlay();
        renderPanel();
    }

    function tryApplyStoredMapRenderMode(attemptsLeft = 40) {
        const bm = getBlueMapApp();
        if (bm?.mapViewer?.data || isSimplifiedMapMode()) {
            applyMapRenderMode(mapRenderMode, false);
            return;
        }
        if (attemptsLeft <= 0) {
            return;
        }
        requestAnimationFrame(() => tryApplyStoredMapRenderMode(attemptsLeft - 1));
    }

    function parseHashParts(parts) {
        if (parts.length < 4) return null;
        const x = Number(parts[1]);
        const y = Number(parts[2]);
        const z = Number(parts[3]);
        if (![x, y, z].every(Number.isFinite)) return null;
        const distance = Number.isFinite(Number(parts[4])) ? Math.max(1, Number(parts[4])) : 128;
        return {
            map: parts[0],
            x,
            y,
            z,
            distance,
            height: distance,
            rotation: Number.isFinite(Number(parts[5])) ? Number(parts[5]) : 0,
            yaw: Number.isFinite(Number(parts[5])) ? Number(parts[5]) : 0,
            angle: Number.isFinite(Number(parts[6])) ? Number(parts[6]) : 0,
            pitch: Number.isFinite(Number(parts[6])) ? Number(parts[6]) : 0,
            mode: parts[9] || 'perspective'
        };
    }

    function parseHash() {
        const parts = String(window.location.hash || '').replace(/^#/, '').split(':');
        return parseHashParts(parts);
    }

    function findCamera(root, seen = new Set(), depth = 0) {
        if (!root || typeof root !== 'object' || seen.has(root) || depth > 5) return null;
        seen.add(root);
        if (root.isCamera && root.projectionMatrix && root.matrixWorldInverse) return root;
        for (const key of Object.keys(root)) {
            if (key === 'parent' || key === 'children' || key === 'domElement') continue;
            const camera = findCamera(root[key], seen, depth + 1);
            if (camera) return camera;
        }
        return null;
    }

    function refreshBlueMapCameraMatrices(camera) {
        if (!camera) return null;
        if (typeof camera.updateMatrixWorld === 'function') {
            camera.updateMatrixWorld(true);
        }
        if (typeof camera.updateProjectionMatrix === 'function') {
            camera.updateProjectionMatrix();
        }
        return camera;
    }

    function getGisBlueMapCamera() {
        const cm = getControlsManager();
        if (cm && typeof cm.updateCamera === 'function') {
            cm.updateCamera();
        }
        gisCachedCamera = findCamera(getBlueMapApp()) || gisCachedCamera;
        return refreshBlueMapCameraMatrices(gisCachedCamera);
    }

    function applyMatrix4(point, matrix) {
        const e = matrix.elements || matrix;
        const x = point.x;
        const y = point.y;
        const z = point.z;
        const w = point.w == null ? 1 : point.w;
        return {
            x: e[0] * x + e[4] * y + e[8] * z + e[12] * w,
            y: e[1] * x + e[5] * y + e[9] * z + e[13] * w,
            z: e[2] * x + e[6] * y + e[10] * z + e[14] * w,
            w: e[3] * x + e[7] * y + e[11] * z + e[15] * w
        };
    }

    /** 与商店钉一致：有相机用 matrix 投影，否则回退 hash/controls 公式 */
    function getViewForProjection() {
        const cm = getControlsManager();
        const hash = parseHash();
        if (cm) {
            const dist = Number(cm.distance) || hash?.distance || hash?.height || 128;
            return {
                map: getCurrentMapId(),
                x: cm.position.x,
                y: cm.position.y,
                z: cm.position.z,
                distance: dist,
                height: dist,
                rotation: cm.rotation ?? hash?.rotation ?? 0,
                yaw: cm.rotation ?? hash?.yaw ?? 0,
                angle: cm.angle ?? hash?.angle ?? 0,
                pitch: cm.angle ?? hash?.pitch ?? 0,
                tilt: cm.tilt ?? 0,
                ortho: cm.ortho ?? 0,
                mode: getMapViewState()
            };
        }
        if (!hash) return null;
        const dist = hash.distance ?? hash.height ?? 128;
        return { ...hash, distance: dist, height: dist };
    }

    function gisWorldY(point, labelStyle) {
        const lift = labelStyle ? 1.2 : 0;
        if (point && Number.isFinite(Number(point.y))) {
            return Number(point.y) + lift;
        }
        return GIS_DEFAULT_Y + lift;
    }

    function projectGisWithCamera(point, camera, labelStyle) {
        const worldPoint = {
            x: point.x,
            y: gisWorldY(point, labelStyle),
            z: point.z,
            w: 1
        };
        const cameraPoint = applyMatrix4(worldPoint, camera.matrixWorldInverse);
        const clipPoint = applyMatrix4(cameraPoint, camera.projectionMatrix);
        if (!clipPoint.w) return null;
        const nx = clipPoint.x / clipPoint.w;
        const ny = clipPoint.y / clipPoint.w;
        const nz = clipPoint.z / clipPoint.w;
        const vp = getGisScreenViewport();
        return {
            x: vp.left + (nx * 0.5 + 0.5) * vp.width,
            y: vp.top + (-ny * 0.5 + 0.5) * vp.height,
            behind: clipPoint.w < 0 || nz < -1 || nz > 1
        };
    }

    function projectGisMarker(point, view, labelStyle) {
        const wy = gisWorldY(point, labelStyle);
        const dx = point.x - view.x;
        const dy = wy - view.y;
        const dz = point.z - view.z;
        const yaw = view.rotation ?? view.yaw ?? 0;
        const cos = Math.cos(yaw);
        const sin = Math.sin(yaw);
        const right = dx * cos - dz * sin;
        const forward = dx * sin + dz * cos;
        const perspectiveBoost = view.mode === 'perspective' ? 1.35 : 1;
        const scale = Math.max(2, Math.min(120, (window.innerHeight / Math.max(10, view.height * 2.2)) * perspectiveBoost));
        const pitchFactor = Math.max(0.2, Math.min(1, Math.abs(Math.sin(view.pitch || -0.8))));
        const vp = getPickViewport();
        return {
            x: vp.centerX + right * scale,
            y: vp.centerY + forward * scale * pitchFactor - dy * scale * 0.65,
            behind: false
        };
    }

    function projectGisPoint(point, view, camera, labelStyle) {
        if (!point) return null;
        const v = view || getViewForProjection();
        if (camera) {
            return projectGisWithCamera(point, camera, labelStyle);
        }
        if (!v) return null;
        return projectGisMarker(point, v, labelStyle);
    }

    function lerpClip4(a, b, t) {
        return {
            x: a.x + (b.x - a.x) * t,
            y: a.y + (b.y - a.y) * t,
            z: a.z + (b.z - a.z) * t,
            w: a.w + (b.w - a.w) * t
        };
    }

    function worldPointToClip(point, camera, labelStyle) {
        const worldPoint = {
            x: point.x,
            y: gisWorldY(point, labelStyle),
            z: point.z,
            w: 1
        };
        const cameraPoint = applyMatrix4(worldPoint, camera.matrixWorldInverse);
        return applyMatrix4(cameraPoint, camera.projectionMatrix);
    }

    /** 近裁剪面：丢弃相机后方线段，保留前方部分 */
    function clipSegmentNearPlane(c0, c1) {
        if (c0.w >= GIS_CLIP_W_EPS && c1.w >= GIS_CLIP_W_EPS) {
            return [c0, c1];
        }
        if (c0.w < GIS_CLIP_W_EPS && c1.w < GIS_CLIP_W_EPS) {
            return null;
        }
        const t = (GIS_CLIP_W_EPS - c0.w) / (c1.w - c0.w);
        const p = lerpClip4(c0, c1, t);
        if (c0.w < GIS_CLIP_W_EPS) {
            return [p, c1];
        }
        return [c0, p];
    }

    /** NDC 方盒裁剪（视锥在屏幕上的投影），避免删掉屏外顶点导致折线乱跳 */
    function clipSegmentNdcBox(n0x, n0y, n1x, n1y) {
        let x0 = n0x;
        let y0 = n0y;
        let x1 = n1x;
        let y1 = n1y;
        const dx = x1 - x0;
        const dy = y1 - y0;
        let t0 = 0;
        let t1 = 1;
        const clip = (p, q) => {
            if (p === 0) {
                return q >= 0;
            }
            const r = q / p;
            if (p < 0) {
                if (r > t1) {
                    return false;
                }
                if (r > t0) {
                    t0 = r;
                }
            } else {
                if (r < t0) {
                    return false;
                }
                if (r < t1) {
                    t1 = r;
                }
            }
            return true;
        };
        if (!clip(-dx, x0 + GIS_NDC_LIMIT)) {
            return null;
        }
        if (!clip(dx, GIS_NDC_LIMIT - x0)) {
            return null;
        }
        if (!clip(-dy, y0 + GIS_NDC_LIMIT)) {
            return null;
        }
        if (!clip(dy, GIS_NDC_LIMIT - y0)) {
            return null;
        }
        if (t0 > t1) {
            return null;
        }
        return [
            { x: x0 + dx * t0, y: y0 + dy * t0 },
            { x: x0 + dx * t1, y: y0 + dy * t1 }
        ];
    }

    function getGisScreenViewport() {
        const rect = getMapCanvasRect();
        if (rect) {
            return {
                left: rect.left,
                top: rect.top,
                width: rect.width,
                height: rect.height
            };
        }
        return {
            left: 0,
            top: 0,
            width: window.innerWidth,
            height: window.innerHeight
        };
    }

    function ndcToScreen(ndc) {
        const vp = getGisScreenViewport();
        return {
            x: vp.left + (ndc.x * 0.5 + 0.5) * vp.width,
            y: vp.top + (-ndc.y * 0.5 + 0.5) * vp.height
        };
    }

    function clipClipSpaceSegmentToScreen(c0, c1) {
        const near = clipSegmentNearPlane(c0, c1);
        if (!near) {
            return null;
        }
        const [a, b] = near;
        const ndcSeg = clipSegmentNdcBox(
            a.x / a.w,
            a.y / a.w,
            b.x / b.w,
            b.y / b.w
        );
        if (!ndcSeg) {
            return null;
        }
        return [ndcToScreen(ndcSeg[0]), ndcToScreen(ndcSeg[1])];
    }

    function screenOutCode(x, y, minX, minY, maxX, maxY) {
        let code = 0;
        if (x < minX) {
            code |= 1;
        } else if (x > maxX) {
            code |= 2;
        }
        if (y < minY) {
            code |= 4;
        } else if (y > maxY) {
            code |= 8;
        }
        return code;
    }

    function clipScreenSegment(p0, p1) {
        const vp = getGisScreenViewport();
        const minX = vp.left - GIS_SCREEN_CLIP_PAD;
        const minY = vp.top - GIS_SCREEN_CLIP_PAD;
        const maxX = vp.left + vp.width + GIS_SCREEN_CLIP_PAD;
        const maxY = vp.top + vp.height + GIS_SCREEN_CLIP_PAD;
        let x0 = p0.x;
        let y0 = p0.y;
        let x1 = p1.x;
        let y1 = p1.y;
        let code0 = screenOutCode(x0, y0, minX, minY, maxX, maxY);
        let code1 = screenOutCode(x1, y1, minX, minY, maxX, maxY);
        while (true) {
            if (!(code0 | code1)) {
                return [{ x: x0, y: y0 }, { x: x1, y: y1 }];
            }
            if (code0 & code1) {
                return null;
            }
            const codeOut = code0 || code1;
            let x;
            let y;
            if (codeOut & 8) {
                x = x0 + ((x1 - x0) * (maxY - y0)) / (y1 - y0);
                y = maxY;
            } else if (codeOut & 4) {
                x = x0 + ((x1 - x0) * (minY - y0)) / (y1 - y0);
                y = minY;
            } else if (codeOut & 2) {
                y = y0 + ((y1 - y0) * (maxX - x0)) / (x1 - x0);
                x = maxX;
            } else {
                y = y0 + ((y1 - y0) * (minX - x0)) / (x1 - x0);
                x = minX;
            }
            if (codeOut === code0) {
                x0 = x;
                y0 = y;
                code0 = screenOutCode(x0, y0, minX, minY, maxX, maxY);
            } else {
                x1 = x;
                y1 = y;
                code1 = screenOutCode(x1, y1, minX, minY, maxX, maxY);
            }
        }
    }

    function screenPointsNear(a, b) {
        const dx = a.x - b.x;
        const dy = a.y - b.y;
        return dx * dx + dy * dy <= GIS_CHAIN_JOIN_EPS * GIS_CHAIN_JOIN_EPS;
    }

    function appendClippedSegment(chains, seg) {
        const [s0, s1] = seg;
        if (!chains.length) {
            chains.push([s0, s1]);
            return;
        }
        const chain = chains[chains.length - 1];
        const last = chain[chain.length - 1];
        if (!screenPointsNear(last, s0)) {
            chains.push([s0, s1]);
            return;
        }
        if (!screenPointsNear(last, s1)) {
            chain.push(s1);
        }
    }

    function chainsToSvgPath(chains) {
        const parts = [];
        chains.forEach((chain) => {
            if (chain.length < 2) {
                return;
            }
            parts.push(
                chain
                    .map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`)
                    .join(' ')
            );
        });
        return parts.join(' ');
    }

    function clipPolygonAgainstPlane(input, insideFn, intersectFn) {
        if (!input.length) {
            return [];
        }
        const output = [];
        for (let i = 0; i < input.length; i += 1) {
            const curr = input[i];
            const prev = input[(i + input.length - 1) % input.length];
            const currIn = insideFn(curr);
            const prevIn = insideFn(prev);
            if (currIn) {
                if (prevIn) {
                    output.push(curr);
                } else {
                    output.push(intersectFn(prev, curr));
                    output.push(curr);
                }
            } else if (prevIn) {
                output.push(intersectFn(prev, curr));
            }
        }
        return output;
    }

    function intersectClipPlane(a, b, valueFn) {
        const va = valueFn(a);
        const vb = valueFn(b);
        const t = va / (va - vb);
        return lerpClip4(a, b, t);
    }

    function clipPolygonHomogeneous(clipVerts) {
        const planes = [
            {
                inside: (c) => c.w >= GIS_CLIP_W_EPS,
                intersect: (a, b) => intersectClipPlane(a, b, (c) => c.w - GIS_CLIP_W_EPS)
            },
            {
                inside: (c) => c.x + c.w >= 0,
                intersect: (a, b) => intersectClipPlane(a, b, (c) => c.x + c.w)
            },
            {
                inside: (c) => c.w - c.x >= 0,
                intersect: (a, b) => intersectClipPlane(a, b, (c) => c.w - c.x)
            },
            {
                inside: (c) => c.y + c.w >= 0,
                intersect: (a, b) => intersectClipPlane(a, b, (c) => c.y + c.w)
            },
            {
                inside: (c) => c.w - c.y >= 0,
                intersect: (a, b) => intersectClipPlane(a, b, (c) => c.w - c.y)
            },
            {
                inside: (c) => c.z + c.w >= 0,
                intersect: (a, b) => intersectClipPlane(a, b, (c) => c.z + c.w)
            },
            {
                inside: (c) => c.w - c.z >= 0,
                intersect: (a, b) => intersectClipPlane(a, b, (c) => c.w - c.z)
            }
        ];
        let poly = clipVerts;
        for (const plane of planes) {
            poly = clipPolygonAgainstPlane(poly, plane.inside, plane.intersect);
            if (!poly.length) {
                return [];
            }
        }
        return poly;
    }

    function clipSpaceToScreen(c) {
        if (!c || c.w < GIS_CLIP_W_EPS) {
            return null;
        }
        const nx = c.x / c.w;
        const ny = c.y / c.w;
        if (
            nx < -GIS_NDC_LIMIT || nx > GIS_NDC_LIMIT
            || ny < -GIS_NDC_LIMIT || ny > GIS_NDC_LIMIT
        ) {
            return null;
        }
        return ndcToScreen({ x: nx, y: ny });
    }

    function clipScreenPolygon(points) {
        const vp = getGisScreenViewport();
        const minX = vp.left - GIS_SCREEN_CLIP_PAD;
        const minY = vp.top - GIS_SCREEN_CLIP_PAD;
        const maxX = vp.left + vp.width + GIS_SCREEN_CLIP_PAD;
        const maxY = vp.top + vp.height + GIS_SCREEN_CLIP_PAD;
        const planes = [
            {
                inside: (p) => p.x >= minX,
                intersect: (a, b) => ({
                    x: a.x + ((b.x - a.x) * (minX - a.x)) / (b.x - a.x),
                    y: a.y + ((b.y - a.y) * (minX - a.x)) / (b.x - a.x)
                })
            },
            {
                inside: (p) => p.x <= maxX,
                intersect: (a, b) => ({
                    x: a.x + ((b.x - a.x) * (maxX - a.x)) / (b.x - a.x),
                    y: a.y + ((b.y - a.y) * (maxX - a.x)) / (b.x - a.x)
                })
            },
            {
                inside: (p) => p.y >= minY,
                intersect: (a, b) => ({
                    x: a.x + ((b.x - a.x) * (minY - a.y)) / (b.y - a.y),
                    y: minY
                })
            },
            {
                inside: (p) => p.y <= maxY,
                intersect: (a, b) => ({
                    x: a.x + ((b.x - a.x) * (maxY - a.y)) / (b.y - a.y),
                    y: maxY
                })
            }
        ];
        let poly = points;
        for (const plane of planes) {
            poly = clipPolygonAgainstPlane(poly, plane.inside, plane.intersect);
            if (!poly.length) {
                return [];
            }
        }
        return poly;
    }

    function screenRingToSvgPath(screenVerts) {
        if (screenVerts.length < 3) {
            return '';
        }
        return `${screenVerts
            .map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`)
            .join(' ')} Z`;
    }

    function getMapCanvasRect() {
        const canvas = document.querySelector('#map-container canvas');
        if (!canvas) {
            return null;
        }
        const rect = canvas.getBoundingClientRect();
        if (!rect.width || !rect.height) {
            return null;
        }
        return rect;
    }

    function getPickViewport() {
        const rect = getMapCanvasRect();
        if (rect) {
            return {
                width: rect.width,
                height: rect.height,
                centerX: rect.left + rect.width / 2,
                centerY: rect.top + rect.height / 2
            };
        }
        return {
            width: window.innerWidth,
            height: window.innerHeight,
            centerX: window.innerWidth / 2,
            centerY: window.innerHeight / 2
        };
    }

    function unprojectNdcToWorld(ndcX, ndcY, ndcZ, camera) {
        if (!camera?.projectionMatrixInverse?.elements || !camera?.matrixWorld?.elements) {
            return null;
        }
        refreshBlueMapCameraMatrices(camera);
        const view = applyMatrix4(
            { x: ndcX, y: ndcY, z: ndcZ, w: 1 },
            camera.projectionMatrixInverse
        );
        const world = applyMatrix4(view, camera.matrixWorld);
        if (Math.abs(world.w) < 1e-8) {
            return null;
        }
        return {
            x: world.x / world.w,
            y: world.y / world.w,
            z: world.z / world.w
        };
    }

    function getDefaultPickPlaneY() {
        if (hasSelectedVertices()) {
            const w = getSelectedVertexWorld();
            if (w && Number.isFinite(w.y)) {
                return w.y;
            }
        }
        if (draftPoints.length) {
            const last = draftPoints[draftPoints.length - 1];
            if (last && Number.isFinite(last.y)) {
                return last.y;
            }
        }
        const cm = getControlsManager();
        if (cm?.position && Number.isFinite(cm.position.y)) {
            return Math.floor(cm.position.y) + 0.5;
        }
        const view = getViewForProjection();
        if (view && Number.isFinite(view.y)) {
            return Math.floor(view.y) + 0.5;
        }
        return GIS_DEFAULT_Y;
    }

    /** 鼠标射线与水平面 y=planeY 求交 */
    function pickWorldOnHorizontalPlane(clientX, clientY, camera, planeY) {
        const rect = getMapCanvasRect();
        if (!rect || !camera) {
            return null;
        }
        const ndcX = ((clientX - rect.left) / rect.width) * 2 - 1;
        const ndcY = -((clientY - rect.top) / rect.height) * 2 + 1;
        const near = unprojectNdcToWorld(ndcX, ndcY, -1, camera);
        const far = unprojectNdcToWorld(ndcX, ndcY, 1, camera);
        if (!near || !far) {
            return null;
        }
        const dy = far.y - near.y;
        if (Math.abs(dy) < 1e-6) {
            return null;
        }
        const t = (planeY - near.y) / dy;
        if (t < 0) {
            return null;
        }
        return snapPoint({
            x: near.x + (far.x - near.x) * t,
            y: planeY,
            z: near.z + (far.z - near.z) * t
        });
    }

    function screenToWorld(screenX, screenY, view) {
        const v = view || getViewForProjection();
        if (!v) {
            return null;
        }
        const vp = getPickViewport();
        const perspectiveBoost = v.mode === 'perspective' ? 1.35 : 1;
        const scale = Math.max(
            2,
            Math.min(120, (vp.height / Math.max(10, v.height * 2.2)) * perspectiveBoost)
        );
        const pitchFactor = Math.max(0.2, Math.min(1, Math.abs(Math.sin(v.pitch || -0.8))));
        const right = (screenX - vp.centerX) / scale;
        const forward = (screenY - vp.centerY) / (scale * pitchFactor);
        const yaw = v.rotation ?? v.yaw ?? 0;
        const cos = Math.cos(yaw);
        const sin = Math.sin(yaw);
        const dx = right * cos + forward * sin;
        const dz = -right * sin + forward * cos;
        return snapPoint({
            x: v.x + dx,
            y: getDefaultPickPlaneY(),
            z: v.z + dz
        });
    }

    function snapPoint(raw) {
        if (!raw) {
            return null;
        }
        const x = Number(raw.x);
        const y = Number(raw.y);
        const z = Number(raw.z);
        if (![x, y, z].every(Number.isFinite)) {
            return null;
        }
        return {
            x: Math.floor(x) + 0.5,
            y: Math.floor(y) + 0.5,
            z: Math.floor(z) + 0.5
        };
    }

    function vec3Sub(a, b) {
        return { x: a.x - b.x, y: a.y - b.y, z: a.z - b.z };
    }

    function vec3Add(a, b) {
        return { x: a.x + b.x, y: a.y + b.y, z: a.z + b.z };
    }

    function vec3Scale(a, s) {
        return { x: a.x * s, y: a.y * s, z: a.z * s };
    }

    function vec3Dot(a, b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    function vec3Cross(a, b) {
        return {
            x: a.y * b.z - a.z * b.y,
            y: a.z * b.x - a.x * b.z,
            z: a.x * b.y - a.y * b.x
        };
    }

    function vec3Len(a) {
        return Math.hypot(a.x, a.y, a.z);
    }

    function vec3Norm(a) {
        const len = vec3Len(a);
        if (len < 1e-8) {
            return null;
        }
        return vec3Scale(a, 1 / len);
    }

    function axisUnit(axis) {
        if (axis === 'x') {
            return { x: 1, y: 0, z: 0 };
        }
        if (axis === 'y') {
            return { x: 0, y: 1, z: 0 };
        }
        return { x: 0, y: 0, z: 1 };
    }

    function getCameraWorldPosition(camera) {
        const e = camera?.matrixWorld?.elements;
        if (!e) {
            return null;
        }
        return { x: e[12], y: e[13], z: e[14] };
    }

    function unprojectScreenToRay(clientX, clientY, camera) {
        const rect = getMapCanvasRect();
        if (!rect || !camera) {
            return null;
        }
        const ndcX = ((clientX - rect.left) / rect.width) * 2 - 1;
        const ndcY = -((clientY - rect.top) / rect.height) * 2 + 1;
        const near = unprojectNdcToWorld(ndcX, ndcY, -1, camera);
        const far = unprojectNdcToWorld(ndcX, ndcY, 1, camera);
        if (!near || !far) {
            return null;
        }
        const dir = vec3Norm(vec3Sub(far, near));
        if (!dir) {
            return null;
        }
        return { origin: near, dir };
    }

    function intersectRayPlane(rayOrigin, rayDir, planePoint, planeNormal) {
        const denom = vec3Dot(rayDir, planeNormal);
        if (Math.abs(denom) < 1e-8) {
            return null;
        }
        const t = vec3Dot(vec3Sub(planePoint, rayOrigin), planeNormal) / denom;
        if (t < 0) {
            return null;
        }
        return vec3Add(rayOrigin, vec3Scale(rayDir, t));
    }

    function lerpWorld3(p0, p1, t) {
        return {
            x: p0.x + t * (p1.x - p0.x),
            y: p0.y + t * (p1.y - p0.y),
            z: p0.z + t * (p1.z - p0.z)
        };
    }

    /** 鼠标到线段（裁剪后屏幕几何）的最近点；用于悬停判定与 + 显示位置 */
    function getScreenHitOnLineSegment(clientX, clientY, p0, p1, view, camera, maxPx) {
        let bestDist = Infinity;
        let bestHit = null;
        iterClippedLineScreenSegments([p0, p1], view, camera, (a, b) => {
            if (screenDist(a.x, a.y, b.x, b.y) < GIS_SEGMENT_MIN_SCREEN_LEN) {
                return;
            }
            const d = distPointToScreenSegment(clientX, clientY, a.x, a.y, b.x, b.y);
            if (d < bestDist) {
                bestDist = d;
                bestHit = closestPointOnScreenSegment(clientX, clientY, a.x, a.y, b.x, b.y);
            }
        });
        if (!bestHit || bestDist > maxPx) {
            return null;
        }
        return { x: bestHit.x, y: bestHit.y, dist: bestDist };
    }

    /** 在 3D 线段上找投影最接近鼠标的 t（用于水平面拾取失败时的回退） */
    function pickSegmentScreenParam(clientX, clientY, p0, p1, view, camera) {
        const s0 = projectGisPoint(p0, view, camera, false);
        const s1 = projectGisPoint(p1, view, camera, false);
        if (!s0 || !s1 || s0.behind || s1.behind) {
            return null;
        }
        return closestPointOnScreenSegment(clientX, clientY, s0.x, s0.y, s1.x, s1.y).t;
    }

    function pickWorldOnSegmentByScreenProjection(clientX, clientY, p0, p1, view, camera) {
        let bestT = 0;
        let bestDist = Infinity;
        const steps = 40;
        const distAt = (t) => {
            const s = projectGisPoint(lerpWorld3(p0, p1, t), view, camera, false);
            if (!s || s.behind) {
                return Infinity;
            }
            return screenDist(clientX, clientY, s.x, s.y);
        };
        for (let i = 0; i <= steps; i += 1) {
            const t = i / steps;
            const d = distAt(t);
            if (d < bestDist) {
                bestDist = d;
                bestT = t;
            }
        }
        let lo = Math.max(0, bestT - 1 / steps);
        let hi = Math.min(1, bestT + 1 / steps);
        for (let k = 0; k < 14; k += 1) {
            const m1 = lo + (hi - lo) / 3;
            const m2 = hi - (hi - lo) / 3;
            if (distAt(m1) < distAt(m2)) {
                hi = m2;
            } else {
                lo = m1;
            }
        }
        return snapPoint(lerpWorld3(p0, p1, (lo + hi) * 0.5));
    }

    /**
     * 插入点世界坐标：与 + 同屏位置，取最近方块中心（与绘制拾取一致的水平面射线，勿用 3D 射线-线段最近点以免吸到邻点）
     */
    function pickWorldOnSegmentAtScreen(clientX, clientY, p0, p1, view, camera) {
        if (!p0 || !p1) {
            return null;
        }
        if (camera) {
            const t = pickSegmentScreenParam(clientX, clientY, p0, p1, view, camera);
            if (t != null) {
                const planeY = p0.y + t * (p1.y - p0.y);
                const onPlane = pickWorldOnHorizontalPlane(clientX, clientY, camera, planeY);
                if (onPlane) {
                    return onPlane;
                }
            }
            return pickWorldOnSegmentByScreenProjection(clientX, clientY, p0, p1, view, camera);
        }
        const s0 = projectGisPoint(p0, view, null, false);
        const s1 = projectGisPoint(p1, view, null, false);
        if (!s0 || !s1) {
            return null;
        }
        const hit = closestPointOnScreenSegment(clientX, clientY, s0.x, s0.y, s1.x, s1.y);
        return snapPoint(lerpWorld3(p0, p1, hit.t));
    }

    function coerceVertexPoint(raw) {
        if (!raw) {
            return null;
        }
        const x = Number(raw.x);
        const y = Number(raw.y);
        const z = Number(raw.z);
        if (![x, y, z].every(Number.isFinite)) {
            return null;
        }
        return {
            x: Math.floor(x) + 0.5,
            y: Math.floor(y) + 0.5,
            z: Math.floor(z) + 0.5
        };
    }

    /** 插入顶点：对齐到距拾取位置最近的方块中心 */
    function coerceInsertVertexPoint(raw) {
        return snapPoint(raw);
    }

    function setFeatureCoordinatesFromPoints(feature, points) {
        if (!feature || !points?.length) {
            return;
        }
        feature.coordinates = points.map((p) => ({ x: p.x, y: p.y, z: p.z }));
    }

    function getFeatureVertexPoints(feature) {
        return coordsToPoints(feature?.coordinates);
    }

    function pointsToCoordList(points) {
        return (points || []).map((p) => coerceVertexPoint(p)).filter(Boolean)
            .map((p) => ({ x: p.x, y: p.y, z: p.z }));
    }

    function parseLaneKey(laneKey) {
        if (!laneKey || laneKey === 'center') {
            return { side: 'center', index: 0 };
        }
        const m = /^(left|right)-(\d+)$/.exec(laneKey);
        if (m) {
            return { side: m[1], index: Number(m[2]) };
        }
        if (laneKey === 'left' || laneKey === 'right') {
            return { side: laneKey, index: 0 };
        }
        return null;
    }

    function formatLaneKey(side, index) {
        return `${side}-${index}`;
    }

    function migrateLanesStructure(feature) {
        const lanes = feature?.properties?.lanes;
        if (!lanes || typeof lanes !== 'object') {
            return;
        }
        ['left', 'right'].forEach((side) => {
            const v = lanes[side];
            if (!Array.isArray(v) || !v.length) {
                return;
            }
            const first = v[0];
            if (first && typeof first === 'object' && !Array.isArray(first) && ('x' in first || 'y' in first)) {
                lanes[side] = [v];
            }
        });
    }

    function getRoadLanesPerSide(feature) {
        const v = Number(feature?.properties?.lanesPerSide);
        if (Number.isFinite(v) && v >= 1) {
            return Math.min(GIS_ROAD_MAX_LANES_PER_SIDE, Math.round(v));
        }
        return 1;
    }

    function ensureVertexLaneCounts(feature) {
        const props = ensureFeatureProperties(feature);
        if (!props.vertexLaneCounts || typeof props.vertexLaneCounts !== 'object') {
            props.vertexLaneCounts = {};
        }
        return props.vertexLaneCounts;
    }

    function getVertexLaneCountAt(feature, side, vertexIndex) {
        const key = String(vertexIndex);
        const entry = feature?.properties?.vertexLaneCounts?.[key];
        const n = Number(entry?.[side]);
        if (Number.isFinite(n) && n >= 1) {
            return Math.min(GIS_ROAD_MAX_LANES_PER_SIDE, Math.round(n));
        }
        return getRoadLanesPerSide(feature);
    }

    function setVertexLaneCountAt(feature, vertexIndex, side, count) {
        const c = Math.max(1, Math.min(GIS_ROAD_MAX_LANES_PER_SIDE, Math.round(count)));
        const vlc = ensureVertexLaneCounts(feature);
        const key = String(vertexIndex);
        if (!vlc[key] || typeof vlc[key] !== 'object') {
            vlc[key] = {};
        }
        vlc[key][side] = c;
        return c;
    }

    function getMaxLaneCountOnSide(feature, side) {
        const center = getFeatureVertexPoints(feature);
        let max = getRoadLanesPerSide(feature);
        for (let i = 0; i < center.length; i += 1) {
            max = Math.max(max, getVertexLaneCountAt(feature, side, i));
        }
        return max;
    }

    /** 任一端有该车道即绘制 i→i+1 段（分岔增道、合流减道都会连线） */
    function shouldDrawLaneSegment(countAtI, countAtI1, laneIndex) {
        return laneIndex < countAtI || laneIndex < countAtI1;
    }

    function getSideLanePolylines(feature, side) {
        migrateLanesStructure(feature);
        const lanes = feature?.properties?.lanes;
        const arr = lanes?.[side];
        if (!Array.isArray(arr)) {
            return [];
        }
        return arr.map((poly) => coordsToPoints(poly));
    }

    function hasPersistedLanes(feature) {
        migrateLanesStructure(feature);
        const left = getSideLanePolylines(feature, 'left');
        const right = getSideLanePolylines(feature, 'right');
        return left.some((pts) => pts.length >= 2) && right.some((pts) => pts.length >= 2);
    }

    function ensureSideLanePolylinesCount(feature, side, targetCount) {
        migrateLanesStructure(feature);
        const props = ensureFeatureProperties(feature);
        if (!props.lanes || typeof props.lanes !== 'object') {
            props.lanes = { left: [], right: [] };
        }
        if (!Array.isArray(props.lanes[side])) {
            props.lanes[side] = [];
        }
        const arr = props.lanes[side];
        const center = getFeatureVertexPoints(feature);
        const sideSign = side === 'left' ? -1 : 1;
        while (arr.length < targetCount) {
            const k = arr.length;
            const inner = k > 0 ? coordsToPoints(arr[k - 1]) : center;
            const off = GIS_ROAD_DUAL_DEFAULT_LANE_OFFSET;
            const source = inner.length >= 2 ? inner : center;
            arr.push(pointsToCoordList(offsetPolylineXZ(source, off, sideSign)));
        }
        while (arr.length > targetCount) {
            arr.pop();
        }
    }

    function padLanePolylineToCenterLength(points, centerLen) {
        const pts = points.slice();
        while (pts.length < centerLen && pts.length > 0) {
            const last = pts[pts.length - 1];
            pts.push({ x: last.x, y: last.y, z: last.z });
        }
        return pts;
    }

    function buildOuterLaneAtVertex(feature, side, laneIndex, fromVertexIndex) {
        const props = ensureFeatureProperties(feature);
        const center = getFeatureVertexPoints(feature);
        const v = Math.max(0, Math.min(fromVertexIndex, center.length - 1));
        const sideSign = side === 'left' ? -1 : 1;
        const inner = laneIndex > 0
            ? coordsToPoints(props.lanes[side][laneIndex - 1])
            : center;
        let newPts;
        if (inner.length >= 2) {
            const head = inner.slice(0, v);
            const tail = inner.slice(v);
            const tailOff = tail.length >= 2
                ? offsetPolylineXZ(tail, GIS_ROAD_DUAL_DEFAULT_LANE_OFFSET, sideSign)
                : [];
            newPts = head.concat(tailOff);
        } else {
            newPts = offsetPolylineXZ(center, GIS_ROAD_DUAL_DEFAULT_LANE_OFFSET * (laneIndex + 1), sideSign);
        }
        return pointsToCoordList(padLanePolylineToCenterLength(newPts, center.length));
    }

    function ensureMaxLaneStorage(feature) {
        if (!isRoadDualCarriagewayEnabled(feature)) {
            return;
        }
        ensureSideLanePolylinesCount(feature, 'left', getMaxLaneCountOnSide(feature, 'left'));
        ensureSideLanePolylinesCount(feature, 'right', getMaxLaneCountOnSide(feature, 'right'));
    }

    function applyVertexLaneCountsAt(feature, vertexIndex, leftCount, rightCount) {
        const prevLeft = getVertexLaneCountAt(feature, 'left', vertexIndex);
        const prevRight = getVertexLaneCountAt(feature, 'right', vertexIndex);
        const nextLeft = setVertexLaneCountAt(feature, vertexIndex, 'left', leftCount);
        const nextRight = setVertexLaneCountAt(feature, vertexIndex, 'right', rightCount);
        ensureMaxLaneStorage(feature);
        migrateLanesStructure(feature);
        const props = ensureFeatureProperties(feature);
        if (nextLeft > prevLeft) {
            for (let k = prevLeft; k < nextLeft; k += 1) {
                props.lanes.left[k] = buildOuterLaneAtVertex(feature, 'left', k, vertexIndex);
            }
        }
        if (nextRight > prevRight) {
            for (let k = prevRight; k < nextRight; k += 1) {
                props.lanes.right[k] = buildOuterLaneAtVertex(feature, 'right', k, vertexIndex);
            }
        }
    }

    /** 仅在尚无车道几何时，按默认每侧车道数从母线生成一次 */
    function initDualLanesIfMissing(feature) {
        if (!feature || !isRoadDualCarriagewayEnabled(feature) || hasPersistedLanes(feature)) {
            return;
        }
        const n = getRoadLanesPerSide(feature);
        ensureSideLanePolylinesCount(feature, 'left', n);
        ensureSideLanePolylinesCount(feature, 'right', n);
    }

    function getFeatureLanePoints(feature, laneKey) {
        if (!feature) {
            return [];
        }
        const center = getFeatureVertexPoints(feature);
        const parsed = parseLaneKey(laneKey);
        if (!isRoadDualCarriagewayEnabled(feature) || !parsed || parsed.side === 'center') {
            return center;
        }
        initDualLanesIfMissing(feature);
        migrateLanesStructure(feature);
        ensureMaxLaneStorage(feature);
        const arr = feature.properties?.lanes?.[parsed.side];
        if (!Array.isArray(arr) || !arr[parsed.index]) {
            return [];
        }
        return coordsToPoints(arr[parsed.index]);
    }

    function isLaneActiveAtVertex(feature, laneKey, vertexIndex) {
        const parsed = parseLaneKey(laneKey);
        if (!parsed || parsed.side === 'center') {
            if (isRoadDualCarriagewayEnabled(feature)) {
                return !isVertexDisplayedAtCamera(feature, vertexIndex);
            }
            return true;
        }
        if (!isVertexDisplayedAtCamera(feature, vertexIndex)) {
            return false;
        }
        const side = parsed.side;
        const laneIndex = parsed.index;
        const centerLen = getFeatureVertexPoints(feature).length;
        if (laneIndex < getVertexLaneCountAt(feature, side, vertexIndex)) {
            return true;
        }
        if (vertexIndex > 0 && laneIndex < getVertexLaneCountAt(feature, side, vertexIndex - 1)) {
            return true;
        }
        if (vertexIndex < centerLen - 1 && laneIndex < getVertexLaneCountAt(feature, side, vertexIndex + 1)) {
            return true;
        }
        return false;
    }

    function getDefaultVertexDisplayHeight(feature) {
        const v = Number(feature?.properties?.defaultVertexDisplayHeight);
        if (Number.isFinite(v) && v > 0) {
            return v;
        }
        const legacy = Number(feature?.properties?.dualSplitHeight);
        if (Number.isFinite(legacy) && legacy > 0) {
            return legacy;
        }
        return GIS_ROAD_DUAL_DEFAULT_SPLIT_HEIGHT;
    }

    function ensureVertexDisplayHeights(feature) {
        const props = ensureFeatureProperties(feature);
        if (!props.vertexDisplayHeights || typeof props.vertexDisplayHeights !== 'object') {
            props.vertexDisplayHeights = {};
        }
        return props.vertexDisplayHeights;
    }

    function getVertexDisplayHeightAt(feature, vertexIndex) {
        const key = String(vertexIndex);
        const v = Number(feature?.properties?.vertexDisplayHeights?.[key]);
        if (Number.isFinite(v) && v > 0) {
            return v;
        }
        return getDefaultVertexDisplayHeight(feature);
    }

    function setVertexDisplayHeightAt(feature, vertexIndex, height) {
        const h = Math.max(1, Math.round(Number(height) || getDefaultVertexDisplayHeight(feature)));
        ensureVertexDisplayHeights(feature)[String(vertexIndex)] = h;
        return h;
    }

    /** 相机高度 ≤ 节点阈值时显示该节点（车道与特征点） */
    function isVertexDisplayedAtCamera(feature, vertexIndex) {
        if (!isRoadDualCarriagewayEnabled(feature)) {
            return true;
        }
        return getMapCameraHeight() <= getVertexDisplayHeightAt(feature, vertexIndex);
    }

    function hasAnyVertexDisplayedForLanes(feature) {
        if (!isRoadDualCarriagewayEnabled(feature)) {
            return false;
        }
        const center = getFeatureVertexPoints(feature);
        for (let i = 0; i < center.length; i += 1) {
            if (isVertexDisplayedAtCamera(feature, i)) {
                return true;
            }
        }
        return false;
    }

    function shouldEditLanesSeparately(feature) {
        return isRoadDualCarriagewayEnabled(feature) && hasAnyVertexDisplayedForLanes(feature);
    }

    /** @returns {{ lane: string, points: object[] }[]} */
    function getEditableLanesForFeature(feature) {
        if (!feature) {
            return [];
        }
        if (shouldEditLanesSeparately(feature)) {
            initDualLanesIfMissing(feature);
            ensureMaxLaneStorage(feature);
            const out = [];
            ['left', 'right'].forEach((side) => {
                const max = getMaxLaneCountOnSide(feature, side);
                for (let i = 0; i < max; i += 1) {
                    const laneKey = formatLaneKey(side, i);
                    const points = getFeatureLanePoints(feature, laneKey);
                    if (points.length >= 1) {
                        out.push({ lane: laneKey, points });
                    }
                }
            });
            out.push({ lane: 'center', points: getFeatureVertexPoints(feature) });
            return out;
        }
        return [{ lane: 'center', points: getFeatureVertexPoints(feature) }];
    }

    function setFeatureLanePoints(feature, laneKey, points) {
        const next = pointsToCoordList(points);
        if (next.length < 1) {
            return;
        }
        const parsed = parseLaneKey(laneKey);
        if (!isRoadDualCarriagewayEnabled(feature) || !parsed || parsed.side === 'center') {
            setFeatureCoordinatesFromPoints(feature, next);
        } else {
            migrateLanesStructure(feature);
            ensureMaxLaneStorage(feature);
            const props = ensureFeatureProperties(feature);
            if (!props.lanes || typeof props.lanes !== 'object') {
                props.lanes = { left: [], right: [] };
            }
            if (!Array.isArray(props.lanes[parsed.side])) {
                props.lanes[parsed.side] = [];
            }
            props.lanes[parsed.side][parsed.index] = next;
        }
        dirty = true;
    }

    function getLaneRenderChains(feature, side, laneIndex) {
        const laneKey = formatLaneKey(side, laneIndex);
        const pts = getFeatureLanePoints(feature, laneKey);
        const center = getFeatureVertexPoints(feature);
        if (pts.length < 2 || center.length < 2) {
            return [];
        }
        const chains = [];
        let current = [];
        for (let i = 0; i < center.length - 1; i += 1) {
            if (!isVertexDisplayedAtCamera(feature, i) && !isVertexDisplayedAtCamera(feature, i + 1)) {
                if (current.length >= 2) {
                    chains.push(current);
                }
                current = [];
                continue;
            }
            const c0 = getVertexLaneCountAt(feature, side, i);
            const c1 = getVertexLaneCountAt(feature, side, i + 1);
            if (!shouldDrawLaneSegment(c0, c1, laneIndex)) {
                if (current.length >= 2) {
                    chains.push(current);
                }
                current = [];
                continue;
            }
            const p0 = pts[i] || pts[Math.min(i, pts.length - 1)];
            const p1 = pts[i + 1] || pts[Math.min(i + 1, pts.length - 1)];
            if (!p0 || !p1) {
                continue;
            }
            if (!current.length) {
                current.push(p0);
            }
            current.push(p1);
        }
        if (current.length >= 2) {
            chains.push(current);
        }
        return chains;
    }

    function buildSvgFromLaneChains(chains, view, camera) {
        const allChains = [];
        chains.forEach((points) => {
            iterClippedLineScreenSegments(points, view, camera, (s0, s1) => {
                appendClippedSegment(allChains, [s0, s1]);
            });
        });
        return chainsToSvgPath(allChains);
    }

    function getCenterlineRenderChains(feature) {
        const center = getFeatureVertexPoints(feature);
        if (center.length < 2) {
            return [];
        }
        if (!isRoadDualCarriagewayEnabled(feature)) {
            return [center];
        }
        const chains = [];
        let current = [];
        for (let i = 0; i < center.length - 1; i += 1) {
            const showSeg = !isVertexDisplayedAtCamera(feature, i)
                && !isVertexDisplayedAtCamera(feature, i + 1);
            if (!showSeg) {
                if (current.length >= 2) {
                    chains.push(current);
                }
                current = [];
                continue;
            }
            const p0 = center[i];
            const p1 = center[i + 1];
            if (!p0 || !p1) {
                continue;
            }
            if (!current.length) {
                current.push(p0);
            }
            current.push(p1);
        }
        if (current.length >= 2) {
            chains.push(current);
        }
        return chains.length ? chains : [];
    }

    function getRenderableLaneIndices(feature, side) {
        const max = getMaxLaneCountOnSide(feature, side);
        const indices = [];
        for (let i = 0; i < max; i += 1) {
            const chains = getLaneRenderChains(feature, side, i);
            if (chains.some((c) => c.length >= 2)) {
                indices.push(i);
            }
        }
        return indices;
    }

    function getPrimaryRoadVertexSelection() {
        const road = getSelectedLineStringRoad();
        if (!road || selectedVertices.size !== 1) {
            return null;
        }
        const sel = parseVertexSelectionKey(selectedVertices.values().next().value);
        if (!sel || sel.featureId !== road.feature.id) {
            return null;
        }
        const parsed = parseLaneKey(sel.lane);
        if (parsed?.side === 'center') {
            return { feature: road.feature, vertexIndex: sel.vertexIndex };
        }
        if (parsed && (parsed.side === 'left' || parsed.side === 'right')) {
            return { feature: road.feature, vertexIndex: sel.vertexIndex };
        }
        return null;
    }

    function vertexSelectionKey(featureId, lane, vertexIndex) {
        return `${featureId}:${lane}:${vertexIndex}`;
    }

    function parseVertexSelectionKey(key) {
        if (!key) {
            return null;
        }
        const parts = key.split(':');
        if (parts.length < 2) {
            return null;
        }
        const vertexIndex = Number(parts[parts.length - 1]);
        if (!Number.isFinite(vertexIndex)) {
            return null;
        }
        if (parts.length >= 3) {
            const lane = parts[parts.length - 2];
            if (lane === 'center' || lane === 'left' || lane === 'right' || /^(left|right)-\d+$/.test(lane)) {
                return {
                    featureId: parts.slice(0, -2).join(':'),
                    lane,
                    vertexIndex
                };
            }
        }
        return {
            featureId: parts.slice(0, -1).join(':'),
            lane: 'center',
            vertexIndex
        };
    }

    function hasSelectedVertices() {
        return selectedVertices.size > 0;
    }

    function isVertexSelected(featureId, lane, vertexIndex) {
        return selectedVertices.has(vertexSelectionKey(featureId, lane, vertexIndex));
    }

    function getPrimarySelectedVertex() {
        const key = selectedVertices.values().next().value;
        return key ? parseVertexSelectionKey(key) : null;
    }

    function getVertexWorld(featureId, lane, vertexIndex) {
        const found = findFeatureById(featureId);
        if (!found) {
            return null;
        }
        const pts = getFeatureLanePoints(found.feature, lane || 'center');
        return pts[vertexIndex] || null;
    }

    function getSelectedVertexWorld() {
        const primary = getPrimarySelectedVertex();
        if (!primary) {
            return null;
        }
        return getVertexWorld(primary.featureId, primary.lane, primary.vertexIndex);
    }

    function getSelectedVerticesCentroid() {
        let count = 0;
        const sum = { x: 0, y: 0, z: 0 };
        selectedVertices.forEach((key) => {
            const sel = parseVertexSelectionKey(key);
            const world = sel ? getVertexWorld(sel.featureId, sel.lane, sel.vertexIndex) : null;
            if (!world) {
                return;
            }
            sum.x += world.x;
            sum.y += world.y;
            sum.z += world.z;
            count += 1;
        });
        if (!count) {
            return null;
        }
        return { x: sum.x / count, y: sum.y / count, z: sum.z / count, count };
    }

    function getSelectedFeaturesCentroid() {
        let count = 0;
        const sum = { x: 0, y: 0, z: 0 };
        iterSelectedVertexFeatures().forEach(({ feature }) => {
            getEditableLanesForFeature(feature).forEach(({ points }) => {
                points.forEach((p) => {
                    sum.x += p.x;
                    sum.y += p.y;
                    sum.z += p.z;
                    count += 1;
                });
            });
        });
        if (!count) {
            return null;
        }
        return { x: sum.x / count, y: sum.y / count, z: sum.z / count, count };
    }

    function shouldShowGizmo() {
        return shouldShowVertexHandles() && hasGisSelection();
    }

    /** 有点选点：点或点集质心；仅选几何体：几何体顶点质心 */
    function getGizmoAnchorWorld() {
        if (hasSelectedVertices()) {
            if (selectedVertices.size === 1) {
                return getSelectedVertexWorld();
            }
            const centroid = getSelectedVerticesCentroid();
            return centroid ? { x: centroid.x, y: centroid.y, z: centroid.z } : null;
        }
        const featureCentroid = getSelectedFeaturesCentroid();
        return featureCentroid
            ? { x: featureCentroid.x, y: featureCentroid.y, z: featureCentroid.z }
            : null;
    }

    function syncGizmoFromVertexSelection() {
        if (!shouldShowGizmo()) {
            hideVertexGizmo();
            return;
        }
        const world = getGizmoAnchorWorld();
        if (world) {
            syncVertexGizmoInputs(world);
        }
    }

    function setFeatureVertexPoint(feature, lane, vertexIndex, point, options = {}) {
        const laneId = lane || 'center';
        const pts = getFeatureLanePoints(feature, laneId).slice();
        if (vertexIndex < 0 || vertexIndex >= pts.length) {
            return;
        }
        const next = coerceVertexPoint(point);
        if (!next) {
            return;
        }
        pts[vertexIndex] = next;
        setFeatureLanePoints(feature, laneId, pts);
        if (!options.skipPanel) {
            renderPanel();
        }
    }

    function insertFeatureVertex(featureId, lane, insertIndex, point) {
        const found = findFeatureById(featureId);
        if (!found) {
            return;
        }
        const feature = found.feature;
        if (!['LineString', 'Polygon'].includes(feature.type)) {
            return;
        }
        const next = coerceInsertVertexPoint(point);
        if (!next) {
            return;
        }
        const laneId = lane || 'center';
        const pts = getFeatureLanePoints(feature, laneId).slice();
        const idx = Math.max(0, Math.min(insertIndex, pts.length));
        recordGisHistory();
        pts.splice(idx, 0, next);
        setFeatureLanePoints(feature, laneId, pts);
        clearGisHoverSegmentInsert();
        selectVertex(featureId, laneId, idx);
        renderPanel();
    }

    function clearGisHoverSegmentInsert() {
        gisHoverSegmentInsert = null;
        if (segmentInsertHandleEl) {
            segmentInsertHandleEl.hidden = true;
            segmentInsertHandleEl.classList.add('is-offscreen');
        }
    }

    function hideVertexGizmo() {
        const gizmo = gisVertexGizmoEl || document.getElementById(VERTEX_GIZMO_ID);
        if (!gizmo) {
            return;
        }
        gizmo.hidden = true;
        gizmo.classList.add('is-hidden');
        gizmo.style.pointerEvents = 'none';
    }

    function showVertexGizmo() {
        const gizmo = gisVertexGizmoEl || document.getElementById(VERTEX_GIZMO_ID);
        if (!gizmo) {
            return;
        }
        gizmo.hidden = false;
        gizmo.classList.remove('is-hidden');
        gizmo.style.pointerEvents = 'auto';
    }

    function clearSelectedVertices() {
        endVertexAxisDrag(null);
        selectedVertices.clear();
        gisVertexCoordHistoryPending = false;
        document.body.classList.remove('mcwws-gis-vertex-dragging');
        hideVertexGizmo();
    }

    function validateSelectedVertices() {
        if (!selectedVertices.size) {
            return;
        }
        const next = new Set();
        selectedVertices.forEach((key) => {
            const sel = parseVertexSelectionKey(key);
            if (!sel || !selectedFeatureIds.has(sel.featureId)) {
                return;
            }
            const found = findFeatureById(sel.featureId);
            const pts = found ? getFeatureLanePoints(found.feature, sel.lane || 'center') : [];
            if (sel.vertexIndex >= 0 && sel.vertexIndex < pts.length) {
                next.add(key);
            }
        });
        selectedVertices.clear();
        next.forEach((key) => selectedVertices.add(key));
        syncGizmoFromVertexSelection();
    }

    function selectVertex(featureId, lane, vertexIndex, options = {}) {
        const found = findFeatureById(featureId);
        if (!found) {
            return;
        }
        const laneId = lane || 'center';
        const pts = getFeatureLanePoints(found.feature, laneId);
        if (vertexIndex < 0 || vertexIndex >= pts.length) {
            return;
        }
        const key = vertexSelectionKey(featureId, laneId, vertexIndex);
        if (options.replace) {
            selectedVertices.clear();
            selectedVertices.add(key);
        } else if (selectedVertices.has(key)) {
            selectedVertices.delete(key);
        } else {
            selectedVertices.add(key);
        }
        syncGizmoFromVertexSelection();
        renderOverlay();
        renderPanel();
    }

    function isGisVertexUiTarget(target) {
        return !!target?.closest?.(`#${VERTEX_LAYER_ID}, #${VERTEX_GIZMO_ID}`);
    }

    function shouldShowVertexHandles() {
        return isGisSelectMode() && hasGisSelection();
    }

    function featureSupportsVertices(feature) {
        return feature && ['LineString', 'Polygon', 'Point', 'Label'].includes(feature.type);
    }

    function iterSelectedVertexFeatures() {
        const out = [];
        selectedFeatureIds.forEach((id) => {
            const found = findFeatureById(id);
            if (found && featureSupportsVertices(found.feature)) {
                out.push(found);
            }
        });
        return out;
    }

    function getScreenAxisDir(originWorld, axis, view, camera) {
        const tip = { ...originWorld };
        const span = GIS_GIZMO_AXIS_WORLD_SPAN;
        if (axis === 'x') {
            tip.x += span;
        } else if (axis === 'y') {
            tip.y += span;
        } else {
            tip.z += span;
        }
        const o = projectGisPoint(originWorld, view, camera, false);
        const t = projectGisPoint(tip, view, camera, false);
        if (!o || !t || o.behind || t.behind) {
            return null;
        }
        const dx = t.x - o.x;
        const dy = t.y - o.y;
        const len = Math.hypot(dx, dy);
        if (len < 1.5) {
            return null;
        }
        return {
            ux: dx / len,
            uy: dy / len,
            len,
            pixelsPerWorld: len / span
        };
    }

    function dragVertexAlongAxisAtScreen(screenAxis, axis, startWorld, startClientX, startClientY, clientX, clientY) {
        if (!screenAxis) {
            return startWorld;
        }
        const along = (clientX - startClientX) * screenAxis.ux + (clientY - startClientY) * screenAxis.uy;
        const ppw = screenAxis.pixelsPerWorld || screenAxis.len || 1;
        const delta = ppw > 1e-6 ? along / ppw : 0;
        const next = { ...startWorld };
        if (axis === 'x') {
            next.x = startWorld.x + delta;
        } else if (axis === 'y') {
            next.y = startWorld.y + delta;
        } else {
            next.z = startWorld.z + delta;
        }
        return coerceVertexPoint(next) || startWorld;
    }

    function dragVertexAlongAxis3D(axis, startWorld, clientX, clientY, camera) {
        const axisDir = axisUnit(axis);
        const ray = unprojectScreenToRay(clientX, clientY, camera);
        if (!ray) {
            return startWorld;
        }
        const camPos = getCameraWorldPosition(camera);
        let viewDir = camPos ? vec3Sub(startWorld, camPos) : ray.dir;
        viewDir = vec3Norm(viewDir) || vec3Norm(ray.dir);
        if (!viewDir) {
            return startWorld;
        }
        let planeNormal = vec3Cross(axisDir, viewDir);
        let nLen = vec3Len(planeNormal);
        if (nLen < 1e-6) {
            const alt = axis === 'y' ? { x: 1, y: 0, z: 0 } : { x: 0, y: 1, z: 0 };
            planeNormal = vec3Cross(axisDir, alt);
            nLen = vec3Len(planeNormal);
        }
        if (nLen < 1e-6) {
            return startWorld;
        }
        planeNormal = vec3Scale(planeNormal, 1 / nLen);
        const hit = intersectRayPlane(ray.origin, ray.dir, startWorld, planeNormal);
        if (!hit) {
            return startWorld;
        }
        const t = vec3Dot(vec3Sub(hit, startWorld), axisDir);
        return coerceVertexPoint({
            x: startWorld.x + axisDir.x * t,
            y: startWorld.y + axisDir.y * t,
            z: startWorld.z + axisDir.z * t
        }) || startWorld;
    }

    function endVertexAxisDrag(event) {
        if (!gisVertexDrag) {
            return;
        }
        if (event && event.pointerId !== gisVertexDrag.pointerId) {
            return;
        }
        if (gisVertexDrag.cleanup) {
            gisVertexDrag.cleanup();
            gisVertexDrag.cleanup = null;
        }
        if (gisVertexDrag.moved) {
            renderPanel();
        }
        gisVertexDrag = null;
        document.body.classList.remove('mcwws-gis-vertex-dragging');
        renderOverlay();
    }

    function startVertexAxisDrag(axisBtn, event) {
        if (!shouldShowGizmo() || event.button !== 0) {
            return;
        }
        const axis = axisBtn.getAttribute('data-axis') || 'x';
        const world = getGizmoAnchorWorld();
        if (!world) {
            return;
        }
        const view = getViewForProjection();
        const camera = getGisBlueMapCamera();
        const screenAxis = getScreenAxisDir(world, axis, view, camera)
            || getScreenAxisDir(world, axis, view, null);
        if (!screenAxis) {
            setStatus('当前视角下该轴不可拖动，请调整视角后重试', 'error');
            return;
        }
        const vertexMode = hasSelectedVertices();
        let dragTargets = [];
        let featureSnapshots = null;
        if (vertexMode) {
            const dragKeys = Array.from(selectedVertices);
            dragTargets = dragKeys.map((key) => {
                const sel = parseVertexSelectionKey(key);
                if (!sel) {
                    return null;
                }
                const startWorld = getVertexWorld(sel.featureId, sel.lane, sel.vertexIndex);
                return startWorld
                    ? {
                        featureId: sel.featureId,
                        lane: sel.lane || 'center',
                        vertexIndex: sel.vertexIndex,
                        startWorld: { ...startWorld }
                    }
                    : null;
            }).filter(Boolean);
            if (!dragTargets.length) {
                return;
            }
        } else {
            featureSnapshots = snapshotSelectedFeatureCoordinates();
            if (!featureSnapshots.size) {
                return;
            }
        }
        event.preventDefault();
        event.stopPropagation();
        if (gisVertexDrag?.cleanup) {
            gisVertexDrag.cleanup();
        }
        gisVertexDrag = {
            mode: vertexMode ? 'vertex' : 'feature',
            axis,
            anchorWorld: { x: world.x, y: world.y, z: world.z },
            dragTargets,
            featureSnapshots,
            startClientX: event.clientX,
            startClientY: event.clientY,
            pointerId: event.pointerId,
            historyRecorded: false,
            moved: false,
            screenAxis,
            cleanup: null
        };
        document.body.classList.add('mcwws-gis-vertex-dragging');
        const onMove = (e) => {
            if (!gisVertexDrag || e.pointerId !== gisVertexDrag.pointerId) {
                return;
            }
            e.preventDefault();
            e.stopPropagation();
            const dx = e.clientX - gisVertexDrag.startClientX;
            const dy = e.clientY - gisVertexDrag.startClientY;
            if (!gisVertexDrag.moved && dx * dx + dy * dy > 0) {
                gisVertexDrag.moved = true;
            }
            if (!gisVertexDrag.moved) {
                return;
            }
            if (!gisVertexDrag.historyRecorded) {
                recordGisHistory();
                gisVertexDrag.historyRecorded = true;
            }
            const nextAnchor = dragVertexAlongAxisAtScreen(
                gisVertexDrag.screenAxis,
                gisVertexDrag.axis,
                gisVertexDrag.anchorWorld,
                gisVertexDrag.startClientX,
                gisVertexDrag.startClientY,
                e.clientX,
                e.clientY
            );
            if (gisVertexDrag.mode === 'feature' && gisVertexDrag.featureSnapshots) {
                const delta = {
                    x: nextAnchor.x - gisVertexDrag.anchorWorld.x,
                    y: nextAnchor.y - gisVertexDrag.anchorWorld.y,
                    z: nextAnchor.z - gisVertexDrag.anchorWorld.z
                };
                applyFeatureTranslateDelta(gisVertexDrag.featureSnapshots, delta);
            } else {
                gisVertexDrag.dragTargets.forEach((target) => {
                    const found = findFeatureById(target.featureId);
                    if (!found) {
                        return;
                    }
                    const next = {
                        x: target.startWorld.x + (nextAnchor.x - gisVertexDrag.anchorWorld.x),
                        y: target.startWorld.y + (nextAnchor.y - gisVertexDrag.anchorWorld.y),
                        z: target.startWorld.z + (nextAnchor.z - gisVertexDrag.anchorWorld.z)
                    };
                    setFeatureVertexPoint(
                        found.feature,
                        target.lane || 'center',
                        target.vertexIndex,
                        next,
                        { skipPanel: true }
                    );
                });
                renderOverlay();
            }
            syncGizmoFromVertexSelection();
        };
        const onUp = (e) => {
            endVertexAxisDrag(e);
        };
        window.addEventListener('pointermove', onMove, true);
        window.addEventListener('pointerup', onUp, true);
        window.addEventListener('pointercancel', onUp, true);
        gisVertexDrag.cleanup = () => {
            window.removeEventListener('pointermove', onMove, true);
            window.removeEventListener('pointerup', onUp, true);
            window.removeEventListener('pointercancel', onUp, true);
        };
        try {
            axisBtn.setPointerCapture(event.pointerId);
        } catch {
            /* ignore */
        }
    }

    function pickVertexAtScreen(clientX, clientY) {
        if (!shouldShowVertexHandles()) {
            return null;
        }
        const view = getViewForProjection();
        const camera = getGisBlueMapCamera();
        let best = null;
        let bestDist = GIS_VERTEX_HIT_PX;
        iterSelectedVertexFeatures().forEach(({ feature }) => {
            getEditableLanesForFeature(feature).forEach(({ lane, points }) => {
                points.forEach((p, idx) => {
                    if (!isLaneActiveAtVertex(feature, lane, idx)) {
                        return;
                    }
                    const s = projectGisPoint(p, view, camera, false);
                    if (!s || s.behind) {
                        return;
                    }
                    const d = screenDist(clientX, clientY, s.x, s.y);
                    if (d < bestDist) {
                        bestDist = d;
                        best = { featureId: feature.id, lane, vertexIndex: idx };
                    }
                });
            });
        });
        return best;
    }

    function pickSegmentInsertAtScreen(clientX, clientY) {
        const view = getViewForProjection();
        const camera = getGisBlueMapCamera();
        let best = null;
        let bestDist = GIS_SEGMENT_HIT_PX;

        iterSelectedVertexFeatures().forEach(({ feature }) => {
            if (feature.type !== 'LineString' && feature.type !== 'Polygon') {
                return;
            }
            getEditableLanesForFeature(feature).forEach(({ lane, points }) => {
            const segCount = feature.type === 'LineString'
                ? points.length - 1
                : points.length;
            if (segCount < 1) {
                return;
            }

            for (let seg = 0; seg < segCount; seg += 1) {
                const i0 = seg;
                const i1 = feature.type === 'Polygon' ? ((seg + 1) % points.length) : (seg + 1);
                if (feature.type === 'LineString' && i1 >= points.length) {
                    continue;
                }
                const p0 = points[i0];
                const p1 = points[i1];
                if (!p0 || !p1) {
                    continue;
                }
                const screenHit = getScreenHitOnLineSegment(
                    clientX,
                    clientY,
                    p0,
                    p1,
                    view,
                    camera,
                    GIS_SEGMENT_HIT_PX
                );
                if (!screenHit) {
                    continue;
                }
                if (screenHit.dist >= bestDist) {
                    continue;
                }
                const s0 = projectGisPoint(p0, view, camera, false);
                const s1 = projectGisPoint(p1, view, camera, false);
                if (s0 && !s0.behind && screenDist(screenHit.x, screenHit.y, s0.x, s0.y) < GIS_SEGMENT_VERTEX_CLEAR_PX) {
                    continue;
                }
                if (s1 && !s1.behind && screenDist(screenHit.x, screenHit.y, s1.x, s1.y) < GIS_SEGMENT_VERTEX_CLEAR_PX) {
                    continue;
                }
                const world = pickWorldOnSegmentAtScreen(clientX, clientY, p0, p1, view, camera);
                if (!world) {
                    continue;
                }
                bestDist = screenHit.dist;
                best = {
                    featureId: feature.id,
                    lane,
                    segmentIndex: seg,
                    insertIndex: seg + 1,
                    world,
                    screenX: screenHit.x,
                    screenY: screenHit.y,
                    clientX,
                    clientY
                };
            }
            });
        });
        return best;
    }

    function refreshHoverSegmentInsertProjection(view, camera) {
        const h = gisHoverSegmentInsert;
        if (!h || !Number.isFinite(h.clientX) || !Number.isFinite(h.clientY)) {
            return;
        }
        const found = findFeatureById(h.featureId);
        if (!found) {
            return;
        }
        const feature = found.feature;
        const points = getFeatureLanePoints(feature, h.lane || 'center');
        const seg = h.segmentIndex;
        const i0 = seg;
        const i1 = feature.type === 'Polygon' ? ((seg + 1) % points.length) : (seg + 1);
        const p0 = points[i0];
        const p1 = points[i1];
        if (!p0 || !p1) {
            return;
        }
        const screenHit = getScreenHitOnLineSegment(
            h.clientX,
            h.clientY,
            p0,
            p1,
            view,
            camera,
            GIS_SEGMENT_HIT_PX
        );
        if (!screenHit) {
            return;
        }
        const world = pickWorldOnSegmentAtScreen(h.clientX, h.clientY, p0, p1, view, camera);
        if (!world) {
            return;
        }
        h.world = world;
        h.screenX = screenHit.x;
        h.screenY = screenHit.y;
    }

    function updateGisHoverSegmentInsert(clientX, clientY) {
        if (!shouldShowVertexHandles() || gisVertexDrag) {
            clearGisHoverSegmentInsert();
            return;
        }
        if (pickVertexAtScreen(clientX, clientY)) {
            clearGisHoverSegmentInsert();
            return;
        }
        gisHoverSegmentInsert = pickSegmentInsertAtScreen(clientX, clientY);
        if (!gisHoverSegmentInsert) {
            clearGisHoverSegmentInsert();
        }
    }

    function syncVertexGizmoInputs(point) {
        const gizmo = ensureVertexGizmo();
        if (!gizmo || !point || !shouldShowGizmo()) {
            hideVertexGizmo();
            return;
        }
        const vertexMode = hasSelectedVertices();
        const multi = vertexMode
            ? selectedVertices.size > 1
            : selectedFeatureIds.size > 1;
        gizmo.classList.toggle('is-multi-vertex', multi);
        const hint = gizmo.querySelector('[data-gizmo-hint]');
        if (hint) {
            hint.hidden = !multi;
            if (!multi) {
                hint.textContent = '';
            } else if (vertexMode) {
                hint.textContent = `已选 ${selectedVertices.size} 点 · 中心`;
            } else {
                hint.textContent = `已选 ${selectedFeatureIds.size} 项 · 中心`;
            }
        }
        const xIn = gizmo.querySelector('[data-coord="x"]');
        const yIn = gizmo.querySelector('[data-coord="y"]');
        const zIn = gizmo.querySelector('[data-coord="z"]');
        if (document.activeElement !== xIn) {
            xIn.value = String(point.x);
        }
        if (document.activeElement !== yIn) {
            yIn.value = String(point.y);
        }
        if (document.activeElement !== zIn) {
            zIn.value = String(point.z);
        }
        const n = vertexMode ? selectedVertices.size : selectedFeatureIds.size;
        const unit = vertexMode ? '点' : '几何';
        gizmo.querySelectorAll('.mcwws-gis-axis').forEach((btn) => {
            const ax = (btn.getAttribute('data-axis') || 'x').toUpperCase();
            btn.title = n > 1
                ? `沿 ${ax} 轴移动已选 ${n} ${unit}`
                : `沿 ${ax} 轴移动`;
        });
    }

    function applyVertexCoordInputsFromGizmo(recordHistory) {
        if (!shouldShowGizmo()) {
            return;
        }
        if (!hasSelectedVertices() && !hasGisSelection()) {
            return;
        }
        const gizmo = ensureVertexGizmo();
        const point = coerceVertexPoint({
            x: gizmo.querySelector('[data-coord="x"]')?.value,
            y: gizmo.querySelector('[data-coord="y"]')?.value,
            z: gizmo.querySelector('[data-coord="z"]')?.value
        });
        if (!point) {
            return;
        }
        if (recordHistory && !gisVertexCoordHistoryPending) {
            recordGisHistory();
            gisVertexCoordHistoryPending = true;
        }
        if (!hasSelectedVertices()) {
            const centroid = getSelectedFeaturesCentroid();
            if (!centroid) {
                return;
            }
            const snapshots = snapshotSelectedFeatureCoordinates();
            const delta = {
                x: point.x - centroid.x,
                y: point.y - centroid.y,
                z: point.z - centroid.z
            };
            if (Math.abs(delta.x) < 1e-9 && Math.abs(delta.y) < 1e-9 && Math.abs(delta.z) < 1e-9) {
                return;
            }
            applyFeatureTranslateDelta(snapshots, delta);
            renderPanel();
            return;
        }
        if (selectedVertices.size === 1) {
            const primary = getPrimarySelectedVertex();
            const found = primary ? findFeatureById(primary.featureId) : null;
            if (!found) {
                return;
            }
            setFeatureVertexPoint(found.feature, primary.lane || 'center', primary.vertexIndex, point);
            renderOverlay();
            return;
        }
        const centroid = getSelectedVerticesCentroid();
        if (!centroid) {
            return;
        }
        const delta = {
            x: point.x - centroid.x,
            y: point.y - centroid.y,
            z: point.z - centroid.z
        };
        if (Math.abs(delta.x) < 1e-9 && Math.abs(delta.y) < 1e-9 && Math.abs(delta.z) < 1e-9) {
            return;
        }
        selectedVertices.forEach((key) => {
            const sel = parseVertexSelectionKey(key);
            if (!sel) {
                return;
            }
            const found = findFeatureById(sel.featureId);
            const world = getVertexWorld(sel.featureId, sel.lane, sel.vertexIndex);
            if (!found || !world) {
                return;
            }
            setFeatureVertexPoint(found.feature, sel.lane || 'center', sel.vertexIndex, {
                x: world.x + delta.x,
                y: world.y + delta.y,
                z: world.z + delta.z
            }, { skipPanel: true });
        });
        dirty = true;
        renderPanel();
        renderOverlay();
    }

    function ensureVertexGizmo() {
        if (gisVertexGizmoEl && !gisVertexGizmoEl.querySelector('.mcwws-gis-gizmo-anchor')) {
            gisVertexGizmoEl.remove();
            gisVertexGizmoEl = null;
            gisVertexGizmoBound = false;
        }
        if (gisVertexGizmoEl) {
            return gisVertexGizmoEl;
        }
        gisVertexGizmoEl = document.createElement('div');
        gisVertexGizmoEl.id = VERTEX_GIZMO_ID;
        gisVertexGizmoEl.className = 'mcwws-gis-vertex-gizmo is-hidden';
        gisVertexGizmoEl.hidden = true;
        gisVertexGizmoEl.innerHTML = `
            <div class="mcwws-gis-gizmo-anchor">
                <div class="mcwws-gis-gizmo-axes" aria-hidden="true">
                    <button type="button" class="mcwws-gis-axis mcwws-gis-axis--x" data-axis="x" title="沿 X 轴移动"></button>
                    <button type="button" class="mcwws-gis-axis mcwws-gis-axis--y" data-axis="y" title="沿 Y 轴移动"></button>
                    <button type="button" class="mcwws-gis-axis mcwws-gis-axis--z" data-axis="z" title="沿 Z 轴移动"></button>
                </div>
            </div>
            <div class="mcwws-gis-gizmo-coords">
                <span class="mcwws-gis-gizmo-hint" data-gizmo-hint hidden></span>
                <label class="mcwws-gis-coord mcwws-gis-coord--x">X
                    <input type="number" data-coord="x" step="0.5" inputmode="decimal">
                </label>
                <label class="mcwws-gis-coord mcwws-gis-coord--y">Y
                    <input type="number" data-coord="y" step="0.5" inputmode="decimal">
                </label>
                <label class="mcwws-gis-coord mcwws-gis-coord--z">Z
                    <input type="number" data-coord="z" step="0.5" inputmode="decimal">
                </label>
            </div>
        `;
        document.body.appendChild(gisVertexGizmoEl);
        bindVertexGizmoEvents();
        return gisVertexGizmoEl;
    }

    function bindVertexGizmoEvents() {
        if (gisVertexGizmoBound || !gisVertexGizmoEl) {
            return;
        }
        gisVertexGizmoBound = true;
        gisVertexGizmoEl.addEventListener('pointerdown', (event) => {
            if (event.target.matches('[data-coord]')) {
                event.stopPropagation();
                return;
            }
            const axisBtn = event.target.closest('.mcwws-gis-axis');
            if (axisBtn) {
                startVertexAxisDrag(axisBtn, event);
            }
        });
        gisVertexGizmoEl.addEventListener('input', (event) => {
            if (!event.target.matches('[data-coord]')) {
                return;
            }
            applyVertexCoordInputsFromGizmo(false);
        });
        gisVertexGizmoEl.addEventListener('change', (event) => {
            if (!event.target.matches('[data-coord]')) {
                return;
            }
            applyVertexCoordInputsFromGizmo(true);
            gisVertexCoordHistoryPending = false;
        });
    }

    function bindVertexLayer(layer) {
        layer.addEventListener('pointerdown', (event) => {
            if (!shouldShowVertexHandles()) {
                return;
            }
            const segHandle = event.target.closest('.mcwws-gis-segment-insert-handle');
            if (segHandle && gisHoverSegmentInsert) {
                event.preventDefault();
                event.stopPropagation();
                insertFeatureVertex(
                    gisHoverSegmentInsert.featureId,
                    gisHoverSegmentInsert.lane || 'center',
                    gisHoverSegmentInsert.insertIndex,
                    gisHoverSegmentInsert.world
                );
                return;
            }
            const handle = event.target.closest('.mcwws-gis-vertex-handle');
            if (!handle) {
                return;
            }
            event.preventDefault();
            event.stopPropagation();
            selectVertex(
                handle.getAttribute('data-fid'),
                handle.getAttribute('data-lane') || 'center',
                Number(handle.getAttribute('data-idx'))
            );
        });
    }

    function clonePointList(points) {
        return (points || []).map((p) => ({ x: p.x, y: p.y, z: p.z }));
    }

    function snapshotFeatureGeometry(feature) {
        migrateLanesStructure(feature);
        const snap = {
            center: clonePointList(getFeatureVertexPoints(feature)),
            lanes: null
        };
        const lanes = feature?.properties?.lanes;
        if (lanes && typeof lanes === 'object') {
            snap.lanes = JSON.parse(JSON.stringify(lanes));
        }
        return snap;
    }

    function snapshotSelectedFeatureCoordinates() {
        const snapshots = new Map();
        selectedFeatureIds.forEach((id) => {
            const found = findFeatureById(id);
            if (found) {
                snapshots.set(id, snapshotFeatureGeometry(found.feature));
            }
        });
        return snapshots;
    }

    function applyFeatureTranslateDelta(snapshots, delta) {
        snapshots.forEach((snap, id) => {
            const found = findFeatureById(id);
            if (!found) {
                return;
            }
            const shift = (pts) => pts.map((p) => coerceVertexPoint({
                x: p.x + delta.x,
                y: p.y + delta.y,
                z: p.z + delta.z
            })).filter(Boolean);
            const centerNext = shift(snap.center || []);
            if (centerNext.length) {
                setFeatureCoordinatesFromPoints(found.feature, centerNext);
            }
            if (snap.lanes) {
                const props = ensureFeatureProperties(found.feature);
                const shiftSide = (side) => {
                    const list = Array.isArray(snap.lanes[side]) ? snap.lanes[side] : [];
                    return list.map((poly) => pointsToCoordList(shift(coordsToPoints(poly))));
                };
                props.lanes = {
                    left: shiftSide('left'),
                    right: shiftSide('right')
                };
                dirty = true;
            } else if (isRoadDualCarriagewayEnabled(found.feature)) {
                initDualLanesIfMissing(found.feature);
            }
        });
        validateSelectedVertices();
        markDirty();
        renderOverlay();
    }

    function ensureVertexLayer() {
        let layer = document.getElementById(VERTEX_LAYER_ID);
        if (!layer) {
            layer = document.createElement('div');
            layer.id = VERTEX_LAYER_ID;
            document.body.appendChild(layer);
            bindVertexLayer(layer);
        }
        return layer;
    }

    function renderVertexHandles(view, camera) {
        const layer = ensureVertexLayer();
        if (!shouldShowVertexHandles()) {
            layer.hidden = true;
            vertexHandleElements.forEach((el) => el.remove());
            vertexHandleElements.clear();
            clearSelectedVertices();
            clearGisHoverSegmentInsert();
            hideVertexGizmo();
            return;
        }
        layer.hidden = false;
        validateSelectedVertices();
        const needed = new Set();
        iterSelectedVertexFeatures().forEach(({ feature }) => {
            getEditableLanesForFeature(feature).forEach(({ lane, points }) => {
            points.forEach((p, idx) => {
                if (!isLaneActiveAtVertex(feature, lane, idx)) {
                    return;
                }
                const key = `${feature.id}:${lane}:${idx}`;
                needed.add(key);
                let handle = vertexHandleElements.get(key);
                if (!handle) {
                    handle = document.createElement('button');
                    handle.type = 'button';
                    handle.className = 'mcwws-gis-vertex-handle';
                    const parsedLane = parseLaneKey(lane);
                    if (parsedLane?.side === 'left') {
                        handle.classList.add('mcwws-gis-vertex-handle--lane-left');
                        if (parsedLane.index > 0) {
                            handle.classList.add(`mcwws-gis-vertex-handle--lane-outer-${parsedLane.index}`);
                        }
                    } else if (parsedLane?.side === 'right') {
                        handle.classList.add('mcwws-gis-vertex-handle--lane-right');
                        if (parsedLane.index > 0) {
                            handle.classList.add(`mcwws-gis-vertex-handle--lane-outer-${parsedLane.index}`);
                        }
                    }
                    handle.setAttribute('data-fid', feature.id);
                    handle.setAttribute('data-lane', lane);
                    handle.setAttribute('data-idx', String(idx));
                    layer.appendChild(handle);
                    vertexHandleElements.set(key, handle);
                }
                handle.classList.toggle('is-active', isVertexSelected(feature.id, lane, idx));
                const projected = projectGisPoint(p, view, camera, false);
                const off = !projected || projected.behind
                    || projected.x < -40 || projected.y < -40
                    || projected.x > window.innerWidth + 40
                    || projected.y > window.innerHeight + 40;
                handle.classList.toggle('is-offscreen', off);
                if (!off) {
                    handle.style.transform = `translate3d(${projected.x}px, ${projected.y}px, 0) translate(-50%, -50%)`;
                }
            });
            });
        });
        vertexHandleElements.forEach((el, key) => {
            if (!needed.has(key)) {
                el.remove();
                vertexHandleElements.delete(key);
            }
        });
        if (shouldShowGizmo()) {
            const world = getGizmoAnchorWorld();
            if (!world) {
                hideVertexGizmo();
            } else {
                syncVertexGizmoInputs(world);
                positionVertexGizmo(world, view, camera);
            }
        } else {
            hideVertexGizmo();
        }
        renderSegmentInsertHandle(view, camera);
    }

    function renderSegmentInsertHandle(view, camera) {
        const layer = ensureVertexLayer();
        if (!gisHoverSegmentInsert || !shouldShowVertexHandles() || gisVertexDrag) {
            if (segmentInsertHandleEl) {
                segmentInsertHandleEl.hidden = true;
                segmentInsertHandleEl.classList.add('is-offscreen');
            }
            return;
        }
        refreshHoverSegmentInsertProjection(view, camera);
        if (!segmentInsertHandleEl) {
            segmentInsertHandleEl = document.createElement('button');
            segmentInsertHandleEl.type = 'button';
            segmentInsertHandleEl.className = 'mcwws-gis-segment-insert-handle';
            segmentInsertHandleEl.title = '点击在当前位置添加顶点';
            segmentInsertHandleEl.setAttribute('aria-label', '添加顶点');
            layer.appendChild(segmentInsertHandleEl);
        }
        const sx = gisHoverSegmentInsert.screenX;
        const sy = gisHoverSegmentInsert.screenY;
        const off = sx == null || sy == null
            || sx < -40 || sy < -40
            || sx > window.innerWidth + 40
            || sy > window.innerHeight + 40;
        segmentInsertHandleEl.hidden = off;
        segmentInsertHandleEl.classList.toggle('is-offscreen', off);
        if (!off) {
            segmentInsertHandleEl.style.transform = `translate3d(${sx}px, ${sy}px, 0) translate(-50%, -50%)`;
        }
    }

    function positionVertexGizmo(world, view, camera) {
        const gizmo = ensureVertexGizmo();
        if (!shouldShowGizmo()) {
            hideVertexGizmo();
            return;
        }
        const projected = projectGisPoint(world, view, camera, false);
        if (!projected || projected.behind) {
            hideVertexGizmo();
            return;
        }
        const anchor = gizmo.querySelector('.mcwws-gis-gizmo-anchor');
        const coords = gizmo.querySelector('.mcwws-gis-gizmo-coords');
        gizmo.style.left = '0';
        gizmo.style.top = '0';
        gizmo.style.transform = 'none';
        if (anchor) {
            anchor.style.left = `${projected.x}px`;
            anchor.style.top = `${projected.y}px`;
            anchor.style.transform = 'translate(-50%, -50%)';
        }
        if (coords) {
            coords.style.left = `${projected.x}px`;
            coords.style.top = `${projected.y}px`;
            coords.style.transform = `translate(-50%, ${GIS_GIZMO_COORDS_OFFSET_Y}px)`;
        }
        const hub = GIS_GIZMO_HUB;
        ['x', 'y', 'z'].forEach((axis) => {
            const dir = getScreenAxisDir(world, axis, view, camera);
            const el = gizmo.querySelector(`.mcwws-gis-axis--${axis}`);
            if (!el) {
                return;
            }
            if (!dir) {
                el.style.display = 'none';
                return;
            }
            el.style.display = 'block';
            const deg = (Math.atan2(dir.uy, dir.ux) * 180) / Math.PI;
            const widthPx = Math.max(
                GIS_GIZMO_AXIS_MIN_WIDTH_PX,
                Math.min(GIS_GIZMO_AXIS_MAX_WIDTH_PX, dir.len * GIS_GIZMO_AXIS_WORLD_SPAN)
            );
            el.style.left = `${hub}px`;
            el.style.top = `${hub}px`;
            el.style.width = `${widthPx}px`;
            el.style.transform = `rotate(${deg.toFixed(2)}deg)`;
        });
        showVertexGizmo();
    }

    function extractInteractionPoint(detail) {
        if (!detail || typeof detail !== 'object') return null;
        const candidates = [
            detail.position,
            detail.hit?.position,
            detail.point,
            detail.block,
            detail
        ];
        for (const c of candidates) {
            if (!c || typeof c !== 'object') continue;
            const x = Number(c.x);
            const y = Number(c.y);
            const z = Number(c.z);
            if ([x, y, z].every(Number.isFinite)) {
                return snapPoint({ x, y, z });
            }
        }
        return null;
    }

    function normalizePointInput(raw) {
        if (Array.isArray(raw) && raw.length >= 3) {
            return snapPoint({ x: Number(raw[0]), y: Number(raw[1]), z: Number(raw[2]) });
        }
        return snapPoint(raw);
    }

    function getActiveLayer() {
        if (!project?.layers) return null;
        return project.layers.find((l) => l.id === activeLayerId) || project.layers[0] || null;
    }

    function iterVisibleFeatures() {
        const mapId = getCurrentMapId();
        const out = [];
        if (!project?.layers) return out;
        project.layers.forEach((layer) => {
            if (!layer.visible) return;
            (layer.features || []).forEach((feature) => {
                if (feature.map === mapId) {
                    out.push({ feature, layer });
                }
            });
        });
        return out;
    }

    function findFeatureById(id) {
        if (!project?.layers || !id) return null;
        for (const layer of project.layers) {
            const feature = (layer.features || []).find((f) => f.id === id);
            if (feature) return { feature, layer };
        }
        return null;
    }

    function hasGisSelection() {
        return selectedFeatureIds.size > 0;
    }

    function isFeatureSelected(id) {
        return !!id && selectedFeatureIds.has(id);
    }

    function clearGisSelection() {
        selectedFeatureIds.clear();
        clearSelectedVertices();
    }

    function clearGisSelectHover() {
        gisHoverFeatureId = null;
        document.body.classList.remove('mcwws-gis-hover-feature');
    }

    function clearGisOverlayDom() {
        svgPathElements.forEach((el) => el.remove());
        svgPathElements.clear();
        svgLaneArrowGroups.forEach((el) => el.remove());
        svgLaneArrowGroups.clear();
        if (svgDraftPathEl) {
            svgDraftPathEl.remove();
            svgDraftPathEl = null;
        }
        pinElements.forEach((pin) => pin.remove());
        pinElements.clear();
        vertexHandleElements.forEach((el) => el.remove());
        vertexHandleElements.clear();
        if (segmentInsertHandleEl) {
            segmentInsertHandleEl.remove();
            segmentInsertHandleEl = null;
        }
        clearGisHoverSegmentInsert();
        clearSelectedVertices();
        hideVertexGizmo();
    }

    function updateGisSelectHoverCursor(clientX, clientY, target) {
        if (!isGisSelectMode()) {
            if (gisHoverFeatureId !== null) {
                clearGisSelectHover();
            }
            return;
        }
        if (target?.closest?.('.mcwws-ctrl-gis-wrap, .mcwws-layer-dialog, .mcwws-map-controls')) {
            if (gisHoverFeatureId !== null) {
                clearGisSelectHover();
            }
            return;
        }
        const vtx = pickVertexAtScreen(clientX, clientY);
        const segInsert = !!target?.closest?.('.mcwws-gis-segment-insert-handle') || !!gisHoverSegmentInsert;
        const fid = vtx ? vtx.featureId : pickFeatureAtScreen(clientX, clientY);
        const hovering = !!(fid || vtx || segInsert);
        if (hovering !== !!gisHoverFeatureId) {
            gisHoverFeatureId = fid || (vtx ? vtx.featureId : null);
            document.body.classList.toggle('mcwws-gis-hover-feature', hovering);
        }
    }

    function setGisSelectionSingle(id) {
        clearGisSelection();
        if (id) {
            selectedFeatureIds.add(id);
        }
    }

    function applyGisSelectionFromState(state) {
        clearGisSelection();
        if (Array.isArray(state?.selectedFeatureIds)) {
            state.selectedFeatureIds.forEach((id) => {
                if (id) selectedFeatureIds.add(id);
            });
        } else if (state?.selectedFeatureId) {
            selectedFeatureIds.add(state.selectedFeatureId);
        }
    }

    function featureColor(feature, layer) {
        const c = feature.properties?.color || layer?.color;
        return c && /^#[0-9a-fA-F]{3,8}$/i.test(c) ? c : '#3b82f6';
    }

    function getMapCameraHeight() {
        const view = getViewForProjection();
        if (!view) {
            return Infinity;
        }
        const h = Number(view.distance ?? view.height ?? 128);
        return Number.isFinite(h) ? h : 128;
    }

    function ensureFeatureProperties(feature) {
        if (!feature.properties || typeof feature.properties !== 'object') {
            feature.properties = {};
        }
        return feature.properties;
    }

    function isRoadDualCarriagewayEnabled(feature) {
        return feature?.type === 'LineString' && !!feature.properties?.dualCarriageway;
    }

    function offsetPolylineXZ(points, offset, side) {
        if (!points || points.length < 2 || !offset) {
            return [];
        }
        const out = [];
        for (let i = 0; i < points.length; i += 1) {
            const prev = points[Math.max(0, i - 1)];
            const curr = points[i];
            const next = points[Math.min(points.length - 1, i + 1)];
            let dx = 0;
            let dz = 0;
            if (i > 0) {
                dx += curr.x - prev.x;
                dz += curr.z - prev.z;
            }
            if (i < points.length - 1) {
                dx += next.x - curr.x;
                dz += next.z - curr.z;
            }
            const len = Math.hypot(dx, dz);
            if (len < 1e-6) {
                out.push({ x: curr.x, y: curr.y, z: curr.z });
                continue;
            }
            const px = (-dz / len) * offset * side;
            const pz = (dx / len) * offset * side;
            out.push({
                x: curr.x + px,
                y: curr.y,
                z: curr.z + pz
            });
        }
        return out;
    }

    /** 沿车道折线按屏幕间距采样箭头位置；reverse 为 true 时箭头反向（左车道 ←） */
    function collectLaneArrowPlacements(lanePoints, view, camera, reverse) {
        const placements = [];
        if (!lanePoints || lanePoints.length < 2) {
            return placements;
        }
        let carry = 0;
        for (let i = 0; i < lanePoints.length - 1; i += 1) {
            const p0 = lanePoints[i];
            const p1 = lanePoints[i + 1];
            let segPlacements = [];
            iterClippedLineScreenSegments([p0, p1], view, camera, (a, b) => {
                let angle = Math.atan2(b.y - a.y, b.x - a.x);
                if (reverse) {
                    angle += Math.PI;
                }
                const segLen = Math.hypot(b.x - a.x, b.y - a.y);
                if (segLen < 4) {
                    return;
                }
                let dist = carry > 0 ? GIS_LANE_ARROW_SPACING_PX - carry : 0;
                while (dist <= segLen) {
                    const t = dist / segLen;
                    segPlacements.push({
                        x: a.x + (b.x - a.x) * t,
                        y: a.y + (b.y - a.y) * t,
                        angle
                    });
                    dist += GIS_LANE_ARROW_SPACING_PX;
                }
                carry = (carry + segLen) % GIS_LANE_ARROW_SPACING_PX;
            });
            placements.push(...segPlacements);
        }
        return placements;
    }

    function ensureLaneArrowGroup(svg, groupKey, featureId, laneSide) {
        let group = svgLaneArrowGroups.get(groupKey);
        if (!group) {
            group = document.createElementNS('http://www.w3.org/2000/svg', 'g');
            group.setAttribute('data-fid', featureId);
            group.classList.add('mcwws-gis-lane-arrow-group', `mcwws-gis-lane-arrow-group--${laneSide}`);
            svg.appendChild(group);
            svgLaneArrowGroups.set(groupKey, group);
        }
        while (group.firstChild) {
            group.removeChild(group.firstChild);
        }
        return group;
    }

    /** 靠右行驶：右线沿母线方向 →；左线反向 ← */
    function renderDualLaneDirectionArrows(svg, featureId, lanePoints, view, camera, color, laneSide, laneIndex, dimmed, neededGroupKeys) {
        const groupKey = `${featureId}:arr-${laneSide}-${laneIndex}`;
        const reverse = laneSide === 'left';
        const placements = collectLaneArrowPlacements(lanePoints, view, camera, reverse);
        if (!placements.length) {
            const stale = svgLaneArrowGroups.get(groupKey);
            if (stale) {
                stale.remove();
                svgLaneArrowGroups.delete(groupKey);
            }
            return;
        }
        neededGroupKeys.add(groupKey);
        const group = ensureLaneArrowGroup(svg, groupKey, featureId, laneSide);
        group.classList.toggle('is-dimmed', dimmed);
        placements.forEach((p) => {
            const arrow = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            arrow.setAttribute('d', GIS_LANE_ARROW_SHAPE_D);
            arrow.setAttribute('fill', color);
            const deg = (p.angle * 180) / Math.PI;
            arrow.setAttribute(
                'transform',
                `translate(${p.x.toFixed(2)} ${p.y.toFixed(2)}) rotate(${deg.toFixed(2)})`
            );
            group.appendChild(arrow);
        });
    }

    function getSelectedLineStringRoad() {
        if (selectedFeatureIds.size !== 1) {
            return null;
        }
        const id = Array.from(selectedFeatureIds)[0];
        const found = findFeatureById(id);
        if (!found || found.feature.type !== 'LineString') {
            return null;
        }
        return found;
    }

    function renderRoadPropertiesPanelHtml() {
        const found = getSelectedLineStringRoad();
        if (!found) {
            return '';
        }
        const props = ensureFeatureProperties(found.feature);
        const dual = !!props.dualCarriageway;
        const defaultDisplayHeight = getDefaultVertexDisplayHeight(found.feature);
        const camH = Math.round(getMapCameraHeight());
        const lanesVisible = hasAnyVertexDisplayedForLanes(found.feature);
        const lanesPerSide = getRoadLanesPerSide(found.feature);
        const vtxSel = getPrimaryRoadVertexSelection();
        const vtxLeft = vtxSel ? getVertexLaneCountAt(found.feature, 'left', vtxSel.vertexIndex) : lanesPerSide;
        const vtxRight = vtxSel ? getVertexLaneCountAt(found.feature, 'right', vtxSel.vertexIndex) : lanesPerSide;
        const vtxDisplayH = vtxSel
            ? getVertexDisplayHeightAt(found.feature, vtxSel.vertexIndex)
            : defaultDisplayHeight;
        const vtxShown = vtxSel ? isVertexDisplayedAtCamera(found.feature, vtxSel.vertexIndex) : false;
        const vtxLabel = vtxSel
            ? `第 ${vtxSel.vertexIndex + 1} 个特征点`
            : '请先选中一个特征点';
        return `
            <div class="mcwws-gis-road-props">
                <p class="mcwws-gis-menu-section-title">道路属性</p>
                <label class="mcwws-gis-road-prop-row">
                    <input type="checkbox" data-road-prop="dualCarriageway" ${dual ? 'checked' : ''}
                        ${!gisCanEdit ? 'disabled' : ''}>
                    <span>双向车道（放大后分多线）</span>
                </label>
                <label class="mcwws-gis-road-prop-row">
                    <span>默认每侧车道数</span>
                    <input type="number" class="mcwws-gis-road-prop-input" data-road-prop="lanesPerSide"
                        min="1" max="${GIS_ROAD_MAX_LANES_PER_SIDE}" step="1" value="${lanesPerSide}"
                        title="整段道路默认车道数；可在特征点处单独增减实现分岔"
                        ${!gisCanEdit || !dual ? 'disabled' : ''}>
                </label>
                <label class="mcwws-gis-road-prop-row">
                    <span>默认显示高度阈值</span>
                    <input type="number" class="mcwws-gis-road-prop-input" data-road-prop="defaultVertexDisplayHeight"
                        min="1" step="1" value="${defaultDisplayHeight}"
                        title="未单独设置的节点：相机高度 ≤ 此值时显示车道，超过则仅显示母线"
                        ${!gisCanEdit || !dual ? 'disabled' : ''}>
                </label>
                ${dual ? `
                <div class="mcwws-gis-road-vertex-lanes">
                    <p class="mcwws-gis-menu-section-title">节点设置（${vtxLabel}）</p>
                    <label class="mcwws-gis-road-prop-row">
                        <span>显示高度阈值</span>
                        <input type="number" class="mcwws-gis-road-prop-input" data-vertex-display-height
                            min="1" step="1" value="${vtxDisplayH}"
                            title="相机高度 ≤ 此值时显示该节点车道；超过则不显示"
                            ${!gisCanEdit || !vtxSel ? 'disabled' : ''}>
                    </label>
                    <label class="mcwws-gis-road-prop-row">
                        <span>左侧车道数</span>
                        <input type="number" class="mcwws-gis-road-prop-input" data-vertex-lane="left"
                            min="1" max="${GIS_ROAD_MAX_LANES_PER_SIDE}" step="1" value="${vtxLeft}"
                            ${!gisCanEdit || !vtxSel ? 'disabled' : ''}>
                    </label>
                    <label class="mcwws-gis-road-prop-row">
                        <span>右侧车道数</span>
                        <input type="number" class="mcwws-gis-road-prop-input" data-vertex-lane="right"
                            min="1" max="${GIS_ROAD_MAX_LANES_PER_SIDE}" step="1" value="${vtxRight}"
                            ${!gisCanEdit || !vtxSel ? 'disabled' : ''}>
                    </label>
                    <p class="mcwws-gis-road-props-hint">增加车道时向路外侧新增一条线，可用于分岔</p>
                </div>
                ` : ''}
                <p class="mcwws-gis-road-props-hint">
                    当前高度 ${camH} · ${dual
                        ? (lanesVisible
                            ? (vtxSel
                                ? (vtxShown ? '该节点已显示车道' : '该节点已隐藏（仅母线）')
                                : '部分节点随高度显示/隐藏')
                            : '全部节点已隐藏，显示母线')
                        : '未启用'}
                    （高度越小越近）
                </p>
            </div>
        `;
    }

    function applyVertexSettingsFromPanel() {
        const vtxSel = getPrimaryRoadVertexSelection();
        const found = getSelectedLineStringRoad();
        if (!vtxSel || !found || !gisCanEdit) {
            return;
        }
        const wrap = document.getElementById(GIS_WRAP_ID);
        const leftInput = wrap?.querySelector('[data-vertex-lane="left"]');
        const rightInput = wrap?.querySelector('[data-vertex-lane="right"]');
        const heightInput = wrap?.querySelector('[data-vertex-display-height]');
        if (!leftInput || !rightInput) {
            return;
        }
        recordGisHistory();
        applyVertexLaneCountsAt(
            found.feature,
            vtxSel.vertexIndex,
            Number(leftInput.value),
            Number(rightInput.value)
        );
        if (heightInput) {
            setVertexDisplayHeightAt(found.feature, vtxSel.vertexIndex, Number(heightInput.value));
        }
        markDirty();
        renderOverlay();
        renderLayerDialog();
    }

    function applyRoadPropertyInput(input) {
        const prop = input?.getAttribute?.('data-road-prop');
        if (!prop) {
            return;
        }
        const found = getSelectedLineStringRoad();
        if (!found || !gisCanEdit) {
            return;
        }
        const props = ensureFeatureProperties(found.feature);
        recordGisHistory();
        if (prop === 'dualCarriageway') {
            props.dualCarriageway = !!input.checked;
            if (props.dualCarriageway && !Number.isFinite(Number(props.defaultVertexDisplayHeight))) {
                props.defaultVertexDisplayHeight = getDefaultVertexDisplayHeight(found.feature);
            }
            if (props.dualCarriageway) {
                if (!Number.isFinite(Number(props.lanesPerSide))) {
                    props.lanesPerSide = 1;
                }
                initDualLanesIfMissing(found.feature);
                ensureMaxLaneStorage(found.feature);
            } else {
                delete props.lanes;
                delete props.vertexLaneCounts;
            }
        } else if (prop === 'lanesPerSide') {
            props.lanesPerSide = Math.max(1, Math.min(
                GIS_ROAD_MAX_LANES_PER_SIDE,
                Math.round(Number(input.value) || 1)
            ));
            if (props.dualCarriageway) {
                ensureMaxLaneStorage(found.feature);
            }
        } else if (prop === 'defaultVertexDisplayHeight') {
            props.defaultVertexDisplayHeight = Math.max(
                1,
                Math.round(Number(input.value) || GIS_ROAD_DUAL_DEFAULT_SPLIT_HEIGHT)
            );
        }
        markDirty();
        renderOverlay();
        renderLayerDialog();
    }

    function coordsToPoints(coords) {
        if (!coords) return [];
        if (!Array.isArray(coords)) {
            const p = normalizePointInput(coords);
            return p ? [p] : [];
        }
        return coords.map(normalizePointInput).filter(Boolean);
    }

    async function applyExternalAuth(payload) {
        if (!payload || typeof payload !== 'object') return;
        mapAuthToken = payload.authToken || null;
        mapAuthUser = payload.user || null;
        if (mapAuthToken && !mapAuthUser) {
            try {
                const res = await fetch(`${NODE_API}/api/profile`, {
                    headers: authHeaders(),
                    cache: 'no-store'
                });
                if (res.ok) {
                    mapAuthUser = await res.json();
                } else {
                    mapAuthToken = null;
                }
            } catch {
                mapAuthToken = null;
                mapAuthUser = null;
            }
        }
        await refreshEditPermission();
    }

    async function refreshEditPermission() {
        gisCanEdit = false;
        if (!mapAuthToken) {
            if (gisEditorOpen) {
                closeGisEditorPanel();
            }
            renderPanel();
            return;
        }
        try {
            const res = await fetch(`${NODE_API}/api/admin/access`, {
                headers: authHeaders(),
                cache: 'no-store'
            });
            gisCanEdit = res.ok;
        } catch {
            gisCanEdit = false;
        }
        if (!gisCanEdit && gisEditorOpen) {
            closeGisEditorPanel();
        }
        renderPanel();
        syncDrawingClass();
    }

    function requestAuthFromParent() {
        if (window.parent === window) return;
        try {
            window.parent.postMessage({ type: 'mcwws-auth-request' }, '*');
        } catch {
            /* ignore */
        }
    }

    function initMapAuth() {
        window.addEventListener('message', (event) => {
            if (event.data?.type === 'mcwws-auth') {
                void applyExternalAuth(event.data);
            }
        });
        requestAuthFromParent();
        let retries = 0;
        const timer = window.setInterval(() => {
            retries += 1;
            if (mapAuthUser || retries >= 12) {
                window.clearInterval(timer);
                return;
            }
            requestAuthFromParent();
        }, 500);
    }

    async function loadGisProject() {
        try {
            const res = await fetch(`${NODE_API}/api/gis?t=${Date.now()}`, { cache: 'no-store' });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            project = data.project || data;
            dirty = false;
            resetGisHistory();
            setStatus('数据已加载', 'ok');
        } catch (err) {
            setStatus(`加载失败：${err.message}`, 'error');
        }
        renderOverlay();
        renderPanel();
    }

    async function saveGisProject() {
        if (!gisCanEdit || !project) return;
        saving = true;
        renderPanel();
        try {
            const res = await fetch(`${NODE_API}/api/gis`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    ...authHeaders()
                },
                body: JSON.stringify({ project })
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                throw new Error(data.error || `HTTP ${res.status}`);
            }
            project = data.project || project;
            dirty = false;
            setStatus('已保存到服务器（仍可 Ctrl+Z 撤销）', 'ok');
        } catch (err) {
            setStatus(`保存失败：${err.message}`, 'error');
        } finally {
            saving = false;
            renderPanel();
        }
    }

    function markDirty() {
        dirty = true;
        renderPanel();
    }

    function cloneGisProjectState() {
        if (!project) {
            return null;
        }
        return {
            project: JSON.parse(JSON.stringify(project)),
            selectedFeatureIds: Array.from(selectedFeatureIds),
            selectedVertices: Array.from(selectedVertices),
            activeLayerId: activeLayerId || project.layers?.[0]?.id || 'roads'
        };
    }

    function applyGisProjectState(state) {
        if (!state?.project) {
            return;
        }
        gisHistoryApplying = true;
        project = JSON.parse(JSON.stringify(state.project));
        applyGisSelectionFromState(state);
        activeLayerId = state.activeLayerId || project.layers?.[0]?.id || 'roads';
        selectedVertices.clear();
        if (Array.isArray(state.selectedVertices)) {
            state.selectedVertices.forEach((key) => {
                if (key) {
                    selectedVertices.add(key);
                }
            });
        }
        syncGizmoFromVertexSelection();
        draftPoints = [];
        draftHover = null;
        gisHistoryApplying = false;
        dirty = true;
        renderOverlay();
        renderPanel();
    }

    function resetGisHistory() {
        gisUndoStack = [];
        gisRedoStack = [];
    }

    function canGisUndo() {
        return gisUndoStack.length > 0;
    }

    function canGisRedo() {
        return gisRedoStack.length > 0;
    }

    function recordGisHistory() {
        if (gisHistoryApplying || !project || !gisEditMode || !gisCanEdit) {
            return;
        }
        const snap = cloneGisProjectState();
        if (!snap) {
            return;
        }
        gisUndoStack.push(snap);
        if (gisUndoStack.length > GIS_HISTORY_MAX) {
            gisUndoStack.shift();
        }
        gisRedoStack = [];
    }

    function undoGisEdit() {
        if (!canGisUndo()) {
            setStatus('没有可撤销的操作', '');
            return;
        }
        const current = cloneGisProjectState();
        if (current) {
            gisRedoStack.push(current);
            if (gisRedoStack.length > GIS_HISTORY_MAX) {
                gisRedoStack.shift();
            }
        }
        const prev = gisUndoStack.pop();
        applyGisProjectState(prev);
        setStatus(`已撤销（还可撤销 ${gisUndoStack.length} 步）`, 'ok');
    }

    function redoGisEdit() {
        if (!canGisRedo()) {
            setStatus('没有可重做的操作', '');
            return;
        }
        const current = cloneGisProjectState();
        if (current) {
            gisUndoStack.push(current);
            if (gisUndoStack.length > GIS_HISTORY_MAX) {
                gisUndoStack.shift();
            }
        }
        const next = gisRedoStack.pop();
        applyGisProjectState(next);
        setStatus(`已重做（还可重做 ${gisRedoStack.length} 步）`, 'ok');
    }

    function promptFeatureMeta(defaultName) {
        const name = window.prompt('名称（可留空）', defaultName || '');
        if (name === null) return null;
        const description = window.prompt('说明（可留空）', '') ?? '';
        return { name: String(name).trim(), description: String(description).trim() };
    }

    function addFeature(feature) {
        const layer = getActiveLayer();
        if (!layer) return;
        recordGisHistory();
        if (!Array.isArray(layer.features)) layer.features = [];
        layer.features.push(feature);
        setGisSelectionSingle(feature.id);
        markDirty();
        renderOverlay();
    }

    function deleteSelectedFeature() {
        if (!hasGisSelection() || !project?.layers) return;
        const ids = new Set(selectedFeatureIds);
        recordGisHistory();
        project.layers.forEach((layer) => {
            layer.features = (layer.features || []).filter((f) => !ids.has(f.id));
        });
        clearGisSelection();
        markDirty();
        renderOverlay();
        renderPanel();
    }

    function minVerticesForFeatureType(type) {
        if (type === 'Polygon') {
            return 3;
        }
        if (type === 'LineString') {
            return 2;
        }
        return 1;
    }

    /** 删除当前选中的特征点（支持多选）；点数不足时删除整个要素 */
    function deleteSelectedVertices() {
        if (!hasSelectedVertices() || !gisEditMode || !isGisSelectMode()) {
            return false;
        }
        const byFeatureLane = new Map();
        selectedVertices.forEach((key) => {
            const sel = parseVertexSelectionKey(key);
            if (!sel) {
                return;
            }
            const laneKey = `${sel.featureId}:${sel.lane || 'center'}`;
            if (!byFeatureLane.has(laneKey)) {
                byFeatureLane.set(laneKey, { featureId: sel.featureId, lane: sel.lane || 'center', indices: [] });
            }
            byFeatureLane.get(laneKey).indices.push(sel.vertexIndex);
        });
        if (!byFeatureLane.size) {
            clearSelectedVertices();
            return false;
        }
        recordGisHistory();
        const deleteWholeFeatures = new Set();
        byFeatureLane.forEach(({ featureId, lane, indices }) => {
            const found = findFeatureById(featureId);
            if (!found) {
                return;
            }
            const feature = found.feature;
            const pts = getFeatureLanePoints(feature, lane).slice();
            const unique = [...new Set(indices)].filter((i) => i >= 0 && i < pts.length).sort((a, b) => b - a);
            if (!unique.length) {
                return;
            }
            const minVerts = minVerticesForFeatureType(feature.type);
            if (pts.length - unique.length < minVerts) {
                deleteWholeFeatures.add(featureId);
                return;
            }
            unique.forEach((i) => pts.splice(i, 1));
            setFeatureLanePoints(feature, lane, pts);
        });
        clearSelectedVertices();
        if (deleteWholeFeatures.size) {
            deleteWholeFeatures.forEach((id) => selectedFeatureIds.add(id));
            project.layers.forEach((layer) => {
                layer.features = (layer.features || []).filter((f) => !deleteWholeFeatures.has(f.id));
            });
            selectedFeatureIds.forEach((id) => {
                if (deleteWholeFeatures.has(id)) {
                    selectedFeatureIds.delete(id);
                }
            });
        }
        markDirty();
        renderOverlay();
        renderPanel();
        return true;
    }

    function finishDraft() {
        if (!draftPoints.length) return;
        const layer = getActiveLayer();
        if (!layer) return;
        const map = getCurrentMapId();
        let type = 'LineString';
        if (activeTool === 'polygon') type = 'Polygon';
        else if (activeTool === 'line') type = 'LineString';
        else return;

        if (type === 'LineString' && draftPoints.length < 2) {
            setStatus('道路至少需要 2 个点', 'error');
            return;
        }
        if (type === 'Polygon' && draftPoints.length < 3) {
            setStatus('区域至少需要 3 个点', 'error');
            return;
        }

        const meta = promptFeatureMeta(type === 'Polygon' ? '区域' : '道路');
        if (!meta) return;

        addFeature({
            id: newFeatureId(),
            type,
            map,
            layerId: layer.id,
            coordinates: draftPoints.map((p) => ({ ...p })),
            properties: meta
        });
        draftPoints = [];
        draftHover = null;
        setStatus('要素已添加，记得保存', 'ok');
        renderOverlay();
    }

    function cancelDraft() {
        draftPoints = [];
        draftHover = null;
        setStatus('已取消当前绘制', '');
        renderOverlay();
    }

    function placePointFeature(toolType) {
        const meta = promptFeatureMeta(toolType === 'Label' ? '标注' : '点位');
        if (!meta) return;
        const layer = getActiveLayer();
        if (!layer) return;
        const last = draftPoints[draftPoints.length - 1];
        if (!last) return;
        addFeature({
            id: newFeatureId(),
            type: toolType,
            map: getCurrentMapId(),
            layerId: layer.id,
            coordinates: { ...last },
            properties: meta
        });
        draftPoints = [];
        renderOverlay();
    }

    function handleMapPick(point) {
        if (!gisInfoEnabled || !gisEditMode || !gisCanEdit || !point) return;
        const pickKey = `${point.x},${point.y},${point.z}`;
        const now = Date.now();
        if (lastPickKey === pickKey && now - lastPickAt < 400) {
            return;
        }
        lastPickAt = now;
        lastPickKey = pickKey;
        if (activeTool === 'point' || activeTool === 'label') {
            draftPoints = [point];
            placePointFeature(activeTool === 'label' ? 'Label' : 'Point');
            return;
        }

        if (activeTool === 'line' || activeTool === 'polygon') {
            draftPoints.push(point);
            setStatus(
                `${activeTool === 'line' ? '道路' : '区域'}：已 ${draftPoints.length} 点 — 双击或点「完成」结束，Esc 取消`,
                ''
            );
            renderOverlay();
        }
    }

    function pickWorldFromScreen(clientX, clientY) {
        const camera = getGisBlueMapCamera();
        if (camera) {
            const onPlane = pickWorldOnHorizontalPlane(clientX, clientY, camera, getDefaultPickPlaneY());
            if (onPlane) {
                return onPlane;
            }
        }
        const view = getViewForProjection();
        if (!view) {
            return null;
        }
        return snapPoint(screenToWorld(clientX, clientY, view));
    }

    function isGisPickTarget(target) {
        return !!target?.closest?.('#map-container canvas');
    }

    function isGisMapInteractionTarget(target) {
        return isGisPickTarget(target)
            || !!target?.closest?.('#mcwws-gis-svg-layer [data-fid]')
            || !!target?.closest?.('.mcwws-gis-pin')
            || isGisVertexUiTarget(target);
    }

    function isGisEditorActive() {
        return gisInfoEnabled && gisEditMode && gisCanEdit;
    }

    function isGisSelectMode() {
        return isGisEditorActive() && activeTool === 'select';
    }

    /** 选择工具下：中键拖动画套索（无单独套索按钮） */
    function canUseGisLasso() {
        return isGisSelectMode();
    }

    function isGisDrawPointerActive() {
        return isGisEditorActive() && activeTool !== 'select';
    }

    function screenDist(ax, ay, bx, by) {
        const dx = ax - bx;
        const dy = ay - by;
        return Math.hypot(dx, dy);
    }

    function closestPointOnScreenSegment(px, py, x1, y1, x2, y2) {
        const dx = x2 - x1;
        const dy = y2 - y1;
        const len2 = dx * dx + dy * dy;
        if (len2 < 1e-6) {
            return { x: x1, y: y1, t: 0 };
        }
        let t = ((px - x1) * dx + (py - y1) * dy) / len2;
        t = Math.max(0, Math.min(1, t));
        return { x: x1 + t * dx, y: y1 + t * dy, t };
    }

    function distPointToScreenSegment(px, py, x1, y1, x2, y2) {
        const hit = closestPointOnScreenSegment(px, py, x1, y1, x2, y2);
        return screenDist(px, py, hit.x, hit.y);
    }

    function pointInScreenPolygon(px, py, ring) {
        let inside = false;
        for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
            const xi = ring[i].x;
            const yi = ring[i].y;
            const xj = ring[j].x;
            const yj = ring[j].y;
            const intersect = (yi > py) !== (yj > py)
                && px < ((xj - xi) * (py - yi)) / (yj - yi + 1e-12) + xi;
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    function getGisSelectHitRadiusPx(camera) {
        return camera ? GIS_SELECT_HIT_PX_3D : GIS_SELECT_HIT_PX;
    }

    /** 屏幕空间命中检测（裁剪后的屏幕几何与 SVG 绘制一致） */
    function pickFeatureAtScreen(clientX, clientY) {
        const view = getViewForProjection();
        const camera = getGisBlueMapCamera();
        let bestId = null;
        let bestDist = getGisSelectHitRadiusPx(camera);

        iterVisibleFeatures().forEach(({ feature }) => {
            const points = coordsToPoints(feature.coordinates);

            if (feature.type === 'Point' || feature.type === 'Label') {
                const p = points[0];
                if (!p) {
                    return;
                }
                const s = projectGisPoint(p, view, camera, true);
                if (!s || s.behind) {
                    return;
                }
                const d = screenDist(clientX, clientY, s.x, s.y);
                if (d < bestDist) {
                    bestDist = d;
                    bestId = feature.id;
                }
                return;
            }

            if (feature.type === 'LineString' && points.length >= 2) {
                iterClippedLineScreenSegments(points, view, camera, (a, b) => {
                    const d = distPointToScreenSegment(clientX, clientY, a.x, a.y, b.x, b.y);
                    if (d < bestDist) {
                        bestDist = d;
                        bestId = feature.id;
                    }
                });
                return;
            }

            if (feature.type === 'Polygon' && points.length >= 3) {
                const ring = getClippedScreenRingForPolygon(points, view, camera);
                if (ring.length < 3) {
                    return;
                }
                if (pointInScreenPolygon(clientX, clientY, ring)) {
                    bestDist = 0;
                    bestId = feature.id;
                    return;
                }
                for (let i = 0; i < ring.length; i += 1) {
                    const j = (i + 1) % ring.length;
                    const d = distPointToScreenSegment(
                        clientX,
                        clientY,
                        ring[i].x,
                        ring[i].y,
                        ring[j].x,
                        ring[j].y
                    );
                    if (d < bestDist) {
                        bestDist = d;
                        bestId = feature.id;
                    }
                }
            }
        });

        return bestId;
    }

    function orient2d(ax, ay, bx, by, cx, cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    function pointOnSegment(ax, ay, bx, by, cx, cy) {
        return Math.min(ax, bx) <= cx + 1e-6 && cx <= Math.max(ax, bx) + 1e-6
            && Math.min(ay, by) <= cy + 1e-6 && cy <= Math.max(ay, by) + 1e-6;
    }

    function segmentsIntersect(ax, ay, bx, by, cx, cy, dx, dy) {
        const o1 = orient2d(ax, ay, bx, by, cx, cy);
        const o2 = orient2d(ax, ay, bx, by, dx, dy);
        const o3 = orient2d(cx, cy, dx, dy, ax, ay);
        const o4 = orient2d(cx, cy, dx, dy, bx, by);
        if (o1 * o2 < 0 && o3 * o4 < 0) {
            return true;
        }
        if (Math.abs(o1) < 1e-6 && pointOnSegment(ax, ay, bx, by, cx, cy)) {
            return true;
        }
        if (Math.abs(o2) < 1e-6 && pointOnSegment(ax, ay, bx, by, dx, dy)) {
            return true;
        }
        if (Math.abs(o3) < 1e-6 && pointOnSegment(cx, cy, dx, dy, ax, ay)) {
            return true;
        }
        if (Math.abs(o4) < 1e-6 && pointOnSegment(cx, cy, dx, dy, bx, by)) {
            return true;
        }
        return false;
    }

    function screenSegmentIntersectsRing(x1, y1, x2, y2, ring) {
        if (pointInScreenPolygon(x1, y1, ring) || pointInScreenPolygon(x2, y2, ring)) {
            return true;
        }
        for (let i = 0; i < ring.length; i += 1) {
            const j = (i + 1) % ring.length;
            if (segmentsIntersect(x1, y1, x2, y2, ring[i].x, ring[i].y, ring[j].x, ring[j].y)) {
                return true;
            }
        }
        return false;
    }

    function screenPointInsideOrTouchesRing(px, py, ring) {
        if (pointInScreenPolygon(px, py, ring)) {
            return true;
        }
        for (let i = 0; i < ring.length; i += 1) {
            const j = (i + 1) % ring.length;
            if (distPointToScreenSegment(px, py, ring[i].x, ring[i].y, ring[j].x, ring[j].y) < 2) {
                return true;
            }
        }
        return false;
    }

    function featureIntersectsLassoRing(feature, ring) {
        const view = getViewForProjection();
        const camera = getGisBlueMapCamera();
        const points = coordsToPoints(feature.coordinates);

        if (feature.type === 'Point' || feature.type === 'Label') {
            const p = points[0];
            if (!p) {
                return false;
            }
            const s = projectGisPoint(p, view, camera, true);
            return !!(s && !s.behind && screenPointInsideOrTouchesRing(s.x, s.y, ring));
        }

        if (feature.type === 'LineString' && points.length >= 2) {
            let hit = false;
            iterClippedLineScreenSegments(points, view, camera, (a, b) => {
                if (hit) {
                    return;
                }
                if (screenSegmentIntersectsRing(a.x, a.y, b.x, b.y, ring)) {
                    hit = true;
                }
            });
            return hit;
        }

        if (feature.type === 'Polygon' && points.length >= 3) {
            const screenRing = getClippedScreenRingForPolygon(points, view, camera);
            if (screenRing.length < 3) {
                return false;
            }
            for (let i = 0; i < screenRing.length; i += 1) {
                if (screenPointInsideOrTouchesRing(screenRing[i].x, screenRing[i].y, ring)) {
                    return true;
                }
            }
            for (let i = 0; i < screenRing.length; i += 1) {
                const j = (i + 1) % screenRing.length;
                if (screenSegmentIntersectsRing(
                    screenRing[i].x,
                    screenRing[i].y,
                    screenRing[j].x,
                    screenRing[j].y,
                    ring
                )) {
                    return true;
                }
            }
            let cx = 0;
            let cy = 0;
            screenRing.forEach((p) => {
                cx += p.x;
                cy += p.y;
            });
            cx /= screenRing.length;
            cy /= screenRing.length;
            return screenPointInsideOrTouchesRing(cx, cy, ring);
        }

        return false;
    }

    function collectFeaturesInLassoRing(ring) {
        const ids = [];
        iterVisibleFeatures().forEach(({ feature }) => {
            if (feature?.id && featureIntersectsLassoRing(feature, ring)) {
                ids.push(feature.id);
            }
        });
        return ids;
    }

    function collectVerticesInLassoRing(ring) {
        const keys = [];
        const view = getViewForProjection();
        const camera = getGisBlueMapCamera();
        iterSelectedVertexFeatures().forEach(({ feature }) => {
            getEditableLanesForFeature(feature).forEach(({ lane, points }) => {
                points.forEach((p, idx) => {
                    if (!isLaneActiveAtVertex(feature, lane, idx)) {
                        return;
                    }
                    const s = projectGisPoint(p, view, camera, true);
                    if (s && !s.behind && screenPointInsideOrTouchesRing(s.x, s.y, ring)) {
                        keys.push(vertexSelectionKey(feature.id, lane, idx));
                    }
                });
            });
        });
        return keys;
    }

    function applyLassoVertexSelection(keys, additive) {
        if (!keys.length) {
            return false;
        }
        if (!additive) {
            selectedVertices.clear();
        }
        keys.forEach((key) => selectedVertices.add(key));
        syncGizmoFromVertexSelection();
        return true;
    }

    function lassoRingDiagonal(ring) {
        let minX = Infinity;
        let minY = Infinity;
        let maxX = -Infinity;
        let maxY = -Infinity;
        ring.forEach((p) => {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
        });
        return Math.hypot(maxX - minX, maxY - minY);
    }

    function ensureLassoLayer() {
        let svg = document.getElementById(LASSO_LAYER_ID);
        if (!svg) {
            svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
            svg.id = LASSO_LAYER_ID;
            svg.setAttribute('aria-hidden', 'true');
            document.body.appendChild(svg);
        }
        svg.setAttribute('width', String(window.innerWidth));
        svg.setAttribute('height', String(window.innerHeight));
        return svg;
    }

    function updateLassoPathVisual() {
        const svg = ensureLassoLayer();
        const pts = gisLassoPointer?.points || [];
        if (pts.length < 2) {
            if (gisLassoPathEl) {
                gisLassoPathEl.removeAttribute('d');
            }
            return;
        }
        if (!gisLassoPathEl) {
            gisLassoPathEl = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            gisLassoPathEl.classList.add('mcwws-gis-lasso-path');
            svg.appendChild(gisLassoPathEl);
        }
        let d = `M ${pts[0].x} ${pts[0].y}`;
        for (let i = 1; i < pts.length; i += 1) {
            d += ` L ${pts[i].x} ${pts[i].y}`;
        }
        if (pts.length >= 3) {
            d += ' Z';
        }
        gisLassoPathEl.setAttribute('d', d);
    }

    function clearGisLassoVisual() {
        if (gisLassoPathEl) {
            gisLassoPathEl.remove();
            gisLassoPathEl = null;
        }
        const svg = document.getElementById(LASSO_LAYER_ID);
        if (svg) {
            svg.remove();
        }
        document.body.classList.remove('mcwws-gis-lasso-active');
    }

    function cancelGisLasso() {
        if (gisLassoPointer?.captureEl?.hasPointerCapture?.(gisLassoPointer.pointerId)) {
            try {
                gisLassoPointer.captureEl.releasePointerCapture(gisLassoPointer.pointerId);
            } catch {
                /* ignore */
            }
        }
        gisLassoPointer = null;
        clearGisLassoVisual();
    }

    function appendGisLassoPoint(clientX, clientY) {
        if (!gisLassoPointer) {
            return;
        }
        const pts = gisLassoPointer.points;
        if (pts.length) {
            const last = pts[pts.length - 1];
            if (screenDist(clientX, clientY, last.x, last.y) < GIS_LASSO_POINT_MIN_DIST_PX) {
                return;
            }
        }
        pts.push({ x: clientX, y: clientY });
        updateLassoPathVisual();
    }

    function startGisLasso(event) {
        cancelGisLasso();
        const captureEl = event.target?.closest?.('#map-container canvas') || document.getElementById('map-container');
        gisLassoPointer = {
            points: [{ x: event.clientX, y: event.clientY }],
            pointerId: event.pointerId,
            captureEl
        };
        document.body.classList.add('mcwws-gis-lasso-active');
        updateLassoPathVisual();
        if (captureEl?.setPointerCapture) {
            try {
                captureEl.setPointerCapture(event.pointerId);
            } catch {
                /* ignore */
            }
        }
    }

    function finishGisLasso(event) {
        if (!gisLassoPointer) {
            return;
        }
        appendGisLassoPoint(event.clientX, event.clientY);
        const ring = gisLassoPointer.points.slice();
        const additive = !!(event?.ctrlKey || event?.metaKey);
        cancelGisLasso();
        if (ring.length < GIS_LASSO_MIN_POINTS || lassoRingDiagonal(ring) < GIS_LASSO_MIN_DIAG_PX) {
            return;
        }
        if (hasGisSelection()) {
            const keys = collectVerticesInLassoRing(ring);
            if (!applyLassoVertexSelection(keys, additive)) {
                return;
            }
            renderOverlay();
            renderPanel();
            return;
        }
        const ids = collectFeaturesInLassoRing(ring);
        if (!ids.length) {
            return;
        }
        if (!additive) {
            clearGisSelection();
        }
        ids.forEach((id) => selectedFeatureIds.add(id));
        validateSelectedVertices();
        renderOverlay();
        renderPanel();
    }

    function onGisLassoPointerDownCapture(event) {
        if (!canUseGisLasso()) {
            return;
        }
        if (event.button !== 1) {
            return;
        }
        if (event.target?.closest?.('.mcwws-ctrl-gis-wrap, .mcwws-layer-dialog')) {
            return;
        }
        if (!isGisPickTarget(event.target)) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        event.stopImmediatePropagation();
        startGisLasso(event);
    }

    function onGisLassoPointerMoveCapture(event) {
        if (!gisLassoPointer || event.pointerId !== gisLassoPointer.pointerId) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        appendGisLassoPoint(event.clientX, event.clientY);
    }

    function onGisLassoPointerUpCapture(event) {
        if (!gisLassoPointer || event.pointerId !== gisLassoPointer.pointerId) {
            return;
        }
        if (event.type !== 'pointercancel' && event.button !== 1) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        event.stopImmediatePropagation();
        finishGisLasso(event);
    }

    function bindGisLassoCapture() {
        if (gisLassoCaptureBound) {
            return;
        }
        gisLassoCaptureBound = true;
        document.addEventListener('pointerdown', onGisLassoPointerDownCapture, true);
        document.addEventListener('pointermove', onGisLassoPointerMoveCapture, true);
        document.addEventListener('pointerup', onGisLassoPointerUpCapture, true);
        document.addEventListener('pointercancel', onGisLassoPointerUpCapture, true);
        document.addEventListener('contextmenu', (event) => {
            if (gisLassoPointer) {
                event.preventDefault();
            }
        }, true);
    }

    function markGisPointerMoved(clientX, clientY) {
        if (!gisCanvasPointer || gisCanvasPointer.moved) {
            return;
        }
        const dx = clientX - gisCanvasPointer.startX;
        const dy = clientY - gisCanvasPointer.startY;
        const threshold = isGisSelectMode() ? GIS_SELECT_DRAG_THRESHOLD_PX : GIS_DRAG_THRESHOLD_PX;
        if (dx * dx + dy * dy > threshold * threshold) {
            gisCanvasPointer.moved = true;
        }
    }

    function onCanvasPointerDown(event) {
        if (!isGisEditorActive()) {
            return;
        }
        if (event.button !== 0) {
            return;
        }
        if (gisVertexDrag) {
            return;
        }
        if (event.target?.closest?.('.mcwws-ctrl-gis-wrap, .mcwws-layer-dialog')) {
            return;
        }
        if (isGisVertexUiTarget(event.target)) {
            return;
        }
        if (!isGisMapInteractionTarget(event.target)) {
            return;
        }
        gisCanvasPointer = {
            startX: event.clientX,
            startY: event.clientY,
            moved: false,
            pointerId: event.pointerId
        };
    }

    function onCanvasPointerMove(event) {
        if (gisLassoPointer) {
            return;
        }
        if (gisCanvasPointer && event.pointerId === gisCanvasPointer.pointerId) {
            markGisPointerMoved(event.clientX, event.clientY);
        }
        updateGisSelectHoverCursor(event.clientX, event.clientY, event.target);
        if (isGisSelectMode()) {
            updateGisHoverSegmentInsert(event.clientX, event.clientY);
        } else {
            clearGisHoverSegmentInsert();
        }
        if (gisCanvasPointer?.moved) {
            return;
        }
        if (!gisEditMode || draftPoints.length === 0) {
            return;
        }
        if (activeTool !== 'line' && activeTool !== 'polygon') {
            return;
        }
        if (!isGisPickTarget(event.target)) {
            return;
        }
        draftHover = pickWorldFromScreen(event.clientX, event.clientY);
        renderOverlay();
    }

    function onCanvasPointerUp(event) {
        if (!gisCanvasPointer || event.pointerId !== gisCanvasPointer.pointerId) {
            return;
        }
        const wasDrag = gisCanvasPointer.moved;
        gisCanvasPointer = null;
        if (!isGisEditorActive()) {
            return;
        }
        if (wasDrag) {
            gisLastMapDragAt = Date.now();
            return;
        }
        if (isGisSelectMode()) {
            const vtx = pickVertexAtScreen(event.clientX, event.clientY);
            if (vtx) {
                selectVertex(vtx.featureId, vtx.lane || 'center', vtx.vertexIndex);
                event.stopPropagation();
                return;
            }
            const fid = pickFeatureAtScreen(event.clientX, event.clientY);
            if (fid) {
                if (!(event.ctrlKey || event.metaKey)) {
                    clearSelectedVertices();
                }
                if (selectedFeatureIds.has(fid)) {
                    selectedFeatureIds.delete(fid);
                } else {
                    selectedFeatureIds.add(fid);
                }
                validateSelectedVertices();
                event.stopPropagation();
            } else {
                clearGisSelection();
            }
            renderOverlay();
            renderPanel();
            return;
        }
        if (!isGisDrawPointerActive()) {
            return;
        }
        const point = pickWorldFromScreen(event.clientX, event.clientY);
        if (!point) {
            return;
        }
        event.stopPropagation();
        handleMapPick(point);
    }

    function onMapInteraction(event) {
        if (!gisInfoEnabled || !gisEditMode || !gisCanEdit) return;
        if (gisCanvasPointer?.moved) {
            return;
        }
        // 3D 下由 canvas 射线拾取（与标注投影同一平面）；无相机时再回退 BlueMap 交点
        if (getGisBlueMapCamera()) {
            return;
        }
        const point = extractInteractionPoint(event.detail);
        if (point) {
            handleMapPick(point);
        }
    }

    function bindMapPicks() {
        const bm = getBlueMapApp();
        if (bm?.events && !mapClickBound) {
            mapClickBound = true;
            bm.events.addEventListener('bluemapMapInteraction', onMapInteraction);
        }
        if (!canvasClickBound) {
            canvasClickBound = true;
            document.addEventListener('pointerdown', onCanvasPointerDown, false);
            document.addEventListener('pointermove', onCanvasPointerMove, false);
            document.addEventListener('pointerup', onCanvasPointerUp, false);
            document.addEventListener('pointercancel', onCanvasPointerUp, false);
        }
        bindGisLassoCapture();
    }

    function exportGeoJson() {
        const features = [];
        (project?.layers || []).forEach((layer) => {
            (layer.features || []).forEach((f) => {
                const geomType = f.type === 'Label' ? 'Point' : f.type;
                let coordinates;
                if (geomType === 'Point') {
                    const p = coordsToPoints(f.coordinates)[0];
                    if (!p) return;
                    coordinates = [p.x, p.y, p.z];
                } else {
                    coordinates = coordsToPoints(f.coordinates).map((p) => [p.x, p.y, p.z]);
                }
                features.push({
                    type: 'Feature',
                    properties: {
                        ...f.properties,
                        mcwwsLayerId: layer.id,
                        mcwwsLayerName: layer.name,
                        mcwwsMap: f.map,
                        mcwwsType: f.type
                    },
                    geometry: { type: geomType, coordinates }
                });
            });
        });
        const blob = new Blob([JSON.stringify({ type: 'FeatureCollection', features }, null, 2)], {
            type: 'application/json'
        });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `mcwws-gis-${getCurrentMapId()}.geojson`;
        a.click();
        URL.revokeObjectURL(url);
        setStatus('GeoJSON 已导出', 'ok');
    }

    function ensureSvgLayer() {
        let svg = document.getElementById(SVG_LAYER_ID);
        if (!svg) {
            svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
            svg.id = SVG_LAYER_ID;
            document.body.appendChild(svg);
        }
        svg.setAttribute('width', String(window.innerWidth));
        svg.setAttribute('height', String(window.innerHeight));
        return svg;
    }

    function ensurePinLayer() {
        let layer = document.getElementById(PIN_LAYER_ID);
        if (!layer) {
            layer = document.createElement('div');
            layer.id = PIN_LAYER_ID;
            document.body.appendChild(layer);
        }
        return layer;
    }

    function ensureSvgFeaturePath(svg, key, featureId, geomKind) {
        let path = svgPathElements.get(key);
        const classNames = String(geomKind || 'mcwws-gis-line').split(/\s+/).filter(Boolean);
        if (!path) {
            path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            path.setAttribute('data-fid', featureId);
            path.classList.add('mcwws-gis-feature', ...classNames);
            svg.appendChild(path);
            svgPathElements.set(key, path);
        } else {
            path.classList.remove('mcwws-gis-line', 'mcwws-gis-polygon', 'mcwws-gis-line--dual');
            path.classList.add('mcwws-gis-feature', ...classNames);
        }
        return path;
    }

    /** 与 SVG 绘制相同的折线裁剪，供拾取与 path 生成共用 */
    function iterClippedLineScreenSegments(points, view, camera, onSegment) {
        if (!points || points.length < 2) {
            return;
        }
        const v = view || getViewForProjection();
        for (let i = 0; i < points.length - 1; i += 1) {
            let seg = null;
            if (camera) {
                const c0 = worldPointToClip(points[i], camera, false);
                const c1 = worldPointToClip(points[i + 1], camera, false);
                if (c0 && c1) {
                    seg = clipClipSpaceSegmentToScreen(c0, c1);
                }
            } else if (v) {
                const p0 = projectGisMarker(points[i], v, false);
                const p1 = projectGisMarker(points[i + 1], v, false);
                if (p0 && p1) {
                    seg = clipScreenSegment(p0, p1);
                }
            }
            if (seg) {
                onSegment(seg[0], seg[1]);
            }
        }
    }

    /** 与 SVG 绘制相同的多边形屏幕环 */
    function getClippedScreenRingForPolygon(points, view, camera) {
        if (!points || points.length < 3) {
            return [];
        }
        const v = view || getViewForProjection();
        if (camera) {
            const clipVerts = points
                .map((p) => worldPointToClip(p, camera, false))
                .filter(Boolean);
            if (clipVerts.length < 3) {
                return [];
            }
            const clipped = clipPolygonHomogeneous(clipVerts);
            return clipped.map(clipSpaceToScreen).filter(Boolean);
        }
        if (v) {
            const screenPts = points.map((p) => projectGisMarker(p, v, false)).filter(Boolean);
            if (screenPts.length < 3) {
                return [];
            }
            return clipScreenPolygon(screenPts);
        }
        return [];
    }

    /** 开放折线：逐边裁剪，可产生多段不相连的 path */
    function buildSvgPolylinePath(points, view, camera) {
        if (!points || points.length < 2) {
            return '';
        }
        const chains = [];
        iterClippedLineScreenSegments(points, view, camera, (s0, s1) => {
            appendClippedSegment(chains, [s0, s1]);
        });
        return chainsToSvgPath(chains);
    }

    /** 封闭多边形：整体 Sutherland–Hodgman，保持顶点顺序，避免拼出虚假三角形 */
    function buildSvgPolygonPath(points, view, camera) {
        return screenRingToSvgPath(getClippedScreenRingForPolygon(points, view, camera));
    }

    function renderOverlay() {
        const svg = ensureSvgLayer();
        const pinLayer = ensurePinLayer();
        if (!svg || !pinLayer) return;

        if (!gisInfoEnabled) {
            clearGisOverlayDom();
            clearGisSelectHover();
            syncMapBackgroundOpacity(false);
            return;
        }

        const view = getViewForProjection();
        const camera = getGisBlueMapCamera();

        const neededPathKeys = new Set();
        const neededArrowGroupKeys = new Set();
        const selectionActive = hasGisSelection();
        iterVisibleFeatures().forEach(({ feature, layer }) => {
            const color = featureColor(feature, layer);
            const dimmed = selectionActive && !isFeatureSelected(feature.id);
            const points = coordsToPoints(feature.coordinates);

            if (feature.type === 'LineString' && points.length >= 2) {
                if (isRoadDualCarriagewayEnabled(feature)) {
                    initDualLanesIfMissing(feature);
                    ensureMaxLaneStorage(feature);
                    const centerChains = getCenterlineRenderChains(feature);
                    centerChains.forEach((chain, ci) => {
                        const d = buildSvgPolylinePath(chain, view, camera);
                        if (!d) {
                            return;
                        }
                        const key = `${feature.id}:line:${ci}`;
                        neededPathKeys.add(key);
                        const path = ensureSvgFeaturePath(svg, key, feature.id, 'mcwws-gis-line mcwws-gis-line--center');
                        path.setAttribute('d', d);
                        path.setAttribute('stroke', color);
                        path.classList.toggle('is-dimmed', dimmed);
                    });
                    if (hasAnyVertexDisplayedForLanes(feature)) {
                        ['left', 'right'].forEach((side) => {
                            getRenderableLaneIndices(feature, side).forEach((laneIndex) => {
                                const chains = getLaneRenderChains(feature, side, laneIndex);
                                const d = buildSvgFromLaneChains(chains, view, camera);
                                if (!d) {
                                    return;
                                }
                                const pathKey = `${feature.id}:dual-${side}-${laneIndex}`;
                                neededPathKeys.add(pathKey);
                                const pathEl = ensureSvgFeaturePath(
                                    svg,
                                    pathKey,
                                    feature.id,
                                    `mcwws-gis-line mcwws-gis-line--dual mcwws-gis-line--dual-${side}`
                                );
                                pathEl.setAttribute('d', d);
                                pathEl.setAttribute('stroke', color);
                                pathEl.classList.toggle('is-dimmed', dimmed);
                                const mergedPts = [];
                                chains.forEach((chain) => {
                                    if (chain.length < 2) {
                                        return;
                                    }
                                    if (mergedPts.length) {
                                        mergedPts.push(chain[0]);
                                    } else {
                                        mergedPts.push(chain[0]);
                                    }
                                    for (let cj = 1; cj < chain.length; cj += 1) {
                                        mergedPts.push(chain[cj]);
                                    }
                                });
                                if (mergedPts.length >= 2) {
                                    renderDualLaneDirectionArrows(
                                        svg,
                                        feature.id,
                                        mergedPts,
                                        view,
                                        camera,
                                        color,
                                        side,
                                        laneIndex,
                                        dimmed,
                                        neededArrowGroupKeys
                                    );
                                }
                            });
                        });
                    }
                } else {
                    const d = buildSvgPolylinePath(points, view, camera);
                    if (d) {
                        const key = `${feature.id}:line`;
                        neededPathKeys.add(key);
                        const path = ensureSvgFeaturePath(svg, key, feature.id, 'mcwws-gis-line');
                        path.setAttribute('d', d);
                        path.setAttribute('stroke', color);
                        path.classList.toggle('is-dimmed', dimmed);
                    }
                }
            }
            if (feature.type === 'Polygon' && points.length >= 3) {
                const d = buildSvgPolygonPath(points, view, camera);
                if (d) {
                    const key = `${feature.id}:polygon`;
                    neededPathKeys.add(key);
                    const path = ensureSvgFeaturePath(svg, key, feature.id, 'mcwws-gis-polygon');
                    path.setAttribute('d', d);
                    path.setAttribute('fill', color);
                    path.setAttribute('stroke', color);
                    path.classList.toggle('is-dimmed', dimmed);
                }
            }
        });

        svgPathElements.forEach((path, key) => {
            if (!neededPathKeys.has(key)) {
                path.remove();
                svgPathElements.delete(key);
            }
        });

        svgLaneArrowGroups.forEach((group, key) => {
            if (!neededArrowGroupKeys.has(key)) {
                group.remove();
                svgLaneArrowGroups.delete(key);
            }
        });

        if (draftPoints.length && (activeTool === 'line' || activeTool === 'polygon')) {
            const draft = draftPoints.slice();
            if (draftHover) draft.push(draftHover);
            const d = activeTool === 'polygon' && draft.length >= 3
                ? buildSvgPolygonPath(draft, view, camera)
                : buildSvgPolylinePath(draft, view, camera);
            if (d) {
                if (!svgDraftPathEl) {
                    svgDraftPathEl = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                    svgDraftPathEl.classList.add('mcwws-gis-draft');
                    svg.appendChild(svgDraftPathEl);
                }
                svgDraftPathEl.setAttribute('d', d);
            } else if (svgDraftPathEl) {
                svgDraftPathEl.removeAttribute('d');
            }
        } else if (svgDraftPathEl) {
            svgDraftPathEl.remove();
            svgDraftPathEl = null;
        }

        const pinIds = new Set();
        iterVisibleFeatures().forEach(({ feature, layer }) => {
            if (feature.type !== 'Point' && feature.type !== 'Label') return;
            const point = coordsToPoints(feature.coordinates)[0];
            if (!point) return;
            pinIds.add(feature.id);
            let pin = pinElements.get(feature.id);
            if (!pin) {
                pin = document.createElement('button');
                pin.type = 'button';
                pin.className = 'mcwws-gis-pin';
                pin.setAttribute('data-fid', feature.id);
                pinLayer.appendChild(pin);
                pinElements.set(feature.id, pin);
            }
            const name = feature.properties?.name || (feature.type === 'Label' ? '标注' : '点');
            pin.innerHTML = `
                <span class="mcwws-gis-pin-icon">${feature.type === 'Label' ? '🏷' : '📍'}</span>
                <span class="mcwws-gis-pin-label">${escapeHtml(name)}</span>
            `;
            pin.classList.toggle('is-dimmed', selectionActive && !isFeatureSelected(feature.id));
            const projected = projectGisPoint(point, view, camera, true);
            const off = !projected || projected.behind
                || projected.x < -40 || projected.y < -40
                || projected.x > window.innerWidth + 40
                || projected.y > window.innerHeight + 40;
            pin.classList.toggle('is-offscreen', off);
            if (!off) {
                pin.style.transform = `translate3d(${projected.x}px, ${projected.y}px, 0) translate(-50%, -100%)`;
            }
        });

        pinElements.forEach((pin, id) => {
            if (!pinIds.has(id)) {
                pin.remove();
                pinElements.delete(id);
            }
        });

        renderVertexHandles(view, camera);
        document.body.classList.toggle('mcwws-gis-vertex-edit', shouldShowVertexHandles());
        syncMapBackgroundOpacity(selectionActive);
    }

    function syncDrawingClass() {
        const drawing = isGisDrawPointerActive();
        document.body.classList.toggle('mcwws-gis-drawing', drawing);
        const selectMode = isGisSelectMode();
        document.body.classList.toggle('mcwws-gis-select-mode', selectMode);
        if (!selectMode) {
            clearGisSelectHover();
            cancelGisLasso();
        }
    }

    function closeGisEditorPanel() {
        gisEditorOpen = false;
        gisEditMode = false;
        draftPoints = [];
        draftHover = null;
        cancelGisLasso();
        clearGisSelection();
        syncDrawingClass();
    }

    function openGisEditorPanel() {
        gisEditorOpen = true;
        if (gisCanEdit) {
            gisEditMode = true;
            syncDrawingClass();
            renderOverlay();
        }
    }

    function mountGisAboveDimension(wrap, column) {
        const layerWrap = column.querySelector('.mcwws-ctrl-layer-wrap');
        if (layerWrap && wrap.nextElementSibling !== layerWrap) {
            column.insertBefore(wrap, layerWrap);
        } else if (!layerWrap && column.firstChild !== wrap) {
            column.insertBefore(wrap, column.firstChild);
        }
    }

    function ensureGisControlsColumn() {
        const existing = document.querySelector('.mcwws-ctrl-dimension-column');
        if (existing) {
            return existing;
        }
        const stack = document.querySelector(MAP_CONTROLS_STACK_SEL);
        if (!stack) {
            return null;
        }
        const column = document.createElement('div');
        column.className = 'mcwws-ctrl-dimension-column';
        // 尽量插到“维度所在的那一行”，避免跑到复位按钮上方
        const mainRow = stack.querySelector('.mcwws-ctrl-main-row');
        if (mainRow) {
            mainRow.insertBefore(column, mainRow.firstChild);
        } else {
            stack.insertBefore(column, stack.firstChild);
        }
        return column;
    }

    function ensureGisControls() {
        const column = ensureGisControlsColumn();

        let wrap = document.getElementById(GIS_WRAP_ID);
        if (!wrap) {
            wrap = document.createElement('div');
            wrap.id = GIS_WRAP_ID;
            wrap.className = 'mcwws-ctrl-gis-wrap';
            wrap.innerHTML = `
                <button type="button" class="mcwws-ctrl-gis" title="图层与地图模式">
                    <span class="mcwws-ctrl-gis-thumb" aria-hidden="true"></span>
                    <span class="mcwws-ctrl-gis-text">
                        <svg class="mcwws-ctrl-gis-icon" viewBox="0 0 24 24" width="14" height="14" aria-hidden="true">
                            <path fill="currentColor" d="M12 2 2 7l10 5 10-5-10-5zm0 8.5L2 6v2.5l10 5 10-5V6l-10 4.5zm0 4.5L2 10.5V13l10 5 10-5v-2.5L12 15z"/>
                        </svg>
                        图层
                    </span>
                </button>
                <div class="mcwws-layer-dialog" hidden></div>
            `;
            if (column) {
                mountGisAboveDimension(wrap, column);
            } else {
                wrap.classList.add('mcwws-ctrl-gis-wrap--floating');
                document.body.appendChild(wrap);
            }
            bindGisControlEvents(wrap);
        } else {
            if (column) {
                wrap.classList.remove('mcwws-ctrl-gis-wrap--floating');
                mountGisAboveDimension(wrap, column);
            } else {
                wrap.classList.add('mcwws-ctrl-gis-wrap--floating');
                if (!wrap.parentElement || wrap.parentElement !== document.body) {
                    document.body.appendChild(wrap);
                }
            }
            let dialog = wrap.querySelector('.mcwws-layer-dialog');
            const legacyMenu = wrap.querySelector('.mcwws-gis-menu');
            if (!dialog && legacyMenu) {
                legacyMenu.classList.replace('mcwws-gis-menu', 'mcwws-layer-dialog');
                dialog = legacyMenu;
            }
            if (!dialog) {
                dialog = document.createElement('div');
                dialog.className = 'mcwws-layer-dialog';
                dialog.hidden = true;
                wrap.appendChild(dialog);
            }
            const textEl = wrap.querySelector('.mcwws-ctrl-gis-text');
            if (textEl) {
                const icon = textEl.querySelector('.mcwws-ctrl-gis-icon');
                textEl.textContent = '';
                if (icon) {
                    textEl.appendChild(icon);
                }
                textEl.append(document.createTextNode('图层'));
            }
        }
        return wrap;
    }

    function updateGisButtonState() {
        const btn = document.querySelector('.mcwws-ctrl-gis');
        if (!btn) {
            return;
        }
        btn.classList.toggle('is-open', layerDialogOpen);
        btn.classList.toggle('is-editing', gisEditMode);
        btn.classList.toggle('is-gis-on', gisInfoEnabled);
        let title = '图层与地图模式';
        if (gisEditMode) {
            title = '图层：地理标注编辑中（2D 俯视）';
        } else if (gisInfoEnabled) {
            title = '图层：地理信息已开启';
        }
        btn.title = title;
    }

    function renderGisEditorHtml() {
        const layer = getActiveLayer();
        const selectedIds = Array.from(selectedFeatureIds);
        const editHint = gisCanEdit
            ? (activeTool === 'select'
                ? '左键点选；选中后拖 XYZ 轴或改坐标移动；点多选无需 Ctrl；中键套索'
                : '2D 俯视下点击地图绘制；道路/区域双击结束')
            : '管理员登录后可编辑地理信息';

        return `
            <div class="mcwws-layer-editor">
                <p class="mcwws-gis-menu-hint">${escapeHtml(editHint)}</p>
                <div class="mcwws-gis-menu-tools" role="toolbar" aria-label="绘制工具">
                    ${TOOLS.map((t) => `
                        <button type="button" class="mcwws-gis-menu-tool${activeTool === t.id ? ' is-active' : ''}"
                            data-tool="${t.id}" title="${escapeHtml(t.label)}"
                            ${!gisCanEdit ? 'disabled' : ''}>
                            <span class="mcwws-gis-menu-tool-icon" aria-hidden="true">${t.icon}</span>
                            <span class="mcwws-gis-menu-tool-label">${escapeHtml(t.label)}</span>
                        </button>
                    `).join('')}
                </div>
                <div class="mcwws-gis-menu-actions">
                    <button type="button" class="mcwws-gis-menu-action mcwws-gis-menu-action--primary" data-action="save"
                        title="Ctrl+S"
                        ${!gisCanEdit || !dirty || saving ? 'disabled' : ''}>${saving ? '保存中…' : '保存'}</button>
                </div>
                <div class="mcwws-gis-menu-actions">
                    <button type="button" class="mcwws-gis-menu-action" data-action="undo" title="Ctrl+Z"
                        ${!canGisUndo() ? 'disabled' : ''}>撤销</button>
                    <button type="button" class="mcwws-gis-menu-action" data-action="redo" title="Ctrl+Y"
                        ${!canGisRedo() ? 'disabled' : ''}>重做</button>
                </div>
                <div class="mcwws-gis-menu-actions">
                    <button type="button" class="mcwws-gis-menu-action" data-action="finish-draft"
                        ${draftPoints.length === 0 ? 'disabled' : ''}>完成绘制</button>
                    <button type="button" class="mcwws-gis-menu-action" data-action="cancel-draft"
                        ${draftPoints.length === 0 ? 'disabled' : ''}>取消</button>
                </div>
                <div class="mcwws-gis-menu-actions">
                    <button type="button" class="mcwws-gis-menu-action" data-action="export">导出 GeoJSON</button>
                    <button type="button" class="mcwws-gis-menu-action mcwws-gis-menu-action--danger" data-action="delete"
                        ${selectedIds.length === 0 ? 'disabled' : ''}>删除选中${selectedIds.length > 1 ? ` (${selectedIds.length})` : ''}</button>
                </div>
                <p class="mcwws-gis-menu-section-title">标注图层 · ${escapeHtml(layer?.name || '—')}</p>
                <div class="mcwws-gis-menu-layers">
                    ${(project?.layers || []).map((l) => `
                        <div class="mcwws-gis-menu-layer-row">
                            <button type="button" class="mcwws-gis-menu-layer-pick${l.id === activeLayerId ? ' is-active' : ''}"
                                data-layer-pick="${escapeHtml(l.id)}">
                                <span class="mcwws-gis-menu-layer-thumb" style="background:${escapeHtml(l.color)}" aria-hidden="true"></span>
                                <span class="mcwws-gis-menu-layer-name">${escapeHtml(l.name)}</span>
                                <span class="mcwws-gis-menu-layer-count">${(l.features || []).length}</span>
                            </button>
                            <label class="mcwws-gis-menu-layer-vis" title="显示图层">
                                <input type="checkbox" data-layer-visible="${escapeHtml(l.id)}" ${l.visible ? 'checked' : ''}>
                                <span aria-hidden="true">👁</span>
                            </label>
                        </div>
                    `).join('')}
                </div>
                ${
                    selectedIds.length
                        ? `<p class="mcwws-gis-menu-selected">${selectedIds.length === 1
                            ? `选中：${escapeHtml(findFeatureById(selectedIds[0])?.feature.properties?.name || selectedIds[0])}`
                            : `已选 ${selectedIds.length} 项`}</p>`
                        : ''
                }
                ${renderRoadPropertiesPanelHtml()}
            </div>
        `;
    }

    function renderLayerDialog() {
        const wrap = ensureGisControls();
        const dialog = wrap?.querySelector('.mcwws-layer-dialog');
        if (!dialog) {
            return;
        }
        dialog.hidden = !layerDialogOpen;

        dialog.innerHTML = `
            <p class="mcwws-layer-dialog-title">图层</p>
            <div class="mcwws-layer-dialog-modes">
                <button type="button" class="mcwws-layer-mode-card${mapRenderMode === 'original' ? ' is-active' : ''}"
                    data-map-mode="original">
                    <span class="mcwws-layer-mode-preview mcwws-layer-mode-preview--original" aria-hidden="true"></span>
                    <span class="mcwws-layer-mode-label">原版地图</span>
                    <span class="mcwws-layer-mode-desc">高清区块细节</span>
                </button>
                <button type="button" class="mcwws-layer-mode-card${mapRenderMode === 'simplified' ? ' is-active' : ''}"
                    data-map-mode="simplified">
                    <span class="mcwws-layer-mode-preview mcwws-layer-mode-preview--simplified" aria-hidden="true"></span>
                    <span class="mcwws-layer-mode-label">简化地图</span>
                    <span class="mcwws-layer-mode-desc">纯白画布 · 仅标注</span>
                </button>
            </div>
            <label class="mcwws-layer-gis-toggle${isSimplifiedMapMode() ? ' is-locked' : ''}">
                <input type="checkbox" data-gis-info-toggle ${gisInfoEnabled ? 'checked' : ''}
                    ${isSimplifiedMapMode() ? 'disabled' : ''}>
                <span>开启地理信息${isSimplifiedMapMode() ? '（简化地图下始终开启）' : ''}</span>
            </label>
            <button type="button" class="mcwws-layer-edit-entry${!gisInfoEnabled ? ' is-disabled' : ''}"
                data-action="toggle-gis-editor"
                ${!gisInfoEnabled ? 'disabled' : ''}
                title="${!gisInfoEnabled ? '请先开启地理信息' : ''}">
                ${
                    !gisInfoEnabled
                        ? '编辑地理标注'
                        : (gisEditorOpen ? '收起地理标注编辑' : '编辑地理标注')
                }
            </button>
            ${gisInfoEnabled && gisEditorOpen ? renderGisEditorHtml() : ''}
            <p class="mcwws-gis-menu-status${statusKind ? ` is-${statusKind}` : ''}">${escapeHtml(statusMessage)}${dirty ? ' · 未保存' : ''}</p>
        `;
        updateGisButtonState();
    }

    function renderPanel() {
        renderLayerDialog();
    }

    function bindGisControlEvents(wrap) {
        if (gisControlsBound) {
            return;
        }
        gisControlsBound = true;

        wrap.querySelector('.mcwws-ctrl-gis')?.addEventListener('click', (e) => {
            e.stopPropagation();
            layerDialogOpen = !layerDialogOpen;
            if (layerDialogOpen) {
                const layerMenu = document.querySelector('.mcwws-layer-menu');
                if (layerMenu) {
                    layerMenu.hidden = true;
                }
            }
            renderLayerDialog();
        });

        wrap.addEventListener('change', (e) => {
            const roadInput = e.target.closest('[data-road-prop]');
            if (roadInput) {
                e.stopPropagation();
                applyRoadPropertyInput(roadInput);
                return;
            }
            if (e.target.matches('[data-vertex-lane], [data-vertex-display-height]')) {
                e.stopPropagation();
                applyVertexSettingsFromPanel();
            }
        });

        wrap.addEventListener('click', (e) => {
            e.stopPropagation();
            const modeCard = e.target.closest('[data-map-mode]');
            if (modeCard) {
                applyMapRenderMode(modeCard.getAttribute('data-map-mode'));
                return;
            }
            const gisToggle = e.target.closest('[data-gis-info-toggle]');
            if (gisToggle && e.target.matches('input[type="checkbox"]')) {
                setGisInfoEnabled(e.target.checked);
                return;
            }
            const toolBtn = e.target.closest('[data-tool]');
            if (toolBtn) {
                activeTool = toolBtn.getAttribute('data-tool') || 'select';
                cancelGisLasso();
                clearGisSelection();
                draftPoints = [];
                draftHover = null;
                syncDrawingClass();
                renderLayerDialog();
                renderOverlay();
                return;
            }
            const layerPick = e.target.closest('[data-layer-pick]');
            if (layerPick) {
                activeLayerId = layerPick.getAttribute('data-layer-pick') || activeLayerId;
                renderLayerDialog();
                return;
            }
            const visInput = e.target.closest('[data-layer-visible]');
            if (visInput && e.target.matches('input[type="checkbox"]')) {
                const id = visInput.getAttribute('data-layer-visible');
                const l = project?.layers?.find((x) => x.id === id);
                if (l) {
                    recordGisHistory();
                    l.visible = e.target.checked;
                    markDirty();
                    renderOverlay();
                    renderLayerDialog();
                }
                return;
            }
            const actionBtn = e.target.closest('[data-action]');
            if (!actionBtn) {
                return;
            }
            const action = actionBtn.getAttribute('data-action');
            if (action === 'toggle-gis-editor') {
                if (!gisInfoEnabled) {
                    return;
                }
                if (!gisCanEdit) {
                    requestAuthFromParent();
                    if (window.parent !== window) {
                        window.parent.postMessage({ type: 'mcwws-auth-required' }, '*');
                    }
                    return;
                }
                if (gisEditorOpen) {
                    closeGisEditorPanel();
                } else {
                    openGisEditorPanel();
                }
                renderLayerDialog();
                return;
            }
            if (action === 'save') {
                void saveGisProject();
            } else if (action === 'undo') {
                undoGisEdit();
            } else if (action === 'redo') {
                redoGisEdit();
            } else if (action === 'finish-draft') {
                finishDraft();
            } else if (action === 'cancel-draft') {
                cancelDraft();
            } else if (action === 'export') {
                exportGeoJson();
            } else if (action === 'delete') {
                deleteSelectedFeature();
            }
        });
    }

    function waitForMapControls(attemptsLeft = 240) {
        const wrap = ensureGisControls();
        if (wrap) {
            renderLayerDialog();
            return;
        }
        if (attemptsLeft <= 0) {
            console.warn('[mcwws-gis] 未找到地图控件栏，已使用悬浮按钮模式（若仍不可见，请检查 CSS 是否加载）');
            return;
        }
        requestAnimationFrame(() => waitForMapControls(attemptsLeft - 1));
    }

    function shouldHandleGisEditShortcut() {
        return gisEditMode && gisCanEdit && gisEditorOpen && !isInputFocused();
    }

    function onKeyDownCapture(event) {
        if (!shouldHandleGisEditShortcut()) {
            return;
        }
        const key = String(event.key || '').toLowerCase();
        if ((event.ctrlKey || event.metaKey) && key === 's') {
            event.preventDefault();
            event.stopPropagation();
            if (!saving) {
                void saveGisProject();
            }
            return;
        }
        if (event.key !== 'Delete' && event.key !== 'Backspace') {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        if (isGisSelectMode() && hasSelectedVertices()) {
            deleteSelectedVertices();
            return;
        }
        if (hasGisSelection()) {
            deleteSelectedFeature();
        }
    }

    function onKeyDown(event) {
        if (event.key === 'Escape') {
            if (gisLassoPointer) {
                cancelGisLasso();
                event.preventDefault();
                return;
            }
            if (gisEditMode && isGisSelectMode() && hasSelectedVertices()) {
                clearSelectedVertices();
                renderOverlay();
                event.preventDefault();
                return;
            }
            if (gisEditMode && isGisSelectMode() && hasGisSelection()) {
                clearGisSelection();
                renderOverlay();
                renderPanel();
                event.preventDefault();
                return;
            }
            if (gisEditMode && (draftPoints.length || activeTool !== 'select')) {
                cancelDraft();
                return;
            }
            if (gisEditorOpen) {
                closeGisEditorPanel();
                renderLayerDialog();
            }
            return;
        }
        if (!gisEditMode) return;
        if (isInputFocused()) return;
        if (event.ctrlKey || event.metaKey) {
            const modKey = String(event.key || '').toLowerCase();
            if (modKey === 's') {
                event.preventDefault();
                if (!saving) {
                    void saveGisProject();
                }
                return;
            }
            if (modKey === 'z' && !event.shiftKey) {
                event.preventDefault();
                undoGisEdit();
                return;
            }
            if (modKey === 'y' || (modKey === 'z' && event.shiftKey)) {
                event.preventDefault();
                redoGisEdit();
                return;
            }
        }
        if (event.key === 'Enter' && draftPoints.length) {
            finishDraft();
        }
    }

    function onDblClick(event) {
        if (!gisInfoEnabled || !gisEditMode || !gisCanEdit) return;
        if (activeTool !== 'line' && activeTool !== 'polygon') return;
        if (Date.now() - gisLastMapDragAt < 450) return;
        if (!isGisPickTarget(event.target)) return;
        event.stopPropagation();
        finishDraft();
    }

    function isInputFocused() {
        const tag = document.activeElement?.tagName;
        return tag === 'INPUT' || tag === 'TEXTAREA';
    }

    function tick() {
        renderOverlay();
        animationId = requestAnimationFrame(tick);
    }

    function enableMapRenderTransitions() {
        document.body.classList.remove('mcwws-map-render-no-transition');
    }

    function start() {
        if (started) return;
        started = true;
        document.body.classList.add('mcwws-map-render-no-transition');
        document.getElementById('mcwws-gis-panel')?.remove();
        loadLayerPrefs();
        initMapAuth();
        bindMapPicks();
        waitForMapControls();
        tryApplyStoredMapRenderMode();
        syncMapRenderModeVisual();
        requestAnimationFrame(() => {
            requestAnimationFrame(enableMapRenderTransitions);
        });
        void loadGisProject().then(() => {
            if (gisInfoEnabled) {
                renderOverlay();
            }
            syncMapRenderModeVisual();
        });
        document.addEventListener('keydown', onKeyDownCapture, true);
        document.addEventListener('keydown', onKeyDown);
        document.addEventListener('pointerleave', clearGisSelectHover);
        document.addEventListener('dblclick', onDblClick, true);
        window.addEventListener('hashchange', () => {
            gisCachedCamera = null;
            renderOverlay();
        });
        window.addEventListener('resize', renderOverlay);
        animationId = requestAnimationFrame(tick);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start, { once: true });
    } else {
        start();
    }
})();
