/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.patterns;

import org.jetbrains.annotations.Nullable;

public class MalformedPatternException
extends IllegalArgumentException {
    public MalformedPatternException(String pattern, String message) {
        this(pattern, message, null);
    }

    public MalformedPatternException(String pattern, String message, @Nullable Throwable cause) {
        super(message + " [pattern: " + pattern + "]", cause);
    }
}

