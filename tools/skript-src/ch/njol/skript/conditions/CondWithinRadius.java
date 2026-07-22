/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Is Within Radius")
@Description(value={"Checks whether a location is within a certain radius of another location."})
@Example(value="on damage:\n\tif attacker's location is within 10 blocks around {_spawn}:\n\t\tcancel event\n\t\tsend \"You can't PVP in spawn.\"\n")
@Since(value={"2.7"})
public class CondWithinRadius
extends Condition {
    private Expression<Location> locations;
    private Expression<Number> radius;
    private Expression<Location> points;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.locations = exprs[0];
        this.radius = exprs[1];
        this.points = exprs[2];
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        double radius = this.radius.getOptionalSingle(event).orElse(0).doubleValue();
        double radiusSquared = radius * radius * 1.00001;
        return this.locations.check(event, location -> this.points.check(event, center -> {
            if (!location.getWorld().equals((Object)center.getWorld())) {
                return false;
            }
            return location.distanceSquared(center) <= radiusSquared;
        }), this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.locations.toString(event, debug) + (this.locations.isSingle() ? " is " : " are ") + (this.isNegated() ? " not " : "") + "within " + this.radius.toString(event, debug) + " blocks around " + this.points.toString(event, debug);
    }

    static {
        PropertyCondition.register(CondWithinRadius.class, "within %number% (block|metre|meter)[s] (around|of) %locations%", "locations");
    }
}

