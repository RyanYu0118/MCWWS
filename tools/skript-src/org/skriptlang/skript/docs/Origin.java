/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 */
package org.skriptlang.skript.docs;

import org.jetbrains.annotations.Contract;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.docs.OriginImpl;

public sealed interface Origin
permits OriginImpl.UnknownOrigin, AddonOrigin {
    public static final Origin UNKNOWN = new OriginImpl.UnknownOrigin();

    @Contract(value="_ -> new")
    public static Origin of(SkriptAddon addon) {
        return new OriginImpl.AddonOriginImpl(addon);
    }

    public String name();

    public static non-sealed interface AddonOrigin
    extends Origin {
        public SkriptAddon addon();

        @Override
        default public String name() {
            return this.addon().name();
        }
    }
}

