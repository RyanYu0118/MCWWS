/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.entity.LingeringPotion
 *  org.bukkit.entity.ThrownPotion
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.localization.Adjective;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.coll.CollectionUtils;
import java.util.Arrays;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LingeringPotion;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

public class ThrownPotionData
extends EntityData<ThrownPotion> {
    private static final Adjective m_adjective;
    private static final boolean LINGERING_POTION_ENTITY_USED;
    private static final Class<? extends ThrownPotion> LINGERING_POTION_ENTITY_CLASS;
    private static final Material POTION;
    private static final Material SPLASH_POTION;
    private static final Material LINGER_POTION;
    private ItemType @Nullable [] types;

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        if (exprs.length > 0 && exprs[0] != null) {
            ItemType[] itemTypes = (ItemType[])exprs[0].getAll();
            this.types = Converters.convert(itemTypes, ItemType.class, itemType -> {
                Material material = itemType.getMaterial();
                if (material == POTION) {
                    ItemMeta itemMeta = itemType.getItemMeta();
                    ItemType splashItem = new ItemType(SPLASH_POTION);
                    splashItem.setItemMeta(itemMeta);
                    return splashItem;
                }
                if (material != SPLASH_POTION && material != LINGER_POTION) {
                    return null;
                }
                return itemType;
            });
            return this.types.length != 0;
        }
        this.types = new ItemType[]{new ItemType(SPLASH_POTION)};
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends ThrownPotion> entityClass, @Nullable ThrownPotion thrownPotion) {
        if (thrownPotion != null) {
            ItemStack itemStack = thrownPotion.getItem();
            this.types = new ItemType[]{new ItemType(itemStack)};
        }
        return true;
    }

    @Override
    public void set(ThrownPotion thrownPotion) {
        if (this.types != null) {
            ItemType itemType = CollectionUtils.getRandom(this.types);
            assert (itemType != null);
            ItemStack itemStack = itemType.getRandom();
            if (itemStack == null) {
                return;
            }
            if (LINGERING_POTION_ENTITY_USED && LINGERING_POTION_ENTITY_CLASS.isInstance(thrownPotion) != (LINGER_POTION == itemStack.getType())) {
                return;
            }
            thrownPotion.setItem(itemStack);
        }
        assert (false);
    }

    @Override
    protected boolean match(ThrownPotion thrownPotion) {
        if (this.types != null) {
            for (ItemType itemType : this.types) {
                if (!itemType.isOfType(thrownPotion.getItem())) continue;
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public Class<? extends ThrownPotion> getType() {
        return ThrownPotion.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new ThrownPotionData();
    }

    @Override
    protected int hashCode_i() {
        return Arrays.hashCode(this.types);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof ThrownPotionData)) {
            return false;
        }
        ThrownPotionData other = (ThrownPotionData)entityData;
        return Arrays.equals(this.types, other.types);
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof ThrownPotionData)) {
            return false;
        }
        ThrownPotionData other = (ThrownPotionData)entityData;
        if (this.types == null) {
            return true;
        }
        return other.types != null && ItemType.isSubset(this.types, other.types);
    }

    @Override
    @Nullable
    public ThrownPotion spawn(Location location, @Nullable Consumer<ThrownPotion> consumer) {
        ItemType itemType = CollectionUtils.getRandom(this.types);
        assert (itemType != null);
        ItemStack itemStack = itemType.getRandom();
        if (itemStack == null) {
            return null;
        }
        Class<? extends ThrownPotion> thrownPotionClass = itemStack.getType() == LINGER_POTION ? LINGERING_POTION_ENTITY_CLASS : ThrownPotion.class;
        ThrownPotion potion = consumer != null ? EntityData.spawn(location, thrownPotionClass, consumer) : (ThrownPotion)location.getWorld().spawn(location, thrownPotionClass);
        if (potion == null) {
            return null;
        }
        potion.setItem(itemStack);
        return potion;
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
        EntityData.register(ThrownPotionData.class, "thrown potion", ThrownPotion.class, "thrown potion");
        m_adjective = new Adjective("entities.thrown potion.adjective");
        LINGERING_POTION_ENTITY_USED = !Skript.isRunningMinecraft(1, 14);
        LINGERING_POTION_ENTITY_CLASS = LINGERING_POTION_ENTITY_USED ? LingeringPotion.class : ThrownPotion.class;
        POTION = Material.POTION;
        SPLASH_POTION = Material.SPLASH_POTION;
        LINGER_POTION = Material.LINGERING_POTION;
    }
}

