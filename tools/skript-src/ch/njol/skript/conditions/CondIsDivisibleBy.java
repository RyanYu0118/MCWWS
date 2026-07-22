/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

@Name(value="Is Evenly Divisible By")
@Description(value={"Checks if a number is evenly divisible by another number.\nAn optional tolerance can be provided to counteract floating point error. The default tolerance is 1e-10.\nAny input smaller than the tolerance is considered to be 0.\nThis means divisors that are too small will always return false, and dividends that are too small will always return true.\n"})
@Example.Examples(value={@Example(value="if 5 is evenly divisible by 5:"), @Example(value="if 11 cannot be evenly divided by 10:"), @Example(value="if 0.3 can be evenly divided by 0.1 with a tolerance of 0.0000001:")})
@Since(value={"2.10, 2.12 (tolerance)"})
public class CondIsDivisibleBy
extends Condition
implements SyntaxRuntimeErrorProducer {
    private Expression<Number> dividend;
    private Expression<Number> divisor;
    @Nullable
    private Expression<Number> epsilon;
    private Node node;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.dividend = exprs[0];
        this.divisor = exprs[1];
        this.epsilon = exprs[2];
        this.setNegated(matchedPattern == 1 || matchedPattern == 3);
        this.node = this.getParser().getNode();
        return true;
    }

    @Override
    public boolean check(Event event) {
        double epsilon;
        Double epsilonNumber;
        Number divisorNumber = this.divisor.getSingle(event);
        if (divisorNumber == null) {
            return this.isNegated();
        }
        double divisor = divisorNumber.doubleValue();
        if (divisor == 0.0) {
            return this.isNegated();
        }
        Number number = epsilonNumber = this.epsilon != null ? (Number)this.epsilon.getSingle(event) : (Number)1.0E-10;
        if (epsilonNumber == null) {
            epsilonNumber = 1.0E-10;
        }
        if ((epsilon = ((Number)epsilonNumber).doubleValue()) <= 0.0 || Double.isNaN(epsilon)) {
            this.error("Tolerance must be a positive, non-zero number, but was " + String.valueOf(epsilonNumber) + ".");
            return this.isNegated();
        }
        if (divisor < epsilon) {
            return this.isNegated();
        }
        return this.dividend.check(event, dividendNumber -> {
            double remainder = Math.abs(dividendNumber.doubleValue() % divisor);
            return remainder <= epsilon || remainder >= divisor - epsilon;
        }, this.isNegated());
    }

    @Override
    public Node getNode() {
        return this.node;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.dividend.toString(event, debug) + " is " + (this.isNegated() ? "not " : "") + "evenly divisible by " + this.divisor.toString(event, debug);
    }

    static {
        Skript.registerCondition(CondIsDivisibleBy.class, "%numbers% (is|are) evenly divisible by %number% [with [a] tolerance [of] %-number%]", "%numbers% (isn't|is not|aren't|are not) evenly divisible by %number% [with [a] tolerance [of] %-number%]", "%numbers% can be evenly divided by %number% [with [a] tolerance [of] %-number%]", "%numbers% (can't|can[ ]not) be evenly divided by %number% [with [a] tolerance [of] %-number%]");
    }
}

