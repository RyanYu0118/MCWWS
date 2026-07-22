/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.paperlib.environments;

import ch.njol.skript.paperlib.environments.CraftBukkitEnvironment;

public class SpigotEnvironment
extends CraftBukkitEnvironment {
    @Override
    public String getName() {
        return "Spigot";
    }

    @Override
    public boolean isSpigot() {
        return true;
    }
}

