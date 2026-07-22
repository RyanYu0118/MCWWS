/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Keyed
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.bukkit.Sound
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.bukkitutil;

import java.util.Locale;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SoundUtils {
    private static final boolean SOUND_IS_INTERFACE = Sound.class.isInterface();

    @Nullable
    public static NamespacedKey getKey(String soundString) {
        soundString = soundString.toUpperCase(Locale.ENGLISH);
        if (SOUND_IS_INTERFACE) {
            try {
                return Sound.valueOf((String)soundString).getKey();
            }
            catch (Exception exception) {
            }
        } else {
            try {
                Sound soundEnum = Enum.valueOf(Sound.class, soundString);
                return ((Keyed)soundEnum).getKey();
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
        return NamespacedKey.fromString((String)soundString.toLowerCase(Locale.ENGLISH));
    }

    @NotNull
    public static NamespacedKey getKey(Sound sound) {
        if (SOUND_IS_INTERFACE) {
            return sound.getKey();
        }
        return sound.getKey();
    }

    @Nullable
    public static Sound getSound(String soundString) {
        NamespacedKey key = SoundUtils.getKey(soundString);
        if (key == null) {
            return null;
        }
        return (Sound)Registry.SOUNDS.get(key);
    }
}

