/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.function.DynamicFunctionReference;
import ch.njol.skript.registrations.experiments.ReflectionExperimentSyntax;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.util.Executable;

@Name(value="Run")
@Description(value={"Executes a task (a function). Any returned result is discarded."})
@Example(value="set {_function} to the function named \"myFunction\"\nrun {_function}\nrun {_function} with arguments {_things::*}\n")
@Since(value={"2.10"})
@Keywords(value={"run", "execute", "reflection", "function"})
public class EffRun
extends Effect
implements ReflectionExperimentSyntax {
    private Expression<Executable> executable;
    private Expression<?> arguments;
    private DynamicFunctionReference.Input input;
    private boolean hasArguments;

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean isDelayed, SkriptParser.ParseResult result) {
        this.executable = expressions[0];
        this.hasArguments = result.hasTag("arguments");
        if (this.hasArguments) {
            this.arguments = LiteralUtils.defendExpression(expressions[1]);
            Expression<T>[] arguments = this.arguments instanceof ExpressionList ? ((ExpressionList)this.arguments).getExpressions() : new Expression[]{this.arguments};
            this.input = new DynamicFunctionReference.Input(arguments);
            return LiteralUtils.canInitSafely(this.arguments);
        }
        this.input = new DynamicFunctionReference.Input(new Expression[0]);
        return true;
    }

    @Override
    protected void execute(Event event) {
        Object[] arguments;
        Executable task = this.executable.getSingle(event);
        if (task == null) {
            return;
        }
        if (task instanceof DynamicFunctionReference) {
            DynamicFunctionReference reference = (DynamicFunctionReference)task;
            Expression<?> validated = reference.validate(this.input);
            if (validated == null) {
                return;
            }
            arguments = validated.getArray(event);
        } else {
            arguments = this.hasArguments ? this.arguments.getArray(event) : new Object[]{};
        }
        task.execute(event, arguments);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.hasArguments) {
            return "run " + this.executable.toString(event, debug) + " with arguments " + this.arguments.toString(event, debug);
        }
        return "run " + this.executable.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffRun.class, "run %executable% [arguments:with arg[ument]s %-objects%]", "execute %executable% [arguments:with arg[ument]s %-objects%]");
    }
}

