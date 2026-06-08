(function () {
    const MCWWS_GIS_BUILD = '20260602-88';
    /** BlueMap 地形渲染使用 10000 方块分块原点，避免大坐标 float32 精度丢失 */
    const GIS_VOLUME_CHUNK_SIZE = 10000;
    /** 原版地图：使用 BlueMap MarkerFill shader（对数深度缓冲），与地形同一 Z 测试 */
    const GIS_VOLUME_WEBGL_ENABLED = true;
    const GIS_VOLUME_FACE_BACK_EPS = -0.015;
    const GIS_VOLUME_FACE_MIN_SCREEN_AREA = 2.5;
    const GIS_VOLUME_LIGHT_DIR = Object.freeze({ x: 0.38, y: 0.9, z: 0.22 });
    const API_PORT = 8002;
    const NODE_API = `${window.location.protocol}//${window.location.hostname}:${API_PORT}`;
    console.info('[mcwws-gis] loaded', { build: MCWWS_GIS_BUILD });
    window.mcwwsGisVolumeDiag = () => ({
        build: MCWWS_GIS_BUILD,
        status: 'initializing'
    });
    const GIS_WRAP_ID = 'mcwws-gis-wrap';
    const MAP_CONTROLS_STACK_SEL = '.mcwws-map-controls-stack';
    const SVG_LAYER_ID = 'mcwws-gis-svg-layer';
    const ROAD_NAME_SVG_LAYER_ID = 'mcwws-gis-road-name-layer';
    const PIN_LAYER_ID = 'mcwws-gis-pin-layer';
    const VERTEX_LAYER_ID = 'mcwws-gis-vertex-layer';
    const VERTEX_GIZMO_ID = 'mcwws-gis-vertex-gizmo';
    const LASSO_LAYER_ID = 'mcwws-gis-lasso-layer';
    const GIS_LASSO_POINT_MIN_DIST_PX = 5;
    const GIS_LASSO_MIN_POINTS = 4;
    const GIS_LASSO_MIN_DIAG_PX = 12;
    const GIS_DEFAULT_Y = 64;
    const GIS_DEFAULT_ROAD_COLOR = '#C0CDD7';
    const GIS_DEFAULT_REGION_COLOR = '#DFEAF3';
    const GIS_VOLUME_SELECTION_STROKE = '#f59e0b';
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
    const GIS_PASTE_OFFSET_BLOCKS = 8;
    const GIS_ROAD_DUAL_DEFAULT_SPLIT_HEIGHT = 80;
    const GIS_ROAD_ARROW_SIZE_PX = 8;
    const GIS_ROAD_ARROW_MAX_PER_SEGMENT = 3;
    const GIS_ROAD_NAME_MIN_CHAIN_WORLD = 20;
    const GIS_ROAD_NAME_TANGENT_WORLD = 8;
    const GIS_ROAD_NAME_MAX_VIEW_HEIGHT = 9000;
    const GIS_ROAD_NAME_MIN_VISIBLE_PX = 3;
    const GIS_ROAD_NAME_SCREEN_MARGIN_PX = 48;
    const GIS_ROAD_NAME_DEDUP_PAD_PX = 10;
    const GIS_ROAD_NAME_SAME_NAME_MERGE_SCREEN_PX = 100;
    const GIS_ROAD_NAME_SAME_NAME_MERGE_WORLD_XZ = 24;
    const GIS_ROAD_NAME_MAX_FONT_PX = 13;
    const GIS_ROAD_NAME_MIN_FONT_PX = 9;
    const GIS_ROAD_NAME_FONT_HEIGHT_MIN = 140;
    const GIS_ROAD_NAME_MAX_TEXT_VS_ROAD = 1.65;
    const VOLUME_SHAPES = Object.freeze({
        FLAT: 'flat',
        BOX: 'box',
        CYLINDER: 'cylinder',
        HEXAHEDRON: 'hexahedron'
    });
    const VOLUME_SHAPE_OPTIONS = [
        { id: VOLUME_SHAPES.BOX, label: '柱状体' },
        { id: VOLUME_SHAPES.HEXAHEDRON, label: '六面体' }
    ];
    const VOLUME_DEFAULT_HEIGHT = 4;
    const VOLUME_CYLINDER_SEGMENTS = 24;

    let mapAuthToken = null;
    let mapAuthUser = null;
    let gisCanEdit = false;
    let gisEditMode = false;
    let gisIgnoreHeightClip = false;
    let gisShowRoadNames = true;
    /** 原版地图编辑模式下是否显示区域三维立体填充 */
    let gisShowVolume3dBuildings = true;
    let activeTool = 'select';
    let activeVolumeShape = VOLUME_SHAPES.BOX;
    let draftVolumePhase = null;
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
    const STORAGE_GIS_IGNORE_HEIGHT_CLIP = 'mcwws-gis-ignore-height-clip';
    const STORAGE_GIS_SHOW_ROAD_NAMES = 'mcwws-gis-show-road-names';
    const STORAGE_GIS_SHOW_VOLUME3D = 'mcwws-gis-show-volume3d';
    let dirty = false;
    let saving = false;
    let statusMessage = '';
    let statusKind = '';
    let animationId = 0;
    let started = false;
    let mapClickBound = false;
    let canvasClickBound = false;
    let volumeRenderHookBound = false;
    let pinElements = new Map();
    /** @type {Map<string, SVGPathElement>} */
    const svgPathElements = new Map();
    /** @type {Map<string, SVGGElement>} */
    const svgLaneArrowGroups = new Map();
    /** @type {Map<string, SVGGElement>} */
    const svgLaneNameGroups = new Map();
    /** @type {SVGPathElement | null} */
    let svgDraftPathEl = null;
    let svgDraftFillPathEl = null;
    let gisHoverFeatureId = null;
    let lastPickAt = 0;
    let lastPickKey = '';
    let gisCachedCamera = null;
    /** @type {import('three').Object3D | null} */
    let volumeMeshRoot = null;
    /** GIS 三维建筑独立图层（不参与 BlueMap markers / 地形深度合成） */
    let volumeMeshScene = null;
    /** @type {Map<string, { sig: string, group: object, anchor: { x: number, y: number, z: number }, dimmed?: boolean }>} */
    const volumeFeatureMeshes = new Map();
    const volumeWebGlFailedFeatureIds = new Set();
    /** BlueMap MapViewer 非 Three 对象，无 userData；用 WeakMap 存 render hook 状态 */
    const mcwwsVolumeLayerHookState = new WeakMap();
    /** @type {null | { Mesh: Function, BufferGeometry: Function, Float32BufferAttribute: Function, MeshBasicMaterial: Function, ShaderMaterial: Function, Color: Function, Group: Function, FrontSide: number, DoubleSide: number }} */
    let gisThree = null;
    /** BlueMap ExtrudeMarkerFill 材质原型（含 logdepthbuf），用于克隆 */
    let gisMarkerFillMaterialTemplate = null;
    /** @type {{ startX: number, startY: number, moved: boolean, pointerId: number } | null} */
    let gisCanvasPointer = null;
    let gisLastMapDragAt = 0;
    /** @type {Array<{ project: object, selectedFeatureIds: string[], activeLayerId: string }>} */
    let gisUndoStack = [];
    /** @type {Array<{ project: object, selectedFeatureIds: string[], activeLayerId: string }>} */
    let gisRedoStack = [];
    let gisHistoryApplying = false;
    /** @type {{ features: object[] } | null} */
    let gisClipboard = null;
    let gisClipboardPasteGen = 0;
    /** @type {Set<string>} 特征点多选，键为 featureId:vertexIndex */
    const selectedVertices = new Set();
    /** @type {{ axis: string, featureId: string, vertexIndex: number, startWorld: object, startClientX: number, startClientY: number, pointerId: number, historyRecorded: boolean, moved: boolean, screenAxis: object | null, cleanup: (() => void) | null } | null} */
    let gisVertexDrag = null;
    /** @type {Map<string, HTMLButtonElement>} */
    const vertexHandleElements = new Map();
    /** @type {{ featureId: string, segmentIndex: number, insertIndex: number, world: object, screenX: number, screenY: number, clientX: number, clientY: number } | null} */
    let gisHoverSegmentInsert = null;
    let segmentInsertHandleEl = null;
    let gisSegmentInsertModifierHeld = false;
    let gisLastPointerClientX = 0;
    let gisLastPointerClientY = 0;
    let gisVertexGizmoEl = null;
    let gisVertexGizmoBound = false;
    let gisVertexCoordHistoryPending = false;
    /** @type {{ points: { x: number, y: number }[], pointerId: number, captureEl: Element | null } | null } */
    let gisLassoPointer = null;
    /** @type {SVGPathElement | null} */
    let gisLassoPathEl = null;
    let gisLassoCaptureBound = false;
    let mapContextMenuBound = false;

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
            gisIgnoreHeightClip = localStorage.getItem(STORAGE_GIS_IGNORE_HEIGHT_CLIP) === '1';
            const roadNames = localStorage.getItem(STORAGE_GIS_SHOW_ROAD_NAMES);
            gisShowRoadNames = roadNames !== '0';
            const volume3d = localStorage.getItem(STORAGE_GIS_SHOW_VOLUME3D);
            gisShowVolume3dBuildings = volume3d !== '0';
        } catch {
            /* ignore */
        }
    }

    function saveLayerPrefs() {
        try {
            localStorage.setItem(STORAGE_RENDER_MODE, mapRenderMode);
            localStorage.setItem(STORAGE_GIS_ENABLED, gisInfoEnabled ? '1' : '0');
            localStorage.setItem(STORAGE_GIS_IGNORE_HEIGHT_CLIP, gisIgnoreHeightClip ? '1' : '0');
            localStorage.setItem(STORAGE_GIS_SHOW_ROAD_NAMES, gisShowRoadNames ? '1' : '0');
            localStorage.setItem(STORAGE_GIS_SHOW_VOLUME3D, gisShowVolume3dBuildings ? '1' : '0');
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
        gisThree = null;
        gisMarkerFillMaterialTemplate = null;
        syncVolume3dVisibilityClass();
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

    function findNamedThreeClass(root, className, seen = new Set(), depth = 0) {
        if (!root || depth > 10 || seen.has(root)) {
            return null;
        }
        if (typeof root === 'function' && root.name === className) {
            return root;
        }
        if (typeof root !== 'object') {
            return null;
        }
        seen.add(root);
        for (const key of Object.keys(root)) {
            if (key === 'parent' || key === 'children' || key === 'domElement') {
                continue;
            }
            const hit = findNamedThreeClass(root[key], className, seen, depth + 1);
            if (hit) {
                return hit;
            }
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
        ensureVertexIds(feature);
        dirty = true;
    }

    function getFeatureVertexPoints(feature) {
        return coordsToPoints(feature?.coordinates);
    }

    function pointsToCoordList(points) {
        return (points || []).map((p) => coerceVertexPoint(p)).filter(Boolean)
            .map((p) => ({ x: p.x, y: p.y, z: p.z }));
    }

    function generateVertexId(featureId) {
        const tag = typeof crypto !== 'undefined' && crypto.randomUUID
            ? crypto.randomUUID().replace(/-/g, '').slice(0, 12)
            : Math.random().toString(36).slice(2, 14);
        return `${featureId}-v-${tag}`;
    }

    function stripLegacyRoadLaneProps(feature) {
        if (!feature?.properties) {
            return;
        }
        const p = feature.properties;
        delete p.dualCarriageway;
        delete p.lanesPerSide;
        delete p.lanes;
        delete p.vertexLaneCounts;
        delete p.lodHeightCenterOnly;
        delete p.lodHeightAllLanes;
        delete p.lodMidLanes;
        delete p.defaultVertexDisplayHeight;
        delete p.defaultSegmentDisplayHeight;
        delete p.vertexDisplayHeights;
        delete p.segmentDisplayHeights;
        delete p.dualSplitHeight;
    }

    function getFeatureVertexCount(feature) {
        if (!feature) {
            return 0;
        }
        if (feature.type === 'Point' || feature.type === 'Label') {
            const pts = coordsToPoints(feature.coordinates);
            return pts.length >= 1 ? 1 : 0;
        }
        if (feature.type === 'LineString' || feature.type === 'Polygon') {
            return getFeatureVertexPoints(feature).length;
        }
        return 0;
    }

    function collectAllVertexIdsInProject() {
        const used = new Set();
        if (!project?.layers) {
            return used;
        }
        project.layers.forEach((layer) => {
            (layer.features || []).forEach((feature) => {
                const ids = feature.properties?.vertexIds;
                if (!Array.isArray(ids)) {
                    return;
                }
                ids.forEach((vid) => {
                    if (typeof vid === 'string' && vid) {
                        used.add(vid);
                    }
                });
            });
        });
        return used;
    }

    function assignFreshVertexIds(feature, usedIds) {
        if (!feature?.id) {
            return [];
        }
        const props = ensureFeatureProperties(feature);
        const count = getFeatureVertexCount(feature);
        props.vertexIds = [];
        for (let i = 0; i < count; i += 1) {
            props.vertexIds.push(generateUniqueVertexId(feature.id, usedIds));
        }
        return props.vertexIds;
    }

    function generateUniqueVertexId(featureId, usedIds) {
        let vid;
        let guard = 0;
        do {
            vid = generateVertexId(featureId);
            guard += 1;
        } while (usedIds.has(vid) && guard < 48);
        usedIds.add(vid);
        return vid;
    }

    function ensureVertexIds(feature) {
        if (!feature?.id) {
            return [];
        }
        const props = ensureFeatureProperties(feature);
        const count = getFeatureVertexCount(feature);
        const used = collectAllVertexIdsInProject();
        if (!Array.isArray(props.vertexIds)) {
            props.vertexIds = [];
        }
        props.vertexIds.forEach((vid) => {
            if (typeof vid === 'string' && vid) {
                used.delete(vid);
            }
        });
        while (props.vertexIds.length < count) {
            props.vertexIds.push(generateUniqueVertexId(feature.id, used));
        }
        if (props.vertexIds.length > count) {
            props.vertexIds.length = count;
        }
        const seenInFeature = new Set();
        for (let i = 0; i < props.vertexIds.length; i += 1) {
            let vid = props.vertexIds[i];
            if (typeof vid !== 'string' || !vid || seenInFeature.has(vid) || used.has(vid)) {
                vid = generateUniqueVertexId(feature.id, used);
                props.vertexIds[i] = vid;
            }
            seenInFeature.add(vid);
            used.add(vid);
        }
        return props.vertexIds;
    }

    function getVertexIdAt(feature, vertexIndex) {
        return ensureVertexIds(feature)[vertexIndex] || null;
    }

    function normalizeVisibilityEntry(raw) {
        if (!raw || typeof raw !== 'object') {
            return { min: null, max: null };
        }
        const minRaw = raw.min;
        const maxRaw = raw.max;
        const min = minRaw != null && minRaw !== '' && Number.isFinite(Number(minRaw)) ? Number(minRaw) : null;
        const max = maxRaw != null && maxRaw !== '' && Number.isFinite(Number(maxRaw)) ? Number(maxRaw) : null;
        return { min, max };
    }

    /** 无效范围（如 min≥max、0~0）视为未设置，始终可见 */
    function sanitizeVertexVisibilityEntry(raw) {
        const { min, max } = normalizeVisibilityEntry(raw);
        if (min == null && max == null) {
            return {};
        }
        const lo = min == null ? -Infinity : min;
        const hi = max == null ? Infinity : max;
        if (!(lo < hi)) {
            return {};
        }
        const out = {};
        if (min != null) {
            out.min = min;
        }
        if (max != null) {
            out.max = max;
        }
        return out;
    }

    function hasActiveVisibilityRange(entry) {
        const s = sanitizeVertexVisibilityEntry(entry);
        return s.min != null || s.max != null;
    }

    function ensureVertexVisibility(feature) {
        if (!featureSupportsVertices(feature)) {
            return [];
        }
        const props = ensureFeatureProperties(feature);
        const count = getFeatureVertexCount(feature);
        if (!Array.isArray(props.vertexVisibility)) {
            props.vertexVisibility = [];
        }
        while (props.vertexVisibility.length < count) {
            props.vertexVisibility.push({});
        }
        if (props.vertexVisibility.length > count) {
            props.vertexVisibility.length = count;
        }
        for (let i = 0; i < props.vertexVisibility.length; i += 1) {
            props.vertexVisibility[i] = sanitizeVertexVisibilityEntry(props.vertexVisibility[i]);
        }
        return props.vertexVisibility;
    }

    function getVertexVisibilityEntry(feature, vertexIndex) {
        return sanitizeVertexVisibilityEntry(ensureVertexVisibility(feature)[vertexIndex]);
    }

    function formatVisibilityBoundForInput(entry, which) {
        const v = entry?.[which];
        return v == null ? '' : String(v);
    }

    function formatVisibilityRangeLabel(entry) {
        const lo = entry.min == null ? '−∞' : String(entry.min);
        const hi = entry.max == null ? '+∞' : String(entry.max);
        return `${lo} < h ≤ ${hi}`;
    }

    /** 编辑模式下是否按顶点可视范围裁切显示 */
    function isGisHeightVisibilityActive() {
        return !(gisEditMode && gisIgnoreHeightClip);
    }

    function setGisIgnoreHeightClip(enabled) {
        gisIgnoreHeightClip = !!enabled;
        saveLayerPrefs();
        invalidateRoadArrowCache();
        document.body.classList.toggle('mcwws-gis-ignore-height-clip', gisEditMode && gisIgnoreHeightClip);
        renderOverlay();
        renderLayerDialog();
    }

    function setGisShowRoadNames(enabled) {
        gisShowRoadNames = !!enabled;
        saveLayerPrefs();
        invalidateRoadLabelCache();
        document.body.classList.toggle('mcwws-gis-road-names-off', !gisShowRoadNames);
        renderOverlay();
        renderLayerDialog();
    }

    /** 简化地图：始终显示；原版地图：仅编辑模式且开关开启时显示 */
    function shouldShowVolume3dSolids() {
        if (!gisInfoEnabled) {
            return false;
        }
        if (isSimplifiedMapMode()) {
            return true;
        }
        return gisEditMode && gisShowVolume3dBuildings;
    }

    /** 简化地图编辑模式下才显示三维建筑线框；原版地图不显示线框 */
    function shouldShowVolume3dWireframes() {
        return isSimplifiedMapMode() && gisEditMode;
    }

    function setGisShowVolume3dBuildings(enabled) {
        gisShowVolume3dBuildings = !!enabled;
        saveLayerPrefs();
        document.body.classList.toggle(
            'mcwws-gis-volume3d-visible',
            shouldShowVolume3dSolids()
        );
        renderOverlay();
        renderLayerDialog();
    }

    function purgeVolumeFillSvgPathsForFeatures(featureIds) {
        const keepWebGl = featureIds instanceof Set ? featureIds : new Set(featureIds);
        const staleKeys = [];
        svgPathElements.forEach((path, key) => {
            if (!key.includes(':volume-fill:')) {
                return;
            }
            const featureId = key.split(':')[0];
            if (!keepWebGl.has(featureId)) {
                return;
            }
            path.remove();
            staleKeys.push(key);
        });
        staleKeys.forEach((key) => svgPathElements.delete(key));
    }

    function queueVolumeSolidSvgFills(entries, view, camera, queue, dimmedByFeature) {
        entries.forEach(({ feature, dimmed }) => {
            const faceItems = buildSvgVolumeSolidFillPath(feature, view, camera);
            faceItems.forEach((item) => {
                queue.push({
                    featureId: feature.id,
                    fillKey: `${feature.id}:volume-fill:${item.index}`,
                    item,
                    dimmed: dimmedByFeature?.get(feature.id) ?? dimmed
                });
            });
        });
    }

    function syncVolume3dVisibilityClass() {
        document.body.classList.toggle('mcwws-gis-volume3d-visible', shouldShowVolume3dSolids());
    }

    function syncVolume3dRenderModeClass(useWebGl) {
        document.body.classList.toggle('mcwws-gis-volumes-webgl', !!useWebGl);
    }

    function getVertexIndexById(feature, vertexId) {
        if (!vertexId) {
            return -1;
        }
        const ids = ensureVertexIds(feature);
        return ids.indexOf(vertexId);
    }

    function normalizeRoadNameSegmentEntry(feature, raw) {
        if (!raw || typeof raw !== 'object') {
            return null;
        }
        const name = String(raw.name ?? '').trim();
        if (!name) {
            return null;
        }
        ensureVertexIds(feature);
        const count = getFeatureVertexCount(feature);
        if (count < 2) {
            return null;
        }
        let fromIndex = getVertexIndexById(feature, raw.fromVertexId);
        let toIndex = getVertexIndexById(feature, raw.toVertexId);
        if (fromIndex < 0 && Number.isFinite(Number(raw.fromIndex))) {
            fromIndex = Number(raw.fromIndex);
        }
        if (toIndex < 0 && Number.isFinite(Number(raw.toIndex))) {
            toIndex = Number(raw.toIndex);
        }
        fromIndex = Math.max(0, Math.min(fromIndex, count - 1));
        toIndex = Math.max(0, Math.min(toIndex, count - 1));
        if (fromIndex >= toIndex) {
            return null;
        }
        const ids = feature.properties.vertexIds;
        return {
            fromVertexId: ids[fromIndex],
            toVertexId: ids[toIndex],
            fromIndex,
            toIndex,
            name
        };
    }

    function getRoadNameSegments(feature) {
        if (!feature || feature.type !== 'LineString') {
            return [];
        }
        ensureVertexIds(feature);
        const count = getFeatureVertexCount(feature);
        if (count < 2) {
            return [];
        }
        const props = ensureFeatureProperties(feature);
        const raw = props.roadNameSegments;
        if (Array.isArray(raw) && raw.length) {
            return raw
                .map((entry) => normalizeRoadNameSegmentEntry(feature, entry))
                .filter(Boolean)
                .sort((a, b) => a.fromIndex - b.fromIndex);
        }
        const legacy = String(props.name || '').trim();
        if (!legacy) {
            return [];
        }
        const ids = props.vertexIds;
        return [{
            fromVertexId: ids[0],
            toVertexId: ids[count - 1],
            fromIndex: 0,
            toIndex: count - 1,
            name: legacy
        }];
    }

    function getRoadNameSegmentsSignature(feature) {
        return getRoadNameSegments(feature)
            .map((seg) => `${seg.fromVertexId}-${seg.toVertexId}:${seg.name}`)
            .join('|');
    }

    function featureHasAnyRoadName(feature) {
        if (getRoadNameSegments(feature).length) {
            return true;
        }
        return !!String(feature?.properties?.name || '').trim();
    }

    function persistRoadNameSegments(feature, segments) {
        const props = ensureFeatureProperties(feature);
        const next = (segments || [])
            .map((seg) => normalizeRoadNameSegmentEntry(feature, seg))
            .filter(Boolean)
            .map(({ fromVertexId, toVertexId, name }) => ({ fromVertexId, toVertexId, name }));
        if (!next.length) {
            delete props.roadNameSegments;
            delete props.name;
            return;
        }
        props.roadNameSegments = next;
        if (next.length === 1) {
            props.name = next[0].name;
        }
    }

    function reindexRoadNameSegments(feature) {
        const props = ensureFeatureProperties(feature);
        if (!Array.isArray(props.roadNameSegments) || !props.roadNameSegments.length) {
            return;
        }
        persistRoadNameSegments(feature, props.roadNameSegments);
    }

    function setRoadNameSegmentName(feature, segmentIndex, name) {
        const segments = getRoadNameSegments(feature);
        if (segmentIndex < 0 || segmentIndex >= segments.length) {
            return false;
        }
        const trimmed = String(name || '').trim();
        const next = segments.map((seg, i) => ({
            fromVertexId: seg.fromVertexId,
            toVertexId: seg.toVertexId,
            name: i === segmentIndex ? trimmed : seg.name
        })).filter((seg) => seg.name);
        persistRoadNameSegments(feature, next);
        return true;
    }

    function splitRoadNameAtVertex(feature, vertexIndex) {
        if (!feature || feature.type !== 'LineString') {
            return false;
        }
        ensureVertexIds(feature);
        const count = getFeatureVertexCount(feature);
        if (vertexIndex <= 0 || vertexIndex >= count - 1) {
            setStatus('请选择道路中间的特征点作为分界（不能是首尾端点）', 'error');
            return false;
        }
        let segments = getRoadNameSegments(feature);
        if (!segments.length) {
            const ids = feature.properties.vertexIds;
            segments = [{
                fromIndex: 0,
                toIndex: count - 1,
                fromVertexId: ids[0],
                toVertexId: ids[count - 1],
                name: ''
            }];
        }
        const segIdx = segments.findIndex((seg) => seg.fromIndex < vertexIndex && vertexIndex < seg.toIndex);
        if (segIdx < 0) {
            setStatus('该特征点不在可拆分的路名分段内（可能已是分界点）', 'error');
            return false;
        }
        const seg = segments[segIdx];
        const defaultBefore = seg.name || getRoadDisplayName(feature) || '';
        const beforeName = window.prompt(
            `第 ${seg.fromIndex + 1}–${vertexIndex + 1} 点段路名（沿线路顶点顺序，含分界点）`,
            defaultBefore
        );
        if (beforeName === null) {
            return false;
        }
        const afterName = window.prompt(
            `第 ${vertexIndex + 1}–${seg.toIndex + 1} 点段路名（沿线路顶点顺序，含分界点）`,
            ''
        );
        if (afterName === null) {
            return false;
        }
        const ids = feature.properties.vertexIds;
        const next = segments.slice();
        next.splice(
            segIdx,
            1,
            {
                fromVertexId: ids[seg.fromIndex],
                toVertexId: ids[vertexIndex],
                name: String(beforeName).trim()
            },
            {
                fromVertexId: ids[vertexIndex],
                toVertexId: ids[seg.toIndex],
                name: String(afterName).trim()
            }
        );
        recordGisHistory();
        persistRoadNameSegments(feature, next.filter((entry) => entry.name));
        markDirty();
        invalidateRoadLabelCache();
        renderOverlay();
        renderLayerDialog();
        setStatus('已在此特征点拆分路名分段', 'ok');
        return true;
    }

    function getRoadLabelWorldChainsForSegment(feature, segment, viewHeight) {
        const all = coordsToPoints(feature.coordinates);
        if (!all.length) {
            return [];
        }
        const from = Math.max(0, segment.fromIndex);
        const to = Math.min(all.length - 1, segment.toIndex);
        if (from >= to) {
            return [];
        }
        if (!isGisHeightVisibilityActive()) {
            return [all.slice(from, to + 1)];
        }
        const chains = [];
        let current = [];
        for (let i = from; i <= to; i += 1) {
            if (isVertexVisibleAtHeight(feature, i, viewHeight)) {
                current.push(all[i]);
            } else if (current.length >= 2) {
                chains.push(current);
                current = [];
            } else {
                current = [];
            }
        }
        if (current.length >= 2) {
            chains.push(current);
        }
        return chains;
    }

    function getRoadDisplayName(feature) {
        const segments = getRoadNameSegments(feature);
        if (segments.length === 1) {
            return segments[0].name;
        }
        if (segments.length > 1) {
            return segments.map((seg) => seg.name).join(' / ');
        }
        return String(feature?.properties?.name || '').trim();
    }

    function shouldShowRoadNameOnFeature(feature) {
        if (!gisShowRoadNames || !feature || feature.type !== 'LineString') {
            return false;
        }
        if (feature.properties?.showRoadName === false) {
            return false;
        }
        return featureHasAnyRoadName(feature);
    }

    /** 可视范围：a < 相机高度 ≤ b；未设 a 视为 −∞，未设 b 视为 +∞ */
    function isVertexVisibleAtHeight(feature, vertexIndex, viewHeight) {
        if (!isGisHeightVisibilityActive()) {
            return true;
        }
        const entry = getVertexVisibilityEntry(feature, vertexIndex);
        if (!hasActiveVisibilityRange(entry)) {
            return true;
        }
        if (!Number.isFinite(viewHeight)) {
            return true;
        }
        const { min, max } = entry;
        const lo = min == null ? -Infinity : min;
        const hi = max == null ? Infinity : max;
        return lo < viewHeight && viewHeight <= hi;
    }

    function isSegmentVisibleAtHeight(feature, indexA, indexB, viewHeight) {
        return isVertexVisibleAtHeight(feature, indexA, viewHeight)
            && isVertexVisibleAtHeight(feature, indexB, viewHeight);
    }

    /** 连续可见顶点拆成折线段（用于道路分级显示） */
    function buildVisiblePointChains(points, feature, viewHeight) {
        if (!points?.length) {
            return [];
        }
        const chains = [];
        let current = [];
        for (let i = 0; i < points.length; i += 1) {
            if (isVertexVisibleAtHeight(feature, i, viewHeight)) {
                current.push(points[i]);
            } else if (current.length >= 2) {
                chains.push(current);
                current = [];
            } else {
                current = [];
            }
        }
        if (current.length >= 2) {
            chains.push(current);
        }
        return chains;
    }

    function insertVertexIdAt(feature, insertIndex) {
        ensureVertexIds(feature);
        ensureVertexVisibility(feature);
        const idx = Math.max(0, Math.min(insertIndex, feature.properties.vertexIds.length));
        feature.properties.vertexIds.splice(idx, 0, generateVertexId(feature.id));
        feature.properties.vertexVisibility.splice(idx, 0, {});
    }

    function removeVertexIdsAt(feature, sortedIndicesDesc) {
        ensureVertexIds(feature);
        ensureVertexVisibility(feature);
        sortedIndicesDesc.forEach((i) => {
            if (i >= 0 && i < feature.properties.vertexIds.length) {
                feature.properties.vertexIds.splice(i, 1);
            }
            if (i >= 0 && i < feature.properties.vertexVisibility.length) {
                feature.properties.vertexVisibility.splice(i, 1);
            }
        });
    }

    function setVertexVisibilityBound(feature, vertexIndex, which, rawValue) {
        ensureVertexVisibility(feature);
        const entry = { ...getVertexVisibilityEntry(feature, vertexIndex) };
        const trimmed = String(rawValue ?? '').trim();
        if (!trimmed) {
            entry[which] = null;
        } else {
            const n = Number(trimmed);
            if (!Number.isFinite(n)) {
                return false;
            }
            entry[which] = n;
        }
        const next = sanitizeVertexVisibilityEntry(entry);
        if (next.min != null && next.max != null && next.min >= next.max) {
            return false;
        }
        feature.properties.vertexVisibility[vertexIndex] = next;
        return true;
    }

    function normalizeGisFeature(feature) {
        if (!feature) {
            return;
        }
        stripLegacyRoadLaneProps(feature);
        ensureVertexIds(feature);
        ensureVertexVisibility(feature);
        if (feature.type === 'LineString') {
            setRoadTravelDirection(feature, getRoadTravelDirection(feature));
            reindexRoadNameSegments(feature);
        }
        if (feature.type === 'Polygon') {
            normalizeVolume3dFeature(feature);
        }
    }

    function normalizeGisProject(projectData) {
        (projectData?.layers || []).forEach((layer) => {
            (layer.features || []).forEach(normalizeGisFeature);
        });
    }

    function parseLaneKey(laneKey) {
        return { side: 'center', index: 0 };
    }

    function getFeatureLanePoints(feature, lane = 'center') {
        if (!feature) {
            return [];
        }
        if (feature.type === 'Point' || feature.type === 'Label') {
            return coordsToPoints(feature.coordinates);
        }
        if (feature.type === 'Polygon' && (lane === 'bottom' || lane === 'top')) {
            const split = splitBoxPrismPoints(getFeatureVertexPoints(feature));
            if (split) {
                return (lane === 'bottom' ? split.bottom : split.top).map((p) => ({ ...p }));
            }
            const cfg = getVolume3dConfig(feature);
            if (cfg?.shape === VOLUME_SHAPES.BOX) {
                const rings = getBoxPrismRings(getFeatureVertexPoints(feature), cfg);
                if (rings) {
                    return (lane === 'bottom' ? rings.bottom : rings.top).map((p) => ({ ...p }));
                }
            }
        }
        return getFeatureVertexPoints(feature);
    }

    function setFeatureLanePoints(feature, laneKey, points) {
        const next = pointsToCoordList(points);
        if (!next.length) {
            return;
        }
        if (feature.type === 'Polygon' && (laneKey === 'bottom' || laneKey === 'top')) {
            const split = splitBoxPrismPoints(getFeatureVertexPoints(feature));
            if (split) {
                const all = [...split.bottom, ...split.top];
                const count = Math.min(next.length, split.n);
                for (let i = 0; i < count; i += 1) {
                    const p = next[i];
                    if (laneKey === 'bottom') {
                        all[i] = { x: p.x, y: p.y, z: p.z };
                    } else {
                        all[split.n + i] = { x: p.x, y: p.y, z: p.z };
                    }
                }
                setFeatureCoordinatesFromPoints(feature, all);
                syncBoxVolumeYMeta(feature);
                return;
            }
        }
        setFeatureCoordinatesFromPoints(feature, next);
    }

    function isLaneActiveAtVertex(feature, laneKey, vertexIndex) {
        return true;
    }

    function isVertexDisplayedAtCamera(feature, vertexIndex) {
        return true;
    }

    /** @returns {{ lane: string, points: object[] }[]} */
    function getEditableLanesForFeature(feature) {
        if (!feature) {
            return [];
        }
        if (feature.type === 'Point' || feature.type === 'Label') {
            const pts = coordsToPoints(feature.coordinates);
            return pts.length ? [{ lane: 'center', points: pts }] : [];
        }
        if (feature.type === 'Polygon') {
            const cfg = getVolume3dConfig(feature);
            if (cfg?.shape === VOLUME_SHAPES.BOX) {
                ensureBoxPrismCoordinates(feature);
                const split = splitBoxPrismPoints(getFeatureVertexPoints(feature));
                if (split && split.n >= 3) {
                    return [
                        { lane: 'bottom', points: split.bottom.map((p) => ({ ...p })) },
                        { lane: 'top', points: split.top.map((p) => ({ ...p })) }
                    ];
                }
            }
        }
        const points = getFeatureVertexPoints(feature);
        return points.length ? [{ lane: 'center', points }] : [];
    }

    function shouldEditLanesSeparately(feature) {
        return false;
    }

    function getPrimaryRoadVertexSelection() {
        const targets = getSelectedRoadVertexTargets();
        return targets.length === 1 ? targets[0] : null;
    }

    /** 当前选中道路上的特征点（支持多选批量编辑） */
    function getSelectedRoadVertexTargets() {
        const road = getSelectedLineStringRoad();
        if (!road || !selectedVertices.size) {
            return [];
        }
        const targets = [];
        selectedVertices.forEach((key) => {
            const sel = parseVertexSelectionKey(key);
            if (!sel || sel.featureId !== road.feature.id) {
                return;
            }
            targets.push({ feature: road.feature, vertexIndex: sel.vertexIndex });
        });
        targets.sort((a, b) => a.vertexIndex - b.vertexIndex);
        return targets;
    }

    function getBatchVisibilityBoundDisplay(targets, which) {
        const values = targets.map((t) => getVertexVisibilityEntry(t.feature, t.vertexIndex)[which]);
        const keys = values.map((v) => (v == null ? '' : String(v)));
        const uniq = [...new Set(keys)];
        if (uniq.length === 1) {
            return { value: formatVisibilityBoundForInput({ [which]: values[0] }, which), mixed: false };
        }
        return { value: '', mixed: true };
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
            if (lane === 'center' || lane === 'bottom' || lane === 'top' || lane === 'left' || lane === 'right' || /^(left|right)-\d+$/.test(lane)) {
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

    function isControlOrMetaKeyEvent(event) {
        const key = event.key;
        if (key === 'Control' || key === 'Meta' || key === 'OS') {
            return true;
        }
        const code = event.code;
        return typeof code === 'string'
            && /^(Control|Meta)(Left|Right)?$/.test(code);
    }

    function isGisSegmentInsertModifierHeld(event) {
        if (event && (event.ctrlKey || event.metaKey)) {
            return true;
        }
        return gisSegmentInsertModifierHeld;
    }

    function syncGisSegmentInsertModifierFromEvent(event) {
        if (!event) {
            return;
        }
        gisSegmentInsertModifierHeld = !!(event.ctrlKey || event.metaKey);
    }

    function canShowSegmentInsertUi(event) {
        return isGisSelectMode()
            && shouldShowVertexHandles()
            && isGisSegmentInsertModifierHeld(event);
    }

    function refreshSegmentInsertHoverAtLastPointer() {
        if (!isGisSelectMode()) {
            clearGisHoverSegmentInsert();
            return;
        }
        updateGisHoverSegmentInsert(gisLastPointerClientX, gisLastPointerClientY);
    }

    function onGisSegmentInsertModifierKey(event) {
        if (!isGisEditorActive()) {
            return;
        }
        if (!isControlOrMetaKeyEvent(event)) {
            return;
        }
        gisSegmentInsertModifierHeld = event.type === 'keydown'
            ? true
            : !!(event.ctrlKey || event.metaKey);
        document.body.classList.toggle(
            'mcwws-gis-ctrl-segment-insert',
            gisSegmentInsertModifierHeld && shouldShowVertexHandles()
        );
        if (!gisSegmentInsertModifierHeld) {
            clearGisHoverSegmentInsert();
        } else {
            refreshSegmentInsertHoverAtLastPointer();
        }
    }

    function tryInsertSegmentAtScreen(clientX, clientY, event) {
        if (!canShowSegmentInsertUi(event)) {
            return false;
        }
        if (isPointerOverLayerDialog(clientX, clientY)) {
            return false;
        }
        const seg = gisHoverSegmentInsert
            || pickSegmentInsertAtScreen(clientX, clientY);
        if (!seg) {
            return false;
        }
        insertFeatureVertex(
            seg.featureId,
            seg.lane || 'center',
            seg.insertIndex,
            seg.world,
            event
        );
        return true;
    }

    function insertFeatureVertexInternal(featureId, lane, insertIndex, point, options = {}) {
        const found = findFeatureById(featureId);
        if (!found) {
            return null;
        }
        const feature = found.feature;
        if (!['LineString', 'Polygon'].includes(feature.type)) {
            return null;
        }
        const next = coerceInsertVertexPoint(point);
        if (!next) {
            return null;
        }
        if (feature.type === 'Polygon' && isBoxPrismFeature(feature)) {
            ensureBoxPrismCoordinates(feature);
            if (!options.skipHistory) {
                recordGisHistory();
            }
            const inserted = insertBoxPrismVertex(feature, lane || 'bottom', insertIndex, next);
            if (!inserted) {
                return null;
            }
            reindexRoadNameSegments(feature);
            if (!options.skipSelect) {
                selectVertex(featureId, inserted.lane, inserted.vertexIndex);
            }
            if (!options.skipDirty) {
                markDirty();
            }
            if (!options.skipRender) {
                renderOverlay();
                renderPanel();
            }
            return inserted.vertexIndex;
        }
        const laneId = lane || 'center';
        const pts = getFeatureLanePoints(feature, laneId).slice();
        const idx = Math.max(0, Math.min(insertIndex, pts.length));
        if (!options.skipHistory) {
            recordGisHistory();
        }
        pts.splice(idx, 0, next);
        insertVertexIdAt(feature, idx);
        setFeatureLanePoints(feature, laneId, pts);
        reindexRoadNameSegments(feature);
        if (!options.skipSelect) {
            selectVertex(featureId, laneId, idx);
        }
        if (!options.skipDirty) {
            markDirty();
        }
        if (!options.skipRender) {
            renderOverlay();
            renderPanel();
        }
        return idx;
    }

    function insertFeatureVertex(featureId, lane, insertIndex, point, event) {
        if (!isGisSegmentInsertModifierHeld(event)) {
            return;
        }
        clearGisHoverSegmentInsert();
        insertFeatureVertexInternal(featureId, lane, insertIndex, point);
    }

    function getLineEndpointExtendConfig(primary) {
        if (!primary) {
            return null;
        }
        const found = findFeatureById(primary.featureId);
        if (!found || found.feature.type !== 'LineString') {
            return null;
        }
        const lane = primary.lane || 'center';
        const pts = getFeatureLanePoints(found.feature, lane);
        if (pts.length < 2) {
            return null;
        }
        const idx = primary.vertexIndex;
        const last = pts.length - 1;
        if (idx !== 0 && idx !== last) {
            return null;
        }
        return {
            featureId: found.feature.id,
            lane,
            endpointIndex: idx,
            end: idx === 0 ? 'start' : 'end'
        };
    }

    function applyLineExtendInsert(extendConfig) {
        const { featureId, lane, endpointIndex, end } = extendConfig;
        const endpoint = getVertexWorld(featureId, lane, endpointIndex);
        if (!endpoint) {
            return null;
        }
        const found = findFeatureById(featureId);
        if (!found) {
            return null;
        }
        const pts = getFeatureLanePoints(found.feature, lane);
        const insertIndex = end === 'end' ? pts.length : 0;
        const idx = insertFeatureVertexInternal(
            featureId,
            lane,
            insertIndex,
            { ...endpoint },
            { skipSelect: true, skipRender: true, skipDirty: true }
        );
        if (idx == null) {
            return null;
        }
        clearGisHoverSegmentInsert();
        selectVertex(featureId, lane, idx);
        return {
            featureId,
            lane,
            vertexIndex: idx,
            startWorld: { ...endpoint }
        };
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

    function isPointerOverLayerDialog(clientX, clientY) {
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

    function resolveAxisDragWorld(screenAxis, axis, anchorWorld, startClientX, startClientY, clientX, clientY, camera) {
        const useScreen = screenAxis
            && screenAxis.len >= 1.5
            && (axis !== 'y' || screenAxis.pixelsPerWorld >= 0.35);
        if (useScreen) {
            return dragVertexAlongAxisAtScreen(
                screenAxis,
                axis,
                anchorWorld,
                startClientX,
                startClientY,
                clientX,
                clientY
            );
        }
        return dragVertexAlongAxis3D(axis, anchorWorld, clientX, clientY, camera);
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
        const extendCtrl = !!(event.ctrlKey || event.metaKey);
        let extendConfig = null;
        if (vertexMode && extendCtrl && dragTargets.length === 1) {
            extendConfig = getLineEndpointExtendConfig(getPrimarySelectedVertex());
        }
        event.preventDefault();
        event.stopPropagation();
        if (gisVertexDrag?.cleanup) {
            gisVertexDrag.cleanup();
        }
        gisVertexDrag = {
            mode: extendConfig ? 'extend' : (vertexMode ? 'vertex' : 'feature'),
            axis,
            anchorWorld: { x: world.x, y: world.y, z: world.z },
            dragTargets,
            extendConfig,
            extendPending: !!extendConfig,
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
            if (gisVertexDrag.mode === 'extend' && gisVertexDrag.extendPending) {
                const inserted = applyLineExtendInsert(gisVertexDrag.extendConfig);
                if (!inserted) {
                    endVertexAxisDrag(e);
                    return;
                }
                gisVertexDrag.dragTargets = [inserted];
                gisVertexDrag.extendPending = false;
                gisVertexDrag.historyRecorded = true;
            }
            if (!gisVertexDrag.historyRecorded) {
                recordGisHistory();
                gisVertexDrag.historyRecorded = true;
            }
            const nextAnchor = resolveAxisDragWorld(
                gisVertexDrag.screenAxis,
                gisVertexDrag.axis,
                gisVertexDrag.anchorWorld,
                gisVertexDrag.startClientX,
                gisVertexDrag.startClientY,
                e.clientX,
                e.clientY,
                getGisBlueMapCamera()
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
        if (isPointerOverLayerDialog(clientX, clientY)) {
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
        const viewHeight = getMapCameraHeight();
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
                if (!isSegmentVisibleAtHeight(feature, i0, i1, viewHeight)) {
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

    function updateGisHoverSegmentInsert(clientX, clientY, event) {
        if (!canShowSegmentInsertUi(event) || gisVertexDrag) {
            clearGisHoverSegmentInsert();
            return;
        }
        if (isPointerOverLayerDialog(clientX, clientY)) {
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

    function onDocumentPointerMoveCapture(event) {
        if (!isGisSelectMode()) {
            return;
        }
        gisLastPointerClientX = event.clientX;
        gisLastPointerClientY = event.clientY;
        syncGisSegmentInsertModifierFromEvent(event);
        updateGisHoverSegmentInsert(event.clientX, event.clientY, event);
        document.body.classList.toggle(
            'mcwws-gis-ctrl-segment-insert',
            isGisSegmentInsertModifierHeld(event) && shouldShowVertexHandles()
        );
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
        const canExtendLine = vertexMode
            && n === 1
            && !!getLineEndpointExtendConfig(getPrimarySelectedVertex());
        gizmo.querySelectorAll('.mcwws-gis-axis').forEach((btn) => {
            const ax = (btn.getAttribute('data-axis') || 'x').toUpperCase();
            if (canExtendLine) {
                btn.title = `沿 ${ax} 轴移动；按住 Ctrl 拖动可延伸线段`;
            } else if (n > 1) {
                btn.title = `沿 ${ax} 轴移动已选 ${n} ${unit}`;
            } else {
                btn.title = `沿 ${ax} 轴移动`;
            }
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
                if (!isGisSegmentInsertModifierHeld(event)) {
                    return;
                }
                event.preventDefault();
                event.stopPropagation();
                insertFeatureVertex(
                    gisHoverSegmentInsert.featureId,
                    gisHoverSegmentInsert.lane || 'center',
                    gisHoverSegmentInsert.insertIndex,
                    gisHoverSegmentInsert.world,
                    event
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
        const snap = {
            center: clonePointList(getFeatureVertexPoints(feature)),
            vertexIds: JSON.parse(JSON.stringify(ensureVertexIds(feature)))
        };
        if (feature.type === 'Polygon') {
            const vol = feature.properties?.volume3d;
            if (vol && typeof vol === 'object') {
                snap.volume3d = {
                    shape: vol.shape,
                    minY: vol.minY,
                    maxY: vol.maxY
                };
            }
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
            if (Array.isArray(snap.vertexIds) && snap.vertexIds.length === centerNext.length) {
                found.feature.properties.vertexIds = snap.vertexIds.slice();
            }
            if (snap.volume3d && Math.abs(delta.y) > 1e-9) {
                const vol = ensureVolume3d(found.feature);
                const shape = normalizeVolumeShape(snap.volume3d.shape);
                if (shape === VOLUME_SHAPES.BOX || shape === VOLUME_SHAPES.CYLINDER) {
                    if (snap.volume3d.minY != null) {
                        vol.minY = snap.volume3d.minY + delta.y;
                    }
                    if (snap.volume3d.maxY != null) {
                        vol.maxY = snap.volume3d.maxY + delta.y;
                    }
                }
            }
            if (found.feature.type === 'Polygon') {
                if (isBoxPrismFeature(found.feature)) {
                    syncBoxVolumeYMeta(found.feature);
                } else {
                    normalizeVolume3dFeature(found.feature);
                }
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
        const viewHeight = getMapCameraHeight();
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
                    handle.setAttribute('data-fid', feature.id);
                    handle.setAttribute('data-lane', lane);
                    handle.setAttribute('data-idx', String(idx));
                    const vid = getVertexIdAt(feature, idx);
                    if (vid) {
                        handle.setAttribute('data-vertex-id', vid);
                        handle.title = `顶点 ${vid}`;
                    }
                    layer.appendChild(handle);
                    vertexHandleElements.set(key, handle);
                }
                handle.classList.toggle('is-active', isVertexSelected(feature.id, lane, idx));
                const visEntry = getVertexVisibilityEntry(feature, idx);
                const visHint = (visEntry.min != null || visEntry.max != null)
                    ? ` · 可视 ${formatVisibilityRangeLabel(visEntry)}`
                    : '';
                const vid = getVertexIdAt(feature, idx);
                handle.title = vid ? `顶点 ${vid}${visHint}` : `顶点${visHint}`;
                const projected = projectGisPoint(p, view, camera, false);
                const notVisible = !isVertexVisibleAtHeight(feature, idx, viewHeight);
                const off = notVisible || !projected || projected.behind
                    || projected.x < -40 || projected.y < -40
                    || projected.x > window.innerWidth + 40
                    || projected.y > window.innerHeight + 40;
                const underDialog = layerDialogOpen
                    && isPointerOverLayerDialog(projected.x, projected.y);
                handle.classList.toggle('is-offscreen', off || underDialog);
                if (!off && !underDialog) {
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
        if (!gisHoverSegmentInsert || !canShowSegmentInsertUi(null) || gisVertexDrag) {
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
            segmentInsertHandleEl.title = '按住 Ctrl 点击添加顶点';
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
            let dir = getScreenAxisDir(world, axis, view, camera);
            const el = gizmo.querySelector(`.mcwws-gis-axis--${axis}`);
            if (!el) {
                return;
            }
            if (!dir && axis === 'y') {
                dir = { ux: 0, uy: -1, len: 42, pixelsPerWorld: 0.1 };
            }
            if (!dir) {
                el.style.display = 'none';
                return;
            }
            el.style.display = 'block';
            const deg = (Math.atan2(dir.uy, dir.ux) * 180) / Math.PI;
            const widthPx = Math.max(
                GIS_GIZMO_AXIS_MIN_WIDTH_PX,
                Math.min(GIS_GIZMO_AXIS_MAX_WIDTH_PX, Math.max(dir.len, 24) * (dir.len >= 1.5 ? GIS_GIZMO_AXIS_WORLD_SPAN : 1))
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
        svgLaneNameGroups.forEach((el) => el.remove());
        svgLaneNameGroups.clear();
        if (svgDraftPathEl) {
            svgDraftPathEl.remove();
            svgDraftPathEl = null;
        }
        if (svgDraftFillPathEl) {
            svgDraftFillPathEl.remove();
            svgDraftFillPathEl = null;
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
        clearVolumeMeshes();
        syncVolume3dRenderModeClass(false);
    }

    function disposeVolumeMeshObject(obj) {
        if (!obj) {
            return;
        }
        obj.traverse?.((child) => {
            child.geometry?.dispose?.();
            if (child.material) {
                if (Array.isArray(child.material)) {
                    child.material.forEach((mat) => mat.dispose?.());
                } else {
                    child.material.dispose?.();
                }
            }
        });
    }

    function clearVolumeMeshes() {
        if (!volumeFeatureMeshes.size && !volumeMeshRoot) {
            return;
        }
        volumeFeatureMeshes.forEach((entry) => {
            volumeMeshRoot?.remove?.(entry.group);
            disposeVolumeMeshObject(entry.group);
        });
        volumeFeatureMeshes.clear();
        if (volumeMeshRoot?.parent) {
            volumeMeshRoot.parent.remove(volumeMeshRoot);
        }
        volumeMeshRoot = null;
        getBlueMapApp()?.mapViewer?.redraw?.();
    }

    function ensureVolumeMeshScene(mv, three) {
        if (!three) {
            return null;
        }
        if (!volumeMeshScene) {
            volumeMeshScene = new three.Group();
            volumeMeshScene.name = 'mcwws-gis-volume-layer';
            volumeMeshScene.matrixAutoUpdate = true;
        }
        return volumeMeshScene;
    }

    /** 独立 GIS 覆盖图层，不再挂到 BlueMap markers */
    function resolveVolumeMeshParent(mv, three) {
        return ensureVolumeMeshScene(mv, three || gisThree || resolveGisThree());
    }

    function getVolumeMeshParentLabel(mv, three) {
        const parent = resolveVolumeMeshParent(mv, three);
        if (!parent) {
            return 'none';
        }
        if (parent === volumeMeshScene) {
            return 'gis-overlay-layer';
        }
        return parent.name || parent.type || 'object3d';
    }

    function renderMcwwsVolumeLayer(mv) {
        if (!volumeMeshScene || !volumeFeatureMeshes.size || !shouldRenderVolumesWithWebGl()) {
            return;
        }
        const renderer = mv?.renderer;
        const camera = mv?.camera;
        if (!renderer || !camera) {
            return;
        }
        syncVolumeMeshChunkOffset(mv);
        volumeMeshScene.updateMatrixWorld?.(true);
        const autoClear = renderer.autoClear;
        renderer.autoClear = false;
        if (typeof renderer.clearDepth === 'function') {
            renderer.clearDepth();
        }
        renderer.render(volumeMeshScene, camera);
        renderer.autoClear = autoClear;
    }

    function getMcwwsVolumeLayerHookState(mv) {
        if (!mv) {
            return null;
        }
        let state = mcwwsVolumeLayerHookState.get(mv);
        if (!state) {
            state = { hookInstalled: false, originalRender: null };
            mcwwsVolumeLayerHookState.set(mv, state);
        }
        return state;
    }

    function installMcwwsVolumeLayerRenderer(mv) {
        if (!mv || typeof mv.render !== 'function') {
            return false;
        }
        const state = getMcwwsVolumeLayerHookState(mv);
        if (!state || state.hookInstalled) {
            return !!state?.hookInstalled;
        }
        if (!state.originalRender) {
            state.originalRender = mv.render.bind(mv);
        }
        const baseRender = state.originalRender;
        mv.render = function mcwwsRenderWithVolumeLayer(delta) {
            baseRender(delta);
            try {
                renderMcwwsVolumeLayer(mv);
            } catch (err) {
                console.warn('[mcwws-gis] renderMcwwsVolumeLayer failed', err);
            }
        };
        state.hookInstalled = true;
        return true;
    }

    /** 原版地图：WebGL + BlueMap 对数深度 shader；失败时回退 SVG */
    function shouldRenderVolumesWithWebGl() {
        if (!GIS_VOLUME_WEBGL_ENABLED) {
            return false;
        }
        if (isSimplifiedMapMode() || !gisInfoEnabled) {
            return false;
        }
        return !!getGisBlueMapCamera();
    }

    function resolveVolumeMeshParentLegacyMarkers(mv) {
        const markers = mv?.markers;
        if (markers && typeof markers.add === 'function') {
            return markers;
        }
        let markerSetRoot = null;
        markers?.traverse?.((obj) => {
            if (!markerSetRoot && obj?.isMarkerSet && typeof obj.add === 'function') {
                markerSetRoot = obj;
            }
        });
        return markerSetRoot;
    }

    /** 从旧版 BlueMap markers 挂载点移除残留 mesh */
    function detachVolumeMeshesFromBlueMapMarkers(mv) {
        const legacy = resolveVolumeMeshParentLegacyMarkers(mv);
        if (!legacy || !volumeMeshRoot) {
            return;
        }
        if (volumeMeshRoot.parent === legacy) {
            legacy.remove(volumeMeshRoot);
        }
    }

    function findThreeGroupCtor(mv, sample) {
        let groupCtor = null;
        const visit = (root) => {
            root?.traverse?.((obj) => {
                if (groupCtor || !obj?.isGroup || obj.isMarkerSet || obj.isMarker) {
                    return;
                }
                if (obj.type === 'Group') {
                    groupCtor = obj.constructor;
                }
            });
        };
        visit(mv.map?.hiresTileManager?.scene);
        if (!groupCtor && mv.map?.lowresTileManager?.length) {
            visit(mv.map.lowresTileManager[0].scene);
        }
        if (!groupCtor && sample?.parent) {
            let parent = sample.parent;
            while (parent) {
                if (parent.isGroup && parent.type === 'Group' && !parent.isMarkerSet) {
                    groupCtor = parent.constructor;
                    break;
                }
                parent = parent.parent;
            }
        }
        return groupCtor;
    }

    function findThreeObject3DCtor(meshCtor) {
        if (!meshCtor?.prototype) {
            return null;
        }
        const parentProto = Object.getPrototypeOf(meshCtor.prototype);
        return parentProto?.constructor && parentProto.constructor !== Object
            ? parentProto.constructor
            : null;
    }

    function isShaderLikeMaterial(mat) {
        return !!(mat && (
            mat.isShaderMaterial
            || mat.type === 'ShaderMaterial'
            || mat.type === 'LineMaterial'
            || mat.type === 'RawShaderMaterial'
            || (mat.vertexShader && mat.fragmentShader)
        ));
    }

    function createMinimalGisColorCtor() {
        function GisColor(r, g, b) {
            this.r = r ?? 1;
            this.g = g ?? 1;
            this.b = b ?? 1;
        }
        GisColor.prototype.setRGB = function setRGB(r, g, b) {
            this.r = r;
            this.g = g;
            this.b = b;
        };
        GisColor.prototype.clone = function clone() {
            return new GisColor(this.r, this.g, this.b);
        };
        return GisColor;
    }

    function collectMaterialFromObject(obj, bucket) {
        if (!obj?.material) {
            return;
        }
        const mats = Array.isArray(obj.material) ? obj.material : [obj.material];
        mats.forEach((mat) => {
            if (!mat) {
                return;
            }
            if (isShaderLikeMaterial(mat) && !bucket.shaderMaterial) {
                bucket.shaderMaterial = mat;
            }
            if (mat.color && !bucket.colorCtor) {
                bucket.colorCtor = mat.color.constructor;
            }
            if (mat.uniforms?.markerColor?.value && !bucket.colorCtor) {
                bucket.colorCtor = mat.uniforms.markerColor.value.constructor;
            }
            const diffuse = mat.uniforms?.diffuse?.value;
            if (diffuse && typeof diffuse.r === 'number' && !bucket.colorCtor) {
                bucket.colorCtor = diffuse.constructor;
            }
        });
    }

    function traverseMapViewerScenes(mv, visitor) {
        const roots = [
            mv?.markers,
            mv?.skyboxScene,
            mv?.map?.hiresTileManager?.scene,
            mv?.map?.hiresTileManager?.sceneParent
        ];
        if (mv?.map?.lowresTileManager?.length) {
            mv.map.lowresTileManager.forEach((tm) => {
                roots.push(tm.scene, tm.sceneParent);
            });
        }
        roots.forEach((root) => {
            root?.traverse?.((obj) => visitor(obj));
        });
    }

    function collectThreeSampleFromScene(mv) {
        let sample = null;
        let basicMaterial = null;
        const bucket = { shaderMaterial: null, colorCtor: null };
        const tryRoot = (root) => {
            root?.traverse?.((obj) => {
                collectMaterialFromObject(obj, bucket);
                if (!obj?.isMesh || !obj.geometry?.attributes?.position) {
                    return;
                }
                if (!sample) {
                    sample = obj;
                }
                if (!basicMaterial && obj.material?.isMeshBasicMaterial) {
                    basicMaterial = obj.material;
                }
            });
        };
        tryRoot(mv?.markers);
        traverseMapViewerScenes(mv, (obj) => {
            collectMaterialFromObject(obj, bucket);
            if (!sample && obj?.isMesh && obj.geometry?.attributes?.position) {
                sample = obj;
            }
            if (!basicMaterial && obj?.isMesh && obj.material?.isMeshBasicMaterial) {
                basicMaterial = obj.material;
            }
        });
        return {
            sample,
            basicMaterial,
            shaderMaterial: bucket.shaderMaterial,
            colorCtor: bucket.colorCtor
        };
    }

    function resolveShaderMaterialCtor(shaderMaterial, bm, mv) {
        if (shaderMaterial?.constructor) {
            return shaderMaterial.constructor;
        }
        return findNamedThreeClass(bm, 'ShaderMaterial')
            || findNamedThreeClass(mv, 'ShaderMaterial')
            || findNamedThreeClass(mv?.renderer, 'ShaderMaterial')
            || null;
    }

    function resolveColorCtor(colorCtor, shaderMaterial, basicMaterial, bm, mv) {
        return colorCtor
            || shaderMaterial?.uniforms?.markerColor?.value?.constructor
            || shaderMaterial?.uniforms?.diffuse?.value?.constructor
            || basicMaterial?.color?.constructor
            || findNamedThreeClass(bm, 'Color')
            || findNamedThreeClass(mv, 'Color')
            || createMinimalGisColorCtor();
    }

    function getMarkerFillShaderSources() {
        const vertexShader = [
            '#include <common>',
            '#include <logdepthbuf_pars_vertex>',
            '',
            'varying float vDistance;',
            '',
            'void main() {',
            '	vec4 worldPos = modelMatrix * vec4(position, 1.0);',
            '	vec4 viewPos = viewMatrix * worldPos;',
            '	vDistance = -viewPos.z;',
            '	gl_Position = projectionMatrix * viewPos;',
            '	#include <logdepthbuf_vertex>',
            '}'
        ].join('\n');
        const fragmentShader = [
            '#include <logdepthbuf_pars_fragment>',
            '',
            '#define FLT_MAX 3.402823466e+38',
            '',
            'varying float vDistance;',
            '',
            'uniform vec3 markerColor;',
            'uniform float markerOpacity;',
            'uniform float fadeDistanceMax;',
            'uniform float fadeDistanceMin;',
            '',
            'void main() {',
            '	vec4 color = vec4(markerColor, markerOpacity);',
            '	float fdMax = fadeDistanceMax > 0.0 ? fadeDistanceMax : FLT_MAX;',
            '	float minDelta = fadeDistanceMin > 0.0 ? (vDistance - fadeDistanceMin) / fadeDistanceMin : 1.0;',
            '	float maxDelta = fadeDistanceMax > 0.0 ? (vDistance - fadeDistanceMax) / (fadeDistanceMax * 0.5) : 0.0;',
            '	float distanceOpacity = min(',
            '		clamp(minDelta, 0.0, 1.0),',
            '		1.0 - clamp(maxDelta + 1.0, 0.0, 1.0)',
            '	);',
            '	color.a *= distanceOpacity;',
            '	gl_FragColor = color;',
            '	#include <logdepthbuf_fragment>',
            '}'
        ].join('\n');
        return { vertexShader, fragmentShader };
    }

    function createMarkerFillUniforms(ColorCtor) {
        return {
            markerColor: { value: new ColorCtor(1, 1, 1) },
            markerOpacity: { value: 1 },
            fadeDistanceMin: { value: 0 },
            fadeDistanceMax: { value: Number.MAX_VALUE }
        };
    }

    function applyMarkerFillShaderToMaterial(material, ColorCtor) {
        const { vertexShader, fragmentShader } = getMarkerFillShaderSources();
        material.vertexShader = vertexShader;
        material.fragmentShader = fragmentShader;
        material.uniforms = createMarkerFillUniforms(ColorCtor);
        material.depthTest = true;
        material.transparent = true;
        material.needsUpdate = true;
        material.userData = { ...(material.userData || {}), mcwwsBuiltinMarkerFill: true, mcwwsAdaptedShader: true };
        return material;
    }

    function resolveGisThree() {
        if (gisThree) {
            return gisThree;
        }
        const bm = getBlueMapApp();
        const mv = bm?.mapViewer;
        if (!bm || !mv) {
            return null;
        }
        const camera = mv.camera || findCamera(bm);
        if (!camera) {
            return null;
        }
        const { sample, basicMaterial, shaderMaterial, colorCtor } = collectThreeSampleFromScene(mv);
        if (!sample) {
            return null;
        }
        const MeshCtor = sample.constructor;
        const BufferGeometryCtor = sample.geometry.constructor;
        const Float32AttrCtor = sample.geometry.attributes.position.constructor;
        const MeshBasicMaterialCtor = basicMaterial?.constructor || null;
        const ShaderMaterialCtor = resolveShaderMaterialCtor(shaderMaterial, bm, mv);
        const ColorCtor = resolveColorCtor(colorCtor, shaderMaterial, basicMaterial, bm, mv);
        const groupCtor = findThreeGroupCtor(mv, sample) || findThreeObject3DCtor(MeshCtor) || MeshCtor;
        if (!MeshCtor || !BufferGeometryCtor || !Float32AttrCtor) {
            return null;
        }
        gisThree = {
            Mesh: MeshCtor,
            BufferGeometry: BufferGeometryCtor,
            Float32BufferAttribute: Float32AttrCtor,
            MeshBasicMaterial: MeshBasicMaterialCtor,
            ShaderMaterial: ShaderMaterialCtor,
            Color: ColorCtor,
            shaderMaterialSample: shaderMaterial || null,
            materialSample: basicMaterial || sample.material,
            Group: groupCtor,
            Object3D: groupCtor,
            FrontSide: 0,
            DoubleSide: 2
        };
        return gisThree;
    }

    function findMarkerFillMaterialInScene(mv) {
        let found = null;
        traverseMapViewerScenes(mv, (obj) => {
            if (found || !obj?.isMesh || !obj.material?.uniforms) {
                return;
            }
            const mat = obj.material;
            if (mat.uniforms.markerColor && mat.uniforms.markerOpacity) {
                found = mat;
            }
        });
        return found;
    }

    function findAnyShaderMaterialInScene(mv) {
        let found = null;
        traverseMapViewerScenes(mv, (obj) => {
            if (found) {
                return;
            }
            const mats = Array.isArray(obj?.material) ? obj.material : [obj?.material];
            mats.forEach((mat) => {
                if (!found && isShaderLikeMaterial(mat)) {
                    found = mat;
                }
            });
        });
        return found;
    }

    function createBuiltinMarkerFillMaterialPrototype(three, mv) {
        const ColorCtor = three.Color || createMinimalGisColorCtor();
        const { vertexShader, fragmentShader } = getMarkerFillShaderSources();
        try {
            if (three.ShaderMaterial) {
                const material = new three.ShaderMaterial({
                    vertexShader,
                    fragmentShader,
                    uniforms: createMarkerFillUniforms(ColorCtor),
            side: three.FrontSide ?? 0,
            depthTest: true,
            transparent: true
        });
        material.userData = { mcwwsBuiltinMarkerFill: true };
        return material;
            }
            const shaderSample = three.shaderMaterialSample || findAnyShaderMaterialInScene(mv);
            if (shaderSample?.clone) {
                const material = applyMarkerFillShaderToMaterial(shaderSample.clone(), ColorCtor);
                return material;
            }
        } catch (err) {
            console.warn('[mcwws-gis] createBuiltinMarkerFillMaterialPrototype failed', err);
        }
        return null;
    }

    function ensureMarkerFillMaterialTemplate(three, mv) {
        if (gisMarkerFillMaterialTemplate) {
            return gisMarkerFillMaterialTemplate;
        }
        const existing = findMarkerFillMaterialInScene(mv);
        if (existing) {
            gisMarkerFillMaterialTemplate = existing;
            return gisMarkerFillMaterialTemplate;
        }
        const created = createBuiltinMarkerFillMaterialPrototype(three, mv);
        if (created) {
            gisMarkerFillMaterialTemplate = created;
            return gisMarkerFillMaterialTemplate;
        }
        console.warn('[mcwws-gis] MarkerFill material unavailable; volume WebGL disabled for this frame');
        return null;
    }

    function getMarkerFillMaterialSourceLabel() {
        if (!gisMarkerFillMaterialTemplate) {
            return 'missing';
        }
        if (gisMarkerFillMaterialTemplate.userData?.mcwwsAdaptedShader) {
            return 'adapted';
        }
        if (gisMarkerFillMaterialTemplate.userData?.mcwwsBuiltinMarkerFill) {
            return 'builtin';
        }
        return 'cloned';
    }

    function ensureVolumeMeshRoot(mv, three) {
        detachVolumeMeshesFromBlueMapMarkers(mv);
        const parent = resolveVolumeMeshParent(mv, three);
        if (!parent) {
            return null;
        }
        if (!volumeMeshRoot) {
            volumeMeshRoot = new three.Object3D();
            volumeMeshRoot.name = 'mcwws-gis-volumes';
            volumeMeshRoot.userData = { chunkOx: null, chunkOz: null, mcwwsRenderSyncBound: false };
        }
        if (volumeMeshRoot.parent !== parent) {
            volumeMeshRoot.parent?.remove?.(volumeMeshRoot);
            parent.add(volumeMeshRoot);
            volumeMeshRoot.userData.mcwwsRenderSyncBound = false;
        }
        bindVolumeMeshRenderSync(mv);
        return volumeMeshRoot;
    }

    function bindVolumeMeshRenderSync(mv) {
        if (!volumeMeshRoot || volumeMeshRoot.userData.mcwwsRenderSyncBound) {
            return;
        }
        volumeMeshRoot.userData.mcwwsRenderSyncBound = true;
        volumeMeshRoot.onBeforeRender = () => {
            syncVolumeMeshChunkOffset(mv);
        };
    }

    function volumeFeaturePolygonOffsetUnits(featureId) {
        let hash = 0;
        const s = String(featureId || '');
        for (let i = 0; i < s.length; i += 1) {
            hash = ((hash << 5) - hash + s.charCodeAt(i)) | 0;
        }
        return (Math.abs(hash) % 6) + 1;
    }

    function applyVolumeMaterialState(mat, opacity, featureId) {
        if (!mat) {
            return;
        }
        const o = opacity ?? 1;
        const opaque = o >= 0.99;
        if (mat.uniforms?.markerOpacity) {
            mat.uniforms.markerOpacity.value = o;
        } else {
            mat.opacity = o;
        }
        mat.transparent = !opaque;
        mat.depthWrite = opaque;
        mat.depthTest = true;
        mat.polygonOffset = true;
        mat.polygonOffsetFactor = 1;
        mat.polygonOffsetUnits = volumeFeaturePolygonOffsetUnits(featureId);
    }

    function volumeFeatureMeshSignature(feature, layer) {
        const cfg = getVolume3dConfig(feature);
        return JSON.stringify({
            coordinates: feature.coordinates,
            shape: cfg?.shape,
            minY: cfg?.minY,
            maxY: cfg?.maxY,
            segments: cfg?.segments,
            color: getRegionVolumeFillColor(feature, layer)
        });
    }

    function parseShadedRgbColor(rgbText) {
        const m = String(rgbText || '').match(/rgb\((\d+),\s*(\d+),\s*(\d+)\)/);
        return {
            r: (m ? Number(m[1]) : 223) / 255,
            g: (m ? Number(m[2]) : 234) / 255,
            b: (m ? Number(m[3]) : 243) / 255
        };
    }

    function createVolumeMarkerFillMaterial(three, mv, rgb, opacity, featureId) {
        const template = ensureMarkerFillMaterialTemplate(three, mv);
        if (!template?.clone) {
            return null;
        }
        try {
            const material = template.clone();
            if (material.uniforms?.markerColor?.value?.setRGB) {
                material.uniforms.markerColor.value.setRGB(rgb.r, rgb.g, rgb.b);
            }
            if ('side' in material) {
                material.side = three.FrontSide ?? 0;
            }
            applyVolumeMaterialState(material, opacity ?? 1, featureId);
            material.needsUpdate = true;
            return material;
        } catch (err) {
            console.warn('[mcwws-gis] createVolumeMarkerFillMaterial failed', err);
            return null;
        }
    }

    function computeFeatureVolumeAnchor(faceSpecs, points) {
        let sx = 0;
        let sy = 0;
        let sz = 0;
        let n = 0;
        const addPoint = (p) => {
            sx += p.x;
            sy += gisWorldY(p, false);
            sz += p.z;
            n += 1;
        };
        if (faceSpecs?.length) {
            faceSpecs.forEach((spec) => {
                spec.ring.forEach(addPoint);
            });
        } else {
            points.forEach(addPoint);
        }
        if (!n) {
            return { x: 0, y: GIS_DEFAULT_Y, z: 0 };
        }
        return { x: sx / n, y: sy / n, z: sz / n };
    }

    function getVolumeMeshChunkOffset(mv) {
        const cam = mv?.camera;
        if (!cam) {
            return { ox: 0, oz: 0 };
        }
        const chunk = GIS_VOLUME_CHUNK_SIZE;
        return {
            ox: Math.round(cam.position.x / chunk) * chunk,
            oz: Math.round(cam.position.z / chunk) * chunk
        };
    }

    function syncVolumeMeshChunkOffset(mv) {
        if (!volumeMeshRoot || !volumeFeatureMeshes.size) {
            return;
        }
        const { ox, oz } = getVolumeMeshChunkOffset(mv);
        const prevOx = volumeMeshRoot.userData?.chunkOx;
        const prevOz = volumeMeshRoot.userData?.chunkOz;
        if (prevOx === ox && prevOz === oz) {
            return;
        }
        volumeMeshRoot.position.set(ox, 0, oz);
        volumeMeshRoot.userData.chunkOx = ox;
        volumeMeshRoot.userData.chunkOz = oz;
        volumeFeatureMeshes.forEach(({ group, anchor }) => {
            if (!group || !anchor) {
                return;
            }
            group.position.set(anchor.x - ox, anchor.y, anchor.z - oz);
        });
        volumeMeshRoot.updateMatrixWorld?.(true);
    }

    function volumeFaceTriangleNormalSign(tri, faceNormal) {
        const p0 = volumeWorldPoint(tri[0]);
        const p1 = volumeWorldPoint(tri[1]);
        const p2 = volumeWorldPoint(tri[2]);
        const cross = vec3Cross(vec3Sub(p1, p0), vec3Sub(p2, p0));
        return cross.x * faceNormal.x + cross.y * faceNormal.y + cross.z * faceNormal.z;
    }

    /** 面环是否为凸多边形（避免三角扇自交成“蝴蝶结”） */
    function isVolumeFaceRingConvex(ring, normal) {
        if (!ring || ring.length < 3) {
            return false;
        }
        const len = Math.hypot(normal.x, normal.y, normal.z);
        if (len < 1e-8) {
            return true;
        }
        const fn = { x: normal.x / len, y: normal.y / len, z: normal.z / len };
        let sign = 0;
        for (let i = 0; i < ring.length; i += 1) {
            const tri = [ring[i], ring[(i + 1) % ring.length], ring[(i + 2) % ring.length]];
            const dot = volumeFaceTriangleNormalSign(tri, fn);
            if (Math.abs(dot) < 1e-8) {
                continue;
            }
            const s = Math.sign(dot);
            if (sign === 0) {
                sign = s;
            } else if (s !== sign) {
                return false;
            }
        }
        return true;
    }

    /** 四边形侧面：顶底环绕序不一致时，交换顶边顶点顺序 */
    function resolveSideFaceRing(ring, normal) {
        if (!ring || ring.length !== 4) {
            return ring;
        }
        const orderA = ring;
        const orderB = [ring[0], ring[1], ring[3], ring[2]];
        if (isVolumeFaceRingConvex(orderA, normal)) {
            return orderA;
        }
        if (isVolumeFaceRingConvex(orderB, normal)) {
            return orderB;
        }
        return orderA;
    }

    function ensureConvexVolumeFaceRing(ring, normal) {
        if (!ring || ring.length < 3) {
            return ring;
        }
        if (isVolumeFaceRingConvex(ring, normal)) {
            return ring;
        }
        const reversed = [...ring].reverse();
        if (isVolumeFaceRingConvex(reversed, normal)) {
            return reversed;
        }
        if (ring.length === 4) {
            const swapped = [ring[0], ring[2], ring[1], ring[3]];
            if (isVolumeFaceRingConvex(swapped, normal)) {
                return swapped;
            }
        }
        return ring;
    }

    function triangulateVolumeFaceRing(ring, normal) {
        if (!ring || ring.length < 3) {
            return [];
        }
        const tris = [];
        if (ring.length === 3) {
            tris.push(orientTriangleForOutwardNormal([ring[0], ring[1], ring[2]], normal));
            return tris;
        }
        for (let i = 1; i < ring.length - 1; i += 1) {
            tris.push(orientTriangleForOutwardNormal([ring[0], ring[i], ring[i + 1]], normal));
        }
        return tris;
    }

    function prepareVolumeFaceRing(spec, normal) {
        let ring = spec.ring;
        if (spec.kind === 'side') {
            ring = resolveSideFaceRing(ring, normal);
        }
        ring = ensureConvexVolumeFaceRing(ring, normal);
        return orientFaceRingForOutwardNormal(ring, normal);
    }

    function buildVolumeFeatureMeshGroup(feature, layer, three, mv) {
        const cfg = getVolume3dConfig(feature);
        const points = coordsToPoints(feature.coordinates);
        const faceSpecs = buildVolumeSolidFaces(cfg.shape, points, cfg);
        const bottomRingCw = getBottomRingCwFlag(cfg.shape, points, cfg);
        const baseColor = getRegionVolumeFillColor(feature, layer);
        const anchor = computeFeatureVolumeAnchor(faceSpecs, points);
        const positions = [];
        const indices = [];
        const groups = [];
        const materials = [];
        const materialCache = new Map();

        const getMaterialIndex = (shaded) => {
            const key = `${shaded.r.toFixed(4)},${shaded.g.toFixed(4)},${shaded.b.toFixed(4)}`;
            if (materialCache.has(key)) {
                return materialCache.get(key);
            }
            const mat = createVolumeMarkerFillMaterial(three, mv, shaded, 1, feature.id);
            if (!mat) {
                return -1;
            }
            const idx = materials.length;
            materials.push(mat);
            materialCache.set(key, idx);
            return idx;
        };

        faceSpecs.forEach((spec) => {
            const normal = computeVolumeFaceNormal(spec.ring, spec.kind, bottomRingCw, spec.sideCorners);
            const ring = prepareVolumeFaceRing(spec, normal);
            const tris = triangulateVolumeFaceRing(ring, normal);
            if (!tris.length) {
                return;
            }
            const shaded = parseShadedRgbColor(shadeRegionFaceColor(baseColor, normal));
            const materialIndex = getMaterialIndex(shaded);
            if (materialIndex < 0) {
                return;
            }
            const indexStart = indices.length;
            tris.forEach((tri) => {
                const base = positions.length / 3;
                tri.forEach((v) => {
                    positions.push(
                        v.x - anchor.x,
                        gisWorldY(v, false) - anchor.y,
                        v.z - anchor.z
                    );
                });
                indices.push(base, base + 1, base + 2);
            });
            groups.push({
                start: indexStart,
                count: indices.length - indexStart,
                materialIndex
            });
        });

        if (!indices.length || !materials.length) {
            return null;
        }

        const geometry = new three.BufferGeometry();
        geometry.setAttribute('position', new three.Float32BufferAttribute(positions, 3));
        geometry.setIndex(indices);
        geometry.groups = groups;
        if (typeof geometry.computeBoundingSphere === 'function') {
            geometry.computeBoundingSphere();
        }
        if (typeof geometry.computeBoundingBox === 'function') {
            geometry.computeBoundingBox();
        }

        const mesh = new three.Mesh(geometry, materials.length === 1 ? materials[0] : materials);
        mesh.userData = { featureId: feature.id };
        const group = new three.Group();
        group.userData = { featureId: feature.id };
        group.add(mesh);

        const { ox, oz } = getVolumeMeshChunkOffset(mv);
        group.position.set(anchor.x - ox, anchor.y, anchor.z - oz);
        return { group, anchor };
    }

    function applyVolumeMeshStyle(group, dimmed, featureId) {
        group.traverse?.((child) => {
            if (!child.isMesh || !child.material) {
                return;
            }
            const opacity = dimmed ? 0.42 : 1;
            const mats = Array.isArray(child.material) ? child.material : [child.material];
            mats.forEach((mat) => {
                applyVolumeMaterialState(mat, opacity, featureId);
            });
        });
    }

    function getGisVolumeRenderDiag() {
        const bm = getBlueMapApp();
        const mv = bm?.mapViewer;
        const three = resolveGisThree();
        if (three && mv) {
            ensureMarkerFillMaterialTemplate(three, mv);
        }
        return {
            build: MCWWS_GIS_BUILD,
            simplifiedMap: isSimplifiedMapMode(),
            gisInfoEnabled,
            gisEditMode,
            gisShowVolume3dBuildings,
            showVolume3dSolids: shouldShowVolume3dSolids(),
            shouldWebGl: shouldRenderVolumesWithWebGl(),
            hasCamera: !!getGisBlueMapCamera(),
            hasThree: !!three,
            hasShaderMaterial: !!three?.ShaderMaterial,
            hasColor: !!three?.Color,
            hasShaderSample: !!three?.shaderMaterialSample,
            meshParent: !!resolveVolumeMeshParent(mv, three),
            meshParentKind: getVolumeMeshParentLabel(mv, three),
            volumeRenderLayer: 'post-map-overlay',
            activeMeshCount: volumeFeatureMeshes.size,
            svgFallbackCount: volumeWebGlFailedFeatureIds.size,
            webglClassOnBody: document.body.classList.contains('mcwws-gis-volumes-webgl'),
            volumeBackend: volumeFeatureMeshes.size > 0 ? 'webgl' : (shouldRenderVolumesWithWebGl() ? 'webgl+svg-fallback' : 'svg'),
            markerFillMaterial: getMarkerFillMaterialSourceLabel(),
            chunkOffset: getVolumeMeshChunkOffset(mv),
            modeHint: shouldRenderVolumesWithWebGl()
                ? 'GIS 三维建筑独立图层：地图渲染后叠加，不被地形深度遮挡；建筑之间仍按深度正确遮挡。'
                : '三维建筑走 SVG 面片。'
        };
    }

    function syncVolumeMeshes(entries) {
        volumeWebGlFailedFeatureIds.clear();
        try {
            const three = resolveGisThree();
            const mv = getBlueMapApp()?.mapViewer;
            if (!three || !mv || !shouldRenderVolumesWithWebGl()) {
                if (volumeFeatureMeshes.size || volumeMeshRoot) {
                    clearVolumeMeshes();
                }
                return false;
            }
            if (!ensureMarkerFillMaterialTemplate(three, mv)) {
                entries.forEach(({ feature }) => volumeWebGlFailedFeatureIds.add(feature.id));
                return false;
            }
            installMcwwsVolumeLayerRenderer(mv);
            ensureVolumeMeshRoot(mv, three);
            if (!volumeMeshRoot) {
                return false;
            }
            const needed = new Set();
            let changed = false;
            let activeCount = 0;
            entries.forEach(({ feature, layer, dimmed }) => {
                needed.add(feature.id);
                const sig = volumeFeatureMeshSignature(feature, layer);
                let entry = volumeFeatureMeshes.get(feature.id);
                if (!entry || entry.sig !== sig) {
                    if (entry) {
                        volumeMeshRoot.remove(entry.group);
                        disposeVolumeMeshObject(entry.group);
                    }
                    const built = buildVolumeFeatureMeshGroup(feature, layer, three, mv);
                    if (!built) {
                        volumeFeatureMeshes.delete(feature.id);
                        volumeWebGlFailedFeatureIds.add(feature.id);
                        return;
                    }
                    volumeMeshRoot.add(built.group);
                    entry = { sig, group: built.group, anchor: built.anchor, dimmed: !!dimmed };
                    volumeFeatureMeshes.set(feature.id, entry);
                    applyVolumeMeshStyle(built.group, dimmed, feature.id);
                    changed = true;
                } else if (entry.dimmed !== !!dimmed) {
                    applyVolumeMeshStyle(entry.group, dimmed, feature.id);
                    entry.dimmed = !!dimmed;
                }
                activeCount += 1;
            });
            volumeFeatureMeshes.forEach((entry, featureId) => {
                if (needed.has(featureId)) {
                    return;
                }
                volumeMeshRoot.remove(entry.group);
                disposeVolumeMeshObject(entry.group);
                volumeFeatureMeshes.delete(featureId);
                changed = true;
            });
            if (changed) {
                syncVolumeMeshChunkOffset(mv);
                mv.redraw?.();
            }
            return activeCount > 0;
        } catch (err) {
            console.warn('[mcwws-gis] syncVolumeMeshes failed, falling back to SVG volumes', err);
            gisThree = null;
            gisMarkerFillMaterialTemplate = null;
            clearVolumeMeshes();
            entries.forEach(({ feature }) => volumeWebGlFailedFeatureIds.add(feature.id));
            return false;
        }
    }

    function updateGisSelectHoverCursor(clientX, clientY, target) {
        if (isPointerOverLayerDialog(clientX, clientY)) {
            clearGisSelectHover();
            return;
        }
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
        const segInsert = isGisSegmentInsertModifierHeld(null)
            && (!!target?.closest?.('.mcwws-gis-segment-insert-handle') || !!gisHoverSegmentInsert);
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

    function parseLineWidth(value, fallback) {
        const n = Number(value);
        if (!Number.isFinite(n)) return fallback;
        return Math.max(1, Math.min(24, n));
    }

    function getRoadStrokeWidth(feature) {
        const w = parseLineWidth(feature?.properties?.strokeWidth, null);
        return w == null ? null : w;
    }

    function getRoadStrokeDasharray(feature) {
        const style = String(feature?.properties?.strokeStyle || 'solid').toLowerCase();
        if (style === 'solid') return null;
        if (style === 'dashed') return '10 6';
        if (style === 'dotted') return '2 6';
        if (style === 'dashdot' || style === 'dash-dot' || style === 'dash_dot') return '10 6 2 6';
        return null;
    }

    /** @returns {'both'|'dir1'|'dir2'} */
    function normalizeRoadTravelDirection(raw) {
        const v = String(raw || '').trim().toLowerCase();
        if (v === 'dir1' || v === 'direction1' || v === 'd1' || v === '1' || v === '方向1') {
            return 'dir1';
        }
        if (v === 'dir2' || v === 'direction2' || v === 'd2' || v === '2' || v === '方向2') {
            return 'dir2';
        }
        return 'both';
    }

    function getRoadTravelDirection(feature) {
        return normalizeRoadTravelDirection(feature?.properties?.travelDirection);
    }

    function setRoadTravelDirection(feature, value) {
        const props = ensureFeatureProperties(feature);
        const dir = normalizeRoadTravelDirection(value);
        if (dir === 'both') {
            delete props.travelDirection;
        } else {
            props.travelDirection = dir;
        }
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

    function normalizeVolumeShape(value) {
        const v = String(value || VOLUME_SHAPES.FLAT).trim().toLowerCase();
        if (v === 'box' || v === 'cuboid' || v === 'cube') {
            return VOLUME_SHAPES.BOX;
        }
        if (v === 'cylinder' || v === 'cyl') {
            return VOLUME_SHAPES.CYLINDER;
        }
        if (v === 'hexahedron' || v === 'hex' || v === '6face') {
            return VOLUME_SHAPES.HEXAHEDRON;
        }
        return VOLUME_SHAPES.FLAT;
    }

    function isCreatableVolumeShape(shape) {
        return shape === VOLUME_SHAPES.BOX
            || shape === VOLUME_SHAPES.HEXAHEDRON;
    }

    function ensureVolume3d(feature) {
        const props = ensureFeatureProperties(feature);
        if (!props.volume3d || typeof props.volume3d !== 'object') {
            props.volume3d = { shape: VOLUME_SHAPES.BOX };
        }
        props.volume3d.shape = normalizeVolumeShape(props.volume3d.shape);
        if (props.volume3d.shape === VOLUME_SHAPES.FLAT) {
            props.volume3d.shape = VOLUME_SHAPES.BOX;
        }
        return props.volume3d;
    }

    function getVolume3dConfig(feature) {
        if (!feature || feature.type !== 'Polygon') {
            return null;
        }
        const raw = feature.properties?.volume3d;
        let shape;
        if (!raw || typeof raw !== 'object') {
            shape = VOLUME_SHAPES.BOX;
        } else {
            shape = normalizeVolumeShape(raw.shape);
            if (shape === VOLUME_SHAPES.FLAT) {
                shape = VOLUME_SHAPES.BOX;
            }
        }
        if (shape === VOLUME_SHAPES.FLAT) {
            return null;
        }
        const cfg = { shape };
        const src = raw && typeof raw === 'object' ? raw : {};
        if (Number.isFinite(Number(src.minY))) {
            cfg.minY = Number(src.minY);
        }
        if (Number.isFinite(Number(src.maxY))) {
            cfg.maxY = Number(src.maxY);
        }
        if (Number.isFinite(Number(src.radius)) && Number(src.radius) > 0) {
            cfg.radius = Number(src.radius);
        }
        if (Number.isFinite(Number(src.segments))) {
            cfg.segments = Math.max(8, Math.min(64, Math.round(Number(src.segments))));
        }
        if (shape === VOLUME_SHAPES.BOX && cfg.minY == null && cfg.maxY == null) {
            const points = coordsToPoints(feature.coordinates);
            const ys = inferFootprintYs(points);
            cfg.minY = ys.minY;
            cfg.maxY = ys.maxY;
        }
        return cfg;
    }

    function getVolumeShape(feature) {
        const cfg = getVolume3dConfig(feature);
        return cfg ? cfg.shape : VOLUME_SHAPES.BOX;
    }

    function getRegionVolumeFillColor(feature, layer) {
        const c = feature?.properties?.color || layer?.color;
        if (c && /^#[0-9a-fA-F]{3,8}$/i.test(c)) {
            return c;
        }
        return GIS_DEFAULT_REGION_COLOR;
    }

    function resolveVolumeEditLane(feature, lane) {
        return lane || 'center';
    }

    function inferFootprintYs(points) {
        const ys = points.map((p) => p.y).filter((y) => Number.isFinite(y));
        if (!ys.length) {
            return { minY: GIS_DEFAULT_Y, maxY: GIS_DEFAULT_Y + VOLUME_DEFAULT_HEIGHT };
        }
        const minY = Math.min(...ys);
        const maxY = Math.max(...ys);
        if (maxY - minY < 0.5) {
            return { minY, maxY: minY + VOLUME_DEFAULT_HEIGHT };
        }
        return { minY, maxY };
    }

    function resolveVolumeYRange(cfg, points) {
        const inferred = inferFootprintYs(points);
        let minY = cfg?.minY != null ? cfg.minY : inferred.minY;
        let maxY = cfg?.maxY != null ? cfg.maxY : inferred.maxY;
        if (maxY < minY) {
            const tmp = minY;
            minY = maxY;
            maxY = tmp;
        }
        if (maxY - minY < 0.5) {
            maxY = minY + VOLUME_DEFAULT_HEIGHT;
        }
        return { minY, maxY };
    }

    function splitBoxPrismPoints(points) {
        const n = points?.length || 0;
        if (n >= 6 && n % 2 === 0) {
            const half = n / 2;
            return {
                n: half,
                bottom: points.slice(0, half).map((p) => ({ ...p })),
                top: points.slice(half).map((p) => ({ ...p }))
            };
        }
        return null;
    }

    function getBoxPrismRings(points, cfg) {
        const split = splitBoxPrismPoints(points);
        if (split) {
            return split;
        }
        if (points.length >= 3 && cfg) {
            const { minY, maxY } = resolveVolumeYRange(cfg, points);
            return {
                n: points.length,
                bottom: points.map((p) => ({ x: p.x, y: minY, z: p.z })),
                top: points.map((p) => ({ x: p.x, y: maxY, z: p.z }))
            };
        }
        return null;
    }

    function isBoxPrismFeature(feature) {
        if (!feature || feature.type !== 'Polygon') {
            return false;
        }
        const cfg = getVolume3dConfig(feature);
        return !!(cfg && cfg.shape === VOLUME_SHAPES.BOX);
    }

    function syncBoxVolumeYMeta(feature, points) {
        const vol = ensureVolume3d(feature);
        const pts = points || getFeatureVertexPoints(feature);
        const split = splitBoxPrismPoints(pts);
        if (split) {
            vol.minY = Math.min(...split.bottom.map((p) => p.y));
            vol.maxY = Math.max(...split.top.map((p) => p.y));
            return;
        }
        const { minY, maxY } = resolveVolumeYRange(getVolume3dConfig(feature), pts);
        vol.minY = minY;
        vol.maxY = maxY;
    }

    function ensureBoxPrismCoordinates(feature) {
        if (!isBoxPrismFeature(feature)) {
            return;
        }
        const points = getFeatureVertexPoints(feature);
        if (splitBoxPrismPoints(points)) {
            syncBoxVolumeYMeta(feature, points);
            return;
        }
        if (points.length < 3) {
            return;
        }
        const cfg = getVolume3dConfig(feature);
        const rings = getBoxPrismRings(points, cfg);
        if (!rings) {
            return;
        }
        setFeatureCoordinatesFromPoints(feature, [...rings.bottom, ...rings.top]);
        syncBoxVolumeYMeta(feature);
    }

    function deleteBoxPrismRingIndices(feature, ringIndices) {
        const split = splitBoxPrismPoints(getFeatureVertexPoints(feature));
        if (!split) {
            return false;
        }
        const unique = [...new Set(ringIndices)].filter((i) => i >= 0 && i < split.n);
        if (!unique.length) {
            return false;
        }
        if (split.n - unique.length < 3) {
            return false;
        }
        const drop = new Set(unique);
        const bottom = split.bottom.filter((_, i) => !drop.has(i));
        const top = split.top.filter((_, i) => !drop.has(i));
        const removeIds = unique.flatMap((i) => [split.n + i, i]).sort((a, b) => b - a);
        removeVertexIdsAt(feature, removeIds);
        setFeatureCoordinatesFromPoints(feature, [...bottom, ...top]);
        syncBoxVolumeYMeta(feature);
        return true;
    }

    function insertBoxPrismVertex(feature, lane, insertIndex, point) {
        const split = splitBoxPrismPoints(getFeatureVertexPoints(feature));
        if (!split) {
            return null;
        }
        const next = coerceInsertVertexPoint(point);
        if (!next) {
            return null;
        }
        const laneId = lane === 'top' ? 'top' : 'bottom';
        const idx = Math.max(0, Math.min(insertIndex, laneId === 'top' ? split.top.length : split.bottom.length));
        const bottom = split.bottom.slice();
        const top = split.top.slice();
        if (laneId === 'top') {
            const refBottom = split.bottom[Math.min(idx, split.n - 1)] || split.bottom[0];
            top.splice(idx, 0, { ...next });
            bottom.splice(idx, 0, {
                x: next.x,
                y: refBottom?.y ?? split.bottom[0]?.y ?? GIS_DEFAULT_Y,
                z: next.z
            });
        } else {
            const refTop = split.top[Math.min(idx, split.n - 1)] || split.top[0];
            bottom.splice(idx, 0, { ...next });
            top.splice(idx, 0, {
                x: next.x,
                y: refTop?.y ?? (split.top[0]?.y ?? next.y + VOLUME_DEFAULT_HEIGHT),
                z: next.z
            });
        }
        setFeatureCoordinatesFromPoints(feature, [...bottom, ...top]);
        insertVertexIdAt(feature, split.n + idx);
        insertVertexIdAt(feature, idx);
        syncBoxVolumeYMeta(feature);
        return { lane: laneId, vertexIndex: idx };
    }

    function getCylinderCenterRadius(feature, points, cfg) {
        if (points.length >= 2) {
            const center = points[0];
            const rim = points[1];
            const radius = cfg?.radius > 0
                ? cfg.radius
                : Math.hypot(rim.x - center.x, rim.z - center.z);
            return { center, radius: Math.max(0.5, radius) };
        }
        if (points.length === 1 && cfg?.radius > 0) {
            return { center: points[0], radius: cfg.radius };
        }
        return null;
    }

    function appendRingEdges(edges, ring, closed = true) {
        for (let i = 0; i < ring.length; i += 1) {
            const j = closed ? ((i + 1) % ring.length) : (i + 1);
            if (j < ring.length) {
                edges.push([ring[i], ring[j]]);
            }
        }
    }

    function buildCylinderRing(center, radius, y, segments) {
        const ring = [];
        for (let i = 0; i < segments; i += 1) {
            const a = (i / segments) * Math.PI * 2;
            ring.push({
                x: center.x + Math.cos(a) * radius,
                y,
                z: center.z + Math.sin(a) * radius
            });
        }
        return ring;
    }

    function buildVolume3dEdgesFromSpec(shape, points, cfg) {
        const edges = [];
        if (!shape || shape === VOLUME_SHAPES.FLAT || !points.length) {
            return edges;
        }
        const { minY, maxY } = resolveVolumeYRange(cfg, points);
        if (shape === VOLUME_SHAPES.BOX) {
            const rings = getBoxPrismRings(points, cfg);
            if (!rings || rings.n < 2) {
                return edges;
            }
            appendRingEdges(edges, rings.bottom, true);
            appendRingEdges(edges, rings.top, true);
            for (let i = 0; i < rings.n; i += 1) {
                edges.push([rings.bottom[i], rings.top[i]]);
            }
            return edges;
        }
        if (shape === VOLUME_SHAPES.CYLINDER) {
            const cr = getCylinderCenterRadius(null, points, cfg);
            if (!cr) {
                return edges;
            }
            const segments = cfg?.segments || VOLUME_CYLINDER_SEGMENTS;
            const bottom = buildCylinderRing(cr.center, cr.radius, minY, segments);
            const top = buildCylinderRing(cr.center, cr.radius, maxY, segments);
            appendRingEdges(edges, bottom, true);
            appendRingEdges(edges, top, true);
            for (let i = 0; i < segments; i += 2) {
                edges.push([bottom[i], top[i]]);
            }
            return edges;
        }
        if (shape === VOLUME_SHAPES.HEXAHEDRON) {
            if (points.length < 8) {
                return edges;
            }
            const bottom = points.slice(0, 4).map((p) => ({ ...p }));
            const top = points.slice(4, 8).map((p) => ({ ...p }));
            appendRingEdges(edges, bottom, true);
            appendRingEdges(edges, top, true);
            for (let i = 0; i < 4; i += 1) {
                edges.push([bottom[i], top[i]]);
            }
        }
        return edges;
    }

    function buildVolume3dEdges(feature) {
        const cfg = getVolume3dConfig(feature);
        if (!cfg) {
            return [];
        }
        return buildVolume3dEdgesFromSpec(cfg.shape, coordsToPoints(feature.coordinates), cfg);
    }

    function clipSpaceToScreenVolume(c) {
        if (!c || c.w < GIS_CLIP_W_EPS) {
            return null;
        }
        return ndcToScreen({ x: c.x / c.w, y: c.y / c.w });
    }

    function getClippedScreenRingForVolumeFace(points, view, camera) {
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
            const screenPts = clipped.map(clipSpaceToScreenVolume).filter(Boolean);
            if (screenPts.length < 3) {
                return [];
            }
            return clipScreenPolygon(screenPts);
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

    function faceCentroidWorld(ring) {
        let x = 0;
        let y = 0;
        let z = 0;
        ring.forEach((p) => {
            x += p.x;
            y += p.y;
            z += p.z;
        });
        const n = ring.length || 1;
        return { x: x / n, y: y / n, z: z / n };
    }

    function newellNormal(ring) {
        let nx = 0;
        let ny = 0;
        let nz = 0;
        for (let i = 0; i < ring.length; i += 1) {
            const j = (i + 1) % ring.length;
            nx += (ring[i].y - ring[j].y) * (ring[i].z + ring[j].z);
            ny += (ring[i].z - ring[j].z) * (ring[i].x + ring[j].x);
            nz += (ring[i].x - ring[j].x) * (ring[i].y + ring[j].y);
        }
        return { x: nx, y: ny, z: nz };
    }

    function ringSignedAreaXZ(ring) {
        let area = 0;
        for (let i = 0; i < ring.length; i += 1) {
            const j = (i + 1) % ring.length;
            area += ring[i].x * ring[j].z - ring[j].x * ring[i].z;
        }
        return area * 0.5;
    }

    function screenPolygonSignedArea(screenVerts) {
        let area = 0;
        for (let i = 0; i < screenVerts.length; i += 1) {
            const j = (i + 1) % screenVerts.length;
            area += screenVerts[i].x * screenVerts[j].y - screenVerts[j].x * screenVerts[i].y;
        }
        return area * 0.5;
    }

    function computeSideFaceNormal(bi, bj, ti, bottomRingCw) {
        const e1 = vec3Sub(bj, bi);
        const e2 = vec3Sub(ti, bi);
        let normal = vec3Cross(e1, e2);
        if (!bottomRingCw) {
            normal = { x: -normal.x, y: -normal.y, z: -normal.z };
        }
        return normal;
    }

    function computeVolumeFaceNormal(ring, kind, bottomRingCw, sideCorners) {
        if (kind === 'side' && sideCorners) {
            return computeSideFaceNormal(sideCorners.bi, sideCorners.bj, sideCorners.ti, bottomRingCw);
        }
        let normal = newellNormal(ring);
        // 底环/顶环与 XZ 俯视 CCW 一致：Newell 法线朝 +Y，底面外法线应朝 -Y，顶面朝 +Y
        if (kind === 'bottom' && normal.y > 0) {
            normal = { x: -normal.x, y: -normal.y, z: -normal.z };
        } else if (kind === 'top' && normal.y < 0) {
            normal = { x: -normal.x, y: -normal.y, z: -normal.z };
        }
        return normal;
    }

    function faceFacingDotAtPoint(normal, point, camPos) {
        const len = Math.hypot(normal.x, normal.y, normal.z);
        if (len < 1e-8) {
            return 1;
        }
        const nx = normal.x / len;
        const ny = normal.y / len;
        const nz = normal.z / len;
        const vx = camPos.x - point.x;
        const vy = camPos.y - point.y;
        const vz = camPos.z - point.z;
        const vlen = Math.hypot(vx, vy, vz);
        if (vlen < 1e-6) {
            return 1;
        }
        return (nx * vx + ny * vy + nz * vz) / vlen;
    }

    /** 仅当面上所有采样点都明确背向相机时才剔除（避免旋转时侧面过早消失） */
    function isVolumeFaceFacingCamera(ring, normal, camera) {
        const camPos = getCameraWorldPosition(camera);
        if (!camPos) {
            return true;
        }
        const samples = ring.concat([faceCentroidWorld(ring)]);
        let anyFront = false;
        for (let i = 0; i < samples.length; i += 1) {
            if (faceFacingDotAtPoint(normal, samples[i], camPos) > GIS_VOLUME_FACE_BACK_EPS) {
                anyFront = true;
                break;
            }
        }
        return anyFront;
    }

    function isVolumeFaceVisibleFlat(normal, kind) {
        const len = Math.hypot(normal.x, normal.y, normal.z);
        if (len < 1e-8) {
            return true;
        }
        const ny = normal.y / len;
        if (kind === 'top') {
            return ny > 0.02;
        }
        if (kind === 'bottom') {
            return ny < -0.02;
        }
        return Math.abs(ny) < 0.98;
    }

    function faceDepthAlongCameraView(ring, camera) {
        if (camera) {
            let minNdcZ = Infinity;
            ring.forEach((p) => {
                const clip = worldPointToClip(p, camera, false);
                if (clip && clip.w > GIS_CLIP_W_EPS) {
                    const ndcZ = clip.z / clip.w;
                    if (ndcZ < minNdcZ) {
                        minNdcZ = ndcZ;
                    }
                }
            });
            if (minNdcZ !== Infinity) {
                return minNdcZ;
            }
        }
        const c = faceCentroidWorld(ring);
        if (camera) {
            const clip = worldPointToClip(c, camera, false);
            if (clip && clip.w > GIS_CLIP_W_EPS) {
                return clip.z / clip.w;
            }
        }
        const camPos = getCameraWorldPosition(camera);
        if (!camPos) {
            return 0;
        }
        const dx = c.x - camPos.x;
        const dy = c.y - camPos.y;
        const dz = c.z - camPos.z;
        const e = camera?.matrixWorld?.elements;
        if (e) {
            const fx = -e[8];
            const fy = -e[9];
            const fz = -e[10];
            const flen = Math.hypot(fx, fy, fz) || 1;
            return (dx * fx + dy * fy + dz * fz) / flen;
        }
        return Math.hypot(dx, dy, dz);
    }

    function faceViewSpaceDepthRange(ring, camera) {
        const camPos = getCameraWorldPosition(camera);
        const e = camera?.matrixWorld?.elements;
        if (!camPos || !e) {
            return { min: 0, max: 0, sort: 0 };
        }
        const fx = -e[8];
        const fy = -e[9];
        const fz = -e[10];
        const flen = Math.hypot(fx, fy, fz) || 1;
        let min = Infinity;
        let max = -Infinity;
        ring.forEach((p) => {
            const wy = gisWorldY(p, false);
            const d = ((p.x - camPos.x) * fx + (wy - camPos.y) * fy + (p.z - camPos.z) * fz) / flen;
            if (d < min) {
                min = d;
            }
            if (d > max) {
                max = d;
            }
        });
        if (min === Infinity) {
            return { min: 0, max: 0, sort: 0 };
        }
        return { min, max, sort: (min + max) * 0.5 };
    }

    function faceDepthRangeAlongCameraView(ring, camera) {
        let minNdcZ = Infinity;
        let maxNdcZ = -Infinity;
        if (camera) {
            ring.forEach((p) => {
                const clip = worldPointToClip(p, camera, false);
                if (clip && clip.w > GIS_CLIP_W_EPS) {
                    const ndcZ = clip.z / clip.w;
                    if (ndcZ < minNdcZ) {
                        minNdcZ = ndcZ;
                    }
                    if (ndcZ > maxNdcZ) {
                        maxNdcZ = ndcZ;
                    }
                }
            });
        }
        if (minNdcZ === Infinity) {
            const fallback = faceDepthAlongCameraView(ring, camera);
            return { min: fallback, max: fallback };
        }
        return { min: minNdcZ, max: maxNdcZ };
    }

    /** 0=平视，1=正俯视；用于切换深度排序策略 */
    function getCameraDownness(camera) {
        const e = camera?.matrixWorld?.elements;
        if (!e) {
            return 0;
        }
        const fx = -e[8];
        const fy = -e[9];
        const fz = -e[10];
        const flen = Math.hypot(fx, fy, fz) || 1;
        return Math.abs(fy / flen);
    }

    function facePlanDistFromCamera(ring, camera) {
        const camPos = getCameraWorldPosition(camera);
        const centroid = faceCentroidWorld(ring);
        if (!camPos || !centroid) {
            return 0;
        }
        return Math.hypot(centroid.x - camPos.x, centroid.z - camPos.z);
    }

    function compareVolumeFacePaintOrder(a, b, camera) {
        const depthDiff = (b.depth ?? 0) - (a.depth ?? 0);
        if (Math.abs(depthDiff) > 1e-4) {
            return depthDiff;
        }
        const maxDiff = (b.depthMax ?? b.depth) - (a.depthMax ?? a.depth);
        if (Math.abs(maxDiff) > 1e-4) {
            return maxDiff;
        }
        const minDiff = (b.depthMin ?? b.depth) - (a.depthMin ?? a.depth);
        if (Math.abs(minDiff) > 1e-4) {
            return minDiff;
        }
        return String(a.index ?? '').localeCompare(String(b.index ?? ''));
    }

    function sortVolumeFacePaintQueue(queue, camera) {
        const featureDepth = new Map();
        queue.forEach((entry) => {
            const d = Number(entry.item?.depth);
            if (!Number.isFinite(d)) {
                return;
            }
            const prev = featureDepth.get(entry.featureId);
            if (prev == null || d < prev) {
                featureDepth.set(entry.featureId, d);
            }
        });
        queue.sort((a, b) => {
            const aFeat = featureDepth.get(a.featureId) ?? a.item?.depth ?? 0;
            const bFeat = featureDepth.get(b.featureId) ?? b.item?.depth ?? 0;
            const featDiff = bFeat - aFeat;
            if (Math.abs(featDiff) > 1e-3) {
                return featDiff;
            }
            return compareVolumeFacePaintOrder(a.item, b.item, camera);
        });
    }

    function paintVolumeFacePaintQueue(svg, queue, neededPathKeys) {
        queue.forEach(({ featureId, fillKey, item, dimmed }) => {
            neededPathKeys.add(fillKey);
            const fillPath = ensureSvgFeaturePath(svg, fillKey, featureId, 'mcwws-gis-volume3d-fill');
            fillPath.setAttribute('d', item.d);
            fillPath.style.fill = item.fill;
            fillPath.removeAttribute('stroke');
            fillPath.classList.toggle('is-dimmed', dimmed);
        });
        applySvgPathPaintOrder(svg, queue.map((entry) => entry.fillKey));
    }

    function applySvgPathPaintOrder(svg, orderedKeys) {
        orderedKeys.forEach((key) => {
            const el = svgPathElements.get(key);
            if (el && el.parentNode === svg) {
                svg.appendChild(el);
            }
        });
    }

    function getBottomRingCwFlag(shape, points, cfg) {
        if (shape === VOLUME_SHAPES.BOX) {
            const rings = getBoxPrismRings(points, cfg);
            if (rings?.bottom?.length >= 3) {
                return ringSignedAreaXZ(rings.bottom) < 0;
            }
        }
        if (shape === VOLUME_SHAPES.HEXAHEDRON && points.length >= 4) {
            return ringSignedAreaXZ(points.slice(0, 4)) < 0;
        }
        return false;
    }

    function parseRegionHexColor(hex) {
        const m = String(hex || '').match(/^#?([0-9a-f]{6})$/i);
        if (!m) {
            return { r: 223, g: 234, b: 243 };
        }
        const n = parseInt(m[1], 16);
        return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 };
    }

    function shadeRegionFaceColor(baseHex, normal) {
        const base = parseRegionHexColor(baseHex);
        const len = Math.hypot(normal.x, normal.y, normal.z) || 1;
        const nx = normal.x / len;
        const ny = normal.y / len;
        const nz = normal.z / len;
        const ndotl = Math.max(
            0,
            nx * GIS_VOLUME_LIGHT_DIR.x + ny * GIS_VOLUME_LIGHT_DIR.y + nz * GIS_VOLUME_LIGHT_DIR.z
        );
        const shade = 0.38 + 0.62 * ndotl;
        const r = Math.min(255, Math.round(base.r * shade));
        const g = Math.min(255, Math.round(base.g * shade));
        const b = Math.min(255, Math.round(base.b * shade));
        return `rgb(${r}, ${g}, ${b})`;
    }

    function faceDepthSortKey(ring, camera) {
        return faceDepthAlongCameraView(ring, camera);
    }

    function buildSvgClippedFacePath(ring, view, camera) {
        const screenRing = getClippedScreenRingForVolumeFace(ring, view, camera);
        return screenRingToSvgPath(screenRing);
    }

    function buildVolumeSolidFaces(shape, points, cfg) {
        const faces = [];
        if (!shape || shape === VOLUME_SHAPES.FLAT || !points.length) {
            return faces;
        }
        const { minY, maxY } = resolveVolumeYRange(cfg, points);
        if (shape === VOLUME_SHAPES.BOX) {
            const rings = getBoxPrismRings(points, cfg);
            if (!rings || rings.n < 3) {
                return faces;
            }
            faces.push({ ring: rings.bottom, kind: 'bottom' });
            faces.push({ ring: rings.top, kind: 'top' });
            for (let i = 0; i < rings.n; i += 1) {
                const j = (i + 1) % rings.n;
                faces.push({
                    ring: [rings.bottom[i], rings.bottom[j], rings.top[j], rings.top[i]],
                    kind: 'side',
                    sideCorners: {
                        bi: rings.bottom[i],
                        bj: rings.bottom[j],
                        ti: rings.top[i]
                    }
                });
            }
            return faces;
        }
        if (shape === VOLUME_SHAPES.HEXAHEDRON && points.length >= 8) {
            const bottom = points.slice(0, 4).map((p) => ({ ...p }));
            const top = points.slice(4, 8).map((p) => ({ ...p }));
            faces.push({ ring: bottom, kind: 'bottom' });
            faces.push({ ring: top, kind: 'top' });
            for (let i = 0; i < 4; i += 1) {
                const j = (i + 1) % 4;
                faces.push({
                    ring: [bottom[i], bottom[j], top[j], top[i]],
                    kind: 'side',
                    sideCorners: { bi: bottom[i], bj: bottom[j], ti: top[i] }
                });
            }
            return faces;
        }
        if (shape === VOLUME_SHAPES.CYLINDER) {
            const cr = getCylinderCenterRadius(null, points, cfg);
            if (!cr) {
                return faces;
            }
            const segments = cfg?.segments || VOLUME_CYLINDER_SEGMENTS;
            const bottom = buildCylinderRing(cr.center, cr.radius, minY, segments);
            const top = buildCylinderRing(cr.center, cr.radius, maxY, segments);
            faces.push({ ring: bottom, kind: 'bottom' });
            faces.push({ ring: top, kind: 'top' });
            for (let i = 0; i < segments; i += 1) {
                const j = (i + 1) % segments;
                faces.push({
                    ring: [bottom[i], bottom[j], top[j], top[i]],
                    kind: 'side',
                    sideCorners: { bi: bottom[i], bj: bottom[j], ti: top[i] }
                });
            }
        }
        return faces;
    }

    function computeVolumeShapeCenter(points) {
        if (!points.length) {
            return { x: 0, y: 0, z: 0 };
        }
        let x = 0;
        let y = 0;
        let z = 0;
        points.forEach((p) => {
            x += p.x;
            y += p.y;
            z += p.z;
        });
        const n = points.length;
        return { x: x / n, y: y / n, z: z / n };
    }

    function volumeWorldPoint(p) {
        return { x: p.x, y: gisWorldY(p, false), z: p.z };
    }

    /** 保证三角形顶点绕序与面外法线一致，避免 SVG 填充自交/翻面 */
    function orientTriangleForOutwardNormal(tri, normal) {
        const p0 = volumeWorldPoint(tri[0]);
        const p1 = volumeWorldPoint(tri[1]);
        const p2 = volumeWorldPoint(tri[2]);
        const e1 = vec3Sub(p1, p0);
        const e2 = vec3Sub(p2, p0);
        const cross = vec3Cross(e1, e2);
        const dot = cross.x * normal.x + cross.y * normal.y + cross.z * normal.z;
        if (dot < 0) {
            return [tri[0], tri[2], tri[1]];
        }
        return tri;
    }

    /** 统一面环顶点绕序，与面外法线一致 */
    function orientFaceRingForOutwardNormal(ring, normal) {
        if (!ring || ring.length < 3) {
            return ring;
        }
        const oriented = orientTriangleForOutwardNormal([ring[0], ring[1], ring[2]], normal);
        if (oriented[1] === ring[1]) {
            return ring;
        }
        return [ring[0], ...ring.slice(1).reverse()];
    }

    function buildVolumeFaceRenderItems(shape, points, cfg, view, camera, baseColor) {
        const faceSpecs = buildVolumeSolidFaces(shape, points, cfg);
        if (!faceSpecs.length) {
            return [];
        }
        const bottomRingCw = getBottomRingCwFlag(shape, points, cfg);
        const items = [];
        faceSpecs.forEach((spec, faceIndex) => {
            const normal = computeVolumeFaceNormal(spec.ring, spec.kind, bottomRingCw, spec.sideCorners);
            if (camera) {
                if (!isVolumeFaceFacingCamera(spec.ring, normal, camera)) {
                    return;
                }
            } else if (!isVolumeFaceVisibleFlat(normal, spec.kind)) {
                return;
            }
            const ring = prepareVolumeFaceRing({ ring: spec.ring, kind: spec.kind }, normal);
            for (let ti = 1; ti < ring.length - 1; ti += 1) {
                const triRing = [ring[0], ring[ti], ring[ti + 1]];
                const screenRing = getClippedScreenRingForVolumeFace(triRing, view, camera);
                if (screenRing.length < 3) {
                    continue;
                }
                if (Math.abs(screenPolygonSignedArea(screenRing)) < GIS_VOLUME_FACE_MIN_SCREEN_AREA) {
                    continue;
                }
                const d = screenRingToSvgPath(screenRing);
                if (!d) {
                    continue;
                }
                const depthRange = camera
                    ? faceViewSpaceDepthRange(triRing, camera)
                    : faceDepthRangeAlongCameraView(triRing, camera);
                const depthSort = camera
                    ? depthRange.sort
                    : (depthRange.min + depthRange.max) * 0.5;
                const centroid = faceCentroidWorld(triRing);
                items.push({
                    index: `${faceIndex}-${ti}`,
                    d,
                    fill: shadeRegionFaceColor(baseColor, normal),
                    depth: depthSort,
                    depthMax: depthRange.max,
                    depthMin: depthRange.min,
                    planDist: facePlanDistFromCamera(triRing, camera),
                    centroidY: centroid?.y ?? 0
                });
            }
        });
        items.sort((a, b) => compareVolumeFacePaintOrder(a, b, camera));
        return items;
    }

    function buildSvgVolumeSolidFillPathFromSpec(shape, points, cfg, view, camera, baseColor) {
        return buildVolumeFaceRenderItems(shape, points, cfg, view, camera, baseColor || GIS_DEFAULT_REGION_COLOR);
    }

    function buildSvgVolumeSolidFillPath(feature, view, camera) {
        const cfg = getVolume3dConfig(feature);
        if (!cfg) {
            return [];
        }
        return buildVolumeFaceRenderItems(
            cfg.shape,
            coordsToPoints(feature.coordinates),
            cfg,
            view,
            camera,
            getRegionVolumeFillColor(feature)
        );
    }

    function buildDraftVolume3dEdges() {
        if (activeTool !== 'polygon') {
            return [];
        }
        const draft = draftPoints.slice();
        if (draftHover) {
            draft.push(draftHover);
        }
        const cfg = { shape: activeVolumeShape };
        if (activeVolumeShape === VOLUME_SHAPES.CYLINDER && draft.length >= 2) {
            const c = draft[0];
            const r = draft[1];
            cfg.minY = Math.min(c.y, r.y);
            cfg.maxY = Math.max(c.y, r.y);
            cfg.radius = Math.hypot(r.x - c.x, r.z - c.z);
            return buildVolume3dEdgesFromSpec(activeVolumeShape, draft, cfg);
        }
        if (activeVolumeShape === VOLUME_SHAPES.BOX && draft.length >= 2) {
            const ys = inferFootprintYs(draft);
            cfg.minY = ys.minY;
            cfg.maxY = ys.maxY;
            return buildVolume3dEdgesFromSpec(activeVolumeShape, draft, cfg);
        }
        if (activeVolumeShape === VOLUME_SHAPES.HEXAHEDRON) {
            if (draft.length >= 8) {
                return buildVolume3dEdgesFromSpec(activeVolumeShape, draft, cfg);
            }
            const edges = [];
            const phase = draftVolumePhase || 'bottom';
            const bottomDraft = phase === 'top' ? draft.slice(0, 4) : draft;
            const n = Math.min(bottomDraft.length, 4);
            if (n >= 2) {
                const ys = inferFootprintYs(draft);
                const minY = ys.minY;
                const maxY = ys.maxY;
                const bottom = bottomDraft.slice(0, n).map((p) => ({ x: p.x, y: minY, z: p.z }));
                appendRingEdges(edges, bottom, n >= 3 && (phase === 'top' || n >= 4));
                if (n >= 4) {
                    const top = bottomDraft.slice(0, 4).map((p) => ({ x: p.x, y: maxY, z: p.z }));
                    appendRingEdges(edges, top, true);
                    for (let i = 0; i < 4; i += 1) {
                        edges.push([bottom[i], top[i]]);
                    }
                }
            }
            if (phase === 'top' && draft.length > 4) {
                const topDraft = draft.slice(4);
                const tn = Math.min(topDraft.length, 4);
                if (tn >= 2) {
                    const ys = inferFootprintYs(draft);
                    const topRing = topDraft.slice(0, tn).map((p) => ({ x: p.x, y: ys.maxY, z: p.z }));
                    appendRingEdges(edges, topRing, tn >= 3 && tn >= 4);
                }
            }
            return edges;
        }
        return buildVolume3dEdgesFromSpec(activeVolumeShape, draft, cfg);
    }

    function getDraftPreviewPoints() {
        const draft = draftPoints.slice();
        if (draftHover) {
            draft.push(draftHover);
        }
        return draft;
    }

    function getDraftFootprintPreviewPoints(draft) {
        if (activeVolumeShape === VOLUME_SHAPES.CYLINDER) {
            return draft.slice(0, Math.min(2, draft.length));
        }
        if (activeVolumeShape === VOLUME_SHAPES.HEXAHEDRON) {
            if (draftVolumePhase === 'top' && draft.length > 4) {
                return draft.slice(4, Math.min(draft.length, 8));
            }
            return draft.slice(0, Math.min(4, draft.length));
        }
        return draft;
    }

    function buildDraftPreviewPath(view, camera) {
        const draft = getDraftPreviewPoints();
        if (draft.length < 2) {
            return '';
        }
        if (activeTool === 'line') {
            return buildSvgPolylinePath(draft, view, camera);
        }
        if (activeTool === 'polygon') {
            if (shouldShowVolume3dWireframes()) {
                const wire = buildSvgVolumeWireframePath(buildDraftVolume3dEdges(), view, camera);
                if (wire) {
                    return wire;
                }
            }
            const footprint = getDraftFootprintPreviewPoints(draft);
            if (footprint.length >= 3) {
                return buildSvgPolygonPath(footprint, view, camera);
            }
            if (footprint.length >= 2) {
                return buildSvgPolylinePath(footprint, view, camera);
            }
        }
        return '';
    }

    function buildSvgVolumeWireframePath(edges, view, camera) {
        if (!edges.length) {
            return '';
        }
        const chains = [];
        edges.forEach(([a, b]) => {
            iterClippedLineScreenSegments([a, b], view, camera, (s0, s1) => {
                appendClippedSegment(chains, [s0, s1]);
            });
        });
        return chainsToSvgPath(chains);
    }

    function resetVolumeDraftState() {
        draftVolumePhase = activeVolumeShape === VOLUME_SHAPES.HEXAHEDRON ? 'bottom' : null;
    }

    function getVolumeDraftStatusText() {
        if (activeTool !== 'polygon') {
            return '';
        }
        if (activeVolumeShape === VOLUME_SHAPES.BOX) {
            return `柱状体：已 ${draftPoints.length} 点底面 — 完成后生成 2n 顶点，可独立拖拽变形`;
        }
        if (activeVolumeShape === VOLUME_SHAPES.HEXAHEDRON) {
            if (draftVolumePhase === 'bottom') {
                return `六面体底面：${draftPoints.length}/4 点`;
            }
            return `六面体顶面：${Math.max(0, draftPoints.length - 4)}/4 点`;
        }
        return `区域：已 ${draftPoints.length} 点 — 双击或点「完成」结束，Esc 取消`;
    }

    function normalizeVolume3dFeature(feature) {
        if (!feature || feature.type !== 'Polygon') {
            return;
        }
        const vol = ensureVolume3d(feature);
        if (vol.shape === VOLUME_SHAPES.FLAT) {
            vol.shape = VOLUME_SHAPES.BOX;
        }
        const points = coordsToPoints(feature.coordinates);
        if (vol.shape === VOLUME_SHAPES.HEXAHEDRON && points.length >= 8) {
            vol.shape = VOLUME_SHAPES.BOX;
        }
        const cfg = getVolume3dConfig(feature);
        if (!cfg) {
            return;
        }
        if (cfg.shape === VOLUME_SHAPES.BOX) {
            ensureBoxPrismCoordinates(feature);
            return;
        }
        const { minY, maxY } = resolveVolumeYRange(cfg, points);
        vol.minY = minY;
        vol.maxY = maxY;
        if (cfg.shape === VOLUME_SHAPES.CYLINDER) {
            const cr = getCylinderCenterRadius(feature, points, cfg);
            if (cr) {
                vol.radius = Math.round(cr.radius * 1000) / 1000;
            }
        }
    }

    function getSelectedPolygonFeature() {
        const items = getSelectedPolygonFeatures();
        return items.length === 1 ? items[0] : null;
    }

    function getSelectedLineStringRoad() {
        const items = getSelectedLineStringFeatures();
        return items.length === 1 ? items[0] : null;
    }

    function iterSelectedFeatures() {
        const items = [];
        selectedFeatureIds.forEach((id) => {
            const found = findFeatureById(id);
            if (found) {
                items.push(found);
            }
        });
        return items;
    }

    function getHomogeneousSelectedFeatures() {
        const items = iterSelectedFeatures();
        if (!items.length) {
            return null;
        }
        const type = items[0].feature.type;
        if (!items.every((entry) => entry.feature.type === type)) {
            return null;
        }
        return { type, items };
    }

    function getSelectedFeaturesByType(type) {
        const sel = getHomogeneousSelectedFeatures();
        if (!sel || sel.type !== type) {
            return [];
        }
        return sel.items;
    }

    function getSelectedLineStringFeatures() {
        return getSelectedFeaturesByType('LineString');
    }

    function getSelectedPolygonFeatures() {
        return getSelectedFeaturesByType('Polygon');
    }

    function getSelectedPinFeatures() {
        const sel = getHomogeneousSelectedFeatures();
        if (!sel || (sel.type !== 'Point' && sel.type !== 'Label')) {
            return [];
        }
        return sel.items;
    }

    function getBatchStringPropDisplay(items, getter) {
        const keys = items.map((entry) => {
            const v = getter(entry.feature);
            return v == null ? '' : String(v).trim();
        });
        const uniq = [...new Set(keys)];
        if (uniq.length === 1) {
            return { value: uniq[0], mixed: false };
        }
        return { value: '', mixed: true };
    }

    function getBatchNumberPropDisplay(items, getter) {
        const keys = items.map((entry) => {
            const v = getter(entry.feature);
            return v == null || v === '' ? '' : String(v);
        });
        const uniq = [...new Set(keys)];
        if (uniq.length === 1) {
            return { value: uniq[0], mixed: false };
        }
        return { value: '', mixed: true };
    }

    function getBatchEnumPropDisplay(items, getter) {
        const keys = items.map((entry) => String(getter(entry.feature) ?? ''));
        const uniq = [...new Set(keys)];
        if (uniq.length === 1) {
            return { value: uniq[0], mixed: false };
        }
        return { value: '', mixed: true };
    }

    function getBatchBoolPropDisplay(items, getter) {
        const keys = items.map((entry) => (getter(entry.feature) ? '1' : '0'));
        const uniq = [...new Set(keys)];
        if (uniq.length === 1) {
            return { checked: uniq[0] === '1', mixed: false };
        }
        return { checked: false, mixed: true };
    }

    function renderBatchCountSuffix(count) {
        return count > 1 ? ` · 已选 ${count} 项（批量）` : '';
    }

    function renderMixedSelectPlaceholder(mixed) {
        return mixed ? '<option value="" disabled selected>多个值</option>' : '';
    }

    function renderSelectionTypeMismatchHintHtml() {
        if (selectedFeatureIds.size <= 1) {
            return '';
        }
        if (getHomogeneousSelectedFeatures()) {
            return '';
        }
        return '<p class="mcwws-gis-road-props-hint">已选多种类型要素，请只选择相同类型（道路 / 区域 / 点 / 标注）以批量编辑属性</p>';
    }

    function applyCheckboxIndeterminateStates(root) {
        if (!root) {
            return;
        }
        root.querySelectorAll('input[type="checkbox"][data-mixed="1"]').forEach((input) => {
            input.indeterminate = true;
        });
    }

    function renderRoadPropertiesPanelHtml() {
        const items = getSelectedLineStringFeatures();
        if (!items.length) {
            return '';
        }
        const batchCount = items.length;
        const found = items[0];
        const singleRoad = batchCount === 1;
        const strokeStyleDisp = getBatchEnumPropDisplay(items, (f) => String(f.properties?.strokeStyle || 'solid'));
        const strokeWidthDisp = getBatchNumberPropDisplay(items, (f) => {
            const w = Number(f.properties?.strokeWidth);
            return Number.isFinite(w) ? w : '';
        });
        const strokeColorDisp = getBatchStringPropDisplay(items, (f) => f.properties?.color || '');
        const showRoadNameDisp = getBatchBoolPropDisplay(items, (f) => f.properties?.showRoadName !== false);
        const travelDirDisp = getBatchEnumPropDisplay(items, (f) => getRoadTravelDirection(f));
        const nameDisp = getBatchStringPropDisplay(items, (f) => String(f.properties?.name || '').trim());
        const roadNameSegments = singleRoad ? getRoadNameSegments(found.feature) : [];
        const showSegmentEditor = singleRoad && roadNameSegments.length > 0;
        const vtxTargets = singleRoad ? getSelectedRoadVertexTargets() : [];
        const vtxSel = vtxTargets.length === 1 ? vtxTargets[0] : null;
        const canSplitRoadName = gisCanEdit && singleRoad
            && vtxSel
            && vtxSel.vertexIndex > 0
            && vtxSel.vertexIndex < getFeatureVertexCount(found.feature) - 1;
        const roadNameSegmentsHtml = showSegmentEditor ? `
                    <div class="mcwws-gis-road-name-segments">
                        <p class="mcwws-gis-menu-section-title">分段路名（特征点分界）</p>
                        ${roadNameSegments.map((seg, i) => `
                            <label class="mcwws-gis-road-prop-row mcwws-gis-road-name-seg-row">
                                <span>第 ${seg.fromIndex + 1}–${seg.toIndex + 1} 点</span>
                                <input type="text" class="mcwws-gis-road-prop-input" data-road-name-seg="${i}"
                                    value="${escapeHtml(seg.name)}" placeholder="路名"
                                    ${!gisCanEdit ? 'disabled' : ''}>
                            </label>
                        `).join('')}
                        ${canSplitRoadName ? `
                        <div class="mcwws-gis-menu-actions">
                            <button type="button" class="mcwws-gis-menu-action" data-action="split-road-name-at-vertex">
                                以选中特征点（第 ${vtxSel.vertexIndex + 1} 点）拆分路名
                            </button>
                        </div>
                        ` : ''}
                        <p class="mcwws-gis-road-props-hint">相邻分段在分界特征点相接；按线路顶点顺序分段，与南北/东西走向无关</p>
                    </div>
        ` : `
                    <label class="mcwws-gis-road-prop-row">
                        <span>路名</span>
                        <input type="text" class="mcwws-gis-road-prop-input" data-road-prop="name"
                            value="${escapeHtml(nameDisp.value)}" placeholder="${nameDisp.mixed ? '多个值' : '如：繁华路（留空不显示）'}"
                            ${!gisCanEdit ? 'disabled' : ''}>
                    </label>
                    ${singleRoad && canSplitRoadName ? `
                    <div class="mcwws-gis-menu-actions">
                        <button type="button" class="mcwws-gis-menu-action" data-action="split-road-name-at-vertex">
                            以选中特征点（第 ${vtxSel.vertexIndex + 1} 点）拆分路名
                        </button>
                    </div>
                    <p class="mcwws-gis-road-props-hint">设置路名后，可选中间特征点拆成多段（同一条路不同区段可设不同名称）</p>
                    ` : ''}
                    ${batchCount > 1 ? '<p class="mcwws-gis-road-props-hint">批量设置路名将应用到每条道路全程</p>' : ''}
        `;
        const vtxIdx = vtxSel ? vtxSel.vertexIndex : -1;
        const vtxId = vtxSel ? getVertexIdAt(found.feature, vtxIdx) : '';
        const vtxLabel = !vtxTargets.length
            ? '请选中一个或多个特征点（套索/点击）'
            : vtxTargets.length === 1
                ? `第 ${vtxTargets[0].vertexIndex + 1} 个特征点`
                : `已选 ${vtxTargets.length} 个特征点（批量）`;
        const camH = getMapCameraHeight();
        const visMinDisp = vtxTargets.length
            ? getBatchVisibilityBoundDisplay(vtxTargets, 'min')
            : { value: '', mixed: false };
        const visMaxDisp = vtxTargets.length
            ? getBatchVisibilityBoundDisplay(vtxTargets, 'max')
            : { value: '', mixed: false };
        const visVisibleNow = vtxSel ? isVertexVisibleAtHeight(found.feature, vtxIdx, camH) : false;
        const heightClipNote = gisIgnoreHeightClip
            ? '<p class="mcwws-gis-road-props-hint">已开启<strong>忽视高度裁切</strong>，编辑时全部顶点/线段可见</p>'
            : '';
        const vertexIdRow = vtxSel ? `
                    <label class="mcwws-gis-road-prop-row mcwws-gis-road-prop-row--readonly">
                        <span>顶点编号</span>
                        <code class="mcwws-gis-vertex-id">${escapeHtml(vtxId || '')}</code>
                    </label>
        ` : '';
        const vertexVisRow = vtxTargets.length ? `
                    <label class="mcwws-gis-road-prop-row">
                        <span>可视下限 a</span>
                        <input type="number" class="mcwws-gis-road-prop-input" data-vertex-vis="min"
                            step="1" value="${escapeHtml(visMinDisp.value)}"
                            placeholder="${visMinDisp.mixed ? '多个值' : '留空 = −∞'}"
                            ${!gisCanEdit ? 'disabled' : ''}>
                    </label>
                    <label class="mcwws-gis-road-prop-row">
                        <span>可视上限 b</span>
                        <input type="number" class="mcwws-gis-road-prop-input" data-vertex-vis="max"
                            step="1" value="${escapeHtml(visMaxDisp.value)}"
                            placeholder="${visMaxDisp.mixed ? '多个值' : '留空 = +∞'}"
                            ${!gisCanEdit ? 'disabled' : ''}>
                    </label>
                    ${vtxTargets.length > 1 && gisCanEdit ? `
                    <div class="mcwws-gis-menu-actions">
                        <button type="button" class="mcwws-gis-menu-action" data-action="clear-vertex-vis">清除所选点可视范围</button>
                    </div>
                    ` : ''}
                    <p class="mcwws-gis-road-props-hint mcwws-gis-vertex-vis-hint">
                        当前相机高度 <strong>${Number.isFinite(camH) ? Math.round(camH) : '—'}</strong>；
                        可见条件 <code>a &lt; h ≤ b</code>（须 <strong>a &lt; b</strong>，留空为无穷）；
                        ${vtxTargets.length > 1
                            ? '修改下限/上限将<strong>批量应用</strong>到所选点'
                            : `此点<strong>${visVisibleNow ? '可见' : '不可见'}</strong>`}
                    </p>
        ` : '';
        return `
            <div class="mcwws-gis-road-props">
                <p class="mcwws-gis-menu-section-title">道路属性${renderBatchCountSuffix(batchCount)}</p>
                <div class="mcwws-gis-road-prop-grid">
                    ${roadNameSegmentsHtml}
                    <label class="mcwws-gis-road-prop-row mcwws-gis-road-prop-row--checkbox">
                        <span>沿路显示路名</span>
                        <input type="checkbox" data-road-prop="showRoadName" data-mixed="${showRoadNameDisp.mixed ? '1' : '0'}"
                            ${showRoadNameDisp.checked ? 'checked' : ''}
                            ${!gisCanEdit ? 'disabled' : ''}>
                    </label>
                    <label class="mcwws-gis-road-prop-row">
                        <span>行驶方向</span>
                        <select class="mcwws-gis-road-prop-input" data-road-prop="travelDirection" ${!gisCanEdit ? 'disabled' : ''}>
                            ${renderMixedSelectPlaceholder(travelDirDisp.mixed)}
                            <option value="both" ${!travelDirDisp.mixed && travelDirDisp.value === 'both' ? 'selected' : ''}>双向（不显示箭头）</option>
                            <option value="dir1" ${!travelDirDisp.mixed && travelDirDisp.value === 'dir1' ? 'selected' : ''}>方向 1（沿顶点顺序）</option>
                            <option value="dir2" ${!travelDirDisp.mixed && travelDirDisp.value === 'dir2' ? 'selected' : ''}>方向 2（与方向 1 相反）</option>
                        </select>
                    </label>
                    <label class="mcwws-gis-road-prop-row">
                        <span>线型</span>
                        <select class="mcwws-gis-road-prop-input" data-road-prop="strokeStyle" ${!gisCanEdit ? 'disabled' : ''}>
                            ${renderMixedSelectPlaceholder(strokeStyleDisp.mixed)}
                            <option value="solid" ${!strokeStyleDisp.mixed && strokeStyleDisp.value === 'solid' ? 'selected' : ''}>实线</option>
                            <option value="dashed" ${!strokeStyleDisp.mixed && strokeStyleDisp.value === 'dashed' ? 'selected' : ''}>虚线</option>
                            <option value="dotted" ${!strokeStyleDisp.mixed && strokeStyleDisp.value === 'dotted' ? 'selected' : ''}>点线</option>
                            <option value="dashdot" ${!strokeStyleDisp.mixed && (strokeStyleDisp.value === 'dashdot' || strokeStyleDisp.value === 'dash-dot') ? 'selected' : ''}>点划线</option>
                        </select>
                    </label>
                    <label class="mcwws-gis-road-prop-row">
                        <span>粗细</span>
                        <input type="number" class="mcwws-gis-road-prop-input" data-road-prop="strokeWidth"
                            min="1" max="24" step="0.5" value="${escapeHtml(strokeWidthDisp.value)}"
                            placeholder="${strokeWidthDisp.mixed ? '多个值' : '默认'}"
                            ${!gisCanEdit ? 'disabled' : ''}>
                    </label>
                    <label class="mcwws-gis-road-prop-row">
                        <span>颜色</span>
                        <input type="text" class="mcwws-gis-road-prop-input" data-road-prop="color"
                            value="${escapeHtml(strokeColorDisp.value)}" placeholder="${strokeColorDisp.mixed ? '多个值' : '#3b82f6（留空=图层默认）'}"
                            ${!gisCanEdit ? 'disabled' : ''}>
                    </label>
                </div>
                ${singleRoad ? `
                <div class="mcwws-gis-road-vertex-lanes">
                    <p class="mcwws-gis-menu-section-title">特征点（${vtxLabel}）</p>
                    ${vertexIdRow}
                    ${vertexVisRow}
                    ${heightClipNote}
                </div>
                <p class="mcwws-gis-road-props-hint">vertexIds 与 vertexVisibility 按顶点顺序保存在 properties 中；缩放地图可预览分级显示</p>
                ` : batchCount > 1 ? '<p class="mcwws-gis-road-props-hint">批量模式下修改线型 / 颜色 / 路名等将应用到全部所选道路；特征点请单选道路后编辑</p>' : ''}
            </div>
        `;
    }

    function renderPolygonVolumePanelHtml() {
        const items = getSelectedPolygonFeatures();
        if (!items.length) {
            return '';
        }
        const batchCount = items.length;
        const feature = items[0].feature;
        const shapeDisp = getBatchEnumPropDisplay(items, (f) => ensureVolume3d(f).shape || VOLUME_SHAPES.BOX);
        const shape = shapeDisp.mixed ? '' : shapeDisp.value;
        const minYDisp = getBatchNumberPropDisplay(items, (f) => {
            const pts = coordsToPoints(f.coordinates);
            return resolveVolumeYRange(getVolume3dConfig(f) || { shape: getVolumeShape(f) }, pts).minY;
        });
        const maxYDisp = getBatchNumberPropDisplay(items, (f) => {
            const pts = coordsToPoints(f.coordinates);
            return resolveVolumeYRange(getVolume3dConfig(f) || { shape: getVolumeShape(f) }, pts).maxY;
        });
        const radiusDisp = getBatchNumberPropDisplay(items, (f) => {
            if (getVolumeShape(f) !== VOLUME_SHAPES.CYLINDER) {
                return '';
            }
            const cr = getCylinderCenterRadius(f, coordsToPoints(f.coordinates), getVolume3dConfig(f) || {});
            return cr ? Math.round(cr.radius * 100) / 100 : '';
        });
        const nameDisp = getBatchStringPropDisplay(items, (f) => f.properties?.name || '区域');
        const colorDisp = getBatchStringPropDisplay(items, (f) => f.properties?.color || '');
        const shapeOptions = VOLUME_SHAPE_OPTIONS.map((opt) => `
            <option value="${opt.id}" ${!shapeDisp.mixed && shape === opt.id ? 'selected' : ''}>${escapeHtml(opt.label)}</option>
        `).join('');
        const legacyCylinder = !shapeDisp.mixed && shape === VOLUME_SHAPES.CYLINDER;
        const showCylinderRadius = legacyCylinder;
        const showYRows = legacyCylinder || (!shapeDisp.mixed && shape === VOLUME_SHAPES.BOX) || shapeDisp.mixed;
        const radiusRow = showCylinderRadius ? `
            <label class="mcwws-gis-road-prop-row">
                <span>半径（格）</span>
                <input type="number" class="mcwws-gis-road-prop-input" data-volume-prop="radius"
                    min="0.5" step="0.5" value="${escapeHtml(String(radiusDisp.value))}"
                    placeholder="${radiusDisp.mixed ? '多个值' : ''}"
                    ${!gisCanEdit ? 'disabled' : ''}>
            </label>
        ` : '';
        const yRows = showYRows ? `
            <label class="mcwws-gis-road-prop-row">
                <span>底面 Y</span>
                <input type="number" class="mcwws-gis-road-prop-input" data-volume-prop="minY"
                    step="0.5" value="${escapeHtml(String(minYDisp.value))}"
                    placeholder="${minYDisp.mixed ? '多个值' : ''}"
                    ${!gisCanEdit ? 'disabled' : ''}>
            </label>
            <label class="mcwws-gis-road-prop-row">
                <span>顶面 Y</span>
                <input type="number" class="mcwws-gis-road-prop-input" data-volume-prop="maxY"
                    step="0.5" value="${escapeHtml(String(maxYDisp.value))}"
                    placeholder="${maxYDisp.mixed ? '多个值' : ''}"
                    ${!gisCanEdit ? 'disabled' : ''}>
            </label>
        ` : shapeDisp.mixed ? `
            <label class="mcwws-gis-road-prop-row">
                <span>底面 Y</span>
                <input type="number" class="mcwws-gis-road-prop-input" data-volume-prop="minY"
                    step="0.5" value="${escapeHtml(String(minYDisp.value))}"
                    placeholder="${minYDisp.mixed ? '多个值' : ''}"
                    ${!gisCanEdit ? 'disabled' : ''}>
            </label>
            <label class="mcwws-gis-road-prop-row">
                <span>顶面 Y</span>
                <input type="number" class="mcwws-gis-road-prop-input" data-volume-prop="maxY"
                    step="0.5" value="${escapeHtml(String(maxYDisp.value))}"
                    placeholder="${maxYDisp.mixed ? '多个值' : ''}"
                    ${!gisCanEdit ? 'disabled' : ''}>
            </label>
        ` : '';
        const hint = shapeDisp.mixed
            ? '所选区域 3D 形状不一致时，修改形状将统一应用到全部'
            : legacyCylinder
                ? '圆柱为旧数据，仍可编辑；新建请选柱状体或六面体'
                : shape === VOLUME_SHAPES.BOX
                    ? '柱状体：底/顶各 n 顶点可独立 XYZ 拖拽，可变形为任意棱柱'
                    : shape === VOLUME_SHAPES.HEXAHEDRON
                        ? '六面体：前 4 点为底面，后 4 点为顶面（可拖拽顶点）'
                        : '区域 3D 体';
        return `
            <div class="mcwws-gis-road-props">
                <p class="mcwws-gis-menu-section-title">区域属性 · ${escapeHtml(nameDisp.mixed ? '多个名称' : (nameDisp.value || '区域'))}${renderBatchCountSuffix(batchCount)}</p>
                <div class="mcwws-gis-road-prop-grid">
                    <label class="mcwws-gis-road-prop-row">
                        <span>名称</span>
                        <input type="text" class="mcwws-gis-road-prop-input" data-pin-prop="name"
                            value="${escapeHtml(nameDisp.value)}"
                            placeholder="${nameDisp.mixed ? '多个值' : '区域名称'}"
                            ${!gisCanEdit ? 'disabled' : ''}>
                    </label>
                    <label class="mcwws-gis-road-prop-row">
                        <span>颜色</span>
                        <input type="text" class="mcwws-gis-road-prop-input" data-pin-prop="color"
                            value="${escapeHtml(colorDisp.value)}"
                            placeholder="${colorDisp.mixed ? '多个值' : '#DFEAF3（留空=默认填充）'}"
                            ${!gisCanEdit ? 'disabled' : ''}>
                    </label>
                    <label class="mcwws-gis-road-prop-row">
                        <span>3D 形状</span>
                        ${legacyCylinder ? `
                        <input type="text" class="mcwws-gis-road-prop-input" value="圆柱（旧数据）" readonly disabled>
                        ` : `
                        <select class="mcwws-gis-road-prop-input" data-volume-prop="shape" ${!gisCanEdit ? 'disabled' : ''}>
                            ${renderMixedSelectPlaceholder(shapeDisp.mixed)}
                            ${shapeOptions}
                        </select>
                        `}
                    </label>
                    ${yRows}
                    ${radiusRow}
                    <p class="mcwws-gis-road-props-hint">${escapeHtml(hint)}</p>
                </div>
            </div>
        `;
    }

    function renderPinPropertiesPanelHtml() {
        const items = getSelectedPinFeatures();
        if (!items.length) {
            return '';
        }
        const batchCount = items.length;
        const pinType = items[0].feature.type;
        const typeLabel = pinType === 'Label' ? '标注' : '点位';
        const nameDisp = getBatchStringPropDisplay(items, (f) => f.properties?.name || '');
        const descDisp = getBatchStringPropDisplay(items, (f) => f.properties?.description || '');
        const colorDisp = getBatchStringPropDisplay(items, (f) => f.properties?.color || '');
        return `
            <div class="mcwws-gis-road-props">
                <p class="mcwws-gis-menu-section-title">${typeLabel}属性${renderBatchCountSuffix(batchCount)}</p>
                <div class="mcwws-gis-road-prop-grid">
                    <label class="mcwws-gis-road-prop-row">
                        <span>名称</span>
                        <input type="text" class="mcwws-gis-road-prop-input" data-pin-prop="name"
                            value="${escapeHtml(nameDisp.value)}"
                            placeholder="${nameDisp.mixed ? '多个值' : '名称'}"
                            ${!gisCanEdit ? 'disabled' : ''}>
                    </label>
                    <label class="mcwws-gis-road-prop-row">
                        <span>说明</span>
                        <input type="text" class="mcwws-gis-road-prop-input" data-pin-prop="description"
                            value="${escapeHtml(descDisp.value)}"
                            placeholder="${descDisp.mixed ? '多个值' : '说明（可留空）'}"
                            ${!gisCanEdit ? 'disabled' : ''}>
                    </label>
                    <label class="mcwws-gis-road-prop-row">
                        <span>颜色</span>
                        <input type="text" class="mcwws-gis-road-prop-input" data-pin-prop="color"
                            value="${escapeHtml(colorDisp.value)}"
                            placeholder="${colorDisp.mixed ? '多个值' : '#3b82f6（留空=图层默认）'}"
                            ${!gisCanEdit ? 'disabled' : ''}>
                    </label>
                </div>
                ${batchCount > 1 ? '<p class="mcwws-gis-road-props-hint">修改将批量应用到全部所选' + escapeHtml(typeLabel) + '</p>' : ''}
            </div>
        `;
    }

    function applyVolumePropertyToFeature(feature, prop, input) {
        const vol = ensureVolume3d(feature);
        if (prop === 'shape') {
            const next = normalizeVolumeShape(input.value);
            if (!isCreatableVolumeShape(next)) {
                return false;
            }
            vol.shape = next;
        } else if (prop === 'minY' || prop === 'maxY') {
            const n = Number(String(input.value ?? '').trim());
            if (!Number.isFinite(n)) {
                delete vol[prop];
            } else {
                vol[prop] = n;
            }
        } else if (prop === 'radius') {
            const n = Number(String(input.value ?? '').trim());
            if (!Number.isFinite(n) || n <= 0) {
                delete vol.radius;
            } else {
                vol.radius = n;
            }
        } else {
            return false;
        }
        normalizeVolume3dFeature(feature);
        return true;
    }

    function syncVolumePropertyFromInput(input) {
        const prop = input?.getAttribute?.('data-volume-prop');
        if (!prop) {
            return false;
        }
        const items = getSelectedPolygonFeatures();
        if (!items.length || !gisCanEdit) {
            return false;
        }
        items.forEach(({ feature }) => applyVolumePropertyToFeature(feature, prop, input));
        return true;
    }

    function applyVolumePropertyInput(input) {
        if (!syncVolumePropertyFromInput(input)) {
            return;
        }
        recordGisHistory();
        markDirty();
        renderOverlay();
        renderLayerDialog();
    }

    function applyPinPropertyToFeature(feature, prop, input) {
        const props = ensureFeatureProperties(feature);
        if (prop === 'name') {
            const v = String(input.value || '').trim();
            if (!v) {
                delete props.name;
            } else {
                props.name = v;
            }
        } else if (prop === 'description') {
            const v = String(input.value || '').trim();
            if (!v) {
                delete props.description;
            } else {
                props.description = v;
            }
        } else if (prop === 'color') {
            const v = String(input.value || '').trim();
            if (!v) {
                delete props.color;
            } else {
                props.color = v;
            }
        } else {
            return false;
        }
        return true;
    }

    function syncPinPropertyFromInput(input) {
        const prop = input?.getAttribute?.('data-pin-prop');
        if (!prop) {
            return false;
        }
        let items = getSelectedPinFeatures();
        if (!items.length) {
            items = getSelectedPolygonFeatures();
        }
        if (!items.length || !gisCanEdit) {
            return false;
        }
        items.forEach(({ feature }) => applyPinPropertyToFeature(feature, prop, input));
        return true;
    }

    function applyPinPropertyInput(input) {
        if (!syncPinPropertyFromInput(input)) {
            return;
        }
        recordGisHistory();
        markDirty();
        renderOverlay();
        renderLayerDialog();
    }

    function applyRoadPropertyToFeature(feature, prop, input) {
        const props = ensureFeatureProperties(feature);
        if (prop === 'name') {
            const v = String(input.value || '').trim();
            const segments = getRoadNameSegments(feature);
            if (segments.length <= 1) {
                if (!v) {
                    delete props.name;
                    delete props.roadNameSegments;
                } else {
                    ensureVertexIds(feature);
                    const count = getFeatureVertexCount(feature);
                    const ids = feature.properties.vertexIds;
                    props.name = v;
                    props.roadNameSegments = [{
                        fromVertexId: ids[0],
                        toVertexId: ids[count - 1],
                        name: v
                    }];
                }
            } else if (v) {
                props.name = v;
            } else {
                delete props.name;
            }
        } else if (prop === 'showRoadName') {
            if (input.checked) {
                delete props.showRoadName;
            } else {
                props.showRoadName = false;
            }
        } else if (prop === 'color') {
            const v = String(input.value || '').trim();
            if (!v) {
                delete props.color;
            } else {
                props.color = v;
            }
        } else if (prop === 'strokeWidth') {
            const v = String(input.value || '').trim();
            if (!v) {
                delete props.strokeWidth;
            } else {
                props.strokeWidth = parseLineWidth(v, 3);
            }
        } else if (prop === 'strokeStyle') {
            const v = String(input.value || 'solid').trim().toLowerCase();
            props.strokeStyle = v || 'solid';
        } else if (prop === 'travelDirection') {
            setRoadTravelDirection(feature, input.value);
        } else {
            return false;
        }
        return true;
    }

    function syncRoadPropertyFromInput(input) {
        const prop = input?.getAttribute?.('data-road-prop');
        if (!prop) {
            return false;
        }
        const items = getSelectedLineStringFeatures();
        if (!items.length || !gisCanEdit) {
            return false;
        }
        items.forEach(({ feature }) => applyRoadPropertyToFeature(feature, prop, input));
        return true;
    }

    function syncRoadNameSegmentFromInput(input) {
        const idxRaw = input?.getAttribute?.('data-road-name-seg');
        if (idxRaw == null) {
            return false;
        }
        const found = getSelectedLineStringRoad();
        if (!found || !gisCanEdit) {
            return false;
        }
        const segmentIndex = Number(idxRaw);
        if (!Number.isFinite(segmentIndex)) {
            return false;
        }
        return setRoadNameSegmentName(found.feature, segmentIndex, input.value);
    }

    function applyRoadNameSegmentInput(input) {
        if (!syncRoadNameSegmentFromInput(input)) {
            return;
        }
        recordGisHistory();
        markDirty();
        invalidateRoadLabelCache();
        renderOverlay();
        renderLayerDialog();
    }

    function applyRoadPropertyInput(input) {
        if (!syncRoadPropertyFromInput(input)) {
            return;
        }
        recordGisHistory();
        markDirty();
        renderOverlay();
        if (input?.getAttribute?.('data-road-prop') === 'name'
            || input?.getAttribute?.('data-road-prop') === 'showRoadName'
            || input?.hasAttribute?.('data-road-name-seg')) {
            renderRoadNameLabelsLayer();
        }
        renderLayerDialog();
    }

    function syncVertexVisibilityFromInput(input) {
        const which = input?.getAttribute?.('data-vertex-vis');
        if (which !== 'min' && which !== 'max') {
            return false;
        }
        const targets = getSelectedRoadVertexTargets();
        if (!targets.length || !gisCanEdit) {
            return false;
        }
        const trimmed = String(input.value ?? '').trim();
        if (trimmed) {
            const n = Number(trimmed);
            if (!Number.isFinite(n)) {
                return false;
            }
        }
        targets.forEach(({ feature, vertexIndex }) => {
            setVertexVisibilityBound(feature, vertexIndex, which, input.value);
        });
        return true;
    }

    function buildVertexVisibilityHintInnerHtml() {
        const targets = getSelectedRoadVertexTargets();
        if (!targets.length) {
            return '';
        }
        const camH = getMapCameraHeight();
        const vtxSel = targets.length === 1 ? targets[0] : null;
        const visVisibleNow = vtxSel
            ? isVertexVisibleAtHeight(vtxSel.feature, vtxSel.vertexIndex, camH)
            : false;
        return `当前相机高度 <strong>${Number.isFinite(camH) ? Math.round(camH) : '—'}</strong>；
                        可见条件 <code>a &lt; h ≤ b</code>（须 <strong>a &lt; b</strong>，留空为无穷）；
                        ${targets.length > 1
            ? '修改下限/上限将<strong>批量应用</strong>到所选点'
            : `此点<strong>${visVisibleNow ? '可见' : '不可见'}</strong>`}`;
    }

    function refreshVertexVisibilityHintOnly() {
        const hint = document.querySelector('.mcwws-gis-vertex-vis-hint');
        if (!hint) {
            return;
        }
        hint.innerHTML = buildVertexVisibilityHintInnerHtml();
    }

    function updateGisMenuStatusLine() {
        const el = document.querySelector('.mcwws-gis-menu-status');
        if (!el) {
            return;
        }
        el.textContent = `${statusMessage || ''}${dirty ? ' · 未保存' : ''}`;
        el.className = `mcwws-gis-menu-status${statusKind ? ` is-${statusKind}` : ''}`;
    }

    /** 标记未保存但不重绘整个图层对话框（避免输入时滚动条跳顶） */
    function markDirtySoft() {
        dirty = true;
        invalidateRoadArrowCache();
        invalidateRoadLabelCache();
        updateGisMenuStatusLine();
    }

    function applyVertexVisibilityInput(input) {
        if (!syncVertexVisibilityFromInput(input)) {
            return;
        }
        recordGisHistory();
        markDirtySoft();
        renderOverlay();
        refreshVertexVisibilityHintOnly();
    }

    function getSelectedVertexAlignTargets() {
        const targets = [];
        selectedVertices.forEach((key) => {
            const sel = parseVertexSelectionKey(key);
            if (!sel) {
                return;
            }
            const found = findFeatureById(sel.featureId);
            if (!found) {
                return;
            }
            const world = getVertexWorld(sel.featureId, sel.lane, sel.vertexIndex);
            if (!world) {
                return;
            }
            targets.push({
                featureId: sel.featureId,
                lane: sel.lane || 'center',
                vertexIndex: sel.vertexIndex,
                feature: found.feature,
                world: { x: world.x, y: world.y, z: world.z }
            });
        });
        return targets;
    }

    function alignSelectedVertices(axis) {
        if (!gisCanEdit || selectedVertices.size < 2) {
            return;
        }
        const axisKey = axis === 'x' || axis === 'y' || axis === 'z' ? axis : null;
        if (!axisKey) {
            return;
        }
        const targets = getSelectedVertexAlignTargets();
        if (targets.length < 2) {
            return;
        }
        let sum = 0;
        targets.forEach((t) => {
            sum += t.world[axisKey];
        });
        const aligned = sum / targets.length;
        const needsChange = targets.some((t) => Math.abs(t.world[axisKey] - aligned) > 1e-9);
        if (!needsChange) {
            setStatus(`${axisKey.toUpperCase()} 轴已对齐`, 'info');
            return;
        }
        recordGisHistory();
        targets.forEach(({ feature, lane, vertexIndex, world }) => {
            setFeatureVertexPoint(feature, lane, vertexIndex, {
                ...world,
                [axisKey]: aligned
            }, { skipPanel: true });
        });
        markDirty();
        renderOverlay();
        syncGizmoFromVertexSelection();
        renderLayerDialog();
        setStatus(`已将 ${targets.length} 个点 ${axisKey.toUpperCase()} 对齐为 ${formatCoordForDisplay(aligned)}`, 'ok');
    }

    function formatCoordForDisplay(value) {
        const n = Number(value);
        if (!Number.isFinite(n)) {
            return '—';
        }
        if (Math.abs(n - Math.round(n)) < 1e-9) {
            return String(Math.round(n));
        }
        return n.toFixed(2);
    }

    function renderVertexAlignPanelHtml() {
        if (selectedVertices.size < 2) {
            return '';
        }
        const n = selectedVertices.size;
        return `
            <div class="mcwws-gis-vertex-align">
                <p class="mcwws-gis-menu-section-title">对齐（${n} 点）</p>
                <div class="mcwws-gis-vertex-align-actions">
                    <button type="button" class="mcwws-gis-menu-action" data-action="align-vertices" data-align-axis="x"
                        title="将所选点的 X 坐标对齐为平均值" ${!gisCanEdit ? 'disabled' : ''}>X 对齐</button>
                    <button type="button" class="mcwws-gis-menu-action" data-action="align-vertices" data-align-axis="y"
                        title="将所选点的 Y 坐标对齐为平均值" ${!gisCanEdit ? 'disabled' : ''}>Y 对齐</button>
                    <button type="button" class="mcwws-gis-menu-action" data-action="align-vertices" data-align-axis="z"
                        title="将所选点的 Z 坐标对齐为平均值" ${!gisCanEdit ? 'disabled' : ''}>Z 对齐</button>
                </div>
                <p class="mcwws-gis-road-props-hint">各轴对齐为当前所选点的坐标平均值（与 Gizmo 中心一致）</p>
            </div>
        `;
    }

    function clearBatchVertexVisibility() {
        const targets = getSelectedRoadVertexTargets();
        if (!targets.length || !gisCanEdit) {
            return;
        }
        recordGisHistory();
        targets.forEach(({ feature, vertexIndex }) => {
            ensureVertexVisibility(feature);
            feature.properties.vertexVisibility[vertexIndex] = {};
        });
        markDirty();
        renderOverlay();
        renderLayerDialog();
    }

    /** 保存前将面板输入写入内存模型（避免未触发 change 丢失） */
    function flushGisPropertyInputsFromDom() {
        const wrap = document.querySelector('.mcwws-layer-editor');
        if (!wrap || !gisCanEdit) {
            return;
        }
        wrap.querySelectorAll('[data-road-prop]').forEach((input) => {
            syncRoadPropertyFromInput(input);
        });
        wrap.querySelectorAll('[data-vertex-vis]').forEach((input) => {
            syncVertexVisibilityFromInput(input);
        });
        wrap.querySelectorAll('[data-road-name-seg]').forEach((input) => {
            syncRoadNameSegmentFromInput(input);
        });
        wrap.querySelectorAll('[data-volume-prop]').forEach((input) => {
            syncVolumePropertyFromInput(input);
        });
        wrap.querySelectorAll('[data-pin-prop]').forEach((input) => {
            syncPinPropertyFromInput(input);
        });
    }

    function splitRoadNameAtSelectedVertex() {
        const road = getSelectedLineStringRoad();
        const vtx = getPrimaryRoadVertexSelection();
        if (!road || !vtx || vtx.feature.id !== road.feature.id) {
            setStatus('请先选中该道路中间的一个特征点', 'error');
            return;
        }
        splitRoadNameAtVertex(road.feature, vtx.vertexIndex);
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
            normalizeGisProject(project);
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
        flushGisPropertyInputsFromDom();
        normalizeGisProject(project);
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
            normalizeGisProject(project);
            dirty = false;
            setStatus('已保存到服务器（仍可 Ctrl+Z 撤销）', 'ok');
        } catch (err) {
            setStatus(`保存失败：${err.message}`, 'error');
        } finally {
            saving = false;
            renderOverlay();
            renderPanel();
        }
    }

    function markDirty() {
        dirty = true;
        invalidateRoadArrowCache();
        invalidateRoadLabelCache();
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
        normalizeGisProject(project);
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
        normalizeGisFeature(feature);
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

    function getFeatureIdsForClipboard() {
        if (selectedFeatureIds.size > 0) {
            return Array.from(selectedFeatureIds);
        }
        if (selectedVertices.size > 0) {
            const ids = new Set();
            selectedVertices.forEach((key) => {
                const sel = parseVertexSelectionKey(key);
                if (sel?.featureId) {
                    ids.add(sel.featureId);
                }
            });
            return Array.from(ids);
        }
        return [];
    }

    function hasGisClipboard() {
        return !!(gisClipboard?.features?.length);
    }

    function cloneFeaturesForClipboard(ids) {
        const features = [];
        ids.forEach((id) => {
            const found = findFeatureById(id);
            if (found) {
                features.push(JSON.parse(JSON.stringify(found.feature)));
            }
        });
        return features;
    }

    function copySelectedFeaturesToClipboard() {
        const ids = getFeatureIdsForClipboard();
        if (!ids.length) {
            setStatus('请先选中要复制的要素（或特征点）', 'error');
            return false;
        }
        const features = cloneFeaturesForClipboard(ids);
        if (!features.length) {
            setStatus('未找到可复制的要素', 'error');
            return false;
        }
        gisClipboard = { features };
        gisClipboardPasteGen = 0;
        setStatus(`已复制 ${features.length} 项（Ctrl+V 粘贴）`, 'ok');
        return true;
    }

    function pasteClipboardFeatures() {
        if (!hasGisClipboard() || !project?.layers) {
            setStatus('剪贴板为空', 'error');
            return false;
        }
        const layer = getActiveLayer();
        if (!layer) {
            setStatus('请先选择目标图层', 'error');
            return false;
        }
        gisClipboardPasteGen += 1;
        const step = GIS_PASTE_OFFSET_BLOCKS * gisClipboardPasteGen;
        const delta = { x: step, y: 0, z: step };
        const usedVertexIds = collectAllVertexIdsInProject();
        const usedFeatureIds = new Set();
        project.layers.forEach((l) => {
            (l.features || []).forEach((f) => {
                if (f?.id) {
                    usedFeatureIds.add(f.id);
                }
            });
        });
        recordGisHistory();
        if (!Array.isArray(layer.features)) {
            layer.features = [];
        }
        const newIds = [];
        const mapId = getCurrentMapId();
        gisClipboard.features.forEach((src) => {
            const feature = JSON.parse(JSON.stringify(src));
            let fid = newFeatureId();
            while (usedFeatureIds.has(fid)) {
                fid = newFeatureId();
            }
            usedFeatureIds.add(fid);
            feature.id = fid;
            feature.map = mapId;
            feature.layerId = layer.id;
            stripLegacyRoadLaneProps(feature);
            const pts = getFeatureVertexPoints(feature);
            if (pts.length) {
                feature.coordinates = pts.map((p) => ({
                    x: p.x + delta.x,
                    y: p.y + delta.y,
                    z: p.z + delta.z
                }));
            }
            assignFreshVertexIds(feature, usedVertexIds);
            normalizeGisFeature(feature);
            layer.features.push(feature);
            newIds.push(feature.id);
        });
        clearGisSelection();
        newIds.forEach((id) => selectedFeatureIds.add(id));
        markDirty();
        renderOverlay();
        renderPanel();
        setStatus(`已粘贴 ${newIds.length} 项`, 'ok');
        return true;
    }

    function cutSelectedFeatures() {
        const ids = getFeatureIdsForClipboard();
        if (!ids.length || !project?.layers) {
            setStatus('请先选中要剪切的要素（或特征点）', 'error');
            return false;
        }
        const features = cloneFeaturesForClipboard(ids);
        if (!features.length) {
            setStatus('未找到可剪切的要素', 'error');
            return false;
        }
        const idSet = new Set(ids);
        recordGisHistory();
        project.layers.forEach((layer) => {
            layer.features = (layer.features || []).filter((f) => !idSet.has(f.id));
        });
        gisClipboard = { features };
        gisClipboardPasteGen = 0;
        clearGisSelection();
        markDirty();
        renderOverlay();
        renderPanel();
        setStatus(`已剪切 ${features.length} 项（Ctrl+V 粘贴）`, 'ok');
        return true;
    }

    function minVerticesForFeatureType(type, feature) {
        if (type === 'Polygon') {
            const shape = feature ? getVolumeShape(feature) : activeVolumeShape;
            if (shape === VOLUME_SHAPES.HEXAHEDRON) {
                return 8;
            }
            if (shape === VOLUME_SHAPES.BOX && feature && splitBoxPrismPoints(getFeatureVertexPoints(feature))) {
                return 3;
            }
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
        const boxFeatureDeletes = new Map();
        const byFeatureLane = new Map();
        selectedVertices.forEach((key) => {
            const sel = parseVertexSelectionKey(key);
            if (!sel) {
                return;
            }
            const found = findFeatureById(sel.featureId);
            if (found && isBoxPrismFeature(found.feature)) {
                if (!boxFeatureDeletes.has(sel.featureId)) {
                    boxFeatureDeletes.set(sel.featureId, new Set());
                }
                boxFeatureDeletes.get(sel.featureId).add(sel.vertexIndex);
                return;
            }
            const laneKey = `${sel.featureId}:${sel.lane || 'center'}`;
            if (!byFeatureLane.has(laneKey)) {
                byFeatureLane.set(laneKey, { featureId: sel.featureId, lane: sel.lane || 'center', indices: [] });
            }
            byFeatureLane.get(laneKey).indices.push(sel.vertexIndex);
        });
        if (!boxFeatureDeletes.size && !byFeatureLane.size) {
            clearSelectedVertices();
            return false;
        }
        recordGisHistory();
        const deleteWholeFeatures = new Set();
        boxFeatureDeletes.forEach((indexSet, featureId) => {
            const found = findFeatureById(featureId);
            if (!found) {
                return;
            }
            const ok = deleteBoxPrismRingIndices(found.feature, [...indexSet]);
            if (!ok) {
                deleteWholeFeatures.add(featureId);
            }
        });
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
            const minVerts = minVerticesForFeatureType(feature.type, feature);
            if (pts.length - unique.length < minVerts) {
                deleteWholeFeatures.add(featureId);
                return;
            }
            removeVertexIdsAt(feature, unique);
            unique.forEach((i) => {
                pts.splice(i, 1);
            });
            setFeatureLanePoints(feature, lane, pts);
            reindexRoadNameSegments(feature);
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
        if (type === 'Polygon') {
            const minVerts = minVerticesForFeatureType('Polygon', { type: 'Polygon', properties: { volume3d: { shape: activeVolumeShape } } });
            if (draftPoints.length < minVerts) {
                const shapeLabel = VOLUME_SHAPE_OPTIONS.find((o) => o.id === activeVolumeShape)?.label || '区域';
                setStatus(`${shapeLabel}至少需要 ${minVerts} 个点`, 'error');
                return;
            }
        }

        const meta = promptFeatureMeta(type === 'Polygon' ? '区域' : '道路');
        if (!meta) return;

        const properties = { ...meta };
        if (type === 'LineString') {
            properties.color = GIS_DEFAULT_ROAD_COLOR;
        }
        if (type === 'Polygon') {
            properties.color = GIS_DEFAULT_REGION_COLOR;
            properties.volume3d = { shape: activeVolumeShape };
        }

        let coordinates = draftPoints.map((p) => ({ ...p }));
        if (type === 'Polygon' && activeVolumeShape === VOLUME_SHAPES.BOX) {
            const ys = inferFootprintYs(draftPoints);
            properties.volume3d.minY = ys.minY;
            properties.volume3d.maxY = ys.maxY;
            const bottom = draftPoints.map((p) => ({ x: p.x, y: ys.minY, z: p.z }));
            const top = draftPoints.map((p) => ({ x: p.x, y: ys.maxY, z: p.z }));
            coordinates = [...bottom, ...top];
        }

        addFeature({
            id: newFeatureId(),
            type,
            map,
            layerId: layer.id,
            coordinates,
            properties
        });
        draftPoints = [];
        draftHover = null;
        resetVolumeDraftState();
        setStatus('要素已添加，记得保存', 'ok');
        renderOverlay();
    }

    function cancelDraft() {
        draftPoints = [];
        draftHover = null;
        resetVolumeDraftState();
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
            if (activeTool === 'polygon' && activeVolumeShape === VOLUME_SHAPES.HEXAHEDRON) {
                draftPoints.push(point);
                if (draftVolumePhase === 'bottom' && draftPoints.length >= 4) {
                    draftVolumePhase = 'top';
                }
                if (draftPoints.length >= 8) {
                    finishDraft();
                    return;
                }
                setStatus(getVolumeDraftStatusText(), '');
                renderOverlay();
                return;
            }
            draftPoints.push(point);
            setStatus(
                activeTool === 'line'
                    ? `道路：已 ${draftPoints.length} 点 — 双击或点「完成」结束，Esc 取消`
                    : getVolumeDraftStatusText(),
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
        return isGisPickTarget(target) || isGisVertexUiTarget(target);
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

    function iterLineStringScreenSegmentsForPick(feature, view, camera, onSegment) {
        if (!feature || feature.type !== 'LineString') {
            return;
        }
        const points = coordsToPoints(feature.coordinates);
        if (points.length < 2) {
            return;
        }
        const viewHeight = getMapCameraHeight();
        buildVisiblePointChains(points, feature, viewHeight).forEach((chain) => {
            iterClippedLineScreenSegments(chain, view, camera, onSegment);
        });
    }

    function pickFeatureIdFromDomTarget(target) {
        const el = target?.closest?.('#mcwws-gis-svg-layer [data-fid], .mcwws-gis-pin[data-fid]');
        const fid = el?.getAttribute?.('data-fid');
        return fid || null;
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
                iterLineStringScreenSegmentsForPick(feature, view, camera, (a, b) => {
                    const d = distPointToScreenSegment(clientX, clientY, a.x, a.y, b.x, b.y);
                    if (d < bestDist) {
                        bestDist = d;
                        bestId = feature.id;
                    }
                });
                return;
            }

            if (feature.type === 'Polygon' && points.length >= 3) {
                const viewHeight = getMapCameraHeight();
                let pickPoints = points;
                const volCfg = getVolume3dConfig(feature);
                if (volCfg?.shape === VOLUME_SHAPES.BOX) {
                    const split = splitBoxPrismPoints(points);
                    if (split) {
                        pickPoints = split.bottom;
                    }
                }
                const allVisible = pickPoints.every((_, i) => isVertexVisibleAtHeight(feature, i, viewHeight));
                if (allVisible) {
                    const ring = getClippedScreenRingForPolygon(pickPoints, view, camera);
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
                    return;
                }
                for (let i = 0; i < pickPoints.length; i += 1) {
                    const j = (i + 1) % pickPoints.length;
                    if (!isSegmentVisibleAtHeight(feature, i, j, viewHeight)) {
                        continue;
                    }
                    iterClippedLineScreenSegments([pickPoints[i], pickPoints[j]], view, camera, (a, b) => {
                        const d = distPointToScreenSegment(clientX, clientY, a.x, a.y, b.x, b.y);
                        if (d < bestDist) {
                            bestDist = d;
                            bestId = feature.id;
                        }
                    });
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
            iterLineStringScreenSegmentsForPick(feature, view, camera, (a, b) => {
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

    function shouldAllowBrowserContextMenu(target) {
        return !!target?.closest?.(
            'input:not([type="button"]):not([type="submit"]):not([type="reset"]), textarea, select, [contenteditable="true"], .mcwws-layer-dialog'
        );
    }

    function isMapViewContextMenuTarget(target) {
        return !!target?.closest?.(
            '#map-container, #mcwws-gis-svg-layer, #mcwws-gis-pin-layer, #mcwws-gis-vertex-layer, #mcwws-gis-lasso-layer'
        );
    }

    function onSuppressMapContextMenu(event) {
        if (shouldAllowBrowserContextMenu(event.target)) {
            return;
        }
        if (gisLassoPointer || isMapViewContextMenuTarget(event.target)) {
            event.preventDefault();
        }
    }

    function bindMapContextMenuSuppression() {
        if (mapContextMenuBound) {
            return;
        }
        mapContextMenuBound = true;
        document.addEventListener('contextmenu', onSuppressMapContextMenu, true);
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
        if (event.target?.closest?.('.mcwws-ctrl-gis-wrap, .mcwws-layer-dialog, .mcwws-map-controls')) {
            return;
        }
        if (isPointerOverLayerDialog(event.clientX, event.clientY)) {
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
        if (gisEditMode && draftPoints.length > 0
            && (activeTool === 'line' || activeTool === 'polygon')
            && isGisPickTarget(event.target)) {
            draftHover = pickWorldFromScreen(event.clientX, event.clientY);
            renderOverlay();
        }
        if (isGisSelectMode()) {
            updateGisHoverSegmentInsert(event.clientX, event.clientY);
        } else {
            clearGisHoverSegmentInsert();
        }
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
            syncGisSegmentInsertModifierFromEvent(event);
            if ((event.ctrlKey || event.metaKey) && tryInsertSegmentAtScreen(event.clientX, event.clientY, event)) {
                event.stopPropagation();
                renderOverlay();
                renderPanel();
                return;
            }
            const vtx = isPointerOverLayerDialog(event.clientX, event.clientY)
                ? null
                : pickVertexAtScreen(event.clientX, event.clientY);
            if (vtx) {
                selectVertex(vtx.featureId, vtx.lane || 'center', vtx.vertexIndex);
                event.stopPropagation();
                return;
            }
            const fid = pickFeatureIdFromDomTarget(event.target)
                || pickFeatureAtScreen(event.clientX, event.clientY);
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
            document.addEventListener('pointermove', onDocumentPointerMoveCapture, true);
        }
        bindGisLassoCapture();
        bindVolumeMeshRenderHook();
    }

    function bindVolumeMeshRenderHook() {
        const bm = getBlueMapApp();
        if (!bm?.mapViewer) {
            return;
        }
        try {
            installMcwwsVolumeLayerRenderer(bm.mapViewer);
            bindVolumeMeshRenderSync(bm.mapViewer);
        } catch (err) {
            console.warn('[mcwws-gis] bindVolumeMeshRenderHook failed', err);
        }
        if (!bm?.events || volumeRenderHookBound) {
            return;
        }
        volumeRenderHookBound = true;
        bm.events.addEventListener('bluemapMapChanged', () => {
            if (volumeMeshRoot) {
                volumeMeshRoot.userData.mcwwsRenderSyncBound = false;
            }
            if (bm.mapViewer) {
                const state = getMcwwsVolumeLayerHookState(bm.mapViewer);
                if (state) {
                    state.hookInstalled = false;
                }
                try {
                    installMcwwsVolumeLayerRenderer(bm.mapViewer);
                    bindVolumeMeshRenderSync(bm.mapViewer);
                } catch (err) {
                    console.warn('[mcwws-gis] rebind volume render hook failed', err);
                }
            }
        });
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

    function ensureSvgRoadNameLayer() {
        let svg = document.getElementById(ROAD_NAME_SVG_LAYER_ID);
        if (!svg) {
            svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
            svg.id = ROAD_NAME_SVG_LAYER_ID;
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
            path.classList.remove('mcwws-gis-line', 'mcwws-gis-polygon', 'mcwws-gis-line--dual', 'mcwws-gis-volume3d', 'mcwws-gis-volume3d-fill');
            path.classList.add('mcwws-gis-feature', ...classNames);
        }
        return path;
    }

    function ensureSvgHitPath(svg, key, featureId) {
        let path = svgPathElements.get(key);
        if (!path) {
            path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            path.setAttribute('data-fid', featureId);
            path.classList.add('mcwws-gis-feature', 'mcwws-gis-hit');
            svg.appendChild(path);
            svgPathElements.set(key, path);
        } else {
            path.classList.add('mcwws-gis-feature', 'mcwws-gis-hit');
        }
        return path;
    }

    function ensureSvgArrowGroup(svg, key, featureId) {
        let group = svgLaneArrowGroups.get(key);
        if (!group) {
            group = document.createElementNS('http://www.w3.org/2000/svg', 'g');
            group.setAttribute('data-fid', featureId);
            group.classList.add('mcwws-gis-lane-arrow-group');
            svg.appendChild(group);
            svgLaneArrowGroups.set(key, group);
        }
        return group;
    }

    function clearSvgGroupChildren(group) {
        while (group.firstChild) {
            group.removeChild(group.firstChild);
        }
    }

    function appendRoadArrowMarkerAt(group, screenX, screenY, angleRad, fillColor) {
        const size = GIS_ROAD_ARROW_SIZE_PX;
        const cos = Math.cos(angleRad);
        const sin = Math.sin(angleRad);
        const tipX = screenX + cos * (size * 0.55);
        const tipY = screenY + sin * (size * 0.55);
        const backX = screenX - cos * (size * 0.45);
        const backY = screenY - sin * (size * 0.45);
        const wing = size * 0.38;
        const lx = -sin * wing;
        const ly = cos * wing;
        const d = `M ${tipX.toFixed(2)} ${tipY.toFixed(2)} L ${(backX + lx).toFixed(2)} ${(backY + ly).toFixed(2)} L ${(backX - lx).toFixed(2)} ${(backY - ly).toFixed(2)} Z`;
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        path.setAttribute('d', d);
        path.setAttribute('fill', fillColor);
        group.appendChild(path);
    }

    function getRoadArrowSpacingPx() {
        const h = getMapCameraHeight();
        if (!Number.isFinite(h)) {
            return 140;
        }
        return Math.max(100, Math.min(220, h * 0.75));
    }

    function getGisViewArrowSignature(view, camera) {
        if (camera?.matrixWorldInverse?.elements && camera?.projectionMatrix?.elements) {
            const wi = camera.matrixWorldInverse.elements;
            const pj = camera.projectionMatrix.elements;
            const snap = (n) => Number(n).toFixed(2);
            return `c:${snap(wi[12])}:${snap(wi[13])}:${snap(wi[14])}:${snap(pj[0])}:${snap(pj[5])}`;
        }
        const rot = Number(view?.rotation ?? 0).toFixed(2);
        const dist = Number(view?.distance ?? view?.height ?? 0).toFixed(1);
        return `v:${Math.round(view?.x ?? 0)}:${Math.round(view?.z ?? 0)}:${rot}:${dist}`;
    }

    function invalidateRoadArrowCache() {
        svgLaneArrowGroups.forEach((group) => {
            delete group.dataset.arrowSig;
        });
    }

    function invalidateRoadLabelCache() {
        svgLaneNameGroups.forEach((group) => {
            delete group.dataset.nameStructSig;
        });
    }

    function worldPolylineLengthXZ(points) {
        if (!points || points.length < 2) {
            return 0;
        }
        let len = 0;
        for (let i = 1; i < points.length; i += 1) {
            len += Math.hypot(points[i].x - points[i - 1].x, points[i].z - points[i - 1].z);
        }
        return len;
    }

    function sampleWorldPolylineAtDistance(points, distance) {
        if (!points?.length) {
            return null;
        }
        if (points.length === 1) {
            return { point: { ...points[0] }, angleRad: 0 };
        }
        let acc = 0;
        for (let i = 1; i < points.length; i += 1) {
            const a = points[i - 1];
            const b = points[i];
            const segLen = Math.hypot(b.x - a.x, b.z - a.z);
            if (segLen < 1e-9) {
                continue;
            }
            if (acc + segLen >= distance) {
                const t = (distance - acc) / segLen;
                return {
                    point: {
                        x: a.x + (b.x - a.x) * t,
                        y: a.y + (b.y - a.y) * t,
                        z: a.z + (b.z - a.z) * t
                    },
                    angleRad: Math.atan2(b.z - a.z, b.x - a.x)
                };
            }
            acc += segLen;
        }
        const last = points[points.length - 1];
        const prev = points[points.length - 2];
        return {
            point: { x: last.x, y: last.y, z: last.z },
            angleRad: Math.atan2(last.z - prev.z, last.x - prev.x)
        };
    }

    function getRoadLabelWorldChains(feature, viewHeight) {
        const points = coordsToPoints(feature.coordinates);
        if (points.length < 2) {
            return [];
        }
        if (!isGisHeightVisibilityActive()) {
            return [points];
        }
        const chains = buildVisiblePointChains(points, feature, viewHeight);
        return chains.length ? chains : [];
    }

    function getRoadLabelWorldChain(feature, viewHeight) {
        const chains = getRoadLabelWorldChains(feature, viewHeight);
        return chains[0] || null;
    }

    function interpolateWorldOnScreenEdge(p0, p1, screen, view, camera) {
        const s0 = projectGisPoint(p0, view, camera, false);
        const s1 = projectGisPoint(p1, view, camera, false);
        if (!s0 || !s1 || s0.behind || s1.behind) {
            return {
                x: (p0.x + p1.x) / 2,
                y: (p0.y + p1.y) / 2,
                z: (p0.z + p1.z) / 2
            };
        }
        const dx = s1.x - s0.x;
        const dy = s1.y - s0.y;
        const len2 = dx * dx + dy * dy;
        let t = 0.5;
        if (len2 > 1e-6) {
            t = ((screen.x - s0.x) * dx + (screen.y - s0.y) * dy) / len2;
            t = Math.max(0, Math.min(1, t));
        }
        return {
            x: p0.x + (p1.x - p0.x) * t,
            y: p0.y + (p1.y - p0.y) * t,
            z: p0.z + (p1.z - p0.z) * t
        };
    }

    function appendClippedWorldScreenSegment(chains, seg) {
        const [a, b] = seg;
        if (!chains.length) {
            chains.push([a, b]);
            return;
        }
        const chain = chains[chains.length - 1];
        const last = chain[chain.length - 1];
        if (!screenPointsNear(last.screen, a.screen)) {
            chains.push([a, b]);
            return;
        }
        if (!screenPointsNear(last.screen, b.screen)) {
            chain.push(b);
        }
    }

    function buildClippedScreenSpansFromWorldChain(worldChain, view, camera) {
        const chains = [];
        for (let i = 0; i < worldChain.length - 1; i += 1) {
            const p0 = worldChain[i];
            const p1 = worldChain[i + 1];
            iterClippedLineScreenSegments([p0, p1], view, camera, (s0, s1) => {
                appendClippedWorldScreenSegment(chains, [
                    { world: interpolateWorldOnScreenEdge(p0, p1, s0, view, camera), screen: s0 },
                    { world: interpolateWorldOnScreenEdge(p0, p1, s1, view, camera), screen: s1 }
                ]);
            });
        }
        return chains.filter((chain) => chain.length >= 2);
    }

    function collectRoadClippedScreenSpans(feature, view, camera, viewHeight) {
        const spans = [];
        getRoadLabelWorldChains(feature, viewHeight).forEach((chain) => {
            buildClippedScreenSpansFromWorldChain(chain, view, camera).forEach((span) => {
                spans.push(span);
            });
        });
        return spans;
    }

    function getRoadVisibleScreenLength(feature, view, camera, viewHeight) {
        return collectRoadClippedScreenSpans(feature, view, camera, viewHeight)
            .reduce((sum, span) => sum + screenSpanLength(span), 0);
    }

    function isRoadVisibleOnScreenForLabels(feature, view, camera, viewHeight) {
        return getRoadNameSegments(feature).some((segment) => {
            if (!segment.name) {
                return false;
            }
            return getRoadLabelWorldChainsForSegment(feature, segment, viewHeight).some((chain) => {
                const spans = buildClippedScreenSpansFromWorldChain(chain, view, camera);
                return spans.some((span) => screenSpanLength(span) >= GIS_ROAD_NAME_MIN_VISIBLE_PX);
            });
        });
    }

    function computeRoadNameFontSize(viewHeight) {
        const h = Number.isFinite(viewHeight) ? viewHeight : getMapCameraHeight();
        const minH = GIS_ROAD_NAME_FONT_HEIGHT_MIN;
        const maxH = GIS_ROAD_NAME_MAX_VIEW_HEIGHT;
        const clamped = Math.max(minH, Math.min(maxH, h));
        const t = Math.log(clamped / minH) / Math.log(maxH / minH);
        const size = GIS_ROAD_NAME_MAX_FONT_PX - t * (GIS_ROAD_NAME_MAX_FONT_PX - GIS_ROAD_NAME_MIN_FONT_PX);
        return Math.max(
            GIS_ROAD_NAME_MIN_FONT_PX,
            Math.min(GIS_ROAD_NAME_MAX_FONT_PX, Math.round(size * 10) / 10)
        );
    }

    function getRoadNameDisplayPolicy(viewHeight) {
        const h = Number.isFinite(viewHeight) ? viewHeight : getMapCameraHeight();
        if (!Number.isFinite(h) || h > GIS_ROAD_NAME_MAX_VIEW_HEIGHT) {
            return { show: false, bucket: 'off', fontSize: 0 };
        }
        const fontSize = computeRoadNameFontSize(h);
        const minH = GIS_ROAD_NAME_FONT_HEIGHT_MIN;
        const maxH = GIS_ROAD_NAME_MAX_VIEW_HEIGHT;
        const clamped = Math.max(minH, Math.min(maxH, h));
        const t = Math.log(clamped / minH) / Math.log(maxH / minH);
        const bucket = t > 0.72 ? 'far' : t > 0.42 ? 'mid' : t > 0.18 ? 'near' : 'close';
        return { show: true, bucket, fontSize };
    }

    function getRoadNameHeightBucket() {
        return getRoadNameDisplayPolicy(getMapCameraHeight()).bucket;
    }

    function isRoadNameOnScreen(screen) {
        if (!screen || screen.behind) {
            return false;
        }
        const m = GIS_ROAD_NAME_SCREEN_MARGIN_PX;
        return screen.x >= -m
            && screen.y >= -m
            && screen.x <= window.innerWidth + m
            && screen.y <= window.innerHeight + m;
    }

    function screenSpanLength(span) {
        let len = 0;
        for (let i = 1; i < span.length; i += 1) {
            len += Math.hypot(
                span[i].screen.x - span[i - 1].screen.x,
                span[i].screen.y - span[i - 1].screen.y
            );
        }
        return len;
    }

    function sampleScreenSpanAt(span, targetDist) {
        let acc = 0;
        for (let i = 1; i < span.length; i += 1) {
            const a = span[i - 1];
            const b = span[i];
            const segLen = Math.hypot(b.screen.x - a.screen.x, b.screen.y - a.screen.y);
            if (segLen < 1e-6) {
                continue;
            }
            if (acc + segLen >= targetDist) {
                const t = (targetDist - acc) / segLen;
                return {
                    world: {
                        x: a.world.x + (b.world.x - a.world.x) * t,
                        y: a.world.y + (b.world.y - a.world.y) * t,
                        z: a.world.z + (b.world.z - a.world.z) * t
                    },
                    screen: {
                        x: a.screen.x + (b.screen.x - a.screen.x) * t,
                        y: a.screen.y + (b.screen.y - a.screen.y) * t
                    },
                    angleRad: Math.atan2(b.screen.y - a.screen.y, b.screen.x - a.screen.x)
                };
            }
            acc += segLen;
        }
        const last = span[span.length - 1];
        const prev = span[span.length - 2] || last;
        return {
            world: { ...last.world },
            screen: { ...last.screen },
            angleRad: Math.atan2(last.screen.y - prev.screen.y, last.screen.x - prev.screen.x)
        };
    }

    function pickRoadNameAnchorForPoints(points, view, camera, labelName) {
        const spans = buildClippedScreenSpansFromWorldChain(points, view, camera);
        let bestSpan = null;
        let bestLen = 0;
        spans.forEach((span) => {
            const len = screenSpanLength(span);
            if (len > bestLen) {
                bestLen = len;
                bestSpan = span;
            }
        });
        if (!bestSpan || bestLen < GIS_ROAD_NAME_MIN_VISIBLE_PX) {
            return null;
        }
        const slot = sampleScreenSpanAt(bestSpan, bestLen / 2);
        return slot ? { ...slot, primary: true, name: labelName } : null;
    }

    function pickRoadNameAnchorsOnScreen(feature, view, camera, viewHeight) {
        const anchors = [];
        getRoadNameSegments(feature).forEach((segment) => {
            if (!segment.name) {
                return;
            }
            let bestAnchor = null;
            let bestLen = 0;
            getRoadLabelWorldChainsForSegment(feature, segment, viewHeight).forEach((chain) => {
                const anchor = pickRoadNameAnchorForPoints(chain, view, camera, segment.name);
                if (!anchor) {
                    return;
                }
                const spans = buildClippedScreenSpansFromWorldChain(chain, view, camera);
                const len = spans.reduce((sum, span) => sum + screenSpanLength(span), 0);
                if (len > bestLen) {
                    bestLen = len;
                    bestAnchor = anchor;
                }
            });
            if (bestAnchor) {
                anchors.push(bestAnchor);
            }
        });
        return anchors;
    }

    function getRoadNameAnchorStructSig(feature, policy, viewHeight) {
        return `${getRoadNameSegmentsSignature(feature)}|${policy.bucket}`;
    }

    function fitRoadNameFontSize(name, desiredFont, roadWidthPx, options = {}) {
        let size = desiredFont;
        if (roadWidthPx > 0 && name) {
            const estText = Math.max(12, name.length * desiredFont * 0.55);
            const limit = roadWidthPx * GIS_ROAD_NAME_MAX_TEXT_VS_ROAD;
            if (limit > 0 && estText > limit) {
                const scaled = Math.floor(desiredFont * (limit / estText));
                size = Math.max(GIS_ROAD_NAME_MIN_FONT_PX, scaled || GIS_ROAD_NAME_MIN_FONT_PX);
            }
        }
        size = Math.min(desiredFont, size);
        if (!options.primary && size < GIS_ROAD_NAME_MIN_FONT_PX) {
            return 0;
        }
        return Math.max(GIS_ROAD_NAME_MIN_FONT_PX, size);
    }

    function estimateRoadWidthScreenPx(world, view, camera) {
        const center = projectGisPoint(world, view, camera, false);
        if (!center || center.behind) {
            return 0;
        }
        const offsets = [
            { x: 4, z: 0 },
            { x: -4, z: 0 },
            { x: 0, z: 4 },
            { x: 0, z: -4 }
        ];
        let best = 0;
        offsets.forEach((off) => {
            const edge = projectGisPoint({
                x: world.x + off.x,
                y: world.y,
                z: world.z + off.z
            }, view, camera, false);
            if (!edge || edge.behind) {
                return;
            }
            best = Math.max(best, Math.hypot(edge.x - center.x, edge.y - center.y) * 2);
        });
        return best;
    }

    function estimateRoadNameLabelRect(screen, name, fontSize, angleDeg) {
        const estW = Math.max(18, name.length * fontSize * 0.55);
        const estH = fontSize * 1.35;
        const rad = (angleDeg * Math.PI) / 180;
        const cos = Math.abs(Math.cos(rad));
        const sin = Math.abs(Math.sin(rad));
        const w = estW * cos + estH * sin;
        const h = estW * sin + estH * cos;
        return {
            left: screen.x - w / 2 - GIS_ROAD_NAME_DEDUP_PAD_PX,
            top: screen.y - h / 2 - GIS_ROAD_NAME_DEDUP_PAD_PX,
            right: screen.x + w / 2 + GIS_ROAD_NAME_DEDUP_PAD_PX,
            bottom: screen.y + h / 2 + GIS_ROAD_NAME_DEDUP_PAD_PX
        };
    }

    function roadNameRectsOverlap(a, b) {
        return !(a.right < b.left || a.left > b.right || a.bottom < b.top || a.top > b.bottom);
    }

    function shouldMergeSameNameRoadLabels(a, b) {
        if (String(a.name || '').trim() !== String(b.name || '').trim()) {
            return false;
        }
        const screenDist = Math.hypot(a.screen.x - b.screen.x, a.screen.y - b.screen.y);
        if (screenDist <= GIS_ROAD_NAME_SAME_NAME_MERGE_SCREEN_PX) {
            return true;
        }
        const wa = a.anchor.world;
        const wb = b.anchor.world;
        const worldDist = Math.hypot(wa.x - wb.x, wa.z - wb.z);
        return worldDist <= GIS_ROAD_NAME_SAME_NAME_MERGE_WORLD_XZ;
    }

    function mergeSameNameRoadLabelAnchors(items, view, camera) {
        const nodes = [];
        items.forEach((item) => {
            const name = String(item.anchor?.name || '').trim();
            if (!name || !item.anchor?.world) {
                return;
            }
            const screen = projectGisPoint(item.anchor.world, view, camera, false);
            if (!screen || screen.behind) {
                return;
            }
            nodes.push({ ...item, name, screen });
        });
        if (!nodes.length) {
            return [];
        }
        const parent = nodes.map((_, index) => index);
        const find = (index) => {
            if (parent[index] !== index) {
                parent[index] = find(parent[index]);
            }
            return parent[index];
        };
        const unite = (a, b) => {
            const ra = find(a);
            const rb = find(b);
            if (ra !== rb) {
                parent[rb] = ra;
            }
        };
        for (let i = 0; i < nodes.length; i += 1) {
            for (let j = i + 1; j < nodes.length; j += 1) {
                if (shouldMergeSameNameRoadLabels(nodes[i], nodes[j])) {
                    unite(i, j);
                }
            }
        }
        const buckets = new Map();
        nodes.forEach((node, index) => {
            const root = find(index);
            if (!buckets.has(root)) {
                buckets.set(root, []);
            }
            buckets.get(root).push(node);
        });
        return [...buckets.values()].map((members) => {
            const count = members.length;
            const world = { x: 0, y: 0, z: 0 };
            let cosSum = 0;
            let sinSum = 0;
            members.forEach(({ anchor }) => {
                world.x += anchor.world.x;
                world.y += anchor.world.y;
                world.z += anchor.world.z;
                cosSum += Math.cos(anchor.angleRad);
                sinSum += Math.sin(anchor.angleRad);
            });
            const mergedDimmed = members.every((member) => member.dimmed);
            return {
                name: members[0].name,
                world: {
                    x: world.x / count,
                    y: world.y / count,
                    z: world.z / count
                },
                angleRad: Math.atan2(sinSum / count, cosSum / count),
                primary: true,
                dimmed: mergedDimmed,
                mergeCount: count
            };
        });
    }

    function getMergedRoadNamesStructSig(anchors, policy, viewHeight, selectionActive) {
        const anchorSig = anchors
            .map((anchor) => {
                const w = anchor.world;
                return `${anchor.name}|${w.x.toFixed(2)},${w.y.toFixed(2)},${w.z.toFixed(2)}|${anchor.mergeCount || 1}|${anchor.dimmed ? 1 : 0}`;
            })
            .join(';');
        return `${anchorSig}|${policy.bucket}|${Math.round(viewHeight)}|${selectionActive ? 1 : 0}`;
    }

    function normalizeLabelRotationDeg(angleRad) {
        let deg = (angleRad * 180) / Math.PI;
        while (deg <= -90) {
            deg += 180;
        }
        while (deg > 90) {
            deg -= 180;
        }
        return deg;
    }

    function computeRoadNameAnchors(feature, view, camera, viewHeight) {
        const policy = getRoadNameDisplayPolicy(viewHeight);
        if (!policy.show) {
            return { policy, anchors: [] };
        }
        if (!featureHasAnyRoadName(feature)) {
            return { policy, anchors: [] };
        }
        if (!isRoadVisibleOnScreenForLabels(feature, view, camera, viewHeight)) {
            return { policy, anchors: [] };
        }
        const anchors = pickRoadNameAnchorsOnScreen(feature, view, camera, viewHeight);
        if (!anchors.length) {
            return { policy, anchors: [] };
        }
        return { policy, anchors };
    }

    function ensureSvgNameGroup(svg, key, featureId) {
        let group = svgLaneNameGroups.get(key);
        if (!group) {
            group = document.createElementNS('http://www.w3.org/2000/svg', 'g');
            group.setAttribute('data-fid', featureId);
            group.classList.add('mcwws-gis-road-name-group');
            svg.appendChild(group);
            svgLaneNameGroups.set(key, group);
        }
        return group;
    }

    function rebuildRoadNameGroupStructure(group, anchors) {
        clearSvgGroupChildren(group);
        anchors.forEach((anchor, index) => {
            if (!anchor?.world || !anchor.name) {
                return;
            }
            const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
            text.classList.add('mcwws-gis-road-name');
            text.setAttribute('data-anchor-index', String(index));
            text.setAttribute('data-ax', anchor.world.x.toFixed(3));
            text.setAttribute('data-ay', anchor.world.y.toFixed(3));
            text.setAttribute('data-az', anchor.world.z.toFixed(3));
            text.setAttribute('text-anchor', 'middle');
            text.setAttribute('dominant-baseline', 'middle');
            text.textContent = anchor.name;
            text.style.display = 'none';
            group.appendChild(text);
        });
    }

    function syncRoadNameGroupPositions(group, anchors, policy, view, camera, groupDimmed, placedRects) {
        group.classList.toggle('is-dimmed', !!groupDimmed);
        const texts = group.querySelectorAll('.mcwws-gis-road-name');
        anchors.forEach((anchor, index) => {
            const text = texts[index];
            const labelName = anchor?.name || '';
            const dimmed = anchor?.dimmed ?? !!groupDimmed;
            if (!text || !anchor?.world || !labelName) {
                if (text) {
                    text.style.display = 'none';
                }
                return;
            }
            const screen = projectGisPoint(anchor.world, view, camera, false);
            if (!isRoadNameOnScreen(screen)) {
                text.style.display = 'none';
                text.classList.remove('is-dimmed');
                return;
            }
            const roadWidthPx = estimateRoadWidthScreenPx(anchor.world, view, camera);
            const fontSize = fitRoadNameFontSize(labelName, policy.fontSize, roadWidthPx, { primary: true });
            if (!fontSize) {
                text.style.display = 'none';
                text.classList.remove('is-dimmed');
                return;
            }
            const deg = normalizeLabelRotationDeg(anchor.angleRad);
            const rect = estimateRoadNameLabelRect(screen, labelName, fontSize, deg);
            placedRects.push(rect);
            const x = screen.x.toFixed(1);
            const y = screen.y.toFixed(1);
            text.style.display = '';
            text.textContent = labelName;
            text.classList.toggle('is-dimmed', dimmed);
            text.setAttribute('x', x);
            text.setAttribute('y', y);
            text.setAttribute('font-size', String(fontSize));
            text.setAttribute('transform', `rotate(${deg.toFixed(2)}, ${x}, ${y})`);
        });
        for (let i = anchors.length; i < texts.length; i += 1) {
            texts[i].style.display = 'none';
        }
    }

    function renderRoadNameLabelsLayer() {
        const svg = ensureSvgRoadNameLayer();
        if (!svg || !gisInfoEnabled || !gisShowRoadNames) {
            svgLaneNameGroups.forEach((group) => group.remove());
            svgLaneNameGroups.clear();
            return;
        }
        const view = getViewForProjection();
        const camera = getGisBlueMapCamera();
        const viewHeight = getMapCameraHeight();
        const policy = getRoadNameDisplayPolicy(viewHeight);
        const selectionActive = hasGisSelection();
        const neededNameGroupKeys = new Set();
        const placedRects = [];

        if (!policy.show) {
            svgLaneNameGroups.forEach((group) => group.remove());
            svgLaneNameGroups.clear();
            return;
        }

        const flatCandidates = [];
        iterVisibleFeatures().forEach(({ feature }) => {
            if (!shouldShowRoadNameOnFeature(feature)) {
                return;
            }
            if (!isRoadVisibleOnScreenForLabels(feature, view, camera, viewHeight)) {
                return;
            }
            const { anchors } = computeRoadNameAnchors(feature, view, camera, viewHeight);
            if (!anchors.length) {
                return;
            }
            const dimmed = selectionActive && !isFeatureSelected(feature.id);
            anchors.forEach((anchor) => {
                flatCandidates.push({ feature, anchor, dimmed });
            });
        });

        const mergedAnchors = mergeSameNameRoadLabelAnchors(flatCandidates, view, camera);
        const key = '__merged_road_names__';
        neededNameGroupKeys.add(key);
        const group = ensureSvgNameGroup(svg, key, '__merged__');
        const structSig = getMergedRoadNamesStructSig(mergedAnchors, policy, viewHeight, selectionActive);
        if (group.dataset.nameStructSig !== structSig) {
            group.dataset.nameStructSig = structSig;
            rebuildRoadNameGroupStructure(group, mergedAnchors);
        }
        syncRoadNameGroupPositions(group, mergedAnchors, policy, view, camera, false, placedRects);

        svgLaneNameGroups.forEach((group, key) => {
            if (!neededNameGroupKeys.has(key)) {
                group.remove();
                svgLaneNameGroups.delete(key);
            }
        });
    }

    function placeRoadArrowsOnScreenSegment(group, s0, s1, fillColor) {
        const dx = s1.x - s0.x;
        const dy = s1.y - s0.y;
        const len = Math.hypot(dx, dy);
        const spacing = getRoadArrowSpacingPx();
        if (len < spacing * 0.55) {
            return;
        }
        const angle = Math.atan2(dy, dx);
        const count = Math.min(
            GIS_ROAD_ARROW_MAX_PER_SEGMENT,
            Math.max(1, Math.floor(len / spacing))
        );
        for (let k = 1; k <= count; k += 1) {
            const t = k / (count + 1);
            appendRoadArrowMarkerAt(
                group,
                s0.x + dx * t,
                s0.y + dy * t,
                angle,
                fillColor
            );
        }
    }

    function populateRoadArrowGroup(group, feature, view, camera, viewHeight, fillColor, dimmed) {
        const dir = getRoadTravelDirection(feature);
        if (dir === 'both') {
            return;
        }
        const points = coordsToPoints(feature.coordinates);
        const chains = buildVisiblePointChains(points, feature, viewHeight);
        if (!chains.length) {
            return;
        }
        group.classList.toggle('is-dimmed', dimmed);
        chains.forEach((chain) => {
            for (let i = 0; i < chain.length - 1; i += 1) {
                const from = dir === 'dir1' ? chain[i] : chain[i + 1];
                const to = dir === 'dir1' ? chain[i + 1] : chain[i];
                iterClippedLineScreenSegments([from, to], view, camera, (s0, s1) => {
                    placeRoadArrowsOnScreenSegment(group, s0, s1, fillColor);
                });
            }
        });
    }

    function renderRoadArrowsLayer() {
        const svg = ensureSvgLayer();
        if (!svg || !gisInfoEnabled) {
            svgLaneArrowGroups.forEach((group) => group.remove());
            svgLaneArrowGroups.clear();
            return;
        }
        const view = getViewForProjection();
        const camera = getGisBlueMapCamera();
        const viewHeight = getMapCameraHeight();
        const viewSig = getGisViewArrowSignature(view, camera);
        const selectionActive = hasGisSelection();
        const neededArrowGroupKeys = new Set();

        iterVisibleFeatures().forEach(({ feature, layer }) => {
            if (feature.type !== 'LineString') {
                return;
            }
            const dir = getRoadTravelDirection(feature);
            if (dir === 'both') {
                return;
            }
            const points = coordsToPoints(feature.coordinates);
            if (!buildVisiblePointChains(points, feature, viewHeight).length) {
                return;
            }
            const color = featureColor(feature, layer);
            const dimmed = selectionActive && !isFeatureSelected(feature.id);
            const key = `${feature.id}:arrows`;
            neededArrowGroupKeys.add(key);
            const group = ensureSvgArrowGroup(svg, key, feature.id);
            const sig = `${viewSig}|${dir}|${dimmed ? 1 : 0}|${color}|${Math.round(viewHeight)}`;
            if (group.dataset.arrowSig === sig) {
                group.classList.toggle('is-dimmed', dimmed);
                return;
            }
            group.dataset.arrowSig = sig;
            clearSvgGroupChildren(group);
            populateRoadArrowGroup(group, feature, view, camera, viewHeight, color, dimmed);
        });

        svgLaneArrowGroups.forEach((group, key) => {
            if (!neededArrowGroupKeys.has(key)) {
                group.remove();
                svgLaneArrowGroups.delete(key);
            }
        });
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

    function buildSvgPolylinePathWithVisibility(points, view, camera, feature, viewHeight) {
        if (!points || points.length < 2) {
            return '';
        }
        const visibleChains = buildVisiblePointChains(points, feature, viewHeight);
        if (!visibleChains.length) {
            return '';
        }
        const chains = [];
        visibleChains.forEach((chain) => {
            iterClippedLineScreenSegments(chain, view, camera, (s0, s1) => {
                appendClippedSegment(chains, [s0, s1]);
            });
        });
        return chainsToSvgPath(chains);
    }

    /** 封闭多边形：整体 Sutherland–Hodgman，保持顶点顺序，避免拼出虚假三角形 */
    function buildSvgPolygonPath(points, view, camera) {
        return screenRingToSvgPath(getClippedScreenRingForPolygon(points, view, camera));
    }

    function buildSvgPolygonPathWithVisibility(points, view, camera, feature, viewHeight) {
        if (!points || points.length < 3) {
            return '';
        }
        const allVisible = points.every((_, i) => isVertexVisibleAtHeight(feature, i, viewHeight));
        if (allVisible) {
            return buildSvgPolygonPath(points, view, camera);
        }
        const chains = [];
        for (let i = 0; i < points.length; i += 1) {
            const j = (i + 1) % points.length;
            if (!isSegmentVisibleAtHeight(feature, i, j, viewHeight)) {
                continue;
            }
            iterClippedLineScreenSegments([points[i], points[j]], view, camera, (s0, s1) => {
                appendClippedSegment(chains, [s0, s1]);
            });
        }
        return chainsToSvgPath(chains);
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
        const viewHeight = getMapCameraHeight();

        const neededPathKeys = new Set();
        const selectionActive = hasGisSelection();
        const showVolume3dSolids = shouldShowVolume3dSolids();
        syncVolume3dVisibilityClass();
        const volumeFacePaintQueue = [];
        const volumeWireframeQueue = [];
        const volumeMeshEntries = [];
        iterVisibleFeatures().forEach(({ feature, layer }) => {
            const color = featureColor(feature, layer);
            const dash = feature.type === 'LineString' ? getRoadStrokeDasharray(feature) : null;
            const w = feature.type === 'LineString' ? getRoadStrokeWidth(feature) : null;
            const dimmed = selectionActive && !isFeatureSelected(feature.id);
            const points = coordsToPoints(feature.coordinates);

            if (feature.type === 'LineString' && points.length >= 2) {
                const d = buildSvgPolylinePathWithVisibility(points, view, camera, feature, viewHeight);
                if (d) {
                    const key = `${feature.id}:line`;
                    neededPathKeys.add(key);
                    const path = ensureSvgFeaturePath(svg, key, feature.id, 'mcwws-gis-line');
                    path.setAttribute('d', d);
                    path.setAttribute('stroke', color);
                    if (w != null) path.setAttribute('stroke-width', String(w));
                    else path.removeAttribute('stroke-width');
                    if (dash) path.setAttribute('stroke-dasharray', dash);
                    else path.removeAttribute('stroke-dasharray');
                    path.classList.toggle('is-dimmed', dimmed);

                    const hitKey = `${key}:hit`;
                    neededPathKeys.add(hitKey);
                    const hit = ensureSvgHitPath(svg, hitKey, feature.id);
                    hit.setAttribute('d', d);
                    hit.setAttribute('stroke-width', String(Math.max(12, (w != null ? w : 3) + 10)));
                    hit.classList.toggle('is-dimmed', dimmed);
                }
            }
            if (feature.type === 'Polygon' && points.length >= 3) {
                const volCfg = getVolume3dConfig(feature);
                const hasVolumeSolid = volCfg && volCfg.shape !== VOLUME_SHAPES.FLAT;
                const regionSelected = isFeatureSelected(feature.id) && gisEditMode;
                const fillColor = getRegionVolumeFillColor(feature);
                const edgeStroke = regionSelected ? GIS_VOLUME_SELECTION_STROKE : 'rgba(90, 110, 130, 0.35)';

                if (hasVolumeSolid) {
                    if (showVolume3dSolids) {
                        volumeMeshEntries.push({ feature, layer, dimmed });
                        if (regionSelected && shouldShowVolume3dWireframes()) {
                            const volEdges = buildVolume3dEdges(feature);
                            if (volEdges.length) {
                                const volD = buildSvgVolumeWireframePath(volEdges, view, camera);
                                if (volD) {
                                    volumeWireframeQueue.push({
                                        featureId: feature.id,
                                        volKey: `${feature.id}:volume3d`,
                                        volD,
                                        edgeStroke,
                                        dimmed
                                    });
                                }
                            }
                        }
                    }
                } else {
                    const allVisible = points.every((_, i) => isVertexVisibleAtHeight(feature, i, viewHeight));
                    const d = buildSvgPolygonPathWithVisibility(points, view, camera, feature, viewHeight);
                    if (d) {
                        const key = `${feature.id}:polygon`;
                        neededPathKeys.add(key);
                        const path = ensureSvgFeaturePath(svg, key, feature.id, 'mcwws-gis-polygon');
                        path.setAttribute('d', d);
                        path.setAttribute('stroke', color);
                        if (allVisible) {
                            path.setAttribute('fill', color);
                        } else {
                            path.removeAttribute('fill');
                        }
                        path.classList.toggle('is-dimmed', dimmed);
                    }
                }
            }
        });

        let volumesRenderedWithWebGl = false;
        try {
            if (showVolume3dSolids && volumeMeshEntries.length && shouldRenderVolumesWithWebGl()) {
                syncVolumeMeshes(volumeMeshEntries);
            } else if (volumeFeatureMeshes.size || volumeMeshRoot) {
                clearVolumeMeshes();
            }
            volumesRenderedWithWebGl = volumeFeatureMeshes.size > 0;
            syncVolume3dRenderModeClass(volumesRenderedWithWebGl);
            if (volumesRenderedWithWebGl) {
                purgeVolumeFillSvgPathsForFeatures(new Set(volumeFeatureMeshes.keys()));
            }
            const svgVolumeEntries = volumeMeshEntries.filter(
                ({ feature }) => !volumeFeatureMeshes.has(feature.id)
            );
            if (svgVolumeEntries.length) {
                queueVolumeSolidSvgFills(svgVolumeEntries, view, camera, volumeFacePaintQueue);
            }
            if (volumeFacePaintQueue.length) {
                sortVolumeFacePaintQueue(volumeFacePaintQueue, camera);
                paintVolumeFacePaintQueue(svg, volumeFacePaintQueue, neededPathKeys);
            }
        } catch (err) {
            console.warn('[mcwws-gis] volume render failed, falling back to SVG', err);
            clearVolumeMeshes();
            syncVolume3dRenderModeClass(false);
            volumeFacePaintQueue.length = 0;
            if (showVolume3dSolids && volumeMeshEntries.length) {
                queueVolumeSolidSvgFills(volumeMeshEntries, view, camera, volumeFacePaintQueue);
                if (volumeFacePaintQueue.length) {
                    sortVolumeFacePaintQueue(volumeFacePaintQueue, camera);
                    paintVolumeFacePaintQueue(svg, volumeFacePaintQueue, neededPathKeys);
                }
            }
        }
        if (showVolume3dSolids && !volumeMeshEntries.length && (volumeFeatureMeshes.size || volumeMeshRoot)) {
            clearVolumeMeshes();
        }

        volumeWireframeQueue.forEach(({ featureId, volKey, volD, edgeStroke, dimmed }) => {
            neededPathKeys.add(volKey);
            const volPath = ensureSvgFeaturePath(svg, volKey, featureId, 'mcwws-gis-volume3d');
            volPath.setAttribute('d', volD);
            volPath.setAttribute('stroke', edgeStroke);
            volPath.setAttribute('stroke-width', '2.5');
            volPath.classList.toggle('is-dimmed', dimmed);
            volPath.classList.add('is-selected');
        });

        svgPathElements.forEach((path, key) => {
            if (!neededPathKeys.has(key)) {
                path.remove();
                svgPathElements.delete(key);
            }
        });

        renderRoadArrowsLayer();
        renderRoadNameLabelsLayer();

        if (draftPoints.length && (activeTool === 'line' || activeTool === 'polygon')) {
            const d = buildDraftPreviewPath(view, camera);
            if (activeTool === 'polygon' && activeVolumeShape === VOLUME_SHAPES.BOX && shouldShowVolume3dSolids()) {
                const draft = getDraftPreviewPoints();
                if (draft.length >= 3) {
                    const ys = inferFootprintYs(draft);
                    const draftFaces = buildVolumeFaceRenderItems(
                        VOLUME_SHAPES.BOX,
                        draft,
                        { shape: VOLUME_SHAPES.BOX, minY: ys.minY, maxY: ys.maxY },
                        view,
                        camera,
                        GIS_DEFAULT_REGION_COLOR
                    );
                    if (draftFaces.length) {
                        if (!svgDraftFillPathEl) {
                            svgDraftFillPathEl = document.createElementNS('http://www.w3.org/2000/svg', 'g');
                            svgDraftFillPathEl.classList.add('mcwws-gis-draft-fill-group');
                            svg.insertBefore(svgDraftFillPathEl, svg.firstChild);
                        }
                        svgDraftFillPathEl.replaceChildren();
                        draftFaces.forEach((item) => {
                            const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                            path.setAttribute('d', item.d);
                            path.style.fill = item.fill;
                            path.classList.add('mcwws-gis-draft-fill');
                            svgDraftFillPathEl.appendChild(path);
                        });
                    } else if (svgDraftFillPathEl) {
                        svgDraftFillPathEl.replaceChildren();
                    }
                } else if (svgDraftFillPathEl) {
                    svgDraftFillPathEl.remove();
                    svgDraftFillPathEl = null;
                }
            } else if (svgDraftFillPathEl) {
                svgDraftFillPathEl.remove();
                svgDraftFillPathEl = null;
            }
            if (d) {
                if (!svgDraftPathEl) {
                    svgDraftPathEl = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                    svgDraftPathEl.classList.add('mcwws-gis-draft');
                    svg.appendChild(svgDraftPathEl);
                }
                svgDraftPathEl.classList.toggle(
                    'mcwws-gis-draft--volume3d',
                    activeTool === 'polygon' && shouldShowVolume3dWireframes()
                );
                svgDraftPathEl.setAttribute('d', d);
            } else if (svgDraftPathEl) {
                svgDraftPathEl.removeAttribute('d');
            }
        } else {
            if (svgDraftPathEl) {
                svgDraftPathEl.remove();
                svgDraftPathEl = null;
            }
            if (svgDraftFillPathEl) {
                svgDraftFillPathEl.remove();
                svgDraftFillPathEl = null;
            }
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

        if (isGisSelectMode() && isGisSegmentInsertModifierHeld(null)) {
            refreshSegmentInsertHoverAtLastPointer();
        }
        renderVertexHandles(view, camera);
        document.body.classList.toggle('mcwws-gis-vertex-edit', shouldShowVertexHandles());
        document.body.classList.toggle(
            'mcwws-gis-ctrl-segment-insert',
            isGisSegmentInsertModifierHeld(null) && shouldShowVertexHandles()
        );
        syncMapBackgroundOpacity(selectionActive);
    }

    function syncDrawingClass() {
        const drawing = isGisDrawPointerActive();
        document.body.classList.toggle('mcwws-gis-drawing', drawing);
        const selectMode = isGisSelectMode();
        document.body.classList.toggle('mcwws-gis-select-mode', selectMode);
        document.body.classList.toggle(
            'mcwws-gis-ignore-height-clip',
            gisEditMode && gisIgnoreHeightClip
        );
        if (!selectMode) {
            clearGisSelectHover();
            cancelGisLasso();
            clearGisHoverSegmentInsert();
            document.body.classList.remove('mcwws-gis-ctrl-segment-insert');
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
        syncVolume3dVisibilityClass();
        renderOverlay();
    }

    function openGisEditorPanel() {
        gisEditorOpen = true;
        if (gisCanEdit) {
            gisEditMode = true;
            syncDrawingClass();
            syncVolume3dVisibilityClass();
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
        const clipboardSourceIds = getFeatureIdsForClipboard();
        const editHint = gisCanEdit
            ? (activeTool === 'select'
                ? '左键点选；按住 Ctrl 移近线段显示绿点，Ctrl+点击或点绿点添加顶点；选中端点 Ctrl+拖坐标轴延伸线段；中键套索'
                : activeTool === 'polygon'
                    ? `3D 区域（${VOLUME_SHAPE_OPTIONS.find((o) => o.id === activeVolumeShape)?.label || ''}）：${getVolumeDraftStatusText()}`
                    : '2D 俯视下点击地图绘制；道路/区域双击结束')
            : '管理员登录后可编辑地理信息';

        return `
            <div class="mcwws-layer-editor">
                <p class="mcwws-gis-menu-hint">${escapeHtml(editHint)}</p>
                <label class="mcwws-gis-edit-option">
                    <input type="checkbox" data-gis-ignore-height-clip ${gisIgnoreHeightClip ? 'checked' : ''}
                        ${!gisCanEdit ? 'disabled' : ''}>
                    <span>忽视高度裁切（编辑时显示全部顶点与线段）</span>
                </label>
                ${isSimplifiedMapMode() ? `
                <p class="mcwws-gis-edit-option mcwws-gis-edit-option--hint">简化地图模式下始终显示三维建筑物</p>
                ` : `
                <label class="mcwws-gis-edit-option">
                    <input type="checkbox" data-gis-show-volume3d ${gisShowVolume3dBuildings ? 'checked' : ''}
                        ${!gisCanEdit ? 'disabled' : ''}>
                    <span>显示三维建筑物</span>
                </label>
                `}
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
                ${activeTool === 'polygon' && gisCanEdit ? `
                <div class="mcwws-gis-volume-shapes" role="toolbar" aria-label="区域 3D 形状">
                    <span class="mcwws-gis-volume-shapes-label">3D 形状</span>
                    ${VOLUME_SHAPE_OPTIONS.map((opt) => `
                        <button type="button" class="mcwws-gis-volume-shape-btn${activeVolumeShape === opt.id ? ' is-active' : ''}"
                            data-volume-shape="${opt.id}" title="${escapeHtml(opt.label)}">${escapeHtml(opt.label)}</button>
                    `).join('')}
                </div>
                ` : ''}
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
                    <button type="button" class="mcwws-gis-menu-action" data-action="copy" title="Ctrl+C"
                        ${clipboardSourceIds.length === 0 ? 'disabled' : ''}>复制</button>
                    <button type="button" class="mcwws-gis-menu-action" data-action="cut" title="Ctrl+X"
                        ${clipboardSourceIds.length === 0 ? 'disabled' : ''}>剪切</button>
                    <button type="button" class="mcwws-gis-menu-action" data-action="paste" title="Ctrl+V"
                        ${!hasGisClipboard() ? 'disabled' : ''}>粘贴</button>
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
                ${renderSelectionTypeMismatchHintHtml()}
                ${renderRoadPropertiesPanelHtml()}
                ${renderPolygonVolumePanelHtml()}
                ${renderPinPropertiesPanelHtml()}
                ${renderVertexAlignPanelHtml()}
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
        document.body.classList.toggle('mcwws-gis-layer-dialog-open', layerDialogOpen);

        const scrollTop = dialog.scrollTop;
        const activeEl = document.activeElement;
        const focusRestore = activeEl?.closest?.('.mcwws-layer-dialog')
            && activeEl.matches?.('[data-vertex-vis], [data-road-prop], [data-road-name-seg], [data-volume-prop], [data-pin-prop]')
            ? {
                selector: activeEl.matches('[data-vertex-vis]')
                    ? `[data-vertex-vis="${activeEl.getAttribute('data-vertex-vis')}"]`
                    : activeEl.matches('[data-road-name-seg]')
                        ? `[data-road-name-seg="${activeEl.getAttribute('data-road-name-seg')}"]`
                        : activeEl.matches('[data-volume-prop]')
                            ? `[data-volume-prop="${activeEl.getAttribute('data-volume-prop')}"]`
                            : activeEl.matches('[data-pin-prop]')
                                ? `[data-pin-prop="${activeEl.getAttribute('data-pin-prop')}"]`
                                : `[data-road-prop="${activeEl.getAttribute('data-road-prop')}"]`,
                start: activeEl.selectionStart,
                end: activeEl.selectionEnd
            }
            : null;

        dialog.innerHTML = `
            <p class="mcwws-layer-dialog-title">图层 <span class="mcwws-gis-build-tag">v${MCWWS_GIS_BUILD}</span></p>
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
            <label class="mcwws-layer-gis-toggle mcwws-layer-gis-toggle--sub">
                <input type="checkbox" data-gis-show-road-names ${gisShowRoadNames ? 'checked' : ''}
                    ${!gisInfoEnabled ? 'disabled' : ''}>
                <span>沿路自动显示路名（每段可见路名各 1 个，超远缩放自动隐藏）</span>
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
        dialog.scrollTop = scrollTop;
        applyCheckboxIndeterminateStates(dialog);
        if (focusRestore) {
            const input = dialog.querySelector(focusRestore.selector);
            if (input && typeof input.focus === 'function') {
                input.focus();
                if (typeof focusRestore.start === 'number' && typeof input.setSelectionRange === 'function') {
                    try {
                        input.setSelectionRange(focusRestore.start, focusRestore.end);
                    } catch {
                        /* ignore */
                    }
                }
            }
        }
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
            const heightClipInput = e.target.closest('[data-gis-ignore-height-clip]');
            if (heightClipInput && e.target.matches('input[type="checkbox"]')) {
                e.stopPropagation();
                setGisIgnoreHeightClip(e.target.checked);
                return;
            }
            const roadNamesInput = e.target.closest('[data-gis-show-road-names]');
            if (roadNamesInput && e.target.matches('input[type="checkbox"]')) {
                e.stopPropagation();
                setGisShowRoadNames(e.target.checked);
                return;
            }
            const volume3dInput = e.target.closest('[data-gis-show-volume3d]');
            if (volume3dInput && e.target.matches('input[type="checkbox"]')) {
                e.stopPropagation();
                setGisShowVolume3dBuildings(e.target.checked);
                return;
            }
            const roadInput = e.target.closest('[data-road-prop]');
            if (roadInput) {
                e.stopPropagation();
                applyRoadPropertyInput(roadInput);
                return;
            }
            const volumePropChange = e.target.closest('[data-volume-prop]');
            if (volumePropChange) {
                e.stopPropagation();
                applyVolumePropertyInput(volumePropChange);
                return;
            }
            const pinPropChange = e.target.closest('[data-pin-prop]');
            if (pinPropChange) {
                e.stopPropagation();
                applyPinPropertyInput(pinPropChange);
                return;
            }
            const roadNameSegInput = e.target.closest('[data-road-name-seg]');
            if (roadNameSegInput) {
                e.stopPropagation();
                applyRoadNameSegmentInput(roadNameSegInput);
                return;
            }
            const vtxVisInput = e.target.closest('[data-vertex-vis]');
            if (vtxVisInput) {
                e.stopPropagation();
                applyVertexVisibilityInput(vtxVisInput);
            }
        });

        wrap.addEventListener('input', (e) => {
            const vtxVisInput = e.target.closest('[data-vertex-vis]');
            if (vtxVisInput) {
                e.stopPropagation();
                if (syncVertexVisibilityFromInput(vtxVisInput)) {
                    markDirtySoft();
                    renderOverlay();
                    refreshVertexVisibilityHintOnly();
                }
                return;
            }
            const roadNameInput = e.target.closest('[data-road-prop="name"]');
            if (roadNameInput) {
                e.stopPropagation();
                if (syncRoadPropertyFromInput(roadNameInput)) {
                    markDirtySoft();
                    invalidateRoadLabelCache();
                    renderRoadNameLabelsLayer();
                }
                return;
            }
            const roadNameSegInput = e.target.closest('[data-road-name-seg]');
            if (roadNameSegInput) {
                e.stopPropagation();
                if (syncRoadNameSegmentFromInput(roadNameSegInput)) {
                    markDirtySoft();
                    invalidateRoadLabelCache();
                    renderRoadNameLabelsLayer();
                }
                return;
            }
            const volumePropInputLive = e.target.closest('[data-volume-prop]');
            if (volumePropInputLive) {
                e.stopPropagation();
                if (syncVolumePropertyFromInput(volumePropInputLive)) {
                    markDirtySoft();
                    renderOverlay();
                }
                return;
            }
            const pinPropInputLive = e.target.closest('[data-pin-prop]');
            if (pinPropInputLive) {
                e.stopPropagation();
                if (syncPinPropertyFromInput(pinPropInputLive)) {
                    markDirtySoft();
                    renderOverlay();
                }
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
            const heightClipToggle = e.target.closest('[data-gis-ignore-height-clip]');
            if (heightClipToggle && e.target.matches('input[type="checkbox"]')) {
                setGisIgnoreHeightClip(e.target.checked);
                return;
            }
            const roadNamesToggle = e.target.closest('[data-gis-show-road-names]');
            if (roadNamesToggle && e.target.matches('input[type="checkbox"]')) {
                setGisShowRoadNames(e.target.checked);
                return;
            }
            const volume3dToggle = e.target.closest('[data-gis-show-volume3d]');
            if (volume3dToggle && e.target.matches('input[type="checkbox"]')) {
                setGisShowVolume3dBuildings(e.target.checked);
                return;
            }
            const toolBtn = e.target.closest('[data-tool]');
            if (toolBtn) {
                activeTool = toolBtn.getAttribute('data-tool') || 'select';
                cancelGisLasso();
                clearGisSelection();
                draftPoints = [];
                draftHover = null;
                resetVolumeDraftState();
                syncDrawingClass();
                renderLayerDialog();
                renderOverlay();
                return;
            }
            const volumeShapeBtn = e.target.closest('[data-volume-shape]');
            if (volumeShapeBtn) {
                const nextShape = normalizeVolumeShape(volumeShapeBtn.getAttribute('data-volume-shape'));
                if (!isCreatableVolumeShape(nextShape)) {
                    return;
                }
                activeVolumeShape = nextShape;
                draftPoints = [];
                draftHover = null;
                resetVolumeDraftState();
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
            } else if (action === 'copy') {
                copySelectedFeaturesToClipboard();
            } else if (action === 'cut') {
                cutSelectedFeatures();
            } else if (action === 'paste') {
                pasteClipboardFeatures();
            } else if (action === 'delete') {
                deleteSelectedFeature();
            } else if (action === 'clear-vertex-vis') {
                clearBatchVertexVisibility();
            } else if (action === 'split-road-name-at-vertex') {
                splitRoadNameAtSelectedVertex();
            } else if (action === 'align-vertices') {
                alignSelectedVertices(actionBtn.getAttribute('data-align-axis'));
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
        const modKey = String(event.key || '').toLowerCase();
        if ((event.ctrlKey || event.metaKey) && (modKey === 'c' || modKey === 'x' || modKey === 'v')) {
            event.preventDefault();
            event.stopPropagation();
            if (modKey === 'c') {
                copySelectedFeaturesToClipboard();
            } else if (modKey === 'x') {
                cutSelectedFeatures();
            } else {
                pasteClipboardFeatures();
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
            if (modKey === 'c') {
                event.preventDefault();
                copySelectedFeaturesToClipboard();
                return;
            }
            if (modKey === 'x') {
                event.preventDefault();
                cutSelectedFeatures();
                return;
            }
            if (modKey === 'v') {
                event.preventDefault();
                pasteClipboardFeatures();
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
        if (typeof window.mcwwsIsTextInputFocused === 'function') {
            return window.mcwwsIsTextInputFocused();
        }
        const active = document.activeElement;
        if (!active) {
            return false;
        }
        const tag = active.tagName;
        return tag === 'INPUT' || tag === 'TEXTAREA' || active.isContentEditable;
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
        document.body.classList.toggle('mcwws-gis-road-names-off', !gisShowRoadNames);
        syncVolume3dVisibilityClass();
        initMapAuth();
        bindMapPicks();
        bindMapContextMenuSuppression();
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
        document.addEventListener('keydown', onGisSegmentInsertModifierKey, true);
        document.addEventListener('keyup', onGisSegmentInsertModifierKey, true);
        window.addEventListener('blur', () => {
            gisSegmentInsertModifierHeld = false;
            document.body.classList.remove('mcwws-gis-ctrl-segment-insert');
            clearGisHoverSegmentInsert();
        });
        document.addEventListener('pointerleave', clearGisSelectHover);
        document.addEventListener('dblclick', onDblClick, true);
        publishGisVolumeDiag();
        window.addEventListener('hashchange', () => {
            gisCachedCamera = null;
            gisThree = null;
            invalidateRoadArrowCache();
            invalidateRoadLabelCache();
            renderOverlay();
        });
        window.addEventListener('resize', () => {
            invalidateRoadArrowCache();
            invalidateRoadLabelCache();
            renderOverlay();
        });
        animationId = requestAnimationFrame(tick);
    }

    function publishGisVolumeDiag() {
        window.mcwwsGisVolumeDiag = getGisVolumeRenderDiag;
        try {
            if (window.parent && window.parent !== window) {
                window.parent.mcwwsGisVolumeDiag = () => getGisVolumeRenderDiag();
                window.parent.postMessage({
                    type: 'mcwws-gis-ready',
                    build: MCWWS_GIS_BUILD
                }, '*');
            }
        } catch {
            /* cross-origin parent */
        }
    }

    publishGisVolumeDiag();

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start, { once: true });
    } else {
        start();
    }
})();
