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

@Name(value="Vectors - Normalized")
@Description(value={"Returns the same vector but with length 1."})
@Example(value="set {_v} to normalized {_v}")
@Since(value={"2.2-dev28"})
public class ExprVectorNormalize
extends SimpleExpression<Vector> {
    private Expression<Vector> vector;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.vector = exprs[0];
        return true;
    }

    protected Vector[] get(Event event) {
        Vector vector = this.vector.getSingle(event);
        if (vector == null) {
            return null;
        }
        if (!(vector = vector.clone()).isZero() && !vector.isNormalized()) {
            vector.normalize();
        }
        return CollectionUtils.array(vector);
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
        if (this.vector instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "normalized " + this.vector.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprVectorNormalize.class, Vector.class, ExpressionType.COMBINED, "normalize[d] %vector%", "%vector% normalized");
    }
}

