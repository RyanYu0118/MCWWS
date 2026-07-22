/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.common.function;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Debuggable;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.KeyedValue;
import ch.njol.skript.lang.function.FunctionEvent;
import ch.njol.skript.lang.function.FunctionRegistry;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Utils;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.Function;
import org.skriptlang.skript.common.function.FunctionArgumentsImpl;
import org.skriptlang.skript.common.function.FunctionReferenceParser;
import org.skriptlang.skript.common.function.Parameter;
import org.skriptlang.skript.common.function.Signature;

public final class FunctionReference<T>
implements Debuggable {
    private final String namespace;
    private final String name;
    private final Signature<T> signature;
    private final Argument<Expression<?>>[] arguments;
    private boolean unloaded = false;
    private Function<T> cachedFunction;
    private LinkedHashMap<String, ArgInfo> cachedArguments;

    public FunctionReference(@Nullable String namespace, @NotNull String name, @NotNull Signature<T> signature, @NotNull Argument<Expression<?>>[] arguments) {
        Preconditions.checkNotNull((Object)name, (Object)"name cannot be null");
        Preconditions.checkNotNull(signature, (Object)"signature cannot be null");
        Preconditions.checkNotNull(arguments, (Object)"arguments cannot be null");
        this.namespace = namespace;
        this.name = name;
        this.signature = signature;
        this.arguments = arguments;
    }

    public void invalidateCache() {
        this.unloaded = true;
    }

    public boolean validate() {
        if (this.signature == null) {
            return false;
        }
        if (this.cachedArguments == null) {
            this.cachedArguments = new LinkedHashMap();
            Parameter<?>[] targets = this.signature.parameters().all();
            for (int i = 0; i < this.arguments.length; ++i) {
                Argument<Expression<?>> argument = this.arguments[i];
                Parameter<?> target = targets[i];
                if (argument.value instanceof FunctionReferenceParser.EmptyExpression) continue;
                Expression converted = ((Expression)argument.value).getConvertedExpression(Utils.getComponentType(target.type()));
                if (!this.validateArgument(target, (Expression)argument.value, converted)) {
                    return false;
                }
                if (converted != null && KeyProviderExpression.areKeysRecommended(converted)) {
                    converted.returnNestedStructures(true);
                }
                this.cachedArguments.put(target.name(), new ArgInfo(converted, target.type(), target.modifiers()));
            }
        }
        this.signature.addCall(this);
        return true;
    }

    private boolean validateArgument(Parameter<?> target, Expression<?> original, Expression<?> converted) {
        if (converted == null) {
            if (LiteralUtils.hasUnparsedLiteral(original)) {
                Skript.error("Can't understand this expression: %s", original);
            } else {
                Skript.error("Expected type %s for argument '%s', but %s is of type %s.", this.getName(target.type(), target.isSingle()), target.name(), original, this.getName(original.getReturnType(), original.isSingle()));
            }
            return false;
        }
        if (target.isSingle() && !converted.isSingle()) {
            Skript.error("Expected type %s for argument '%s', but %s is of type %s.", this.getName(target.type(), target.isSingle()), target.name(), converted, this.getName(converted.getReturnType(), converted.isSingle()));
            return false;
        }
        return true;
    }

    private String getName(Class<?> clazz, boolean single) {
        if (single) {
            return Classes.getSuperClassInfo(clazz).getName().getSingular();
        }
        return Classes.getSuperClassInfo(Utils.getComponentType(clazz)).getName().getPlural();
    }

    public T execute(Event event) {
        if (!this.validate()) {
            Skript.error("Failed to verify function %s before execution.", this.name);
            return null;
        }
        LinkedHashMap<String, Object> args = new LinkedHashMap<String, Object>();
        this.cachedArguments.forEach((k, v) -> {
            if (v.modifiers().contains(Parameter.Modifier.KEYED)) {
                args.put((String)k, Classes.clone(this.evaluateKeyed(v.expression(), event)));
                return;
            }
            if (!v.type().isArray()) {
                args.put((String)k, Classes.clone(v.expression().getSingle(event)));
            } else {
                args.put((String)k, Classes.clone(v.expression().getArray(event)));
            }
        });
        Function<T> function = this.function();
        FunctionEvent<T> fnEvent = new FunctionEvent<T>(function);
        if (Functions.callFunctionEvents) {
            Bukkit.getPluginManager().callEvent(fnEvent);
        }
        return function.execute(fnEvent, new FunctionArgumentsImpl(args));
    }

    private KeyedValue<?>[] evaluateKeyed(Expression<?> expression, Event event) {
        if (expression instanceof ExpressionList) {
            ExpressionList list = (ExpressionList)expression;
            return this.evaluateSingleListParameter(list.getExpressions(), event);
        }
        return this.evaluateParameter(expression, event);
    }

    private KeyedValue<?>[] evaluateSingleListParameter(Expression<?>[] arguments, Event event) {
        ArrayList<Object> values = new ArrayList<Object>();
        LinkedHashSet<String> keys = new LinkedHashSet<String>();
        int keyIndex = 1;
        for (Expression<?> argument : arguments) {
            ?[] valuesArray = argument.getArray(event);
            String[] keysArray = KeyProviderExpression.areKeysRecommended(argument) ? ((KeyProviderExpression)argument).getArrayKeys(event) : null;
            for (int i = 0; i < valuesArray.length; ++i) {
                if (keysArray == null) {
                    while (keys.contains(String.valueOf(keyIndex))) {
                        ++keyIndex;
                    }
                    keys.add(String.valueOf(keyIndex++));
                } else if (!keys.add(keysArray[i])) continue;
                values.add(Classes.clone(valuesArray[i]));
            }
        }
        return KeyedValue.zip(values.toArray(), keys.toArray(new String[0]));
    }

    private KeyedValue<?>[] evaluateParameter(Expression<?> argument, Event event) {
        if (argument == null) {
            return null;
        }
        ?[] values = argument.getArray(event);
        for (int i = 0; i < values.length; ++i) {
            values[i] = Classes.clone(values[i]);
        }
        if (!(argument instanceof KeyProviderExpression)) {
            return KeyedValue.zip(values, null);
        }
        KeyProviderExpression kpe = (KeyProviderExpression)argument;
        String[] keys = KeyProviderExpression.areKeysRecommended(argument) ? kpe.getArrayKeys(event) : null;
        return KeyedValue.zip(values, keys);
    }

    public Function<T> function() {
        if (this.unloaded || this.cachedFunction == null) {
            Class[] parameters = (Class[])Arrays.stream(this.signature.parameters().all()).map(Parameter::type).toArray(Class[]::new);
            FunctionRegistry.Retrieval<ch.njol.skript.lang.function.Function<?>> retrieval = FunctionRegistry.getRegistry().getFunction(this.namespace, this.name, parameters);
            if (retrieval.result() == FunctionRegistry.RetrievalResult.EXACT) {
                this.cachedFunction = retrieval.retrieved();
                this.unloaded = false;
            }
        }
        return this.cachedFunction;
    }

    public Signature<T> signature() {
        return this.signature;
    }

    public String namespace() {
        return this.namespace;
    }

    @NotNull
    public String name() {
        return this.name;
    }

    @NotNull
    public Argument<Expression<?>>[] arguments() {
        return this.arguments;
    }

    public boolean isSingle() {
        if (this.signature.contract() != null) {
            Expression[] args = (Expression[])Arrays.stream(this.arguments).map(it -> (Expression)it.value).toArray(Expression[]::new);
            return this.signature.contract().isSingle(args);
        }
        return this.signature.isSingle();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        StringBuilder builder = new StringBuilder();
        builder.append(this.name);
        builder.append("(");
        StringJoiner args = new StringJoiner(", ");
        for (Argument<Expression<?>> argument : this.arguments) {
            args.add("%s: %s".formatted(argument.name, ((Expression)argument.value).toString(event, debug)));
        }
        builder.append(args);
        builder.append(")");
        return builder.toString();
    }

    public record Argument<T>(ArgumentType type, String name, T value, @Nullable String raw) {
        public Argument(ArgumentType type, String name, T value) {
            this(type, name, value, null);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Argument)) {
                return false;
            }
            Argument argument = (Argument)o;
            return Objects.equals(this.value, argument.value) && Objects.equals(this.name, argument.name) && this.type == argument.type;
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode((Object)this.type);
            result = 31 * result + Objects.hashCode(this.name);
            result = 31 * result + Objects.hashCode(this.value);
            return result;
        }
    }

    private record ArgInfo(Expression<?> expression, Class<?> type, Set<Parameter.Modifier> modifiers) {
    }

    public static enum ArgumentType {
        NAMED,
        UNNAMED;

    }
}

