/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.literals;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="NaN")
@Description(value={"A number representing an undefined value. NaN occurs as a result of illegal math, like dividing 0 by 0.", "NaN is deliberately not equal to any other number, including itself."})
@Example.Examples(value={@Example(value="if {_number} is not {_number}:"), @Example(value="if isNaN({_number}) is true:")})
@Since(value={"2.2-dev32d"})
public class LitNaN
extends SimpleLiteral<Double> {
    public LitNaN() {
        super(Double.NaN, false);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "NaN";
    }

    static {
        Skript.registerExpression(LitNaN.class, Double.class, ExpressionType.SIMPLE, "NaN [value]", "value of NaN");
    }
}

