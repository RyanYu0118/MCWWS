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
import java.lang.reflect.Array;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprNewArray
extends SimpleExpression<ObjectWrapper> {
    private JavaTypeWrapper javaTypeWrapper;
    private Expression<? extends Number> sizeExpression;

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.javaTypeWrapper = JavaTypeWrapper.of(exprs[0], parseResult.regexes);
        this.sizeExpression = exprs[1];
        return true;
    }

    @Nullable
    protected ObjectWrapper[] get(Event e) {
        JavaType javaType = this.javaTypeWrapper.get(e);
        Number length = (Number)this.sizeExpression.getSingle(e);
        if (javaType == null || length == null) {
            return null;
        }
        int size = length.intValue();
        Class<?> clazz = javaType.getJavaClass();
        Object array = Array.newInstance(clazz, size);
        return new ObjectWrapper[]{ObjectWrapper.create(array)};
    }

    public boolean isSingle() {
        return true;
    }

    public Class<? extends ObjectWrapper> getReturnType() {
        return ObjectWrapper.class;
    }

    public String toString(@Nullable Event e, boolean debug) {
        return "new " + this.javaTypeWrapper.toString(e, debug) + "[" + this.sizeExpression.toString(e, debug) + "]";
    }

    static {
        Skript.registerExpression(ExprNewArray.class, ObjectWrapper.class, (ExpressionType)ExpressionType.COMBINED, (String[])new String[]{"new (<(" + JavaTypeWrapper.PRIMITIVE_PATTERNS + ")>|%-javatype%)\\[%number%\\]"});
    }
}

