package work.mcwws.axiomsurvival;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.EnderChest;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 容器方块与其内容物：堆叠/粘贴会复制 NBT，计费必须把里面的物品算进去；
 * Slimefun 与带自定义属性修饰符的物品禁止复制。
 */
final class ContainerContents {

    static final String BLOCKED_SLIMEFUN = "slimefun";
    static final String BLOCKED_ATTRIBUTES = "attributes";

    record Scan(Map<String, Long> counts, String blocked) {
        static Scan empty() {
            return new Scan(Map.of(), null);
        }

        boolean isBlocked() {
            return blocked != null;
        }

        boolean hasItems() {
            return !counts.isEmpty();
        }
    }

    private ContainerContents() {
    }

    static Scan fromWorld(Block block) {
        if (block == null || block.getType().isAir()) {
            return Scan.empty();
        }
        BlockState state;
        try {
            state = block.getState();
        } catch (RuntimeException ex) {
            return Scan.empty();
        }
        if (state instanceof EnderChest) {
            return Scan.empty();
        }
        List<ItemStack> items = new ArrayList<>();
        if (state instanceof Chest chest) {
            collect(chest.getBlockInventory().getContents(), items);
        } else if (state instanceof Container container) {
            collect(container.getInventory().getContents(), items);
        } else if (state instanceof InventoryHolder holder) {
            collect(holder.getInventory().getContents(), items);
        }
        return fromStacks(items);
    }

    static Scan fromNbt(Object compoundTag, World world) {
        return fromStacks(NbtItems.fromBlockEntity(compoundTag, world));
    }

    static Scan fromStacks(Iterable<ItemStack> stacks) {
        Map<String, Long> counts = new HashMap<>();
        String blocked = collectStacks(stacks, counts, 0);
        if (counts.isEmpty() && blocked == null) {
            return Scan.empty();
        }
        return new Scan(Map.copyOf(counts), blocked);
    }

    static boolean isForbidden(ItemStack stack) {
        return forbiddenReason(stack) != null;
    }

    static String forbiddenReason(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        if (isSlimefun(stack)) {
            return BLOCKED_SLIMEFUN;
        }
        ItemMeta meta = stack.hasItemMeta() ? stack.getItemMeta() : null;
        if (meta != null && hasCustomAttributeModifiers(meta)) {
            return BLOCKED_ATTRIBUTES;
        }
        return null;
    }

    private static String collectStacks(Iterable<ItemStack> stacks, Map<String, Long> counts, int depth) {
        if (stacks == null || depth > 8) {
            return null;
        }
        for (ItemStack stack : stacks) {
            String blocked = collectStack(stack, counts, depth);
            if (blocked != null) {
                return blocked;
            }
        }
        return null;
    }

