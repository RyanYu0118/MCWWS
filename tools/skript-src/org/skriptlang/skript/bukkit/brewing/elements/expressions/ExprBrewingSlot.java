/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.BrewingStand
 *  org.bukkit.event.Event
 *  org.bukkit.event.block.BrewingStartEvent
 *  org.bukkit.event.inventory.BrewEvent
 *  org.bukkit.event.inventory.BrewingStandFuelEvent
 *  org.bukkit.inventory.BrewerInventory
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.brewing.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.slot.InventorySlot;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.Event;
import org.bukkit.event.block.BrewingStartEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Brewing Stand Slot")
@Description(value={"A slot of a brewing stand, i.e. the first, second, or third bottle slot, the fuel slot or the ingredient slot."})
@Example.Examples(value={@Example(value="set the 1st bottle slot of {_block} to potion of water"), @Example(value="clear the brewing stand second bottle slot of {_block}")})
@Since(value={"2.13"})
public class ExprBrewingSlot
extends PropertyExpression<Block, Slot> {
    private static final BrewingSlot[] BREWING_SLOTS = BrewingSlot.values();
    private BrewingSlot selectedSlot;
    private boolean isEvent = false;

    public static void register(SyntaxRegistry registry) {
        String[] patterns = new String[BREWING_SLOTS.length * 2];
        for (BrewingSlot slot : BREWING_SLOTS) {
            patterns[2 * slot.ordinal()] = "[the] " + slot.pattern + " slot[s] [of %blocks%]";
            patterns[2 * slot.ordinal() + 1] = "%blocks%'[s] " + slot.pattern + " slot[s]";
        }
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprBrewingSlot.class, Slot.class).addPatterns(patterns)).supplier(ExprBrewingSlot::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.selectedSlot = BREWING_SLOTS[matchedPattern / 2];
        this.setExpr(exprs[0]);
        this.isEvent = this.getParser().isCurrentEvent(BrewEvent.class, BrewingStartEvent.class, BrewingStandFuelEvent.class);
        return true;
    }

    protected Slot @Nullable [] get(Event event, Block[] source) {
        BlockState brewingStartEvent;
        ArrayList blocks = new ArrayList(this.getExpr().stream(event).toList());
        ArrayList<InventorySlot> slots = new ArrayList<InventorySlot>();
        if (this.isEvent) {
            Block eventBlock = null;
            if (event instanceof BrewingStandFuelEvent) {
                BrewingStandFuelEvent brewingStandFuelEvent = (BrewingStandFuelEvent)event;
                eventBlock = brewingStandFuelEvent.getBlock();
            } else if (event instanceof BrewEvent) {
                BrewEvent brewEvent = (BrewEvent)event;
                eventBlock = brewEvent.getBlock();
            } else if (event instanceof BrewingStartEvent) {
                brewingStartEvent = (BrewingStartEvent)event;
                eventBlock = brewingStartEvent.getBlock();
            }
            if (eventBlock != null && blocks.remove(eventBlock)) {
                BrewerInventory brewerInventory = ((BrewingStand)eventBlock.getState()).getInventory();
                if (!Delay.isDelayed(event)) {
                    slots.add(new BrewingEventSlot(event, brewerInventory));
                } else {
                    slots.add(new InventorySlot((Inventory)brewerInventory, this.selectedSlot.ordinal()));
                }
            }
        }
        for (Block block : blocks) {
            brewingStartEvent = block.getState();
            if (!(brewingStartEvent instanceof BrewingStand)) continue;
            BrewingStand brewingStand = (BrewingStand)brewingStartEvent;
            BrewerInventory brewerInventory = brewingStand.getInventory();
            slots.add(new InventorySlot((Inventory)brewerInventory, this.selectedSlot.ordinal()));
        }
        return slots.toArray(new Slot[0]);
    }

    @Override
    public Class<? extends Slot> getReturnType() {
        return InventorySlot.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.selectedSlot.toString + " slot of " + this.getExpr().toString(event, debug);
    }

    private static enum BrewingSlot {
        FIRST("[brewing stand['s]] (first|1st) bottle", "brewing stand first bottle"),
        SECOND("[brewing stand['s]] (second|2nd) bottle", "brewing stand second bottle"),
        THIRD("[brewing stand['s]] (third|3rd) bottle", "brewing stand third bottle"),
        INGREDIENT("brewing [stand] ingredient", "brewing stand ingredient"),
        FUEL("brewing [stand] fuel", "brewing stand fuel");

        private final String pattern;
        private final String toString;

        private BrewingSlot(String pattern, String toString) {
            this.pattern = pattern;
            this.toString = toString;
        }
    }

    private final class BrewingEventSlot
    extends InventorySlot {
        private final Event event;

        public BrewingEventSlot(Event event, BrewerInventory brewerInventory) {
            super((Inventory)brewerInventory, ExprBrewingSlot.this.selectedSlot.ordinal());
            this.event = event;
        }

        @Override
        @Nullable
        public ItemStack getItem() {
            Event event;
            if (ExprBrewingSlot.this.selectedSlot == BrewingSlot.FUEL && (event = this.event) instanceof BrewingStandFuelEvent) {
                BrewingStandFuelEvent brewingStandFuelEvent = (BrewingStandFuelEvent)event;
                ItemStack source = brewingStandFuelEvent.getFuel().clone();
                if (ExprBrewingSlot.this.getTime() != EventValues.TIME_FUTURE || !brewingStandFuelEvent.isConsuming()) {
                    return source;
                }
                if (source.getAmount() <= 1) {
                    return null;
                }
                source.setAmount(source.getAmount() - 1);
                return source;
            }
            return super.getItem();
        }
    }
}

