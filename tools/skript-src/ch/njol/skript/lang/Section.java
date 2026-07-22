/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Debuggable;
import ch.njol.skript.lang.ExecutionIntent;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SectionSkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.TriggerSection;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public abstract class Section
extends TriggerSection
implements SyntaxElement,
SyntaxRuntimeErrorProducer {
    private Node node;

    @Override
    public boolean preInit() {
        this.node = this.getParser().getNode();
        return SyntaxElement.super.preInit();
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        SectionContext sectionContext = this.getParser().getData(SectionContext.class);
        return sectionContext.attemptClaim(this, parseResult.expr, (context, syntax) -> this.init(expressions, matchedPattern, isDelayed, parseResult, context.sectionNode, context.triggerItems));
    }

    public abstract boolean init(Expression<?>[] var1, int var2, Kleenean var3, SkriptParser.ParseResult var4, SectionNode var5, List<TriggerItem> var6);

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void loadCode(SectionNode sectionNode) {
        ParserInstance parser = this.getParser();
        List<TriggerSection> previousSections = parser.getCurrentSections();
        ArrayList<TriggerSection> sections = new ArrayList<TriggerSection>(previousSections);
        sections.add(this);
        parser.setCurrentSections(sections);
        Kleenean hadDelayBefore = parser.getHasDelayBefore();
        try {
            this.setTriggerItems(ScriptLoader.loadItems(sectionNode));
        }
        finally {
            ExecutionIntent intent;
            if (hadDelayBefore != parser.getHasDelayBefore() && (intent = this.triggerExecutionIntent()) instanceof ExecutionIntent.StopTrigger) {
                parser.setHasDelayBefore(hadDelayBefore);
            }
            parser.setCurrentSections(previousSections);
        }
    }

    @SafeVarargs
    protected final Trigger loadCode(SectionNode sectionNode, String name, Class<? extends Event> ... events) {
        return this.loadCode(sectionNode, name, (Runnable)null, events);
    }

    @SafeVarargs
    @Deprecated(since="2.12", forRemoval=true)
    protected final Trigger loadCode(SectionNode sectionNode, String name, @Nullable Runnable afterLoading, Class<? extends Event> ... events) {
        return this.loadCode(sectionNode, name, null, afterLoading, events);
    }

    @SafeVarargs
    protected final Trigger loadCode(SectionNode sectionNode, String name, @Nullable Runnable beforeLoading, @Nullable Runnable afterLoading, Class<? extends Event> ... events) {
        ParserInstance parser = this.getParser();
        ParserInstance.Backup parserBackup = parser.backup();
        parser.reset();
        if (beforeLoading != null) {
            beforeLoading.run();
        }
        parser.setCurrentEvent(name, events);
        SectionSkriptEvent skriptEvent = new SectionSkriptEvent(name, this);
        parser.setCurrentStructure(skriptEvent);
        ArrayList<TriggerItem> triggerItems = ScriptLoader.loadItems(sectionNode);
        if (afterLoading != null) {
            afterLoading.run();
        }
        parser.restoreBackup(parserBackup);
        return new Trigger(parser.getCurrentScript(), name, skriptEvent, triggerItems);
    }

    protected void loadOptionalCode(SectionNode sectionNode) {
        ParserInstance parser = this.getParser();
        Kleenean hadDelayBefore = parser.getHasDelayBefore();
        this.loadCode(sectionNode);
        if (!hadDelayBefore.isTrue() && !parser.getHasDelayBefore().isFalse()) {
            parser.setHasDelayBefore(Kleenean.UNKNOWN);
        }
    }

    @Nullable
    public static Section parse(String expr, @Nullable String defaultError, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        SectionContext sectionContext = ParserInstance.get().getData(SectionContext.class);
        return sectionContext.modify(sectionNode, triggerItems, () -> {
            Iterator<SyntaxInfo<? extends Section>> iterator = Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.SECTION).iterator();
            return SkriptParser.parse(expr, iterator, defaultError);
        });
    }

    @Override
    public Node getNode() {
        return this.node;
    }

    @Override
    @NotNull
    public String getSyntaxTypeName() {
        return "section";
    }

    static {
        ParserInstance.registerData(SectionContext.class, SectionContext::new);
    }

    public static class SectionContext
    extends ParserInstance.Data {
        protected SectionNode sectionNode;
        protected List<TriggerItem> triggerItems;
        @Nullable
        protected Debuggable owner;
        @Nullable
        protected String ownerErrorRepresentation;

        public SectionContext(ParserInstance parserInstance) {
            super(parserInstance);
        }

        public <T> T modify(SectionNode sectionNode, List<TriggerItem> triggerItems, Supplier<T> supplier) {
            SectionNode prevSectionNode = this.sectionNode;
            List<TriggerItem> prevTriggerItems = this.triggerItems;
            Debuggable owner = this.owner;
            String ownerErrorRepresentation = this.ownerErrorRepresentation;
            this.sectionNode = sectionNode;
            this.triggerItems = triggerItems;
            this.owner = null;
            this.ownerErrorRepresentation = null;
            T result = supplier.get();
            this.sectionNode = prevSectionNode;
            this.triggerItems = prevTriggerItems;
            this.owner = owner;
            this.ownerErrorRepresentation = ownerErrorRepresentation;
            return result;
        }

        @ApiStatus.Internal
        public <Syntax extends SyntaxElement & Debuggable> boolean attemptClaim(Syntax syntax, String errorRepresentation, BiFunction<SectionContext, Syntax, Boolean> runIfClaimed) {
            if (!this.claim(syntax, errorRepresentation)) {
                return false;
            }
            boolean success = runIfClaimed.apply(this, syntax);
            if (!success) {
                this.unclaim(syntax);
                return false;
            }
            return true;
        }

        @ApiStatus.Internal
        public <Syntax extends SyntaxElement & Debuggable> boolean claim(Syntax syntax, String errorRepresentation) {
            if (this.sectionNode == null) {
                return true;
            }
            if (this.claimed()) {
                if (this.owner == syntax) {
                    return true;
                }
                assert (this.owner != null);
                Skript.error("The syntax '" + errorRepresentation + "' tried to claim the current section, but it was already claimed by '" + this.ownerErrorRepresentation + "'. You cannot have two section-starters in the same line.");
                return false;
            }
            this.owner = syntax;
            this.ownerErrorRepresentation = errorRepresentation;
            return true;
        }

        @ApiStatus.Internal
        public <Syntax extends SyntaxElement & Debuggable> void unclaim(Syntax syntax) {
            if (this.sectionNode == null) {
                return;
            }
            if (!this.claimed() || this.owner != syntax) {
                throw new IllegalStateException("The syntax '" + ((Debuggable)syntax).toString(null, false) + "' tried to unclaim the current section, but it does not own it.");
            }
            this.owner = null;
            this.ownerErrorRepresentation = null;
        }

        public boolean claimed() {
            return this.owner != null;
        }
    }
}

