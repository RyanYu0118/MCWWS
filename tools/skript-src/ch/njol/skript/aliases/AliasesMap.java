/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.aliases;

import ch.njol.skript.aliases.ItemData;
import ch.njol.skript.aliases.MatchQuality;
import ch.njol.skript.aliases.MaterialName;
import ch.njol.skript.entity.EntityData;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public class AliasesMap {
    private MaterialEntry[] materialEntries;

    public AliasesMap() {
        this.clear();
    }

    private MaterialEntry getEntry(ItemData item) {
        MaterialEntry entry = this.materialEntries[item.getType().ordinal()];
        assert (entry != null);
        return entry;
    }

    public void addAlias(AliasData data) {
        MaterialEntry entry = this.getEntry(data.getItem());
        if (data.getItem().isDefault()) {
            entry.defaultItem = data;
        } else {
            entry.items.add(data);
        }
    }

    public Match matchAlias(ItemData item) {
        MaterialEntry entry = this.getEntry(item);
        if (entry.defaultItem == null && entry.items.isEmpty()) {
            return new Match(MatchQuality.DIFFERENT, null);
        }
        MatchQuality maxQuality = MatchQuality.DIFFERENT;
        AliasData bestMatch = null;
        for (AliasData data : entry.items) {
            MatchQuality quality = item.matchAlias(data.getItem());
            if (!quality.isBetter(maxQuality)) continue;
            maxQuality = quality;
            bestMatch = data;
        }
        if (maxQuality.isBetter(MatchQuality.SAME_MATERIAL)) {
            assert (bestMatch != null);
            return new Match(maxQuality, bestMatch);
        }
        AliasData defaultItem = entry.defaultItem;
        if (defaultItem != null) {
            return new Match(item.matchAlias(defaultItem.getItem()), defaultItem);
        }
        if (bestMatch != null) {
            return new Match(MatchQuality.SAME_MATERIAL, bestMatch);
        }
        throw new AssertionError();
    }

    public Match exactMatch(ItemData item) {
        MaterialEntry entry = this.getEntry(item);
        if (entry.defaultItem == null && entry.items.isEmpty()) {
            return new Match(MatchQuality.DIFFERENT, null);
        }
        for (AliasData data : entry.items) {
            if (item.matchAlias(data.getItem()) != MatchQuality.EXACT) continue;
            return new Match(MatchQuality.EXACT, data);
        }
        return new Match(MatchQuality.DIFFERENT, null);
    }

    public void clear() {
        this.materialEntries = new MaterialEntry[Material.values().length];
        for (int i = 0; i < this.materialEntries.length; ++i) {
            this.materialEntries[i] = new MaterialEntry();
        }
    }

    private static class MaterialEntry {
        @Nullable
        public AliasData defaultItem;
        public final List<AliasData> items = new ArrayList<AliasData>();
    }

    public static class AliasData {
        private final ItemData item;
        private final MaterialName name;
        private final String minecraftId;
        @Nullable
        private final EntityData<?> relatedEntity;

        public AliasData(ItemData item, MaterialName name, String minecraftId, @Nullable EntityData<?> relatedEntity) {
            this.item = item;
            this.name = name;
            this.minecraftId = minecraftId;
            this.relatedEntity = relatedEntity;
        }

        public ItemData getItem() {
            return this.item;
        }

        public MaterialName getName() {
            return this.name;
        }

        public String getMinecraftId() {
            return this.minecraftId;
        }

        @Nullable
        public EntityData<?> getRelatedEntity() {
            return this.relatedEntity;
        }
    }

    public static class Match {
        private final MatchQuality quality;
        @Nullable
        private final AliasData data;

        public Match(MatchQuality quality, @Nullable AliasData data) {
            this.quality = quality;
            this.data = data;
        }

        public MatchQuality getQuality() {
            return this.quality;
        }

        @Nullable
        public AliasData getData() {
            return this.data;
        }
    }
}

