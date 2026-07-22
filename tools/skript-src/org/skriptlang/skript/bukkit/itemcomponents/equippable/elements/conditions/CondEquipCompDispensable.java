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
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import io.papermc.paper.datacomponent.item.Equippable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableExperimentSyntax;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableWrapper;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Equippable Component - Can Be Dispensed")
@Description(value={"Whether an item can be dispensed by a dispenser.\nNOTE: Equippable component elements are experimental. Thus, they are subject to change and may not work as intended.\n"})
@Example.Examples(value={@Example(value="if {_item} can be dispensed:\n\tadd \"Dispensable\" to lore of {_item}\n"), @Example(value="set {_component} to the equippable component of {_item}\nif {_component} is not able to be dispensed:\n\tallow {_component} to be dispensed\n")})
@RequiredPlugins(value={"Minecraft 1.21.2+"})
@Since(value={"2.13"})
public class CondEquipCompDispensable
extends PropertyCondition<EquippableWrapper>
implements EquippableExperimentSyntax {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, SyntaxInfo.builder(CondEquipCompDispensable.class).addPatterns(CondEquipCompDispensable.getPatterns(PropertyCondition.PropertyType.CAN, "be dispensed", "equippablecomponents")).addPatterns(CondEquipCompDispensable.getPatterns(PropertyCondition.PropertyType.BE, "(able to be dispensed|dispensable)", "equippablecomponents")).supplier(CondEquipCompDispensable::new).priority(DEFAULT_PRIORITY).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        this.setNegated(matchedPattern % 2 == 1);
        return true;
    }

    @Override
    public boolean check(EquippableWrapper wrapper) {
        return ((Equippable)wrapper.getComponent()).dispensable();
    }

    @Override
    protected String getPropertyName() {
        return "dispensable";
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append(this.getExpr(), "are");
        if (this.isNegated()) {
            builder.append((Object)"not");
        }
        builder.append((Object)"able to be dispensed");
        return builder.toString();
    }
}

