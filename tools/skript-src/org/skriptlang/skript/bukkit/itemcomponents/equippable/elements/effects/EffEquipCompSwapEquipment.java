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

@Name(value="Equippable Component - Swap Equipment")
@Description(value={"Whether the item can be swapped by right clicking with it in your hand.\nNOTE: Equippable component elements are experimental. Thus, they are subject to change and may not work as intended.\n"})
@Example.Examples(value={@Example(value="allow {_item} to swap equipment"), @Example(value="set {_component} to the equippable component of {_item}\nprevent {_component} from swapping equipment on right click\n")})
@RequiredPlugins(value={"Minecraft 1.21.2+"})
@Since(value={"2.13"})
public class EffEquipCompSwapEquipment
extends Effect
implements EquippableExperimentSyntax {
    private Expression<EquippableWrapper> wrappers;
    private boolean swappable;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffEquipCompSwapEquipment.class).addPatterns("(allow|force) %equippablecomponents% to swap equipment [on right click|when right clicked]", "(make|let) %equippablecomponents% swap equipment [on right click|when right clicked]", "(block|prevent|disallow) %equippablecomponents% from swapping equipment [on right click|when right clicked]", "make %equippablecomponents% not swap equipment [on right click|when right clicked]").supplier(EffEquipCompSwapEquipment::new).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.wrappers = exprs[0];
        this.swappable = matchedPattern < 2;
        return true;
    }

    @Override
    protected void execute(Event event) {
        this.wrappers.stream(event).forEach(wrapper -> wrapper.editBuilder(builder -> builder.swappable(this.swappable)));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.swappable) {
            return "allow " + this.wrappers.toString(event, debug) + " to swap equipment";
        }
        return "prevent " + this.wrappers.toString(event, debug) + " from swapping equipment";
    }
}

