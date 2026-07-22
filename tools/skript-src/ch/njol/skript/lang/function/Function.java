/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.function;

import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.KeyedValue;
import ch.njol.skript.lang.function.FunctionEvent;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.lang.function.Parameter;
import ch.njol.skript.lang.function.Signature;
import ch.njol.util.coll.CollectionUtils;
import java.util.Arrays;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.DefaultFunction;
import org.skriptlang.skript.common.function.Parameter;
import org.skriptlang.skript.common.function.Parameters;
import org.skriptlang.skript.common.function.ScriptParameter;

public abstract class Function<T>
implements org.skriptlang.skript.common.function.Function<T> {
    public static boolean executeWithNulls = SkriptConfig.executeFunctionsWithMissingParams.value();
    private final Signature<T> sign;

    public Function(Signature<T> sign) {
        this.sign = sign;
    }

    public Signature<T> getSignature() {
        return this.sign;
    }

    @Override
    public @NotNull org.skriptlang.skript.common.function.Signature<T> signature() {
        return this.sign;
    }

    public String getName() {
        return this.sign.getName();
    }

    @Deprecated(forRemoval=true, since="2.14")
    public Parameter<?>[] getParameters() {
        return (Parameter[])Arrays.stream(this.sign.getParameters()).map(Signature::toOldParameter).toArray(Parameter[]::new);
    }

    @Deprecated(forRemoval=true, since="2.14")
    public Parameter<?> getParameter(int index) {
        return Signature.toOldParameter(this.sign.getParameter(index));
    }

    public boolean isSingle() {
        return this.sign.isSingle();
    }

    @Nullable
    public ClassInfo<T> getReturnType() {
        return this.sign.getReturnType();
    }

    public Class<T> type() {
        return this.sign.returnType();
    }

    @Deprecated(forRemoval=true, since="2.14")
    public final T @Nullable [] execute(Object[][] params) {
        Parameters parameters;
        FunctionEvent event = new FunctionEvent(this);
        if (Functions.callFunctionEvents) {
            Bukkit.getPluginManager().callEvent(event);
        }
        if (params.length > (parameters = this.sign.parameters()).size()) {
            assert (false) : params.length;
            return null;
        }
        Object[][] parameterValues = params.length < parameters.size() ? (Object[][])Arrays.copyOf(params, parameters.size()) : params;
        int i = 0;
        for (org.skriptlang.skript.common.function.Parameter<?> parameter : parameters.all()) {
            Expression<Object> defaultValueExpr;
            Object[] parameterValue;
            Object[] objectArray = parameterValue = parameter.hasModifier(Parameter.Modifier.KEYED) ? this.convertToKeyed(parameterValues[i]) : parameterValues[i];
            if (parameter instanceof Parameter) {
                Parameter script = (Parameter)parameter;
                defaultValueExpr = script.def;
            } else if (parameter instanceof ScriptParameter) {
                ScriptParameter script = (ScriptParameter)parameter;
                defaultValueExpr = script.defaultValue();
            } else {
                defaultValueExpr = null;
            }
            if ((parameterValues[i] == null || parameterValues[i].length == 0) && parameter.hasModifier(Parameter.Modifier.KEYED) && defaultValueExpr != null) {
                Object[] defaultValue = defaultValueExpr.getArray(event);
                parameterValue = defaultValue.length == 1 ? KeyedValue.zip(defaultValue, null) : defaultValue;
            } else if (parameterValue == null && !(this instanceof DefaultFunction)) {
                assert (defaultValueExpr != null);
                parameterValue = parameter.evaluate(defaultValueExpr, event);
            }
            if (!(this instanceof DefaultFunction) && !executeWithNulls && parameterValue != null && parameterValue.length == 0) {
                return null;
            }
            parameterValues[i] = parameterValue;
            ++i;
        }
        Object[] r = this.execute(event, parameterValues);
        assert (this.sign.getReturnType() != null ? r == null || (r.length <= 1 || !this.sign.isSingle()) && !CollectionUtils.contains(r, null) && this.sign.getReturnType().getC().isAssignableFrom(r.getClass().getComponentType()) : r == null) : String.valueOf(this) + "; " + Arrays.toString(r);
        return r == null || r.length > 0 ? r : null;
    }

    private KeyedValue<Object> @Nullable [] convertToKeyed(Object[] values) {
        if (values == null) {
            return null;
        }
        if (values.length == 0) {
            return new KeyedValue[0];
        }
        if (values instanceof KeyedValue[]) {
            return (KeyedValue[])values;
        }
        return KeyedValue.zip(values, null);
    }

    @Deprecated(since="2.14", forRemoval=true)
    public abstract T @Nullable [] execute(FunctionEvent<?> var1, Object[][] var2);

    @Override
    public @NotNull String @Nullable [] returnedKeys() {
        return null;
    }

    @Override
    public abstract boolean resetReturnValue();

    public String toString() {
        return (this.sign.isLocal() ? "local " : "") + "function " + this.sign.getName();
    }
}

