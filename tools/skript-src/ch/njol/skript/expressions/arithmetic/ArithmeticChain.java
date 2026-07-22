/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions.arithmetic;

import ch.njol.skript.expressions.arithmetic.ArithmeticExpressionInfo;
import ch.njol.skript.expressions.arithmetic.ArithmeticGettable;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.util.Utils;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.OperationInfo;
import org.skriptlang.skript.lang.arithmetic.Operator;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.util.Priority;

public class ArithmeticChain<L, R, T>
implements ArithmeticGettable<T> {
    private static List<Set<Operator>> operatorGroups = null;
    private final ArithmeticGettable<L> left;
    private final ArithmeticGettable<R> right;
    private final Operator operator;
    private final Class<? extends T> returnType;
    @Nullable
    private final OperationInfo<? extends L, ? extends R, ? extends T> operationInfo;

    public ArithmeticChain(ArithmeticGettable<L> left, Operator operator, ArithmeticGettable<R> right, @Nullable OperationInfo<L, R, T> operationInfo) {
        this.left = left;
        this.right = right;
        this.operator = operator;
        this.operationInfo = operationInfo;
        this.returnType = operationInfo != null ? operationInfo.returnType() : Object.class;
    }

    @Override
    @Nullable
    public T get(Event event) {
        Class<Object> rightClass;
        L left = this.left.get(event);
        if (left == null && this.left instanceof ArithmeticChain) {
            return null;
        }
        R right = this.right.get(event);
        if (right == null && this.right instanceof ArithmeticChain) {
            return null;
        }
        Class<Object> leftClass = left != null ? left.getClass() : this.left.getReturnType();
        Class<Object> clazz = rightClass = right != null ? right.getClass() : this.right.getReturnType();
        if (leftClass == Object.class && rightClass == Object.class) {
            return null;
        }
        OperationInfo<Object, Object, Object> operationInfo = this.operationInfo;
        if (left == null && leftClass == Object.class) {
            operationInfo = this.lookupOperationInfo(rightClass, OperationInfo::right);
        } else if (right == null && rightClass == Object.class) {
            operationInfo = this.lookupOperationInfo(leftClass, OperationInfo::left);
        } else if (operationInfo == null) {
            operationInfo = Arithmetics.lookupOperationInfo(this.operator, leftClass, rightClass, this.returnType);
        }
        if (operationInfo == null) {
            return null;
        }
        Object object = left = left != null ? left : Arithmetics.getDefaultValue(operationInfo.left());
        if (left == null) {
            return null;
        }
        R r = right = right != null ? right : Arithmetics.getDefaultValue(operationInfo.right());
        if (right == null) {
            return null;
        }
        return (T)operationInfo.operation().calculate(left, right);
    }

    @Nullable
    private OperationInfo<L, R, T> lookupOperationInfo(Class<?> anchor, Function<OperationInfo<?, ?, ?>, Class<?>> anchorFunction) {
        OperationInfo<?, ?, ?> operationInfo = Arithmetics.lookupOperationInfo(this.operator, anchor, anchor);
        if (operationInfo != null) {
            return operationInfo;
        }
        return Arithmetics.getOperations(this.operator).stream().filter(info -> ((Class)anchorFunction.apply((OperationInfo<?, ?, ?>)info)).isAssignableFrom(anchor)).filter(info -> Converters.converterExists(info.returnType(), this.returnType)).reduce((info, info2) -> {
            if (anchorFunction.apply((OperationInfo<?, ?, ?>)info2) == anchor) {
                return info2;
            }
            return info;
        }).orElse(null);
    }

    @Override
    public Class<? extends T> getReturnType() {
        return this.returnType;
    }

    private static void createOperatorGroups() {
        if (operatorGroups != null) {
            throw new RuntimeException("Operator groups have already been created");
        }
        operatorGroups = new LinkedList<Set<Operator>>();
        LinkedList<Operator> operators = new LinkedList<Operator>(Arithmetics.getAllOperators());
        Collections.reverse(operators);
        if (operators.isEmpty()) {
            return;
        }
        HashSet<Operator> currentGroup = new HashSet<Operator>();
        Priority currentGroupPriority = ((Operator)operators.get(0)).priority();
        for (Operator operator : operators) {
            if (operator.priority().compareTo(currentGroupPriority) != 0) {
                if (!currentGroup.isEmpty()) {
                    operatorGroups.add(currentGroup);
                }
                currentGroup = new HashSet();
                currentGroupPriority = operator.priority();
            }
            currentGroup.add(operator);
        }
        operatorGroups.add(currentGroup);
    }

    @Nullable
    public static <L, R, T> ArithmeticGettable<T> parse(List<?> chain) {
        if (operatorGroups == null) {
            ArithmeticChain.createOperatorGroups();
        }
        for (Set<Operator> group : operatorGroups) {
            int lastIndex = Utils.findLastIndex(chain, group::contains);
            if (lastIndex == -1) continue;
            ArithmeticGettable<T> left = ArithmeticChain.parse(chain.subList(0, lastIndex));
            Operator operator = (Operator)chain.get(lastIndex);
            ArithmeticGettable<T> right = ArithmeticChain.parse(chain.subList(lastIndex + 1, chain.size()));
            if (left == null || right == null) {
                return null;
            }
            OperationInfo<T, T, ?> operationInfo = null;
            if (left.getReturnType() != Object.class && right.getReturnType() != Object.class && (operationInfo = Arithmetics.lookupOperationInfo(operator, left.getReturnType(), right.getReturnType())) == null) {
                return null;
            }
            return new ArithmeticChain<T, T, T>(left, operator, right, operationInfo);
        }
        if (chain.size() != 1) {
            throw new IllegalStateException();
        }
        return new ArithmeticExpressionInfo((Expression)chain.get(0));
    }
}

