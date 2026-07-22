/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.player.PlayerQuitEvent$QuitReason
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.registrations.EventValues;
import org.bukkit.event.player.PlayerQuitEvent;

@Name(value="Quit Reason")
@Description(value={"The <a href='#quitreason'>quit reason</a> as to why a player disconnected in a <a href='#quit'>quit</a> event."})
@Example(value="on quit:\n\tquit reason was kicked\n\tplayer is banned\n\tclear {server::player::%uuid of player%::*}\n")
@Since(value={"2.8.0"})
public class ExprQuitReason
extends EventValueExpression<PlayerQuitEvent.QuitReason> {
    public ExprQuitReason() {
        super(PlayerQuitEvent.QuitReason.class);
    }

    @Override
    public boolean setTime(int time) {
        return time != EventValues.TIME_FUTURE;
    }

    static {
        if (Skript.classExists("org.bukkit.event.player.PlayerQuitEvent$QuitReason")) {
            ExprQuitReason.register(ExprQuitReason.class, PlayerQuitEvent.QuitReason.class, "(quit|disconnect) (cause|reason)");
        }
    }
}

