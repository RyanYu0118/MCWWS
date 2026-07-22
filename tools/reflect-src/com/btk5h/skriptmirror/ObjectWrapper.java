/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.registrations.Classes
 */
package com.btk5h.skriptmirror;

import ch.njol.skript.registrations.Classes;
import com.btk5h.skriptmirror.util.JavaUtil;
import java.util.Objects;

public class ObjectWrapper {
    protected Object object;

    private ObjectWrapper(Object object) {
        this.object = object;
    }

    public static ObjectWrapper create(Object object) {
        if (object instanceof ObjectWrapper) {
            return (ObjectWrapper)object;
        }
        return new ObjectWrapper(object);
    }

    public static Object wrapIfNecessary(Object returnedValue, boolean forceWrap) {
        Class<?> returnedClass = returnedValue.getClass();
        if (returnedClass.isArray()) {
            returnedValue = ObjectWrapper.create(JavaUtil.boxPrimitiveArray(returnedValue));
        } else if (forceWrap || Classes.getSuperClassInfo(returnedClass).getC() == Object.class) {
            returnedValue = ObjectWrapper.create(returnedValue);
        }
        return returnedValue;
    }

    public static Object unwrapIfNecessary(Object o) {
        if (o instanceof ObjectWrapper) {
            return ((ObjectWrapper)o).get();
        }
        return o;
    }

    public Object get() {
        return this.object;
    }

    public boolean isArray() {
        if (this.object != null) {
            return this.object.getClass().isArray();
        }
        return false;
    }

    public String toString() {
        if (this.isArray()) {
            return JavaUtil.arrayToString(this.object, Object::toString);
        }
        return this.object.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ObjectWrapper that = (ObjectWrapper)o;
        return Objects.equals(this.object, that.object);
    }

    public int hashCode() {
        return Objects.hash(this.object);
    }
}

