/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.plugin.Plugin
 */
package cat.necko.bags.bag.inventory;

import cat.necko.bags.Plugin;
import cat.necko.bags.bag.data.PlayerData;
import cat.necko.bags.config.bags.BagsData;
import cat.necko.bags.utils.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class BagInventory {
    public static final String METADATA_KEY = "betterbags:opened";
    private Inventory inventory;
    private final PlayerData playerData;
    private int currentPage = 0;

    public BagInventory(PlayerData playerData) {
        this.playerData = playerData;
        this.inventory = playerData.getInventoryPage(this.currentPage);
    }

    public int addItem(ItemStack item) {
        return this.playerData.addItem(item);
    }

    public boolean removeItem(ItemStack item) {
        return this.playerData.removeItem(item);
    }

    public void nextPage(Player player) {
        ++this.currentPage;
        this.updateFor(player);
    }

    public void previousPage(Player player) {
        --this.currentPage;
        this.updateFor(player);
    }

    public void sellAll(Player player) {
        Tuple<Float, Integer> result = this.playerData.sellAndDeposit();
        if (result.b() > 0) {
            this.updateFor(player);
        }
        String message = result.b() > 0 ? "something" : "nothing";
        player.sendMessage(Plugin.getInstance().getMessages().getString("sell-all." + message, s -> s.replace("%amount%", String.valueOf(result.b())).replace("%cost%", String.valueOf(result.a()))));
    }

    public void upgradeBag(Player player) {
        boolean upgraded = this.playerData.upgradeBag();
        if (upgraded) {
            this.updateFor(player);
            BagsData.updatePlayerBag(player);
        }
        String message = upgraded ? "success" : "fail";
        int cost = upgraded ? this.playerData.getLevel().cost() : this.playerData.getNextLevel().cost();
        int level = this.playerData.getLevel().level() - (upgraded ? 1 : 0);
        player.sendMessage(Plugin.getInstance().getMessages().getString("bag-upgrade." + message, s -> s.replace("%old%", String.valueOf(level)).replace("%new%", String.valueOf(level + 1)).replace("%cost%", String.valueOf(cost))));
    }

    public void setIgnoreItemValue(Player player, boolean ignoreItemValue) {
        this.playerData.setIgnoreItemValue(ignoreItemValue);
        this.updateFor(player);
    }

    public void updateFor(Player player) {
        this.updateFor(player, false);
    }

    private void updateFor(Player player, boolean retry) {
        this.playerData.update();
        Inventory updated = this.playerData.getInventoryPage(this.currentPage);
        if (updated == null) {
            if (!retry) {
                this.currentPage = 0;
                this.updateFor(player, true);
                return;
            }
            Plugin.getInstance().getLogger().severe("Failed to update inventory for %s (updated inventory is null)".formatted(player.getName()));
            return;
        }
        this.inventory = updated;
        this.openFor(player);
    }

    public void openFor(Player player) {
        Bukkit.getScheduler().runTask((org.bukkit.plugin.Plugin)Plugin.getInstance(), () -> {
            player.openInventory(this.inventory);
            player.setMetadata(METADATA_KEY, (MetadataValue)new FixedMetadataValue((org.bukkit.plugin.Plugin)Plugin.getInstance(), (Object)this));
        });
    }

    public static boolean isOpened(Player player) {
        return player.hasMetadata(METADATA_KEY);
    }
}

