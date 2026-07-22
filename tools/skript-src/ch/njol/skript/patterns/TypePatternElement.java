/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.patterns;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.lang.parser.ExpressionParseCache;
import ch.njol.skript.lang.parser.LiteralParseCache;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.patterns.LiteralPatternElement;
import ch.njol.skript.patterns.MatchResult;
import ch.njol.skript.patterns.PatternElement;
import ch.njol.skript.patterns.SkriptPattern;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class TypePatternElement
extends PatternElement {
    private final ClassInfo<?>[] classes;
    private final boolean[] isPlural;
    private final boolean isNullable;
    private final int flagMask;
    private final int time;
    private final int expressionIndex;

    public TypePatternElement(ClassInfo<?>[] classes, boolean[] isPlural, boolean isNullable, int flagMask, int time, int expressionIndex) {
        this.classes = classes;
        this.isPlural = isPlural;
        this.isNullable = isNullable;
        this.flagMask = flagMask;
        this.time = time;
        this.expressionIndex = expressionIndex;
    }

    public static TypePatternElement fromString(String string, int expressionIndex) {
        int caret = 0;
        int flagMask = -1;
        boolean isNullable = false;
        block5: while (true) {
            switch (string.charAt(caret)) {
                case '-': {
                    isNullable = true;
                    break;
                }
                case '*': {
                    flagMask &= 0xFFFFFFFE;
                    break;
                }
                case '~': {
                    flagMask &= 0xFFFFFFFD;
                    break;
                }
                default: {
                    break block5;
                }
            }
            ++caret;
        }
        int time = 0;
        int timeStart = string.indexOf(64, caret);
        if (timeStart != -1) {
            time = Integer.parseInt(string.substring(timeStart + 1));
            string = string.substring(0, timeStart);
        } else {
            string = string.substring(caret);
        }
        String[] classes = string.split("/");
        ClassInfo[] classInfos = new ClassInfo[classes.length];
        boolean[] isPlural = new boolean[classes.length];
        for (int i = 0; i < classes.length; ++i) {
            Utils.PluralResult p = Utils.isPlural(classes[i]);
            classInfos[i] = Classes.getClassInfo(p.updated());
            isPlural[i] = p.plural();
        }
        return new TypePatternElement(classInfos, isPlural, isNullable, flagMask, time, expressionIndex);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * WARNING - Removed back jump from a try to a catch block - possible behaviour change.
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    @Nullable
    public MatchResult match(String expr, MatchResult matchResult) {
        state = new OffsetState();
        exprOffset = this.initOffset(expr, matchResult, state);
        if (exprOffset == -1) {
            return null;
        }
        exprInfo = this.getExprInfo();
        parseCache = ParserInstance.get().getExpressionParseCache();
        matchBackup = null;
        loopLogBackup = null;
        exprLogBackup = null;
        loopLog = SkriptLogger.startParseLogHandler();
        while (true) {
            block29: {
                block28: {
                    block26: {
                        block27: {
                            try {
                                while (exprOffset != -1) {
                                    loopLog.clear();
                                    substring = expr.substring(matchResult.exprOffset, exprOffset);
                                    effectiveFlags = matchResult.flags & this.flagMask;
                                    cacheKey = new ExpressionParseCache.Failure(substring, effectiveFlags, this.classes, this.isPlural, this.isNullable, this.time);
                                    if (parseCache.contains(cacheKey)) {
                                        exprOffset = this.advanceOffset(expr, exprOffset, matchResult.parseContext, state);
                                        continue;
                                    }
                                    copy = matchResult.copy();
                                    copy.exprOffset = exprOffset;
                                    tailMatch = this.matchNext(expr, copy);
                                    if (tailMatch == null) {
                                        exprOffset = this.advanceOffset(expr, exprOffset, matchResult.parseContext, state);
                                        continue;
                                    }
                                    exprLog = SkriptLogger.startParseLogHandler();
                                    expression = new SkriptParser(substring, effectiveFlags, matchResult.parseContext).parseExpression(exprInfo);
                                    if (expression == null) {
                                        parseCache.add(cacheKey);
                                        exprOffset = this.advanceOffset(expr, exprOffset, matchResult.parseContext, state);
                                    }
                                    ** GOTO lbl-1000
                                    if (exprLog.isStopped()) continue;
                                    exprLog.printError();
                                    continue;
lbl-1000:
                                    // 1 sources

                                    {
                                        if (this.applyTimeState(expression)) ** GOTO lbl-1000
                                        var18_18 = null;
                                    }
                                    if (!exprLog.isStopped()) {
                                        exprLog.printError();
                                    }
                                    if (loopLogBackup == null) break block26;
                                    loopLog.restore(loopLogBackup);
                                    if (TypePatternElement.$assertionsDisabled || exprLogBackup != null) break block27;
                                    throw new AssertionError();
                                }
                                break;
                            }
                            catch (Throwable var20_21) {
                                if (loopLogBackup != null) {
                                    loopLog.restore(loopLogBackup);
                                    if (!TypePatternElement.$assertionsDisabled && exprLogBackup == null) {
                                        throw new AssertionError();
                                    }
                                    exprLogBackup.printLog();
                                }
                                if (loopLog.isStopped() != false) throw var20_21;
                                loopLog.printError();
                                throw var20_21;
                            }
                        }
                        exprLogBackup.printLog();
                    }
                    if (loopLog.isStopped() != false) return var18_18;
                    loopLog.printError();
                    return var18_18;
lbl-1000:
                    // 1 sources

                    {
                        tailMatch.expressions[this.expressionIndex] = expression;
                        if (this.hasUnparsedLiterals(tailMatch)) break block28;
                        exprLog.printLog();
                        loopLog.printLog();
                        var18_19 = tailMatch;
                    }
                    if (!exprLog.isStopped()) {
                        exprLog.printError();
                    }
                    if (loopLogBackup != null) {
                        loopLog.restore(loopLogBackup);
                        if (!TypePatternElement.$assertionsDisabled && exprLogBackup == null) {
                            throw new AssertionError();
                        }
                        exprLogBackup.printLog();
                    }
                    if (loopLog.isStopped() != false) return var18_19;
                    loopLog.printError();
                    return var18_19;
                }
                ** try [egrp 6[TRYBLOCK] [3 : 433->456)] { 
lbl81:
                // 1 sources

                if (matchBackup == null) {
                    matchBackup = tailMatch;
                    loopLogBackup = loopLog.backup();
                    exprLogBackup = exprLog.backup();
                }
                break block29;
lbl86:
                // 1 sources

                finally {
                    if (!exprLog.isStopped()) {
                        exprLog.printError();
                    }
                }
            }
            exprOffset = this.advanceOffset(expr, exprOffset, matchResult.parseContext, state);
        }
        if (loopLogBackup != null) {
            loopLog.restore(loopLogBackup);
            if (!TypePatternElement.$assertionsDisabled && exprLogBackup == null) {
                throw new AssertionError();
            }
            exprLogBackup.printLog();
        }
        if (loopLog.isStopped() != false) return matchBackup;
        loopLog.printError();
        return matchBackup;
    }

    private boolean applyTimeState(Expression<?> expression) {
        if (this.time == 0) {
            return true;
        }
        if (expression instanceof Literal) {
            return false;
        }
        if (ParserInstance.get().getHasDelayBefore() == Kleenean.TRUE) {
            Skript.error("Cannot use time states after the event has already passed");
            return false;
        }
        if (!expression.setTime(this.time)) {
            Skript.error(String.valueOf(expression) + " does not have a " + (this.time == EventValue.Time.PAST.value() ? "past" : "future") + " state");
            return false;
        }
        return true;
    }

    private boolean hasUnparsedLiterals(@NotNull MatchResult matchResult) {
        LiteralParseCache literalCache = ParserInstance.get().getLiteralParseCache();
        for (int i = this.expressionIndex + 1; i < matchResult.expressions.length; ++i) {
            Expression<?> expression = matchResult.expressions[i];
            if (!(expression instanceof UnparsedLiteral)) continue;
            UnparsedLiteral unparsed = (UnparsedLiteral)expression;
            LiteralParseCache.Failure key = new LiteralParseCache.Failure(unparsed.getData(), matchResult.parseContext);
            if (literalCache.contains(key)) {
                return true;
            }
            if (Classes.parse(unparsed.getData(), Object.class, matchResult.parseContext) != null) continue;
            literalCache.add(key);
            return true;
        }
        return false;
    }

    private int initOffset(String expr, MatchResult matchResult, OffsetState state) {
        int offset;
        if (this.next == null) {
            return expr.length();
        }
        if (!(this.next instanceof LiteralPatternElement)) {
            state.nextLiteral = null;
            return SkriptParser.next(expr, matchResult.exprOffset, matchResult.parseContext);
        }
        state.nextLiteral = this.next.toString();
        state.nextLiteralIsWhitespace = state.nextLiteral.trim().isEmpty();
        if (!state.nextLiteralIsWhitespace) {
            state.nextLiteral = state.nextLiteral.stripTrailing();
        }
        if ((offset = SkriptParser.nextOccurrence(expr, state.nextLiteral, matchResult.exprOffset, matchResult.parseContext, false)) == -1 && state.nextLiteralIsWhitespace) {
            state.nextLiteral = null;
            offset = SkriptParser.next(expr, matchResult.exprOffset, matchResult.parseContext);
        }
        return offset;
    }

    private int advanceOffset(String expr, int currentOffset, ParseContext parseContext, OffsetState state) {
        if (state.nextLiteral == null) {
            return SkriptParser.next(expr, currentOffset, parseContext);
        }
        int newOffset = SkriptParser.nextOccurrence(expr, state.nextLiteral, currentOffset + 1, parseContext, false);
        if (newOffset == -1 && state.nextLiteralIsWhitespace) {
            state.nextLiteral = null;
            return SkriptParser.next(expr, currentOffset, parseContext);
        }
        return newOffset;
    }

    @Override
    public String toString() {
        return this.toString(SkriptPattern.StringificationProperties.DEFAULT);
    }

    @Override
    public String toString(SkriptPattern.StringificationProperties properties) {
        StringBuilder stringBuilder = new StringBuilder().append("%");
        if (!properties.excludeTypeFlags()) {
            if (this.isNullable) {
                stringBuilder.append("-");
            }
            if (this.flagMask != -1) {
                if ((this.flagMask & 2) == 0) {
                    stringBuilder.append("~");
                } else if ((this.flagMask & 1) == 0) {
                    stringBuilder.append("*");
                }
            }
        }
        for (int i = 0; i < this.classes.length; ++i) {
            String codeName = this.classes[i].getCodeName();
            if (this.isPlural[i]) {
                stringBuilder.append(Utils.toEnglishPlural(codeName));
            } else {
                stringBuilder.append(codeName);
            }
            if (i == this.classes.length - 1) continue;
            stringBuilder.append("/");
        }
        if (!properties.excludeTypeFlags() && this.time != 0) {
            stringBuilder.append("@").append(this.time);
        }
        return stringBuilder.append("%").toString();
    }

    public SkriptParser.ExprInfo getExprInfo() {
        SkriptParser.ExprInfo exprInfo = new SkriptParser.ExprInfo(this.classes.length);
        for (int i = 0; i < this.classes.length; ++i) {
            exprInfo.classes[i] = this.classes[i];
            exprInfo.isPlural[i] = this.isPlural[i];
        }
        exprInfo.isOptional = this.isNullable;
        exprInfo.flagMask = this.flagMask;
        exprInfo.time = this.time;
        return exprInfo;
    }

    @Override
    public Set<String> getCombinations(boolean clean) {
        HashSet<String> combinations = new HashSet<String>();
        if (!clean || this.flagMask == 2) {
            combinations.add(this.toString());
        } else {
            combinations.add("%*%");
        }
        return combinations;
    }

    private static final class OffsetState {
        @Nullable
        String nextLiteral;
        boolean nextLiteralIsWhitespace;

        private OffsetState() {
        }
    }
}

