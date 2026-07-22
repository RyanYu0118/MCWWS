/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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
import ch.njol.skript.util.Date;
import org.jetbrains.annotations.Nullable;

@Name(value="Unix Timestamp")
@Description(value={"Converts given date to Unix timestamp. This is roughly how many seconds have elapsed since 1 January 1970."})
@Example(value="unix timestamp of now")
@Since(value={"2.2-dev31"})
public class ExprUnixTicks
extends SimplePropertyExpression<Date, Number> {
    @Override
    @Nullable
    public Number convert(Date f) {
        return (double)f.getTime() / 1000.0;
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
        return "unix timestamp";
    }

    static {
        ExprUnixTicks.register(ExprUnixTicks.class, Number.class, "unix timestamp", "dates");
    }
}

