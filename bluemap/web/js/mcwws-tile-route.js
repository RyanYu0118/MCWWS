/* 必须在 BlueMap 主模块之前执行。
 * 主模块是 defer 的，会先把出生点一带的瓦片请求发出去。
 * 图层脚本要等地图起来才插入，拦不住这第一批。
 * 正 X、正 Z 正好是行星发动机和默认视角，所以只有那边还显示完整图。
 *
 * 地表图把 Y=320 及以上当成空气，发动机下半截仍会在低清图上留成接近 319 的平顶。
 * 缩到最远时看到的就是这截平顶。这里只改最远两级低清图的显示，不重烘焙。
 */
(function mcwwsInstallTileRoute() {
    if (window.__mcwwsTileRouteInstalled) {
        return;
    }
    window.__mcwwsTileRouteInstalled = true;
    window.__mcwwsPlateau = { tiles: 0, pixels: 0 };
    const STORAGE_KEY = 'mcwws-gis-render-above-320';
    const PLATEAU_CUT = 250;
    const PNG_SIGNATURE = new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10]);
    const CRC_TABLE = new Uint32Array(256);
    for (let n = 0; n < 256; n++) {
        let c = n;
        for (let k = 0; k < 8; k++) {
            c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1);
        }
        CRC_TABLE[n] = c >>> 0;
    }

    function renderAbove320() {
        try {
            return localStorage.getItem(STORAGE_KEY) === '1';
        } catch (err) {
            return false;
        }
    }

    function rewriteWorldTileUrl(url) {
        const text = String(url || '');
        if (renderAbove320() || !text.includes('maps/world/tiles/')) {
            return text;
        }
        if (text.includes('maps/world_surface/tiles/')) {
            return text;
        }
        return text.replace('maps/world/tiles/', 'maps/world_surface/tiles/');
    }

    function isFarSurfacePng(url) {
        return /maps\/world_surface\/tiles\/[23]\//.test(url) && /\.png(?:$|\?)/.test(url);
    }

    function crc32(bytes) {
        let c = 0xffffffff;
        for (let i = 0; i < bytes.length; i++) {
            c = CRC_TABLE[(c ^ bytes[i]) & 255] ^ (c >>> 8);
        }
        return (c ^ 0xffffffff) >>> 0;
    }

    function pngChunk(type, data) {
        const typeBytes = [type.charCodeAt(0), type.charCodeAt(1), type.charCodeAt(2), type.charCodeAt(3)];
        const body = new Uint8Array(4 + data.length);
        body.set(typeBytes, 0);
        body.set(data, 4);
        const out = new Uint8Array(12 + data.length);
        const view = new DataView(out.buffer);
        view.setUint32(0, data.length);
        out.set(body, 4);
        view.setUint32(8 + data.length, crc32(body));
        return out;
    }

    function concatBytes(parts) {
        let length = 0;
        for (let i = 0; i < parts.length; i++) {
            length += parts[i].length;
        }
        const out = new Uint8Array(length);
        let offset = 0;
        for (let i = 0; i < parts.length; i++) {
            out.set(parts[i], offset);
            offset += parts[i].length;
        }
        return out;
    }

    function paeth(left, up, upLeft) {
        const estimate = left + up - upLeft;
        const distLeft = Math.abs(estimate - left);
        const distUp = Math.abs(estimate - up);
        const distUpLeft = Math.abs(estimate - upLeft);
        if (distLeft <= distUp && distLeft <= distUpLeft) {
            return left;
        }
        if (distUp <= distUpLeft) {
            return up;
        }
        return upLeft;
    }

    async function inflateZlib(bytes) {
        const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream('deflate'));
        return new Uint8Array(await new Response(stream).arrayBuffer());
    }

    async function deflateZlib(bytes) {
        const stream = new Blob([bytes]).stream().pipeThrough(new CompressionStream('deflate'));
        return new Uint8Array(await new Response(stream).arrayBuffer());
    }

    function decodePng(bytes) {
        if (bytes.length < 8 || bytes[0] !== 137 || bytes[1] !== 80) {
            throw new Error('not a png');
        }
        let offset = 8;
        let width = 0;
        let height = 0;
        const idat = [];
        while (offset + 8 <= bytes.length) {
            const length = new DataView(bytes.buffer, bytes.byteOffset + offset, 4).getUint32(0);
            const type = String.fromCharCode(bytes[offset + 4], bytes[offset + 5], bytes[offset + 6], bytes[offset + 7]);
            const data = bytes.subarray(offset + 8, offset + 8 + length);
            if (type === 'IHDR') {
                const view = new DataView(data.buffer, data.byteOffset, data.byteLength);
                width = view.getUint32(0);
                height = view.getUint32(4);
                if (data[8] !== 8 || data[9] !== 6 || data[12] !== 0) {
                    throw new Error('unsupported png');
                }
            } else if (type === 'IDAT') {
                idat.push(data);
            } else if (type === 'IEND') {
                break;
            }
            offset += 12 + length;
        }
        return { width: width, height: height, idat: concatBytes(idat) };
    }

    function unfilter(inflated, width, height) {
        const stride = width * 4;
        const out = new Uint8Array(height * stride);
        let src = 0;
        let previous = new Uint8Array(stride);
        for (let y = 0; y < height; y++) {
            const filter = inflated[src];
            src += 1;
            const row = out.subarray(y * stride, (y + 1) * stride);
            for (let i = 0; i < stride; i++) {
                const value = inflated[src + i];
                const left = i >= 4 ? row[i - 4] : 0;
                const up = previous[i];
                const upLeft = i >= 4 ? previous[i - 4] : 0;
                let decoded = value;
                if (filter === 1) {
                    decoded = value + left;
                } else if (filter === 2) {
                    decoded = value + up;
                } else if (filter === 3) {
                    decoded = value + ((left + up) >> 1);
                } else if (filter === 4) {
                    decoded = value + paeth(left, up, upLeft);
                } else if (filter !== 0) {
                    throw new Error('bad png filter');
                }
                row[i] = decoded & 255;
            }
            src += stride;
            previous = row;
        }
        return out;
    }

    function minBlobFor(url) {
        return /\/tiles\/3\//.test(url) ? 180 : 2000;
    }

    function columnHeightAt(rgba, width, half, x, y) {
        const meta = ((y + half) * width + x) * 4;
        if (rgba[meta + 3] === 0) {
            return null;
        }
        let columnHeight = (rgba[meta + 1] << 8) | rgba[meta + 2];
        if (columnHeight >= 32768) {
            columnHeight -= 65536;
        }
        return columnHeight;
    }

    function engineMask(worldRgba, width, height) {
        const half = height >> 1;
        const cut = new Uint8Array(width * half);
        let count = 0;
        for (let y = 0; y < half; y++) {
            for (let x = 0; x < width; x++) {
                const columnHeight = columnHeightAt(worldRgba, width, half, x, y);
                if (columnHeight !== null && columnHeight >= 320) {
                    cut[y * width + x] = 1;
                    count += 1;
                }
            }
        }
        if (count === 0) {
            return null;
        }
        const source = cut.slice();
        const radius = 12;
        for (let y = 0; y < half; y++) {
            for (let x = 0; x < width; x++) {
                if (!source[y * width + x]) {
                    continue;
                }
                for (let dy = -radius; dy <= radius; dy++) {
                    const ny = y + dy;
                    if (ny < 0 || ny >= half) {
                        continue;
                    }
                    for (let dx = -radius; dx <= radius; dx++) {
                        if (dx * dx + dy * dy > radius * radius) {
                            continue;
                        }
                        const nx = x + dx;
                        if (nx < 0 || nx >= width) {
                            continue;
                        }
                        cut[ny * width + nx] = 1;
                    }
                }
            }
        }
        return cut;
    }

    function flattenPlateau(rgba, width, height, minBlob, presetCut) {
        const half = height >> 1;
        if (half < 2 || width < 2) {
            return 0;
        }
        const cellCount = width * half;
        const cut = presetCut || new Uint8Array(cellCount);
        let cutCount = 0;
        if (!presetCut) {
            for (let y = 0; y < half; y++) {
                for (let x = 0; x < width; x++) {
                    const columnHeight = columnHeightAt(rgba, width, half, x, y);
                    if (columnHeight !== null && columnHeight >= PLATEAU_CUT) {
                        cut[y * width + x] = 1;
                        cutCount += 1;
                    }
                }
            }
        } else {
            for (let i = 0; i < cellCount; i++) {
                if (cut[i]) {
                    cutCount += 1;
                }
            }
        }
        if (cutCount === 0) {
            return 0;
        }
        const seen = new Uint8Array(cellCount);
        if (!presetCut) {
        for (let i = 0; i < cellCount; i++) {
            if (!cut[i] || seen[i]) {
                continue;
            }
            const stack = [i];
            const members = [i];
            seen[i] = 1;
            let head = 0;
            while (head < stack.length) {
                const index = stack[head];
                head += 1;
                const x = index % width;
                const y = (index / width) | 0;
                const neighbors = [];
                if (x > 0) {
                    neighbors.push(index - 1);
                }
                if (x + 1 < width) {
                    neighbors.push(index + 1);
                }
                if (y > 0) {
                    neighbors.push(index - width);
                }
                if (y + 1 < half) {
                    neighbors.push(index + width);
                }
                for (let n = 0; n < neighbors.length; n++) {
                    const next = neighbors[n];
                    if (seen[next] || !cut[next]) {
                        continue;
                    }
                    seen[next] = 1;
                    stack.push(next);
                    members.push(next);
                }
            }
            if (members.length < minBlob) {
                for (let n = 0; n < members.length; n++) {
                    cut[members[n]] = 0;
                }
            }
        }
        }
        const nearest = new Int32Array(cellCount);
        nearest.fill(-1);
        const queue = [];
        for (let i = 0; i < cellCount; i++) {
            if (cut[i]) {
                continue;
            }
            const x = i % width;
            const y = (i / width) | 0;
            const meta = ((y + half) * width + x) * 4;
            if (rgba[meta + 3] === 0) {
                continue;
            }
            if (presetCut) {
                const colorIndex = (y * width + x) * 4;
                const brightness = rgba[colorIndex] + rgba[colorIndex + 1] + rgba[colorIndex + 2];
                if (brightness < 100) {
                    continue;
                }
            }
            nearest[i] = i;
            queue.push(i);
        }
        if (queue.length === 0) {
            return 0;
        }
        let head = 0;
        while (head < queue.length) {
            const index = queue[head];
            head += 1;
            const x = index % width;
            const y = (index / width) | 0;
            const source = nearest[index];
            if (x > 0) {
                spread(index - 1, source);
            }
            if (x + 1 < width) {
                spread(index + 1, source);
            }
            if (y > 0) {
                spread(index - width, source);
            }
            if (y + 1 < half) {
                spread(index + width, source);
            }
        }
        function spread(index, source) {
            if (nearest[index] !== -1 || !cut[index]) {
                return;
            }
            nearest[index] = source;
            queue.push(index);
        }
        let painted = 0;
        for (let i = 0; i < cellCount; i++) {
            if (!cut[i] || nearest[i] < 0) {
                continue;
            }
            const x = i % width;
            const y = (i / width) | 0;
            const source = nearest[i];
            const sx = source % width;
            const sy = (source / width) | 0;
            const color = (y * width + x) * 4;
            const sourceColor = (sy * width + sx) * 4;
            rgba[color] = rgba[sourceColor];
            rgba[color + 1] = rgba[sourceColor + 1];
            rgba[color + 2] = rgba[sourceColor + 2];
            const meta = ((y + half) * width + x) * 4;
            const sourceMeta = ((sy + half) * width + sx) * 4;
            rgba[meta] = rgba[sourceMeta];
            rgba[meta + 1] = rgba[sourceMeta + 1];
            rgba[meta + 2] = rgba[sourceMeta + 2];
            painted += 1;
        }
        return painted;
    }

    async function encodePng(rgba, width, height) {
        const stride = width * 4;
        const raw = new Uint8Array(height * (stride + 1));
        for (let y = 0; y < height; y++) {
            const dest = y * (stride + 1);
            raw[dest] = 0;
            raw.set(rgba.subarray(y * stride, (y + 1) * stride), dest + 1);
        }
        const compressed = await deflateZlib(raw);
        const ihdr = new Uint8Array(13);
        const view = new DataView(ihdr.buffer);
        view.setUint32(0, width);
        view.setUint32(4, height);
        ihdr[8] = 8;
        ihdr[9] = 6;
        return concatBytes([
            PNG_SIGNATURE,
            pngChunk('IHDR', ihdr),
            pngChunk('IDAT', compressed),
            pngChunk('IEND', new Uint8Array(0))
        ]);
    }

    async function flattenPngBytes(bytes, url) {
        const decoded = decodePng(bytes);
        const inflated = await inflateZlib(decoded.idat);
        const rgba = unfilter(inflated, decoded.width, decoded.height);
        let engineCut = null;
        try {
            const worldUrl = String(url).replace('maps/world_surface/tiles/', 'maps/world/tiles/');
            const worldResponse = await origFetch(worldUrl);
            if (worldResponse.ok) {
                const worldDecoded = decodePng(new Uint8Array(await worldResponse.arrayBuffer()));
                if (worldDecoded.width === decoded.width && worldDecoded.height === decoded.height) {
                    const worldRgba = unfilter(await inflateZlib(worldDecoded.idat), worldDecoded.width, worldDecoded.height);
                    engineCut = engineMask(worldRgba, worldDecoded.width, worldDecoded.height);
                }
            }
        } catch (err) {
            engineCut = null;
        }
        const painted = flattenPlateau(rgba, decoded.width, decoded.height, minBlobFor(url), engineCut);
        if (!painted) {
            return null;
        }
        window.__mcwwsPlateau.tiles += 1;
        window.__mcwwsPlateau.pixels += painted;
        return encodePng(rgba, decoded.width, decoded.height);
    }

    function flattenResponse(response, url) {
        if (!response || !response.ok || renderAbove320() || !isFarSurfacePng(url)) {
            return Promise.resolve(response);
        }
        return response.arrayBuffer().then(async (buffer) => {
            const bytes = new Uint8Array(buffer);
            try {
                const flattened = await flattenPngBytes(bytes, url);
                if (!flattened) {
                    return new Response(bytes, { status: 200, headers: { 'Content-Type': 'image/png' } });
                }
                return new Response(flattened, { status: 200, headers: { 'Content-Type': 'image/png' } });
            } catch (err) {
                return new Response(bytes, { status: 200, headers: { 'Content-Type': 'image/png' } });
            }
        });
    }

    const origFetch = window.fetch;
    if (typeof origFetch === 'function') {
        window.fetch = function mcwwsTileFetch(input, init) {
            const original = typeof input === 'string'
                ? input
                : (input && typeof input.url === 'string' ? input.url : '');
            const rewritten = rewriteWorldTileUrl(original);
            let responsePromise;
            if (!original || rewritten === original) {
                responsePromise = origFetch.apply(this, arguments);
            } else {
                const request = typeof input === 'string'
                    ? rewritten
                    : new Request(rewritten, input.clone());
                responsePromise = origFetch.call(this, request, init).then((response) => {
                    if (response.status !== 404 && response.status !== 204) {
                        return response;
                    }
                    const fallback = typeof input === 'string'
                        ? input
                        : new Request(original, input.clone());
                    return origFetch.call(this, fallback, init);
                });
            }
            return responsePromise.then((response) => flattenResponse(response, (response && response.url) || rewritten || original));
        };
    }

    const imgSrc = Object.getOwnPropertyDescriptor(HTMLImageElement.prototype, 'src');
    if (imgSrc && imgSrc.set && imgSrc.get) {
        Object.defineProperty(HTMLImageElement.prototype, 'src', {
            configurable: true,
            enumerable: imgSrc.enumerable,
            get() {
                return imgSrc.get.call(this);
            },
            set(value) {
                const original = String(value || '');
                const rewritten = rewriteWorldTileUrl(original);
                const target = rewritten || original;
                if (!renderAbove320() && isFarSurfacePng(target) && !original.startsWith('blob:')) {
                    const image = this;
                    window.fetch(original).then((response) => {
                        if (!response.ok) {
                            throw new Error('tile');
                        }
                        return response.blob();
                    }).then((blob) => {
                        const objectUrl = URL.createObjectURL(blob);
                        image.addEventListener('load', function mcwwsRevokePlateauUrl() {
                            URL.revokeObjectURL(objectUrl);
                        }, { once: true });
                        imgSrc.set.call(image, objectUrl);
                    }).catch(() => {
                        imgSrc.set.call(image, rewritten);
                    });
                    return;
                }
                if (rewritten !== original) {
                    this.addEventListener('error', function mcwwsImgFallback() {
                        this.removeEventListener('error', mcwwsImgFallback);
                        imgSrc.set.call(this, original);
                    });
                }
                imgSrc.set.call(this, rewritten);
            }
        });
    }
})();
