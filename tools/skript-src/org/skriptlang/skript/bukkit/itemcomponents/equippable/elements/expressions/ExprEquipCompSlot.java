/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.datacomponent.item.Equippable
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.EquipmentSlot
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import io.papermc.paper.datacomponent.item.Equippable;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableExperimentSyntax;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableWrapper;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Equippable Component - Equipment Slot")
@Description(value={"The equipment slot an item can be equipped to.\nNOTE: Equippable component elements are experimental. Thus, they are subject to change and may not work as intended.\n"})
@Example.Examples(value={@Example(value="set the equipment slot of {_item} to chest slot"), @Example(value="set {_component} to the equippable component of {_item}\nset the equipment slot of {_component} to boots slot\n")})
@RequiredPlugins(value={"Minecraft 1.21.2+"})
@Since(value={"2.13"})
public class ExprEquipCompSlot
extends SimplePropertyExpression<EquippableWrapper, EquipmentSlot>
implements EquippableExperimentSyntax {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprEquipCompSlot.infoBuilder(ExprEquipCompSlot.class, EquipmentSlot.class, "equipment slot", "equippablecomponents", true).supplier(ExprEquipCompSlot::new)).build());
    }

    @Override
    @Nullable
    public EquipmentSlot convert(EquippableWrapper wrapper) {
        return ((Equippable)wrapper.getComponent()).slot();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(EquipmentSlot.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        assert (delta != null);
        EquipmentSlot providedSlot = (EquipmentSlot)delta[0];
        this.getExpr().stream(event).forEach(wrapper -> {
            Equippable changed = wrapper.clone(providedSlot);
            wrapper.applyComponent(changed);
        });
    }

    @Override
    public Class<EquipmentSlot> getReturnType() {
        return EquipmentSlot.class;
    }

    @Override
    protected String getPropertyName() {
        return "equipment slot";
    }
}

