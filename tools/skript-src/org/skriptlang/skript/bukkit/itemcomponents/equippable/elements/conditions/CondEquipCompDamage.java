/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.datacomponent.item.Equippable
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.SyntaxStringBuilder;
import io.papermc.paper.datacomponent.item.Equippable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableExperimentSyntax;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableWrapper;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Equippable Component - Will Lose Durability")
@Description(value={"Whether an item will be damaged when the wearer gets injured.\nNOTE: Equippable component elements are experimental. Thus, they are subject to change and may not work as intended.\n"})
@Example.Examples(value={@Example(value="if {_item} will lose durability when hurt:\n\tadd \"Damageable on injury\" to lore of {_item}\n"), @Example(value="set {_component} to the equippable component of {_item}\nif {_component} won't lose durability on injury:\n\tmake {_component} lose durability when injured\n")})
@RequiredPlugins(value={"Minecraft 1.21.2+"})
@Since(value={"2.13"})
public class CondEquipCompDamage
extends PropertyCondition<EquippableWrapper>
implements EquippableExperimentSyntax {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, SyntaxInfo.builder(CondEquipCompDamage.class).addPatterns("%equippablecomponents% will (lose durability|be damaged) (on [wearer['s]] injury|when [[the] wearer [is]] (hurt|injured|damaged))", "%equippablecomponents% (will not|won't) (lose durability|be damaged) (on [wearer['s]] injury|when [[the] wearer [is]] (hurt|injured|damaged))").supplier(CondEquipCompDamage::new).build());
    }

    @Override
    public boolean check(EquippableWrapper wrapper) {
        return ((Equippable)wrapper.getComponent()).damageOnHurt();
    }

    @Override
    protected String getPropertyName() {
        return "lose durability when injured";
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append(this.getExpr(), "will");
        if (this.isNegated()) {
            builder.append((Object)"not");
        }
        builder.append((Object)"lose durability when injured");
        return builder.toString();
    }
}

