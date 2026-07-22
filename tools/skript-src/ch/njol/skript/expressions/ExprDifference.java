/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.CondCompare;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import java.lang.reflect.Array;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.DifferenceInfo;

@Name(value="Difference")
@Description(value={"The difference between two values", "Supported types include <a href='#number'>numbers</a>, <a href='./classes/#date'>dates</a> and <a href='./classes/#time'>times</a>."})
@Example(value="if difference between {command::%player%::lastuse} and now is smaller than a minute:\n\tmessage \"You have to wait a minute before using this command again!\"\n")
@Since(value={"1.4"})
public class ExprDifference
extends SimpleExpression<Object> {
    private Expression<?> first;
    private Expression<?> second;
    @Nullable
    private DifferenceInfo differenceInfo;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Expression<Object> second;
        Expression<Object> first = LiteralUtils.defendExpression(exprs[0]);
        if (!LiteralUtils.canInitSafely(first, second = LiteralUtils.defendExpression(exprs[1]))) {
            return false;
        }
        Class firstReturnType = first.getReturnType();
        Class secondReturnType = second.getReturnType();
        Class<?> superType = Utils.getSuperType(firstReturnType, secondReturnType);
        boolean fail = false;
        if (superType == Object.class && (firstReturnType != Object.class || secondReturnType != Object.class)) {
            if (firstReturnType != Object.class && secondReturnType != Object.class) {
                Expression secondConverted;
                fail = true;
                this.differenceInfo = Arithmetics.getDifferenceInfo(firstReturnType);
                if (this.differenceInfo != null && (secondConverted = second.getConvertedExpression(firstReturnType)) != null) {
                    second = secondConverted;
                    fail = false;
                }
                if (fail) {
                    Expression firstConverted;
                    this.differenceInfo = Arithmetics.getDifferenceInfo(secondReturnType);
                    if (this.differenceInfo != null && (firstConverted = first.getConvertedExpression(secondReturnType)) != null) {
                        first = firstConverted;
                        fail = false;
                    }
                }
            } else {
                Expression converted;
                if (firstReturnType == Object.class) {
                    converted = first.getConvertedExpression(secondReturnType);
                    if (converted != null) {
                        first = converted;
                    }
                } else {
                    converted = second.getConvertedExpression(firstReturnType);
                    if (converted != null) {
                        second = converted;
                    }
                }
                if (converted == null) {
                    fail = true;
                } else {
                    superType = Utils.getSuperType(first.getReturnType(), second.getReturnType());
                }
            }
        }
        if (superType != Object.class && (this.differenceInfo = Arithmetics.getDifferenceInfo(superType)) == null) {
            fail = true;
        }
        if (fail) {
            Skript.error("Can't get the difference of " + CondCompare.f(first) + " and " + CondCompare.f(second));
            return false;
        }
        this.first = first;
        this.second = second;
        return true;
    }

    @Override
    @Nullable
    protected Object[] get(Event event) {
        Class<?> superType;
        Object first = this.first.getSingle(event);
        Object second = this.second.getSingle(event);
        if (first == null || second == null) {
            return new Object[0];
        }
        DifferenceInfo<?, ?> differenceInfo = this.differenceInfo;
        if (differenceInfo == null && (differenceInfo = Arithmetics.getDifferenceInfo(superType = Utils.getSuperType(first.getClass(), second.getClass()))) == null) {
            return new Object[0];
        }
        assert (differenceInfo != null);
        Object[] one = (Object[])Array.newInstance(differenceInfo.returnType(), 1);
        one[0] = differenceInfo.operation().calculate(first, second);
        return one;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        return this.differenceInfo == null ? Object.class : this.differenceInfo.returnType();
    }

    @Override
    public Expression<?> simplify() {
        if (this.first instanceof Literal && this.second instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "difference between " + this.first.toString(event, debug) + " and " + this.second.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprDifference.class, Object.class, ExpressionType.COMBINED, "difference (between|of) %object% and %object%");
    }
}

