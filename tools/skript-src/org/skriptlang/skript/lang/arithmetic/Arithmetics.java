/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 *  org.jetbrains.annotations.UnmodifiableView
 */
package org.skriptlang.skript.lang.arithmetic;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;
import org.skriptlang.skript.lang.arithmetic.DifferenceInfo;
import org.skriptlang.skript.lang.arithmetic.Operation;
import org.skriptlang.skript.lang.arithmetic.OperationInfo;
import org.skriptlang.skript.lang.arithmetic.Operator;

public final class Arithmetics {
    private static final Map<Operator, List<OperationInfo<?, ?, ?>>> OPERATIONS = Collections.synchronizedMap(new HashMap());
    private static final Map<Operator, Map<OperandTypes, OperationInfo<?, ?, ?>>> CACHED_OPERATIONS = Collections.synchronizedMap(new HashMap());
    private static final Map<Operator, Map<OperandTypes, OperationInfo<?, ?, ?>>> CACHED_CONVERTED_OPERATIONS = Collections.synchronizedMap(new HashMap());
    private static final Map<Class<?>, DifferenceInfo<?, ?>> DIFFERENCES = Collections.synchronizedMap(new HashMap());
    private static final Map<Class<?>, DifferenceInfo<?, ?>> CACHED_DIFFERENCES = Collections.synchronizedMap(new HashMap());
    private static final Map<Class<?>, Supplier<?>> DEFAULT_VALUES = Collections.synchronizedMap(new HashMap());
    private static final Map<Class<?>, Supplier<?>> CACHED_DEFAULT_VALUES = Collections.synchronizedMap(new HashMap());

    public static <T> void registerOperation(Operator operator, Class<T> type, Operation<T, T, T> operation) {
        Arithmetics.registerOperation(operator, type, type, type, operation);
    }

    public static <L, R> void registerOperation(Operator operator, Class<L> leftClass, Class<R> rightClass, Operation<L, R, L> operation) {
        Arithmetics.registerOperation(operator, leftClass, rightClass, leftClass, operation);
    }

    public static <L, R> void registerOperation(Operator operator, Class<L> leftClass, Class<R> rightClass, Operation<L, R, L> operation, Operation<R, L, L> commutativeOperation) {
        Arithmetics.registerOperation(operator, leftClass, rightClass, leftClass, operation);
        Arithmetics.registerOperation(operator, rightClass, leftClass, leftClass, commutativeOperation);
    }

    public static <L, R, T> void registerOperation(Operator operator, Class<L> leftClass, Class<R> rightClass, Class<T> returnType, Operation<L, R, T> operation, Operation<R, L, T> commutativeOperation) {
        Arithmetics.registerOperation(operator, leftClass, rightClass, returnType, operation);
        Arithmetics.registerOperation(operator, rightClass, leftClass, returnType, commutativeOperation);
    }

    public static <L, R, T> void registerOperation(Operator operator, Class<L> leftClass, Class<R> rightClass, Class<T> returnType, Operation<L, R, T> operation) {
        Skript.checkAcceptRegistrations();
        if (Arithmetics.exactOperationExists(operator, leftClass, rightClass)) {
            throw new SkriptAPIException("There's already a " + operator.getName() + " operation registered for types '" + leftClass.getName() + "' and '" + rightClass.getName() + "'");
        }
        Arithmetics.getRawOperations(operator).add(new OperationInfo<L, R, T>(leftClass, rightClass, returnType, operation));
    }

    public static boolean exactOperationExists(Operator operator, Class<?> leftClass, Class<?> rightClass) {
        for (OperationInfo<?, ?, ?> info : Arithmetics.getRawOperations(operator)) {
            if (info.left() != leftClass || info.right() != rightClass) continue;
            return true;
        }
        return false;
    }

    public static boolean operationExists(Operator operator, Class<?> leftClass, Class<?> rightClass) {
        return Arithmetics.getOperationInfo(operator, leftClass, rightClass) != null;
    }

    private static List<OperationInfo<?, ?, ?>> getRawOperations(Operator operator) {
        return OPERATIONS.computeIfAbsent(operator, o -> Collections.synchronizedList(new ArrayList()));
    }

    public static @UnmodifiableView List<OperationInfo<?, ?, ?>> getOperations(Operator operator) {
        return Collections.unmodifiableList(Arithmetics.getRawOperations(operator));
    }

    public static <T> @Unmodifiable List<OperationInfo<T, ?, ?>> getOperations(Operator operator, Class<T> type) {
        return Arithmetics.getOperations(operator).stream().filter(info -> info.left().isAssignableFrom(type)).collect(Collectors.toList());
    }

