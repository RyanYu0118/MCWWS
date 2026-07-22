/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.util.Kleenean;
import java.util.Map;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NoDoc
public class ExprKeyValueSet
extends SimpleExpression<Object>
implements KeyProviderExpression<Object> {
    private static final Map<String, String> testSet;
    private Variable<?> variable;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0) {
            Variable variable;
            Expression<?> expression = expressions[0];
            if (!(expression instanceof Variable) || !(variable = (Variable)expression).isList()) {
                Skript.error("The expression '" + String.valueOf(expression) + "' is not a list variable.");
                return false;
            }
            this.variable = variable;
        }
        return true;
    }

    @Override
    @NotNull
    public @NotNull String @NotNull [] getArrayKeys(Event event) throws IllegalStateException {
        if (this.variable == null) {
            return testSet.keySet().toArray(new String[0]);
        }
        return this.variable.getArrayKeys(event);
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        if (this.variable == null) {
            return testSet.values().toArray();
        }
        return this.variable.getArray(event);
    }

    @Override
    public Class<?> getReturnType() {
        return this.variable == null ? String.class : this.variable.getReturnType();
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        if (this.variable == null) {
            return super.possibleReturnTypes();
        }
        return this.variable.possibleReturnTypes();
    }

    @Override
    public boolean canReturn(Class<?> returnType) {
        if (this.variable == null) {
            return super.canReturn(returnType);
        }
        return this.variable.canReturn(returnType);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.variable != null) {
            return "test key values of " + this.variable.toString(event, debug);
        }
        return "test key values";
    }

    static {
        if (TestMode.ENABLED) {
            Skript.registerExpression(ExprKeyValueSet.class, Object.class, ExpressionType.SIMPLE, "test key values of %~objects%", "test key values");
        }
        testSet = Map.of("hello", "there", "foo", "bar", "a", "b");
    }
}

