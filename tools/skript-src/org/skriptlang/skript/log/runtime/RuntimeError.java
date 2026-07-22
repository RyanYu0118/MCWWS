/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.log.runtime;

import java.util.logging.Level;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.ErrorSource;

public record RuntimeError(Level level, ErrorSource source, String error, @Nullable String toHighlight) {
}

