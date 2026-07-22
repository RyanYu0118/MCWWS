/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Debuggable;
import ch.njol.skript.lang.Loopable;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.simplification.Simplifiable;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.slot.Slot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Expression<T>
extends SyntaxElement,
Debuggable,
Loopable<T>,
Simplifiable<Expression<? extends T>> {
    @Nullable
    public T getSingle(Event var1);

    default public Optional<T> getOptionalSingle(Event event) {
        return Optional.ofNullable(this.getSingle(event));
    }

    public T[] getArray(Event var1);

    public T[] getAll(Event var1);

    default public Stream<? extends @NotNull T> stream(Event event) {
        Iterator iterator = this.iterator(event);
        if (iterator == null) {
            return Stream.empty();
        }
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }

    default public Stream<? extends @NotNull T> streamAll(Event event) {
        Iterator iterator = Arrays.stream(this.getAll(event)).iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }

    public boolean isSingle();

    default public boolean canBeSingle() {
        return this.isSingle();
    }

    public boolean check(Event var1, Predicate<? super T> var2, boolean var3);

    public boolean check(Event var1, Predicate<? super T> var2);

    @Nullable
    public <R> Expression<? extends R> getConvertedExpression(Class<R> ... var1);

    public Class<? extends T> getReturnType();

    default public Class<? extends T>[] possibleReturnTypes() {
        return new Class[]{this.getReturnType()};
    }

    default public boolean canReturn(Class<?> returnType) {
        for (Class<T> type : this.possibleReturnTypes()) {
            if (!returnType.isAssignableFrom(type) && type != Object.class) continue;
            return true;
        }
        return false;
    }

    default public boolean canReturnAnyOf(Class<?> ... returnTypes) {
        for (Class<?> type : returnTypes) {
            if (!this.canReturn(type)) continue;
            return true;
        }
        return false;
    }

    public boolean getAnd();

    public boolean setTime(int var1);

    public int getTime();

    default public boolean returnNestedStructures(boolean nested) {
        return false;
    }

    default public boolean returnsNestedStructures() {
        return false;
    }

    public boolean isDefault();

    public Expression<?> getSource();

    @Override
    default public Expression<? extends T> simplify() {
        return this;
    }

    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode var1);

    default public Map<Changer.ChangeMode, Class<?>[]> getAcceptedChangeModes() {
        HashMap<Changer.ChangeMode, Class<?>[]> map = new HashMap<Changer.ChangeMode, Class<?>[]>();
        for (Changer.ChangeMode mode : Changer.ChangeMode.values()) {
            Class<?>[] validClasses = this.acceptChange(mode);
            if (validClasses == null) continue;
            map.put(mode, validClasses);
        }
        return map;
    }

    public void change(Event var1, Object @Nullable [] var2, Changer.ChangeMode var3);

    @ApiStatus.Internal
    default public <R> void changeInPlace(Event event, Function<T, R> changeFunction) {
        this.changeInPlace(event, changeFunction, false);
    }

    @ApiStatus.Internal
    default public <R> void changeInPlace(Event event, Function<T, R> changeFunction, boolean getAll) {
        T[] values;
        T[] TArray = values = getAll ? this.getAll(event) : this.getArray(event);
        if (values.length == 0) {
            return;
        }
        Class[] validClasses = (Class[])Arrays.stream(this.acceptChange(Changer.ChangeMode.SET)).map(c -> c.isArray() ? c.getComponentType() : c).toArray(Class[]::new);
        ArrayList<R> newValues = new ArrayList<R>();
        for (T value : values) {
            R newValue = changeFunction.apply(value);
            if (newValue == null || !Changer.ChangerUtils.acceptsChangeTypes(validClasses, newValue.getClass())) continue;
            newValues.add(newValue);
        }
        this.change(event, newValues.toArray(), Changer.ChangeMode.SET);
    }

    default public Object @Nullable [] beforeChange(Expression<?> changed, Object @Nullable [] delta) {
        if (delta == null || delta.length == 0) {
            return null;
        }
        Object[] newDelta = null;
        if (changed instanceof Variable) {
            newDelta = new Object[delta.length];
            for (int i = 0; i < delta.length; ++i) {
                Object value = delta[i];
                if (value instanceof Slot) {
                    ItemStack item = ((Slot)value).getItem();
                    if (item != null) {
                        item = item.clone();
                    }
                    newDelta[i] = item;
                    continue;
                }
                newDelta[i] = Classes.clone(delta[i]);
            }
        }
        return newDelta == null ? delta : newDelta;
    }

    @Override
    @NotNull
    default public String getSyntaxTypeName() {
        return "expression";
    }
}

