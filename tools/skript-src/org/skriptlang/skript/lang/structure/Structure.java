/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package org.skriptlang.skript.lang.structure;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.SimpleNode;
import ch.njol.skript.lang.Debuggable;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.CheckedIterator;
import ch.njol.util.coll.iterator.ConsumingIterator;
import java.util.Arrays;
import java.util.Iterator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.structure.StructureInfo;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

public abstract class Structure
implements SyntaxElement,
Debuggable {
    public static final Priority DEFAULT_PRIORITY = new Priority(1000);
    @Nullable
    private EntryContainer entryContainer = null;

    @Deprecated(since="2.10.0", forRemoval=true)
    public final EntryContainer getEntryContainer() {
        if (this.entryContainer == null) {
            throw new IllegalStateException("This Structure hasn't been initialized!");
        }
        return this.entryContainer;
    }

    @Override
    public final boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Literal[] literals = (Literal[])Arrays.copyOf(expressions, expressions.length, Literal[].class);
        StructureData structureData = this.getParser().getData(StructureData.class);
        StructureInfo<? extends Structure> structureInfo = structureData.structureInfo;
        assert (structureInfo != null);
        if (structureData.node instanceof SimpleNode) {
            return this.init(literals, matchedPattern, parseResult, null);
        }
        EntryValidator entryValidator = structureInfo.entryValidator;
        if (entryValidator == null) {
            this.entryContainer = EntryContainer.withoutValidator((SectionNode)structureData.node);
        } else {
            EntryContainer entryContainer = entryValidator.validate((SectionNode)structureData.node);
            if (entryContainer == null) {
                return false;
            }
            this.entryContainer = entryContainer;
        }
        return this.init(literals, matchedPattern, parseResult, this.entryContainer);
    }

    public abstract boolean init(Literal<?>[] var1, int var2, SkriptParser.ParseResult var3, @UnknownNullability EntryContainer var4);

    public boolean preLoad() {
        return true;
    }

    public abstract boolean load();

    public boolean postLoad() {
        return true;
    }

    public void unload() {
    }

    public void postUnload() {
    }

    public Priority getPriority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    public String toString() {
        return this.toString(null, false);
    }

    @Override
    @NotNull
    public String getSyntaxTypeName() {
        return "structure";
    }

    @Nullable
    public static Structure parse(String expr, Node node, @Nullable String defaultError) {
        if (!(node instanceof SimpleNode) && !(node instanceof SectionNode)) {
            throw new IllegalArgumentException("only simple or section nodes may be parsed as a structure");
        }
        ParserInstance.get().getData(StructureData.class).node = node;
        Iterator<DefaultSyntaxInfos.Structure<?>> iterator = Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.STRUCTURE).iterator();
        iterator = node instanceof SimpleNode ? new CheckedIterator<DefaultSyntaxInfos.Structure>(iterator, info -> info != null && info.nodeType().canBeSimple()) : new CheckedIterator<DefaultSyntaxInfos.Structure>(iterator, info -> info != null && info.nodeType().canBeSection());
        iterator = new ConsumingIterator<DefaultSyntaxInfos.Structure>(iterator, info -> {
            ParserInstance.get().getData(StructureData.class).structureInfo = (StructureInfo)SyntaxElementInfo.fromModern(info);
        });
        try (ParseLogHandler parseLogHandler = SkriptLogger.startParseLogHandler();){
            Structure structure = (Structure)SkriptParser.parseStatic(expr, iterator, ParseContext.EVENT, defaultError);
            if (structure != null) {
                parseLogHandler.printLog();
                Structure structure2 = structure;
                return structure2;
            }
            parseLogHandler.printError();
            Structure structure3 = null;
            return structure3;
        }
    }

    @Nullable
    public static Structure parse(String expr, Node node, @Nullable String defaultError, Iterator<? extends StructureInfo<? extends Structure>> iterator) {
        if (!(node instanceof SimpleNode) && !(node instanceof SectionNode)) {
            throw new IllegalArgumentException("only simple or section nodes may be parsed as a structure");
        }
        ParserInstance.get().getData(StructureData.class).node = node;
        iterator = node instanceof SimpleNode ? new CheckedIterator<StructureInfo>(iterator, item -> item != null && item.nodeType.canBeSimple()) : new CheckedIterator<StructureInfo>(iterator, item -> item != null && item.nodeType.canBeSection());
        iterator = new ConsumingIterator<StructureInfo>(iterator, elementInfo -> {
            ParserInstance.get().getData(StructureData.class).structureInfo = elementInfo;
        });
        try (ParseLogHandler parseLogHandler = SkriptLogger.startParseLogHandler();){
            Structure structure = (Structure)SkriptParser.parseStatic(expr, iterator, ParseContext.EVENT, defaultError);
            if (structure != null) {
                parseLogHandler.printLog();
                Structure structure2 = structure;
                return structure2;
            }
            parseLogHandler.printError();
            Structure structure3 = null;
            return structure3;
        }
    }

    static {
        ParserInstance.registerData(StructureData.class, StructureData::new);
    }

    public static class StructureData
    extends ParserInstance.Data {
        @ApiStatus.Internal
        public Node node;
        @ApiStatus.Internal
        @Nullable
        public StructureInfo<? extends Structure> structureInfo;

        public StructureData(ParserInstance parserInstance) {
            super(parserInstance);
        }

        @Nullable
        public StructureInfo<? extends Structure> getStructureInfo() {
            return this.structureInfo;
        }
    }

    public static class Priority
    implements Comparable<Priority> {
        private final int priority;

        public Priority(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return this.priority;
        }

        @Override
        public int compareTo(@NotNull Priority o) {
            return Integer.compare(this.priority, o.priority);
        }
    }
}

