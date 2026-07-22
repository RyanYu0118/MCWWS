/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.lang.util.common;

import ch.njol.skript.lang.util.common.AnyProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnknownNullability;

@FunctionalInterface
@Deprecated(since="2.13", forRemoval=true)
public interface AnyContains<Type>
extends AnyProvider {
    public boolean contains(@UnknownNullability Type var1);

    default public boolean isSafeToCheck(Object value) {
        return true;
    }

    @ApiStatus.Internal
    default public boolean checkSafely(Object value) {
        return this.isSafeToCheck(value) && this.contains(value);
    }
}

