package work.mcwws.pickblockbuy;

import io.papermc.paper.event.player.PlayerPickBlockEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PickBlockBuyListener implements Listener {

    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.##");

    private final McwwsPickBlockBuyPlugin plugin;
    private final ShopMappingIndex mappingIndex;
    private final Map<UUID, PendingPickState> pendingByPlayer = new ConcurrentHashMap<>();

    public PickBlockBuyListener(McwwsPickBlockBuyPlugin plugin, ShopMappingIndex mappingIndex) {
        this.plugin = plugin;
        this.mappingIndex = mappingIndex;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPickBlock(PlayerPickBlockEvent event) {
        if (!plugin.getConfig().getBoolean("enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("mcwws.shop.pickbuy")) {
            return;
        }
        if (plugin.getConfig().getBoolean("require-survival", true)) {
            GameMode mode = player.getGameMode();
            if (mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE) {
                return;
            }
        }

        // 背包里已有该方块时，保留原版选块到快捷栏的行为。
        if (event.getSourceSlot() != -1) {
            pendingByPlayer.remove(player.getUniqueId());
            return;
        }

        Material material = event.getBlock().getType();
        ShopOffer offer = mappingIndex.find(material);
        if (offer == null) {
            return;
        }

        event.setCancelled(true);

        long now = System.currentTimeMillis();
        long timeoutMs = plugin.confirmTimeoutMillis();
        PendingPickState pending = pendingByPlayer.get(player.getUniqueId());

        if (pending != null
                && pending.material() == material
                && now - pending.timestampMillis() <= timeoutMs) {
            pendingByPlayer.remove(player.getUniqueId());
            executePurchase(player, offer);
            return;
        }

        pendingByPlayer.put(player.getUniqueId(), new PendingPickState(material, now));
        sendPrompt(player, offer);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingByPlayer.remove(event.getPlayer().getUniqueId());
    }

    private void executePurchase(Player player, ShopOffer offer) {
        int amount = Math.max(1, plugin.getConfig().getInt("buy-amount", 64));
        String command = "shop quickbuy " + offer.shopId() + " " + offer.slot() + " " + amount;
        Bukkit.getScheduler().runTask(plugin, () -> player.performCommand(command));

        String purchased = plugin.formatMessage(
                "messages.purchased",
                "amount", String.valueOf(amount),
                "price", formatPrice(offer.unitBuyPrice() * amount)
        );
        player.sendMessage(plugin.prefixComponent().append(Component.text(purchased, NamedTextColor.GREEN)));
    }

    private void sendPrompt(Player player, ShopOffer offer) {
        int amount = Math.max(1, plugin.getConfig().getInt("buy-amount", 64));
        double totalPrice = offer.unitBuyPrice() * amount;

        Component blockName = Component.translatable(offer.material().translationKey())
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false);

        Component actionBar = Component.text("再中键一次购买 ", NamedTextColor.YELLOW)
                .append(Component.text(String.valueOf(amount), NamedTextColor.WHITE))
                .append(Component.text(" 个 ", NamedTextColor.YELLOW))
                .append(blockName)
                .append(Component.text("（约 ¥" + formatPrice(totalPrice) + "）", NamedTextColor.GOLD));

        player.sendActionBar(actionBar);

        String prompt = plugin.formatMessage(
                "messages.prompt",
                "amount", String.valueOf(amount),
                "price", formatPrice(totalPrice)
        );
        player.sendMessage(plugin.prefixComponent()
                .append(blockName)
                .append(Component.text(" " + prompt, NamedTextColor.YELLOW)));
    }

    private static String formatPrice(double value) {
        if (value <= 0D) {
            return "?";
        }
        return PRICE_FORMAT.format(value);
    }
}
