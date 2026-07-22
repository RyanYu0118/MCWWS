/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.yggdrasil;

import ch.njol.yggdrasil.PseudoEnum;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public final class Tag
extends Enum<Tag> {
    public static final /* enum */ Tag T_NULL = new Tag(0, null, "null");
    public static final /* enum */ Tag T_BYTE = new Tag(1, Byte.TYPE, "byte");
    public static final /* enum */ Tag T_SHORT = new Tag(2, Short.TYPE, "short");
    public static final /* enum */ Tag T_INT = new Tag(3, Integer.TYPE, "int");
    public static final /* enum */ Tag T_LONG = new Tag(4, Long.TYPE, "long");
    public static final /* enum */ Tag T_FLOAT = new Tag(8, Float.TYPE, "float");
    public static final /* enum */ Tag T_DOUBLE = new Tag(9, Double.TYPE, "double");
    public static final /* enum */ Tag T_CHAR = new Tag(14, Character.TYPE, "char");
    public static final /* enum */ Tag T_BOOLEAN = new Tag(15, Boolean.TYPE, "boolean");
    public static final /* enum */ Tag T_BYTE_OBJ = new Tag(16 + Tag.T_BYTE.tag, Byte.class, "Byte");
    public static final /* enum */ Tag T_SHORT_OBJ = new Tag(16 + Tag.T_SHORT.tag, Short.class, "Short");
    public static final /* enum */ Tag T_INT_OBJ = new Tag(16 + Tag.T_INT.tag, Integer.class, "Integer");
    public static final /* enum */ Tag T_LONG_OBJ = new Tag(16 + Tag.T_LONG.tag, Long.class, "Long");
    public static final /* enum */ Tag T_FLOAT_OBJ = new Tag(16 + Tag.T_FLOAT.tag, Float.class, "Float");
    public static final /* enum */ Tag T_DOUBLE_OBJ = new Tag(16 + Tag.T_DOUBLE.tag, Double.class, "Double");
    public static final /* enum */ Tag T_CHAR_OBJ = new Tag(16 + Tag.T_CHAR.tag, Character.class, "Character");
    public static final /* enum */ Tag T_BOOLEAN_OBJ = new Tag(16 + Tag.T_BOOLEAN.tag, Boolean.class, "Boolean");
    public static final /* enum */ Tag T_STRING = new Tag(32, String.class, "string");
    public static final /* enum */ Tag T_ARRAY = new Tag(48, null, "array");
    public static final /* enum */ Tag T_ENUM = new Tag(64, null, "enum");
    public static final /* enum */ Tag T_CLASS = new Tag(65, Class.class, "class");
    public static final /* enum */ Tag T_OBJECT = new Tag(128, Object.class, "object");
    public static final /* enum */ Tag T_REFERENCE = new Tag(255, null, "reference");
    public static final int MIN_PRIMITIVE;
    public static final int MAX_PRIMITIVE;
    public static final int MIN_WRAPPER;
    public static final int MAX_WRAPPER;
    public final byte tag;
    @Nullable
    public final Class<?> type;
    public final String name;
    private static final Map<Class<?>, Tag> types;
    private static final Tag[] byID;
    private static final Map<String, Tag> byName;
    private static final HashMap<Class<?>, Tag> wrapperTypes;
    private static final /* synthetic */ Tag[] $VALUES;

    public static Tag[] values() {
        return (Tag[])$VALUES.clone();
    }

    public static Tag valueOf(String name) {
        return Enum.valueOf(Tag.class, name);
    }

    private Tag(int tag, Class<?> type, String name) {
        assert (0 <= tag && tag <= 255) : tag;
        this.tag = (byte)tag;
        this.type = type;
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

    public boolean isPrimitive() {
        return MIN_PRIMITIVE <= this.tag && this.tag <= MAX_PRIMITIVE;
    }

    @Nullable
    public Tag getPrimitive() {
        if (!this.isWrapper()) {
            assert (false);
            return null;
        }
        return byID[this.tag - MIN_WRAPPER + MIN_PRIMITIVE];
    }

    public boolean isWrapper() {
        return MIN_WRAPPER <= this.tag && this.tag <= MAX_WRAPPER;
    }

    public Tag getWrapper() {
        if (!this.isPrimitive()) {
            assert (false);
            return T_NULL;
        }
        return byID[this.tag - MIN_PRIMITIVE + MIN_WRAPPER];
    }

    public static Tag getType(@Nullable Class<?> type) {
        if (type == null) {
            return T_NULL;
        }
        Tag t = types.get(type);
        if (t != null) {
            return t;
        }
        return type.isArray() ? T_ARRAY : (Enum.class.isAssignableFrom(type) || PseudoEnum.class.isAssignableFrom(type) ? T_ENUM : T_OBJECT);
    }

    @Nullable
    public static Tag byID(byte tag) {
        return byID[tag & 0xFF];
    }

    @Nullable
    public static Tag byID(int tag) {
        return byID[tag];
    }

    @Nullable
    public static Tag byName(String name) {
        return byName.get(name);
    }

    public static boolean isWrapper(Class<?> type) {
        return wrapperTypes.containsKey(type);
    }

    public static Tag getPrimitiveFromWrapper(Class<?> wrapper) {
        Tag tag = wrapperTypes.get(wrapper);
        if (tag == null) {
            assert (false) : wrapper;
            return T_NULL;
        }
        return tag;
    }

    public static Class<?> getWrapperClass(Class<?> primitive) {
        assert (primitive.isPrimitive());
        Tag tag = types.get(primitive);
        if (tag == null) {
            assert (false) : primitive;
            return Object.class;
        }
        Class<?> wrapper = tag.getWrapper().type;
        assert (wrapper != null) : tag;
        return wrapper;
    }

    private static /* synthetic */ Tag[] $values() {
        return new Tag[]{T_NULL, T_BYTE, T_SHORT, T_INT, T_LONG, T_FLOAT, T_DOUBLE, T_CHAR, T_BOOLEAN, T_BYTE_OBJ, T_SHORT_OBJ, T_INT_OBJ, T_LONG_OBJ, T_FLOAT_OBJ, T_DOUBLE_OBJ, T_CHAR_OBJ, T_BOOLEAN_OBJ, T_STRING, T_ARRAY, T_ENUM, T_CLASS, T_OBJECT, T_REFERENCE};
    }

    static {
        $VALUES = Tag.$values();
        MIN_PRIMITIVE = Tag.T_BYTE.tag;
        MAX_PRIMITIVE = Tag.T_BOOLEAN.tag;
        MIN_WRAPPER = Tag.T_BYTE_OBJ.tag;
        MAX_WRAPPER = Tag.T_BOOLEAN_OBJ.tag;
        types = new HashMap();
        byID = new Tag[256];
        byName = new HashMap<String, Tag>();
        for (Tag tag : Tag.values()) {
            types.put(tag.type, tag);
            Tag.byID[tag.tag & 0xFF] = tag;
            byName.put(tag.name, tag);
        }
        wrapperTypes = new HashMap();
        wrapperTypes.put(Byte.class, T_BYTE);
        wrapperTypes.put(Short.class, T_SHORT);
        wrapperTypes.put(Integer.class, T_INT);
        wrapperTypes.put(Long.class, T_LONG);
        wrapperTypes.put(Float.class, T_FLOAT);
        wrapperTypes.put(Double.class, T_DOUBLE);
        wrapperTypes.put(Character.class, T_CHAR);
        wrapperTypes.put(Boolean.class, T_BOOLEAN);
    }
}

