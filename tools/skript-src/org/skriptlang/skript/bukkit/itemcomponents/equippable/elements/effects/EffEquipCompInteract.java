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

@Name(value="Equippable Component - Equip On Entities")
@Description(value={"Whether an entity should equip the item when right clicking on the entity with the item.\nNOTE: Equippable component elements are experimental. Thus, they are subject to change and may not work as intended.\n"})
@Example(value="allow {_item} to be equipped onto entities")
@Since(value={"2.13"})
@RequiredPlugins(value={"Minecraft 1.21.5+"})
public class EffEquipCompInteract
extends Effect
implements EquippableExperimentSyntax {
    private boolean equip;
    private Expression<EquippableWrapper> wrappers;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffEquipCompInteract.class).addPatterns("allow %equippablecomponents% to be equipped on[to] entities", "make %equippablecomponents% equippable on[to] entities", "let %equippablecomponents% be equipped on[to] entities", "(block|prevent|disallow) %equippablecomponents% from being equipped on[to] entities", "make %equippablecomponents% not equippable on[to] entities").supplier(EffEquipCompInteract::new).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.wrappers = exprs[0];
        this.equip = matchedPattern < 3;
        return true;
    }

    @Override
    protected void execute(Event event) {
        this.wrappers.stream(event).forEach(wrapper -> wrapper.editBuilder(builder -> builder.equipOnInteract(this.equip)));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.equip) {
            return "allow " + this.wrappers.toString(event, debug) + " to be equipped onto entities";
        }
        return "prevent " + this.wrappers.toString(event, debug) + " from being equipped onto entities";
    }
}

