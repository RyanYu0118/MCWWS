/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.patterns;

import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.patterns.ChoicePatternElement;
import ch.njol.skript.patterns.GroupPatternElement;
import ch.njol.skript.patterns.LiteralPatternElement;
import ch.njol.skript.patterns.MalformedPatternException;
import ch.njol.skript.patterns.OptionalPatternElement;
import ch.njol.skript.patterns.ParseTagPatternElement;
import ch.njol.skript.patterns.PatternElement;
import ch.njol.skript.patterns.RegexPatternElement;
import ch.njol.skript.patterns.SkriptPattern;
import ch.njol.skript.patterns.TypePatternElement;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.jetbrains.annotations.Nullable;

public class PatternCompiler {
    private static PatternElement getEmpty() {
        return new LiteralPatternElement("");
    }

    public static SkriptPattern compile(String pattern) throws MalformedPatternException {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        try {
            PatternElement first = PatternCompiler.compile(pattern, atomicInteger);
            return new SkriptPattern(first, atomicInteger.get());
        }
        catch (MalformedPatternException e) {
            throw e;
        }
        catch (RuntimeException e) {
            throw new MalformedPatternException(pattern, "caught exception while compiling pattern", e);
        }
    }

    static PatternElement compile(String pattern, AtomicInteger expressionOffset) {
        StringBuilder literalBuilder = new StringBuilder();
        PatternElement first = null;
        for (int i = 0; i < pattern.length(); ++i) {
            char c = pattern.charAt(i);
            if (c == '[') {
                if (!literalBuilder.isEmpty()) {
                    first = PatternCompiler.appendElement(first, new LiteralPatternElement(literalBuilder.toString()));
                    literalBuilder = new StringBuilder();
                }
                int end = SkriptParser.nextBracket(pattern, ']', c, i + 1, true);
                PatternElement patternElement = PatternCompiler.compile(pattern.substring(i + 1, end), expressionOffset);
                first = PatternCompiler.appendElement(first, new OptionalPatternElement(patternElement));
                i = end;
                continue;
            }
            if (c == '(') {
                if (!literalBuilder.isEmpty()) {
                    first = PatternCompiler.appendElement(first, new LiteralPatternElement(literalBuilder.toString()));
                    literalBuilder = new StringBuilder();
                }
                int end = SkriptParser.nextBracket(pattern, ')', c, i + 1, true);
                PatternElement patternElement = PatternCompiler.compile(pattern.substring(i + 1, end), expressionOffset);
                first = PatternCompiler.appendElement(first, new GroupPatternElement(patternElement));
                i = end;
                continue;
            }
            if (c == '|') {
                ChoicePatternElement choicePatternElement;
                if (!literalBuilder.isEmpty()) {
                    first = PatternCompiler.appendElement(first, new LiteralPatternElement(literalBuilder.toString()));
                    literalBuilder = new StringBuilder();
                }
                PatternElement prevFirst = first;
                if (first instanceof ChoicePatternElement) {
                    choicePatternElement = (ChoicePatternElement)first;
                } else {
                    choicePatternElement = new ChoicePatternElement();
                    first = choicePatternElement;
                    choicePatternElement.add(prevFirst != null ? prevFirst : PatternCompiler.getEmpty());
                }
                choicePatternElement.add(PatternCompiler.getEmpty());
                continue;
            }
            if (c == '\u00a6' || c == ':') {
                ParseTagPatternElement parseTagPatternElement;
                String tag = literalBuilder.toString();
                literalBuilder = new StringBuilder();
                if (c == '\u00a6') {
                    int mark;
                    try {
                        mark = Integer.parseInt(tag);
                    }
                    catch (NumberFormatException e) {
                        throw new MalformedPatternException(pattern, "invalid parse mark at " + i, e);
                    }
                    parseTagPatternElement = new ParseTagPatternElement(mark);
                } else {
                    parseTagPatternElement = new ParseTagPatternElement(tag);
                }
                first = PatternCompiler.appendElement(first, parseTagPatternElement);
                continue;
            }
            if (c == '%') {
                int end;
                if (!literalBuilder.isEmpty()) {
                    first = PatternCompiler.appendElement(first, new LiteralPatternElement(literalBuilder.toString()));
                    literalBuilder = new StringBuilder();
                }
                if ((end = pattern.indexOf(37, i + 1)) == -1) {
                    throw new MalformedPatternException(pattern, "single percentage sign at " + i);
                }
                int exprOffset = expressionOffset.getAndIncrement();
                TypePatternElement typePatternElement = TypePatternElement.fromString(pattern.substring(i + 1, end), exprOffset);
                first = PatternCompiler.appendElement(first, typePatternElement);
                i = end;
                continue;
            }
            if (c == '<') {
                Pattern regexPattern;
                int end;
                if (!literalBuilder.isEmpty()) {
                    first = PatternCompiler.appendElement(first, new LiteralPatternElement(literalBuilder.toString()));
                    literalBuilder = new StringBuilder();
                }
                if ((end = pattern.indexOf(62, i + 1)) == -1) {
                    throw new MalformedPatternException(pattern, "missing closing regex bracket '>' at " + i);
                }
                try {
                    regexPattern = Pattern.compile(pattern.substring(i + 1, end));
                }
                catch (PatternSyntaxException e) {
                    throw new MalformedPatternException(pattern, "invalid regex <" + pattern.substring(i + 1, end) + "> at " + i, e);
                }
                first = PatternCompiler.appendElement(first, new RegexPatternElement(regexPattern));
                i = end;
                continue;
            }
            if (c == '\\') {
                literalBuilder.append(pattern.charAt(++i));
                continue;
            }
            literalBuilder.append(c);
        }
        if (!literalBuilder.isEmpty()) {
            first = PatternCompiler.appendElement(first, new LiteralPatternElement(literalBuilder.toString()));
        }
        if (first == null) {
            return PatternCompiler.getEmpty();
        }
        return first;
    }

    private static PatternElement appendElement(@Nullable PatternElement first, PatternElement next) {
        block6: {
            block5: {
                if (first == null) break block5;
                if (!(first instanceof LiteralPatternElement)) break block6;
                LiteralPatternElement literalPatternElement = (LiteralPatternElement)first;
                if (first.next != null || !literalPatternElement.isEmpty()) break block6;
            }
            return next;
        }
        if (first instanceof ChoicePatternElement) {
            ChoicePatternElement choicePatternElement = (ChoicePatternElement)first;
            PatternElement last = choicePatternElement.getLast();
            choicePatternElement.setLast(PatternCompiler.appendElement(last, next));
            return first;
        }
        PatternElement last = first;
        while (last.next != null) {
            last = last.next;
        }
        last.setNext(next);
        last.originalNext = next;
        return first;
    }
}

