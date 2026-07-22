/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.lang.arithmetic;

import ch.njol.skript.localization.Noun;
import java.util.Comparator;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.util.Priority;

public record Operator(String sign, Priority priority, Noun node) implements Comparable<Operator>
{
    public static final Priority ADDITION_SUBTRACTION_PRIORITY = Priority.base();
    public static final Priority MULTIPLICATION_DIVISION_PRIORITY = Priority.before(ADDITION_SUBTRACTION_PRIORITY);
    public static final Priority EXPONENTIATION_PRIORITY = Priority.before(MULTIPLICATION_DIVISION_PRIORITY);
    public static final Operator ADDITION = new Operator('+', ADDITION_SUBTRACTION_PRIORITY, "add");
    public static final Operator SUBTRACTION = new Operator('-', ADDITION_SUBTRACTION_PRIORITY, "subtract");
    public static final Operator MULTIPLICATION = new Operator('*', MULTIPLICATION_DIVISION_PRIORITY, "multiply");
    public static final Operator DIVISION = new Operator('/', MULTIPLICATION_DIVISION_PRIORITY, "divide");
    public static final Operator EXPONENTIATION = new Operator('^', EXPONENTIATION_PRIORITY, "exponentiate");
    private static final Comparator<Operator> COMPARATOR = Comparator.comparing(Operator::priority).thenComparing(Operator::sign);

    public Operator(String sign, Priority priority, String node) {
        this(sign, priority, new Noun("operators." + node));
    }

    public Operator(char sign, Priority priority, Noun node) {
        this(String.valueOf(sign), priority, node);
    }

    public Operator(char sign, Priority priority, String node) {
        this(String.valueOf(sign), priority, node);
    }

    public String getName() {
        return this.node.toString();
    }

    @Override
    public String toString() {
        return this.sign;
    }

    @Override
    public int compareTo(@NotNull Operator o) {
        return COMPARATOR.compare(this, o);
    }
}

