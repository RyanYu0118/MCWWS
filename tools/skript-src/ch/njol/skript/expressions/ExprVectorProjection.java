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
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Name(value="Vectors - Vector Projection")
@Description(value={"An expression to get the vector projection of two vectors."})
@Example(value="set {_projection} to vector projection of vector(1, 2, 3) onto vector(4, 4, 4)")
@Since(value={"2.8.0"})
public class ExprVectorProjection
extends SimpleExpression<Vector> {
    private Expression<Vector> left;
    private Expression<Vector> right;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.left = exprs[0];
        this.right = exprs[1];
        return true;
    }

    @Nullable
    protected Vector[] get(Event event) {
        Vector left = this.left.getOptionalSingle(event).orElse(new Vector());
        Vector right = this.right.getOptionalSingle(event).orElse(new Vector());
        double dot = left.dot(right);
        double length = right.lengthSquared();
        double scalar = dot / length;
        return new Vector[]{right.clone().multiply(scalar)};
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
        if (this.left instanceof Literal && this.right instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "vector projection of " + this.left.toString(event, debug) + " onto " + this.right.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprVectorProjection.class, Vector.class, ExpressionType.COMBINED, "[vector] projection [of] %vector% on[to] %vector%");
    }
}

