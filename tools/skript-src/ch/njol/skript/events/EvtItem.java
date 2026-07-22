/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.PlayerStonecutterRecipeSelectEvent
 *  org.bukkit.event.Event
 *  org.bukkit.event.block.BlockDispenseEvent
 *  org.bukkit.event.entity.EntityDropItemEvent
 *  org.bukkit.event.entity.EntityPickupItemEvent
 *  org.bukkit.event.entity.ItemDespawnEvent
 *  org.bukkit.event.entity.ItemMergeEvent
 *  org.bukkit.event.entity.ItemSpawnEvent
 *  org.bukkit.event.inventory.CraftItemEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryMoveItemEvent
 *  org.bukkit.event.inventory.PrepareItemCraftEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerItemConsumeEvent
 *  org.bukkit.event.player.PlayerPickupItemEvent
 *  org.bukkit.inventory.ComplexRecipe
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.Recipe
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.sections.EffSecSpawn;
import ch.njol.util.coll.CollectionUtils;
import io.papermc.paper.event.player.PlayerStonecutterRecipeSelectEvent;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ComplexRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.jetbrains.annotations.Nullable;

public class EvtItem
extends SkriptEvent {
    private static final boolean HAS_PLAYER_STONECUTTER_RECIPE_SELECT_EVENT = Skript.classExists("io.papermc.paper.event.player.PlayerStonecutterRecipeSelectEvent");
    @Nullable
    private Literal<ItemType> types;
    private boolean entity;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        this.types = args[0];
        this.entity = parser.mark == 1;
        return true;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public boolean check(Event event) {
        ItemStack itemStack;
        if (event instanceof ItemSpawnEvent) {
            ItemSpawnEvent itemSpawnEvent = (ItemSpawnEvent)event;
            EffSecSpawn.lastSpawned = itemSpawnEvent.getEntity();
        }
        if (!this.entity && event instanceof EntityPickupItemEvent || this.entity && event instanceof PlayerPickupItemEvent) {
            return false;
        }
        if (!this.entity && event instanceof EntityDropItemEvent || this.entity && event instanceof PlayerDropItemEvent) {
            return false;
        }
        if (this.types == null) {
            return true;
        }
        if (event instanceof BlockDispenseEvent) {
            BlockDispenseEvent blockDispenseEvent = (BlockDispenseEvent)event;
            itemStack = blockDispenseEvent.getItem();
        } else if (event instanceof ItemSpawnEvent) {
            ItemSpawnEvent itemSpawnEvent = (ItemSpawnEvent)event;
            itemStack = itemSpawnEvent.getEntity().getItemStack();
        } else if (event instanceof PlayerDropItemEvent) {
            PlayerDropItemEvent playerDropItemEvent = (PlayerDropItemEvent)event;
            itemStack = playerDropItemEvent.getItemDrop().getItemStack();
        } else if (event instanceof EntityDropItemEvent) {
            EntityDropItemEvent entityDropItemEvent = (EntityDropItemEvent)event;
            itemStack = entityDropItemEvent.getItemDrop().getItemStack();
        } else if (event instanceof CraftItemEvent) {
            CraftItemEvent craftItemEvent = (CraftItemEvent)event;
            Recipe recipe = craftItemEvent.getRecipe();
            itemStack = recipe instanceof ComplexRecipe ? craftItemEvent.getCurrentItem() : recipe.getResult();
        } else if (event instanceof PrepareItemCraftEvent) {
            PrepareItemCraftEvent prepareItemCraftEvent = (PrepareItemCraftEvent)event;
            Recipe recipe = prepareItemCraftEvent.getRecipe();
            if (recipe == null) return false;
            itemStack = recipe.getResult();
        } else if (HAS_PLAYER_STONECUTTER_RECIPE_SELECT_EVENT && event instanceof PlayerStonecutterRecipeSelectEvent) {
            PlayerStonecutterRecipeSelectEvent stonecutterRecipeSelectEvent = (PlayerStonecutterRecipeSelectEvent)event;
            itemStack = stonecutterRecipeSelectEvent.getStonecuttingRecipe().getResult();
        } else if (event instanceof EntityPickupItemEvent) {
            EntityPickupItemEvent entityPickupItemEvent = (EntityPickupItemEvent)event;
            itemStack = entityPickupItemEvent.getItem().getItemStack();
        } else if (event instanceof PlayerPickupItemEvent) {
            PlayerPickupItemEvent playerPickupItemEvent = (PlayerPickupItemEvent)event;
            itemStack = playerPickupItemEvent.getItem().getItemStack();
        } else if (event instanceof PlayerItemConsumeEvent) {
            PlayerItemConsumeEvent playerItemConsumeEvent = (PlayerItemConsumeEvent)event;
            itemStack = playerItemConsumeEvent.getItem();
        } else if (event instanceof InventoryClickEvent) {
            InventoryClickEvent inventoryClickEvent = (InventoryClickEvent)event;
            itemStack = inventoryClickEvent.getCurrentItem();
        } else if (event instanceof ItemDespawnEvent) {
            ItemDespawnEvent itemDespawnEvent = (ItemDespawnEvent)event;
            itemStack = itemDespawnEvent.getEntity().getItemStack();
        } else if (event instanceof ItemMergeEvent) {
            ItemMergeEvent itemMergeEvent = (ItemMergeEvent)event;
            itemStack = itemMergeEvent.getTarget().getItemStack();
        } else if (event instanceof InventoryMoveItemEvent) {
            InventoryMoveItemEvent inventoryMoveItemEvent = (InventoryMoveItemEvent)event;
            itemStack = inventoryMoveItemEvent.getItem();
        } else {
            assert (false);
            return false;
        }
        if (itemStack != null) return this.types.check(event, itemType -> itemType.isOfType(itemStack));
        return false;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "dispense/spawn/drop/craft/pickup/consume/break/despawn/merge/move/stonecutting" + (String)(this.types == null ? "" : " of " + String.valueOf(this.types));
    }

    static {
        Skript.registerEvent("Dispense", EvtItem.class, BlockDispenseEvent.class, "dispens(e|ing) [[of] %-itemtypes%]").description("Called when a dispenser dispenses an item.").examples("on dispense of iron block:", "\tsend \"that'd be 19.99 please!\"").since("unknown (before 2.1)");
        Skript.registerEvent("Item Spawn", EvtItem.class, ItemSpawnEvent.class, "item spawn[ing] [[of] %-itemtypes%]").description("Called whenever an item stack is spawned in a world, e.g. as drop of a block or mob, a player throwing items out of their inventory, or a dispenser dispensing an item (not shooting it).").examples("on item spawn of iron sword:", "\tbroadcast \"Someone dropped an iron sword!\"").since("unknown (before 2.1)");
        Skript.registerEvent("Drop", EvtItem.class, CollectionUtils.array(PlayerDropItemEvent.class, EntityDropItemEvent.class), "[player|1:entity] drop[ping] [[of] %-itemtypes%]").description("Called when a player drops an item from their inventory, or an entity drops an item, such as a chicken laying an egg.").examples("on drop:", "\tif event-item is compass:", "\t\tcancel event", "", "on entity drop of an egg:", "\tif event-entity is a chicken:", "\t\tset item of event-dropped item to a diamond").since("unknown (before 2.1), 2.7 (entity)");
        Skript.registerEvent("Prepare Craft", EvtItem.class, PrepareItemCraftEvent.class, "[player] (preparing|beginning) craft[ing] [[of] %-itemtypes%]").description("Called just before displaying crafting result to player. Note that setting the result item might or might not work due to Bukkit bugs.").examples("on preparing craft of torch:").since("2.2-Fixes-V10");
        Skript.registerEvent("Craft", EvtItem.class, CraftItemEvent.class, "[player] craft[ing] [[of] %-itemtypes%]").description("Called when a player crafts an item.").examples("on craft:").since("unknown (before 2.1)");
        Skript.registerEvent("Pick Up", EvtItem.class, CollectionUtils.array(PlayerPickupItemEvent.class, EntityPickupItemEvent.class), "[(player|1\u00a6entity)] (pick[ ]up|picking up) [[of] %-itemtypes%]").description("Called when a player/entity picks up an item. Please note that the item is still on the ground when this event is called.").examples("on pick up:", "on entity pickup of wheat:").since("unknown (before 2.1), 2.5 (entity)").keywords("pickup");
        Skript.registerEvent("Consume", EvtItem.class, PlayerItemConsumeEvent.class, "[player] ((eat|drink)[ing]|consum(e|ing)) [[of] %-itemtypes%]").description("Called when a player is done eating/drinking something, e.g. an apple, bread, meat, milk or a potion.").examples("on consume:").since("2.0");
        Skript.registerEvent("Inventory Click", EvtItem.class, InventoryClickEvent.class, "[player] inventory(-| )click[ing] [[at] %-itemtypes%]").description("Called when clicking on inventory slot.").examples("on inventory click:", "\tif event-item is stone:", "\t\tgive player 1 stone", "\t\tremove 20$ from player's balance").since("2.2-Fixes-V10");
        Skript.registerEvent("Item Despawn", EvtItem.class, ItemDespawnEvent.class, "(item[ ][stack]|[item] %-itemtypes%) despawn[ing]", "[item[ ][stack]] despawn[ing] [[of] %-itemtypes%]").description("Called when an item is about to be despawned from the world, usually 5 minutes after it was dropped.").examples("on item despawn of diamond:", "\tsend \"Not my precious!\"", "\tcancel event").since("2.2-dev35");
        Skript.registerEvent("Item Merge", EvtItem.class, ItemMergeEvent.class, "(item[ ][stack]|[item] %-itemtypes%) merg(e|ing)", "item[ ][stack] merg(e|ing) [[of] %-itemtypes%]").description("Called when dropped items merge into a single stack. event-entity will be the entity which is trying to merge, and future event-entity will be the entity which is being merged into.").examples("on item merge of gold blocks:", "\tcancel event").since("2.2-dev35");
        Skript.registerEvent("Inventory Item Move", SimpleEvent.class, InventoryMoveItemEvent.class, "inventory item (move|transport)", "inventory (mov(e|ing)|transport[ing]) [an] item").description("Called when an entity or block (e.g. hopper) tries to move items directly from one inventory to another.", "When this event is called, the initiator may have already removed the item from the source inventory and is ready to move it into the destination inventory.", "If this event is cancelled, the items will be returned to the source inventory.").examples("on inventory item move:", "\tbroadcast \"%holder of past event-inventory% is transporting %event-item% to %holder of event-inventory%!\"").since("2.8.0");
        if (HAS_PLAYER_STONECUTTER_RECIPE_SELECT_EVENT) {
            Skript.registerEvent("Stonecutter Recipe Select", EvtItem.class, PlayerStonecutterRecipeSelectEvent.class, "stonecutting [[of] %-itemtypes%]").description("Called when a player selects a recipe in a stonecutter.").examples("on stonecutting stone slabs", "\tcancel the event", "", "on stonecutting:", "\tbroadcast \"%player% is using stonecutter to craft %event-item%!\"").since("2.8.0");
        }
    }
}

