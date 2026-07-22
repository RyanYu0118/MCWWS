/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.ApiStatus$Experimental
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.properties.handlers.base;

import ch.njol.skript.classes.Changer;
import java.util.function.Function;
import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.properties.handlers.base.PropertyHandler;

@ApiStatus.Experimental
public interface ExpressionPropertyHandler<Type, ReturnType>
extends PropertyHandler<Type> {
    @Nullable
    default public ReturnType convert(Event event, Type propertyHolder) {
        return this.convert(propertyHolder);
    }

    @Nullable
    public ReturnType convert(Type var1);

    default public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return null;
    }

    default public void change(Type propertyHolder, Object @Nullable [] delta, Changer.ChangeMode mode) {
        throw new UnsupportedOperationException("Changing is not supported for this property.");
    }

    default public boolean requiresSourceExprChange() {
        return false;
    }

    @NotNull
    public Class<ReturnType> returnType();

    default public Class<?> @NotNull [] possibleReturnTypes() {
        return new Class[]{this.returnType()};
    }

    @Contract(value="_, _ -> new", pure=true)
    @NotNull
    public static <Type, ReturnType> ExpressionPropertyHandler<Type, ReturnType> of(final Function<Type, ReturnType> converter, final @NotNull Class<ReturnType> returnType) {
        return new ExpressionPropertyHandler<Type, ReturnType>(){

            @Override
            @Nullable
            public ReturnType convert(Type propertyHolder) {
                return converter.apply(propertyHolder);
            }

            @Override
            @NotNull
            public Class<ReturnType> returnType() {
                return returnType;
            }
        };
    }
}

