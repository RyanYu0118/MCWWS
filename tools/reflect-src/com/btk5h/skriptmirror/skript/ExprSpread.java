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
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.bukkit.event.Event;
import org.skriptlang.skript.lang.converter.Converters;

public class ExprSpread<T>
implements Expression<T> {
    private Expression<Object> object;
    private final ExprSpread<?> source;
    private final Class<? extends T>[] types;
    private final Class<T> superType;

    public ExprSpread() {
        this(null, Object.class);
    }

    @SafeVarargs
    private ExprSpread(ExprSpread<?> source, Class<? extends T> ... types) {
        this.source = source;
        if (source != null) {
            this.object = source.object;
        }
        this.types = types;
        this.superType = Utils.getSuperType((Class[])types);
    }

    public T getSingle(Event e) {
        throw new UnsupportedOperationException();
    }

    public T[] getArray(Event e) {
        return this.getAll(e);
    }

    public T[] getAll(Event e) {
        Object[] obj = ObjectWrapper.unwrapIfNecessary(this.object.getSingle(e));
        if (obj instanceof Collection) {
            obj = ((Collection)obj).toArray();
        } else if (obj instanceof Iterable) {
            obj = this.toArray(((Iterable)obj).iterator());
        } else if (obj instanceof Stream) {
            obj = this.toArray(((Stream)obj).iterator());
        } else if (obj instanceof Iterator) {
            obj = this.toArray((Iterator)obj);
        }
        if (obj == null || !obj.getClass().isArray()) {
            return JavaUtil.newArray(this.superType, 0);
        }
        obj = JavaUtil.boxPrimitiveArray(obj);
        return Converters.convert((Object[])obj, (Class[])this.types, this.superType);
    }

    private Object[] toArray(Iterator<?> iter) {
        ArrayList list = new ArrayList();
        iter.forEachRemaining(list::add);
        return list.toArray();
    }

    public boolean isSingle() {
        return false;
    }

    public boolean check(Event e, Predicate<? super T> c, boolean negated) {
        return SimpleExpression.check((Object[])this.getAll(e), c, (boolean)negated, (boolean)this.getAnd());
    }

    public boolean check(Event e, Predicate<? super T> c) {
        return SimpleExpression.check((Object[])this.getAll(e), c, (boolean)false, (boolean)this.getAnd());
    }

    public <R> Expression<? extends R> getConvertedExpression(Class<R>[] to) {
        return new ExprSpread<R>(this, to);
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
        return null;
    }

    public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
        throw new UnsupportedOperationException();
    }

    public String toString(Event e, boolean debug) {
        return "spread " + this.object.toString(e, debug);
    }

    public String toString() {
        return this.toString(null, false);
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.object = SkriptUtil.defendExpression(exprs[0]);
        return SkriptUtil.canInitSafely(this.object);
    }

    static {
        Skript.registerExpression(ExprSpread.class, Object.class, (ExpressionType)ExpressionType.COMBINED, (String[])new String[]{"...%object%", "\u2026%object%"});
    }
}

