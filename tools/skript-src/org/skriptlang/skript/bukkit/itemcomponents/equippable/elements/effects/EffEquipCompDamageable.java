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
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableExperimentSyntax;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableWrapper;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Equippable Component - Lose Durability")
@Description(value={"Whether the item should take damage when the wearer gets injured.\nNOTE: Equippable component elements are experimental. Thus, they are subject to change and may not work as intended.\n"})
@Example.Examples(value={@Example(value="make {_item} lose durability when hurt"), @Example(value="set {_component} to the equippable component of {_item}\nif {_component} will lose durability when injured:\n\tmake {_component} lose durability on injury\n")})
@RequiredPlugins(value={"Minecraft 1.21.2+"})
@Since(value={"2.13"})
public class EffEquipCompDamageable
extends Effect
implements EquippableExperimentSyntax {
    private Expression<EquippableWrapper> wrappers;
    private boolean loseDurability;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffEquipCompDamageable.class).addPatterns("(make|let) %equippablecomponents% (lose durability|be damaged) (on [wearer['s]] injury|when [[the] wearer [is]] (hurt|injured|damaged))", "(allow|force) %equippablecomponents% to (lose durability|be damaged) (on [wearer['s]] injury|when [[the] wearer [is]] (hurt|injured|damaged))", "make %equippablecomponents% not (lose durability|be damaged) (on [wearer['s]] injury|when [[the] wearer [is]] (hurt|injured|damaged))", "(disallow|prevent) %equippablecomponents% from (lose durability|being damaged) (on [wearer['s]] injury|when [[the] wearer [is]] (hurt|injured|damaged))").supplier(EffEquipCompDamageable::new).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.wrappers = exprs[0];
        this.loseDurability = matchedPattern <= 1;
        return true;
    }

    @Override
    protected void execute(Event event) {
        this.wrappers.stream(event).forEach(wrapper -> wrapper.editBuilder(builder -> builder.damageOnHurt(this.loseDurability)));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append("make", this.wrappers);
        if (this.loseDurability) {
            builder.append((Object)"not");
        }
        builder.append((Object)"lose durability when injured");
        return builder.toString();
    }
}

