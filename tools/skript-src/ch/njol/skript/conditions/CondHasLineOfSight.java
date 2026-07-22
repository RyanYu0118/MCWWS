/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Has Line of Sight")
@Description(value={"Checks whether living entities have an unobstructed line of sight to other entities or locations."})
@Example.Examples(value={@Example(value="player has direct line of sight to location 5 blocks to the right of player"), @Example(value="victim has line of sight to attacker"), @Example(value="player has no line of sight to location 100 blocks in front of player")})
@Since(value={"2.8.0"})
public class CondHasLineOfSight
extends Condition {
    private Expression<LivingEntity> viewers;
    private Expression<?> targets;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.viewers = exprs[0];
        this.targets = exprs[1];
        this.setNegated(matchedPattern > 0);
        return true;
    }

    @Override
    public boolean check(Event event) {
        return this.targets.check(event, target -> {
            if (target instanceof Entity) {
                return this.viewers.check(event, viewer -> viewer.hasLineOfSight((Entity)target));
            }
            if (target instanceof Location) {
                return this.viewers.check(event, viewer -> viewer.hasLineOfSight((Location)target));
            }
            return false;
        }, this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.viewers.toString(event, debug) + " has" + (this.isNegated() ? " no" : "") + " line of sight to " + this.targets.toString(event, debug);
    }

    static {
        Skript.registerCondition(CondHasLineOfSight.class, "%livingentities% (has|have) [a] [direct] line of sight to %entities/locations%", "%livingentities% does(n't| not) have [a] [direct] line of sight to %entities/locations%", "%livingentities% (has|have) no [direct] line of sight to %entities/locations%");
    }
}

