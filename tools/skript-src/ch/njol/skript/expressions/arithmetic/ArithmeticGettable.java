/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions.arithmetic;

import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public interface ArithmeticGettable<T> {
    @Nullable
    public T get(Event var1);

    public Class<? extends T> getReturnType();
}

