/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.entity.Pathfinder
 *  com.destroystokyo.paper.entity.Pathfinder$PathResult
 *  org.bukkit.Location
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Mob
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.destroystokyo.paper.entity.Pathfinder;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Is Pathfinding")
@Description(value={"Checks whether living entities are pathfinding.", "Can only be a living entity that is a Mob."})
@Example(value="make {_entity} pathfind to {_location} at speed 2\nwhile {_entity} is pathfinding\n\twait a second\nlaunch flickering trailing burst firework colored red at location of {_entity}\nsubtract 10 from {defence::tower::health}\nclear entity within {_entity}\n")
@Since(value={"2.9.0"})
public class CondIsPathfinding
extends Condition {
    private Expression<LivingEntity> entities;
    private Expression<?> target;
    static final /* synthetic */ boolean $assertionsDisabled;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = expressions[0];
        this.target = expressions[1];
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        return this.entities.check(event, entity -> {
            if (!(entity instanceof Mob)) {
                return false;
            }
            Pathfinder pathfind = ((Mob)entity).getPathfinder();
            if (this.target == null) {
                return pathfind.hasPath();
            }
            Pathfinder.PathResult current = pathfind.getCurrentPath();
            Object target = this.target.getSingle(event);
            if (target == null || current == null) {
                return false;
            }
            Location location = current.getFinalPoint();
            if (target instanceof Location) {
                return location.equals(target);
            }
            if (!$assertionsDisabled && !(target instanceof LivingEntity)) {
                throw new AssertionError();
            }
            LivingEntity entityTarget = (LivingEntity)target;
            return location.distance(((Mob)entityTarget).getLocation()) < 1.0;
        }, this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.BE, event, debug, this.entities, (String)("pathfinding" + String.valueOf(this.target) == null ? "" : " to " + this.target.toString(event, debug)));
    }

    static {
        boolean bl = $assertionsDisabled = !CondIsPathfinding.class.desiredAssertionStatus();
        if (Skript.classExists("org.bukkit.entity.Mob") && Skript.methodExists(Mob.class, "getPathfinder", new Class[0])) {
            PropertyCondition.register(CondIsPathfinding.class, "pathfinding [to[wards] %-livingentity/location%]", "livingentities");
        }
    }
}

