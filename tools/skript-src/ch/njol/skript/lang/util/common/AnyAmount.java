/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package ch.njol.skript.lang.util.common;

import ch.njol.skript.lang.util.common.AnyProvider;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
@Deprecated(since="2.13", forRemoval=true)
public interface AnyAmount
extends AnyProvider {
    @NotNull
    public Number amount();

    default public boolean supportsAmountChange() {
        return false;
    }

    default public void setAmount(Number amount) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    default public boolean isEmpty() {
        return this.amount().intValue() == 0;
    }
}

