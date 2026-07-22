/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.server.PaperServerListPingEvent
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerLoginEvent
 *  org.bukkit.event.server.ServerListPingEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.stream.Stream;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="IP")
@Description(value={"The IP address of a player, or the connected player in a <a href='#connect'>connect</a> event, or the pinger in a <a href='#server_list_ping'>server list ping</a> event."})
@Example.Examples(value={@Example(value="ban the IP address of the player\")\nbroadcast \"Banned the IP %IP of player%\"\n"), @Example(value="on connect:\n\tlog \"[%now%] %player% (%ip%) is connected to the server.\"\n"), @Example(value="on server list ping:\n\tsend \"%IP-address%\" to the console\n")})
@Since(value={"1.4, 2.2-dev26 (when used in connect event), 2.3 (when used in server list ping event)"})
public class ExprIP
extends SimpleExpression<String> {
    private Expression<Player> players;
    private boolean isProperty;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        boolean isServerPingEvent;
        this.isProperty = matchedPattern < 2;
        boolean isConnectEvent = this.getParser().isCurrentEvent((Class<? extends Event>)PlayerLoginEvent.class);
        boolean bl = isServerPingEvent = this.getParser().isCurrentEvent((Class<? extends Event>)ServerListPingEvent.class) || this.getParser().isCurrentEvent((Class<? extends Event>)PaperServerListPingEvent.class);
        if (this.isProperty) {
            this.players = exprs[0];
        } else if (!isConnectEvent && !isServerPingEvent) {
            Skript.error("You must specify players whose IP addresses to get outside of server list ping and connect events.");
            return false;
        }
        return true;
    }

    @Nullable
    protected String[] get(Event e) {
        if (!this.isProperty) {
            InetAddress address;
            if (e instanceof PlayerLoginEvent) {
                PlayerLoginEvent loginEvent = (PlayerLoginEvent)e;
                address = loginEvent.getAddress();
            } else if (e instanceof ServerListPingEvent) {
                ServerListPingEvent pingEvent = (ServerListPingEvent)e;
                address = pingEvent.getAddress();
            } else {
                return null;
            }
            return CollectionUtils.array(address.getHostAddress());
        }
        return (String[])Stream.of(this.players.getArray(e)).map(player -> {
            assert (player != null);
            return this.getIP((Player)player, e);
        }).toArray(String[]::new);
    }

    private String getIP(Player player, Event e) {
        String hostAddress;
        InetAddress address;
        if (e instanceof PlayerLoginEvent && ((PlayerLoginEvent)e).getPlayer().equals((Object)player)) {
            address = ((PlayerLoginEvent)e).getAddress();
        } else {
            InetSocketAddress sockAddr = player.getAddress();
            assert (sockAddr != null);
            address = sockAddr.getAddress();
        }
        String string = hostAddress = address == null ? "unknown" : address.getHostAddress();
        assert (hostAddress != null);
        return hostAddress;
    }

    @Override
    public boolean isSingle() {
        return !this.isProperty || this.players.isSingle();
    }

    @Override
    public Class<String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        if (e == null || !this.isProperty) {
            return "the IP address";
        }
        return "the IP address of " + this.players.toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprIP.class, String.class, ExpressionType.PROPERTY, "IP[s][( |-)address[es]] of %players%", "%players%'[s] IP[s][( |-)address[es]]", "IP[( |-)address]");
    }
}

