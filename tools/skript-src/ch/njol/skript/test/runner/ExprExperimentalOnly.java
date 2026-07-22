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
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.test.runner.TestFeatures;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Experimental Only")
@Description(value={"A do-nothing syntax that only parses when `example feature` is enabled."})
@NoDoc
public class ExprExperimentalOnly
extends SimpleExpression<Boolean> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return this.getParser().hasExperiment(TestFeatures.EXAMPLE_FEATURE);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "experimental only";
    }

    @Nullable
    protected Boolean[] get(Event event) {
        return new Boolean[]{true};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Boolean> getReturnType() {
        return Boolean.class;
    }

    static {
        if (TestMode.ENABLED) {
            Skript.registerExpression(ExprExperimentalOnly.class, Boolean.class, ExpressionType.SIMPLE, "experimental only");
        }
    }
}

