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

@Name(value="Vectors - Vector from Location")
@Description(value={"Creates a vector from a location."})
@Example(value="set {_v} to vector of {_loc}")
@Since(value={"2.2-dev28"})
public class ExprVectorOfLocation
extends SimpleExpression<Vector> {
    private Expression<Location> location;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.location = exprs[0];
        return true;
    }

    protected Vector[] get(Event event) {
        Location location = this.location.getSingle(event);
        if (location == null) {
            return null;
        }
        return CollectionUtils.array(location.toVector());
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
        if (this.location instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "vector from " + this.location.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprVectorOfLocation.class, Vector.class, ExpressionType.PROPERTY, "[the] vector (of|from|to) %location%", "%location%'s vector");
    }
}

