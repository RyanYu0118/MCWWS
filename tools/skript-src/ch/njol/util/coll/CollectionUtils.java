/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.util.coll;

import ch.njol.util.Pair;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

public abstract class CollectionUtils {
    private static final Random random = new Random();

    private CollectionUtils() {
    }

    public static <T> int indexOf(@Nullable T[] array, @Nullable T t) {
        if (array == null) {
            return -1;
        }
        for (int i = 0; i < array.length; ++i) {
            if (!(array[i] == null ? t == null : array[i].equals(t))) continue;
            return i;
        }
        return -1;
    }

    public static <T> int lastIndexOf(@Nullable T[] array, @Nullable T t) {
        if (array == null) {
            return -1;
        }
        for (int i = array.length - 1; i >= 0; --i) {
            if (!(array[i] == null ? t == null : array[i].equals(t))) continue;
            return i;
        }
        return -1;
    }

    public static <T> int indexOf(@Nullable T[] array, @Nullable T t, int start, int end) {
        if (array == null) {
            return -1;
        }
        for (int i = start; i < end; ++i) {
            if (!Objects.equals(array[i], t)) continue;
            return i;
        }
        return -1;
    }

    public static <T> boolean contains(@Nullable T[] array, @Nullable T o) {
        return CollectionUtils.indexOf(array, o) != -1;
    }

    public static <T> boolean containsAny(@Nullable T[] array, T ... os) {
        if (array == null || os == null) {
            return false;
        }
        for (T o : os) {
            if (CollectionUtils.indexOf(array, o) == -1) continue;
            return true;
        }
        return false;
    }

    public static <T> boolean containsAll(@Nullable T[] array, T ... os) {
        if (array == null || os == null) {
            return false;
        }
        for (T o : os) {
            if (CollectionUtils.indexOf(array, o) != -1) continue;
            return false;
        }
        return true;
    }

    public static <T> boolean check(T @Nullable [] array, Predicate<T> predicate, boolean and) {
        if (array == null) {
            return false;
        }
        for (T value : array) {
            boolean result = predicate.test(value);
            if (and && !result) {
                return false;
            }
            if (and || !result) continue;
            return true;
        }
        return and;
    }

    public static int indexOf(@Nullable int[] array, int num) {
        if (array == null) {
            return -1;
        }
        return CollectionUtils.indexOf(array, num, 0, array.length);
    }

    public static int indexOf(@Nullable int[] array, int num, int start) {
        if (array == null) {
            return -1;
        }
        return CollectionUtils.indexOf(array, num, start, array.length);
    }

    public static int indexOf(@Nullable int[] array, int num, int start, int end) {
        if (array == null) {
            return -1;
        }
        for (int i = start; i < end; ++i) {
            if (array[i] != num) continue;
            return i;
        }
        return -1;
    }

    public static boolean contains(@Nullable int[] array, int num) {
        return CollectionUtils.indexOf(array, num) != -1;
    }

    public static int indexOfIgnoreCase(@Nullable String[] array, @Nullable String s) {
        if (array == null) {
            return -1;
        }
        int i = 0;
        for (String a : array) {
            if (a == null ? s == null : a.equalsIgnoreCase(s)) {
                return i;
            }
            ++i;
        }
        return -1;
    }

    public static boolean containsIgnoreCase(@Nullable String[] array, @Nullable String s) {
        return CollectionUtils.indexOfIgnoreCase(array, s) != -1;
    }

    public static <T> int indexOf(@Nullable Iterable<T> iter, @Nullable T o) {
        if (iter == null) {
            return -1;
        }
        int i = 0;
        for (T a : iter) {
            if (a == null ? o == null : a.equals(o)) {
                return i;
            }
            ++i;
        }
        return -1;
    }

    public static int indexOfIgnoreCase(@Nullable Iterable<String> iter, @Nullable String s) {
        if (iter == null) {
            return -1;
        }
        int i = 0;
        for (String a : iter) {
            if (a == null ? s == null : a.equalsIgnoreCase(s)) {
                return i;
            }
            ++i;
        }
        return -1;
    }

    @Nullable
    public static <T, U> Map.Entry<T, U> containsKey(@Nullable Map<T, U> map, @Nullable T key) {
        if (map == null) {
            return null;
        }
        if (map.containsKey(key)) {
            return new Pair<T, U>(key, map.get(key));
        }
        return null;
    }

    @Nullable
    public static <U> Map.Entry<String, U> containsKeyIgnoreCase(@Nullable Map<String, U> map, @Nullable String key) {
        if (key == null) {
            return CollectionUtils.containsKey(map, null);
        }
        if (map == null) {
            return null;
        }
        for (Map.Entry<String, U> e : map.entrySet()) {
            if (!key.equalsIgnoreCase(e.getKey())) continue;
            return e;
        }
        return null;
    }

    public static boolean containsSuperclass(@Nullable Class<?>[] classes, @Nullable Class<?> c) {
        if (classes == null || c == null) {
            return false;
        }
        for (Class<?> cl : classes) {
            if (cl == null || !cl.isAssignableFrom(c)) continue;
            return true;
        }
        return false;
    }

