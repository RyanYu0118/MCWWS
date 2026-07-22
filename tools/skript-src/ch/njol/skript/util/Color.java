/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.DyeColor
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.yggdrasil.YggdrasilSerializable;
import org.bukkit.DyeColor;
import org.jetbrains.annotations.Nullable;

public interface Color
extends YggdrasilSerializable.YggdrasilExtendedSerializable {
    public org.bukkit.Color asBukkitColor();

    public int getAlpha();

    public int getRed();

    public int getGreen();

    public int getBlue();

    @Nullable
    public DyeColor asDyeColor();

    public String getName();

    default public int asARGB() {
        return this.asBukkitColor().asARGB();
    }

    default public String toHexString() {
        return String.format("%02X%02X%02X", this.getRed(), this.getGreen(), this.getBlue());
    }
}

