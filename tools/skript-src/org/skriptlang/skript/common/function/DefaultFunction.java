/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.common.function;

import ch.njol.skript.doc.Documentable;
import ch.njol.skript.util.Contract;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.common.function.DefaultFunctionImpl;
import org.skriptlang.skript.common.function.Function;
import org.skriptlang.skript.common.function.FunctionArguments;
import org.skriptlang.skript.common.function.Parameter;

public sealed interface DefaultFunction<T>
extends Function<T>,
Documentable
permits DefaultFunctionImpl {
    @org.jetbrains.annotations.Contract(value="_, _, _ -> new")
    @NotNull
    public static <T> Builder<T> builder(@NotNull SkriptAddon source, @NotNull String name, @NotNull Class<T> returnType) {
        return new DefaultFunctionImpl.BuilderImpl<T>(source, name, returnType);
    }

    @NotNull
    public SkriptAddon source();

    public static interface Builder<T> {
        @org.jetbrains.annotations.Contract(value="_ -> this")
        public Builder<T> contract(@NotNull Contract var1);

        @org.jetbrains.annotations.Contract(value="_ -> this")
        public Builder<T> description(String ... var1);

        @org.jetbrains.annotations.Contract(value="_ -> this")
        public Builder<T> since(String ... var1);

        @org.jetbrains.annotations.Contract(value="_ -> this")
        public Builder<T> examples(String ... var1);

        @org.jetbrains.annotations.Contract(value="_ -> this")
        public Builder<T> keywords(String ... var1);

        @org.jetbrains.annotations.Contract(value="_ -> this")
        public Builder<T> requires(String ... var1);

        @org.jetbrains.annotations.Contract(value="_, _, _ -> this")
        public Builder<T> parameter(@NotNull String var1, @NotNull Class<?> var2, Parameter.Modifier ... var3);

        public DefaultFunction<T> build(@NotNull java.util.function.Function<FunctionArguments, T> var1);
    }
}

