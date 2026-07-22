/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.ContextlessEvent;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.Task;
import com.google.common.collect.Iterables;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData;
import org.skriptlang.skript.lang.structure.Structure;

@Name(value="Parse Structure")
@Description(value={"Parses the code inside this structure as a structure and use 'parse logs' to grab any logs from it."})
@NoDoc
public class StructParse
extends Structure {
    private static final EntryValidator validator;
    private SectionNode structureSectionNodeToParse;
    private String[] logs;
    private Expression<?> resultsExpression;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, EntryContainer entryContainer) {
        SectionNode parseStructureSectionNode = entryContainer.getSource();
        Class<? extends Event>[] originalEvents = this.getParser().getCurrentEvents();
        this.getParser().setCurrentEvent("parse", ContextlessEvent.class);
        EntryContainer validatedEntries = validator.validate(parseStructureSectionNode);
        this.getParser().setCurrentEvents(originalEvents);
        if (validatedEntries == null) {
            Skript.error("A parse structure must have a result entry and a code section");
            return false;
        }
        Expression maybeResultsExpression = (Expression)validatedEntries.get("results", false);
        if (!Changer.ChangerUtils.acceptsChange(maybeResultsExpression, Changer.ChangeMode.SET, String[].class)) {
            Skript.error(maybeResultsExpression.toString(null, false) + " cannot be set to strings");
            return false;
        }
        SectionNode codeSectionNode = (SectionNode)validatedEntries.get("code", false);
        Node maybeStructureSectionNodeToParse = (Node)Iterables.getFirst((Iterable)codeSectionNode, null);
        if (Iterables.size((Iterable)codeSectionNode) != 1 || !(maybeStructureSectionNodeToParse instanceof SectionNode)) {
            Skript.error("The code section must contain a single section to parse as a structure");
            return false;
        }
        this.resultsExpression = maybeResultsExpression;
        this.structureSectionNodeToParse = (SectionNode)maybeStructureSectionNodeToParse;
        return true;
    }

    @Override
    public boolean postLoad() {
        Task.callSync(() -> {
            this.resultsExpression.change(ContextlessEvent.get(), this.logs, Changer.ChangeMode.SET);
            return null;
        });
        return true;
    }

    @Override
    public boolean load() {
        try (RetainingLogHandler handler = SkriptLogger.startRetainingLog();){
            String structureSectionNodeKey = ScriptLoader.replaceOptions(this.structureSectionNodeToParse.getKey());
            String error = "Can't understand this structure: " + structureSectionNodeKey;
            Structure structure = Structure.parse(structureSectionNodeKey, this.structureSectionNodeToParse, error);
            this.getParser().setCurrentStructure(structure);
            if (structure != null && structure.preLoad() && structure.load()) {
                structure.postLoad();
            }
            this.getParser().setCurrentStructure(null);
            this.logs = (String[])handler.getLog().stream().map(LogEntry::getMessage).toArray(String[]::new);
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "parse";
    }

    static {
        Skript.registerStructure(StructParse.class, "parse");
        validator = EntryValidator.builder().addEntryData(new ExpressionEntryData<Object>("results", (Expression<Object>)null, false, Object.class)).addSection("code", false).build();
    }
}

