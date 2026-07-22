/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.sound.Sound
 *  net.kyori.adventure.sound.Sound$Emitter
 *  net.kyori.adventure.sound.Sound$Source$Provider
 *  org.bukkit.Location
 *  org.bukkit.NamespacedKey
 *  org.bukkit.SoundCategory
 *  org.bukkit.World
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 */
package ch.njol.skript.bukkitutil.sounds;

import java.util.OptionalLong;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class AdventureSoundUtils {
    public static Sound getAdventureSound(NamespacedKey key, SoundCategory category, float volume, float pitch, OptionalLong seed) {
        return (Sound)Sound.sound().source((Sound.Source.Provider)category).volume(volume).pitch(pitch).seed(seed).type((Key)key).build();
    }

    public static void playSound(World world, Location location, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed) {
        world.playSound(AdventureSoundUtils.getAdventureSound(sound, category, volume, pitch, seed), location.x(), location.y(), location.z());
    }

    public static void playSound(World world, Entity entity, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed) {
        world.playSound(AdventureSoundUtils.getAdventureSound(sound, category, volume, pitch, seed), (Sound.Emitter)entity);
    }

    public static void playSound(Player player, Location location, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed) {
        player.playSound(AdventureSoundUtils.getAdventureSound(sound, category, volume, pitch, seed), location.x(), location.y(), location.z());
    }

    public static void playSound(Player player, Entity entity, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed) {
        player.playSound(AdventureSoundUtils.getAdventureSound(sound, category, volume, pitch, seed), (Sound.Emitter)entity);
    }
}

