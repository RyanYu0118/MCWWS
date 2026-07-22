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

@Name(value="Infinity")
@Description(value={"A number representing positive infinity."})
@Example(value="if {_number} is infinity:")
@Since(value={"2.2-dev32d"})
public class LitInfinity
extends SimpleLiteral<Double> {
    public LitInfinity() {
        super(Double.POSITIVE_INFINITY, false);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "infinity";
    }

    static {
        Skript.registerExpression(LitInfinity.class, Double.class, ExpressionType.SIMPLE, "positive (infinity|\u221e) [value]", "\u221e [value]", "infinity value", "value of [positive] (infinity|\u221e)");
    }
}

