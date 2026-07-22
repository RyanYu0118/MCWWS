/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.entry;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SimpleNode;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryData;

public abstract class KeyValueEntryData<T>
extends EntryData<T> {
    public KeyValueEntryData(String key, @Nullable T defaultValue, boolean optional) {
        super(key, defaultValue, optional);
    }

    public KeyValueEntryData(String key, @Nullable T defaultValue, boolean optional, boolean multiple) {
        super(key, defaultValue, optional, multiple);
    }

    @Override
    @Nullable
    public T getValue(Node node) {
        assert (node instanceof SimpleNode);
        String key = node.getKey();
        if (key == null) {
            throw new IllegalArgumentException("EntryData#getValue() called with invalid node.");
        }
        return this.getValue(ScriptLoader.replaceOptions(key).substring(this.getKey().length() + this.getSeparator().length()));
    }

    @Nullable
    protected abstract T getValue(String var1);

    public String getSeparator() {
        return ": ";
    }

    @Override
    public boolean canCreateWith(Node node) {
        if (!(node instanceof SimpleNode)) {
            return false;
        }
        String key = node.getKey();
        if (key == null) {
            return false;
        }
        key = ScriptLoader.replaceOptions(key);
        String prefix = this.getKey() + this.getSeparator();
        return key.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}

