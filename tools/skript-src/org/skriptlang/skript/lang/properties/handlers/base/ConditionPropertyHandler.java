/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Experimental
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.lang.properties.handlers.base;

import java.util.function.Predicate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.lang.properties.handlers.base.PropertyHandler;

@ApiStatus.Experimental
public interface ConditionPropertyHandler<Type>
extends PropertyHandler<Type> {
    public boolean check(Type var1);

    @Contract(value="_ -> new", pure=true)
    @NotNull
    public static <Type> ConditionPropertyHandler<Type> of(Predicate<Type> predicate) {
        return predicate::test;
    }
}

