/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.variables;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

@Deprecated(since="2.12", forRemoval=true)
public class TypeHints {
    private static final Deque<Map<String, Class<?>>> typeHints = new ArrayDeque();

    public static void add(String variable, Class<?> hint) {
        if (hint.equals(Object.class)) {
            return;
        }
        Map<String, Class<?>> hints = typeHints.getFirst();
        hints.put(variable, hint);
    }

    @Nullable
    public static Class<?> get(String variable) {
        for (Map<String, Class<?>> hints : typeHints) {
            Class<?> hint = hints.get(variable);
            if (hint == null) continue;
            return hint;
        }
        return null;
    }

    public static void enterScope() {
        typeHints.push(new HashMap());
    }

    public static void exitScope() {
        typeHints.pop();
    }

    public static void clear() {
        typeHints.clear();
        typeHints.push(new HashMap());
    }

    static {
        TypeHints.clear();
    }
}

