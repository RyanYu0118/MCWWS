/**
 * 通过 File System Access API 读取客户端 .minecraft/config/litematica 配置，
 * 实时同步投影放置原点、旋转、镜像等数据。
 */
(function () {
    const IDB_NAME = 'mcwws-litematica-sync';
    const IDB_STORE = 'handles';
    const IDB_KEY = 'minecraftDir';
    const POLL_MS = 2000;
    const LITEMATICA_CONFIG_DIR = 'config/litematica';

    const ROTATION_LABELS = {
        NONE: '无',
        CLOCKWISE_90: '顺时针 90°',
        CLOCKWISE_180: '180°',
        COUNTERCLOCKWISE_90: '逆时针 90°'
    };

    const MIRROR_LABELS = {
        NONE: '无',
        LEFT_RIGHT: '左右',
        FRONT_BACK: '前后'
    };

    let rootHandle = null;
    let pollTimer = null;
    let worldFiles = [];
    let selectedWorldFile = '';
    let state = {
        connected: false,
        polling: false,
        lastError: '',
        lastUpdatedAt: null,
        placements: [],
        selectedPlacementIndex: -1,
        worldFileName: ''
    };
    const listeners = new Set();

    function emit() {
        listeners.forEach((fn) => {
            try { fn(getSnapshot()); } catch (e) { console.warn('[LitematicaSync]', e); }
        });
    }

    function getSnapshot() {
        const selected = getSelectedPlacement();
        return {
            ...state,
            worldFiles: [...worldFiles],
            selectedWorldFile,
            selectedPlacement: selected,
            hasTransform: selected && (selected.rotation !== 'NONE' || selected.mirror !== 'NONE')
        };
    }

    function subscribe(fn) {
        listeners.add(fn);
        return () => listeners.delete(fn);
    }

    function basename(path) {
        const normalized = String(path || '').replace(/\\/g, '/');
        const parts = normalized.split('/').filter(Boolean);
        return parts[parts.length - 1] || '';
    }

    function normalizePos(value) {
        if (Array.isArray(value) && value.length >= 3) {
            return { x: Number(value[0]) || 0, y: Number(value[1]) || 0, z: Number(value[2]) || 0 };
        }
        if (value && typeof value === 'object') {
            return {
                x: Number(value.x ?? value.X ?? 0) || 0,
                y: Number(value.y ?? value.Y ?? 0) || 0,
                z: Number(value.z ?? value.Z ?? 0) || 0
            };
        }
        return { x: 0, y: 0, z: 0 };
    }

    function formatPos(pos) {
        if (!pos) return '—';
        return `${Math.trunc(pos.x)}, ${Math.trunc(pos.y)}, ${Math.trunc(pos.z)}`;
    }

    function labelRotation(value) {
        return ROTATION_LABELS[value] || value || '无';
    }

    function labelMirror(value) {
        return MIRROR_LABELS[value] || value || '无';
    }

    function parseWorldConfig(text, fileName) {
        const data = JSON.parse(text);
        const placementsRoot = data.placements && typeof data.placements === 'object'
            ? data.placements
            : data;
        const arr = Array.isArray(placementsRoot.placements) ? placementsRoot.placements : [];
        const selectedIndex = Number.isInteger(placementsRoot.selected)
            ? placementsRoot.selected
            : -1;

        const placements = arr.map((entry, index) => {
            const origin = entry.origin ?? entry.pos;
            return {
                index,
                selected: index === selectedIndex,
                name: entry.name || `投影 ${index + 1}`,
                schematicPath: entry.schematic || '',
                schematicFileName: basename(entry.schematic),
                origin: normalizePos(origin),
                rotation: entry.rotation || 'NONE',
                mirror: entry.mirror || 'NONE',
                enabled: entry.enabled !== false,
                enableRender: entry.enable_render !== false,
                ignoreEntities: !!entry.ignore_entities,
                locked: !!entry.locked
            };
        });

        return {
            fileName,
            placements,
            selectedIndex,
            updatedAt: Date.now()
        };
    }

    function openDb() {
        return new Promise((resolve, reject) => {
            const req = indexedDB.open(IDB_NAME, 1);
            req.onupgradeneeded = () => {
                req.result.createObjectStore(IDB_STORE);
            };
            req.onsuccess = () => resolve(req.result);
            req.onerror = () => reject(req.error || new Error('IndexedDB 打开失败'));
        });
    }

    async function storeDirHandle(handle) {
        const db = await openDb();
        await new Promise((resolve, reject) => {
            const tx = db.transaction(IDB_STORE, 'readwrite');
            tx.objectStore(IDB_STORE).put(handle, IDB_KEY);
            tx.oncomplete = () => resolve();
            tx.onerror = () => reject(tx.error);
        });
        db.close();
    }

    async function loadStoredDirHandle() {
        const db = await openDb();
        const handle = await new Promise((resolve, reject) => {
            const tx = db.transaction(IDB_STORE, 'readonly');
            const req = tx.objectStore(IDB_STORE).get(IDB_KEY);
            req.onsuccess = () => resolve(req.result || null);
            req.onerror = () => reject(req.error);
        });
        db.close();
        return handle;
    }

    async function clearStoredDirHandle() {
        const db = await openDb();
        await new Promise((resolve, reject) => {
            const tx = db.transaction(IDB_STORE, 'readwrite');
            tx.objectStore(IDB_STORE).delete(IDB_KEY);
            tx.oncomplete = () => resolve();
            tx.onerror = () => reject(tx.error);
        });
        db.close();
    }

    async function ensureReadPermission(handle) {
        if (!handle) return false;
        const opts = { mode: 'read' };
        if ((await handle.queryPermission(opts)) === 'granted') return true;
        if ((await handle.requestPermission(opts)) === 'granted') return true;
        return false;
    }

    async function getPathHandle(relativePath, createDir = false) {
        const parts = String(relativePath).split('/').filter(Boolean);
        let handle = rootHandle;
        for (let i = 0; i < parts.length; i += 1) {
            const part = parts[i];
            const isLast = i === parts.length - 1;
            if (isLast && part.includes('.')) {
                return handle.getFileHandle(part, createDir ? { create: true } : undefined);
            }
            handle = await handle.getDirectoryHandle(part, createDir ? { create: true } : undefined);
        }
        return handle;
    }

    async function readTextFile(relativePath) {
        const fileHandle = await getPathHandle(relativePath);
        const file = await fileHandle.getFile();
        return file.text();
    }

    async function listLitematicaWorldConfigs() {
        const dirHandle = await getPathHandle(LITEMATICA_CONFIG_DIR);
        const files = [];
        for await (const entry of dirHandle.values()) {
            if (entry.kind !== 'file' || !entry.name.endsWith('.json')) continue;
            if (entry.name === 'litematica.json') continue;
            files.push(entry.name);
        }
        files.sort((a, b) => a.localeCompare(b, 'zh-CN'));
        return files;
    }

    function resolveRelativeFromAbsolute(absPath) {
        const normalized = String(absPath || '').replace(/\\/g, '/');
        const lower = normalized.toLowerCase();
        const markers = ['/.minecraft/', '.minecraft/'];
        for (const marker of markers) {
            const idx = lower.indexOf(marker);
            if (idx >= 0) {
                return normalized.slice(idx + marker.length);
            }
        }
        return null;
    }

    async function readSchematicAsFile(placement) {
        if (!rootHandle || !placement) return null;
        const candidates = [
            resolveRelativeFromAbsolute(placement.schematicPath),
            placement.schematicFileName ? `schematics/${placement.schematicFileName}` : null,
            placement.schematicFileName || null
        ].filter(Boolean);

        for (const rel of candidates) {
            try {
                const fileHandle = await getPathHandle(rel);
                const file = await fileHandle.getFile();
                return new File([file], placement.schematicFileName || file.name, {
                    type: 'application/octet-stream',
                    lastModified: file.lastModified
                });
            } catch (_) {
                // try next candidate
            }
        }
        return null;
    }

    function getSelectedPlacement() {
        if (!state.placements.length) return null;
        if (state.selectedPlacementIndex >= 0) {
            const manual = state.placements[state.selectedPlacementIndex];
            if (manual) return manual;
        }
        return state.placements.find((p) => p.selected) || state.placements[0] || null;
    }

    async function refreshWorldFileList() {
        worldFiles = await listLitematicaWorldConfigs();
        if (!selectedWorldFile || !worldFiles.includes(selectedWorldFile)) {
            selectedWorldFile = worldFiles[0] || '';
        }
    }

    async function refreshPlacements() {
        if (!rootHandle || !selectedWorldFile) {
            state.placements = [];
            state.selectedPlacementIndex = -1;
            state.worldFileName = '';
            return;
        }
        const text = await readTextFile(`${LITEMATICA_CONFIG_DIR}/${selectedWorldFile}`);
        const parsed = parseWorldConfig(text, selectedWorldFile);
        const prevIndex = state.selectedPlacementIndex;
        state.placements = parsed.placements;
        if (prevIndex >= 0 && prevIndex < parsed.placements.length) {
            state.selectedPlacementIndex = prevIndex;
        } else {
            state.selectedPlacementIndex = parsed.selectedIndex;
        }
        state.worldFileName = parsed.fileName;
        state.lastUpdatedAt = parsed.updatedAt;
        state.lastError = '';
    }

    async function pollOnce() {
        if (!rootHandle) return;
        state.polling = true;
        emit();
        try {
            await refreshPlacements();
        } catch (error) {
            state.lastError = error.message || String(error);
        } finally {
            state.polling = false;
            emit();
        }
    }

    function startPolling() {
        stopPolling();
        pollTimer = window.setInterval(() => { void pollOnce(); }, POLL_MS);
    }

    function stopPolling() {
        if (pollTimer) {
            clearInterval(pollTimer);
            pollTimer = null;
        }
    }

    async function connectDirectory(pickedHandle) {
        if (!window.showDirectoryPicker && !pickedHandle) {
            throw new Error('当前浏览器不支持文件夹访问，请使用 Chrome / Edge 桌面版。');
        }
        const handle = pickedHandle || await window.showDirectoryPicker({ mode: 'read' });
        const ok = await ensureReadPermission(handle);
        if (!ok) throw new Error('未获得 .minecraft 文件夹读取权限。');

        rootHandle = handle;
        await storeDirHandle(handle);
        state.connected = true;
        state.lastError = '';
        await refreshWorldFileList();
        await pollOnce();
        startPolling();
        emit();
        return getSnapshot();
    }

    async function tryRestoreConnection() {
        if (!window.showDirectoryPicker) return null;
        try {
            const handle = await loadStoredDirHandle();
            if (!handle) return null;
            const ok = await ensureReadPermission(handle);
            if (!ok) return null;
            rootHandle = handle;
            state.connected = true;
            await refreshWorldFileList();
            await pollOnce();
            startPolling();
            emit();
            return getSnapshot();
        } catch (_) {
            return null;
        }
    }

    async function disconnect() {
        stopPolling();
        rootHandle = null;
        worldFiles = [];
        selectedWorldFile = '';
        state = {
            connected: false,
            polling: false,
            lastError: '',
            lastUpdatedAt: null,
            placements: [],
            selectedPlacementIndex: -1,
            worldFileName: ''
        };
        await clearStoredDirHandle();
        emit();
    }

    async function setWorldFile(fileName) {
        selectedWorldFile = fileName || '';
        await pollOnce();
    }

    function setSelectedPlacementIndex(index) {
        state.selectedPlacementIndex = Number(index);
        emit();
    }

    function isSupported() {
        return typeof window.showDirectoryPicker === 'function';
    }

    window.MCWWS_LitematicaClientSync = {
        subscribe,
        getSnapshot,
        connectDirectory,
        tryRestoreConnection,
        disconnect,
        setWorldFile,
        setSelectedPlacementIndex,
        getSelectedPlacement,
        readSchematicAsFile,
        pollOnce,
        formatPos,
        labelRotation,
        labelMirror,
        isSupported
    };
})();
