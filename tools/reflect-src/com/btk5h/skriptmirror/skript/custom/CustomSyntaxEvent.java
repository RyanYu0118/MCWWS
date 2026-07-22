/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  org.bukkit.event.Event
 */
package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import com.btk5h.skriptmirror.WrappedEvent;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxExpression;
import java.util.Arrays;
import org.bukkit.event.Event;

public abstract class CustomSyntaxEvent
extends WrappedEvent {
    private final Expression<?>[] expressions;
    private final int matchedPattern;
    private final SkriptParser.ParseResult parseResult;

    protected CustomSyntaxEvent(Event event, Expression<?>[] expressions, int matchedPattern, SkriptParser.ParseResult parseResult) {
        super(event);
        this.expressions = (Expression[])Arrays.stream(expressions).map(expr -> CustomSyntaxExpression.wrap(expr, event)).toArray(Expression[]::new);
        this.matchedPattern = matchedPattern;
        this.parseResult = parseResult;
    }

    public Expression<?>[] getExpressions() {
        return this.expressions;
    }

    public int getMatchedPattern() {
        return this.matchedPattern;
    }

    public SkriptParser.ParseResult getParseResult() {
        return this.parseResult;
    }
}

