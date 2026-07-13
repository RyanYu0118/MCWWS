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
    const POLL_MS = 1000;

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

    const REPLACE_BEHAVIOR_LABELS = {
        NONE: '仅空气（不替换）',
        WITH_NON_AIR: '替换非空气',
        ALL: '全部替换'
    };

    const LITEMATICA_MOD_CONFIG_PATHS = [
        'config/litematica/litematica.json',
        'config/litematica.json'
    ];

    const PASTE_REPLACE_MODE_OPTIONS = [
        ['NONE', '仅空气'],
        ['WITH_NON_AIR', '替换非空气'],
        ['ALL', '全部替换']
    ];

    const PASTE_REPLACE_MANUAL_STORAGE_KEY = 'mcwws.pasteReplaceManual';

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
        lastError: '',
        lastUpdatedAt: null,
        placements: [],
        selectedPlacementIndex: -1,
        worldFileName: '',
        parseHint: '',
        supportHint: '',
        pasteReplaceBehavior: '',
        placementReplaceBehavior: '',
        pasteReplaceManual: false,
        pasteReplaceManualMode: 'NONE'
    };
    const listeners = new Set();

    function loadPasteReplaceManualPrefs() {
        try {
            const raw = localStorage.getItem(PASTE_REPLACE_MANUAL_STORAGE_KEY);
            if (!raw) return;
            const parsed = JSON.parse(raw);
            state.pasteReplaceManual = !!parsed.enabled;
            state.pasteReplaceManualMode = normalizeReplaceBehavior(parsed.mode || 'NONE');
        } catch (_) { /* ignore */ }
    }

    function savePasteReplaceManualPrefs() {
        try {
            localStorage.setItem(PASTE_REPLACE_MANUAL_STORAGE_KEY, JSON.stringify({
                enabled: state.pasteReplaceManual,
                mode: state.pasteReplaceManualMode
            }));
        } catch (_) { /* ignore */ }
    }

    function getEffectivePasteReplaceBehavior() {
        if (state.pasteReplaceManual) {
            return state.pasteReplaceManualMode || 'NONE';
        }
        return state.pasteReplaceBehavior || 'NONE';
    }

    function getPasteReplaceBehaviorSource() {
        return state.pasteReplaceManual ? 'manual' : (state.pasteReplaceBehavior ? 'litematica' : 'default');
    }

    function setPasteReplaceManual(enabled, mode) {
        state.pasteReplaceManual = !!enabled;
        if (mode != null) {
            state.pasteReplaceManualMode = normalizeReplaceBehavior(mode);
        }
        savePasteReplaceManualPrefs();
        emit(true);
    }

    function computeDataRevision() {
        return JSON.stringify({
            connected: state.connected,
            connectionMode: state.connectionMode,
            lastError: state.lastError,
            selectedConfigPath,
            selectedPlacementIndex: state.selectedPlacementIndex,
            lastUpdatedAt: state.lastUpdatedAt,
            placements: state.placements,
            configFiles: configFiles.map((f) => ({
                path: f.path,
                placementCount: f.placementCount,
                label: f.label,
                modifiedAt: f.modifiedAt
            })),
            parseHint: state.parseHint,
            pasteReplaceBehavior: state.pasteReplaceBehavior,
            placementReplaceBehavior: state.placementReplaceBehavior,
            pasteReplaceManual: state.pasteReplaceManual,
            pasteReplaceManualMode: state.pasteReplaceManualMode,
            effectivePasteReplaceBehavior: getEffectivePasteReplaceBehavior()
        });
    }

    let lastEmittedRevision = '';

    function emit(force = false) {
        const revision = computeDataRevision();
        if (!force && revision === lastEmittedRevision) return;
        lastEmittedRevision = revision;
        const snapshot = getSnapshot();
        listeners.forEach((fn) => {
            try { fn(snapshot); } catch (e) { console.warn('[LitematicaSync]', e); }
        });
    }

    function getSnapshot() {
        const selected = getSelectedPlacement();
        const effectivePasteReplaceBehavior = getEffectivePasteReplaceBehavior();
        return {
            ...state,
            configFiles: configFiles.map((f) => ({ ...f })),
            worldFiles: configFiles.map((f) => f.path),
            selectedWorldFile: selectedConfigPath,
            selectedPlacement: selected,
            hasTransform: selected && (selected.rotation !== 'NONE' || selected.mirror !== 'NONE'),
            effectivePasteReplaceBehavior,
            pasteReplaceBehaviorSource: getPasteReplaceBehaviorSource()
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

    function findConfigValueDeep(obj, key) {
        if (!obj || typeof obj !== 'object') return null;
        if (Object.prototype.hasOwnProperty.call(obj, key) && obj[key] != null && obj[key] !== '') {
            return obj[key];
        }
        for (const value of Object.values(obj)) {
            if (!value || typeof value !== 'object' || Array.isArray(value)) continue;
            const found = findConfigValueDeep(value, key);
            if (found != null && found !== '') return found;
        }
        return null;
    }

    function normalizeReplaceBehavior(value) {
        const raw = String(value || 'NONE').trim().toUpperCase().replace(/[\s-]+/g, '_');
        if (raw === 'WITH_NON_AIR_BLOCKS' || raw === 'WITHNONAIR' || raw === 'NON_AIR') {
            return 'WITH_NON_AIR';
        }
        if (raw === 'ALL_BLOCKS' || raw === 'REPLACE_ALL') {
            return 'ALL';
        }
        if (REPLACE_BEHAVIOR_LABELS[raw]) {
            return raw;
        }
        return 'NONE';
    }

    function labelReplaceBehavior(value) {
        const normalized = normalizeReplaceBehavior(value);
        return REPLACE_BEHAVIOR_LABELS[normalized] || value || REPLACE_BEHAVIOR_LABELS.NONE;
    }

    loadPasteReplaceManualPrefs();

    async function loadGenericSettings() {
        state.pasteReplaceBehavior = '';
        state.placementReplaceBehavior = '';
        if (!isConnected()) return;

        for (const configPath of LITEMATICA_MOD_CONFIG_PATHS) {
            try {
                if (!(await pathExists(configPath))) continue;
                const text = await readTextFile(configPath);
                const data = JSON.parse(text);
                const paste = findConfigValueDeep(data, 'pasteReplaceBehavior');
                const placement = findConfigValueDeep(data, 'placementReplaceBehavior');
                if (paste != null) {
                    state.pasteReplaceBehavior = normalizeReplaceBehavior(paste);
                }
                if (placement != null) {
                    state.placementReplaceBehavior = normalizeReplaceBehavior(placement);
                }
                if (paste != null || placement != null) {
                    return;
                }
            } catch (_) {
                // try next path
            }
        }
    }

    function isPlacementEntry(entry) {
        if (!entry || typeof entry !== 'object' || Array.isArray(entry)) return false;
        const hasSchematic = typeof entry.schematic === 'string' && entry.schematic.length > 0;
        const hasOrigin = entry.origin != null || entry.pos != null;
        return hasSchematic && hasOrigin;
    }

    function inferWorldNameFromConfigPath(sourcePath) {
        const path = String(sourcePath || '').replace(/\\/g, '/');
        const match = path.match(/world_specific_data\/(?:.+?\/)?([^/]+)\.json$/i);
        if (!match) {
            return 'world';
        }
        const base = match[1].toLowerCase();
        if (base.includes('overworld') || base.includes('minecraft_overworld')) {
            return 'world';
        }
        if (base.includes('the_nether') || base.includes('nether')) {
            return 'world_nether';
        }
        if (base.includes('the_end') || base.endsWith('_end')) {
            return 'world_the_end';
        }
        return match[1];
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
            sourcePath,
            worldHint: inferWorldNameFromConfigPath(sourcePath)
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

    function buildConfigLabel(path, placementCount, modifiedAt) {
        const short = path
            .replace(/^config\/litematica\//, 'config/')
            .replace(/^litematica\/world_specific_data\//, 'world/')
            .replace(/^litematica\/placements\//, 'saved/');
        const timeLabel = modifiedAt ? formatFileTime(modifiedAt) : '';
        const timePart = timeLabel ? ` · ${timeLabel}` : '';
        return `${short} (${placementCount} 个投影${timePart})`;
    }

    function formatFileTime(ts) {
        try {
            return new Date(ts).toLocaleString('zh-CN', {
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                hour12: false
            });
        } catch (_) {
            return '';
        }
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

    /** 兼容 webkitdirectory：用户可能误选了 config 或 .minecraft 上一级目录 */
    function normalizeFileIndexForMinecraftRoot() {
        if (!fileIndex.size) return { ok: false, reason: 'empty' };

        const keys = [...fileIndex.keys()];
        const hasStandardRoot = keys.some((key) =>
            key === 'config/litematica'
            || key.startsWith('config/litematica/')
            || key.startsWith('litematica/world_specific_data/')
            || key.startsWith('litematica/placements/')
        );
        if (hasStandardRoot) {
            return { ok: true, mode: 'standard' };
        }

        // 误选了 .minecraft/config 文件夹：路径形如 litematica/...
        const looksLikeConfigDir = keys.some((key) =>
            key.startsWith('litematica/') || key === 'litematica'
        );
        if (looksLikeConfigDir) {
            const rebased = new Map();
            for (const [path, file] of fileIndex.entries()) {
                rebased.set(`config/${path}`, file);
            }
            fileIndex = rebased;
            return { ok: true, mode: 'rebased-config' };
        }

        // 误选了 Minecraft 启动器根目录：仅有 versions、libraries 等，没有完整 config
        const hasDotMinecraftPrefix = keys.some((key) =>
            key.startsWith('.minecraft/config/litematica/')
            || key.startsWith('.minecraft/litematica/')
        );
        if (hasDotMinecraftPrefix) {
            const rebased = new Map();
            for (const [path, file] of fileIndex.entries()) {
                const stripped = normalizeIndexedPath(path);
                if (stripped) rebased.set(stripped, file);
            }
            fileIndex = rebased;
            return { ok: true, mode: 'rebased-dot-minecraft' };
        }

        // 宽松：任意 litematica 相关 json
        const loose = keys.some((key) =>
            /(^|\/)litematica(\/|\.)/i.test(key) && key.endsWith('.json')
        );
        if (loose) {
            return { ok: true, mode: 'loose' };
        }

        const sample = keys
            .filter((key) => key.includes('config') || key.includes('litematica'))
            .slice(0, 6);
        return { ok: false, reason: 'no-litematica', sample };
    }

    async function hasLitematicaLayout() {
        const normalized = normalizeFileIndexForMinecraftRoot();
        if (connectionMode === 'files') {
            if (normalized.ok) return true;
            return false;
        }
        return (await pathExists('config/litematica'))
            || (await pathExists('litematica/world_specific_data'))
            || (await pathExists('litematica/placements'));
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
            let modifiedAt = 0;
            let text = '';
            if (connectionMode === 'handle' && rootHandle) {
                const fileHandle = await getPathHandle(meta.path);
                const file = await fileHandle.getFile();
                modifiedAt = file.lastModified || 0;
                text = await file.text();
            } else if (connectionMode === 'files') {
                const file = getFileFromIndex(meta.path);
                if (!file) throw new Error('missing file');
                modifiedAt = file.lastModified || 0;
                text = await file.text();
            } else {
                text = await readTextFile(meta.path);
            }
            const parsed = parseWorldConfig(text, meta.path);
            return {
                ...meta,
                modifiedAt,
                placementCount: parsed.placements.length,
                parseHint: parsed.parseHint
            };
        } catch (_) {
            return { ...meta, modifiedAt: 0, placementCount: 0, parseHint: '读取失败' };
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
            const timeDiff = (b.modifiedAt || 0) - (a.modifiedAt || 0);
            if (timeDiff !== 0) return timeDiff;
            return a.path.localeCompare(b.path, 'zh-CN');
        });

        return probed.map((f) => ({
            path: f.path,
            kind: f.kind,
            placementCount: f.placementCount,
            modifiedAt: f.modifiedAt || 0,
            parseHint: f.parseHint,
            label: buildConfigLabel(f.path, f.placementCount, f.modifiedAt)
        }));
    }

    function pickDefaultConfigFile(files) {
        if (!files.length) return '';
        const withPlacements = files.find((f) => f.placementCount > 0);
        if (withPlacements) return withPlacements.path;
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
        try {
            await loadGenericSettings();
            if (connectionMode === 'files') {
                await refreshPlacements();
            } else {
                await refreshConfigFileList();
                await refreshPlacements();
            }
        } catch (error) {
            state.lastError = error.message || String(error);
        }
        emit();
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
        await loadGenericSettings();
        await refreshConfigFileList();
        await refreshPlacements();
        startPolling();
        emit(true);
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
        const layout = normalizeFileIndexForMinecraftRoot();
        const hasLitematicaData = layout.ok || await hasLitematicaLayout();
        if (!hasLitematicaData) {
            const sample = layout.sample?.length
                ? ` 当前读到的路径示例：${layout.sample.join('、')}`
                : '';
            throw new Error(
                '未在该文件夹中找到 Litematica 配置。请直接选择 .minecraft 文件夹（需含 config/litematica 或 litematica 子目录；安装 Litematica 并至少进一次游戏后才会生成）。'
                + sample
            );
        }

        rootHandle = null;
        connectionMode = 'files';
        state.connected = true;
        state.connectionMode = 'files';
        state.supportHint = getSupportInfo().hint;
        state.lastError = '';
        await loadGenericSettings();
        await refreshConfigFileList();
        await refreshPlacements();
        stopPolling();
        emit(true);
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
            connectionMode = 'handle';
            state.connected = true;
            state.connectionMode = 'handle';
            await loadGenericSettings();
            await refreshConfigFileList();
            await refreshPlacements();
            startPolling();
            emit(true);
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
            lastError: '',
            lastUpdatedAt: null,
            placements: [],
            selectedPlacementIndex: -1,
            worldFileName: '',
            parseHint: '',
            supportHint: '',
            pasteReplaceBehavior: '',
            placementReplaceBehavior: '',
            pasteReplaceManual: state.pasteReplaceManual,
            pasteReplaceManualMode: state.pasteReplaceManualMode
        };
        await clearStoredDirHandle();
        lastEmittedRevision = '';
        emit(true);
    }

    async function setWorldFile(filePath) {
        selectedConfigPath = filePath || '';
        await pollOnce();
    }

    function setSelectedPlacementIndex(index) {
        state.selectedPlacementIndex = Number(index);
        emit(true);
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
        labelReplaceBehavior,
        normalizeReplaceBehavior,
        getEffectivePasteReplaceBehavior,
        setPasteReplaceManual,
        PASTE_REPLACE_MODE_OPTIONS,
        inferWorldNameFromConfigPath,
        buildAnchorFromPlacement(placement, worldName) {
            if (!placement?.origin) return null;
            const world = String(worldName || placement.worldHint || 'world').trim();
            if (!world) return null;
            return {
                world,
                x: Math.trunc(Number(placement.origin.x) || 0),
                y: Math.trunc(Number(placement.origin.y) || 0),
                z: Math.trunc(Number(placement.origin.z) || 0),
                yaw: 0,
                pitch: 0
            };
        },
        isSupported
    };
})();
