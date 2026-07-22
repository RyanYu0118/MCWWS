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
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public abstract class YggdrasilInputStream
implements Closeable {
    protected final Yggdrasil yggdrasil;
    private final List<Object> readObjects = new ArrayList<Object>();

    protected YggdrasilInputStream(Yggdrasil yggdrasil) {
        this.yggdrasil = yggdrasil;
    }

    protected abstract Tag readTag() throws IOException;

    protected abstract Object readPrimitive(Tag var1) throws IOException;

    protected abstract Object readPrimitive_(Tag var1) throws IOException;

    protected abstract String readString() throws IOException;

    protected abstract Class<?> readArrayComponentType() throws IOException;

    protected abstract int readArrayLength() throws IOException;

    private void readArrayContents(Object array) throws IOException {
        if (array.getClass().getComponentType().isPrimitive()) {
            int length = Array.getLength(array);
            Tag type = Tag.getType(array.getClass().getComponentType());
            for (int i = 0; i < length; ++i) {
                Array.set(array, i, this.readPrimitive_(type));
            }
        } else {
            for (int i = 0; i < ((Object[])array).length; ++i) {
                ((Object[])array)[i] = this.readObject();
            }
        }
    }

    protected abstract Class<?> readEnumType() throws IOException;

    protected abstract String readEnumID() throws IOException;

    private Object readEnum() throws IOException {
        Class<?> c = this.readEnumType();
        String id = this.readEnumID();
        if (Enum.class.isAssignableFrom(c)) {
            return Yggdrasil.getEnumConstant(c, id);
        }
        if (PseudoEnum.class.isAssignableFrom(c)) {
            Object o = PseudoEnum.valueOf(c, id);
            if (o != null) {
                return o;
            }
            throw new StreamCorruptedException("Enum constant " + id + " does not exist in " + String.valueOf(c));
        }
        throw new StreamCorruptedException(String.valueOf(c) + " is not an enum type");
    }

    protected abstract Class<?> readClass() throws IOException;

    protected abstract int readReference() throws IOException;

    protected abstract Class<?> readObjectType() throws IOException;

    protected abstract short readNumFields() throws IOException;

    protected abstract String readFieldID() throws IOException;

    private Fields readFields() throws IOException {
        Fields fields = new Fields(this.yggdrasil);
        int numFields = this.readNumFields();
        for (int i = 0; i < numFields; ++i) {
            String id = this.readFieldID();
            Tag tag = this.readTag();
            if (tag.isPrimitive()) {
                fields.putPrimitive(id, this.readPrimitive(tag));
                continue;
            }
            fields.putObject(id, this.readObject(tag));
        }
        return fields;
    }

    @Nullable
    public final Object readObject() throws IOException {
        Tag tag = this.readTag();
        return this.readObject(tag);
    }

    @Nullable
    public final <T> T readObject(Class<T> expectedType) throws IOException {
        Tag tag = this.readTag();
        Object object = this.readObject(tag);
        if (object != null && !expectedType.isInstance(object)) {
            throw new StreamCorruptedException("Object " + String.valueOf(object) + " is of " + String.valueOf(object.getClass()) + " but expected " + String.valueOf(expectedType));
        }
        return (T)object;
    }

    @Nullable
    private Object readObject(Tag tag) throws IOException {
        Object object;
        if (tag == Tag.T_NULL) {
            return null;
        }
        if (tag == Tag.T_REFERENCE) {
            int ref = this.readReference();
            if (ref < 0 || ref >= this.readObjects.size()) {
                throw new StreamCorruptedException("Invalid reference " + ref + ", " + this.readObjects.size() + " object(s) read so far");
            }
            Object object2 = this.readObjects.get(ref);
            if (object2 == null) {
                throw new StreamCorruptedException("Reference to uninstantiable object: " + ref);
            }
            return object2;
        }
        switch (tag) {
            case T_ARRAY: {
                Class<?> type = this.readArrayComponentType();
                Object object3 = Array.newInstance(type, this.readArrayLength());
                this.readObjects.add(object3);
                this.readArrayContents(object3);
                return object3;
            }
            case T_CLASS: {
                object = this.readClass();
                break;
            }
            case T_ENUM: {
                object = this.readEnum();
                break;
            }
            case T_STRING: {
                object = this.readString();
                break;
            }
            case T_OBJECT: {
                Object object4;
                Class<?> c = this.readObjectType();
                YggdrasilSerializer<?> s = this.yggdrasil.getSerializer(c);
                if (s != null && !s.canBeInstantiated(c)) {
                    int ref = this.readObjects.size();
                    this.readObjects.add(null);
                    Fields fields = this.readFields();
                    object4 = s.deserialize(c, fields);
                    if (object4 == null) {
                        throw new YggdrasilException("YggdrasilSerializer " + String.valueOf(s) + " returned null from deserialize(" + String.valueOf(c) + "," + String.valueOf(fields) + ")");
                    }
                    this.readObjects.set(ref, object4);
                } else {
                    object4 = this.yggdrasil.newInstance(c);
                    if (object4 == null) {
                        throw new StreamCorruptedException();
                    }
                    this.readObjects.add(object4);
                    Fields fields = this.readFields();
                    if (s != null) {
                        s.deserialize(object4, fields);
                    } else if (object4 instanceof YggdrasilSerializable.YggdrasilExtendedSerializable) {
                        ((YggdrasilSerializable.YggdrasilExtendedSerializable)object4).deserialize(fields);
                    } else {
                        fields.setFields(object4);
                    }
                }
                return object4;
            }
            case T_BOOLEAN_OBJ: 
            case T_BYTE_OBJ: 
            case T_CHAR_OBJ: 
            case T_DOUBLE_OBJ: 
            case T_FLOAT_OBJ: 
            case T_INT_OBJ: 
            case T_LONG_OBJ: 
            case T_SHORT_OBJ: {
                Tag primitive = tag.getPrimitive();
                assert (primitive != null);
                object = this.readPrimitive(primitive);
                break;
            }
            case T_BYTE: 
            case T_BOOLEAN: 
            case T_CHAR: 
            case T_DOUBLE: 
            case T_FLOAT: 
            case T_INT: 
            case T_LONG: 
            case T_SHORT: {
                throw new StreamCorruptedException();
            }
            default: {
                assert (false);
                throw new StreamCorruptedException();
            }
        }
        this.readObjects.add(object);
        return object;
    }
}

