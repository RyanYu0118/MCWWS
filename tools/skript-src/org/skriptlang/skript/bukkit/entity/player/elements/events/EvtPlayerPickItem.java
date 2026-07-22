/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.PlayerPickBlockEvent
 *  io.papermc.paper.event.player.PlayerPickEntityEvent
 *  io.papermc.paper.event.player.PlayerPickItemEvent
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.Inventory
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.entity.player.elements.events;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.data.DefaultComparators;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.registrations.EventConverter;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.util.slot.InventorySlot;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.coll.CollectionUtils;
import io.papermc.paper.event.player.PlayerPickBlockEvent;
import io.papermc.paper.event.player.PlayerPickEntityEvent;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import java.lang.runtime.SwitchBootstraps;
import java.util.Objects;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.lang.comparator.Relation;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class EvtPlayerPickItem
extends SkriptEvent {
    private static final Patterns<PickType> PATTERNS = new Patterns(new Object[][]{{"[player] pick[ing] [of] [an|any] item", PickType.ANY}, {"[player] pick[ing] [of] [a|any] block", PickType.BLOCK}, {"[player] pick[ing] [of] [an|any] entity", PickType.ENTITY}, {"[player] pick[ing] [of] %entitydata/itemtype/blockdata%", null}});
    @Nullable
    private PickType pickType;
    @Nullable
    private Literal<?> type;

    public static void register(SyntaxRegistry registry) {
        registry.register(BukkitSyntaxInfos.Event.KEY, ((BukkitSyntaxInfos.Event.Builder)((BukkitSyntaxInfos.Event.Builder)BukkitSyntaxInfos.Event.builder(EvtPlayerPickItem.class, "Player Pick Item").supplier(EvtPlayerPickItem::new)).addEvents(CollectionUtils.array(PlayerPickBlockEvent.class, PlayerPickEntityEvent.class)).addPatterns(PATTERNS.getPatterns())).addDescription("Called when a player picks an item, block or an entity using the pick block key (default middle mouse button).", "The past event-slot represents the slot containing the item that will be put into the players hotbar, or nothing, if the item is not in the inventory.", "The event-slot represents the slot in the hotbar where the picked item will be placed.", "Both event-slots may be set to new slots.").addExample("on player picking a diamond block:\n\tcancel event\n\tsend \"You cannot pick diamond blocks!\" to the player\n").addSince("2.15").addRequiredPlugin("1.21.5+").build());
        EventValues.registerEventValue(PlayerPickItemEvent.class, Slot.class, new EventConverter<PlayerPickItemEvent, Slot>(){

            @Override
            public void set(PlayerPickItemEvent event, @Nullable Slot slot) {
                InventorySlot inventorySlot;
                if (!(slot instanceof InventorySlot) || (inventorySlot = (InventorySlot)slot).getInventory() != event.getPlayer().getInventory()) {
                    return;
                }
                event.setSourceSlot(inventorySlot.getIndex());
            }

            @Override
            @Nullable
            public Slot convert(PlayerPickItemEvent event) {
                int source = event.getSourceSlot();
                if (source == -1) {
                    return null;
                }
                return new InventorySlot((Inventory)event.getPlayer().getInventory(), source);
            }
        }, EventValues.TIME_PAST);
        EventValues.registerEventValue(PlayerPickItemEvent.class, Slot.class, new EventConverter<PlayerPickItemEvent, Slot>(){

            @Override
            public void set(PlayerPickItemEvent event, @Nullable Slot slot) {
                InventorySlot inventorySlot;
                if (!(slot instanceof InventorySlot) || (inventorySlot = (InventorySlot)slot).getInventory() != event.getPlayer().getInventory()) {
                    return;
                }
                event.setTargetSlot(inventorySlot.getIndex());
            }

            @Override
            public Slot convert(PlayerPickItemEvent event) {
                return new InventorySlot((Inventory)event.getPlayer().getInventory(), event.getTargetSlot());
            }
        });
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.pickType = PATTERNS.getInfo(matchedPattern);
        if (this.pickType == null) {
            this.type = args[0];
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        Entity pickedEntity;
        Block pickedBlock;
        if (this.pickType != null) {
            return switch (this.pickType.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> true;
                case 1 -> event instanceof PlayerPickBlockEvent;
                case 2 -> event instanceof PlayerPickEntityEvent;
            };
        }
        if (event instanceof PlayerPickBlockEvent) {
            PlayerPickBlockEvent pickBlockEvent = (PlayerPickBlockEvent)event;
            pickedBlock = pickBlockEvent.getBlock();
            pickedEntity = null;
        } else if (event instanceof PlayerPickEntityEvent) {
            PlayerPickEntityEvent pickEntityEvent = (PlayerPickEntityEvent)event;
            pickedEntity = pickEntityEvent.getEntity();
            pickedBlock = null;
        } else {
            assert (false);
            return false;
        }
        assert (this.type != null);
        return this.type.check(event, object -> {
            boolean bl;
            Object object2 = object;
            Objects.requireNonNull(object2);
            Object selector0$temp = object2;
            int index$1 = 0;
            block6: while (true) {
                switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{EntityData.class, ItemType.class, ItemType.class, BlockData.class}, (Object)selector0$temp, index$1)) {
                    case 0: {
                        EntityData entityData = (EntityData)selector0$temp;
                        if (pickedEntity == null) {
                            index$1 = 1;
                            continue block6;
                        }
                        bl = entityData.isInstance(pickedEntity);
                        break block6;
                    }
                    case 1: {
                        ItemType itemType = (ItemType)selector0$temp;
                        if (pickedEntity == null) {
                            index$1 = 2;
                            continue block6;
                        }
                        Relation comparison = DefaultComparators.entityItemComparator.compare(EntityData.fromEntity(pickedEntity), itemType);
                        bl = Relation.EQUAL.isImpliedBy(comparison);
                        break block6;
                    }
                    case 2: {
                        ItemType itemType = (ItemType)selector0$temp;
                        bl = itemType.isOfType(pickedBlock);
                        break block6;
                    }
                    case 3: {
                        BlockData blockData = (BlockData)selector0$temp;
                        if (pickedBlock == null) {
                            index$1 = 4;
                            continue block6;
                        }
                        bl = pickedBlock.getBlockData().matches(blockData);
                        break block6;
                    }
                    default: {
                        bl = false;
                        break block6;
                    }
                }
                break;
            }
            return bl;
        });
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append((Object)"player picking");
        if (this.pickType != null) {
            switch (this.pickType.ordinal()) {
                case 0: {
                    builder.append((Object)"an item");
                    break;
                }
                case 1: {
                    builder.append((Object)"a block");
                    break;
                }
                case 2: {
                    builder.append((Object)"an entity");
                }
            }
        } else if (this.type != null) {
            builder.append((Object)this.type);
        }
        return builder.toString();
    }

    private static enum PickType {
        ANY,
        BLOCK,
        ENTITY;

    }
}

