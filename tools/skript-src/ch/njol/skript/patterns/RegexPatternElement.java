/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.patterns;

import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.patterns.MatchResult;
import ch.njol.skript.patterns.PatternElement;
import ch.njol.skript.patterns.SkriptPattern;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

public class RegexPatternElement
extends PatternElement {
    private final Pattern pattern;

    public RegexPatternElement(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    @Nullable
    public MatchResult match(String expr, MatchResult matchResult) {
        int exprIndex = matchResult.exprOffset;
        try (ParseLogHandler log = SkriptLogger.startParseLogHandler();){
            Matcher matcher = this.pattern.matcher(expr);
            int nextExprOffset = SkriptParser.next(expr, exprIndex, matchResult.parseContext);
            while (nextExprOffset != -1) {
                log.clear();
                matcher.region(exprIndex, nextExprOffset);
                if (matcher.matches()) {
                    MatchResult matchResultCopy = matchResult.copy();
                    matchResultCopy.exprOffset = nextExprOffset;
                    MatchResult newMatchResult = this.matchNext(expr, matchResultCopy);
                    if (newMatchResult != null) {
                        newMatchResult.regexResults.addFirst(matcher.toMatchResult());
                        log.printLog();
                        MatchResult matchResult2 = newMatchResult;
                        return matchResult2;
                    }
                }
                nextExprOffset = SkriptParser.next(expr, nextExprOffset, matchResult.parseContext);
            }
            log.printError(null);
            MatchResult matchResult3 = null;
            return matchResult3;
        }
    }

    @Override
    public String toString() {
        return this.toString(SkriptPattern.StringificationProperties.DEFAULT);
    }

    @Override
    public String toString(SkriptPattern.StringificationProperties properties) {
        return "<" + String.valueOf(this.pattern) + ">";
    }

    @Override
    public Set<String> getCombinations(boolean clean) {
        return new HashSet<String>(Set.of(this.toString()));
    }
}

