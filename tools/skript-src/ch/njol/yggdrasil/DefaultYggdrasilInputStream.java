/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.yggdrasil;

import ch.njol.util.coll.CollectionUtils;
import ch.njol.yggdrasil.Tag;
import ch.njol.yggdrasil.Yggdrasil;
import ch.njol.yggdrasil.YggdrasilException;
import ch.njol.yggdrasil.YggdrasilInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class DefaultYggdrasilInputStream
extends YggdrasilInputStream {
    private final InputStream in;
    private final short version;
    private final List<String> readShortStrings = new ArrayList<String>();

    public DefaultYggdrasilInputStream(Yggdrasil yggdrasil, InputStream in) throws IOException {
        super(yggdrasil);
        this.in = in;
        if (this.readInt() != 1499948800) {
            throw new StreamCorruptedException("Not an Yggdrasil stream");
        }
        this.version = this.readShort();
        if (this.version <= 0 || this.version > 1) {
            throw new StreamCorruptedException("Input was saved using a later version of Yggdrasil");
        }
    }

    private int read() throws IOException {
        int b = this.in.read();
        if (b < 0) {
            throw new EOFException();
        }
        return b;
    }

    private void readFully(byte[] buf) throws IOException {
        this.readFully(buf, buf.length);
    }

    private void readFully(byte[] buf, int startLength) throws IOException {
        int n;
        int offset = 0;
        for (int length = startLength; length > 0; length -= n) {
            n = this.in.read(buf, offset, length);
            if (n < 0) {
                throw new EOFException("Expected " + startLength + " bytes, but could only read " + (startLength - length));
            }
            offset += n;
        }
    }

    private String readShortString() throws IOException {
        int length = this.read();
        if (length == (Tag.T_REFERENCE.tag & 0xFF)) {
            int i;
            int n = i = this.version <= 1 ? this.readInt() : this.readUnsignedInt();
            if (i < 0 || i > this.readShortStrings.size()) {
                throw new StreamCorruptedException("Invalid short string reference " + i);
            }
            return this.readShortStrings.get(i);
        }
        byte[] d = new byte[length];
        this.readFully(d);
        String s = new String(d, StandardCharsets.UTF_8);
        if (length > 4) {
            this.readShortStrings.add(s);
        }
        return s;
    }

    @Override
    protected Tag readTag() throws IOException {
        int t = this.read();
        Tag tag = Tag.byID(t);
        if (tag == null) {
            throw new StreamCorruptedException("Invalid tag 0x" + Integer.toHexString(t));
        }
        return tag;
    }

    private byte readByte() throws IOException {
        return (byte)this.read();
    }

    private short readShort() throws IOException {
        return (short)(this.read() << 8 | this.read());
    }

    private short readUnsignedShort() throws IOException {
        int b = this.read();
        if ((b & 0x80) != 0) {
            return (short)(b & 0xFFFFFF7F);
        }
        return (short)(b << 8 | this.read());
    }

    private int readInt() throws IOException {
        return this.read() << 24 | this.read() << 16 | this.read() << 8 | this.read();
    }

    private int readUnsignedInt() throws IOException {
        int b = this.read();
        if ((b & 0x80) != 0) {
            return (b & 0xFFFFFF7F) << 8 | this.read();
        }
        return b << 24 | this.read() << 16 | this.read() << 8 | this.read();
    }

    private long readLong() throws IOException {
        return (long)this.in.read() << 56 | (long)this.in.read() << 48 | (long)this.in.read() << 40 | (long)this.in.read() << 32 | (long)this.in.read() << 24 | (long)this.in.read() << 16 | (long)this.in.read() << 8 | (long)this.read();
    }

    private float readFloat() throws IOException {
        return Float.intBitsToFloat(this.readInt());
    }

    private double readDouble() throws IOException {
        return Double.longBitsToDouble(this.readLong());
    }

    private char readChar() throws IOException {
        return (char)this.readShort();
    }

    private boolean readBoolean() throws IOException {
        int r = this.read();
        if (r == 0) {
            return false;
        }
        if (r == 1) {
            return true;
        }
        throw new StreamCorruptedException("Invalid boolean value " + r);
    }

    @Override
    protected Object readPrimitive(Tag type) throws IOException {
        switch (type) {
            case T_BYTE: {
                return this.readByte();
            }
            case T_SHORT: {
                return this.readShort();
            }
            case T_INT: {
                return this.readInt();
            }
            case T_LONG: {
                return this.readLong();
            }
            case T_FLOAT: {
                return Float.valueOf(this.readFloat());
            }
            case T_DOUBLE: {
                return this.readDouble();
            }
            case T_CHAR: {
                return Character.valueOf(this.readChar());
            }
            case T_BOOLEAN: {
                return this.readBoolean();
            }
        }
        throw new YggdrasilException("Internal error; " + String.valueOf((Object)type));
    }

    @Override
    protected Object readPrimitive_(Tag type) throws IOException {
        return this.readPrimitive(type);
    }

    @Override
    protected String readString() throws IOException {
        int length = this.readUnsignedInt();
        byte[] d = new byte[length];
        this.readFully(d);
        return new String(d, StandardCharsets.UTF_8);
    }

    @Override
    protected Class<?> readArrayComponentType() throws IOException {
        return this.readClass();
    }

    @Override
    protected int readArrayLength() throws IOException {
        return this.readUnsignedInt();
    }

    @Override
    protected Class<?> readEnumType() throws IOException {
        return this.yggdrasil.getClass(this.readShortString());
    }

    @Override
    protected String readEnumID() throws IOException {
        return this.readShortString();
    }

    @Override
    protected Class<?> readClass() throws IOException {
        Tag type;
        int dimensions = 0;
        while ((type = this.readTag()) == Tag.T_ARRAY) {
            ++dimensions;
        }
        Class<Object> clazz = switch (type) {
            case Tag.T_OBJECT, Tag.T_ENUM -> this.yggdrasil.getClass(this.readShortString());
            case Tag.T_BYTE, Tag.T_SHORT, Tag.T_INT, Tag.T_LONG, Tag.T_FLOAT, Tag.T_DOUBLE, Tag.T_CHAR, Tag.T_BOOLEAN, Tag.T_BOOLEAN_OBJ, Tag.T_BYTE_OBJ, Tag.T_CHAR_OBJ, Tag.T_DOUBLE_OBJ, Tag.T_FLOAT_OBJ, Tag.T_INT_OBJ, Tag.T_LONG_OBJ, Tag.T_SHORT_OBJ, Tag.T_CLASS, Tag.T_STRING -> {
                if (!$assertionsDisabled && type.type == null) {
                    throw new AssertionError();
                }
                yield type.type;
            }
            case Tag.T_NULL, Tag.T_REFERENCE -> throw new StreamCorruptedException("unexpected tag " + String.valueOf((Object)type));
            default -> throw new YggdrasilException("Internal error; " + String.valueOf((Object)type));
        };
        while (dimensions-- > 0) {
            clazz = CollectionUtils.arrayType(clazz);
        }
        return clazz;
    }

    @Override
    protected int readReference() throws IOException {
        return this.readUnsignedInt();
    }

    @Override
    protected Class<?> readObjectType() throws IOException {
        return this.yggdrasil.getClass(this.readShortString());
    }

    @Override
    protected short readNumFields() throws IOException {
        return this.readUnsignedShort();
    }

    @Override
    protected String readFieldID() throws IOException {
        return this.readShortString();
    }

    @Override
    public void close() throws IOException {
        try {
            try {
                this.read();
                throw new StreamCorruptedException("Stream still has data, at least " + (1 + this.in.available()) + " bytes remain");
            }
            catch (EOFException eOFException) {
                this.in.close();
            }
        }
        catch (Throwable throwable) {
            this.in.close();
            throw throwable;
        }
    }
}

