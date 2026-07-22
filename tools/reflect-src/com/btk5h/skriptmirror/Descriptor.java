/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.skriptlang.skript.lang.script.Script
 */
package com.btk5h.skriptmirror;

import com.btk5h.skriptmirror.ImportNotFoundException;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.util.JavaUtil;
import com.btk5h.skriptmirror.util.SkriptMirrorUtil;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.skriptlang.reflect.java.elements.structures.StructImport;
import org.skriptlang.skript.lang.script.Script;

public final class Descriptor {
    private static final String PACKAGE_ARRAY = "(?:[_a-zA-Z$][\\w$]*\\.)*(?:[_a-zA-Z$][\\w$]*)(?:\\[])*";
    private static final Pattern PACKAGE_ARRAY_SINGLE = Pattern.compile("\\[]");
    private static final Pattern DESCRIPTOR = Pattern.compile("(?:\\[((?:[_a-zA-Z$][\\w$]*\\.)*(?:[_a-zA-Z$][\\w$]*))])?([_a-zA-Z$][\\w$]*)(?:\\[((?:(?:[_a-zA-Z$][\\w$]*\\.)*(?:[_a-zA-Z$][\\w$]*)(?:\\[])*\\s*,\\s*)*(?:(?:[_a-zA-Z$][\\w$]*\\.)*(?:[_a-zA-Z$][\\w$]*)(?:\\[])*))])?");
    private final Class<?> javaClass;
    private final String name;
    private final Class<?>[] parameterTypes;

    public Descriptor(Class<?> javaClass, String name, Class<?>[] parameterTypes) {
        this.javaClass = javaClass;
        this.name = name;
        this.parameterTypes = parameterTypes;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Descriptor that = (Descriptor)o;
        return Objects.equals(this.javaClass, that.javaClass) && Objects.equals(this.name, that.name);
    }

    public int hashCode() {
        return Objects.hash(this.javaClass, this.name);
    }

    public Class<?> getJavaClass() {
        return this.javaClass;
    }

    public String getName() {
        return this.name;
    }

    public Class<?>[] getParameterTypes() {
        return this.parameterTypes;
    }

    public String toString() {
        return this.toString(false);
    }

    public String toString(boolean isStatic) {
        return this.javaClass == null ? "(unspecified)" : SkriptMirrorUtil.getDebugName(this.javaClass) + (isStatic ? "." : "#") + this.name;
    }

    public Descriptor orDefaultClass(Class<?> cls) {
        if (this.javaClass != null) {
            return this;
        }
        return new Descriptor(cls, this.name, this.parameterTypes);
    }

    public static Descriptor parse(String desc, Script script) throws ImportNotFoundException {
        Matcher m = DESCRIPTOR.matcher(desc);
        if (m.matches()) {
            String cls = m.group(1);
            Class<?> javaClass = cls == null ? null : Descriptor.lookupClass(script, cls);
            String name = m.group(2);
            String args = m.group(3);
            Class<?>[] parameterTypes = args == null ? null : Descriptor.parseParams(args, script);
            return new Descriptor(javaClass, name, parameterTypes);
        }
        return null;
    }

    private static Class<?>[] parseParams(String args, Script script) throws ImportNotFoundException {
        String[] rawClasses = args.split(",");
        Class[] parsedClasses = new Class[rawClasses.length];
        for (int i = 0; i < rawClasses.length; ++i) {
            String userType = rawClasses[i].trim();
            Matcher arrayDepthMatcher = PACKAGE_ARRAY_SINGLE.matcher(userType);
            int arrayDepth = 0;
            while (arrayDepthMatcher.find()) {
                ++arrayDepth;
            }
            Class<?> cls = JavaUtil.PRIMITIVE_CLASS_NAMES.containsKey(userType = userType.substring(0, userType.length() - 2 * arrayDepth)) ? JavaUtil.PRIMITIVE_CLASS_NAMES.get(userType) : Descriptor.lookupClass(script, userType);
            parsedClasses[i] = cls = JavaUtil.getArrayClass(cls, arrayDepth);
        }
        return parsedClasses;
    }

    private static Class<?> lookupClass(Script script, String userType) throws ImportNotFoundException {
        JavaType customImport = StructImport.lookup(script, userType);
        if (customImport == null) {
            throw new ImportNotFoundException(userType);
        }
        return customImport.getJavaClass();
    }
}

