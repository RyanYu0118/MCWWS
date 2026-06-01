(function () {
    const API_PORT = 8002;
    const NODE_API = `${window.location.protocol}//${window.location.hostname}:${API_PORT}`;
    const PANEL_ID = 'mcwws-gis-panel';
    const SVG_LAYER_ID = 'mcwws-gis-svg-layer';
    const PIN_LAYER_ID = 'mcwws-gis-pin-layer';

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
    let panelCollapsed = false;
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

    function getViewForProjection() {
        const cm = getControlsManager();
        const hash = parseHash();
        if (!cm) return hash;
        const dist = Number(cm.distance) || hash?.distance || 128;
        return {
            map: getCurrentMapId(),
            x: cm.position.x,
            y: cm.position.y,
            z: cm.position.z,
            distance: dist,
            height: dist,
            rotation: cm.rotation ?? 0,
            yaw: cm.rotation ?? 0,
            angle: cm.angle ?? 0,
            pitch: cm.angle ?? 0,
            mode: getMapViewState()
        };
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

    function getBlueMapCamera() {
        return findCamera(getBlueMapApp());
    }

    function projectWorldPoint(point, camera, view) {
        if (camera) {
            const worldPoint = { x: point.x, y: point.y + 0.8, z: point.z, w: 1 };
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
        if (!view) return null;
        const dx = point.x - view.x;
        const dy = point.y - view.y;
        const dz = point.z - view.z;
        const yaw = view.rotation ?? view.yaw ?? 0;
        const cos = Math.cos(yaw);
        const sin = Math.sin(yaw);
        const right = dx * cos - dz * sin;
        const forward = dx * sin + dz * cos;
        const perspectiveBoost = view.mode === 'perspective' ? 1.35 : 1;
        const scale = Math.max(2, Math.min(120, (window.innerHeight / Math.max(10, view.height * 2.2)) * perspectiveBoost));
        const pitchFactor = Math.max(0.2, Math.min(1, Math.abs(Math.sin(view.pitch || -0.8))));
        const x = window.innerWidth / 2 + right * scale;
        const y = window.innerHeight / 2 + forward * scale * pitchFactor - dy * scale * 0.65;
        return { x, y, behind: false };
    }

    function screenToWorld(screenX, screenY, view) {
        if (!view) return null;
        const perspectiveBoost = view.mode === 'perspective' ? 1.35 : 1;
        const scale = Math.max(2, Math.min(120, (window.innerHeight / Math.max(10, view.height * 2.2)) * perspectiveBoost));
        const pitchFactor = Math.max(0.2, Math.min(1, Math.abs(Math.sin(view.pitch || -0.8))));
        const right = (screenX - window.innerWidth / 2) / scale;
        const forward = (screenY - window.innerHeight / 2) / (scale * pitchFactor);
        const yaw = view.rotation ?? view.yaw ?? 0;
        const cos = Math.cos(yaw);
        const sin = Math.sin(yaw);
        const dx = right * cos + forward * sin;
        const dz = -right * sin + forward * cos;
        return {
            x: view.x + dx,
            y: view.y,
            z: view.z + dz
        };
    }

    function snapPoint(raw) {
        if (!raw) return null;
        return {
            x: Math.floor(raw.x) + 0.5,
            y: Math.round(raw.y),
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
        if (!gisEditMode || !gisCanEdit || !point) return;
        const pickKey = `${point.x},${point.y},${point.z}`;
        const now = Date.now();
        if (lastPickKey === pickKey && now - lastPickAt < 400) {
            return;
        }
        lastPickAt = now;
        lastPickKey = pickKey;
        if (getMapViewState() !== 'flat') {
            setStatus('请切换到 2D 俯视后再绘制', 'error');
            return;
        }

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
        if (!gisEditMode || !gisCanEdit) return;
        if (activeTool === 'select') return;
        const target = event.target;
        if (target?.closest?.(`#${PANEL_ID}`)) return;
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
        if (!gisEditMode || !gisCanEdit) return;
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

    function buildSvgPath(points, closed) {
        const view = getViewForProjection();
        const camera = getBlueMapCamera();
        const screen = points
            .map((p) => projectWorldPoint(p, camera, view))
            .filter((p) => p && !p.behind);
        if (screen.length < 2) return '';
        return screen.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ')
            + (closed ? ' Z' : '');
    }

    function renderOverlay() {
        const svg = ensureSvgLayer();
        const pinLayer = ensurePinLayer();
        if (!svg || !pinLayer) return;

        const fragments = [];
        iterVisibleFeatures().forEach(({ feature, layer }) => {
            const color = featureColor(feature, layer);
            const selected = feature.id === selectedFeatureId;
            const width = selected ? 4 : 3;
            const points = coordsToPoints(feature.coordinates);

            if (feature.type === 'LineString' && points.length >= 2) {
                const d = buildSvgPath(points, false);
                if (d) {
                    fragments.push(
                        `<path data-fid="${escapeHtml(feature.id)}" d="${d}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="round" stroke-linejoin="round" opacity="0.9"/>`
                    );
                }
            }
            if (feature.type === 'Polygon' && points.length >= 3) {
                const d = buildSvgPath(points, true);
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
            const d = buildSvgPath(draft, activeTool === 'polygon' && draft.length >= 3);
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
            const projected = projectWorldPoint(point, getBlueMapCamera(), getViewForProjection());
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
        document.body.classList.toggle('mcwws-gis-drawing', gisEditMode && gisCanEdit && activeTool !== 'select');
    }

    function renderPanel() {
        let panel = document.getElementById(PANEL_ID);
        if (!panel) {
            panel = document.createElement('div');
            panel.id = PANEL_ID;
            document.body.appendChild(panel);
        }

        const layer = getActiveLayer();
        const selected = selectedFeatureId ? findFeatureById(selectedFeatureId) : null;
        const editHint = gisCanEdit
            ? (gisEditMode ? '编辑中：在 2D 俯视点击地图添加要素' : '开启编辑后即可绘制')
            : '登录管理员账户后可编辑';

        panel.innerHTML = `
            <div class="mcwws-gis-card">
                <div class="mcwws-gis-header">
                    <h2>地图标注 GIS</h2>
                    <button type="button" class="mcwws-gis-toggle-btn" data-action="collapse" title="折叠">${
                        panelCollapsed ? '◀' : '▶'
                    }</button>
                </div>
                <div class="mcwws-gis-body${panelCollapsed ? ' is-collapsed' : ''}">
                    <p class="mcwws-gis-hint">${escapeHtml(editHint)}。道路/区域：<strong>双击</strong>或点完成结束。</p>
                    <div class="mcwws-gis-tools">
                        ${TOOLS.map((t) => `
                            <button type="button" class="mcwws-gis-tool${activeTool === t.id ? ' is-active' : ''}"
                                data-tool="${t.id}" ${!gisEditMode || !gisCanEdit ? 'disabled' : ''}>
                                ${t.icon}<br>${t.label}
                            </button>
                        `).join('')}
                    </div>
                    <div class="mcwws-gis-actions">
                        <button type="button" class="mcwws-gis-btn mcwws-gis-btn--ghost" data-action="toggle-edit"
                            ${!gisCanEdit ? 'disabled' : ''}>
                            ${gisEditMode ? '退出编辑' : '开始编辑'}
                        </button>
                        <button type="button" class="mcwws-gis-btn mcwws-gis-btn--primary" data-action="save"
                            ${!gisCanEdit || !dirty || saving ? 'disabled' : ''}>
                            ${saving ? '保存中…' : '保存'}
                        </button>
                    </div>
                    <div class="mcwws-gis-actions">
                        <button type="button" class="mcwws-gis-btn mcwws-gis-btn--ghost" data-action="finish-draft"
                            ${!gisEditMode || draftPoints.length === 0 ? 'disabled' : ''}>完成绘制</button>
                        <button type="button" class="mcwws-gis-btn mcwws-gis-btn--ghost" data-action="cancel-draft"
                            ${!gisEditMode || draftPoints.length === 0 ? 'disabled' : ''}>取消</button>
                    </div>
                    <div class="mcwws-gis-actions">
                        <button type="button" class="mcwws-gis-btn mcwws-gis-btn--ghost" data-action="export">导出 GeoJSON</button>
                        <button type="button" class="mcwws-gis-btn mcwws-gis-btn--danger" data-action="delete"
                            ${!selected ? 'disabled' : ''}>删除选中</button>
                    </div>
                    <div class="mcwws-gis-layers">
                        <h3>图层 · 当前：${escapeHtml(layer?.name || '—')}</h3>
                        ${(project?.layers || []).map((l) => `
                            <label class="mcwws-gis-layer-row">
                                <input type="radio" name="mcwws-gis-layer" value="${escapeHtml(l.id)}"
                                    ${l.id === activeLayerId ? 'checked' : ''}>
                                <span class="mcwws-gis-layer-swatch" style="background:${escapeHtml(l.color)}"></span>
                                <input type="checkbox" data-layer-visible="${escapeHtml(l.id)}"
                                    ${l.visible ? 'checked' : ''}>
                                <span>${escapeHtml(l.name)} (${(l.features || []).length})</span>
                            </label>
                        `).join('')}
                    </div>
                    ${
                        selected
                            ? `<p class="mcwws-gis-hint">选中：<strong>${escapeHtml(selected.feature.properties?.name || selected.feature.id)}</strong> (${escapeHtml(selected.feature.type)})</p>`
                            : ''
                    }
                    <p class="mcwws-gis-status${statusKind ? ` is-${statusKind}` : ''}">${escapeHtml(statusMessage)}${dirty ? ' · 未保存' : ''}</p>
                </div>
            </div>
        `;

        panel.querySelector('[data-action="collapse"]')?.addEventListener('click', () => {
            panelCollapsed = !panelCollapsed;
            renderPanel();
        });
        panel.querySelector('[data-action="toggle-edit"]')?.addEventListener('click', () => {
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
            renderPanel();
            renderOverlay();
        });
        panel.querySelector('[data-action="save"]')?.addEventListener('click', () => void saveGisProject());
        panel.querySelector('[data-action="finish-draft"]')?.addEventListener('click', finishDraft);
        panel.querySelector('[data-action="cancel-draft"]')?.addEventListener('click', cancelDraft);
        panel.querySelector('[data-action="export"]')?.addEventListener('click', exportGeoJson);
        panel.querySelector('[data-action="delete"]')?.addEventListener('click', deleteSelectedFeature);
        panel.querySelectorAll('[data-tool]').forEach((btn) => {
            btn.addEventListener('click', () => {
                activeTool = btn.getAttribute('data-tool') || 'select';
                draftPoints = [];
                draftHover = null;
                syncDrawingClass();
                renderPanel();
                renderOverlay();
            });
        });
        panel.querySelectorAll('input[name="mcwws-gis-layer"]').forEach((input) => {
            input.addEventListener('change', () => {
                activeLayerId = input.value;
                renderPanel();
            });
        });
        panel.querySelectorAll('[data-layer-visible]').forEach((input) => {
            input.addEventListener('change', () => {
                const id = input.getAttribute('data-layer-visible');
                const l = project?.layers?.find((x) => x.id === id);
                if (l) {
                    l.visible = input.checked;
                    markDirty();
                    renderOverlay();
                }
            });
        });
    }

    function onKeyDown(event) {
        if (!gisEditMode) return;
        if (event.key === 'Escape') {
            cancelDraft();
        }
        if (event.key === 'Enter' && draftPoints.length) {
            finishDraft();
        }
        if ((event.key === 'Delete' || event.key === 'Backspace') && selectedFeatureId && !isInputFocused()) {
            deleteSelectedFeature();
        }
    }

    function onDblClick(event) {
        if (!gisEditMode || !gisCanEdit) return;
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
        initMapAuth();
        bindMapPicks();
        renderPanel();
        void loadGisProject();
        document.addEventListener('keydown', onKeyDown);
        document.addEventListener('dblclick', onDblClick, true);
        window.addEventListener('hashchange', renderOverlay);
        window.addEventListener('resize', renderOverlay);
        animationId = requestAnimationFrame(tick);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start, { once: true });
    } else {
        start();
    }
})();
