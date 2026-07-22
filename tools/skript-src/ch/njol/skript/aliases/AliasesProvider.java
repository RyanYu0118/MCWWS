/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
 *  org.bukkit.Material
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.aliases;

import ch.njol.skript.aliases.AliasesMap;
import ch.njol.skript.aliases.InvalidMinecraftIdException;
import ch.njol.skript.aliases.ItemData;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.aliases.MatchQuality;
import ch.njol.skript.aliases.MaterialName;
import ch.njol.skript.bukkitutil.BukkitUnsafe;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.bukkitutil.block.BlockCompat;
import ch.njol.skript.bukkitutil.block.BlockValues;
import ch.njol.skript.entity.EntityData;
import com.google.gson.Gson;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class AliasesProvider {
    @Nullable
    private final AliasesProvider parent;
    private final Map<String, ItemType> aliases;
    private final Set<Material> materials;
    private final Gson gson;
    private final Map<String, VariationGroup> variations;
    private final AliasesMap aliasesMap;

    public AliasesProvider(int expectedCount, @Nullable AliasesProvider parent) {
        this.parent = parent;
        this.aliases = new HashMap<String, ItemType>(expectedCount);
        this.variations = new HashMap<String, VariationGroup>(expectedCount / 20);
        this.aliasesMap = new AliasesMap();
        this.materials = new ObjectOpenHashSet();
        this.gson = new Gson();
    }

    public Map<String, Object> parseMojangson(String raw) {
        return (Map)this.gson.fromJson(raw, Object.class);
    }

    public int applyTags(ItemStack stack, Map<String, Object> tags) {
        Object damage = tags.get("Damage");
        int flags = 0;
        if (damage instanceof Number) {
            ItemUtils.setDamage(stack, (int)((Number)damage).shortValue());
            tags.remove("Damage");
            flags |= 1;
        }
        if (tags.isEmpty()) {
            return flags;
        }
        String components = (String)tags.get("components");
        assert (components != null);
        ItemUtils.applyComponentData(stack, components);
        return flags |= 2;
    }

    public void addAlias(AliasName name, String id, @Nullable Map<String, Object> tags, Map<String, String> blockStates) {
        ItemType type;
        List<ItemData> datas;
        ItemType typeOfId = this.getAlias(id);
        EntityData<?> related = null;
        boolean deduplicate = true;
        String deduplicateFlag = blockStates.remove("deduplicate");
        if (deduplicateFlag != null) {
            boolean bl = deduplicate = !deduplicateFlag.equals("false");
        }
        if (typeOfId != null) {
            datas = typeOfId.getTypes();
        } else {
            AliasesMap.Match canonical;
            Material material = BukkitUnsafe.getMaterialFromNamespacedId(id);
            if (material == null) {
                throw new InvalidMinecraftIdException(id);
            }
            this.materials.add(material);
            String entityName = blockStates.remove("relatedEntity");
            if (entityName != null) {
                related = EntityData.parse(entityName);
            }
            ItemStack stack = null;
            int itemFlags = 0;
            if (material.isItem()) {
                stack = new ItemStack(material);
                if (tags != null) {
                    itemFlags = this.applyTags(stack, new HashMap<String, Object>(tags));
                }
            }
            BlockValues blockValues = BlockCompat.INSTANCE.createBlockValues(material, blockStates, stack, itemFlags);
            ItemData data = stack != null ? new ItemData(stack, blockValues) : new ItemData(material, blockValues);
            data.isAlias = true;
            data.itemFlags = itemFlags;
            if (deduplicate && (canonical = this.aliasesMap.exactMatch(data)).getQuality().isAtLeast(MatchQuality.EXACT)) {
                AliasesMap.AliasData aliasData = canonical.getData();
                assert (aliasData != null);
                data = aliasData.getItem();
            }
            datas = Collections.singletonList(data);
        }
        if (typeOfId == null) {
            ItemData data = datas.get(0);
            MaterialName materialName = new MaterialName(data.type, name.singular, name.plural, name.gender);
            this.aliasesMap.addAlias(new AliasesMap.AliasData(data, materialName, id, related));
        }
        if ((type = this.aliases.get(name.singular)) == null) {
            type = this.aliases.get(name.plural);
        }
        if (type == null) {
            type = new ItemType();
            this.aliases.put(name.singular, type);
            this.aliases.put(name.plural, type);
            type.addAll(datas);
        } else {
            block0: for (ItemData newData : datas) {
                for (ItemData existingData : type.getTypes()) {
                    if (newData != existingData && !newData.matchAlias(existingData).isAtLeast(MatchQuality.EXACT)) continue;
                    continue block0;
                }
                type.add(newData);
            }
        }
    }

    public void addVariationGroup(String name, VariationGroup group) {
        this.variations.put(name, group);
    }

    @Nullable
    public VariationGroup getVariationGroup(String name) {
        return this.variations.get(name);
    }

    @Nullable
    public ItemType getAlias(String alias) {
        ItemType item = this.aliases.get(alias);
        if (item == null && this.parent != null) {
            return this.parent.getAlias(alias);
        }
        return item;
    }

    public @Nullable AliasesMap.AliasData getAliasData(ItemData item) {
        AliasesMap.AliasData data = this.aliasesMap.matchAlias(item).getData();
        if (data == null && this.parent != null) {
            return this.parent.getAliasData(item);
        }
        return data;
    }

    @Nullable
    public String getMinecraftId(ItemData item) {
        AliasesMap.AliasData data = this.getAliasData(item);
        if (data != null) {
            return data.getMinecraftId();
        }
        return null;
    }

    @Nullable
    public MaterialName getMaterialName(ItemData item) {
        AliasesMap.AliasData data = this.getAliasData(item);
        if (data != null) {
            return data.getName();
        }
        return null;
    }

    @Nullable
    public EntityData<?> getRelatedEntity(ItemData item) {
        AliasesMap.AliasData data = this.getAliasData(item);
        if (data != null) {
            return data.getRelatedEntity();
        }
        return null;
    }

    public void clearAliases() {
        this.aliases.clear();
        this.materials.clear();
        this.variations.clear();
        this.aliasesMap.clear();
    }

    public int getAliasCount() {
        return this.aliases.size();
    }

    public boolean hasAliasForMaterial(Material material) {
        return this.materials.contains(material);
    }

    public static class AliasName {
        public final String singular;
        public final String plural;
        public final int gender;

        public AliasName(String singular, String plural, int gender) {
            this.singular = singular;
            this.plural = plural;
            this.gender = gender;
        }
    }

    public static class VariationGroup {
        public final List<String> keys = new ArrayList<String>();
        public final List<Variation> values = new ArrayList<Variation>();

        public void put(String key, Variation value) {
            this.keys.add(key);
            this.values.add(value);
        }
    }

    public static class Variation {
        @Nullable
        private final String id;
        private final int insertPoint;
        private final Map<String, Object> tags;
        private final Map<String, String> states;

        public Variation(@Nullable String id, int insertPoint, Map<String, Object> tags, Map<String, String> states) {
            this.id = id;
            this.insertPoint = insertPoint;
            this.tags = tags;
            this.states = states;
        }

        @Nullable
        public String getId() {
            return this.id;
        }

        public int getInsertPoint() {
            return this.insertPoint;
        }

        @Nullable
        public String insertId(@Nullable String inserted) {
            if (inserted == null) {
                return this.id;
            }
            inserted = inserted.substring(0, inserted.length() - 1);
            if (this.id == null) {
                return inserted;
            }
            String id = this.id;
            assert (id != null);
            if (this.insertPoint == -1) {
                return inserted;
            }
            String before = id.substring(0, this.insertPoint);
            String after = id.substring(this.insertPoint + 1);
            return before + inserted + after;
        }

        public Map<String, Object> getTags() {
            return this.tags;
        }

        public Map<String, String> getBlockStates() {
            return this.states;
        }

        public Variation merge(Variation other) {
            HashMap<String, Object> mergedTags = new HashMap<String, Object>(other.tags);
            mergedTags.putAll(this.tags);
            HashMap<String, String> mergedStates = new HashMap<String, String>(other.states);
            mergedStates.putAll(this.states);
            String id = this.insertId(other.id);
            return new Variation(id, -1, mergedTags, mergedStates);
        }
    }
}

