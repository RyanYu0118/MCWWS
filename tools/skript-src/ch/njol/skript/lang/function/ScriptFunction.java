/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.function;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.KeyedValue;
import ch.njol.skript.lang.ReturnHandler;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.FunctionEvent;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.lang.function.Signature;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.util.Utils;
import ch.njol.skript.variables.HintManager;
import ch.njol.skript.variables.Variables;
import java.util.Arrays;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.FunctionArguments;
import org.skriptlang.skript.common.function.Parameter;
import org.skriptlang.skript.common.function.Parameters;

public class ScriptFunction<T>
extends Function<T>
implements ReturnHandler<T> {
    private final Trigger trigger;
    private final ThreadLocal<Boolean> returnValueSet = ThreadLocal.withInitial(() -> false);
    private final ThreadLocal<T @Nullable []> returnValues = new ThreadLocal();
    private final ThreadLocal<String @Nullable []> returnKeys = new ThreadLocal();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public ScriptFunction(Signature<T> sign, SectionNode node) {
        super(sign);
        Functions.currentFunction = this;
        HintManager hintManager = ParserInstance.get().getHintManager();
        try {
            hintManager.enterScope(false);
            for (Parameter<?> parameter : sign.parameters().all()) {
                Object hintName = parameter.name();
                if (!parameter.isSingle()) {
                    hintName = (String)hintName + "::*";
                    assert (parameter.type().isArray());
                    hintManager.set((String)hintName, new Class[]{parameter.type().componentType()});
                    continue;
                }
                assert (!parameter.type().isArray());
                hintManager.set((String)hintName, parameter.type());
            }
            this.trigger = this.loadReturnableTrigger(node, "function " + sign.getName(), new SimpleEvent());
        }
        finally {
            hintManager.exitScope();
            Functions.currentFunction = null;
        }
        this.trigger.setLineNumber(node.getLine());
    }

    @Override
    public T @Nullable [] execute(FunctionEvent<?> event, Object[][] params) {
        int i = 0;
        for (Parameter<?> parameter : this.getSignature().parameters().all()) {
            Object[] val = params[i];
            if (parameter.isSingle() && val.length > 0) {
                Variables.setVariable(parameter.name(), val[0], event, true);
                ++i;
                continue;
            }
            boolean keyed = Arrays.stream(val).allMatch(it -> it instanceof KeyedValue);
            if (keyed) {
                for (Object value : val) {
                    KeyedValue keyedValue = (KeyedValue)value;
                    Variables.setVariable(parameter.name() + "::" + keyedValue.key(), keyedValue.value(), event, true);
                }
            } else {
                int count = 0;
                for (Object value : val) {
                    Variables.setVariable(parameter.name() + "::" + count, value, event, true);
                    ++count;
                }
            }
            ++i;
        }
        this.trigger.execute(event);
        return this.type() != null ? this.returnValues.get() : null;
    }

    @Override
    public T execute(@NotNull FunctionEvent<?> event, @NotNull FunctionArguments arguments) {
        Parameters parameters = this.getSignature().parameters();
        FunctionEvent newEvent = new FunctionEvent(this);
        for (String name : arguments.names()) {
            Parameter<?> parameter = parameters.get(name);
            Object value = arguments.get(name);
            if (value == null) continue;
            if (parameter.isSingle()) {
                Variables.setVariable(name, value, newEvent, true);
                continue;
            }
            if (value instanceof KeyedValue[]) {
                KeyedValue[] keyedValues;
                for (KeyedValue keyedValue : keyedValues = (KeyedValue[])value) {
                    Variables.setVariable(name + "::" + keyedValue.key(), keyedValue.value(), newEvent, true);
                }
                continue;
            }
            int i = 0;
            for (Object o : (Object[])value) {
                Variables.setVariable(name + "::" + i, o, newEvent, true);
                ++i;
            }
        }
        this.trigger.execute(newEvent);
        T[] vs = this.returnValues.get();
        if (this.type() == null || vs == null || vs.length == 0) {
            return null;
        }
        if (vs.length == 1) {
            return vs[0];
        }
        return (T)vs;
    }

    @Override
    public @NotNull String @Nullable [] returnedKeys() {
        return this.type() != null ? this.returnKeys.get() : null;
    }

    @Override
    public boolean resetReturnValue() {
        this.returnValueSet.remove();
        this.returnValues.remove();
        this.returnKeys.remove();
        return true;
    }

    @Override
    public final void returnValues(Event event, Expression<? extends T> value) {
        assert (!this.returnValueSet.get().booleanValue());
        this.returnValueSet.set(true);
        this.returnValues.set(value.getArray(event));
        if (KeyProviderExpression.canReturnKeys(value)) {
            this.returnKeys.set(((KeyProviderExpression)value).getArrayKeys(event));
        }
    }

    @Override
    public final boolean isSingleReturnValue() {
        return this.isSingle();
    }

    @Override
    @Nullable
    public final Class<? extends T> returnValueType() {
        return Utils.getComponentType(this.type());
    }
}

