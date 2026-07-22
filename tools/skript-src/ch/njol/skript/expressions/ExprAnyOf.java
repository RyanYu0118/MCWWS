/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.WrapperExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Any Of")
@Description(value={"Returns an 'or list' composed of the given objects. For example, `any of (1, 2, and 3)` is equivalent to `1, 2, or 3`", "Useful when doing comparisons with variable lists."})
@Example.Examples(value={@Example(value="if any of {_numbers::*} are 1:"), @Example(value="if any of {teamA::*} are within location(0, 0, 0) and location(10, 10, 10):")})
@Since(value={"2.11"})
public class ExprAnyOf
extends WrapperExpression<Object> {
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Expression expr = LiteralUtils.defendExpression(expressions[0]);
        this.setExpr(expr);
        return LiteralUtils.canInitSafely(expr);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return null;
    }

    @Override
    public boolean getAnd() {
        return false;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "any of " + this.getExpr().toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprAnyOf.class, Object.class, ExpressionType.PATTERN_MATCHES_EVERYTHING, "(any [one]|one) of [the] %objects%");
    }
}

