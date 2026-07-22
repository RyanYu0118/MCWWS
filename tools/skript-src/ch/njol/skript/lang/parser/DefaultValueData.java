/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.parser;

import ch.njol.skript.lang.DefaultExpression;
import ch.njol.skript.lang.parser.ParserInstance;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class DefaultValueData
extends ParserInstance.Data {
    private final Map<Class<?>, Deque<DefaultExpression<?>>> defaults = new HashMap();

    public DefaultValueData(ParserInstance parserInstance) {
        super(parserInstance);
    }

    public <T> void addDefaultValue(Class<T> type, DefaultExpression<T> value) {
        this.defaults.computeIfAbsent(type, key -> new ArrayDeque()).push(value);
    }

    @Nullable
    public <T> DefaultExpression<T> getDefaultValue(Class<T> type) {
        Deque<DefaultExpression<?>> stack = this.defaults.get(type);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.peek();
    }

    public void removeDefaultValue(Class<?> type) {
        Deque<DefaultExpression<?>> stack = this.defaults.get(type);
        if (stack == null || stack.isEmpty()) {
            throw new IllegalStateException("No default value for " + type.getName() + " to remove. Imbalanced add/remove?");
        }
        stack.pop();
    }
}

