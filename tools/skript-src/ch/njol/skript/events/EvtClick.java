/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.entity.ArmorStand
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Vehicle
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.Event
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerInteractAtEntityEvent
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.ClickEventTracker;
import ch.njol.skript.classes.data.DefaultComparators;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.coll.CollectionUtils;
import java.util.function.Predicate;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.comparator.Relation;

public class EvtClick
extends SkriptEvent {
    private static final boolean USE_OLD_PIAEE_BEHAVIOR = !Skript.isRunningMinecraft(26, 1, 1);
    private static final int RIGHT = 1;
    private static final int LEFT = 2;
    private static final int ANY = 3;
    public static final ClickEventTracker interactTracker = new ClickEventTracker(Skript.getInstance());
    @Nullable
    private Literal<?> type;
    @Nullable
    private Literal<ItemType> tools;
    private int click = 3;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.click = parseResult.mark == 0 ? 3 : parseResult.mark;
        this.type = args[matchedPattern];
        if (this.type != null && !this.type.canReturn(ItemType.class) && !this.type.canReturn(BlockData.class)) {
            Literal<?> entitydata = this.type;
            if (this.click == 2) {
                if (Vehicle.class.isAssignableFrom(((EntityData)entitydata.getSingle()).getType())) {
                    Skript.error("A leftclick on a vehicle entity is an attack and thus not covered by the 'click' event, but the 'vehicle damage' event.");
                } else {
                    Skript.error("A leftclick on an entity is an attack and thus not covered by the 'click' event, but the 'damage' event.");
                }
                return false;
            }
            if (this.click == 3) {
                if (Vehicle.class.isAssignableFrom(((EntityData)entitydata.getSingle()).getType())) {
                    Skript.error("A leftclick on a vehicle entity is an attack and thus not covered by the 'click' event, but the 'vehicle damage' event. Change this event to a rightclick to fix this warning message.");
                } else {
                    Skript.error("A leftclick on an entity is an attack and thus not covered by the 'click' event, but the 'damage' event. Change this event to a rightclick to fix this warning message.");
                }
            }
        }
        this.tools = args[1 - matchedPattern];
        return true;
    }

    @Override
    public boolean check(Event event) {
        Block block;
        Entity entity;
        if (event instanceof PlayerInteractEntityEvent) {
            PlayerInteractEntityEvent interactEntityEvent = (PlayerInteractEntityEvent)event;
            Entity clicked = interactEntityEvent.getRightClicked();
            if (USE_OLD_PIAEE_BEHAVIOR && interactEntityEvent instanceof PlayerInteractAtEntityEvent && !(clicked instanceof ArmorStand)) {
                return false;
            }
            if (this.click == 2) {
                return false;
            }
            if (USE_OLD_PIAEE_BEHAVIOR ? !(event instanceof PlayerInteractAtEntityEvent) && !interactTracker.checkEvent(interactEntityEvent.getPlayer(), (Cancellable)interactEntityEvent, interactEntityEvent.getHand()) : !interactTracker.checkEvent(interactEntityEvent.getPlayer(), (Cancellable)interactEntityEvent, interactEntityEvent.getHand())) {
                return false;
            }
            entity = clicked;
            block = null;
        } else if (event instanceof PlayerInteractEvent) {
            int click;
            PlayerInteractEvent interactEvent = (PlayerInteractEvent)event;
            Action action = interactEvent.getAction();
            switch (action) {
                case LEFT_CLICK_AIR: 
                case LEFT_CLICK_BLOCK: {
                    click = 2;
                    break;
                }
                case RIGHT_CLICK_AIR: 
                case RIGHT_CLICK_BLOCK: {
                    click = 1;
                    break;
                }
                default: {
                    return false;
                }
            }
            if ((this.click & click) == 0) {
                return false;
            }
            EquipmentSlot hand = interactEvent.getHand();
            assert (hand != null);
            if (!interactTracker.checkEvent(interactEvent.getPlayer(), (Cancellable)interactEvent, hand)) {
                return false;
            }
            block = interactEvent.getClickedBlock();
            entity = null;
        } else {
            assert (false);
            return false;
        }
        Predicate<ItemType> checker = itemType -> {
            if (event instanceof PlayerInteractEvent) {
                PlayerInteractEvent interactEvent = (PlayerInteractEvent)event;
                return itemType.isOfType(interactEvent.getItem());
            }
            PlayerInventory invi = ((PlayerInteractEntityEvent)event).getPlayer().getInventory();
            ItemStack item = ((PlayerInteractEntityEvent)event).getHand() == EquipmentSlot.HAND ? invi.getItemInMainHand() : invi.getItemInOffHand();
            return itemType.isOfType(item);
        };
        if (this.tools != null && !this.tools.check(event, checker)) {
            return false;
        }
        if (this.type != null) {
            BlockData blockDataCheck = block != null ? block.getBlockData() : null;
            return this.type.check(event, object -> {
                if (entity != null) {
                    if (object instanceof EntityData) {
                        EntityData entityData = (EntityData)object;
                        return entityData.isInstance(entity);
                    }
                    if (object instanceof ItemType) {
                        ItemType itemType = (ItemType)object;
                        Relation compare = DefaultComparators.entityItemComparator.compare(EntityData.fromEntity(entity), itemType);
                        return Relation.EQUAL.isImpliedBy(compare);
                    }
                } else {
                    if (object instanceof ItemType) {
                        ItemType itemType = (ItemType)object;
                        return itemType.isOfType(block);
                    }
                    if (object instanceof BlockData) {
                        BlockData blockData = (BlockData)object;
                        return blockDataCheck != null && blockDataCheck.matches(blockData);
                    }
                }
                return false;
            });
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (switch (this.click) {
            case 2 -> "left";
            case 1 -> "right";
            default -> "";
        }) + "click" + (String)(this.type != null ? " on " + this.type.toString(event, debug) : "") + (String)(this.tools != null ? " holding " + this.tools.toString(event, debug) : "");
    }

    static {
        Class[] eventTypes = CollectionUtils.array(PlayerInteractEvent.class, PlayerInteractEntityEvent.class, PlayerInteractAtEntityEvent.class);
        Skript.registerEvent("Click", EvtClick.class, eventTypes, "[(1:right|2:left)(| |-)][mouse(| |-)]click[ing] [on %-entitydata/itemtype/blockdata%] [(with|using|holding) %-itemtype%]", "[(1:right|2:left)(| |-)][mouse(| |-)]click[ing] (with|using|holding) %itemtype% on %entitydata/itemtype/blockdata%").description("Called when a user clicks on a block, an entity or air with or without an item in their hand.", "Please note that rightclick events with an empty hand while not looking at a block are not sent to the server, so there's no way to detect them.", "Also note that a leftclick on an entity is an attack and thus not covered by the 'click' event, but the 'damage' event.").examples("on click:", "on rightclick holding a fishing rod:", "on leftclick on a stone or obsidian:", "on rightclick on a creeper:", "on click with a sword:", "on click on chest[facing=north]:", "on click on campfire[lit=true]:").since("1.0, 2.10 (blockdata)");
    }
}

