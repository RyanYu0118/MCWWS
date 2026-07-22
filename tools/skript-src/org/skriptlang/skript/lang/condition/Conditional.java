/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.condition;

import ch.njol.skript.lang.Debuggable;
import ch.njol.util.Kleenean;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.condition.CompoundConditional;
import org.skriptlang.skript.lang.condition.DNFConditionalBuilder;

public interface Conditional<T>
extends Debuggable {
    @Contract(pure=true)
    public Kleenean evaluate(T var1);

    @Contract(pure=true)
    default public Kleenean evaluate(T context, @Nullable Map<Conditional<T>, Kleenean> cache) {
        if (cache == null) {
            return this.evaluate(context);
        }
        return cache.computeIfAbsent(this, cond -> cond.evaluate(context));
    }

    @Contract(pure=true)
    default public Kleenean evaluateAnd(Conditional<T> other, T context) {
        return this.evaluateAnd(other.evaluate(context, null), context, null);
    }

    @Contract(pure=true)
    default public Kleenean evaluateAnd(Conditional<T> other, T context, @Nullable Map<Conditional<T>, Kleenean> cache) {
        return this.evaluateAnd(other.evaluate(context, cache), context, cache);
    }

    @Contract(pure=true)
    default public Kleenean evaluateAnd(Kleenean other, T context) {
        return this.evaluateAnd(other, context, null);
    }

    @Contract(pure=true)
    default public Kleenean evaluateAnd(Kleenean other, T context, @Nullable Map<Conditional<T>, Kleenean> cache) {
        if (other.isFalse()) {
            return other;
        }
        return other.and(this.evaluate(context, cache));
    }

    @Contract(pure=true)
    default public Kleenean evaluateOr(Conditional<T> other, T context) {
        return this.evaluateOr(other.evaluate(context, null), context, null);
    }

    @Contract(pure=true)
    default public Kleenean evaluateOr(Conditional<T> other, T context, @Nullable Map<Conditional<T>, Kleenean> cache) {
        return this.evaluateOr(other.evaluate(context, cache), context, cache);
    }

    @Contract(pure=true)
    default public Kleenean evaluateOr(Kleenean other, T context) {
        return this.evaluateOr(other, context, null);
    }

    @Contract(pure=true)
    default public Kleenean evaluateOr(Kleenean other, T context, @Nullable Map<Conditional<T>, Kleenean> cache) {
        if (other.isTrue()) {
            return other;
        }
        return other.or(this.evaluate(context, cache));
    }

    @Contract(pure=true)
    default public Kleenean evaluateNot(T context) {
        return this.evaluateNot(context, null);
    }

    @Contract(pure=true)
    default public Kleenean evaluateNot(T context, @Nullable Map<Conditional<T>, Kleenean> cache) {
        return this.evaluate(context, cache).not();
    }

    @Contract(value="_, _ -> new")
    public static <T> Conditional<T> compound(Operator operator, Collection<Conditional<T>> conditionals) {
        Preconditions.checkArgument((operator != Operator.NOT ? 1 : 0) != 0, (Object)"Cannot combine conditionals using NOT!");
        return new CompoundConditional<T>(operator, conditionals);
    }

    @Contract(value="_, _ -> new")
    public static <T> Conditional<T> compound(Operator operator, Conditional<T> ... conditionals) {
        return Conditional.compound(operator, List.of(conditionals));
    }

    @Contract(value="_ -> new", pure=true)
    @NotNull
    public static <T> DNFConditionalBuilder<T> builderDNF(Class<T> ignoredContextClass) {
        return new DNFConditionalBuilder();
    }

    @Contract(value="_ -> new")
    @NotNull
    public static <T> DNFConditionalBuilder<T> builderDNF(Conditional<T> conditional) {
        return new DNFConditionalBuilder<T>(conditional);
    }

    public static <T> Conditional<T> negate(Conditional<T> conditional) {
        if (!(conditional instanceof CompoundConditional)) {
            return new CompoundConditional(Operator.NOT, conditional);
        }
        CompoundConditional compound = (CompoundConditional)conditional;
        return switch (compound.getOperator().ordinal()) {
            default -> throw new MatchException(null, null);
            case 2 -> compound.getConditionals().getFirst();
            case 0 -> {
                ArrayList newConditionals = new ArrayList();
                for (Conditional cond : compound.getConditionals()) {
                    newConditionals.add(Conditional.negate(cond));
                }
                yield new CompoundConditional(Operator.OR, newConditionals);
            }
            case 1 -> {
                ArrayList newConditionals = new ArrayList();
                for (Conditional cond : compound.getConditionals()) {
                    newConditionals.add(Conditional.negate(cond));
                }
                yield new CompoundConditional(Operator.AND, newConditionals);
            }
        };
    }

    public static enum Operator {
        AND("&&"),
        OR("||"),
        NOT("!");

        private final String symbol;

        private Operator(String symbol) {
            this.symbol = symbol;
        }

        String getSymbol() {
            return this.symbol;
        }
    }
}

