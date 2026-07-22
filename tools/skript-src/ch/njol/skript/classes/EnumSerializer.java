/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.classes;

import ch.njol.skript.classes.Serializer;
import ch.njol.yggdrasil.Fields;
import java.io.StreamCorruptedException;
import org.jetbrains.annotations.Nullable;

public class EnumSerializer<T extends Enum<T>>
extends Serializer<T> {
    private final Class<T> c;

    public EnumSerializer(Class<T> c) {
        this.c = c;
    }

    @Override
    @Deprecated(since="2.3.0", forRemoval=true)
    @Nullable
    public T deserialize(String s) {
        try {
            return Enum.valueOf(this.c, s);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public boolean mustSyncDeserialization() {
        return false;
    }

    @Override
    public boolean canBeInstantiated() {
        return false;
    }

    @Override
    public Fields serialize(T e) {
        Fields fields = new Fields();
        fields.putObject("name", ((Enum)e).name());
        return fields;
    }

    @Override
    public T deserialize(Fields fields) {
        try {
            String name = fields.getAndRemoveObject("name", String.class);
            if (name == null) {
                return null;
            }
            return Enum.valueOf(this.c, name);
        }
        catch (StreamCorruptedException | IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void deserialize(T o, Fields f) {
        assert (false);
    }
}

