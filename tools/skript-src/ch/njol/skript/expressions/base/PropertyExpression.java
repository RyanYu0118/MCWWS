/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.ApiStatus$Experimental
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.expressions.base;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.util.SimpleExpression;
import com.google.common.base.Preconditions;
import java.util.Arrays;
import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.Priority;

public abstract class PropertyExpression<F, T>
extends SimpleExpression<T> {
    public static final Priority DEFAULT_PRIORITY = Priority.before(SyntaxInfo.PATTERN_MATCHES_EVERYTHING);
    private @UnknownNullability Expression<? extends F> expr;

    private static String[] patternsOf(String property, String fromType, boolean defaultExpr) {
        Preconditions.checkNotNull((Object)property, (Object)"property must be present");
        Preconditions.checkNotNull((Object)fromType, (Object)"fromType must be present");
        String types = defaultExpr ? "[of %" + fromType + "%]" : "of %" + fromType + "%";
        return new String[]{"[the] " + property + " " + types, "%" + fromType + "%'[s] " + property};
    }

    public static String[] getPatterns(String property, String fromType) {
        return PropertyExpression.patternsOf(property, fromType, false);
    }

    public static String[] getDefaultPatterns(String property, String fromType) {
        return PropertyExpression.patternsOf(property, fromType, true);
    }

    @Deprecated(since="2.12", forRemoval=true)
    @ApiStatus.Experimental
    public static <E extends Expression<T>, T> DefaultSyntaxInfos.Expression<E, T> register(SyntaxRegistry registry, Class<E> expressionClass, Class<T> returnType, String property, String fromType) {
        SyntaxInfo info = PropertyExpression.infoBuilder(expressionClass, returnType, property, fromType, false).build();
        registry.register(SyntaxRegistry.EXPRESSION, info);
        return info;
    }

    @Deprecated(since="2.12", forRemoval=true)
    @ApiStatus.Experimental
    public static <E extends Expression<T>, T> DefaultSyntaxInfos.Expression<E, T> registerDefault(SyntaxRegistry registry, Class<E> expressionClass, Class<T> returnType, String property, String fromType) {
        SyntaxInfo info = PropertyExpression.infoBuilder(expressionClass, returnType, property, fromType, true).build();
        registry.register(SyntaxRegistry.EXPRESSION, info);
        return info;
    }

    public static <E extends Expression<T>, T> DefaultSyntaxInfos.Expression.Builder<? extends DefaultSyntaxInfos.Expression.Builder<?, E, T>, E, T> infoBuilder(Class<E> expressionClass, Class<T> returnType, String property, String type, boolean isDefault) {
        return (DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(expressionClass, returnType).priority(DEFAULT_PRIORITY)).addPatterns(PropertyExpression.patternsOf(property, type, isDefault));
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static <T> void register(Class<? extends Expression<T>> expressionClass, Class<T> type, String property, String fromType) {
        Skript.registerExpression(expressionClass, type, ExpressionType.PROPERTY, PropertyExpression.getPatterns(property, fromType));
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static <T> void registerDefault(Class<? extends Expression<T>> expressionClass, Class<T> type, String property, String fromType) {
        Skript.registerExpression(expressionClass, type, ExpressionType.PROPERTY, PropertyExpression.getDefaultPatterns(property, fromType));
    }

    protected final void setExpr(@NotNull Expression<? extends F> expr) {
        Preconditions.checkNotNull(expr, (Object)"The expr param cannot be null");
        this.expr = expr;
    }

    public final Expression<? extends F> getExpr() {
        return this.expr;
    }

    @Override
    protected final T[] get(Event event) {
        return this.get(event, this.expr.getArray(event));
    }

    @Override
    public final T[] getAll(Event event) {
        T[] result = this.get(event, this.expr.getAll(event));
        if (result == null) {
            throw new SkriptAPIException("PropertyExpression must not return a null array");
        }
        for (T t : result) {
            if (t != null) continue;
            throw new SkriptAPIException("PropertyExpression must not return an array containing null elements");
        }
        return Arrays.copyOf(result, result.length);
    }

    protected abstract T[] get(Event var1, F[] var2);

    protected T[] get(F[] source, Converter<? super F, ? extends T> converter) {
        assert (source != null);
        assert (converter != null);
        return Converters.convertUnsafe(source, this.getReturnType(), converter);
    }

    @Override
    public boolean isSingle() {
        return this.expr.isSingle();
    }

    @Override
    public final boolean getAnd() {
        return this.expr.getAnd();
    }

    @Override
    public Expression<? extends T> simplify() {
        this.expr = this.expr.simplify();
        return this;
    }
}

