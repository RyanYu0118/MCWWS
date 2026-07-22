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

@Name(value="Offline players")
@Description(value={"All players that have ever joined the server. This includes the players currently online."})
@Example(value="send \"Size of all players who have joined the server: %size of all offline players%\"")
@Since(value={"2.2-dev35"})
public class ExprOfflinePlayers
extends SimpleExpression<OfflinePlayer> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends OfflinePlayer> getReturnType() {
        return OfflinePlayer.class;
    }

    @Nullable
    protected OfflinePlayer[] get(Event event) {
        return Bukkit.getOfflinePlayers();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "offline players";
    }

    static {
        Skript.registerExpression(ExprOfflinePlayers.class, OfflinePlayer.class, ExpressionType.SIMPLE, "[(all [[of] the]|the)] offline[ ]players");
    }
}

