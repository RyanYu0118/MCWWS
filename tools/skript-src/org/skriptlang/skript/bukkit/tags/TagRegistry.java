/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.MaterialTags
 *  com.google.common.collect.ArrayListMultimap
 *  com.google.common.collect.Iterators
 *  io.papermc.paper.tag.EntityTags
 *  org.bukkit.Keyed
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Tag
 *  org.bukkit.entity.EntityType
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.tags;

import ch.njol.util.coll.iterator.CheckedIterator;
import com.destroystokyo.paper.MaterialTags;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import io.papermc.paper.tag.EntityTags;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.tags.TagModule;
import org.skriptlang.skript.bukkit.tags.TagType;
import org.skriptlang.skript.bukkit.tags.sources.BukkitTagSource;
import org.skriptlang.skript.bukkit.tags.sources.PaperTagSource;
import org.skriptlang.skript.bukkit.tags.sources.SkriptTagSource;
import org.skriptlang.skript.bukkit.tags.sources.TagOrigin;
import org.skriptlang.skript.bukkit.tags.sources.TagSource;

public class TagRegistry {
    private final TagSourceMap tagSourceMap = new TagSourceMap();

    TagRegistry() {
        this.tagSourceMap.put(TagType.ITEMS, new BukkitTagSource<Material>("items", TagType.ITEMS));
        this.tagSourceMap.put(TagType.BLOCKS, new BukkitTagSource<Material>("blocks", TagType.BLOCKS));
        this.tagSourceMap.put(TagType.ENTITIES, new BukkitTagSource<EntityType>("entity_types", TagType.ENTITIES));
        if (TagModule.PAPER_TAGS_EXIST) {
            try {
                ArrayList itemTags = new ArrayList();
                ArrayList blockTags = new ArrayList();
                ArrayList blockAndItemTag = new ArrayList();
                block2: for (Field field : MaterialTags.class.getDeclaredFields()) {
                    if (!field.canAccess(null)) continue;
                    Field[] tag = (Field[])field.get(null);
                    boolean hasItem = false;
                    boolean hasBlock = false;
                    for (Material material : tag.getValues()) {
                        if (!hasBlock && material.isBlock()) {
                            blockTags.add(tag);
                            hasBlock = true;
                        }
                        if (!hasItem && material.isItem()) {
                            itemTags.add(tag);
                            hasItem = true;
                        }
                        if (!hasItem || !hasBlock) continue;
                        blockAndItemTag.add(tag);
                        continue block2;
                    }
                }
                PaperTagSource paperMaterialTags = new PaperTagSource(blockAndItemTag, TagType.BLOCKS, TagType.ITEMS);
                PaperTagSource paperItemTags = new PaperTagSource(itemTags, TagType.ITEMS);
                PaperTagSource paperBlockTags = new PaperTagSource(blockTags, TagType.BLOCKS);
                this.tagSourceMap.put(TagType.BLOCKS, paperMaterialTags);
                this.tagSourceMap.put(TagType.ITEMS, paperMaterialTags);
                this.tagSourceMap.put(TagType.BLOCKS, paperBlockTags);
                this.tagSourceMap.put(TagType.ITEMS, paperItemTags);
                ArrayList entityTags = new ArrayList();
                for (Field field : EntityTags.class.getDeclaredFields()) {
                    if (!field.canAccess(null)) continue;
                    entityTags.add((Tag)field.get(null));
                }
                PaperTagSource paperEntityTags = new PaperTagSource(entityTags, TagType.ENTITIES);
                this.tagSourceMap.put(TagType.ENTITIES, paperEntityTags);
            }
            catch (IllegalAccessException illegalAccessException) {
                // empty catch block
            }
        }
        SkriptTagSource.makeDefaultSources();
        this.tagSourceMap.put(TagType.ITEMS, SkriptTagSource.ITEMS());
        this.tagSourceMap.put(TagType.BLOCKS, SkriptTagSource.BLOCKS());
        this.tagSourceMap.put(TagType.ENTITIES, SkriptTagSource.ENTITIES());
    }

    public <T extends Keyed> Iterable<Tag<T>> getTags(TagOrigin origin, Class<T> typeClass, TagType<?> ... types) {
        final ArrayList tagIterators = new ArrayList();
        if (types == null) {
            types = (TagType[])this.tagSourceMap.map.keys().toArray((Object[])new TagType[0]);
        }
        for (TagType<?> type : types) {
            Iterator<Tag<?>> iterator;
            if (!typeClass.isAssignableFrom(type.type()) || !(iterator = this.getTags(origin, type).iterator()).hasNext()) continue;
            tagIterators.add(iterator);
        }
        return new Iterable<Tag<T>>(this){

            @Override
            @NotNull
            public Iterator<Tag<T>> iterator() {
                return Iterators.concat(tagIterators.iterator());
            }
        };
    }

    public <T extends Keyed> Iterable<Tag<T>> getTags(TagOrigin origin, TagType<T> type) {
        if (!this.tagSourceMap.containsKey(type)) {
            return List.of();
        }
        final Iterator<TagSource<T>> tagSources = this.tagSourceMap.get(origin, type).iterator();
        if (!tagSources.hasNext()) {
            return List.of();
        }
        return new Iterable<Tag<T>>(this){

            @Override
            @NotNull
            public Iterator<Tag<T>> iterator() {
                return Iterators.concat((Iterator)new Iterator<Iterator<Tag<T>>>(){

                    @Override
                    public boolean hasNext() {
                        return tagSources.hasNext();
                    }

                    @Override
                    public Iterator<Tag<T>> next() {
                        return ((TagSource)tagSources.next()).getAllTags().iterator();
                    }
                });
            }
        };
    }

    public <T extends Keyed> Iterable<Tag<T>> getMatchingTags(TagOrigin origin, TagType<T> type, final Predicate<Tag<T>> predicate) {
        final Iterator<Tag<T>> tagIterator = this.getTags(origin, type).iterator();
        return new Iterable<Tag<T>>(this){

            @Override
            @NotNull
            public Iterator<Tag<T>> iterator() {
                return new CheckedIterator(tagIterator, predicate::test);
            }
        };
    }

    @Nullable
    public <T extends Keyed> Tag<T> getTag(TagOrigin origin, TagType<T> type, NamespacedKey key) {
        for (TagSource<T> source : this.tagSourceMap.get(origin, type)) {
            Tag<T> tag = source.getTag(key);
            if (tag == null) continue;
            return tag;
        }
        return null;
    }

    private static class TagSourceMap {
        private final ArrayListMultimap<TagType<?>, TagSource<?>> map = ArrayListMultimap.create();

        private TagSourceMap() {
        }

        public <T extends Keyed> void put(TagType<T> key, TagSource<T> value) {
            this.map.put(key, value);
        }

        @NotNull
        public <T extends Keyed> List<TagSource<T>> get(TagOrigin origin, TagType<T> key) {
            ArrayList<TagSource<T>> sources = new ArrayList<TagSource<T>>();
            for (TagSource source : this.map.get(key)) {
                if (!source.getOrigin().matches(origin)) continue;
                sources.add(source);
            }
            return sources;
        }

        public <T extends Keyed> boolean containsKey(TagType<T> type) {
            return this.map.containsKey(type);
        }
    }
}

