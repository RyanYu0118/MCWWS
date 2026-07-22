/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Display
 *  org.bukkit.entity.ItemDisplay
 *  org.bukkit.entity.ItemDisplay$ItemDisplayTransform
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.entity.displays.item.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Item Display Transform")
@Description(value={"Returns or changes the <a href='#itemdisplaytransform'>item display transform</a> of <a href='#display'>item displays</a>."})
@Example.Examples(value={@Example(value="set the item transform of the last spawned item display to first person left handed"), @Example(value="set the item transform of the last spawned item display to no transform # Reset to default")})
@Since(value={"2.10"})
public class ExprItemDisplayTransform
extends SimplePropertyExpression<Display, ItemDisplay.ItemDisplayTransform> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprItemDisplayTransform.infoBuilder(ExprItemDisplayTransform.class, ItemDisplay.ItemDisplayTransform.class, "item [display] transform", "displays", true).supplier(ExprItemDisplayTransform::new)).build());
    }

    @Override
    @Nullable
    public ItemDisplay.ItemDisplayTransform convert(Display display) {
        if (display instanceof ItemDisplay) {
            ItemDisplay itemDisplay = (ItemDisplay)display;
            return itemDisplay.getItemDisplayTransform();
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.RESET -> CollectionUtils.array(new Class[0]);
            case Changer.ChangeMode.SET -> CollectionUtils.array(ItemDisplay.ItemDisplayTransform.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        ItemDisplay.ItemDisplayTransform transform = mode == Changer.ChangeMode.SET ? (ItemDisplay.ItemDisplayTransform)delta[0] : ItemDisplay.ItemDisplayTransform.NONE;
        for (Display display : (Display[])this.getExpr().getArray(event)) {
            if (!(display instanceof ItemDisplay)) continue;
            ItemDisplay itemDisplay = (ItemDisplay)display;
            itemDisplay.setItemDisplayTransform(transform);
        }
    }

    @Override
    public Class<? extends ItemDisplay.ItemDisplayTransform> getReturnType() {
        return ItemDisplay.ItemDisplayTransform.class;
    }

    @Override
    protected String getPropertyName() {
        return "item display transform";
    }
}

