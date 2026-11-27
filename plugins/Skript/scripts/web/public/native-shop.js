import * as THREE from './vendor/three.module.min.js?v=2';

window.THREE = THREE;
let catalog = [];
let renderQueue = Promise.resolve();

function iconElement(markup) {
    const template = document.createElement('template');
    template.innerHTML = markup.trim();
    return template.content.firstElementChild;
}

function loadImage(url) {
    const img = new Image();
    img.src = url;
    return img.decode().then(() => img);
}

async function canvasPreview(canvas, itemId) {
    const glint = window.McEnchantGlint;
    if (glint.itemHasGlint(itemId)) {
        await glint.loadGlintImage();
        const overlay = document.createElement('canvas');
        overlay.width = canvas.width;
        overlay.height = canvas.height;
        glint.drawGlintOnCanvas(overlay.getContext('2d'), performance.now(), canvas);
        const ctx = canvas.getContext('2d');
        ctx.globalCompositeOperation = 'lighter';
        ctx.drawImage(overlay, 0, 0);
        ctx.globalCompositeOperation = 'source-over';
    }
    return { source: canvas.toDataURL('image/png'), smooth: false };
}

async function captureItem(itemId) {
    if (!/^[a-z0-9_.:-]{1,150}$/i.test(itemId)) return null;
    const icon = window.McItemIcon;
    const slot = iconElement(icon.getIconSlotHtml(itemId, ''));
    let bitmap;
    try {
        bitmap = await icon.capturePreview(slot, itemId);
    } catch {
        // Try the same flat fallback used on the web catalog.
    }
    if (bitmap) {
        if (!window.McEnchantGlint.itemHasGlint(itemId)) return { source: bitmap, smooth: true };
        const img = await loadImage(bitmap);
        const canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        canvas.getContext('2d').drawImage(img, 0, 0);
        return { ...await canvasPreview(canvas, itemId), smooth: true };
    }

    const flat = iconElement(window.getTextureHtml(itemId, ''));
    const img = flat.querySelector('img.item-mc-wiki-invicon');
    if (img) {
        window.initMcWikiInviconImages(flat);
        const finish = Date.now() + 2500;
        while (img.dataset.texReady !== '1' && Date.now() < finish) {
            await new Promise(resolve => setTimeout(resolve, 50));
        }
        if (img.dataset.texReady === '1') {
            return { source: img.src, mirror: img.style.transform === 'scaleX(-1)', smooth: false };
        }
    }
    const canvas = flat.querySelector('canvas.item-tex-anim') || document.createElement('canvas');
    if (!canvas.dataset.itemId) {
        canvas.dataset.itemId = itemId;
        canvas.width = canvas.height = window.McIconConfig.RENDER_SIZE;
    }
    const urls = (canvas.dataset.texUrls || window.flatTextureUrlsForItem(itemId).join('|')).split('|').filter(Boolean);
    document.body.append(canvas);
    try {
        if (!(await window.McTextureAnim.initCanvasFromUrls(canvas, urls))) return null;
        if (window.isMcClockItemId(itemId) || window.isMcMouseCompassItemId(itemId)) {
            for (let i = 0; i < 8; i++) {
                const pixels = canvas.getContext('2d').getImageData(0, 0, canvas.width, canvas.height).data;
                if (pixels.some((value, index) => index % 4 === 3 && value)) break;
                await new Promise(resolve => setTimeout(resolve, 80));
            }
        }
        return canvasPreview(canvas, itemId);
    } finally {
        canvas.remove();
    }
}

await window.mcLangReady;
window.MCWWSNativeShopPage = {
    setCatalog(rows) {
        window.McShopSearch.reset();
        catalog = rows.map(row => ({ id: row.id, name: row.name || window.getChineseName(row.id) }));
        return catalog.map(row => row.name);
    },

    filter(query, letter) {
        const search = window.McShopSearch;
        const normalized = search.normalizeSearchText(query);
        return catalog.filter(item => (!letter || search.getItemSortLetter(item) === letter)
            && search.itemMatchesSearchQuery(item, normalized)).map(item => item.id);
    },

    preview(ticket, itemId) {
        renderQueue = renderQueue.then(async () => {
            const value = await captureItem(itemId).catch(() => null);
            window.MCWWSNativeShop?.previewReady(ticket, JSON.stringify(value));
        }).catch(() => {});
        return true;
    }
};
