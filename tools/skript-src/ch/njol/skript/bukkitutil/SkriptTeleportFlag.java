/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.entity.TeleportFlag
 *  io.papermc.paper.entity.TeleportFlag$EntityState
 *  io.papermc.paper.entity.TeleportFlag$Relative
 */
package ch.njol.skript.bukkitutil;

import io.papermc.paper.entity.TeleportFlag;

public enum SkriptTeleportFlag {
    RETAIN_OPEN_INVENTORY(new TeleportFlag[]{TeleportFlag.EntityState.RETAIN_OPEN_INVENTORY}),
    RETAIN_PASSENGERS(new TeleportFlag[]{TeleportFlag.EntityState.RETAIN_PASSENGERS}),
    RETAIN_VEHICLE(new TeleportFlag[]{TeleportFlag.EntityState.RETAIN_VEHICLE}),
    RETAIN_DIRECTION(new TeleportFlag[]{TeleportFlag.Relative.PITCH, TeleportFlag.Relative.YAW}),
    RETAIN_PITCH(new TeleportFlag[]{TeleportFlag.Relative.PITCH}),
    RETAIN_YAW(new TeleportFlag[]{TeleportFlag.Relative.YAW}),
    RETAIN_MOVEMENT(new TeleportFlag[]{TeleportFlag.Relative.X, TeleportFlag.Relative.Y, TeleportFlag.Relative.Z}),
    RETAIN_X(new TeleportFlag[]{TeleportFlag.Relative.X}),
    RETAIN_Y(new TeleportFlag[]{TeleportFlag.Relative.Y}),
    RETAIN_Z(new TeleportFlag[]{TeleportFlag.Relative.Z});

    final TeleportFlag[] teleportFlags;

    private SkriptTeleportFlag(TeleportFlag ... teleportFlags) {
        this.teleportFlags = teleportFlags;
    }

    public TeleportFlag[] getTeleportFlags() {
        return this.teleportFlags;
    }
}

