/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.UnknownNullability;

@Name(value="Whether")
@Description(value={"A shorthand for returning the result of a condition (true or false). This is functionally identical to using `true if <condition> else false`."})
@Example.Examples(value={@Example(value="set {fly} to whether player can fly"), @Example(value="broadcast \"Flying: %whether player is flying%\"")})
@Since(value={"2.9.0"})
public class ExprWhether
extends SimpleExpression<Boolean> {
    private @UnknownNullability Condition condition;

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, SkriptParser.ParseResult result) {
        String input = result.regexes.get(0).group();
        this.condition = Condition.parse(input, "Can't understand this condition: " + input);
        return this.condition != null;
    }

    protected Boolean[] get(Event event) {
        return new Boolean[]{this.condition.check(event)};
    }

    @Override
    public Class<? extends Boolean> getReturnType() {
        return Boolean.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "whether " + this.condition.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprWhether.class, Boolean.class, ExpressionType.PATTERN_MATCHES_EVERYTHING, "whether <.+>");
    }
}

