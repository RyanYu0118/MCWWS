/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.lang.properties.handlers;

import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;
import org.skriptlang.skript.lang.properties.handlers.base.PropertyHandler;

public abstract class WXYZHandler<Type, ValueType>
implements ExpressionPropertyHandler<Type, ValueType> {
    protected Axis axis;

    @Override
    public abstract PropertyHandler<Type> newInstance();

    public abstract boolean supportsAxis(Axis var1);

    public void axis(Axis axis) {
        this.axis = axis;
    }

    public Axis axis() {
        return this.axis;
    }

    public static enum Axis {
        W,
        X,
        Y,
        Z;

    }
}

