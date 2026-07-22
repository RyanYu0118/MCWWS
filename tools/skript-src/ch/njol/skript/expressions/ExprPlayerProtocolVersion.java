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
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Name(value="Player Protocol Version")
@Description(value={"Player's protocol version. For more information and list of protocol versions <a href='https://wiki.vg/Protocol_version_numbers'>visit wiki.vg</a>."})
@Example(value="command /protocolversion <player>:\n\ttrigger:\n\t\tsend \"Protocol version of %arg-1%: %protocol version of arg-1%\"\n")
@Since(value={"2.6.2"})
public class ExprPlayerProtocolVersion
extends SimplePropertyExpression<Player, Integer> {
    @Override
    @Nullable
    public Integer convert(Player player) {
        int version = player.getProtocolVersion();
        return version == -1 ? null : Integer.valueOf(version);
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return "protocol version";
    }

    static {
        if (Skript.classExists("com.destroystokyo.paper.network.NetworkClient")) {
            ExprPlayerProtocolVersion.register(ExprPlayerProtocolVersion.class, Integer.class, "protocol version", "players");
        }
    }
}

