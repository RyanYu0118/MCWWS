/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.entity.FallingBlock
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
import ch.njol.skript.localization.Adjective;
import ch.njol.skript.localization.Message;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.coll.CollectionUtils;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.FallingBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FallingBlockData
extends EntityData<FallingBlock> {
    private static final Message m_not_a_block_error = new Message("entities.falling block.not a block error");
    private static final Adjective m_adjective = new Adjective("entities.falling block.adjective");
    private ItemType @Nullable [] types = null;

    public FallingBlockData() {
    }

    public FallingBlockData(ItemType @Nullable [] types) {
        this.types = types;
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 1) {
            assert (exprs[0] != null);
            ItemType[] itemTypes = (ItemType[])exprs[0].getAll();
            this.types = (ItemType[])Arrays.stream(itemTypes).map(itemType -> {
                ItemType clone = itemType.getBlock().clone();
                Iterator<ItemData> iterator = clone.iterator();
                while (iterator.hasNext()) {
                    Material material = iterator.next().getType();
                    if (material.isBlock()) continue;
                    iterator.remove();
                }
                if (clone.numTypes() == 0) {
                    return null;
                }
                clone.setAmount(-1);
                clone.setAll(false);
                clone.clearEnchantments();
                return clone;
            }).filter(Objects::nonNull).toArray(ItemType[]::new);
            if (this.types.length == 0) {
                Skript.error(m_not_a_block_error.toString());
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends FallingBlock> entityClass, @Nullable FallingBlock fallingBlock) {
        if (fallingBlock != null) {
            this.types = new ItemType[]{new ItemType(fallingBlock.getBlockData())};
        }
        return true;
    }

    @Override
    public void set(FallingBlock fallingBlock) {
        assert (false);
    }

    @Override
    protected boolean match(FallingBlock fallingBlock) {
        if (this.types != null) {
            for (ItemType itemType : this.types) {
                if (!itemType.isOfType(fallingBlock.getBlockData())) continue;
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public Class<? extends FallingBlock> getType() {
        return FallingBlock.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new FallingBlockData();
    }

    @Override
    protected int hashCode_i() {
        return Arrays.hashCode(this.types);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof FallingBlockData)) {
            return false;
        }
        FallingBlockData other = (FallingBlockData)entityData;
        return Arrays.equals(this.types, other.types);
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof FallingBlockData)) {
            return false;
        }
        FallingBlockData other = (FallingBlockData)entityData;
        if (this.types != null) {
            if (other.types != null) {
                return ItemType.isSubset(this.types, other.types);
            }
            return false;
        }
        return true;
    }

    @Override
    @Nullable
    public FallingBlock spawn(Location loc, @Nullable Consumer<FallingBlock> consumer) {
        ItemType t;
        ItemType itemType = t = this.types == null ? new ItemType(Material.STONE) : CollectionUtils.getRandom(this.types);
        assert (t != null);
        Material material = t.getMaterial();
        if (!material.isBlock()) {
            assert (false) : t;
            return null;
        }
        FallingBlock fallingBlock = loc.getWorld().spawnFallingBlock(loc, material.createBlockData());
        if (consumer != null) {
            consumer.accept(fallingBlock);
        }
        return fallingBlock;
    }

    @Override
    public String toString(int flags) {
        Object[] types = this.types;
        if (types == null) {
            return super.toString(flags);
        }
        StringBuilder builder = new StringBuilder();
        builder.append(Noun.getArticleWithSpace(types[0].getTypes().get(0).getGender(), flags));
        builder.append(m_adjective.toString(types[0].getTypes().get(0).getGender(), flags));
        builder.append(" ");
        builder.append(Classes.toString(types, flags & 0xFFFFFFF9, false));
        return builder.toString();
    }

    static {
        EntityData.register(FallingBlockData.class, "falling block", FallingBlock.class, "falling block");
    }
}

