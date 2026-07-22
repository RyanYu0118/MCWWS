/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Experimental
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.ApiStatus$NonExtendable
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnmodifiableView
 */
package org.skriptlang.skript.common.function;

import ch.njol.skript.util.Contract;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.skriptlang.skript.common.function.FunctionReference;
import org.skriptlang.skript.common.function.Parameters;

@ApiStatus.NonExtendable
@ApiStatus.Internal
@ApiStatus.Experimental
public interface Signature<T> {
    @Nullable
    public Class<T> returnType();

    public @UnmodifiableView @NotNull Parameters parameters();

    @ApiStatus.Experimental
    public Contract contract();

    @ApiStatus.Experimental
    public void addCall(FunctionReference<?> var1);

    default public boolean isSingle() {
        if (this.returnType() == null) {
            return false;
        }
        return !this.returnType().isArray();
    }
}

