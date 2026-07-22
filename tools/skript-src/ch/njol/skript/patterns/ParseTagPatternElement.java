/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.patterns;

import ch.njol.skript.patterns.ChoicePatternElement;
import ch.njol.skript.patterns.GroupPatternElement;
import ch.njol.skript.patterns.LiteralPatternElement;
import ch.njol.skript.patterns.MatchResult;
import ch.njol.skript.patterns.OptionalPatternElement;
import ch.njol.skript.patterns.PatternElement;
import ch.njol.skript.patterns.SkriptPattern;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParseTagPatternElement
extends PatternElement {
    @Nullable
    private String tag;
    private final int mark;

    public ParseTagPatternElement(int mark) {
        this.tag = null;
        this.mark = mark;
    }

    public ParseTagPatternElement(@NotNull String tag) {
        this.tag = tag;
        int mark = 0;
        try {
            mark = Integer.parseInt(tag);
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
        this.mark = mark;
    }

    @Override
    void setNext(@Nullable PatternElement next) {
        if (this.tag != null && this.tag.isEmpty()) {
            if (next instanceof LiteralPatternElement) {
                this.tag = next.toString().trim();
            } else {
                PatternElement inner = null;
                if (next instanceof GroupPatternElement) {
                    inner = ((GroupPatternElement)next).getPatternElement();
                } else if (next instanceof OptionalPatternElement) {
                    inner = ((OptionalPatternElement)next).getPatternElement();
                }
                if (inner instanceof ChoicePatternElement) {
                    ChoicePatternElement choicePatternElement = (ChoicePatternElement)inner;
                    List<PatternElement> patternElements = choicePatternElement.getPatternElements();
                    for (int i = 0; i < patternElements.size(); ++i) {
                        PatternElement patternElement = patternElements.get(i);
                        if (!(patternElement instanceof LiteralPatternElement) || patternElement.toString().isEmpty()) continue;
                        ParseTagPatternElement newTag = new ParseTagPatternElement(patternElement.toString().trim());
                        newTag.setNext(patternElement);
                        newTag.originalNext = patternElement;
                        patternElements.set(i, newTag);
                    }
                }
            }
        }
        super.setNext(next);
    }

    @Override
    @Nullable
    public MatchResult match(String expr, MatchResult matchResult) {
        if (this.tag != null && !this.tag.isEmpty()) {
            matchResult.tags.add(this.tag);
        }
        matchResult.mark ^= this.mark;
        return this.matchNext(expr, matchResult);
    }

    @Override
    public String toString() {
        return this.toString(SkriptPattern.StringificationProperties.DEFAULT);
    }

    @Override
    public String toString(SkriptPattern.StringificationProperties properties) {
        if (properties.excludeParseTags()) {
            return "";
        }
        if (this.tag == null) {
            return this.mark + ":";
        }
        if (this.tag.isEmpty()) {
            return "";
        }
        return this.tag + ":";
    }

    @Override
    public Set<String> getCombinations(boolean clean) {
        HashSet<String> combinations = new HashSet<String>();
        if (!clean) {
            combinations.add(this.toString());
        }
        return combinations;
    }
}

