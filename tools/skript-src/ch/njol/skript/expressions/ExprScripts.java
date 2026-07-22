/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.ExprScript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.experiments.ReflectionExperimentSyntax;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.Objects;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;

@Name(value="All Scripts")
@Description(value={"Returns all of the scripts, or just the enabled or disabled ones."})
@Example(value="command /scripts:\n\ttrigger:\n\t\tsend \"All Scripts: %scripts%\" to player\n\t\tsend \"Loaded Scripts: %enabled scripts%\" to player\n\t\tsend \"Unloaded Scripts: %disabled scripts%\" to player\n")
@Since(value={"2.10"})
public class ExprScripts
extends SimpleExpression<Script>
implements ReflectionExperimentSyntax {
    private int pattern;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.pattern = matchedPattern;
        return true;
    }

    protected Script[] get(Event event) {
        ArrayList<Script> scripts = new ArrayList<Script>();
        if (this.pattern <= 1) {
            scripts.addAll(ScriptLoader.getLoadedScripts());
        }
        if (this.pattern != 1) {
            scripts.addAll(ScriptLoader.getDisabledScripts().stream().map(ExprScript::getHandle).filter(Objects::nonNull).toList());
        }
        return scripts.toArray(new Script[0]);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends Script> getReturnType() {
        return Script.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.pattern == 1) {
            return "all enabled scripts";
        }
        if (this.pattern == 2) {
            return "all disabled scripts";
        }
        return "all scripts";
    }

    static {
        Skript.registerExpression(ExprScripts.class, Script.class, ExpressionType.SIMPLE, "[all [[of] the]|the] scripts", "[all [[of] the]|the] (enabled|loaded) scripts", "[all [[of] the]|the] (disabled|unloaded) scripts");
    }
}

