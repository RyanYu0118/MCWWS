/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Keyed
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Tag
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.tags.sources;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.tags.TagType;
import org.skriptlang.skript.bukkit.tags.sources.TagOrigin;
import org.skriptlang.skript.bukkit.tags.sources.TagSource;

public class BukkitTagSource<T extends Keyed>
extends TagSource<T> {
    private final String registry;

    public BukkitTagSource(String registry, TagType<T> type) {
        super(TagOrigin.BUKKIT, type);
        this.registry = registry;
    }

    @Override
    @NotNull
    public Iterable<Tag<T>> getAllTags() {
        return Bukkit.getTags((String)this.registry, this.getTypes()[0].type());
    }

    @Override
    @Nullable
    public Tag<T> getTag(NamespacedKey key) {
        return Bukkit.getTag((String)this.registry, (NamespacedKey)key, this.getTypes()[0].type());
    }
}

