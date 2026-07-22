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

@Name(value="Minimum Double Value")
@Description(value={"A number representing the minimum value of a double number type."})
@Example(value="if {_number} <= minimum double value:")
@Since(value={"2.13"})
public class LitDoubleMinValue
extends SimpleLiteral<Double> {
    public LitDoubleMinValue() {
        super(Double.MIN_VALUE, false);
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "min double value";
    }

    static {
        Skript.registerExpression(LitDoubleMinValue.class, Double.class, ExpressionType.SIMPLE, "[the] min[imum] double value");
    }
}

