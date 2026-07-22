/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.util;

import java.util.Collection;
import java.util.Iterator;
import org.jetbrains.annotations.NotNull;

public interface Registry<T>
extends Iterable<T> {
    public Collection<T> elements();

    @Override
    @NotNull
    default public Iterator<T> iterator() {
        return this.elements().iterator();
    }
}

