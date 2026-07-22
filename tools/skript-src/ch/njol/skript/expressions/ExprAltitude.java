/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
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

@Name(value="Altitude")
@Description(value={"Effectively an alias of 'y-<a href='#ExprCoordinate'>coordinate</a> of \u2026', it represents the height of some location within the world."})
@Example(value="on damage:\n\taltitude of the attacker is higher than the altitude of the victim\n\tset damage to damage * 1.2\n")
@Since(value={"1.4.3"})
public class ExprAltitude
extends SimplePropertyExpression<Location, Number> {
    @Override
    public Number convert(Location l) {
        return l.getY();
    }

    @Override
    protected String getPropertyName() {
        return "altitude";
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public Expression<? extends Number> simplify() {
        if (this.getExpr() instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    static {
        ExprAltitude.register(ExprAltitude.class, Number.class, "altitude[s]", "locations");
    }
}

