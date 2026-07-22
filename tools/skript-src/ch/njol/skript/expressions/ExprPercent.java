/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
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
import org.jetbrains.annotations.Nullable;

@Name(value="Percent of")
@Description(value={"Returns a percentage of one or more numbers."})
@Example.Examples(value={@Example(value="set damage to 10% of victim's health"), @Example(value="set damage to 125 percent of damage"), @Example(value="set {_result} to {_percent} percent of 999"), @Example(value="set {_result::*} to 10% of {_numbers::*}"), @Example(value="set experience to 50% of player's total experience")})
@Since(value={"2.8.0"})
public class ExprPercent
extends SimpleExpression<Number> {
    private Expression<Number> percent;
    private Expression<Number> numbers;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.percent = exprs[0];
        this.numbers = exprs[1];
        return true;
    }

    @Nullable
    protected Number[] get(Event event) {
        Number percent = this.percent.getSingle(event);
        Number[] numbers = this.numbers.getArray(event);
        if (percent == null || numbers.length == 0) {
            return null;
        }
        Number[] results = new Number[numbers.length];
        for (int i = 0; i < numbers.length; ++i) {
            results[i] = numbers[i].doubleValue() * percent.doubleValue() / 100.0;
        }
        return results;
    }

    @Override
    public boolean isSingle() {
        return this.numbers.isSingle();
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public Expression<? extends Number> simplify() {
        if (this.percent instanceof Literal && this.numbers instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.percent.toString(event, debug) + " percent of " + this.numbers.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprPercent.class, Number.class, ExpressionType.COMBINED, "%number%(\\%| percent) of %numbers%");
    }
}

