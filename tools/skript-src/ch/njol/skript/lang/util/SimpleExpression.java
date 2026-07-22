/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.util;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.util.ConvertedExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.ArrayIterator;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

public abstract class SimpleExpression<T>
implements Expression<T>,
SyntaxRuntimeErrorProducer {
    private int time = 0;
    private Node node;
    @Nullable
    private ClassInfo<?> returnTypeInfo;

    protected SimpleExpression() {
    }

    @Override
    public boolean preInit() {
        this.node = this.getParser().getNode();
        return Expression.super.preInit();
    }

    @Override
    @Nullable
    public final T getSingle(Event event) {
        T[] values = this.getArray(event);
        if (values.length == 0) {
            return null;
        }
        if (values.length > 1) {
            throw new SkriptAPIException("Call to getSingle() on a non-single expression");
        }
        return values[0];
    }

    @Override
    public T[] getAll(Event event) {
        T[] values = this.get(event);
        if (values == null) {
            Object[] emptyArray = (Object[])Array.newInstance(this.getReturnType(), 0);
            assert (emptyArray != null);
            return emptyArray;
        }
        if (values.length == 0) {
            return values;
        }
        int numNonNull = 0;
        for (T value : values) {
            if (value == null) continue;
            ++numNonNull;
        }
        if (numNonNull == values.length) {
            return Arrays.copyOf(values, values.length);
        }
        Object[] valueArray = (Object[])Array.newInstance(this.getReturnType(), numNonNull);
        assert (valueArray != null);
        int i = 0;
        for (T value : values) {
            if (value == null) continue;
            valueArray[i++] = value;
        }
        return valueArray;
    }

    @Override
    public final T[] getArray(Event event) {
        T[] values = this.get(event);
        if (values == null) {
            return (Object[])Array.newInstance(this.getReturnType(), 0);
        }
        if (values.length == 0) {
            return values;
        }
        int numNonNull = 0;
        for (T value : values) {
            if (value == null) continue;
            ++numNonNull;
        }
        if (!this.getAnd()) {
            if (values.length == 1 && values[0] != null) {
                return Arrays.copyOf(values, 1);
            }
            int rand = Utils.random(0, numNonNull);
            Object[] valueArray = (Object[])Array.newInstance(this.getReturnType(), 1);
            T[] TArray = values;
            int n = TArray.length;
            for (int i = 0; i < n; ++i) {
                T value = TArray[i];
                if (value == null) continue;
                if (rand == 0) {
                    valueArray[0] = value;
                    return valueArray;
                }
                --rand;
            }
            assert (false);
        }
        if (numNonNull == values.length) {
            return Arrays.copyOf(values, values.length);
        }
        Object[] valueArray = (Object[])Array.newInstance(this.getReturnType(), numNonNull);
        int i = 0;
        for (T value : values) {
            if (value == null) continue;
            valueArray[i++] = value;
        }
        return valueArray;
    }

    protected abstract T @Nullable [] get(Event var1);

    @Override
    public final boolean check(Event event, Predicate<? super T> checker) {
        return this.check(event, checker, false);
    }

    @Override
    public final boolean check(Event event, Predicate<? super T> checker, boolean negated) {
        return SimpleExpression.check(this.get(event), checker, negated, this.getAnd());
    }

    public static <T> boolean check(T @Nullable [] values, Predicate<? super T> checker, boolean invert, boolean and) {
        if (values == null) {
            return invert;
        }
        boolean hasElement = false;
        for (T value : values) {
            if (value == null) continue;
            hasElement = true;
            boolean b = checker.test(value);
            if (and && !b) {
                return invert;
            }
            if (and || !b) continue;
            return !invert;
        }
        if (!hasElement) {
            return invert;
        }
        return invert ^ and;
    }

    @Nullable
    protected <R> ConvertedExpression<T, ? extends R> getConvertedExpr(Class<R> ... to) {
        assert (!CollectionUtils.containsSuperclass(to, this.getReturnType()));
        return ConvertedExpression.newInstance(this, to);
    }

    @Override
    @Nullable
    public <R> Expression<? extends R> getConvertedExpression(Class<R> ... to) {
        if (CollectionUtils.containsSuperclass(to, this.getReturnType())) {
            return this;
        }
        return this.getConvertedExpr(to);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        Changer<?> changer;
        ClassInfo<Object> returnTypeInfo = this.returnTypeInfo;
        if (returnTypeInfo == null) {
            returnTypeInfo = Classes.getSuperClassInfo(this.getReturnType());
            this.returnTypeInfo = returnTypeInfo;
        }
        if ((changer = returnTypeInfo.getChanger()) == null) {
            return null;
        }
        return changer.acceptChange(mode);
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        ClassInfo<?> returnTypeInfo = this.returnTypeInfo;
        if (returnTypeInfo == null) {
            throw new UnsupportedOperationException();
        }
        Changer<?> changer = returnTypeInfo.getChanger();
        if (changer == null) {
            throw new UnsupportedOperationException();
        }
        changer.change(this.getArray(event), delta, mode);
    }

    @Override
    public boolean setTime(int time) {
        if (this.getParser().getHasDelayBefore() == Kleenean.TRUE && time != 0) {
            Skript.error("Can't use time states after the event has already passed.");
            return false;
        }
        this.time = time;
        return false;
    }

    protected final boolean setTime(int time, Class<? extends Event> applicableEvent) {
        if (this.getParser().getHasDelayBefore() == Kleenean.TRUE && time != 0) {
            Skript.error("Can't use time states after the event has already passed.");
            return false;
        }
        if (!this.getParser().isCurrentEvent(applicableEvent)) {
            return false;
        }
        this.time = time;
        return true;
    }

    @SafeVarargs
    protected final boolean setTime(int time, Class<? extends Event> ... applicableEvents) {
        if (this.getParser().getHasDelayBefore() == Kleenean.TRUE && time != 0) {
            Skript.error("Can't use time states after the event has already passed.");
            return false;
        }
        if (!this.getParser().isCurrentEvent(applicableEvents)) {
            return false;
        }
        this.time = time;
        return true;
    }

    protected final boolean setTime(int time, Class<? extends Event> applicableEvent, Expression<?> ... mustbeDefaultVars) {
        if (this.getParser().getHasDelayBefore() == Kleenean.TRUE && time != 0) {
            Skript.error("Can't use time states after the event has already passed.");
            return false;
        }
        if (!this.getParser().isCurrentEvent(applicableEvent)) {
            return false;
        }
        for (Expression<?> var : mustbeDefaultVars) {
            if (var.isDefault()) continue;
            return false;
        }
        this.time = time;
        return true;
    }

    @SafeVarargs
    protected final boolean setTime(int time, Expression<?> mustbeDefaultVar, Class<? extends Event> ... applicableEvents) {
        if (this.getParser().getHasDelayBefore() == Kleenean.TRUE && time != 0) {
            Skript.error("Can't use time states after the event has already passed.");
            return false;
        }
        if (mustbeDefaultVar == null) {
            Skript.exception((Throwable)new SkriptAPIException("Default expression was null. If the default expression can be null, don't be using 'SimpleExpression#setTime(int, Expression<?>, Class<? extends Event>...)' instead use the setTime without an expression if null."), new String[0]);
            return false;
        }
        if (!mustbeDefaultVar.isDefault()) {
            return false;
        }
        if (this.getParser().isCurrentEvent(applicableEvents)) {
            this.time = time;
            return true;
        }
        return false;
    }

    @Override
    public int getTime() {
        return this.time;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public boolean isLoopOf(String input) {
        return false;
    }

    @Override
    @Nullable
    public Iterator<? extends T> iterator(Event event) {
        return new ArrayIterator<T>(this.getArray(event));
    }

    @Override
    public String toString() {
        return this.toString(null, false);
    }

    @Override
    public Expression<?> getSource() {
        return this;
    }

    @Override
    public Node getNode() {
        return this.node;
    }

    @Override
    public Expression<? extends T> simplify() {
        return this;
    }

    @Override
    public boolean getAnd() {
        return true;
    }

    @Override
    public boolean supportsLoopPeeking() {
        return true;
    }
}

