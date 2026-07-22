/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Interaction
 */
package org.skriptlang.skript.bukkit.entity.interactions.elements.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Is Responsive")
@Description(value={"Checks whether an interaction is responsive or not. Responsiveness determines whether clicking the entity will cause the clicker's arm to swing.\n"})
@Example.Examples(value={@Example(value="if last spawned interaction is responsive:"), @Example(value="if last spawned interaction is unresponsive:")})
@Since(value={"2.14"})
public class CondIsResponsive
extends PropertyCondition<Entity> {
    private boolean responsive;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, CondIsResponsive.infoBuilder(CondIsResponsive.class, PropertyCondition.PropertyType.BE, "(responsive|:unresponsive)", "entities").supplier(CondIsResponsive::new).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.responsive = !parseResult.hasTag("unresponsive");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public boolean check(Entity entity) {
        if (entity instanceof Interaction) {
            Interaction interaction = (Interaction)entity;
            return interaction.isResponsive() == this.responsive;
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return this.responsive ? "responsive" : "unresponsive";
    }
}

