/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.ImmutableSet$Builder
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript;

import ch.njol.skript.SkriptAPIException;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.Skript;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.localization.Localizer;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.Registry;

final class SkriptImpl
implements Skript {
    private final SkriptAddon addon;
    private final Map<Class<?>, Registry<?>> registries = new ConcurrentHashMap();
    private final Map<String, SkriptAddon> addons = new HashMap<String, SkriptAddon>();

    SkriptImpl(Class<?> source, String name) {
        this.addon = new SkriptAddonImpl(this, source, name, Localizer.of(this));
        this.storeRegistry(SyntaxRegistry.class, SyntaxRegistry.withOrigin(SyntaxRegistry.empty(), Origin.of(this)));
    }

    @Override
    public <R extends Registry<?>> void storeRegistry(Class<R> registryClass, R registry) {
        this.registries.put(registryClass, registry);
    }

    @Override
    public void removeRegistry(Class<? extends Registry<?>> registryClass) {
        this.registries.remove(registryClass);
    }

    @Override
    public boolean hasRegistry(Class<? extends Registry<?>> registryClass) {
        return this.registries.containsKey(registryClass);
    }

    @Override
    public <R extends Registry<?>> R registry(Class<R> registryClass) {
        Registry<?> registry = this.registries.get(registryClass);
        if (registry == null) {
            throw new NullPointerException("Registry not present for " + String.valueOf(registryClass));
        }
        return (R)registry;
    }

    @Override
    public <R extends Registry<?>> R registry(Class<R> registryClass, Supplier<R> putIfAbsent) {
        return (R)this.registries.computeIfAbsent(registryClass, key -> (Registry)putIfAbsent.get());
    }

    @Override
    public SkriptAddon registerAddon(Class<?> source, String name) {
        if (this.addon.name().equals(name)) {
            throw new SkriptAPIException("Registering an addon with the same name as the Skript instance is not possible");
        }
        SkriptAddon existing = this.addons.get(name);
        if (existing != null) {
            throw new SkriptAPIException("An addon (provided by '" + existing.source().getName() + "') with the name '" + name + "' is already registered");
        }
        SkriptAddonImpl addon = new SkriptAddonImpl(this, source, name, null);
        this.addons.put(name, addon);
        return addon;
    }

    @Override
    public @Unmodifiable Collection<SkriptAddon> addons() {
        return ImmutableSet.copyOf(this.addons.values());
    }

    @Override
    public Class<?> source() {
        return this.addon.source();
    }

    @Override
    public String name() {
        return this.addon.name();
    }

    @Override
    public SyntaxRegistry syntaxRegistry() {
        return this.registry(SyntaxRegistry.class);
    }

    @Override
    public Localizer localizer() {
        return this.addon.localizer();
    }

    @Override
    public void loadModules(AddonModule ... modules) {
        this.addon.loadModules(modules);
    }

    private static final class SkriptAddonImpl
    implements SkriptAddon {
        private final Skript skript;
        private final Class<?> source;
        private final String name;
        private final Localizer localizer;
        private final Origin origin;

        SkriptAddonImpl(Skript skript, Class<?> source, String name, @Nullable Localizer localizer) {
            this.skript = skript;
            this.source = source;
            this.name = name;
            this.localizer = localizer == null ? Localizer.of(this) : localizer;
            this.origin = Origin.of(this);
        }

        @Override
        public Class<?> source() {
            return this.source;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public <R extends Registry<?>> void storeRegistry(Class<R> registryClass, R registry) {
            this.skript.storeRegistry(registryClass, registry);
        }

        @Override
        public void removeRegistry(Class<? extends Registry<?>> registryClass) {
            this.skript.removeRegistry(registryClass);
        }

        @Override
        public boolean hasRegistry(Class<? extends Registry<?>> registryClass) {
            return this.skript.hasRegistry(registryClass);
        }

        @Override
        public <R extends Registry<?>> R registry(Class<R> registryClass) {
            R registry = this.skript.registry(registryClass);
            if (registryClass == SyntaxRegistry.class) {
                return (R)SyntaxRegistry.withOrigin((SyntaxRegistry)registry, this.origin);
            }
            return registry;
        }

        @Override
        public <R extends Registry<?>> R registry(Class<R> registryClass, Supplier<R> putIfAbsent) {
            return this.skript.registry(registryClass, putIfAbsent);
        }

        @Override
        public SyntaxRegistry syntaxRegistry() {
            return this.registry(SyntaxRegistry.class);
        }

        @Override
        public Localizer localizer() {
            return this.localizer;
        }
    }

    static final class UnmodifiableSkript
    implements Skript {
        private final Skript skript;
        private final SkriptAddon unmodifiableAddon;

        UnmodifiableSkript(Skript skript, SkriptAddon unmodifiableAddon) {
            this.skript = skript;
            this.unmodifiableAddon = unmodifiableAddon;
        }

        @Override
        public SkriptAddon registerAddon(Class<?> source, String name) {
            throw new UnsupportedOperationException("Cannot register addons using an unmodifiable Skript");
        }

        @Override
        public @Unmodifiable Collection<SkriptAddon> addons() {
            ImmutableSet.Builder addons = ImmutableSet.builder();
            this.skript.addons().stream().map(SkriptAddon::unmodifiableView).forEach(arg_0 -> ((ImmutableSet.Builder)addons).add(arg_0));
            return addons.build();
        }

        @Override
        public Class<?> source() {
            return this.skript.source();
        }

        @Override
        public String name() {
            return this.skript.name();
        }

        @Override
        public <R extends Registry<?>> void storeRegistry(Class<R> registryClass, R registry) {
            this.unmodifiableAddon.storeRegistry(registryClass, registry);
        }

        @Override
        public void removeRegistry(Class<? extends Registry<?>> registryClass) {
            this.unmodifiableAddon.removeRegistry(registryClass);
        }

        @Override
        public boolean hasRegistry(Class<? extends Registry<?>> registryClass) {
            return this.unmodifiableAddon.hasRegistry(registryClass);
        }

        @Override
        public <R extends Registry<?>> R registry(Class<R> registryClass) {
            return this.unmodifiableAddon.registry(registryClass);
        }

        @Override
        public <R extends Registry<?>> R registry(Class<R> registryClass, Supplier<R> putIfAbsent) {
            return this.unmodifiableAddon.registry(registryClass, putIfAbsent);
        }

        @Override
        public SyntaxRegistry syntaxRegistry() {
            return this.unmodifiableAddon.syntaxRegistry();
        }

        @Override
        public Localizer localizer() {
            return this.unmodifiableAddon.localizer();
        }

        @Override
        public void loadModules(AddonModule ... modules) {
            this.unmodifiableAddon.loadModules(modules);
        }

        @Override
        public Skript unmodifiableView() {
            return this;
        }
    }
}

