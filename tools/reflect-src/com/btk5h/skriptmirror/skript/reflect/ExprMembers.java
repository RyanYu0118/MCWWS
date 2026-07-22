/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.expressions.base.PropertyExpression
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.util.SimpleExpression
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 */
package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.util.JavaUtil;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;
import org.bukkit.event.Event;

public class ExprMembers
extends SimpleExpression<String> {
    private Expression<Object> target;
    private Function<Class<?>, Stream<? extends Member>> mapper;

    protected String[] get(Event e) {
        return (String[])Arrays.stream(this.target.getArray(e)).map(SkriptMirrorUtil::toClassUnwrapJavaTypes).flatMap(this.mapper).map(JavaUtil::toGenericString).distinct().toArray(String[]::new);
    }

    public boolean isSingle() {
        return false;
    }

    public Class<? extends String> getReturnType() {
        return String.class;
    }

    public String toString(Event e, boolean debug) {
        return "members of " + this.target.toString(e, debug);
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.target = SkriptUtil.defendExpression(exprs[0]);
        switch (parseResult.mark) {
            case 0: {
                this.mapper = JavaUtil::fields;
                break;
            }
            case 1: {
                this.mapper = JavaUtil::methods;
                break;
            }
            case 2: {
                this.mapper = JavaUtil::constructors;
            }
        }
        return SkriptUtil.canInitSafely(this.target);
    }

    static {
        PropertyExpression.register(ExprMembers.class, String.class, (String)"(0\u00a6fields|1\u00a6methods|2\u00a6constructors)", (String)"objects");
    }
}

