/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  org.bukkit.event.HandlerList
 */
package org.skriptlang.reflect.syntax.expression;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.HandlerList;
import org.skriptlang.reflect.syntax.expression.ExpressionGetEvent;

public class ConstantGetEvent
extends ExpressionGetEvent {
    private static final HandlerList handlers = new HandlerList();

    public ConstantGetEvent(int matchedPattern, SkriptParser.ParseResult parseResult) {
        super(null, new Expression[0], matchedPattern, parseResult);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}

