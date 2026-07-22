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
package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.Null;
import com.btk5h.skriptmirror.ObjectWrapper;
import com.btk5h.skriptmirror.util.JavaUtil;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.bukkit.event.Event;

public class ExprCollect
extends SimpleExpression<ObjectWrapper> {
    private Expression<Object> objects;
    private Expression<JavaType> type;

    protected ObjectWrapper[] get(Event e) {
        JavaType componentType;
        Stream<Object> objectStream = Arrays.stream(this.objects.getArray(e)).map(o -> o instanceof Null ? null : o).map(ObjectWrapper::unwrapIfNecessary);
        if (this.type != null && (componentType = (JavaType)this.type.getSingle(e)) != null) {
            objectStream = objectStream.filter(o -> o == null || componentType.getJavaClass().isInstance(o));
        }
        Object[] items = objectStream.toArray();
        ?[] castedItems = JavaUtil.newArray(ExprCollect.getCommonSuperclass(items), items.length);
        System.arraycopy(items, 0, castedItems, 0, items.length);
        return new ObjectWrapper[]{ObjectWrapper.create(castedItems)};
    }

    private static Class<?> getCommonSuperclass(Object[] objects) {
        Optional<Object> firstNonnull = Arrays.stream(objects).filter(Objects::nonNull).findFirst();
        if (firstNonnull.isPresent()) {
            return Arrays.stream(objects).filter(Objects::nonNull).map(Object::getClass).map(o -> o).reduce(firstNonnull.get().getClass(), ExprCollect::getCommonSuperclass);
        }
        return Object.class;
    }

    private static Class<?> getCommonSuperclass(Class<?> c1, Class<?> c2) {
        while (!c1.isAssignableFrom(c2)) {
            c1 = c1.getSuperclass();
        }
        return c1;
    }

    public boolean isSingle() {
        return true;
    }

    public Class<? extends ObjectWrapper> getReturnType() {
        return ObjectWrapper.class;
    }

    public String toString(Event e, boolean debug) {
        return "array of " + this.objects.toString(e, debug);
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.objects = SkriptUtil.defendExpression(exprs[0]);
        this.type = exprs[1];
        return SkriptUtil.canInitSafely(this.objects);
    }

    static {
        Skript.registerExpression(ExprCollect.class, ObjectWrapper.class, (ExpressionType)ExpressionType.COMBINED, (String[])new String[]{"\\[%objects%[ as %-javatype%[ ]]\\]"});
    }
}

