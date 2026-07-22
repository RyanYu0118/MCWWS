/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  edu.umd.cs.findbugs.annotations.SuppressFBWarnings
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Chunk
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package ch.njol.skript.registrations;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.PatternedParser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.DefaultExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.localization.Language;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.StringMode;
import ch.njol.skript.util.Utils;
import ch.njol.skript.variables.SerializedVariable;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import ch.njol.yggdrasil.Tag;
import ch.njol.yggdrasil.YggdrasilInputStream;
import ch.njol.yggdrasil.YggdrasilOutputStream;
import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.SequenceInputStream;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.ConverterInfo;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.lang.properties.Property;

public abstract class Classes {
    @Nullable
    private static ClassInfo<?>[] classInfos = null;
    private static final List<ClassInfo<?>> tempClassInfos = new ArrayList();
    private static final HashMap<Class<?>, ClassInfo<?>> exactClassInfos = new HashMap();
    private static final HashMap<Class<?>, ClassInfo<?>> superClassInfos = new HashMap();
    private static final HashMap<String, ClassInfo<?>> classInfosByCodeName = new HashMap();
    private static final Map<String, List<ClassInfo<?>>> registeredLiteralPatterns = new HashMap();
    private static final Map<Property<?>, Set<ClassInfo<?>>> CLASS_INFOS_BY_PROPERTY = new HashMap();
    private static final byte[] YGGDRASIL_START = new byte[]{89, 103, 103, 0, 0, 1};
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private Classes() {
    }

    public static <T> void registerClass(ClassInfo<T> info) {
        try {
            Skript.checkAcceptRegistrations();
            if (classInfosByCodeName.containsKey(info.getCodeName())) {
                throw new IllegalArgumentException("Can't register " + info.getC().getName() + " with the code name " + info.getCodeName() + " because that name is already used by " + String.valueOf(classInfosByCodeName.get(info.getCodeName())));
            }
            if (exactClassInfos.containsKey(info.getC())) {
                throw new IllegalArgumentException("Can't register the class info " + info.getCodeName() + " because the class " + info.getC().getName() + " is already registered");
            }
            if (info.getCodeName().length() > 50) {
                throw new IllegalArgumentException("The codename '" + info.getCodeName() + "' is too long to be saved in a database, the maximum length allowed is 50");
            }
            exactClassInfos.put(info.getC(), info);
            classInfosByCodeName.put(info.getCodeName(), info);
            tempClassInfos.add(info);
            Parser<T> parser = info.getParser();
            if (parser instanceof PatternedParser) {
                String[] patterns;
                PatternedParser patternedParser = (PatternedParser)parser;
                for (String pattern : patterns = patternedParser.getPatterns()) {
                    registeredLiteralPatterns.computeIfAbsent(pattern, list -> new ArrayList()).add(info);
                }
            }
        }
        catch (RuntimeException e) {
            if (SkriptConfig.apiSoftExceptions.value().booleanValue()) {
                Skript.warning("Ignored an exception due to user configuration: " + e.getMessage());
            }
            throw e;
        }
    }

    public static void onRegistrationsStop() {
        Classes.sortClassInfos();
        for (ClassInfo<?> ci : Classes.getClassInfos()) {
            if (ci.getSerializeAs() == null) continue;
            ClassInfo<?> sa = Classes.getExactClassInfo(ci.getSerializeAs());
            if (sa == null) {
                Skript.error(ci.getCodeName() + "'s 'serializeAs' class is not registered");
                continue;
            }
            if (sa.getSerializer() != null) continue;
            Skript.error(ci.getCodeName() + "'s 'serializeAs' class is not serializable");
        }
        for (ClassInfo<?> ci : Classes.getClassInfos()) {
            Serializer<?> s = ci.getSerializer();
            if (s == null) continue;
            Variables.yggdrasil.registerClassResolver(s);
        }
        EntityData.onRegistrationStop();
    }

