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

@Name(value="Equippable Component - Shear Off")
@Description(value={"Whether the item can be sheared off of entities.\nNOTE: Equippable component elements are experimental. Thus, they are subject to change and may not work as intended.\n"})
@Example.Examples(value={@Example(value="allow {_item} to be sheared off"), @Example(value="set {_component} to the equippable component of {_item}\nif {_component} can be sheared off of entities:\n\tprevent {_component} from being sheared off of entities\n")})
@RequiredPlugins(value={"Minecraft 1.21.6+"})
@Since(value={"2.13"})
public class EffEquipCompShearable
extends Effect
implements EquippableExperimentSyntax {
    private Expression<EquippableWrapper> wrappers;
    private boolean shearable;

    public static void register(SyntaxRegistry registry) {
        if (!EquippableWrapper.HAS_CAN_BE_SHEARED) {
            return;
        }
        registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffEquipCompShearable.class).addPatterns("allow %equippablecomponents% to be sheared off [of entities]", "(disallow|prevent) %equippablecomponents% from being sheared off [of entities]").supplier(EffEquipCompShearable::new).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.wrappers = exprs[0];
        this.shearable = matchedPattern == 0;
        return true;
    }

    @Override
    protected void execute(Event event) {
        this.wrappers.stream(event).forEach(wrapper -> wrapper.editBuilder(builder -> builder.canBeSheared(this.shearable)));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        if (this.shearable) {
            builder.append("allow", this.wrappers, "to be");
        } else {
            builder.append("prevent", this.wrappers, "from being");
        }
        builder.append((Object)"sheared off of entities");
        return builder.toString();
    }
}

