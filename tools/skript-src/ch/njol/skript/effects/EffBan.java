/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.BanList$Type
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import java.net.InetSocketAddress;
import java.util.Date;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Ban")
@Description(value={"Bans or unbans a player or an IP address.", "If a reason is given, it will be shown to the player when they try to join the server while banned.", "A length of ban may also be given to apply a temporary ban. If it is absent for any reason, a permanent ban will be used instead.", "We recommend that you test your scripts so that no accidental permanent bans are applied.", "", "Note that banning people does not kick them from the server.", "You can optionally use 'and kick' or consider using the <a href='#EffKick'>kick effect</a> after applying a ban."})
@Example.Examples(value={@Example(value="unban player"), @Example(value="ban \"127.0.0.1\""), @Example(value="IP-ban the player because \"he is an idiot\""), @Example(value="ban player due to \"inappropriate language\" for 2 days"), @Example(value="ban and kick player due to \"inappropriate language\" for 2 days")})
@Since(value={"1.4, 2.1.1 (ban reason), 2.5 (timespan), 2.9.0 (kick)"})
public class EffBan
extends Effect {
    private Expression<?> players;
    @Nullable
    private Expression<String> reason;
    @Nullable
    private Expression<Timespan> expires;
    private boolean ban;
    private boolean ipBan;
    private boolean kick;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.players = exprs[0];
        this.reason = exprs.length > 1 ? exprs[1] : null;
        this.expires = exprs.length > 1 ? exprs[2] : null;
        this.ban = matchedPattern % 2 == 0;
        this.ipBan = matchedPattern >= 2;
        this.kick = parseResult.hasTag("kick");
        return true;
    }

    @Override
    protected void execute(Event e) {
        String reason = this.reason != null ? this.reason.getSingle(e) : null;
        Timespan ts = this.expires != null ? this.expires.getSingle(e) : null;
        Date expires = ts != null ? new Date(System.currentTimeMillis() + ts.getAs(Timespan.TimePeriod.MILLISECOND)) : null;
        String source = "Skript ban effect";
        for (Object o : this.players.getArray(e)) {
            if (o instanceof Player) {
                Player player = (Player)o;
                if (this.ipBan) {
                    InetSocketAddress addr = player.getAddress();
                    if (addr == null) {
                        return;
                    }
                    String ip = addr.getAddress().getHostAddress();
                    if (this.ban) {
                        Bukkit.getBanList((BanList.Type)BanList.Type.IP).addBan(ip, reason, expires, "Skript ban effect");
                    } else {
                        Bukkit.getBanList((BanList.Type)BanList.Type.IP).pardon(ip);
                    }
                } else if (this.ban) {
                    Bukkit.getBanList((BanList.Type)BanList.Type.NAME).addBan(player.getName(), reason, expires, "Skript ban effect");
                } else {
                    Bukkit.getBanList((BanList.Type)BanList.Type.NAME).pardon(player.getName());
                }
                if (!this.kick) continue;
                player.kickPlayer(reason);
                continue;
            }
            if (o instanceof OfflinePlayer) {
                String name = ((OfflinePlayer)o).getName();
                if (name == null) {
                    return;
                }
                if (this.ban) {
                    Bukkit.getBanList((BanList.Type)BanList.Type.NAME).addBan(name, reason, expires, "Skript ban effect");
                    continue;
                }
                Bukkit.getBanList((BanList.Type)BanList.Type.NAME).pardon(name);
                continue;
            }
            if (o instanceof String) {
                String s = (String)o;
                if (this.ban) {
                    Bukkit.getBanList((BanList.Type)BanList.Type.IP).addBan(s, reason, expires, "Skript ban effect");
                    Bukkit.getBanList((BanList.Type)BanList.Type.NAME).addBan(s, reason, expires, "Skript ban effect");
                    continue;
                }
                Bukkit.getBanList((BanList.Type)BanList.Type.IP).pardon(s);
                Bukkit.getBanList((BanList.Type)BanList.Type.NAME).pardon(s);
                continue;
            }
            assert (false);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        if (this.ipBan) {
            builder.append((Object)"IP");
        }
        builder.append((Object)(this.ban ? "ban" : "unban"));
        if (this.kick) {
            builder.append((Object)"and kick");
        }
        builder.append((Object)this.players);
        if (this.reason != null) {
            builder.append("on account of", this.reason);
        }
        if (this.expires != null) {
            builder.append("for", this.expires);
        }
        return builder.toString();
    }

    static {
        Skript.registerEffect(EffBan.class, "ban [kick:and kick] %strings/offlineplayers% [(by reason of|because [of]|on account of|due to) %-string%] [for %-timespan%]", "unban %strings/offlineplayers%", "ban [kick:and kick] %players% by IP [(by reason of|because [of]|on account of|due to) %-string%] [for %-timespan%]", "unban %players% by IP", "IP(-| )ban [kick:and kick] %players% [(by reason of|because [of]|on account of|due to) %-string%] [for %-timespan%]", "(IP(-| )unban|un[-]IP[-]ban) %players%");
    }
}

