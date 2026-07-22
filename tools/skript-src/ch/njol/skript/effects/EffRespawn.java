/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityDeathEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
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
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

@Name(value="Force Respawn")
@Description(value={"Forces player(s) to respawn if they are dead. If this is called without delay from death event, one tick is waited before respawn attempt."})
@Example(value="on death of player:\n\tforce event-player to respawn\n")
@Since(value={"2.2-dev21"})
public class EffRespawn
extends Effect {
    private Expression<Player> players;
    private boolean forceDelay;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (this.getParser().isCurrentEvent((Class<? extends Event>)PlayerRespawnEvent.class)) {
            Skript.error("Respawning the player in a respawn event is not possible", ErrorQuality.SEMANTIC_ERROR);
            return false;
        }
        this.players = exprs[0];
        this.forceDelay = this.getParser().isCurrentEvent((Class<? extends Event>)EntityDeathEvent.class) && isDelayed.isFalse();
        return true;
    }

    @Override
    protected void execute(Event e) {
        for (final Player p : this.players.getArray(e)) {
            if (this.forceDelay) {
                new BukkitRunnable(this){

                    public void run() {
                        p.spigot().respawn();
                    }
                }.runTaskLater((Plugin)Skript.getInstance(), 1L);
                continue;
            }
            p.spigot().respawn();
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "force " + this.players.toString(e, debug) + " to respawn";
    }

    static {
        Skript.registerEffect(EffRespawn.class, "force %players% to respawn");
    }
}

