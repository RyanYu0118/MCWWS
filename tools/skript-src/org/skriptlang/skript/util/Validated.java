/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Internal
 */
package org.skriptlang.skript.util;

import org.jetbrains.annotations.ApiStatus;
import org.skriptlang.skript.util.Validator;

public interface Validated {
    @ApiStatus.Internal
    public void invalidate() throws UnsupportedOperationException;

    public boolean valid();

    public static Validated validator() {
        return new Validator();
    }
}