    public static boolean containsAnySuperclass(@Nullable Class<?>[] classes, Class<?> ... cs) {
        if (classes == null || cs == null) {
            return false;
        }
        for (Class<?> cl : classes) {
            if (cl == null) continue;
            for (Class<?> c : cs) {
                if (!cl.isAssignableFrom(c)) continue;
                return true;
            }
        }
        return false;
    }

    public static boolean isAnyInstanceOf(Object object, Class<?> ... classes) {
        for (Class<?> clazz : classes) {
            if (!clazz.isInstance(object)) continue;
            return true;
        }
        return false;
    }

    @Nullable
    public static <T> T getRandom(@Nullable T[] os) {
        if (os == null || os.length == 0) {
            return null;
        }
        return os[random.nextInt(os.length)];
    }

    @Nullable
    public static <T> T getRandom(@Nullable T[] os, int start) {
        if (os == null || os.length == 0) {
            return null;
        }
        return os[random.nextInt(os.length - start) + start];
    }

    @Nullable
    public static <T> T getRandom(@Nullable List<T> os) {
        if (os == null || os.isEmpty()) {
            return null;
        }
        return os.get(random.nextInt(os.size()));
    }

    public static boolean isSubset(@Nullable Object[] set, @Nullable Object[] sub) {
        if (set == null || sub == null) {
            return false;
        }
        for (Object s : set) {
            if (CollectionUtils.contains(sub, s)) continue;
            return false;
        }
        return true;
    }

    public static <E> Set<E> intersection(Set<E> ... sets) {
        if (sets == null || sets.length == 0) {
            return Collections.emptySet();
        }
        if (sets.length == 1 && sets[0] != null) {
            return sets[0];
        }
        HashSet<E> l = new HashSet<E>(sets[0]);
        for (int i = 1; i < sets.length; ++i) {
            if (sets[i] == null) continue;
            l.retainAll(sets[i]);
        }
        return l;
    }

    public static <E> Set<E> union(Set<E> ... sets) {
        if (sets == null || sets.length == 0) {
            return Collections.emptySet();
        }
        if (sets.length == 1 && sets[0] != null) {
            return sets[0];
        }
        HashSet<E> l = new HashSet<E>(sets[0]);
        for (int i = 1; i < sets.length; ++i) {
            if (sets[i] == null) continue;
            l.addAll(sets[i]);
        }
        return l;
    }

    @SafeVarargs
    public static <T> T[] array(T ... array) {
        return array;
    }

    public static <T> Class<T[]> arrayType(Class<T> c) {
        return Array.newInstance(c, 0).getClass();
    }

    public static <T> T[] subarray(T[] array, int startIndex, int endIndex) {
        Class<?> componentType = array.getClass().getComponentType();
        if ((startIndex = Math.max(startIndex, 0)) >= (endIndex = Math.min(Math.max(endIndex, 0), array.length))) {
            return (Object[])Array.newInstance(componentType, 0);
        }
        int size = endIndex - startIndex;
        Object[] subarray = (Object[])Array.newInstance(componentType, size);
        System.arraycopy(array, startIndex, subarray, 0, size);
        return subarray;
    }

    public static int[] permutation(int start, int end) {
        int i;
        if (start > end) {
            return new int[0];
        }
        int length = end - start + 1;
        int[] r = new int[length];
        for (i = 0; i < length; ++i) {
            r[i] = start + i;
        }
        for (i = length - 1; i > 0; --i) {
            int j = random.nextInt(i + 1);
            int b = r[i];
            r[i] = r[j];
            r[j] = b;
        }
        return r;
    }

    public static byte[] permutation(byte start, byte end) {
        int i;
        if (start > end) {
            return new byte[0];
        }
        int length = end - start + 1;
        byte[] r = new byte[length];
        for (i = 0; i < length; i = (int)((byte)(i + 1))) {
            r[i] = (byte)(start + i);
        }
        for (i = length - 1; i > 0; --i) {
            int j = random.nextInt(i + 1);
            byte b = r[i];
            r[i] = r[j];
            r[j] = b;
        }
        return r;
    }

    public static int[] permutation(int length) {
        return CollectionUtils.permutation(0, length - 1);
    }

    public static int[] toArray(@Nullable Collection<Integer> ints) {
        if (ints == null) {
            return new int[0];
        }
        int[] r = new int[ints.size()];
        int i = 0;
        for (Integer n : ints) {
            r[i++] = n;
        }
        if (i != r.length) {
            assert (false) : ints;
            return Arrays.copyOfRange(r, 0, i);
        }
        return r;
    }

    public static float[] toFloats(@Nullable double[] doubles) {
        if (doubles == null) {
            return new float[0];
        }
        float[] floats = new float[doubles.length];
        for (int i = 0; i < floats.length; ++i) {
            floats[i] = (float)doubles[i];
        }
        return floats;
    }

    public static Double[] wrap(double[] primitive) {
        Double[] wrapped = new Double[primitive.length];
        for (int i = 0; i < primitive.length; ++i) {
            wrapped[i] = primitive[i];
        }
        return wrapped;
    }
}

