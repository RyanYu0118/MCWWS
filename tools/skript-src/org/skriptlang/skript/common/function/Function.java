/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Experimental
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.ApiStatus$NonExtendable
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.common.function;

import ch.njol.skript.lang.function.FunctionEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.FunctionArguments;
import org.skriptlang.skript.common.function.Signature;

@ApiStatus.NonExtendable
@ApiStatus.Internal
@ApiStatus.Experimental
public interface Function<T> {
    public T execute(@NotNull FunctionEvent<?> var1, @NotNull FunctionArguments var2);

    @NotNull
    public Signature<T> signature();

    @ApiStatus.Experimental
    public boolean resetReturnValue();

    @ApiStatus.Experimental
    public @NotNull String @Nullable [] returnedKeys();
}

