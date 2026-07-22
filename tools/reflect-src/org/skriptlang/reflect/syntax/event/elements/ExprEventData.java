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
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.reflect.syntax.event.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.event.BukkitCustomEvent;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;

public class ExprEventData
extends SimpleExpression<Object> {
    private Expression<String> dataIndex;

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent(new Class[]{BukkitCustomEvent.class, EventTriggerEvent.class})) {
            Skript.error((String)"The event data expression can only be used in a custom event");
            return false;
        }
        this.dataIndex = exprs[0];
        return true;
    }

    @Nullable
    protected Object[] get(Event e) {
        String key = (String)this.dataIndex.getSingle(e);
        if (key == null) {
            return null;
        }
        BukkitCustomEvent bukkitCustomEvent = e instanceof BukkitCustomEvent ? (BukkitCustomEvent)e : (BukkitCustomEvent)((EventTriggerEvent)e).getDirectEvent();
        Object data = bukkitCustomEvent.getData(key);
        return new Object[]{data};
    }

    public boolean isSingle() {
        return true;
    }

    public Class<?> getReturnType() {
        return Object.class;
    }

    public String toString(@Nullable Event e, boolean debug) {
        return "event data";
    }

    static {
        Skript.registerExpression(ExprEventData.class, Object.class, (ExpressionType)ExpressionType.COMBINED, (String[])new String[]{"[extra] [event(-| )]data %string%"});
    }
}

