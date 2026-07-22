/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.player.PlayerRespawnEvent$RespawnReason
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import org.bukkit.event.player.PlayerRespawnEvent;

@Name(value="Respawn Reason")
@Description(value={"The <a href='#respawnreason'>respawn reason</a> in a <a href='#respawn'>respawn</a> event."})
@Example(value="on respawn:\n\tif respawn reason is end portal:\n\t\tbroadcast \"%player% took the end portal to the overworld!\"\n")
@Since(value={"2.14"})
public class ExprRespawnReason
extends EventValueExpression<PlayerRespawnEvent.RespawnReason> {
    public ExprRespawnReason() {
        super(PlayerRespawnEvent.RespawnReason.class);
    }

    static {
        ExprRespawnReason.register(ExprRespawnReason.class, PlayerRespawnEvent.RespawnReason.class, "respawn[ing] reason");
    }
}

