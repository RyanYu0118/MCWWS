/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryAction
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 */
package cat.necko.bags.bag.inventory;

import cat.necko.bags.Plugin;
import cat.necko.bags.bag.inventory.BagInventory;
import cat.necko.bags.config.bags.BagsData;
import java.util.Map;
import java.util.function.BiConsumer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;

public class InventoryListener
implements Listener {
    private final Plugin plugin;
    private final Map<BagsData.ItemHash, BiConsumer<BagInventory, Player>> handlers = Map.of(BagsData.NEXT_PAGE, BagInventory::nextPage, BagsData.PREV_PAGE, BagInventory::previousPage, BagsData.SELL_ALL, BagInventory::sellAll, BagsData.UPGRADE, BagInventory::upgradeBag, BagsData.IIV_TRUE, (inventory, player) -> inventory.setIgnoreItemValue((Player)player, false), BagsData.IIV_FALSE, (inventory, player) -> inventory.setIgnoreItemValue((Player)player, true));

    public InventoryListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        HumanEntity humanEntity = event.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        if (!BagInventory.isOpened(player)) {
            return;
        }
        player.removeMetadata("betterbags:opened", (org.bukkit.plugin.Plugin)this.plugin);
    }

    @EventHandler
    public void onBagInventoryClick(InventoryClickEvent event) {
        boolean shouldUpdate;
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        if (!BagInventory.isOpened(player)) {
            return;
        }
        BagInventory inventory = (BagInventory)((MetadataValue)player.getMetadata("betterbags:opened").getFirst()).value();
        if (inventory == null) {
            event.setCancelled(true);
            this.plugin.getLogger().severe("Failed to get bag inventory for player %s".formatted(player.getName()));
            return;
        }
        if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() instanceof PlayerInventory) {
            boolean shouldUpdate2 = this.onPlayerInventoryClick(inventory, event);
            if (shouldUpdate2) {
                BagsData.updatePlayerBag(player);
                inventory.updateFor(player);
            }
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }
        if (clicked == null || clicked.getType().isAir()) {
            this.handleEmptySlotClick(clickedInventory, inventory, player, event);
            return;
        }
        if (clicked.getPersistentDataContainer().has(BagsData.ItemHash.KEY, PersistentDataType.STRING)) {
            event.setCancelled(true);
            this.handlers.forEach((itemHash, handler) -> {
                if (itemHash.compareTags(clicked)) {
                    handler.accept(inventory, player);
                }
            });
            return;
        }
        switch (event.getAction()) {
            case PLACE_SOME: 
            case PLACE_ALL: {
                boolean bl = this.handlePlaceSome(clickedInventory, inventory, event);
                break;
            }
            case PLACE_ONE: {
                boolean bl = this.handlePlaceOne(clickedInventory, inventory, event);
                break;
            }
            case MOVE_TO_OTHER_INVENTORY: {
                boolean bl;
                if (!this.plugin.getConfigData().playerGetAbility) {
                    event.setCancelled(true);
                    bl = false;
                    break;
                }
                boolean removed = inventory.removeItem(clicked);
                if (!removed) {
                    this.plugin.getLogger().severe("Failed to MOVE_TO_OTHER_INVENTORY (from) " + String.valueOf(clicked) + " from player " + player.getName() + "'s bag");
                }
                bl = removed;
                break;
            }
            case PICKUP_ALL: {
                boolean bl;
                if (!this.plugin.getConfigData().playerGetAbility) {
                    event.setCancelled(true);
                    bl = false;
                    break;
                }
                boolean removed = inventory.removeItem(clicked);
                if (!removed) {
                    this.plugin.getLogger().severe("Failed to PICKUP_ALL " + String.valueOf(clicked) + " from player " + player.getName() + "'s bag");
                }
                bl = removed;
                break;
            }
            case PICKUP_HALF: {
                ItemStack onCursor;
                boolean removed;
                boolean bl;
                if (!this.plugin.getConfigData().playerGetAbility) {
                    event.setCancelled(true);
                    bl = false;
                    break;
                }
                int half = clicked.getAmount() / 2;
                if (clicked.getAmount() % 2 != 0) {
                    ++half;
                }
                if (!(removed = inventory.removeItem(onCursor = clicked.asQuantity(half)))) {
                    this.plugin.getLogger().severe("Failed to PICKUP_HALF " + String.valueOf(clicked) + " from player " + player.getName() + "'s bag");
                }
                bl = removed;
                break;
            }
            case SWAP_WITH_CURSOR: {
                boolean bl;
                event.setCancelled(true);
                if (!this.plugin.getConfigData().playerPutAbility) {
                    bl = false;
                    break;
                }
                ItemStack added = event.getCursor();
                int amount = added.getAmount();
                int returned = inventory.addItem(added);
                if (returned == amount) {
                    bl = false;
                    break;
                }
                clickedInventory.addItem(new ItemStack[]{added.clone()});
                added.setAmount(returned);
                bl = true;
                break;
            }
            case CLONE_STACK: {
                boolean bl = false;
                break;
            }
            default: {
                event.setCancelled(true);
                boolean bl = shouldUpdate = false;
            }
        }
        if (shouldUpdate) {
            BagsData.updatePlayerBag(player);
        }
    }

    private boolean onPlayerInventoryClick(BagInventory inventory, InventoryClickEvent event) {
        return switch (event.getAction()) {
            case InventoryAction.MOVE_TO_OTHER_INVENTORY -> {
                event.setCancelled(true);
                if (!this.plugin.getConfigData().playerPutAbility) {
                    yield false;
                }
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null) {
                    yield false;
                }
                int amount = clicked.getAmount();
                int returned = inventory.addItem(clicked);
                if (returned == amount) {
                    yield false;
                }
                clicked.setAmount(returned);
                yield true;
            }
            case InventoryAction.COLLECT_TO_CURSOR -> {
                event.setCancelled(true);
                yield false;
            }
            default -> false;
        };
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        if (!BagInventory.isOpened(player)) {
            return;
        }
        int slots = event.getInventory().getSize();
        for (Map.Entry entry : event.getNewItems().entrySet()) {
            if ((Integer)entry.getKey() >= slots) continue;
            event.setCancelled(true);
            return;
        }
    }

    private boolean handlePlaceOne(Inventory clickedInventory, BagInventory inventory, InventoryClickEvent event) {
        event.setCancelled(true);
        if (!this.plugin.getConfigData().playerPutAbility) {
            return false;
        }
        ItemStack added = event.getCursor();
        int amount = added.getAmount() - 1;
        added.setAmount(1);
        int returned = inventory.addItem(added);
        if (returned == 1) {
            added.setAmount(amount + 1);
            return false;
        }
        ItemStack clickedOn = clickedInventory.getItem(event.getSlot());
        if (clickedOn == null) {
            clickedInventory.setItem(event.getSlot(), added.clone());
        } else {
            int sum = clickedOn.getAmount() + added.getAmount();
            clickedOn.setAmount(sum);
        }
        added.setAmount(amount);
        return true;
    }

    private boolean handlePlaceSome(Inventory clickedInventory, BagInventory inventory, InventoryClickEvent event) {
        event.setCancelled(true);
        if (!this.plugin.getConfigData().playerPutAbility) {
            return false;
        }
        ItemStack added = event.getCursor();
        int amount = added.getAmount();
        int returned = inventory.addItem(added);
        if (returned == amount) {
            return false;
        }
        ItemStack clickedOn = clickedInventory.getItem(event.getSlot());
        if (clickedOn == null) {
            clickedInventory.setItem(event.getSlot(), added.clone());
        } else {
            int sum = clickedOn.getAmount() + added.getAmount();
            if (sum > clickedOn.getMaxStackSize()) {
                clickedOn.setAmount(clickedOn.getMaxStackSize());
                int remaining = sum - clickedOn.getMaxStackSize();
                added.setAmount(remaining);
                inventory.removeItem(added);
                returned += remaining;
            } else {
                clickedOn.setAmount(sum);
            }
        }
        added.setAmount(returned);
        return true;
    }

    private void handleEmptySlotClick(Inventory clickedInventory, BagInventory inventory, Player player, InventoryClickEvent event) {
        switch (event.getAction()) {
            case PLACE_SOME: 
            case PLACE_ALL: {
                event.setCancelled(true);
                if (!this.plugin.getConfigData().playerPutAbility) {
                    return;
                }
                ItemStack added = event.getCursor();
                int amount = added.getAmount();
                int returned = inventory.addItem(added);
                if (returned == amount) {
                    return;
                }
                clickedInventory.setItem(event.getSlot(), added.clone());
                added.setAmount(returned);
                BagsData.updatePlayerBag(player);
                break;
            }
            case PLACE_ONE: {
                event.setCancelled(true);
                if (!this.plugin.getConfigData().playerPutAbility) {
                    return;
                }
                ItemStack added = event.getCursor();
                int amount = added.getAmount() - 1;
                added.setAmount(1);
                int returned = inventory.addItem(added);
                if (returned == 1) {
                    added.setAmount(amount + 1);
                    return;
                }
                clickedInventory.setItem(event.getSlot(), added.clone());
                added.setAmount(amount);
                BagsData.updatePlayerBag(player);
                break;
            }
            case COLLECT_TO_CURSOR: {
                event.setCancelled(true);
            }
        }
    }
}

