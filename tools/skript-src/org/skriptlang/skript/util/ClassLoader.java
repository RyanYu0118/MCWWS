/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  com.google.common.reflect.ClassPath
 *  com.google.common.reflect.ClassPath$ResourceInfo
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.util;

import ch.njol.skript.Skript;
import ch.njol.util.StringUtils;
import com.google.common.base.MoreObjects;
import com.google.common.reflect.ClassPath;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class ClassLoader {
    private final String basePackage;
    private final Collection<String> subPackages;
    private final Collection<String> excludedPackages;
    @Nullable
    private final Predicate<String> filter;
    private final boolean initialize;
    private final boolean deep;
    @Nullable
    private final Consumer<Class<?>> forEachClass;

    public static Builder builder() {
        return new Builder();
    }

    public static void loadClasses(Class<?> source, File jarFile, String basePackage, String ... subPackages) {
        ClassLoader.builder().basePackage(basePackage).addSubPackages(subPackages).initialize(true).deep(true).build().loadClasses(source, jarFile);
    }

    private ClassLoader(String basePackage, Collection<String> subPackages, Collection<String> excludedPackages, @Nullable Predicate<String> filter, boolean initialize, boolean deep, @Nullable Consumer<Class<?>> forEachClass) {
        if (!((String)basePackage).isEmpty()) {
            basePackage = ((String)basePackage).replace('.', '/') + "/";
        }
        this.basePackage = basePackage;
        this.subPackages = this.formatPackageNames(subPackages);
        this.excludedPackages = this.formatPackageNames(excludedPackages);
        this.filter = filter;
        this.initialize = initialize;
        this.deep = deep;
        this.forEachClass = forEachClass;
    }

    private Collection<String> formatPackageNames(Collection<String> packages) {
        return packages.stream().map(packageName -> packageName.replace('.', '/') + "/").collect(Collectors.toSet());
    }

    public void loadClasses(Class<?> source) {
        this.loadClasses(source, (JarFile)null);
    }

    public void loadClasses(Class<?> source, File jarFile) {
        try (JarFile jar = new JarFile(jarFile);){
            this.loadClasses(source, jar);
        }
        catch (IOException e) {
            Skript.warning("Failed to access jar file: " + String.valueOf(e));
            this.loadClasses(source);
        }
    }

    public void loadClasses(Class<?> source, @Nullable JarFile jar) {
        Collection classPaths;
        try {
            classPaths = jar != null ? (Collection)jar.stream().map(ZipEntry::getName).collect(Collectors.toSet()) : (Collection)ClassPath.from((java.lang.ClassLoader)source.getClassLoader()).getResources().stream().map(ClassPath.ResourceInfo::getResourceName).collect(Collectors.toSet());
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to load classes: " + String.valueOf(e));
        }
        int expectedDepth = !this.deep ? StringUtils.count(this.basePackage, '/') : 0;
        int offset = this.basePackage.length();
        TreeSet<String> classNames = new TreeSet<String>(String::compareToIgnoreCase);
        for (String name : classPaths) {
            boolean load;
            if (!name.startsWith(this.basePackage) || !name.endsWith(".class") || name.endsWith("package-info.class")) continue;
            if (this.subPackages.isEmpty()) {
                load = this.deep || StringUtils.count(name, '/') == expectedDepth;
            } else {
                load = false;
                for (String subPackage : this.subPackages) {
                    if (!name.startsWith(subPackage, offset) || !this.deep && StringUtils.count(name, '/') != expectedDepth + StringUtils.count(subPackage, '/')) continue;
                    load = true;
                    break;
                }
            }
            if (load && !this.excludedPackages.isEmpty()) {
                for (String excluded : this.excludedPackages) {
                    if (!name.startsWith(excluded, offset)) continue;
                    load = false;
                    break;
                }
            }
            if (!load) continue;
            name = name.replace('/', '.').substring(0, name.length() - 6);
            if (this.filter != null && !this.filter.test(name)) continue;
            classNames.add(name);
        }
        java.lang.ClassLoader loader = source.getClassLoader();
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className, this.initialize, loader);
                if (this.forEachClass == null) continue;
                this.forEachClass.accept(clazz);
            }
            catch (ClassNotFoundException ex) {
                throw new RuntimeException("Failed to load class: " + className, ex);
            }
            catch (ExceptionInInitializerError err) {
                throw new RuntimeException(className + " generated an exception while loading", err.getCause());
            }
        }
    }

    @Contract(value="-> new")
    public Builder toBuilder() {
        Builder builder = ClassLoader.builder().basePackage(this.basePackage).addSubPackages(this.subPackages).initialize(this.initialize).deep(this.deep);
        if (this.filter != null) {
            builder.filter(this.filter);
        }
        if (this.forEachClass != null) {
            builder.forEachClass(this.forEachClass);
        }
        return builder;
    }

    public String toString() {
        return MoreObjects.toStringHelper((Object)this).add("basePackage", (Object)this.basePackage).add("subPackages", this.subPackages).add("filter", this.filter).add("initialize", this.initialize).add("deep", this.deep).add("forEachClass", this.forEachClass).toString();
    }

    public static final class Builder {
        private String basePackage = "";
        private final Collection<String> subPackages = new HashSet<String>();
        private final Collection<String> excludedPackages = new HashSet<String>();
        @Nullable
        private Predicate<String> filter = null;
        private boolean initialize;
        private boolean deep;
        @Nullable
        private Consumer<Class<?>> forEachClass;

        private Builder() {
        }

        @Contract(value="_ -> this")
        public Builder basePackage(String basePackage) {
            this.basePackage = basePackage;
            return this;
        }

        @Contract(value="_ -> this")
        public Builder addSubPackage(String subPackage) {
            this.subPackages.add(subPackage);
            return this;
        }

        @Contract(value="_ -> this")
        public Builder addSubPackages(String ... subPackages) {
            Collections.addAll(this.subPackages, subPackages);
            return this;
        }

        @Contract(value="_ -> this")
        public Builder addSubPackages(Collection<String> subPackages) {
            this.subPackages.addAll(subPackages);
            return this;
        }

        @Contract(value="_ -> this")
        public Builder excludeSubPackage(String subPackage) {
            this.excludedPackages.add(subPackage);
            return this;
        }

        @Contract(value="_ -> this")
        public Builder excludeSubPackages(String ... subPackages) {
            Collections.addAll(this.excludedPackages, subPackages);
            return this;
        }

        @Contract(value="_ -> this")
        public Builder excludeSubPackages(Collection<String> subPackages) {
            this.excludedPackages.addAll(subPackages);
            return this;
        }

        @Contract(value="_ -> this")
        public Builder filter(Predicate<String> filter) {
            this.filter = filter;
            return this;
        }

        @Contract(value="_ -> this")
        public Builder initialize(boolean initialize) {
            this.initialize = initialize;
            return this;
        }

        @Contract(value="_ -> this")
        public Builder deep(boolean deep) {
            this.deep = deep;
            return this;
        }

        @Contract(value="_ -> this")
        public Builder forEachClass(Consumer<Class<?>> forEachClass) {
            this.forEachClass = forEachClass;
            return this;
        }

        @Contract(value="-> new")
        public ClassLoader build() {
            return new ClassLoader(this.basePackage, this.subPackages, this.excludedPackages, this.filter, this.initialize, this.deep, this.forEachClass);
        }
    }
}

