/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.localization;

import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.localization.Language;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.localization.Localizer;

final class LocalizerImpl
implements Localizer {
    private final SkriptAddon addon;
    private String languageFileDirectory;
    private String dataFileDirectory;

    LocalizerImpl(SkriptAddon addon) {
        this.addon = addon;
    }

    @Override
    public void setSourceDirectories(String languageFileDirectory, @Nullable String dataFileDirectory) {
        if (this.languageFileDirectory != null) {
            throw new SkriptAPIException("A localizer's source directories may only be set once.");
        }
        this.languageFileDirectory = languageFileDirectory;
        this.dataFileDirectory = dataFileDirectory;
        Language.loadDefault(this.addon);
    }

    @Override
    @Nullable
    public String languageFileDirectory() {
        return this.languageFileDirectory;
    }

    @Override
    @Nullable
    public String dataFileDirectory() {
        return this.dataFileDirectory;
    }

    @Override
    @Nullable
    public String translate(String key) {
        return Language.get_(key);
    }

    static final class UnmodifiableLocalizer
    implements Localizer {
        private final Localizer localizer;

        UnmodifiableLocalizer(Localizer localizer) {
            this.localizer = localizer;
        }

        @Override
        public void setSourceDirectories(String languageFileDirectory, @Nullable String dataFileDirectory) {
            throw new UnsupportedOperationException("Cannot set the source directories of an unmodifiable Localizer.");
        }

        @Override
        @Nullable
        public String languageFileDirectory() {
            return this.localizer.languageFileDirectory();
        }

        @Override
        @Nullable
        public String dataFileDirectory() {
            return this.localizer.dataFileDirectory();
        }

        @Override
        @Nullable
        public String translate(String key) {
            return this.localizer.translate(key);
        }

        @Override
        public Localizer unmodifiableView() {
            return this;
        }
    }
}

