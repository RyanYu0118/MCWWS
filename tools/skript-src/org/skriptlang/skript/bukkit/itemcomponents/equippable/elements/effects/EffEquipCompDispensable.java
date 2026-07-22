/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableExperimentSyntax;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableWrapper;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Equippable Component - Dispense")
@Description(value={"Whether the item can be dispensed by a dispenser.\nNOTE: Equippable component elements are experimental. Thus, they are subject to change and may not work as intended.\n"})
@Example.Examples(value={@Example(value="allow {_item} to be dispensed"), @Example(value="set {_component} to the equippable component of {_item}\nprevent {_component} from being dispensed\n")})
@RequiredPlugins(value={"Minecraft 1.21.2+"})
@Since(value={"2.13"})
public class EffEquipCompDispensable
extends Effect
implements EquippableExperimentSyntax {
    private Expression<EquippableWrapper> wrappers;
    private boolean dispensable;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffEquipCompDispensable.class).addPatterns("allow %equippablecomponents% to be dispensed", "make %equippablecomponents% dispensable", "let %equippablecomponents% be dispensed", "(block|prevent|disallow) %equippablecomponents% from being dispensed", "make %equippablecomponents% not dispensable").supplier(EffEquipCompDispensable::new).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.wrappers = exprs[0];
        this.dispensable = matchedPattern < 3;
        return true;
    }

    @Override
    protected void execute(Event event) {
        this.wrappers.stream(event).forEach(wrapper -> wrapper.editBuilder(builder -> builder.dispensable(this.dispensable)));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.dispensable) {
            return "allow " + this.wrappers.toString(event, debug) + " to be dispensed";
        }
        return "prevent " + this.wrappers.toString(event, debug) + " from being dispensed";
    }
}

