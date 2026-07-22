/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Sprinting")
@Description(value={"Make a player start or stop sprinting.", "If the player is not moving when this effect is used, they will be put in sprint mode for a tick and then stopped (this causes the FOV to change). Using it a second time, without the player manually sprinting in between, causes the player to stay in sprint mode, with some quirks.", " - Particles may not be produced under the player's feet.", " - The player will not exit the sprinting state if they stop moving.", " - Restrictions like low hunger will not prevent the player from sprinting", " - The player pressing shift will stop them sprinting, and pressing sprint will re-assert normal sprinting behavior", "Using this effect two or more consecutive times on a stationary player produces undefined behavior and should not be relied on."})
@Example.Examples(value={@Example(value="make player start sprinting"), @Example(value="force player to start sprinting")})
@Since(value={"2.11"})
public class EffSprinting
extends Effect {
    private Expression<Player> players;
    private boolean sprint;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.players = exprs[0];
        this.sprint = matchedPattern <= 1;
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (Player player : this.players.getArray(event)) {
            player.setSprinting(this.sprint);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "make " + this.players.toString(event, debug) + (this.sprint ? " start" : " stop") + " sprinting";
    }

    static {
        Skript.registerEffect(EffSprinting.class, "make %players% (start sprinting|sprint)", "force %players% to (start sprinting|sprint)", "make %players% (stop sprinting|not sprint)", "force %players% to (stop sprinting|not sprint)");
    }
}

