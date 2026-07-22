/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.yggdrasil;

import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.YggdrasilSerializer;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import org.jetbrains.annotations.Nullable;

public abstract class SimpleClassSerializer<T>
extends YggdrasilSerializer<T> {
    protected final Class<T> type;
    protected final String id;

    public SimpleClassSerializer(Class<T> type, String id) {
        this.type = type;
        this.id = id;
    }

    @Override
    @Nullable
    public Class<? extends T> getClass(String id) {
        return this.id.equals(id) ? this.type : null;
    }

    @Override
    @Nullable
    public String getID(Class<?> clazz) {
        return this.type.equals(clazz) ? this.id : null;
    }

    public static abstract class NonInstantiableClassSerializer<T>
    extends SimpleClassSerializer<T> {
        public NonInstantiableClassSerializer(Class<T> type, String id) {
            super(type, id);
        }

        @Override
        public final boolean canBeInstantiated(Class<? extends T> type) {
            return false;
        }

        @Override
        @Nullable
        public final <E extends T> E newInstance(Class<E> c) {
            return null;
        }

        @Override
        public final void deserialize(T object, Fields fields) throws StreamCorruptedException, NotSerializableException {
            throw new UnsupportedOperationException("This class cannot be instantiated");
        }

        @Override
        public <E extends T> E deserialize(Class<E> type, Fields fields) throws StreamCorruptedException, NotSerializableException {
            assert (this.type.equals(type));
            return (E)this.deserialize(fields);
        }

        protected abstract T deserialize(Fields var1) throws StreamCorruptedException, NotSerializableException;
    }
}

