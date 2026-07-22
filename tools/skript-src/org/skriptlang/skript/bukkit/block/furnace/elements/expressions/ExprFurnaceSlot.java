/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.Furnace
 *  org.bukkit.event.Event
 *  org.bukkit.event.block.BlockEvent
 *  org.bukkit.event.inventory.FurnaceBurnEvent
 *  org.bukkit.event.inventory.FurnaceExtractEvent
 *  org.bukkit.event.inventory.FurnaceSmeltEvent
 *  org.bukkit.event.inventory.FurnaceStartSmeltEvent
 *  org.bukkit.inventory.FurnaceInventory
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.block.furnace.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.slot.InventorySlot;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Furnace Slot")
@Description(value={"A slot of a furnace, i.e. either the ore, fuel or result slot."})
@Example.Examples(value={@Example(value="set the fuel slot of the clicked block to a lava bucket"), @Example(value="set the block's ore slot to 64 iron ore"), @Example(value="clear the result slot of the block"), @Example(value="on smelt:\n\tif the fuel slot is charcoal:\n\t\tadd 5 seconds to the burn time\n")})
@Events(value={"smelt", "fuel burn"})
@Since(value={"1.0, 2.8.0 (syntax rework)"})
public class ExprFurnaceSlot
extends SimpleExpression<Slot> {
    private static final FurnaceSlot[] furnaceSlots = FurnaceSlot.values();
    @Nullable
    private Expression<Block> blocks;
    private FurnaceSlot selectedSlot;
    private boolean isEvent;

    public static void register(SyntaxRegistry registry) {
        String[] patterns = new String[furnaceSlots.length * 2];
        for (FurnaceSlot slot : furnaceSlots) {
            patterns[2 * slot.ordinal()] = "[the] " + slot.pattern + " slot[s] [of %blocks%]";
            patterns[2 * slot.ordinal() + 1] = "%blocks%'[s] " + slot.pattern + " slot[s]";
        }
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprFurnaceSlot.class, Slot.class).addPatterns(patterns)).supplier(ExprFurnaceSlot::new)).priority(PropertyExpression.DEFAULT_PRIORITY)).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.selectedSlot = furnaceSlots[(int)Math2.floor(matchedPattern / 2)];
        if (exprs[0] != null) {
            this.blocks = exprs[0];
        } else {
            if (!this.getParser().isCurrentEvent(FurnaceBurnEvent.class, FurnaceStartSmeltEvent.class, FurnaceExtractEvent.class, FurnaceSmeltEvent.class)) {
                Skript.error("There's no furnace in a " + this.getParser().getCurrentEventName() + " event.");
                return false;
            }
            this.isEvent = true;
        }
        return true;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    protected Slot @Nullable [] get(Event event) {
        Block[] blocks;
        if (this.isEvent) {
            blocks = new Block[1];
            if (!(event instanceof BlockEvent)) return new Slot[0];
            BlockEvent blockEvent = (BlockEvent)event;
            blocks[0] = blockEvent.getBlock();
        } else {
            assert (this.blocks != null);
            blocks = this.blocks.getArray(event);
        }
        ArrayList<InventorySlot> slots = new ArrayList<InventorySlot>();
        for (Block block : blocks) {
            BlockState state = block.getState();
            if (!(state instanceof Furnace)) continue;
            FurnaceInventory furnaceInventory = ((Furnace)state).getInventory();
            if (this.isEvent && !Delay.isDelayed(event)) {
                slots.add(new FurnaceEventSlot(event, furnaceInventory));
                continue;
            }
            slots.add(new InventorySlot((Inventory)furnaceInventory, this.selectedSlot.ordinal()));
        }
        return slots.toArray(new Slot[0]);
    }

    @Override
    public boolean isSingle() {
        if (this.isEvent) {
            return true;
        }
        assert (this.blocks != null);
        return this.blocks.isSingle();
    }

    @Override
    public Class<? extends Slot> getReturnType() {
        return InventorySlot.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.selectedSlot.toString + " slot of " + (this.isEvent ? event.getEventName() : this.blocks.toString(event, debug));
    }

    @Override
    public boolean setTime(int time) {
        if (this.isEvent) {
            if (this.selectedSlot == FurnaceSlot.FUEL) {
                return this.setTime(time, (Class<? extends Event>)FurnaceBurnEvent.class);
            }
            return this.setTime(time, (Class<? extends Event>)FurnaceSmeltEvent.class);
        }
        return false;
    }

    private static enum FurnaceSlot {
        INPUT("(ore|input)", "input"),
        FUEL("fuel", "fuel"),
        OUTPUT("(result|output)", "output");

        private String pattern;
        private String toString;

        private FurnaceSlot(String pattern, String toString) {
            this.pattern = pattern;
            this.toString = toString;
        }
    }

    private final class FurnaceEventSlot
    extends InventorySlot {
        private final Event event;

        public FurnaceEventSlot(Event event, FurnaceInventory furnaceInventory) {
            super((Inventory)furnaceInventory, ExprFurnaceSlot.this.selectedSlot.ordinal());
            this.event = event;
        }

        @Override
        @Nullable
        public ItemStack getItem() {
            return switch (ExprFurnaceSlot.this.selectedSlot.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> {
                    Event var2_1 = this.event;
                    if (var2_1 instanceof FurnaceSmeltEvent) {
                        FurnaceSmeltEvent furnaceSmeltEvent = (FurnaceSmeltEvent)var2_1;
                        ItemStack source = furnaceSmeltEvent.getSource().clone();
                        if (ExprFurnaceSlot.this.getTime() != EventValues.TIME_FUTURE) {
                            yield source;
                        }
                        source.setAmount(source.getAmount() - 1);
                        yield source;
                    }
                    yield super.getItem();
                }
                case 1 -> {
                    Event source = this.event;
                    if (source instanceof FurnaceBurnEvent) {
                        FurnaceBurnEvent furnaceBurnEvent = (FurnaceBurnEvent)source;
                        ItemStack fuel = furnaceBurnEvent.getFuel().clone();
                        if (ExprFurnaceSlot.this.getTime() != EventValues.TIME_FUTURE) {
                            yield fuel;
                        }
                        Material newMaterial = fuel.getType() == Material.LAVA_BUCKET ? Material.BUCKET : Material.AIR;
                        fuel.setAmount(fuel.getAmount() - 1);
                        if (fuel.getAmount() == 0) {
                            fuel = new ItemStack(newMaterial);
                        }
                        yield fuel;
                    }
                    yield super.getItem();
                }
                case 2 -> {
                    Event fuel = this.event;
                    if (fuel instanceof FurnaceSmeltEvent) {
                        FurnaceSmeltEvent furnaceSmeltEvent = (FurnaceSmeltEvent)fuel;
                        ItemStack result = furnaceSmeltEvent.getResult().clone();
                        ItemStack currentResult = ((FurnaceInventory)this.getInventory()).getResult();
                        if (currentResult != null) {
                            currentResult = currentResult.clone();
                        }
                        if (ExprFurnaceSlot.this.getTime() != EventValues.TIME_FUTURE) {
                            yield currentResult;
                        }
                        if (currentResult != null && currentResult.isSimilar(result)) {
                            currentResult.setAmount(currentResult.getAmount() + result.getAmount());
                            yield currentResult;
                        }
                        yield result;
                    }
                    yield super.getItem();
                }
            };
        }

        @Override
        public void setItem(@Nullable ItemStack item) {
            Event event;
            if (ExprFurnaceSlot.this.selectedSlot == FurnaceSlot.OUTPUT && (event = this.event) instanceof FurnaceSmeltEvent) {
                FurnaceSmeltEvent furnaceSmeltEvent = (FurnaceSmeltEvent)event;
                furnaceSmeltEvent.setResult(item != null ? item : new ItemStack(Material.AIR));
            } else if (ExprFurnaceSlot.this.getTime() == EventValues.TIME_FUTURE) {
                Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), () -> FurnaceEventSlot.super.setItem(item));
            } else {
                super.setItem(item);
            }
        }
    }
}

