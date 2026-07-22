/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.KeyedIterableExpression;
import ch.njol.skript.lang.KeyedValue;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.sections.SecLoop;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import java.lang.reflect.Array;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Loop value")
@Description(value={"Returns the previous, current, or next looped value."})
@Example.Examples(value={@Example(value="# Countdown\nloop 10 times:\n\tmessage \"%11 - loop-number%\"\n\twait a second\n"), @Example(value="# Generate a 10x10 floor made of randomly colored wool below the player\nloop blocks from the block below the player to the block 10 east of the block below the player:\n\tloop blocks from the loop-block to the block 10 north of the loop-block:\n\t\tset loop-block-2 to any wool\n"), @Example(value="loop {top-balances::*}:\n\tloop-iteration <= 10\n\tsend \"#%loop-iteration% %loop-index% has $%loop-value%\"\n"), @Example(value="loop shuffled (integers between 0 and 8):\n\tif all:\n\t\tprevious loop-value = 1\n\t\tloop-value = 4\n\t\tnext loop-value = 8\n\tthen:\n\t\t kill all players\n")})
@Since(value={"1.0, 2.8.0 (loop-counter), 2.10 (previous, next)"})
public class ExprLoopValue
extends SimpleExpression<Object> {
    private static final LoopState[] loopStates = LoopState.values();
    private String name;
    private SecLoop loop;
    boolean isKeyedLoop = false;
    boolean isIndex = false;
    private LoopState selectedState;
    private static final Pattern LOOP_PATTERN;

    @Override
    public boolean init(Expression<?>[] vars, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.selectedState = loopStates[matchedPattern];
        this.name = parser.expr;
        String loopOf = parser.regexes.get(0).group();
        int expectedDepth = -1;
        Matcher m = LOOP_PATTERN.matcher(loopOf);
        if (m.matches()) {
            loopOf = m.group(1);
            expectedDepth = Utils.parseInt(m.group(2));
        }
        if ("counter".equalsIgnoreCase(loopOf) || "iteration".equalsIgnoreCase(loopOf)) {
            return false;
        }
        Class<?> expectedClass = Classes.getClassFromUserInput(loopOf);
        int candidateDepth = 1;
        SecLoop loop = null;
        for (SecLoop candidate : this.getParser().getCurrentSections(SecLoop.class)) {
            if ((expectedClass == null || !expectedClass.isAssignableFrom(candidate.getLoopedExpression().getReturnType())) && !"value".equalsIgnoreCase(loopOf) && !candidate.getLoopedExpression().isLoopOf(loopOf)) continue;
            if (candidateDepth < expectedDepth) {
                ++candidateDepth;
                continue;
            }
            if (loop != null) {
                Skript.error("There are multiple loops that match loop-" + loopOf + ". Use loop-" + loopOf + "-1/2/3/etc. to specify which loop's value you want.");
                return false;
            }
            loop = candidate;
            if (candidateDepth != expectedDepth) continue;
            break;
        }
        if (loop == null) {
            Skript.error("There's no loop that matches 'loop-" + loopOf + "'");
            return false;
        }
        if (this.selectedState == LoopState.NEXT && !loop.supportsPeeking()) {
            Skript.error("The expression '" + loop.getExpression().toString() + "' does not allow the usage of 'next loop-" + loopOf + "'.");
            return false;
        }
        if (loop.isKeyedLoop()) {
            this.isKeyedLoop = true;
            if (((KeyedIterableExpression)loop.getLoopedExpression()).isIndexLoop(loopOf)) {
                this.isIndex = true;
            }
        }
        this.loop = loop;
        return true;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        if (this.isIndex) {
            return String.class;
        }
        return this.loop.getLoopedExpression().getReturnType();
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        if (this.isIndex) {
            return new Class[]{String.class};
        }
        return this.loop.getLoopedExpression().possibleReturnTypes();
    }

    @Override
    public boolean canReturn(Class<?> returnType) {
        if (this.isIndex) {
            return super.canReturn(returnType);
        }
        return this.loop.getLoopedExpression().canReturn(returnType);
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        if (this.isKeyedLoop) {
            KeyedValue value = (KeyedValue)(switch (this.selectedState.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> this.loop.getCurrent(event);
                case 1 -> this.loop.getNext(event);
                case 2 -> this.loop.getPrevious(event);
            });
            if (value == null) {
                return null;
            }
            if (this.isIndex) {
                return new String[]{value.key()};
            }
            Object[] one = (Object[])Array.newInstance(this.getReturnType(), 1);
            one[0] = value.value();
            return one;
        }
        Object[] one = (Object[])Array.newInstance(this.getReturnType(), 1);
        one[0] = switch (this.selectedState.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> this.loop.getCurrent(event);
            case 1 -> this.loop.getNext(event);
            case 2 -> this.loop.getPrevious(event);
        };
        return one;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (event == null) {
            return this.name;
        }
        if (this.isKeyedLoop) {
            KeyedValue value = (KeyedValue)(switch (this.selectedState.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> this.loop.getCurrent(event);
                case 1 -> this.loop.getNext(event);
                case 2 -> this.loop.getPrevious(event);
            });
            if (value == null) {
                return Classes.getDebugMessage(null);
            }
            return this.isIndex ? "\"" + value.key() + "\"" : Classes.getDebugMessage(value.value());
        }
        return Classes.getDebugMessage(switch (this.selectedState.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> this.loop.getCurrent(event);
            case 1 -> this.loop.getNext(event);
            case 2 -> this.loop.getPrevious(event);
        });
    }

    static {
        String[] patterns = new String[loopStates.length];
        for (LoopState state : loopStates) {
            patterns[state.ordinal()] = "[the] " + state.pattern + " loop-<.+>";
        }
        Skript.registerExpression(ExprLoopValue.class, Object.class, ExpressionType.SIMPLE, patterns);
        LOOP_PATTERN = Pattern.compile("^(.+)-(\\d+)$");
    }

    static enum LoopState {
        CURRENT("[current]"),
        NEXT("next"),
        PREVIOUS("previous");

        private final String pattern;

        private LoopState(String pattern) {
            this.pattern = pattern;
        }
    }
}

