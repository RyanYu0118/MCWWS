/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  com.google.common.collect.ImmutableSet
 *  org.jetbrains.annotations.Contract
 */
package ch.njol.skript.patterns;

import ch.njol.skript.patterns.ChoicePatternElement;
import ch.njol.skript.patterns.GroupPatternElement;
import ch.njol.skript.patterns.LiteralPatternElement;
import ch.njol.skript.patterns.ParseTagPatternElement;
import ch.njol.skript.patterns.PatternElement;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Contract;

abstract class Keyword {
    Keyword() {
    }

    abstract boolean isPresent(String var1);

    public static int computeMinLength(PatternElement first) {
        int length = 0;
        PatternElement next = first;
        while (next != null) {
            PatternElement patternElement;
            Objects.requireNonNull(next);
            int n = 0;
            switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{LiteralPatternElement.class, ChoicePatternElement.class, GroupPatternElement.class}, (Object)patternElement, n)) {
                case 0: {
                    LiteralPatternElement ignored = (LiteralPatternElement)patternElement;
                    String literal = next.toString();
                    for (int i = 0; i < literal.length(); ++i) {
                        if (literal.charAt(i) == ' ') continue;
                        ++length;
                    }
                    break;
                }
                case 1: {
                    ChoicePatternElement choicePatternElement = (ChoicePatternElement)patternElement;
                    int min = Integer.MAX_VALUE;
                    for (PatternElement choice : choicePatternElement.getPatternElements()) {
                        int choiceLen = Keyword.computeMinLength(choice);
                        if (choiceLen >= min) continue;
                        min = choiceLen;
                    }
                    if (min == Integer.MAX_VALUE) break;
                    length += min;
                    break;
                }
                case 2: {
                    GroupPatternElement groupPatternElement = (GroupPatternElement)patternElement;
                    length += Keyword.computeMinLength(groupPatternElement.getPatternElement());
                    break;
                }
            }
            next = next.originalNext;
        }
        return length;
    }

    @Contract(value="_ -> new")
    public static Keyword[] buildKeywords(PatternElement first) {
        return Keyword.buildKeywords(first, true, 0);
    }

    @Contract(value="_, _, _ -> new")
    private static Keyword[] buildKeywords(PatternElement first, boolean starting, int depth) {
        ArrayList<Keyword> keywords = new ArrayList<Keyword>();
        PatternElement next = first;
        while (next != null) {
            Objects.requireNonNull(next);
            int n = 0;
            block6: while (true) {
                PatternElement patternElement;
                switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{LiteralPatternElement.class, ChoicePatternElement.class, GroupPatternElement.class}, (Object)patternElement, n)) {
                    case 0: {
                        LiteralPatternElement ignored = (LiteralPatternElement)patternElement;
                        String literal = next.toString().trim();
                        while (literal.contains("  ")) {
                            literal = literal.replace("  ", " ");
                        }
                        if (literal.isEmpty()) break block6;
                        keywords.add(new SimpleKeyword(literal, starting, next.next == null));
                        break block6;
                    }
                    case 1: {
                        ChoicePatternElement choicePatternElement = (ChoicePatternElement)patternElement;
                        if (depth > 1) {
                            n = 2;
                            continue block6;
                        }
                        boolean finalStarting = starting;
                        int finalDepth = depth;
                        Set<Set<Keyword>> choices = choicePatternElement.getPatternElements().stream().map(element -> Keyword.buildKeywords(element, finalStarting, finalDepth)).map(ImmutableSet::copyOf).collect(Collectors.toSet());
                        if (!choices.stream().noneMatch(Collection::isEmpty)) break block6;
                        keywords.add(new ChoiceKeyword(choices));
                        break block6;
                    }
                    case 2: {
                        GroupPatternElement groupPatternElement = (GroupPatternElement)patternElement;
                        Collections.addAll(keywords, Keyword.buildKeywords(groupPatternElement.getPatternElement(), starting, depth + 1));
                        break block6;
                    }
                }
                break;
            }
            if (!(next instanceof ParseTagPatternElement)) {
                starting = false;
            }
            next = next.originalNext;
        }
        return keywords.toArray(new Keyword[0]);
    }

    private static final class SimpleKeyword
    extends Keyword {
        private final String keyword;
        private final boolean starting;
        private final boolean ending;

        SimpleKeyword(String keyword, boolean starting, boolean ending) {
            this.keyword = keyword;
            this.starting = starting;
            this.ending = ending;
        }

        @Override
        public boolean isPresent(String expr) {
            if (this.starting) {
                return expr.startsWith(this.keyword);
            }
            if (this.ending) {
                return expr.endsWith(this.keyword);
            }
            return expr.contains(this.keyword);
        }

        public int hashCode() {
            return Objects.hash(this.keyword, this.starting, this.ending);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SimpleKeyword)) {
                return false;
            }
            SimpleKeyword simpleKeyword = (SimpleKeyword)obj;
            return this.keyword.equals(simpleKeyword.keyword) && this.starting == simpleKeyword.starting && this.ending == simpleKeyword.ending;
        }

        public String toString() {
            return MoreObjects.toStringHelper((Object)this).add("keyword", (Object)this.keyword).add("starting", this.starting).add("ending", this.ending).toString();
        }
    }

    private static final class ChoiceKeyword
    extends Keyword {
        private final Set<Set<Keyword>> choices;

        ChoiceKeyword(Set<Set<Keyword>> choices) {
            this.choices = choices;
        }

        @Override
        public boolean isPresent(String expr) {
            return this.choices.stream().anyMatch(keywords -> keywords.stream().allMatch(keyword -> keyword.isPresent(expr)));
        }

        public int hashCode() {
            return Arrays.hashCode(this.choices.toArray());
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ChoiceKeyword)) {
                return false;
            }
            ChoiceKeyword choiceKeyword = (ChoiceKeyword)obj;
            return this.choices.equals(choiceKeyword.choices);
        }

        public String toString() {
            return MoreObjects.toStringHelper((Object)this).add("choices", (Object)this.choices.stream().map(Object::toString).collect(Collectors.joining(", "))).toString();
        }
    }
}

