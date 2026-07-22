/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Name(value="Vectors - Vector Between Locations")
@Description(value={"Creates a vector between two locations."})
@Example(value="set {_v} to vector between {_loc1} and {_loc2}")
@Since(value={"2.2-dev28"})
public class ExprVectorBetweenLocations
extends SimpleExpression<Vector> {
    private Expression<Location> from;
    private Expression<Location> to;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.from = exprs[0];
        this.to = exprs[1];
        return true;
    }

    protected Vector[] get(Event event) {
        Location from = this.from.getSingle(event);
        Location to = this.to.getSingle(event);
        if (from == null || to == null) {
            return null;
        }
        return CollectionUtils.array(new Vector(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ()));
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Vector> getReturnType() {
        return Vector.class;
    }

    @Override
    public Expression<? extends Vector> simplify() {
        if (this.from instanceof Literal && this.to instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "vector from " + this.from.toString(event, debug) + " to " + this.to.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprVectorBetweenLocations.class, Vector.class, ExpressionType.COMBINED, "[the] vector (from|between) %location% (to|and) %location%");
    }
}

