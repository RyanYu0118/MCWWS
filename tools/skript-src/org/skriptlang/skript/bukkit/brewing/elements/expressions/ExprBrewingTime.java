/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.BrewingStand
 *  org.bukkit.event.Event
 *  org.bukkit.event.block.BrewingStartEvent
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.brewing.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.Event;
import org.bukkit.event.block.BrewingStartEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Brewing Time")
@Description(value={"The remaining brewing time of a brewing stand."})
@Example.Examples(value={@Example(value="set the brewing time of {_block} to 10 seconds"), @Example(value="clear the remaining brewing time of {_block}")})
@Since(value={"2.13"})
public class ExprBrewingTime
extends PropertyExpression<Block, Timespan> {
    private static final boolean BREWING_START_EVENT_1_21 = Skript.methodExists(BrewingStartEvent.class, "setBrewingTime", Integer.TYPE);
    private boolean isEvent = false;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprBrewingTime.infoBuilder(ExprBrewingTime.class, Timespan.class, "[current|remaining] brewing time", "blocks", true).supplier(ExprBrewingTime::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        this.isEvent = this.getParser().isCurrentEvent((Class<? extends Event>)BrewingStartEvent.class);
        return true;
    }

    protected Timespan[] get(Event event, Block[] source) {
        BrewingStand brewingStand;
        BrewingStartEvent brewingStartEvent;
        Block eventBlock;
        ArrayList blocks = new ArrayList(this.getExpr().stream(event).toList());
        ArrayList<Timespan> timespans = new ArrayList<Timespan>();
        if (this.isEvent && event instanceof BrewingStartEvent && blocks.remove(eventBlock = (brewingStartEvent = (BrewingStartEvent)event).getBlock())) {
            if (!Delay.isDelayed(event)) {
                if (BREWING_START_EVENT_1_21) {
                    timespans.add(new Timespan(Timespan.TimePeriod.TICK, brewingStartEvent.getBrewingTime()));
                } else {
                    timespans.add(new Timespan(Timespan.TimePeriod.TICK, brewingStartEvent.getTotalBrewTime()));
                }
            } else {
                brewingStand = (BrewingStand)eventBlock.getState();
                timespans.add(new Timespan(Timespan.TimePeriod.TICK, brewingStand.getBrewingTime()));
            }
        }
        for (Block block : blocks) {
            BlockState blockState = block.getState();
            if (!(blockState instanceof BrewingStand)) continue;
            brewingStand = (BrewingStand)blockState;
            timespans.add(new Timespan(Timespan.TimePeriod.TICK, brewingStand.getBrewingTime()));
        }
        return (Timespan[])timespans.toArray(Timespan[]::new);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> CollectionUtils.array(Timespan.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        BrewingStartEvent brewingStartEvent;
        Block eventBlock;
        int providedValue = delta != null ? (int)((Timespan)delta[0]).getAs(Timespan.TimePeriod.TICK) : 0;
        ArrayList blocks = new ArrayList(this.getExpr().stream(event).toList());
        if (event instanceof BrewingStartEvent && blocks.remove(eventBlock = (brewingStartEvent = (BrewingStartEvent)event).getBlock())) {
            if (BREWING_START_EVENT_1_21) {
                this.changeBrewingTime(providedValue, mode, () -> ((BrewingStartEvent)brewingStartEvent).getBrewingTime(), arg_0 -> ((BrewingStartEvent)brewingStartEvent).setBrewingTime(arg_0));
            } else {
                this.changeBrewingTime(providedValue, mode, () -> ((BrewingStartEvent)brewingStartEvent).getTotalBrewTime(), arg_0 -> ((BrewingStartEvent)brewingStartEvent).setTotalBrewTime(arg_0));
            }
        }
        for (Block block : blocks) {
            BlockState blockState = block.getState();
            if (!(blockState instanceof BrewingStand)) continue;
            BrewingStand brewingStand = (BrewingStand)blockState;
            this.changeBrewingTime(providedValue, mode, () -> ((BrewingStand)brewingStand).getBrewingTime(), arg_0 -> ((BrewingStand)brewingStand).setBrewingTime(arg_0));
        }
    }

    private void changeBrewingTime(int providedValue, Changer.ChangeMode mode, Supplier<Integer> getter, Consumer<Integer> setter) {
        if (mode == Changer.ChangeMode.REMOVE) {
            providedValue = -providedValue;
        }
        setter.accept(switch (mode) {
            case Changer.ChangeMode.SET -> Math2.fit(0, providedValue, Integer.MAX_VALUE);
            case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> Math2.fit(0, getter.get() + providedValue, Integer.MAX_VALUE);
            case Changer.ChangeMode.DELETE -> 0;
            default -> throw new IllegalStateException("Unexpected mode: " + String.valueOf((Object)mode));
        });
    }

    @Override
    public Class<Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return new SyntaxStringBuilder(event, debug).append("the current brewing time of", this.getExpr()).toString();
    }
}

