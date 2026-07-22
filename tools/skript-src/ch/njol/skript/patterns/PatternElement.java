/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.patterns;

import ch.njol.skript.patterns.MatchResult;
import ch.njol.skript.patterns.SkriptPattern;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public abstract class PatternElement {
    @Nullable
    PatternElement originalNext;
    @Nullable
    PatternElement next;

    void setNext(@Nullable PatternElement next) {
        this.next = next;
    }

    void setLastNext(@Nullable PatternElement newNext) {
        PatternElement next = this;
        while (true) {
            if (next.next == null) {
                next.setNext(newNext);
                return;
            }
            next = next.next;
        }
    }

    @Nullable
    public abstract MatchResult match(String var1, MatchResult var2);

    @Nullable
    protected MatchResult matchNext(String expr, MatchResult matchResult) {
        if (this.next == null) {
            return matchResult.exprOffset == expr.length() ? matchResult : null;
        }
        return this.next.match(expr, matchResult);
    }

    public abstract String toString();

    public abstract String toString(SkriptPattern.StringificationProperties var1);

    @Deprecated(since="2.15", forRemoval=true)
    public String toFullString() {
        return this.toFullString(SkriptPattern.StringificationProperties.DEFAULT);
    }

    public String toFullString(SkriptPattern.StringificationProperties properties) {
        StringBuilder stringBuilder = new StringBuilder(this.toString(properties));
        PatternElement next = this;
        while ((next = next.originalNext) != null) {
            stringBuilder.append(next.toString(properties));
        }
        return stringBuilder.toString();
    }

    public abstract Set<String> getCombinations(boolean var1);

    public final Set<String> getAllCombinations(boolean clean) {
        Set<String> combinations = this.getCombinations(clean);
        if (combinations.isEmpty()) {
            combinations.add("");
        }
        PatternElement next = this;
        while ((next = next.originalNext) != null) {
            HashSet<String> newCombinations = new HashSet<String>();
            Set<String> nextCombinations = next.getCombinations(clean);
            if (nextCombinations.isEmpty()) continue;
            for (String base : combinations) {
                for (String add : nextCombinations) {
                    newCombinations.add(PatternElement.combineCombination(base, add));
                }
            }
            combinations = newCombinations;
        }
        return combinations;
    }

    private static String combineCombination(String first, String second) {
        if (first.isBlank()) {
            return second.stripLeading();
        }
        if (second.isEmpty()) {
            return first.stripTrailing();
        }
        if (first.endsWith(" ") && second.startsWith(" ")) {
            return first + second.stripLeading();
        }
        return first + second;
    }
}

