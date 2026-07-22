/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.ApiStatus$NonExtendable
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ReturnableTrigger;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SectionSkriptEvent;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.parser.ParserInstance;
import java.util.Deque;
import java.util.LinkedList;
import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public interface ReturnHandler<T> {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @ApiStatus.NonExtendable
    default public void loadReturnableSectionCode(SectionNode node) {
        if (!(this instanceof Section)) {
            throw new SkriptAPIException("loadReturnableSectionCode called on a non-section object");
        }
        ParserInstance parser = ParserInstance.get();
        ReturnHandlerStack stack = parser.getData(ReturnHandlerStack.class);
        stack.push(this);
        Section section = (Section)((Object)this);
        try {
            section.loadCode(node);
        }
        finally {
            stack.pop();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @ApiStatus.NonExtendable
    default public ReturnableTrigger<T> loadReturnableSectionCode(SectionNode node, String name, Class<? extends Event>[] events) {
        if (!(this instanceof Section)) {
            throw new SkriptAPIException("loadReturnableSectionCode called on a non-section object");
        }
        ParserInstance parser = ParserInstance.get();
        ParserInstance.Backup parserBackup = parser.backup();
        parser.reset();
        parser.setCurrentEvent(name, events);
        SectionSkriptEvent skriptEvent = new SectionSkriptEvent(name, (Section)((Object)this));
        parser.setCurrentStructure(skriptEvent);
        ReturnHandlerStack stack = parser.getData(ReturnHandlerStack.class);
        try {
            ReturnableTrigger returnableTrigger = new ReturnableTrigger(this, parser.getCurrentScript(), name, skriptEvent, trigger -> {
                stack.push((ReturnHandler<?>)trigger);
                return ScriptLoader.loadItems(node);
            });
            return returnableTrigger;
        }
        finally {
            stack.pop();
            parser.restoreBackup(parserBackup);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @ApiStatus.NonExtendable
    default public ReturnableTrigger<T> loadReturnableTrigger(SectionNode node, String name, SkriptEvent event) {
        ParserInstance parser = ParserInstance.get();
        ReturnHandlerStack stack = parser.getData(ReturnHandlerStack.class);
        try {
            ReturnableTrigger returnableTrigger = new ReturnableTrigger(this, parser.getCurrentScript(), name, event, trigger -> {
                stack.push((ReturnHandler<?>)trigger);
                return ScriptLoader.loadItems(node);
            });
            return returnableTrigger;
        }
        finally {
            stack.pop();
        }
    }

    public void returnValues(Event var1, Expression<? extends T> var2);

    public boolean isSingleReturnValue();

    @Nullable
    public Class<? extends T> returnValueType();

    public static class ReturnHandlerStack
    extends ParserInstance.Data {
        private final Deque<ReturnHandler<?>> stack = new LinkedList();

        public ReturnHandlerStack(ParserInstance parserInstance) {
            super(parserInstance);
        }

        public Deque<ReturnHandler<?>> getStack() {
            return this.stack;
        }

        @Nullable
        public ReturnHandler<?> getCurrentHandler() {
            return this.stack.peek();
        }

        public void push(ReturnHandler<?> handler) {
            this.stack.push(handler);
        }

        public ReturnHandler<?> pop() {
            return this.stack.pop();
        }
    }
}

