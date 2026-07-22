/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.util;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.ConvertedKeyProviderExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.ConverterInfo;
import org.skriptlang.skript.lang.converter.Converters;

public class ConvertedExpression<F, T>
implements Expression<T> {
    protected Expression<? extends F> source;
    protected Class<T> to;
    protected Class<T>[] toExact;
    final Converter<? super F, ? extends T> converter;
    private final Collection<ConverterInfo<? super F, ? extends T>> converterInfos;
    private final Class<? extends T>[] returnTypes;
    @Nullable
    private ClassInfo<? super T> returnTypeInfo;

    public ConvertedExpression(Expression<? extends F> source, Class<T> to, ConverterInfo<? super F, ? extends T> info) {
        this.source = source;
        this.to = to;
        this.toExact = new Class[]{to};
        this.converter = info.getConverter();
        this.converterInfos = Collections.singleton(info);
        this.returnTypes = new Class[]{info.getTo()};
    }

    public ConvertedExpression(Expression<? extends F> source, Class<T> to, Collection<ConverterInfo<? super F, ? extends T>> infos, boolean performFromCheck) {
        this(source, new Class[]{to}, infos, performFromCheck);
    }

    public ConvertedExpression(Expression<? extends F> source, Class<T>[] toExact, Collection<ConverterInfo<? super F, ? extends T>> infos, boolean performFromCheck) {
        this.source = source;
        this.to = Utils.getSuperType(toExact);
        this.toExact = toExact;
        this.converterInfos = infos;
        this.returnTypes = (Class[])this.converterInfos.stream().map(ConverterInfo::getTo).distinct().toArray(Class[]::new);
        this.converter = fromObject -> {
            for (ConverterInfo<F, T> info : this.converterInfos) {
                T converted;
                if (performFromCheck && !info.getFrom().isInstance(fromObject) || (converted = info.getConverter().convert(fromObject)) == null) continue;
                return converted;
            }
            return null;
        };
    }

    @SafeVarargs
    @Nullable
    public static <F, T> ConvertedExpression<F, T> newInstance(Expression<F> from, Class<T> ... to) {
        assert (!CollectionUtils.containsSuperclass(to, from.getReturnType()));
        ArrayList<ConverterInfo<F, T>> infos = new ArrayList<ConverterInfo<F, T>>();
        for (Class<F> type : from.possibleReturnTypes()) {
            if (CollectionUtils.containsSuperclass(to, type)) {
                Class<F> toType = type;
                infos.add(new ConverterInfo<Object, Object>(type, toType, toType::cast, 0));
                continue;
            }
            for (Class<T> toType : to) {
                ConverterInfo<F, T> converter = Converters.getConverterInfo(type, toType);
                if (converter == null) continue;
                infos.add(converter);
            }
        }
        if (!infos.isEmpty()) {
            ConvertedExpression<? super F, ? extends T> convertedExpression;
            Class[] converterTypes = (Class[])infos.stream().map(ConverterInfo::getTo).distinct().toArray(Class[]::new);
            if (from instanceof KeyProviderExpression) {
                KeyProviderExpression keyProvider = (KeyProviderExpression)from;
                convertedExpression = new ConvertedKeyProviderExpression<F, T>(keyProvider, (Class<T>[])converterTypes, infos, true);
            } else {
                convertedExpression = new ConvertedExpression<F, T>(from, converterTypes, infos, true);
            }
            return convertedExpression;
        }
        return null;
    }

    @Override
    public final boolean init(Expression<?>[] vars, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult matcher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (debug && event == null) {
            return "(" + this.source.toString(event, debug) + " >> " + String.valueOf(this.converter) + ": " + this.converterInfos.stream().map(Object::toString).collect(Collectors.joining(", ")) + ")";
        }
        return this.source.toString(event, debug);
    }

    @Override
    public String toString() {
        return this.toString(null, false);
    }

    @Override
    public Class<T> getReturnType() {
        return this.to;
    }

    @Override
    public Class<? extends T>[] possibleReturnTypes() {
        return this.toExact;
    }

    @Override
    public boolean isSingle() {
        return this.source.isSingle();
    }

    @Override
    @Nullable
    public <R> Expression<? extends R> getConvertedExpression(Class<R> ... to) {
        if (CollectionUtils.containsSuperclass(to, this.to)) {
            return this;
        }
        return this.source.getConvertedExpression(to);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        Class<?>[] validClasses = this.source.acceptChange(mode);
        if (validClasses == null) {
            ClassInfo<T> returnTypeInfo = Classes.getSuperClassInfo(this.getReturnType());
            this.returnTypeInfo = returnTypeInfo;
            Changer<T> changer = returnTypeInfo.getChanger();
            return changer == null ? null : changer.acceptChange(mode);
        }
        return validClasses;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        ClassInfo<T> returnTypeInfo = this.returnTypeInfo;
        if (returnTypeInfo != null) {
            Changer<T> changer = returnTypeInfo.getChanger();
            if (changer != null) {
                changer.change(this.getArray(event), delta, mode);
            }
        } else {
            this.source.change(event, delta, mode);
        }
    }

    @Override
    @Nullable
    public T getSingle(Event event) {
        F value = this.source.getSingle(event);
        if (value == null) {
            return null;
        }
        return this.converter.convert(value);
    }

    @Override
    public T[] getArray(Event event) {
        return Converters.convert(this.source.getArray(event), this.to, this.converter);
    }

    @Override
    public T[] getAll(Event event) {
        return Converters.convert(this.source.getAll(event), this.to, this.converter);
    }

    @Override
    public boolean check(Event event, Predicate<? super T> checker, boolean negated) {
        return negated ^ this.check(event, checker);
    }

    @Override
    public boolean check(Event event, Predicate<? super T> checker) {
        return this.source.check(event, value -> {
            T convertedValue = this.converter.convert(value);
            if (convertedValue == null) {
                return false;
            }
            return checker.test((T)convertedValue);
        });
    }

    @Override
    public boolean getAnd() {
        return this.source.getAnd();
    }

    @Override
    public boolean setTime(int time) {
        return this.source.setTime(time);
    }

    @Override
    public int getTime() {
        return this.source.getTime();
    }

    @Override
    public boolean returnNestedStructures(boolean nested) {
        return this.source.returnNestedStructures(nested);
    }

    @Override
    public boolean returnsNestedStructures() {
        return this.source.returnsNestedStructures();
    }

    @Override
    public boolean isDefault() {
        return this.source.isDefault();
    }

    @Override
    public boolean isLoopOf(String input) {
        return false;
    }

    @Override
    @Nullable
    public Iterator<T> iterator(Event event) {
        final Iterator iterator = this.source.iterator(event);
        if (iterator == null) {
            return null;
        }
        return new Iterator<T>(this){
            @Nullable
            T next = null;
            final /* synthetic */ ConvertedExpression this$0;
            {
                this.this$0 = this$0;
            }

            @Override
            public boolean hasNext() {
                if (this.next != null) {
                    return true;
                }
                while (this.next == null && iterator.hasNext()) {
                    Object value = iterator.next();
                    this.next = value == null ? null : this.this$0.converter.convert(value);
                }
                return this.next != null;
            }

            @Override
            public T next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }
                Object n = this.next;
                this.next = null;
                assert (n != null);
                return n;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Expression<? extends F> getSource() {
        return this.source;
    }

    @Override
    public Expression<? extends T> simplify() {
        Expression<T> convertedExpression = this.source.simplify().getConvertedExpression(this.toExact);
        if (convertedExpression != null) {
            return convertedExpression;
        }
        return this;
    }

    @Override
    public Object @Nullable [] beforeChange(Expression<?> changed, Object @Nullable [] delta) {
        return this.source.beforeChange(changed, delta);
    }
}

