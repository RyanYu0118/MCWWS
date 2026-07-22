/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript;

import java.util.Collection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.SkriptImpl;
import org.skriptlang.skript.addon.SkriptAddon;

public interface Skript
extends SkriptAddon {
    @Contract(value="_, _ -> new")
    public static Skript of(Class<?> source, String name) {
        return new SkriptImpl(source, name);
    }

    @Contract(value="_, _ -> new")
    public SkriptAddon registerAddon(Class<?> var1, String var2);

    public @Unmodifiable Collection<SkriptAddon> addons();

    @Nullable
    default public SkriptAddon addon(String name) {
        return this.addons().stream().filter(addon -> addon.name().equals(name)).findFirst().orElse(null);
    }

    @Override
    @Contract(value="-> new")
    default public Skript unmodifiableView() {
        return new SkriptImpl.UnmodifiableSkript(this, SkriptAddon.super.unmodifiableView());
    }
}

