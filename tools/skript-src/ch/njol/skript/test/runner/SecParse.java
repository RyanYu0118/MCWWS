/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.TriggerSection;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.test.runner.ExprParseLogs;
import ch.njol.util.Kleenean;
import com.google.common.collect.Iterables;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Parse Section")
@Description(value={"Parse code inside this section and use 'parse logs' to grab any logs from it."})
@NoDoc
public class SecParse
extends Section {
    private String[] logs;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        if (Iterables.size((Iterable)sectionNode) == 0) {
            Skript.error("A parse section must contain code");
            return false;
        }
        RetainingLogHandler handler = SkriptLogger.startRetainingLog();
        boolean inParseSection = this.getParser().isCurrentSection((Class<? extends TriggerSection>)SecParse.class);
        this.loadCode(sectionNode);
        if (!inParseSection) {
            this.logs = (String[])handler.getLog().stream().map(LogEntry::getMessage).toArray(String[]::new);
        }
        handler.stop();
        return true;
    }

    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        ExprParseLogs.lastLogs = this.logs;
        return this.walk(event, false);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "parse";
    }

    static {
        Skript.registerSection(SecParse.class, "parse");
    }
}

