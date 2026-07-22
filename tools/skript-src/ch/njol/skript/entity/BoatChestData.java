/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.Boat$Type
 *  org.bukkit.entity.ChestBoat
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemData;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BoatChestData
extends EntityData<ChestBoat> {
    private static final Boat.Type[] types = Boat.Type.values();
    private static final Map<Material, Boat.Type> materialToType;

    public BoatChestData() {
        this(0);
    }

    public BoatChestData(@Nullable Boat.Type type) {
        this(type != null ? type.ordinal() + 2 : 1);
    }

    private BoatChestData(int type) {
        this.codeNameIndex = type;
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends ChestBoat> entityClass, @Nullable ChestBoat chestBoat) {
        if (chestBoat != null) {
            this.codeNameIndex = 2 + chestBoat.getBoatType().ordinal();
        }
        return true;
    }

    @Override
    public void set(ChestBoat chestBoat) {
        if (this.codeNameIndex == 1) {
            this.codeNameIndex += new Random().nextInt(Boat.Type.values().length);
        }
        if (this.codeNameIndex > 1) {
            chestBoat.setBoatType(types[this.codeNameIndex - 2]);
        }
    }

    @Override
    protected boolean match(ChestBoat chestBoat) {
        return this.codeNameIndex <= 1 || chestBoat.getBoatType().ordinal() == this.codeNameIndex - 2;
    }

    @Override
    public Class<? extends ChestBoat> getType() {
        return ChestBoat.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new BoatChestData();
    }

    @Override
    protected int hashCode_i() {
        return this.codeNameIndex <= 1 ? 0 : this.codeNameIndex;
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (entityData instanceof BoatChestData) {
            BoatChestData other = (BoatChestData)entityData;
            return this.codeNameIndex == other.codeNameIndex;
        }
        return false;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (entityData instanceof BoatChestData) {
            BoatChestData other = (BoatChestData)entityData;
            return this.codeNameIndex <= 1 || this.codeNameIndex == other.codeNameIndex;
        }
        return false;
    }

    public boolean isOfItemType(ItemType itemType) {
        for (ItemData itemData : itemType.getTypes()) {
            Material material = itemData.getType();
            Boat.Type type = materialToType.get(material);
            if (type == null) continue;
            int ordinal = type.ordinal();
            if (this.codeNameIndex > 1 && this.codeNameIndex != ordinal + 2) continue;
            return true;
        }
        return false;
    }

    static {
        if (!Skript.isRunningMinecraft(1, 21, 2)) {
            String[] patterns = new String[types.length + 2];
            patterns[0] = "chest boat";
            patterns[1] = "any chest boat";
            for (Boat.Type boat : types) {
                Object boatName = boat == Boat.Type.BAMBOO ? "bamboo chest raft" : boat.toString().replace("_", " ").toLowerCase(Locale.ENGLISH) + " chest boat";
                patterns[boat.ordinal() + 2] = boatName;
            }
            EntityData.register(BoatChestData.class, "chest boat", ChestBoat.class, 0, patterns);
        }
        materialToType = new HashMap<Material, Boat.Type>();
        materialToType.put(Material.OAK_CHEST_BOAT, Boat.Type.OAK);
        materialToType.put(Material.BIRCH_CHEST_BOAT, Boat.Type.BIRCH);
        materialToType.put(Material.SPRUCE_CHEST_BOAT, Boat.Type.SPRUCE);
        materialToType.put(Material.JUNGLE_CHEST_BOAT, Boat.Type.JUNGLE);
        materialToType.put(Material.DARK_OAK_CHEST_BOAT, Boat.Type.DARK_OAK);
        materialToType.put(Material.ACACIA_CHEST_BOAT, Boat.Type.ACACIA);
        materialToType.put(Material.MANGROVE_CHEST_BOAT, Boat.Type.MANGROVE);
        materialToType.put(Material.CHERRY_CHEST_BOAT, Boat.Type.CHERRY);
        materialToType.put(Material.BAMBOO_CHEST_RAFT, Boat.Type.BAMBOO);
    }
}

