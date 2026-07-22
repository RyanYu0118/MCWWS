/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.Skript;
import ch.njol.skript.localization.Language;
import java.io.IOException;
import org.jetbrains.annotations.Nullable;

public abstract class ExceptionUtils {
    private static final String IO_NODE = "io exceptions";

    private ExceptionUtils() {
    }

    @Nullable
    public static String toString(IOException e) {
        if (Language.keyExists("io exceptions." + e.getClass().getSimpleName())) {
            return Language.format("io exceptions." + e.getClass().getSimpleName(), e.getLocalizedMessage());
        }
        if (Skript.testing()) {
            e.printStackTrace();
        }
        return e.getLocalizedMessage();
    }
}

