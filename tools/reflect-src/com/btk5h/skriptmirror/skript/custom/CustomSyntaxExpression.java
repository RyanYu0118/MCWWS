/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.util.SimpleExpression
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 */
package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;

public class CustomSyntaxExpression
extends SimpleExpression<Object> {
    private final Expression<?> source;
    private final Event realEvent;
    private final Object[] value;

    public CustomSyntaxExpression(Expression<?> source, Event realEvent) {
        this.source = source;
        this.realEvent = realEvent;
        this.value = source == null ? new Object[]{} : source.getAll(realEvent);
    }

    public static CustomSyntaxExpression wrap(Expression<?> source, Event realEvent) {
        if (source instanceof CustomSyntaxExpression) {
            return (CustomSyntaxExpression)source;
        }
        return new CustomSyntaxExpression(source, realEvent);
    }

    protected Object[] get(Event e) {
        return this.value;
    }

    public boolean isSingle() {
        return this.source == null || this.source.isSingle();
    }

    public Class<?> getReturnType() {
        return this.source == null ? Object.class : this.source.getReturnType();
    }

    public String toString(Event e, boolean debug) {
        return this.source == null ? "" : this.source.toString(this.realEvent, debug);
    }

    public Expression<?> getSource() {
        return this.source;
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        throw new UnsupportedOperationException();
    }
}

