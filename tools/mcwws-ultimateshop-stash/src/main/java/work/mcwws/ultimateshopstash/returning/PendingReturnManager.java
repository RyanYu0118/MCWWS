package work.mcwws.ultimateshopstash.returning;

import org.bukkit.entity.Player;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;
import work.mcwws.ultimateshopstash.util.Chat;
import work.mcwws.ultimateshopstash.util.Messages;

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
        String token = UUID.randomUUID().toString().replace("-", "");
        pending.put(token, new PendingReturn(player.getUniqueId(), itemKey, amount));
        return token;
    }

    public void returnBatch(Player player, String token) {
        PendingReturn receipt = pending.get(token);
        if (receipt == null || !receipt.playerId().equals(player.getUniqueId())) {
            Chat.send(player, plugin.messages(), "return-invalid", null);
            return;
        }

        long stored = plugin.storage().getAmount(player.getUniqueId(), receipt.itemKey());
        if (stored < receipt.amount()) {
            Chat.send(player, plugin.messages(), "return-missing", Map.of(
                    "item", Messages.displayMaterial(receipt.itemKey())
            ));
            return;
        }

        ItemReturnService.ReturnResult result = returnService.returnItems(
                player, receipt.itemKey(), receipt.amount());
        if (!result.success()) {
            Chat.send(player, plugin.messages(), "return-full", null);
            return;
        }

        if (!plugin.storage().tryRemove(player.getUniqueId(), receipt.itemKey(), receipt.amount())) {
            plugin.getLogger().severe("物品已放回玩家容器，但仓库扣除失败: "
                    + player.getName() + " " + receipt.itemKey() + " x" + receipt.amount());
            return;
        }

        pending.remove(token);
        plugin.exemptManager().grant(player, receipt.itemKey());
        Chat.send(player, plugin.messages(), "return-success", Map.of(
                "item", Messages.displayMaterial(receipt.itemKey()),
                "amount", String.valueOf(receipt.amount()),
                "hotbar", String.valueOf(result.hotbarAmount()),
                "inventory", String.valueOf(result.inventoryAmount()),
                "betterbags", String.valueOf(result.betterBagsAmount()),
                "seconds", String.valueOf(plugin.exemptDurationSeconds())
        ));
    }

    private record PendingReturn(UUID playerId, String itemKey, long amount) {
    }
}
