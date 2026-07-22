/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.patterns;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.patterns.ChoicePatternElement;
import ch.njol.skript.patterns.GroupPatternElement;
import ch.njol.skript.patterns.Keyword;
import ch.njol.skript.patterns.MatchResult;
import ch.njol.skript.patterns.OptionalPatternElement;
import ch.njol.skript.patterns.PatternElement;
import ch.njol.skript.patterns.TypePatternElement;
import com.google.common.collect.ImmutableList;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public class SkriptPattern {
    private final PatternElement first;
    private final int expressionAmount;
    private final Keyword[] keywords;
    private final int minLength;
    @Nullable
    private List<TypePatternElement> types;

    public SkriptPattern(PatternElement first, int expressionAmount) {
        this.first = first;
        this.expressionAmount = expressionAmount;
        this.keywords = Keyword.buildKeywords(first);
        this.minLength = Keyword.computeMinLength(first);
    }

    @Nullable
    public MatchResult match(String expr, int flags, ParseContext parseContext) {
        String lowerExpr = expr.toLowerCase(Locale.ENGLISH);
        if (lowerExpr.length() < this.minLength) {
            return null;
        }
        for (Keyword keyword : this.keywords) {
            if (keyword.isPresent(lowerExpr)) continue;
            return null;
        }
        expr = expr.trim();
        MatchResult matchResult = new MatchResult();
        matchResult.source = this;
        matchResult.expr = expr;
        matchResult.expressions = new Expression[this.expressionAmount];
        matchResult.parseContext = parseContext;
        matchResult.flags = flags;
        return this.first.match(expr, matchResult);
    }

    @Nullable
    public MatchResult match(String expr) {
        return this.match(expr, 3, ParseContext.DEFAULT);
    }

    public int countTypes() {
        return this.expressionAmount;
    }

    public int countNonNullTypes() {
        return SkriptPattern.countNonNullTypes(this.first);
    }

    private static int countNonNullTypes(PatternElement patternElement) {
        int count = 0;
        while (patternElement != null) {
            PatternElement patternElement2;
            Objects.requireNonNull(patternElement);
            int n = 0;
            switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{ChoicePatternElement.class, GroupPatternElement.class, OptionalPatternElement.class, TypePatternElement.class}, (Object)patternElement2, n)) {
                case 0: {
                    ChoicePatternElement choicePatternElement = (ChoicePatternElement)patternElement2;
                    int max = 0;
                    for (PatternElement component : choicePatternElement.getPatternElements()) {
                        int componentCount = SkriptPattern.countNonNullTypes(component);
                        if (componentCount <= max) continue;
                        max = componentCount;
                    }
                    count += max;
                    break;
                }
                case 1: {
                    GroupPatternElement groupPatternElement = (GroupPatternElement)patternElement2;
                    count += SkriptPattern.countNonNullTypes(groupPatternElement.getPatternElement());
                    break;
                }
                case 2: {
                    OptionalPatternElement optionalPatternElement = (OptionalPatternElement)patternElement2;
                    count += SkriptPattern.countNonNullTypes(optionalPatternElement.getPatternElement());
                    break;
                }
                case 3: {
                    TypePatternElement ignored = (TypePatternElement)patternElement2;
                    ++count;
                    break;
                }
            }
            patternElement = patternElement.originalNext;
        }
        return count;
    }

    public <T extends PatternElement> List<T> getElements(Class<T> type) {
        if (type == TypePatternElement.class) {
            if (this.types == null) {
                this.types = ImmutableList.copyOf(SkriptPattern.getElements(TypePatternElement.class, this.first, new ArrayList()));
            }
            return this.types;
        }
        return SkriptPattern.getElements(type, this.first, new ArrayList());
    }

    private static <T extends PatternElement> List<T> getElements(Class<T> type, PatternElement element, List<T> elements) {
        while (element != null) {
            if (element instanceof ChoicePatternElement) {
                ChoicePatternElement choicePatternElement = (ChoicePatternElement)element;
                choicePatternElement.getPatternElements().forEach(e -> SkriptPattern.getElements(type, e, elements));
            } else if (element instanceof GroupPatternElement) {
                GroupPatternElement groupPatternElement = (GroupPatternElement)element;
                SkriptPattern.getElements(type, groupPatternElement.getPatternElement(), elements);
            } else if (element instanceof OptionalPatternElement) {
                OptionalPatternElement optionalPatternElement = (OptionalPatternElement)element;
                SkriptPattern.getElements(type, optionalPatternElement.getPatternElement(), elements);
            } else if (type.isInstance(element)) {
                elements.add(element);
            }
            element = element.originalNext;
        }
        return elements;
    }

    public String toString(StringificationProperties properties) {
        return this.first.toFullString(properties);
    }

    public String toString() {
        return this.toString(StringificationProperties.DEFAULT);
    }

    public static interface StringificationProperties {
        public static final StringificationProperties DEFAULT = StringificationProperties.builder().build();

        public static Builder builder() {
            return new StringificationPropertiesImpl.BuilderImpl();
        }

        public boolean excludeParseTags();

        public boolean excludeTypeFlags();

        public static interface Builder {
            public Builder excludeParseTags();

            public Builder excludeTypeFlags();

            public StringificationProperties build();
        }
    }

    private record StringificationPropertiesImpl(boolean excludeParseTags, boolean excludeTypeFlags) implements StringificationProperties
    {

        private static class BuilderImpl
        implements StringificationProperties.Builder {
            private boolean excludeParseTags = false;
            private boolean excludeTypeFlags = false;

            private BuilderImpl() {
            }

            @Override
            public StringificationProperties.Builder excludeParseTags() {
                this.excludeParseTags = true;
                return this;
            }

            @Override
            public StringificationProperties.Builder excludeTypeFlags() {
                this.excludeTypeFlags = true;
                return this;
            }

            @Override
            public StringificationProperties build() {
                return new StringificationPropertiesImpl(this.excludeParseTags, this.excludeTypeFlags);
            }
        }
    }
}

