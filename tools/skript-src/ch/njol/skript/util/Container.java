/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Iterator;

public interface Container<T> {
    public Iterator<T> containerIterator();

    @Target(value={ElementType.TYPE})
    @Retention(value=RetentionPolicy.RUNTIME)
    @Documented
    public static @interface ContainerType {
        public Class<?> value();
    }
}

