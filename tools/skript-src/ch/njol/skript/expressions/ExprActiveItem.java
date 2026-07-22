/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Active Item")
@Description(value={"Returns the item the entities are currently using (ie: the food they're eating, the bow they're drawing back, etc.). This cannot be changed. If an entity is not using any item, this will return null."})
@Example(value="on damage of player:\n\tif victim's active tool is a bow:\n\t\tinterrupt player's active item use\n")
@Since(value={"2.8.0"})
public class ExprActiveItem
extends SimplePropertyExpression<LivingEntity, ItemStack> {
    @Override
    @Nullable
    public ItemStack convert(LivingEntity livingEntity) {
        ItemStack item = livingEntity.getActiveItem();
        return item.getType() == Material.AIR ? null : item;
    }

    @Override
    public Class<? extends ItemStack> getReturnType() {
        return ItemStack.class;
    }

    @Override
    protected String getPropertyName() {
        return "active item";
    }

    static {
        if (Skript.methodExists(LivingEntity.class, "getActiveItem", new Class[0])) {
            ExprActiveItem.register(ExprActiveItem.class, ItemStack.class, "(raised|active) (tool|item|weapon)", "livingentities");
        }
    }
}

