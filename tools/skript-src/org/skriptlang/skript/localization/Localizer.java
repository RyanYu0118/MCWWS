/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.localization;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.localization.LocalizerImpl;
import org.skriptlang.skript.util.ViewProvider;

public interface Localizer
extends ViewProvider<Localizer> {
    @Contract(value="_ -> new")
    public static Localizer of(SkriptAddon addon) {
        return new LocalizerImpl(addon);
    }

    public void setSourceDirectories(String var1, @Nullable String var2);

    @Nullable
    public String languageFileDirectory();

    @Nullable
    public String dataFileDirectory();

    @Nullable
    public String translate(String var1);

    @Override
    @Contract(value="-> new")
    default public Localizer unmodifiableView() {
        return new LocalizerImpl.UnmodifiableLocalizer(this);
    }
}

