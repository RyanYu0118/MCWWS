/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.lang.condition;

import ch.njol.util.Kleenean;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.lang.condition.Conditional;

class CompoundConditional<T>
implements Conditional<T> {
    private final LinkedHashSet<Conditional<T>> componentConditionals = new LinkedHashSet();
    private final Conditional.Operator operator;
    private boolean useCache;

    CompoundConditional(Conditional.Operator operator, @NotNull Collection<Conditional<T>> conditionals) {
        Preconditions.checkArgument((!conditionals.isEmpty() ? 1 : 0) != 0, (Object)"CompoundConditionals must contain at least 1 component conditional.");
        Preconditions.checkArgument((operator != Conditional.Operator.NOT || conditionals.size() == 1 ? 1 : 0) != 0, (Object)"The NOT operator cannot be applied to multiple Conditionals.");
        this.componentConditionals.addAll(conditionals);
        this.useCache = conditionals.stream().anyMatch(cond -> cond instanceof CompoundConditional);
        this.operator = operator;
    }

    @SafeVarargs
    CompoundConditional(Conditional.Operator operator, Conditional<T> ... conditionals) {
        this(operator, List.of(conditionals));
    }

    @Override
    public Kleenean evaluate(T context) {
        HashMap<Conditional<T>, Kleenean> cache = null;
        if (this.useCache) {
            cache = new HashMap<Conditional<T>, Kleenean>();
        }
        return this.evaluate(context, cache);
    }

    @Override
    public Kleenean evaluate(T context, Map<Conditional<T>, Kleenean> cache) {
        return switch (this.operator) {
            default -> throw new MatchException(null, null);
            case Conditional.Operator.OR -> {
                Kleenean result = Kleenean.FALSE;
                for (Conditional var5_8 : this.componentConditionals) {
                    result = var5_8.evaluateOr(result, context, cache);
                }
                yield result;
            }
            case Conditional.Operator.AND -> {
                Kleenean result = Kleenean.TRUE;
                for (Conditional var5_9 : this.componentConditionals) {
                    result = var5_9.evaluateAnd(result, context, cache);
                }
                yield result;
            }
            case Conditional.Operator.NOT -> {
                if (this.componentConditionals.size() > 1) {
                    throw new IllegalStateException("Cannot apply NOT to multiple conditionals! Cannot evaluate.");
                }
                Iterator var4_7 = this.componentConditionals.iterator();
                if (var4_7.hasNext()) {
                    Conditional var5_10 = (Conditional)var4_7.next();
                    yield var5_10.evaluate(context, cache).not();
                }
                throw new IllegalStateException("Cannot apply NOT to zero conditionals! Cannot evaluate.");
            }
        };
    }

    public @Unmodifiable List<Conditional<T>> getConditionals() {
        return this.componentConditionals.stream().toList();
    }

    public Conditional.Operator getOperator() {
        return this.operator;
    }

    @SafeVarargs
    public final void addConditionals(Conditional<T> ... conditionals) {
        this.addConditionals(List.of(conditionals));
    }

    public void addConditionals(Collection<Conditional<T>> conditionals) {
        this.componentConditionals.addAll(conditionals);
        this.useCache |= conditionals.stream().anyMatch(cond -> cond instanceof CompoundConditional);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        Object output = this.joinConditionals(event, debug);
        if (this.componentConditionals.size() > 1) {
            output = "(" + (String)output + ")";
        }
        if (this.operator == Conditional.Operator.NOT) {
            return "!" + (String)output;
        }
        return output;
    }

    private String joinConditionals(@Nullable Event event, boolean debug) {
        return this.componentConditionals.stream().map(conditional -> conditional.toString(event, debug)).collect(Collectors.joining(" " + this.operator.getSymbol() + " "));
    }

    @Override
    public String toString() {
        return this.toString(null, false);
    }
}

