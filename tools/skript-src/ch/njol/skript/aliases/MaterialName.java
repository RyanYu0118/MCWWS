/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 */
package ch.njol.skript.aliases;

import org.bukkit.Material;

final class MaterialName {
    String singular;
    String plural;
    int gender = 0;
    Material id;

    public MaterialName(Material id, String singular, String plural, int gender) {
        this.id = id;
        this.singular = singular;
        this.plural = plural;
        this.gender = gender;
    }

    public String toString(boolean p) {
        return p ? this.plural : this.singular;
    }

    public String getDebugName(boolean p) {
        return p ? this.plural : this.singular;
    }
}

