/*
 * Decompiled with CFR 0.152.
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

@Name(value="Length")
@Description(value={"The length of a text, in number of characters."})
@Example(value="set {_l} to length of the string argument")
@Since(value={"2.1"})
public class ExprLength
extends SimplePropertyExpression<String, Long> {
    @Override
    public Long convert(String s) {
        return s.length();
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    public Expression<? extends Long> simplify() {
        if (this.getExpr() instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    protected String getPropertyName() {
        return "length";
    }

    static {
        ExprLength.register(ExprLength.class, Long.class, "length", "strings");
    }
}

