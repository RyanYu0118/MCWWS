/**
 * 通过 File System Access API 读取客户端 .minecraft 中 Litematica 配置，
 * 实时同步投影放置原点、旋转、镜像等数据。
 *
 * 兼容多种 Litematica 版本路径：
 * - config/litematica/*.json（旧版 / 部分 fork）
 * - litematica/world_specific_data/（新版 maruohon，递归扫描 json）
 * - litematica/placements/（单独保存的放置配置）
 */
(function () {
    const IDB_NAME = 'mcwws-litematica-sync';
    const IDB_STORE = 'handles';
    const IDB_KEY = 'minecraftDir';
    const POLL_MS = 2000;

    const SCAN_ROOTS = [
        'config/litematica',
        'litematica/world_specific_data',
        'litematica/placements'
    ];

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
    let connectionMode = 'none'; // 'handle' | 'files'
    /** @type {Map<string, File>} */
    let fileIndex = new Map();
    let pollTimer = null;
    /** @type {{ path: string, label: string, placementCount: number, kind: string }[]} */
    let configFiles = [];
    let selectedConfigPath = '';
    let state = {
        connected: false,
        connectionMode: 'none',
        polling: false,
        lastError: '',
        lastUpdatedAt: null,
        placements: [],
        selectedPlacementIndex: -1,
        worldFileName: '',
        parseHint: '',
        supportHint: ''
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
            configFiles: configFiles.map((f) => ({ ...f })),
            worldFiles: configFiles.map((f) => f.path),
            selectedWorldFile: selectedConfigPath,
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

    function isPlacementEntry(entry) {
        if (!entry || typeof entry !== 'object' || Array.isArray(entry)) return false;
        const hasSchematic = typeof entry.schematic === 'string' && entry.schematic.length > 0;
        const hasOrigin = entry.origin != null || entry.pos != null;
        return hasSchematic && hasOrigin;
    }

    function mapPlacementEntry(entry, index, selectedIndex, sourcePath) {
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
            locked: !!entry.locked,
            sourcePath
        };
    }

    function extractPlacementsFromData(data, sourcePath) {
        if (!data || typeof data !== 'object') {
            return { placements: [], selectedIndex: -1, hint: 'JSON 为空或格式无效' };
        }

        // 单独保存的放置文件：根对象即一个 placement
        if (isPlacementEntry(data)) {
            return {
                placements: [mapPlacementEntry(data, 0, 0, sourcePath)],
                selectedIndex: 0,
                hint: '单独放置配置文件'
            };
        }

        const placementsRoot = data.placements && typeof data.placements === 'object' && !Array.isArray(data.placements)
            ? data.placements
            : data;

        let arr = [];
        let selectedIndex = -1;

        if (Array.isArray(placementsRoot.placements)) {
            arr = placementsRoot.placements;
            selectedIndex = Number.isInteger(placementsRoot.selected) ? placementsRoot.selected : -1;
        } else if (Array.isArray(data.placements)) {
            arr = data.placements;
            selectedIndex = Number.isInteger(data.selected) ? data.selected : -1;
        }

        const placements = arr
            .filter((entry) => isPlacementEntry(entry))
            .map((entry, index) => mapPlacementEntry(entry, index, selectedIndex, sourcePath));

        let hint = '';
        if (!placements.length) {
            if (arr.length > 0) {
                hint = `发现 ${arr.length} 条记录，但缺少 schematic/原点 字段，无法识别为投影`;
            } else if (data.placements && typeof data.placements === 'object' && !Array.isArray(data.placements)) {
                hint = '配置文件存在，但 placements 列表为空（可能尚未保存，或投影在其它配置文件）';
            } else {
                hint = '未找到 placements 数据';
            }
        }

        return { placements, selectedIndex, hint };
    }

    function parseWorldConfig(text, filePath) {
        const data = JSON.parse(text);
        const result = extractPlacementsFromData(data, filePath);
        return {
            fileName: basename(filePath),
            filePath,
            placements: result.placements,
            selectedIndex: result.selectedIndex,
            parseHint: result.hint,
            updatedAt: Date.now()
        };
    }

    function buildConfigLabel(path, placementCount) {
        const short = path
            .replace(/^config\/litematica\//, 'config/')
            .replace(/^litematica\/world_specific_data\//, 'world/')
            .replace(/^litematica\/placements\//, 'saved/');
        return `${short} (${placementCount} 个投影)`;
    }

    function normalizeIndexedPath(relativePath) {
        let normalized = String(relativePath || '').replace(/\\/g, '/');
        const lower = normalized.toLowerCase();
        const marker = '.minecraft/';
        const idx = lower.indexOf(marker);
        if (idx >= 0) {
            normalized = normalized.slice(idx + marker.length);
        }
        while (normalized.startsWith('./')) {
            normalized = normalized.slice(2);
        }
        return normalized;
    }

    function buildFileIndexFromFileList(fileList) {
        fileIndex.clear();
        for (const file of fileList) {
            const rel = normalizeIndexedPath(file.webkitRelativePath || file.name);
            if (rel) fileIndex.set(rel, file);
        }
    }

    function isConnected() {
        return connectionMode === 'handle' || connectionMode === 'files';
    }

    function getSupportInfo() {
        const secure = window.isSecureContext === true;
        const hasPicker = typeof window.showDirectoryPicker === 'function';
        if (hasPicker && secure) {
            return {
                canConnect: true,
                mode: 'picker',
                hint: '支持实时监听配置变更（推荐 HTTPS 或 localhost 访问）'
            };
        }
        const input = document.createElement('input');
        const hasFallback = 'webkitdirectory' in input;
        if (hasFallback) {
            return {
                canConnect: true,
                mode: 'fallback',
                hint: secure
                    ? '将使用兼容模式选择文件夹'
                    : '当前为 HTTP 访问，已启用兼容模式；选文件夹后若游戏内改了配置，需点「重新选择文件夹」刷新'
            };
        }
        return {
            canConnect: false,
            mode: 'none',
            hint: '请使用 Chrome / Edge 桌面版，并通过 https:// 或 http://localhost 访问本页'
        };
    }

    function getFileFromIndex(relativePath) {
        const key = normalizeIndexedPath(relativePath);
        if (fileIndex.has(key)) return fileIndex.get(key);
        const base = basename(key);
        for (const [path, file] of fileIndex.entries()) {
            if (path === key || path.endsWith(`/${base}`)) return file;
        }
        return null;
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

    async function pathExists(relativePath) {
        if (connectionMode === 'handle' && rootHandle) {
            try {
                await getPathHandle(relativePath);
                return true;
            } catch (_) {
                return false;
            }
        }
        if (connectionMode === 'files') {
            const prefix = String(relativePath).replace(/\\/g, '/').replace(/\/$/, '');
            for (const key of fileIndex.keys()) {
                if (key === prefix || key.startsWith(`${prefix}/`)) return true;
            }
        }
        return false;
    }

    async function readTextFile(relativePath) {
        if (connectionMode === 'handle' && rootHandle) {
            const fileHandle = await getPathHandle(relativePath);
            const file = await fileHandle.getFile();
            return file.text();
        }
        if (connectionMode === 'files') {
            const file = getFileFromIndex(relativePath);
            if (!file) throw new Error(`找不到文件: ${relativePath}`);
            return file.text();
        }
        throw new Error('尚未连接 .minecraft 文件夹');
    }

    async function collectJsonFilesRecursive(dirPath, dirHandle, out, kind) {
        for await (const entry of dirHandle.values()) {
            const childPath = dirPath ? `${dirPath}/${entry.name}` : entry.name;
            if (entry.kind === 'directory') {
                await collectJsonFilesRecursive(childPath, entry, out, kind);
                continue;
            }
            if (entry.kind !== 'file' || !entry.name.endsWith('.json')) continue;
            if (entry.name === 'litematica.json') continue;
            out.push({ path: childPath, kind });
        }
    }

    function collectJsonFilesFromIndex(out) {
        for (const root of SCAN_ROOTS) {
            const kind = root.includes('placements') ? 'saved' : (root.includes('world_specific') ? 'world' : 'config');
            for (const path of fileIndex.keys()) {
                if (!path.startsWith(`${root}/`)) continue;
                if (!path.endsWith('.json')) continue;
                if (basename(path) === 'litematica.json') continue;
                out.push({ path, kind });
            }
        }
    }

    async function probeConfigFile(meta) {
        try {
            const text = await readTextFile(meta.path);
            const parsed = parseWorldConfig(text, meta.path);
            return {
                ...meta,
                placementCount: parsed.placements.length,
                parseHint: parsed.parseHint
            };
        } catch (_) {
            return { ...meta, placementCount: 0, parseHint: '读取失败' };
        }
    }

    async function listLitematicaConfigFiles() {
        const found = [];
        if (connectionMode === 'handle' && rootHandle) {
            for (const root of SCAN_ROOTS) {
                if (!(await pathExists(root))) continue;
                const dirHandle = await getPathHandle(root);
                const kind = root.includes('placements') ? 'saved' : (root.includes('world_specific') ? 'world' : 'config');
                await collectJsonFilesRecursive(root, dirHandle, found, kind);
            }
        } else if (connectionMode === 'files') {
            collectJsonFilesFromIndex(found);
        }

        const probed = await Promise.all(found.map((f) => probeConfigFile(f)));
        probed.sort((a, b) => {
            if (b.placementCount !== a.placementCount) return b.placementCount - a.placementCount;
            return a.path.localeCompare(b.path, 'zh-CN');
        });

        return probed.map((f) => ({
            path: f.path,
            kind: f.kind,
            placementCount: f.placementCount,
            parseHint: f.parseHint,
            label: buildConfigLabel(f.path, f.placementCount)
        }));
    }

    function pickDefaultConfigFile(files) {
        if (!files.length) return '';
        const withPlacements = files.find((f) => f.placementCount > 0);
        if (withPlacements) return withPlacements.path;
        const overworld = files.find((f) => /overworld/i.test(f.path));
        if (overworld) return overworld.path;
        return files[0].path;
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
        if (!isConnected() || !placement) return null;
        const candidates = [
            resolveRelativeFromAbsolute(placement.schematicPath),
            placement.schematicFileName ? `schematics/${placement.schematicFileName}` : null,
            placement.schematicFileName || null
        ].filter(Boolean);

        for (const rel of candidates) {
            try {
                if (connectionMode === 'handle' && rootHandle) {
                    const fileHandle = await getPathHandle(rel);
                    const file = await fileHandle.getFile();
                    return new File([file], placement.schematicFileName || file.name, {
                        type: 'application/octet-stream',
                        lastModified: file.lastModified
                    });
                }
                if (connectionMode === 'files') {
                    const file = getFileFromIndex(rel);
                    if (!file) continue;
                    return new File([file], placement.schematicFileName || file.name, {
                        type: 'application/octet-stream',
                        lastModified: file.lastModified
                    });
                }
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

    async function refreshConfigFileList() {
        configFiles = await listLitematicaConfigFiles();
        const paths = configFiles.map((f) => f.path);
        if (!selectedConfigPath || !paths.includes(selectedConfigPath)) {
            selectedConfigPath = pickDefaultConfigFile(configFiles);
        }
    }

    async function refreshPlacements() {
        if (!isConnected() || !selectedConfigPath) {
            state.placements = [];
            state.selectedPlacementIndex = -1;
            state.worldFileName = '';
            state.parseHint = configFiles.length ? '未选择配置文件' : '未找到任何 Litematica JSON 配置';
            return;
        }
        const text = await readTextFile(selectedConfigPath);
        const parsed = parseWorldConfig(text, selectedConfigPath);
        const prevIndex = state.selectedPlacementIndex;
        state.placements = parsed.placements;
        if (prevIndex >= 0 && prevIndex < parsed.placements.length) {
            state.selectedPlacementIndex = prevIndex;
        } else {
            state.selectedPlacementIndex = parsed.selectedIndex;
        }
        state.worldFileName = parsed.fileName;
        state.parseHint = parsed.parseHint;
        state.lastUpdatedAt = parsed.updatedAt;
        state.lastError = '';

        const entry = configFiles.find((f) => f.path === selectedConfigPath);
        if (entry) entry.placementCount = parsed.placements.length;
    }

    async function pollOnce() {
        if (!isConnected()) return;
        state.polling = true;
        emit();
        try {
            if (connectionMode === 'files') {
                await refreshPlacements();
            } else {
                await refreshConfigFileList();
                await refreshPlacements();
            }
        } catch (error) {
            state.lastError = error.message || String(error);
        } finally {
            state.polling = false;
            emit();
        }
    }

    function startPolling() {
        stopPolling();
        if (connectionMode !== 'handle') return;
        pollTimer = window.setInterval(() => { void pollOnce(); }, POLL_MS);
    }

    function stopPolling() {
        if (pollTimer) {
            clearInterval(pollTimer);
            pollTimer = null;
        }
    }

    async function connectDirectory(pickedHandle) {
        const support = getSupportInfo();
        if (support.mode !== 'picker') {
            throw new Error('当前环境不支持系统文件夹选择器，请使用兼容模式按钮。');
        }
        const handle = pickedHandle || await window.showDirectoryPicker({ mode: 'read' });
        const ok = await ensureReadPermission(handle);
        if (!ok) throw new Error('未获得 .minecraft 文件夹读取权限。');

        rootHandle = handle;
        fileIndex.clear();
        connectionMode = 'handle';
        await storeDirHandle(handle);
        state.connected = true;
        state.connectionMode = 'handle';
        state.supportHint = support.hint;
        state.lastError = '';
        await refreshConfigFileList();
        await refreshPlacements();
        startPolling();
        emit();
        return getSnapshot();
    }

    async function connectFromFileList(fileList) {
        if (!fileList || !fileList.length) {
            throw new Error('未选择任何文件，请选中 .minecraft 文件夹后确认。');
        }
        buildFileIndexFromFileList(fileList);
        if (!fileIndex.size) {
            throw new Error('所选文件夹为空或无法读取。');
        }
        const hasLitematicaData = await pathExists('config/litematica')
            || await pathExists('litematica/world_specific_data')
            || await pathExists('litematica/placements');
        if (!hasLitematicaData) {
            throw new Error('未在该文件夹中找到 Litematica 配置。请直接选择 .minecraft 文件夹（含 config/litematica 或 litematica 子目录）。');
        }

        rootHandle = null;
        connectionMode = 'files';
        state.connected = true;
        state.connectionMode = 'files';
        state.supportHint = getSupportInfo().hint;
        state.lastError = '';
        await refreshConfigFileList();
        await refreshPlacements();
        stopPolling();
        emit();
        return getSnapshot();
    }

    async function openFolderDialog() {
        const support = getSupportInfo();
        if (!support.canConnect) {
            throw new Error(support.hint);
        }
        if (support.mode === 'picker') {
            return connectDirectory();
        }
        return new Promise((resolve, reject) => {
            const input = document.getElementById('buildClientSyncFolderInput');
            if (!input) {
                reject(new Error('兼容模式文件选择器未就绪，请刷新页面后重试。'));
                return;
            }
            input.value = '';
            const onChange = async () => {
                input.removeEventListener('change', onChange);
                try {
                    const result = await connectFromFileList(input.files);
                    resolve(result);
                } catch (error) {
                    reject(error);
                }
            };
            input.addEventListener('change', onChange);
            input.click();
        });
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
            await refreshConfigFileList();
            await refreshPlacements();
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
        fileIndex.clear();
        connectionMode = 'none';
        configFiles = [];
        selectedConfigPath = '';
        state = {
            connected: false,
            connectionMode: 'none',
            polling: false,
            lastError: '',
            lastUpdatedAt: null,
            placements: [],
            selectedPlacementIndex: -1,
            worldFileName: '',
            parseHint: '',
            supportHint: ''
        };
        await clearStoredDirHandle();
        emit();
    }

    async function setWorldFile(filePath) {
        selectedConfigPath = filePath || '';
        await pollOnce();
    }

    function setSelectedPlacementIndex(index) {
        state.selectedPlacementIndex = Number(index);
        emit();
    }

    function isSupported() {
        return getSupportInfo().canConnect;
    }

    window.MCWWS_LitematicaClientSync = {
        subscribe,
        getSnapshot,
        getSupportInfo,
        connectDirectory,
        connectFromFileList,
        openFolderDialog,
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
