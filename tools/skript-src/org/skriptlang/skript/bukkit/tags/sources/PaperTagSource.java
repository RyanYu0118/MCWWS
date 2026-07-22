/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Keyed
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Tag
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.bukkit.tags.sources;

import java.util.ArrayList;
import java.util.Set;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.bukkit.tags.TagType;
import org.skriptlang.skript.bukkit.tags.sources.CustomTagSource;
import org.skriptlang.skript.bukkit.tags.sources.TagOrigin;

public final class PaperTagSource<T extends Keyed>
extends CustomTagSource<T> {
    @NotNull
    private static <T extends Keyed> Iterable<Tag<T>> getPaperTags(@NotNull Iterable<Tag<T>> tags) {
        ArrayList<Tag<T>> modifiedTags = new ArrayList<Tag<T>>();
        for (Tag<T> tag : tags) {
            modifiedTags.add(new PaperTag<T>(tag));
        }
        return modifiedTags;
    }

    @SafeVarargs
    public PaperTagSource(Iterable<Tag<T>> tags, TagType<T> ... types) {
        super(TagOrigin.PAPER, PaperTagSource.getPaperTags(tags), types);
    }

    private static class PaperTag<T1 extends Keyed>
    implements Tag<T1> {
        private final Tag<T1> paperTag;
        private final NamespacedKey key;

        public PaperTag(@NotNull Tag<T1> paperTag) {
            this.paperTag = paperTag;
            this.key = NamespacedKey.fromString((String)paperTag.getKey().toString().replace("_settag", ""));
        }

        public boolean isTagged(@NotNull T1 item) {
            return this.paperTag.isTagged(item);
        }

        @NotNull
        public Set<T1> getValues() {
            return this.paperTag.getValues();
        }

        @NotNull
        public NamespacedKey getKey() {
            return this.key;
        }
    }
}

