/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.aliases.ItemType
 *  ch.njol.skript.classes.ClassInfo
 *  org.bukkit.inventory.ItemStack
 */
package com.btk5h.skriptmirror.util;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.ClassInfo;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.Null;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.bukkit.inventory.ItemStack;

public final class JavaUtil {
    public static final Map<Class<?>, Class<?>> WRAPPER_CLASSES = new HashMap();
    public static final Set<Class<?>> NUMERIC_CLASSES = new HashSet();
    public static final Map<String, Class<?>> PRIMITIVE_CLASS_NAMES = new HashMap();

    public static Stream<Field> fields(Class<?> cls) {
        return Stream.concat(Arrays.stream(cls.getFields()), Arrays.stream(cls.getDeclaredFields())).distinct();
    }

    public static Stream<Method> methods(Class<?> cls) {
        return Stream.concat(Arrays.stream(cls.getMethods()), Arrays.stream(cls.getDeclaredMethods())).distinct();
    }

    public static Stream<Constructor> constructors(Class<?> cls) {
        return Arrays.stream(cls.getDeclaredConstructors());
    }

    public static String toGenericString(Member o) {
        if (o instanceof Field) {
            return ((Field)o).toGenericString();
        }
        if (o instanceof Method) {
            return ((Method)o).toGenericString();
        }
        if (o instanceof Constructor) {
            return ((Constructor)o).toGenericString();
        }
        return null;
    }

    public static Object boxPrimitiveArray(Object obj) {
        Class<?> componentType = obj.getClass().getComponentType();
        if (componentType == null || !componentType.isPrimitive()) {
            return obj;
        }
        int length = Array.getLength(obj);
        if (componentType == Integer.TYPE) {
            int[] source = (int[])obj;
            Integer[] target = new Integer[length];
            for (int i = 0; i < length; ++i) {
                target[i] = source[i];
            }
            return target;
        }
        if (componentType == Long.TYPE) {
            long[] source = (long[])obj;
            Long[] target = new Long[length];
            for (int i = 0; i < length; ++i) {
                target[i] = source[i];
            }
            return target;
        }
        if (componentType == Double.TYPE) {
            double[] source = (double[])obj;
            Double[] target = new Double[length];
            for (int i = 0; i < length; ++i) {
                target[i] = source[i];
            }
            return target;
        }
        if (componentType == Float.TYPE) {
            float[] source = (float[])obj;
            Float[] target = new Float[length];
            for (int i = 0; i < length; ++i) {
                target[i] = Float.valueOf(source[i]);
            }
            return target;
        }
        if (componentType == Boolean.TYPE) {
            boolean[] source = (boolean[])obj;
            Boolean[] target = new Boolean[length];
            for (int i = 0; i < length; ++i) {
                target[i] = source[i];
            }
            return target;
        }
        if (componentType == Byte.TYPE) {
            byte[] source = (byte[])obj;
            Byte[] target = new Byte[length];
            for (int i = 0; i < length; ++i) {
                target[i] = source[i];
            }
            return target;
        }
        if (componentType == Character.TYPE) {
            char[] source = (char[])obj;
            Character[] target = new Character[length];
            for (int i = 0; i < length; ++i) {
                target[i] = Character.valueOf(source[i]);
            }
            return target;
        }
        if (componentType == Short.TYPE) {
            short[] source = (short[])obj;
            Short[] target = new Short[length];
            for (int i = 0; i < length; ++i) {
                target[i] = source[i];
            }
            return target;
        }
        return obj;
    }