    public static <L> @Unmodifiable List<OperationInfo<L, ?, ?>> lookupLeftOperations(Operator operator, Class<L> leftClass) {
        return Arithmetics.getOperations(operator).stream().map(info -> {
            if (info.left().isAssignableFrom(leftClass)) {
                return info;
            }
            return info.getConverted(leftClass, info.right(), info.returnType());
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static <R> @Unmodifiable List<OperationInfo<?, R, ?>> lookupRightOperations(Operator operator, Class<R> rightClass) {
        return Arithmetics.getOperations(operator).stream().map(info -> {
            if (info.right().isAssignableFrom(rightClass)) {
                return info;
            }
            return info.getConverted(info.left(), rightClass, info.returnType());
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Nullable
    public static <L, R, T> OperationInfo<L, R, T> getOperationInfo(Operator operator, Class<L> leftClass, Class<R> rightClass, Class<T> returnType) {
        OperationInfo<L, R, ?> info = Arithmetics.getOperationInfo(operator, leftClass, rightClass);
        if (info != null && returnType.isAssignableFrom(info.returnType())) {
            return info;
        }
        return null;
    }

    @Nullable
    public static <L, R> OperationInfo<L, R, ?> getOperationInfo(Operator operator, Class<L> leftClass, Class<R> rightClass) {
        Arithmetics.assertIsOperationsDoneLoading();
        OperandTypes operandTypes = new OperandTypes(leftClass, rightClass);
        Map operations = CACHED_OPERATIONS.computeIfAbsent(operator, o -> Collections.synchronizedMap(new HashMap()));
        OperationInfo operationInfo = (OperationInfo)operations.get(operandTypes);
        if (operations.containsKey(operandTypes)) {
            return operationInfo;
        }
        operationInfo = Arithmetics.getOperations(operator).stream().filter(info -> info.left().isAssignableFrom(leftClass) && info.right().isAssignableFrom(rightClass)).reduce((info, info2) -> {
            if (info2.left() == leftClass && info2.right() == rightClass) {
                return info2;
            }
            return info;
        }).orElse(null);
        operations.put(operandTypes, operationInfo);
        return operationInfo;
    }

    @Nullable
    public static <L, R, T> Operation<L, R, T> getOperation(Operator operator, Class<L> leftClass, Class<R> rightClass, Class<T> returnType) {
        OperationInfo<L, R, T> info = Arithmetics.getOperationInfo(operator, leftClass, rightClass, returnType);
        return info == null ? null : info.operation();
    }

    @Nullable
    public static <L, R> Operation<L, R, ?> getOperation(Operator operator, Class<L> leftClass, Class<R> rightClass) {
        OperationInfo<L, R, ?> info = Arithmetics.getOperationInfo(operator, leftClass, rightClass);
        return info == null ? null : info.operation();
    }

    @Nullable
    public static <L, R, T> OperationInfo<L, R, T> lookupOperationInfo(Operator operator, Class<L> leftClass, Class<R> rightClass, Class<T> returnType) {
        OperationInfo<L, R, ?> info = Arithmetics.lookupOperationInfo(operator, leftClass, rightClass);
        return info != null ? info.getConverted(leftClass, rightClass, returnType) : null;
    }

    @Nullable
    public static <L, R> OperationInfo<L, R, ?> lookupOperationInfo(Operator operator, Class<L> leftClass, Class<R> rightClass) {
        OperationInfo operationInfo = Arithmetics.getOperationInfo(operator, leftClass, rightClass);
        if (operationInfo != null) {
            return operationInfo;
        }
        OperandTypes operandTypes = new OperandTypes(leftClass, rightClass);
        Map operations = CACHED_CONVERTED_OPERATIONS.computeIfAbsent(operator, o -> Collections.synchronizedMap(new HashMap()));
        operationInfo = (OperationInfo)operations.get(operandTypes);
        if (operations.containsKey(operandTypes)) {
            return operationInfo;
        }
        for (OperationInfo<?, ?, ?> info : Arithmetics.getOperations(operator)) {
            OperationInfo<L, R, ?> convertedInfo = info.getConverted(leftClass, rightClass, info.returnType());
            if (convertedInfo == null) continue;
            operations.put(operandTypes, convertedInfo);
            return convertedInfo;
        }
        operations.put(operandTypes, null);
        return null;
    }

    @Nullable
    public static <L, R, T> T calculate(Operator operator, L left, R right, Class<T> returnType) {
        Operation<?, ?, T> operation = Arithmetics.getOperation(operator, left.getClass(), right.getClass(), returnType);
        return operation == null ? null : (T)operation.calculate(left, right);
    }

    @Nullable
    public static <L, R, T> T calculateUnsafe(Operator operator, L left, R right) {
        Operation<?, ?, ?> operation = Arithmetics.getOperation(operator, left.getClass(), right.getClass());
        return operation == null ? null : (T)operation.calculate(left, right);
    }

    public static <T> void registerDifference(Class<T> type, Operation<T, T, T> operation) {
        Arithmetics.registerDifference(type, type, operation);
    }

    public static <T, R> void registerDifference(Class<T> type, Class<R> returnType, Operation<T, T, R> operation) {
        Skript.checkAcceptRegistrations();
        if (Arithmetics.exactDifferenceExists(type)) {
            throw new SkriptAPIException("There's already a difference registered for type '" + String.valueOf(type) + "'");
        }
        DIFFERENCES.put(type, new DifferenceInfo<T, R>(type, returnType, operation));
    }

    public static boolean exactDifferenceExists(Class<?> type) {
        return DIFFERENCES.containsKey(type);
    }

    public static boolean differenceExists(Class<?> type) {
        return Arithmetics.getDifferenceInfo(type) != null;
    }

    @Nullable
    public static <T, R> DifferenceInfo<T, R> getDifferenceInfo(Class<T> type, Class<R> returnType) {
        DifferenceInfo<T, ?> info = Arithmetics.getDifferenceInfo(type);
        if (info != null && returnType.isAssignableFrom(info.returnType())) {
            return info;
        }
        return null;
    }

    @Nullable
    public static <T> DifferenceInfo<T, ?> getDifferenceInfo(Class<T> type) {
        if (Skript.isAcceptRegistrations()) {
            throw new SkriptAPIException("Differences cannot be retrieved until Skript has finished registrations.");
        }
        if (CACHED_DIFFERENCES.containsKey(type)) {
            return CACHED_DIFFERENCES.get(type);
        }
        DifferenceInfo<?, ?> difference = null;
        if (DIFFERENCES.containsKey(type)) {
            difference = DIFFERENCES.get(type);
            CACHED_DIFFERENCES.put(type, difference);
            return difference;
        }
        for (Map.Entry<Class<?>, DifferenceInfo<?, ?>> entry : DIFFERENCES.entrySet()) {
            if (!entry.getKey().isAssignableFrom(type)) continue;
            difference = entry.getValue();
            break;
        }
        CACHED_DIFFERENCES.put(type, difference);
        return difference;
    }

    @Nullable
    public static <T, R> Operation<T, T, R> getDifference(Class<T> type, Class<R> returnType) {
        DifferenceInfo<T, R> info = Arithmetics.getDifferenceInfo(type, returnType);
        return info == null ? null : info.operation();
    }

    @Nullable
    public static <T> Operation<T, T, ?> getDifference(Class<T> type) {
        DifferenceInfo<T, ?> info = Arithmetics.getDifferenceInfo(type);
        return info == null ? null : info.operation();
    }

    @Nullable
    public static <T, R> R difference(T left, T right, Class<R> returnType) {
        Operation<?, ?, R> operation = Arithmetics.getDifference(left.getClass(), returnType);
        return operation == null ? null : (R)operation.calculate(left, right);
    }

    @Nullable
    public static <T, R> R differenceUnsafe(T left, T right) {
        Operation<?, ?, ?> operation = Arithmetics.getDifference(left.getClass());
        return operation == null ? null : (R)operation.calculate(left, right);
    }

    public static <T> void registerDefaultValue(Class<T> type, Supplier<T> supplier) {
        Skript.checkAcceptRegistrations();
        if (DEFAULT_VALUES.containsKey(type)) {
            throw new SkriptAPIException("There's already a default value registered for type '" + type.getName() + "'");
        }
        DEFAULT_VALUES.put(type, supplier);
    }

    @Nullable
    public static <R, T extends R> R getDefaultValue(Class<T> type) {
        if (Skript.isAcceptRegistrations()) {
            throw new SkriptAPIException("Default values cannot be retrieved until Skript has finished registrations.");
        }
        Supplier<?> supplier = null;
        if (CACHED_DEFAULT_VALUES.containsKey(type)) {
            supplier = CACHED_DEFAULT_VALUES.get(type);
            return supplier != null ? (R)supplier.get() : null;
        }
        if (DEFAULT_VALUES.containsKey(type)) {
            supplier = DEFAULT_VALUES.get(type);
            CACHED_DEFAULT_VALUES.put(type, supplier);
            return (R)supplier.get();
        }
        for (Map.Entry<Class<?>, Supplier<?>> entry : DEFAULT_VALUES.entrySet()) {
            if (!entry.getKey().isAssignableFrom(type)) continue;
            supplier = entry.getValue();
            break;
        }
        CACHED_DEFAULT_VALUES.put(type, supplier);
        return supplier != null ? (R)supplier.get() : null;
    }

    private static void assertIsOperationsDoneLoading() {
        if (Skript.isAcceptRegistrations()) {
            throw new SkriptAPIException("Operations cannot be retrieved until Skript has finished registrations.");
        }
    }

    public static Collection<Class<?>> getAllReturnTypes(Operator operator) {
        HashSet types = new HashSet();
        for (OperationInfo<?, ?, ?> info : Arithmetics.getRawOperations(operator)) {
            types.add(info.returnType());
        }
        return types;
    }

    public static Set<Operator> getAllOperators() {
        LinkedList<Operator> operators = new LinkedList<Operator>(OPERATIONS.keySet());
        Collections.sort(operators);
        return new LinkedHashSet<Operator>(operators);
    }

    private Arithmetics() {
        throw new UnsupportedOperationException();
    }

    private record OperandTypes(Class<?> left, Class<?> right) {
    }
}

