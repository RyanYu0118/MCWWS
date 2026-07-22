/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerKickEvent
 *  org.bukkit.event.player.PlayerLoginEvent
 *  org.bukkit.event.player.PlayerLoginEvent$Result
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Kick")
@Description(value={"Kicks a player from the server."})
@Example(value="on place of TNT, lava, or obsidian:\n\tkick the player due to \"You may not place %block%!\"\n\tcancel the event\n")
@Since(value={"1.0"})
public class EffKick
extends Effect {
    private Expression<Player> players;
    @Nullable
    private Expression<String> reason;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.players = exprs[0];
        this.reason = exprs[1];
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "kick " + this.players.toString(e, debug) + (String)(this.reason != null ? " on account of " + this.reason.toString(e, debug) : "");
    }

    @Override
    protected void execute(Event e) {
        String r;
        String string = r = this.reason != null ? this.reason.getSingle(e) : "";
        if (r == null) {
            return;
        }
        for (Player p : this.players.getArray(e)) {
            if (e instanceof PlayerLoginEvent && p.equals((Object)((PlayerLoginEvent)e).getPlayer()) && !Delay.isDelayed(e)) {
                ((PlayerLoginEvent)e).disallow(PlayerLoginEvent.Result.KICK_OTHER, r);
                continue;
            }
            if (e instanceof PlayerKickEvent && p.equals((Object)((PlayerKickEvent)e).getPlayer()) && !Delay.isDelayed(e)) {
                ((PlayerKickEvent)e).setLeaveMessage(r);
                continue;
            }
            p.kickPlayer(r);
        }
    }

    static {
        Skript.registerEffect(EffKick.class, "kick %players% [(by reason of|because [of]|on account of|due to) %-string%]");
    }
}

