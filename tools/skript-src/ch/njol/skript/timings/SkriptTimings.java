/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  co.aikar.timings.Timing
 *  co.aikar.timings.Timings
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.timings;

import ch.njol.skript.Skript;
import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class SkriptTimings {
    private static volatile boolean enabled;
    private static Skript skript;

    @Nullable
    public static Object start(String name) {
        if (!SkriptTimings.enabled()) {
            return null;
        }
        Timing timing = Timings.of((Plugin)skript, (String)name);
        timing.startTimingIfSync();
        assert (timing != null);
        return timing;
    }

    public static void stop(@Nullable Object timing) {
        if (timing == null) {
            return;
        }
        ((Timing)timing).stopTimingIfSync();
    }

    public static boolean enabled() {
        return enabled && Timings.isTimingsEnabled();
    }

    public static void setEnabled(boolean flag) {
        enabled = flag;
    }

    public static void setSkript(Skript plugin) {
        skript = plugin;
    }
}

