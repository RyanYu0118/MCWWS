/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Keyed
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Tag
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.UnmodifiableView
 */
package org.skriptlang.skript.bukkit.tags;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public class SkriptTag<T extends Keyed>
implements Tag<T> {
    private final Set<T> contents;
    private final NamespacedKey key;

    public SkriptTag(NamespacedKey key, Collection<T> contents) {
        this.key = key;
        this.contents = new HashSet<T>(contents);
    }

    public boolean isTagged(@NotNull T item) {
        return this.contents.contains(item);
    }

    @NotNull
    public @UnmodifiableView Set<T> getValues() {
        return Collections.unmodifiableSet(this.contents);
    }

    @NotNull
    public NamespacedKey getKey() {
        return this.key;
    }
}

