/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Keyed
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Tag
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.tags.sources;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.tags.TagType;
import org.skriptlang.skript.bukkit.tags.sources.PaperTagSource;
import org.skriptlang.skript.bukkit.tags.sources.SkriptTagSource;
import org.skriptlang.skript.bukkit.tags.sources.TagOrigin;
import org.skriptlang.skript.bukkit.tags.sources.TagSource;

public sealed class CustomTagSource<T extends Keyed>
extends TagSource<T>
permits PaperTagSource, SkriptTagSource {
    final Map<NamespacedKey, Tag<T>> tags = new ConcurrentHashMap<NamespacedKey, Tag<T>>();

    @SafeVarargs
    CustomTagSource(TagOrigin origin, @NotNull Iterable<Tag<T>> tags, TagType<T> ... types) {
        super(origin, types);
        for (Tag<T> tag : tags) {
            this.tags.put(tag.getKey(), tag);
        }
    }

    @Override
    public Iterable<Tag<T>> getAllTags() {
        return Collections.unmodifiableCollection(this.tags.values());
    }

    @Override
    @Nullable
    public Tag<T> getTag(NamespacedKey key) {
        return this.tags.get(key);
    }
}

