/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventPriority
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.structures;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.parser.ParserInstance;
import java.util.Locale;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.structure.Structure;

@Name(value="Event")
@Description(value={"The structure used for listening to events.\n\nOptionally allows specifying whether to listen to events that have been cancelled, and allows specifying with which priority to listen to events. Events are called in the following order of priorities.\n\n```\nlowest -> low -> normal -> high -> highest -> monitor\n```\n\nModifying event-values or cancelling events is not supported when using the 'monitor' priority. It should only be used for monitoring the outcome of an event.\n"})
@Example.Examples(value={@Example(value="on load:\n\tbroadcast \"loading!\"\n"), @Example(value="on join:\n\tif {first-join::%player's uuid%} is not set:\n\t\tset {first-join::%player's uuid%} to now\n"), @Example(value="cancelled block break:\n\tsend \"<red>You can't break that here\" to player\n"), @Example(value="on join with priority lowest:\n\t# called first\n\non join:\n\t# called second\n\non join with priority highest:\n\t# called last\n")})
@Since(value={"1.0", "2.6 (per-event priority)", "2.9 (listening to cancellable events)"})
public class StructEvent
extends Structure {
    private SkriptEvent event;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, @Nullable EntryContainer entryContainer) {
        String expr = parseResult.regexes.get(0).group();
        EventData data = this.getParser().getData(EventData.class);
        data.clear();
        if (parseResult.hasTag("uncancelled")) {
            data.behavior = SkriptEvent.ListeningBehavior.UNCANCELLED;
        } else if (parseResult.hasTag("cancelled")) {
            data.behavior = SkriptEvent.ListeningBehavior.CANCELLED;
        } else if (parseResult.hasTag("any")) {
            data.behavior = SkriptEvent.ListeningBehavior.ANY;
        }
        if (parseResult.hasTag("priority")) {
            String lastTag = parseResult.tags.get(parseResult.tags.size() - 1);
            data.priority = EventPriority.valueOf((String)lastTag.toUpperCase(Locale.ENGLISH));
        }
        assert (entryContainer != null);
        this.event = SkriptEvent.parse(expr, entryContainer.getSource(), null);
        data.clear();
        return this.event != null;
    }

    @Override
    public boolean preLoad() {
        this.getParser().setCurrentStructure(this.event);
        return this.event.preLoad();
    }

    @Override
    public boolean load() {
        this.getParser().setCurrentStructure(this.event);
        return this.event.load();
    }

    @Override
    public boolean postLoad() {
        this.getParser().setCurrentStructure(this.event);
        return this.event.postLoad();
    }

    @Override
    public void unload() {
        this.event.unload();
    }

    @Override
    public void postUnload() {
        this.event.postUnload();
    }

    @Override
    public Structure.Priority getPriority() {
        return this.event.getPriority();
    }

    public SkriptEvent getSkriptEvent() {
        return this.event;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.event.toString(event, debug);
    }

    static {
        Skript.registerStructure(StructEvent.class, "[on] [:uncancelled|:cancelled|any:(any|all)] <.+> [priority:with priority (:(lowest|low|normal|high|highest|monitor))]");
        ParserInstance.registerData(EventData.class, EventData::new);
    }

    public static class EventData
    extends ParserInstance.Data {
        @Nullable
        private EventPriority priority;
        @Nullable
        private SkriptEvent.ListeningBehavior behavior;

        public EventData(ParserInstance parserInstance) {
            super(parserInstance);
        }

        @Nullable
        public EventPriority getPriority() {
            return this.priority;
        }

        @Nullable
        public SkriptEvent.ListeningBehavior getListenerBehavior() {
            return this.behavior;
        }

        public void clear() {
            this.priority = null;
            this.behavior = null;
        }
    }
}

