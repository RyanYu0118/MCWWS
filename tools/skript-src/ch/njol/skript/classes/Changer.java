/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.classes;

import ch.njol.skript.expressions.base.WrapperExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.OperationInfo;
import org.skriptlang.skript.lang.arithmetic.Operator;
import org.skriptlang.skript.lang.converter.Converters;

public interface Changer<T> {
    public Class<?> @Nullable [] acceptChange(ChangeMode var1);

    public void change(T[] var1, Object @Nullable [] var2, ChangeMode var3);

    public static abstract class ChangerUtils {
        public static <T> void change(@NotNull Changer<T> changer, Object[] what, Object @Nullable [] delta, ChangeMode mode) {
            changer.change(what, delta, mode);
        }

        public static boolean acceptsChange(@NotNull Expression<?> expression, ChangeMode mode, Class<?> ... types) {
            Class<?>[] validTypes = expression.acceptChange(mode);
            if (validTypes == null) {
                return false;
            }
            for (int i = 0; i < validTypes.length; ++i) {
                if (!validTypes[i].isArray()) continue;
                validTypes[i] = validTypes[i].getComponentType();
            }
            return ChangerUtils.acceptsChangeTypes(validTypes, types);
        }

        @ApiStatus.Internal
        @Nullable
        public static <T> Expression<T> acceptsChangeWithConverters(@NotNull Expression<T> expression, ChangeMode mode, Class<?> ... types) {
            Class[] validTypes = expression.acceptChange(mode);
            if (validTypes == null) {
                return null;
            }
            for (int i = 0; i < validTypes.length; ++i) {
                if (!validTypes[i].isArray()) continue;
                validTypes[i] = validTypes[i].getComponentType();
            }
            if (ChangerUtils.acceptsChangeTypes(validTypes, types)) {
                return expression;
            }
            for (Class<?> type : types) {
                if (!Converters.converterExists(type, validTypes)) continue;
                class ChangeWrapper
                extends WrapperExpression<T> {
                    final /* synthetic */ Class[] val$validTypes;

                    public ChangeWrapper(Expression<T> expression2) {
                        this.val$validTypes = expression2;
                        super(expression);
                    }

                    @Override
                    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void change(Event event, @Nullable Object[] delta, ChangeMode mode) {
                        super.change(event, Converters.convert(delta, this.val$validTypes, Object.class), mode);
                    }

                    @Override
                    public <R> void changeInPlace(Event event, Function<T, R> changeFunction, boolean getAll) {
                        T[] values;
                        T[] TArray = values = getAll ? this.getAll(event) : this.getArray(event);
                        if (values.length == 0) {
                            return;
                        }
                        ArrayList<R> newValues = new ArrayList<R>();
                        for (Object value : values) {
                            newValues.add(changeFunction.apply(value));
                        }
                        this.change(event, newValues.toArray(), ChangeMode.SET);
                    }

                    @Override
                    public String toString(@Nullable Event event, boolean debug) {
                        return this.getExpr().toString(event, debug);
                    }
                }
                return new ChangeWrapper(expression, validTypes);
            }
            return null;
        }

        public static boolean acceptsChangeTypes(Class<?>[] validTypes, Class<?> ... types) {
            for (Class<?> type : types) {
                for (Class<?> validType : validTypes) {
                    if (!validType.isAssignableFrom(type)) continue;
                    return true;
                }
            }
            return false;
        }

        public static <T> Class<?>[] getArithmeticChangeTypes(Class<T> type, ChangeMode mode, Predicate<OperationInfo<T, ?, ?>> filter) {
            Preconditions.checkArgument((mode == ChangeMode.ADD || mode == ChangeMode.REMOVE ? 1 : 0) != 0, (Object)"Only ADD and REMOVE modes are supported for arithmetic change types");
            List<OperationInfo<T, ?, ?>> opInfos = mode == ChangeMode.ADD ? Arithmetics.getOperations(Operator.ADDITION, type) : Arithmetics.getOperations(Operator.SUBTRACTION, type);
            return (Class[])opInfos.stream().filter(filter).map(OperationInfo::right).toArray(Class[]::new);
        }
    }

    public static enum ChangeMode {
        ADD,
        SET,
        REMOVE,
        REMOVE_ALL,
        DELETE,
        RESET;


        public boolean supportsKeyedChange() {
            return this == SET;
        }
    }
}

