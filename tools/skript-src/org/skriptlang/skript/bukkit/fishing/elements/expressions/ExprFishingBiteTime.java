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

@Name(value="Fishing Bite Time")
@Description(value={"Returns the time it takes a fish to bite the fishing hook, after it started approaching the hook.", "May return a timespan of 0 seconds. If modifying the value, it should be at least 1 tick."})
@Example(value="on fish approach:\n\tset fishing bite time to 5 seconds\n")
@Events(value={"Fishing"})
@Since(value={"2.10"})
public class ExprFishingBiteTime
extends SimpleExpression<Timespan> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprFishingBiteTime.class, Timespan.class).addPatterns("fish[ing] bit(e|ing) [wait] time")).supplier(ExprFishingBiteTime::new)).priority(EventValueExpression.DEFAULT_PRIORITY)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent((Class<? extends Event>)PlayerFishEvent.class)) {
            Skript.error("The 'fishing bite time' expression can only be used in a fishing event.");
            return false;
        }
        return true;
    }

    protected Timespan @Nullable [] get(Event event) {
        if (!(event instanceof PlayerFishEvent)) {
            return null;
        }
        PlayerFishEvent fishEvent = (PlayerFishEvent)event;
        return new Timespan[]{new Timespan(Timespan.TimePeriod.TICK, fishEvent.getHook().getTimeUntilBite())};
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        Class[] classArray;
        switch (mode) {
            case ADD: 
            case REMOVE: 
            case SET: {
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
        assert (delta != null);
        int ticks = (int)((Timespan)delta[0]).getAs(Timespan.TimePeriod.TICK);
        switch (mode) {
            case SET: {
                hook.setTimeUntilBite(Math.max(1, ticks));
                break;
            }
            case ADD: {
                hook.setTimeUntilBite(Math.max(1, hook.getTimeUntilBite() + ticks));
                break;
            }
            case REMOVE: {
                hook.setTimeUntilBite(Math.max(1, hook.getTimeUntilBite() - ticks));
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
        return "fishing bite time";
    }
}

