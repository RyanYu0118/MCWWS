/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.util;

import java.lang.reflect.Modifier;

public final class ClassUtils {
    public static boolean isNormalClass(Class<?> clazz) {
        return !clazz.isAnnotation() && !clazz.isArray() && !clazz.isPrimitive() && !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers());
    }

    public static int hierarchyDistance(Class<?> a, Class<?> b) {
        if (!a.isAssignableFrom(b)) {
            return -1;
        }
        if (a.equals(b)) {
            return 0;
        }
        int distance = 0;
        Class<?> current = b;
        while (current != null && !a.equals(current)) {
            current = current.getSuperclass();
            ++distance;
        }
        return distance;
    }

    public static int hierarchyDistanceBetween(Class<?> a, Class<?> b) {
        int dist = ClassUtils.hierarchyDistance(a, b);
        return dist != -1 ? dist : ClassUtils.hierarchyDistance(b, a);
    }

    public static boolean isRelatedTo(Class<?> a, Class<?> b) {
        return a.isAssignableFrom(b) || b.isAssignableFrom(a);
    }
}

