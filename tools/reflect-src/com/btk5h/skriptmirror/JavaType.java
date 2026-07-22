/*
 * Decompiled with CFR 0.152.
 */
package com.btk5h.skriptmirror;

import java.util.Objects;

public final class JavaType {
    private final Class<?> javaClass;

    public JavaType(Class<?> javaClass) {
        this.javaClass = javaClass;
    }

    public Class<?> getJavaClass() {
        return this.javaClass;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        JavaType javaType1 = (JavaType)o;
        return Objects.equals(this.javaClass, javaType1.javaClass);
    }

    public int hashCode() {
        return Objects.hash(this.javaClass);
    }
}

