/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.classes.Changer$ChangeMode
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionType
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.util.SimpleExpression
 *  ch.njol.skript.util.Utils
 *  ch.njol.util.Kleenean
 *  ch.njol.util.coll.iterator.ArrayIterator
 *  org.bukkit.event.Event
 *  org.skriptlang.skript.lang.converter.Converters
 */
package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.ArrayIterator;
import com.btk5h.skriptmirror.ObjectWrapper;
import com.btk5h.skriptmirror.util.JavaUtil;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.function.Predicate;
import org.bukkit.event.Event;
import org.skriptlang.skript.lang.converter.Converters;

public class ExprArrayAccess<T>
implements Expression<T> {
    private Expression<ObjectWrapper> arrays;
    private Expression<Number> index;
    private final ExprArrayAccess<?> source;
    private final Class<? extends T>[] types;
    private final Class<T> superType;

    public ExprArrayAccess() {
        this(null, Object.class);
    }

    @SafeVarargs
    private ExprArrayAccess(ExprArrayAccess<?> source, Class<? extends T> ... types) {
        this.source = source;
        if (source != null) {
            this.arrays = source.arrays;
            this.index = source.index;
        }
        this.types = types;
        this.superType = Utils.getSuperType((Class[])types);
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.arrays = exprs[0];
        this.index = exprs[1];
        return true;
    }

    public T getSingle(Event e) {
        ObjectWrapper wrapper = (ObjectWrapper)this.arrays.getSingle(e);
        Number number = (Number)this.index.getSingle(e);
        if (wrapper == null || number == null || !wrapper.isArray()) {
            return null;
        }
        Object array = wrapper.get();
        int length = Array.getLength(array);
        int i = number.intValue();
        if (i < 0 || i >= length) {
            return null;
        }
        return (T)Converters.convert((Object)Array.get(array, i), (Class[])this.types);
    }

    public T[] getArray(Event e) {
        T single = this.getSingle(e);
        if (single == null) {
            return JavaUtil.newArray(this.superType, 0);
        }
        T[] all = JavaUtil.newArray(this.superType, 1);
        all[0] = single;
        return all;
    }

    public T[] getAll(Event e) {
        return this.getArray(e);
    }

    public boolean isSingle() {
        return true;
    }

    public boolean check(Event e, Predicate<? super T> c, boolean negated) {
        return SimpleExpression.check((Object[])this.getAll(e), c, (boolean)negated, (boolean)this.getAnd());
    }

    public boolean check(Event e, Predicate<? super T> c) {
        return SimpleExpression.check((Object[])this.getAll(e), c, (boolean)false, (boolean)this.getAnd());
    }

    public <R> Expression<? extends R> getConvertedExpression(Class<R>[] to) {
        return new ExprArrayAccess<R>(this, to);
    }

    public Class<? extends T> getReturnType() {
        return this.superType;
    }

    public boolean getAnd() {
        return true;
    }

    public boolean setTime(int time) {
        return false;
    }

    public int getTime() {
        return 0;
    }

    public boolean isDefault() {
        return false;
    }

    public Iterator<? extends T> iterator(Event e) {
        return new ArrayIterator((Object[])this.getAll(e));
    }

    public boolean isLoopOf(String s) {
        return false;
    }

    public Expression<?> getSource() {
        return this.source == null ? this : this.source;
    }

    public Expression<? extends T> simplify() {
        return this;
    }

    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE) {
            return new Class[]{Object.class};
        }
        return null;
    }

    public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
        ObjectWrapper wrapper = (ObjectWrapper)this.arrays.getSingle(e);
        Number number = (Number)this.index.getSingle(e);
        if (wrapper == null || number == null || !wrapper.isArray()) {
            return;
        }
        Object array = wrapper.get();
        int length = Array.getLength(array);
        int i = number.intValue();
        if (i < 0 || i >= length) {
            return;
        }
        switch (mode) {
            case SET: {
                Class<?> to = array.getClass().getComponentType();
                if (!JavaUtil.canConvert(delta[0], to)) break;
                Object converted = JavaUtil.convert(delta[0], to);
                Array.set(array, i, converted);
                break;
            }
            case DELETE: {
                Array.set(array, i, null);
            }
        }
    }

    public String toString(Event e, boolean debug) {
        return this.arrays.toString(e, debug) + "[" + this.index.toString(e, debug) + "]";
    }

    public String toString() {
        return this.toString(null, false);
    }

    static {
        Skript.registerExpression(ExprArrayAccess.class, Object.class, (ExpressionType)ExpressionType.COMBINED, (String[])new String[]{"%javaobject%\\[%number%\\]"});
    }
}

