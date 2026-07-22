/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.addon;

import java.util.SequencedCollection;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.addon.AddonModuleImpl;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.registration.SyntaxRegistry;

public interface AddonModule {
    public static ModuleOrigin origin(SkriptAddon addon, AddonModule module) {
        return new AddonModuleImpl.ModuleOriginImpl(addon, module);
    }

    public static ModuleOrigin origin(SkriptAddon addon, AddonModule ... modules) {
        return new AddonModuleImpl.ModuleOriginImpl(addon, modules);
    }

    default public boolean canLoad(SkriptAddon addon) {
        return true;
    }

    default public void init(SkriptAddon addon) {
    }

    public void load(SkriptAddon var1);

    public String name();

    default public Origin origin(SkriptAddon addon) {
        return AddonModule.origin(addon, this);
    }

    default public SyntaxRegistry moduleRegistry(SkriptAddon addon) {
        return SyntaxRegistry.withOrigin(addon.syntaxRegistry(), this.origin(addon));
    }

    default public void register(SkriptAddon addon, Registrar ... registrationMethods) {
        SyntaxRegistry registry = this.moduleRegistry(addon);
        for (Registrar func : registrationMethods) {
            func.register(registry);
        }
    }

    public static sealed interface ModuleOrigin
    extends Origin.AddonOrigin
    permits AddonModuleImpl.ModuleOriginImpl {
        public @Unmodifiable SequencedCollection<AddonModule> modules();

        @Deprecated(since="2.15", forRemoval=true)
        default public String moduleName() {
            return this.modules().stream().map(AddonModule::name).collect(Collectors.joining(", "));
        }
    }

    @FunctionalInterface
    public static interface Registrar {
        public void register(SyntaxRegistry var1);
    }
}

