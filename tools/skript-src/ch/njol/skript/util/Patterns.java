/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

public class Patterns<T> {
    private final String[] patterns;
    private final Object[] types;
    private final Map<Object, List<Integer>> matchedPatterns = new HashMap<Object, List<Integer>>();

    public Patterns(Object[][] info) {
        this.patterns = new String[info.length];
        this.types = new Object[info.length];
        for (int i = 0; i < info.length; ++i) {
            if (info[i].length != 2 || !(info[i][0] instanceof String)) {
                throw new IllegalArgumentException("given array is not like {{String, T}, {String, T}, ...}");
            }
            this.patterns[i] = (String)info[i][0];
            this.types[i] = info[i][1];
            this.matchedPatterns.computeIfAbsent(info[i][1], list -> new ArrayList()).add(i);
        }
    }

    public String[] getPatterns() {
        return this.patterns;
    }

    public T getInfo(int matchedPattern) {
        Object object = this.types[matchedPattern];
        if (object == null) {
            return null;
        }
        return (T)object;
    }

    public Integer @Nullable [] getMatchedPatterns(@Nullable T type) {
        if (this.matchedPatterns.containsKey(type)) {
            return (Integer[])this.matchedPatterns.get(type).toArray(Integer[]::new);
        }
        return null;
    }

    public Optional<Integer> getMatchedPattern(@Nullable T type, int arrayIndex) {
        Integer[] patternIndices = this.getMatchedPatterns(type);
        if (patternIndices == null || patternIndices.length < arrayIndex + 1) {
            return Optional.empty();
        }
        return Optional.of(patternIndices[arrayIndex]);
    }
}

