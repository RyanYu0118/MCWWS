/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.experiment.ExperimentSet;
import org.skriptlang.skript.lang.script.Script;

@Name(value="Is Using Experimental Feature")
@Description(value={"Checks whether a script is using an experimental feature by name."})
@Example.Examples(value={@Example(value="the script is using \"example feature\""), @Example(value="on load:\n\tif the script is using \"example feature\":\n\t\tbroadcast \"You're using an experimental feature!\"\n")})
@Since(value={"2.9.0"})
public class CondIsUsingFeature
extends Condition {
    private Expression<String> names;
    private Expression<Script> scripts;

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, SkriptParser.ParseResult result) {
        this.names = expressions[1];
        this.scripts = expressions[0];
        this.setNegated(pattern > 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        String[] array = this.names.getArray(event);
        if (array.length == 0) {
            return true;
        }
        boolean isUsing = true;
        for (Script script : this.scripts.getArray(event)) {
            ExperimentSet data = script.getData(ExperimentSet.class);
            if (data == null) {
                isUsing = false;
                continue;
            }
            for (String object : array) {
                isUsing &= data.hasExperiment(object);
            }
        }
        return isUsing ^ this.isNegated();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String whether = this.scripts.isSingle() ? (this.isNegated() ? "isn't" : "is") : (this.isNegated() ? "aren't" : "are");
        return this.scripts.toString(event, debug) + " " + whether + " using " + this.names.toString(event, debug);
    }

    static {
        Skript.registerCondition(CondIsUsingFeature.class, "%script% is using %strings%", "%scripts% are using %strings%", "%script% is(n't| not) using %strings%", "%scripts% are(n't| not) using %strings%");
    }
}

