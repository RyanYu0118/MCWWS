/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.classes.Changer$ChangeMode
 *  ch.njol.skript.classes.Changer$ChangerUtils
 *  ch.njol.skript.expressions.base.PropertyExpression
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.util.SimpleExpression
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 */
package com.btk5h.skriptmirror.skript;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.Arrays;
import org.bukkit.event.Event;

public class ExprBits
extends SimpleExpression<Number> {
    private Expression<Number> numbers;
    private Expression<Number> from;
    private Expression<Number> to;

    protected Number[] get(Event e) {
        Number f = (Number)this.from.getSingle(e);
        Number t = (Number)this.to.getSingle(e);
        if (f == null || t == null) {
            return null;
        }
        long mask = ExprBits.getRangeMaskIndexed(f.intValue(), t.intValue());
        return (Number[])Arrays.stream((Number[])this.numbers.getArray(e)).map(l -> (l.longValue() & mask) >>> f.intValue()).toArray(Number[]::new);
    }

    private static long getRangeMaskOrdinal(int from) {
        if (from <= 0 || from >= 64) {
            return 0L;
        }
        return (1L << from) - 1L;
    }

    private static long getRangeMaskOrdinal(int from, int to) {
        if (from > to) {
            return 0L;
        }
        return ExprBits.getRangeMaskOrdinal(to) - ExprBits.getRangeMaskOrdinal(from - 1);
    }

    private static long getRangeMaskIndexed(int from, int to) {
        return ExprBits.getRangeMaskOrdinal(from + 1, to + 1);
    }

    public boolean isSingle() {
        return this.numbers.isSingle();
    }

    public String toString(Event e, boolean debug) {
        return String.format("the bits %s to %s of %s", this.from.toString(e, debug), this.to.toString(e, debug), this.numbers.toString(e, debug));
    }

    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (this.isSingle() && (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE) && Changer.ChangerUtils.acceptsChange(this.numbers, (Changer.ChangeMode)Changer.ChangeMode.SET, (Class[])new Class[]{Number.class})) {
            return new Class[]{Number.class, Boolean.class};
        }
        return null;
    }

    public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
        Number num = (Number)this.numbers.getSingle(e);
        Number f = (Number)this.from.getSingle(e);
        Number t = (Number)this.to.getSingle(e);
        if (num == null || f == null || t == null) {
            return;
        }
        long mask = ExprBits.getRangeMaskIndexed(f.intValue(), t.intValue());
        long number = num.longValue();
        switch (mode) {
            case SET: {
                if (delta[0] instanceof Number) {
                    number &= mask ^ 0xFFFFFFFFFFFFFFFFL;
                    mask &= ((Number)delta[0]).longValue() << f.intValue();
                } else if (delta[0] instanceof Boolean) {
                    if (!((Boolean)delta[0]).booleanValue()) {
                        mask ^= 0xFFFFFFFFFFFFFFFFL;
                    }
                } else {
                    throw new IllegalStateException();
                }
                number |= mask;
                break;
            }
            case DELETE: {
                number &= mask ^ 0xFFFFFFFFFFFFFFFFL;
                break;
            }
            default: {
                throw new IllegalStateException();
            }
        }
        this.numbers.change(e, new Object[]{number}, Changer.ChangeMode.SET);
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.numbers = exprs[matchedPattern == 1 ? 0 : 3];
        this.from = exprs[parseResult.mark + matchedPattern];
        this.to = parseResult.mark == 0 ? this.from : exprs[2 + matchedPattern];
        return true;
    }

    static {
        PropertyExpression.register(ExprBits.class, Number.class, (String)"(0\u00a6bit %-number%|1\u00a6bit(s| range) [from] %-number%( to |[ ]-[ ])%-number%)", (String)"numbers");
    }
}

