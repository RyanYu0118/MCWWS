/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.concurrent.NotThreadSafe
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.yggdrasil;

import ch.njol.yggdrasil.Tag;
import ch.njol.yggdrasil.Yggdrasil;
import ch.njol.yggdrasil.YggdrasilException;
import ch.njol.yggdrasil.YggdrasilSerializable;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.concurrent.NotThreadSafe;
import org.jetbrains.annotations.Nullable;

@NotThreadSafe
public final class Fields
implements Iterable<FieldContext> {
    @Nullable
    private final Yggdrasil yggdrasil;
    private final Map<String, FieldContext> fields = new HashMap<String, FieldContext>();
    private static final Map<Class<?>, Collection<Field>> cache = new HashMap();

    public Fields() {
        this.yggdrasil = null;
    }

    public Fields(Yggdrasil yggdrasil) {
        this.yggdrasil = yggdrasil;
    }

    public Fields(Class<?> type, Yggdrasil yggdrasil) throws NotSerializableException {
        this.yggdrasil = yggdrasil;
        for (Field field : Fields.getFields(type)) {
            assert (field != null);
            String id = Yggdrasil.getID(field);
            this.fields.put(id, new FieldContext(id));
        }
    }

    public Fields(Object object) throws NotSerializableException {
        this(object, null);
    }

    public Fields(Object object, @Nullable Yggdrasil yggdrasil) throws NotSerializableException {
        this.yggdrasil = yggdrasil;
        Class<?> type = object.getClass();
        assert (type != null);
        for (Field field : Fields.getFields(type)) {
            assert (field != null);
            try {
                this.fields.put(Yggdrasil.getID(field), new FieldContext(field, object));
            }
            catch (IllegalAccessException | IllegalArgumentException e) {
                assert (false);
            }
        }
    }

    public static Fields singletonObject(String fieldID, @Nullable Object value) {
        Fields fields = new Fields();
        fields.putObject(fieldID.toLowerCase(Locale.ENGLISH), value);
        return fields;
    }

    public static Fields singletonPrimitive(String fieldID, Object value) {
        Fields fields = new Fields();
        fields.putPrimitive(fieldID.toLowerCase(Locale.ENGLISH), value);
        return fields;
    }

    public <T, R> R mapObject(String fieldID, Class<T> type, Function<T, R> function) throws StreamCorruptedException {
        return function.apply(this.getObject(fieldID, type));
    }

    public <T, R> R mapPrimitive(String fieldID, Class<T> type, Function<T, R> function) throws StreamCorruptedException {
        return function.apply(this.getPrimitive(fieldID, type));
    }

    public static Collection<Field> getFields(Class<?> type) throws NotSerializableException {
        Collection<Field> fields = cache.get(type);
        if (fields != null) {
            return fields;
        }
        fields = new ArrayList<Field>();
        HashSet<String> ids = new HashSet<String>();
        for (Class<?> superClass = type; superClass != null; superClass = superClass.getSuperclass()) {
            Field[] declaredFields;
            for (Field field : declaredFields = superClass.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (field.isSynthetic() || Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) continue;
                String id = Yggdrasil.getID(field);
                if (ids.contains(id)) {
                    throw new NotSerializableException(String.valueOf(type) + "/" + String.valueOf(superClass) + ": duplicate field id '" + id + "'");
                }
                field.setAccessible(true);
                fields.add(field);
                ids.add(id);
            }
        }
        fields = Collections.unmodifiableCollection(fields);
        cache.put(type, fields);
        return fields;
    }

    public void setFields(Object object) throws StreamCorruptedException, NotSerializableException {
        Yggdrasil yggdrasil = this.yggdrasil;
        if (yggdrasil == null) {
            throw new YggdrasilException("");
        }
        HashSet<FieldContext> excessive = new HashSet<FieldContext>(this.fields.values());
        Class<?> type = object.getClass();
        assert (type != null);
        for (Field field : Fields.getFields(type)) {
            assert (field != null);
            String id = Yggdrasil.getID(field);
            FieldContext context = this.fields.get(id);
            if (context == null) {
                if (!(object instanceof YggdrasilSerializable.YggdrasilRobustSerializable) || !((YggdrasilSerializable.YggdrasilRobustSerializable)object).missingField(field)) {
                    yggdrasil.missingField(object, field);
                }
            } else {
                context.setField(object, field, yggdrasil);
            }
            excessive.remove(context);
        }
        for (FieldContext context : excessive) {
            assert (context != null);
            if (object instanceof YggdrasilSerializable.YggdrasilRobustSerializable && ((YggdrasilSerializable.YggdrasilRobustSerializable)object).excessiveField(context)) continue;
            yggdrasil.excessiveField(object, context);
        }
    }

    @Deprecated(since="2.3.0", forRemoval=true)
    public void setFields(Object object, Yggdrasil yggdrasil) throws StreamCorruptedException, NotSerializableException {
        assert (this.yggdrasil == yggdrasil);
        this.setFields(object);
    }

    public int size() {
        return this.fields.size();
    }

    public void putObject(String fieldID, @Nullable Object value) {
        fieldID = fieldID.toLowerCase(Locale.ENGLISH);
        FieldContext context = this.fields.computeIfAbsent(fieldID, FieldContext::new);
        context.setObject(value);
    }

    public void putPrimitive(String fieldID, Object value) {
        fieldID = fieldID.toLowerCase(Locale.ENGLISH);
        FieldContext context = this.fields.computeIfAbsent(fieldID, FieldContext::new);
        context.setPrimitive(value);
    }

    public boolean contains(String fieldID) {
        fieldID = fieldID.toLowerCase(Locale.ENGLISH);
        return this.fields.containsKey(fieldID);
    }

    public boolean hasField(String fieldID) {
        fieldID = fieldID.toLowerCase(Locale.ENGLISH);
        return this.fields.containsKey(fieldID);
    }

    @Nullable
    public Object getObject(String field) throws StreamCorruptedException {
        FieldContext context = this.fields.get(field = field.toLowerCase(Locale.ENGLISH));
        if (context == null) {
            throw new StreamCorruptedException("Nonexistent field " + field);
        }
        return context.getObject();
    }

    @Nullable
    public <T> T getObject(String fieldID, Class<T> expectedType) throws StreamCorruptedException {
        assert (!expectedType.isPrimitive());
        FieldContext context = this.fields.get(fieldID = fieldID.toLowerCase(Locale.ENGLISH));
        if (context == null) {
            throw new StreamCorruptedException("Nonexistent field " + fieldID);
        }
        return context.getObject(expectedType);
    }

    public Object getPrimitive(String fieldID) throws StreamCorruptedException {
        FieldContext context = this.fields.get(fieldID = fieldID.toLowerCase(Locale.ENGLISH));
        if (context == null) {
            throw new StreamCorruptedException("Nonexistent field " + fieldID);
        }
        return context.getPrimitive();
    }

    public <T> T getPrimitive(String fieldID, Class<T> expectedType) throws StreamCorruptedException {
        assert (expectedType.isPrimitive() || Tag.getPrimitiveFromWrapper(expectedType).isPrimitive());
        FieldContext context = this.fields.get(fieldID = fieldID.toLowerCase(Locale.ENGLISH));
        if (context == null) {
            throw new StreamCorruptedException("Nonexistent field " + fieldID);
        }
        return context.getPrimitive(expectedType);
    }

    @Nullable
    public <T> T getAndRemoveObject(String field, Class<T> expectedType) throws StreamCorruptedException {
        T object = this.getObject(field, expectedType);
        this.removeField(field);
        return object;
    }

    public <T> T getAndRemovePrimitive(String field, Class<T> expectedType) throws StreamCorruptedException {
        T object = this.getPrimitive(field, expectedType);
        this.removeField(field);
        return object;
    }

    public boolean removeField(String fieldID) {
        return this.fields.remove(fieldID = fieldID.toLowerCase(Locale.ENGLISH)) != null;
    }

    @Override
    public Iterator<FieldContext> iterator() {
        return this.fields.values().iterator();
    }

    @NotThreadSafe
    public static final class FieldContext {
        final String id;
        @Nullable
        private Object value;
        private boolean isPrimitiveValue;

        FieldContext(String id) {
            this.id = id.toLowerCase(Locale.ENGLISH);
        }

        FieldContext(Field field, Object object) throws IllegalArgumentException, IllegalAccessException {
            this.id = Yggdrasil.getID(field);
            this.value = field.get(object);
            this.isPrimitiveValue = field.getType().isPrimitive();
        }

        public String getID() {
            return this.id;
        }

        public boolean isPrimitive() {
            return this.isPrimitiveValue;
        }

        @Nullable
        public Class<?> getType() {
            Object value = this.value;
            if (value == null) {
                return null;
            }
            Class<?> type = value.getClass();
            assert (type != null);
            return this.isPrimitiveValue ? Tag.getPrimitiveFromWrapper(type).type : type;
        }

        @Nullable
        public Object getObject() throws StreamCorruptedException {
            if (this.isPrimitiveValue) {
                throw new StreamCorruptedException("field " + this.id + " is a primitive, but expected an object");
            }
            return this.value;
        }

        @Nullable
        public <T> T getObject(Class<T> expectedType) throws StreamCorruptedException {
            if (this.isPrimitiveValue) {
                throw new StreamCorruptedException("field " + this.id + " is a primitive, but expected " + String.valueOf(expectedType));
            }
            Object value = this.value;
            if (value != null && !expectedType.isInstance(value)) {
                throw new StreamCorruptedException("Field " + this.id + " of " + String.valueOf(value.getClass()) + ", but expected " + String.valueOf(expectedType));
            }
            return (T)value;
        }

        public Object getPrimitive() throws StreamCorruptedException {
            if (!this.isPrimitiveValue) {
                throw new StreamCorruptedException("field " + this.id + " is not a primitive, but expected one");
            }
            assert (this.value != null);
            return this.value;
        }

        public <T> T getPrimitive(Class<T> expectedType) throws StreamCorruptedException {
            if (!this.isPrimitiveValue) {
                throw new StreamCorruptedException("field " + this.id + " is not a primitive, but expected " + String.valueOf(expectedType));
            }
            assert (expectedType.isPrimitive() || Tag.isWrapper(expectedType));
            Object value = this.value;
            assert (value != null);
            if (!(!expectedType.isPrimitive() ? expectedType.isInstance(value) : Tag.getWrapperClass(expectedType).isInstance(value))) {
                throw new StreamCorruptedException("Field " + this.id + " of " + String.valueOf(value.getClass()) + ", but expected " + String.valueOf(expectedType));
            }
            return (T)value;
        }

        public void setObject(@Nullable Object value) {
            this.value = value;
            this.isPrimitiveValue = false;
        }

        public void setPrimitive(Object value) {
            assert (Tag.isWrapper(value.getClass()));
            this.value = value;
            this.isPrimitiveValue = true;
        }

        public void setField(Object object, Field field, Yggdrasil yggdrasil) throws StreamCorruptedException {
            block7: {
                if (Modifier.isStatic(field.getModifiers())) {
                    throw new StreamCorruptedException("The field " + this.id + " of " + String.valueOf(field.getDeclaringClass()) + " is static");
                }
                if (Modifier.isTransient(field.getModifiers())) {
                    throw new StreamCorruptedException("The field " + this.id + " of " + String.valueOf(field.getDeclaringClass()) + " is transient");
                }
                if (field.getType().isPrimitive() != this.isPrimitiveValue) {
                    throw new StreamCorruptedException("The field " + this.id + " of " + String.valueOf(field.getDeclaringClass()) + " is " + (field.getType().isPrimitive() ? "" : "not ") + "primitive");
                }
                try {
                    field.setAccessible(true);
                    field.set(object, this.value);
                }
                catch (IllegalArgumentException e) {
                    if (!(object instanceof YggdrasilSerializable.YggdrasilRobustSerializable) || !((YggdrasilSerializable.YggdrasilRobustSerializable)object).incompatibleField(field, this)) {
                        yggdrasil.incompatibleField(object, field, this);
                    }
                }
                catch (IllegalAccessException e) {
                    if ($assertionsDisabled) break block7;
                    throw new AssertionError();
                }
            }
        }

        public int hashCode() {
            return this.id.hashCode();
        }

        public boolean equals(@Nullable Object object) {
            if (this == object) {
                return true;
            }
            if (object == null) {
                return false;
            }
            if (!(object instanceof FieldContext)) {
                return false;
            }
            FieldContext other = (FieldContext)object;
            return this.id.equals(other.id);
        }
    }
}

