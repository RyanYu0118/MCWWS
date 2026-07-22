/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.entity.Enderman
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.coll.CollectionUtils;
import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Enderman;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EndermanData
extends EntityData<Enderman> {
    private static final ArgsMessage FORMAT = new ArgsMessage("entities.enderman.format");
    private ItemType @Nullable [] hand = null;

    public EndermanData() {
    }

    public EndermanData(ItemType @Nullable [] hand) {
        this.hand = hand;
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        if (exprs[0] != null) {
            this.hand = (ItemType[])exprs[0].getAll();
        }
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Enderman> entityClass, @Nullable Enderman enderman) {
        BlockData data;
        if (enderman != null && (data = enderman.getCarriedBlock()) != null) {
            Material type = data.getMaterial();
            this.hand = new ItemType[]{new ItemType(type)};
        }
        return true;
    }

    @Override
    public void set(Enderman enderman) {
        if (this.hand != null) {
            ItemType itemType = CollectionUtils.getRandom(this.hand);
            assert (itemType != null);
            ItemStack itemStack = itemType.getBlock().getRandom();
            if (itemStack != null) {
                enderman.setCarriedBlock(Bukkit.createBlockData((Material)itemStack.getType()));
            }
        }
    }

    @Override
    public boolean match(Enderman enderman) {
        return this.hand == null || SimpleExpression.check(this.hand, type -> type != null && type.isOfType(enderman.getCarriedBlock().getMaterial()), false, false);
    }

    @Override
    public Class<Enderman> getType() {
        return Enderman.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new EndermanData();
    }

    @Override
    protected int hashCode_i() {
        return Arrays.hashCode(this.hand);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof EndermanData)) {
            return false;
        }
        EndermanData other = (EndermanData)entityData;
        return Arrays.equals(this.hand, other.hand);
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof EndermanData)) {
            return false;
        }
        EndermanData other = (EndermanData)entityData;
        if (this.hand != null) {
            return other.hand != null && ItemType.isSubset(this.hand, other.hand);
        }
        return true;
    }

    @Override
    public String toString(int flags) {
        Object[] hand = this.hand;
        if (hand == null) {
            return super.toString(flags);
        }
        return FORMAT.toString(super.toString(flags), Classes.toString(hand, false));
    }

    private boolean isSubhand(@Nullable ItemType[] sub) {
        if (this.hand != null) {
            return sub != null && ItemType.isSubset(this.hand, sub);
        }
        return true;
    }

    static {
        EntityData.register(EndermanData.class, "enderman", Enderman.class, "enderman");
    }
}

