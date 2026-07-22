/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.yggdrasil;

import ch.njol.yggdrasil.ClassResolver;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.YggdrasilException;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import org.jetbrains.annotations.Nullable;

public abstract class YggdrasilSerializer<T>
implements ClassResolver {
    @Nullable
    public abstract Class<? extends T> getClass(String var1);

    public abstract Fields serialize(T var1) throws NotSerializableException;

    public boolean canBeInstantiated(Class<? extends T> type) {
        return true;
    }

    @Nullable
    public abstract <E extends T> E newInstance(Class<E> var1);

    public abstract void deserialize(T var1, Fields var2) throws StreamCorruptedException, NotSerializableException;

    public <E extends T> E deserialize(Class<E> type, Fields fields) throws StreamCorruptedException, NotSerializableException {
        throw new YggdrasilException(String.valueOf(this.getClass()) + " does not override deserialize(Class, Fields)");
    }
}

