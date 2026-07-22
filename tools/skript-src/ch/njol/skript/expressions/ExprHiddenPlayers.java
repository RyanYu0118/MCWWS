/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
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
import java.util.ArrayList;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Hidden Players")
@Description(value={"The players hidden from a player that were hidden using the <a href='#EffEntityVisibility'>entity visibility</a> effect."})
@Example(value="message \"&lt;light red&gt;You are currently hiding: &lt;light gray&gt;%hidden players of the player%\"")
@Since(value={"2.3"})
public class ExprHiddenPlayers
extends SimpleExpression<Player> {
    private Expression<Player> viewers;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult result) {
        this.viewers = exprs[0];
        return true;
    }

    @Nullable
    public Player[] get(Event event) {
        ArrayList list = new ArrayList();
        for (Player player : this.viewers.getArray(event)) {
            list.addAll(player.spigot().getHiddenPlayers());
        }
        return list.toArray(new Player[0]);
    }

    @Nullable
    public Expression<Player> getViewers() {
        return this.viewers;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends Player> getReturnType() {
        return Player.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "hidden players for " + this.viewers.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprHiddenPlayers.class, Player.class, ExpressionType.PROPERTY, "[(all [[of] the]|the)] hidden players (of|for) %players%", "[(all [[of] the]|the)] players hidden (from|for|by) %players%");
    }
}

