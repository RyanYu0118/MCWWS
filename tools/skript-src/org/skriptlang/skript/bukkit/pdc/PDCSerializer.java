/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.primitives.Primitives
 *  org.bukkit.Bukkit
 *  org.bukkit.NamespacedKey
 *  org.bukkit.persistence.PersistentDataAdapterContext
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.bukkit.pdc;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.Variables;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.Yggdrasil;
import ch.njol.yggdrasil.YggdrasilInputStream;
import ch.njol.yggdrasil.YggdrasilOutputStream;
import com.google.common.primitives.Primitives;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.bukkit.pdc.SkriptDataType;
import org.skriptlang.skript.lang.converter.Converters;

public class PDCSerializer {
    private static final NamespacedKey BLOB_KEY = new NamespacedKey("skript", "blob");
    private static final NamespacedKey TYPE_KEY = new NamespacedKey("skript", "pdc_type");
    private static final NamespacedKey VALUE_KEY = new NamespacedKey("skript", "pdc_value");
    private static final Map<Class<?>, PersistentDataType<?, ?>> REPRESENTABLE_TYPES = new LinkedHashMap();
    private static final Yggdrasil YGGDRASIL = Variables.yggdrasil;

    public static @Unmodifiable Collection<PersistentDataType<?, ?>> getRepresentablePDCTypes() {
        return Collections.unmodifiableCollection(REPRESENTABLE_TYPES.values());
    }

    public static PersistentDataType<?, ?> getPDCType(ClassInfo<?> classInfo) {
        if (REPRESENTABLE_TYPES.containsKey(classInfo.getC())) {
            return REPRESENTABLE_TYPES.get(classInfo.getC());
        }
        return SkriptDataType.get();
    }

    @NotNull
    public static PersistentDataContainer serialize(@NotNull Object unserializedData, @NotNull PersistentDataAdapterContext context) {
        return PDCSerializer.serialize(unserializedData, context, false);
    }

    @NotNull
    private static PersistentDataContainer serialize(@NotNull Object unserializedData, @NotNull PersistentDataAdapterContext context, boolean nested) {
        Serializer<?> serializer;
        assert (Bukkit.isPrimaryThread());
        ClassInfo<?> classInfo = Classes.getSuperClassInfo(unserializedData.getClass());
        if (classInfo.getSerializeAs() != null) {
            if ((classInfo = Classes.getExactClassInfo(classInfo.getSerializeAs())) == null) {
                assert (false) : unserializedData.getClass();
                return null;
            }
            if ((unserializedData = Converters.convert(unserializedData, classInfo.getC())) == null) {
                assert (false) : classInfo.getCodeName();
                return null;
            }
        }
        if ((serializer = classInfo.getSerializer()) == null) {
            if (nested) {
                return PDCSerializer.serializeToBase64(unserializedData, context);
            }
            throw new RuntimeException("The value " + String.valueOf(unserializedData) + " is not serializable!");
        }
        assert (!serializer.mustSyncDeserialization() || Bukkit.isPrimaryThread());
        PersistentDataContainer container = context.newPersistentDataContainer();
        if (REPRESENTABLE_TYPES.containsKey(classInfo.getC())) {
            container.set(TYPE_KEY, PersistentDataType.STRING, (Object)classInfo.getCodeName());
            PersistentDataType<?, ?> pdcType = REPRESENTABLE_TYPES.get(classInfo.getC());
            container.set(VALUE_KEY, pdcType, unserializedData);
            return container;
        }
        try {
            Fields fields = serializer.serialize(unserializedData);
            container.set(TYPE_KEY, PersistentDataType.STRING, (Object)classInfo.getCodeName());
            for (Fields.FieldContext field : fields) {
                Object data;
                NamespacedKey tag = new NamespacedKey("skript", field.getID());
                Object object = data = field.isPrimitive() ? field.getPrimitive() : field.getObject();
                if (data == null) {
                    container.set(tag, PersistentDataType.TAG_CONTAINER, (Object)context.newPersistentDataContainer());
                    continue;
                }
                if (field.isPrimitive() || data instanceof String) {
                    PersistentDataType<?, ?> type = REPRESENTABLE_TYPES.get(data.getClass());
                    if (type == null) {
                        throw new NotSerializableException("Unsupported primitive type: " + String.valueOf(data.getClass()));
                    }
                    container.set(tag, type, data);
                    continue;
                }
                data = PDCSerializer.serialize(data, context, true);
                container.set(tag, PersistentDataType.TAG_CONTAINER, (Object)((PersistentDataContainer)data));
            }
        }
        catch (NotSerializableException | StreamCorruptedException e) {
            throw new RuntimeException(e);
        }
        return container;
    }

