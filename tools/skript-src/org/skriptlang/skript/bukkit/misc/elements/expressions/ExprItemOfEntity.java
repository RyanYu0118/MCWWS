/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.AbstractArrow
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.ItemDisplay
 *  org.bukkit.entity.ItemFrame
 *  org.bukkit.entity.ThrowableProjectile
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.misc.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.slot.AbstractArrowSlot;
import ch.njol.skript.util.slot.DisplayEntitySlot;
import ch.njol.skript.util.slot.DroppedItemSlot;
import ch.njol.skript.util.slot.ItemFrameSlot;
import ch.njol.skript.util.slot.Slot;
import ch.njol.skript.util.slot.ThrowableProjectileSlot;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.ThrowableProjectile;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Item of an Entity")
@Description(value={"An item associated with an entity. For dropped item entities, it gets the item that was dropped.", "For item frames, the item inside the frame is returned.", "For throwable projectiles (snowballs, enderpearls etc.) or item displays, it gets the displayed item.", "For arrows, it gets the item that will be picked up when retrieving the arrow. Note that setting the item may not change the displayed model, and that setting a spectral arrow to a non-spectral arrow or vice-versa will not affect the effects of the projectile.", "Other entities do not have items associated with them."})
@Example.Examples(value={@Example(value="item of event-entity"), @Example(value="set the item inside of event-entity to a diamond sword named \"Example\"")})
@Since(value={"2.2-dev35, 2.2-dev36 (improved), 2.5.2 (throwable projectiles), 2.10 (item displays), 2.14.1 (arrows)"})
public class ExprItemOfEntity
extends SimplePropertyExpression<Entity, Slot> {
    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprItemOfEntity.infoBuilder(ExprItemOfEntity.class, Slot.class, "item [inside]", "entities", false).supplier(ExprItemOfEntity::new)).build());
    }

    @Override
    @Nullable
    public Slot convert(Entity entity) {
        if (entity instanceof ItemFrame) {
            ItemFrame itemFrame = (ItemFrame)entity;
            return new ItemFrameSlot(itemFrame);
        }
        if (entity instanceof Item) {
            Item item = (Item)entity;
            return new DroppedItemSlot(item);
        }
        if (entity instanceof ThrowableProjectile) {
            ThrowableProjectile throwableProjectile = (ThrowableProjectile)entity;
            return new ThrowableProjectileSlot(throwableProjectile);
        }
        if (entity instanceof AbstractArrow) {
            AbstractArrow arrow = (AbstractArrow)entity;
            return new AbstractArrowSlot(arrow);
        }
        if (entity instanceof ItemDisplay) {
            ItemDisplay itemDisplay = (ItemDisplay)entity;
            return new DisplayEntitySlot(itemDisplay);
        }
        return null;
    }

    @Override
    public Class<? extends Slot> getReturnType() {
        return Slot.class;
    }

    @Override
    protected String getPropertyName() {
        return "item inside";
    }
}

