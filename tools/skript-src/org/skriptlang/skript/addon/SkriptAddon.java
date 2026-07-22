/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 */
package org.skriptlang.skript.addon;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.jetbrains.annotations.Contract;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.SkriptAddonImpl;
import org.skriptlang.skript.localization.Localizer;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.Registry;
import org.skriptlang.skript.util.ViewProvider;

public interface SkriptAddon
extends ViewProvider<SkriptAddon> {
    public Class<?> source();

    public String name();

    public <R extends Registry<?>> void storeRegistry(Class<R> var1, R var2);

    public void removeRegistry(Class<? extends Registry<?>> var1);

    public boolean hasRegistry(Class<? extends Registry<?>> var1);

    public <R extends Registry<?>> R registry(Class<R> var1);

    public <R extends Registry<?>> R registry(Class<R> var1, Supplier<R> var2);

    public SyntaxRegistry syntaxRegistry();

    public Localizer localizer();

    default public void loadModules(AddonModule ... modules) {
        List<AddonModule> filtered = Arrays.stream(modules).filter(addonModule -> addonModule.canLoad(this)).toList();
        for (AddonModule module : filtered) {
            module.init(this);
        }
        for (AddonModule module : filtered) {
            module.load(this);
        }
    }

    @Override
    @Contract(value="-> new")
    default public SkriptAddon unmodifiableView() {
        return new SkriptAddonImpl.UnmodifiableAddon(this);
    }
}

