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
import ch.njol.util.StringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Repeat String")
@Description(value={"Repeats inputted strings a given amount of times."})
@Example(value="broadcast nl and nl repeated 200 times\nbroadcast \"Hello World \" repeated 5 times\nif \"aa\" repeated 2 times is \"aaaa\":\n\tbroadcast \"Ahhhh\" repeated 100 times\n")
@Since(value={"2.8.0"})
public class ExprRepeat
extends SimpleExpression<String> {
    private Expression<String> strings;
    private Expression<Integer> repeatCount;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.strings = exprs[0];
        this.repeatCount = exprs[1];
        return true;
    }

    @Nullable
    protected String[] get(Event event) {
        int repeatCount = this.repeatCount.getOptionalSingle(event).orElse(0);
        if (repeatCount < 1) {
            return new String[0];
        }
        return (String[])this.strings.stream(event).map(string -> StringUtils.multiply(string, repeatCount)).toArray(String[]::new);
    }

    @Override
    public boolean isSingle() {
        return this.strings.isSingle();
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public Expression<? extends String> simplify() {
        if (this.strings instanceof Literal && this.repeatCount instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.strings.toString(event, debug) + " repeated " + this.repeatCount.toString(event, debug) + " times";
    }

    static {
        Skript.registerExpression(ExprRepeat.class, String.class, ExpressionType.COMBINED, "%strings% repeated %integer% time[s]");
    }
}

