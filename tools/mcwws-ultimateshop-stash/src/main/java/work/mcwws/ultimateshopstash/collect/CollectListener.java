package work.mcwws.ultimateshopstash.collect;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;
import work.mcwws.ultimateshopstash.notify.StashNotifier;
import work.mcwws.ultimateshopstash.util.ItemKeys;
import work.mcwws.ultimateshopstash.util.Messages;

import java.util.Iterator;

public final class CollectListener implements Listener {

    private final McwwsUltimateShopStashPlugin plugin;

    public CollectListener(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!plugin.collectPickup()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Item entity = event.getItem();
        ItemStack stack = entity.getItemStack();
        deposit(player, stack, () -> {
            event.setCancelled(true);
            entity.remove();
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        if (!plugin.collectKill()) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            String key = ItemKeys.matchStack(plugin.catalog(), stack);
            if (key == null) {
                continue;
            }
            if (!shouldCollect(killer, key)) {
                continue;
            }
            long amount = stack.getAmount();
            plugin.storage().add(killer.getUniqueId(), key, amount);
            StashNotifier.collect(plugin, killer, key, amount);
            iterator.remove();
        }
    }

    private void deposit(Player player, ItemStack stack, Runnable onSuccess) {
        String key = ItemKeys.matchStack(plugin.catalog(), stack);
        if (key == null) {
            return;
        }
        if (!shouldCollect(player, key)) {
            return;
        }
        long amount = stack.getAmount();
        plugin.storage().add(player.getUniqueId(), key, amount);
        StashNotifier.collect(plugin, player, key, amount);
        onSuccess.run();
    }

    private boolean shouldCollect(Player player, String itemKey) {
        return plugin.shouldAutoCollect(player, itemKey);
    }
}
