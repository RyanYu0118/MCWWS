/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.yggdrasil;

import ch.njol.yggdrasil.Tag;
import ch.njol.yggdrasil.Yggdrasil;
import ch.njol.yggdrasil.YggdrasilException;
import ch.njol.yggdrasil.YggdrasilOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class DefaultYggdrasilOutputStream
extends YggdrasilOutputStream {
    private final OutputStream out;
    private final short version;
    private final Map<String, Integer> writtenShortStrings = new HashMap<String, Integer>();
    int nextShortStringID = 0;

    public DefaultYggdrasilOutputStream(Yggdrasil yggdrasil, OutputStream out) throws IOException {
        super(yggdrasil);
        this.out = out;
        this.version = yggdrasil.version;
        this.writeInt(1499948800);
        this.writeShort(this.version);
    }

    private void write(int b) throws IOException {
        this.out.write(b);
    }

    @Override
    protected void writeTag(Tag tag) throws IOException {
        this.out.write(tag.tag);
    }

    private void writeShortString(String string) throws IOException {
        if (this.writtenShortStrings.containsKey(string)) {
            this.writeTag(Tag.T_REFERENCE);
            if (this.version <= 1) {
                this.writeInt(this.writtenShortStrings.get(string));
            } else {
                this.writeUnsignedInt(this.writtenShortStrings.get(string));
            }
        } else {
            if (this.nextShortStringID < 0) {
                throw new YggdrasilException("Too many field names/class IDs (max: 2147483647)");
            }
            byte[] d = string.getBytes(StandardCharsets.UTF_8);
            if (d.length >= (Tag.T_REFERENCE.tag & 0xFF)) {
                throw new YggdrasilException("Field name or Class ID too long: " + string);
            }
            this.write(d.length);
            this.out.write(d);
            if (d.length > 4) {
                this.writtenShortStrings.put(string, this.nextShortStringID++);
            }
        }
    }

    private void writeByte(byte b) throws IOException {
        this.write(b & 0xFF);
    }

    private void writeShort(short s) throws IOException {
        this.write(s >>> 8 & 0xFF);
        this.write(s & 0xFF);
    }

    private void writeUnsignedShort(short s) throws IOException {
        assert (s >= 0);
        if (s <= 127) {
            this.writeByte((byte)(0x80 | s));
        } else {
            this.writeShort(s);
        }
    }

    private void writeInt(int i) throws IOException {
        this.write(i >>> 24 & 0xFF);
        this.write(i >>> 16 & 0xFF);
        this.write(i >>> 8 & 0xFF);
        this.write(i & 0xFF);
    }

    private void writeUnsignedInt(int i) throws IOException {
        assert (i >= 0);
        if (i <= Short.MAX_VALUE) {
            this.writeShort((short)(0x8000 | i));
        } else {
            this.writeInt(i);
        }
    }

    private void writeLong(long l) throws IOException {
        this.write((int)(l >>> 56 & 0xFFL));
        this.write((int)(l >>> 48 & 0xFFL));
        this.write((int)(l >>> 40 & 0xFFL));
        this.write((int)(l >>> 32 & 0xFFL));
        this.write((int)(l >>> 24 & 0xFFL));
        this.write((int)(l >>> 16 & 0xFFL));
        this.write((int)(l >>> 8 & 0xFFL));
        this.write((int)(l & 0xFFL));
    }

    private void writeFloat(float f) throws IOException {
        this.writeInt(Float.floatToIntBits(f));
    }

    private void writeDouble(double d) throws IOException {
        this.writeLong(Double.doubleToLongBits(d));
    }

    private void writeChar(char c) throws IOException {
        this.writeShort((short)c);
    }

    private void writeBoolean(boolean b) throws IOException {
        this.write(b ? 1 : 0);
    }

    @Override
    protected void writePrimitive_(Object object) throws IOException {
        switch (Tag.getPrimitiveFromWrapper(object.getClass())) {
            case T_BYTE: {
                this.writeByte((Byte)object);
                break;
            }
            case T_SHORT: {
                this.writeShort((Short)object);
                break;
            }
            case T_INT: {
                this.writeInt((Integer)object);
                break;
            }
            case T_LONG: {
                this.writeLong((Long)object);
                break;
            }
            case T_FLOAT: {
                this.writeFloat(((Float)object).floatValue());
                break;
            }
            case T_DOUBLE: {
                this.writeDouble((Double)object);
                break;
            }
            case T_CHAR: {
                this.writeChar(((Character)object).charValue());
                break;
            }
            case T_BOOLEAN: {
                this.writeBoolean((Boolean)object);
                break;
            }
            default: {
                throw new YggdrasilException("Invalid call to writePrimitive with argument " + String.valueOf(object));
            }
        }
    }

    @Override
    protected void writePrimitiveValue(Object object) throws IOException {
        this.writePrimitive_(object);
    }

    @Override
    protected void writeStringValue(String string) throws IOException {
        byte[] d = string.getBytes(StandardCharsets.UTF_8);
        this.writeUnsignedInt(d.length);
        this.out.write(d);
    }

    @Override
    protected void writeArrayComponentType(Class<?> componentType) throws IOException {
        this.writeClass_(componentType);
    }

    @Override
    protected void writeArrayLength(int length) throws IOException {
        this.writeUnsignedInt(length);
    }

    @Override
    protected void writeArrayEnd() {
    }

    @Override
    protected void writeClassType(Class<?> type) throws IOException {
        this.writeClass_(type);
    }

    private void writeClass_(Class<?> type) throws IOException {
        while (type.isArray()) {
            this.writeTag(Tag.T_ARRAY);
            type = type.getComponentType();
        }
        Tag tag = Tag.getType(type);
        switch (tag) {
            case T_OBJECT: 
            case T_ENUM: {
                this.writeTag(tag);
                this.writeShortString(this.yggdrasil.getID(type));
                break;
            }
            case T_BYTE: 
            case T_SHORT: 
            case T_INT: 
            case T_LONG: 
            case T_FLOAT: 
            case T_DOUBLE: 
            case T_CHAR: 
            case T_BOOLEAN: 
            case T_BOOLEAN_OBJ: 
            case T_BYTE_OBJ: 
            case T_CHAR_OBJ: 
            case T_DOUBLE_OBJ: 
            case T_FLOAT_OBJ: 
            case T_INT_OBJ: 
            case T_LONG_OBJ: 
            case T_SHORT_OBJ: 
            case T_CLASS: 
            case T_STRING: {
                this.writeTag(tag);
                break;
            }
            default: {
                throw new YggdrasilException(type.getCanonicalName());
            }
        }
    }

    @Override
    protected void writeEnumType(String type) throws IOException {
        this.writeShortString(type);
    }

    @Override
    protected void writeEnumID(String id) throws IOException {
        this.writeShortString(id);
    }

    @Override
    protected void writeObjectType(String type) throws IOException {
        this.writeShortString(type);
    }

    @Override
    protected void writeNumFields(short numFields) throws IOException {
        this.writeUnsignedShort(numFields);
    }

    @Override
    protected void writeFieldID(String id) throws IOException {
        this.writeShortString(id);
    }

    @Override
    protected void writeObjectEnd() {
    }

    @Override
    protected void writeReferenceID(int reference) throws IOException {
        this.writeUnsignedInt(reference);
    }

    @Override
    public void flush() throws IOException {
        this.out.flush();
    }

    @Override
    public void close() throws IOException {
        this.out.close();
    }
}

