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
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Random Numbers")
@Description(value={"A given amount of random numbers or integers between two given numbers. Use 'number' if you want any number with decimal parts, or use use 'integer' if you only want whole numbers.", "Please note that the order of the numbers doesn't matter, i.e. <code>random number between 2 and 1</code> will work as well as <code>random number between 1 and 2</code>."})
@Example.Examples(value={@Example(value="set the player's health to a random number between 5 and 10"), @Example(value="send \"You rolled a %random integer from 1 to 6%!\" to the player"), @Example(value="set {_chances::*} to 5 random integers between 5 and 96"), @Example(value="set {_decimals::*} to 3 random numbers between 2.7 and -1.5")})
@Since(value={"1.4, 2.10 (Multiple random numbers)"})
public class ExprRandomNumber
extends SimpleExpression<Number> {
    @Nullable
    private Expression<Integer> amount;
    private Expression<Number> lower;
    private Expression<Number> upper;
    private boolean isInteger;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.amount = exprs[0];
        this.lower = exprs[1];
        this.upper = exprs[2];
        this.isInteger = parseResult.hasTag("integer");
        return true;
    }

    @Nullable
    protected Number[] get(Event event) {
        Integer amount;
        Number lowerNumber = this.lower.getSingle(event);
        Number upperNumber = this.upper.getSingle(event);
        if (upperNumber == null || lowerNumber == null || !Double.isFinite(lowerNumber.doubleValue()) || !Double.isFinite(upperNumber.doubleValue())) {
            return new Number[0];
        }
        Integer n = amount = this.amount == null ? Integer.valueOf(1) : this.amount.getSingle(event);
        if (amount == null || amount <= 0) {
            return new Number[0];
        }
        double lower = Math.min(lowerNumber.doubleValue(), upperNumber.doubleValue());
        double upper = Math.max(lowerNumber.doubleValue(), upperNumber.doubleValue());
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (this.isInteger) {
            Object[] longs = new Long[amount.intValue()];
            long floored_upper = Math2.floor(upper);
            long ceiled_lower = Math2.ceil(lower);
            if (upper - lower < 1.0 && ceiled_lower - floored_upper <= 1L) {
                if (floored_upper == ceiled_lower || lower == (double)ceiled_lower) {
                    Arrays.fill(longs, (Object)ceiled_lower);
                    return longs;
                }
                if (upper == (double)floored_upper) {
                    Arrays.fill(longs, (Object)floored_upper);
                    return longs;
                }
                return new Long[0];
            }
            for (int i = 0; i < amount; ++i) {
                longs[i] = Math2.ceil(lower) + Math2.mod(((Random)random).nextLong(), floored_upper - ceiled_lower + 1L);
            }
            return longs;
        }
        Number[] doubles = new Double[amount.intValue()];
        for (int i = 0; i < amount; ++i) {
            doubles[i] = Math.min(lower + ((Random)random).nextDouble() * (upper - lower), upper);
        }
        return doubles;
    }

    @Override
    public boolean isSingle() {
        if (this.amount instanceof Literal) {
            return (Integer)((Literal)this.amount).getSingle() == 1;
        }
        return this.amount == null;
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return this.isInteger ? Long.class : Double.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (this.amount == null ? "a" : this.amount.toString(event, debug)) + " random " + (this.isInteger ? "integer" : "number") + (this.amount == null ? "" : "s") + " between " + this.lower.toString(event, debug) + " and " + this.upper.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprRandomNumber.class, Number.class, ExpressionType.COMBINED, "[a|%-integer%] random (:integer|number)[s] (from|between) %number% (to|and) %number%");
    }
}

