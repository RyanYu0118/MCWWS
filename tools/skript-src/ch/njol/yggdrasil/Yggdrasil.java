/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.concurrent.NotThreadSafe
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.yggdrasil;

import ch.njol.yggdrasil.ClassResolver;
import ch.njol.yggdrasil.DefaultYggdrasilInputStream;
import ch.njol.yggdrasil.DefaultYggdrasilOutputStream;
import ch.njol.yggdrasil.FieldHandler;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.JRESerializer;
import ch.njol.yggdrasil.PseudoEnum;
import ch.njol.yggdrasil.SimpleClassResolver;
import ch.njol.yggdrasil.Tag;
import ch.njol.yggdrasil.YggdrasilException;
import ch.njol.yggdrasil.YggdrasilID;
import ch.njol.yggdrasil.YggdrasilInputStream;
import ch.njol.yggdrasil.YggdrasilOutputStream;
import ch.njol.yggdrasil.YggdrasilSerializable;
import ch.njol.yggdrasil.YggdrasilSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.concurrent.NotThreadSafe;
import org.jetbrains.annotations.Nullable;

@NotThreadSafe
public final class Yggdrasil {
    public static final int MAGIC_NUMBER = 1499948800;
    public static final short LATEST_VERSION = 1;
    public final short version;
    private final List<ClassResolver> classResolvers = new ArrayList<ClassResolver>();
    private final List<FieldHandler> fieldHandlers = new ArrayList<FieldHandler>();
    private final SimpleClassResolver simpleClassResolver = new SimpleClassResolver();

    public Yggdrasil() {
        this(1);
    }

    public Yggdrasil(short version) {
        if (version <= 0 || version > 1) {
            throw new YggdrasilException("Unsupported version number");
        }
        this.version = version;
        this.classResolvers.add(new JRESerializer());
        this.classResolvers.add(this.simpleClassResolver);
    }

    public YggdrasilOutputStream newOutputStream(OutputStream out) throws IOException {
        return new DefaultYggdrasilOutputStream(this, out);
    }

    public YggdrasilInputStream newInputStream(InputStream in) throws IOException {
        return new DefaultYggdrasilInputStream(this, in);
    }

    public void registerClassResolver(ClassResolver resolver) {
        if (!this.classResolvers.contains(resolver)) {
            this.classResolvers.add(resolver);
        }
    }

    public void registerSingleClass(Class<?> type, String id) {
        this.simpleClassResolver.registerClass(type, id);
    }

    public void registerSingleClass(Class<?> type) {
        YggdrasilID id = type.getAnnotation(YggdrasilID.class);
        if (id == null) {
            throw new IllegalArgumentException(type.toString());
        }
        this.simpleClassResolver.registerClass(type, id.value());
    }

    public void registerFieldHandler(FieldHandler handler) {
        if (!this.fieldHandlers.contains(handler)) {
            this.fieldHandlers.add(handler);
        }
    }

    public boolean isSerializable(Class<?> type) {
        try {
            return type.isPrimitive() || type == Object.class || (Enum.class.isAssignableFrom(type) || PseudoEnum.class.isAssignableFrom(type)) && this.getIDNoError(type) != null || (YggdrasilSerializable.class.isAssignableFrom(type) || this.getSerializer(type) != null) && this.newInstance(type) != type;
        }
        catch (StreamCorruptedException e) {
            return false;
        }
        catch (NotSerializableException e) {
            return false;
        }
    }

    @Nullable
    YggdrasilSerializer<?> getSerializer(Class<?> type) {
        for (ClassResolver resolver : this.classResolvers) {
            if (!(resolver instanceof YggdrasilSerializer) || resolver.getID(type) == null) continue;
            return (YggdrasilSerializer)resolver;
        }
        return null;
    }

    public Class<?> getClass(String id) throws StreamCorruptedException {
        if ("Object".equals(id)) {
            return Object.class;
        }
        for (ClassResolver resolver : this.classResolvers) {
            Class<?> type = resolver.getClass(id);
            if (type == null) continue;
            assert (Tag.byName(id) == null && (Tag.getType(type) == Tag.T_OBJECT || Tag.getType(type) == Tag.T_ENUM)) : "Tag IDs should not be matched: " + id + " (class resolver: " + String.valueOf(resolver) + ")";
            assert (id.equals(resolver.getID(type))) : String.valueOf(resolver) + " returned " + String.valueOf(type) + " for id " + id + ", but returns id " + resolver.getID(type) + " for that class";
            return type;
        }
        throw new StreamCorruptedException("No class found for ID " + id);
    }

    @Nullable
    private String getIDNoError(Class<?> type) {
        if (type == Object.class) {
            return "Object";
        }
        assert (Tag.getType(type) == Tag.T_OBJECT || Tag.getType(type) == Tag.T_ENUM);
        if (Enum.class.isAssignableFrom(type) && type.getSuperclass() != Enum.class) {
            Class<?> s = type.getSuperclass();
            assert (s != null);
            type = s;
        }
        if (PseudoEnum.class.isAssignableFrom(type)) {
            type = PseudoEnum.getDeclaringClass(type);
        }
        for (ClassResolver resolver : this.classResolvers) {
            String id = resolver.getID(type);
            if (id == null) continue;
            assert (Tag.byName(id) == null) : "Class IDs should not match Tag IDs: " + id + " (class resolver: " + String.valueOf(resolver) + ")";
            Class<?> c2 = resolver.getClass(id);
            assert (c2 != null && (!(resolver instanceof YggdrasilSerializer) ? resolver.getClass(id) == type : id.equals(resolver.getID(c2)))) : String.valueOf(resolver) + " returned id " + id + " for " + String.valueOf(type) + ", but returns " + String.valueOf(c2) + " for that id";
            return id;
        }
        return null;
    }

