/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
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
import java.net.InetSocketAddress;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@Name(value="Is Banned")
@Description(value={"Checks whether a player or IP is banned."})
@Example.Examples(value={@Example(value="player is banned"), @Example(value="victim is not IP-banned"), @Example(value="\"127.0.0.1\" is banned")})
@Since(value={"1.4"})
public class CondIsBanned
extends PropertyCondition<Object> {
    private boolean ipBanned;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        this.setNegated(matchedPattern >= 2);
        this.ipBanned = matchedPattern % 2 != 0;
        return true;
    }

    @Override
    public boolean check(Object obj) {
        if (obj instanceof Player) {
            Player player = (Player)obj;
            if (this.ipBanned) {
                InetSocketAddress sockAddr = player.getAddress();
                if (sockAddr == null) {
                    return false;
                }
                return Bukkit.getIPBans().contains(sockAddr.getAddress().getHostAddress());
            }
            return player.isBanned();
        }
        if (obj instanceof OfflinePlayer) {
            OfflinePlayer offlinePlayer = (OfflinePlayer)obj;
            return offlinePlayer.isBanned();
        }
        if (obj instanceof String) {
            return Bukkit.getIPBans().contains(obj) || !this.ipBanned && Bukkit.getBannedPlayers().stream().anyMatch(offPlayer -> offPlayer != null && obj.equals(offPlayer.getName()));
        }
        assert (false);
        return false;
    }

    @Override
    protected String getPropertyName() {
        return this.ipBanned ? "IP-banned" : "banned";
    }

    static {
        Skript.registerCondition(CondIsBanned.class, "%offlineplayers/strings% (is|are) banned", "%players/strings% (is|are) IP(-| |)banned", "%offlineplayers/strings% (isn't|is not|aren't|are not) banned", "%players/strings% (isn't|is not|aren't|are not) IP(-| |)banned");
    }
}

