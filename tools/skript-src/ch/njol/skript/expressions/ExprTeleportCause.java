/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.player.PlayerTeleportEvent$TeleportCause
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import org.bukkit.event.player.PlayerTeleportEvent;

@Name(value="Teleport Cause")
@Description(value={"The <a href='#teleportcause'>teleport cause</a> within a player <a href='#teleport'>teleport</a> event."})
@Example(value="on teleport:\n\tteleport cause is nether portal, end portal or end gateway\n\tcancel event\n")
@Since(value={"2.2-dev35"})
public class ExprTeleportCause
extends EventValueExpression<PlayerTeleportEvent.TeleportCause> {
    public ExprTeleportCause() {
        super(PlayerTeleportEvent.TeleportCause.class);
    }

    static {
        ExprTeleportCause.register(ExprTeleportCause.class, PlayerTeleportEvent.TeleportCause.class, "teleport (cause|reason|type)");
    }
}

