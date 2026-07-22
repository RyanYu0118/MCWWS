/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import java.util.Iterator;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public interface Loopable<T> {
    @Nullable
    public Iterator<? extends T> iterator(Event var1);

    public boolean isLoopOf(String var1);

    default public boolean supportsLoopPeeking() {
        return false;
    }
}

