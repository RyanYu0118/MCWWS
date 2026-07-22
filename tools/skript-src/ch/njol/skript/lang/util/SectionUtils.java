/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.util;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.variables.HintManager;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import org.jetbrains.annotations.Nullable;

public final class SectionUtils {
    private SectionUtils() {
    }

    @Nullable
    public static Trigger loadLinkedCode(String name, BiFunction<Runnable, Runnable, Trigger> triggerSupplier) {
        AtomicBoolean delayed = new AtomicBoolean(false);
        AtomicReference hintBackup = new AtomicReference();
        Runnable beforeLoading = () -> ParserInstance.get().getHintManager().enterScope(false);
        Runnable afterLoading = () -> {
            ParserInstance parser = ParserInstance.get();
            delayed.set(!parser.getHasDelayBefore().isFalse());
            HintManager hintManager = parser.getHintManager();
            hintBackup.set(hintManager.backup());
            hintManager.exitScope();
        };
        Trigger trigger = triggerSupplier.apply(beforeLoading, afterLoading);
        if (delayed.get()) {
            Skript.error("Delays can't be used within a '" + name + "' section");
            return null;
        }
        HintManager hintManager = ParserInstance.get().getHintManager();
        hintManager.enterScope(false);
        hintManager.restore((HintManager.Backup)hintBackup.get());
        hintManager.exitScope();
        return trigger;
    }
}

