/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.entry.util;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.localization.Message;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.log.ParseLogHandler;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.KeyValueEntryData;

public class ExpressionEntryData<T>
extends KeyValueEntryData<Expression<? extends T>> {
    private static final Message M_IS = new Message("is");
    private final Class<? extends T>[] returnTypes;
    private final int flags;

    public ExpressionEntryData(String key, @Nullable Expression<? extends T> defaultValue, boolean optional, Class<? extends T> returnType) {
        this(key, defaultValue, optional, 3, returnType);
    }

    public ExpressionEntryData(String key, @Nullable Expression<? extends T> defaultValue, boolean optional, Class<? extends T> returnType, int flags) {
        this(key, defaultValue, optional, flags, returnType);
    }

    @SafeVarargs
    public ExpressionEntryData(String key, @Nullable Expression<? extends T> defaultValue, boolean optional, Class<? extends T> ... returnTypes) {
        this(key, defaultValue, optional, 3, returnTypes);
    }

    @SafeVarargs
    public ExpressionEntryData(String key, @Nullable Expression<? extends T> defaultValue, boolean optional, int flags, Class<? extends T> ... returnTypes) {
        super(key, defaultValue, optional);
        this.returnTypes = returnTypes;
        this.flags = flags;
    }

    @Override
    @Nullable
    protected Expression<? extends T> getValue(String value) {
        Expression<? extends T> expression;
        try (ParseLogHandler log = new ParseLogHandler().start();){
            expression = new SkriptParser(value, this.flags, ParseContext.DEFAULT).parseExpression(this.returnTypes);
            if (expression == null) {
                log.printError("'" + value + "' " + String.valueOf(M_IS) + " " + SkriptParser.notOfType(this.returnTypes), ErrorQuality.NOT_AN_EXPRESSION);
            }
        }
        return expression;
    }
}

