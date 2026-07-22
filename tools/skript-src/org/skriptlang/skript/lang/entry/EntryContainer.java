/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.lang.entry;

import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.parser.ParserInstance;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.lang.entry.EntryData;
import org.skriptlang.skript.lang.entry.EntryValidator;

public class EntryContainer {
    private final SectionNode source;
    @Nullable
    private final EntryValidator entryValidator;
    @Nullable
    private final Map<String, Collection<Node>> handledNodes;
    private final List<Node> unhandledNodes;

    protected EntryContainer(SectionNode source, @Nullable EntryValidator entryValidator, @Nullable Map<String, Collection<Node>> handledNodes, List<Node> unhandledNodes) {
        this.source = source;
        this.entryValidator = entryValidator;
        this.handledNodes = handledNodes;
        this.unhandledNodes = unhandledNodes;
    }

    public static EntryContainer withoutValidator(SectionNode source) {
        ArrayList<Node> unhandledNodes = new ArrayList<Node>();
        for (Node node : source) {
            unhandledNodes.add(node);
        }
        return new EntryContainer(source, null, null, unhandledNodes);
    }

    public SectionNode getSource() {
        return this.source;
    }

    public List<Node> getUnhandledNodes() {
        return this.unhandledNodes;
    }

    public <E, R extends E> @Unmodifiable List<R> getAll(String key, Class<E> expectedType, boolean useDefaultValue) {
        List<Object> parsed = this.getAll(key, useDefaultValue);
        for (Object object : parsed) {
            if (expectedType.isInstance(object)) continue;
            throw new RuntimeException("Expected entry with key '" + key + "' to be '" + String.valueOf(expectedType) + "', but got '" + String.valueOf(object.getClass()) + "'");
        }
        return parsed;
    }

    public @Unmodifiable List<Object> getAll(String key, boolean useDefaultValue) {
        if (this.entryValidator == null || this.handledNodes == null) {
            return Collections.emptyList();
        }
        EntryData entryData = this.entryValidator.getEntryData().stream().filter(data -> data.getKey().equals(key)).findFirst().orElse(null);
        if (entryData == null) {
            return Collections.emptyList();
        }
        Collection<Node> nodes = this.handledNodes.get(key);
        if (nodes == null || nodes.isEmpty()) {
            Object defaultValue = entryData.getDefaultValue();
            return defaultValue != null ? Collections.singletonList(defaultValue) : Collections.emptyList();
        }
        LinkedList values = new LinkedList();
        ParserInstance parser = ParserInstance.get();
        Node oldNode = parser.getNode();
        for (Node node : nodes) {
            parser.setNode(node);
            Object value = entryData.getValue(node);
            if (value == null && useDefaultValue) {
                value = entryData.getDefaultValue();
            }
            if (value == null) continue;
            values.add(value);
        }
        parser.setNode(oldNode);
        return Collections.unmodifiableList(values);
    }

    public <E, R extends E> R get(String key, Class<E> expectedType, boolean useDefaultValue) {
        List<R> all = this.getAll(key, expectedType, useDefaultValue);
        if (all.isEmpty()) {
            throw new RuntimeException("Null value for asserted non-null value");
        }
        return all.getFirst();
    }

    public Object get(String key, boolean useDefaultValue) {
        List<Object> all = this.getAll(key, useDefaultValue);
        if (all.isEmpty()) {
            throw new RuntimeException("Null value for asserted non-null value");
        }
        return all.getFirst();
    }

    @Nullable
    public <E, R extends E> R getOptional(String key, Class<E> expectedType, boolean useDefaultValue) {
        List<R> all = this.getAll(key, expectedType, useDefaultValue);
        return all.isEmpty() ? null : (R)all.getFirst();
    }

    @Nullable
    public Object getOptional(String key, boolean useDefaultValue) {
        List<Object> all = this.getAll(key, useDefaultValue);
        return all.isEmpty() ? null : all.getFirst();
    }

    public boolean hasEntry(@NotNull String key) {
        return this.handledNodes != null && this.handledNodes.containsKey(key);
    }
}

