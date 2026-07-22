/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Experimental
 */
package org.skriptlang.skript.lang.properties.handlers.base;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.parser.ParserInstance;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public interface PropertyHandler<Type> {
    default public PropertyHandler<Type> newInstance() {
        return this;
    }

    default public boolean init(Expression<?> parentExpression, ParserInstance parser) {
        return true;
    }
}

