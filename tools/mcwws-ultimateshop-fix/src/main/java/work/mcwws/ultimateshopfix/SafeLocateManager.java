package work.mcwws.ultimateshopfix;

import cn.superiormc.ultimateshop.managers.LocateManager;
import org.bukkit.inventory.ItemStack;

/**
 * UltimateShop 用 {@link ItemStack#getTranslationKey()} 查 zh_cn.json。
 * Paper 26.2 上商店配置生成的物品可能没有 craftDelegate，这里先转成 CraftItemStack。
 * 查不到时插件会退回 {@code <lang:键名>}，聊天里被压成裸键名，所以这里自己再查一遍语言文件。
 */
public final class SafeLocateManager extends LocateManager {

    @Override
    public String getLocateName(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "";
        }
        ItemStack safe = ItemStacks.asCraft(item);
        String vanilla = null;
        try {
            vanilla = super.getLocateName(safe);
        } catch (Throwable ignored) {
        }
        if (isDisplayable(vanilla)) {
            return vanilla;
        }
        for (String key : ItemStacks.translationKeys(item, safe)) {
            String name = LocaleNames.lookup(key);
            if (name != null) {
                return name;
            }
        }
        return ItemStacks.fallbackName(item);
    }

    /**
     * 裸翻译键（{@code block.minecraft.oak_sign}）和未解析的 {@code <lang:...>} 都不能直接发给玩家。
     */
    private static boolean isDisplayable(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        if (name.startsWith("<lang:")) {
            return false;
        }
        return !name.matches("[a-z0-9_]+\\.[a-z0-9_]+\\.[a-z0-9_./]+");
    }
}
