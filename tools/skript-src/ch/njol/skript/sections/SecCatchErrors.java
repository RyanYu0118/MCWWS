/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.ExprCaughtErrors;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.registrations.Feature;
import ch.njol.util.Kleenean;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.experiment.ExperimentSet;
import org.skriptlang.skript.lang.experiment.ExperimentalSyntax;
import org.skriptlang.skript.log.runtime.RuntimeError;
import org.skriptlang.skript.log.runtime.RuntimeErrorCatcher;

@Name(value="Catch Runtime Errors")
@Description(value={"Catch any runtime errors produced by code within the section. This is an in progress feature."})
@Example(value="catch runtime errors:\n\tset worldborder center of {_border} to location(0, 0, NaN value)\nif last caught runtime errors contains \"Your location can't have a NaN value as one of its components\":\n\tset worldborder center of {_border} to location(0, 0, 0)\n")
@Since(value={"2.12"})
public class SecCatchErrors
extends Section
implements ExperimentalSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        if (sectionNode.isEmpty()) {
            Skript.error("A catch errors section must contain code.");
            return false;
        }
        ParserInstance parser = this.getParser();
        Kleenean previousDelay = parser.getHasDelayBefore();
        parser.setHasDelayBefore(Kleenean.FALSE);
        this.loadCode(sectionNode);
        if (parser.getHasDelayBefore().isTrue()) {
            Skript.error("Delays can't be used within a catch errors section.");
            return false;
        }
        parser.setHasDelayBefore(previousDelay);
        return true;
    }

    @Override
    public boolean isSatisfiedBy(ExperimentSet experimentSet) {
        return experimentSet.hasExperiment(Feature.CATCH_ERRORS);
    }

    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        if (this.first != null && this.last != null) {
            try (RuntimeErrorCatcher catcher = new RuntimeErrorCatcher().start();){
                this.last.setNext(null);
                TriggerItem.walk(this.first, event);
                ExprCaughtErrors.lastErrors = (String[])catcher.getCachedErrors().stream().map(RuntimeError::error).toArray(String[]::new);
            }
        }
        return this.walk(event, false);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "catch runtime errors";
    }

    static {
        Skript.registerSection(SecCatchErrors.class, "catch [run[ ]time] error[s]");
    }
}