    @SuppressFBWarnings(value={"LI_LAZY_INIT_STATIC"})
    private static void sortClassInfos() {
        assert (classInfos == null);
        if (!Skript.testing() && SkriptConfig.addonSafetyChecks.value().booleanValue()) {
            Classes.removeNullElements();
        }
        block0: for (ClassInfo<?> ci : tempClassInfos) {
            Set<String> before = ci.before();
            if (before == null || before.isEmpty()) continue;
            for (ClassInfo<?> classInfo : tempClassInfos) {
                if (!before.contains(classInfo.getCodeName())) continue;
                classInfo.after().add(ci.getCodeName());
                before.remove(classInfo.getCodeName());
                if (!before.isEmpty()) continue;
                continue block0;
            }
        }
        for (ClassInfo<?> ci : tempClassInfos) {
            for (ClassInfo classInfo : tempClassInfos) {
                if (ci == classInfo || !ci.getC().isAssignableFrom(classInfo.getC())) continue;
                ci.after().add(classInfo.getCodeName());
            }
        }
        for (ClassInfo<?> ci : tempClassInfos) {
            HashSet<String> s = new HashSet<String>();
            Set<String> set = ci.before();
            if (set != null) {
                for (String string : set) {
                    if (Classes.getClassInfoNoError(string) != null) continue;
                    s.add(string);
                }
                set.removeAll(s);
            }
            for (String string : ci.after()) {
                if (Classes.getClassInfoNoError(string) != null) continue;
                s.add(string);
            }
            ci.after().removeAll(s);
            if (s.isEmpty() || !Skript.testing()) continue;
            Skript.warning(s.size() + " dependency/ies could not be resolved for " + String.valueOf(ci) + ": " + StringUtils.join(s, ", "));
        }
        ArrayList classInfos = new ArrayList(tempClassInfos.size());
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < tempClassInfos.size(); ++i) {
                ClassInfo<?> classInfo = tempClassInfos.get(i);
                if (!classInfo.after().isEmpty()) continue;
                classInfos.add(classInfo);
                tempClassInfos.remove(i);
                --i;
                for (ClassInfo<?> classInfo2 : tempClassInfos) {
                    classInfo2.after().remove(classInfo.getCodeName());
                }
                changed = true;
            }
        }
        Classes.classInfos = classInfos.toArray(new ClassInfo[classInfos.size()]);
        if (!tempClassInfos.isEmpty()) {
            StringBuilder b = new StringBuilder();
            for (ClassInfo<?> classInfo : tempClassInfos) {
                if (b.length() != 0) {
                    b.append(", ");
                }
                b.append(classInfo.getCodeName() + " (after: " + StringUtils.join(classInfo.after(), ", ") + ")");
            }
            throw new IllegalStateException("ClassInfos with circular dependencies detected: " + b.toString());
        }
        if (Skript.debug()) {
            StringBuilder b = new StringBuilder();
            for (ClassInfo classInfo : classInfos) {
                if (b.length() != 0) {
                    b.append(", ");
                }
                b.append(classInfo.getCodeName());
            }
            Skript.info("All registered classes in order: " + b.toString());
        }
    }

    private static void removeNullElements() {
        Iterator<ClassInfo<?>> it = tempClassInfos.iterator();
        while (it.hasNext()) {
            ClassInfo<?> ci = it.next();
            if (ci.getC() != null) continue;
            it.remove();
        }
    }

    private static void checkAllowClassInfoInteraction() {
        if (Skript.isAcceptRegistrations()) {
            throw new IllegalStateException("Cannot use classinfos until registration is over");
        }
    }

    public static @Unmodifiable @Nullable List<ClassInfo<?>> getPatternInfos(String pattern) {
        List<ClassInfo<?>> infos = registeredLiteralPatterns.get(pattern = pattern.toLowerCase(Locale.ENGLISH));
        if (infos != null) {
            return Collections.unmodifiableList(infos);
        }
        return null;
    }

    public static List<ClassInfo<?>> getClassInfos() {
        Classes.checkAllowClassInfoInteraction();
        ClassInfo<?>[] ci = classInfos;
        if (ci == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(ci));
    }

    public static ClassInfo<?> getClassInfo(String codeName) {
        ClassInfo<?> ci = classInfosByCodeName.get(codeName);
        if (ci == null) {
            throw new SkriptAPIException("No class info found for " + codeName);
        }
        return ci;
    }

    @Nullable
    public static ClassInfo<?> getClassInfoNoError(@Nullable String codeName) {
        return classInfosByCodeName.get(codeName);
    }

    @Nullable
    public static <T> ClassInfo<T> getExactClassInfo(@Nullable Class<T> c) {
        return exactClassInfos.get(c);
    }

    @Contract(pure=true, value="!null -> !null")
    public static <T> ClassInfo<? super T> getSuperClassInfo(Class<T> c) {
        assert (c != null);
        ClassInfo<Object> info = Classes.getExactClassInfo(c);
        if (info != null) {
            return info;
        }
        info = superClassInfos.get(c);
        if (info != null) {
            return info;
        }
        for (ClassInfo<?> ci : Classes.getClassInfos()) {
            if (!ci.getC().isAssignableFrom(c)) continue;
            if (!Skript.isAcceptRegistrations()) {
                superClassInfos.put(c, ci);
            }
            return ci;
        }
        assert (false);
        return null;
    }

    public static ClassInfo<?> getSuperClassInfo(Class<?> ... classes) {
        return Classes.getSuperClassInfo(Utils.getSuperType(classes));
    }

    public static <T> List<ClassInfo<? super T>> getAllSuperClassInfos(Class<T> c) {
        assert (c != null);
        Classes.checkAllowClassInfoInteraction();
        ArrayList<ClassInfo<T>> list = new ArrayList<ClassInfo<T>>();
        for (ClassInfo<?> ci : Classes.getClassInfos()) {
            if (!ci.getC().isAssignableFrom(c)) continue;
            list.add(ci);
        }
        return list;
    }

    @ApiStatus.Internal
    public static void hasProperty(@NotNull Property<?> property, @NotNull ClassInfo<?> classInfo) {
        Preconditions.checkNotNull(property, (Object)"property cannot be null");
        Preconditions.checkNotNull(classInfo, (Object)"classInfo cannot be null");
        CLASS_INFOS_BY_PROPERTY.computeIfAbsent(property, key -> new HashSet()).add(classInfo);
    }

    @NotNull
    public static Set<ClassInfo<?>> getClassInfosByProperty(@NotNull Property<?> property) {
        Preconditions.checkNotNull(property, (Object)"property cannot be null");
        return CLASS_INFOS_BY_PROPERTY.getOrDefault(property, Collections.emptySet());
    }

    public static Class<?> getClass(String codeName) {
        Classes.checkAllowClassInfoInteraction();
        return Classes.getClassInfo(codeName).getC();
    }

    @Nullable
    public static ClassInfo<?> getClassInfoFromUserInput(String name) {
        Classes.checkAllowClassInfoInteraction();
        name = ((String)name).toLowerCase(Locale.ENGLISH);
        for (ClassInfo<?> ci : Classes.getClassInfos()) {
            Pattern[] uip = ci.getUserInputPatterns();
            if (uip == null) continue;
            for (Pattern pattern : uip) {
                if (!pattern.matcher((CharSequence)name).matches()) continue;
                return ci;
            }
        }
        return null;
    }

    @Nullable
    public static Class<?> getClassFromUserInput(String name) {
        Classes.checkAllowClassInfoInteraction();
        ClassInfo<?> ci = Classes.getClassInfoFromUserInput(name);
        return ci == null ? null : ci.getC();
    }

    @Nullable
    public static DefaultExpression<?> getDefaultExpression(String codeName) {
        Classes.checkAllowClassInfoInteraction();
        return Classes.getClassInfo(codeName).getDefaultExpression();
    }

    @Nullable
    public static <T> DefaultExpression<T> getDefaultExpression(Class<T> c) {
        Classes.checkAllowClassInfoInteraction();
        ClassInfo<T> ci = Classes.getExactClassInfo(c);
        return ci == null ? null : ci.getDefaultExpression();
    }

    public static Object clone(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj.getClass().isArray()) {
            int length = Array.getLength(obj);
            Object clone = Array.newInstance(obj.getClass().getComponentType(), length);
            for (int i = 0; i < length; ++i) {
                Array.set(clone, i, Classes.clone(Array.get(obj, i)));
            }
            return clone;
        }
        ClassInfo<?> classInfo = Classes.getSuperClassInfo(obj.getClass());
        return classInfo.clone(obj);
    }

    @Nullable
    public static String getExactClassName(Class<?> c) {
        Classes.checkAllowClassInfoInteraction();
        ClassInfo<?> ci = exactClassInfos.get(c);
        return ci == null ? null : ci.getCodeName();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Nullable
    public static <T> T parseSimple(String s, Class<T> c, ParseContext context) {
        ParseLogHandler log = SkriptLogger.startParseLogHandler();
        try {
            for (ClassInfo<?> info : Classes.getClassInfos()) {
                Parser<?> parser = info.getParser();
                if (parser == null || !parser.canParse(context) || !c.isAssignableFrom(info.getC())) continue;
                log.clear();
                Object t = parser.parse(s, context);
                if (t == null) continue;
                log.printLog();
                Object obj = t;
                return (T)obj;
            }
            log.printError();
        }
        finally {
            log.stop();
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Nullable
    public static <T> T parse(String s, Class<T> c, ParseContext context) {
        ParseLogHandler log = SkriptLogger.startParseLogHandler();
        try {
            Object t = Classes.parseSimple(s, c, context);
            if (t != null) {
                log.printLog();
                T t2 = t;
                return t2;
            }
            for (ConverterInfo<?, ?> conv : Converters.getConverterInfos()) {
                if ((context == ParseContext.COMMAND || context == ParseContext.PARSE) && (conv.getFlags() & 8) != 0) continue;
                if (!c.isAssignableFrom(conv.getTo())) continue;
                log.clear();
                Object object = Classes.parseSimple(s, conv.getFrom(), context);
                if (object == null || (t = conv.getConverter().convert(object)) == null) continue;
                log.printLog();
                Object object2 = t;
                return object2;
            }
            log.printError();
        }
        finally {
            log.stop();
        }
        return null;
    }

    @Nullable
    public static <T> Parser<? extends T> getParser(Class<T> to) {
        Classes.checkAllowClassInfoInteraction();
        ClassInfo<?>[] classInfos = Classes.classInfos;
        if (classInfos == null) {
            return null;
        }
        for (int i = classInfos.length - 1; i >= 0; --i) {
            ClassInfo<?> ci = classInfos[i];
            if (!to.isAssignableFrom(ci.getC()) || ci.getParser() == null) continue;
            return ci.getParser();
        }
        for (ConverterInfo<?, ?> conv : Converters.getConverterInfos()) {
            if (!to.isAssignableFrom(conv.getTo())) continue;
            for (int i = classInfos.length - 1; i >= 0; --i) {
                ClassInfo<?> ci = classInfos[i];
                Parser<?> parser = ci.getParser();
                if (!conv.getFrom().isAssignableFrom(ci.getC()) || parser == null) continue;
                return Classes.createConvertedParser(parser, conv.getConverter());
            }
        }
        return null;
    }

    @Nullable
    public static <T> Parser<? extends T> getExactParser(Class<T> c) {
        if (Skript.isAcceptRegistrations()) {
            for (ClassInfo<?> ci : tempClassInfos) {
                if (ci.getC() != c) continue;
                return ci.getParser();
            }
            return null;
        }
        ClassInfo<T> ci = Classes.getExactClassInfo(c);
        return ci == null ? null : ci.getParser();
    }

    private static <F, T> Parser<T> createConvertedParser(final Parser<?> parser, final Converter<F, T> converter) {
        return new Parser<T>(){

            @Override
            @Nullable
            public T parse(String s, ParseContext context) {
                Object f = parser.parse(s, context);
                if (f == null) {
                    return null;
                }
                return converter.convert(f);
            }

            @Override
            public String toString(T o, int flags) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String toVariableNameString(T o) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static String toString(@Nullable Object o) {
        return Classes.toString(o, StringMode.MESSAGE, 0);
    }

    public static String getDebugMessage(@Nullable Object o) {
        return Classes.toString(o, StringMode.DEBUG, 0);
    }

    public static <T> String toString(@Nullable T o, StringMode mode) {
        return Classes.toString(o, mode, 0);
    }

    private static <T> String toString(@Nullable T o, StringMode mode, int flags) {
        assert (flags == 0 || mode == StringMode.MESSAGE);
        if (o == null) {
            return Language.get("none");
        }
        if (o.getClass().isArray()) {
            if (((Object[])o).length == 0) {
                return Language.get("none");
            }
            StringBuilder b = new StringBuilder();
            boolean first = true;
            for (Object i : (Object[])o) {
                if (!first) {
                    b.append(", ");
                }
                b.append(Classes.toString(i, mode, flags));
                first = false;
            }
            return "[" + b.toString() + "]";
        }
        for (ClassInfo<?> ci : Classes.getClassInfos()) {
            Parser<?> parser = ci.getParser();
            if (parser == null || !ci.getC().isInstance(o)) continue;
            String s = mode == StringMode.MESSAGE ? parser.toString(o, flags) : (mode == StringMode.DEBUG ? "[" + ci.getCodeName() + ":" + parser.toString(o, mode) + "]" : parser.toString(o, mode));
            return s;
        }
        return mode == StringMode.VARIABLE_NAME ? "object:" + String.valueOf(o) : String.valueOf(o);
    }

    public static String toString(Object[] os, int flags, boolean and) {
        return Classes.toString(os, and, null, StringMode.MESSAGE, flags);
    }

    public static String toString(Object[] os, int flags, @Nullable ChatColor c) {
        return Classes.toString(os, true, c, StringMode.MESSAGE, flags);
    }

    public static String toString(Object[] os, boolean and) {
        return Classes.toString(os, and, null, StringMode.MESSAGE, 0);
    }

    public static String toString(Object[] os, boolean and, StringMode mode) {
        return Classes.toString(os, and, null, mode, 0);
    }

    private static String toString(Object[] os, boolean and, @Nullable ChatColor c, StringMode mode, int flags) {
        if (os.length == 0) {
            return Classes.toString(null);
        }
        if (os.length == 1) {
            return Classes.toString(os[0], mode, flags);
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < os.length; ++i) {
            if (i != 0) {
                if (c != null) {
                    b.append(c.toString());
                }
                if (i == os.length - 1) {
                    b.append(and ? " and " : " or ");
                } else {
                    b.append(", ");
                }
            }
            b.append(Classes.toString(os[i], mode, flags));
        }
        return b.toString();
    }

    private static byte[] getYggdrasilStart(ClassInfo<?> c) throws NotSerializableException {
        int i;
        assert (Enum.class.isAssignableFrom(Kleenean.class) && Tag.getType(Kleenean.class) == Tag.T_ENUM) : Tag.getType(Kleenean.class);
        Tag t = Tag.getType(c.getC());
        assert (t.isWrapper() || t == Tag.T_STRING || t == Tag.T_OBJECT || t == Tag.T_ENUM);
        byte[] cn = t == Tag.T_OBJECT || t == Tag.T_ENUM ? Variables.yggdrasil.getID(c.getC()).getBytes(UTF_8) : null;
        byte[] r = new byte[YGGDRASIL_START.length + 1 + (cn == null ? 0 : 1 + cn.length)];
        for (i = 0; i < YGGDRASIL_START.length; ++i) {
            r[i] = YGGDRASIL_START[i];
        }
        r[i++] = t.tag;
        if (cn != null) {
            r[i++] = (byte)cn.length;
            for (int j = 0; j < cn.length; ++j) {
                r[i++] = cn[j];
            }
        }
        assert (i == r.length);
        return r;
    }

    public static  @Nullable SerializedVariable.Value serialize(@Nullable Object object) {
        Serializer<?> serializer;
        if (object == null) {
            return null;
        }
        assert (Bukkit.isPrimaryThread());
        ClassInfo<?> classInfo = Classes.getSuperClassInfo(object.getClass());
        if (classInfo.getSerializeAs() != null) {
            if ((classInfo = Classes.getExactClassInfo(classInfo.getSerializeAs())) == null) {
                assert (false) : object.getClass();
                return null;
            }
            if ((object = Converters.convert(object, classInfo.getC())) == null) {
                assert (false) : classInfo.getCodeName();
                return null;
            }
        }
        if ((serializer = classInfo.getSerializer()) == null) {
            return null;
        }
        assert (!serializer.mustSyncDeserialization() || Bukkit.isPrimaryThread());
        try {
            Object deserialized;
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            YggdrasilOutputStream yggdrasilOutputStream = Variables.yggdrasil.newOutputStream(byteOutputStream);
            yggdrasilOutputStream.writeObject(object);
            yggdrasilOutputStream.flush();
            yggdrasilOutputStream.close();
            byte[] byteArray = byteOutputStream.toByteArray();
            byte[] start = Classes.getYggdrasilStart(classInfo);
            for (int i = 0; i < start.length; ++i) {
                assert (byteArray[i] == start[i]) : String.valueOf(object) + " (" + classInfo.getC().getName() + "); " + Arrays.toString(start) + ", " + Arrays.toString(byteArray);
            }
            byte[] byteArrayCopy = new byte[byteArray.length - start.length];
            System.arraycopy(byteArray, start.length, byteArrayCopy, 0, byteArrayCopy.length);
            assert (Classes.equals(object, deserialized = Classes.deserialize(classInfo, new ByteArrayInputStream(byteArrayCopy)))) : String.valueOf(object) + " (" + String.valueOf(object.getClass()) + ") != " + String.valueOf(deserialized) + " (" + String.valueOf(deserialized == null ? null : deserialized.getClass()) + "): " + Arrays.toString(byteArray);
            return new SerializedVariable.Value(classInfo.getCodeName(), byteArrayCopy);
        }
        catch (IOException ex) {
            Skript.exception((Throwable)ex, new String[0]);
            return null;
        }
    }

    private static boolean equals(@Nullable Object o, @Nullable Object d) {
        if (o instanceof Chunk) {
            if (!(d instanceof Chunk)) {
                return false;
            }
            Chunk c1 = (Chunk)o;
            Chunk c2 = (Chunk)d;
            return c1.getWorld().equals((Object)c2.getWorld()) && c1.getX() == c2.getX() && c1.getZ() == c2.getZ();
        }
        return o == null ? d == null : o.equals(d);
    }

    @Nullable
    public static Object deserialize(ClassInfo<?> type, byte[] value) {
        return Classes.deserialize(type, new ByteArrayInputStream(value));
    }

    @Nullable
    public static Object deserialize(String type, byte[] value) {
        ClassInfo<?> ci = Classes.getClassInfoNoError(type);
        if (ci == null) {
            return null;
        }
        return Classes.deserialize(ci, new ByteArrayInputStream(value));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Nullable
    public static Object deserialize(ClassInfo<?> type, InputStream value) {
        Serializer<?> s;
        assert ((s = type.getSerializer()) != null && (!s.mustSyncDeserialization() || Bukkit.isPrimaryThread())) : String.valueOf(type) + "; " + String.valueOf(s) + "; " + Bukkit.isPrimaryThread();
        Closeable in = null;
        try {
            value = new SequenceInputStream(new ByteArrayInputStream(Classes.getYggdrasilStart(type)), value);
            in = Variables.yggdrasil.newInputStream(value);
            Object object = ((YggdrasilInputStream)in).readObject();
            return object;
        }
        catch (IOException e) {
            if (Skript.testing()) {
                e.printStackTrace();
            }
            Object var5_8 = null;
            return var5_8;
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (IOException iOException) {}
            }
            try {
                value.close();
            }
            catch (IOException iOException) {}
        }
    }

    @Deprecated(since="2.3.0", forRemoval=true)
    @Nullable
    public static Object deserialize(String type, String value) {
        assert (Bukkit.isPrimaryThread());
        ClassInfo<?> ci = Classes.getClassInfoNoError(type);
        if (ci == null) {
            return null;
        }
        Serializer<?> s = ci.getSerializer();
        if (s == null) {
            return null;
        }
        return s.deserialize(value);
    }
}

