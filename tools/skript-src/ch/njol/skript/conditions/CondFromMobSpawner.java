/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Entity;

@Name(value="Is From A Mob Spawner")
@Description(value={"Checks if an entity was spawned from a mob spawner."})
@Example(value="send whether target is from a mob spawner")
@Since(value={"2.10"})
public class CondFromMobSpawner
extends PropertyCondition<Entity> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setNegated(matchedPattern == 1 || matchedPattern == 3);
        this.setExpr(exprs[0]);
        return true;
    }

    @Override
    public boolean check(Entity entity) {
        return entity.fromMobSpawner();
    }

    @Override
    protected String getPropertyName() {
        return "from a mob spawner";
    }

    static {
        if (Skript.methodExists(Entity.class, "fromMobSpawner", new Class[0])) {
            Skript.registerCondition(CondFromMobSpawner.class, "%entities% (is|are) from a [mob] spawner", "%entities% (isn't|aren't|is not|are not) from a [mob] spawner", "%entities% (was|were) spawned (from|by) a [mob] spawner", "%entities% (wasn't|weren't|was not|were not) spawned (from|by) a [mob] spawner");
        }
    }
}

