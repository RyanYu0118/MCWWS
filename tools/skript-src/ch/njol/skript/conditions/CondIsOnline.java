/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.OfflinePlayer
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.OfflinePlayer;

@Name(value="Is Online")
@Description(value={"Checks whether a player is online. The 'connected' pattern will return false once this player leaves the server, even if they rejoin. Be aware that using the 'connected' pattern with a variable will not have this special behavior. Use the direct event-player or other non-variable expression for best results."})
@Example.Examples(value={@Example(value="player is online"), @Example(value="player-argument is offline"), @Example(value="while player is connected:\n\twait 60 seconds\n\tsend \"hello!\" to player\n"), @Example(value="# The following will act like `{_player} is online`.\n# Using variables with `is connected` will not behave the same as with non-variables.\nwhile {_player} is connected:\n\tbroadcast \"online!\"\n\twait 1 tick\n")})
@Since(value={"1.4"})
public class CondIsOnline
extends PropertyCondition<OfflinePlayer> {
    private boolean connected;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        this.setNegated(matchedPattern == 1 ^ parseResult.hasTag("offline"));
        this.connected = parseResult.hasTag("connected");
        return true;
    }

    @Override
    public boolean check(OfflinePlayer op) {
        if (this.connected) {
            return op.isConnected();
        }
        return op.isOnline();
    }

    @Override
    protected String getPropertyName() {
        return this.connected ? "connected" : "online";
    }

    static {
        if (Skript.methodExists(OfflinePlayer.class, "isConnected", new Class[0])) {
            CondIsOnline.register(CondIsOnline.class, "(online|:offline|:connected)", "offlineplayers");
        } else {
            CondIsOnline.register(CondIsOnline.class, "(online|:offline)", "offlineplayers");
        }
    }
}

