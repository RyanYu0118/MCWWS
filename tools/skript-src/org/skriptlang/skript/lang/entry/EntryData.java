/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.entry;

import ch.njol.skript.config.Node;
import org.jetbrains.annotations.Nullable;

public abstract class EntryData<T> {
    private final String key;
    @Nullable
    private final T defaultValue;
    private final boolean optional;
    private final boolean multiple;

    public EntryData(String key, @Nullable T defaultValue, boolean optional) {
        this(key, defaultValue, optional, false);
    }

    public EntryData(String key, @Nullable T defaultValue, boolean optional, boolean multiple) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.optional = optional;
        this.multiple = multiple;
    }

    public String getKey() {
        return this.key;
    }

    @Nullable
    public T getDefaultValue() {
        return this.defaultValue;
    }

    public boolean isOptional() {
        return this.optional;
    }

    public boolean supportsMultiple() {
        return this.multiple;
    }

    @Nullable
    public abstract T getValue(Node var1);

    public abstract boolean canCreateWith(Node var1);
}

