package work.mcwws.ultimateshopfix;

import cn.superiormc.ultimateshop.managers.LocateManager;
import org.bukkit.inventory.ItemStack;

/**
 * UltimateShop 用 {@link ItemStack#getTranslationKey()} 查 zh_cn.json。
 * Paper 26.2 上商店配置生成的物品可能没有 craftDelegate，这里先转成 CraftItemStack。
 */
public final class SafeLocateManager extends LocateManager {

    @Override
    public String getLocateName(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "";
        }
        ItemStack safe = ItemStacks.asCraft(item);
        try {
            return super.getLocateName(safe);
        } catch (Throwable ignored) {
            return ItemStacks.fallbackName(item);
        }
    }
}
