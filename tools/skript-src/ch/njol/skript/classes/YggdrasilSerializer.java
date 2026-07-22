/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.classes;

import ch.njol.skript.classes.Serializer;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.YggdrasilSerializable;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;

public class YggdrasilSerializer<T extends YggdrasilSerializable>
extends Serializer<T> {
    @Override
    public final Fields serialize(T o) throws NotSerializableException {
        if (o instanceof YggdrasilSerializable.YggdrasilExtendedSerializable) {
            return ((YggdrasilSerializable.YggdrasilExtendedSerializable)o).serialize();
        }
        return new Fields(o);
    }

    @Override
    public final void deserialize(T o, Fields f) throws StreamCorruptedException, NotSerializableException {
        if (o instanceof YggdrasilSerializable.YggdrasilExtendedSerializable) {
            ((YggdrasilSerializable.YggdrasilExtendedSerializable)o).deserialize(f);
        } else {
            f.setFields(o);
        }
    }

    @Override
    @Deprecated(since="2.3.0", forRemoval=true)
    public T deserialize(String s) {
        return null;
    }

    @Override
    public boolean mustSyncDeserialization() {
        return false;
    }

    @Override
    public boolean canBeInstantiated() {
        return true;
    }
}