    public static Object convertNumericArray(Object array, Class<?> to) {
        Object newArray;
        Class<?> componentType = array.getClass().getComponentType();
        int length = Array.getLength(array);
        if (componentType.isArray()) {
            newArray = Array.newInstance(componentType, length);
            for (int i = 0; i < length; ++i) {
                Object innerArray = Array.get(array, i);
                if (innerArray == null) {
                    innerArray = Array.newInstance(componentType.getComponentType(), 0);
                    Array.set(newArray, i, innerArray);
                    continue;
                }
                Array.set(newArray, i, JavaUtil.convertNumericArray(innerArray, to));
            }
        } else {
            newArray = Array.newInstance(to, length);
            for (int i = 0; i < length; ++i) {
                Object what = Array.get(array, i);
                if (to == Byte.TYPE || to == Byte.class) {
                    what = ((Number)what).byteValue();
                } else if (to == Double.TYPE || to == Double.class) {
                    what = ((Number)what).doubleValue();
                } else if (to == Float.TYPE || to == Float.class) {
                    what = Float.valueOf(((Number)what).floatValue());
                } else if (to == Integer.TYPE || to == Integer.class) {
                    what = ((Number)what).intValue();
                } else if (to == Long.TYPE || to == Long.class) {
                    what = ((Number)what).longValue();
                } else if (to == Short.TYPE || to == Short.class) {
                    what = ((Number)what).shortValue();
                }
                Array.set(newArray, i, what);
            }
        }
        return newArray;
    }

    public static int getArrayDepth(Class<?> cls) {
        int depth = 0;
        while (cls.isArray()) {
            cls = cls.getComponentType();
            ++depth;
        }
        return depth;
    }

    public static Class<?> getBaseComponent(Class<?> obj) {
        Class<?> componentType = obj.getComponentType();
        while (componentType.isArray()) {
            componentType = componentType.getComponentType();
        }
        return componentType;
    }

    public static <T, R> Function<T, R> propagateErrors(ExceptionalFunction<T, R> function) {
        return t -> {
            try {
                return function.apply(t);
            }
            catch (Exception e) {
                Skript.warning((String)("skript-reflect encountered a " + e.getClass().getSimpleName() + ": " + e.getMessage()));
                if (Skript.logVeryHigh()) {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    Skript.warning((String)errors.toString());
                } else {
                    Skript.warning((String)"Run Skript with the verbosity 'very high' for the stack trace.");
                }
                return null;
            }
        };
    }

    public static boolean isNumericClass(Class<?> cls) {
        return Number.class.isAssignableFrom(cls) || NUMERIC_CLASSES.contains(cls);
    }

    public static <T> T[] newArray(Class<? extends T> type, int length) {
        return (Object[])Array.newInstance(type, length);
    }

    public static <T> Class<?> getArrayClass(Class<T> type) {
        return Array.newInstance(type, 0).getClass();
    }

    public static Class<?> getArrayClass(Class<?> type, int layers) {
        for (int i = 0; i < layers; ++i) {
            type = JavaUtil.getArrayClass(type);
        }
        return type;
    }

    public static String arrayToString(Object array, Function<Object, String> function) {
        int length = Array.getLength(array);
        StringBuilder stringBuilder = new StringBuilder("[");
        for (int i = 0; i < length; ++i) {
            Object value = Array.get(array, i);
            stringBuilder.append(function.apply(value));
            if (i == length - 1) continue;
            stringBuilder.append(", ");
        }
        return stringBuilder.append("]").toString();
    }

    public static boolean canConvert(Object object, Class<?> to) {
        if (to.isInstance(object)) {
            return true;
        }
        if (object instanceof Number && NUMERIC_CLASSES.contains(to)) {
            return true;
        }
        if (to.isArray() && JavaUtil.getArrayDepth(to) == JavaUtil.getArrayDepth(object.getClass())) {
            Class<?> paramComponent = JavaUtil.getBaseComponent(to);
            Class<?> argComponent = JavaUtil.getBaseComponent(object.getClass());
            if (JavaUtil.isNumericClass(paramComponent) && JavaUtil.isNumericClass(argComponent)) {
                return true;
            }
        }
        if (to.isPrimitive() && WRAPPER_CLASSES.get(to).isInstance(object)) {
            return true;
        }
        if (object instanceof String && (to == Character.TYPE || to == Character.class) && ((String)object).length() == 1) {
            return true;
        }
        if (object instanceof ItemType && to == ItemStack.class) {
            return true;
        }
        if (to == Class.class && (object instanceof JavaType || object instanceof ClassInfo)) {
            return true;
        }
        return !to.isPrimitive() && object instanceof Null;
    }

