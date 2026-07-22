/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.event.Event
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
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="All Banned Players/IPs")
@Description(value={"Obtains the list of all banned players or IP addresses."})
@Example(value="command /banlist:\n\ttrigger:\n\t\tsend all the banned players\n")
@Since(value={"2.7"})
public class ExprAllBannedEntries
extends SimpleExpression<Object> {
    private boolean ip;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.ip = parseResult.hasTag("ips");
        return true;
    }

    @Override
    @Nullable
    protected Object[] get(Event event) {
        if (this.ip) {
            return Bukkit.getIPBans().toArray(new String[0]);
        }
        return Bukkit.getBannedPlayers().toArray(new OfflinePlayer[0]);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<?> getReturnType() {
        return this.ip ? String.class : OfflinePlayer.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "all banned " + (this.ip ? "ip addresses" : "players");
    }

    static {
        Skript.registerExpression(ExprAllBannedEntries.class, Object.class, ExpressionType.SIMPLE, "[all [[of] the]|the] banned (players|ips:(ips|ip addresses))");
    }
}

