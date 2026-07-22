/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Sound
 *  org.bukkit.SoundCategory
 *  org.bukkit.World
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 */
package ch.njol.skript.bukkitutil.sounds;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.sounds.AdventureSoundUtils;
import java.util.Locale;
import java.util.OptionalLong;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface SoundReceiver {
    public static final boolean ADVENTURE_API = Skript.classExists("net.kyori.adventure.sound.Sound$Builder") && Skript.methodExists(SoundCategory.class, "soundSource", new Class[0]);
    public static final boolean SPIGOT_SOUND_SEED = Skript.methodExists(Player.class, "playSound", Entity.class, Sound.class, SoundCategory.class, Float.TYPE, Float.TYPE, Long.TYPE);
    public static final boolean ENTITY_EMITTER_SOUND = Skript.methodExists(Player.class, "playSound", Entity.class, Sound.class, SoundCategory.class, Float.TYPE, Float.TYPE);
    public static final boolean ENTITY_EMITTER_STRING = Skript.methodExists(Player.class, "playSound", Entity.class, String.class, SoundCategory.class, Float.TYPE, Float.TYPE);

    public void playSound(Location var1, NamespacedKey var2, SoundCategory var3, float var4, float var5, OptionalLong var6);

    public void playSound(Entity var1, NamespacedKey var2, SoundCategory var3, float var4, float var5, OptionalLong var6);

    public static SoundReceiver of(Player player) {
        return new PlayerSoundReceiver(player);
    }

    public static SoundReceiver of(World world) {
        return new WorldSoundReceiver(world);
    }

    public static class PlayerSoundReceiver
    implements SoundReceiver {
        private final Player player;

        protected PlayerSoundReceiver(Player player) {
            this.player = player;
        }

        @Override
        public void playSound(Location location, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed) {
            if (ADVENTURE_API) {
                AdventureSoundUtils.playSound(this.player, location, sound, category, volume, pitch, seed);
            } else if (!SPIGOT_SOUND_SEED || seed.isEmpty()) {
                this.player.playSound(location, sound.getKey(), category, volume, pitch);
            } else {
                this.player.playSound(location, sound.getKey(), category, volume, pitch, seed.getAsLong());
            }
        }

        private void playSound(Entity entity, String sound, SoundCategory category, float volume, float pitch) {
            if (ENTITY_EMITTER_STRING) {
                this.player.playSound(entity, sound, category, volume, pitch);
            } else if (ENTITY_EMITTER_SOUND) {
                Sound enumSound;
                try {
                    enumSound = Sound.valueOf((String)sound.replace('.', '_').toUpperCase(Locale.ENGLISH));
                }
                catch (IllegalArgumentException e) {
                    return;
                }
                this.player.playSound(entity, enumSound, category, volume, pitch);
            } else {
                this.player.playSound(entity.getLocation(), sound, category, volume, pitch);
            }
        }

        @Override
        public void playSound(Entity entity, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed) {
            if (ADVENTURE_API) {
                AdventureSoundUtils.playSound(this.player, entity, sound, category, volume, pitch, seed);
            } else if (!SPIGOT_SOUND_SEED || seed.isEmpty()) {
                this.playSound(entity, sound.getKey(), category, volume, pitch);
            } else {
                this.player.playSound(entity, sound.getKey(), category, volume, pitch, seed.getAsLong());
            }
        }
    }

    public static class WorldSoundReceiver
    implements SoundReceiver {
        private final World world;

        protected WorldSoundReceiver(World world) {
            this.world = world;
        }

        @Override
        public void playSound(Location location, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed) {
            if (ADVENTURE_API) {
                AdventureSoundUtils.playSound(this.world, location, sound, category, volume, pitch, seed);
            } else if (!SPIGOT_SOUND_SEED || seed.isEmpty()) {
                this.world.playSound(location, sound.getKey(), category, volume, pitch);
            } else {
                this.world.playSound(location, sound.getKey(), category, volume, pitch, seed.getAsLong());
            }
        }

        private void playSound(Entity entity, String sound, SoundCategory category, float volume, float pitch) {
            if (ENTITY_EMITTER_STRING) {
                this.world.playSound(entity, sound, category, volume, pitch);
            } else if (ENTITY_EMITTER_SOUND) {
                Sound enumSound;
                try {
                    enumSound = Sound.valueOf((String)sound.replace('.', '_').toUpperCase(Locale.ENGLISH));
                }
                catch (IllegalArgumentException e) {
                    return;
                }
                this.world.playSound(entity, enumSound, category, volume, pitch);
            } else {
                this.world.playSound(entity.getLocation(), sound, category, volume, pitch);
            }
        }

        @Override
        public void playSound(Entity entity, NamespacedKey sound, SoundCategory category, float volume, float pitch, OptionalLong seed) {
            if (ADVENTURE_API) {
                AdventureSoundUtils.playSound(this.world, entity, sound, category, volume, pitch, seed);
            } else if (!SPIGOT_SOUND_SEED || seed.isEmpty()) {
                this.playSound(entity, sound.getKey(), category, volume, pitch);
            } else {
                this.world.playSound(entity, sound.getKey(), category, volume, pitch, seed.getAsLong());
            }
        }
    }
}

