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
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Feature;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.experiment.ExperimentData;
import org.skriptlang.skript.lang.experiment.SimpleExperimentalSyntax;

@Name(value="Suppress Type Hints (Experimental)")
@Description(value={"An effect to suppress local variable type hint errors for the syntax lines that follow this effect.", "NOTE: Suppressing type hints also prevents syntax from providing new type hints. For example, with type hints suppressed, 'set {_x} to true' would not provide 'boolean' as a type hint for '{_x}'"})
@Example(value="\tstart suppressing local variable type hints\n\t# potentially unsafe code goes here\n\tstop suppressing local variable type hints\n")
@Since(value={"2.12"})
public class EffSuppressTypeHints
extends Effect
implements SimpleExperimentalSyntax {
    private static final ExperimentData EXPERIMENT_DATA = ExperimentData.createSingularData(Feature.TYPE_HINTS);
    private boolean stop;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.stop = parseResult.hasTag("stop");
        this.getParser().getHintManager().setActive(this.stop);
        return true;
    }

    @Override
    protected void execute(Event event) {
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (this.stop ? "stop" : "start") + " suppressing type hints";
    }

    @Override
    public ExperimentData getExperimentData() {
        return EXPERIMENT_DATA;
    }

    static {
        Skript.registerEffect(EffSuppressTypeHints.class, "[stop:un]suppress [local variable] type hints", "(start|:stop) suppressing [local variable] type hints");
    }
}

