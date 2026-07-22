/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 */
package org.skriptlang.skript.util;

import org.jetbrains.annotations.Contract;

public interface ViewProvider<T> {
    @Contract(value="-> new")
    public T unmodifiableView();
}

