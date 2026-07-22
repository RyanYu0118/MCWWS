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

@Name(value="Negative Infinity")
@Description(value={"A number representing negative infinity."})
@Example(value="if {_number} is -infinity:")
@Since(value={"2.2-dev32d"})
public class LitNegativeInfinity
extends SimpleLiteral<Double> {
    public LitNegativeInfinity() {
        super(Double.NEGATIVE_INFINITY, false);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "negative infinity";
    }

    static {
        Skript.registerExpression(LitNegativeInfinity.class, Double.class, ExpressionType.SIMPLE, "(-|minus |negative )(infinity|\u221e) [value]", "value of (-|minus |negative )(infinity|\u221e)");
    }
}

