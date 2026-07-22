/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterators
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;
import com.google.common.collect.Iterators;
import java.util.Iterator;
import java.util.stream.LongStream;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="X Times")
@Description(value={"Integers between 1 and X, used in loops to loop X times."})
@Example(value="loop 20 times:\n\tbroadcast \"%21 - loop-number% seconds left..\"\n\twait 1 second\n")
@Since(value={"1.4.6"})
public class ExprTimes
extends SimpleExpression<Long> {
    private Expression<Number> end;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Expression<Integer> expression = this.end = matchedPattern == 0 ? exprs[0] : new SimpleLiteral<Integer>(matchedPattern, false);
        if (this.end instanceof Literal) {
            int amount = ((Number)((Literal)this.end).getSingle()).intValue();
            if (amount == 0 && this.isInLoop()) {
                Skript.warning("Looping zero times makes the code inside of the loop useless");
            } else if (amount == 1 & this.isInLoop()) {
                Skript.warning("Since you're looping exactly one time, you could simply remove the loop instead");
            } else if (amount < 0) {
                if (this.isInLoop()) {
                    Skript.error("Looping a negative amount of times is impossible");
                } else {
                    Skript.error("The times expression only supports positive numbers");
                }
                return false;
            }
        }
        return true;
    }

    private boolean isInLoop() {
        Node node = SkriptLogger.getNode();
        if (node == null) {
            return false;
        }
        String key = node.getKey();
        if (key == null) {
            return false;
        }
        return key.startsWith("loop ");
    }

    @Nullable
    protected Long[] get(Event e) {
        Iterator<? extends Long> iter = this.iterator(e);
        if (iter == null) {
            return null;
        }
        return (Long[])Iterators.toArray(iter, Long.class);
    }

    @Override
    @Nullable
    public Iterator<? extends Long> iterator(Event e) {
        Number end = this.end.getSingle(e);
        if (end == null) {
            return null;
        }
        long fixed = (long)(end.doubleValue() + 1.0E-10);
        return LongStream.range(1L, fixed + 1L).iterator();
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    public Expression<? extends Long> simplify() {
        return this;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.end.toString(e, debug) + " times";
    }

    static {
        Skript.registerExpression(ExprTimes.class, Long.class, ExpressionType.SIMPLE, "%number% time[s]", "once", "twice", "thrice");
    }
}

