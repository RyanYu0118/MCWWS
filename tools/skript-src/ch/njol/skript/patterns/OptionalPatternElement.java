/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.patterns;

import ch.njol.skript.patterns.GroupPatternElement;
import ch.njol.skript.patterns.MatchResult;
import ch.njol.skript.patterns.PatternElement;
import ch.njol.skript.patterns.SkriptPattern;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public class OptionalPatternElement
extends PatternElement {
    private final PatternElement patternElement;

    public OptionalPatternElement(PatternElement patternElement) {
        if (patternElement instanceof GroupPatternElement) {
            GroupPatternElement groupPatternElement = (GroupPatternElement)patternElement;
            if (groupPatternElement.next == null) {
                patternElement = groupPatternElement.getPatternElement();
            }
        }
        this.patternElement = patternElement;
    }

    @Override
    void setNext(@Nullable PatternElement next) {
        super.setNext(next);
        this.patternElement.setLastNext(next);
    }

    @Override
    @Nullable
    public MatchResult match(String expr, MatchResult matchResult) {
        MatchResult newMatchResult = this.patternElement.match(expr, matchResult.copy());
        if (newMatchResult != null) {
            return newMatchResult;
        }
        return this.matchNext(expr, matchResult);
    }

    public PatternElement getPatternElement() {
        return this.patternElement;
    }

    @Override
    public String toString() {
        return this.toString(SkriptPattern.StringificationProperties.DEFAULT);
    }

    @Override
    public String toString(SkriptPattern.StringificationProperties properties) {
        return "[" + this.patternElement.toFullString(properties) + "]";
    }

    @Override
    public Set<String> getCombinations(boolean clean) {
        Set<String> combinations = this.patternElement.getAllCombinations(clean);
        combinations.add("");
        return combinations;
    }
}

