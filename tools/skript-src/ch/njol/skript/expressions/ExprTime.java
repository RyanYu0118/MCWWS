/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.World
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Time;
import ch.njol.skript.util.Timeperiod;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Time")
@Description(value={"The <a href='#time'>time</a> of a world.", "Use the \"minecraft <a href='#timespan'>timespan</a>\" syntax to change the time according to Minecraft's time intervals.", "Since Minecraft uses discrete intervals for time (ticks), changing the time by real-world minutes or real-world seconds only changes it approximately.", "Removing an amount of time from a world's time will move the clock forward a day."})
@Example.Examples(value={@Example(value="set time of world \"world\" to 2:00"), @Example(value="add 2 minecraft hours to time of world \"world\""), @Example(value="add 54 real seconds to time of world \"world\" # approximately 1 minecraft hour")})
@Since(value={"1.0"})
public class ExprTime
extends PropertyExpression<World, Time> {
    private static final int TIME_TO_TIMESPAN_OFFSET = 18000;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.setExpr(expressions[0]);
        return true;
    }

    protected Time[] get(Event event, World[] worlds) {
        return this.get(worlds, world -> new Time((int)world.getTime()));
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case ADD: 
            case REMOVE: {
                return CollectionUtils.array(Time.class, Timespan.class);
            }
            case SET: {
                return CollectionUtils.array(Time.class, Timeperiod.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (delta == null) {
            return;
        }
        Object time = delta[0];
        if (time == null) {
            return;
        }
        World[] worlds = (World[])this.getExpr().getArray(event);
        long ticks = 0L;
        if (time instanceof Time) {
            ticks = mode != Changer.ChangeMode.SET ? (long)(((Time)time).getTicks() - 18000) : (long)((Time)time).getTicks();
        } else if (time instanceof Timespan) {
            ticks = ((Timespan)time).getAs(Timespan.TimePeriod.TICK);
        } else if (time instanceof Timeperiod) {
            ticks = ((Timeperiod)time).start;
        }
        block5: for (World world : worlds) {
            switch (mode) {
                case ADD: {
                    world.setTime(world.getTime() + ticks);
                    continue block5;
                }
                case REMOVE: {
                    world.setTime(world.getTime() - ticks);
                    continue block5;
                }
                case SET: {
                    world.setTime(ticks);
                }
            }
        }
    }

    @Override
    public Class<Time> getReturnType() {
        return Time.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the time in " + this.getExpr().toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprTime.class, Time.class, ExpressionType.PROPERTY, "[the] time[s] [([with]in|of) %worlds%]", "%worlds%'[s] time[s]");
    }
}

