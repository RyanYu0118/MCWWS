/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.player.PlayerResourcePackStatusEvent$Status
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Last Resource Pack Response")
@Description(value={"Returns the last resource pack response received from a player."})
@Example(value="if player's last resource pack response is deny or download fail:")
@Since(value={"2.4"})
public class ExprLastResourcePackResponse
extends SimplePropertyExpression<Player, PlayerResourcePackStatusEvent.Status> {
    @Override
    @Nullable
    public PlayerResourcePackStatusEvent.Status convert(Player p) {
        return p.getResourcePackStatus();
    }

    @Override
    protected String getPropertyName() {
        return "resource pack response";
    }

    @Override
    public Class<PlayerResourcePackStatusEvent.Status> getReturnType() {
        return PlayerResourcePackStatusEvent.Status.class;
    }

    static {
        if (Skript.methodExists(Player.class, "getResourcePackStatus", new Class[0])) {
            ExprLastResourcePackResponse.register(ExprLastResourcePackResponse.class, PlayerResourcePackStatusEvent.Status.class, "[last] resource pack response[s]", "players");
        }
    }
}

