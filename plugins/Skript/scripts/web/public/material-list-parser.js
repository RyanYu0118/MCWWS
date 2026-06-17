/**
 * Litematica / Stormatica 等材料清单 JSON 解析（浏览器与 Node 共用）
 */
(function (root, factory) {
    const api = factory();
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    }
    root.MCWWSMaterialListParser = api;
}(typeof globalThis !== 'undefined' ? globalThis : this, function () {
    function normalizeMaterialId(material) {
        if (material == null || material === '') return null;
        let s = String(material).trim().toLowerCase().replace(/-/g, '_');
        if (s.includes(':')) s = s.split(':').pop();
        return s || null;
    }

    function normalizeLookupKey(text) {
        return String(text || '')
            .toLowerCase()
            .replace(/[''`´′＇]/g, '')
            .replace(/minecraft:/gi, '')
            .replace(/[\s_-]+/g, '');
    }

    /** 不区分大小写读取对象字段（兼容 Litematica 的 PascalCase） */
    function getField(obj, ...names) {
        if (!obj || typeof obj !== 'object') return undefined;
        const index = {};
        Object.keys(obj).forEach((key) => {
            index[key.toLowerCase()] = obj[key];
        });
        for (const name of names) {
            const val = index[String(name).toLowerCase()];
            if (val !== undefined && val !== null && String(val).trim() !== '') {
                return val;
            }
        }
        return undefined;
    }

    function findArrayField(obj, ...names) {
        if (!obj || typeof obj !== 'object' || Array.isArray(obj)) return null;
        const index = {};
        Object.keys(obj).forEach((key) => {
            index[key.toLowerCase()] = key;
        });
        for (const name of names) {
            const actual = index[String(name).toLowerCase()];
            if (actual && Array.isArray(obj[actual])) {
                return obj[actual];
            }
        }
        return null;
    }

    function readCount(entry) {
        if (!entry || typeof entry !== 'object') return 0;
        const fields = ['count', 'total', 'amount', 'quantity', 'stack_size', 'stacksize', 'missing'];
        for (const key of fields) {
            const n = Number(getField(entry, key));
            if (Number.isFinite(n) && n > 0) return Math.floor(n);
        }
        return 0;
    }

    function readLabel(entry) {
        if (!entry || typeof entry !== 'object') return '';
        const fields = [
            'id', 'item', 'name', 'itemid', 'item_id', 'registry_name', 'registryname',
            'material', 'block', 'stack'
        ];
        for (const key of fields) {
            const val = getField(entry, key);
            if (val != null && String(val).trim() !== '') return String(val).trim();
        }
        return '';
    }

    function pushEntry(bucket, label, count, source) {
        const qty = Math.floor(Number(count) || 0);
        if (!label || qty <= 0) return;
        bucket.push({
            label: String(label).trim(),
            count: qty,
            source: source || 'json'
        });
    }

    function collectFromArray(arr, bucket, source) {
        if (!Array.isArray(arr)) return;
        arr.forEach((entry) => {
            if (typeof entry === 'string') {
                const parts = entry.trim().split(/\s+/);
                if (parts.length >= 2) {
                    pushEntry(bucket, parts.slice(0, -1).join(' '), parts[parts.length - 1], source);
                }
                return;
            }
            pushEntry(bucket, readLabel(entry), readCount(entry), source);
        });
    }

    function collectFromMap(map, bucket, source) {
        if (!map || typeof map !== 'object' || Array.isArray(map)) return;
        Object.keys(map).forEach((key) => {
            const val = map[key];
            if (typeof val === 'number' || typeof val === 'string') {
                pushEntry(bucket, key, val, source);
                return;
            }
            if (val && typeof val === 'object') {
                pushEntry(bucket, readLabel(val) || key, readCount(val) || Number(val.count), source);
            }
        });
    }

    function extractRawEntries(data) {
        const bucket = [];
        if (data == null) return bucket;

        if (Array.isArray(data)) {
            collectFromArray(data, bucket, 'array');
            return bucket;
        }

        if (typeof data !== 'object') return bucket;

        const materialsArray = findArrayField(
            data,
            'materials', 'items', 'entries', 'blocks', 'stacks', 'material_list', 'materiallist'
        );
        if (materialsArray) {
            collectFromArray(materialsArray, bucket, 'materials');
        }

        if (!bucket.length && data.data && typeof data.data === 'object') {
            return extractRawEntries(data.data);
        }

        if (!bucket.length) {
            const numericKeys = Object.keys(data).filter((k) => {
                const v = data[k];
                return typeof v === 'number' || (typeof v === 'string' && /^\d+$/.test(v.trim()));
            });
            if (numericKeys.length >= 1 && numericKeys.length >= Object.keys(data).length * 0.6) {
                numericKeys.forEach((k) => pushEntry(bucket, k, data[k], 'map'));
            }
        }

        return bucket;
    }

    function mergeRawEntries(rawEntries) {
        const merged = new Map();
        rawEntries.forEach((entry) => {
            const id = normalizeMaterialId(entry.label);
            const key = id || normalizeLookupKey(entry.label);
            if (!key) return;
            const prev = merged.get(key);
            if (prev) {
                prev.count += entry.count;
                if (!prev.itemId && id) prev.itemId = id;
                if (!prev.label) prev.label = entry.label;
            } else {
                merged.set(key, {
                    label: entry.label,
                    itemId: id,
                    count: entry.count
                });
            }
        });
        return [...merged.values()];
    }

    function parseLitematicaMaterialJson(data) {
        const listName = data && typeof data === 'object' && !Array.isArray(data)
            ? (getField(data, 'name', 'title', 'schematic', 'schematic_name', 'schematicname') || '')
            : '';
        const rawEntries = extractRawEntries(data);
        const materials = mergeRawEntries(rawEntries);
        return {
            listName: String(listName || '').trim(),
            materials,
            rawCount: rawEntries.length
        };
    }

    function parseLitematicaMaterialText(text) {
        const lines = String(text || '').split(/\r?\n/);
        const bucket = [];
        let inTable = false;
        lines.forEach((line) => {
            const trimmed = line.trim();
            if (!trimmed) return;
            if (/^\|?\s*item\s*\|/i.test(trimmed) || /^\+\-+\+/.test(trimmed)) {
                inTable = true;
                return;
            }
            if (!inTable) {
                const plain = trimmed.match(/^([a-z0-9:_-]+(?:\s+[a-z0-9:_-]+)*)\s+(\d+)\s*$/i);
                if (plain) pushEntry(bucket, plain[1], plain[2], 'text');
                return;
            }
            if (!trimmed.includes('|')) return;
            const parts = trimmed.split('|').map((p) => p.trim()).filter(Boolean);
            if (parts.length < 2) return;
            if (/^item$/i.test(parts[0]) || /^[-+]+$/.test(parts[0])) return;
            const total = Number(parts[1]);
            const missing = parts.length >= 3 ? Number(parts[2]) : total;
            const count = Number.isFinite(missing) && missing > 0 ? missing : total;
            pushEntry(bucket, parts[0], count, 'table');
        });
        return {
            listName: '',
            materials: mergeRawEntries(bucket),
            rawCount: bucket.length
        };
    }

    function parseLitematicaMaterialFile(text, fileName) {
        const trimmed = String(text || '').trim();
        if (!trimmed) {
            throw new Error('文件为空。');
        }
        const lowerName = String(fileName || '').toLowerCase();
        if (lowerName.endsWith('.json') || trimmed.startsWith('{') || trimmed.startsWith('[')) {
            let data;
            try {
                data = JSON.parse(trimmed);
            } catch (error) {
                throw new Error('JSON 解析失败：' + (error.message || error));
            }
            return parseLitematicaMaterialJson(data);
        }
        return parseLitematicaMaterialText(trimmed);
    }

    return {
        normalizeMaterialId,
        normalizeLookupKey,
        parseLitematicaMaterialJson,
        parseLitematicaMaterialText,
        parseLitematicaMaterialFile
    };
}));
