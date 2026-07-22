/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.addon;

import java.util.function.Supplier;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.localization.Localizer;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.Registry;
import org.skriptlang.skript.util.ViewProvider;

class SkriptAddonImpl {
    SkriptAddonImpl() {
    }

    static final class UnmodifiableAddon
    implements SkriptAddon {
        private final SkriptAddon addon;
        private final Localizer unmodifiableLocalizer;

        UnmodifiableAddon(SkriptAddon addon) {
            this.addon = addon;
            this.unmodifiableLocalizer = addon.localizer().unmodifiableView();
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
        public <R extends Registry<?>> void storeRegistry(Class<R> registryClass, R registry) {
            throw new UnsupportedOperationException("Cannot store registries on an unmodifiable addon");
        }

        @Override
        public void removeRegistry(Class<? extends Registry<?>> registryClass) {
            throw new UnsupportedOperationException("Cannot remove registries from an unmodifiable addon");
        }

        @Override
        public boolean hasRegistry(Class<? extends Registry<?>> registryClass) {
            return this.addon.hasRegistry(registryClass);
        }

        @Override
        public <R extends Registry<?>> R registry(Class<R> registryClass) {
            Object registry = this.addon.registry(registryClass);
            if (registry instanceof ViewProvider) {
                registry = (Registry)((ViewProvider)registry).unmodifiableView();
            }
            return registry;
        }

        @Override
        public <R extends Registry<?>> R registry(Class<R> registryClass, Supplier<R> putIfAbsent) {
            throw new UnsupportedOperationException("Cannot store registries on an unmodifiable addon");
        }

        @Override
        public SyntaxRegistry syntaxRegistry() {
            return this.addon.syntaxRegistry().unmodifiableView();
        }

        @Override
        public Localizer localizer() {
            return this.unmodifiableLocalizer;
        }

        @Override
        public void loadModules(AddonModule ... modules) {
            throw new UnsupportedOperationException("Cannot load modules using an unmodifiable addon");
        }

        @Override
        public SkriptAddon unmodifiableView() {
            return this;
        }
    }
}

