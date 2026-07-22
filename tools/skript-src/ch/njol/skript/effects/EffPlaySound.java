/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Sound
 *  org.bukkit.SoundCategory
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.SoundUtils;
import ch.njol.skript.bukkitutil.sounds.SoundReceiver;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.OptionalLong;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Play Sound")
@Description(value={"Plays a sound at given location for everyone or just for given players, or plays a sound to specified players. Both Minecraft sound names and <a href=\"https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html\">Spigot sound names</a> are supported. Playing resource pack sounds are supported too. The sound category is 'master' by default. ", "", "When running 1.19+, playing a sound from an entity directly will result in the sound coming from said entity, even while moving.", "If the sound is custom, a location emitter will follow the entity. Do note that pitch and volume ", "are reflected based on the entity, and Minecraft may not use the values from this syntax.", "", "Minecraft sometimes has a set of sounds under one sound ID that will randomly play. To counter this, you can directly state which seed to use.", "", "Please note that sound names can get changed in any Minecraft or Spigot version, or even removed from Minecraft itself."})
@Example.Examples(value={@Example(value="play sound \"block.note_block.pling\""), @Example(value="play sound \"entity.experience_orb.pickup\" with volume 0.5 to the player"), @Example(value="play sound \"custom.music.1\" in jukebox category at {speakerBlock}"), @Example(value="play sound \"BLOCK_AMETHYST_BLOCK_RESONATE\" with seed 1 on target entity for the player")})
@Since(value={"2.2-dev28, 2.4 (sound categories), 2.9 (sound seed & entity emitter)"})
public class EffPlaySound
extends Effect {
    private static final boolean SPIGOT_SOUND_SEED = Skript.methodExists(Player.class, "playSound", Entity.class, Sound.class, SoundCategory.class, Float.TYPE, Float.TYPE, Long.TYPE);
    private static final boolean HAS_SEED = SoundReceiver.ADVENTURE_API || SPIGOT_SOUND_SEED;
    private static final boolean ENTITY_EMITTER_SOUND = Skript.methodExists(Player.class, "playSound", Entity.class, Sound.class, SoundCategory.class, Float.TYPE, Float.TYPE);
    private static final boolean ENTITY_EMITTER_STRING = Skript.methodExists(Player.class, "playSound", Entity.class, String.class, SoundCategory.class, Float.TYPE, Float.TYPE);
    private static final boolean ENTITY_EMITTER = ENTITY_EMITTER_SOUND || ENTITY_EMITTER_STRING;
    private Expression<String> sounds;
    @Nullable
    private Expression<SoundCategory> category;
    @Nullable
    private Expression<Player> players;
    @Nullable
    private Expression<Number> volume;
    @Nullable
    private Expression<Number> pitch;
    @Nullable
    private Expression<Number> seed;
    @Nullable
    private Expression<?> emitters;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.sounds = exprs[0];
        int index = 1;
        if (HAS_SEED) {
            this.seed = exprs[index++];
        }
        this.category = exprs[index++];
        this.volume = exprs[index++];
        this.pitch = exprs[index++];
        if (matchedPattern == 0) {
            this.emitters = exprs[index++];
            this.players = exprs[index];
        } else {
            this.players = exprs[index++];
            this.emitters = exprs[index];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        block17: {
            ArrayList<NamespacedKey> validSounds;
            float pitch;
            float volume;
            SoundCategory category;
            OptionalLong seed;
            block16: {
                Number number;
                seed = OptionalLong.empty();
                if (this.seed != null && (number = this.seed.getSingle(event)) != null) {
                    seed = OptionalLong.of(number.longValue());
                }
                category = this.category == null ? SoundCategory.MASTER : this.category.getOptionalSingle(event).orElse(SoundCategory.MASTER);
                volume = this.volume == null ? 1.0f : this.volume.getOptionalSingle(event).orElse(1).floatValue();
                pitch = this.pitch == null ? 1.0f : this.pitch.getOptionalSingle(event).orElse(1).floatValue();
                validSounds = new ArrayList<NamespacedKey>();
                for (String string : this.sounds.getArray(event)) {
                    NamespacedKey key = SoundUtils.getKey(string);
                    if (key == null) continue;
                    validSounds.add(key);
                }
                if (validSounds.isEmpty()) {
                    return;
                }
                if (this.players == null) break block16;
                if (this.emitters == null) {
                    for (String string : this.players.getArray(event)) {
                        receiver = SoundReceiver.of((Player)string);
                        Location emitter = string.getLocation();
                        for (NamespacedKey sound : validSounds) {
                            receiver.playSound(emitter, sound, category, volume, pitch, seed);
                        }
                    }
                } else {
                    for (String string : this.players.getArray(event)) {
                        receiver = SoundReceiver.of((Player)string);
                        for (Object emitter : this.emitters.getArray(event)) {
                            if (emitter instanceof Location) {
                                for (NamespacedKey sound : validSounds) {
                                    receiver.playSound((Location)emitter, sound, category, volume, pitch, seed);
                                }
                                continue;
                            }
                            if (!(emitter instanceof Entity)) continue;
                            for (NamespacedKey sound : validSounds) {
                                receiver.playSound((Entity)emitter, sound, category, volume, pitch, seed);
                            }
                        }
                    }
                }
                break block17;
            }
            if (this.emitters == null) break block17;
            for (String string : this.emitters.getArray(event)) {
                SoundReceiver receiver;
                if (ENTITY_EMITTER && string instanceof Entity) {
                    Entity entity = (Entity)string;
                    receiver = SoundReceiver.of(entity.getWorld());
                    for (NamespacedKey sound : validSounds) {
                        receiver.playSound((Entity)string, sound, category, volume, pitch, seed);
                    }
                    continue;
                }
                if (!(string instanceof Location)) continue;
                Location location = (Location)string;
                receiver = SoundReceiver.of(location.getWorld());
                for (NamespacedKey sound : validSounds) {
                    receiver.playSound((Location)string, sound, category, volume, pitch, seed);
                }
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        StringBuilder builder = new StringBuilder().append("play sound ").append(this.sounds.toString(event, debug));
        if (this.seed != null) {
            builder.append(" with seed ").append(this.seed.toString(event, debug));
        }
        if (this.category != null) {
            builder.append(" in ").append(this.category.toString(event, debug));
        }
        if (this.volume != null) {
            builder.append(" with volume ").append(this.volume.toString(event, debug));
        }
        if (this.pitch != null) {
            builder.append(" with pitch ").append(this.pitch.toString(event, debug));
        }
        if (this.emitters != null) {
            builder.append(" from ").append(this.emitters.toString(event, debug));
        }
        if (this.players != null) {
            builder.append(" to ").append(this.players.toString(event, debug));
        }
        return builder.toString();
    }

    static {
        String seedOption = HAS_SEED ? "[[with] seed %-number%] " : "";
        Object emitterTypes = "locations";
        if (ENTITY_EMITTER) {
            emitterTypes = (String)emitterTypes + "/entities";
        }
        Skript.registerEffect(EffPlaySound.class, "play sound[s] %strings% " + seedOption + "[(in|from) %-soundcategory%] [(at|with) volume %-number%] [(and|at|with) pitch %-number%] (at|on|from) %" + (String)emitterTypes + "% [(to|for) %-players%]", "play sound[s] %strings% " + seedOption + "[(in|from) %-soundcategory%] [(at|with) volume %-number%] [(and|at|with) pitch %-number%] [(to|for) %players%] [(at|on|from) %-" + (String)emitterTypes + "%]");
    }
}