    public static Object convert(Object object, Class<?> to) {
        if (object instanceof Number && NUMERIC_CLASSES.contains(to)) {
            if (to == Byte.TYPE || to == Byte.class) {
                return ((Number)object).byteValue();
            }
            if (to == Double.TYPE || to == Double.class) {
                return ((Number)object).doubleValue();
            }
            if (to == Float.TYPE || to == Float.class) {
                return Float.valueOf(((Number)object).floatValue());
            }
            if (to == Integer.TYPE || to == Integer.class) {
                return ((Number)object).intValue();
            }
            if (to == Long.TYPE || to == Long.class) {
                return ((Number)object).longValue();
            }
            if (to == Short.TYPE || to == Short.class) {
                return ((Number)object).shortValue();
            }
        }
        if (to.isArray() && JavaUtil.getArrayDepth(to) == JavaUtil.getArrayDepth(object.getClass()) && JavaUtil.isNumericClass(JavaUtil.getBaseComponent(to))) {
            return JavaUtil.convertNumericArray(object, JavaUtil.getBaseComponent(to));
        }
        if (object instanceof String && (to == Character.TYPE || to == Character.class)) {
            return Character.valueOf(((String)object).charAt(0));
        }
        if (object instanceof ItemType && to == ItemStack.class) {
            return ((ItemType)object).getRandom();
        }
        if (to == Class.class) {
            if (object instanceof JavaType) {
                return ((JavaType)object).getJavaClass();
            }
            if (object instanceof ClassInfo) {
                return ((ClassInfo)object).getC();
            }
        }
        if (object instanceof Null) {
            return null;
        }
        return object;
    }

    static {
        WRAPPER_CLASSES.put(Boolean.TYPE, Boolean.class);
        WRAPPER_CLASSES.put(Byte.TYPE, Byte.class);
        WRAPPER_CLASSES.put(Character.TYPE, Character.class);
        WRAPPER_CLASSES.put(Double.TYPE, Double.class);
        WRAPPER_CLASSES.put(Float.TYPE, Float.class);
        WRAPPER_CLASSES.put(Integer.TYPE, Integer.class);
        WRAPPER_CLASSES.put(Long.TYPE, Long.class);
        WRAPPER_CLASSES.put(Short.TYPE, Short.class);
        NUMERIC_CLASSES.add(Byte.TYPE);
        NUMERIC_CLASSES.add(Double.TYPE);
        NUMERIC_CLASSES.add(Float.TYPE);
        NUMERIC_CLASSES.add(Integer.TYPE);
        NUMERIC_CLASSES.add(Long.TYPE);
        NUMERIC_CLASSES.add(Short.TYPE);
        NUMERIC_CLASSES.add(Byte.class);
        NUMERIC_CLASSES.add(Double.class);
        NUMERIC_CLASSES.add(Float.class);
        NUMERIC_CLASSES.add(Integer.class);
        NUMERIC_CLASSES.add(Long.class);
        NUMERIC_CLASSES.add(Short.class);
        PRIMITIVE_CLASS_NAMES.put("boolean", Boolean.TYPE);
        PRIMITIVE_CLASS_NAMES.put("byte", Byte.TYPE);
        PRIMITIVE_CLASS_NAMES.put("char", Character.TYPE);
        PRIMITIVE_CLASS_NAMES.put("double", Double.TYPE);
        PRIMITIVE_CLASS_NAMES.put("float", Float.TYPE);
        PRIMITIVE_CLASS_NAMES.put("int", Integer.TYPE);
        PRIMITIVE_CLASS_NAMES.put("long", Long.TYPE);
        PRIMITIVE_CLASS_NAMES.put("short", Short.TYPE);
    }

    @FunctionalInterface
    public static interface ExceptionalFunction<T, R> {
        public R apply(T var1) throws Exception;
    }
}