    private static String collectStack(ItemStack stack, Map<String, Long> counts, int depth) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        String forbidden = forbiddenReason(stack);
        if (forbidden != null) {
            return forbidden;
        }
        String id = PriceCatalog.normalize(stack.getType().name().toLowerCase());
        counts.merge(id, (long) Math.max(stack.getAmount(), 1), Long::sum);
        ItemMeta meta = stack.hasItemMeta() ? stack.getItemMeta() : null;
        if (meta instanceof BlockStateMeta blockMeta && blockMeta.hasBlockState()) {
            BlockState nested = blockMeta.getBlockState();
            if (nested instanceof InventoryHolder holder) {
                String blocked = collectStacks(
                        java.util.Arrays.asList(holder.getInventory().getContents()),
                        counts,
                        depth + 1
                );
                if (blocked != null) {
                    return blocked;
                }
            }
        }
        if (meta instanceof BundleMeta bundle) {
            return collectStacks(bundle.getItems(), counts, depth + 1);
        }
        return null;
    }

    private static void collect(ItemStack[] contents, List<ItemStack> out) {
        if (contents == null) {
            return;
        }
        for (ItemStack stack : contents) {
            if (stack != null && !stack.getType().isAir()) {
                out.add(stack);
            }
        }
    }

    private static boolean isSlimefun(ItemStack stack) {
        try {
            Class<?> type = Class.forName("io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem");
            Object item = type.getMethod("getByItem", ItemStack.class).invoke(null, stack);
            if (item != null) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        if (!stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        for (NamespacedKey key : meta.getPersistentDataContainer().getKeys()) {
            if ("slimefun".equalsIgnoreCase(key.getNamespace())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 原版武器护甲自带的默认攻击/护甲分量不算；只有物品上另写的 AttributeModifiers 才拦截。
     */
    private static boolean hasCustomAttributeModifiers(ItemMeta meta) {
        try {
            if (!meta.hasAttributeModifiers()) {
                return false;
            }
            var modifiers = meta.getAttributeModifiers();
            return modifiers != null && !modifiers.isEmpty();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /** 从 Axiom {@code CompoundTag} 里抽出方块实体物品（箱子/潜影盒/熔炉等）。 */
    static final class NbtItems {
        private static volatile Method asBukkitCopy;
        private static volatile Method parseItem;
        private static volatile boolean parseResolved;

        private NbtItems() {
        }

        static List<ItemStack> fromBlockEntity(Object compoundTag, World world) {
            if (compoundTag == null) {
                return List.of();
            }
            Object registries = registryAccess(world);
            List<ItemStack> out = new ArrayList<>();
            collectList(compoundTag, "Items", registries, out);
            collectList(compoundTag, "Inventory", registries, out);
            collectOne(compoundTag, "item", registries, out);
            collectOne(compoundTag, "RecordItem", registries, out);
            collectOne(compoundTag, "Book", registries, out);
            return out;
        }

        private static void collectList(Object tag, String key, Object registries, List<ItemStack> out) {
            Object list = readList(tag, key);
            if (list == null) {
                return;
            }
            int size;
            try {
                size = (int) list.getClass().getMethod("size").invoke(list);
            } catch (ReflectiveOperationException ex) {
                return;
            }
            for (int i = 0; i < size; i++) {
                Object entry = listEntry(list, i);
                ItemStack stack = toBukkit(entry, registries);
                if (stack != null) {
                    out.add(stack);
                }
            }
        }

        private static void collectOne(Object tag, String key, Object registries, List<ItemStack> out) {
            Object nested = readCompound(tag, key);
            ItemStack stack = toBukkit(nested, registries);
            if (stack != null) {
                out.add(stack);
            }
        }

        private static Object registryAccess(World world) {
            if (world == null) {
                return null;
            }
            try {
                Object handle = world.getClass().getMethod("getHandle").invoke(world);
                return handle.getClass().getMethod("registryAccess").invoke(handle);
            } catch (ReflectiveOperationException ex) {
                return null;
            }
        }

        private static Object readList(Object tag, String key) {
            for (String name : new String[]{"getListOrEmpty", "getList"}) {
                try {
                    for (Method method : tag.getClass().getMethods()) {
                        if (!method.getName().equals(name)) {
                            continue;
                        }
                        Class<?>[] params = method.getParameterTypes();
                        Object result;
                        if (params.length == 1 && params[0] == String.class) {
                            result = method.invoke(tag, key);
                        } else if (params.length == 2 && params[0] == String.class) {
                            result = method.invoke(tag, key, 10);
                        } else {
                            continue;
                        }
                        if (result == null) {
                            return null;
                        }
                        if (result instanceof java.util.Optional<?> optional) {
                            return optional.orElse(null);
                        }
                        try {
                            if ((int) result.getClass().getMethod("size").invoke(result) <= 0) {
                                return null;
                            }
                        } catch (ReflectiveOperationException ignored) {
                        }
                        return result;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
            return null;
        }

        private static Object readCompound(Object tag, String key) {
            try {
                Method contains = tag.getClass().getMethod("contains", String.class);
                if (!(boolean) contains.invoke(tag, key)) {
                    return null;
                }
            } catch (ReflectiveOperationException ignored) {
            }
            try {
                Object result = tag.getClass().getMethod("getCompound", String.class).invoke(tag, key);
                if (result instanceof java.util.Optional<?> optional) {
                    return optional.orElse(null);
                }
                return result;
            } catch (ReflectiveOperationException ex) {
                return null;
            }
        }

        private static Object listEntry(Object list, int index) {
            try {
                return list.getClass().getMethod("getCompound", int.class).invoke(list, index);
            } catch (ReflectiveOperationException ignored) {
            }
            try {
                Object result = list.getClass().getMethod("get", int.class).invoke(list, index);
                if (result instanceof java.util.Optional<?> optional) {
                    return optional.orElse(null);
                }
                return result;
            } catch (ReflectiveOperationException ex) {
                return null;
            }
        }

        private static ItemStack toBukkit(Object itemTag, Object registries) {
            if (itemTag == null) {
                return null;
            }
            Object nms = parseNms(itemTag, registries);
            if (nms == null) {
                return null;
            }
            try {
                Method copy = bukkitCopy();
                if (copy == null) {
                    return null;
                }
                Object bukkit = copy.invoke(null, nms);
                if (bukkit instanceof ItemStack stack && stack.getType() != Material.AIR) {
                    return stack;
                }
            } catch (ReflectiveOperationException ignored) {
            }
            return null;
        }

        private static Object parseNms(Object itemTag, Object registries) {
            Method parse = itemParse();
            if (parse == null) {
                return null;
            }
            try {
                Class<?>[] params = parse.getParameterTypes();
                Object result;
                if (params.length == 2 && registries != null && params[0].isInstance(registries)) {
                    result = parse.invoke(null, registries, itemTag);
                } else if (params.length == 2 && registries != null && params[1].isInstance(registries)) {
                    result = parse.invoke(null, itemTag, registries);
                } else if (params.length == 1) {
                    result = parse.invoke(null, itemTag);
                } else {
                    return null;
                }
                if (result instanceof java.util.Optional<?> optional) {
                    return optional.orElse(null);
                }
                return result;
            } catch (ReflectiveOperationException ex) {
                return null;
            }
        }

        private static Method itemParse() {
            if (parseResolved) {
                return parseItem;
            }
            synchronized (NbtItems.class) {
                if (parseResolved) {
                    return parseItem;
                }
                try {
                    Class<?> nmsItem = Class.forName("net.minecraft.world.item.ItemStack");
                    Class<?> compound = Class.forName("net.minecraft.nbt.CompoundTag");
                    Method found = null;
                    for (String name : new String[]{"parseOptional", "parse", "of"}) {
                        for (Method method : nmsItem.getMethods()) {
                            if (!method.getName().equals(name) || method.getParameterCount() < 1) {
                                continue;
                            }
                            boolean hasCompound = false;
                            for (Class<?> param : method.getParameterTypes()) {
                                if (param.isAssignableFrom(compound) || compound.isAssignableFrom(param)) {
                                    hasCompound = true;
                                    break;
                                }
                            }
                            if (hasCompound) {
                                found = method;
                                break;
                            }
                        }
                        if (found != null) {
                            break;
                        }
                    }
                    parseItem = found;
                } catch (ClassNotFoundException ignored) {
                    parseItem = null;
                }
                parseResolved = true;
                return parseItem;
            }
        }

        private static Method bukkitCopy() {
            Method cached = asBukkitCopy;
            if (cached != null) {
                return cached;
            }
            synchronized (NbtItems.class) {
                if (asBukkitCopy != null) {
                    return asBukkitCopy;
                }
                try {
                    Class<?> craft = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
                    Class<?> nmsItem = Class.forName("net.minecraft.world.item.ItemStack");
                    asBukkitCopy = craft.getMethod("asBukkitCopy", nmsItem);
                } catch (ReflectiveOperationException ignored) {
                    asBukkitCopy = null;
                }
                return asBukkitCopy;
            }
        }
    }
}
