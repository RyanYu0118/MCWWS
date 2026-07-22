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
package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.ObjectWrapper;
import com.btk5h.skriptmirror.util.JavaTypeWrapper;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprClassReference
extends SimpleExpression<ObjectWrapper> {
    private JavaTypeWrapper javaTypeWrapper;

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.javaTypeWrapper = JavaTypeWrapper.of(exprs[0], parseResult.regexes);
        return true;
    }

    @Nullable
    protected ObjectWrapper[] get(Event e) {
        JavaType javaType = this.javaTypeWrapper.get(e);
        if (javaType == null) {
            return null;
        }
        return new ObjectWrapper[]{ObjectWrapper.create(javaType.getJavaClass())};
    }

    public boolean isSingle() {
        return true;
    }

    public Class<? extends ObjectWrapper> getReturnType() {
        return ObjectWrapper.class;
    }

    public String toString(@Nullable Event e, boolean debug) {
        return this.javaTypeWrapper.toString(e, debug) + ".class";
    }

    static {
        Skript.registerExpression(ExprClassReference.class, ObjectWrapper.class, (ExpressionType)ExpressionType.COMBINED, (String[])new String[]{"(<(" + JavaTypeWrapper.PRIMITIVE_PATTERNS + ")>|%-javatype%).class"});
    }
}

