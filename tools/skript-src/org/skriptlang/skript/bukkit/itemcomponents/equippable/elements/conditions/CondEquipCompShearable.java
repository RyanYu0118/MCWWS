/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.datacomponent.item.Equippable
 */
package org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import io.papermc.paper.datacomponent.item.Equippable;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableExperimentSyntax;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableWrapper;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Equippable Component - Can Be Sheared Off")
@Description(value={"Whether an item can be sheared off of an entity.\nNOTE: Equippable component elements are experimental. Thus, they are subject to change and may not work as intended.\n"})
@Example.Examples(value={@Example(value="if {_item} can be sheared off:\n\tadd \"Shearable\" to lore of {_item}\n"), @Example(value="set {_component} to the equippable component of {_item}\nif {_component} can not be sheared off:\n\tallow {_component} to be sheared off\n")})
@RequiredPlugins(value={"Minecraft 1.21.6+"})
@Since(value={"2.13"})
public class CondEquipCompShearable
extends PropertyCondition<EquippableWrapper>
implements EquippableExperimentSyntax {
    public static void register(SyntaxRegistry registry) {
        if (!EquippableWrapper.HAS_CAN_BE_SHEARED) {
            return;
        }
        registry.register(SyntaxRegistry.CONDITION, CondEquipCompShearable.infoBuilder(CondEquipCompShearable.class, PropertyCondition.PropertyType.CAN, "be sheared off [of entities]", "equippablecomponents").supplier(CondEquipCompShearable::new).build());
    }

    @Override
    public boolean check(EquippableWrapper wrapper) {
        return ((Equippable)wrapper.getComponent()).canBeSheared();
    }

    @Override
    protected PropertyCondition.PropertyType getPropertyType() {
        return PropertyCondition.PropertyType.CAN;
    }

    @Override
    protected String getPropertyName() {
        return "be sheared off of entities";
    }
}

