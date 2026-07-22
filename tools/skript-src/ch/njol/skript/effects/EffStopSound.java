/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Sound
 *  org.bukkit.SoundCategory
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.SoundUtils;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.util.regex.Pattern;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Stop Sound")
@Description(value={"Stops specific or all sounds from playing to a group of players. Both Minecraft sound names and <a href=\"https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html\">Spigot sound names</a> are supported. Resource pack sounds are supported too. The sound category is 'master' by default. A sound can't be stopped from a different category. ", "", "Please note that sound names can get changed in any Minecraft or Spigot version, or even removed from Minecraft itself."})
@Example.Examples(value={@Example(value="stop sound \"block.chest.open\" for the player"), @Example(value="stop playing sounds \"ambient.underwater.loop\" and \"ambient.underwater.loop.additions\" to the player"), @Example(value="stop all sounds for all players"), @Example(value="stop sound in the record category")})
@Since(value={"2.4, 2.7 (stop all sounds)"})
public class EffStopSound
extends Effect {
    private static final Pattern KEY_PATTERN = Pattern.compile("([a-z0-9._-]+:)?[a-z0-9/._-]+");
    @Nullable
    private Expression<SoundCategory> category;
    @Nullable
    private Expression<String> sounds;
    private Expression<Player> players;
    private boolean allSounds;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.allSounds = parseResult.hasTag("all");
        this.sounds = exprs[0];
        this.category = exprs[1];
        this.players = exprs[2];
        return true;
    }

    @Override
    protected void execute(Event event) {
        block9: {
            Player[] targets;
            SoundCategory category;
            block8: {
                category = this.category == null ? null : this.category.getOptionalSingle(event).orElse(this.allSounds ? null : SoundCategory.MASTER);
                targets = this.players.getArray(event);
                if (!this.allSounds) break block8;
                if (category == null) {
                    for (Player player : targets) {
                        player.stopAllSounds();
                    }
                } else {
                    for (Player player : targets) {
                        player.stopSound(category);
                    }
                }
                break block9;
            }
            if (this.sounds == null) break block9;
            for (String soundString : this.sounds.getArray(event)) {
                Sound sound = SoundUtils.getSound(soundString);
                if (sound != null) {
                    for (Player player : targets) {
                        player.stopSound(sound, category);
                    }
                    continue;
                }
                if (!KEY_PATTERN.matcher(soundString).matches()) continue;
                for (Player player : targets) {
                    player.stopSound(soundString, category);
                }
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (String)(this.allSounds ? "stop all sounds " : "stop sound " + this.sounds.toString(event, debug)) + (String)(this.category != null ? " in " + this.category.toString(event, debug) : "") + " from playing to " + this.players.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffStopSound.class, "stop (all:all sound[s]|sound[s] %-strings%) [(in [the]|from) %-soundcategory%] [(from playing to|for) %players%]", "stop playing sound[s] %strings% [(in [the]|from) %-soundcategory%] [(to|for) %players%]");
    }
}

