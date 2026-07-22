/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.yggdrasil;

import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.PseudoEnum;
import ch.njol.yggdrasil.Tag;
import ch.njol.yggdrasil.Yggdrasil;
import ch.njol.yggdrasil.YggdrasilException;
import ch.njol.yggdrasil.YggdrasilSerializable;
import ch.njol.yggdrasil.YggdrasilSerializer;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.lang.reflect.Array;
import java.util.IdentityHashMap;
import org.jetbrains.annotations.Nullable;

public abstract class YggdrasilOutputStream
implements Flushable,
Closeable {
    protected final Yggdrasil yggdrasil;
    private int nextObjectID = 0;
    private final IdentityHashMap<Object, Integer> writtenObjects = new IdentityHashMap();

    protected YggdrasilOutputStream(Yggdrasil yggdrasil) {
        this.yggdrasil = yggdrasil;
    }

    protected abstract void writeTag(Tag var1) throws IOException;

    private void writeNull() throws IOException {
        this.writeTag(Tag.T_NULL);
    }

    protected abstract void writePrimitiveValue(Object var1) throws IOException;

    protected abstract void writePrimitive_(Object var1) throws IOException;

    private void writePrimitive(Object object) throws IOException {
        Tag tag = Tag.getType(object.getClass());
        assert (tag.isWrapper());
        Tag primitive = tag.getPrimitive();
        assert (primitive != null);
        this.writeTag(primitive);
        this.writePrimitiveValue(object);
    }

    private void writeWrappedPrimitive(Object object) throws IOException {
        Tag tag = Tag.getType(object.getClass());
        assert (tag.isWrapper());
        this.writeTag(tag);
        this.writePrimitiveValue(object);
    }

    protected abstract void writeStringValue(String var1) throws IOException;

    private void writeString(String string) throws IOException {
        this.writeTag(Tag.T_STRING);
        this.writeStringValue(string);
    }

    protected abstract void writeArrayComponentType(Class<?> var1) throws IOException;

    protected abstract void writeArrayLength(int var1) throws IOException;

    protected abstract void writeArrayEnd() throws IOException;

    private void writeArray(Object array) throws IOException {
        int length = Array.getLength(array);
        Class<?> type = array.getClass().getComponentType();
        assert (type != null);
        this.writeTag(Tag.T_ARRAY);
        this.writeArrayComponentType(type);
        this.writeArrayLength(length);
        if (type.isPrimitive()) {
            for (int i = 0; i < length; ++i) {
                Object object = Array.get(array, i);
                assert (object != null);
                this.writePrimitive_(object);
            }
            this.writeArrayEnd();
        } else {
            for (Object object : (Object[])array) {
                this.writeObject(object);
            }
            this.writeArrayEnd();
        }
    }

    protected abstract void writeEnumType(String var1) throws IOException;

    protected abstract void writeEnumID(String var1) throws IOException;

    private void writeEnum(Enum<?> object) throws IOException {
        this.writeTag(Tag.T_ENUM);
        Class<?> type = object.getDeclaringClass();
        this.writeEnumType(this.yggdrasil.getID(type));
        this.writeEnumID(Yggdrasil.getID(object));
    }

    private void writeEnum(PseudoEnum<?> object) throws IOException {
        this.writeTag(Tag.T_ENUM);
        this.writeEnumType(this.yggdrasil.getID(object.getDeclaringClass()));
        this.writeEnumID(object.name());
    }

    protected abstract void writeClassType(Class<?> var1) throws IOException;

    private void writeClass(Class<?> type) throws IOException {
        this.writeTag(Tag.T_CLASS);
        this.writeClassType(type);
    }

    protected abstract void writeReferenceID(int var1) throws IOException;

    protected final void writeReference(int ref) throws IOException {
        assert (ref >= 0);
        this.writeTag(Tag.T_REFERENCE);
        this.writeReferenceID(ref);
    }

    protected abstract void writeObjectType(String var1) throws IOException;

    protected abstract void writeNumFields(short var1) throws IOException;

    protected abstract void writeFieldID(String var1) throws IOException;

    protected abstract void writeObjectEnd() throws IOException;

    private void writeGenericObject(Object object, int ref) throws IOException {
        Fields fields;
        Class<?> type = object.getClass();
        assert (type != null);
        if (!this.yggdrasil.isSerializable(type)) {
            throw new NotSerializableException(type.getName());
        }
        YggdrasilSerializer<?> serializer = this.yggdrasil.getSerializer(type);
        if (serializer != null) {
            fields = serializer.serialize(object);
            if (!serializer.canBeInstantiated(type)) {
                this.writtenObjects.put(object, ref ^= 0xFFFFFFFF);
            }
        } else {
            fields = object instanceof YggdrasilSerializable.YggdrasilExtendedSerializable ? ((YggdrasilSerializable.YggdrasilExtendedSerializable)object).serialize() : new Fields(object, this.yggdrasil);
        }
        if (fields.size() > Short.MAX_VALUE) {
            throw new YggdrasilException("Class " + type.getCanonicalName() + " has too many fields (" + fields.size() + ")");
        }
        this.writeTag(Tag.T_OBJECT);
        this.writeObjectType(this.yggdrasil.getID(type));
        this.writeNumFields((short)fields.size());
        for (Fields.FieldContext field : fields) {
            this.writeFieldID(field.id);
            if (field.isPrimitive()) {
                this.writePrimitive(field.getPrimitive());
                continue;
            }
            this.writeObject(field.getObject());
        }
        this.writeObjectEnd();
        if (ref < 0) {
            this.writtenObjects.put(object, ~ref);
        }
    }

    public final void writeObject(@Nullable Object object) throws IOException {
        if (object == null) {
            this.writeNull();
            return;
        }
        if (this.writtenObjects.containsKey(object)) {
            int ref = this.writtenObjects.get(object);
            if (ref < 0) {
                throw new YggdrasilException("Uninstantiable object " + String.valueOf(object) + " is referenced in its fields' graph");
            }
            this.writeReference(ref);
            return;
        }
        int ref = this.nextObjectID++;
        this.writtenObjects.put(object, ref);
        Tag type = Tag.getType(object.getClass());
        if (type.isWrapper()) {
            this.writeWrappedPrimitive(object);
            return;
        }
        switch (type) {
            case T_ARRAY: {
                this.writeArray(object);
                return;
            }
            case T_STRING: {
                this.writeString((String)object);
                return;
            }
            case T_ENUM: {
                if (object instanceof Enum) {
                    this.writeEnum((Enum)object);
                } else {
                    this.writeEnum((PseudoEnum)object);
                }
                return;
            }
            case T_CLASS: {
                this.writeClass((Class)object);
                return;
            }
            case T_OBJECT: {
                this.writeGenericObject(object, ref);
                return;
            }
        }
        throw new YggdrasilException("unhandled type " + String.valueOf((Object)type));
    }
}

