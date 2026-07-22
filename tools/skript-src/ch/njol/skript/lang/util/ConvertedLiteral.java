/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.util;

import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.util.ConvertedExpression;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.ArrayIterator;
import java.util.Iterator;
import java.util.function.Predicate;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.ConverterInfo;
import org.skriptlang.skript.lang.converter.Converters;

public class ConvertedLiteral<F, T>
extends ConvertedExpression<F, T>
implements Literal<T> {
    protected transient T[] data;

    public ConvertedLiteral(Literal<F> source, T[] data, Class<T> to) {
        super(source, to, new ConverterInfo<Object, Object>(source.getReturnType(), to, from -> Converters.convert(from, to), 0));
        this.data = data;
        assert (data.length > 0);
    }

    @Override
    @Nullable
    public <R> Literal<? extends R> getConvertedExpression(Class<R> ... to) {
        if (CollectionUtils.containsSuperclass(to, this.to)) {
            return this;
        }
        return ((Literal)this.source).getConvertedExpression((Class[])to);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return Classes.toString(this.data, this.getAnd());
    }

    @Override
    public T[] getArray() {
        return this.data;
    }

    @Override
    public T[] getAll() {
        return this.data;
    }

    @Override
    public T[] getArray(Event event) {
        return this.getArray();
    }

    @Override
    public T getSingle() {
        if (this.getAnd() && this.data.length > 1) {
            throw new SkriptAPIException("Call to getSingle on a non-single expression");
        }
        return CollectionUtils.getRandom(this.data);
    }

    @Override
    public T getSingle(Event event) {
        return this.getSingle();
    }

    @Override
    @Nullable
    public Iterator<T> iterator(Event event) {
        return new ArrayIterator<T>(this.data);
    }

    @Override
    public boolean check(Event event, Predicate<? super T> checker) {
        return SimpleExpression.check(this.data, checker, false, this.getAnd());
    }

    @Override
    public boolean check(Event event, Predicate<? super T> checker, boolean negated) {
        return SimpleExpression.check(this.data, checker, negated, this.getAnd());
    }
}

