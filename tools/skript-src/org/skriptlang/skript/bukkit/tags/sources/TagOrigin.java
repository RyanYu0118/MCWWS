/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.bukkit.tags.sources;

import java.util.Collection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.bukkit.tags.TagModule;

public enum TagOrigin {
    BUKKIT,
    PAPER,
    SKRIPT,
    ANY;


    @Contract(pure=true)
    @NotNull
    public static String getFullPattern() {
        if (TagModule.PAPER_TAGS_EXIST) {
            return "[:minecraft|:datapack|:paper|custom:(custom|skript)]";
        }
        return "[:minecraft|:datapack|custom:(custom|skript)]";
    }

    @Contract(value="_ -> new", pure=true)
    public static TagOrigin fromParseTags(@NotNull Collection<String> tags) {
        TagOrigin origin = ANY;
        if (tags.contains("minecraft") || tags.contains("datapack")) {
            origin = BUKKIT;
        } else if (tags.contains("paper")) {
            origin = PAPER;
        } else if (tags.contains("custom")) {
            origin = SKRIPT;
        }
        return origin;
    }

    public boolean matches(TagOrigin other) {
        return this == other || this == ANY || other == ANY;
    }

    @Contract(pure=true)
    @NotNull
    public String toString(boolean datapackOnly) {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                if (datapackOnly) {
                    yield "datapack";
                }
                yield "minecraft";
            }
            case 1 -> "paper";
            case 2 -> "custom";
            case 3 -> "";
        };
    }
}

