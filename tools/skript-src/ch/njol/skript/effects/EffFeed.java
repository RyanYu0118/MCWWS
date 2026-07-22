/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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
import ch.njol.util.Kleenean;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Feed")
@Description(value={"Feeds the specified players."})
@Example.Examples(value={@Example(value="feed all players"), @Example(value="feed the player by 5 beefs")})
@Since(value={"2.2-dev34"})
public class EffFeed
extends Effect {
    private Expression<Player> players;
    @Nullable
    private Expression<Number> beefs;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.players = exprs[0];
        this.beefs = exprs[1];
        return true;
    }

    @Override
    protected void execute(Event e) {
        int level = 20;
        if (this.beefs != null) {
            Number n = this.beefs.getSingle(e);
            if (n == null) {
                return;
            }
            level = n.intValue();
        }
        for (Player player : this.players.getArray(e)) {
            player.setFoodLevel(player.getFoodLevel() + level);
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "feed " + this.players.toString(e, debug) + (String)(this.beefs != null ? " by " + this.beefs.toString(e, debug) : "");
    }

    static {
        Skript.registerEffect(EffFeed.class, "feed [the] %players% [by %-number% [beef[s]]]");
    }
}

