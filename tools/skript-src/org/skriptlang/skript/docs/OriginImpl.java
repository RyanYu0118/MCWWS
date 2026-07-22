/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.docs;

import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.docs.Origin;

final class OriginImpl {
    OriginImpl() {
    }

    public record AddonOriginImpl(SkriptAddon addon) implements Origin.AddonOrigin
    {
        public AddonOriginImpl(SkriptAddon addon) {
            this.addon = addon.unmodifiableView();
        }
    }

    public static final class UnknownOrigin
    implements Origin {
        @Override
        public String name() {
            return "unknown";
        }
    }
}

