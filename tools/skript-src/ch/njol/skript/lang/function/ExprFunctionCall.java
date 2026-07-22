/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.ArrayUtils
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.function;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.FunctionReference;
import org.skriptlang.skript.lang.converter.Converters;

public class ExprFunctionCall<T>
extends SimpleExpression<T>
implements KeyProviderExpression<T> {
    private final FunctionReference<?> reference;
    private final Class<? extends T>[] returnTypes;
    private final Class<T> returnType;
    private final Map<Event, String[]> cache = Collections.synchronizedMap(new WeakHashMap());

    public ExprFunctionCall(FunctionReference<T> function) {
        this(function, CollectionUtils.array(function.signature().returnType()));
    }

    public ExprFunctionCall(FunctionReference<?> reference, Class<? extends T>[] expectedReturnTypes) {
        this.reference = reference;
        Class<?> functionReturnType = reference.signature().returnType();
        Class<?> returnType = Utils.getComponentType(functionReturnType);
        if (CollectionUtils.containsSuperclass(expectedReturnTypes, returnType)) {
            this.returnTypes = new Class[]{returnType};
            this.returnType = returnType;
        } else {
            this.returnTypes = expectedReturnTypes;
            this.returnType = Utils.getSuperType(expectedReturnTypes);
        }
    }

    @Override
    protected T @Nullable [] get(Event event) {
        Object execute = this.reference.execute(event);
        Object[] values = execute == null ? null : (!execute.getClass().isArray() ? new Object[]{execute} : (Object[])execute);
        Object[] keys = this.reference.function().returnedKeys();
        this.reference.function().resetReturnValue();
        Object[] convertedValues = (Object[])Array.newInstance(this.returnType, values != null ? values.length : 0);
        if (values == null || values.length == 0) {
            this.cache.put(event, new String[0]);
            return convertedValues;
        }
        Converters.convert(values, convertedValues, this.returnTypes);
        if (keys != null) {
            for (int i = 0; i < convertedValues.length; ++i) {
                if (convertedValues[i] != null) continue;
                keys[i] = null;
            }
            convertedValues = ArrayUtils.removeAllOccurrences((Object[])convertedValues, null);
            this.cache.put(event, (String[])ArrayUtils.removeAllOccurrences((Object[])keys, null));
        } else {
            convertedValues = ArrayUtils.removeAllOccurrences((Object[])convertedValues, null);
            this.cache.put(event, ExprFunctionCall.generateNumericalKeys(convertedValues.length));
        }
        return convertedValues;
    }

    @Override
    @NotNull
    public @NotNull String @NotNull [] getArrayKeys(Event event) throws IllegalStateException {
        if (!this.cache.containsKey(event)) {
            throw new IllegalStateException();
        }
        return this.cache.remove(event);
    }

    @Override
    public boolean areKeysRecommended() {
        return false;
    }

    @Override
    @Nullable
    public <R> Expression<? extends R> getConvertedExpression(Class<R> ... to) {
        if (CollectionUtils.containsSuperclass(to, this.getReturnType())) {
            return this;
        }
        Class<?> returns = this.reference.signature().returnType();
        Class<?> converterType = Utils.getComponentType(returns);
        if (Converters.converterExists(converterType, to)) {
            return new ExprFunctionCall<R>(this.reference, to);
        }
        return null;
    }

    @Override
    public boolean isSingle() {
        return this.reference.isSingle();
    }

    @Override
    public Class<? extends T> getReturnType() {
        return this.returnType;
    }

    @Override
    public Class<? extends T>[] possibleReturnTypes() {
        return Arrays.copyOf(this.returnTypes, this.returnTypes.length);
    }

    @Override
    public boolean isLoopOf(String input) {
        return KeyProviderExpression.super.isLoopOf(input);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.reference.toString(event, debug);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        assert (false);
        return false;
    }

    private static String[] generateNumericalKeys(int length) {
        String[] keys = new String[length];
        for (int i = 0; i < length; ++i) {
            keys[i] = String.valueOf(i);
        }
        return keys;
    }
}

