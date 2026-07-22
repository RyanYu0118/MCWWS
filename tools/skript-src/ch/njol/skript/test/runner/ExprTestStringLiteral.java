/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.LiteralString;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Test String Literal")
@Description(value={"Accepts only a string literal. Used for testing correct parsing & literal treatment. Returns the value."})
@NoDoc
public class ExprTestStringLiteral
extends SimpleExpression<String> {
    private Expression<String> literal;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.literal = expressions[0];
        return this.literal instanceof LiteralString;
    }

    @Nullable
    protected String[] get(Event event) {
        return this.literal.getArray(event);
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "test string literal " + this.literal.toString(event, debug);
    }

    static {
        if (TestMode.ENABLED) {
            Skript.registerExpression(ExprTestStringLiteral.class, String.class, ExpressionType.SIMPLE, "test string literal %*string%");
        }
    }
}

