/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.common.function;

import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.FunctionEvent;
import ch.njol.skript.util.Contract;
import com.google.common.base.Preconditions;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.common.function.DefaultFunction;
import org.skriptlang.skript.common.function.FunctionArguments;
import org.skriptlang.skript.common.function.FunctionArgumentsImpl;
import org.skriptlang.skript.common.function.Parameter;
import org.skriptlang.skript.common.function.Signature;

final class DefaultFunctionImpl<T>
extends Function<T>
implements DefaultFunction<T> {
    private final SkriptAddon source;
    private final SequencedMap<String, Parameter<?>> parameters;
    private final java.util.function.Function<FunctionArguments, T> execute;
    private final List<String> description;
    private final List<String> since;
    private final List<String> examples;
    private final List<String> keywords;
    private final List<String> requires;

    DefaultFunctionImpl(SkriptAddon source, String name, SequencedMap<String, Parameter<?>> parameters, Class<T> returnType, boolean single, @Nullable Contract contract, java.util.function.Function<FunctionArguments, T> execute, String[] description, String[] since, String[] examples, String[] keywords, String[] requires) {
        super(new ch.njol.skript.lang.function.Signature<T>(null, name, parameters.values().toArray(new Parameter[0]), returnType, single, contract));
        Preconditions.checkNotNull((Object)source, (Object)"source cannot be null");
        Preconditions.checkNotNull((Object)name, (Object)"name cannot be null");
        Preconditions.checkNotNull(parameters, (Object)"parameters cannot be null");
        Preconditions.checkNotNull(returnType, (Object)"return type cannot be null");
        Preconditions.checkNotNull(execute, (Object)"execute cannot be null");
        this.source = source;
        this.parameters = parameters;
        this.execute = execute;
        this.description = description != null ? List.of(description) : Collections.emptyList();
        this.since = since != null ? List.of(since) : Collections.emptyList();
        this.examples = examples != null ? List.of(examples) : Collections.emptyList();
        this.keywords = keywords != null ? List.of(keywords) : Collections.emptyList();
        this.requires = requires != null ? List.of(requires) : Collections.emptyList();
    }

    @Override
    public T @Nullable [] execute(FunctionEvent<?> event, Object[][] params) {
        LinkedHashMap<String, Object> args = new LinkedHashMap<String, Object>();
        int length = Math.min(this.parameters.size(), params.length);
        Parameter[] arrayParams = this.parameters.values().toArray(new Parameter[0]);
        for (int i = 0; i < length; ++i) {
            Parameter.Modifier.RangedModifier range;
            Object[] arg = params[i];
            Parameter parameter = arrayParams[i];
            if (arg == null || arg.length == 0) {
                if (parameter.hasModifier(Parameter.Modifier.OPTIONAL)) continue;
                return null;
            }
            if (parameter.hasModifier(Parameter.Modifier.RANGED) && !(range = parameter.getModifier(Parameter.Modifier.RangedModifier.class)).inRange(arg)) {
                return null;
            }
            if (parameter.isSingle()) {
                if (arg.length != 1) {
                    return null;
                }
                assert (parameter.type().isAssignableFrom(arg[0].getClass())) : "argument type %s does not match parameter type %s".formatted(parameter.type().getSimpleName(), arg[0].getClass().getSimpleName());
                args.put(parameter.name(), arg[0]);
                continue;
            }
            assert (parameter.type().isAssignableFrom(arg.getClass())) : "argument type %s does not match parameter type %s".formatted(parameter.type().getSimpleName(), arg.getClass().getSimpleName());
            args.put(parameter.name(), arg);
        }
        FunctionArgumentsImpl arguments = new FunctionArgumentsImpl(args);
        T result = this.execute.apply(arguments);
        if (result == null) {
            return null;
        }
        if (result.getClass().isArray()) {
            return (Object[])result;
        }
        Object[] array = (Object[])Array.newInstance(result.getClass(), 1);
        array[0] = result;
        return array;
    }

    @Override
    public T execute(@NotNull FunctionEvent<?> event, @NotNull FunctionArguments arguments) {
        for (String name : arguments.names()) {
            Parameter.Modifier.RangedModifier range;
            Parameter parameter = (Parameter)this.parameters.get(name);
            Object value = arguments.get(name);
            if (value == null && !parameter.hasModifier(Parameter.Modifier.OPTIONAL)) {
                return null;
            }
            if (!parameter.hasModifier(Parameter.Modifier.RANGED) || (range = parameter.getModifier(Parameter.Modifier.RangedModifier.class)).inRange(value)) continue;
            return null;
        }
        return this.execute.apply(arguments);
    }

    @Override
    public boolean resetReturnValue() {
        return true;
    }

    @Override
    @NotNull
    public String name() {
        return this.getName();
    }

    @Override
    public @Unmodifiable @NotNull List<String> description() {
        return this.description;
    }

    @Override
    public @Unmodifiable @NotNull List<String> since() {
        return this.since;
    }

    @Override
    public @Unmodifiable @NotNull List<String> examples() {
        return this.examples;
    }

    @Override
    public @Unmodifiable @NotNull List<String> keywords() {
        return this.keywords;
    }

    @Override
    public @Unmodifiable @NotNull List<String> requires() {
        return this.requires;
    }

    @Override
    @NotNull
    public SkriptAddon source() {
        return this.source;
    }

    @Override
    public @NotNull Signature<T> signature() {
        return this.getSignature();
    }

    record DefaultParameter<T>(String name, Class<T> type, Set<Parameter.Modifier> modifiers) implements Parameter<T>
    {
        DefaultParameter(String name, Class<T> type, Parameter.Modifier ... modifiers) {
            this(name, type, Set.of(modifiers));
        }

        @Override
        @NotNull
        public String toString() {
            return this.toFormattedString();
        }
    }

    static class BuilderImpl<T>
    implements DefaultFunction.Builder<T> {
        private final SkriptAddon source;
        private final String name;
        private final Class<T> returnType;
        private final SequencedMap<String, Parameter<?>> parameters = new LinkedHashMap();
        private Contract contract = null;
        private String[] description;
        private String[] since;
        private String[] examples;
        private String[] keywords;
        private String[] requires;

        BuilderImpl(@NotNull SkriptAddon source, @NotNull String name, @NotNull Class<T> returnType) {
            Preconditions.checkNotNull((Object)source, (Object)"source cannot be null");
            Preconditions.checkNotNull((Object)name, (Object)"name cannot be null");
            Preconditions.checkNotNull(returnType, (Object)"return type cannot be null");
            this.source = source;
            this.name = name;
            this.returnType = returnType;
        }

        @Override
        public DefaultFunction.Builder<T> contract(@NotNull Contract contract) {
            Preconditions.checkNotNull((Object)contract, (Object)"contract cannot be null");
            this.contract = contract;
            return this;
        }

        @Override
        public DefaultFunction.Builder<T> description(String ... description) {
            Preconditions.checkNotNull((Object)description, (Object)"description cannot be null");
            BuilderImpl.checkNotNull(description, "description contents cannot be null");
            this.description = description;
            return this;
        }

        @Override
        public DefaultFunction.Builder<T> since(String ... since) {
            Preconditions.checkNotNull((Object)since, (Object)"since cannot be null");
            BuilderImpl.checkNotNull(since, "since contents cannot be null");
            this.since = since;
            return this;
        }

        @Override
        public DefaultFunction.Builder<T> examples(String ... examples) {
            Preconditions.checkNotNull((Object)examples, (Object)"examples cannot be null");
            BuilderImpl.checkNotNull(examples, "examples contents cannot be null");
            this.examples = examples;
            return this;
        }

        @Override
        public DefaultFunction.Builder<T> keywords(String ... keywords) {
            Preconditions.checkNotNull((Object)keywords, (Object)"keywords cannot be null");
            BuilderImpl.checkNotNull(keywords, "keywords contents cannot be null");
            this.keywords = keywords;
            return this;
        }

        @Override
        public DefaultFunction.Builder<T> requires(String ... requires) {
            Preconditions.checkNotNull((Object)this.keywords, (Object)"requires cannot be null");
            BuilderImpl.checkNotNull(this.keywords, "requires contents cannot be null");
            this.requires = requires;
            return this;
        }

        @Override
        public DefaultFunction.Builder<T> parameter(@NotNull String name, @NotNull Class<?> type, Parameter.Modifier ... modifiers) {
            Preconditions.checkNotNull((Object)name, (Object)"name cannot be null");
            Preconditions.checkNotNull(type, (Object)"type cannot be null");
            this.parameters.put(name, new DefaultParameter(name, type, modifiers));
            return this;
        }

        @Override
        public DefaultFunction<T> build(@NotNull java.util.function.Function<FunctionArguments, T> execute) {
            Preconditions.checkNotNull(execute, (Object)"execute cannot be null");
            return new DefaultFunctionImpl<T>(this.source, this.name, this.parameters, this.returnType, !this.returnType.isArray(), this.contract, execute, this.description, this.since, this.examples, this.keywords, this.requires);
        }

        private static void checkNotNull(@NotNull String[] strings, @NotNull String message) {
            for (String string : strings) {
                Preconditions.checkNotNull((Object)string, (Object)message);
            }
        }
    }
}