    @NotNull
    public static Object deserialize(@NotNull PersistentDataContainer serializedData, @NotNull PersistentDataAdapterContext context) {
        if (serializedData.has(BLOB_KEY, PersistentDataType.STRING)) {
            return PDCSerializer.deserializeFromBase64(serializedData);
        }
        String typeName = (String)serializedData.get(TYPE_KEY, PersistentDataType.STRING);
        if (typeName == null) {
            throw new IllegalArgumentException("Cannot deserialize PDC because it has no type");
        }
        ClassInfo<?> classInfo = Classes.getClassInfo(typeName);
        Serializer<?> serializer = classInfo.getSerializer();
        if (serializer == null) {
            throw new IllegalArgumentException("Cannot deserialize " + classInfo.getCodeName() + " because it has no serializer");
        }
        if (REPRESENTABLE_TYPES.containsKey(classInfo.getC())) {
            PersistentDataType<?, ?> pdcType = REPRESENTABLE_TYPES.get(classInfo.getC());
            Object value = serializedData.get(VALUE_KEY, pdcType);
            if (value == null) {
                throw new IllegalArgumentException("Cannot deserialize " + classInfo.getCodeName() + " because its value is missing");
            }
            return value;
        }
        try {
            Fields fields = new Fields(YGGDRASIL);
            for (NamespacedKey key : serializedData.getKeys()) {
                if (key.equals((Object)TYPE_KEY)) continue;
                Object data = null;
                boolean primitive = true;
                for (Map.Entry<Class<?>, PersistentDataType<?, ?>> entry : REPRESENTABLE_TYPES.entrySet()) {
                    PersistentDataType<?, ?> type = entry.getValue();
                    if (!serializedData.has(key, type)) continue;
                    data = serializedData.get(key, type);
                    primitive = entry.getKey().isPrimitive() || PDCSerializer.isPrimitiveWrapper(entry.getKey());
                    break;
                }
                if (data == null) {
                    if (serializedData.has(key, PersistentDataType.TAG_CONTAINER)) {
                        PersistentDataContainer nestedContainer = (PersistentDataContainer)serializedData.get(key, PersistentDataType.TAG_CONTAINER);
                        assert (nestedContainer != null);
                        if (nestedContainer.isEmpty()) {
                            fields.putObject(key.getKey(), null);
                            continue;
                        }
                        data = PDCSerializer.deserialize(nestedContainer, context);
                        primitive = false;
                    } else {
                        throw new NotSerializableException("Unsupported data type for key: " + String.valueOf(key));
                    }
                }
                if (primitive) {
                    fields.putPrimitive(key.getKey(), data);
                    continue;
                }
                fields.putObject(key.getKey(), data);
            }
            assert (!serializer.mustSyncDeserialization() || Bukkit.isPrimaryThread());
            if (serializer.canBeInstantiated(classInfo.getC())) {
                Object obj = serializer.newInstance(classInfo.getC());
                serializer.deserialize(obj, fields);
                if (obj == null) {
                    throw new NotSerializableException("Could not deserialize object of type " + String.valueOf(classInfo.getC()));
                }
                return obj;
            }
            return serializer.deserialize(classInfo.getC(), fields);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static PersistentDataContainer serializeToBase64(@NotNull Object data, @NotNull PersistentDataAdapterContext context) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            YggdrasilOutputStream yggOut = YGGDRASIL.newOutputStream(byteOut);
            yggOut.writeObject(data);
            yggOut.flush();
            yggOut.close();
            PersistentDataContainer container = context.newPersistentDataContainer();
            container.set(BLOB_KEY, PersistentDataType.STRING, (Object)Base64.getEncoder().encodeToString(byteOut.toByteArray()));
            return container;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object deserializeFromBase64(@NotNull PersistentDataContainer container) {
        String blob = (String)container.get(BLOB_KEY, PersistentDataType.STRING);
        if (blob == null) {
            throw new IllegalArgumentException("Cannot deserialize PDC because blob value is missing");
        }
        try {
            YggdrasilInputStream yggIn = YGGDRASIL.newInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(blob)));
            return yggIn.readObject();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isPrimitiveWrapper(Class<?> key) {
        return Primitives.isWrapperType(key);
    }

    static {
        REPRESENTABLE_TYPES.put(Boolean.class, PersistentDataType.BOOLEAN);
        REPRESENTABLE_TYPES.put(Byte.class, PersistentDataType.BYTE);
        REPRESENTABLE_TYPES.put(Short.class, PersistentDataType.SHORT);
        REPRESENTABLE_TYPES.put(Integer.class, PersistentDataType.INTEGER);
        REPRESENTABLE_TYPES.put(Long.class, PersistentDataType.LONG);
        REPRESENTABLE_TYPES.put(Double.class, PersistentDataType.DOUBLE);
        REPRESENTABLE_TYPES.put(Float.class, PersistentDataType.FLOAT);
        REPRESENTABLE_TYPES.put(String.class, PersistentDataType.STRING);
    }
}

