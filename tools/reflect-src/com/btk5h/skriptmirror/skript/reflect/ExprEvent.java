/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionType
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.util.SimpleExpression
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 */
package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.WrappedEvent;
import org.bukkit.event.Event;

public class ExprEvent
extends SimpleExpression<Event> {
    protected Event[] get(Event e) {
        if (e instanceof WrappedEvent) {
            return new Event[]{((WrappedEvent)e).getEvent()};
        }
        return new Event[]{e};
    }

    public boolean isSingle() {
        return true;
    }

    public Class<? extends Event> getReturnType() {
        return Event.class;
    }

    public String toString(Event e, boolean debug) {
        if (e == null) {
            return "the event";
        }
        return e.getEventName();
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    static {
        Skript.registerExpression(ExprEvent.class, Event.class, (ExpressionType)ExpressionType.SIMPLE, (String[])new String[]{"[the] event"});
    }
}

