/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.patterns;

import ch.njol.skript.patterns.MatchResult;
import ch.njol.skript.patterns.PatternElement;
import ch.njol.skript.patterns.SkriptPattern;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public class ChoicePatternElement
extends PatternElement {
    private final List<PatternElement> patternElements = new ArrayList<PatternElement>();

    public void add(PatternElement patternElement) {
        this.patternElements.add(patternElement);
    }

    public PatternElement getLast() {
        return this.patternElements.getLast();
    }

    public void setLast(PatternElement patternElement) {
        this.patternElements.removeLast();
        this.patternElements.add(patternElement);
    }

    public List<PatternElement> getPatternElements() {
        return this.patternElements;
    }

    @Override
    void setNext(@Nullable PatternElement next) {
        super.setNext(next);
        for (PatternElement patternElement : this.patternElements) {
            patternElement.setLastNext(next);
        }
    }

    @Override
    @Nullable
    public MatchResult match(String expr, MatchResult matchResult) {
        for (PatternElement patternElement : this.patternElements) {
            MatchResult matchResultCopy;
            MatchResult newMatchResult = patternElement.match(expr, matchResultCopy = matchResult.copy());
            if (newMatchResult == null) continue;
            return newMatchResult;
        }
        return null;
    }

    @Override
    public String toString() {
        return this.toString(SkriptPattern.StringificationProperties.DEFAULT);
    }

    @Override
    public String toString(SkriptPattern.StringificationProperties properties) {
        return this.patternElements.stream().map(element -> element.toFullString(properties)).collect(Collectors.joining("|"));
    }

    @Override
    public Set<String> getCombinations(boolean clean) {
        HashSet<String> combinations = new HashSet<String>();
        this.patternElements.forEach(patternElement -> combinations.addAll(patternElement.getAllCombinations(clean)));
        return combinations;
    }
}