    public String getID(Class<?> type) throws NotSerializableException {
        String id = this.getIDNoError(type);
        if (id == null) {
            throw new NotSerializableException("No ID found for " + String.valueOf(type));
        }
        if (!this.isSerializable(type)) {
            throw new NotSerializableException(type.getCanonicalName());
        }
        return id;
    }

    public static String getID(Field field) {
        YggdrasilID yid = field.getAnnotation(YggdrasilID.class);
        if (yid != null) {
            return yid.value().toLowerCase(Locale.ENGLISH);
        }
        return field.getName().toLowerCase(Locale.ENGLISH);
    }

    public static String getID(Enum<?> e) {
        try {
            return Yggdrasil.getID(e.getDeclaringClass().getDeclaredField(e.name()));
        }
        catch (NoSuchFieldException ex) {
            assert (false) : e;
            return e.name().toLowerCase(Locale.ENGLISH);
        }
    }

    public static <T extends Enum<T>> Enum<T> getEnumConstant(Class<T> type, String id) throws StreamCorruptedException {
        Field[] fields;
        for (Field field : fields = type.getDeclaredFields()) {
            assert (field != null);
            if (!Yggdrasil.getID(field).equalsIgnoreCase(id)) continue;
            return Enum.valueOf(type, field.getName());
        }
        if (YggdrasilSerializable.YggdrasilRobustEnum.class.isAssignableFrom(type)) {
            T[] constants = type.getEnumConstants();
            if (constants.length == 0) {
                throw new StreamCorruptedException(String.valueOf(type) + " does not have any enum constants");
            }
            Enum<?> e = ((YggdrasilSerializable.YggdrasilRobustEnum)constants[0]).excessiveConstant(id);
            if (!type.isInstance(e)) {
                throw new YggdrasilException(String.valueOf(type) + " returned a foreign enum constant: " + String.valueOf(e.getClass()) + "." + String.valueOf(e));
            }
            return e;
        }
        throw new StreamCorruptedException("Enum constant " + id + " does not exist in " + String.valueOf(type));
    }

    public void excessiveField(Object object, Fields.FieldContext field) throws StreamCorruptedException {
        for (FieldHandler handler : this.fieldHandlers) {
            if (!handler.excessiveField(object, field)) continue;
            return;
        }
        throw new StreamCorruptedException("Excessive field " + field.id + " in class " + object.getClass().getCanonicalName() + " was not handled");
    }

    public void missingField(Object object, Field field) throws StreamCorruptedException {
        for (FieldHandler handler : this.fieldHandlers) {
            if (!handler.missingField(object, field)) continue;
            return;
        }
        throw new StreamCorruptedException("Missing field " + Yggdrasil.getID(field) + " in class " + object.getClass().getCanonicalName() + " was not handled");
    }

    public void incompatibleField(Object object, Field field, Fields.FieldContext context) throws StreamCorruptedException {
        for (FieldHandler handler : this.fieldHandlers) {
            if (!handler.incompatibleField(object, field, context)) continue;
            return;
        }
        throw new StreamCorruptedException("Incompatible field " + Yggdrasil.getID(field) + " in class " + object.getClass().getCanonicalName() + " of incompatible " + String.valueOf(context.getType()) + " was not handled");
    }

    public void saveToFile(Object object, File file) throws IOException {
        try (FileOutputStream fout = new FileOutputStream(file);
             YggdrasilOutputStream yout = this.newOutputStream(fout);){
            yout.writeObject(object);
            yout.flush();
        }
    }

    @Nullable
    public <T> T loadFromFile(File file, Class<T> expectedType) throws IOException {
        try (FileInputStream fin = new FileInputStream(file);){
            T t;
            block11: {
                YggdrasilInputStream yin = this.newInputStream(fin);
                try {
                    t = yin.readObject(expectedType);
                    if (yin == null) break block11;
                }
                catch (Throwable throwable) {
                    if (yin != null) {
                        try {
                            yin.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                yin.close();
            }
            return t;
        }
    }

    @Nullable
    Object newInstance(Class<?> type) throws StreamCorruptedException, NotSerializableException {
        YggdrasilSerializer<?> serializer = this.getSerializer(type);
        if (serializer != null) {
            if (!serializer.canBeInstantiated(type)) {
                try {
                    serializer.deserialize(type, new Fields(this));
                }
                catch (StreamCorruptedException streamCorruptedException) {
                    // empty catch block
                }
                return null;
            }
            Object o = serializer.newInstance(type);
            if (o == null) {
                throw new YggdrasilException("YggdrasilSerializer " + String.valueOf(serializer) + " returned null from newInstance(" + String.valueOf(type) + ")");
            }
            return o;
        }
        try {
            Constructor<?> constr = type.getDeclaredConstructor(new Class[0]);
            constr.setAccessible(true);
            return constr.newInstance(new Object[0]);
        }
        catch (NoSuchMethodException e) {
            throw new StreamCorruptedException("Cannot create an instance of " + String.valueOf(type) + " because it has no nullary constructor");
        }
        catch (SecurityException e) {
            throw new StreamCorruptedException("Cannot create an instance of " + String.valueOf(type) + " because the security manager didn't allow it");
        }
        catch (InstantiationException e) {
            throw new StreamCorruptedException("Cannot create an instance of " + String.valueOf(type) + " because it is abstract");
        }
        catch (IllegalAccessException | IllegalArgumentException e) {
            e.printStackTrace();
            assert (false);
            return null;
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}

