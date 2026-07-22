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
package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import java.util.Arrays;
import org.bukkit.event.Event;

public class ExprEventClasses
extends SimpleExpression<JavaType> {
    protected JavaType[] get(Event e) {
        return (JavaType[])Arrays.stream(((SyntaxParseEvent)e).getEventClasses()).map(JavaType::new).toArray(JavaType[]::new);
    }

    public boolean isSingle() {
        return false;
    }

    public Class<? extends JavaType> getReturnType() {
        return JavaType.class;
    }

    public String toString(Event e, boolean debug) {
        return "event-classes";
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return this.getParser().isCurrentEvent(SyntaxParseEvent.class);
    }

    static {
        Skript.registerExpression(ExprEventClasses.class, JavaType.class, (ExpressionType)ExpressionType.SIMPLE, (String[])new String[]{"event-classes"});
    }
}

