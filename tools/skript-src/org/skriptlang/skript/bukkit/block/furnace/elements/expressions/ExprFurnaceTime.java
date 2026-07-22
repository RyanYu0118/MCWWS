/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.Furnace
 *  org.bukkit.event.Event
 *  org.bukkit.event.inventory.FurnaceBurnEvent
 *  org.bukkit.event.inventory.FurnaceExtractEvent
 *  org.bukkit.event.inventory.FurnaceSmeltEvent
 *  org.bukkit.event.inventory.FurnaceStartSmeltEvent
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.block.furnace.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Furnace Times")
@Description(value={"The cook time, total cook time, and burn time of a furnace. Can be changed.", "<ul>", "<li>cook time: The amount of time an item has been smelting for.</li>", "<li>total cook time: The amount of time required to finish smelting an item.</li>", "<li>burn time: The amount of time left for the current fuel until consumption of another fuel item.</li>", "</ul>"})
@Example.Examples(value={@Example(value="set the cooking time of {_block} to 10"), @Example(value="set the total cooking time of {_block} to 50"), @Example(value="set the fuel burning time of {_block} to 100"), @Example(value="on smelt:\n\tif the fuel slot is charcoal:\n\t\tadd 5 seconds to the fuel burn time\n")})
@Since(value={"2.10"})
public class ExprFurnaceTime
extends PropertyExpression<Block, Timespan> {
    private static final FurnaceExpressions[] furnaceExprs = FurnaceExpressions.values();
    private FurnaceExpressions type;
    private boolean explicitlyBlock = false;

    public static void register(SyntaxRegistry registry) {
        int size = furnaceExprs.length;
        String[] patterns = new String[size * 2];
        for (FurnaceExpressions value : furnaceExprs) {
            patterns[2 * value.ordinal()] = "[the] [furnace] " + value.pattern + " [of %blocks%]";
            patterns[2 * value.ordinal() + 1] = "%blocks%'[s]" + value.pattern;
        }
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprFurnaceTime.class, Timespan.class).addPatterns(patterns)).supplier(ExprFurnaceTime::new)).priority(PropertyExpression.DEFAULT_PRIORITY)).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.type = furnaceExprs[matchedPattern / 2];
        if (exprs[0] != null) {
            this.explicitlyBlock = true;
            this.setExpr(exprs[0]);
        } else {
            if (!this.getParser().isCurrentEvent(FurnaceBurnEvent.class, FurnaceStartSmeltEvent.class, FurnaceExtractEvent.class, FurnaceSmeltEvent.class)) {
                Skript.error("There's no furnace in a '" + this.getParser().getCurrentEventName() + "' event.");
                return false;
            }
            this.explicitlyBlock = false;
            this.setExpr(new EventValueExpression<Block>(Block.class));
        }
        return true;
    }

    protected Timespan @Nullable [] get(Event event, Block[] source) {
        return this.get(source, block -> {
            BlockState patt0$temp;
            if (block == null || !((patt0$temp = block.getState()) instanceof Furnace)) {
                return null;
            }
            Furnace furnace = (Furnace)patt0$temp;
            switch (this.type.ordinal()) {
                case 0: {
                    return new Timespan(Timespan.TimePeriod.TICK, furnace.getCookTime());
                }
                case 1: {
                    FurnaceStartSmeltEvent startEvent;
                    if (event instanceof FurnaceStartSmeltEvent && block.equals((Object)(startEvent = (FurnaceStartSmeltEvent)event).getBlock())) {
                        return new Timespan(Timespan.TimePeriod.TICK, startEvent.getTotalCookTime());
                    }
                    return new Timespan(Timespan.TimePeriod.TICK, furnace.getCookTimeTotal());
                }
                case 2: {
                    FurnaceBurnEvent burnEvent;
                    if (event instanceof FurnaceBurnEvent && block.equals((Object)(burnEvent = (FurnaceBurnEvent)event).getBlock())) {
                        return new Timespan(Timespan.TimePeriod.TICK, burnEvent.getBurnTime());
                    }
                    return new Timespan(Timespan.TimePeriod.TICK, furnace.getBurnTime());
                }
            }
            return null;
        });
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.REMOVE_ALL || mode == Changer.ChangeMode.RESET) {
            return null;
        }
        return CollectionUtils.array(Timespan.class);
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Object object;
        int providedTime = 0;
        if (delta != null && (object = delta[0]) instanceof Timespan) {
            Timespan span = (Timespan)object;
            providedTime = (int)span.get(Timespan.TimePeriod.TICK);
        }
        int finalTime = providedTime;
        Function<Integer, Integer> calculateTime = switch (mode) {
            case Changer.ChangeMode.SET -> time -> finalTime;
            case Changer.ChangeMode.ADD -> time -> Math2.fit(0, time + finalTime, Integer.MAX_VALUE);
            case Changer.ChangeMode.REMOVE -> time -> Math2.fit(0, time - finalTime, Integer.MAX_VALUE);
            case Changer.ChangeMode.DELETE -> time -> 0;
            default -> throw new IllegalStateException("Unexpected value: " + String.valueOf((Object)mode));
        };
        Function<Short, Short> calculateTimeShort = switch (mode) {
            case Changer.ChangeMode.SET -> time -> (short)finalTime;
            case Changer.ChangeMode.ADD -> time -> (short)Math2.fit(0, time + finalTime, Short.MAX_VALUE);
            case Changer.ChangeMode.REMOVE -> time -> (short)Math2.fit(0, time - finalTime, Short.MAX_VALUE);
            case Changer.ChangeMode.DELETE -> time -> (short)0;
            default -> throw new IllegalStateException("Unexpected value: " + String.valueOf((Object)mode));
        };
        switch (this.type.ordinal()) {
            case 0: {
                this.changeFurnaces(event, furnace -> ExprFurnaceTime.change(arg_0 -> ((Furnace)furnace).setCookTime(arg_0), () -> ((Furnace)furnace).getCookTime(), calculateTimeShort));
                break;
            }
            case 1: {
                if (!this.explicitlyBlock && event instanceof FurnaceStartSmeltEvent) {
                    FurnaceStartSmeltEvent smeltEvent = (FurnaceStartSmeltEvent)event;
                    ExprFurnaceTime.change(arg_0 -> ((FurnaceStartSmeltEvent)smeltEvent).setTotalCookTime(arg_0), () -> ((FurnaceStartSmeltEvent)smeltEvent).getTotalCookTime(), calculateTime);
                    break;
                }
                this.changeFurnaces(event, furnace -> ExprFurnaceTime.change(arg_0 -> ((Furnace)furnace).setCookTimeTotal(arg_0), () -> ((Furnace)furnace).getCookTimeTotal(), calculateTime));
                break;
            }
            case 2: {
                if (!this.explicitlyBlock && event instanceof FurnaceBurnEvent) {
                    FurnaceBurnEvent burnEvent = (FurnaceBurnEvent)event;
                    ExprFurnaceTime.change(arg_0 -> ((FurnaceBurnEvent)burnEvent).setBurnTime(arg_0), () -> ((FurnaceBurnEvent)burnEvent).getBurnTime(), calculateTime);
                    break;
                }
                this.changeFurnaces(event, furnace -> ExprFurnaceTime.change(arg_0 -> ((Furnace)furnace).setBurnTime(arg_0), () -> ((Furnace)furnace).getBurnTime(), calculateTimeShort));
            }
        }
    }

    private void changeFurnaces(Event event, Consumer<Furnace> changer) {
        for (Block block : (Block[])this.getExpr().getArray(event)) {
            Furnace furnace = (Furnace)block.getState();
            changer.accept(furnace);
            furnace.update(true);
        }
    }

    private static <T extends Number> void change(@NotNull Consumer<T> set, @NotNull Supplier<T> get, @NotNull Function<T, T> calculateTime) {
        set.accept((Number)calculateTime.apply((Number)get.get()));
    }

    @Override
    public boolean isSingle() {
        return this.getExpr().isSingle();
    }

    @Override
    public Class<Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.type.toString + " of " + this.getExpr().toString(event, debug);
    }

    static enum FurnaceExpressions {
        COOKTIME("cook[ing] time", "cook time"),
        TOTALCOOKTIME("total cook[ing] time", "total cook time"),
        BURNTIME("fuel burn[ing] time", "fuel burn time");

        private String pattern;
        private String toString;

        private FurnaceExpressions(String pattern, String toString) {
            this.pattern = pattern;
            this.toString = toString;
        }
    }
}

