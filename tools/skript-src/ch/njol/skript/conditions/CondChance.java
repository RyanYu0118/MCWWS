/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SimplifiedCondition;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Chance")
@Description(value={"A condition that randomly succeeds or fails.\nValid values are between 0% and 100%, or if the percent sign is omitted, between 0 and 1.\n"})
@Example.Examples(value={@Example(value="chance of 50%:\n\tdrop a diamond at location(100, 100, 100, \"world')\n"), @Example(value="chance of {chance}% # {chance} between 0 and 100"), @Example(value="chance of {chance} # {chance} between 0 and 1"), @Example(value="if chance of 99% fails:\n\tbroadcast \"Haha loser! *points and laughs*\"\n")})
@Since(value={"1.0, 2.14 (chance fails)"})
public class CondChance
extends Condition {
    private Expression<Number> chance;
    private boolean percent;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.chance = exprs[0];
        this.percent = parseResult.mark == 1;
        this.setNegated(parseResult.hasTag("fail"));
        return true;
    }

    @Override
    public boolean check(Event event) {
        Number chance = this.chance.getSingle(event);
        if (chance == null) {
            return false;
        }
        boolean result = Math.random() < (this.percent ? chance.doubleValue() / 100.0 : chance.doubleValue());
        return result == !this.isNegated();
    }

    @Override
    public Condition simplify() {
        Expression<Number> expression = this.chance;
        if (expression instanceof Literal) {
            double maxValue;
            Literal litChance = (Literal)expression;
            Number chance = (Number)litChance.getSingle();
            double d = maxValue = this.percent ? 100.0 : 1.0;
            if (chance.doubleValue() >= maxValue || chance.doubleValue() <= 0.0) {
                return SimplifiedCondition.fromCondition(this);
            }
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String baseString = "chance of " + this.chance.toString(event, debug) + (this.percent ? "%" : "");
        if (this.isNegated()) {
            baseString = baseString + " failed";
        }
        return baseString;
    }

    static {
        Skript.registerCondition(CondChance.class, "chance of %number%(1:\\%|) [fail:(fails|failed)]");
    }
}

