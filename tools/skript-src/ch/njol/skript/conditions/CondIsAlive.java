/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Entity;

@Name(value="Is Alive")
@Description(value={"Checks whether an entity is alive. Works for non-living entities too."})
@Example.Examples(value={@Example(value="if {villager-buddy::%player's uuid%} is not dead:"), @Example(value="on shoot:\n\twhile the projectile is alive:\n")})
@Since(value={"2.0, 2.4-alpha4 (non-living entity support)"})
public class CondIsAlive
extends PropertyCondition<Entity> {
    private boolean isNegated;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.isNegated = parseResult.mark == 1;
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public boolean check(Entity e) {
        return this.isNegated == e.isDead();
    }

    @Override
    protected String getPropertyName() {
        return this.isNegated ? "dead" : "alive";
    }

    static {
        CondIsAlive.register(CondIsAlive.class, "(alive|1\u00a6dead)", "entities");
    }
}

