package work.mcwws.immersivecreative;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class CreativeSlotListener {

    private final McwwsImmersiveCreativePlugin plugin;
    private final Map<UUID, Map<Material, Integer>> credit = new ConcurrentHashMap<>();
    private PacketAdapter adapter;

    public CreativeSlotListener(McwwsImmersiveCreativePlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        final McwwsImmersiveCreativePlugin host = plugin;
        adapter = new PacketAdapter(host, ListenerPriority.HIGH, PacketType.Play.Client.SET_CREATIVE_SLOT) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.isCancelled()) {
                    return;
                }
                Player player = event.getPlayer();
                if (player == null) {
                    return;
                }
                boolean enabled = host.state().isEnabled(player);
                if (host.debug()) {
                    host.getLogger().info("[debug] 收到 SET_CREATIVE_SLOT: " + player.getName()
                            + " enabled=" + enabled + " mode=" + player.getGameMode());
                }
                if (!enabled) {
                    return;
                }
                GameMode mode = player.getGameMode();
                if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
                    return;
                }
                event.setCancelled(true);
                int slot = readSlot(event.getPacket());
                ItemStack item = readItem(event.getPacket());
                host.getServer().getScheduler().runTask(host, () -> handle(player, slot, item));
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(adapter);
        plugin.getLogger().info("已通过 ProtocolLib 注册创造槽位监听（SET_CREATIVE_SLOT）。");
    }

    /** 玩家在创造栏里拿在手上或丢掉的物品：服务端已从背包扣掉，记账以便原样放回时不再收费。 */
    public void clearCredit(UUID uuid) {
        credit.remove(uuid);
    }

    /** 客户端模组通过自建通道上报的槽位变更，与 ProtocolLib 路径共用同一套计费逻辑。 */
    public void applyFromClient(Player player, int slot, ItemStack item) {
        handle(player, slot, item);
    }

    public void unregister() {
        if (adapter != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(adapter);
            adapter = null;
        }
    }

    private void handle(Player player, int slot, ItemStack incoming) {
        if (!player.isOnline() || !plugin.state().isEnabled(player)) {
            return;
        }
        if (slot == SLOT_UNKNOWN) {
            plugin.getLogger().warning("无法解析创造槽位包的槽位号，已忽略。");
            return;
        }
        // 创造栏一打开，客户端会把整个背包逐格回报一遍。这些回报只是服务端刚下发的内容，
        // 照写一遍不但白费，还会把服务端的物品换成客户端副本，所以内容一致时直接跳过。
        if (isUnchanged(player, slot, incoming)) {
            return;
        }
        Snapshot before = Snapshot.capture(player);
        try {
            applyChange(player, slot, incoming);
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

        // 客户端把物品拿在光标上时，服务端只看到槽位被清空。先把这些记成额度，
        // 等它落到另一个槽位再抵扣，否则单纯挪动位置也会被当成新买一份。
        Map<Material, Integer> nextCredit = new EnumMap<>(Material.class);
        nextCredit.putAll(credit.getOrDefault(player.getUniqueId(), Map.of()));
        removed.forEach((material, amount) -> nextCredit.merge(material, amount, Integer::sum));

        Map<Material, Integer> billable = new EnumMap<>(Material.class);
        for (Map.Entry<Material, Integer> entry : gained.entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();
            int fromCredit = Math.min(nextCredit.getOrDefault(material, 0), amount);
            if (fromCredit > 0) {
                int rest = nextCredit.get(material) - fromCredit;
                if (rest > 0) {
                    nextCredit.put(material, rest);
                } else {
                    nextCredit.remove(material);
                }
            }
            if (amount > fromCredit) {
                billable.put(material, amount - fromCredit);
            }
        }

        if (plugin.debug()) {
            plugin.getLogger().info("[debug] slot=" + slot + " 取得=" + gained + " 放回=" + removed
                    + " 计费=" + billable + " 余额度=" + nextCredit);
        }

        double total = 0D;
        int amountSum = 0;
        for (Map.Entry<Material, Integer> entry : billable.entrySet()) {
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
            total += offer.unitBuyPrice() * amount;
            amountSum += amount;
        }

        if (total > 0D && plugin.economy().getBalance(player) + 1e-6 < total) {
            before.restore(player);
            player.updateInventory();
            plugin.send(player, "messages.cannot-afford", "price", EconomyHook.format(total));
            return;
        }
        if (total > 0D && !plugin.economy().withdraw(player, total)) {
            before.restore(player);
            player.updateInventory();
            plugin.send(player, "messages.cannot-afford", "price", EconomyHook.format(total));
            return;
        }

        if (nextCredit.isEmpty()) {
            credit.remove(player.getUniqueId());
        } else {
            credit.put(player.getUniqueId(), nextCredit);
        }

        if (total > 0D) {
            plugin.send(player, "messages.purchased",
                    "amount", String.valueOf(amountSum),
                    "price", EconomyHook.format(total));
        }
    }

    /** 槽位内容与服务端现状完全一致（含 NBT 组件与数量）时为真。 */
    private static boolean isUnchanged(Player player, int slot, ItemStack incoming) {
        if (slot < 0) {
            return false;
        }
        ItemStack current = currentAt(player, slot);
        boolean currentEmpty = current == null || current.getType().isAir();
        boolean incomingEmpty = incoming == null || incoming.getType().isAir();
        if (currentEmpty || incomingEmpty) {
            return currentEmpty && incomingEmpty;
        }
        return current.getAmount() == incoming.getAmount() && current.isSimilar(incoming);
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

    private void applyChange(Player player, int slot, ItemStack incoming) {
        ItemStack stack = incoming == null ? new ItemStack(Material.AIR) : incoming.clone();
        if (slot < 0) {
            // 本服禁用创造栏的销毁/丢弃：物品早已在清空槽位时记入额度，这里不再生成掉落物。
            return;
        }
        PlayerInventory inv = player.getInventory();
        ItemStack toSet = emptyIfAir(stack);
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

    /** 读不出槽位时不能当成 -1，那会被误判为「丢弃」。 */
    private static final int SLOT_UNKNOWN = Integer.MIN_VALUE;

    private static int readSlot(PacketContainer packet) {
        try {
            if (packet.getShorts().size() > 0) {
                return packet.getShorts().read(0);
            }
        } catch (Exception ignored) {
        }
        try {
            if (packet.getIntegers().size() > 0) {
                return packet.getIntegers().read(0);
            }
        } catch (Exception ignored) {
        }
        return SLOT_UNKNOWN;
    }

    private static ItemStack readItem(PacketContainer packet) {
        try {
            if (packet.getItemModifier().size() > 0) {
                ItemStack stack = packet.getItemModifier().read(0);
                return stack == null ? new ItemStack(Material.AIR) : stack;
            }
        } catch (Exception ignored) {
        }
        return new ItemStack(Material.AIR);
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
