/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.Contract
 */
package org.skriptlang.skript.lang.condition;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.Contract;
import org.skriptlang.skript.lang.condition.CompoundConditional;
import org.skriptlang.skript.lang.condition.Conditional;

public class DNFConditionalBuilder<T> {
    private CompoundConditional<T> root;

    DNFConditionalBuilder() {
        this.root = null;
    }

    DNFConditionalBuilder(Conditional<T> root) {
        CompoundConditional compoundConditional;
        this.root = root instanceof CompoundConditional ? (compoundConditional = (CompoundConditional)root) : new CompoundConditional(Conditional.Operator.OR, root);
    }

    public Conditional<T> build() {
        return (Conditional)Preconditions.checkNotNull(this.root, (Object)"Cannot build an empty conditional!");
    }

    @SafeVarargs
    @Contract(value="_ -> this")
    public final DNFConditionalBuilder<T> and(Conditional<T> ... andConditionals) {
        if (this.root == null) {
            this.root = new CompoundConditional<T>(Conditional.Operator.AND, andConditionals);
            return this;
        }
        List<Conditional<T>> newConditionals = DNFConditionalBuilder.unroll(List.of(andConditionals), Conditional.Operator.AND);
        if (this.root.getOperator() == Conditional.Operator.AND) {
            this.root.addConditionals(newConditionals);
            return this;
        }
        ArrayList transformedConditionals = new ArrayList();
        newConditionals.add(0, null);
        for (Conditional<T> conditional : this.root.getConditionals()) {
            newConditionals.set(0, conditional);
            transformedConditionals.add(new CompoundConditional<T>(Conditional.Operator.AND, newConditionals));
        }
        this.root = new CompoundConditional(Conditional.Operator.OR, transformedConditionals);
        return this;
    }

    @SafeVarargs
    @Contract(value="_ -> this")
    public final DNFConditionalBuilder<T> or(Conditional<T> ... orConditionals) {
        if (this.root == null) {
            this.root = new CompoundConditional<T>(Conditional.Operator.OR, orConditionals);
            return this;
        }
        List<Conditional<T>> newConditionals = DNFConditionalBuilder.unroll(List.of(orConditionals), Conditional.Operator.OR);
        if (this.root.getOperator() == Conditional.Operator.OR) {
            this.root.addConditionals(newConditionals);
            return this;
        }
        newConditionals.add(0, this.root);
        this.root = new CompoundConditional<T>(Conditional.Operator.OR, newConditionals);
        return this;
    }

    @Contract(value="_ -> this")
    public DNFConditionalBuilder<T> andNot(Conditional<T> conditional) {
        return this.and(Conditional.negate(conditional));
    }

    @Contract(value="_ -> this")
    public DNFConditionalBuilder<T> orNot(Conditional<T> conditional) {
        return this.or(Conditional.negate(conditional));
    }

    @SafeVarargs
    @Contract(value="_,_ -> this")
    public final DNFConditionalBuilder<T> add(boolean or, Conditional<T> ... conditionals) {
        if (or) {
            return this.or(conditionals);
        }
        return this.and(conditionals);
    }

    @Contract(value="_,_ -> new")
    private static <T> List<Conditional<T>> unroll(Collection<Conditional<T>> conditionals, Conditional.Operator operator) {
        ArrayList<Conditional<T>> newConditionals = new ArrayList<Conditional<T>>();
        for (Conditional<T> conditional : conditionals) {
            CompoundConditional compound;
            if (conditional instanceof CompoundConditional && (compound = (CompoundConditional)conditional).getOperator() == operator) {
                newConditionals.addAll(DNFConditionalBuilder.unroll(compound.getConditionals(), operator));
                continue;
            }
            newConditionals.add(conditional);
        }
        return newConditionals;
    }
}

