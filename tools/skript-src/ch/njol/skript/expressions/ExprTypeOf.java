/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.ConvertedExpression;
import ch.njol.skript.util.EnchantmentType;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.yggdrasil.YggdrasilSerializable;
import java.util.ArrayList;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

@Name(value="Type of")
@Description(value={"Type of a block, item, entity, inventory, potion effect or enchantment type.", "Types of items, blocks and block datas are item types similar to them but have amounts", "of one, no display names and, on Minecraft 1.13 and newer versions, are undamaged.", "Types of entities and inventories are entity types and inventory types known to Skript.", "Types of potion effects are potion effect types.", "Types of enchantment types are enchantments."})
@Example(value="on rightclick on an entity:\n\tmessage \"This is a %type of clicked entity%!\"\n")
@Since(value={"1.4, 2.5.2 (potion effect), 2.7 (block datas), 2.10 (enchantment type)"})
public class ExprTypeOf
extends SimplePropertyExpression<Object, Object> {
    private Class<?>[] returnTypes;
    private Class<?> superReturnType;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Expression<YggdrasilSerializable> expression = expressions[0];
        ArrayList<Class<Enchantment>> returnTypes = new ArrayList<Class<Enchantment>>();
        if (expression.canReturn(EntityData.class)) {
            returnTypes.add(EntityData.class);
        }
        if (expression.canReturn(ItemType.class) || expression.canReturn(BlockData.class)) {
            returnTypes.add(ItemType.class);
        }
        if (expression.canReturn(Inventory.class)) {
            returnTypes.add(InventoryType.class);
        }
        if (expression.canReturn(PotionEffect.class)) {
            returnTypes.add(PotionEffectType.class);
        }
        if (expression.canReturn(EnchantmentType.class)) {
            returnTypes.add(Enchantment.class);
        }
        this.returnTypes = returnTypes.toArray(new Class[0]);
        this.superReturnType = Utils.getSuperType(this.returnTypes);
        return super.init(expressions, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Object convert(Object object) {
        if (object instanceof EntityData) {
            EntityData entityData = (EntityData)object;
            return entityData.getSuperType();
        }
        if (object instanceof ItemType) {
            ItemType itemType = (ItemType)object;
            return itemType.getBaseType();
        }
        if (object instanceof Inventory) {
            Inventory inventory = (Inventory)object;
            return inventory.getType();
        }
        if (object instanceof PotionEffect) {
            PotionEffect potionEffect = (PotionEffect)object;
            return potionEffect.getType();
        }
        if (object instanceof BlockData) {
            BlockData blockData = (BlockData)object;
            return new ItemType(blockData.getMaterial());
        }
        if (object instanceof EnchantmentType) {
            EnchantmentType enchantmentType = (EnchantmentType)object;
            return enchantmentType.getType();
        }
        assert (false);
        return null;
    }

    @Override
    public Class<?> getReturnType() {
        return this.superReturnType;
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        return this.returnTypes;
    }

    @Override
    @Nullable
    protected <R> ConvertedExpression<Object, ? extends R> getConvertedExpr(Class<R> ... to) {
        if (!Converters.converterExists(EntityData.class, to) && !Converters.converterExists(ItemType.class, to)) {
            return null;
        }
        return super.getConvertedExpr(to);
    }

    @Override
    protected String getPropertyName() {
        return "type";
    }

    static {
        ExprTypeOf.register(ExprTypeOf.class, Object.class, "type", "entitydatas/itemtypes/inventories/potioneffects/blockdatas/enchantmenttypes");
    }
}

