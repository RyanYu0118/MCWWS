/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.concurrent.ThreadSafe
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.yggdrasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.concurrent.ThreadSafe;
import org.jetbrains.annotations.Nullable;

@ThreadSafe
public abstract class PseudoEnum<T extends PseudoEnum<T>> {
    private final String name;
    private final int ordinal;
    private final Info<T> info;
    private static final Map<Class<? extends PseudoEnum<?>>, Info<?>> infos = new HashMap();

    protected PseudoEnum(String name) throws IllegalArgumentException {
        this.name = name;
        this.info = PseudoEnum.getInfo(this.getClass());
        this.info.writeLock.lock();
        try {
            if (this.info.map.containsKey(name)) {
                throw new IllegalArgumentException("Duplicate name '" + name + "'");
            }
            this.ordinal = this.info.values.size();
            this.info.values.add(this);
            this.info.map.put(name, this);
        }
        finally {
            this.info.writeLock.unlock();
        }
    }

    public final String name() {
        return this.name;
    }

    public String toString() {
        return this.name;
    }

    public final int ordinal() {
        return this.ordinal;
    }

    public final int hashCode() {
        return this.ordinal;
    }

    public final boolean equals(@Nullable Object object) {
        return object == this;
    }

    protected final Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public final Class<T> getDeclaringClass() {
        return PseudoEnum.getDeclaringClass(this.getClass());
    }

    public static <T extends PseudoEnum<T>> Class<? super T> getDeclaringClass(Class<T> type) {
        Class<T> superClass = type;
        while (superClass.isAnonymousClass()) {
            superClass = superClass.getSuperclass();
        }
        return superClass;
    }

    public final List<T> values() {
        return PseudoEnum.values(this.getDeclaringClass());
    }

    public static <T extends PseudoEnum<T>> List<T> values(Class<T> type) throws IllegalArgumentException {
        if (type != PseudoEnum.getDeclaringClass(type)) {
            throw new IllegalArgumentException(String.valueOf(type) + " != " + String.valueOf(PseudoEnum.getDeclaringClass(type)));
        }
        return PseudoEnum.values(PseudoEnum.getInfo(type));
    }

    private static <T extends PseudoEnum<T>> List<T> values(Info<T> info) {
        info.readLock.lock();
        try {
            ArrayList arrayList = new ArrayList(info.values);
            return arrayList;
        }
        finally {
            info.readLock.unlock();
        }
    }

    public final T getConstant(int id) throws IndexOutOfBoundsException {
        this.info.readLock.lock();
        try {
            PseudoEnum pseudoEnum = (PseudoEnum)this.info.values.get(id);
            return (T)pseudoEnum;
        }
        finally {
            this.info.readLock.unlock();
        }
    }

    public final int numConstants() {
        this.info.readLock.lock();
        try {
            int n = this.info.values.size();
            return n;
        }
        finally {
            this.info.readLock.unlock();
        }
    }

    @Nullable
    public final T valueOf(String name) {
        this.info.readLock.lock();
        try {
            PseudoEnum pseudoEnum = (PseudoEnum)this.info.map.get(name);
            return (T)pseudoEnum;
        }
        finally {
            this.info.readLock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Nullable
    public static <T extends PseudoEnum<T>> T valueOf(Class<T> type, String name) {
        Info<T> info = PseudoEnum.getInfo(type);
        info.readLock.lock();
        try {
            PseudoEnum pseudoEnum = (PseudoEnum)info.map.get(name);
            return (T)pseudoEnum;
        }
        finally {
            info.readLock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static <T extends PseudoEnum<T>> Info<T> getInfo(Class<T> type) {
        Map<Class<? extends PseudoEnum<?>>, Info<?>> map = infos;
        synchronized (map) {
            Info<Object> info = infos.get(PseudoEnum.getDeclaringClass(type));
            if (info == null) {
                info = new Info();
                infos.put(type, info);
            }
            return info;
        }
    }

    private static final class Info<T extends PseudoEnum<T>> {
        final List<T> values = new ArrayList<T>();
        final Map<String, T> map = new HashMap<String, T>();
        final ReadWriteLock lock = new ReentrantReadWriteLock(true);
        final Lock readLock = this.lock.readLock();
        final Lock writeLock = this.lock.writeLock();

        private Info() {
        }
    }
}

