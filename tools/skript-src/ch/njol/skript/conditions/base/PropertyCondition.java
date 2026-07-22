/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions.base;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.util.function.Predicate;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.util.Priority;

public abstract class PropertyCondition<T>
extends Condition
implements Predicate<T> {
    public static final Priority DEFAULT_PRIORITY = Priority.before(SyntaxInfo.PATTERN_MATCHES_EVERYTHING);
    private Expression<? extends T> expr;

    public static <E extends Condition> SyntaxInfo.Builder<? extends SyntaxInfo.Builder<?, E>, E> infoBuilder(Class<E> condition, PropertyType propertyType, String property, String type) {
        if (type.contains("%")) {
            throw new SkriptAPIException("The type argument must not contain any '%'s");
        }
        return SyntaxInfo.builder(condition).priority(DEFAULT_PRIORITY).addPatterns(PropertyCondition.getPatterns(propertyType, property, type));
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static void register(Class<? extends Condition> condition, String property, String type) {
        PropertyCondition.register(condition, PropertyType.BE, property, type);
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static void register(Class<? extends Condition> condition, PropertyType propertyType, String property, String type) {
        Skript.registerCondition(condition, Condition.ConditionType.PROPERTY, PropertyCondition.getPatterns(propertyType, property, type));
    }

    public static String[] getPatterns(PropertyType propertyType, String property, String type) {
        String[] stringArray;
        if (type.contains("%")) {
            throw new SkriptAPIException("The type argument must not contain any '%'s");
        }
        switch (propertyType.ordinal()) {
            default: {
                throw new MatchException(null, null);
            }
            case 0: {
                String[] stringArray2 = new String[2];
                stringArray2[0] = "%" + type + "% (is|are) " + property;
                stringArray = stringArray2;
                stringArray2[1] = "%" + type + "% (isn't|is not|aren't|are not) " + property;
                break;
            }
            case 1: {
                String[] stringArray3 = new String[2];
                stringArray3[0] = "%" + type + "% can " + property;
                stringArray = stringArray3;
                stringArray3[1] = "%" + type + "% (can't|cannot|can not) " + property;
                break;
            }
            case 2: {
                String[] stringArray4 = new String[2];
                stringArray4[0] = "%" + type + "% (has|have) " + property;
                stringArray = stringArray4;
                stringArray4[1] = "%" + type + "% (doesn't|does not|do not|don't) have " + property;
                break;
            }
            case 3: {
                String[] stringArray5 = new String[2];
                stringArray5[0] = "%" + type + "% will " + property;
                stringArray = stringArray5;
                stringArray5[1] = "%" + type + "% (will (not|neither)|won't) " + property;
            }
        }
        return stringArray;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.expr = expressions[0];
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public final boolean check(Event event) {
        return this.expr.check(event, this, this.isNegated());
    }

    public abstract boolean check(T var1);

    @Override
    public final boolean test(T value) {
        return this.check(value);
    }

    protected abstract String getPropertyName();

    protected PropertyType getPropertyType() {
        return PropertyType.BE;
    }

    public final Expression<? extends T> getExpr() {
        return this.expr;
    }

    protected final void setExpr(Expression<? extends T> expr) {
        this.expr = expr;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return PropertyCondition.toString(this, this.getPropertyType(), event, debug, this.expr, this.getPropertyName());
    }

    public static String toString(Condition condition, PropertyType propertyType, @Nullable Event event, boolean debug, Expression<?> expr, String property) {
        switch (propertyType.ordinal()) {
            case 0: {
                return expr.toString(event, debug) + (expr.isSingle() ? " is " : " are ") + (condition.isNegated() ? "not " : "") + property;
            }
            case 1: {
                return expr.toString(event, debug) + (condition.isNegated() ? " can't " : " can ") + property;
            }
            case 2: {
                if (expr.isSingle()) {
                    return expr.toString(event, debug) + (condition.isNegated() ? " doesn't have " : " has ") + property;
                }
                return expr.toString(event, debug) + (condition.isNegated() ? " don't have " : " have ") + property;
            }
            case 3: {
                return expr.toString(event, debug) + (condition.isNegated() ? " won't " : " will ") + "be " + property;
            }
        }
        assert (false);
        return null;
    }

    @Override
    @NotNull
    public Predicate<T> and(@NotNull Predicate<? super T> other) {
        throw new UnsupportedOperationException("Combining property conditions is undefined behaviour");
    }

    @Override
    @NotNull
    public Predicate<T> negate() {
        throw new UnsupportedOperationException("Negating property conditions without setNegated is undefined behaviour");
    }

    @Override
    @NotNull
    public Predicate<T> or(@NotNull Predicate<? super T> other) {
        throw new UnsupportedOperationException("Combining property conditions is undefined behaviour");
    }

    public static enum PropertyType {
        BE,
        CAN,
        HAVE,
        WILL;

    }
}

