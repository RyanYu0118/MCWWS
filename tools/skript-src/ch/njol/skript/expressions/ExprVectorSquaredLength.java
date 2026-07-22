/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.util.Vector
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
import org.bukkit.util.Vector;

@Name(value="Vectors - Squared Length")
@Description(value={"Gets the squared length of a vector."})
@Example(value="send \"%squared length of vector 1, 2, 3%\"")
@Since(value={"2.2-dev28"})
public class ExprVectorSquaredLength
extends SimplePropertyExpression<Vector, Number> {
    @Override
    public Number convert(Vector vector) {
        return vector.lengthSquared();
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

    @Override
    protected String getPropertyName() {
        return "squared length of vector";
    }

    static {
        ExprVectorSquaredLength.register(ExprVectorSquaredLength.class, Number.class, "squared length[s]", "vectors");
    }
}

