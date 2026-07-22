/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.YggdrasilSerializer;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.jetbrains.annotations.Nullable;

public abstract class Serializer<T>
extends YggdrasilSerializer<T> {
    @Nullable
    protected ClassInfo<? extends T> info = null;

    void register(ClassInfo<? extends T> info) {
        assert (this.info == null && info != null);
        this.info = info;
    }

    @Override
    @Nullable
    public Class<? extends T> getClass(String id) {
        ClassInfo<T> info = this.info;
        assert (info != null);
        return id.equals(info.getCodeName()) ? info.getC() : null;
    }

    @Override
    @Nullable
    public String getID(Class<?> c) {
        ClassInfo<T> info = this.info;
        assert (info != null);
        return info.getC().isAssignableFrom(c) ? info.getCodeName() : null;
    }

    @Override
    @Nullable
    public <E extends T> E newInstance(Class<E> c) {
        ClassInfo<T> info = this.info;
        assert (info != null);
        assert (info.getC().isAssignableFrom(c));
        try {
            Constructor<E> constr = c.getDeclaredConstructor(new Class[0]);
            constr.setAccessible(true);
            return constr.newInstance(new Object[0]);
        }
        catch (InstantiationException e) {
            throw new SkriptAPIException("Serializer of " + info.getCodeName() + " must override newInstance(), canBeInstantiated() or mustSyncDeserialization() if its class does not have a nullary constructor");
        }
        catch (NoSuchMethodException e) {
            throw new SkriptAPIException("Serializer of " + info.getCodeName() + " must override newInstance(), canBeInstantiated() or mustSyncDeserialization() if its class does not have a nullary constructor");
        }
        catch (SecurityException e) {
            throw Skript.exception("Security manager present");
        }
        catch (IllegalArgumentException e) {
            assert (false);
            return null;
        }
        catch (IllegalAccessException e) {
            assert (false);
            return null;
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public abstract Fields serialize(T var1) throws NotSerializableException;

    @Override
    public void deserialize(T o, Fields f) throws StreamCorruptedException, NotSerializableException {
        throw new SkriptAPIException("deserialize(Object, Fields) has not been overridden in " + String.valueOf(this.getClass()) + " (serializer of " + String.valueOf(this.info) + ")");
    }

    public abstract boolean mustSyncDeserialization();

    @Override
    public boolean canBeInstantiated(Class<? extends T> c) {
        assert (this.info != null && this.info.getC().isAssignableFrom(c));
        return this.canBeInstantiated();
    }

    protected abstract boolean canBeInstantiated();

    @Override
    public <E extends T> E deserialize(Class<E> c, Fields fields) throws StreamCorruptedException, NotSerializableException {
        ClassInfo<T> info = this.info;
        assert (info != null);
        assert (info.getC().isAssignableFrom(c));
        return (E)this.deserialize(fields);
    }

    protected T deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
        throw new SkriptAPIException("deserialize(Fields) has not been overridden in " + String.valueOf(this.getClass()) + " (serializer of " + String.valueOf(this.info) + ")");
    }

    @Deprecated(since="2.3.0", forRemoval=true)
    @Nullable
    public T deserialize(String s) {
        return null;
    }
}

