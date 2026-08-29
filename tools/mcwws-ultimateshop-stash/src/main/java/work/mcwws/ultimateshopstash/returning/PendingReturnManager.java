package work.mcwws.ultimateshopstash.returning;

import org.bukkit.entity.Player;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;
import work.mcwws.ultimateshopstash.util.Chat;
import work.mcwws.ultimateshopstash.util.Messages;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingReturnManager {

    private final McwwsUltimateShopStashPlugin plugin;
    private final ItemReturnService returnService;
    private final Map<String, PendingReturn> pending = new ConcurrentHashMap<>();

    public PendingReturnManager(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
        this.returnService = new ItemReturnService(plugin);
    }

    public String register(Player player, String itemKey, long amount) {
        pruneExpired();
        String token = UUID.randomUUID().toString().replace("-", "");
        pending.put(token, new PendingReturn(
                player.getUniqueId(),
                Messages.normalizeKey(itemKey),
                amount,
                System.currentTimeMillis()));
        return token;
    }

    public void returnBatch(Player player, String token) {
        PendingReturn receipt = pending.get(token);
        if (receipt == null || !receipt.playerId().equals(player.getUniqueId())) {
            Chat.send(player, plugin.messages(), "return-invalid", null);
            return;
        }

        String itemKey = receipt.itemKey();
        long now = System.currentTimeMillis();
        long windowMs = Math.max(1, plugin.exemptDurationSeconds()) * 1000L;
        long cutoff = now - windowMs;

        List<String> matchedTokens = new ArrayList<>();
        long total = 0;
        for (Map.Entry<String, PendingReturn> entry : pending.entrySet()) {
            PendingReturn candidate = entry.getValue();
            if (!candidate.playerId().equals(player.getUniqueId())) {
                continue;
            }
            if (!candidate.itemKey().equals(itemKey)) {
                continue;
            }
            if (candidate.createdAtMs() < cutoff) {
                continue;
            }
            matchedTokens.add(entry.getKey());
            total += candidate.amount();
        }

        // Older chat buttons still work for their own receipt if nothing is in the window.
        if (total <= 0) {
            matchedTokens.add(token);
            total = receipt.amount();
        }

        long stored = plugin.storage().getAmount(player.getUniqueId(), itemKey);
        if (stored < total) {
            if (stored <= 0) {
                Chat.send(player, plugin.messages(), "return-missing", Map.of(
                        "item", Messages.displayMaterial(itemKey)
                ));
                return;
            }
            total = stored;
        }

        ItemReturnService.ReturnResult result = returnService.returnItems(player, itemKey, total);
        if (!result.success()) {
            Chat.send(player, plugin.messages(), "return-full", null);
            return;
        }

        if (!plugin.storage().tryRemove(player.getUniqueId(), itemKey, total)) {
            plugin.getLogger().severe("物品已放回玩家容器，但仓库扣除失败: "
                    + player.getName() + " " + itemKey + " x" + total);
            return;
        }

        for (String matched : matchedTokens) {
            pending.remove(matched);
        }
        plugin.exemptManager().grant(player, itemKey);
        Chat.send(player, plugin.messages(), "return-success", Map.of(
                "item", Messages.displayMaterial(itemKey),
                "amount", String.valueOf(total),
                "hotbar", String.valueOf(result.hotbarAmount()),
                "inventory", String.valueOf(result.inventoryAmount()),
                "betterbags", String.valueOf(result.betterBagsAmount()),
                "seconds", String.valueOf(plugin.exemptDurationSeconds())
        ));
    }

    private void pruneExpired() {
        long cutoff = System.currentTimeMillis() - Math.max(1, plugin.exemptDurationSeconds()) * 1000L * 10;
        Iterator<Map.Entry<String, PendingReturn>> iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().createdAtMs() < cutoff) {
                iterator.remove();
            }
        }
    }

    private record PendingReturn(UUID playerId, String itemKey, long amount, long createdAtMs) {
    }
}
