/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Name(value="View Distance of Client")
@Description(value={"The view distance of the client. Can not be changed. This differs from the server side view distance of player as this will retrieve the view distance the player has set on their client."})
@Example.Examples(value={@Example(value="set {_clientView} to the client view distance of player"), @Example(value="set view distance of player to client view distance of player")})
@RequiredPlugins(value={"1.13.2+"})
@Since(value={"2.5"})
public class ExprClientViewDistance
extends SimplePropertyExpression<Player, Long> {
    @Override
    @Nullable
    public Long convert(Player player) {
        return player.getClientViewDistance();
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    protected String getPropertyName() {
        return "client view distance";
    }

    static {
        if (Skript.methodExists(Player.class, "getClientViewDistance", new Class[0])) {
            ExprClientViewDistance.register(ExprClientViewDistance.class, Long.class, "client view distance[s]", "players");
        }
    }
}

