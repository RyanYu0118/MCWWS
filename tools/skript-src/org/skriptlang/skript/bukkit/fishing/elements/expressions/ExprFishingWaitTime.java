/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.FishHook
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerFishEvent
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.fishing.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import org.bukkit.entity.FishHook;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Fishing Wait Time")
@Description(value={"Returns the minimum and/or maximum waiting time of the fishing hook. ", "Default minimum value is 5 seconds and maximum is 30 seconds, before lure is applied."})
@Example(value="on fishing line cast:\n\tset min fish waiting time to 10 seconds\n\tset max fishing waiting time to 20 seconds\n")
@Events(value={"Fishing"})
@Since(value={"2.10"})
public class ExprFishingWaitTime
extends SimpleExpression<Timespan> {
    private static final int DEFAULT_MINIMUM_TICKS = 100;
    private static final int DEFAULT_MAXIMUM_TICKS = 600;
    private boolean isMin;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprFishingWaitTime.class, Timespan.class).addPatterns("(min:min[imum]|max[imum]) fish[ing] wait[ing] time")).supplier(ExprFishingWaitTime::new)).priority(EventValueExpression.DEFAULT_PRIORITY)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent((Class<? extends Event>)PlayerFishEvent.class)) {
            Skript.error("The 'fishing wait time' expression can only be used in a fishing event.");
            return false;
        }
        this.isMin = parseResult.hasTag("min");
        return true;
    }

    protected Timespan @Nullable [] get(Event event) {
        if (!(event instanceof PlayerFishEvent)) {
            return null;
        }
        PlayerFishEvent fishEvent = (PlayerFishEvent)event;
        if (this.isMin) {
            return new Timespan[]{new Timespan(Timespan.TimePeriod.TICK, fishEvent.getHook().getMinWaitTime())};
        }
        return new Timespan[]{new Timespan(Timespan.TimePeriod.TICK, fishEvent.getHook().getMaxWaitTime())};
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        Class[] classArray;
        switch (mode) {
            case ADD: 
            case REMOVE: 
            case SET: 
            case RESET: {
                Class[] classArray2 = new Class[1];
                classArray = classArray2;
                classArray2[0] = Timespan.class;
                break;
            }
            default: {
                classArray = null;
            }
        }
        return classArray;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (!(event instanceof PlayerFishEvent)) {
            return;
        }
        PlayerFishEvent fishEvent = (PlayerFishEvent)event;
        FishHook hook = fishEvent.getHook();
        int ticks = mode == Changer.ChangeMode.RESET ? (this.isMin ? 100 : 600) : (int)((Timespan)delta[0]).getAs(Timespan.TimePeriod.TICK);
        switch (mode) {
            case SET: 
            case RESET: {
                if (this.isMin) {
                    hook.setMinWaitTime(Math.max(0, ticks));
                    break;
                }
                hook.setMaxWaitTime(Math.max(0, ticks));
                break;
            }
            case ADD: {
                if (this.isMin) {
                    hook.setMinWaitTime(Math.max(0, hook.getMinWaitTime() + ticks));
                    break;
                }
                hook.setMaxWaitTime(Math.max(0, hook.getMaxWaitTime() + ticks));
                break;
            }
            case REMOVE: {
                if (this.isMin) {
                    hook.setMinWaitTime(Math.max(0, hook.getMinWaitTime() - ticks));
                    break;
                }
                hook.setMaxWaitTime(Math.max(0, hook.getMaxWaitTime() - ticks));
            }
        }
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (this.isMin ? "minimum" : "maximum") + " fishing waiting time";
    }
}

