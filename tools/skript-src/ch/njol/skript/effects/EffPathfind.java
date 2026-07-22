/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Mob
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Pathfind")
@Description(value={"Make an entity pathfind towards a location or another entity. Not all entities can pathfind. If the pathfinding target is another entity, the entities may or may not continuously follow the target."})
@Example.Examples(value={@Example(value="make all creepers pathfind towards player"), @Example(value="make all cows stop pathfinding"), @Example(value="make event-entity pathfind towards player at speed 1")})
@Since(value={"2.7"})
public class EffPathfind
extends Effect {
    private Expression<LivingEntity> entities;
    @Nullable
    private Expression<Number> speed;
    @Nullable
    private Expression<?> target;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.target = matchedPattern == 0 ? exprs[1] : null;
        this.speed = matchedPattern == 0 ? exprs[2] : null;
        return true;
    }

    @Override
    protected void execute(Event event) {
        Object target = this.target != null ? this.target.getSingle(event) : null;
        double speed = this.speed != null ? this.speed.getOptionalSingle(event).orElse(1).doubleValue() : 1.0;
        for (LivingEntity entity : this.entities.getArray(event)) {
            if (!(entity instanceof Mob)) continue;
            if (target instanceof LivingEntity) {
                ((Mob)entity).getPathfinder().moveTo((LivingEntity)target, speed);
                continue;
            }
            if (target instanceof Location) {
                ((Mob)entity).getPathfinder().moveTo((Location)target, speed);
                continue;
            }
            if (this.target != null) continue;
            ((Mob)entity).getPathfinder().stopPathfinding();
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.target == null) {
            return "make " + this.entities.toString(event, debug) + " stop pathfinding";
        }
        String repr = "make " + this.entities.toString(event, debug) + " pathfind towards " + this.target.toString(event, debug);
        if (this.speed != null) {
            repr = repr + " at speed " + this.speed.toString(event, debug);
        }
        return repr;
    }

    static {
        if (Skript.classExists("org.bukkit.entity.Mob") && Skript.methodExists(Mob.class, "getPathfinder", new Class[0])) {
            Skript.registerEffect(EffPathfind.class, "make %livingentities% (pathfind|move) to[wards] %livingentity/location% [at speed %-number%]", "make %livingentities% stop (pathfinding|moving)");
        }
    }
}

