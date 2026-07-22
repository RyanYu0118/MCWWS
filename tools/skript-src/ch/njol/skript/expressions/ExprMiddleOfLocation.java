/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

@Name(value="Middle of Location")
@Description(value={"Returns the middle/center of a location. In other words, returns the middle of the X, Z coordinates and the floor value of the Y coordinate of a location."})
@Example(value="command /stuck:\n\texecutable by: players\n\ttrigger:\n\t\tteleport player to the center of player's location\n\t\tsend \"You're no longer stuck.\"\n")
@Since(value={"2.6.1"})
public class ExprMiddleOfLocation
extends SimplePropertyExpression<Location, Location> {
    @Override
    @Nullable
    public Location convert(Location loc) {
        return new Location(loc.getWorld(), (double)loc.getBlockX() + 0.5, (double)loc.getBlockY(), (double)loc.getBlockZ() + 0.5);
    }

    @Override
    public Class<? extends Location> getReturnType() {
        return Location.class;
    }

    @Override
    public Expression<? extends Location> simplify() {
        if (this.getExpr() instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    protected String getPropertyName() {
        return "middle point";
    }

    static {
        ExprMiddleOfLocation.register(ExprMiddleOfLocation.class, Location.class, "(middle|center) [point]", "location");
    }
}

