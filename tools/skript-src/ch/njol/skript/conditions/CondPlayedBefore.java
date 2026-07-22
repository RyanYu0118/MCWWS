/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Has Played Before")
@Description(value={"Checks whether a player has played on this server before. You can also use <a href='#first_join'>on first join</a> if you want to make triggers for new players."})
@Example.Examples(value={@Example(value="player has played on this server before"), @Example(value="player hasn't played before")})
@Since(value={"1.4, 2.7 (multiple players)"})
public class CondPlayedBefore
extends Condition {
    private Expression<OfflinePlayer> players;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.players = exprs[0];
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event e) {
        return this.players.check(e, OfflinePlayer::hasPlayedBefore, this.isNegated());
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.players.toString(e, debug) + (this.isNegated() ? (this.players.isSingle() ? " hasn't" : " haven't") : (this.players.isSingle() ? " has" : " have")) + " played on this server before";
    }

    static {
        Skript.registerCondition(CondPlayedBefore.class, "%offlineplayers% [(has|have|did)] [already] play[ed] [on (this|the) server] (before|already)", "%offlineplayers% (has not|hasn't|have not|haven't|did not|didn't) [(already|yet)] play[ed] [on (this|the) server] (before|already|yet)");
    }
}

