/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.UnmodifiableView
 */
package org.skriptlang.skript.util;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.jetbrains.annotations.UnmodifiableView;

public class IndexTrackingTreeMap<V>
extends TreeMap<String, V> {
    private final Set<String> mapIndices = new HashSet<String>();
    private final Set<Integer> numericalIndices = new HashSet<Integer>();
    private int nextIndex = 1;
    private int maxIndex = -1;

    public IndexTrackingTreeMap() {
    }

    public IndexTrackingTreeMap(Comparator<? super String> comparator) {
        super(comparator);
    }

    @Override
    public V put(String key, V value) {
        V previous = super.put(key, value);
        if (previous == null && value != null) {
            this.handleInsert(key, this.parsePositiveInt(key), value);
        } else if (previous != null && value == null) {
            this.handleRemove(key, previous);
        } else if (previous != null) {
            this.handleReplace(key, previous, value);
        }
        return previous;
    }

    public void add(V value) {
        Preconditions.checkNotNull(value, (Object)"value");
        String key = String.valueOf(this.nextIndex);
        super.put(key, value);
        this.handleInsert(key, this.nextIndex, value);
    }

    @Override
    public V remove(Object key) {
        Object value = super.remove(key);
        if (value != null && key instanceof String) {
            String index = (String)key;
            this.handleRemove(index, value);
        }
        return value;
    }

    @Override
    public void clear() {
        super.clear();
        this.numericalIndices.clear();
        this.mapIndices.clear();
        this.nextIndex = 1;
        this.maxIndex = -1;
    }

    public int nextOpenIndex() {
        return this.nextIndex;
    }

    public boolean consecutive() {
        return this.nextIndex == this.maxIndex + 1;
    }

    public @UnmodifiableView Collection<String> mapIndices() {
        return Collections.unmodifiableCollection(this.mapIndices);
    }

    private void handleInsert(String key, int index, V value) {
        if (value instanceof Map) {
            this.mapIndices.add(key);
        }
        if (index < 0) {
            return;
        }
        this.numericalIndices.add(index);
        this.maxIndex = Math.max(this.maxIndex, index);
        this.advanceNextIndex();
    }

    private void handleReplace(String key, V previous, V value) {
        if (value instanceof Map) {
            this.mapIndices.add(key);
        } else if (previous instanceof Map) {
            this.mapIndices.remove(key);
        }
    }

    private void handleRemove(String key, V previous) {
        int index;
        if (previous instanceof Map) {
            this.mapIndices.remove(key);
        }
        if ((index = this.parsePositiveInt(key)) < 0) {
            return;
        }
        this.numericalIndices.remove(index);
        if (index == this.maxIndex) {
            this.recomputeMaxIndex();
        }
        this.nextIndex = Math.min(this.nextIndex, index);
    }

    private void advanceNextIndex() {
        if (this.nextIndex == this.maxIndex) {
            ++this.nextIndex;
            return;
        }
        while (this.numericalIndices.contains(this.nextIndex)) {
            ++this.nextIndex;
        }
    }

    private void recomputeMaxIndex() {
        while (this.maxIndex >= 0 && !this.numericalIndices.contains(this.maxIndex)) {
            --this.maxIndex;
        }
    }

    private int parsePositiveInt(String string) {
        if (string == null || string.isBlank() || string.charAt(0) == '0') {
            return -1;
        }
        int value = 0;
        try {
            for (int i = 0; i < string.length(); ++i) {
                char c = string.charAt(i);
                if (!this.isDigit(c)) {
                    return -1;
                }
                value = Math.addExact(value * 10, c - 48);
            }
        }
        catch (ArithmeticException e) {
            return -1;
        }
        return value;
    }

    private boolean isDigit(int codepoint) {
        return codepoint >= 48 && codepoint <= 57;
    }
}

