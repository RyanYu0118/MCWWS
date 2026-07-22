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

@Name(value="Pi")
@Description(value={"Returns the mathematical constant pi. (approx. 3.1415926535)"})
@Example(value="set {_tau} to pi * 2")
@Since(value={"2.7"})
public class LitPi
extends SimpleLiteral<Double> {
    public LitPi() {
        super(Math.PI, false);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "pi";
    }

    static {
        Skript.registerExpression(LitPi.class, Double.class, ExpressionType.SIMPLE, "(pi|\u03c0)");
    }
}

