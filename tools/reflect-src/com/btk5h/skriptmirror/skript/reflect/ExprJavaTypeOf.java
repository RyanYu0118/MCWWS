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
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.Arrays;
import org.bukkit.event.Event;

public class ExprJavaTypeOf
extends SimpleExpression<JavaType> {
    private Expression<Object> target;

    protected JavaType[] get(Event e) {
        return (JavaType[])Arrays.stream(this.target.getArray(e)).map(SkriptMirrorUtil::getClass).map(JavaType::new).toArray(JavaType[]::new);
    }

    public boolean isSingle() {
        return this.target.isSingle();
    }

    public Class<? extends JavaType> getReturnType() {
        return JavaType.class;
    }

    public String toString(Event e, boolean debug) {
        return "class of " + this.target.toString(e, debug);
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.target = SkriptUtil.defendExpression(exprs[0]);
        return SkriptUtil.canInitSafely(this.target);
    }

    static {
        PropertyExpression.register(ExprJavaTypeOf.class, JavaType.class, (String)"[java] class[es]", (String)"objects");
    }
}

