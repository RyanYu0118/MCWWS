/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions.base;

import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converter;

public abstract class SimplePropertyExpression<F, T>
extends PropertyExpression<F, T>
implements Converter<F, T> {
    protected String rawExpr;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (LiteralUtils.hasUnparsedLiteral(expressions[0])) {
            this.setExpr(LiteralUtils.defendExpression(expressions[0]));
            return LiteralUtils.canInitSafely(this.getExpr());
        }
        this.setExpr(expressions[0]);
        this.rawExpr = parseResult.expr;
        return true;
    }

    @Override
    @Nullable
    public abstract T convert(F var1);

    @Override
    protected T[] get(Event event, F[] source) {
        return super.get(source, this);
    }

    protected abstract String getPropertyName();

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.getPropertyName() + " of " + this.getExpr().toString(event, debug);
    }
}

