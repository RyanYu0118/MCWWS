package work.mcwws.ultimateshopfix;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Paper 26.2 里 {@code new ItemStack(Material)} 可能没有 craftDelegate，
 * 调用 {@link ItemStack#getTranslationKey()} 会 NPE。先转成 CraftItemStack。
 */
final class ItemStacks {

    private static Method asCraftCopy;
    private static boolean craftCopyResolved;
    private static boolean loggedFailure;

    private ItemStacks() {
    }

    static ItemStack asCraft(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return item;
        }
        ItemStack crafted = viaCraftCopy(item);
        if (crafted != null && canReadTranslationKey(crafted)) {
            return crafted;
        }
        ItemStack factory = viaItemFactory(item);
        if (factory != null && canReadTranslationKey(factory)) {
            return factory;
        }
        return item;
    }

    /**
     * 同一个物品在不同版本里可能挂在 item.* 或 block.* 键上，按优先级全试一遍。
     */
    static List<String> translationKeys(ItemStack original, ItemStack safe) {
        Set<String> keys = new LinkedHashSet<>();
        addKey(keys, () -> safe == null ? null : safe.getTranslationKey());
        addKey(keys, () -> original.getType().getItemTranslationKey());
        addKey(keys, () -> original.getType().isBlock() ? original.getType().getBlockTranslationKey() : null);
        return new ArrayList<>(keys);
    }

    static String fallbackName(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "";
        }
        String raw = item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        if (raw.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private static void addKey(Set<String> keys, Supplier<String> supplier) {
        try {
            String key = supplier.get();
            if (key != null && !key.isBlank()) {
                keys.add(key);
            }
        } catch (Throwable ignored) {
        }
    }

    static boolean canReadTranslationKey(ItemStack item) {
        if (item == null) {
            return false;
        }
        try {
            item.getTranslationKey();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static ItemStack viaCraftCopy(ItemStack item) {
        resolveCraftCopy();
        if (asCraftCopy == null) {
            return null;
        }
        try {
            Object copy = asCraftCopy.invoke(null, item);
            return copy instanceof ItemStack stack ? stack : null;
        } catch (Throwable t) {
            logOnce("CraftItemStack.asCraftCopy 失败", t);
            return null;
        }
    }

    private static ItemStack viaItemFactory(ItemStack item) {
        try {
            String key = item.getType().getKey().toString();
            ItemStack created = Bukkit.getItemFactory().createItemStack(key);
            created.setAmount(Math.max(1, item.getAmount()));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                created.setItemMeta(meta);
            }
            return created;
        } catch (Throwable t) {
            logOnce("ItemFactory.createItemStack 失败", t);
            return null;
        }
    }

    private static void resolveCraftCopy() {
        if (craftCopyResolved) {
            return;
        }
        craftCopyResolved = true;
        try {
            Class<?> clazz = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
            asCraftCopy = clazz.getMethod("asCraftCopy", ItemStack.class);
        } catch (Throwable t) {
            logOnce("未找到 CraftItemStack.asCraftCopy", t);
        }
    }

    private static void logOnce(String message, Throwable t) {
        if (loggedFailure) {
            return;
        }
        loggedFailure = true;
        McwwsUltimateShopFixPlugin plugin = McwwsUltimateShopFixPlugin.getInstance();
        if (plugin != null) {
            plugin.getLogger().log(Level.WARNING, message, t);
        }
    }
}
