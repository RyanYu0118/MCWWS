/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.util;

import java.util.Collection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.util.PriorityImpl;

public interface Priority
extends Comparable<Priority> {
    @Contract(value="-> new")
    public static Priority base() {
        return new PriorityImpl();
    }

    @Contract(value="_ -> new")
    public static Priority before(Priority priority) {
        return new PriorityImpl(priority, true);
    }

    @Contract(value="_ -> new")
    public static Priority after(Priority priority) {
        return new PriorityImpl(priority, false);
    }

    public @Unmodifiable Collection<Priority> after();

    public @Unmodifiable Collection<Priority> before();
}

