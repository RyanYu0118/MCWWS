(function () {
    const API_PORT = 8002;
    const NODE_API = `${window.location.protocol}//${window.location.hostname}:${API_PORT}`;
    const GIS_WRAP_ID = 'mcwws-gis-wrap';
    const MAP_CONTROLS_STACK_SEL = '.mcwws-map-controls-stack';
    const SVG_LAYER_ID = 'mcwws-gis-svg-layer';
    const PIN_LAYER_ID = 'mcwws-gis-pin-layer';
    const GIS_DISPLAY_Y = 64;

    let mapAuthToken = null;
    let mapAuthUser = null;
    let gisCanEdit = false;
    let gisEditMode = false;
    let activeTool = 'select';
    let activeLayerId = 'roads';
    let project = null;
    let selectedFeatureId = null;
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
    let lastPickAt = 0;
    let lastPickKey = '';
    let gisCachedCamera = null;

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

    function syncMapRenderModeVisual() {
        document.body.classList.toggle('mcwws-map-simplified-mode', isSimplifiedMapMode());
        const mapContainer = document.getElementById('map-container');
        if (mapContainer) {
            mapContainer.style.display = '';
        }
        gisCachedCamera = null;
        renderOverlay();
    }

    function applyMapRenderMode(mode, persist = true) {
        mapRenderMode = mode === 'simplified' ? 'simplified' : 'original';
        if (mapRenderMode === 'original') {
            restoreOriginalMapRendering();
        } else {
            setGisInfoEnabled(true, false);
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
        if (!gisInfoEnabled) {
            gisEditorOpen = false;
            if (gisEditMode) {
                gisEditMode = false;
                draftPoints = [];
                draftHover = null;
                syncDrawingClass();
            }
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

    function projectGisWithCamera(point, camera, labelStyle) {
        const worldPoint = {
            x: point.x,
            y: GIS_DISPLAY_Y + (labelStyle ? 1.2 : 0),
            z: point.z,
            w: 1
        };
        const cameraPoint = applyMatrix4(worldPoint, camera.matrixWorldInverse);
        const clipPoint = applyMatrix4(cameraPoint, camera.projectionMatrix);
        if (!clipPoint.w) return null;
        const nx = clipPoint.x / clipPoint.w;
        const ny = clipPoint.y / clipPoint.w;
        const nz = clipPoint.z / clipPoint.w;
        return {
            x: (nx * 0.5 + 0.5) * window.innerWidth,
            y: (-ny * 0.5 + 0.5) * window.innerHeight,
            behind: clipPoint.w < 0 || nz < -1 || nz > 1
        };
    }

    function projectGisMarker(point, view, labelStyle) {
        const wy = GIS_DISPLAY_Y + (labelStyle ? 1.2 : 0);
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
        return {
            x: window.innerWidth / 2 + right * scale,
            y: window.innerHeight / 2 + forward * scale * pitchFactor - dy * scale * 0.65,
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

    function screenToWorld(screenX, screenY, view) {
        const v = view || getViewForProjection();
        if (!v) {
            return null;
        }
        const perspectiveBoost = v.mode === 'perspective' ? 1.35 : 1;
        const scale = Math.max(2, Math.min(120, (window.innerHeight / Math.max(10, v.height * 2.2)) * perspectiveBoost));
        const pitchFactor = Math.max(0.2, Math.min(1, Math.abs(Math.sin(v.pitch || -0.8))));
        const right = (screenX - window.innerWidth / 2) / scale;
        const forward = (screenY - window.innerHeight / 2) / (scale * pitchFactor);
        const yaw = v.rotation ?? v.yaw ?? 0;
        const cos = Math.cos(yaw);
        const sin = Math.sin(yaw);
        const dx = right * cos + forward * sin;
        const dz = -right * sin + forward * cos;
        return {
            x: v.x + dx,
            y: v.y,
            z: v.z + dz
        };
    }

    function snapPoint(raw) {
        if (!raw) return null;
        return {
            x: Math.floor(raw.x) + 0.5,
            y: GIS_DISPLAY_Y,
            z: Math.floor(raw.z) + 0.5
        };
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

    function featureColor(feature, layer) {
        const c = feature.properties?.color || layer?.color;
        return c && /^#[0-9a-fA-F]{3,8}$/i.test(c) ? c : '#3b82f6';
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
            if (gisEditMode) {
                gisEditMode = false;
                draftPoints = [];
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
        if (!gisCanEdit && gisEditMode) {
            gisEditMode = false;
            draftPoints = [];
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
            setStatus('已保存到服务器', 'ok');
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

    function promptFeatureMeta(defaultName) {
        const name = window.prompt('名称（可留空）', defaultName || '');
        if (name === null) return null;
        const description = window.prompt('说明（可留空）', '') ?? '';
        return { name: String(name).trim(), description: String(description).trim() };
    }

    function addFeature(feature) {
        const layer = getActiveLayer();
        if (!layer) return;
        if (!Array.isArray(layer.features)) layer.features = [];
        layer.features.push(feature);
        selectedFeatureId = feature.id;
        markDirty();
        renderOverlay();
    }

    function deleteSelectedFeature() {
        if (!selectedFeatureId || !project?.layers) return;
        project.layers.forEach((layer) => {
            layer.features = (layer.features || []).filter((f) => f.id !== selectedFeatureId);
        });
        selectedFeatureId = null;
        markDirty();
        renderOverlay();
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
        if (activeTool === 'select') {
            return;
        }

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
        const view = getViewForProjection();
        if (!view) return null;
        return snapPoint(screenToWorld(clientX, clientY, view));
    }

    function onCanvasClick(event) {
        if (!gisInfoEnabled || !gisEditMode || !gisCanEdit) return;
        if (activeTool === 'select') return;
        const target = event.target;
        if (target?.closest?.('.mcwws-ctrl-gis-wrap, .mcwws-layer-dialog')) return;
        if (!target?.closest?.('canvas')) return;
        const point = pickWorldFromScreen(event.clientX, event.clientY);
        if (!point) return;
        event.preventDefault();
        event.stopPropagation();
        handleMapPick(point);
    }

    function onCanvasMove(event) {
        if (!gisEditMode || draftPoints.length === 0) return;
        if (activeTool !== 'line' && activeTool !== 'polygon') return;
        draftHover = pickWorldFromScreen(event.clientX, event.clientY);
        renderOverlay();
    }

    function onMapInteraction(event) {
        if (!gisInfoEnabled || !gisEditMode || !gisCanEdit) return;
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
            document.addEventListener('click', onCanvasClick, true);
            document.addEventListener('mousemove', onCanvasMove, true);
        }
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

    function buildSvgPath(points, closed, view, camera) {
        const v = view || getViewForProjection();
        const screen = points
            .map((p) => projectGisPoint(p, v, camera, false))
            .filter((p) => p && !p.behind);
        if (screen.length < 2) return '';
        return screen.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ')
            + (closed ? ' Z' : '');
    }

    function renderOverlay() {
        const svg = ensureSvgLayer();
        const pinLayer = ensurePinLayer();
        if (!svg || !pinLayer) return;

        if (!gisInfoEnabled) {
            svg.innerHTML = '';
            pinElements.forEach((pin) => pin.remove());
            pinElements.clear();
            return;
        }

        const view = getViewForProjection();
        const camera = getGisBlueMapCamera();

        const fragments = [];
        iterVisibleFeatures().forEach(({ feature, layer }) => {
            const color = featureColor(feature, layer);
            const selected = feature.id === selectedFeatureId;
            const width = selected ? 4 : 3;
            const points = coordsToPoints(feature.coordinates);

            if (feature.type === 'LineString' && points.length >= 2) {
                const d = buildSvgPath(points, false, view, camera);
                if (d) {
                    fragments.push(
                        `<path data-fid="${escapeHtml(feature.id)}" d="${d}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="round" stroke-linejoin="round" opacity="0.9"/>`
                    );
                }
            }
            if (feature.type === 'Polygon' && points.length >= 3) {
                const d = buildSvgPath(points, true, view, camera);
                if (d) {
                    fragments.push(
                        `<path data-fid="${escapeHtml(feature.id)}" d="${d}" fill="${color}" fill-opacity="0.22" stroke="${color}" stroke-width="${width}"/>`
                    );
                }
            }
        });

        if (draftPoints.length && (activeTool === 'line' || activeTool === 'polygon')) {
            const draft = draftPoints.slice();
            if (draftHover) draft.push(draftHover);
            const d = buildSvgPath(draft, activeTool === 'polygon' && draft.length >= 3, view, camera);
            if (d) {
                fragments.push(
                    `<path class="mcwws-gis-draft" d="${d}" fill="none" stroke="#14b8a6" stroke-width="2" stroke-dasharray="6 4"/>`
                );
            }
        }

        svg.innerHTML = fragments.join('');
        svg.querySelectorAll('[data-fid]').forEach((el) => {
            el.addEventListener('click', (e) => {
                e.stopPropagation();
                selectedFeatureId = el.getAttribute('data-fid');
                renderOverlay();
                renderPanel();
            });
        });

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
                pin.addEventListener('click', (e) => {
                    e.stopPropagation();
                    selectedFeatureId = feature.id;
                    renderOverlay();
                    renderPanel();
                });
                pinLayer.appendChild(pin);
                pinElements.set(feature.id, pin);
            }
            const name = feature.properties?.name || (feature.type === 'Label' ? '标注' : '点');
            pin.innerHTML = `
                <span class="mcwws-gis-pin-icon">${feature.type === 'Label' ? '🏷' : '📍'}</span>
                <span class="mcwws-gis-pin-label">${escapeHtml(name)}</span>
            `;
            pin.classList.toggle('is-selected', feature.id === selectedFeatureId);
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
    }

    function syncDrawingClass() {
        const drawing = gisInfoEnabled && gisEditMode && gisCanEdit && activeTool !== 'select';
        document.body.classList.toggle('mcwws-gis-drawing', drawing);
    }

    function mountGisAboveDimension(wrap, column) {
        const layerWrap = column.querySelector('.mcwws-ctrl-layer-wrap');
        if (layerWrap && wrap.nextElementSibling !== layerWrap) {
            column.insertBefore(wrap, layerWrap);
        } else if (!layerWrap && column.firstChild !== wrap) {
            column.insertBefore(wrap, column.firstChild);
        }
    }

    function ensureGisControls() {
        const column = document.querySelector('.mcwws-ctrl-dimension-column');
        if (!column) {
            return null;
        }

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
            mountGisAboveDimension(wrap, column);
            bindGisControlEvents(wrap);
        } else {
            mountGisAboveDimension(wrap, column);
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
        const selected = selectedFeatureId ? findFeatureById(selectedFeatureId) : null;
        const editHint = gisCanEdit
            ? (gisEditMode ? '2D 俯视下点击地图绘制；道路/区域双击结束' : '进入编辑后可在地图上标注')
            : '管理员登录后可编辑地理信息';

        return `
            <div class="mcwws-layer-editor">
                <p class="mcwws-gis-menu-hint">${escapeHtml(editHint)}</p>
                <div class="mcwws-gis-menu-tools" role="toolbar" aria-label="绘制工具">
                    ${TOOLS.map((t) => `
                        <button type="button" class="mcwws-gis-menu-tool${activeTool === t.id ? ' is-active' : ''}"
                            data-tool="${t.id}" title="${escapeHtml(t.label)}"
                            ${!gisEditMode || !gisCanEdit ? 'disabled' : ''}>
                            <span class="mcwws-gis-menu-tool-icon" aria-hidden="true">${t.icon}</span>
                            <span class="mcwws-gis-menu-tool-label">${escapeHtml(t.label)}</span>
                        </button>
                    `).join('')}
                </div>
                <div class="mcwws-gis-menu-actions">
                    <button type="button" class="mcwws-gis-menu-action" data-action="toggle-edit"
                        ${!gisCanEdit ? 'disabled' : ''}>${gisEditMode ? '退出编辑' : '开始编辑'}</button>
                    <button type="button" class="mcwws-gis-menu-action mcwws-gis-menu-action--primary" data-action="save"
                        ${!gisCanEdit || !dirty || saving ? 'disabled' : ''}>${saving ? '保存中…' : '保存'}</button>
                </div>
                <div class="mcwws-gis-menu-actions">
                    <button type="button" class="mcwws-gis-menu-action" data-action="finish-draft"
                        ${!gisEditMode || draftPoints.length === 0 ? 'disabled' : ''}>完成绘制</button>
                    <button type="button" class="mcwws-gis-menu-action" data-action="cancel-draft"
                        ${!gisEditMode || draftPoints.length === 0 ? 'disabled' : ''}>取消</button>
                </div>
                <div class="mcwws-gis-menu-actions">
                    <button type="button" class="mcwws-gis-menu-action" data-action="export">导出 GeoJSON</button>
                    <button type="button" class="mcwws-gis-menu-action mcwws-gis-menu-action--danger" data-action="delete"
                        ${!selected ? 'disabled' : ''}>删除选中</button>
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
                    selected
                        ? `<p class="mcwws-gis-menu-selected">选中：${escapeHtml(selected.feature.properties?.name || selected.feature.id)}</p>`
                        : ''
                }
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
                gisEditorOpen = !gisEditorOpen;
                if (!gisEditorOpen) {
                    gisEditMode = false;
                    draftPoints = [];
                    draftHover = null;
                    syncDrawingClass();
                }
                renderLayerDialog();
                return;
            }
            if (action === 'toggle-edit') {
                if (!gisCanEdit) {
                    requestAuthFromParent();
                    if (window.parent !== window) {
                        window.parent.postMessage({ type: 'mcwws-auth-required' }, '*');
                    }
                    return;
                }
                gisEditMode = !gisEditMode;
                if (!gisEditMode) {
                    draftPoints = [];
                    draftHover = null;
                }
                syncDrawingClass();
                renderLayerDialog();
                renderOverlay();
            } else if (action === 'save') {
                void saveGisProject();
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

    function waitForMapControls(attemptsLeft = 80) {
        const wrap = ensureGisControls();
        if (wrap) {
            renderLayerDialog();
            return;
        }
        if (attemptsLeft <= 0) {
            return;
        }
        requestAnimationFrame(() => waitForMapControls(attemptsLeft - 1));
    }

    function onKeyDown(event) {
        if (event.key === 'Escape') {
            if (gisEditMode && (draftPoints.length || activeTool !== 'select')) {
                cancelDraft();
                return;
            }
            if (gisEditorOpen) {
                gisEditorOpen = false;
                gisEditMode = false;
                draftPoints = [];
                draftHover = null;
                syncDrawingClass();
                renderLayerDialog();
            }
            return;
        }
        if (!gisEditMode) return;
        if (event.key === 'Enter' && draftPoints.length) {
            finishDraft();
        }
        if ((event.key === 'Delete' || event.key === 'Backspace') && selectedFeatureId && !isInputFocused()) {
            deleteSelectedFeature();
        }
    }

    function onDblClick(event) {
        if (!gisInfoEnabled || !gisEditMode || !gisCanEdit) return;
        if (activeTool !== 'line' && activeTool !== 'polygon') return;
        if (!event.target?.closest?.('canvas')) return;
        event.preventDefault();
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

    function start() {
        if (started) return;
        started = true;
        document.getElementById('mcwws-gis-panel')?.remove();
        loadLayerPrefs();
        initMapAuth();
        bindMapPicks();
        waitForMapControls();
        tryApplyStoredMapRenderMode();
        void loadGisProject().then(() => {
            if (gisInfoEnabled) {
                renderOverlay();
            }
        });
        document.addEventListener('keydown', onKeyDown);
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
