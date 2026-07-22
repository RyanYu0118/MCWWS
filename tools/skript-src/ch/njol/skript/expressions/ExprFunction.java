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
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.function.DynamicFunctionReference;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.lang.function.Namespace;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.experiments.ReflectionExperimentSyntax;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.Objects;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;

@Name(value="Function")
@Description(value={"Obtain a function by name, which can be executed."})
@Example.Examples(value={@Example(value="set {_function} to the function named \"myFunction\""), @Example(value="run {_function} with arguments 13 and true")})
@Since(value={"2.10"})
public class ExprFunction
extends SimpleExpression<DynamicFunctionReference>
implements ReflectionExperimentSyntax {
    private Expression<String> name;
    private Expression<Script> script;
    private int mode;
    private boolean local;
    private Script here;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult result) {
        this.mode = matchedPattern;
        this.local = this.mode == 2 || expressions[1] != null;
        switch (this.mode) {
            case 0: 
            case 1: {
                this.name = expressions[0];
                if (!this.local) break;
                this.script = expressions[1];
                break;
            }
            case 2: {
                this.script = expressions[0];
            }
        }
        this.here = this.getParser().getCurrentScript();
        return true;
    }

    protected DynamicFunctionReference<?>[] get(Event event) {
        @Nullable Script script = this.local ? this.script.getSingle(event) : this.here;
        return switch (this.mode) {
            case 0 -> {
                @Nullable String name = this.name.getSingle(event);
                if (name == null) {
                    yield CollectionUtils.array(new DynamicFunctionReference[0]);
                }
                @Nullable DynamicFunctionReference<?> reference = DynamicFunctionReference.resolveFunction(name, script);
                if (reference == null) {
                    yield CollectionUtils.array(new DynamicFunctionReference[0]);
                }
                yield CollectionUtils.array(reference);
            }
            case 1 -> (DynamicFunctionReference[])this.name.stream(event).map(string -> DynamicFunctionReference.resolveFunction(string, script)).filter(Objects::nonNull).toArray(DynamicFunctionReference[]::new);
            case 2 -> {
                if (script == null) {
                    yield CollectionUtils.array(new DynamicFunctionReference[0]);
                }
                @Nullable Namespace namespace = Functions.getScriptNamespace(script.getConfig().getFileName());
                if (namespace == null) {
                    yield CollectionUtils.array(new DynamicFunctionReference[0]);
                }
                yield (DynamicFunctionReference[])namespace.getFunctions().stream().map(DynamicFunctionReference::new).toArray(DynamicFunctionReference[]::new);
            }
            default -> throw new IllegalStateException("Unexpected value: " + this.mode);
        };
    }

    @Override
    public boolean isSingle() {
        return this.mode != 2 && this.name.isSingle();
    }

    @Override
    public Class<? extends DynamicFunctionReference> getReturnType() {
        return DynamicFunctionReference.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return switch (this.mode) {
            case 0 -> "the function named " + this.name.toString(event, debug) + (String)(this.local ? " from " + this.script.toString(event, debug) : "");
            case 1 -> "functions named " + this.name.toString(event, debug) + (String)(this.local ? " from " + this.script.toString(event, debug) : "");
            case 2 -> "the functions from " + this.script.toString(event, debug);
            default -> throw new IllegalStateException("Unexpected value: " + this.mode);
        };
    }

    static {
        Skript.registerExpression(ExprFunction.class, DynamicFunctionReference.class, ExpressionType.COMBINED, "[the|a] function [named] %string% [(in|from) %-script%]", "[the] functions [named] %strings% [(in|from) %-script%]", "[all [[of] the]|the] functions (in|from) %script%");
    }
}

