/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventPriority
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.SkriptEventHandler;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptEventInfo;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.structures.StructEvent;
import ch.njol.skript.util.Utils;
import ch.njol.util.coll.iterator.ConsumingIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.lang.structure.StructureInfo;

public abstract class SkriptEvent
extends Structure {
    public static final Structure.Priority PRIORITY = new Structure.Priority(600);
    private String expr;
    private SectionNode source;
    @Nullable
    protected EventPriority eventPriority;
    @Nullable
    protected ListeningBehavior listeningBehavior;
    protected boolean supportsListeningBehavior;
    private SkriptEventInfo<?> skriptEventInfo;
    protected Trigger trigger;

    @Override
    public final boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, @Nullable EntryContainer entryContainer) {
        this.expr = parseResult.expr;
        StructEvent.EventData eventData = this.getParser().getData(StructEvent.EventData.class);
        EventPriority priority = eventData.getPriority();
        if (priority != null && !this.isEventPrioritySupported()) {
            Skript.error("This event doesn't support event priority");
            return false;
        }
        this.eventPriority = priority;
        StructureInfo<? extends Structure> syntaxElementInfo = this.getParser().getData(Structure.StructureData.class).getStructureInfo();
        if (!(syntaxElementInfo instanceof SkriptEventInfo)) {
            throw new IllegalStateException();
        }
        this.skriptEventInfo = (SkriptEventInfo)syntaxElementInfo;
        assert (entryContainer != null);
        this.source = entryContainer.getSource();
        this.listeningBehavior = eventData.getListenerBehavior();
        if (!this.init(args, matchedPattern, parseResult)) {
            return false;
        }
        for (Class<? extends Event> eventClass : this.getEventClasses()) {
            if (!Cancellable.class.isAssignableFrom(eventClass)) continue;
            this.supportsListeningBehavior = true;
            break;
        }
        if (this.listeningBehavior != null && !this.isListeningBehaviorSupported()) {
            String eventName = this.skriptEventInfo.name.toLowerCase(Locale.ENGLISH);
            Skript.error(Utils.A(eventName) + " event does not support listening for cancelled or uncancelled events.");
            return false;
        }
        return true;
    }

    public abstract boolean init(Literal<?>[] var1, int var2, SkriptParser.ParseResult var3);

    @Override
    public boolean preLoad() {
        return super.preLoad();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean load() {
        if (!this.shouldLoadEvent()) {
            return false;
        }
        if (Skript.debug() || this.source.debug()) {
            Skript.debug(this.expr + " (" + String.valueOf(this) + "):");
        }
        Class<? extends Event>[] eventClasses = this.getEventClasses();
        try {
            this.getParser().setCurrentEvent(this.skriptEventInfo.getName().toLowerCase(Locale.ENGLISH), eventClasses);
            @Nullable ArrayList<TriggerItem> items = ScriptLoader.loadItems(this.source);
            Script script = this.getParser().getCurrentScript();
            this.trigger = new Trigger(script, this.expr, this, items);
            int lineNumber = this.source.getLine();
            this.trigger.setLineNumber(lineNumber);
            this.trigger.setDebugLabel(String.valueOf(script) + ": line " + lineNumber);
        }
        finally {
            this.getParser().deleteCurrentEvent();
        }
        return true;
    }

    @Override
    public boolean postLoad() {
        SkriptEventHandler.registerBukkitEvents(this.trigger, this.getEventClasses());
        return true;
    }

    @Override
    public void unload() {
        SkriptEventHandler.unregisterBukkitEvents(this.trigger);
    }

    @Override
    public void postUnload() {
        super.postUnload();
    }

    @Override
    public Structure.Priority getPriority() {
        return PRIORITY;
    }

    public abstract boolean check(Event var1);

    public boolean shouldLoadEvent() {
        return true;
    }

    public Class<? extends Event>[] getEventClasses() {
        return this.skriptEventInfo.events;
    }

    public EventPriority getEventPriority() {
        return this.eventPriority != null ? this.eventPriority : SkriptConfig.defaultEventPriority.value();
    }

    public boolean isEventPrioritySupported() {
        return true;
    }

    public ListeningBehavior getListeningBehavior() {
        return this.listeningBehavior != null ? this.listeningBehavior : this.skriptEventInfo.getListeningBehavior();
    }

    public boolean isListeningBehaviorSupported() {
        return this.supportsListeningBehavior;
    }

    public boolean canExecuteAsynchronously() {
        return false;
    }

    public static String fixPattern(String pattern) {
        return BukkitSyntaxInfos.fixPattern(pattern);
    }

    @Nullable
    public static SkriptEvent parse(String expr, SectionNode sectionNode, @Nullable String defaultError) {
        ParserInstance.get().getData(Structure.StructureData.class).node = sectionNode;
        Iterator<BukkitSyntaxInfos.Event<?>> iterator = Skript.instance().syntaxRegistry().syntaxes(BukkitSyntaxInfos.Event.KEY).iterator();
        iterator = new ConsumingIterator<BukkitSyntaxInfos.Event>(iterator, info -> {
            ParserInstance.get().getData(Structure.StructureData.class).structureInfo = (SkriptEventInfo)SyntaxElementInfo.fromModern(info);
        });
        try (ParseLogHandler parseLogHandler = SkriptLogger.startParseLogHandler();){
            SkriptEvent event = (SkriptEvent)SkriptParser.parseStatic(expr, iterator, ParseContext.EVENT, defaultError);
            if (event != null) {
                parseLogHandler.printLog();
                SkriptEvent skriptEvent = event;
                return skriptEvent;
            }
            parseLogHandler.printError();
            SkriptEvent skriptEvent = null;
            return skriptEvent;
        }
    }

    public static final class ListeningBehavior
    extends Enum<ListeningBehavior> {
        public static final /* enum */ ListeningBehavior UNCANCELLED = new ListeningBehavior();
        public static final /* enum */ ListeningBehavior CANCELLED = new ListeningBehavior();
        public static final /* enum */ ListeningBehavior ANY = new ListeningBehavior();
        private static final /* synthetic */ ListeningBehavior[] $VALUES;

        public static ListeningBehavior[] values() {
            return (ListeningBehavior[])$VALUES.clone();
        }

        public static ListeningBehavior valueOf(String name) {
            return Enum.valueOf(ListeningBehavior.class, name);
        }

        public boolean matches(boolean cancelled) {
            switch (this.ordinal()) {
                case 1: {
                    return cancelled;
                }
                case 0: {
                    return !cancelled;
                }
                case 2: {
                    return true;
                }
            }
            assert (false);
            return false;
        }

        private static /* synthetic */ ListeningBehavior[] $values() {
            return new ListeningBehavior[]{UNCANCELLED, CANCELLED, ANY};
        }

        static {
            $VALUES = ListeningBehavior.$values();
        }
    }
}

