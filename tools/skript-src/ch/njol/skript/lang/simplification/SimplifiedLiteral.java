/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.simplification;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.util.ContextlessEvent;
import ch.njol.skript.lang.util.SimpleLiteral;
import java.util.function.Function;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class SimplifiedLiteral<T>
extends SimpleLiteral<T> {
    public static <T> SimplifiedLiteral<T> fromExpression(Expression<T> original) {
        if (original instanceof SimplifiedLiteral) {
            SimplifiedLiteral literal = (SimplifiedLiteral)original;
            return literal;
        }
        ContextlessEvent event = ContextlessEvent.get();
        T[] values = original.getAll(event);
        return new SimplifiedLiteral<T>(values, values.getClass().getComponentType(), original.getAnd(), original);
    }

    public SimplifiedLiteral(T[] data, Class<T> type, boolean and, Expression<T> source) {
        super(data, type, and, source);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return this.source.acceptChange(mode);
    }

    @Override
    public Object @Nullable [] beforeChange(Expression<?> changed, Object @Nullable [] delta) {
        return this.source.beforeChange(changed, delta);
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
        this.source.change(event, delta, mode);
    }

    @Override
    public boolean isLoopOf(String input) {
        return this.source.isLoopOf(input);
    }

    @Override
    public <R> void changeInPlace(Event event, Function<T, R> changeFunction) {
        this.getSource().changeInPlace(event, changeFunction);
    }

    @Override
    public <R> void changeInPlace(Event event, Function<T, R> changeFunction, boolean getAll) {
        this.getSource().changeInPlace(event, changeFunction, getAll);
    }

    @Override
    public Expression<T> getSource() {
        return this.source;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (debug) {
            return "[" + this.source.toString(event, true) + " (SIMPLIFIED)]";
        }
        return this.source.toString(event, false);
    }
}

