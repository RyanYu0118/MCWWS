/**
 * 建造工具模块 — 材料清单导入与投影粘贴
 */
document.addEventListener('DOMContentLoaded', async () => {
    if (window.mcLangReady) {
        await window.mcLangReady;
    }

    const cart = window.MCWWS_ShopCart;
    if (!cart) {
        console.error('[建造工具] shop-cart-shared.js 未加载');
        return;
    }

    const deps = cart.getLitematicaDeps();
    window.MCWWS_LitematicaDeps = deps;
    window.MCWWS_BuildPasteDeps = deps;

    document.getElementById('buildMaterialImportCard')?.addEventListener('click', () => {
        window.MCWWS_LitematicaImport?.open?.();
    });
    document.getElementById('buildPasteCard')?.addEventListener('click', () => {
        window.MCWWS_BuildPasteImport?.open?.();
    });

    await cart.init();

    if (window.MCWWS_AUTH) {
        void window.MCWWS_AUTH.init();
    }
});
