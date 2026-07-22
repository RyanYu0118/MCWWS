/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Keyed
 *  org.bukkit.Material
 *  org.bukkit.entity.EntityType
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.UnmodifiableView
 */
package org.skriptlang.skript.bukkit.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public class TagType<T extends Keyed> {
    private static final List<TagType<?>> REGISTERED_TAG_TYPES = Collections.synchronizedList(new ArrayList());
    public static final TagType<Material> ITEMS = new TagType<Material>("item", Material.class);
    public static final TagType<Material> BLOCKS = new TagType<Material>("block", Material.class);
    public static final TagType<EntityType> ENTITIES = new TagType<EntityType>("entity [type]", "entity type", EntityType.class);
    private final String pattern;
    private final String toString;
    private final Class<T> type;

    public TagType(String pattern, Class<T> type) {
        this(pattern, pattern, type);
    }

    public TagType(String pattern, String toString, Class<T> type) {
        this.pattern = pattern;
        this.type = type;
        this.toString = toString;
    }

    public String pattern() {
        return this.pattern;
    }

    public Class<T> type() {
        return this.type;
    }

    public String toString() {
        return this.toString;
    }

    public static void addType(TagType<?> ... type) {
        REGISTERED_TAG_TYPES.addAll(List.of(type));
    }

    @Contract(pure=true)
    @NotNull
    public static @UnmodifiableView List<TagType<?>> getTypes() {
        return Collections.unmodifiableList(REGISTERED_TAG_TYPES);
    }

    public static TagType<?> @NotNull [] getType(int i) {
        if (i < 0) {
            return REGISTERED_TAG_TYPES.toArray(new TagType[0]);
        }
        return new TagType[]{REGISTERED_TAG_TYPES.get(i)};
    }

    public static TagType<?> @NotNull [] fromParseMark(int i) {
        return TagType.getType(i - 1);
    }

    @NotNull
    public static String getFullPattern() {
        return TagType.getFullPattern(false);
    }

    @NotNull
    public static String getFullPattern(boolean required) {
        StringBuilder fullPattern = new StringBuilder(required ? "(" : "[");
        int numRegistries = REGISTERED_TAG_TYPES.size();
        for (int i = 0; i < numRegistries; ++i) {
            fullPattern.append(i + 1).append(":").append(REGISTERED_TAG_TYPES.get(i).pattern());
            if (i + 1 == numRegistries) continue;
            fullPattern.append("|");
        }
        fullPattern.append(required ? ")" : "]");
        return fullPattern.toString();
    }

    static {
        TagType.addType(ITEMS, BLOCKS, ENTITIES);
    }
}

