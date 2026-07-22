/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.entry;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryData;
import org.skriptlang.skript.lang.entry.KeyValueEntryData;
import org.skriptlang.skript.lang.entry.SectionEntryData;

public class EntryValidator {
    private static final Function<String, String> DEFAULT_UNEXPECTED_ENTRY_MESSAGE = key -> "Unexpected entry '" + key + "'. Check whether it's spelled correctly or remove it";
    private static final Function<String, String> DEFAULT_MISSING_REQUIRED_ENTRY_MESSAGE = key -> "Required entry '" + key + "' is missing";
    private final List<EntryData<?>> entryData;
    @Nullable
    private final Predicate<Node> unexpectedNodeTester;
    private final Function<String, String> unexpectedEntryMessage;
    private final Function<String, String> missingRequiredEntryMessage;

    public static EntryValidatorBuilder builder() {
        return new EntryValidatorBuilder();
    }

    protected EntryValidator(List<EntryData<?>> entryData, @Nullable Predicate<Node> unexpectedNodeTester, @Nullable Function<String, String> unexpectedEntryMessage, @Nullable Function<String, String> missingRequiredEntryMessage) {
        this.entryData = entryData;
        this.unexpectedNodeTester = unexpectedNodeTester;
        this.unexpectedEntryMessage = unexpectedEntryMessage != null ? unexpectedEntryMessage : DEFAULT_UNEXPECTED_ENTRY_MESSAGE;
        this.missingRequiredEntryMessage = missingRequiredEntryMessage != null ? missingRequiredEntryMessage : DEFAULT_MISSING_REQUIRED_ENTRY_MESSAGE;
    }

    public List<EntryData<?>> getEntryData() {
        return Collections.unmodifiableList(this.entryData);
    }

    @Nullable
    public Predicate<Node> getUnexpectedNodeTester() {
        return this.unexpectedNodeTester;
    }

    public Function<String, String> getUnexpectedEntryMessage() {
        return this.unexpectedEntryMessage;
    }

    public Function<String, String> getMissingRequiredEntryMessage() {
        return this.missingRequiredEntryMessage;
    }

    @Nullable
    public EntryContainer validate(SectionNode sectionNode) {
        ArrayList entries = new ArrayList(this.entryData);
        HashMap<String, Collection<Node>> handledNodes = new HashMap<String, Collection<Node>>();
        ArrayList<Node> unhandledNodes = new ArrayList<Node>();
        boolean ok = true;
        block0: for (Node node : sectionNode) {
            if (node.getKey() == null) continue;
            Iterator iterator = entries.iterator();
            while (iterator.hasNext()) {
                EntryData data = (EntryData)iterator.next();
                if (!data.canCreateWith(node)) continue;
                Collection nodes = handledNodes.computeIfAbsent(data.getKey(), k -> new LinkedList());
                nodes.add(node);
                if (data.supportsMultiple()) continue block0;
                iterator.remove();
                continue block0;
            }
            if (this.unexpectedNodeTester == null || this.unexpectedNodeTester.test(node)) {
                ok = false;
                Skript.error(this.unexpectedEntryMessage.apply(ScriptLoader.replaceOptions(node.getKey())));
                continue;
            }
            unhandledNodes.add(node);
        }
        for (EntryData entryData : entries) {
            if (entryData.supportsMultiple() && handledNodes.containsKey(entryData.getKey()) || entryData.isOptional()) continue;
            Skript.error(this.missingRequiredEntryMessage.apply(entryData.getKey()));
            ok = false;
        }
        if (!ok) {
            return null;
        }
        return new EntryContainer(sectionNode, this, handledNodes, unhandledNodes);
    }

    public static class EntryValidatorBuilder {
        public static final String DEFAULT_ENTRY_SEPARATOR = ": ";
        private final List<EntryData<?>> entryData = new ArrayList();
        private String entrySeparator = ": ";
        @Nullable
        private Predicate<Node> unexpectedNodeTester;
        @Nullable
        private Function<String, String> unexpectedEntryMessage;
        @Nullable
        private Function<String, String> missingRequiredEntryMessage;

        protected EntryValidatorBuilder() {
        }

        @Nullable
        protected Function<String, String> getMissingRequiredEntryMessage() {
            return this.missingRequiredEntryMessage;
        }

        @Nullable
        protected Function<String, String> getUnexpectedEntryMessage() {
            return this.unexpectedEntryMessage;
        }

        @Nullable
        protected Predicate<Node> getUnexpectedNodeTester() {
            return this.unexpectedNodeTester;
        }

        protected List<EntryData<?>> getEntryData() {
            return this.entryData;
        }

        protected String getEntrySeparator() {
            return this.entrySeparator;
        }

        public EntryValidatorBuilder entrySeparator(String separator) {
            this.entrySeparator = separator;
            return this;
        }

        public EntryValidatorBuilder unexpectedNodeTester(Predicate<Node> unexpectedNodeTester) {
            this.unexpectedNodeTester = unexpectedNodeTester;
            return this;
        }

        public EntryValidatorBuilder unexpectedEntryMessage(Function<String, String> unexpectedEntryMessage) {
            this.unexpectedEntryMessage = unexpectedEntryMessage;
            return this;
        }

        public EntryValidatorBuilder missingRequiredEntryMessage(Function<String, String> message) {
            this.missingRequiredEntryMessage = message;
            return this;
        }

        public EntryValidatorBuilder addEntry(String key, @Nullable String defaultValue, boolean optional) {
            return this.addEntry(key, defaultValue, optional, false);
        }

        public EntryValidatorBuilder addEntry(String key, @Nullable String defaultValue, boolean optional, boolean multiple) {
            this.entryData.add(new KeyValueEntryData<String>(key, defaultValue, optional, multiple){

                @Override
                protected String getValue(String value) {
                    return value;
                }

                @Override
                public String getSeparator() {
                    return entrySeparator;
                }
            });
            return this;
        }

        public EntryValidatorBuilder addSection(String key, boolean optional) {
            return this.addSection(key, optional, false);
        }

        public EntryValidatorBuilder addSection(String key, boolean optional, boolean multiple) {
            this.entryData.add(new SectionEntryData(key, null, optional, multiple));
            return this;
        }

        public EntryValidatorBuilder addEntryData(EntryData<?> entryData) {
            this.entryData.add(entryData);
            return this;
        }

        public EntryValidator build() {
            return new EntryValidator(this.entryData, this.unexpectedNodeTester, this.unexpectedEntryMessage, this.missingRequiredEntryMessage);
        }
    }
}

