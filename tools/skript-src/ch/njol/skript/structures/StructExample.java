/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.structures;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.function.FunctionEvent;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.registrations.Feature;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.experiment.ExperimentData;
import org.skriptlang.skript.lang.experiment.SimpleExperimentalSyntax;
import org.skriptlang.skript.lang.structure.Structure;

@NoDoc
@Name(value="Example")
@Description(value={"Examples are structures that are parsed, but will never be run.", "They are used as miniature tutorials for demonstrating code snippets in the example files.", "Scripts containing an example are seen as 'examples' by the parser and may have special safety restrictions."})
@Example(value="example:\n\tbroadcast \"hello world\"\n\t# this is never run\n")
@Since(value={"2.10"})
public class StructExample
extends Structure
implements SimpleExperimentalSyntax {
    private static final ExperimentData EXPERIMENT_DATA = ExperimentData.createSingularData(Feature.EXAMPLES);
    public static final Structure.Priority PRIORITY = new Structure.Priority(550);
    private SectionNode source;

    @Override
    public boolean init(Literal<?>[] literals, int matchedPattern, SkriptParser.ParseResult parseResult, @Nullable EntryContainer entryContainer) {
        assert (entryContainer != null);
        this.source = entryContainer.getSource();
        return true;
    }

    @Override
    public ExperimentData getExperimentData() {
        return EXPERIMENT_DATA;
    }

    @Override
    public boolean load() {
        ParserInstance parser = this.getParser();
        parser.setCurrentEvent("example", FunctionEvent.class);
        ScriptLoader.loadItems(this.source);
        parser.deleteCurrentEvent();
        return true;
    }

    @Override
    public Structure.Priority getPriority() {
        return PRIORITY;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "example";
    }

    static {
        Skript.registerStructure(StructExample.class, "example");
    }
}

