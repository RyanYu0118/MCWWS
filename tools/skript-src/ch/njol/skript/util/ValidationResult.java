/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import org.jetbrains.annotations.Nullable;

public record ValidationResult<T>(boolean valid, @Nullable String message, @Nullable T data) {
    public ValidationResult(boolean valid) {
        this(valid, null, null);
    }

    public ValidationResult(boolean valid, @Nullable String message) {
        this(valid, message, null);
    }

    public ValidationResult(boolean valid, @Nullable T data) {
        this(valid, null, data);
    }
}

