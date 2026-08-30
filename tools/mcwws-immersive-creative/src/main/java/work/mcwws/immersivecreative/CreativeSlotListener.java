package work.mcwws.immersivecreative;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

public final class CreativeSlotListener {

    /** 读不出槽位时不能当成 -1，那会被误判为「丢弃」。 */
    private static final int SLOT_UNKNOWN = Integer.MIN_VALUE;
    /** 只同步光标、不动任何槽位。 */
    public static final int SLOT_CURSOR_ONLY = -2;
    /** 客户端把物品丢到界面外。 */
    public static final int SLOT_DROP = -1;

    private final McwwsImmersiveCreativePlugin plugin;
    private PacketAdapter adapter;

    public CreativeSlotListener(McwwsImmersiveCreativePlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        final McwwsImmersiveCreativePlugin host = plugin;
        // 客户端模组会取消原版发包，改走自建通道上报（那条路径才带光标）。这里只做兜底拦截，
        // 防止没装模组或版本不符的客户端把生存背包改成创造语义，不参与任何计费。
        adapter = new PacketAdapter(host, ListenerPriority.HIGH, PacketType.Play.Client.SET_CREATIVE_SLOT) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.isCancelled()) {
                    return;
                }
                Player player = event.getPlayer();
                if (player == null || !host.state().isEnabled(player)) {
                    return;
                }
                GameMode mode = player.getGameMode();
                if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
                    return;
                }
                event.setCancelled(true);
                if (host.debug()) {
                    host.getLogger().info("[debug] 丢弃原版 SET_CREATIVE_SLOT（应由模组通道上报）: " + player.getName());
                }
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(adapter);
        plugin.getLogger().info("已注册创造槽位兜底拦截（SET_CREATIVE_SLOT）。");
    }

    public void unregister() {
        if (adapter != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(adapter);
            adapter = null;
        }
    }

    /** 客户端模组上报的一次界面操作：槽位新内容 + 操作后的光标内容。 */
    public void applyFromClient(Player player, int slot, ItemStack item, ItemStack carried) {
        handle(player, slot, item, carried);
    }

    private void handle(Player player, int slot, ItemStack incoming, ItemStack carried) {
        if (!player.isOnline() || !plugin.state().isEnabled(player)) {
            return;
        }
        if (slot == SLOT_UNKNOWN) {
            plugin.getLogger().warning("无法解析创造槽位号，已忽略。");
            return;
        }
        if (slot == SLOT_DROP) {
            // 沉浸式创造禁止把物品丢出界面：客户端已做本地预测，这里回滚并刷新即可。
            player.updateInventory();
            plugin.send(player, "messages.drop-denied");
            return;
        }
        // 创造栏一打开，客户端会把整个背包逐格回报一遍。这些回报只是服务端刚下发的内容，
        // 照写一遍不但白费，还会把服务端的物品换成客户端副本，所以内容一致时直接跳过。
        if (isUnchanged(player, slot, incoming) && isSameStack(player.getItemOnCursor(), carried)) {
            return;
        }

        // 一次操作最多只可能让「原光标内容」和「原槽位内容」消失，特殊物品在此拦下，
        // 避免附魔装备、Slimefun 物品被按原版材质价贱卖或误毁。
        ItemStack priorCursor = player.getItemOnCursor();
        ItemStack priorSlot = currentAt(player, slot);

        Snapshot before = Snapshot.capture(player);
        try {
            applyChange(player, slot, incoming, carried);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "应用创造栏槽位失败", ex);
            before.restore(player);
            player.updateInventory();
            return;
        }

        Map<Material, Integer> beforeCounts = before.counts();
        Map<Material, Integer> afterCounts = Snapshot.capture(player).counts();
        Map<Material, Integer> gained = positiveDelta(beforeCounts, afterCounts);
        Map<Material, Integer> removed = positiveDelta(afterCounts, beforeCounts);

        if (plugin.debug()) {
            plugin.getLogger().info("[debug] slot=" + slot + " 取得=" + gained + " 卖出=" + removed);
        }

        if (!removed.isEmpty() && (hasCustomData(priorCursor) || hasCustomData(priorSlot))) {
            before.restore(player);
            player.updateInventory();
            plugin.send(player, "messages.special-item");
            return;
        }

        double feeRate = Math.max(plugin.getConfig().getDouble("instant-delivery-fee", 1.0D), 0D);
        double buyTotal = 0D;
        double baseTotal = 0D;
        int boughtCount = 0;
        for (Map.Entry<Material, Integer> entry : gained.entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();
            if (plugin.prices().isBlacklisted(material) || material.isAir()) {
                before.restore(player);
                player.updateInventory();
                plugin.send(player, "messages.denied");
                return;
            }
            ShopOffer offer = plugin.prices().find(material);
            if (offer == null) {
                if (plugin.getConfig().getBoolean("deny-unmapped", true)) {
                    before.restore(player);
                    player.updateInventory();
                    plugin.send(player, "messages.not-buyable");
                    return;
                }
                continue;
            }
            baseTotal += offer.unitBuyPrice() * amount;
            boughtCount += amount;
        }
        buyTotal = baseTotal * (1D + feeRate);

        double sellTotal = 0D;
        int soldCount = 0;
        for (Map.Entry<Material, Integer> entry : removed.entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();
            ShopOffer offer = plugin.prices().find(material);
            if (offer == null || offer.unitSellPrice() <= 0D) {
                // 卖不掉的东西宁可不让它消失，也不要凭空蒸发。
                before.restore(player);
                player.updateInventory();
                plugin.send(player, "messages.not-sellable");
                return;
            }
            sellTotal += offer.unitSellPrice() * amount;
            soldCount += amount;
        }

        double net = buyTotal - sellTotal;
        if (net > 0D) {
            if (plugin.economy().getBalance(player) + 1e-6 < net) {
                before.restore(player);
                player.updateInventory();
                plugin.send(player, "messages.cannot-afford", "price", EconomyHook.format(net));
                return;
            }
            if (!plugin.economy().withdraw(player, net)) {
                before.restore(player);
                player.updateInventory();
                plugin.send(player, "messages.cannot-afford", "price", EconomyHook.format(net));
                return;
            }
        } else if (net < 0D) {
            plugin.economy().deposit(player, -net);
        }

        if (buyTotal > 0D) {
            plugin.send(player, "messages.purchased",
                    "amount", String.valueOf(boughtCount),
                    "price", EconomyHook.format(buyTotal),
                    "base", EconomyHook.format(baseTotal),
                    "fee", EconomyHook.format(buyTotal - baseTotal));
        }
        if (sellTotal > 0D) {
            plugin.send(player, "messages.sold",
                    "amount", String.valueOf(soldCount),
                    "price", EconomyHook.format(sellTotal));
        }
    }

    /** 带自定义名称、附魔、PDC 等数据的物品视为特殊物品，不参与创造栏买卖。 */
    private static boolean hasCustomData(ItemStack stack) {
        return stack != null && !stack.getType().isAir() && stack.hasItemMeta();
    }

    /** 槽位内容与服务端现状完全一致（含 NBT 组件与数量）时为真。 */
    private static boolean isUnchanged(Player player, int slot, ItemStack incoming) {
        if (slot == SLOT_CURSOR_ONLY) {
            return true;
        }
        if (slot < 0) {
            return false;
        }
        return isSameStack(currentAt(player, slot), incoming);
    }

    private static boolean isSameStack(ItemStack a, ItemStack b) {
        boolean aEmpty = a == null || a.getType().isAir();
        boolean bEmpty = b == null || b.getType().isAir();
        if (aEmpty || bEmpty) {
            return aEmpty && bEmpty;
        }
        return a.getAmount() == b.getAmount() && a.isSimilar(b);
    }

    /** 槽位号到背包位置的映射，与 {@link #applyChange} 一一对应。未知槽位返回 null。 */
    private static ItemStack currentAt(Player player, int slot) {
        PlayerInventory inv = player.getInventory();
        if (slot >= 36 && slot <= 44) {
            return inv.getItem(slot - 36);
        }
        if (slot >= 9 && slot <= 35) {
            return inv.getItem(slot);
        }
        return switch (slot) {
            case 5 -> inv.getHelmet();
            case 6 -> inv.getChestplate();
            case 7 -> inv.getLeggings();
            case 8 -> inv.getBoots();
            case 45 -> inv.getItemInOffHand();
            default -> null;
        };
    }

    private void applyChange(Player player, int slot, ItemStack incoming, ItemStack carried) {
        player.setItemOnCursor(emptyIfAir(carried == null ? null : carried.clone()));
        if (slot == SLOT_CURSOR_ONLY) {
            return;
        }
        ItemStack toSet = emptyIfAir(incoming == null ? null : incoming.clone());
        PlayerInventory inv = player.getInventory();
        if (slot >= 36 && slot <= 44) {
            inv.setItem(slot - 36, toSet);
            return;
        }
        if (slot >= 9 && slot <= 35) {
            inv.setItem(slot, toSet);
            return;
        }
        switch (slot) {
            case 5 -> inv.setHelmet(toSet);
            case 6 -> inv.setChestplate(toSet);
            case 7 -> inv.setLeggings(toSet);
            case 8 -> inv.setBoots(toSet);
            case 45 -> inv.setItemInOffHand(toSet);
            default -> {
                if (slot >= 0 && slot < player.getOpenInventory().countSlots()) {
                    player.getOpenInventory().setItem(slot, toSet);
                }
            }
        }
    }

    private static ItemStack emptyIfAir(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return new ItemStack(Material.AIR);
        }
        return stack;
    }

    private static Map<Material, Integer> positiveDelta(Map<Material, Integer> before, Map<Material, Integer> after) {
        Map<Material, Integer> gained = new EnumMap<>(Material.class);
        for (Map.Entry<Material, Integer> entry : after.entrySet()) {
            int delta = entry.getValue() - before.getOrDefault(entry.getKey(), 0);
            if (delta > 0) {
                gained.put(entry.getKey(), delta);
            }
        }
        return gained;
    }

    private record Snapshot(ItemStack[] storage, ItemStack[] extra, ItemStack cursor) {

        static Snapshot capture(Player player) {
            PlayerInventory inv = player.getInventory();
            return new Snapshot(
                    cloneArray(inv.getStorageContents()),
                    cloneArray(inv.getExtraContents()),
                    cloneOrEmpty(player.getItemOnCursor())
            );
        }

        void restore(Player player) {
            PlayerInventory inv = player.getInventory();
            inv.setStorageContents(cloneArray(storage));
            inv.setExtraContents(cloneArray(extra));
            player.setItemOnCursor(cloneOrEmpty(cursor));
        }

        Map<Material, Integer> counts() {
            Map<Material, Integer> map = new EnumMap<>(Material.class);
            add(map, storage);
            add(map, extra);
            add(map, new ItemStack[] { cursor });
            return map;
        }

        private static void add(Map<Material, Integer> map, ItemStack[] items) {
            if (items == null) {
                return;
            }
            for (ItemStack stack : items) {
                if (stack == null || stack.getType().isAir()) {
                    continue;
                }
                map.merge(stack.getType(), stack.getAmount(), Integer::sum);
            }
        }

        private static ItemStack[] cloneArray(ItemStack[] source) {
            if (source == null) {
                return new ItemStack[0];
            }
            ItemStack[] copy = new ItemStack[source.length];
            for (int i = 0; i < source.length; i++) {
                copy[i] = cloneOrEmpty(source[i]);
            }
            return copy;
        }

        private static ItemStack cloneOrEmpty(ItemStack stack) {
            if (stack == null || stack.getType().isAir()) {
                return new ItemStack(Material.AIR);
            }
            return stack.clone();
        }
    }
}
