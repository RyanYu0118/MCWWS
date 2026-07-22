/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.common.function;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.common.function.FunctionArguments;

record FunctionArgumentsImpl(@Unmodifiable @NotNull Map<String, Object> arguments) implements FunctionArguments
{
    FunctionArgumentsImpl(@Unmodifiable @NotNull Map<String, Object> arguments) {
        Preconditions.checkNotNull(arguments, (Object)"arguments cannot be null");
    }

    @Override
    public <T> T get(@NotNull String name) {
        Preconditions.checkNotNull((Object)name, (Object)"name cannot be null");
        return (T)this.arguments.get(name);
    }

    @Override
    public <T> T getOrDefault(@NotNull String name, T defaultValue) {
        Preconditions.checkNotNull((Object)name, (Object)"name cannot be null");
        return (T)this.arguments.getOrDefault(name, defaultValue);
    }

    @Override
    public <T> T getOrDefault(@NotNull String name, Supplier<T> defaultValue) {
        Preconditions.checkNotNull((Object)name, (Object)"name cannot be null");
        Object existing = this.arguments.get(name);
        if (existing == null) {
            return defaultValue.get();
        }
        return (T)existing;
    }

    @Override
    public @Unmodifiable @NotNull Set<String> names() {
        return Collections.unmodifiableSet(this.arguments.keySet());
    }
}

