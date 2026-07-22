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

@Name(value="Maximum Long Value")
@Description(value={"A number representing the maximum value of a long number type."})
@Example(value="if {_number} >= maximum long value:")
@Since(value={"2.13"})
public class LitLongMaxValue
extends SimpleLiteral<Long> {
    public LitLongMaxValue() {
        super(Long.MAX_VALUE, false);
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "max long value";
    }

    static {
        Skript.registerExpression(LitLongMaxValue.class, Long.class, ExpressionType.SIMPLE, "[the] max[imum] long value");
    }
}

