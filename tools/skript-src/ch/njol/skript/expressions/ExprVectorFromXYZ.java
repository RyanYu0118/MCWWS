/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Name(value="Vectors - Create from XYZ")
@Description(value={"Creates a vector from x, y and z values."})
@Example(value="set {_v} to vector 0, 1, 0")
@Since(value={"2.2-dev28"})
public class ExprVectorFromXYZ
extends SimpleExpression<Vector> {
    private Expression<Number> x;
    private Expression<Number> y;
    private Expression<Number> z;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.x = exprs[0];
        this.y = exprs[1];
        this.z = exprs[2];
        return true;
    }

    protected Vector[] get(Event event) {
        Number x = this.x.getSingle(event);
        Number y = this.y.getSingle(event);
        Number z = this.z.getSingle(event);
        if (x == null || y == null || z == null) {
            return null;
        }
        return CollectionUtils.array(new Vector(x.doubleValue(), y.doubleValue(), z.doubleValue()));
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
        if (this.x instanceof Literal && this.y instanceof Literal && this.z instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "vector from x " + this.x.toString(event, debug) + ", y " + this.y.toString(event, debug) + ", z " + this.z.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprVectorFromXYZ.class, Vector.class, ExpressionType.COMBINED, "[a] [new] vector [(from|at|to)] %number%,[ ]%number%(,[ ]| and )%number%");
    }
}

