/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.common.function;

import java.util.Set;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.common.function.FunctionArgumentsImpl;

public sealed interface FunctionArguments
permits FunctionArgumentsImpl {
    public <T> T get(@NotNull String var1);

    public <T> T getOrDefault(@NotNull String var1, T var2);

    public <T> T getOrDefault(@NotNull String var1, Supplier<T> var2);

    public @Unmodifiable @NotNull Set<String> names();
}

