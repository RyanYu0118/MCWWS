/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 */
package org.skriptlang.reflect.syntax.expression;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ExpressionChangeEvent
extends CustomSyntaxEvent {
    private static final HandlerList handlers = new HandlerList();
    private final Object[] delta;

    public ExpressionChangeEvent(Event event, Expression<?>[] expressions, int matchedPattern, SkriptParser.ParseResult parseResult, Object[] delta) {
        super(event, expressions, matchedPattern, parseResult);
        this.delta = delta;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Object[] getDelta() {
        return this.delta;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}

