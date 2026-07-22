/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.addon;

import java.util.List;
import java.util.SequencedCollection;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.SkriptAddon;

class AddonModuleImpl {
    AddonModuleImpl() {
    }

    public record ModuleOriginImpl(SkriptAddon addon, SequencedCollection<AddonModule> modules) implements AddonModule.ModuleOrigin
    {
        public ModuleOriginImpl(SkriptAddon addon, AddonModule ... modules) {
            this(addon.unmodifiableView(), List.of(modules));
        }
    }
}

